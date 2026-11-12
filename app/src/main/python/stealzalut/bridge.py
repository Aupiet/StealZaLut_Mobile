"""Kotlin-facing façade for StealZaLUT.

This module exposes the public API called by the Android (Chaquopy) layer:
- :func:`process_preview` : Fast proxy preview generation with WYSIWYG 3D LUT
- :func:`process_editor_preview` : Standalone Photo Editor preview without reference image
- :func:`export_cube_file` : Adobe .cube 3D LUT generation with full parameter baking
- :func:`export_graded_image` : Direct high-resolution image export via identical 3D LUT
- :func:`save_preset_bridge` / :func:`load_preset_bridge` : Presets management
- :func:`extract_palette` : Quick dominant color extraction from any image
- :func:`generate_color_card` : Visual calibration card generation

Every function returns a plain ``dict`` so Chaquopy can convert it to a Kotlin ``Map``.
All exceptions are caught and surfaced as ``{"status": "error", "error_code": "...", "message": ...}``.
"""

from __future__ import annotations

import os
import time
from typing import Any, Dict, Optional

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
from stealzalut.core.ai_pack import PACK_FILES, analysis_dict, build_prompt, write_pack
from stealzalut.core.palette import DEFAULT_ZONE_COUNT, analyze_zones
from stealzalut.core.matching import match_zones
from stealzalut.core.recipe import RecipeError, dump_recipe, parse_recipe, recipe_to_dict
from stealzalut.core.zone_transfer import fit_zone_transfer
from stealzalut.core.preset_manager import (
    create_preset,
    delete_preset,
    duplicate_preset,
    export_preset,
    import_preset,
    list_presets,
    load_preset,
    rename_preset,
    save_preset,
)
from stealzalut.utils.image_io import extract_dominant_palette
from stealzalut.workers.export_worker import (
    run_export_pipeline,
    run_image_export_pipeline,
)
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
    exposure: float = 0.0,
    contrast: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    whites: float = 0.0,
    blacks: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
    temperature: float = 0.0,
    tint: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    luma_lock: Optional[float] = None,
    neutral_axis_lock: Optional[float] = None,
    skin_tone_lock: Optional[float] = None,
    extremes_rolloff: float = 0.0,
    spatial_3d: Optional[float] = None,
    custom_lut_path: Optional[str] = None,
    develop_settings_json: Optional[str] = None,
) -> Dict[str, Any]:
    """Generate a colour-transferred preview image using the exact 3D LUT."""
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if ref_path and not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        if not output_preview_path:
            src_dir: str = os.path.dirname(os.path.abspath(src_path))
            output_preview_path = os.path.join(src_dir, "stealzalut_preview.jpg")

        res = run_transfer_pipeline(
            src_path=src_path,
            ref_path=ref_path if ref_path else None,
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
            exposure=exposure,
            contrast=contrast,
            highlights=highlights,
            shadows=shadows,
            temperature=temperature,
            tint=tint,
            vibrance=vibrance,
            saturation=saturation,
            clarity=clarity,
            sharpen=sharpen,
            grain=grain,
            luma_lock=luma_lock,
            neutral_axis_lock=neutral_axis_lock,
            skin_tone_lock=skin_tone_lock,
            extremes_rolloff=extremes_rolloff,
            spatial_3d=spatial_3d,
            custom_lut_path=custom_lut_path,
            lr_settings=develop_settings_json,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))[
                "error_code"
            ]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def process_editor_preview(
    src_path: str,
    output_preview_path: str = "",
    max_preview_dim: int = 1024,
    lut_size: int = DEFAULT_LUT_SIZE,
    intensity: float = 1.0,
    luma_lock: float = 0.0,
    neutral_axis_lock: float = 0.0,
    skin_tone_lock: float = 0.0,
    extremes_rolloff: float = 0.0,
    spatial_3d: float = 0.0,
    exposure: float = 0.0,
    contrast: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    temperature: float = 0.0,
    tint: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    custom_lut_path: Optional[str] = None,
    develop_settings_json: Optional[str] = None,
) -> Dict[str, Any]:
    """Execute preview generation for the Standalone Photo Editor (no reference image)."""
    return process_preview(
        src_path=src_path,
        ref_path="",  # No reference
        intensity=intensity,
        output_preview_path=output_preview_path,
        max_preview_dim=max_preview_dim,
        lut_size=lut_size,
        exposure=exposure,
        contrast=contrast,
        highlights=highlights,
        shadows=shadows,
        temperature=temperature,
        tint=tint,
        vibrance=vibrance,
        saturation=saturation,
        clarity=clarity,
        sharpen=sharpen,
        grain=grain,
        luma_lock=luma_lock,
        neutral_axis_lock=neutral_axis_lock,
        skin_tone_lock=skin_tone_lock,
        extremes_rolloff=extremes_rolloff,
        spatial_3d=spatial_3d,
        custom_lut_path=custom_lut_path,
        develop_settings_json=develop_settings_json,
    )


