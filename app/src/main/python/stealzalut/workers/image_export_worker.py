"""Worker pipeline for high-resolution graded image export.

Processes the source image at full or high export resolution (e.g. 2048px, 4096px, or native),
applying the reference colorimetry and saving directly to the specified destination.
"""

from __future__ import annotations

import time
from typing import Any, Dict

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import MAX_ANALYSIS_DIMENSION
from stealzalut.core.color_transfer import compute_color_transfer
from stealzalut.utils.image_io import (
    create_proxy_image,
    load_image,
    save_image,
    to_float64,
    to_uint8,
)


def run_image_export_pipeline(
    src_path: str,
    ref_path: str,
    output_path: str,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
    max_export_dim: int = 2048,
    quality: int = 95,
) -> Dict[str, Any]:
    """Execute high-resolution color-graded image export.

    Args:
        src_path: Path to the source image to be graded.
        ref_path: Path to the reference look image.
        output_path: Destination file path (e.g. ``.jpg`` or ``.png``).
        method: Color transfer method key.
        intensity: Opacity blend in ``[0.0, 1.0]``.
        max_export_dim: Maximum dimension for output image. If <= 0, preserves original resolution.
        quality: JPEG compression quality (1-100).

    Returns:
        Dictionary with status, image path, output dimensions, and execution time.
    """
    start: float = time.monotonic()

    try:
        # Load full resolution source image
        src_img: NDArray[np.uint8] = load_image(src_path)

        # Apply export scaling if specified
        if max_export_dim > 0 and max(src_img.shape[:2]) > max_export_dim:
            src_work = create_proxy_image(src_img, max_dim=max_export_dim)
            del src_img
        else:
            src_work = src_img

        # Load reference image & build analysis proxy to keep ARM64 RAM usage safe
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

        # Blend intensity if not 100%
        if intensity < 1.0:
            src_f = to_float64(src_work)
            trn_f = to_float64(transferred)
            blended = np.clip((1.0 - intensity) * src_f + intensity * trn_f, 0.0, 1.0)
            transferred = to_uint8(blended)
            del src_f, trn_f, blended

        del src_work

        out_h, out_w = transferred.shape[:2]

        # Save to final file
        saved_path: str = save_image(transferred, output_path, quality=quality)
        del transferred

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "image_path": saved_path,
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
