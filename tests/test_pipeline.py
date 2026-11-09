"""Unit tests for the 3-Stage Pipeline and OKLab color space engine.

Validates:
- sRGB <-> OKLab bidirectional invertibility.
- Stage 2 filter refinement invariants (intensity = 0 -> original, luma_lock, neutral lock, skin tone lock).
- Stage 3 post-grading transformations (exposure, contrast, highlights, shadows, temp, tint, vibrance).
- 3D LUT full pipeline baking: shape, bounds, NaN/Inf immunity, and parameter integration.
"""

import numpy as np

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
                [0.2, 0.8, 0.7],  # Teal
                [0.9, 0.4, 0.1],  # Orange
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
        orig = (
            np.random.RandomState(1).uniform(0.0, 1.0, (32, 32, 3)).astype(np.float32)
        )
        pure = (
            np.random.RandomState(2).uniform(0.0, 1.0, (32, 32, 3)).astype(np.float32)
        )

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
        pure = np.full(
            (16, 16, 3), [0.1, 0.9, 0.2], dtype=np.float32
        )  # Vivid matrix green

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
        raw_lut = (
            np.random.RandomState(42).uniform(0.0, 1.0, (17**3, 3)).astype(np.float32)
        )
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


class TestStudioToEditorRelayParity:
    """Validate WYSIWYG parity (<= 1 8-bit level) when relaying Studio 3D LUT to Editor."""

    def test_relay_parity_within_one_8bit_level(self, tmp_path) -> None:
        from stealzalut.core.lut_engine import apply_lut, generate_identity_lattice
        from stealzalut.core.lut_export import export_cube, load_cube_file

        np.random.seed(42)
        # Create a test synthetic image (e.g. 64x64x3)
        img = np.random.randint(0, 256, (64, 64, 3), dtype=np.uint8)

        # Generate a smooth non-trivial grading LUT (e.g. warm tint + contrast)
        size = 33
        lattice = generate_identity_lattice(size)
        grading_lut = np.clip(
            lattice * np.array([1.1, 1.0, 0.9], dtype=np.float32), 0.0, 1.0
        )

        # 1. Studio preview rendering: directly applies final 3D LUT
        studio_rendered = apply_lut(img, grading_lut)

        # 2. Studio exports LUT to .cube file in cache
        cube_path = str(tmp_path / "studio_grade.cube")
        export_cube(grading_lut, cube_path, size=size, title="Studio Grade")

        # 3. Editor loads custom LUT from .cube and evaluates with neutral sliders
        loaded_size, loaded_lut = load_cube_file(cube_path)
        assert loaded_size == size
        editor_rendered = apply_lut(img, loaded_lut)

        # 4. Strict WYSIWYG rule: delta <= 1 8-bit level
        diff = np.abs(
            studio_rendered.astype(np.int16) - editor_rendered.astype(np.int16)
        )
        max_diff = np.max(diff)
        assert max_diff <= 1, (
            f"Studio to Editor relay exceeded WYSIWYG tolerance: max diff = {max_diff}"
        )


