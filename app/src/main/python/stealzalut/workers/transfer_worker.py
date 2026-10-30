"""Preview-generation worker pipeline.

Orchestrates image loading, proxy creation, colour transfer, intensity
blending, and preview saving into a single callable that returns a
result dictionary suitable for the Kotlin bridge.
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
    to_float32,
    to_uint8,
)


def run_transfer_pipeline(
    src_path: str,
    ref_path: str,
    output_path: str,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Execute the full colour-transfer preview pipeline.

    Args:
        src_path: Path to the source image.
        ref_path: Path to the reference image.
        output_path: Destination path for the preview image.
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.

    Returns:
        A dictionary with the following keys:

        - ``"status"`` – ``"success"`` or ``"error"``.
        - ``"preview_path"`` – absolute path of the saved preview
          (only on success).
        - ``"execution_time_ms"`` – wall-clock time in milliseconds.
        - ``"message"`` – error description (only on failure).
    """
    start: float = time.monotonic()

    try:
        # Load -----------------------------------------------------------
        src_img: NDArray[np.uint8] = load_image(src_path)
        ref_img: NDArray[np.uint8] = load_image(ref_path)

        # Proxy ----------------------------------------------------------
        src_proxy: NDArray[np.uint8] = create_proxy_image(
            src_img, max_dim=MAX_ANALYSIS_DIMENSION
        )
        ref_proxy: NDArray[np.uint8] = create_proxy_image(
            ref_img, max_dim=MAX_ANALYSIS_DIMENSION
        )

        # Free full-resolution images
        del src_img, ref_img

        # Transfer -------------------------------------------------------
        transferred: NDArray[np.uint8] = compute_color_transfer(
            src_proxy, ref_proxy, method=method
        )

        del ref_proxy

        # Intensity ------------------------------------------------------
        if intensity < 1.0:
            src_f: NDArray[np.float32] = to_float32(src_proxy)
            trn_f: NDArray[np.float32] = to_float32(transferred)
            inv_int = np.float32(1.0) - np.float32(intensity)
            w = np.float32(intensity)
            blended: NDArray[np.float32] = np.clip(
                inv_int * src_f + w * trn_f, 0.0, 1.0
            )
            transferred = to_uint8(blended)
            del src_f, trn_f, blended

        del src_proxy

        # Save -----------------------------------------------------------
        preview_path: str = save_image(transferred, output_path)
        del transferred

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "preview_path": preview_path,
            "execution_time_ms": round(elapsed_ms, 1),
        }

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = (time.monotonic() - start) * 1000.0
        return {
            "status": "error",
            "message": str(exc),
            "execution_time_ms": round(elapsed_ms, 1),
        }
