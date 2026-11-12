"""Perceptual color space conversions and utility functions.

Implements high-performance, vectorized conversions between sRGB and the
OKLab perceptual color space (Ottosson, 2020), as well as smoothstep interpolation
and chromatic analysis primitives.
"""

from __future__ import annotations

from typing import Callable

import numpy as np
from numpy.typing import NDArray


# ---------------------------------------------------------------------------
# sRGB <-> Linear Conversions
# ---------------------------------------------------------------------------


def srgb_to_linear(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert sRGB array in [0.0, 1.0] to linear RGB."""
    clamped = np.clip(rgb, 0.0, 1.0).astype(np.float32)
    return np.where(
        clamped <= 0.04045,
        clamped / 12.92,
        ((clamped + 0.055) / 1.055) ** 2.4,
    ).astype(np.float32)


def linear_to_srgb(lin: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert linear RGB array to sRGB in [0.0, 1.0]."""
    clamped = np.clip(lin, 0.0, None).astype(np.float32)
    return np.where(
        clamped <= 0.0031308,
        clamped * 12.92,
        1.055 * (clamped ** (1.0 / 2.4)) - 0.055,
    ).astype(np.float32)


# ---------------------------------------------------------------------------
# OKLab Color Space Transformation Matrices
# ---------------------------------------------------------------------------

# Linear sRGB to cone response (LMS)
_M_SRGB_TO_LMS = np.array(
    [
        [0.4122214708, 0.5363325363, 0.0514459929],
        [0.2119034982, 0.6806995451, 0.1073969566],
        [0.0883024619, 0.2817188376, 0.6299787005],
    ],
    dtype=np.float32,
)

# Non-linear LMS to OKLab
_M_LMS_TO_OKLAB = np.array(
    [
        [0.2104542553, 0.7936177850, -0.0040720468],
        [1.9779984951, -2.4285922050, 0.4505937099],
        [0.0259040371, 0.7827717662, -0.8086757660],
    ],
    dtype=np.float32,
)

# OKLab to non-linear LMS
_M_OKLAB_TO_LMS = np.array(
    [
        [1.0, +0.3963377774, +0.2158037573],
        [1.0, -0.1055613458, -0.0638541728],
        [1.0, -0.0894841775, -1.2914855480],
    ],
    dtype=np.float32,
)

# Linear LMS to linear sRGB
_M_LMS_TO_SRGB = np.array(
    [
        [+4.0767439362, -3.3077115913, +0.2309699294],
        [-1.2684380046, +2.6097574011, -0.3413193965],
        [-0.0041960863, -0.7034186147, +1.7076147010],
    ],
    dtype=np.float32,
)


def rgb_to_oklab(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert sRGB array in [0.0, 1.0] to OKLab (L in [0, 1], a, b in [-0.4, 0.4]).

    Args:
        rgb: Float32 array of shape (..., 3) with values in [0.0, 1.0].

    Returns:
        Float32 array of shape (..., 3) representing (L, a, b).
    """
    lin = srgb_to_linear(rgb)
    lms = lin @ _M_SRGB_TO_LMS.T
    # Non-linear cubic root compression (guarding against negative epsilon)
    lms_clamped = np.maximum(lms, 0.0)
    lms_cbrt = np.cbrt(lms_clamped)
    oklab = lms_cbrt @ _M_LMS_TO_OKLAB.T
    return oklab.astype(np.float32)


def srgb_to_oklab(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Alias kept for compatibility with the desktop implementation."""
    return rgb_to_oklab(rgb)


def oklab_to_rgb(oklab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert OKLab array to clamped sRGB in [0.0, 1.0].

    Args:
        oklab: Float32 array of shape (..., 3) representing (L, a, b).

    Returns:
        Float32 array of shape (..., 3) with values clamped to [0.0, 1.0].
    """
    lms_cbrt = oklab.astype(np.float32) @ _M_OKLAB_TO_LMS.T
    lms = lms_cbrt**3
    lin = lms @ _M_LMS_TO_SRGB.T
    rgb = linear_to_srgb(lin)
    return np.clip(rgb, 0.0, 1.0).astype(np.float32)


def oklab_to_srgb(oklab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Alias for the desktop module naming."""
    return oklab_to_rgb(oklab)


def oklab_to_oklch(oklab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert OKLab to OKLCH: (L, C, h) with hue in degrees."""
    lab = np.asarray(oklab, dtype=np.float32)
    chroma = np.hypot(lab[..., 1], lab[..., 2])
    hue = np.mod(np.degrees(np.arctan2(lab[..., 2], lab[..., 1])), 360.0)
    return np.stack([lab[..., 0], chroma, hue], axis=-1).astype(np.float32)


def oklch_to_oklab(oklch: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert OKLCH to OKLab."""
    lch = np.asarray(oklch, dtype=np.float32)
    hue = np.radians(lch[..., 2])
    a = lch[..., 1] * np.cos(hue)
    b = lch[..., 1] * np.sin(hue)
    return np.stack([lch[..., 0], a, b], axis=-1).astype(np.float32)


def linear_to_oklab(linear_rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert linear RGB to OKLab."""
    return srgb_to_oklab(linear_to_srgb(linear_rgb))


def oklab_to_linear(oklab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert OKLab to linear RGB."""
    return srgb_to_linear(oklab_to_srgb(oklab))


# ---------------------------------------------------------------------------
# Interpolation & Smoothing Primitives
# ---------------------------------------------------------------------------


def smoothstep(
    edge0: float | NDArray[np.float32],
    edge1: float | NDArray[np.float32],
    x: float | NDArray[np.float32],
) -> NDArray[np.float32]:
    """Standard Hermite smoothstep interpolation function.

    Returns 0.0 for x <= edge0, 1.0 for x >= edge1, and smooth Hermite polynomial in between.
    """
    diff = np.maximum(edge1 - edge0, 1e-6)
    t = np.clip((x - edge0) / diff, 0.0, 1.0)
    return (t * t * (3.0 - 2.0 * t)).astype(np.float32)


def _in_gamut(linear_rgb: NDArray[np.float32]) -> NDArray[np.bool_]:
    """Return True for linear RGB values that remain inside the sRGB gamut."""
    lower = linear_rgb >= -1e-6
    upper = linear_rgb <= 1.0 + 1e-6
    return np.all(lower & upper, axis=-1)


def gamut_compress(
    lab: NDArray[np.float32],
    iters: int = 12,
    to_linear: Callable[[NDArray[np.float32]], NDArray[np.float32]] | None = None,
) -> NDArray[np.float32]:
    """Reduce Oklab chroma until the colour fits inside the sRGB gamut.

    This is a light-weight, deterministic variant of the desktop gamut-mapping
    approach: it preserves hue and lightness while scaling down the chroma on
    out-of-gamut values using a binary search.
    """
    arr = np.asarray(lab, dtype=np.float32).copy()
    if arr.size == 0:
        return arr
    if arr.ndim == 1:
        arr = arr.reshape(1, 3)

    arr[:, 0] = np.clip(arr[:, 0], 0.0, 1.0)

    if to_linear is None:
        def convert(x: NDArray[np.float32]) -> NDArray[np.float32]:
            lms_cbrt = x.astype(np.float32) @ _M_OKLAB_TO_LMS.T
            lms = lms_cbrt**3
            return (lms @ _M_LMS_TO_SRGB.T).astype(np.float32)
    else:
        convert = to_linear

    mask = ~_in_gamut(convert(arr))
    if not np.any(mask):
        return arr

    lightness = arr[mask, :1].copy()
    ab = arr[mask, 1:].copy()
    lo = np.zeros(lightness.shape[0], dtype=np.float32)
    hi = np.ones(lightness.shape[0], dtype=np.float32)

    for _ in range(max(1, iters)):
        mid = 0.5 * (lo + hi)
        candidate = np.concatenate([lightness, ab * mid[:, None]], axis=1)
        ok = _in_gamut(convert(candidate))
        lo = np.where(ok, mid, lo)
        hi = np.where(ok, hi, mid)

    arr[mask, 1:] = ab * lo[:, None]
    return arr.astype(np.float32)
