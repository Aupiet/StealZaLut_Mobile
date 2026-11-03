"""Tests for ``stealzalut.core.lut_engine``.

Covers identity-lattice generation, LUT computation from identical images,
intensity-blend semantics, 3D LUT refinement (skin tone protection,
neutral lock, 3D Gaussian smoothing), and vectorized trilinear application.
"""

import numpy as np
import pytest

try:
    from stealzalut.core.lut_engine import (  # type: ignore[import-untyped]
        apply_intensity,
        apply_lut,
        apply_lut_trilinear,
        compute_lut,
        generate_identity_grid,
        generate_identity_lattice,
        refine_and_blend_lut,
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
        expected_shape = (size**3, 3)
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
            lattice[0],
            [0.0, 0.0, 0.0],
            decimal=6,
            err_msg="First row should be [0, 0, 0]",
        )
        np.testing.assert_array_almost_equal(
            lattice[-1],
            [1.0, 1.0, 1.0],
            decimal=6,
            err_msg="Last row should be [1, 1, 1]",
        )

    def test_identity_grid_structure(self) -> None:
        grid = generate_identity_grid(17)
        assert grid.shape == (17, 17, 17, 3)
        # Flattened grid must match generate_identity_lattice exactly
        lattice = generate_identity_lattice(17)
        np.testing.assert_array_almost_equal(grid.reshape(-1, 3), lattice, decimal=6)


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
# Legacy intensity blending function
# ------------------------------------------------------------------


class TestApplyIntensity:
    """``apply_intensity`` must interpolate between identity and transformed LUT."""

    @pytest.fixture
    def _lut_pair(self) -> tuple[np.ndarray, np.ndarray]:
        identity = generate_identity_lattice(17)
        transformed = np.clip(identity + 0.1, 0.0, 1.0)
        return identity, transformed

    def test_apply_intensity_zero(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=0.0)
        np.testing.assert_array_almost_equal(
            result,
            identity,
            decimal=6,
            err_msg="Intensity 0.0 should return the identity lattice",
        )

    def test_apply_intensity_one(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=1.0)
        np.testing.assert_array_almost_equal(
            result,
            transformed,
            decimal=6,
            err_msg="Intensity 1.0 should return the transformed LUT",
        )

    def test_apply_intensity_half(
        self, _lut_pair: tuple[np.ndarray, np.ndarray]
    ) -> None:
        identity, transformed = _lut_pair
        result = apply_intensity(identity, transformed, intensity=0.5)
        expected = (identity + transformed) / 2.0
        np.testing.assert_array_almost_equal(
            result,
            expected,
            decimal=5,
            err_msg="Intensity 0.5 should return the midpoint",
        )


# ------------------------------------------------------------------
# Refine and Blend 3D LUT (Professional colorimetric engine)
# ------------------------------------------------------------------


