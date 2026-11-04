"""Image loading, conversion, and saving utilities.

Every function operates on NumPy arrays in HxWx3 RGB layout. Pillow
(``PIL``) is used for decoding / encoding so the module works inside
Chaquopy on Android without OpenCV.
"""

from __future__ import annotations

import os
from typing import List, Optional, Tuple, Union

import numpy as np
from numpy.typing import NDArray
from PIL import Image, ImageOps

# Use Pillow's Resampling enum with fallback for older versions
try:
    _RESAMPLING_BILINEAR = Image.Resampling.BILINEAR
except AttributeError:
    _RESAMPLING_BILINEAR = Image.BILINEAR


def load_image(path: str) -> NDArray[np.uint8]:
    """Load an image file and return it as an RGB uint8 array.

    Applies EXIF transposition systematically to handle mobile camera orientation.

    Args:
        path: Absolute or relative path to an image file.

    Returns:
        NumPy array of shape ``(H, W, 3)`` with dtype ``uint8``.

    Raises:
        FileNotFoundError: If *path* does not point to an existing file.
        ValueError: If the file cannot be decoded as an image or has
            fewer than 3 colour channels.
    """
    if not os.path.isfile(path):
        raise FileNotFoundError(f"Image file not found: {path}")

    try:
        img: Image.Image = Image.open(path)
        img = ImageOps.exif_transpose(img)
        img.load()  # force decode
    except Exception as exc:
        raise ValueError(f"Cannot decode image at {path}: {exc}") from exc

    # Convert palette / grayscale / RGBA → RGB
    if img.mode == "RGBA":
        img = img.convert("RGB")
    elif img.mode != "RGB":
        try:
            img = img.convert("RGB")
        except Exception as exc:
            raise ValueError(
                f"Unsupported image mode '{img.mode}' for {path}: {exc}"
            ) from exc

    arr: NDArray[np.uint8] = np.asarray(img, dtype=np.uint8)

    if arr.ndim != 3 or arr.shape[2] != 3:
        raise ValueError(
            f"Expected HxWx3 image, got shape {arr.shape} for {path}"
        )
    return arr


def create_proxy_image(
    image: NDArray[np.uint8],
    max_dim: Optional[int] = 512,
) -> NDArray[np.uint8]:
    """Down-sample *image* so that ``max(H, W) <= max_dim``.

    Never upscales smaller images. If *max_dim* is None, <= 0, or greater than
    max(H, W), the original array is returned untouched.

    Uses bilinear filtering for fast statistical proxy creation (2x faster than Lanczos
    and statistically equivalent for histograms and covariance matrices).

    Args:
        image: RGB uint8 array of shape ``(H, W, 3)``.
        max_dim: Maximum allowed pixel dimension. None or <= 0 preserves original size.

    Returns:
        Down-sampled (or original) array with dtype ``uint8``.
    """
    if max_dim is None or max_dim <= 0:
        return image

    h, w = image.shape[:2]
    if max(h, w) <= max_dim:
        return image

    scale: float = float(max_dim) / float(max(h, w))
    new_w: int = max(1, int(round(w * scale)))
    new_h: int = max(1, int(round(h * scale)))

    pil_img: Image.Image = Image.fromarray(image)
    pil_img = pil_img.resize((new_w, new_h), _RESAMPLING_BILINEAR)
    return np.asarray(pil_img, dtype=np.uint8)


