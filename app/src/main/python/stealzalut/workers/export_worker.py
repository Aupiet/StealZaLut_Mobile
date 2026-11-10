"""LUT-export worker pipeline.

Orchestrates 3-D LUT retrieval/computation, Stage 2 filter refinement,
Stage 3 post-grading baking, and standard Adobe ``.cube`` file export.
"""

from __future__ import annotations

import os
import time
from typing import Any, Dict, Optional


from stealzalut.config import (
    DEFAULT_INTENSITY,
    DEFAULT_LUT_SIZE,
    DEFAULT_METHOD,
    DEFAULT_NEUTRAL_LOCK,
    DEFAULT_PRESERVE_LUMINANCE,
    DEFAULT_SKIN_PROTECTION,
    DEFAULT_SMOOTH_SIGMA,
)
from stealzalut.core.lut_engine import apply_lut, generate_identity_lattice
from stealzalut.core.lut_export import export_cube, load_cube_file
from stealzalut.core.pipeline import (
    Step2Params,
    Step3Params,
    apply_step3_color_grading,
    bake_full_pipeline_lut,
)
from stealzalut.utils.image_io import (
    create_proxy_image,
    get_clean_exif_bytes,
    load_image,
    resolve_export_dimensions,
    save_image,
)
from stealzalut.workers.transfer_worker import get_or_compute_raw_lut


def run_export_pipeline(
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
    # Extended 3-Stage Pipeline Parameters
    luma_lock: Optional[float] = None,
    neutral_axis_lock: Optional[float] = None,
    skin_tone_lock: Optional[float] = None,
    extremes_rolloff: float = 0.0,
    spatial_3d: Optional[float] = None,
    temperature: float = 0.0,
    tint: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    custom_lut_path: Optional[str] = None,
    lr_settings: Optional[Any] = None,
) -> Dict[str, Any]:
    """Execute the full WYSIWYG LUT-export pipeline.

    Computes or retrieves cached raw LUT, bakes Stage 2 refinement and Stage 3
    colorimetric post-grading directly into the 3D grid, and writes an Adobe .cube file.
    """
    start: float = time.monotonic()

    if isinstance(lr_settings, str) and lr_settings.strip():
        import json

        try:
            lr_settings = json.loads(lr_settings)
        except Exception:
            pass

    try:
        # Resolve Step 2 parameters with backward-compatible aliases
        eff_luma = preserve_luminance if luma_lock is None else luma_lock
        eff_neutral = neutral_lock if neutral_axis_lock is None else neutral_axis_lock
        eff_skin = skin_protection if skin_tone_lock is None else skin_tone_lock
        eff_spatial = smooth_sigma if spatial_3d is None else spatial_3d

        step2 = Step2Params(
            global_intensity=intensity,
            luma_lock=eff_luma,
            neutral_axis_lock=eff_neutral,
            skin_tone_lock=eff_skin,
            extremes_rolloff=extremes_rolloff,
            spatial_3d=eff_spatial,
        )

        step3 = Step3Params(
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
        )

        # 1. Retrieve or build the base raw LUT
        if custom_lut_path and os.path.isfile(custom_lut_path):
            _, raw_lut = load_cube_file(custom_lut_path)
        elif ref_path and os.path.isfile(ref_path):
            raw_lut = get_or_compute_raw_lut(
                src_path=src_path,
                ref_path=ref_path,
                method=method,
                lut_size=lut_size,
                preserve_luminance=eff_luma,
                soft_clip=True,
            )
        else:
            # Standalone photo editor mode (no reference image)
            raw_lut = generate_identity_lattice(lut_size)

        # 2. Fully bake Stages 1, 2, and 3 into the 3D volume
        baked_lut = bake_full_pipeline_lut(
            raw_lut=raw_lut,
            step2_params=step2,
            step3_params=step3,
            size=lut_size,
            lr_settings=lr_settings,
        )
        del raw_lut

        # 3. Export Adobe .cube file with industry standard header
        cube_path: str = export_cube(
            lut_data=baked_lut,
            output_path=output_cube_path,
            size=lut_size,
            title="StealZaLut_Export",
        )
        del baked_lut

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "cube_path": cube_path,
            "lut_path": cube_path,
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


