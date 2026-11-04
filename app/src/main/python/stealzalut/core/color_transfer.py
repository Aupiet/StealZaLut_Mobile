"""Colour-transfer engine backed by *color-matcher* with resilient fallback.

This module provides professional color transfer between a source and reference
image with CIE LAB luminance preservation, contrast ratio clamping, and smooth
tanh-based soft clipping.
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import (
    CHROMA_BOOST_DEFAULT,
    DEFAULT_LUMINANCE_BINS,
    DEFAULT_METHOD,
    DEFAULT_PRESERVE_LUMINANCE,
    EPSILON,
    LUMINANCE_ALIGN_WEIGHT,
    MAX_CHROMA_BOOST,
    MAX_STD_RATIO,
    MIN_CHROMA_BOOST,
    MIN_STD_RATIO,
    SHADOW_ROLLOFF_L_THRESHOLD,
    SOFT_CLIP_HIGHLIGHT_THRESHOLD,
    SOFT_CLIP_SHADOW_THRESHOLD,
    SUPPORTED_METHODS,
    VIVID_CHROMA_PERCENTILE,
)

# Standard D65 sRGB to XYZ transformation matrix (IEC 61966-2-1)
_M_SRGB_TO_XYZ: NDArray[np.float32] = np.array(
    [
        [0.4124564, 0.3575761, 0.1804375],
        [0.2126729, 0.7151522, 0.0721750],
        [0.0193339, 0.1191920, 0.9503041],
    ],
    dtype=np.float32,
)

# Standard XYZ to sRGB inverse matrix
_M_XYZ_TO_SRGB: NDArray[np.float32] = np.array(
    [
        [3.2404542, -1.5371385, -0.4985314],
        [-0.9692660, 1.8760108, 0.0415560],
        [0.0556434, -0.2040259, 1.0572252],
    ],
    dtype=np.float32,
)

# CIE 1931 2° D65 standard illuminant reference white
_D65_WHITE: NDArray[np.float32] = np.array(
    [0.95047, 1.00000, 1.08883], dtype=np.float32
)

_CIE_DELTA: float = 6.0 / 29.0
_CIE_DELTA_CUBED: float = _CIE_DELTA**3
_CIE_3_DELTA_SQUARED: float = 3.0 * (_CIE_DELTA**2)


def rgb_to_lab(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert sRGB array in [0.0, 1.0] to CIE L*a*b*.

    Args:
        rgb: Float32 array of shape (..., 3) with values in [0.0, 1.0].

    Returns:
        Float32 array of shape (..., 3) with L in [0.0, 100.0],
        a and b in approximately [-128.0, 127.0].
    """
    rgb_clipped = np.clip(rgb, 0.0, 1.0)
    # Linearize sRGB gamma
    mask = rgb_clipped > 0.04045
    rgb_lin = np.where(
        mask,
        ((rgb_clipped + 0.055) / 1.055) ** 2.4,
        rgb_clipped / 12.92,
    )

    # sRGB -> XYZ
    xyz = rgb_lin @ _M_SRGB_TO_XYZ.T
    xyz_n = xyz / _D65_WHITE

    # Nonlinear transformation for CIE LAB
    f_xyz = np.where(
        xyz_n > _CIE_DELTA_CUBED,
        np.cbrt(xyz_n),
        xyz_n / _CIE_3_DELTA_SQUARED + (4.0 / 29.0),
    )

    l_channel = 116.0 * f_xyz[..., 1] - 16.0
    a_channel = 500.0 * (f_xyz[..., 0] - f_xyz[..., 1])
    b_channel = 200.0 * (f_xyz[..., 1] - f_xyz[..., 2])

    return np.stack([l_channel, a_channel, b_channel], axis=-1)


