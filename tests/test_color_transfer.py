"""Tests for ``stealzalut.core.color_transfer``.

Validates output shape, dtype, value range, parametric method coverage,
error handling for invalid methods, differently-sized inputs,
CIE LAB luminance preservation, and smooth soft-clipping roll-off.
"""

import numpy as np
import pytest

try:
    from stealzalut.core.color_transfer import (  # type: ignore[import-untyped]
        _numpy_statistical_transfer,
        apply_soft_clip,
        compute_color_transfer as color_transfer,
        lab_to_rgb,
        rgb_to_lab,
    )

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
        assert result.dtype == np.uint8, (
            f"Expected dtype uint8, got {result.dtype}"
        )

    def test_output_values_in_range(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        result = color_transfer(sample_src_image, sample_ref_image)
        assert result.min() >= 0, f"Found value below 0: {result.min()}"
        assert result.max() <= 255, f"Found value above 255: {result.max()}"


# ------------------------------------------------------------------
# Parametric method tests
# ------------------------------------------------------------------

ALL_METHODS = ["hm-mkl-hm", "mkl", "hm", "reinhard"]


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


class TestStatisticalTransferFallback:
    """Validate pure numpy Reinhard statistical transfer in CIE LAB space."""

    def test_statistical_fallback_execution(
        self, sample_src_image: np.ndarray, sample_ref_image: np.ndarray
    ) -> None:
        result = _numpy_statistical_transfer(
            sample_src_image, sample_ref_image, preserve_luminance=0.8, soft_clip=True
        )
        assert result.shape == sample_src_image.shape
        assert result.dtype == np.uint8
        assert 0 <= result.min() and result.max() <= 255
