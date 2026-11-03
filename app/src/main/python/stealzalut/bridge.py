"""Kotlin-facing façade for StealZaLUT.

This module exposes the public API called by the Android (Chaquopy) layer:
- :func:`process_preview` : Fast proxy preview generation with WYSIWYG 3D LUT
- :func:`export_cube_file` : Adobe .cube 3D LUT generation with full parameter baking
- :func:`export_graded_image` : Direct high-resolution image export via identical 3D LUT
- :func:`extract_palette` : Quick dominant color extraction from any image
- :func:`generate_color_card` : Visual calibration card generation

Every function returns a plain ``dict`` so Chaquopy can convert it to a Kotlin ``Map``.
All exceptions are caught and surfaced as ``{"status": "error", "error_code": "...", "message": ...}``.
"""

from __future__ import annotations

import os
from typing import Any, Dict

from stealzalut.config import (
    DEFAULT_INTENSITY,
    DEFAULT_LUT_SIZE,
    DEFAULT_METHOD,
    DEFAULT_NEUTRAL_LOCK,
    DEFAULT_PRESERVE_LUMINANCE,
    DEFAULT_SKIN_PROTECTION,
    DEFAULT_SMOOTH_SIGMA,
)
from stealzalut.utils.image_io import extract_dominant_palette, load_image
from stealzalut.workers.export_worker import run_export_pipeline
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
    intensity: float = DEFAULT_INTENSITY,
    output_preview_path: str = "",
    max_preview_dim: int = 1024,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
    lut_size: int = DEFAULT_LUT_SIZE,
) -> Dict[str, Any]:
    """Generate a colour-transferred preview image using the exact 3D LUT.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        method: Colour-transfer algorithm key.
        intensity: Global strength in [0.0, 1.0].
        output_preview_path: Where to save the preview image.
        max_preview_dim: Maximum preview pixel dimension.
        preserve_luminance: Luminance preservation weight in [0.0, 1.0].
        skin_protection: Human skin tone protection weight in [0.0, 1.0].
        smooth_sigma: 3D Gaussian spatial smoothing sigma.
        neutral_lock: Neutral axis / black/white locking strength in [0.0, 1.0].
        lut_size: 3D LUT grid size per channel (17, 33, 65).

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
            preserve_luminance=preserve_luminance,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
            lut_size=lut_size,
            max_dimension=max_preview_dim,
            quality=90,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(
                message=res.get("message", "")
            )["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_cube_file(
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
    """Compute and export a 3-D LUT as an Adobe ``.cube`` file.

    All active parameters (intensity, luminance preservation, skin protection,
    neutral axis lock, 3D smoothing) are baked directly into the .cube file.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        output_cube_path: Destination path for the ``.cube`` file.
        lut_size: Number of grid points per channel axis (17, 33, 65).
        method: Colour-transfer algorithm key.
        intensity: Global strength in [0.0, 1.0].
        preserve_luminance: Luminance preservation in [0.0, 1.0].
        skin_protection: Skin tone protection in [0.0, 1.0].
        smooth_sigma: 3D Gaussian smoothing sigma.
        neutral_lock: Neutral axis and black/white locking strength.

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
            preserve_luminance=preserve_luminance,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(
                message=res.get("message", "")
            )["error_code"]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_graded_image(
    src_path: str,
    ref_path: str,
    output_image_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    max_export_dim: int = 2048,
    quality: int = 95,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
    lut_size: int = DEFAULT_LUT_SIZE,
) -> Dict[str, Any]:
    """Export the graded source image directly in high resolution.

    Applies the identical 3-D LUT used for preview and .cube export to guarantee
    absolute WYSIWYG consistency.

    Args:
        src_path: Absolute path to the source image.
        ref_path: Absolute path to the reference image.
        output_image_path: Destination path for the exported image.
        method: Colour-transfer algorithm key.
        intensity: Global strength in [0.0, 1.0].
        max_export_dim: Maximum dimension (width or height). If <= 0, full resolution.
        quality: Compression quality (1-100).
        preserve_luminance: Luminance preservation in [0.0, 1.0].
        skin_protection: Skin tone protection in [0.0, 1.0].
        smooth_sigma: 3D Gaussian smoothing sigma.
        neutral_lock: Neutral axis and black/white locking strength.
        lut_size: Grid size per axis.

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

        max_dim = max_export_dim if max_export_dim > 0 else None
        res = run_transfer_pipeline(
            src_path=src_path,
            ref_path=ref_path,
            output_path=output_image_path,
            method=method,
            intensity=intensity,
            preserve_luminance=preserve_luminance,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
            lut_size=lut_size,
            max_dimension=max_dim,
            quality=quality,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(
                message=res.get("message", "")
            )["error_code"]
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


def generate_color_card(
    ref_path: str,
    output_card_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
) -> Dict[str, Any]:
    """Generate a visual color calibration card showcasing the look's colors.

    Args:
        ref_path: Absolute path to the reference style image.
        output_card_path: Destination path where the card image will be saved.
        method: Color transfer method key (e.g. "hm-mkl-hm").
        intensity: Opacity blend in [0.0, 1.0].

    Returns:
        Dictionary with status, card_path, palette, and execution_time_ms.
    """
    try:
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )
        from stealzalut.core.color_card import run_color_card_pipeline

        res = run_color_card_pipeline(
            ref_path=ref_path,
            output_card_path=output_card_path,
            method=method,
            intensity=intensity,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(
                message=res.get("message", "")
            )["error_code"]
        return res
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)
