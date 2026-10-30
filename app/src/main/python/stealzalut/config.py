"""Centralised business constants for StealZaLUT.

All tuneable parameters live here so that the rest of the codebase
references a single source of truth.
"""

from typing import Dict, Tuple

# ---------------------------------------------------------------------------
# Supported color-transfer methods
# ---------------------------------------------------------------------------

SUPPORTED_METHODS: Dict[str, str] = {
    "hm-mkl-hm": "Histogram Matching + MKL",
    "mkl": "Monge-Kantorovitch Linear",
    "hm": "Histogram Matching",
    "reinhard": "Reinhard (LAB stats)",
}
"""Mapping of method key → human-readable display name."""

DEFAULT_METHOD: str = "hm-mkl-hm"
"""Default colour-transfer method key."""

# ---------------------------------------------------------------------------
# LUT sizes
# ---------------------------------------------------------------------------

LUT_SIZE_FAST: int = 17
"""Fast / low-quality LUT grid size (17³ = 4 913 nodes)."""

LUT_SIZE_STANDARD: int = 33
"""Standard quality LUT grid size (33³ = 35 937 nodes)."""

LUT_SIZE_HIGH: int = 65
"""High-quality LUT grid size (65³ = 274 625 nodes)."""

DEFAULT_LUT_SIZE: int = 33
"""Default LUT grid size used when none is specified."""

# ---------------------------------------------------------------------------
# Image processing
# ---------------------------------------------------------------------------

MAX_ANALYSIS_DIMENSION: int = 512
"""Maximum pixel dimension (height or width) used for analysis proxies."""

# ---------------------------------------------------------------------------
# I/O
# ---------------------------------------------------------------------------

SUPPORTED_EXTENSIONS: Tuple[str, ...] = (".jpg", ".jpeg", ".png", ".webp")
"""File extensions accepted for source / reference images."""
