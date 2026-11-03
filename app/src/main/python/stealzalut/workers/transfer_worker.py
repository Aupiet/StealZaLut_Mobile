"""Unified colour-transfer worker pipeline with intelligent caching and WYSIWYG LUT application.

Handles fast preview generation and high-resolution exports by evaluating
the exact refined 3D LUT via trilinear interpolation directly on the source image.
"""

from __future__ import annotations

from collections import OrderedDict
import os
import time
from typing import Any, Dict, Optional, Tuple

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import (
    DEFAULT_INTENSITY,
    DEFAULT_LUT_SIZE,
    DEFAULT_METHOD,
    DEFAULT_NEUTRAL_LOCK,
    DEFAULT_PRESERVE_LUMINANCE,
    DEFAULT_SKIN_PROTECTION,
    DEFAULT_SMOOTH_SIGMA,
    MAX_ANALYSIS_DIMENSION,
    MAX_LUT_CACHE_SIZE,
)
from stealzalut.core.lut_engine import (
    apply_lut,
    compute_lut,
    refine_and_blend_lut,
)
from stealzalut.utils.image_io import (
    create_proxy_image,
    load_image,
    save_image,
)

# In-memory LRU caches for ultra-responsive slider adjustments (<5ms)
_RAW_LUT_CACHE: OrderedDict[
    Tuple[str, str, str, int, float, bool], NDArray[np.float32]
] = OrderedDict()
_SRC_PROXY_CACHE: OrderedDict[
    Tuple[str, Optional[int]], NDArray[np.uint8]
] = OrderedDict()


def clear_lut_cache() -> None:
    """Clear all in-memory raw LUT and source proxy caches."""
    _RAW_LUT_CACHE.clear()
    _SRC_PROXY_CACHE.clear()


def get_or_compute_raw_lut(
    src_path: str,
    ref_path: str,
    method: str = DEFAULT_METHOD,
    lut_size: int = DEFAULT_LUT_SIZE,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.float32]:
    """Retrieve raw 3D LUT from cache or compute and cache it.

    Args:
        src_path: Path to source image.
        ref_path: Path to reference look image.
        method: Color transfer method key.
        lut_size: Grid size per axis (17, 33, 65).
        preserve_luminance: Luminance preservation blend factor in [0.0, 1.0].
        soft_clip: Whether to apply soft-knee roll-off.

    Returns:
        Raw 3D LUT array of shape ``(lut_size ** 3, 3)``.
    """
    abs_src = os.path.abspath(src_path) if src_path else ""
    abs_ref = os.path.abspath(ref_path) if ref_path else ""
    cache_key = (
        abs_src,
        abs_ref,
        method,
        lut_size,
        round(float(preserve_luminance), 4),
        soft_clip,
    )

    if cache_key in _RAW_LUT_CACHE:
        _RAW_LUT_CACHE.move_to_end(cache_key)
        return _RAW_LUT_CACHE[cache_key].copy()

    # Compute raw LUT using compact analysis proxies for optimal speed
    src_img = load_image(src_path)
    ref_img = load_image(ref_path)

    src_proxy = create_proxy_image(src_img, max_dim=MAX_ANALYSIS_DIMENSION)
    ref_proxy = create_proxy_image(ref_img, max_dim=MAX_ANALYSIS_DIMENSION)
    del src_img, ref_img

    raw_lut = compute_lut(
        src_img=src_proxy,
        ref_img=ref_proxy,
        size=lut_size,
        method=method,
        preserve_luminance=preserve_luminance,
        soft_clip=soft_clip,
    )
    del src_proxy, ref_proxy

    # Maintain LRU cache capacity
    if len(_RAW_LUT_CACHE) >= MAX_LUT_CACHE_SIZE:
        _RAW_LUT_CACHE.popitem(last=False)

    _RAW_LUT_CACHE[cache_key] = raw_lut.copy()
    return raw_lut


def get_or_load_source_proxy(
    src_path: str,
    max_dimension: Optional[int] = 1024,
) -> NDArray[np.uint8]:
    """Retrieve or load and downsample the source image for fast preview."""
    abs_src = os.path.abspath(src_path) if src_path else ""
    cache_key = (abs_src, max_dimension)

    if cache_key in _SRC_PROXY_CACHE:
        _SRC_PROXY_CACHE.move_to_end(cache_key)
        return _SRC_PROXY_CACHE[cache_key].copy()

    src_img = load_image(src_path)
    if (
        max_dimension is not None
        and max_dimension > 0
        and max(src_img.shape[:2]) > max_dimension
    ):
        src_work = create_proxy_image(src_img, max_dim=max_dimension)
        del src_img
    else:
        src_work = src_img

    if len(_SRC_PROXY_CACHE) >= MAX_LUT_CACHE_SIZE:
        _SRC_PROXY_CACHE.popitem(last=False)

    _SRC_PROXY_CACHE[cache_key] = src_work.copy()
    return src_work


def run_transfer_pipeline(
    src_path: str,
    ref_path: str,
    output_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
    lut_size: int = DEFAULT_LUT_SIZE,
    max_dimension: Optional[int] = 1024,
    quality: int = 95,
) -> Dict[str, Any]:
    """Execute the unified WYSIWYG colour-transfer pipeline.

    Computes or retrieves the cached raw 3D LUT, integrates slider parameters
    (intensity, skin protection, neutral lock, 3D smoothing) directly into the
    3D lattice, and applies that exact LUT to the source preview.

    Args:
        src_path: Path to source image.
        ref_path: Path to reference style image.
        output_path: Destination path for rendered image.
        method: Color transfer method key.
        intensity: Global strength in [0.0, 1.0].
        preserve_luminance: Luminance preservation in [0.0, 1.0].
        skin_protection: Skin tone preservation factor in [0.0, 1.0].
        smooth_sigma: 3D Gaussian smoothing sigma.
        neutral_lock: Neutral axis and black/white locking strength.
        lut_size: Grid size per axis (17, 33, 65).
        max_dimension: Maximum proxy dimension (None for full resolution).
        quality: JPEG compression quality (1-100).

    Returns:
        Dictionary with status, image_path, preview_path, width, height, and execution_time_ms.
    """
    start: float = time.monotonic()

    try:
        # 1. Base calculation / cache retrieval
        raw_lut = get_or_compute_raw_lut(
            src_path=src_path,
            ref_path=ref_path,
            method=method,
            lut_size=lut_size,
            preserve_luminance=preserve_luminance,
            soft_clip=True,
        )

        # 2. Refine 3D LUT with current user parameters (<5ms)
        final_lut = refine_and_blend_lut(
            raw_lut=raw_lut,
            intensity=intensity,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
        )

        # 3. Retrieve or load source proxy
        src_work = get_or_load_source_proxy(src_path, max_dimension=max_dimension)

        # 4. Strict WYSIWYG: Apply the exact 3D LUT via trilinear interpolation
        transferred = apply_lut(src_work, final_lut)
        del src_work, final_lut, raw_lut

        out_h, out_w = transferred.shape[:2]

        # 5. Save output image
        saved_path: str = save_image(transferred, output_path, quality=quality)
        del transferred

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "image_path": saved_path,
            "preview_path": saved_path,
            "width": out_w,
            "height": out_h,
            "execution_time_ms": round(elapsed_ms, 1),
        }

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = (time.monotonic() - start) * 1000.0
        return {
            "status": "error",
            "message": str(exc),
            "execution_time_ms": round(elapsed_ms, 1),
        }
