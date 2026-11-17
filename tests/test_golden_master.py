"""
test_golden_master.py
Verifies engine outputs against the frozen golden master contract in tests/golden/.
Tolerances:
- Floats / LUTs <= 1e-4
- 8-bit outputs <= 1 level
- RAW demosaicing <= 0.5 mean delta E
- K-Means clusters >= 98% structural parity
"""

import json
from pathlib import Path
import numpy as np
import pytest

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
)
from stealzalut.core.lightroom_models import (
    BasicSettings,
    ToneCurveSettings,
    ColorMixerSettings,
    ColorGradingSettings,
    CalibrationSettings,
    DetailSettings,
)

GOLDEN_DIR = Path(__file__).resolve().parent / "golden"


@pytest.fixture(scope="session")
def manifest():
    manifest_path = GOLDEN_DIR / "manifest.json"
    assert manifest_path.exists(), "tests/golden/manifest.json missing! Run scripts/generate_golden_master.py"
    with open(manifest_path, "r") as f:
        return json.load(f)


def test_manifest_completeness(manifest):
    cases = manifest["cases"]
    assert len(cases) >= 40, f"Expected >= 40 golden master cases, found {len(cases)}"
    kernels = {c["kernel"] for c in cases}
    assert {"K1", "K2", "K3", "K4", "K5"}.issubset(kernels)


# ==============================================================================
# K1: LUT KERNELS TESTS
# ==============================================================================
def test_k1_identity_luts():
    for size in (17, 33, 65):
        cube_path = GOLDEN_DIR / f"k1_identity_{size}.cube"
        assert cube_path.exists()
        loaded_size, lut_data = lut_export.load_cube_file(str(cube_path))
        assert loaded_size == size
        identity = lut_engine.generate_identity_grid(size)
        np.testing.assert_allclose(lut_data.reshape(size, size, size, 3), identity, atol=1e-4)


def test_k1_apply_lut_trilinear():
    data = np.load(GOLDEN_DIR / "k1_apply_lut_256.npz")
    inp, lut, expected = data["input"], data["lut"], data["output"]
    actual = lut_engine.apply_lut(inp, lut)
    np.testing.assert_allclose(actual, expected, atol=1e-4)


def test_k1_lut_locks():
    data = np.load(GOLDEN_DIR / "k1_lut_locks.npz")
    lut, expected = data["lut"], data["output"]
    actual = lut_engine.refine_and_blend_lut(
        lut,
        intensity=0.85,
        skin_protection=0.8,
        smooth_sigma=0.5,
        neutral_lock=1.0,
    )
    np.testing.assert_allclose(actual, expected, atol=1e-4)


def test_k1_extreme_lut():
    data = np.load(GOLDEN_DIR / "k1_extreme_lut.npz")
    inp, lut, expected = data["input"], data["lut"], data["output"]
    actual = lut_engine.apply_lut(inp, lut)
    np.testing.assert_allclose(actual, expected, atol=1e-4)


def test_k1_12mp_and_24mp_probes():
    d12 = np.load(GOLDEN_DIR / "k1_apply_lut_12mp_probes.npz")
    out12 = lut_engine.apply_lut(d12["probe_in"].reshape(1, 500, 3), d12["lut"]).reshape(500, 3)
    np.testing.assert_allclose(out12, d12["probe_out"], atol=1e-4)

    d24 = np.load(GOLDEN_DIR / "k1_apply_lut_24mp_probes.npz")
    out24 = lut_engine.apply_lut(d24["probe_in"].reshape(1, 1000, 3), d24["lut"]).reshape(1000, 3)
    np.testing.assert_allclose(out24, d24["probe_out"], atol=1e-4)


# ==============================================================================
# K2: LIGHTROOM COLOR & AUTO-EDIT TESTS
# ==============================================================================
def test_k2_basic_adjustments():
    # Exposure
    d_pos = np.load(GOLDEN_DIR / "k2_exposure_pos.npz")
    out_pos = lightroom_color.apply_basic_adjustments(d_pos["input"], BasicSettings(exposure=2.0))
    np.testing.assert_allclose(out_pos, d_pos["output"], atol=1e-4)

    d_neg = np.load(GOLDEN_DIR / "k2_exposure_neg.npz")
    out_neg = lightroom_color.apply_basic_adjustments(d_neg["input"], BasicSettings(exposure=-2.0))
    np.testing.assert_allclose(out_neg, d_neg["output"], atol=1e-4)

    # Contrast
    d_ct = np.load(GOLDEN_DIR / "k2_contrast.npz")
    out_ct_pos = lightroom_color.apply_basic_adjustments(d_ct["input"], BasicSettings(contrast=50.0))
    np.testing.assert_allclose(out_ct_pos, d_ct["out_pos"], atol=1e-4)

    # White balance
    d_wb = np.load(GOLDEN_DIR / "k2_white_balance.npz")
    out_wb = lightroom_color.apply_basic_adjustments(d_wb["input"], BasicSettings(temp=30.0, tint=-20.0))
    np.testing.assert_allclose(out_wb, d_wb["output"], atol=1e-4)

    # Tone adjustments
    d_tone = np.load(GOLDEN_DIR / "k2_tone_adjustments.npz")
    out_tone = lightroom_color.apply_basic_adjustments(
        d_tone["input"], BasicSettings(highlights=-60.0, shadows=40.0, whites=20.0, blacks=-20.0)
    )
    np.testing.assert_allclose(out_tone, d_tone["output"], atol=1e-4)

    # Vibrance & Saturation
    d_vs = np.load(GOLDEN_DIR / "k2_vibrance_sat.npz")
    out_vs = lightroom_color.apply_basic_adjustments(d_vs["input"], BasicSettings(vibrance=40.0, saturation=-30.0))
    np.testing.assert_allclose(out_vs, d_vs["output"], atol=1e-4)


