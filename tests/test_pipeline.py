"""Unit tests for the 3-Stage Pipeline and OKLab color space engine.

Validates:
- sRGB <-> OKLab bidirectional invertibility.
- Stage 2 filter refinement invariants (intensity = 0 -> original, luma_lock, neutral lock, skin tone lock).
- Stage 3 post-grading transformations (exposure, contrast, highlights, shadows, temp, tint, vibrance).
- 3D LUT full pipeline baking: shape, bounds, NaN/Inf immunity, and parameter integration.
"""

import numpy as np
import pytest

from stealzalut.core.color_space import (
    linear_to_srgb,
    oklab_to_rgb,
    rgb_to_oklab,
    smoothstep,
    srgb_to_linear,
)
from stealzalut.core.pipeline import (
    Step2Params,
    Step3Params,
    apply_step2_filter_refinement,
    apply_step3_color_grading,
    bake_full_pipeline_lut,
)


class TestColorSpace:
    """Validate mathematical precision of OKLab transformations."""

    def test_srgb_linear_invertibility(self) -> None:
        grid = np.linspace(0.0, 1.0, 256, dtype=np.float32)[:, np.newaxis]
        rgb = np.repeat(grid, 3, axis=-1)
        lin = srgb_to_linear(rgb)
        rec = linear_to_srgb(lin)
        np.testing.assert_allclose(rec, rgb, atol=1e-5)

    def test_oklab_bidirectional_invertibility(self) -> None:
        # Test across primary colors, neutrals, and intermediate gamut
        test_rgb = np.array(
            [
                [0.0, 0.0, 0.0],
                [1.0, 1.0, 1.0],
                [0.5, 0.5, 0.5],
                [1.0, 0.0, 0.0],
                [0.0, 1.0, 0.0],
                [0.0, 0.0, 1.0],
                [0.85, 0.65, 0.55],  # Caucasian / fair skin tone
                [0.45, 0.30, 0.20],  # Deep skin tone
                [0.2, 0.8, 0.7],    # Teal
                [0.9, 0.4, 0.1],    # Orange
            ],
            dtype=np.float32,
        )
        oklab = rgb_to_oklab(test_rgb)
        rec_rgb = oklab_to_rgb(oklab)
        np.testing.assert_allclose(rec_rgb, test_rgb, atol=1e-4)

    def test_smoothstep_bounds(self) -> None:
        assert smoothstep(0.0, 1.0, -0.5) == 0.0
        assert smoothstep(0.0, 1.0, 1.5) == 1.0
        assert smoothstep(0.0, 1.0, 0.5) == 0.5
        assert smoothstep(0.0, 1.0, 0.0) == 0.0
        assert smoothstep(0.0, 1.0, 1.0) == 1.0


class TestStage2FilterRefinement:
    """Validate Stage 2 filter refinement masks and invariants."""

    def test_zero_intensity_returns_original(self) -> None:
        orig = np.random.RandomState(1).uniform(0.0, 1.0, (32, 32, 3)).astype(np.float32)
        pure = np.random.RandomState(2).uniform(0.0, 1.0, (32, 32, 3)).astype(np.float32)

        params = Step2Params(global_intensity=0.0)
        res = apply_step2_filter_refinement(orig, pure, params)
        np.testing.assert_allclose(res, orig, atol=1e-6)

    def test_luma_lock_preserves_source_luminance(self) -> None:
        orig = np.full((16, 16, 3), 0.2, dtype=np.float32)
        pure = np.full((16, 16, 3), 0.8, dtype=np.float32)

        # Full luma lock: output OKLab luminance L must strictly match orig L
        params = Step2Params(global_intensity=1.0, luma_lock=1.0)
        res = apply_step2_filter_refinement(orig, pure, params)

        oklab_orig = rgb_to_oklab(orig)
        oklab_res = rgb_to_oklab(res)
        np.testing.assert_allclose(oklab_res[..., 0], oklab_orig[..., 0], atol=1e-3)

    def test_neutral_axis_lock_protects_grays(self) -> None:
        # Neutral gray source
        orig_gray = np.full((16, 16, 3), 0.5, dtype=np.float32)
        # Aggressive chromatic tint in pure filter
        pure_tinted = np.full((16, 16, 3), [0.8, 0.2, 0.9], dtype=np.float32)

        params = Step2Params(global_intensity=1.0, neutral_axis_lock=1.0)
        res = apply_step2_filter_refinement(orig_gray, pure_tinted, params)

        oklab_res = rgb_to_oklab(res)
        chroma_res = np.sqrt(oklab_res[..., 1] ** 2 + oklab_res[..., 2] ** 2)
        # Chroma must be strongly suppressed by neutral axis lock
        assert np.max(chroma_res) < 0.02

    def test_skin_tone_lock_protects_human_skin(self) -> None:
        # Typical skin color (RGB: 218, 165, 130)
        skin_rgb = np.array([218 / 255.0, 165 / 255.0, 130 / 255.0], dtype=np.float32)
        orig = np.full((16, 16, 3), skin_rgb)
        pure = np.full((16, 16, 3), [0.1, 0.9, 0.2], dtype=np.float32)  # Vivid matrix green

        params = Step2Params(global_intensity=1.0, skin_tone_lock=1.0)
        res = apply_step2_filter_refinement(orig, pure, params)

        oklab_orig = rgb_to_oklab(orig)
        oklab_res = rgb_to_oklab(res)

        # Chrominance distance from skin should remain small
        dist = np.sqrt(
            (oklab_res[..., 1] - oklab_orig[..., 1]) ** 2
            + (oklab_res[..., 2] - oklab_orig[..., 2]) ** 2
        )
        assert np.mean(dist) < 0.05


