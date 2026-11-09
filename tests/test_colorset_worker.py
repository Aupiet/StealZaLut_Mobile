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
            assert HEX_REGEX.match(hex_code) is not None, (
                f"Invalid hex color: {hex_code}"
            )


def test_palette_extraction_degenerate_uniform_black():
    """Verify palette extraction on a 100% black uniform image (degenerate case)."""
    uniform_black = np.zeros((120, 120, 3), dtype=np.uint8)
    for k in [4, 6]:
        palette = extract_dominant_palette(uniform_black, num_colors=k)
        assert len(palette) == k, (
            f"Expected {k} colors on black image, got {len(palette)}"
        )
        for hex_code in palette:
            assert HEX_REGEX.match(hex_code) is not None, (
                f"Invalid hex color: {hex_code}"
            )
            assert hex_code.upper() == "#000000"


def test_palette_extraction_degenerate_uniform_white():
    """Verify palette extraction on a 100% white uniform image (degenerate case)."""
    uniform_white = np.full((120, 120, 3), 255, dtype=np.uint8)
    for k in [4, 6]:
        palette = extract_dominant_palette(uniform_white, num_colors=k)
        assert len(palette) == k, (
            f"Expected {k} colors on white image, got {len(palette)}"
        )
        for hex_code in palette:
            assert HEX_REGEX.match(hex_code) is not None, (
                f"Invalid hex color: {hex_code}"
            )
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


def test_palette_extraction_night_scene_captures_vibrant_neons():
    """Verify that night scenes with bright neons extract vivid colors and not only blacks."""
    # 95% dark night city sky/buildings (black/dark navy) + 5% intense neon cyan & red signs
    night_img = np.full((200, 200, 3), 12, dtype=np.uint8)
    # Bright cyan neon sign
    night_img[20:40, 20:80] = [0, 230, 255]
    # Bright red neon sign
    night_img[100:120, 100:150] = [255, 30, 40]

    palette = extract_dominant_palette(night_img, num_colors=6)
    assert len(palette) == 6

    # Verify that not all colors are dark (at least one vivid/bright color is captured)
    def is_bright_or_vivid(hex_str: str) -> bool:
        r = int(hex_str[1:3], 16)
        g = int(hex_str[3:5], 16)
        b = int(hex_str[5:7], 16)
        return max(r, g, b) > 100

    vivid_count = sum(1 for c in palette if is_bright_or_vivid(c))
    assert vivid_count >= 1, (
        f"Expected vibrant colors in night scene palette, got: {palette}"
    )


def test_bridge_extract_palette_large_image(tmp_path):
    """Verify bridge.extract_palette executes safely on large image file via proxy loading."""
    from PIL import Image
    from stealzalut.bridge import extract_palette

    large_path = tmp_path / "large_palette_input.jpg"
    # Create a 3000x2000 JPEG
    img = Image.new("RGB", (3000, 2000), (120, 150, 180))
    img.save(str(large_path), format="JPEG")

    res = extract_palette(str(large_path), num_colors=6)
    assert res["status"] == "success"
    assert len(res["palette"]) == 6
    for c in res["palette"]:
        assert HEX_REGEX.match(c) is not None


def test_bridge_extract_palette_missing_file():
    """Verify bridge.extract_palette returns error structure on missing file."""
    from stealzalut.bridge import extract_palette

    res = extract_palette("/nonexistent/image.jpg", num_colors=6)
    assert res["status"] == "error"
    assert res["palette"] == []
    assert res["error_code"] == "SOURCE_NOT_FOUND"


if __name__ == "__main__":
    test_palette_extraction_counts_and_format()
    test_palette_extraction_degenerate_uniform_black()
    test_palette_extraction_degenerate_uniform_white()
    test_palette_extraction_degenerate_uniform_red()
    print("All palette extraction tests passed successfully!")
