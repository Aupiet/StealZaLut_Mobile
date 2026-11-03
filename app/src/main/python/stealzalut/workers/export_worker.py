"""LUT-export worker pipeline.

Orchestrates 3-D LUT retrieval, professional parameter refinement (intensity,
skin protection, neutral lock, 3D smoothing), and Adobe ``.cube`` file export.
"""

from __future__ import annotations

import time
from typing import Any, Dict

import numpy as np

from stealzalut.config import (
    DEFAULT_INTENSITY,
    DEFAULT_LUT_SIZE,
    DEFAULT_METHOD,
    DEFAULT_NEUTRAL_LOCK,
    DEFAULT_PRESERVE_LUMINANCE,
    DEFAULT_SKIN_PROTECTION,
    DEFAULT_SMOOTH_SIGMA,
)
from stealzalut.core.lut_engine import refine_and_blend_lut
from stealzalut.core.lut_export import export_cube
from stealzalut.workers.transfer_worker import get_or_compute_raw_lut


def run_export_pipeline(
    src_path: str,
    ref_path: str,
    output_cube_path: str,
    lut_size: int = DEFAULT_LUT_SIZE,
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
) -> Dict[str, Any]:
    """Execute the full WYSIWYG LUT-export pipeline.

    Computes or retrieves cached raw LUT, refines it with the user's active
    slider settings, and writes an Adobe .cube file.

    Args:
        src_path: Path to the source image.
        ref_path: Path to the reference style image.
        output_cube_path: Destination path for the .cube file.
        lut_size: Number of grid points per channel axis (17, 33, 65).
        method: Colour-transfer algorithm key.
        intensity: Global LUT intensity in [0.0, 1.0].
        preserve_luminance: Luminance preservation in [0.0, 1.0].
        skin_protection: Skin tone protection in [0.0, 1.0].
        smooth_sigma: 3D Gaussian smoothing sigma.
        neutral_lock: Neutral axis and black/white locking in [0.0, 1.0].

    Returns:
        Result dictionary containing status, cube_path, lut_size, and execution_time_ms.
    """
    start: float = time.monotonic()

    try:
        # Retrieve or compute raw LUT (shared cache with preview worker)
        raw_lut = get_or_compute_raw_lut(
            src_path=src_path,
            ref_path=ref_path,
            method=method,
            lut_size=lut_size,
            preserve_luminance=preserve_luminance,
            soft_clip=True,
        )

        # Refine LUT directly into 3D grid with the exact parameters
        refined_lut = refine_and_blend_lut(
            raw_lut=raw_lut,
            intensity=intensity,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
        )

        # Ensure flat shape (lut_size ** 3, 3) for .cube exporter
        flat_lut = refined_lut.reshape(-1, 3).astype(np.float32)
        del raw_lut, refined_lut

        # Export Adobe .cube file
        cube_path: str = export_cube(
            flat_lut, output_cube_path, size=lut_size
        )
        del flat_lut

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "cube_path": cube_path,
            "lut_size": lut_size,
            "execution_time_ms": round(elapsed_ms, 1),
        }

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = (time.monotonic() - start) * 1000.0
        return {
            "status": "error",
            "message": str(exc),
            "execution_time_ms": round(elapsed_ms, 1),
        }
