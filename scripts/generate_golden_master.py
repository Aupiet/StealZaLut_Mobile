#!/usr/bin/env python3
"""
generate_golden_master.py
Freezes current Python engine outputs across K1-K5 (~45 test cases) into tests/golden/.
Serves as golden master contract between Python engine and Kotlin engine.
"""

import json
import os
import sys
from pathlib import Path
import numpy as np
from PIL import Image

# Ensure stealzalut and tests are importable
repo_root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(repo_root))
sys.path.insert(0, str(repo_root / "app" / "src" / "main" / "python"))

from stealzalut.core import (
    lut_engine,
    lut_export,
    pipeline,
    lightroom_color,
    lightroom_spatial,
    spatial_filters,
    auto_edit,
    color_transfer,
    palette,
    matching,
    zone_transfer,
    ambiance,
    raw_decoder,
    recipe,
    ai_pack,
    preset_manager,
)
from stealzalut.core.lightroom_models import (
    BasicSettings,
    ToneCurveSettings,
    ColorMixerSettings,
    ColorGradingSettings,
    CalibrationSettings,
    DetailSettings,
)

GOLDEN_DIR = repo_root / "tests" / "golden"
GOLDEN_DIR.mkdir(parents=True, exist_ok=True)

manifest = {
    "version": "1.0",
    "tolerances": {
        "float": 1e-4,
        "uint8": 1,
        "raw_delta_e": 0.5,
        "zone_cluster_ratio": 0.98,
        "zone_delta_e": 1.0,
    },
    "cases": [],
}


def make_gradient_image(width: int, height: int, seed: int = 42) -> np.ndarray:
    """Generate reproducible RGB image in [0, 1] float32."""
    rng = np.random.RandomState(seed)
    y, x = np.mgrid[0:height, 0:width].astype(np.float32)
    r = x / max(1, width - 1)
    g = y / max(1, height - 1)
    b = 0.5 * (r + g)
    noise = rng.uniform(-0.02, 0.02, (height, width)).astype(np.float32)
    img = np.stack([r, g, b], axis=-1) + noise[..., None]
    return np.clip(img, 0.0, 1.0).astype(np.float32)


def make_uint8_image(width: int, height: int, seed: int = 42) -> np.ndarray:
    return (make_gradient_image(width, height, seed) * 255.0).round().astype(np.uint8)


def record_case(name: str, kernel: str, desc: str, tol_type: str, files: list, metrics: dict):
    manifest["cases"].append({
        "name": name,
        "kernel": kernel,
        "description": desc,
        "tolerance_type": tol_type,
        "files": files,
        "metrics": metrics,
    })
    print(f"[{kernel}] Generated {name}: {desc}")


