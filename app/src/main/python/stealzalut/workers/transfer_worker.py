"""Unified colour-transfer worker pipeline.

Handles both fast proxy preview generation and high-resolution final export.
"""

from __future__ import annotations

import time
from typing import Any, Dict, Optional

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import MAX_ANALYSIS_DIMENSION
from stealzalut.core.color_transfer import compute_color_transfer
from stealzalut.utils.image_io import (
    create_proxy_image,
    load_image,
    save_image,
    to_float32,
    to_uint8,
)


def run_transfer_pipeline(
    src_path: str,
    ref_path: str,
    output_path: str,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
    max_dimension: Optional[int] = 1024,
    quality: int = 95,
) -> Dict[str, Any]:
    """Execute the unified colour-transfer pipeline (preview or high-res export).

    Args:
        src_path: Path to the source image to be graded.
        ref_path: Path to the reference look image.
        output_path: Destination file path (e.g. .jpg or .png).
        method: Colour-transfer algorithm key.
        intensity: Blend strength in [0.0, 1.0].
        max_dimension: Maximum allowed dimension (width or height).
            If None or <= 0, full source resolution is processed.
        quality: Compression quality for JPEG output (1-100).

    Returns:
        Dictionary with status, image_path, preview_path, width, height, and execution_time_ms.
    """
    start: float = time.monotonic()

    try:
        # Load source image
        src_img: NDArray[np.uint8] = load_image(src_path)

        # Scale source if max_dimension is specified
        if max_dimension is not None and max_dimension > 0 and max(src_img.shape[:2]) > max_dimension:
            src_work = create_proxy_image(src_img, max_dim=max_dimension)
            del src_img
        else:
            src_work = src_img

        # Load reference image & create compact proxy for fast statistical analysis
        ref_img: NDArray[np.uint8] = load_image(ref_path)
        ref_proxy: NDArray[np.uint8] = create_proxy_image(
            ref_img, max_dim=MAX_ANALYSIS_DIMENSION
        )
        del ref_img

        # Perform color transfer
        transferred: NDArray[np.uint8] = compute_color_transfer(
            src_img=src_work,
            ref_img=ref_proxy,
            method=method,
        )
        del ref_proxy

        # Blend intensity if < 1.0 (vectorized float32 for RAM & CPU efficiency)
        if intensity < 1.0:
            src_f: NDArray[np.float32] = to_float32(src_work)
            trn_f: NDArray[np.float32] = to_float32(transferred)
            inv_int = np.float32(1.0) - np.float32(intensity)
            w = np.float32(intensity)
            blended: NDArray[np.float32] = np.clip(
                inv_int * src_f + w * trn_f, 0.0, 1.0
            )
            transferred = to_uint8(blended)
            del src_f, trn_f, blended

        del src_work

        out_h, out_w = transferred.shape[:2]

        # Save output image
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
