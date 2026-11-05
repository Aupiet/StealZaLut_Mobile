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

        arr: NDArray[np.uint8] = np.asarray(img, dtype=np.uint8)
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
        Cleaned EXIF bytes if available, or None.
    """
    if not os.path.isfile(path):
        return None
    try:
        with Image.open(path) as img:
            exif = img.getexif()
            if not exif:
                return img.info.get("exif")
            # 0x0112 is EXIF Orientation tag
            if 0x0112 in exif:
                exif[0x0112] = 1
            return exif.tobytes()
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
    pil_img.save(path, **save_kwargs)
    return os.path.abspath(path)


def extract_dominant_palette(
    image: NDArray[np.uint8],
    num_colors: int = PALETTE_DEFAULT_COLORS,
) -> List[str]:
    """Extract perceptually dominant color palette from an image as hex strings.

    Operates in CIE L*a*b* space using chroma-weighted clustering to capture vibrant
    highlights and colorful neon lights in night photography, while deduplicating
    near-identical shades using Delta E.

    Args:
        image: RGB uint8 array ``(H, W, 3)``.
        num_colors: Number of dominant colors to extract.

    Returns:
        List of uppercase hex color strings like ``["#E67E22", "#2C3E50", ...]``.
    """
    if num_colors <= 0:
        return []

    # Check for degenerate/uniform images
    min_val = np.min(image, axis=(0, 1))
    max_val = np.max(image, axis=(0, 1))
    if np.array_equal(min_val, max_val):
        r, g, b = int(min_val[0]), int(min_val[1]), int(min_val[2])
        hex_val = f"#{r:02X}{g:02X}{b:02X}"
        return [hex_val] * num_colors

    # Downsample for ultra-fast processing (< 20ms)
    pil_img = Image.fromarray(image)
    pil_img.thumbnail(
        (PALETTE_RESIZE_DIM, PALETTE_RESIZE_DIM),
        getattr(Image, "Resampling", Image).BILINEAR,
    )
    downsampled = np.asarray(pil_img, dtype=np.uint8)

    # Convert to CIE LAB
    from stealzalut.core.color_transfer import lab_to_rgb, rgb_to_lab

    rgb_f = to_float32(downsampled)
    lab = rgb_to_lab(rgb_f).reshape(-1, 3)  # (N, 3): L in [0, 100], a, b in [-128, 127]

    # Calculate Chroma C = sqrt(a^2 + b^2)
    chroma = np.sqrt(lab[:, 1] ** 2 + lab[:, 2] ** 2)

    # Separate chromatic and neutral/shadow pixels
    chromatic_mask = (chroma >= PALETTE_MIN_CHROMA) & (lab[:, 0] >= PALETTE_MIN_LUMA)
    chromatic_pixels = lab[chromatic_mask]
    chromatic_weights = chroma[chromatic_mask]

    # Candidate centroids pool
    candidates_lab: List[NDArray[np.float32]] = []

    # If we have chromatic pixels, sample / cluster them weighted by chroma
    if len(chromatic_pixels) > num_colors:
        probs = chromatic_weights / np.sum(chromatic_weights)
        first_idx = int(np.argmax(chromatic_weights))
        candidates_lab.append(chromatic_pixels[first_idx])

        rng = np.random.RandomState(42)
        sample_indices = rng.choice(
            len(chromatic_pixels),
            size=min(1000, len(chromatic_pixels)),
            p=probs,
            replace=False,
        )
        subset_lab = chromatic_pixels[sample_indices]

        for _ in range(num_colors * 2):
            if len(candidates_lab) >= num_colors * 2:
                break
            dists = np.min(
                [np.sum((subset_lab - c) ** 2, axis=1) for c in candidates_lab], axis=0
            )
            next_idx = int(np.argmax(dists))
            candidates_lab.append(subset_lab[next_idx])

    # Also include the overall median / mean color of the scene
    overall_mean_lab = np.median(lab, axis=0)
    candidates_lab.append(overall_mean_lab)

    # Deduplicate candidate centroids by Delta E
    unique_centroids: List[NDArray[np.float32]] = []
    for cand in candidates_lab:
        if not unique_centroids:
            unique_centroids.append(cand)
            continue
        min_de = min(np.sqrt(np.sum((cand - u) ** 2)) for u in unique_centroids)
        if min_de >= PALETTE_DELTA_E_THRESHOLD:
            unique_centroids.append(cand)
        if len(unique_centroids) >= num_colors:
            break

    # If still fewer than num_colors, use standard median-cut RGB fallback for remainder
    if len(unique_centroids) < num_colors:
        quantized = pil_img.quantize(colors=num_colors, method=Image.Quantize.MEDIANCUT)
        raw_palette = quantized.getpalette() or []
        palette_slice = raw_palette[: num_colors * 3]
        for i in range(0, len(palette_slice), 3):
            r_c, g_c, b_c = (
                palette_slice[i] / 255.0,
                palette_slice[i + 1] / 255.0,
                palette_slice[i + 2] / 255.0,
            )
            lab_c = rgb_to_lab(np.array([[[r_c, g_c, b_c]]], dtype=np.float32)).reshape(3)
            if not any(
                np.sqrt(np.sum((lab_c - u) ** 2)) < (PALETTE_DELTA_E_THRESHOLD / 2.0)
                for u in unique_centroids
            ):
                unique_centroids.append(lab_c)
            if len(unique_centroids) >= num_colors:
                break

    # Convert chosen centroids back to RGB
    hex_list: List[str] = []
    for c in unique_centroids[:num_colors]:
        c_lab = c.reshape(1, 1, 3).astype(np.float32)
        c_rgb = lab_to_rgb(c_lab).reshape(3)
        c_uint8 = np.clip(np.round(c_rgb * 255.0), 0, 255).astype(np.uint8)
        hex_str = f"#{c_uint8[0]:02X}{c_uint8[1]:02X}{c_uint8[2]:02X}"
        if hex_str not in hex_list:
            hex_list.append(hex_str)

    # Pad if necessary for extreme degenerate cases
    while len(hex_list) < num_colors:
        if hex_list:
            hex_list.append(hex_list[-1])
        else:
            hex_list.append("#000000")

    return hex_list[:num_colors]
