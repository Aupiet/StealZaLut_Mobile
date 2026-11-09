"""Non-destructive 3-Stage Image Processing and 3D LUT Baking Pipeline.

Stage 1: Pure Filter generation (cached raw LUT & initial transfer).
Stage 2: Filter Refinement (OKLab-based masks: Luma Lock, Neutral Axis, Skin Tone, Extremes, Spatial 3D).
Stage 3: Post-Grading (Exposure, Contrast, Highlights, Shadows, Temp, Tint, Vibrance, Saturation + 2D Effects).
"""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any, Dict, Optional, Union

import numpy as np
from numpy.typing import NDArray

from stealzalut.core.color_space import (
    linear_to_srgb,
    oklab_to_rgb,
    rgb_to_oklab,
    smoothstep,
    srgb_to_linear,
)
from stealzalut.core.lut_engine import (
    _gaussian_smooth_3d,
    generate_identity_lattice,
)
from stealzalut.core.lightroom_color import apply_full_lightroom_color_pipeline
from stealzalut.core.lightroom_models import LightroomDevelopSettings
from stealzalut.core.lightroom_spatial import apply_full_lightroom_spatial_pipeline


# ---------------------------------------------------------------------------
# Calibrated Constants for Perceptual Masks in OKLab
# ---------------------------------------------------------------------------

# Universal human skin tone cluster in OKLab space
# Typical skin coordinates: a in [0.03, 0.09], b in [0.04, 0.10]
SKIN_H_CENTER: float = 0.86  # ~49 degrees in radians (atan2(b, a))
SKIN_C_CENTER: float = 0.092  # Chroma magnitude sqrt(a^2 + b^2)
SKIN_H_SIGMA: float = 0.35  # Angular dispersion
SKIN_C_SIGMA: float = 0.045  # Radial chroma dispersion

NEUTRAL_CHROMA_THRESHOLD: float = 0.08  # OKLab chroma boundary for pure neutrals


# ---------------------------------------------------------------------------
# Parameter Data Classes
# ---------------------------------------------------------------------------

@dataclass
class Step2Params:
    """Filter Refinement Parameters (Stage 2).

    Attributes:
        global_intensity: Blend between original and refined look [0.0, 1.0].
        luma_lock: Source luminance preservation factor [0.0, 1.0].
        neutral_axis_lock: Protection factor for neutral grays, whites, and blacks [0.0, 1.0].
        skin_tone_lock: Protection factor for human skin hues [0.0, 1.0].
        extremes_rolloff: Highlight & shadow protection factor [0.0, 1.0].
        spatial_3d: 3D Gaussian lattice smoothing strength [0.0, 1.0].
    """
    global_intensity: float = 1.0
    luma_lock: float = 0.0
    neutral_axis_lock: float = 0.0
    skin_tone_lock: float = 0.0
    extremes_rolloff: float = 0.0
    spatial_3d: float = 0.0

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> Step2Params:
        if not data:
            return cls()
        return cls(
            global_intensity=float(np.clip(data.get("global_intensity", 1.0), 0.0, 1.0)),
            luma_lock=float(np.clip(data.get("luma_lock", 0.0), 0.0, 1.0)),
            neutral_axis_lock=float(np.clip(data.get("neutral_axis_lock", 0.0), 0.0, 1.0)),
            skin_tone_lock=float(np.clip(data.get("skin_tone_lock", 0.0), 0.0, 1.0)),
            extremes_rolloff=float(np.clip(data.get("extremes_rolloff", 0.0), 0.0, 1.0)),
            spatial_3d=float(np.clip(data.get("spatial_3d", 0.0), 0.0, 1.0)),
        )

    def to_dict(self) -> Dict[str, float]:
        return asdict(self)


