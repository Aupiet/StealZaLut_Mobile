"""Unit tests for Adobe Camera Raw Crop and Straighten spatial processing."""

from __future__ import annotations

import numpy as np
import pytest

from stealzalut.core.lightroom_models import (
    CropSettings,
    EffectsSettings,
    LightroomDevelopSettings,
)
from stealzalut.core.lightroom_spatial import (
    apply_crop_spatial,
    apply_full_lightroom_spatial_pipeline,
)


@pytest.fixture
def synthetic_image() -> np.ndarray:
    """Create a 120x160 RGB float32 test pattern with gradient."""
    h, w = 120, 160
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    img = np.zeros((h, w, 3), dtype=np.float32)
    img[..., 0] = x / float(w)  # Horizontal gradient Red
    img[..., 1] = y / float(h)  # Vertical gradient Green
    img[..., 2] = 0.5            # Neutral Blue
    return img


def test_default_crop_is_noop(synthetic_image: np.ndarray) -> None:
    """Default crop settings (0, 0, 1, 1, 0) should return the exact same array."""
    settings = CropSettings()
    assert not settings.has_crop
    cropped = apply_crop_spatial(synthetic_image, settings)
    assert cropped.shape == synthetic_image.shape
    np.testing.assert_array_equal(cropped, synthetic_image)


def test_normalized_crop_geometry(synthetic_image: np.ndarray) -> None:
    """Test standard normalized bounding box crop."""
    # 120x160 image: left 0.25 (x=40), right 0.75 (x=120), top 0.25 (y=30), bottom 0.75 (y=90)
    settings = CropSettings(left=0.25, top=0.25, right=0.75, bottom=0.75, angle=0.0)
    assert settings.has_crop
    cropped = apply_crop_spatial(synthetic_image, settings)

    assert cropped.shape == (60, 80, 3)
    # Check pixel bounds: center of original image should be present in cropped
    assert np.isclose(cropped[0, 0, 0], 40.0 / 160.0, atol=0.02)
    assert np.isclose(cropped[0, 0, 1], 30.0 / 120.0, atol=0.02)


def test_straighten_rotation(synthetic_image: np.ndarray) -> None:
    """Straightening rotates pixels around the center before cropping."""
    settings = CropSettings(left=0.0, top=0.0, right=1.0, bottom=1.0, angle=10.0)
    assert settings.has_crop
    straightened = apply_crop_spatial(synthetic_image, settings)
    assert straightened.shape == synthetic_image.shape
    # Content must have rotated (not identical to original)
    assert not np.array_equal(straightened, synthetic_image)


def test_inverted_and_out_of_bounds_clamping(synthetic_image: np.ndarray) -> None:
    """Test that inverted coordinates (left > right) and out-of-range floats clamp gracefully."""
    # Inverted left/right and top/bottom
    settings = CropSettings(left=0.8, top=0.7, right=0.2, bottom=0.3, angle=0.0)
    cropped = apply_crop_spatial(synthetic_image, settings)
    # Expected: x from 0.2 to 0.8 (96 px), y from 0.3 to 0.7 (48 px)
    assert cropped.shape[0] == 48
    assert cropped.shape[1] == 96

    # Extreme out-of-bounds
    extreme = CropSettings(left=-0.5, top=-0.5, right=1.5, bottom=1.5)
    cropped_extreme = apply_crop_spatial(synthetic_image, extreme)
    assert cropped_extreme.shape == synthetic_image.shape


def test_integrated_pipeline_with_crop_and_post_crop_effects(synthetic_image: np.ndarray) -> None:
    """Full spatial pipeline applies crop and post-crop effects in proper sequence."""
    settings = LightroomDevelopSettings(
        crop=CropSettings(left=0.2, top=0.2, right=0.8, bottom=0.8),
        is_effects_enabled=True,
        effects=EffectsSettings(vignette_amount=-50.0)
    )

    result = apply_full_lightroom_spatial_pipeline(synthetic_image, settings)
    # Target shape must be 60% of original (120*0.6 = 72, 160*0.6 = 96)
    assert result.shape == (72, 96, 3)
    # Corners of cropped image should have vignetting applied (darker than center)
    center_val = result[36, 48, :2].mean()
    corner_val = result[0, 0, :2].mean()
    assert corner_val < center_val