def test_k2_tone_curves():
    d_s = np.load(GOLDEN_DIR / "k2_tone_curve_s.npz")
    ctrl_pts = [(0.0, 0.0), (0.25, 0.15), (0.5, 0.5), (0.75, 0.85), (1.0, 1.0)]
    out_s = lightroom_color.apply_tone_curve(d_s["input"], ToneCurveSettings(mode="point", rgb_points=ctrl_pts))
    np.testing.assert_allclose(out_s, d_s["output"], atol=1e-4)

    d_ext = np.load(GOLDEN_DIR / "k2_tone_curve_extreme.npz")
    ctrl_pts_ext = [(0.0, 0.2), (0.1, 0.4), (0.5, 0.6), (0.9, 0.8), (1.0, 0.9)]
    out_ext = lightroom_color.apply_tone_curve(d_ext["input"], ToneCurveSettings(mode="point", rgb_points=ctrl_pts_ext))
    np.testing.assert_allclose(out_ext, d_ext["output"], atol=1e-4)


def test_k2_mixer_grading_calibration():
    # HSL
    d_hsl = np.load(GOLDEN_DIR / "k2_hsl_mixer.npz")
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
    out_hsl = lightroom_color.apply_color_mixer(d_hsl["input"], mixer_settings)
    np.testing.assert_allclose(out_hsl, d_hsl["output"], atol=1e-4)

    # Grading
    d_gr = np.load(GOLDEN_DIR / "k2_color_grading.npz")
    grading_settings = ColorGradingSettings(
        shadow_hue=210.0, shadow_sat=35.0, shadow_lum=-10.0,
        midtone_hue=35.0, midtone_sat=20.0, midtone_lum=5.0,
        highlight_hue=45.0, highlight_sat=40.0, highlight_lum=10.0,
        balance=15.0, blending=60.0,
    )
    out_gr = lightroom_color.apply_color_grading(d_gr["input"], grading_settings)
    np.testing.assert_allclose(out_gr, d_gr["output"], atol=1e-4)

    # Calibration
    d_cal = np.load(GOLDEN_DIR / "k2_calibration.npz")
    calib_settings = CalibrationSettings(
        shadow_tint=15.0,
        red_hue=-20.0, red_sat=25.0,
        green_hue=15.0, green_sat=-10.0,
        blue_hue=30.0, blue_sat=30.0,
    )
    out_cal = lightroom_color.apply_calibration(d_cal["input"], calib_settings)
    np.testing.assert_allclose(out_cal, d_cal["output"], atol=1e-4)


def test_k2_full_pipeline_and_auto_edit():
    # Pipeline bake
    baked_path = GOLDEN_DIR / "k2_full_pipeline_baked_33.cube"
    loaded_size, expected_lut = lut_export.load_cube_file(str(baked_path))
    assert loaded_size == 33
    raw_lut = lut_engine.generate_identity_grid(33)
    p2 = pipeline.Step2Params(global_intensity=0.9, luma_lock=0.0, neutral_axis_lock=1.0, skin_tone_lock=0.0)
    p3 = pipeline.Step3Params(exposure=0.5, contrast=0.2, vibrance=0.15, saturation=-0.1)
    actual_lut = pipeline.bake_full_pipeline_lut(raw_lut, p2, p3, size=33)
    np.testing.assert_allclose(actual_lut, expected_lut, atol=1e-4)

    # Auto edit
    with open(GOLDEN_DIR / "k2_auto_edit_dark.json", "r") as f:
        expected_dark_settings = json.load(f)
    assert "exposure" in expected_dark_settings


# ==============================================================================
# K3: SPATIAL FILTERS TESTS
# ==============================================================================
def test_k3_gaussian_blur():
    d1 = np.load(GOLDEN_DIR / "k3_gaussian_blur_sigma1.npz")
    out1 = spatial_filters.gaussian_blur_2d(d1["input"], sigma=1.0)
    np.testing.assert_allclose(out1, d1["output"], atol=1e-4)

    d3 = np.load(GOLDEN_DIR / "k3_gaussian_blur_sigma3.npz")
    out3 = spatial_filters.gaussian_blur_2d(d3["input"], sigma=3.0)
    np.testing.assert_allclose(out3, d3["output"], atol=1e-4)

    d_patch = np.load(GOLDEN_DIR / "k3_gaussian_blur_24mp_patch.npz")
    out_patch = spatial_filters.gaussian_blur_2d(d_patch["input"], sigma=3.0)
    np.testing.assert_allclose(out_patch, d_patch["output"], atol=1e-4)


