"""Worker pipeline for generating calibration test patterns and color sets.

Applies the learned color transfer to a neutral test chart (pure white,
grayscale gradient, skin tones, primaries) and extracts dominant palettes.
"""

from __future__ import annotations

import time
from typing import Any, Dict, List

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import MAX_ANALYSIS_DIMENSION
from stealzalut.core.color_transfer import compute_color_transfer
from stealzalut.core.test_pattern import (
    extract_dominant_palette,
    generate_neutral_test_pattern,
)
from stealzalut.utils.image_io import (
    create_proxy_image,
    load_image,
    save_image,
    to_float64,
    to_uint8,
)


def run_colorset_pipeline(
    ref_path: str,
    output_card_path: str,
    method: str = "hm-mkl-hm",
    intensity: float = 1.0,
) -> Dict[str, Any]:
    """Execute the calibration color-set pipeline.

    Args:
        ref_path: Path to the reference image whose look is extracted.
        output_card_path: Destination path where the transformed test card is saved.
        method: Color transfer method key.
        intensity: Opacity blend in ``[0.0, 1.0]``.

    Returns:
        Dictionary with status, saved card path, extracted palettes, and execution time.
    """
    start: float = time.monotonic()

    try:
        # Load reference image & create proxy for fast stats
        ref_img: NDArray[np.uint8] = load_image(ref_path)
        ref_proxy: NDArray[np.uint8] = create_proxy_image(
            ref_img, max_dim=MAX_ANALYSIS_DIMENSION
        )

        # Extract reference palette
        ref_palette: List[str] = extract_dominant_palette(ref_proxy, num_colors=6)
        del ref_img

        # Generate neutral calibration card (720x480)
        neutral_card: NDArray[np.uint8] = generate_neutral_test_pattern(720, 480)

        # Apply color transfer to the neutral card
        transferred: NDArray[np.uint8] = compute_color_transfer(
            src_img=neutral_card,
            ref_img=ref_proxy,
            method=method,
        )
        del ref_proxy

        # Apply intensity blend if < 1.0
        if intensity < 1.0:
            neutral_f = to_float64(neutral_card)
            trans_f = to_float64(transferred)
            blended = np.clip((1.0 - intensity) * neutral_f + intensity * trans_f, 0.0, 1.0)
            transferred = to_uint8(blended)
            del neutral_f, trans_f, blended

        del neutral_card

        # Extract palette from the transformed card
        graded_palette: List[str] = extract_dominant_palette(transferred, num_colors=6)

        # Save card
        saved_path: str = save_image(transferred, output_card_path, quality=95)
        del transferred

        elapsed_ms: float = (time.monotonic() - start) * 1000.0

        return {
            "status": "success",
            "colorset_path": saved_path,
            "ref_palette": ref_palette,
            "graded_palette": graded_palette,
            "execution_time_ms": round(elapsed_ms, 1),
        }

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = (time.monotonic() - start) * 1000.0
        return {
            "status": "error",
            "message": str(exc),
            "execution_time_ms": round(elapsed_ms, 1),
        }
