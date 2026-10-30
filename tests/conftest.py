"""
Shared pytest fixtures for the StealZaLUT test suite.

Provides reproducible sample images (source / reference / identical pairs),
temporary directories, and on-disk JPEG paths for integration-level tests.
"""

import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# Make the stealzalut package importable from the Android project tree
# ---------------------------------------------------------------------------
sys.path.insert(
    0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python")
)

import numpy as np
import pytest

# PIL is needed for saving sample images to disk
from PIL import Image


# ---------------------------------------------------------------------------
# Image fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def sample_src_image() -> np.ndarray:
    """100×100 blue-ish uint8 image (reproducible via fixed seed)."""
    rng = np.random.RandomState(42)
    img = rng.randint(0, 256, (100, 100, 3), dtype=np.uint8)
    # Push towards blue: boost B channel, dampen R channel
    img[:, :, 0] = np.clip(img[:, :, 0].astype(np.int16) - 80, 0, 255).astype(np.uint8)  # R down
    img[:, :, 2] = np.clip(img[:, :, 2].astype(np.int16) + 80, 0, 255).astype(np.uint8)  # B up
    return img


@pytest.fixture
def sample_ref_image() -> np.ndarray:
    """100×100 warm/orange-ish uint8 image (reproducible via fixed seed)."""
    rng = np.random.RandomState(99)
    img = rng.randint(0, 256, (100, 100, 3), dtype=np.uint8)
    # Push towards orange: boost R, dampen B
    img[:, :, 0] = np.clip(img[:, :, 0].astype(np.int16) + 80, 0, 255).astype(np.uint8)  # R up
    img[:, :, 1] = np.clip(img[:, :, 1].astype(np.int16) + 30, 0, 255).astype(np.uint8)  # G slight up
    img[:, :, 2] = np.clip(img[:, :, 2].astype(np.int16) - 80, 0, 255).astype(np.uint8)  # B down
    return img


@pytest.fixture
def identical_images() -> tuple[np.ndarray, np.ndarray]:
    """Two identical 100×100 uint8 images (same underlying data, independent arrays)."""
    rng = np.random.RandomState(7)
    img = rng.randint(0, 256, (100, 100, 3), dtype=np.uint8)
    return img.copy(), img.copy()


# ---------------------------------------------------------------------------
# Temporary directory fixture (delegates to pytest built-in)
# ---------------------------------------------------------------------------

@pytest.fixture
def tmp_dir(tmp_path: Path) -> Path:
    """Alias for pytest's ``tmp_path``."""
    return tmp_path


# ---------------------------------------------------------------------------
# On-disk JPEG paths
# ---------------------------------------------------------------------------

@pytest.fixture
def sample_src_path(sample_src_image: np.ndarray, tmp_path: Path) -> Path:
    """Save the blue-ish source image as a JPEG and return the path."""
    path = tmp_path / "source.jpg"
    Image.fromarray(sample_src_image).save(str(path), format="JPEG")
    return path


@pytest.fixture
def sample_ref_path(sample_ref_image: np.ndarray, tmp_path: Path) -> Path:
    """Save the orange-ish reference image as a JPEG and return the path."""
    path = tmp_path / "reference.jpg"
    Image.fromarray(sample_ref_image).save(str(path), format="JPEG")
    return path