class TestLightroomPanelsPipeline:
    """Validate reproduction of Adobe Lightroom Classic develop panels."""

    def test_basic_adjustments_and_bypass(self) -> None:
        from stealzalut.core.lightroom_models import (
            LightroomDevelopSettings,
            BasicSettings,
        )
        from stealzalut.core.pipeline import apply_step3_color_grading

        img = np.full((32, 32, 3), 0.5, dtype=np.float32)
        settings = LightroomDevelopSettings(
            basic=BasicSettings(exposure=1.0, contrast=20.0, temp=15.0),
            is_basic_enabled=True,
        )

        # Active
        res_active = apply_step3_color_grading(img, Step3Params(), lr_settings=settings)
        assert not np.allclose(res_active, img)
        assert np.mean(res_active) > np.mean(img)

        # Bypass (Eye icon toggle = False)
        settings.is_basic_enabled = False
        res_bypass = apply_step3_color_grading(img, Step3Params(), lr_settings=settings)
        np.testing.assert_allclose(res_bypass, img, atol=1e-5)

    def test_tone_curve_parametric_and_point(self) -> None:
        from stealzalut.core.lightroom_models import ToneCurveSettings
        from stealzalut.core.lightroom_color import apply_tone_curve

        img = np.linspace(0.0, 1.0, 100, dtype=np.float32)[:, np.newaxis]
        rgb = np.repeat(img, 3, axis=-1)

        # Parametric S-curve: highlights up, shadows down
        param_settings = ToneCurveSettings(
            mode="parametric", highlights=50.0, shadows=-50.0
        )
        res_param = apply_tone_curve(rgb, param_settings)
        assert res_param[80, 0] > rgb[80, 0]  # highlights boosted
        assert res_param[20, 0] < rgb[20, 0]  # shadows depressed

        # Point curve (custom points)
        pt_settings = ToneCurveSettings(
            mode="point",
            rgb_points=[(0.0, 0.0), (0.5, 0.7), (1.0, 1.0)],
        )
        res_pt = apply_tone_curve(rgb, pt_settings)
        assert res_pt[50, 0] > rgb[50, 0]

    def test_color_mixer_hsl_and_point_color(self) -> None:
        from stealzalut.core.lightroom_models import ColorMixerSettings
        from stealzalut.core.lightroom_color import apply_color_mixer

        # Pure green patch (H = 120)
        green_img = np.full((16, 16, 3), [0.0, 1.0, 0.0], dtype=np.float32)
        # Shift green hue and saturation
        hsl_settings = ColorMixerSettings(mode="hsl", hue_green=-50.0, sat_green=-30.0)
        res_green = apply_color_mixer(green_img, hsl_settings)
        assert (
            res_green[0, 0, 0] > 0.0 or res_green[0, 0, 2] > 0.0
        )  # Hue shifted away from pure green

        # Point Color
        pt_settings = ColorMixerSettings(
            mode="point_color",
            point_active=True,
            point_target_r=0.0,
            point_target_g=1.0,
            point_target_b=0.0,
            point_hue_shift=50.0,
            point_range=50.0,
        )
        res_pt = apply_color_mixer(green_img, pt_settings)
        assert not np.allclose(res_pt, green_img)

    def test_color_grading_wheels_and_balance(self) -> None:
        from stealzalut.core.lightroom_models import ColorGradingSettings
        from stealzalut.core.lightroom_color import apply_color_grading

        # Dark shadow patch and bright highlight patch
        dark_img = np.full((16, 16, 3), 0.15, dtype=np.float32)
        bright_img = np.full((16, 16, 3), 0.85, dtype=np.float32)

        # Teal shadows (H = 180), Orange highlights (H = 30)
        grading = ColorGradingSettings(
            shadow_hue=180.0,
            shadow_sat=60.0,
            highlight_hue=30.0,
            highlight_sat=60.0,
            blending=50.0,
            balance=0.0,
        )
        res_dark = apply_color_grading(dark_img, grading)
        res_bright = apply_color_grading(bright_img, grading)

        # Dark patch should gain cyan/teal (higher B & G than R)
        assert res_dark[0, 0, 2] > res_dark[0, 0, 0]
        # Bright patch should gain warm orange (higher R than B)
        assert res_bright[0, 0, 0] > res_bright[0, 0, 2]

    def test_calibration_primaries(self) -> None:
        from stealzalut.core.lightroom_models import CalibrationSettings
        from stealzalut.core.lightroom_color import apply_calibration

        img = np.full((16, 16, 3), [0.8, 0.4, 0.2], dtype=np.float32)
        calib = CalibrationSettings(red_hue=25.0, red_sat=30.0, shadow_tint=-15.0)
        res = apply_calibration(img, calib)
        assert not np.allclose(res, img)
        assert np.min(res) >= 0.0 and np.max(res) <= 1.0

    def test_spatial_operations_detail_lens_blur_effects(self) -> None:
        from stealzalut.core.lightroom_models import (
            DetailSettings,
            EffectsSettings,
            LensBlurSettings,
            LensCorrectionsSettings,
            TransformSettings,
        )
        from stealzalut.core.lightroom_spatial import (
            apply_detail_spatial,
            apply_effects_spatial,
            apply_lens_blur_spatial,
            apply_lens_corrections_spatial,
            apply_transform_spatial,
        )

        img = (
            np.random.RandomState(42).uniform(0.1, 0.9, (64, 64, 3)).astype(np.float32)
        )

        # Detail
        res_detail = apply_detail_spatial(
            img, DetailSettings(sharpen_amount=60.0, sharpen_masking=20.0)
        )
        assert res_detail.shape == img.shape
        assert not np.isnan(res_detail).any()

        # Lens Corrections
        res_lens = apply_lens_corrections_spatial(
            img, LensCorrectionsSettings(distortion=15.0, vignette_amount=-30.0)
        )
        assert res_lens.shape == img.shape

        # Transform
        res_trans = apply_transform_spatial(
            img, TransformSettings(rotate=5.0, scale=105.0)
        )
        assert res_trans.shape == img.shape

        # Lens Blur
        res_blur = apply_lens_blur_spatial(
            img, LensBlurSettings(enabled=True, blur_amount=25.0, bokeh_style="bubble")
        )
        assert res_blur.shape == img.shape

        # Effects
        res_fx = apply_effects_spatial(
            img, EffectsSettings(vignette_amount=-40.0, grain_amount=20.0)
        )
        assert res_fx.shape == img.shape
        assert np.min(res_fx) >= 0.0 and np.max(res_fx) <= 1.0

    def test_full_baking_with_lightroom_develop_settings(self) -> None:
        from stealzalut.core.lightroom_models import (
            LightroomDevelopSettings,
            BasicSettings,
            ColorGradingSettings,
        )
        from stealzalut.core.pipeline import bake_full_pipeline_lut

        settings = LightroomDevelopSettings(
            basic=BasicSettings(exposure=0.5, contrast=15.0, temp=10.0),
            color_grading=ColorGradingSettings(shadow_hue=210.0, shadow_sat=30.0),
        )

        baked = bake_full_pipeline_lut(
            None, Step2Params(), Step3Params(), size=17, lr_settings=settings
        )
        assert baked.shape == (17**3, 3)
        assert np.min(baked) >= 0.0
        assert np.max(baked) <= 1.0
        assert not np.isnan(baked).any()
        assert not np.isinf(baked).any()