# ==============================================================================
# K1: LUT KERNELS (Identity, Apply LUT Trilinear, Locks, Probes 12MP/24MP, .cube)
# ==============================================================================
def gen_k1_lut():
    print("Generating K1: LUT Kernels...")

    # 1. Identity 17, 33, 65
    for size in (17, 33, 65):
        grid = lut_engine.generate_identity_grid(size).astype(np.float32)
        cube_path = GOLDEN_DIR / f"k1_identity_{size}.cube"
        lut_export.export_cube(grid, str(cube_path), size=size, title=f"Identity {size}")
        record_case(
            f"k1_identity_{size}",
            "K1",
            f"Identity LUT {size}x{size}x{size}",
            "float",
            [f"k1_identity_{size}.cube"],
            {"size": size, "shape": list(grid.shape), "min": float(grid.min()), "max": float(grid.max())},
        )

    # 2. Sample Color LUT 33x33x33 with non-trivial transform (contrast + tint)
    id33 = lut_engine.generate_identity_grid(33).astype(np.float32)
    test_lut = np.clip((id33 - 0.5) * 1.3 + 0.5, 0.0, 1.0)
    test_lut[..., 0] = np.clip(test_lut[..., 0] * 1.15, 0.0, 1.0)  # Red boost
    test_lut[..., 2] = np.clip(test_lut[..., 2] * 0.85, 0.0, 1.0)  # Blue decrease
    test_lut_path = GOLDEN_DIR / "k1_test_lut_33.cube"
    lut_export.export_cube(test_lut, str(test_lut_path), size=33, title="Test LUT 33")

    # 3. Apply LUT Trilinear on 256x256 image
    img256 = make_gradient_image(256, 256, seed=42)
    out256 = lut_engine.apply_lut(img256, test_lut).astype(np.float32)
    np.savez_compressed(
        GOLDEN_DIR / "k1_apply_lut_256.npz",
        input=img256,
        lut=test_lut,
        output=out256,
    )
    record_case(
        "k1_apply_lut_256",
        "K1",
        "Trilinear LUT interpolation on 256x256 image",
        "float",
        ["k1_apply_lut_256.npz"],
        {"shape": list(out256.shape), "mean": float(out256.mean()), "std": float(out256.std())},
    )

    # 4. Refine and blend LUT (skin protection, neutral lock, smoothing)
    blended_lut = lut_engine.refine_and_blend_lut(
        test_lut,
        intensity=0.85,
        skin_protection=0.8,
        smooth_sigma=0.5,
        neutral_lock=1.0,
    ).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k1_lut_locks.npz", lut=test_lut, output=blended_lut)
    record_case(
        "k1_lut_locks",
        "K1",
        "LUT with skin protection, neutral lock, and smoothing",
        "float",
        ["k1_lut_locks.npz"],
        {"shape": list(blended_lut.shape), "mean": float(blended_lut.mean()), "std": float(blended_lut.std())},
    )

    # 5. Extreme LUT (inversion and extreme saturation)
    inv_lut = 1.0 - id33
    out_inv = lut_engine.apply_lut(img256, inv_lut).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k1_extreme_lut.npz", input=img256, lut=inv_lut, output=out_inv)
    record_case(
        "k1_extreme_lut",
        "K1",
        "Extreme inverted LUT on 256x256 image",
        "float",
        ["k1_extreme_lut.npz"],
        {"mean": float(out_inv.mean()), "std": float(out_inv.std())},
    )

    # 6. 12 MP (4000x3000) probe samples
    rng = np.random.RandomState(12000)
    probe_y_12 = rng.randint(0, 3000, size=500)
    probe_x_12 = rng.randint(0, 4000, size=500)
    probe_pixels_in = np.zeros((500, 3), dtype=np.float32)
    for i in range(500):
        y, x = probe_y_12[i], probe_x_12[i]
        r = x / 3999.0
        g = y / 2999.0
        b = 0.5 * (r + g)
        probe_pixels_in[i] = [r, g, b]
    probe_out_12 = lut_engine.apply_lut(probe_pixels_in.reshape(1, 500, 3), test_lut).reshape(500, 3).astype(np.float32)
    np.savez_compressed(
        GOLDEN_DIR / "k1_apply_lut_12mp_probes.npz",
        probe_y=probe_y_12,
        probe_x=probe_x_12,
        probe_in=probe_pixels_in,
        probe_out=probe_out_12,
        lut=test_lut,
    )
    record_case(
        "k1_apply_lut_12mp_probes",
        "K1",
        "12 MP (4000x3000) trilinear LUT probe coordinates",
        "float",
        ["k1_apply_lut_12mp_probes.npz"],
        {"probe_count": 500, "mean": float(probe_out_12.mean()), "std": float(probe_out_12.std())},
    )

    # 7. 24 MP (6000x4000) probe samples
    rng24 = np.random.RandomState(24000)
    probe_y_24 = rng24.randint(0, 4000, size=1000)
    probe_x_24 = rng24.randint(0, 6000, size=1000)
    probe_pixels_in_24 = np.zeros((1000, 3), dtype=np.float32)
    for i in range(1000):
        y, x = probe_y_24[i], probe_x_24[i]
        r = x / 5999.0
        g = y / 3999.0
        b = 0.5 * (r + g)
        probe_pixels_in_24[i] = [r, g, b]
    probe_out_24 = lut_engine.apply_lut(probe_pixels_in_24.reshape(1, 1000, 3), test_lut).reshape(1000, 3).astype(np.float32)
    np.savez_compressed(
        GOLDEN_DIR / "k1_apply_lut_24mp_probes.npz",
        probe_y=probe_y_24,
        probe_x=probe_x_24,
        probe_in=probe_pixels_in_24,
        probe_out=probe_out_24,
        lut=test_lut,
    )
    record_case(
        "k1_apply_lut_24mp_probes",
        "K1",
        "24 MP (6000x4000) trilinear LUT probe coordinates",
        "float",
        ["k1_apply_lut_24mp_probes.npz"],
        {"probe_count": 1000, "mean": float(probe_out_24.mean()), "std": float(probe_out_24.std())},
    )


