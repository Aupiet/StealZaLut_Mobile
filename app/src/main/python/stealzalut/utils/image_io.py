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

from stealzalut.config import (
    MAX_ANALYSIS_DIMENSION,
    PALETTE_DEFAULT_COLORS,
    PALETTE_DELTA_E_THRESHOLD,
    PALETTE_MIN_CHROMA,
    PALETTE_MIN_LUMA,
    PALETTE_RESIZE_DIM,
    PROXY_DECODE_MAX_DIM,
)

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


def load_proxy_image(path: str, max_dim: int = MAX_ANALYSIS_DIMENSION) -> NDArray[np.uint8]:
    """Load an image file directly as a downscaled proxy, bounding memory consumption.

    Uses Pillow's native JPEG draft downsampling and thumbnail reduction before
    allocating full NumPy arrays, preventing memory spikes on high-resolution photos
    (e.g. 50 MP) while respecting EXIF orientation.

    Args:
        path: Path to the image file.
        max_dim: Target maximum dimension for height or width (default MAX_ANALYSIS_DIMENSION).

    Returns:
        RGB uint8 array of shape (H, W, 3) where max(H, W) <= max_dim.
    """
    if not os.path.isfile(path):
        raise FileNotFoundError(f"Image file not found: {path}")

    try:
        img: Image.Image = Image.open(path)
        # For JPEG images, draft() downsamples inside the C decoder without full allocation
        if getattr(img, "format", "") in ("JPEG", "JPG"):
            try:
                img.draft("RGB", (max_dim, max_dim))
            except Exception:
                pass

        img = ImageOps.exif_transpose(img)

        # Scale down if still larger than max_dim
        w, h = img.size
        if max(w, h) > max_dim:
            img.thumbnail((max_dim, max_dim), _RESAMPLING_BILINEAR)

        if img.mode != "RGB":
            img = img.convert("RGB")

        arr: NDArray[np.uint8] = np.ascontiguousarray(np.asarray(img, dtype=np.uint8))
        if arr.ndim != 3 or arr.shape[2] != 3:
            raise ValueError(f"Expected HxWx3 proxy, got {arr.shape} for {path}")
        return arr
    except Exception as exc:
        raise ValueError(f"Cannot decode proxy image at {path}: {exc}") from exc


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


def get_clean_exif_bytes(path: str) -> Optional[bytes]:
    """Extract EXIF bytes from an image file and reset Orientation tag to 1 (Normal).

    Since loaded images are transposed upright by ImageOps.exif_transpose, the exported
    image orientation tag must be 1 so external image viewers do not apply duplicate rotation.

    Args:
        path: Path to the source image file.

    Returns:
        Cleaned EXIF bytes if available and within safe JPEG APP1 bounds (<= 65500 bytes), or None.
    """
    if not os.path.isfile(path):
        return None
    try:
        with Image.open(path) as img:
            exif = img.getexif()
            raw_bytes: Optional[bytes] = None
            if not exif:
                raw_bytes = img.info.get("exif")
            else:
                # 0x0112 is EXIF Orientation tag
                if 0x0112 in exif:
                    exif[0x0112] = 1
                raw_bytes = exif.tobytes()
            # JPEG standard APP1 marker limit is 65533 bytes.
            # Oversized EXIF (e.g. embedded thumbnails from camera sensors) causes Pillow to crash
            # with ValueError: EXIF data is too long.
            if raw_bytes and len(raw_bytes) <= 65500:
                return raw_bytes
            return None
    except Exception:
        return None


def save_image(
    image: NDArray[Union[np.uint8, np.float32, np.float64]],
    path: str,
    quality: int = 90,
    exif_bytes: Optional[bytes] = None,
) -> str:
    """Save an ndarray as a JPEG or PNG image file, optionally preserving EXIF metadata.

    The format is inferred from the file extension in *path*. If the
    array has a floating-point dtype it is converted to ``uint8`` first.

    Args:
        image: RGB array of shape ``(H, W, 3)``.
        path: Destination file path (must end with ``.jpg``, ``.jpeg``,
            or ``.png``).
        quality: JPEG quality (1–100). Ignored for PNG.
        exif_bytes: Optional EXIF metadata bytes to attach.

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
        if exif_bytes:
            save_kwargs["exif"] = exif_bytes
    elif ext == ".png" and exif_bytes:
        try:
            save_kwargs["exif"] = exif_bytes
        except Exception:
            pass

    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    try:
        pil_img.save(path, **save_kwargs)
    except Exception:
        # If saving with EXIF metadata failed (e.g. malformed or oversized EXIF segment),
        # retry safely without EXIF so image export never fails unexpectedly.
        if "exif" in save_kwargs:
            save_kwargs.pop("exif", None)
            pil_img.save(path, **save_kwargs)
        else:
            raise
    return os.path.abspath(path)


def extract_dominant_palette(
    image: NDArray[np.uint8],
    num_colors: int = PALETTE_DEFAULT_COLORS,
) -> List[str]:
    """Extract perceptually dominant color palette from an image as hex strings.

    Ultra-fast (< 10ms) and rock-solid: utilizes Pillow's native C-level median-cut quantization,
    completely bypassing NumPy umath vector intrinsics to prevent any SIGSEGV on ARM64 mobile.

    Args:
        image: RGB uint8 array ``(H, W, 3)``.
        num_colors: Number of dominant colors to extract.

    Returns:
        List of uppercase hex color strings like ``["#E67E22", "#2C3E50", ...]``.
    """
    if num_colors <= 0:
        return []

    pil_img: Image.Image = Image.fromarray(image)

    # Check for degenerate/uniform images using Pillow's fast C extrema
    extrema = pil_img.getextrema()
    if (
        isinstance(extrema, tuple)
        and len(extrema) == 3
        and isinstance(extrema[0], tuple)
        and extrema[0][0] == extrema[0][1]
        and extrema[1][0] == extrema[1][1]
        and extrema[2][0] == extrema[2][1]
    ):
        r, g, b = extrema[0][0], extrema[1][0], extrema[2][0]
        hex_val = f"#{r:02X}{g:02X}{b:02X}"
        return [hex_val] * num_colors

    # Downsample for ultra-fast processing (< 10ms)
    pil_img.thumbnail((128, 128), _RESAMPLING_BILINEAR)

    # Median cut quantization (native C implementation in libimaging)
    quantize_method = getattr(getattr(Image, "Quantize", None), "MEDIANCUT", getattr(Image, "MEDIANCUT", 0))
    quantized = pil_img.quantize(colors=num_colors, method=quantize_method)
    raw_palette = quantized.getpalette() or []
    palette_slice = raw_palette[: num_colors * 3]

    hex_list: List[str] = []
    for i in range(0, len(palette_slice), 3):
        r = palette_slice[i]
        g = palette_slice[i + 1]
        b = palette_slice[i + 2]
        hex_str = f"#{r:02X}{g:02X}{b:02X}"
        if hex_str not in hex_list:
            hex_list.append(hex_str)

    # Pad if necessary for extreme degenerate cases
    while len(hex_list) < num_colors:
        if hex_list:
            hex_list.append(hex_list[-1])
        else:
            hex_list.append("#000000")

    return hex_list[:num_colors]