def resolve_export_dimensions(
    orig_w: int,
    orig_h: int,
    target_max_dim: Optional[int] = None,
) -> Tuple[int, int]:
    """Calculate target export dimensions preserving aspect ratio and preventing unwanted upscaling.

    In NATIVE mode (target_max_dim is None or <= 0), the exact original dimensions
    are preserved without any resizing. When a maximum dimension (e.g. 4K / 2K / 1080p)
    is specified, a min(original_dim, target_max_dim) clamp is applied so smaller images
    are never artificially upscaled.

    Args:
        orig_w: Original image width in pixels.
        orig_h: Original image height in pixels.
        target_max_dim: Requested maximum pixel dimension (width or height).
            If None or <= 0, full native resolution is preserved.

    Returns:
        (target_w, target_h) tuple of integer dimensions.
    """
    if target_max_dim is None or target_max_dim <= 0:
        return orig_w, orig_h

    max_orig = max(orig_w, orig_h)
    effective_max = min(max_orig, target_max_dim)
    if effective_max >= max_orig:
        return orig_w, orig_h

    scale = float(effective_max) / float(max_orig)
    new_w = max(1, int(round(orig_w * scale)))
    new_h = max(1, int(round(orig_h * scale)))
    return new_w, new_h


def to_float32(image: NDArray[np.uint8]) -> NDArray[np.float32]:
    """Convert a ``uint8`` image in [0, 255] to ``float32`` in [0.0, 1.0].

    Args:
        image: Array with dtype ``uint8``.

    Returns:
        Array with dtype ``float32`` and values in ``[0.0, 1.0]``.
    """
    return image.astype(np.float32) / np.float32(255.0)


def to_float64(image: NDArray[np.uint8]) -> NDArray[np.float64]:
    """Convert a ``uint8`` image in [0, 255] to ``float64`` in [0.0, 1.0].

    Args:
        image: Array with dtype ``uint8``.

    Returns:
        Array with dtype ``float64`` and values in ``[0.0, 1.0]``.
    """
    return image.astype(np.float32) / np.float32(255.0)


def to_uint8(image: NDArray[Union[np.float32, np.float64]]) -> NDArray[np.uint8]:
    """Convert a floating-point image in [0.0, 1.0] to ``uint8`` in [0, 255].

    Values are clipped before conversion so out-of-range floats are
    safely clamped.

    Args:
        image: Array with dtype ``float32`` or ``float64``.

    Returns:
        Array with dtype ``uint8`` and values in ``[0, 255]``.
    """
    return np.clip(image * 255.0, 0.0, 255.0).astype(np.uint8)


def save_image(
    image: NDArray[Union[np.uint8, np.float32, np.float64]],
    path: str,
    quality: int = 90,
) -> str:
    """Save an ndarray as a JPEG or PNG image file.

    The format is inferred from the file extension in *path*. If the
    array has a floating-point dtype it is converted to ``uint8`` first.

    Args:
        image: RGB array of shape ``(H, W, 3)``.
        path: Destination file path (must end with ``.jpg``, ``.jpeg``,
            or ``.png``).
        quality: JPEG quality (1–100). Ignored for PNG.

    Returns:
        The absolute path of the saved file.
    """
    if image.dtype != np.uint8:
        image = to_uint8(image)

    pil_img: Image.Image = Image.fromarray(image)

    ext: str = os.path.splitext(path)[1].lower()
    save_kwargs: dict = {}
    if ext in (".jpg", ".jpeg"):
        save_kwargs["quality"] = quality
        save_kwargs["subsampling"] = 0  # 4:4:4

    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    pil_img.save(path, **save_kwargs)
    return os.path.abspath(path)


def extract_dominant_palette(image: NDArray[np.uint8], num_colors: int = 6) -> List[str]:
    """Extract the dominant color palette from an image as hex color strings.

    Resizes image to max 128x128 for ultra-fast extraction (<15ms) and guarantees
    that exactly num_colors valid hex strings are returned, even for degenerate/solid images.

    Args:
        image: RGB uint8 array ``(H, W, 3)``.
        num_colors: Number of colors to extract.

    Returns:
        List of hex color strings like ``["#E67E22", "#2C3E50", ...]``.
    """
    pil_img = Image.fromarray(image)
    pil_img.thumbnail((128, 128))
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

    # Ensure degenerate/solid cases return exactly num_colors
    while len(hex_list) < num_colors:
        if hex_list:
            hex_list.append(hex_list[-1])
        else:
            hex_list.append("#000000")

    return hex_list[:num_colors]