# ==============================================================================
# K2: LIGHTROOM COLOR PIPELINE & AUTO EDIT
# ==============================================================================
def gen_k2_lightroom():
    print("Generating K2: Lightroom Pipeline & Auto-Edit...")
    img = make_gradient_image(128, 128, seed=100)

    # 8. Exposure positive (+2.0 EV)
    s_exp_pos = BasicSettings(exposure=2.0)
    out_exp_pos = lightroom_color.apply_basic_adjustments(img, s_exp_pos).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_exposure_pos.npz", input=img, output=out_exp_pos)
    record_case("k2_exposure_pos", "K2", "Exposure +2.0 EV", "float", ["k2_exposure_pos.npz"], {"mean": float(out_exp_pos.mean())})

    # 9. Exposure negative (-2.0 EV)
    s_exp_neg = BasicSettings(exposure=-2.0)
    out_exp_neg = lightroom_color.apply_basic_adjustments(img, s_exp_neg).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_exposure_neg.npz", input=img, output=out_exp_neg)
    record_case("k2_exposure_neg", "K2", "Exposure -2.0 EV", "float", ["k2_exposure_neg.npz"], {"mean": float(out_exp_neg.mean())})

    # 10. Contrast (+50, -50)
    out_contrast_pos = lightroom_color.apply_basic_adjustments(img, BasicSettings(contrast=50.0)).astype(np.float32)
    out_contrast_neg = lightroom_color.apply_basic_adjustments(img, BasicSettings(contrast=-50.0)).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_contrast.npz", input=img, out_pos=out_contrast_pos, out_neg=out_contrast_neg)
    record_case("k2_contrast", "K2", "Contrast +/- 50", "float", ["k2_contrast.npz"], {"mean_pos": float(out_contrast_pos.mean())})

    # 11. White Balance: Temp +30, Tint -20
    out_wb = lightroom_color.apply_basic_adjustments(img, BasicSettings(temp=30.0, tint=-20.0)).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_white_balance.npz", input=img, output=out_wb)
    record_case("k2_white_balance", "K2", "White Balance Temp/Tint", "float", ["k2_white_balance.npz"], {"mean": float(out_wb.mean())})

    # 12. Highlights & Shadows
    out_hl_sh = lightroom_color.apply_basic_adjustments(
        img, BasicSettings(highlights=-60.0, shadows=40.0, whites=20.0, blacks=-20.0)
    ).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_tone_adjustments.npz", input=img, output=out_hl_sh)
    record_case("k2_tone_adjustments", "K2", "Highlights/Shadows/Whites/Blacks", "float", ["k2_tone_adjustments.npz"], {"mean": float(out_hl_sh.mean())})

    # 13. Vibrance & Saturation
    out_vib_sat = lightroom_color.apply_basic_adjustments(
        img, BasicSettings(vibrance=40.0, saturation=-30.0)
    ).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_vibrance_sat.npz", input=img, output=out_vib_sat)
    record_case("k2_vibrance_sat", "K2", "Vibrance +40 / Saturation -30", "float", ["k2_vibrance_sat.npz"], {"mean": float(out_vib_sat.mean())})

    # 14. Tone Curve Fritsch-Carlson S-curve
    ctrl_pts = [(0.0, 0.0), (0.25, 0.15), (0.5, 0.5), (0.75, 0.85), (1.0, 1.0)]
    curve_s_settings = ToneCurveSettings(mode="point", rgb_points=ctrl_pts)
    out_curve_s = lightroom_color.apply_tone_curve(img, curve_s_settings).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_tone_curve_s.npz", input=img, output=out_curve_s)
    record_case("k2_tone_curve_s", "K2", "Fritsch-Carlson S-Curve", "float", ["k2_tone_curve_s.npz"], {"mean": float(out_curve_s.mean())})

    # 15. Tone Curve Extreme shadow lift
    ctrl_pts_ext = [(0.0, 0.2), (0.1, 0.4), (0.5, 0.6), (0.9, 0.8), (1.0, 0.9)]
    curve_ext_settings = ToneCurveSettings(mode="point", rgb_points=ctrl_pts_ext)
    out_curve_ext = lightroom_color.apply_tone_curve(img, curve_ext_settings).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_tone_curve_extreme.npz", input=img, output=out_curve_ext)
    record_case("k2_tone_curve_extreme", "K2", "Extreme lifted Tone Curve", "float", ["k2_tone_curve_extreme.npz"], {"mean": float(out_curve_ext.mean())})

    # 16. HSL 8 Channels Color Mixer
    mixer_settings = ColorMixerSettings(
        hue_red=15.0, sat_red=25.0, lum_red=-10.0,
        hue_orange=-10.0, sat_orange=30.0, lum_orange=5.0,
        hue_yellow=20.0, sat_yellow=-20.0, lum_yellow=15.0,
        hue_green=-30.0, sat_green=40.0, lum_green=-15.0,
        hue_aqua=10.0, sat_aqua=15.0, lum_aqua=10.0,
        hue_blue=-25.0, sat_blue=35.0, lum_blue=-20.0,
        hue_purple=15.0, sat_purple=-15.0, lum_purple=5.0,
        hue_magenta=-10.0, sat_magenta=20.0, lum_magenta=-5.0,
    )
    out_hsl = lightroom_color.apply_color_mixer(img, mixer_settings).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_hsl_mixer.npz", input=img, output=out_hsl)
    record_case("k2_hsl_mixer", "K2", "8-channel HSL color mixer", "float", ["k2_hsl_mixer.npz"], {"mean": float(out_hsl.mean())})

    # 17. Color Grading 3-Way Wheels
    grading_settings = ColorGradingSettings(
        shadow_hue=210.0, shadow_sat=35.0, shadow_lum=-10.0,
        midtone_hue=35.0, midtone_sat=20.0, midtone_lum=5.0,
        highlight_hue=45.0, highlight_sat=40.0, highlight_lum=10.0,
        balance=15.0, blending=60.0,
    )
    out_grading = lightroom_color.apply_color_grading(img, grading_settings).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_color_grading.npz", input=img, output=out_grading)
    record_case("k2_color_grading", "K2", "3-way Color Grading wheels", "float", ["k2_color_grading.npz"], {"mean": float(out_grading.mean())})

    # 18. Calibration
    calib_settings = CalibrationSettings(
        shadow_tint=15.0,
        red_hue=-20.0, red_sat=25.0,
        green_hue=15.0, green_sat=-10.0,
        blue_hue=30.0, blue_sat=30.0,
    )
    out_calib = lightroom_color.apply_calibration(img, calib_settings).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k2_calibration.npz", input=img, output=out_calib)
    record_case("k2_calibration", "K2", "Camera Calibration primaries", "float", ["k2_calibration.npz"], {"mean": float(out_calib.mean())})

    # 19. Full Pipeline baked into 33³ LUT
    raw_lut_33 = lut_engine.generate_identity_grid(33)
    p2 = pipeline.Step2Params(global_intensity=0.9, luma_lock=0.0, neutral_axis_lock=1.0, skin_tone_lock=0.0)
    p3 = pipeline.Step3Params(exposure=0.5, contrast=0.2, vibrance=0.15, saturation=-0.1)
    baked_lut_33 = pipeline.bake_full_pipeline_lut(
        raw_lut=raw_lut_33,
        step2_params=p2,
        step3_params=p3,
        size=33,
    ).astype(np.float32)
    baked_path = GOLDEN_DIR / "k2_full_pipeline_baked_33.cube"
    lut_export.export_cube(baked_lut_33, str(baked_path), size=33, title="Full Pipeline Baked 33")
    record_case("k2_full_pipeline_baked_33", "K2", "Full pipeline baked into 33³ LUT", "float", ["k2_full_pipeline_baked_33.cube"], {"mean": float(baked_lut_33.mean()), "shape": list(baked_lut_33.shape)})

    # 20. Auto-Edit on dark underexposed image
    dark_img = (make_gradient_image(128, 128, seed=200) * 0.25).astype(np.float32)
    auto_dark_settings = auto_edit.get_auto_edit_settings(dark_img)
    auto_dark_lut = auto_edit.generate_auto_edit_lut(dark_img, lut_size=33).astype(np.float32)
    with open(GOLDEN_DIR / "k2_auto_edit_dark.json", "w") as f:
        json.dump(auto_dark_settings, f, indent=2)
    lut_export.export_cube(auto_dark_lut, str(GOLDEN_DIR / "k2_auto_edit_dark_lut.cube"), size=33, title="Auto Edit Dark")
    record_case("k2_auto_edit_dark", "K2", "Auto-Edit analysis & LUT on dark image", "float", ["k2_auto_edit_dark.json", "k2_auto_edit_dark_lut.cube"], {"exposure": float(auto_dark_settings.get("exposure", 0.0))})

    # 21. Auto-Edit on bright high-key image
    bright_img = np.clip(make_gradient_image(128, 128, seed=300) * 0.7 + 0.3, 0.0, 1.0).astype(np.float32)
    auto_bright_settings = auto_edit.get_auto_edit_settings(bright_img)
    auto_bright_lut = auto_edit.generate_auto_edit_lut(bright_img, lut_size=33).astype(np.float32)
    with open(GOLDEN_DIR / "k2_auto_edit_bright.json", "w") as f:
        json.dump(auto_bright_settings, f, indent=2)
    lut_export.export_cube(auto_bright_lut, str(GOLDEN_DIR / "k2_auto_edit_bright_lut.cube"), size=33, title="Auto Edit Bright")
    record_case("k2_auto_edit_bright", "K2", "Auto-Edit analysis & LUT on bright image", "float", ["k2_auto_edit_bright.json", "k2_auto_edit_bright_lut.cube"], {"exposure": float(auto_bright_settings.get("exposure", 0.0))})


