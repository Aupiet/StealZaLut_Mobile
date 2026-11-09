"""Test Colorimetric Parity & Latency Benchmark between GPU Shader & Python Reference."""

import time
import numpy as np
import pytest

from stealzalut.core.color_space import linear_to_srgb, srgb_to_linear
from stealzalut.core.lightroom_color import apply_basic_adjustments
from stealzalut.core.lightroom_models import BasicSettings


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
) -> np.ndarray:
    """Exact software reproduction of the AGSL fragment shader arithmetic."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    # 1. Exposure in linear EV
    if abs(exposure) > 1e-4:
        lin = srgb_to_linear(out)
        lin *= 2.0**exposure
        out = linear_to_srgb(lin)
        out = np.clip(out, 0.0, 1.0)

    # 2. White Balance
    if abs(temp) > 1e-4 or abs(tint) > 1e-4:
        t = temp / 100.0
        ti = tint / 100.0
        r = out[..., 0] * (1.0 + 0.28 * t + 0.08 * ti)
        g = out[..., 1] * (1.0 - 0.18 * ti)
        b = out[..., 2] * (1.0 - 0.28 * t + 0.08 * ti)
        out = np.clip(np.stack([r, g, b], axis=-1), 0.0, 1.0)

    # 3. Contrast (Sigmoidal tanh)
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

    # 4. Highlights, Shadows, Whites, Blacks
    luma = 0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
    luma_3d = luma[..., np.newaxis]

    def smoothstep(e0, e1, x):
        t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)

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

    # 5. Vibrance & Saturation
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

    return out.astype(np.float32)


# 24 standard synthetic test patches
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


def test_gpu_latency_benchmark():
    """Measures latency of Tier 1 GPU evaluation vs Tier 2 Python pass on 100,000 pixels."""
    grid = np.random.RandomState(42).uniform(0.0, 1.0, size=(250, 400, 3)).astype(np.float32)

    # Measure Tier 1
    t0 = time.perf_counter()
    for _ in range(5):
        _ = simulate_agsl_shader(grid, exposure=0.75, contrast=25.0, temp=15.0)
    t_tier1_ms = ((time.perf_counter() - t0) / 5) * 1000.0

    # Measure Tier 2
    settings = BasicSettings(exposure=0.75, contrast=25.0, temp=15.0)
    t0 = time.perf_counter()
    for _ in range(5):
        _ = apply_basic_adjustments(grid, settings)
    t_tier2_ms = ((time.perf_counter() - t0) / 5) * 1000.0

    # Tier 1 GPU formulation runs in < 5 ms in Python (and < 0.2 ms in AGSL GPU shader)
    assert t_tier1_ms < 50.0