def export_cube_file(
    src_path: str,
    ref_path: Optional[str] = None,
    output_cube_path: str = "",
    lut_size: int = DEFAULT_LUT_SIZE,
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
    exposure: float = 0.0,
    contrast: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    whites: float = 0.0,
    blacks: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
    temperature: float = 0.0,
    tint: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    luma_lock: Optional[float] = None,
    neutral_axis_lock: Optional[float] = None,
    skin_tone_lock: Optional[float] = None,
    extremes_rolloff: float = 0.0,
    spatial_3d: Optional[float] = None,
    custom_lut_path: Optional[str] = None,
    develop_settings_json: Optional[str] = None,
) -> Dict[str, Any]:
    """Compute and export a 3-D LUT as an Adobe ``.cube`` file."""
    try:
        # Check source image path: if specified and invalid, return error
        if src_path and not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        # Check reference image path: if specified and invalid, return error
        if ref_path and not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )
        # If neither reference, nor custom LUT, nor source is provided, fail
        if not ref_path and not custom_lut_path and not src_path:
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )

        res = run_export_pipeline(
            src_path=src_path,
            ref_path=ref_path if ref_path else None,
            output_cube_path=output_cube_path,
            lut_size=lut_size,
            method=method,
            intensity=intensity,
            preserve_luminance=preserve_luminance,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
            exposure=exposure,
            contrast=contrast,
            highlights=highlights,
            shadows=shadows,
            whites=whites,
            blacks=blacks,
            vibrance=vibrance,
            saturation=saturation,
            temperature=temperature,
            tint=tint,
            clarity=clarity,
            sharpen=sharpen,
            grain=grain,
            luma_lock=luma_lock,
            neutral_axis_lock=neutral_axis_lock,
            skin_tone_lock=skin_tone_lock,
            extremes_rolloff=extremes_rolloff,
            spatial_3d=spatial_3d,
            custom_lut_path=custom_lut_path,
            lr_settings=develop_settings_json,
        )
        if res.get("status") == "success":
            path = res.get("cube_path") or res.get("lut_path")
            res["cube_path"] = path
            res["lut_path"] = path
        elif res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))[
                "error_code"
            ]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_graded_image(
    src_path: str,
    ref_path: Optional[str] = None,
    output_image_path: str = "",
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    max_export_dim: int = 2048,
    quality: int = 95,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
    lut_size: int = DEFAULT_LUT_SIZE,
    exposure: float = 0.0,
    contrast: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    whites: float = 0.0,
    blacks: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
    temperature: float = 0.0,
    tint: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    luma_lock: Optional[float] = None,
    neutral_axis_lock: Optional[float] = None,
    skin_tone_lock: Optional[float] = None,
    extremes_rolloff: float = 0.0,
    spatial_3d: Optional[float] = None,
    custom_lut_path: Optional[str] = None,
    develop_settings_json: Optional[str] = None,
    pre_downscale: bool = False,
) -> Dict[str, Any]:
    """Export the graded source image directly in high resolution."""
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if ref_path and not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        res = run_image_export_pipeline(
            src_path=src_path,
            ref_path=ref_path if ref_path else None,
            output_image_path=output_image_path,
            method=method,
            intensity=intensity,
            max_export_dim=max_export_dim,
            quality=quality,
            preserve_luminance=preserve_luminance,
            skin_protection=skin_protection,
            smooth_sigma=smooth_sigma,
            neutral_lock=neutral_lock,
            lut_size=lut_size,
            exposure=exposure,
            contrast=contrast,
            highlights=highlights,
            shadows=shadows,
            whites=whites,
            blacks=blacks,
            vibrance=vibrance,
            saturation=saturation,
            temperature=temperature,
            tint=tint,
            clarity=clarity,
            sharpen=sharpen,
            grain=grain,
            luma_lock=luma_lock,
            neutral_axis_lock=neutral_axis_lock,
            skin_tone_lock=skin_tone_lock,
            extremes_rolloff=extremes_rolloff,
            spatial_3d=spatial_3d,
            custom_lut_path=custom_lut_path,
            lr_settings=develop_settings_json,
            pre_downscale=pre_downscale,
        )
        if res.get("status") == "error" and "error_code" not in res:
            res["error_code"] = _make_error_response(message=res.get("message", ""))[
                "error_code"
            ]
        return res

    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


