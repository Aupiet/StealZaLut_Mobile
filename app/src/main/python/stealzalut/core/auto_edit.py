"""LightCraft-calibrated Auto Edit and Tone Mapping engine for StealZaLUT.

Reverse-engineered from LightCraft (LightCraft develop.auto):
- Exposure estimation: Evaluates log2 deviation of linear Rec.2020 luminance
  from the standard 18% gray card (0.18 reflectance):
    EV_diff = mean(log2(max(Y, 1e-6) / 0.18))
    exposure = clamp(-0.41 * EV_diff - 0.58, -2.0, 3.0)
- Auto Develop Parameters:
    * Exposure: dynamic EV adjustment
    * Contrast: +68.0 (symmetric S-curve around midtones)
    * Highlights: -66.0 (smooth shoulder roll-off protecting bright details)
    * Shadows: +52.0 (smooth toe lift opening dark shadow details)
    * Whites: 0.0
    * Blacks: 0.0
    * Vibrance: +30.0 (smart saturation boost on muted tones)
    * Saturation: 0.0
- 3D LUT generation for 100% WYSIWYG consistency and Adobe .cube export.
"""

from __future__ import annotations

import os
import time
from typing import Any, Dict, Optional, Union

import numpy as np
from numpy.typing import NDArray

from stealzalut.core.lut_engine import apply_lut_trilinear, generate_identity_grid
from stealzalut.utils.image_io import (
    load_proxy_image,
    save_image,
    to_float32,
)


# Rec.2020 luminance coefficients (ITU-R BT.2020)
REC2020_R = 0.2627
REC2020_G = 0.6780
REC2020_B = 0.0593

# LightCraft calibration constants
REF_MIDDLE_GRAY = 0.18
EPSILON_LUM = 1e-6
EXPOSURE_SLOPE = -0.41
EXPOSURE_INTERCEPT = -0.58
EXPOSURE_MIN = -2.0
EXPOSURE_MAX = 3.0

DEFAULT_AUTO_CONTRAST = 68.0
DEFAULT_AUTO_HIGHLIGHTS = -66.0
DEFAULT_AUTO_SHADOWS = 52.0
DEFAULT_AUTO_WHITES = 0.0
DEFAULT_AUTO_BLACKS = 0.0
DEFAULT_AUTO_VIBRANCE = 30.0
DEFAULT_AUTO_SATURATION = 0.0