@dataclass
class Step3Params:
    """Post-Grading Parameters (Stage 3).

    Colorimetric 1D/3D adjustments are baked into exported .cube LUTs,
    while 2D spatial effects (clarity, sharpen, grain) apply to images only.
    """
    exposure: float = 0.0       # [-2.0, +2.0] EV
    contrast: float = 0.0       # [-1.0, +1.0]
    highlights: float = 0.0     # [-1.0, +1.0]
    shadows: float = 0.0        # [-1.0, +1.0]
    temperature: float = 0.0    # [-1.0, +1.0] (Cool / Warm)
    tint: float = 0.0           # [-1.0, +1.0] (Green / Magenta)
    vibrance: float = 0.0       # [-1.0, +1.0] (Skin-safe saturation)
    saturation: float = 0.0     # [-1.0, +1.0] (Global saturation)
    clarity: float = 0.0        # [0.0, 1.0] (Local midtone contrast)
    sharpen: float = 0.0        # [0.0, 1.0] (Edge sharpness)
    grain: float = 0.0          # [0.0, 1.0] (Photographic film grain)

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> Step3Params:
        if not data:
            return cls()
        return cls(
            exposure=float(np.clip(data.get("exposure", 0.0), -2.0, 2.0)),
            contrast=float(np.clip(data.get("contrast", 0.0), -1.0, 1.0)),
            highlights=float(np.clip(data.get("highlights", 0.0), -1.0, 1.0)),
            shadows=float(np.clip(data.get("shadows", 0.0), -1.0, 1.0)),
            temperature=float(np.clip(data.get("temperature", 0.0), -1.0, 1.0)),
            tint=float(np.clip(data.get("tint", 0.0), -1.0, 1.0)),
            vibrance=float(np.clip(data.get("vibrance", 0.0), -1.0, 1.0)),
            saturation=float(np.clip(data.get("saturation", 0.0), -1.0, 1.0)),
            clarity=float(np.clip(data.get("clarity", 0.0), 0.0, 1.0)),
            sharpen=float(np.clip(data.get("sharpen", 0.0), 0.0, 1.0)),
            grain=float(np.clip(data.get("grain", 0.0), 0.0, 1.0)),
        )

    def to_dict(self) -> Dict[str, float]:
        return asdict(self)


# ---------------------------------------------------------------------------
# STAGE 2: FILTER REFINEMENT (OKLab Masks & Blending)
# ---------------------------------------------------------------------------

