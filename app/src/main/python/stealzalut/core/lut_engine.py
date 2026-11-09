"""3-D LUT generation and application engine with professional refinement.

Builds an identity lattice, computes raw look transformation, refines the
3D lattice (skin tone protection, neutral axis lock, separable 3D Gaussian
smoothing, and intensity blending), and applies the exact 3D LUT to images
via vectorized trilinear interpolation.
"""

from __future__ import annotations

from typing import Union

import numpy as np
from numpy.typing import NDArray
from PIL import Image

from stealzalut.config import (
    DEFAULT_INTENSITY,
    DEFAULT_LUT_SIZE,
    DEFAULT_METHOD,
    DEFAULT_NEUTRAL_LOCK,
    DEFAULT_PRESERVE_LUMINANCE,
    DEFAULT_SKIN_PROTECTION,
    DEFAULT_SMOOTH_SIGMA,
    NEUTRAL_LOCK_SIGMA,
    SKIN_CB_CENTER,
    SKIN_CB_SIGMA,
    SKIN_CR_CENTER,
    SKIN_CR_SIGMA,
)
from stealzalut.utils.image_io import to_float32, to_uint8


def generate_identity_lattice(size: int = DEFAULT_LUT_SIZE) -> NDArray[np.float32]:
    """Create a 3-D identity LUT lattice.

    The lattice contains ``size ** 3`` RGB triplets whose values span
    linearly from 0.0 to 1.0. The ordering follows the Adobe ``.cube``
    convention: **R varies fastest**, then G, then B.

    Args:
        size: Number of grid points per channel axis (e.g. 17, 33, 65).

    Returns:
        Float32 array of shape ``(size ** 3, 3)`` with values in [0.0, 1.0].
    """
    axis: NDArray[np.float32] = np.linspace(0.0, 1.0, size, dtype=np.float32)

    # Build meshgrid with B as slowest (outermost) and R as fastest (innermost).
    # With indexing='ij' and (b, g, r) order, C-order flattening yields R-fastest.
    b, g, r = np.meshgrid(axis, axis, axis, indexing="ij")

    lattice: NDArray[np.float32] = np.stack([r.ravel(), g.ravel(), b.ravel()], axis=-1)
    return lattice


def generate_identity_grid(size: int = DEFAULT_LUT_SIZE) -> NDArray[np.float32]:
    """Create a 3-D identity LUT grid of shape ``(size, size, size, 3)``.

    Coordinates ``[b, g, r]`` map to RGB vector ``[r_val, g_val, b_val]``.

    Args:
        size: Number of grid points per channel axis.

    Returns:
        Float32 array of shape ``(size, size, size, 3)`` in [0.0, 1.0].
    """
    axis: NDArray[np.float32] = np.linspace(0.0, 1.0, size, dtype=np.float32)
    b, g, r = np.meshgrid(axis, axis, axis, indexing="ij")
    return np.stack([r, g, b], axis=-1)


def _gaussian_smooth_3d(
    grid: NDArray[np.float32],
    sigma: float = DEFAULT_SMOOTH_SIGMA,
) -> NDArray[np.float32]:
    """Apply a light separable 3-D Gaussian spatial filter in pure NumPy.

    Convolves sequentially along axis 0, 1, and 2 using edge padding so that
    extremities (0.0 and 1.0) do not bleed inwards. Eliminates gradient
    breaks (banding / posterisation) and ensures monotonic transitions.

    Args:
        grid: Float32 array of shape ``(N, N, N, 3)``.
        sigma: Standard deviation of Gaussian kernel. If <= 1e-4, returns a copy.

    Returns:
        Smoothed float32 array of shape ``(N, N, N, 3)``.
    """
    if sigma <= 1e-4:
        return grid.copy()

    radius = max(1, int(np.ceil(2.0 * sigma)))
    x = np.arange(-radius, radius + 1, dtype=np.float32)
    kernel = np.exp(-0.5 * (x / sigma) ** 2)
    kernel = kernel / np.sum(kernel)
    k_len = len(kernel)

    d, h, w, _ = grid.shape
    out = grid.copy()

    # Axis 0 (B dimension)
    padded = np.pad(out, ((radius, radius), (0, 0), (0, 0), (0, 0)), mode="edge")
    out = sum(kernel[i] * padded[i : i + d] for i in range(k_len))

    # Axis 1 (G dimension)
    padded = np.pad(out, ((0, 0), (radius, radius), (0, 0), (0, 0)), mode="edge")
    out = sum(kernel[i] * padded[:, i : i + h] for i in range(k_len))

    # Axis 2 (R dimension)
    padded = np.pad(out, ((0, 0), (0, 0), (radius, radius), (0, 0)), mode="edge")
    out = sum(kernel[i] * padded[:, :, i : i + w] for i in range(k_len))

    return out.astype(np.float32)


