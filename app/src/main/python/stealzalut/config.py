"""Centralised constants for StealZaLUT Mobile (Chaquopy)."""

from typing import Dict, Tuple

# Supported color-transfer methods
SUPPORTED_METHODS: Dict[str, str] = {
    "hm-mkl-hm": "Histogram Matching + MKL",
    "mkl": "Monge-Kantorovitch Linear",
    "hm": "Histogram Matching",
    "reinhard": "Reinhard (LAB stats)",
}

DEFAULT_METHOD: str = "hm-mkl-hm"

# LUT grid sizes
LUT_SIZE_FAST: int = 17
LUT_SIZE_STANDARD: int = 33
LUT_SIZE_HIGH: int = 65
DEFAULT_LUT_SIZE: int = 33

# Image processing proxy maximum dimension
MAX_ANALYSIS_DIMENSION: int = 512

# File extensions supported
SUPPORTED_EXTENSIONS: Tuple[str, ...] = (".jpg", ".jpeg", ".png", ".webp")
