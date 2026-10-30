"""Colour-transfer engine backed by *color-matcher* with resilient fallback.

This module wraps the ``ColorMatcher`` library to map the colour distribution
of a *source* image onto a *reference* image. It includes mathematical fallbacks
for matrix singularities in SciPy and environments where color-matcher is initializing.
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import SUPPORTED_METHODS


def _numpy_statistical_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
) -> NDArray[np.uint8]:
    """Pure NumPy statistical color transfer (Reinhard LAB / RGB moment matching).

    Used as an ultra-fast, fail-safe fallback when SciPy encounters singular matrices.
    """
    src_f = src_img.astype(np.float32)
    ref_f = ref_img.astype(np.float32)
    result = np.zeros_like(src_f)

    for c in range(3):
        src_mean, src_std = np.mean(src_f[:, :, c]), np.std(src_f[:, :, c])
        ref_mean, ref_std = np.mean(ref_f[:, :, c]), np.std(ref_f[:, :, c])
        scale = ref_std / (src_std + np.float32(1e-6))
        result[:, :, c] = (src_f[:, :, c] - src_mean) * scale + ref_mean

    return np.clip(result, 0.0, 255.0).astype(np.uint8)


def compute_color_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    method: str = "hm-mkl-hm",
) -> NDArray[np.uint8]:
    """Transfer the colour distribution of *ref_img* onto *src_img*.

    Args:
        src_img: Source image as an RGB ``uint8`` array of shape ``(H, W, 3)``.
        ref_img: Reference image as an RGB ``uint8`` array of shape ``(H, W, 3)``.
        method: Colour-transfer algorithm key (see :pydata:`stealzalut.config.SUPPORTED_METHODS`).

    Returns:
        Colour-matched image as an RGB ``uint8`` array with the same spatial
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
            # Chain: HM → MKL → HM
            res: NDArray = cm.transfer(src=src_img, ref=ref_img, method="hm")
            res = Normalizer(res).uint8_norm()
            res = cm.transfer(src=res, ref=ref_img, method="mkl")
            res = Normalizer(res).uint8_norm()
            res = cm.transfer(src=res, ref=ref_img, method="hm")
        else:
            res = cm.transfer(src=src_img, ref=ref_img, method=method)

        res = Normalizer(res).uint8_norm()
        return res.astype(np.uint8)

    except ImportError:
        # Fallback to pure numpy statistical transfer if color-matcher isn't installed
        return _numpy_statistical_transfer(src_img, ref_img)

    except Exception:
        # Graceful fallback in case of SciPy linear algebra singularity (LinAlgError)
        return _numpy_statistical_transfer(src_img, ref_img)