def _compute_skin_mask(
    identity_grid: NDArray[np.float32],
) -> NDArray[np.float32]:
    """Compute a continuous, smooth 3-D skin tone mask on the identity grid.

    Converts identity RGB lattice points to ITU-R BT.601 YCbCr space and evaluates
    bivariate Gaussian distance from the universal human skin tone cluster,
    weighted by R > G > B and midtone luminance constraints.

    Args:
        identity_grid: Float32 array of shape ``(N, N, N, 3)`` in [0.0, 1.0].

    Returns:
        Float32 array of shape ``(N, N, N, 1)`` with weights in [0.0, 1.0].
    """
    r = identity_grid[..., 0]
    g = identity_grid[..., 1]
    b = identity_grid[..., 2]

    # ITU-R BT.601 RGB to YCbCr
    y = 0.299 * r + 0.587 * g + 0.114 * b
    cb = -0.168736 * r - 0.331264 * g + 0.5 * b + 0.5
    cr = 0.5 * r - 0.418688 * g - 0.081312 * b + 0.5

    # Gaussian distance from typical skin chrominance
    d_sq = ((cb - SKIN_CB_CENTER) / SKIN_CB_SIGMA) ** 2 + (
        (cr - SKIN_CR_CENTER) / SKIN_CR_SIGMA
    ) ** 2
    chroma_mask = np.exp(-0.5 * d_sq)

    # Smooth physiological R > G > B ordering
    rg_smooth = np.clip((r - g) / 0.05, 0.0, 1.0)
    gb_smooth = np.clip((g - b) / 0.05, 0.0, 1.0)

    # Luminance window: exclude pure crushed blacks (< 0.08) and blown whites (> 0.98)
    y_window = np.clip((y - 0.08) / 0.08, 0.0, 1.0) * np.clip(
        (0.98 - y) / 0.08, 0.0, 1.0
    )

    mask = chroma_mask * rg_smooth * gb_smooth * y_window
    return mask[..., np.newaxis].astype(np.float32)


