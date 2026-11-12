"""Tests for ``stealzalut.core.color_transfer``.

Validates output shape, dtype, value range, parametric method coverage,
error handling for invalid methods, differently-sized inputs,
CIE LAB luminance preservation, and smooth soft-clipping roll-off.
"""

import numpy as np
import pytest

try:
    from stealzalut.core.color_space import gamut_compress, oklab_to_rgb, rgb_to_oklab
    from stealzalut.core.color_transfer import (  # type: ignore[import-untyped]
        apply_soft_clip,
        compute_color_transfer as color_transfer,
        rgb_to_lab,
    )
    from stealzalut.core.regularize import fold_fraction, smooth_field
    from stealzalut.utils.image_io import resolve_export_dimensions

    _HAS_COLOR_TRANSFER = True
except ImportError:
    _HAS_COLOR_TRANSFER = False

pytestmark = pytest.mark.skipif(
    not _HAS_COLOR_TRANSFER,
    reason="stealzalut.core.color_transfer is not importable (missing dependency?)",
)


# ------------------------------------------------------------------
# Basic contract tests
# ------------------------------------------------------------------


class TestColorTransferContract:
    """Output must honour the same-shape / uint8 / [0,255] contract."""

    def test_output_shape_matches_source(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        result = color_transfer(sample_src_image, sample_ref_image)
        assert result.shape == sample_src_image.shape, (
            f"Expected shape {sample_src_image.shape}, got {result.shape}"
        )

    def test_output_dtype_uint8(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        result = color_transfer(sample_src_image, sample_ref_image)
        assert result.dtype == np.uint8, f"Expected dtype uint8, got {result.dtype}"

    def test_output_values_in_range(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        result = color_transfer(sample_src_image, sample_ref_image)
        assert result.min() >= 0, f"Found value below 0: {result.min()}"
        assert result.max() <= 255, f"Found value above 255: {result.max()}"


# ------------------------------------------------------------------
# Parametric method tests
# ------------------------------------------------------------------

ALL_METHODS = ["hm-mkl-hm", "mkl", "hm", "reinhard", "oklab"]


class TestColorTransferMethods:
    """Every supported method must produce valid output."""

    @pytest.mark.parametrize("method", ALL_METHODS)
    def test_all_methods(
        self,
        sample_src_image: np.ndarray,
        sample_ref_image: np.ndarray,
        method: str,
    ) -> None:
        result = color_transfer(sample_src_image, sample_ref_image, method=method)
        assert result.shape == sample_src_image.shape
        assert result.dtype == np.uint8
        assert 0 <= result.min() and result.max() <= 255


# ------------------------------------------------------------------
# Error handling
# ------------------------------------------------------------------


class TestColorTransferErrors:
    """Invalid inputs must raise clear exceptions."""

    def test_invalid_method_raises(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        with pytest.raises(ValueError):
            color_transfer(
                sample_src_image, sample_ref_image, method="nonexistent-method"
            )


# ------------------------------------------------------------------
# Edge-case: different sizes
# ------------------------------------------------------------------


class TestColorTransferSizes:
    """Differently-sized source and reference should still work."""

    def test_different_sized_inputs(self, sample_ref_image: np.ndarray) -> None:
        rng = np.random.RandomState(123)
        small_src = rng.randint(0, 256, (64, 80, 3), dtype=np.uint8)

        result = color_transfer(small_src, sample_ref_image)
        assert result.shape == small_src.shape, (
            f"Output shape {result.shape} != source shape {small_src.shape}"
        )
        assert result.dtype == np.uint8


# ------------------------------------------------------------------
# Luminance Preservation (CIE LAB)
# ------------------------------------------------------------------


class TestPreserveLuminance:
    """Validate that preserve_luminance = 1.0 preserves source L while transferring A & B."""

    def test_preserve_luminance_one_keeps_source_l(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        # Transfer with preserve_luminance = 1.0 (100% source lightness)
        result = color_transfer(
            sample_src_image,
            sample_ref_image,
            preserve_luminance=1.0,
            soft_clip=False,
        )

        src_f = sample_src_image.astype(np.float32) / 255.0
        res_f = result.astype(np.float32) / 255.0

        src_lab = rgb_to_lab(src_f)
        res_lab = rgb_to_lab(res_f)

        # L channel of result must closely match source L (within 8-bit quantization tolerance < 1.5 L units)
        diff_l = np.mean(np.abs(src_lab[..., 0] - res_lab[..., 0]))
        assert diff_l < 1.5, f"Luminance L was not preserved: mean diff = {diff_l:.3f}"

        # Chrominance (A or B) must be significantly altered by the reference image
        diff_ab = np.mean(np.abs(src_lab[..., 1:] - res_lab[..., 1:]))
        assert diff_ab > 5.0, (
            f"Chrominance A/B did not receive the reference look: mean diff = {diff_ab:.3f}"
        )

    def test_preserve_luminance_zero_transfers_luminance(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        # Transfer with preserve_luminance = 0.0 (full reference lightness)
        res_0 = color_transfer(
            sample_src_image,
            sample_ref_image,
            preserve_luminance=0.0,
            soft_clip=False,
        )
        res_1 = color_transfer(
            sample_src_image,
            sample_ref_image,
            preserve_luminance=1.0,
            soft_clip=False,
        )

        # Output with preserve_luminance = 0.0 must differ from preserve_luminance = 1.0
        assert not np.array_equal(res_0, res_1)


# ------------------------------------------------------------------
# Soft-clipping roll-off
# ------------------------------------------------------------------


class TestSoftClipping:
    """Validate smooth C1 tanh compression on extreme highlights and shadows."""

    def test_soft_clip_linear_in_midtones(self) -> None:
        midtones = np.array([0.1, 0.25, 0.5, 0.75, 0.9], dtype=np.float32)
        clipped = apply_soft_clip(midtones, shadow_th=0.05, highlight_th=0.95)
        np.testing.assert_allclose(clipped, midtones, atol=1e-6)

    def test_soft_clip_smooth_extremes(self) -> None:
        extremes = np.array([-0.2, 0.0, 0.02, 0.98, 1.0, 1.3], dtype=np.float32)
        clipped = apply_soft_clip(extremes, shadow_th=0.05, highlight_th=0.95)

        # All values strictly within [0.0, 1.0]
        assert np.all(clipped >= 0.0)
        assert np.all(clipped <= 1.0)

        # Monotonically increasing
        assert np.all(np.diff(clipped) > 0)

        # Shadow roll-off: 0.0 maps above 0.0 (smooth curve)
        assert clipped[1] > 0.0
        # Highlight roll-off: 1.0 maps below 1.0 (smooth curve)
        assert clipped[4] < 1.0


# ------------------------------------------------------------------
# Fallback statistical transfer
# ------------------------------------------------------------------


class TestNativeExportResolution:
    """Validate that Native mode preserves exact dimensions and never upscales."""

    def test_native_mode_preserves_exact_dimensions(self) -> None:
        # 1200x800 image
        w, h = resolve_export_dimensions(orig_w=1200, orig_h=800, target_max_dim=0)
        assert (w, h) == (1200, 800), f"Expected (1200, 800), got ({w}, {h})"

        # None also indicates native
        w2, h2 = resolve_export_dimensions(orig_w=1200, orig_h=800, target_max_dim=None)
        assert (w2, h2) == (1200, 800)

    def test_never_upscales_smaller_image(self) -> None:
        # Source 1080p (1920x1080) with 4K target (3840) must NOT upscale
        w, h = resolve_export_dimensions(orig_w=1920, orig_h=1080, target_max_dim=3840)
        assert (w, h) == (1920, 1080), f"Expected no upscaling, got ({w}, {h})"

    def test_downsamples_larger_image(self) -> None:
        # Source 6000x4000 with 2048 target must downsample proportionally
        w, h = resolve_export_dimensions(orig_w=6000, orig_h=4000, target_max_dim=2048)
        assert max(w, h) == 2048
        assert abs(w / h - 1.5) < 0.01


# ------------------------------------------------------------------
# Reinhard / Perceptual CIE LAB Resilience Tests
# ------------------------------------------------------------------


class TestReinhardResilience:
    """Validate that Reinhard / Perceptual CIE LAB never crashes on edge cases."""

    def test_reinhard_uniform_black_source(self) -> None:
        src = np.zeros((32, 32, 3), dtype=np.uint8)
        ref = np.ones((32, 32, 3), dtype=np.uint8) * 128
        out = color_transfer(src, ref, method="reinhard")
        assert out.shape == src.shape
        assert out.dtype == np.uint8
        assert not np.isnan(out).any()

    def test_reinhard_uniform_black_reference(self) -> None:
        src = np.ones((32, 32, 3), dtype=np.uint8) * 200
        ref = np.zeros((32, 32, 3), dtype=np.uint8)
        out = color_transfer(src, ref, method="reinhard")
        assert out.shape == src.shape
        assert out.dtype == np.uint8
        assert not np.isnan(out).any()

    def test_reinhard_identity_lattice_input(self) -> None:
        # Simulate pseudo_img used in compute_lut
        identity_lattice = np.linspace(0, 255, 33 * 33 * 33, dtype=np.uint8).reshape(
            -1, 1, 3
        )
        ref = (np.random.rand(64, 64, 3) * 255).astype(np.uint8)
        out = color_transfer(identity_lattice, ref, method="reinhard")
        assert out.shape == identity_lattice.shape
        assert out.dtype == np.uint8
        assert not np.isnan(out).any()


class TestPhaseZeroFoundations:
    """Validate the new desktop-parity foundations added for the zones port."""

    def test_oklab_method_is_supported_and_valid(self, sample_src_image, sample_ref_image):
        out = color_transfer(sample_src_image, sample_ref_image, method="oklab")
        assert out.shape == sample_src_image.shape
        assert out.dtype == np.uint8
        assert 0 <= out.min() <= out.max() <= 255

    def test_gamut_compress_keeps_output_in_srgb_gamut(self):
        lab = np.array(
            [[0.75, 0.45, 0.40], [0.5, 0.10, -0.10], [0.25, -0.25, 0.30]],
            dtype=np.float32,
        )
        compressed = gamut_compress(lab)
        rgb = oklab_to_rgb(compressed)
        assert np.all(rgb >= 0.0)
        assert np.all(rgb <= 1.0)
        assert np.all(np.isfinite(rgb))

    def test_smooth_field_is_stable_and_fold_fraction_respects_monotonicity(self):
        field = np.ones((27, 3), dtype=np.float64)
        smoothed = smooth_field(field, 3, 0.0)
        np.testing.assert_allclose(smoothed, field)

        monotone = np.linspace(0.1, 0.9, 27, dtype=np.float64)
        assert fold_fraction(monotone, 3) == 0.0


class TestZonePortFoundations:
    """Validate the zone-analysis pipeline introduced in the next porting phase."""

    def test_zone_analysis_returns_consistent_palette(self):
        from stealzalut.core.palette import analyze_zones

        src = np.zeros((80, 120, 3), dtype=np.uint8)
        src[:40, :60] = (40, 60, 90)
        src[:40, 60:] = (180, 150, 120)
        src[40:, :60] = (210, 185, 150)
        src[40:, 60:] = (245, 240, 235)

        palette = analyze_zones(src, zone_count=4)
        assert len(palette.zones) >= 2
        assert all(z.area > 0.0 for z in palette.zones)
        assert palette.labels.shape == src.shape[:2]

    def test_zone_matching_produces_valid_mapping(self):
        from stealzalut.core.matching import match_zones
        from stealzalut.core.palette import analyze_zones

        src = np.array([[[30, 30, 30]], [[200, 180, 150]]], dtype=np.uint8)
        ref = np.array([[[80, 120, 140]], [[240, 220, 200]]], dtype=np.uint8)

        src_palette = analyze_zones(src.reshape(2, 1, 3), zone_count=2)
        ref_palette = analyze_zones(ref.reshape(2, 1, 3), zone_count=2)
        mapping = match_zones(src_palette, ref_palette)

        assert set(mapping) == set(src_palette.ids)
        for src_id, targets in mapping.items():
            assert np.isclose(sum(targets.values()), 1.0)
            assert set(targets).issubset(set(ref_palette.ids))

    def test_zone_transfer_model_fits_on_sample_images(self):
        from stealzalut.core.zone_transfer import fit_zone_transfer

        src = np.zeros((80, 120, 3), dtype=np.uint8)
        src[:40, :60] = (20, 50, 70)
        src[:40, 60:] = (120, 180, 220)
        src[40:, :60] = (180, 160, 130)
        src[40:, 60:] = (240, 235, 230)

        model = fit_zone_transfer(src, src, zone_count=4)
        assert model.method == "zones"
        assert len(model.source.zones) >= 2
        transferred = model.apply(src.reshape(-1, 3).astype(np.float64) / 255.0)
        assert transferred.shape == (src.size // 3, 3)
        assert np.all(np.isfinite(transferred))

    def test_zone_bridge_api_exposes_palette_and_matching(self, tmp_path):
        from PIL import Image

        from stealzalut.bridge import (
            analyze_zone_palette_bridge,
            fit_zone_transfer_bridge,
            match_zone_palette_bridge,
        )

        src = np.zeros((32, 32, 3), dtype=np.uint8)
        src[:16, :] = (60, 70, 80)
        src[16:, :] = (200, 190, 180)
        ref = np.zeros_like(src)
        ref[:, :16] = (100, 140, 160)
        ref[:, 16:] = (240, 230, 220)

        src_path = tmp_path / "source.png"
        ref_path = tmp_path / "reference.png"
        Image.fromarray(src).save(src_path)
        Image.fromarray(ref).save(ref_path)

        palette = analyze_zone_palette_bridge(str(src_path), zone_count=4)
        assert palette["status"] == "success"
        assert palette["zone_count"] >= 2

        mapping = match_zone_palette_bridge(str(src_path), str(ref_path), zone_count=4)
        assert mapping["status"] == "success"
        assert len(mapping["matching"]) >= 2

        model = fit_zone_transfer_bridge(str(src_path), str(ref_path), zone_count=4)
        assert model["status"] == "success"
        assert model["method"] == "zones"
        assert len(model["correspondence"]) >= 2