# ==============================================================================
# K3: SPATIAL FILTERS (Gaussian Blur, Clarity, Sharpening, 24MP Probes)
# ==============================================================================
def gen_k3_spatial():
    print("Generating K3: Spatial Filters...")
    img = make_gradient_image(128, 128, seed=400)

    # 22. Gaussian Blur sigma=1.0
    blur1 = spatial_filters.gaussian_blur_2d(img, sigma=1.0).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k3_gaussian_blur_sigma1.npz", input=img, output=blur1)
    record_case("k3_gaussian_blur_sigma1", "K3", "Separable Gaussian blur sigma=1.0", "float", ["k3_gaussian_blur_sigma1.npz"], {"mean": float(blur1.mean())})

    # 23. Gaussian Blur sigma=3.0 (proxy 512x512)
    img512 = make_gradient_image(512, 512, seed=401)
    blur3 = spatial_filters.gaussian_blur_2d(img512, sigma=3.0).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k3_gaussian_blur_sigma3.npz", input=img512, output=blur3)
    record_case("k3_gaussian_blur_sigma3", "K3", "Separable Gaussian blur sigma=3.0 on 512x512", "float", ["k3_gaussian_blur_sigma3.npz"], {"mean": float(blur3.mean())})

    # 24. 24 MP Gaussian blur probe verification:
    # A 256x256 patch taken from center of synthetic 24 MP image blurred with sigma=3.0
    patch = make_gradient_image(256, 256, seed=402)
    blur_patch = spatial_filters.gaussian_blur_2d(patch, sigma=3.0).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k3_gaussian_blur_24mp_patch.npz", input=patch, output=blur_patch)
    record_case("k3_gaussian_blur_24mp_patch", "K3", "Gaussian blur sigma=3.0 tile for 24 MP", "float", ["k3_gaussian_blur_24mp_patch.npz"], {"mean": float(blur_patch.mean())})

    # 25. Spatial Presence (Clarity + Texture + Dehaze)
    s_presence = BasicSettings(clarity=50.0, texture=30.0, dehaze=20.0)
    clarity_out = lightroom_spatial.apply_presence_spatial(img, s_presence).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k3_spatial_clarity.npz", input=img, output=clarity_out)
    record_case("k3_spatial_clarity", "K3", "Spatial Presence clarity, texture, dehaze", "float", ["k3_spatial_clarity.npz"], {"mean": float(clarity_out.mean())})

    # 26. Spatial Detail (Sharpening)
    s_detail = DetailSettings(sharpen_amount=60.0, sharpen_radius=1.2, sharpen_detail=35.0)
    sharpen_out = lightroom_spatial.apply_detail_spatial(img, s_detail).astype(np.float32)
    np.savez_compressed(GOLDEN_DIR / "k3_spatial_detail.npz", input=img, output=sharpen_out)
    record_case("k3_spatial_detail", "K3", "Spatial Detail sharpening & noise", "float", ["k3_spatial_detail.npz"], {"mean": float(sharpen_out.mean())})