def refine_and_blend_lut(
    raw_lut: NDArray[np.float32],
    intensity: float = DEFAULT_INTENSITY,
    skin_protection: float = DEFAULT_SKIN_PROTECTION,
    smooth_sigma: float = DEFAULT_SMOOTH_SIGMA,
    neutral_lock: float = DEFAULT_NEUTRAL_LOCK,
) -> NDArray[np.float32]:
    """Post-process and refine raw 3-D LUT lattice directly in the 3D volume.

    Executes the following operations in sequence:
    1. Generates neutral identity LUT.
    2. Protects human skin tones by smoothly blending towards identity.
    3. Locks pure blacks (0,0,0) and whites (1,1,1) / neutral axis to prevent color casts.
    4. Applies separable 3D Gaussian smoothing to prevent banding and posterisation.
    5. Blends final intensity directly into the 3D grid values.

    Args:
        raw_lut: Raw 3D LUT array, either 4-D ``(N, N, N, 3)`` or 2-D ``(N ** 3, 3)``.
        intensity: Overall LUT strength in [0.0, 1.0]. 0.0 yields identity; 1.0 full effect.
        skin_protection: Protection strength for skin tones in [0.0, 1.0].
        smooth_sigma: Gaussian smoothing radius (0.0 disables smoothing).
        neutral_lock: Neutral axis and black/white locking strength in [0.0, 1.0].

    Returns:
        Refined LUT with shape matching *raw_lut* and values clipped to [0.0, 1.0].
    """
    is_2d = raw_lut.ndim == 2
    if is_2d:
        total_points = raw_lut.shape[0]
        size = int(round(total_points ** (1.0 / 3.0)))
        if size**3 != total_points:
            raise ValueError(f"raw_lut row count {total_points} is not a cubic number.")
        lut_3d = raw_lut.reshape((size, size, size, 3)).copy().astype(np.float32)
    elif raw_lut.ndim == 4:
        size = raw_lut.shape[0]
        lut_3d = raw_lut.copy().astype(np.float32)
    else:
        raise ValueError(
            f"Expected 2-D (N^3, 3) or 4-D (N, N, N, 3) LUT, got {raw_lut.shape}"
        )

    # 1. Identity LUT
    identity_3d = generate_identity_grid(size)

    # Fast path: intensity == 0.0 strictly returns identity
    clamped_intensity = float(np.clip(intensity, 0.0, 1.0))
    if clamped_intensity == 0.0:
        return identity_3d.reshape(-1, 3) if is_2d else identity_3d

    # 2. Protection of Skin Tones
    clamped_skin = float(np.clip(skin_protection, 0.0, 1.0))
    if clamped_skin > 0.0:
        skin_mask = _compute_skin_mask(identity_3d)
        skin_blend = np.clip(clamped_skin * skin_mask, 0.0, 1.0)
        lut_3d = (1.0 - skin_blend) * lut_3d + skin_blend * identity_3d

    # 3. Neutral Axis / Black & White Lock
    clamped_lock = float(np.clip(neutral_lock, 0.0, 1.0))
    if clamped_lock > 0.0:
        r_coords = identity_3d[..., 0]
        g_coords = identity_3d[..., 1]
        b_coords = identity_3d[..., 2]

        # Normalized distance to (0,0,0) and (1,1,1)
        sqrt_3 = float(np.sqrt(3.0))
        d_black = np.sqrt(r_coords**2 + g_coords**2 + b_coords**2) / sqrt_3
        d_white = (
            np.sqrt(
                (1.0 - r_coords) ** 2 + (1.0 - g_coords) ** 2 + (1.0 - b_coords) ** 2
            )
            / sqrt_3
        )

        w_black = np.exp(-0.5 * (d_black / NEUTRAL_LOCK_SIGMA) ** 2)
        w_white = np.exp(-0.5 * (d_white / NEUTRAL_LOCK_SIGMA) ** 2)
        w_neutral = np.clip(clamped_lock * np.maximum(w_black, w_white), 0.0, 1.0)[
            ..., np.newaxis
        ]

        lut_3d = (1.0 - w_neutral) * lut_3d + w_neutral * identity_3d

    # 4. 3D Spatial Grid Smoothing
    if smooth_sigma > 1e-4:
        lut_3d = _gaussian_smooth_3d(lut_3d, sigma=smooth_sigma)

    # 5. Direct Intensity Integration
    final_3d = (1.0 - clamped_intensity) * identity_3d + clamped_intensity * lut_3d
    final_3d = np.clip(final_3d, 0.0, 1.0).astype(np.float32)

    return final_3d.reshape(-1, 3) if is_2d else final_3d