class TestRefineAndBlendLut:
    """Validate 3D LUT refinement: identity return at intensity 0, skin preservation, neutral lock."""

    @pytest.fixture
    def sample_raw_lut(self) -> np.ndarray:
        identity = generate_identity_lattice(17)
        # Artificial aggressive green / cold color grading cast
        cast = np.zeros_like(identity)
        cast[:, 1] += 0.25  # Heavy green shift
        cast[:, 0] -= 0.15  # Heavy red reduction
        return np.clip(identity + cast, 0.0, 1.0)

    def test_intensity_zero_returns_exact_identity(
        self, sample_raw_lut: np.ndarray
    ) -> None:
        """With intensity = 0.0, refine_and_blend_lut must return exact identity (diff < 1e-6)."""
        identity = generate_identity_lattice(17)
        result = refine_and_blend_lut(
            raw_lut=sample_raw_lut,
            intensity=0.0,
            skin_protection=0.8,
            smooth_sigma=0.6,
            neutral_lock=0.9,
        )
        max_diff = float(np.max(np.abs(result - identity)))
        assert max_diff < 1e-6, (
            f"Expected identity LUT at intensity 0.0, got max diff = {max_diff:.2e}"
        )

    def test_intensity_half_linear_midpoint(
        self, sample_raw_lut: np.ndarray
    ) -> None:
        """With intensity = 0.5 and no non-linear modifiers, result must be exact midpoint."""
        identity = generate_identity_lattice(17)
        result = refine_and_blend_lut(
            raw_lut=sample_raw_lut,
            intensity=0.5,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )
        expected = (identity + sample_raw_lut) / 2.0
        np.testing.assert_allclose(result, expected, atol=1e-5)

    def test_skin_protection_preserves_skin_tones(self) -> None:
        """Skin protection must pull skin tone coordinates closer to identity."""
        size = 17
        identity_grid = generate_identity_grid(size)
        # Aggressive blue shift that ruins skin
        distorted_grid = identity_grid.copy()
        distorted_grid[..., 0] = np.clip(distorted_grid[..., 0] - 0.4, 0.0, 1.0)
        distorted_grid[..., 2] = np.clip(distorted_grid[..., 2] + 0.4, 0.0, 1.0)

        # Refine without skin protection
        unprotected = refine_and_blend_lut(
            raw_lut=distorted_grid,
            intensity=1.0,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )

        # Refine with strong skin protection
        protected = refine_and_blend_lut(
            raw_lut=distorted_grid,
            intensity=1.0,
            skin_protection=1.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )

        # Locate typical skin point: R=0.8, G=0.6, B=0.4 in grid
        r_idx = int(round(0.8 * (size - 1)))
        g_idx = int(round(0.6 * (size - 1)))
        b_idx = int(round(0.4 * (size - 1)))

        ident_skin = identity_grid[b_idx, g_idx, r_idx]
        unprot_skin = unprotected[b_idx, g_idx, r_idx]
        prot_skin = protected[b_idx, g_idx, r_idx]

        dist_unprotected = np.linalg.norm(unprot_skin - ident_skin)
        dist_protected = np.linalg.norm(prot_skin - ident_skin)

        assert dist_protected < dist_unprotected * 0.5, (
            f"Skin tone was not protected: dist_protected ({dist_protected:.3f}) "
            f"should be much smaller than dist_unprotected ({dist_unprotected:.3f})"
        )

    def test_neutral_lock_protects_blacks_and_whites(self) -> None:
        """Neutral lock must keep (0,0,0) and (1,1,1) neutral."""
        size = 17
        identity_grid = generate_identity_grid(size)
        # Cast that tints deep blacks red and whites cyan
        tinted = identity_grid.copy()
        tinted += 0.2
        tinted = np.clip(tinted, 0.0, 1.0)

        locked = refine_and_blend_lut(
            raw_lut=tinted,
            intensity=1.0,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=1.0,
        )

        # Pure black corner [0, 0, 0] must remain at [0, 0, 0]
        np.testing.assert_allclose(locked[0, 0, 0], [0.0, 0.0, 0.0], atol=1e-5)
        # Pure white corner [-1, -1, -1] must remain at [1, 1, 1]
        np.testing.assert_allclose(locked[-1, -1, -1], [1.0, 1.0, 1.0], atol=1e-5)

    def test_3d_smoothing_reduces_noise(self) -> None:
        """3D Gaussian smoothing must reduce localized high-frequency grid noise."""
        identity = generate_identity_lattice(17)
        rng = np.random.RandomState(42)
        noisy = np.clip(identity + rng.normal(0, 0.1, identity.shape).astype(np.float32), 0.0, 1.0)

        smoothed = refine_and_blend_lut(
            raw_lut=noisy,
            intensity=1.0,
            skin_protection=0.0,
            smooth_sigma=0.8,
            neutral_lock=0.0,
        )

        # Variance of gradient differences should be reduced by smoothing
        grad_noisy = np.diff(noisy, axis=0)
        grad_smoothed = np.diff(smoothed, axis=0)
        assert np.var(grad_smoothed) < np.var(grad_noisy)


# ------------------------------------------------------------------
# Vectorized Trilinear LUT Application
# ------------------------------------------------------------------


class TestApplyLutTrilinear:
    """Validate apply_lut and apply_lut_trilinear on image arrays."""

    def test_apply_identity_lut_reproduces_image(
        self, sample_src_image: np.ndarray
    ) -> None:
        """Applying identity LUT to any image must return identical pixels."""
        identity = generate_identity_lattice(33)
        graded = apply_lut(sample_src_image, identity)

        assert graded.shape == sample_src_image.shape
        assert graded.dtype == np.uint8

        # Max pixel difference must be <= 1 uint8 level (due to float rounding)
        max_diff = int(np.max(np.abs(sample_src_image.astype(int) - graded.astype(int))))
        assert max_diff <= 1, f"Identity LUT shifted pixels: max diff = {max_diff}"

    def test_apply_lut_float_image(self) -> None:
        """Float32 image input returns float32 output in [0.0, 1.0]."""
        identity = generate_identity_lattice(17)
        float_img = np.random.uniform(0.0, 1.0, (50, 60, 3)).astype(np.float32)

        result = apply_lut(float_img, identity)
        assert result.dtype == np.float32
        assert result.shape == float_img.shape
        np.testing.assert_allclose(result, float_img, atol=1e-5)

    def test_apply_lut_2d_and_4d_equivalence(
        self, sample_src_image: np.ndarray
    ) -> None:
        """Applying LUT as 2-D (N³, 3) or 4-D (N, N, N, 3) must yield identical results."""
        lut_2d = generate_identity_lattice(17)
        lut_4d = generate_identity_grid(17)

        res_2d = apply_lut(sample_src_image, lut_2d)
        res_4d = apply_lut(sample_src_image, lut_4d)

        np.testing.assert_array_equal(res_2d, res_4d)


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
        expected = (size**3, 3)
        assert lut.shape == expected, (
            f"For size={size}, expected shape {expected}, got {lut.shape}"
        )
