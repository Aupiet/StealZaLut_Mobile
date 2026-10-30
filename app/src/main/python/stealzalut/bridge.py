"""Kotlin-facing façade for StealZaLUT.

This module exposes the public API called by the Android (Chaquopy) layer:
- :func:`process_preview` : Fast proxy preview generation
- :func:`export_cube_file` : Adobe .cube 3D LUT generation
- :func:`export_graded_image` : Direct high-resolution image export
- :func:`generate_color_set` : Neutral calibration pattern & palette generation
- :func:`extract_palette` : Quick dominant color extraction from any image

Every function returns a plain ``dict`` so Chaquopy can convert it to a Kotlin ``Map``.
All exceptions are caught and surfaced as ``{"status": "error", "error_code": "...", "message": ...}``.
"""

from __future__ import annotations

import os
from typing import Any, Dict, List

from stealzalut.config import DEFAULT_LUT_SIZE, DEFAULT_METHOD
from stealzalut.core.test_pattern import extract_dominant_palette
from stealzalut.utils.image_io import load_image
from stealzalut.workers.colorset_worker import run_colorset_pipeline
from stealzalut.workers.export_worker import run_export_pipeline
from stealzalut.workers.image_export_worker import run_image_export_pipeline
from stealzalut.workers.transfer_worker import run_transfer_pipeline


def _make_error_response(
    exc: Exception | None = None,
    message: str = "",
    error_code: str = "",
) -> Dict[str, Any]:
    """Build a standardised error dictionary containing an explicit error_code."""
    msg = message or (str(exc) if exc is not None else "Unknown error")
    if not error_code:
        if exc is not None:
            if isinstance(exc, FileNotFoundError):
                error_code = "SOURCE_NOT_FOUND"
            elif isinstance(exc, MemoryError):
                error_code = "OUT_OF_MEMORY"
            elif "singul" in msg.lower() or "linalg" in msg.lower():
                error_code = "MATRIX_SINGULARITY"
            elif isinstance(exc, (IOError, OSError, PermissionError)):
                error_code = "EXPORT_IO_ERROR"
            else:
                error_code = "GENERIC_ERROR"
        else:
            lower = msg.lower()
            if "source" in lower and "not found" in lower:
                error_code = "SOURCE_NOT_FOUND"
            elif "reference" in lower and "not found" in lower:
                error_code = "REFERENCE_NOT_FOUND"
            elif "singul" in lower or "linalg" in lower:
                error_code = "MATRIX_SINGULARITY"
            elif "memory" in lower or "oom" in lower:
                error_code = "OUT_OF_MEMORY"
            elif "cannot decode" in lower or "io" in lower or "permission" in lower:
                error_code = "EXPORT_IO_ERROR"
            else:
                error_code = "GENERIC_ERROR"

    return {
        "status": "error",
        "error_code": error_code,
        "message": msg,
    }


def process_preview(
    src_path: str,
    ref_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = 1.0,
    output_preview_path: str = "",
    max_preview_dim: int = 1024,
) -> Dict[str, Any]:
    """Generate a colour-transferred preview image.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.
        output_preview_path: Where to save the preview.
        max_preview_dim: Maximum preview pixel dimension (default: 1024).

    Returns:
        Result dictionary with status, preview_path, and execution_time_ms.
    """
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        if not output_preview_path:
            src_dir: str = os.path.dirname(os.path.abspath(src_path))
            output_preview_path = os.path.join(src_dir, "stealzalut_preview.jpg")

        res = run_transfer_pipeline(
            src_path=src_path,
            ref_path=ref_path,
            output_path=output_preview_path,
            method=method,
            intensity=intensity,
            max_preview_dim=max_preview_dim,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_cube_file(
    src_path: str,
    ref_path: str,
    output_cube_path: str,
    lut_size: int = DEFAULT_LUT_SIZE,
    method: str = DEFAULT_METHOD,
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Compute and export a 3-D LUT as an Adobe ``.cube`` file.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        output_cube_path: Destination path for the ``.cube`` file.
        lut_size: Number of grid points per channel axis (17, 33, 65).
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.

    Returns:
        Result dictionary with status, cube_path, lut_size, and execution_time_ms.
    """
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        res = run_export_pipeline(
            src_path=src_path,
            ref_path=ref_path,
            output_cube_path=output_cube_path,
            lut_size=lut_size,
            method=method,
            intensity=intensity,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_graded_image(
    src_path: str,
    ref_path: str,
    output_image_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = 1.0,
    max_export_dim: int = 2048,
    quality: int = 95,
) -> Dict[str, Any]:
    """Export the graded source image directly in high resolution.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        output_image_path: Destination path for the exported JPEG/PNG image.
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.
        max_export_dim: Maximum dimension (width or height). If <= 0, full resolution.
        quality: JPEG compression quality (1-100).

    Returns:
        Result dictionary with status, image_path, width, height, and execution_time_ms.
    """
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        res = run_image_export_pipeline(
            src_path=src_path,
            ref_path=ref_path,
            output_path=output_image_path,
            method=method,
            intensity=intensity,
            max_export_dim=max_export_dim,
            quality=quality,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def generate_color_set(
    ref_path: str,
    output_card_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Generate a neutral test card and apply the color transformation to it.

    Produces a visual calibration swatch showing pure white highlight tinting,
    grayscale gradient roll-off, skin tone shifts, and extracts dominant palettes.

    Args:
        ref_path: Absolute path to the reference image.
        output_card_path: Destination path where the card is saved.
        method: Colour-transfer algorithm key.
        intensity: Blend strength in ``[0.0, 1.0]``.

    Returns:
        Result dictionary with status, colorset_path, ref_palette, graded_palette, execution_time_ms.
    """
    try:
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        res = run_colorset_pipeline(
            ref_path=ref_path,
            output_card_path=output_card_path,
            method=method,
            intensity=intensity,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def extract_palette(image_path: str, num_colors: int = 6) -> Dict[str, Any]:
    """Extract dominant color hex codes from any image file.

    Args:
        image_path: Absolute path to the image.
        num_colors: Number of color swatches to extract.

    Returns:
        Result dictionary with status, error_code (if error), and list of hex colors.
    """
    try:
        if not image_path or not os.path.isfile(image_path):
            return {
                "status": "error",
                "error_code": "SOURCE_NOT_FOUND",
                "message": f"Image file not found: {image_path}",
                "palette": [],
            }

        img_arr = load_image(image_path)
        palette = extract_dominant_palette(img_arr, num_colors=num_colors)
        return {"status": "success", "palette": palette}
    except Exception as exc:  # noqa: BLE001
        err = _make_error_response(exc=exc)
        err["palette"] = []
        return err