def apply_lut_trilinear(
    image: NDArray[Union[np.float32, np.float64]],
    lut: NDArray[Union[np.float32, np.float64]],
) -> NDArray[np.float32]:
    """Apply a 3-D LUT to an RGB float image via vectorized trilinear interpolation.

    Args:
        image: Floating-point array of shape ``(..., 3)`` with values in [0.0, 1.0].
        lut: 3D LUT array of shape ``(N, N, N, 3)`` or ``(N ** 3, 3)``.

    Returns:
        Interpolated float32 array of shape matching *image* in [0.0, 1.0].
    """
    if lut.ndim == 4:
        size = lut.shape[0]
        lut_flat = lut.reshape(-1, 3).astype(np.float32)
    elif lut.ndim == 2:
        size = int(round(lut.shape[0] ** (1.0 / 3.0)))
        lut_flat = lut.astype(np.float32)
    else:
        raise ValueError(f"Expected 2-D or 4-D LUT, got shape {lut.shape}")

    scale = float(size - 1)
    img_f = np.clip(image.astype(np.float32), 0.0, 1.0)

    # Channels: r is dim2 (fastest in .cube), g is dim1, b is dim0 (slowest in .cube)
    r = img_f[..., 0] * scale
    g = img_f[..., 1] * scale
    b = img_f[..., 2] * scale

    x0 = np.clip(np.floor(r).astype(np.int32), 0, size - 2)
    dx = (r - x0)[..., np.newaxis]

    y0 = np.clip(np.floor(g).astype(np.int32), 0, size - 2)
    dy = (g - y0)[..., np.newaxis]

    z0 = np.clip(np.floor(b).astype(np.int32), 0, size - 2)
    dz = (b - z0)[..., np.newaxis]

    n2 = size * size
    base = z0 * n2 + y0 * size + x0

    # 1. Trilinear stage: Interpolate along X (fastest axis) for 4 Y-Z lines
    c00 = (1.0 - dx) * lut_flat[base] + dx * lut_flat[base + 1]
    c01 = (1.0 - dx) * lut_flat[base + size] + dx * lut_flat[base + size + 1]
    c10 = (1.0 - dx) * lut_flat[base + n2] + dx * lut_flat[base + n2 + 1]
    c11 = (1.0 - dx) * lut_flat[base + n2 + size] + dx * lut_flat[base + n2 + size + 1]

    # 2. Stage: Interpolate along Y
    c0 = (1.0 - dy) * c00 + dy * c01
    c1 = (1.0 - dy) * c10 + dy * c11

    # 3. Stage: Interpolate along Z
    out = (1.0 - dz) * c0 + dz * c1
    return np.clip(out, 0.0, 1.0).astype(np.float32)


LUT_CHUNK_PIXEL_LIMIT: int = 2_097_152  # 2 Megapixels per chunk (~24 MB peak RAM)


