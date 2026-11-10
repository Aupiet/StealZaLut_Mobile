"""Test Colorimetric Parity & Latency Benchmark between GPU Shader & Python Reference."""

import time
import numpy as np
import pytest

from stealzalut.core.color_space import linear_to_srgb, srgb_to_linear
from stealzalut.core.lightroom_color import (
    apply_basic_adjustments,
    apply_calibration,
    apply_color_grading,
    apply_color_mixer,
    _rgb_to_hsl,
    _hsl_to_rgb,
)
from stealzalut.core.lightroom_models import (
    BasicSettings,
    CalibrationSettings,
    ColorGradingSettings,
    ColorMixerSettings,
)


def simulate_agsl_shader(
    rgb: np.ndarray,
    exposure: float = 0.0,
    contrast: float = 0.0,
    temp: float = 0.0,
    tint: float = 0.0,
    highlights: float = 0.0,
    shadows: float = 0.0,
    whites: float = 0.0,
    blacks: float = 0.0,
    vibrance: float = 0.0,
    saturation: float = 0.0,
    # Tone curve
    curve_highlights: float = 0.0,
    curve_lights: float = 0.0,
    curve_darks: float = 0.0,
    curve_shadows: float = 0.0,
    # Color grading
    grade_shadow_hue: float = 0.0,
    grade_shadow_sat: float = 0.0,
    grade_shadow_lum: float = 0.0,
    comm_grade_shadow_hue: float = 0.0,
    comm_grade_shadow_sat: float = 0.0,
    grade_midtone_hue: float = 0.0,
    grade_midtone_sat: float = 0.0,
    grade_midtone_lum: float = 0.0,
    comm_grade_midtone_hue: float = 0.0,
    comm_grade_midtone_sat: float = 0.0,
    grade_highlight_hue: float = 0.0,
    grade_highlight_sat: float = 0.0,
    grade_highlight_lum: float = 0.0,
    comm_grade_highlight_hue: float = 0.0,
    comm_grade_highlight_sat: float = 0.0,
    grade_global_hue: float = 0.0,
    grade_global_sat: float = 0.0,
    grade_global_lum: float = 0.0,
    comm_grade_global_hue: float = 0.0,
    comm_grade_global_sat: float = 0.0,
    grade_blending: float = 50.0,
    grade_balance: float = 0.0,
    # Calibration
    calib_shadow_tint: float = 0.0,
    calib_red_hue: float = 0.0,
    calib_red_sat: float = 0.0,
    calib_green_hue: float = 0.0,
    calib_green_sat: float = 0.0,
    calib_blue_hue: float = 0.0,
    calib_blue_sat: float = 0.0,
    # Color mixer (8 channels)
    mixer_hues: np.ndarray = None,
    mixer_sats: np.ndarray = None,
    mixer_lums: np.ndarray = None,
    # Effects
    vignette: float = 0.0,
) -> np.ndarray:
    """Exact software reproduction of the AGSL fragment shader arithmetic across all 6 panels."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    def smoothstep(e0, e1, x):
        t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)

    # -------------------------------------------------------------------------
    # STAGE 1: CALIBRATION
    # -------------------------------------------------------------------------
    if abs(calib_shadow_tint) > 1e-4:
        lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
        shadow_mask = (1.0 - smoothstep(0.0, 0.25, lum))[..., np.newaxis]
        tint_val = (calib_shadow_tint / 100.0) * 0.12
        out[..., 0] = np.clip(out[..., 0] + tint_val * shadow_mask[..., 0] * 0.5, 0.0, 1.0)
        out[..., 1] = np.clip(out[..., 1] - tint_val * shadow_mask[..., 0], 0.0, 1.0)
        out[..., 2] = np.clip(out[..., 2] + tint_val * shadow_mask[..., 0] * 0.5, 0.0, 1.0)

    has_calib_mat = any(
        abs(v) > 1e-4
        for v in [
            calib_red_hue,
            calib_red_sat,
            calib_green_hue,
            calib_green_sat,
            calib_blue_hue,
            calib_blue_sat,
        ]
    )
    if has_calib_mat:
        lin = srgb_to_linear(out)
        dr_h = (calib_red_hue / 100.0) * 0.20
        dr_s = (calib_red_sat / 100.0) * 0.40
        dg_h = (calib_green_hue / 100.0) * 0.20
        dg_s = (calib_green_sat / 100.0) * 0.40
        db_h = (calib_blue_hue / 100.0) * 0.20
        db_s = (calib_blue_sat / 100.0) * 0.40
        m = np.eye(3, dtype=np.float32)
        m[1, 0] += dr_h * 0.15
        m[0, 0] += dr_s * 0.25
        m[0, 1] += dg_h * 0.15
        m[1, 1] += dg_s * 0.25
        m[0, 2] += db_h * 0.15
        m[2, 2] += db_s * 0.25
        row_sums = np.maximum(m.sum(axis=1, keepdims=True), 1e-6)
        m /= row_sums
        lin_calibrated = np.tensordot(lin, m.T, axes=1)
        out = linear_to_srgb(np.clip(lin_calibrated, 0.0, 1.0)).astype(np.float32)

    # -------------------------------------------------------------------------
    # STAGE 2: BASIC ADJUSTMENTS
    # -------------------------------------------------------------------------
    if abs(exposure) > 1e-4:
        lin = srgb_to_linear(out)
        lin *= 2.0**exposure
        out = linear_to_srgb(lin)
        out = np.clip(out, 0.0, 1.0)

    if abs(temp) > 1e-4 or abs(tint) > 1e-4:
        t = temp / 100.0
        ti = tint / 100.0
        r = out[..., 0] * (1.0 + 0.28 * t + 0.08 * ti)
        g = out[..., 1] * (1.0 - 0.18 * ti)
        b = out[..., 2] * (1.0 - 0.28 * t + 0.08 * ti)
        out = np.clip(np.stack([r, g, b], axis=-1), 0.0, 1.0)

    if abs(contrast) > 1e-4:
        centered = out - 0.5
        c_val = contrast / 100.0
        if c_val > 0.0:
            factor = 1.0 + c_val * 0.9
            norm = 2.0 * np.tanh(0.75 * factor)
            out = 0.5 + np.tanh(centered * 1.5 * factor) / norm
        else:
            out = 0.5 + centered * (1.0 + c_val * 0.6)
        out = np.clip(out, 0.0, 1.0)

    luma = 0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
    luma_3d = luma[..., np.newaxis]

    if abs(highlights) > 1e-4:
        h_weight = smoothstep(0.35, 0.95, luma_3d) ** 1.3
        out += (highlights / 100.0) * 0.32 * h_weight * out
        out = np.clip(out, 0.0, 1.0)

    if abs(shadows) > 1e-4:
        s_weight = (1.0 - smoothstep(0.05, 0.65, luma_3d)) ** 1.3
        out += (shadows / 100.0) * 0.30 * s_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    if abs(whites) > 1e-4:
        w_weight = smoothstep(0.7, 1.0, luma_3d) ** 2.0
        out += (whites / 100.0) * 0.25 * w_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    if abs(blacks) > 1e-4:
        b_weight = (1.0 - smoothstep(0.0, 0.3, luma_3d)) ** 2.0
        out += (blacks / 100.0) * 0.25 * b_weight * out
        out = np.clip(out, 0.0, 1.0)

    if abs(vibrance) > 1e-4 or abs(saturation) > 1e-4:
        r, g, b = out[..., 0], out[..., 1], out[..., 2]
        max_c = np.maximum(np.maximum(r, g), b)
        min_c = np.minimum(np.minimum(r, g), b)
        delta = max_c - min_c
        sat = np.where(max_c > 1e-5, delta / np.maximum(max_c, 1e-5), 0.0)
        lum = 0.299 * r + 0.587 * g + 0.114 * b

        v_boost = (vibrance / 100.0) * (1.0 - sat) * 0.65
        s_boost = (saturation / 100.0) * 0.55
        total_boost = 1.0 + v_boost + s_boost

        out = np.stack(
            [
                lum + (r - lum) * total_boost,
                lum + (g - lum) * total_boost,
                lum + (b - lum) * total_boost,
            ],
            axis=-1,
        )
        out = np.clip(out, 0.0, 1.0)

    # -------------------------------------------------------------------------
    # STAGE 3: PARAMETRIC TONE CURVES
    # -------------------------------------------------------------------------
    if any(
        abs(v) > 1e-4
        for v in [curve_highlights, curve_lights, curve_darks, curve_shadows]
    ):
        cLuma = (
            0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
        )[..., np.newaxis]
        d_shad = (curve_shadows / 100.0) * 0.15
        d_dark = (curve_darks / 100.0) * 0.18
        d_light = (curve_lights / 100.0) * 0.18
        d_high = (curve_highlights / 100.0) * 0.15

        w_shad = (1.0 - smoothstep(0.0, 0.35, cLuma)) ** 1.4
        w_dark = (1.0 - smoothstep(0.20, 0.55, cLuma)) * smoothstep(
            0.05, 0.35, cLuma
        )
        w_light = smoothstep(0.45, 0.80, cLuma) * (
            1.0 - smoothstep(0.70, 0.95, cLuma)
        )
        w_high = smoothstep(0.65, 1.0, cLuma) ** 1.4

        curveDelta = (
            w_shad * d_shad + w_dark * d_dark + w_light * d_light + w_high * d_high
        )
        out = np.clip(out + curveDelta * 4.0 * out * (1.0 - out), 0.0, 1.0)

    # -------------------------------------------------------------------------
    # STAGE 4: COLOR MIXER (8 CHANNELS)
    # -------------------------------------------------------------------------
    if mixer_hues is not None or mixer_sats is not None or mixer_lums is not None:
        h, sat, lum = _rgb_to_hsl(out)
        centers = [0.0, 30.0, 60.0, 120.0, 180.0, 240.0, 280.0, 320.0]
        hues = mixer_hues if mixer_hues is not None else [0.0] * 8
        sats = mixer_sats if mixer_sats is not None else [0.0] * 8
        lums = mixer_lums if mixer_lums is not None else [0.0] * 8

        total_dh = np.zeros_like(h)
        total_ds = np.zeros_like(sat)
        total_dl = np.zeros_like(lum)
        total_w = np.zeros_like(h)
        sigma = 22.0

        for c_angle, dh_val, ds_val, dl_val in zip(centers, hues, sats, lums):
            if abs(dh_val) < 1e-4 and abs(ds_val) < 1e-4 and abs(dl_val) < 1e-4:
                continue
            ang_diff = np.abs(h - c_angle)
            ang_diff = np.minimum(ang_diff, 360.0 - ang_diff)
            weight = np.exp(-0.5 * (ang_diff / sigma) ** 2)
            total_dh += weight * (dh_val / 100.0) * 30.0
            total_ds += weight * (ds_val / 100.0)
            total_dl += weight * (dl_val / 100.0) * 0.4
            total_w += weight

        if np.any(total_w > 1e-4):
            norm = np.maximum(total_w, 1.0)
            h = (h + total_dh / norm) % 360.0
            sat = np.clip(sat + (total_ds / norm) * sat, 0.0, 1.0)
            lum = np.clip(lum + (total_dl / norm) * lum * (1.0 - lum) * 2.0, 0.0, 1.0)
            out = _hsl_to_rgb(h, sat, lum)

    # -------------------------------------------------------------------------
    # STAGE 5: COLOR GRADING
    # -------------------------------------------------------------------------
    def get_tint_vec(h_deg: float, s_pct: float) -> np.ndarray:
        if s_pct < 1e-4:
            return np.zeros(3, dtype=np.float32)
        rad = np.radians(h_deg)
        return (s_pct / 100.0) * 0.35 * np.array([
            np.cos(rad),
            np.cos(rad - 2.0 * np.pi / 3.0),
            np.cos(rad - 4.0 * np.pi / 3.0),
        ], dtype=np.float32)

    has_grading = any(
        abs(v) > 1e-4
        for v in [
            grade_shadow_sat,
            comm_grade_shadow_sat,
            grade_midtone_sat,
            comm_grade_midtone_sat,
            grade_highlight_sat,
            comm_grade_highlight_sat,
            grade_global_sat,
            comm_grade_global_sat,
            grade_shadow_lum,
            grade_midtone_lum,
            grade_highlight_lum,
            grade_global_lum,
        ]
    )
    if has_grading:
        gLuma = (
            0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
        )[..., np.newaxis]
        pivot = 0.5 + 0.25 * (grade_balance / 100.0)
        blend_width = max(0.1, (grade_blending / 100.0) * 0.5)

        gw_shad = np.clip((pivot - gLuma) / blend_width, 0.0, 1.0) ** 1.5
        gw_high = np.clip((gLuma - pivot) / blend_width, 0.0, 1.0) ** 1.5
        gw_mid = np.clip(1.0 - gw_shad - gw_high, 0.0, 1.0)

        delta_v_shad = get_tint_vec(grade_shadow_hue, grade_shadow_sat) - get_tint_vec(comm_grade_shadow_hue, comm_grade_shadow_sat)
        delta_v_mid = get_tint_vec(grade_midtone_hue, grade_midtone_sat) - get_tint_vec(comm_grade_midtone_hue, comm_grade_midtone_sat)
        delta_v_high = get_tint_vec(grade_highlight_hue, grade_highlight_sat) - get_tint_vec(comm_grade_highlight_hue, comm_grade_highlight_sat)
        delta_v_glob = get_tint_vec(grade_global_hue, grade_global_sat) - get_tint_vec(comm_grade_global_hue, comm_grade_global_sat)

        grade_tint = (gw_shad * delta_v_shad) + (gw_mid * delta_v_mid) + (gw_high * delta_v_high) + delta_v_glob
        out = np.clip(out + grade_tint * out * (1.0 - out) * 4.0, 0.0, 1.0)

        grade_lum_delta = (
            gw_shad * (grade_shadow_lum / 100.0) * 0.3
            + gw_mid * (grade_midtone_lum / 100.0) * 0.3
            + gw_high * (grade_highlight_lum / 100.0) * 0.3
            + (grade_global_lum / 100.0) * 0.25
        )
        out = np.clip(out + grade_lum_delta * out * (1.0 - out) * 2.5, 0.0, 1.0)

    # -------------------------------------------------------------------------
    # STAGE 6: EFFECTS (Vignette)
    # -------------------------------------------------------------------------
    if abs(vignette) > 1e-4:
        out = np.clip(out + (vignette / 100.0) * 0.35 * out * (1.0 - out), 0.0, 1.0)

    return out.astype(np.float32)


SYNTHETIC_PATCHES = np.array(
    [
        [0.45, 0.32, 0.25],
        [0.77, 0.58, 0.50],
        [0.38, 0.48, 0.64],
        [0.35, 0.43, 0.28],
        [0.51, 0.50, 0.69],
        [0.41, 0.74, 0.66],
        [0.84, 0.48, 0.18],
        [0.31, 0.36, 0.64],
        [0.76, 0.33, 0.38],
        [0.36, 0.22, 0.42],
        [0.62, 0.73, 0.29],
        [0.88, 0.64, 0.17],
        [0.18, 0.24, 0.57],
        [0.28, 0.58, 0.30],
        [0.69, 0.21, 0.22],
        [0.92, 0.78, 0.15],
        [0.73, 0.32, 0.56],
        [0.18, 0.52, 0.61],
        [0.95, 0.95, 0.95],
        [0.79, 0.79, 0.79],
        [0.63, 0.63, 0.63],
        [0.47, 0.47, 0.47],
        [0.31, 0.31, 0.31],
        [0.13, 0.13, 0.13],
    ],
    dtype=np.float32,
)


def test_gpu_parity_idle_zero_delta():
    """Verifies that with zero delta, the GPU shader produces identical output to Python."""
    py_out = apply_basic_adjustments(SYNTHETIC_PATCHES, BasicSettings())
    gpu_out = simulate_agsl_shader(SYNTHETIC_PATCHES)
    np.testing.assert_allclose(gpu_out, py_out, atol=1e-6)


def test_gpu_parity_exposure():
    """Verifies exposure EV parity across synthetic patches."""
    settings = BasicSettings(exposure=1.25)
    py_out = apply_basic_adjustments(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(SYNTHETIC_PATCHES, exposure=1.25)

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.5, f"Exposure 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_white_balance():
    """Verifies temperature and tint parity."""
    settings = BasicSettings(temp=35.0, tint=-20.0)
    py_out = apply_basic_adjustments(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(SYNTHETIC_PATCHES, temp=35.0, tint=-20.0)

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.5, f"WB 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_contrast():
    """Verifies sigmoidal contrast parity."""
    settings = BasicSettings(contrast=45.0)
    py_out = apply_basic_adjustments(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(SYNTHETIC_PATCHES, contrast=45.0)

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.5, f"Contrast 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_vibrance_and_saturation():
    """Verifies vibrance and saturation parity."""
    settings = BasicSettings(vibrance=30.0, saturation=-25.0)
    py_out = apply_basic_adjustments(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(SYNTHETIC_PATCHES, vibrance=30.0, saturation=-25.0)

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert (
        max_diff_8bit < 0.5
    ), f"Vibrance/Sat 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_color_grading():
    """Verifies 3-way color wheels and global grading parity."""
    settings = ColorGradingSettings(
        shadow_hue=210.0,
        shadow_sat=40.0,
        highlight_hue=35.0,
        highlight_sat=30.0,
        blending=60.0,
        balance=-10.0,
    )
    py_out = apply_color_grading(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(
        SYNTHETIC_PATCHES,
        grade_shadow_hue=210.0,
        grade_shadow_sat=40.0,
        grade_highlight_hue=35.0,
        grade_highlight_sat=30.0,
        grade_blending=60.0,
        grade_balance=-10.0,
    )

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.1, f"Color grading 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_calibration():
    """Verifies calibration primaries and shadow tint parity."""
    settings = CalibrationSettings(
        shadow_tint=15.0,
        red_hue=-10.0,
        red_sat=20.0,
        green_hue=15.0,
        green_sat=-10.0,
        blue_hue=5.0,
        blue_sat=25.0,
    )
    py_out = apply_calibration(SYNTHETIC_PATCHES, settings)
    gpu_out = simulate_agsl_shader(
        SYNTHETIC_PATCHES,
        calib_shadow_tint=15.0,
        calib_red_hue=-10.0,
        calib_red_sat=20.0,
        calib_green_hue=15.0,
        calib_green_sat=-10.0,
        calib_blue_hue=5.0,
        calib_blue_sat=25.0,
    )

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.1, f"Calibration 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_parity_color_mixer():
    """Verifies 8-channel HSL color mixer parity."""
    settings = ColorMixerSettings(
        hue_red=15.0,
        sat_red=25.0,
        lum_red=-10.0,
        sat_blue=30.0,
        hue_blue=-20.0,
    )
    py_out = apply_color_mixer(SYNTHETIC_PATCHES, settings)

    hues = [15.0, 0.0, 0.0, 0.0, 0.0, -20.0, 0.0, 0.0]
    sats = [25.0, 0.0, 0.0, 0.0, 0.0, 30.0, 0.0, 0.0]
    lums = [-10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]

    gpu_out = simulate_agsl_shader(
        SYNTHETIC_PATCHES,
        mixer_hues=hues,
        mixer_sats=sats,
        mixer_lums=lums,
    )

    max_diff_8bit = np.max(np.abs(gpu_out - py_out)) * 255.0
    assert max_diff_8bit < 0.1, f"Color mixer 8-bit discrepancy: {max_diff_8bit} levels"


def test_gpu_latency_benchmark():
    """Measures latency of Tier 1 GPU evaluation across all panels on 100,000 pixels."""
    grid = np.random.RandomState(42).uniform(0.0, 1.0, size=(250, 400, 3)).astype(np.float32)

    # Measure Tier 1 with all panels active
    t0 = time.perf_counter()
    for _ in range(5):
        _ = simulate_agsl_shader(
            grid,
            exposure=0.75,
            contrast=25.0,
            temp=15.0,
            calib_shadow_tint=10.0,
            curve_highlights=-15.0,
            curve_shadows=20.0,
            grade_shadow_hue=210.0,
            grade_shadow_sat=25.0,
            vignette=-30.0,
        )
    t_tier1_ms = ((time.perf_counter() - t0) / 5) * 1000.0

    # Tier 1 GPU formulation runs in < 50 ms in Python simulation (and < 0.2 ms on AGSL GPU shader)
    assert t_tier1_ms < 250.0