def apply_step2_filter_refinement(
    orig_rgb: NDArray[np.float32],
    pure_rgb: NDArray[np.float32],
    params: Step2Params,
) -> NDArray[np.float32]:
    """Execute Stage 2: Refine and blend a pure filter look towards the original.

    Operates in the perceptual OKLab space using continuous, non-linear masks.
    Can be called on an RGB image (H, W, 3) or a 3D LUT lattice (N, N, N, 3) / (N^3, 3).

    Args:
        orig_rgb: Original image or identity lattice, float32 in [0.0, 1.0].
        pure_rgb: Pure transferred image or raw 3D LUT, float32 in [0.0, 1.0].
        params: Active Step2Params configuration.

    Returns:
        Refined float32 array matching the shape of pure_rgb, bounded to [0.0, 1.0].
    """
    orig_f = np.clip(orig_rgb.astype(np.float32), 0.0, 1.0)
    pure_f = np.clip(pure_rgb.astype(np.float32), 0.0, 1.0)

    # 1. Spatial 3D Smoothing (only if input is a 3D LUT volume or cubic lattice)
    if params.spatial_3d > 1e-4 and (pure_f.ndim == 4 or (pure_f.ndim == 2 and pure_f.shape[0] in (17**3, 33**3, 64**3, 65**3))):
        is_2d = pure_f.ndim == 2
        orig_shape = pure_f.shape
        if is_2d:
            size = int(round(pure_f.shape[0] ** (1.0 / 3.0)))
            grid_3d = pure_f.reshape((size, size, size, 3))
        else:
            grid_3d = pure_f

        # Scale sigma dynamically between 0.5 and 1.8 according to spatial_3d strength
        smooth_sigma = float(0.5 + 1.3 * params.spatial_3d)
        smoothed = _gaussian_smooth_3d(grid_3d, sigma=smooth_sigma)
        alpha_s = float(params.spatial_3d)
        pure_f = (1.0 - alpha_s) * grid_3d + alpha_s * smoothed
        if is_2d:
            pure_f = pure_f.reshape(orig_shape)

    # Fast bypass: if intensity is 0, strictly return original/identity
    if params.global_intensity <= 1e-6:
        return orig_f.copy()

    # Convert both representations to OKLab
    oklab_orig = rgb_to_oklab(orig_f)
    oklab_pure = rgb_to_oklab(pure_f)

    l_orig = oklab_orig[..., 0]
    a_orig = oklab_orig[..., 1]
    b_orig = oklab_orig[..., 2]

    l_pure = oklab_pure[..., 0]
    a_pure = oklab_pure[..., 1]
    b_pure = oklab_pure[..., 2]

    # 2. Luma Lock: Interpolate luminance
    if params.luma_lock > 1e-4:
        alpha_luma = params.luma_lock
        l_out = (1.0 - alpha_luma) * l_pure + alpha_luma * l_orig
    else:
        l_out = l_pure

    # 3. Neutral Axis Lock: Protect low-chroma grays, blacks, and whites
    c_orig = np.sqrt(a_orig**2 + b_orig**2)
    if params.neutral_axis_lock > 1e-4:
        m_neutral = 1.0 - smoothstep(0.0, NEUTRAL_CHROMA_THRESHOLD, c_orig)
        w_neutral = params.neutral_axis_lock * m_neutral
    else:
        w_neutral = np.zeros_like(l_orig)

    # 4. Skin Tone Protection: Gaussian ellipsoidal cluster in OKLab hue/chroma
    if params.skin_tone_lock > 1e-4:
        h_orig = np.arctan2(b_orig, a_orig)
        # Circular angular difference wrapped to [-pi, pi]
        dh = (h_orig - SKIN_H_CENTER + np.pi) % (2.0 * np.pi) - np.pi
        d_skin_sq = (dh / SKIN_H_SIGMA) ** 2 + ((c_orig - SKIN_C_CENTER) / SKIN_C_SIGMA) ** 2
        m_skin = np.exp(-0.5 * d_skin_sq)
        w_skin = params.skin_tone_lock * m_skin
    else:
        w_skin = np.zeros_like(l_orig)

    # 5. Extremes Roll-off: Protect deep shadows (L < 0.15) and bright highlights (L > 0.85)
    if params.extremes_rolloff > 1e-4:
        m_shadows = 1.0 - smoothstep(0.0, 0.15, l_orig)
        m_highlights = smoothstep(0.85, 1.0, l_orig)
        m_extremes = np.clip(m_shadows + m_highlights, 0.0, 1.0)
        w_extremes = params.extremes_rolloff * m_extremes
    else:
        w_extremes = np.zeros_like(l_orig)

    # 6. Combined Protection Mask & Chrominance Blending
    w_protect = 1.0 - (1.0 - w_neutral) * (1.0 - w_skin) * (1.0 - w_extremes)
    w_protect = np.clip(w_protect, 0.0, 1.0)

    a_blended = (1.0 - w_protect) * a_pure + w_protect * a_orig
    b_blended = (1.0 - w_protect) * b_pure + w_protect * b_orig

    # Reconstruct RGB from refined OKLab coordinates
    oklab_refined = np.stack([l_out, a_blended, b_blended], axis=-1)
    rgb_masked = oklab_to_rgb(oklab_refined)

    # 7. Global Intensity Blending
    intensity = float(params.global_intensity)
    final_rgb = (1.0 - intensity) * orig_f + intensity * rgb_masked
    return np.clip(final_rgb, 0.0, 1.0).astype(np.float32)


# ---------------------------------------------------------------------------
# STAGE 3: POST-GRADING (Colorimetric & 2D Spatial)
# ---------------------------------------------------------------------------

def _apply_simple_box_blur(img: NDArray[np.float32], radius: int = 2) -> NDArray[np.float32]:
    """Lightweight 2D spatial box blur in pure NumPy."""
    if radius <= 0:
        return img.copy()
    h, w = img.shape[:2]
    # Pad image
    padded = np.pad(img, ((radius, radius), (radius, radius), (0, 0)), mode="edge")
    # Integral image or uniform sliding sum
    # Fast uniform sliding mean along axis 0
    k_size = 2 * radius + 1
    v_blur = np.zeros_like(img)
    for i in range(k_size):
        v_blur += padded[i : i + h, radius : radius + w]
    v_blur /= float(k_size)

    # Sliding mean along axis 1
    v_padded = np.pad(v_blur, ((0, 0), (radius, radius), (0, 0)), mode="edge")
    out = np.zeros_like(img)
    for j in range(k_size):
        out += v_padded[:, j : j + w]
    out /= float(k_size)
    return out