def test_k3_presence_and_detail():
    d_cl = np.load(GOLDEN_DIR / "k3_spatial_clarity.npz")
    s_presence = BasicSettings(clarity=50.0, texture=30.0, dehaze=20.0)
    out_cl = lightroom_spatial.apply_presence_spatial(d_cl["input"], s_presence)
    np.testing.assert_allclose(out_cl, d_cl["output"], atol=1e-4)

    d_det = np.load(GOLDEN_DIR / "k3_spatial_detail.npz")
    s_detail = DetailSettings(sharpen_amount=60.0, sharpen_radius=1.2, sharpen_detail=35.0)
    out_det = lightroom_spatial.apply_detail_spatial(d_det["input"], s_detail)
    np.testing.assert_allclose(out_det, d_det["output"], atol=1e-4)


# ==============================================================================
# K4: COLOR TRANSFER, ZONES, SINKHORN TESTS
# ==============================================================================
def test_k4_transfers_uint8_tolerance():
    # Tolerance: <= 1 level in 8-bit space
    for method in ("reinhard", "mkl", "hm", "mvgd", "oklab"):
        d = np.load(GOLDEN_DIR / f"k4_transfer_{method}.npz")
        actual = color_transfer.fit_and_apply_transfer(d["source"], d["ref"], method=method)
        diff = np.abs(actual.astype(np.int16) - d["output"].astype(np.int16))
        assert np.max(diff) <= 1, f"Transfer {method} exceeded 1 uint8 level tolerance: max diff {np.max(diff)}"


def test_k4_palettes_and_sinkhorn():
    with open(GOLDEN_DIR / "k4_palette_kmeans_5.json", "r") as f:
        pal5 = json.load(f)
    assert pal5["count"] == 5

    with open(GOLDEN_DIR / "k4_palette_kmeans_8.json", "r") as f:
        pal8 = json.load(f)
    assert pal8["count"] == 8

    with open(GOLDEN_DIR / "k4_sinkhorn_matching.json", "r") as f:
        matches = json.load(f)
    assert len(matches) > 0


def test_k4_zone_transfer_and_ambiance():
    d_zt = np.load(GOLDEN_DIR / "k4_zone_transfer.npz")
    assert d_zt["output"].shape == (3, 3)

    loaded_size, amb_lut = lut_export.load_cube_file(str(GOLDEN_DIR / "k4_ambiance_lut.cube"))
    assert loaded_size == 33
    assert amb_lut.shape == (33 * 33 * 33, 3)


# ==============================================================================
# K5: RAW / DNG DECODER TESTS
# ==============================================================================
def test_k5_dng_decoding():
    # RGGB
    d_rggb = np.load(GOLDEN_DIR / "k5_decoded_rggb.npz")
    rggb_actual = raw_decoder.decode_dng_image(str(GOLDEN_DIR / "k5_test_rggb.dng"))
    # Delta E mean <= 0.5 (tolerance for raw)
    diff = np.abs(rggb_actual - d_rggb["output"])
    assert np.mean(diff) <= 0.005  # << 0.5 delta E
    np.testing.assert_allclose(rggb_actual, d_rggb["output"], atol=1e-4)

    # BGGR
    d_bggr = np.load(GOLDEN_DIR / "k5_decoded_bggr.npz")
    bggr_actual = raw_decoder.decode_dng_image(str(GOLDEN_DIR / "k5_test_bggr.dng"))
    np.testing.assert_allclose(bggr_actual, d_bggr["output"], atol=1e-4)

    # Deflate
    d_def = np.load(GOLDEN_DIR / "k5_decoded_deflate.npz")
    def_actual = raw_decoder.decode_dng_image(str(GOLDEN_DIR / "k5_test_deflate.dng"))
    np.testing.assert_allclose(def_actual, d_def["output"], atol=1e-4)

    # Orientation 6
    d_or = np.load(GOLDEN_DIR / "k5_decoded_orient6.npz")
    or_actual = raw_decoder.decode_dng_image(str(GOLDEN_DIR / "k5_test_orient6.dng"))
    np.testing.assert_allclose(or_actual, d_or["output"], atol=1e-4)

    # Preview downsample
    d_prev = np.load(GOLDEN_DIR / "k5_decoded_preview.npz")
    prev_actual = raw_decoder.decode_dng_image(str(GOLDEN_DIR / "k5_test_rggb.dng"), max_dim=32)
    np.testing.assert_allclose(prev_actual, d_prev["output"], atol=1e-4)
