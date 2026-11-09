"""Centralised constants for StealZaLUT Mobile (Chaquopy).

Eliminates magic numbers across the colorimetric processing, LUT generation,
soft clipping, and caching pipelines.
"""

from typing import Dict

# Supported color-transfer methods aligned with desktop
SUPPORTED_METHODS: Dict[str, str] = {
    "hm-mkl-hm": "Histogram Matching + MKL",
    "mkl": "Monge-Kantorovitch Linear",
    "hm-mvgd-hm": "Histogram Matching + MVGD",
    "mvgd": "Multi-Variate Gaussian Distribution",
    "hm": "Histogram Matching",
    "reinhard": "Reinhard (LAB stats)",
}

DEFAULT_METHOD: str = "hm-mkl-hm"

# LUT grid sizes
DEFAULT_LUT_SIZE: int = 33

# Image processing proxy maximum dimension (high fidelity for neon lights & fine details)
MAX_ANALYSIS_DIMENSION: int = 512
PROXY_DECODE_MAX_DIM: int = 1024

# Direct image export maximum dimension (4K UHD)

# File extensions supported

# Default colorimetric quality parameters
# All Stage 2 protections are 0.0 / False by default to achieve 100% visual parity with desktop
DEFAULT_INTENSITY: float = 1.0
DEFAULT_PRESERVE_LUMINANCE: float = 0.0
DEFAULT_SKIN_PROTECTION: float = 0.0
DEFAULT_SMOOTH_SIGMA: float = 0.0
DEFAULT_NEUTRAL_LOCK: float = 0.0

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

# Cache settings
MAX_LUT_CACHE_SIZE: int = 4

# Dominant Palette Extraction parameters
PALETTE_RESIZE_DIM: int = 256
PALETTE_DEFAULT_COLORS: int = 6
PALETTE_MIN_LUMA: float = 8.0
PALETTE_MIN_CHROMA: float = 6.0
PALETTE_DELTA_E_THRESHOLD: float = 12.0
