"""Centralised constants for StealZaLUT Mobile (Chaquopy).

Eliminates magic numbers across the colorimetric processing, LUT generation,
soft clipping, and caching pipelines.
"""

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

# Direct image export maximum dimension (4K UHD)
MAX_EXPORT_DIMENSION: int = 3840

# File extensions supported
SUPPORTED_EXTENSIONS: Tuple[str, ...] = (".jpg", ".jpeg", ".png", ".webp")

# Default colorimetric quality parameters
DEFAULT_INTENSITY: float = 1.0
DEFAULT_PRESERVE_LUMINANCE: float = 0.75
DEFAULT_SKIN_PROTECTION: float = 0.5
DEFAULT_SMOOTH_SIGMA: float = 0.6
DEFAULT_NEUTRAL_LOCK: float = 0.8

# Soft clipping thresholds (5% darkest shadows and 5% brightest highlights)
SOFT_CLIP_SHADOW_THRESHOLD: float = 0.05
SOFT_CLIP_HIGHLIGHT_THRESHOLD: float = 0.95

# Statistical bounds to prevent contrast explosion
MIN_STD_RATIO: float = 0.5
MAX_STD_RATIO: float = 2.0
EPSILON: float = 1e-6

# Skin tone detection in YCbCr space (ITU-R BT.601)
SKIN_CB_CENTER: float = 0.40
SKIN_CR_CENTER: float = 0.60
SKIN_CB_SIGMA: float = 0.08
SKIN_CR_SIGMA: float = 0.08

# Neutral axis lock parameters
NEUTRAL_LOCK_SIGMA: float = 0.08

# Luminance-Binned Chroma Transfer & Vividness parameters
DEFAULT_LUMINANCE_BINS: int = 5
VIVID_CHROMA_PERCENTILE: float = 30.0
MIN_CHROMA_BOOST: float = 0.8
MAX_CHROMA_BOOST: float = 1.6
CHROMA_BOOST_DEFAULT: float = 1.25
SHADOW_ROLLOFF_L_THRESHOLD: float = 20.0
LUMINANCE_ALIGN_WEIGHT: float = 0.35

# Cache settings
MAX_LUT_CACHE_SIZE: int = 4
