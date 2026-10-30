"""LUT-export worker pipeline.

Orchestrates image loading, proxy creation, 3-D LUT computation,
intensity blending, and ``.cube`` file export into a single callable
that returns a result dictionary suitable for the Kotlin bridge.
"""

from __future__ import annotations

import time
from typing import Any, Dict

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import MAX_ANALYSIS_DIMENSION
from stealzalut.core.lut_engine import (
    apply_intensity,
    compute_lut,
    generate_identity_lattice,
)
from stealzalut.core.lut_export import export_cube
from stealzalut.utils.image_io import create_proxy_image, load_image


def run_export_pipeline(
    src_path: str,
    ref_path: str,
    output_cube_path: str,
    lut_size: int = 33,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Execute the full LUT-export pipeline.

    Args:
        src_path: Path to the source image.
        ref_path: Path to the reference image.
        output_cube_path: Destination path for the ``.cube`` file.
        lut_size: Number of grid points per channel axis.
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.

    Returns:
        A dictionary with the following keys:

        - ``"status"`` – ``"success"`` or ``"error"``.
        - ``"cube_path"`` – absolute path of the exported file
          (only on success).
        - ``"lut_size"`` – grid size used (only on success).
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

        # Compute LUT ----------------------------------------------------
        transformed: NDArray[np.float32] = compute_lut(
            src_proxy, ref_proxy, size=lut_size, method=method
        )

        del src_proxy, ref_proxy

        # Intensity ------------------------------------------------------
        if intensity < 1.0:
            identity: NDArray[np.float32] = generate_identity_lattice(lut_size)
            transformed = apply_intensity(identity, transformed, intensity)
            del identity

        # Export ---------------------------------------------------------
        cube_path: str = export_cube(
            transformed, output_cube_path, size=lut_size
        )

        del transformed

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
