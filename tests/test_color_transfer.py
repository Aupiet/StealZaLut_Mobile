"""
Tests for ``stealzalut.core.color_transfer``.

Validates output shape, dtype, value range, parametric method coverage,
error handling for invalid methods, and handling of differently-sized inputs.
"""

import numpy as np
import pytest

# Graceful degradation: skip the whole module when stealzalut isn't importable
# (e.g. color-matcher or the package itself is missing).
try:
    from stealzalut.core.color_transfer import compute_color_transfer as color_transfer  # type: ignore[import-untyped]

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
        # Source is 64×80, reference is the standard 100×100
        rng = np.random.RandomState(123)
        small_src = rng.randint(0, 256, (64, 80, 3), dtype=np.uint8)

        result = color_transfer(small_src, sample_ref_image)
        assert result.shape == small_src.shape, (
            f"Output shape {result.shape} != source shape {small_src.shape}"
        )
        assert result.dtype == np.uint8
