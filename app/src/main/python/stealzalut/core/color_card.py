"""Calibration test card and visual color spectrum generator.

Generates professional neutral reference test charts and applies the learned
color transfer to produce a visual color card showing highlights, grayscale
gradient roll-off, midtones, skin tones, and gamut primaries/secondaries.
"""

from __future__ import annotations

import time
from typing import Any, Dict, List, Tuple

import numpy as np
from numpy.typing import NDArray
from PIL import Image, ImageDraw

from stealzalut.config import MAX_ANALYSIS_DIMENSION
from stealzalut.core.color_transfer import compute_color_transfer
from stealzalut.utils.image_io import (
    extract_dominant_palette,
    load_proxy_image,
    save_image,
    to_float32,
    to_uint8,
)


def generate_neutral_calibration_card(
    width: int = 540, height: int = 360
) -> NDArray[np.uint8]:
    """Generate a neutral calibration test card.

    Includes:
    1. Highlight reference blocks (pure white and high-key off-white) for highlight tint analysis.
    2. Continuous 0-255 grayscale gradient ramp for gamma & shadow roll-off.
    3. Neutral 18% and 50% midtone gray bars.
    4. 12 reference patches: skin tones (light, olive, deep), primaries (RGB),
       secondaries (CMY), atmosphere (sky blue, foliage green, sunset orange).

    Args:
        width: Image width in pixels.
        height: Image height in pixels.

    Returns:
        RGB uint8 array of shape ``(height, width, 3)``.
    """
    card = Image.new("RGB", (width, height), (32, 34, 40))
    draw = ImageDraw.Draw(card)

    margin = 16

    # 1. Highlights / Pure White & Off-White
    w_block_h = 44
    mid_x = width // 2
    draw.rectangle(
        [margin, margin, mid_x - 8, margin + w_block_h], fill=(255, 255, 255)
    )
    draw.rectangle(
        [mid_x + 8, margin, width - margin, margin + w_block_h], fill=(240, 240, 240)
    )

    # 2. Continuous 0-255 grayscale gradient ramp
    ramp_y0 = margin + w_block_h + 12
    ramp_h = 38
    ramp_w = width - (2 * margin)
    for x in range(ramp_w):
        val = int(round(255.0 * x / max(1, ramp_w - 1)))
        draw.line(
            [(margin + x, ramp_y0), (margin + x, ramp_y0 + ramp_h)],
            fill=(val, val, val),
        )

    # 3. 18% photographic gray & 50% neutral gray
    gray_y0 = ramp_y0 + ramp_h + 12
    gray_h = 34
    draw.rectangle([margin, gray_y0, mid_x - 8, gray_y0 + gray_h], fill=(118, 118, 118))
    draw.rectangle(
        [mid_x + 8, gray_y0, width - margin, gray_y0 + gray_h], fill=(128, 128, 128)
    )

    # 4. 12 Gamut Reference patches (Skin tones + Primaries + Secondaries)
    patches: List[Tuple[int, int, int]] = [
        # Skin tones
        (205, 155, 132),  # Light skin
        (155, 110, 85),  # Olive skin
        (95, 62, 45),  # Deep skin
        # Primaries
        (225, 45, 45),  # Red
        (45, 185, 55),  # Green
        (45, 85, 225),  # Blue
        # Secondaries
        (45, 215, 225),  # Cyan
        (215, 45, 215),  # Magenta
        (235, 215, 35),  # Yellow
        # Atmosphere & foliage
        (75, 125, 185),  # Sky blue
        (85, 155, 85),  # Foliage green
        (215, 125, 45),  # Sunset orange
    ]

    cols = 6
    gap = 6
    cell_w = (width - (2 * margin) - (cols - 1) * gap) // cols
    cell_h = 42
    patch_y0 = gray_y0 + gray_h + 12

    for idx, col_rgb in enumerate(patches):
        r_idx = idx // cols
        c_idx = idx % cols
        x0 = margin + c_idx * (cell_w + gap)
        y0 = patch_y0 + r_idx * (cell_h + gap)
        draw.rectangle([x0, y0, x0 + cell_w, y0 + cell_h], fill=col_rgb)

    return np.asarray(card, dtype=np.uint8)


def run_color_card_pipeline(
    ref_path: str,
    output_card_path: str,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
    width: int = 540,
    height: int = 360,
) -> Dict[str, Any]:
    """Generate a visual color calibration card showcasing the look's colors.

    Args:
        ref_path: Path to the style reference image.
        output_card_path: File path where the resulting color card image will be saved.
        method: Color transfer method key (e.g. "hm-mkl-hm").
        intensity: Opacity blend in [0.0, 1.0].
        width: Card width in pixels.
        height: Card height in pixels.

    Returns:
        Dictionary with status, saved card path, palette, and execution time.
    """
    start: float = time.monotonic()
    try:
        ref_proxy: NDArray[np.uint8] = load_proxy_image(
            ref_path, max_dim=MAX_ANALYSIS_DIMENSION
        )
        ref_palette: List[str] = extract_dominant_palette(ref_proxy, num_colors=6)

        neutral_card: NDArray[np.uint8] = generate_neutral_calibration_card(
            width, height
        )

        transferred: NDArray[np.uint8] = compute_color_transfer(
            src_img=neutral_card,
            ref_img=ref_proxy,
            method=method,
        )
        del ref_proxy

        clamped_intensity = max(0.0, min(1.0, float(intensity)))
        if clamped_intensity < 0.999:
            neutral_f = to_float32(neutral_card)
            trans_f = to_float32(transferred)
            blended = np.clip(
                (1.0 - clamped_intensity) * neutral_f + clamped_intensity * trans_f,
                0.0,
                1.0,
            )
            transferred = to_uint8(blended)
            del neutral_f, trans_f, blended

        del neutral_card

        saved_path = save_image(transferred, output_card_path, quality=92)
        del transferred

        elapsed_ms: float = (time.monotonic() - start) * 1000.0
        return {
            "status": "success",
            "card_path": saved_path,
            "palette": ref_palette,
            "execution_time_ms": round(elapsed_ms, 1),
        }
    except Exception as exc:  # noqa: BLE001
        elapsed_ms = (time.monotonic() - start) * 1000.0
        return {
            "status": "error",
            "message": str(exc),
            "execution_time_ms": round(elapsed_ms, 1),
        }