# ==============================================================================
# K4: COLOR TRANSFER, ZONES, SINKHORN, AMBIANCE, RECIPE/AI PACK
# ==============================================================================
def gen_k4_color_transfer_and_zones():
    print("Generating K4: Color Transfer & Zones...")
    src_u8 = make_uint8_image(128, 128, seed=500)
    ref_u8 = make_uint8_image(128, 128, seed=501)

    # 27. Reinhard (Lab)
    t_reinhard = color_transfer.fit_and_apply_transfer(src_u8, ref_u8, method="reinhard")
    np.savez_compressed(GOLDEN_DIR / "k4_transfer_reinhard.npz", source=src_u8, ref=ref_u8, output=t_reinhard)
    record_case("k4_transfer_reinhard", "K4", "Reinhard Lab color transfer", "uint8", ["k4_transfer_reinhard.npz"], {"mean": float(t_reinhard.mean())})

    # 28. MKL
    t_mkl = color_transfer.fit_and_apply_transfer(src_u8, ref_u8, method="mkl")
    np.savez_compressed(GOLDEN_DIR / "k4_transfer_mkl.npz", source=src_u8, ref=ref_u8, output=t_mkl)
    record_case("k4_transfer_mkl", "K4", "MKL color transfer", "uint8", ["k4_transfer_mkl.npz"], {"mean": float(t_mkl.mean())})

    # 29. Histogram Matching (HM)
    t_hm = color_transfer.fit_and_apply_transfer(src_u8, ref_u8, method="hm")
    np.savez_compressed(GOLDEN_DIR / "k4_transfer_hm.npz", source=src_u8, ref=ref_u8, output=t_hm)
    record_case("k4_transfer_hm", "K4", "Histogram matching color transfer", "uint8", ["k4_transfer_hm.npz"], {"mean": float(t_hm.mean())})

    # 30. MVGD
    t_mvgd = color_transfer.fit_and_apply_transfer(src_u8, ref_u8, method="mvgd")
    np.savez_compressed(GOLDEN_DIR / "k4_transfer_mvgd.npz", source=src_u8, ref=ref_u8, output=t_mvgd)
    record_case("k4_transfer_mvgd", "K4", "MVGD color transfer", "uint8", ["k4_transfer_mvgd.npz"], {"mean": float(t_mvgd.mean())})

    # 31. Oklab transfer
    t_oklab = color_transfer.fit_and_apply_transfer(src_u8, ref_u8, method="oklab")
    np.savez_compressed(GOLDEN_DIR / "k4_transfer_oklab.npz", source=src_u8, ref=ref_u8, output=t_oklab)
    record_case("k4_transfer_oklab", "K4", "Oklab color transfer", "uint8", ["k4_transfer_oklab.npz"], {"mean": float(t_oklab.mean())})

    # 32. Palette K-Means (5 zones)
    pal5 = palette.analyze_zones(src_u8, zone_count=5)
    pal5_dict = {
        "count": len(pal5.zones),
        "zones": [{"id": int(z.id), "hex": str(z.hex), "area": float(z.area), "mean": [float(x) for x in z.mean]} for z in pal5.zones],
    }
    with open(GOLDEN_DIR / "k4_palette_kmeans_5.json", "w") as f:
        json.dump(pal5_dict, f, indent=2)
    record_case("k4_palette_kmeans_5", "K4", "K-Means Oklab palette extraction (5 zones)", "zone_kmeans", ["k4_palette_kmeans_5.json"], {"count": 5})

    # 33. Palette K-Means (8 zones)
    pal8 = palette.analyze_zones(src_u8, zone_count=8)
    pal8_dict = {
        "count": len(pal8.zones),
        "zones": [{"id": int(z.id), "hex": str(z.hex), "area": float(z.area), "mean": [float(x) for x in z.mean]} for z in pal8.zones],
    }
    with open(GOLDEN_DIR / "k4_palette_kmeans_8.json", "w") as f:
        json.dump(pal8_dict, f, indent=2)
    record_case("k4_palette_kmeans_8", "K4", "K-Means Oklab palette extraction (8 zones)", "zone_kmeans", ["k4_palette_kmeans_8.json"], {"count": 8})

    # 34. Sinkhorn Matching
    pal_ref = palette.analyze_zones(ref_u8, zone_count=5)
    matches = matching.match_zones(pal5, pal_ref)
    matches_serializable = {
        str(k): {str(sub_k): float(val) for sub_k, val in sub_v.items()}
        for k, sub_v in matches.items()
    }
    with open(GOLDEN_DIR / "k4_sinkhorn_matching.json", "w") as f:
        json.dump(matches_serializable, f, indent=2)
    record_case("k4_sinkhorn_matching", "K4", "Sinkhorn log-domain optimal transport matching", "float", ["k4_sinkhorn_matching.json"], {"num_sources": len(matches)})

    # 35. Zone Transfer Model with User Overrides
    zt_model = zone_transfer.fit_zone_transfer(src_u8, ref_u8, zone_count=5, user_overrides={"1": 2})
    colors_f64 = np.array([[0.2, 0.3, 0.4], [0.7, 0.8, 0.9], [0.1, 0.9, 0.1]], dtype=np.float64)
    transferred = zt_model.apply(colors_f64)
    np.savez_compressed(GOLDEN_DIR / "k4_zone_transfer.npz", input=colors_f64, output=transferred)
    record_case("k4_zone_transfer", "K4", "Zone transfer application on sample points", "float", ["k4_zone_transfer.npz"], {"mean": float(transferred.mean())})

    # 36. Ambiance Model & LUT
    amb_lut = lut_engine.compute_lut(src_u8, ref_u8, method="ambiance", size=33).astype(np.float32)
    lut_export.export_cube(amb_lut, str(GOLDEN_DIR / "k4_ambiance_lut.cube"), size=33, title="Ambiance LUT")
    record_case("k4_ambiance_lut", "K4", "Ambiance mood transfer 33³ LUT", "float", ["k4_ambiance_lut.cube"], {"mean": float(amb_lut.mean())})

    # 37. Recipe serialization / deserialization roundtrip
    rec_text = '''{
  "format": "stealzalut.recipe",
  "version": 2,
  "analysis_id": "golden42",
  "mode": "zones",
  "global": {"zone_separation": "soft", "mix": 0.85},
  "links": [
    {"source_zone": "S1", "targets": {"2": 0.7, "3": 0.3}, "strength": "strong", "luminance_strength": 1.0, "chroma_strength": 1.0},
    {"source_zone": 3, "keep": true}
  ]
}'''
    rec_obj = recipe.parse_recipe(rec_text)
    rec_dump = recipe.dump_recipe(rec_obj)
    with open(GOLDEN_DIR / "k4_recipe.json", "w") as f:
        f.write(rec_dump)
    record_case("k4_recipe", "K4", "Recipe JSON serialization format", "float", ["k4_recipe.json"], {"analysis_id": rec_obj.analysis_id})

    # 38. AI Pack serialization
    pack_folder = ai_pack.write_pack(str(GOLDEN_DIR), "k4", src_u8, ref_u8, zt_model, zone_count=5)
    pack_data = {}
    for p_file in ai_pack.PACK_FILES:
        fp = pack_folder / p_file
        if fp.exists():
            with open(fp, "r", encoding="utf-8", errors="ignore") as pf:
                pack_data[p_file] = pf.read()[:500]  # Store prefix
    with open(GOLDEN_DIR / "k4_ai_pack.json", "w") as f:
        json.dump(pack_data, f, indent=2)
    record_case("k4_ai_pack", "K4", "AI Pack JSON container serialization", "float", ["k4_ai_pack.json"], {"pack_files": list(pack_data.keys())})