def apply_step3_color_grading(
    rgb: NDArray[np.float32],
    params: Step3Params,
    is_lut_baking: bool = False,
    lr_settings: Optional[Union[LightroomDevelopSettings, Dict[str, Any]]] = None,
) -> NDArray[np.float32]:
    """Execute Stage 3: Post-Grading colorimetric and spatial enhancements.

    When `is_lut_baking` is True, 2D spatial operations (clarity, sharpen, grain)
    are strictly bypassed because a 3D LUT is a point-operation color mapping.

    Args:
        rgb: Float32 image array in [0.0, 1.0].
        params: Active Step3Params configuration.
        is_lut_baking: True when applying to 3D LUT identity lattice for .cube baking.
        lr_settings: Optional full Adobe Lightroom Classic develop settings.

    Returns:
        Graded float32 array in [0.0, 1.0].
    """
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    # If Lightroom develop settings are provided, run through Lightroom engine
    if lr_settings is not None:
        if isinstance(lr_settings, dict):
            lr_obj = LightroomDevelopSettings.from_dict(lr_settings)
        else:
            lr_obj = lr_settings

        out = apply_full_lightroom_color_pipeline(out, lr_obj)
        if not is_lut_baking and out.ndim >= 2 and min(out.shape[:2]) >= 16:
            out = apply_full_lightroom_spatial_pipeline(out, lr_obj)
        return np.clip(out, 0.0, 1.0).astype(np.float32)

    # 1. Linear Exposure Compensation (EV)
    if abs(params.exposure) > 1e-4:
        lin = srgb_to_linear(out)
        lin_exp = lin * (2.0 ** params.exposure)
        out = linear_to_srgb(lin_exp)

    # 2. Contrast Adjustment
    if abs(params.contrast) > 1e-4:
        centered = out - 0.5
        if params.contrast > 0.0:
            c_factor = 1.0 + params.contrast * 0.8
            norm_factor = 2.0 * np.tanh(0.75 * c_factor)
            out = 0.5 + np.tanh(centered * 1.5 * c_factor) / norm_factor
        else:
            # Linear softening towards midtone 0.5 reduces variance smoothly
            soften = 1.0 + params.contrast * 0.6  # for contrast = -0.5, factor is 0.70
            out = 0.5 + centered * soften
        out = np.clip(out, 0.0, 1.0)

    # 3. Highlights Recovery / Compression
    if abs(params.highlights) > 1e-4:
        high_weight = np.clip((out - 0.25) / 0.75, 0.0, 1.0) ** 1.5
        out = out + params.highlights * 0.32 * high_weight * out
        out = np.clip(out, 0.0, 1.0)

    # 4. Shadows Lifting / Darkening
    if abs(params.shadows) > 1e-4:
        shadow_weight = np.clip(1.0 - 2.0 * out, 0.0, 1.0) ** 1.8
        out = out + params.shadows * 0.28 * shadow_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    # 5. Temperature & Tint Adjustment
    if abs(params.temperature) > 1e-4 or abs(params.tint) > 1e-4:
        r = out[..., 0]
        g = out[..., 1]
        b = out[..., 2]

        if abs(params.temperature) > 1e-4:
            # Warm: boost red, reduce blue. Cool: inverse
            t = params.temperature
            r = r * (1.0 + 0.18 * t)
            b = b * (1.0 - 0.18 * t)

        if abs(params.tint) > 1e-4:
            # Green / Magenta balance
            ti = params.tint
            g = g * (1.0 - 0.14 * ti)
            r = r * (1.0 + 0.06 * ti)
            b = b * (1.0 + 0.06 * ti)

        out = np.clip(np.stack([r, g, b], axis=-1), 0.0, 1.0)

    # 6. Vibrance & Saturation Adjustment
    if abs(params.vibrance) > 1e-4 or abs(params.saturation) > 1e-4:
        r, g, b = out[..., 0], out[..., 1], out[..., 2]
        max_c = np.maximum(np.maximum(r, g), b)
        min_c = np.minimum(np.minimum(r, g), b)
        lum = 0.299 * r + 0.587 * g + 0.114 * b

        delta = max_c - min_c
        sat = np.where(max_c > 1e-5, delta / np.maximum(max_c, 1e-5), 0.0)

        # Vibrance favors desaturated areas while preserving skin tones
        v_boost = params.vibrance * (1.0 - sat) * 0.60
        s_boost = params.saturation * 0.50
        total_boost = 1.0 + v_boost + s_boost

        out_r = lum + (r - lum) * total_boost
        out_g = lum + (g - lum) * total_boost
        out_b = lum + (b - lum) * total_boost
        out = np.clip(np.stack([out_r, out_g, out_b], axis=-1), 0.0, 1.0)

    # 7. 2D Spatial Effects (Bypassed during LUT Baking)
    if not is_lut_baking and out.ndim >= 2 and out.shape[0] >= 16 and out.shape[1] >= 16:
        # Clarity: Local midtone contrast boost using unsharp high-pass
        if params.clarity > 1e-4:
            blur_clarity = _apply_simple_box_blur(out, radius=max(2, min(out.shape[:2]) // 64))
            high_pass = out - blur_clarity
            # Weight towards midtones (bell curve centered at 0.5)
            lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
            midtone_weight = np.clip(1.0 - 4.0 * (lum - 0.5) ** 2, 0.0, 1.0)[..., np.newaxis]
            out = np.clip(out + params.clarity * 0.35 * high_pass * midtone_weight, 0.0, 1.0)

        # Sharpen: Unsharp masking
        if params.sharpen > 1e-4:
            blur_sharp = _apply_simple_box_blur(out, radius=1)
            edge_delta = out - blur_sharp
            out = np.clip(out + params.sharpen * 0.45 * edge_delta, 0.0, 1.0)

        # Grain: Film grain simulation modulated by luminance
        if params.grain > 1e-4:
            lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
            # Grain is most apparent in midtones and suppressed in pure crushed blacks and clipped whites
            grain_weight = np.clip(4.0 * lum * (1.0 - lum), 0.0, 1.0)[..., np.newaxis]
            rng = np.random.RandomState(42)  # Deterministic seed for preview stability
            noise = rng.normal(0.0, 0.08 * params.grain, size=out.shape).astype(np.float32)
            out = np.clip(out + noise * grain_weight, 0.0, 1.0)

    return np.clip(out, 0.0, 1.0).astype(np.float32)


# ---------------------------------------------------------------------------
# UNIFIED 3D LUT BAKING
# ---------------------------------------------------------------------------

def bake_full_pipeline_lut(
    raw_lut: NDArray[np.float32],
    step2_params: Step2Params,
    step3_params: Step3Params,
    size: int = 33,
    lr_settings: Optional[Union[LightroomDevelopSettings, Dict[str, Any]]] = None,
) -> NDArray[np.float32]:
    """Bake Stages 1, 2, and 3 (Colorimetric) directly into a 3D LUT volume.

    Transforms the identity lattice through Stage 2 refinement (against raw_lut)
    and Stage 3 color grading (with is_lut_baking=True).

    Args:
        raw_lut: Raw 3D LUT array from Stage 1, shape (size, size, size, 3) or (size^3, 3).
        step2_params: Active Stage 2 parameters.
        step3_params: Active Stage 3 parameters.
        size: Grid size per axis (default 33).
        lr_settings: Optional full Adobe Lightroom Classic develop settings.

    Returns:
        Fully baked 3D LUT array of shape (size^3, 3) with values in [0.0, 1.0].
    """
    identity_2d = generate_identity_lattice(size)

    if raw_lut is None or raw_lut.size == 0:
        pure_lut = identity_2d.copy()
    else:
        pure_lut = raw_lut.reshape(-1, 3).astype(np.float32)

    # Stage 2 Refinement on the 3D volume
    refined_lut = apply_step2_filter_refinement(
        orig_rgb=identity_2d,
        pure_rgb=pure_lut,
        params=step2_params,
    )

    # Stage 3 Colorimetric Grading on the 3D volume (spatial effects bypassed)
    baked_lut = apply_step3_color_grading(
        refined_lut,
        params=step3_params,
        is_lut_baking=True,
        lr_settings=lr_settings,
    )

    # Clamping and sanity validation (no NaN or Inf)
    clean_lut = np.nan_to_num(baked_lut, copy=False, nan=0.0, posinf=1.0, neginf=0.0)
    return np.clip(clean_lut, 0.0, 1.0).astype(np.float32).reshape(-1, 3)
