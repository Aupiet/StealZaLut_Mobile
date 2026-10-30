"""
Tests for ``stealzalut.core.lut_engine``.

Covers identity-lattice generation, LUT computation from identical images,
intensity-blend semantics, and shape invariants across various lattice sizes.
"""

import numpy as np
import pytest

# Graceful degradation
try:
    from stealzalut.core.lut_engine import (  # type: ignore[import-untyped]
        compute_lut,
        generate_identity_lattice,
        apply_intensity,
    )

    _HAS_LUT_ENGINE = True
except ImportError:
    _HAS_LUT_ENGINE = False

pytestmark = pytest.mark.skipif(
    not _HAS_LUT_ENGINE,
    reason="stealzalut.core.lut_engine is not importable (missing dependency?)",
)


# ------------------------------------------------------------------
# Identity lattice
# ------------------------------------------------------------------


class TestIdentityLattice:
    """The identity lattice must be a well-formed (N³, 3) grid in [0, 1]."""

    def test_identity_lattice_shape(self) -> None:
        size = 17
        lattice = generate_identity_lattice(size)
        expected_shape = (size ** 3, 3)
        assert lattice.shape == expected_shape, (
            f"Expected {expected_shape}, got {lattice.shape}"
        )

    def test_identity_lattice_range(self) -> None:
        lattice = generate_identity_lattice(17)
        assert lattice.min() >= 0.0, f"Min below 0: {lattice.min()}"
        assert lattice.max() <= 1.0, f"Max above 1: {lattice.max()}"

    def test_identity_lattice_corners(self) -> None:
        lattice = generate_identity_lattice(17)
        np.testing.assert_array_almost_equal(
            lattice[0], [0.0, 0.0, 0.0], decimal=6,
            err_msg="First row should be [0, 0, 0]",
        )
        np.testing.assert_array_almost_equal(
            lattice[-1], [1.0, 1.0, 1.0], decimal=6,
            err_msg="Last row should be [1, 1, 1]",
        )


# ------------------------------------------------------------------
# LUT from identical images (should ≈ identity)
# ------------------------------------------------------------------


class TestIdentityLUT:
    """When source == reference the resulting LUT should be near-identity."""

    def test_identity_lut_from_identical_images(
        self, identical_images: tuple[np.ndarray, np.ndarray]
    ) -> None:
        src, ref = identical_images
        lut = compute_lut(src, ref, size=17)

        identity = generate_identity_lattice(17)
        mse = float(np.mean((lut - identity) ** 2))
        assert mse < 1e-3, (
            f"LUT from identical images should be near-identity, but MSE = {mse:.6f}"
        )


# ------------------------------------------------------------------
# Intensity blending
# ------------------------------------------------------------------


class TestApplyIntensity:
    """``apply_intensity`` must interpolate between identity and transformed LUT."""

    @pytest.fixture
    def _lut_pair(self) -> tuple[np.ndarray, np.ndarray]:
        """Return (identity, transformed) LUT pair for blending tests."""
        identity = generate_identity_lattice(17)
        # Simulate a transformed LUT: shift all values slightly
        transformed = np.clip(identity + 0.1, 0.0, 1.0)
        return identity, transformed

    def test_apply_intensity_zero(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=0.0)
        np.testing.assert_array_almost_equal(
            result, identity, decimal=6,
            err_msg="Intensity 0.0 should return the identity lattice",
        )

    def test_apply_intensity_one(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=1.0)
        np.testing.assert_array_almost_equal(
            result, transformed, decimal=6,
            err_msg="Intensity 1.0 should return the transformed LUT",
        )

    def test_apply_intensity_half(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=0.5)
        expected = (identity + transformed) / 2.0
        np.testing.assert_array_almost_equal(
            result, expected, decimal=5,
            err_msg="Intensity 0.5 should return the midpoint",
        )


# ------------------------------------------------------------------
# compute_lut shape for various sizes
# ------------------------------------------------------------------


class TestComputeLutShape:
    """``compute_lut`` must return (size³, 3) for any valid size."""

    @pytest.mark.parametrize("size", [9, 17, 33])
    def test_compute_lut_shape(
        self,
        sample_src_image: np.ndarray,
        sample_ref_image: np.ndarray,
        size: int,
    ) -> None:
        lut = compute_lut(sample_src_image, sample_ref_image, size=size)
        expected = (size ** 3, 3)
        assert lut.shape == expected, (
            f"For size={size}, expected shape {expected}, got {lut.shape}"
        )