# ==============================================================================
# K5: RAW / DNG DECODER (TIFF/DNG, Bayer Malvar-He-Cutler, Color Matrix, EXIF)
# ==============================================================================
from tests.test_raw_decoder import create_synthetic_dng_file


def gen_k5_raw():
    print("Generating K5: RAW / DNG Decoder...")

    # 39. DNG Uncompressed RGGB
    dng_rggb_path = GOLDEN_DIR / "k5_test_rggb.dng"
    create_synthetic_dng_file(str(dng_rggb_path), width=64, height=48, pattern=[0, 1, 1, 2], compression=1)
    rgb_rggb = raw_decoder.decode_dng_image(str(dng_rggb_path))
    np.savez_compressed(GOLDEN_DIR / "k5_decoded_rggb.npz", output=rgb_rggb.astype(np.float32))
    record_case("k5_decoded_rggb", "K5", "DNG uncompressed RGGB demosaiced", "raw_delta_e", ["k5_test_rggb.dng", "k5_decoded_rggb.npz"], {"shape": list(rgb_rggb.shape), "mean": float(rgb_rggb.mean())})

    # 40. DNG Uncompressed BGGR
    dng_bggr_path = GOLDEN_DIR / "k5_test_bggr.dng"
    create_synthetic_dng_file(str(dng_bggr_path), width=64, height=48, pattern=[2, 1, 1, 0], compression=1)
    rgb_bggr = raw_decoder.decode_dng_image(str(dng_bggr_path))
    np.savez_compressed(GOLDEN_DIR / "k5_decoded_bggr.npz", output=rgb_bggr.astype(np.float32))
    record_case("k5_decoded_bggr", "K5", "DNG uncompressed BGGR demosaiced", "raw_delta_e", ["k5_test_bggr.dng", "k5_decoded_bggr.npz"], {"shape": list(rgb_bggr.shape), "mean": float(rgb_bggr.mean())})

    # 41. DNG Deflate Compressed
    dng_deflate_path = GOLDEN_DIR / "k5_test_deflate.dng"
    create_synthetic_dng_file(str(dng_deflate_path), width=64, height=48, pattern=[0, 1, 1, 2], compression=8)
    rgb_def = raw_decoder.decode_dng_image(str(dng_deflate_path))
    np.savez_compressed(GOLDEN_DIR / "k5_decoded_deflate.npz", output=rgb_def.astype(np.float32))
    record_case("k5_decoded_deflate", "K5", "DNG deflate-compressed demosaiced", "raw_delta_e", ["k5_test_deflate.dng", "k5_decoded_deflate.npz"], {"shape": list(rgb_def.shape), "mean": float(rgb_def.mean())})

    # 42. DNG EXIF Orientations: 6 (90 deg CW)
    dng_orient_path = GOLDEN_DIR / "k5_test_orient6.dng"
    create_synthetic_dng_file(str(dng_orient_path), width=64, height=48, pattern=[0, 1, 1, 2], orientation=6)
    rgb_or = raw_decoder.decode_dng_image(str(dng_orient_path))
    np.savez_compressed(GOLDEN_DIR / "k5_decoded_orient6.npz", output=rgb_or.astype(np.float32))
    record_case("k5_decoded_orient6", "K5", "DNG EXIF orientation 6 handling", "raw_delta_e", ["k5_test_orient6.dng", "k5_decoded_orient6.npz"], {"shape": list(rgb_or.shape)})

    # 43. DNG Preview Downsampling
    preview_rgb = raw_decoder.decode_dng_image(str(dng_rggb_path), max_dim=32)
    np.savez_compressed(GOLDEN_DIR / "k5_decoded_preview.npz", output=preview_rgb.astype(np.float32))
    record_case("k5_decoded_preview", "K5", "DNG preview fast downsampling before demosaicing", "raw_delta_e", ["k5_decoded_preview.npz"], {"shape": list(preview_rgb.shape)})


def main():
    print(f"Generating Golden Master in {GOLDEN_DIR}...")
    gen_k1_lut()
    gen_k2_lightroom()
    gen_k3_spatial()
    gen_k4_color_transfer_and_zones()
    gen_k5_raw()

    manifest_path = GOLDEN_DIR / "manifest.json"
    with open(manifest_path, "w") as f:
        json.dump(manifest, f, indent=2)

    total_cases = len(manifest["cases"])
    print(f"\nSuccessfully generated {total_cases} Golden Master test cases in {manifest_path}!")


if __name__ == "__main__":
    main()
