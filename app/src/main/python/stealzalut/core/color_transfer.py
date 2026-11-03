"""Colour-transfer engine backed by *color-matcher* with resilient fallback.

This module provides professional color transfer between a source and reference
image with CIE LAB luminance preservation, contrast ratio clamping, and smooth
tanh-based soft clipping.
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import (
    DEFAULT_METHOD,
    DEFAULT_PRESERVE_LUMINANCE,
    EPSILON,
    MAX_STD_RATIO,
    MIN_STD_RATIO,
    SOFT_CLIP_HIGHLIGHT_THRESHOLD,
    SOFT_CLIP_SHADOW_THRESHOLD,
    SUPPORTED_METHODS,
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


def _numpy_statistical_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.uint8]:
    """Pure NumPy statistical Reinhard color transfer in CIE LAB space.

    Used as an ultra-fast, fail-safe fallback when SciPy encounters singular
    matrices or color-matcher is unavailable.

    Args:
        src_img: Source image (RGB uint8, shape (H, W, 3) or (N, 1, 3)).
        ref_img: Reference image (RGB uint8, shape (H_ref, W_ref, 3)).
        preserve_luminance: Lightness preservation weight in [0.0, 1.0].
        soft_clip: Whether to apply smooth tanh-based roll-off.

    Returns:
        Transferred image as RGB uint8 array matching src_img shape.
    """
    src_f = src_img.astype(np.float32) / 255.0
    ref_f = ref_img.astype(np.float32) / 255.0

    src_lab = rgb_to_lab(src_f)
    ref_lab = rgb_to_lab(ref_f)

    # Moments across spatial dimensions
    src_mean = np.mean(src_lab, axis=(0, 1), keepdims=True)
    src_std = np.std(src_lab, axis=(0, 1), keepdims=True)
    ref_mean = np.mean(ref_lab, axis=(0, 1), keepdims=True)
    ref_std = np.std(ref_lab, axis=(0, 1), keepdims=True)

    # Standard deviation scale ratio with safety bounds
    scale = ref_std / (src_std + np.float32(EPSILON))
    scale[..., 0] = np.clip(scale[..., 0], MIN_STD_RATIO, MAX_STD_RATIO)

    # Statistical moment matching
    transferred_lab = (src_lab - src_mean) * scale + ref_mean

    # Blend luminance channel L
    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
    final_l = (
        clamped_preserve * src_lab[..., 0]
        + (1.0 - clamped_preserve) * transferred_lab[..., 0]
    )

    final_lab = np.stack(
        [final_l, transferred_lab[..., 1], transferred_lab[..., 2]],
        axis=-1,
    )

    rgb_out = lab_to_rgb(final_lab)

    if soft_clip:
        rgb_out = apply_soft_clip(rgb_out)
    else:
        rgb_out = np.clip(rgb_out, 0.0, 1.0)

    return (rgb_out * 255.0).astype(np.uint8)


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