def srgb_to_linear(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert sRGB float array in [0.0, 1.0] to linear light."""
    return np.where(
        rgb <= 0.04045,
        rgb / 12.92,
        ((rgb + 0.055) / 1.055) ** 2.4,
    ).astype(np.float32)


def linear_to_srgb(lin: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert linear light float array to sRGB in [0.0, 1.0]."""
    clamped = np.clip(lin, 0.0, None)
    return np.where(
        clamped <= 0.0031308,
        clamped * 12.92,
        1.055 * (clamped ** (1.0 / 2.4)) - 0.055,
    ).astype(np.float32)


def _ensure_float32(image: NDArray) -> NDArray[np.float32]:
    """Ensure array is float32 with values normalized in [0.0, 1.0]."""
    if np.issubdtype(image.dtype, np.integer):
        return image.astype(np.float32) / np.float32(255.0)
    return image.astype(np.float32)


def calculate_auto_exposure(
    image: NDArray[Union[np.float32, np.uint8]],
    max_dim: int = 512,
) -> float:
    """Calculate the optimal exposure adjustment (EV) based on LightCraft calibration.

    Args:
        image: RGB image array, either float32 [0.0, 1.0] or uint8 [0..255].
        max_dim: Maximum dimension for proxy downsampling (default 512).

    Returns:
        Exposure adjustment in EV, rounded to 2 decimal places, clamped to [-2.0, 3.0].
    """
    img_f = _ensure_float32(image)
    h, w = img_f.shape[:2]

    # Downsample to proxy resolution for fast and stable statistical pooling
    if max(h, w) > max_dim:
        scale = max_dim / float(max(h, w))
        step = max(1, int(round(1.0 / scale)))
        img_f = img_f[::step, ::step, :3]

    # Convert to linear light
    lin = srgb_to_linear(img_f)

    # Rec. 2020 luminance
    y = REC2020_R * lin[..., 0] + REC2020_G * lin[..., 1] + REC2020_B * lin[..., 2]

    # Epsilon clamp and log2 deviation from 18% middle gray
    y_clamped = np.maximum(y, EPSILON_LUM)
    v = np.log2(y_clamped / REF_MIDDLE_GRAY)
    v_mean = float(np.mean(v))

    # Linear calibration model derived from LightCraft
    exp_val = EXPOSURE_SLOPE * v_mean + EXPOSURE_INTERCEPT
    exp_clamped = float(np.clip(exp_val, EXPOSURE_MIN, EXPOSURE_MAX))
    return round(exp_clamped, 2)


def get_auto_edit_settings(
    image_or_exposure: Union[NDArray, float, int],
) -> Dict[str, float]:
    """Compute the complete LightCraft auto edit parameter set for an image or exposure value.

    Args:
        image_or_exposure: RGB image array or precomputed exposure float.

    Returns:
        Dictionary mapping control names to their calibrated values.
    """
    if isinstance(image_or_exposure, (int, float, np.floating)):
        exp_val = float(image_or_exposure)
    else:
        exp_val = calculate_auto_exposure(image_or_exposure)

    return {
        "exposure": exp_val,
        "contrast": DEFAULT_AUTO_CONTRAST,
        "highlights": DEFAULT_AUTO_HIGHLIGHTS,
        "shadows": DEFAULT_AUTO_SHADOWS,
        "whites": DEFAULT_AUTO_WHITES,
        "blacks": DEFAULT_AUTO_BLACKS,
        "vibrance": DEFAULT_AUTO_VIBRANCE,
        "saturation": DEFAULT_AUTO_SATURATION,
    }


def _apply_tone_curve(
    rgb: NDArray[np.float32],
    contrast: float = DEFAULT_AUTO_CONTRAST,
    highlights: float = DEFAULT_AUTO_HIGHLIGHTS,
    shadows: float = DEFAULT_AUTO_SHADOWS,
    whites: float = DEFAULT_AUTO_WHITES,
    blacks: float = DEFAULT_AUTO_BLACKS,
) -> NDArray[np.float32]:
    """Apply the LightCraft tone curve parameters in perceptual space.

    - Shadows (+): smoothly lifts dark areas while tapering off before midtones.
    - Highlights (-): smoothly compresses highlights (highlight recovery) above midtones.
    - Contrast (+): standard sigmoidal contrast S-curve around midtone 0.5.
    - Whites / Blacks: endpoint anchoring and dynamic range expansion.
    """
    out = np.clip(rgb.copy(), 0.0, 1.0)

    # 1. Shadows lifting (tapers off above 0.5)
    if abs(shadows) > 1e-4:
        s_norm = shadows / 100.0
        # Quadratic toe lift: (1 - 2*x)^2 for x < 0.5
        shadow_weight = np.clip(1.0 - 2.0 * out, 0.0, 1.0) ** 1.8
        out = out + s_norm * 0.25 * shadow_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    # 2. Highlights recovery / roll-off (tapers off below 0.3)
    if abs(highlights) > 1e-4:
        h_norm = highlights / 100.0  # typically negative, e.g. -0.66
        # Smooth shoulder curve above 0.3
        high_weight = np.clip((out - 0.25) / 0.75, 0.0, 1.0) ** 1.5
        out = out + h_norm * 0.32 * high_weight * out
        out = np.clip(out, 0.0, 1.0)

    # 3. Contrast adjustment (S-curve around 0.5)
    if abs(contrast) > 1e-4:
        c_factor = 1.0 + (contrast / 100.0) * 0.65
        centered = out - 0.5
        # Soft sigmoid contrast expansion
        out = 0.5 + np.tanh(centered * c_factor * 1.5) / (
            2.0 * np.tanh(0.75 * c_factor)
        )
        out = np.clip(out, 0.0, 1.0)

    # 4. Whites and Blacks fine-tuning
    if abs(blacks) > 1e-4:
        b_norm = blacks / 100.0
        black_weight = np.clip(1.0 - 4.0 * out, 0.0, 1.0)
        out = out + b_norm * 0.1 * black_weight
        out = np.clip(out, 0.0, 1.0)

    if abs(whites) > 1e-4:
        w_norm = whites / 100.0
        white_weight = np.clip((out - 0.75) * 4.0, 0.0, 1.0)
        out = out + w_norm * 0.1 * white_weight
        out = np.clip(out, 0.0, 1.0)

    return out.astype(np.float32)


def _apply_vibrance(
    rgb: NDArray[np.float32],
    vibrance: float = DEFAULT_AUTO_VIBRANCE,
    saturation: float = DEFAULT_AUTO_SATURATION,
) -> NDArray[np.float32]:
    """Apply selective vibrance and saturation adjustments.

    Vibrance selectively boosts low-saturated colors while preserving skin tones
    and saturated highlights to avoid unnatural color clipping.
    """
    if abs(vibrance) < 1e-4 and abs(saturation) < 1e-4:
        return rgb

    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    max_c = np.maximum(np.maximum(r, g), b)
    min_c = np.minimum(np.minimum(r, g), b)
    lum = 0.299 * r + 0.587 * g + 0.114 * b

    delta = max_c - min_c
    sat = np.where(max_c > 1e-5, delta / np.maximum(max_c, 1e-5), 0.0)

    # Vibrance boost: stronger on muted colors (1.0 - sat), dampened on already saturated
    v_boost = (vibrance / 100.0) * (1.0 - sat) * 0.65
    s_boost = (saturation / 100.0) * 0.5
    total_boost = 1.0 + v_boost + s_boost

    # Scale RGB relative to luminance
    out_r = lum + (r - lum) * total_boost
    out_g = lum + (g - lum) * total_boost
    out_b = lum + (b - lum) * total_boost

    return np.clip(np.stack([out_r, out_g, out_b], axis=-1), 0.0, 1.0).astype(
        np.float32
    )


def apply_auto_edit(
    image: NDArray[Union[np.float32, np.uint8]],
    settings: Optional[Dict[str, float]] = None,
) -> NDArray[np.float32]:
    """Execute the full LightCraft Auto Edit pipeline on an image.

    Pipeline stages:
    1. Exposure compensation in linear optical space (L -> L * 2^EV).
    2. Tone curve enhancement (Shadows recovery, Highlight roll-off, Midtone contrast).
    3. Vibrance color boost.

    Args:
        image: RGB image array.
        settings: Optional precomputed settings dictionary (if None, computed automatically).

    Returns:
        Enhanced float32 RGB image in [0.0, 1.0].
    """
    img_f = _ensure_float32(image)
    if settings is None:
        settings = get_auto_edit_settings(img_f)

    exp_val = settings.get("exposure", 0.0)
    contrast = settings.get("contrast", DEFAULT_AUTO_CONTRAST)
    highlights = settings.get("highlights", DEFAULT_AUTO_HIGHLIGHTS)
    shadows = settings.get("shadows", DEFAULT_AUTO_SHADOWS)
    whites = settings.get("whites", DEFAULT_AUTO_WHITES)
    blacks = settings.get("blacks", DEFAULT_AUTO_BLACKS)
    vibrance = settings.get("vibrance", DEFAULT_AUTO_VIBRANCE)
    saturation = settings.get("saturation", DEFAULT_AUTO_SATURATION)

    # 1. Linear Exposure Compensation
    lin = srgb_to_linear(img_f)
    lin_exp = lin * (2.0**exp_val)
    srgb_exp = linear_to_srgb(lin_exp)

    # 2. Tone Curve
    srgb_toned = _apply_tone_curve(
        srgb_exp,
        contrast=contrast,
        highlights=highlights,
        shadows=shadows,
        whites=whites,
        blacks=blacks,
    )

    # 3. Vibrance
    srgb_final = _apply_vibrance(srgb_toned, vibrance=vibrance, saturation=saturation)
    return srgb_final


def generate_auto_edit_lut(
    image: NDArray[Union[np.float32, np.uint8]],
    lut_size: int = 33,
    settings: Optional[Dict[str, float]] = None,
) -> NDArray[np.float32]:
    """Generate a 3-D LUT volume corresponding to the Auto Edit transformation.

    Passes the 3-D identity grid through the Auto Edit transformation,
    enabling WYSIWYG real-time GPU previews and Adobe .cube exports.

    Args:
        image: Source RGB image used to calibrate auto settings (if settings not given).
        lut_size: Lattice resolution per channel (17, 33, 65).
        settings: Optional precomputed settings dict.

    Returns:
        Float32 array of shape (lut_size, lut_size, lut_size, 3) in [0.0, 1.0].
    """
    if settings is None:
        settings = get_auto_edit_settings(image)

    grid = generate_identity_grid(lut_size)
    lut_applied = apply_auto_edit(grid, settings=settings)
    return np.clip(lut_applied, 0.0, 1.0).astype(np.float32)


def run_auto_edit_pipeline(
    src_path: str,
    output_path: str = "",
    max_preview_dim: int = 1024,
    lut_size: int = 33,
) -> Dict[str, Any]:
    """High-level pipeline executing Auto Edit and generating a WYSIWYG preview.

    Args:
        src_path: Path to the input image.
        output_path: Destination path for the preview image.
        max_preview_dim: Maximum preview long edge.
        lut_size: Grid size for the underlying 3D LUT.

    Returns:
        Status dictionary with preview_path, settings, and execution_time_ms.
    """
    t0 = time.perf_counter()

    if not src_path or not os.path.isfile(src_path):
        return {
            "status": "error",
            "error_code": "SOURCE_NOT_FOUND",
            "message": f"Source image file not found: {src_path}",
        }

    target_dim = max_preview_dim if max_preview_dim > 0 else 1024
    preview_arr = load_proxy_image(src_path, max_dim=target_dim)

    # Compute settings
    settings = get_auto_edit_settings(preview_arr)

    # Build 3D LUT and apply trilinearly for 100% WYSIWYG consistency
    lut_3d = generate_auto_edit_lut(preview_arr, lut_size=lut_size, settings=settings)
    graded_preview = apply_lut_trilinear(to_float32(preview_arr), lut_3d)

    # Save output preview
    if not output_path:
        src_dir = os.path.dirname(os.path.abspath(src_path))
        output_path = os.path.join(src_dir, "auto_edit_preview.jpg")

    save_image(graded_preview, output_path, quality=90)

    elapsed_ms = (time.perf_counter() - t0) * 1000.0

    return {
        "status": "success",
        "preview_path": output_path,
        "settings": settings,
        "exposure": settings["exposure"],
        "contrast": settings["contrast"],
        "highlights": settings["highlights"],
        "shadows": settings["shadows"],
        "whites": settings["whites"],
        "blacks": settings["blacks"],
        "vibrance": settings["vibrance"],
        "execution_time_ms": elapsed_ms,
    }