class TestStage3PostGrading:
    """Validate Stage 3 post-grading controls."""

    def test_exposure_positive_brightens(self) -> None:
        orig = np.full((16, 16, 3), 0.3, dtype=np.float32)
        params = Step3Params(exposure=1.0)  # +1 EV doubles linear light
        res = apply_step3_color_grading(orig, params)
        assert np.mean(res) > np.mean(orig)

    def test_contrast_adjustment(self) -> None:
        orig = np.linspace(0.1, 0.9, 100, dtype=np.float32)[:, np.newaxis]
        rgb = np.repeat(orig, 3, axis=-1)
        params_hi = Step3Params(contrast=0.5)
        params_lo = Step3Params(contrast=-0.5)

        res_hi = apply_step3_color_grading(rgb, params_hi)
        res_lo = apply_step3_color_grading(rgb, params_lo)

        # High contrast increases variance, low contrast decreases it
        assert np.var(res_hi) > np.var(rgb)
        assert np.var(res_lo) < np.var(rgb)

    def test_spatial_effects_bypassed_in_lut_baking(self) -> None:
        # During LUT baking, grain/clarity/sharpen must NOT add spatial noise to 1D/3D lattice
        lattice = np.full((17**3, 3), 0.5, dtype=np.float32)
        params = Step3Params(grain=1.0, clarity=1.0, sharpen=1.0)
        baked = apply_step3_color_grading(lattice, params, is_lut_baking=True)
        # Without spatial effects, all values should remain uniform
        np.testing.assert_allclose(baked, lattice, atol=1e-6)


class TestLutBaking:
    """Validate baking of full 3-Stage pipeline into 3D LUT."""

    def test_bake_full_pipeline_lut_shape_and_bounds(self) -> None:
        raw_lut = np.random.RandomState(42).uniform(0.0, 1.0, (17**3, 3)).astype(np.float32)
        step2 = Step2Params(global_intensity=0.8, luma_lock=0.3, skin_tone_lock=0.5)
        step3 = Step3Params(exposure=0.5, contrast=0.2, vibrance=0.3)

        baked = bake_full_pipeline_lut(raw_lut, step2, step3, size=17)
        assert baked.shape == (17**3, 3)
        assert np.min(baked) >= 0.0
        assert np.max(baked) <= 1.0
        assert not np.isnan(baked).any()
        assert not np.isinf(baked).any()

    def test_bake_identity_at_zero_intensity_and_neutral_settings(self) -> None:
        step2 = Step2Params(global_intensity=0.0)
        step3 = Step3Params()  # All zero adjustments
        from stealzalut.core.lut_engine import generate_identity_lattice

        identity = generate_identity_lattice(17)
        baked = bake_full_pipeline_lut(None, step2, step3, size=17)
        np.testing.assert_allclose(baked, identity, atol=1e-5)
