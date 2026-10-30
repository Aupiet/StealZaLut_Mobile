"""
Unit tests for dominant palette extraction in stealzalut.utils.image_io.

Validates that extract_dominant_palette returns exactly K colors in
valid HEX format (^#[0-9A-Fa-f]{6}$) and handles degenerate / uniform images safely.
"""

import re
import sys
from pathlib import Path

# Ensure stealzalut package is importable
sys.path.insert(
    0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python")
)

import numpy as np

from stealzalut.utils.image_io import extract_dominant_palette

HEX_REGEX = re.compile(r"^#[0-9A-Fa-f]{6}$")


def _generate_synthetic_image(h=200, w=200):
    rng = np.random.RandomState(42)
    return rng.randint(0, 256, (h, w, 3), dtype=np.uint8)


def test_palette_extraction_counts_and_format():
    """Verify palette extraction for various K values on arbitrary image."""
    img = _generate_synthetic_image(150, 150)
    for k in [1, 4, 6, 8]:
        palette = extract_dominant_palette(img, num_colors=k)
        assert len(palette) == k, f"Expected {k} colors, got {len(palette)}"
        for hex_code in palette:
            assert HEX_REGEX.match(hex_code) is not None, f"Invalid hex color: {hex_code}"


def test_palette_extraction_degenerate_uniform_black():
    """Verify palette extraction on a 100% black uniform image (degenerate case)."""
    uniform_black = np.zeros((120, 120, 3), dtype=np.uint8)
    for k in [4, 6]:
        palette = extract_dominant_palette(uniform_black, num_colors=k)
        assert len(palette) == k, f"Expected {k} colors on black image, got {len(palette)}"
        for hex_code in palette:
            assert HEX_REGEX.match(hex_code) is not None, f"Invalid hex color: {hex_code}"
            assert hex_code.upper() == "#000000"


def test_palette_extraction_degenerate_uniform_white():
    """Verify palette extraction on a 100% white uniform image (degenerate case)."""
    uniform_white = np.full((120, 120, 3), 255, dtype=np.uint8)
    for k in [4, 6]:
        palette = extract_dominant_palette(uniform_white, num_colors=k)
        assert len(palette) == k, f"Expected {k} colors on white image, got {len(palette)}"
        for hex_code in palette:
            assert HEX_REGEX.match(hex_code) is not None, f"Invalid hex color: {hex_code}"
            assert hex_code.upper() == "#FFFFFF"


def test_palette_extraction_degenerate_uniform_red():
    """Verify palette extraction on a solid red image."""
    uniform_red = np.zeros((100, 100, 3), dtype=np.uint8)
    uniform_red[:, :, 0] = 255
    palette = extract_dominant_palette(uniform_red, num_colors=6)
    assert len(palette) == 6
    for hex_code in palette:
        assert HEX_REGEX.match(hex_code) is not None
        assert hex_code.upper() == "#FF0000"


if __name__ == "__main__":
    test_palette_extraction_counts_and_format()
    test_palette_extraction_degenerate_uniform_black()
    test_palette_extraction_degenerate_uniform_white()
    test_palette_extraction_degenerate_uniform_red()
    print("All palette extraction tests passed successfully!")