def run_image_export_pipeline(
    src_path: str,
    ref_path: Optional[str] = None,
    output_image_path: str = "",
    method: str = DEFAULT_METHOD,
    intensity: float = DEFAULT_INTENSITY,
    max_export_dim: int | None = 2048,
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
    # Extended 3-Stage Pipeline Parameters
    luma_lock: Optional[float] = None,
    neutral_axis_lock: Optional[float] = None,
    skin_tone_lock: Optional[float] = None,
    extremes_rolloff: float = 0.0,
    spatial_3d: Optional[float] = None,
    temperature: float = 0.0,
    tint: float = 0.0,
    clarity: float = 0.0,
    sharpen: float = 0.0,
    grain: float = 0.0,
    custom_lut_path: Optional[str] = None,
    lr_settings: Optional[Any] = None,
) -> Dict[str, Any]:
    """Execute high-resolution image export with absolute WYSIWYG fidelity."""
    start: float = time.monotonic()

    if isinstance(lr_settings, str) and lr_settings.strip():
        import json

        try:
            lr_settings = json.loads(lr_settings)
        except Exception:
            pass

    try:
        eff_luma = preserve_luminance if luma_lock is None else luma_lock
        eff_neutral = neutral_lock if neutral_axis_lock is None else neutral_axis_lock
        eff_skin = skin_protection if skin_tone_lock is None else skin_tone_lock
        eff_spatial = smooth_sigma if spatial_3d is None else spatial_3d

        step2 = Step2Params(
            global_intensity=intensity,
            luma_lock=eff_luma,
            neutral_axis_lock=eff_neutral,
            skin_tone_lock=eff_skin,
            extremes_rolloff=extremes_rolloff,
            spatial_3d=eff_spatial,
        )

        step3 = Step3Params(
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
        )

        # 1. Base LUT retrieval
        if custom_lut_path and os.path.isfile(custom_lut_path):
            _, raw_lut = load_cube_file(custom_lut_path)
        elif ref_path and os.path.isfile(ref_path):
            raw_lut = get_or_compute_raw_lut(
                src_path=src_path,
                ref_path=ref_path,
                method=method,
                lut_size=lut_size,
                preserve_luminance=eff_luma,
                soft_clip=True,
            )
        else:
            raw_lut = generate_identity_lattice(lut_size)

        # 2. Bake colorimetric parameters into the 3D LUT
        baked_lut = bake_full_pipeline_lut(
            raw_lut=raw_lut,
            step2_params=step2,
            step3_params=step3,
            size=lut_size,
            lr_settings=lr_settings,
        )
        del raw_lut

        # 3. Check if crop is active in Lightroom develop settings
        has_active_crop = False
        if isinstance(lr_settings, dict):
            crop_dict = lr_settings.get("crop", {})
            if isinstance(crop_dict, dict):
                has_active_crop = (
                    abs(crop_dict.get("left", 0.0)) > 1e-4
                    or abs(crop_dict.get("top", 0.0)) > 1e-4
                    or abs(crop_dict.get("right", 1.0) - 1.0) > 1e-4
                    or abs(crop_dict.get("bottom", 1.0) - 1.0) > 1e-4
                    or abs(crop_dict.get("angle", 0.0)) > 1e-4
                )

        # 4. Load source image at full native resolution
        src_img = load_image(src_path)
        orig_h, orig_w = src_img.shape[:2]

        # In non-cropped mode, downscaling early saves peak memory; in cropped mode,
        # we preserve full resolution through the crop pipeline to maintain maximum sensor detail
        if not has_active_crop and max_export_dim > 0 and max(orig_w, orig_h) > max_export_dim:
            src_work = create_proxy_image(src_img, max_dim=max_export_dim)
            del src_img
        else:
            src_work = src_img

        # 5. Apply the exact 3D LUT via trilinear interpolation (WYSIWYG guarantee)
        graded = apply_lut(src_work, baked_lut)
        del src_work, baked_lut

        # 6. Apply 2D spatial effects (clarity, sharpen, grain, crop & straighten)
        if lr_settings is not None or clarity > 1e-4 or sharpen > 1e-4 or grain > 1e-4:
            from stealzalut.utils.image_io import to_float32, to_uint8

            graded_f = to_float32(graded)
            graded_f = apply_step3_color_grading(
                graded_f,
                params=step3,
                is_lut_baking=False,
                lr_settings=lr_settings,
            )
            graded = to_uint8(graded_f)
            del graded_f

        # 7. If crop was active and a maximum export dimension was requested, scale the cropped result
        out_h, out_w = graded.shape[:2]
        if max_export_dim > 0 and max(out_w, out_h) > max_export_dim:
            graded = create_proxy_image(graded, max_dim=max_export_dim)
            out_h, out_w = graded.shape[:2]

        # 8. Extract EXIF metadata from source image and save to disk
        source_exif = get_clean_exif_bytes(src_path)
        saved_path: str = save_image(
            graded,
            output_image_path,
            quality=quality,
            exif_bytes=source_exif,
        )
        del graded

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