def lab_to_rgb(lab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert CIE L*a*b* array to sRGB in [0.0, 1.0].

    Args:
        lab: Float32 array of shape (..., 3).

    Returns:
        Float32 array of shape (..., 3) with sRGB values in [0.0, 1.0].
    """
    fy = (lab[..., 0] + 16.0) / 116.0
    fx = fy + lab[..., 1] / 500.0
    fz = fy - lab[..., 2] / 200.0

    f_xyz = np.stack([fx, fy, fz], axis=-1)
    xyz_n = np.where(
        f_xyz > _CIE_DELTA,
        f_xyz**3,
        _CIE_3_DELTA_SQUARED * (f_xyz - 4.0 / 29.0),
    )
    xyz = xyz_n * _D65_WHITE

    # XYZ -> linear sRGB
    rgb_lin = xyz @ _M_XYZ_TO_SRGB.T
    rgb_lin = np.clip(rgb_lin, 0.0, None)

    # Apply sRGB gamma
    rgb = np.where(
        rgb_lin > 0.0031308,
        1.055 * (rgb_lin ** (1.0 / 2.4)) - 0.055,
        12.92 * rgb_lin,
    )
    return np.clip(rgb, 0.0, 1.0)


def apply_soft_clip(
    x: NDArray[np.float32],
    shadow_th: float = SOFT_CLIP_SHADOW_THRESHOLD,
    highlight_th: float = SOFT_CLIP_HIGHLIGHT_THRESHOLD,
) -> NDArray[np.float32]:
    """Apply a smooth C1-continuous soft-knee compression using np.tanh.

    Protects shadow details (deepest 5%) and highlight roll-off (brightest 5%)
    to avoid harsh clipping and posterisation.

    Args:
        x: Float32 array with normalized values nominally in [0.0, 1.0].
        shadow_th: Lower threshold below which soft compression begins.
        highlight_th: Upper threshold above which soft compression begins.

    Returns:
        Smoothly compressed array with strictly bounded values in [0.0, 1.0].
    """
    out = x.copy()

    # Soft compression on highlights (x > highlight_th)
    m_hi = 1.0 - highlight_th
    mask_hi = x > highlight_th
    if np.any(mask_hi):
        out[mask_hi] = highlight_th + m_hi * np.tanh(
            (x[mask_hi] - highlight_th) / m_hi
        )

    # Soft compression on shadows (x < shadow_th)
    m_lo = shadow_th
    mask_lo = x < shadow_th
    if np.any(mask_lo):
        out[mask_lo] = shadow_th - m_lo * np.tanh((shadow_th - x[mask_lo]) / m_lo)

    return np.clip(out, 0.0, 1.0)


def luminance_binned_chroma_transfer(
    src_lab: NDArray[np.float32],
    ref_lab: NDArray[np.float32],
    num_bins: int = DEFAULT_LUMINANCE_BINS,
    vivid_percentile: float = VIVID_CHROMA_PERCENTILE,
    chroma_boost: float = CHROMA_BOOST_DEFAULT,
    shadow_th: float = SHADOW_ROLLOFF_L_THRESHOLD,
    luminance_align_weight: float = LUMINANCE_ALIGN_WEIGHT,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
) -> NDArray[np.float32]:
    """Transfer chroma across luminance-partitioned bins with vividness selection.

    Overcomes high-contrast scene bottlenecks (e.g. dark archways with vivid blue skies)
    by:
    1. Splitting luminance L into continuous Gaussian-weighted bins.
    2. Filtering out the dullest 30% desaturated reference pixels (vivid_mask) to capture true dominant hues.
    3. Applying a controlled standard deviation expansion factor (chroma boost).
    4. Gradually fading chrominance adjustments in deep shadows (L < 20) to prevent color noise.
    5. Softly mapping contrast percentiles while honoring preserve_luminance.

    Args:
        src_lab: Source CIE LAB array of shape (..., 3).
        ref_lab: Reference CIE LAB array of shape (..., 3).
        num_bins: Number of Gaussian luminance bins.
        vivid_percentile: Exclusion threshold for dullest reference pixels.
        chroma_boost: Chromatic dynamic expansion factor.
        shadow_th: Lightness threshold below which chromatic adjustments roll off.
        luminance_align_weight: Strength of soft quantile luminance alignment.
        preserve_luminance: Luminance preservation blend factor in [0.0, 1.0].

    Returns:
        Transferred CIE LAB float32 array with shape matching *src_lab*.
    """
    src_shape = src_lab.shape
    src_flat = src_lab.reshape(-1, 3)
    ref_flat = ref_lab.reshape(-1, 3)

    l_src = src_flat[:, 0]
    a_src = src_flat[:, 1]
    b_src = src_flat[:, 2]

    l_ref = ref_flat[:, 0]
    a_ref = ref_flat[:, 1]
    b_ref = ref_flat[:, 2]

    chroma_ref = np.sqrt(a_ref**2 + b_ref**2)

    # 1. Luminance band centers
    bin_centers = np.linspace(15.0, 85.0, num_bins, dtype=np.float32)
    step = float(bin_centers[1] - bin_centers[0]) if num_bins > 1 else 30.0
    sigma_l = step * 0.75

    w_src_list = []
    a_trans_bins = []
    b_trans_bins = []

    boost = float(np.clip(chroma_boost, MIN_CHROMA_BOOST, MAX_CHROMA_BOOST))

    # Global fallback statistics
    global_mu_a_ref = float(np.mean(a_ref))
    global_mu_b_ref = float(np.mean(b_ref))
    global_std_a_ref = float(np.std(a_ref))
    global_std_b_ref = float(np.std(b_ref))

    for center in bin_centers:
        # Gaussian weights for source and reference
        w_s = np.exp(-0.5 * ((l_src - center) / sigma_l) ** 2)
        w_r = np.exp(-0.5 * ((l_ref - center) / sigma_l) ** 2)
        w_src_list.append(w_s)

        # 2. Vivid reference pixel selection
        active_ref = w_r > 0.10
        if np.count_nonzero(active_ref) >= 16:
            ref_bin_chroma = chroma_ref[active_ref]
            th_chroma = float(np.percentile(ref_bin_chroma, vivid_percentile))
            vivid_mask = active_ref & (chroma_ref >= th_chroma)
            w_eff_r = w_r * vivid_mask.astype(np.float32)
        else:
            w_eff_r = w_r

        sum_w_r = float(np.sum(w_eff_r))
        if sum_w_r > float(EPSILON):
            mu_a_r = float(np.sum(w_eff_r * a_ref) / sum_w_r)
            mu_b_r = float(np.sum(w_eff_r * b_ref) / sum_w_r)
            std_a_r = float(np.sqrt(max(0.0, float(np.sum(w_eff_r * (a_ref - mu_a_r) ** 2) / sum_w_r))))
            std_b_r = float(np.sqrt(max(0.0, float(np.sum(w_eff_r * (b_ref - mu_b_r) ** 2) / sum_w_r))))
        else:
            mu_a_r = global_mu_a_ref
            mu_b_r = global_mu_b_ref
            std_a_r = global_std_a_ref
            std_b_r = global_std_b_ref

        # Source moments for this band
        sum_w_s = float(np.sum(w_s))
        if sum_w_s > float(EPSILON):
            mu_a_s = float(np.sum(w_s * a_src) / sum_w_s)
            mu_b_s = float(np.sum(w_s * b_src) / sum_w_s)
            std_a_s = float(np.sqrt(max(0.0, float(np.sum(w_s * (a_src - mu_a_s) ** 2) / sum_w_s))))
            std_b_s = float(np.sqrt(max(0.0, float(np.sum(w_s * (b_src - mu_b_s) ** 2) / sum_w_s))))
        else:
            mu_a_s = float(np.mean(a_src))
            mu_b_s = float(np.mean(b_src))
            std_a_s = float(np.std(a_src))
            std_b_s = float(np.std(b_src))

        # 3. Controlled chroma boost
        scale_a = float(np.clip((std_a_r / (std_a_s + float(EPSILON))) * boost, MIN_STD_RATIO, MAX_STD_RATIO))
        scale_b = float(np.clip((std_b_r / (std_b_s + float(EPSILON))) * boost, MIN_STD_RATIO, MAX_STD_RATIO))

        a_trans_bins.append((a_src - mu_a_s) * scale_a + mu_a_r)
        b_trans_bins.append((b_src - mu_b_s) * scale_b + mu_b_r)

    # Soft partition of unity across bins
    w_src_stack = np.stack(w_src_list, axis=0)
    w_src_norm = w_src_stack / (np.sum(w_src_stack, axis=0, keepdims=True) + float(EPSILON))

    a_trans_stack = np.stack(a_trans_bins, axis=0)
    b_trans_stack = np.stack(b_trans_bins, axis=0)

    a_trans = np.sum(w_src_norm * a_trans_stack, axis=0)
    b_trans = np.sum(w_src_norm * b_trans_stack, axis=0)

    # 4. Shadow roll-off (L < shadow_th fades to neutral to prevent color noise in blacks)
    t_shadow = np.clip(l_src / (shadow_th + float(EPSILON)), 0.0, 1.0)
    shadow_roll = (t_shadow ** 2.5).astype(np.float32)
    a_chroma = a_src + shadow_roll * (a_trans - a_src)
    b_chroma = b_src + shadow_roll * (b_trans - b_src)

    # 5. Soft contrast adaptation on L
    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
    if clamped_preserve < 1.0 and luminance_align_weight > 0.0:
        percentiles = np.linspace(0.0, 100.0, 21, dtype=np.float32)
        src_pct = np.percentile(l_src, percentiles)
        ref_pct = np.percentile(l_ref, percentiles)
        if float(src_pct[-1] - src_pct[0]) > float(EPSILON):
            l_aligned = np.interp(l_src, src_pct, ref_pct).astype(np.float32)
            l_soft = l_src + luminance_align_weight * (l_aligned - l_src)
            l_final = clamped_preserve * l_src + (1.0 - clamped_preserve) * l_soft
        else:
            l_final = l_src.copy()
    else:
        l_final = l_src.copy()

    l_final = np.nan_to_num(np.clip(l_final, 0.0, 100.0), nan=50.0, posinf=100.0, neginf=0.0)
    a_chroma = np.nan_to_num(a_chroma, nan=0.0, posinf=127.0, neginf=-128.0)
    b_chroma = np.nan_to_num(b_chroma, nan=0.0, posinf=127.0, neginf=-128.0)

    result_flat = np.stack([l_final, a_chroma, b_chroma], axis=-1)
    return result_flat.reshape(src_shape).astype(np.float32)


def _standard_reinhard_transfer(
    src_lab: NDArray[np.float32],
    ref_lab: NDArray[np.float32],
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
) -> NDArray[np.float32]:
    """Robust global statistical Reinhard transfer in CIE LAB space.

    Acts as an unbreakable baseline fallback if binned chroma transfer fails.

    Args:
        src_lab: Source CIE LAB array of shape (..., 3).
        ref_lab: Reference CIE LAB array of shape (..., 3).
        preserve_luminance: Lightness preservation blend factor in [0.0, 1.0].

    Returns:
        Transferred CIE LAB float32 array matching *src_lab* shape.
    """
    src_shape = src_lab.shape
    src_flat = src_lab.reshape(-1, 3)
    ref_flat = ref_lab.reshape(-1, 3)

    out_flat = np.empty_like(src_flat)
    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))

    for c in range(3):
        mu_s = float(np.mean(src_flat[:, c]))
        std_s = float(np.std(src_flat[:, c]))
        mu_r = float(np.mean(ref_flat[:, c]))
        std_r = float(np.std(ref_flat[:, c]))

        scale = float(np.clip(std_r / (std_s + float(EPSILON)), MIN_STD_RATIO, MAX_STD_RATIO))
        trans_c = (src_flat[:, c] - mu_s) * scale + mu_r

        if c == 0:
            trans_c = np.clip(trans_c, 0.0, 100.0)
            out_flat[:, 0] = clamped_preserve * src_flat[:, 0] + (1.0 - clamped_preserve) * trans_c
        else:
            out_flat[:, c] = trans_c

    out_flat[:, 0] = np.nan_to_num(np.clip(out_flat[:, 0], 0.0, 100.0), nan=50.0)
    out_flat[:, 1] = np.nan_to_num(out_flat[:, 1], nan=0.0)
    out_flat[:, 2] = np.nan_to_num(out_flat[:, 2], nan=0.0)

    return out_flat.reshape(src_shape).astype(np.float32)


def _numpy_statistical_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.uint8]:
    """Pure NumPy statistical Reinhard color transfer in CIE LAB space.

    Enhanced with luminance-binned chromatic transfer, vividness filtering,
    shadow roll-off, and soft-knee contrast roll-off. Fully safeguarded with
    fallbacks.

    Args:
        src_img: Source image (RGB uint8, shape (H, W, 3) or (N, 1, 3)).
        ref_img: Reference image (RGB uint8, shape (H_ref, W_ref, 3)).
        preserve_luminance: Lightness preservation weight in [0.0, 1.0].
        soft_clip: Whether to apply smooth tanh-based roll-off.

    Returns:
        Transferred image as RGB uint8 array matching src_img shape.
    """
    try:
        src_f = src_img.astype(np.float32) / 255.0
        ref_f = ref_img.astype(np.float32) / 255.0

        src_lab = rgb_to_lab(src_f)
        ref_lab = rgb_to_lab(ref_f)

        try:
            final_lab = luminance_binned_chroma_transfer(
                src_lab=src_lab,
                ref_lab=ref_lab,
                num_bins=DEFAULT_LUMINANCE_BINS,
                vivid_percentile=VIVID_CHROMA_PERCENTILE,
                chroma_boost=CHROMA_BOOST_DEFAULT,
                shadow_th=SHADOW_ROLLOFF_L_THRESHOLD,
                luminance_align_weight=LUMINANCE_ALIGN_WEIGHT,
                preserve_luminance=preserve_luminance,
            )
        except Exception:
            final_lab = _standard_reinhard_transfer(
                src_lab=src_lab,
                ref_lab=ref_lab,
                preserve_luminance=preserve_luminance,
            )

        rgb_out = lab_to_rgb(final_lab)

        if soft_clip:
            rgb_out = apply_soft_clip(rgb_out)
        else:
            rgb_out = np.clip(rgb_out, 0.0, 1.0)

        rgb_out = np.nan_to_num(rgb_out, nan=0.0, posinf=1.0, neginf=0.0)
        return (np.clip(rgb_out, 0.0, 1.0) * 255.0).astype(np.uint8)
    except Exception:
        return src_img.copy()


def compute_color_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    method: str = DEFAULT_METHOD,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.uint8]:
    """Transfer the colour distribution of *ref_img* onto *src_img*.

    Applies the chosen color-transfer method, followed by CIE LAB luminance
    blending (to preserve source lightness and avoid overexposure/underexposure)
    and optional soft-knee roll-off.

    Args:
        src_img: Source image as an RGB uint8 array of shape (H, W, 3) or (N, 1, 3).
        ref_img: Reference image as an RGB uint8 array of shape (H, W, 3).
        method: Colour-transfer algorithm key (see config.SUPPORTED_METHODS).
        preserve_luminance: Luminance preservation blend factor in [0.0, 1.0].
            1.0 keeps 100% of source lightness, 0.0 applies full reference lightness.
        soft_clip: Whether to apply smooth tanh compression on extreme highlights/shadows.

    Returns:
        Colour-matched image as an RGB uint8 array with the same spatial
        dimensions as *src_img*.

    Raises:
        ValueError: If *method* is not a supported algorithm key.
    """
    if method not in SUPPORTED_METHODS:
        raise ValueError(
            f"Unsupported method '{method}'. "
            f"Choose from: {list(SUPPORTED_METHODS.keys())}"
        )

    if method == "reinhard":
        return _numpy_statistical_transfer(
            src_img=src_img,
            ref_img=ref_img,
            preserve_luminance=preserve_luminance,
            soft_clip=soft_clip,
        )

    try:
        from color_matcher import ColorMatcher
        from color_matcher.normalizer import Normalizer

        cm = ColorMatcher()

        if method == "hm-mkl-hm":
            # Chain: HM -> MKL -> HM
            res: NDArray = cm.transfer(src=src_img, ref=ref_img, method="hm")
            res = Normalizer(res).uint8_norm()
            res = cm.transfer(src=res, ref=ref_img, method="mkl")
            res = Normalizer(res).uint8_norm()
            res = cm.transfer(src=res, ref=ref_img, method="hm")
        else:
            res = cm.transfer(src=src_img, ref=ref_img, method=method)

        res = Normalizer(res).uint8_norm().astype(np.uint8)

        # CIE LAB Luminance Preservation
        clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
        if clamped_preserve > 0.0:
            src_f = src_img.astype(np.float32) / 255.0
            trans_f = res.astype(np.float32) / 255.0

            src_lab = rgb_to_lab(src_f)
            trans_lab = rgb_to_lab(trans_f)

            final_l = (
                clamped_preserve * src_lab[..., 0]
                + (1.0 - clamped_preserve) * trans_lab[..., 0]
            )

            final_lab = np.stack(
                [final_l, trans_lab[..., 1], trans_lab[..., 2]],
                axis=-1,
            )
            rgb_f = lab_to_rgb(final_lab)
        else:
            rgb_f = res.astype(np.float32) / 255.0

        # Soft clipping roll-off
        if soft_clip:
            rgb_f = apply_soft_clip(rgb_f)
        else:
            rgb_f = np.clip(rgb_f, 0.0, 1.0)

        return (rgb_f * 255.0).astype(np.uint8)

    except ImportError:
        # Fallback to pure numpy statistical transfer
        return _numpy_statistical_transfer(
            src_img=src_img,
            ref_img=ref_img,
            preserve_luminance=preserve_luminance,
            soft_clip=soft_clip,
        )

    except Exception:
        # Graceful fallback in case of SciPy linear algebra singularity (LinAlgError)
        return _numpy_statistical_transfer(
            src_img=src_img,
            ref_img=ref_img,
            preserve_luminance=preserve_luminance,
            soft_clip=soft_clip,
        )