def apply_lut(
    image: NDArray[Union[np.uint8, np.float32, np.float64]],
    lut: NDArray[Union[np.float32, np.float64]],
    chunk_limit: int = LUT_CHUNK_PIXEL_LIMIT,
) -> NDArray[Union[np.uint8, np.float32]]:
    """Apply a 3-D LUT to an RGB image, preserving the input data type.

    For high-resolution images (> 2 MP, e.g. 12 MP - 50 MP HD exports), applies
    row-based chunked processing to strictly constrain mobile RAM peak consumption
    and eliminate Out-Of-Memory (OOM) failures.

    Because trilinear LUT interpolation is a purely point-wise pixel transformation,
    chunking guarantees 100% bit-exact parity with zero seam or boundary artifacts.

    Args:
        image: RGB array of shape ``(H, W, 3)``, dtype uint8 or float.
        lut: 3D LUT array of shape ``(N, N, N, 3)`` or ``(N ** 3, 3)``.
        chunk_limit: Maximum pixels processed per chunk (default 2M pixels).

    Returns:
        Graded array with the same shape and dtype as *image*.
    """
    is_uint8 = image.dtype == np.uint8
    h, w = image.shape[:2]

    if h * w <= chunk_limit:
        img_f = to_float32(image) if is_uint8 else image.astype(np.float32)
        out_f = apply_lut_trilinear(img_f, lut)
        return to_uint8(out_f) if is_uint8 else out_f

    # Large image chunked processing (preserves memory on mobile)
    out = np.empty_like(image, dtype=np.uint8 if is_uint8 else np.float32)
    chunk_rows = max(32, chunk_limit // w)

    for y_start in range(0, h, chunk_rows):
        y_end = min(y_start + chunk_rows, h)
        chunk = image[y_start:y_end]
        chunk_f = to_float32(chunk) if is_uint8 else chunk.astype(np.float32)
        chunk_out_f = apply_lut_trilinear(chunk_f, lut)
        if is_uint8:
            out[y_start:y_end] = to_uint8(chunk_out_f)
        else:
            out[y_start:y_end] = chunk_out_f

    return out


def generate_lut_from_pair(
    source: Union[NDArray[np.uint8], Image.Image],
    reference: Union[NDArray[np.uint8], Image.Image],
    lut_size: int = DEFAULT_LUT_SIZE,
    method: str = DEFAULT_METHOD,
    sample_size: int = 512,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = False,
) -> NDArray[np.float32]:
    """Generate a 3-D colour LUT from a source and reference image pair.

    Sample size defaults to 512x512 with LANCZOS downsampling to preserve
    small bright lights (neon signs, red beacons, windows) without dynamic compression.
    """
    if isinstance(source, np.ndarray):
        pil_src = Image.fromarray(source)
    else:
        pil_src = source.copy()

    if isinstance(reference, np.ndarray):
        pil_ref = Image.fromarray(reference)
    else:
        pil_ref = reference.copy()

    resample_mode = getattr(Image, "Resampling", Image).LANCZOS

    if sample_size is not None and sample_size > 0:
        pil_src.thumbnail((sample_size, sample_size), resample_mode)
        pil_ref.thumbnail((sample_size, sample_size), resample_mode)

    src_sample = np.asarray(pil_src, dtype=np.uint8)
    ref_sample = np.asarray(pil_ref, dtype=np.uint8)

    identity: NDArray[np.float32] = generate_identity_lattice(lut_size)

    from stealzalut.core.color_transfer import fit_and_apply_transfer

    transferred = fit_and_apply_transfer(
        src_img=src_sample,
        ref_img=ref_sample,
        target_img=identity,
        method=method,
        preserve_luminance=preserve_luminance,
        soft_clip=soft_clip,
    )

    # Reshape and apply light pre-smoothing to prevent any discrete stepping
    grid = transferred.reshape((lut_size, lut_size, lut_size, 3))
    grid = _gaussian_smooth_3d(grid, sigma=0.25)
    return np.clip(grid.reshape(-1, 3), 0.0, 1.0).astype(np.float32)


def compute_lut(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    size: int = DEFAULT_LUT_SIZE,
    method: str = DEFAULT_METHOD,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = False,
) -> NDArray[np.float32]:
    """Compute a raw 3-D colour LUT by transferring colour onto an identity lattice.

    Args:
        src_img: Source image (RGB uint8, (H, W, 3)).
        ref_img: Reference image (RGB uint8, (H, W, 3)).
        size: LUT grid size per channel (e.g. 17, 33, 65).
        method: Colour-transfer algorithm key.
        preserve_luminance: Luminance preservation weight in [0.0, 1.0].
        soft_clip: Whether to apply soft-knee roll-off.

    Returns:
        Transformed raw LUT as a float32 array of shape
        ``(size ** 3, 3)`` with values in [0.0, 1.0].
    """
    return generate_lut_from_pair(
        source=src_img,
        reference=ref_img,
        lut_size=size,
        method=method,
        sample_size=512,
        preserve_luminance=preserve_luminance,
        soft_clip=soft_clip,
    )