# ---------------------------------------------------------------------------
# Presets Bridge API
# ---------------------------------------------------------------------------


def save_preset_bridge(
    name: str,
    step2_params: Any,
    step3_params: Any,
    lut_reference: Optional[str] = None,
    thumbnail_base64: Optional[str] = None,
    output_dir: Optional[str] = None,
    lightroom_params: Optional[Any] = None,
) -> Dict[str, Any]:
    """Create and save a new editing preset."""
    try:
        preset_dict = create_preset(
            name=name,
            step2_params=step2_params,
            step3_params=step3_params,
            lut_reference=lut_reference,
            thumbnail_base64=thumbnail_base64,
            lightroom_params=lightroom_params,
        )
        saved_path = save_preset(preset_dict, directory=output_dir)
        return {
            "status": "success",
            "preset_path": saved_path,
            "preset_id": preset_dict["id"],
            "preset": preset_dict,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def load_preset_bridge(
    path_or_id: str, directory: Optional[str] = None
) -> Dict[str, Any]:
    """Load an existing editing preset."""
    try:
        p = load_preset(path_or_id, directory=directory)
        return {
            "status": "success",
            "preset": p,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def list_presets_bridge(directory: Optional[str] = None) -> Dict[str, Any]:
    """List all saved presets."""
    try:
        presets = list_presets(directory=directory)
        return {
            "status": "success",
            "presets": presets,
            "count": len(presets),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def delete_preset_bridge(
    path_or_id: str, directory: Optional[str] = None
) -> Dict[str, Any]:
    """Delete a saved preset."""
    try:
        ok = delete_preset(path_or_id, directory=directory)
        return {
            "status": "success" if ok else "error",
            "deleted": ok,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def duplicate_preset_bridge(
    path_or_id: str,
    new_name: Optional[str] = None,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Duplicate an existing editing preset."""
    try:
        dup = duplicate_preset(path_or_id, new_name=new_name, directory=directory)
        return {
            "status": "success",
            "preset": dup,
            "preset_id": dup["id"],
            "preset_path": dup.get("_file_path"),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def rename_preset_bridge(
    path_or_id: str,
    new_name: str,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Rename an existing editing preset."""
    try:
        updated = rename_preset(path_or_id, new_name=new_name, directory=directory)
        return {
            "status": "success",
            "preset": updated,
            "preset_id": updated["id"],
            "preset_path": updated.get("_file_path"),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_preset_bridge(
    path_or_id: str,
    export_path: str,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Export an editing preset to a file."""
    try:
        final_path = export_preset(
            path_or_id, export_path=export_path, directory=directory
        )
        return {
            "status": "success",
            "export_path": final_path,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def import_preset_bridge(
    import_path: str,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Import an editing preset from an external file."""
    try:
        imported = import_preset(import_path, target_directory=directory)
        return {
            "status": "success",
            "preset": imported,
            "preset_id": imported["id"],
            "preset_path": imported.get("_file_path"),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


# ---------------------------------------------------------------------------
# LightCraft Auto Edit & Palette Utilities
# ---------------------------------------------------------------------------


def compute_auto_edit(
    src_path: str,
    output_preview_path: str = "",
    max_preview_dim: int = 1024,
    lut_size: int = 33,
    output_path: str = "",
) -> Dict[str, Any]:
    """Execute LightCraft Auto Edit on an image."""
    try:
        from stealzalut.core.auto_edit import run_auto_edit_pipeline

        dest = output_path or output_preview_path
        return run_auto_edit_pipeline(
            src_path=src_path,
            output_path=dest,
            max_preview_dim=max_preview_dim,
            lut_size=lut_size,
        )
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def export_auto_edit_cube(
    src_path: str,
    output_cube_path: str,
    lut_size: int = 33,
) -> Dict[str, Any]:
    """Generate and export a 3D LUT corresponding to LightCraft Auto Edit."""
    t0 = time.perf_counter()
    try:
        if not src_path or not os.path.isfile(src_path):
            return _make_error_response(
                message=f"Source image file not found: {src_path}",
                error_code="SOURCE_NOT_FOUND",
            )

        from stealzalut.config import PROXY_DECODE_MAX_DIM
        from stealzalut.core.auto_edit import (
            generate_auto_edit_lut,
            get_auto_edit_settings,
        )
        from stealzalut.core.lut_export import export_cube
        from stealzalut.utils.image_io import load_proxy_image

        src_arr = load_proxy_image(src_path, max_dim=PROXY_DECODE_MAX_DIM)
        settings = get_auto_edit_settings(src_arr)
        lut_3d = generate_auto_edit_lut(src_arr, lut_size=lut_size, settings=settings)

        flat_lut = lut_3d.reshape(-1, 3)
        cube_path = export_cube(
            flat_lut,
            output_cube_path,
            size=lut_size,
            title="StealZaLUT_AutoEdit",
        )
        elapsed_ms = (time.perf_counter() - t0) * 1000.0

        return {
            "status": "success",
            "cube_path": cube_path,
            "lut_path": cube_path,
            "lut_size": lut_size,
            "settings": settings,
            "execution_time_ms": elapsed_ms,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def apply_image_edits(
    image_path: str,
    output_path: Optional[str] = None,
    exposure: float = 0.0,
    contrast: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    whites: float = 0.0,
    blacks: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
) -> Dict[str, Any]:
    """Apply LightCraft edit adjustments to an image file."""
    t0 = time.perf_counter()
    try:
        if not image_path or not os.path.isfile(image_path):
            return _make_error_response(
                message=f"Image file not found: {image_path}",
                error_code="IMAGE_NOT_FOUND",
            )

        from stealzalut.core.auto_edit import apply_auto_edit
        from stealzalut.utils.image_io import load_image, save_image

        arr = load_image(image_path)
        settings = {
            "exposure": float(exposure),
            "contrast": float(contrast),
            "highlights": float(highlights),
            "shadows": float(shadows),
            "whites": float(whites),
            "blacks": float(blacks),
            "vibrance": float(vibrance),
            "saturation": float(saturation),
        }

        edited_arr = apply_auto_edit(arr, settings=settings)

        if not output_path:
            import tempfile

            base, ext = os.path.splitext(image_path)
            output_path = os.path.join(
                tempfile.gettempdir(),
                f"stealzalut_edit_{int(time.time() * 1000)}{ext or '.jpg'}",
            )

        saved = save_image(edited_arr, output_path)
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
        return {
            "status": "success",
            "edited_path": saved,
            "preview_path": saved,
            "settings": settings,
            "execution_time_ms": elapsed_ms,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def compute_auto_settings(image_path: str) -> Dict[str, Any]:
    """Calculate LightCraft auto exposure and tone parameters for an image."""
    t0 = time.perf_counter()
    try:
        if not image_path or not os.path.isfile(image_path):
            return _make_error_response(
                message=f"Image file not found: {image_path}",
                error_code="IMAGE_NOT_FOUND",
            )

        from stealzalut.config import MAX_ANALYSIS_DIMENSION
        from stealzalut.core.auto_edit import get_auto_edit_settings
        from stealzalut.utils.image_io import load_proxy_image

        arr = load_proxy_image(image_path, max_dim=MAX_ANALYSIS_DIMENSION)
        settings = get_auto_edit_settings(arr)
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
        return {
            "status": "success",
            "settings": settings,
            "exposure": settings["exposure"],
            "contrast": settings["contrast"],
            "highlights": settings["highlights"],
            "shadows": settings["shadows"],
            "whites": settings["whites"],
            "blacks": settings["blacks"],
            "vibrance": settings["vibrance"],
            "saturation": settings["saturation"],
            "execution_time_ms": elapsed_ms,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def extract_palette(image_path: str, num_colors: int = 6) -> Dict[str, Any]:
    """Extract a dominant colour palette from an image."""
    try:
        if not image_path or not os.path.isfile(image_path):
            resp = _make_error_response(
                message=f"Image file not found: {image_path}",
                error_code="SOURCE_NOT_FOUND",
            )
            resp["palette"] = []
            return resp
        from stealzalut.config import PALETTE_RESIZE_DIM
        from stealzalut.utils.image_io import load_proxy_image

        img = load_proxy_image(image_path, max_dim=PALETTE_RESIZE_DIM)
        hex_colors = extract_dominant_palette(img, num_colors=num_colors)
        if not hex_colors:
            resp = _make_error_response(
                message="No dominant colours found in image",
                error_code="PALETTE_EXTRACTION_FAILED",
            )
            resp["palette"] = []
            return resp
        return {
            "status": "success",
            "palette": hex_colors,
            "count": len(hex_colors),
        }
    except Exception as exc:  # noqa: BLE001
        resp = _make_error_response(exc=exc)
        resp["palette"] = []
        return resp


def analyze_zone_palette_bridge(
    image_path: str,
    zone_count: int = DEFAULT_ZONE_COUNT,
) -> Dict[str, Any]:
    """Analyze a source image into colour zones and expose a JSON-friendly summary."""
    try:
        if not image_path or not os.path.isfile(image_path):
            return _make_error_response(
                message=f"Image file not found: {image_path}",
                error_code="SOURCE_NOT_FOUND",
            )

        from PIL import Image

        img = np.asarray(Image.open(image_path).convert("RGB"), dtype=np.uint8)
        palette = analyze_zones(img, zone_count=zone_count)
        return {
            "status": "success",
            "zone_count": len(palette.zones),
            "zones": [
                {
                    "id": int(zone.id),
                    "area": float(zone.area),
                    "l_rank": float(zone.l_rank),
                    "mean": [float(v) for v in zone.mean.tolist()],
                    "hex": zone.hex,
                    "centroid_xy": [float(v) for v in zone.centroid_xy],
                    "bbox": [float(v) for v in zone.bbox],
                }
                for zone in palette.zones
            ],
            "labels_shape": list(palette.labels.shape),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def match_zone_palette_bridge(
    source_path: str,
    reference_path: str,
    zone_count: int = DEFAULT_ZONE_COUNT,
) -> Dict[str, Any]:
    """Compute source→reference zone matching using the same Sinkhorn logic as the engine."""
    try:
        if not source_path or not os.path.isfile(source_path):
            return _make_error_response(
                message=f"Source image file not found: {source_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not reference_path or not os.path.isfile(reference_path):
            return _make_error_response(
                message=f"Reference image file not found: {reference_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        from PIL import Image

        src = np.asarray(Image.open(source_path).convert("RGB"), dtype=np.uint8)
        ref = np.asarray(Image.open(reference_path).convert("RGB"), dtype=np.uint8)
        match = match_zones(analyze_zones(src, zone_count), analyze_zones(ref, zone_count))
        return {
            "status": "success",
            "matching": {
                str(src_id): {
                    str(ref_id): float(weight)
                    for ref_id, weight in targets.items()
                }
                for src_id, targets in match.items()
            },
            "matching_entries": [
                {
                    "source_zone": str(src_id),
                    "targets": [
                        {"target_zone": str(ref_id), "weight": float(weight)}
                        for ref_id, weight in targets.items()
                    ],
                }
                for src_id, targets in match.items()
            ],
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


ZONE_NAMES: tuple[str, ...] = (
    "deep_shadows",
    "shadows",
    "low_mids",
    "midtones",
    "high_mids",
    "highlights",
    "whites",
)


def _oklab_to_oklch(lab: list[float]) -> list[float]:
    L, a, b = lab[0], lab[1], lab[2]
    C = float(np.hypot(a, b))
    h = float((np.degrees(np.arctan2(b, a))) % 360.0)
    return [float(L), C, h]


def _dominant_hue(C: float, h: float) -> str:
    if C < 0.03:
        return "Neutral"
    if 345.0 <= h or h < 30.0:
        return "Red"
    elif 30.0 <= h < 65.0:
        return "Orange"
    elif 65.0 <= h < 110.0:
        return "Yellow"
    elif 110.0 <= h < 165.0:
        return "Green"
    elif 165.0 <= h < 215.0:
        return "Cyan"
    elif 215.0 <= h < 275.0:
        return "Blue"
    elif 275.0 <= h < 315.0:
        return "Purple"
    else:
        return "Magenta"


def _hex_to_rgb_ints(hex_str: str) -> list[int]:
    clean = hex_str.lstrip("#")
    if len(clean) == 6:
        return [int(clean[i:i + 2], 16) for i in (0, 2, 4)]
    return [128, 128, 128]


def _describe_zone(name: str, hue: str) -> str:
    return f"{name.replace('_', ' ').capitalize()} · {hue}"


def _describe_zone_change(
    src_name: str,
    src_l: float,
    src_hue: str,
    ref_l: float,
    ref_hue: str,
) -> str:
    label = src_name.replace("_", " ").capitalize()
    dl = ref_l - src_l
    if dl < -0.04:
        lum_desc = "Assombri"
    elif dl > 0.04:
        lum_desc = "Éclairci"
    else:
        lum_desc = "Luminance préservée"

    if ref_hue == "Neutral":
        hue_desc = "désaturé / neutre"
    elif ref_hue != src_hue:
        hue_desc = f"teinte vers {ref_hue}"
    else:
        hue_desc = f"teinte {ref_hue} maintenue"
    return f"{label} -> {lum_desc}, {hue_desc}"


def fit_zone_transfer_bridge(
    source_path: str,
    reference_path: str,
    zone_count: int = DEFAULT_ZONE_COUNT,
    user_overrides_json: Optional[str] = None,
) -> Dict[str, Any]:
    """Fit a zone-aware transfer model and expose the resulting correspondence summary.

    Args:
        source_path: Path to the source image (RGB uint8).
        reference_path: Path to the reference image (RGB uint8).
        zone_count: Number of zones per palette.
        user_overrides_json: Optional JSON string mapping source_zone (str) to
            reference_zone (int), e.g. ``'{"0": 2, "3": 1}'``.  When provided,
            the automatic correspondence is overridden for those zones only.
    """
    try:
        if not source_path or not os.path.isfile(source_path):
            return _make_error_response(
                message=f"Source image file not found: {source_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not reference_path or not os.path.isfile(reference_path):
            return _make_error_response(
                message=f"Reference image file not found: {reference_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        import json

        from PIL import Image
        from stealzalut.core.correspondence import retarget

        src = np.asarray(Image.open(source_path).convert("RGB"), dtype=np.uint8)
        ref = np.asarray(Image.open(reference_path).convert("RGB"), dtype=np.uint8)
        model = fit_zone_transfer(src, ref, zone_count=zone_count)

        # Apply user overrides to the automatic correspondence table
        table = model.table
        user_overrides: Dict[str, int] = {}
        if user_overrides_json:
            try:
                raw = json.loads(user_overrides_json)
                user_overrides = {str(k): int(v) for k, v in raw.items()}
                for src_zone_str, ref_zone_id in user_overrides.items():
                    src_zone_id = int(src_zone_str)
                    table = retarget(table, src_zone_id, ref_zone_id)
            except (json.JSONDecodeError, ValueError, KeyError):
                pass  # Silently ignore malformed overrides; keep auto table

        def enrich_zone(zone_obj: Any, idx: int) -> Dict[str, Any]:
            name = ZONE_NAMES[idx] if idx < len(ZONE_NAMES) else f"zone_{idx}"
            oklab = [float(v) for v in zone_obj.mean.tolist()]
            oklch = _oklab_to_oklch(oklab)
            dominant = _dominant_hue(oklch[1], oklch[2])
            return {
                "index": idx,
                "id": int(zone_obj.id),
                "name": name,
                "hex": zone_obj.hex,
                "mean_rgb": zone_obj.hex,
                "mean_rgb_int": _hex_to_rgb_ints(zone_obj.hex),
                "mean_oklab": oklab,
                "mean_oklch": oklch,
                "area": float(zone_obj.area),
                "pixel_percentage": float(round(zone_obj.area * 100.0, 2)),
                "l_rank": float(zone_obj.l_rank),
                "dominant_hue": dominant,
                "describe_zone": _describe_zone(name, dominant),
            }

        src_zones = [enrich_zone(z, i) for i, z in enumerate(model.source.zones)]
        ref_zones = [enrich_zone(z, i) for i, z in enumerate(model.reference.zones)]
        ref_zone_map = {z["id"]: z for z in ref_zones}

        corr_list: list[Dict[str, Any]] = []
        for i, link in enumerate(table.links):
            primary_target = int(max(link.targets, key=lambda k: link.targets[k])) if link.targets else -1
            target_obj = ref_zone_map.get(primary_target)
            src_obj = src_zones[i] if i < len(src_zones) else None

            if src_obj and target_obj:
                change_desc = _describe_zone_change(
                    src_obj["name"],
                    src_obj["mean_oklch"][0],
                    src_obj["dominant_hue"],
                    target_obj["mean_oklch"][0],
                    target_obj["dominant_hue"],
                )
            else:
                change_desc = ""

            similarity = float(link.targets.get(primary_target, 1.0)) if primary_target != -1 else 0.0

            corr_list.append({
                "source_zone": int(link.source_zone),
                "src_idx": i,
                "ref_idx": (target_obj["index"] if target_obj else -1),
                "targets": {str(ref_id): float(weight) for ref_id, weight in link.targets.items()},
                "target_entries": [
                    {"target_zone": str(ref_id), "weight": float(weight)}
                    for ref_id, weight in link.targets.items()
                ],
                "strength": float(link.strength),
                "similarity": similarity,
                "locked": bool(link.locked),
                "origin": link.origin,
                "is_overridden": str(link.source_zone) in user_overrides,
                "primary_target": primary_target,
                "describe_change": change_desc,
            })

        result_table: Dict[str, Any] = {
            "source_zone_ids": [int(zone.id) for zone in model.source.zones],
            "reference_zone_ids": [int(zone.id) for zone in model.reference.zones],
            "analysis_id": model.analysis_id,
            "method": model.method,
            "source_zones": src_zones,
            "reference_zones": ref_zones,
            "correspondence": corr_list,
            "user_overrides": user_overrides,
        }
        return {"status": "success", **result_table}
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def get_zone_mask_bridge(
    image_path: str,
    zone_index: int,
    zone_count: int = DEFAULT_ZONE_COUNT,
    output_mask_path: str = "",
    mode: str = "bw",
) -> Dict[str, Any]:
    """Generate and save a soft zone-membership mask for the given zone.

    Args:
        image_path: Path to the source (or reference) image.
        zone_index: 0-based zone id whose mask to render.
        zone_count: Number of zones used for clustering.
        output_mask_path: Where to write the PNG mask (auto-generated if empty).
        mode: ``"bw"`` = greyscale uint8 (zone weight × 255); ``"color"`` = darkened
            image with the zone's mean colour overlaid where the mask is active.
    """
    try:
        if not image_path or not os.path.isfile(image_path):
            return _make_error_response(
                message=f"Image file not found: {image_path}",
                error_code="SOURCE_NOT_FOUND",
            )

        import tempfile

        from PIL import Image as PILImage

        from stealzalut.core.palette import analyze_zones

        img_arr = np.asarray(PILImage.open(image_path).convert("RGB"), dtype=np.uint8)
        palette = analyze_zones(img_arr, zone_count=zone_count)

        # labels is a 2-D array (H, W) of zone ids
        labels = palette.labels  # shape (H, W)

        if zone_index < 0 or zone_index >= len(palette.zones):
            return _make_error_response(
                message=f"zone_index {zone_index} out of range [0, {len(palette.zones) - 1}]",
                error_code="GENERIC_ERROR",
            )

        zone = palette.zones[zone_index]

        # Build a soft membership mask: 1.0 where label == zone.id, 0.0 elsewhere.
        # (The desktop uses a Mahalanobis-based soft weight; for the mask view a
        #  hard membership is sufficient and much faster on mobile.)
        mask_f = (labels == zone.id).astype(np.float32)  # shape (H, W) in [0,1]

        if mode == "color":
            # Darken image to 25 % then overlay zone colour where mask is active
            dark = (img_arr * 0.25).astype(np.uint8)
            mean_rgb = np.clip(zone.mean[::-1] * 255, 0, 255).astype(np.uint8) if hasattr(zone, "mean") else np.array([128, 128, 128], dtype=np.uint8)
            # Zone mean is in Oklab; convert back to sRGB
            from stealzalut.core.colorspace import oklab_to_srgb, gamut_compress
            mean_rgb_f = oklab_to_srgb(gamut_compress(zone.mean[None, :]))[0]
            mean_rgb_u8 = np.clip(mean_rgb_f * 255, 0, 255).astype(np.uint8)
            overlay = np.where(
                mask_f[:, :, None] > 0.5,
                mean_rgb_u8[None, None, :],
                dark,
            ).astype(np.uint8)
            out_img = PILImage.fromarray(overlay, mode="RGB")
        else:
            # Black-and-white mask
            mask_u8 = (mask_f * 255).astype(np.uint8)
            out_img = PILImage.fromarray(mask_u8, mode="L")

        if not output_mask_path:
            suffix = "_mask_color.png" if mode == "color" else "_mask_bw.png"
            fd, output_mask_path = tempfile.mkstemp(suffix=suffix, prefix=f"szl_zone{zone_index}")
            import os as _os
            _os.close(fd)

        out_img.save(output_mask_path, format="PNG")

        return {
            "status": "success",
            "mask_path": output_mask_path,
            "zone_id": int(zone.id),
            "zone_index": zone_index,
            "mode": mode,
            "area": float(zone.area),
            "hex": zone.hex,
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def parse_recipe_bridge(recipe_text: str) -> Dict[str, Any]:
    """Parse a recipe from free-form AI output or raw JSON and return a structured payload."""
    try:
        recipe = parse_recipe(recipe_text)
        return {
            "status": "success",
            "analysis_id": recipe.analysis_id,
            "mode": recipe.mode,
            "zone_count": recipe.zone_count,
            "recipe": recipe_to_dict(recipe),
            "json": dump_recipe(recipe, pretty=False),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def write_ai_pack_bridge(
    source_path: str,
    reference_path: str,
    output_dir: str,
    pack_name: str = "demo",
    zone_count: int = DEFAULT_ZONE_COUNT,
) -> Dict[str, Any]:
    """Build the lightweight AI analysis pack folder for a source/reference pair."""
    try:
        if not source_path or not os.path.isfile(source_path):
            return _make_error_response(
                message=f"Source image file not found: {source_path}",
                error_code="SOURCE_NOT_FOUND",
            )
        if not reference_path or not os.path.isfile(reference_path):
            return _make_error_response(
                message=f"Reference image file not found: {reference_path}",
                error_code="REFERENCE_NOT_FOUND",
            )

        from PIL import Image

        src = np.asarray(Image.open(source_path).convert("RGB"), dtype=np.uint8)
        ref = np.asarray(Image.open(reference_path).convert("RGB"), dtype=np.uint8)
        model = fit_zone_transfer(src, ref, zone_count=zone_count)
        folder = write_pack(output_dir, pack_name, src, ref, model, zone_count)
        return {
            "status": "success",
            "pack_dir": str(folder),
            "pack_name": pack_name,
            "analysis_id": model.analysis_id,
            "files": list(PACK_FILES),
            "prompt": build_prompt(model),
        }
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def generate_color_card(
    ref_path: str,
    output_card_path: str,
    method: str = DEFAULT_METHOD,
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Generate a visual calibration card showcasing how the reference style transforms colors."""
    try:
        if not ref_path or not os.path.isfile(ref_path):
            return _make_error_response(
                message=f"Reference image file not found: {ref_path}",
                error_code="REFERENCE_NOT_FOUND",
            )
        from stealzalut.core.color_card import run_color_card_pipeline

        return run_color_card_pipeline(
            ref_path=ref_path,
            output_card_path=output_card_path,
            method=method,
            intensity=intensity,
        )
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)


def clear_caches() -> Dict[str, Any]:
    """Clear all in-memory raw LUT and proxy caches."""
    try:
        from stealzalut.workers.transfer_worker import clear_lut_cache

        clear_lut_cache()
        return {"status": "success"}
    except Exception as exc:  # noqa: BLE001
        return _make_error_response(exc=exc)
