"""Calibration test pattern and color palette extractor.

Generates neutral reference test charts (pure white highlight block,
continuous grayscale gradient, skin tone patches, primary/secondary swatches)
for evaluating how a 3D LUT or color-transfer transform behaves across the gamut.
"""

from __future__ import annotations

from typing import List, Tuple
import numpy as np
from numpy.typing import NDArray
from PIL import Image, ImageDraw


def generate_neutral_test_pattern(width: int = 720, height: int = 480) -> NDArray[np.uint8]:
    """Generate a professional neutral calibration test card.

    Includes:
    1. Pure white (255) and high-key (242) reference blocks for highlight tint analysis.
    2. Continuous 0–255 grayscale gradient ramp for gamma & shadow roll-off.
    3. Neutral 18% and 50% midtone gray blocks.
    4. 12 reference patches: skin tones (light, olive, deep), primaries (RGB),
       secondaries (CMY), earth & sky colors.

    Args:
        width: Image width in pixels.
        height: Image height in pixels.

    Returns:
        RGB uint8 array of shape ``(height, width, 3)``.
    """
    card = Image.new("RGB", (width, height), (32, 34, 40))
    draw = ImageDraw.Draw(card)

    margin = 20

    # 1. Highlights / Pure White & Off-White
    w_block_h = 60
    mid_x = width // 2
    draw.rectangle([margin, margin, mid_x - 10, margin + w_block_h], fill=(255, 255, 255))
    draw.rectangle([mid_x + 10, margin, width - margin, margin + w_block_h], fill=(240, 240, 240))

    # 2. Continuous 0-255 grayscale gradient ramp
    ramp_y0 = margin + w_block_h + 15
    ramp_h = 50
    ramp_w = width - (2 * margin)
    for x in range(ramp_w):
        val = int(round(255.0 * x / max(1, ramp_w - 1)))
        draw.line([(margin + x, ramp_y0), (margin + x, ramp_y0 + ramp_h)], fill=(val, val, val))

    # 3. 18% photographic gray & 50% neutral gray
    gray_y0 = ramp_y0 + ramp_h + 15
    gray_h = 45
    draw.rectangle([margin, gray_y0, mid_x - 10, gray_y0 + gray_h], fill=(118, 118, 118))  # sRGB 18%
    draw.rectangle([mid_x + 10, gray_y0, width - margin, gray_y0 + gray_h], fill=(128, 128, 128))

    # 4. Reference patches (Skin tones + Primaries + Secondaries)
    patches: List[Tuple[int, int, int]] = [
        # Skin tones
        (205, 155, 132),  # Light skin
        (155, 110, 85),   # Olive skin
        (95, 62, 45),     # Deep skin
        # Primaries
        (225, 45, 45),    # Red
        (45, 185, 55),    # Green
        (45, 85, 225),    # Blue
        # Secondaries
        (45, 215, 225),   # Cyan
        (215, 45, 215),   # Magenta
        (235, 215, 35),   # Yellow
        # Atmosphere & foliage
        (75, 125, 185),   # Sky blue
        (85, 155, 85),    # Foliage green
        (215, 125, 45),   # Sunset orange
    ]

    cols = 6
    rows = 2
    gap = 8
    cell_w = (width - (2 * margin) - (cols - 1) * gap) // cols
    cell_h = 55
    patch_y0 = gray_y0 + gray_h + 15

    for idx, col_rgb in enumerate(patches):
        r_idx = idx // cols
        c_idx = idx % cols
        x0 = margin + c_idx * (cell_w + gap)
        y0 = patch_y0 + r_idx * (cell_h + gap)
        draw.rectangle([x0, y0, x0 + cell_w, y0 + cell_h], fill=col_rgb)

    return np.asarray(card, dtype=np.uint8)


def extract_dominant_palette(image: NDArray[np.uint8], num_colors: int = 6) -> List[str]:
    """Extract the dominant color palette from an image as hex color strings.

    Args:
        image: RGB uint8 array ``(H, W, 3)``.
        num_colors: Number of colors to extract.

    Returns:
        List of hex color strings like ``["#E67E22", "#2C3E50", ...]``.
    """
    pil_img = Image.fromarray(image)
    pil_img.thumbnail((150, 150))
    # Median cut quantization
    quantized = pil_img.quantize(colors=num_colors, method=Image.Quantize.MEDIANCUT)
    raw_palette = quantized.getpalette() or []
    palette_slice = raw_palette[: num_colors * 3]

    hex_list: List[str] = []
    for i in range(0, len(palette_slice), 3):
        r = palette_slice[i]
        g = palette_slice[i + 1]
        b = palette_slice[i + 2]
        hex_list.append(f"#{r:02X}{g:02X}{b:02X}")

    return hex_list
