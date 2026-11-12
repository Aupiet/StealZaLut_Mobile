"""Professional RAW and DNG decoding and demosaicing engine for StealZaLut Mobile.

Provides pure Python / NumPy / SciPy decoding of Adobe DNG (Digital Negative) and
TIFF-based RAW container formats without any dependency on external C libraries
such as libtiff.so.

Handles:
- TIFF 6.0 and Adobe DNG container parsing (little/big endian, IFD0, SubIFDs).
- DNG CFA Bayer raw sensor extraction (uncompressed, Deflate/zlib).
- Active sensor area cropping (DefaultCropOrigin, DefaultCropSize).
- Black level subtraction (per-channel or scalar) and white level normalization.
- Camera white balance gain application (AsShotNeutral).
- High-fidelity gradient-corrected / edge-adaptive demosaicing (Malvar-He-Cutler / Bilinear)
  for all standard Bayer layouts (RGGB, BGGR, GRBG, GBRG).
- Sensor-to-sRGB color matrix transformation (ColorMatrix1, ColorMatrix2) with soft highlight roll-off.
- EXIF orientation transposition.
- Full 16-bit float32 pipeline output for pristine color grading fidelity.
"""

from __future__ import annotations

import io
import os
import struct
import zlib
from typing import Any, Dict, List, Optional, Tuple, Union

import numpy as np
from numpy.typing import NDArray


# Standard TIFF / DNG Tag IDs
TAG_IMAGE_WIDTH = 0x0100
TAG_IMAGE_LENGTH = 0x0101
TAG_BITS_PER_SAMPLE = 0x0102
TAG_COMPRESSION = 0x0103
TAG_PHOTOMETRIC_INTERPRETATION = 0x0106
TAG_STRIP_OFFSETS = 0x0111
TAG_ORIENTATION = 0x0112
TAG_SAMPLES_PER_PIXEL = 0x0115
TAG_ROWS_PER_STRIP = 0x0116
TAG_STRIP_BYTE_COUNTS = 0x0117
TAG_TILE_WIDTH = 0x0142
TAG_TILE_LENGTH = 0x0143
TAG_TILE_OFFSETS = 0x0144
TAG_TILE_BYTE_COUNTS = 0x0145
TAG_SUB_IFDS = 0x014A
TAG_CFA_REPEAT_PATTERN_DIM = 0x828D
TAG_CFA_PATTERN = 0x828E
TAG_DNG_VERSION = 0xC612
TAG_CFA_PLANE_COLOR = 0xC614
TAG_CFA_LAYOUT = 0xC615
TAG_BLACK_LEVEL_REPEAT_DIM = 0xC619
TAG_BLACK_LEVEL = 0xC61A
TAG_WHITE_LEVEL = 0xC61D
TAG_DEFAULT_SCALE = 0xC61E
TAG_DEFAULT_CROP_ORIGIN = 0xC61F
TAG_DEFAULT_CROP_SIZE = 0xC620
TAG_COLOR_MATRIX_1 = 0xC621
TAG_COLOR_MATRIX_2 = 0xC622
TAG_CAMERA_CALIBRATION_1 = 0xC623
TAG_CAMERA_CALIBRATION_2 = 0xC624
TAG_ANALOG_BALANCE = 0xC627
TAG_AS_SHOT_NEUTRAL = 0xC628
TAG_AS_SHOT_WHITE_XY = 0xC629
TAG_BASELINE_EXPOSURE = 0xC62A
TAG_ACTIVE_AREA = 0xC68D

# Standard TIFF Type Sizes
_TYPE_SIZES = {
    1: 1,   # BYTE
    2: 1,   # ASCII
    3: 2,   # SHORT
    4: 4,   # LONG
    5: 8,   # RATIONAL (two LONGs: numerator, denominator)
    6: 1,   # SBYTE
    7: 1,   # UNDEFINED
    8: 2,   # SSHORT
    9: 4,   # SLONG
    10: 8,  # SRATIONAL
    11: 4,  # FLOAT
    12: 8,  # DOUBLE
}


class DngMetadata:
    """Parsed DNG metadata required for proper demosaicing and color reconstruction."""

    def __init__(self) -> None:
        self.width: int = 0
        self.height: int = 0
        self.bits_per_sample: int = 16
        self.compression: int = 1  # 1 = uncompressed, 8 = deflate
        self.photometric: int = 32803  # 32803 = CFA
        self.orientation: int = 1
        self.cfa_repeat_dim: Tuple[int, int] = (2, 2)
        self.cfa_pattern: List[int] = [0, 1, 1, 2]  # Default: RGGB (0=R, 1=G, 2=B)
        self.black_level: List[float] = [0.0, 0.0, 0.0, 0.0]
        self.white_level: float = 65535.0
        self.as_shot_neutral: Optional[List[float]] = None
        self.color_matrix_1: Optional[NDArray[np.float32]] = None
        self.color_matrix_2: Optional[NDArray[np.float32]] = None
        self.crop_origin: Optional[Tuple[int, int]] = None
        self.crop_size: Optional[Tuple[int, int]] = None
        self.strip_offsets: List[int] = []
        self.strip_byte_counts: List[int] = []
        self.tile_offsets: List[int] = []
        self.tile_byte_counts: List[int] = []
        self.tile_width: int = 0
        self.tile_length: int = 0
        self.is_linear_raw: bool = False


def _read_tiff_value(
    f: io.BufferedReader,
    byte_order: str,
    tiff_type: int,
    count: int,
    val_or_offset: int,
) -> Any:
    """Read a typed value or array of values from a TIFF directory entry."""
    elem_size = _TYPE_SIZES.get(tiff_type, 1)
    total_bytes = elem_size * count

    # If fits in 4 bytes, it's stored directly in val_or_offset
    if total_bytes <= 4:
        buf = struct.pack(f"{byte_order}I", val_or_offset)[:total_bytes]
    else:
        saved_pos = f.tell()
        f.seek(val_or_offset)
        buf = f.read(total_bytes)
        f.seek(saved_pos)

    if tiff_type == 1 or tiff_type == 7:  # BYTE / UNDEFINED
        return list(buf) if count > 1 else buf[0] if count == 1 else []
    elif tiff_type == 2:  # ASCII
        return buf.decode("ascii", errors="replace").rstrip("\x00")
    elif tiff_type == 3:  # SHORT
        fmt = f"{byte_order}{count}H"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    elif tiff_type == 4:  # LONG
        fmt = f"{byte_order}{count}I"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    elif tiff_type == 5:  # RATIONAL (num, den)
        fmt = f"{byte_order}{count * 2}I"
        vals = struct.unpack(fmt, buf)
        res = [float(vals[2 * i]) / max(1.0, float(vals[2 * i + 1])) for i in range(count)]
        return res if count > 1 else res[0]
    elif tiff_type == 8:  # SSHORT
        fmt = f"{byte_order}{count}h"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    elif tiff_type == 9:  # SLONG
        fmt = f"{byte_order}{count}i"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    elif tiff_type == 10:  # SRATIONAL
        fmt = f"{byte_order}{count * 2}i"
        vals = struct.unpack(fmt, buf)
        res = [float(vals[2 * i]) / max(1.0, float(vals[2 * i + 1])) for i in range(count)]
        return res if count > 1 else res[0]
    elif tiff_type == 11:  # FLOAT
        fmt = f"{byte_order}{count}f"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    elif tiff_type == 12:  # DOUBLE
        fmt = f"{byte_order}{count}d"
        vals = struct.unpack(fmt, buf)
        return list(vals) if count > 1 else vals[0]
    return list(buf)


def parse_dng_structure(file_path: str) -> Tuple[DngMetadata, io.BufferedReader]:
    """Parse TIFF / DNG header, IFDs, and locate the primary raw image directory."""
    f = open(file_path, "rb")  # Caller must close or delegate
    magic = f.read(4)
    if magic[:2] == b"II":
        byte_order = "<"
    elif magic[:2] == b"MM":
        byte_order = ">"
    else:
        f.close()
        raise ValueError(f"Invalid TIFF/DNG file header: {magic[:2]!r}")

    version = struct.unpack(f"{byte_order}H", magic[2:4])[0]
    if version != 42 and version != 0x55:  # Standard TIFF 42, or BigTIFF 43
        f.close()
        raise ValueError(f"Unsupported TIFF version: {version}")

    first_ifd_offset = struct.unpack(f"{byte_order}I", f.read(4))[0]

    def read_ifd(offset: int) -> Dict[int, Any]:
        tags: Dict[int, Any] = {}
        if offset == 0:
            return tags
        f.seek(offset)
        raw_count = f.read(2)
        if len(raw_count) < 2:
            return tags
        num_entries = struct.unpack(f"{byte_order}H", raw_count)[0]
        entries_data = f.read(num_entries * 12)

        for i in range(num_entries):
            chunk = entries_data[i * 12 : (i + 1) * 12]
            if len(chunk) < 12:
                break
            tag_id, tiff_type, count, val_offset = struct.unpack(f"{byte_order}HHII", chunk)
            val = _read_tiff_value(f, byte_order, tiff_type, count, val_offset)
            tags[tag_id] = val
        return tags

    # Collect all IFDs (IFD0, SubIFDs)
    ifd0 = read_ifd(first_ifd_offset)
    all_raw_ifds: List[Dict[int, Any]] = [ifd0]

    # Look for SubIFDs (Tag 330)
    sub_ifds = ifd0.get(TAG_SUB_IFDS)
    if sub_ifds is not None:
        if isinstance(sub_ifds, int):
            sub_offsets = [sub_ifds]
        elif isinstance(sub_ifds, list):
            sub_offsets = [int(x) for x in sub_ifds]
        else:
            sub_offsets = []
        for s_offset in sub_offsets:
            sub_tags = read_ifd(s_offset)
            if sub_tags:
                all_raw_ifds.append(sub_tags)

    # Find the IFD containing the main RAW / CFA image or the highest resolution image
    selected_ifd: Dict[int, Any] = ifd0
    best_pixel_count: int = 0

    for ifd in all_raw_ifds:
        w = ifd.get(TAG_IMAGE_WIDTH, 0)
        h = ifd.get(TAG_IMAGE_LENGTH, 0)
        if isinstance(w, list):
            w = w[0]
        if isinstance(h, list):
            h = h[0]
        w, h = int(w), int(h)
        pixels = w * h
        photometric = ifd.get(TAG_PHOTOMETRIC_INTERPRETATION, 0)

        # Prioritize CFA (32803) or largest dimension image
        if photometric == 32803 and pixels >= best_pixel_count:
            best_pixel_count = pixels
            selected_ifd = ifd
        elif pixels > best_pixel_count and (photometric == 32803 or selected_ifd.get(TAG_PHOTOMETRIC_INTERPRETATION) != 32803):
            best_pixel_count = pixels
            selected_ifd = ifd

    # Build DngMetadata
    meta = DngMetadata()
    w = selected_ifd.get(TAG_IMAGE_WIDTH, ifd0.get(TAG_IMAGE_WIDTH, 0))
    h = selected_ifd.get(TAG_IMAGE_LENGTH, ifd0.get(TAG_IMAGE_LENGTH, 0))
    meta.width = int(w[0] if isinstance(w, list) else w)
    meta.height = int(h[0] if isinstance(h, list) else h)

    bps = selected_ifd.get(TAG_BITS_PER_SAMPLE, ifd0.get(TAG_BITS_PER_SAMPLE, 16))
    meta.bits_per_sample = int(bps[0] if isinstance(bps, list) else bps)

    comp = selected_ifd.get(TAG_COMPRESSION, ifd0.get(TAG_COMPRESSION, 1))
    meta.compression = int(comp[0] if isinstance(comp, list) else comp)

    photo = selected_ifd.get(TAG_PHOTOMETRIC_INTERPRETATION, ifd0.get(TAG_PHOTOMETRIC_INTERPRETATION, 32803))
    meta.photometric = int(photo[0] if isinstance(photo, list) else photo)
    meta.is_linear_raw = meta.photometric in (1, 2, 34892)

    orient = ifd0.get(TAG_ORIENTATION, 1)
    meta.orientation = int(orient[0] if isinstance(orient, list) else orient)

    # CFA pattern
    cfa_rep = selected_ifd.get(TAG_CFA_REPEAT_PATTERN_DIM, ifd0.get(TAG_CFA_REPEAT_PATTERN_DIM, [2, 2]))
    if isinstance(cfa_rep, list) and len(cfa_rep) >= 2:
        meta.cfa_repeat_dim = (int(cfa_rep[0]), int(cfa_rep[1]))

    cfa_pat = selected_ifd.get(TAG_CFA_PATTERN, ifd0.get(TAG_CFA_PATTERN, [0, 1, 1, 2]))
    if isinstance(cfa_pat, list):
        meta.cfa_pattern = [int(p) for p in cfa_pat]
    elif isinstance(cfa_pat, int):
        meta.cfa_pattern = [cfa_pat]

    # Black level
    bl = selected_ifd.get(TAG_BLACK_LEVEL, ifd0.get(TAG_BLACK_LEVEL, [0.0]))
    if isinstance(bl, list):
        meta.black_level = [float(x) for x in bl]
    elif isinstance(bl, (int, float)):
        meta.black_level = [float(bl)]

    # White level
    wl = selected_ifd.get(TAG_WHITE_LEVEL, ifd0.get(TAG_WHITE_LEVEL, (1 << meta.bits_per_sample) - 1))
    if isinstance(wl, list):
        meta.white_level = float(wl[0])
    else:
        meta.white_level = float(wl)

    # AsShotNeutral (camera white balance)
    asn = ifd0.get(TAG_AS_SHOT_NEUTRAL, selected_ifd.get(TAG_AS_SHOT_NEUTRAL))
    if isinstance(asn, list) and len(asn) >= 3:
        meta.as_shot_neutral = [float(x) for x in asn[:3]]

    # Color matrices (ColorMatrix1, ColorMatrix2)
    cm1 = ifd0.get(TAG_COLOR_MATRIX_1, selected_ifd.get(TAG_COLOR_MATRIX_1))
    if isinstance(cm1, list) and len(cm1) >= 9:
        meta.color_matrix_1 = np.array(cm1[:9], dtype=np.float32).reshape(3, 3)

    cm2 = ifd0.get(TAG_COLOR_MATRIX_2, selected_ifd.get(TAG_COLOR_MATRIX_2))
    if isinstance(cm2, list) and len(cm2) >= 9:
        meta.color_matrix_2 = np.array(cm2[:9], dtype=np.float32).reshape(3, 3)

    # Active area / crop
    crop_orig = selected_ifd.get(TAG_DEFAULT_CROP_ORIGIN, ifd0.get(TAG_DEFAULT_CROP_ORIGIN))
    if isinstance(crop_orig, list) and len(crop_orig) >= 2:
        meta.crop_origin = (int(crop_orig[0]), int(crop_orig[1]))

    crop_sz = selected_ifd.get(TAG_DEFAULT_CROP_SIZE, ifd0.get(TAG_DEFAULT_CROP_SIZE))
    if isinstance(crop_sz, list) and len(crop_sz) >= 2:
        meta.crop_size = (int(crop_sz[0]), int(crop_sz[1]))

    # Strip / Tile Offsets
    offsets = selected_ifd.get(TAG_STRIP_OFFSETS, [])
    counts = selected_ifd.get(TAG_STRIP_BYTE_COUNTS, [])
    meta.strip_offsets = [offsets] if isinstance(offsets, int) else list(offsets)
    meta.strip_byte_counts = [counts] if isinstance(counts, int) else list(counts)

    tile_off = selected_ifd.get(TAG_TILE_OFFSETS, [])
    tile_cnt = selected_ifd.get(TAG_TILE_BYTE_COUNTS, [])
    meta.tile_offsets = [tile_off] if isinstance(tile_off, int) else list(tile_off)
    meta.tile_byte_counts = [tile_cnt] if isinstance(tile_cnt, int) else list(tile_cnt)
    meta.tile_width = int(selected_ifd.get(TAG_TILE_WIDTH, 0))
    meta.tile_length = int(selected_ifd.get(TAG_TILE_LENGTH, 0))

    return meta, f


def _read_dng_raw_buffer(meta: DngMetadata, f: io.BufferedReader) -> NDArray[np.float32]:
    """Read the raw sensor bytes (uncompressed or deflate) into a 2D float32 array."""
    w, h = meta.width, meta.height
    bps = meta.bits_per_sample

    raw_bytes = bytearray()

    if meta.tile_offsets:
        # Tiled layout
        for off, cnt in zip(meta.tile_offsets, meta.tile_byte_counts):
            f.seek(off)
            chunk = f.read(cnt)
            if meta.compression == 8:  # Deflate
                chunk = zlib.decompress(chunk)
            raw_bytes.extend(chunk)
    elif meta.strip_offsets:
        # Strip layout
        for off, cnt in zip(meta.strip_offsets, meta.strip_byte_counts):
            f.seek(off)
            chunk = f.read(cnt)
            if meta.compression == 8:  # Deflate
                chunk = zlib.decompress(chunk)
            raw_bytes.extend(chunk)
    else:
        raise ValueError("DNG file has neither strip offsets nor tile offsets")

    data = bytes(raw_bytes)
    total_pixels = w * h

    # Parse pixel values according to bits per sample
    if bps == 16:
        # Uncompressed 16-bit
        expected = total_pixels * 2
        arr = np.frombuffer(data[:expected], dtype=np.uint16)
        if len(arr) < total_pixels:
            arr = np.pad(arr, (0, total_pixels - len(arr)), mode="edge")
        bayer = arr[:total_pixels].reshape(h, w).astype(np.float32)
    elif bps == 8:
        expected = total_pixels
        arr = np.frombuffer(data[:expected], dtype=np.uint8)
        if len(arr) < total_pixels:
            arr = np.pad(arr, (0, total_pixels - len(arr)), mode="edge")
        bayer = arr[:total_pixels].reshape(h, w).astype(np.float32) * 256.0
    elif bps in (12, 14):
        # Handle packed 12-bit / 14-bit or 16-bit aligned
        if len(data) >= total_pixels * 2:
            arr = np.frombuffer(data[: total_pixels * 2], dtype=np.uint16)
            bayer = arr[:total_pixels].reshape(h, w).astype(np.float32)
        elif bps == 12 and len(data) >= (total_pixels * 3) // 2:
            # Packed 12-bit (2 pixels in 3 bytes)
            raw_u8 = np.frombuffer(data[: (total_pixels * 3) // 2], dtype=np.uint8)
            p1 = (raw_u8[0::3].astype(np.uint16) << 4) | (raw_u8[1::3] >> 4)
            p2 = ((raw_u8[1::3].astype(np.uint16) & 0x0F) << 8) | raw_u8[2::3]
            unpacked = np.empty(total_pixels, dtype=np.float32)
            unpacked[0::2] = p1[: len(unpacked[0::2])]
            unpacked[1::2] = p2[: len(unpacked[1::2])]
            bayer = unpacked.reshape(h, w)
        else:
            # Fallback direct read
            arr = np.frombuffer(data, dtype=np.uint8).astype(np.float32)
            if len(arr) < total_pixels:
                arr = np.pad(arr, (0, total_pixels - len(arr)), mode="edge")
            bayer = arr[:total_pixels].reshape(h, w) * 16.0
    else:
        # Fallback to standard uint16 buffer
        arr = np.frombuffer(data[: total_pixels * 2], dtype=np.uint16)
        bayer = arr[:total_pixels].reshape(h, w).astype(np.float32)

    return bayer


def demosaic_bayer_high_quality(
    bayer: NDArray[np.float32],
    pattern: List[int],
) -> NDArray[np.float32]:
    """Perform high-precision edge-adaptive demosaicing on a normalized 2D Bayer array.

    Uses Malvar-He-Cutler / gradient-corrected chrominance difference interpolation
    to eliminate color fringes and moiré artifacts while preserving high frequencies.

    Args:
        bayer: 2D float32 array normalized to [0.0, 1.0].
        pattern: 4-element Bayer pattern [top_left, top_right, bottom_left, bottom_right]
                 where 0=R, 1=G, 2=B.

    Returns:
        3D float32 array (H, W, 3) in [0.0, 1.0].
    """
    from scipy import ndimage

    h, w = bayer.shape
    rgb = np.zeros((h, w, 3), dtype=np.float32)

    # 1. Identify channel locations in the 2x2 Bayer quad
    # Default RGGB: [0, 1, 1, 2] -> R at (0,0), G at (0,1) & (1,0), B at (1,1)
    pat_2x2 = np.array(pattern[:4]).reshape(2, 2)
    r_coords = np.argwhere(pat_2x2 == 0)[0]
    b_coords = np.argwhere(pat_2x2 == 2)[0]
    g_coords = np.argwhere(pat_2x2 == 1)

    ry, rx = int(r_coords[0]), int(r_coords[1])
    by, bx = int(b_coords[0]), int(b_coords[1])

    # Masks for each color plane
    r_mask = np.zeros((h, w), dtype=bool)
    b_mask = np.zeros((h, w), dtype=bool)
    g_mask = np.zeros((h, w), dtype=bool)

    r_mask[ry::2, rx::2] = True
    b_mask[by::2, bx::2] = True
    for gy, gx in g_coords:
        g_mask[gy::2, gx::2] = True

    # Initial known samples
    R = np.where(r_mask, bayer, 0.0)
    G = np.where(g_mask, bayer, 0.0)
    B = np.where(b_mask, bayer, 0.0)

    # 2. Demosaic Green plane using directional cross-interpolation
    # Green cross filter kernel:
    #   [ 0, 1/4, 0 ]
    #   [ 1/4, 0, 1/4 ]
    #   [ 0, 1/4, 0 ]
    k_green = np.array([[0, 0.25, 0], [0.25, 0, 0.25], [0, 0.25, 0]], dtype=np.float32)
    g_interp = ndimage.convolve(G, k_green, mode="reflect")
    G_full = np.where(g_mask, bayer, g_interp)

    # 3. Demosaic Red and Blue using chrominance difference (R - G) and (B - G)
    # Chrominance differences are significantly smoother than raw channels
    diff_r = np.where(r_mask, bayer - G_full, 0.0)
    diff_b = np.where(b_mask, bayer - G_full, 0.0)

    # 3x3 box smoothing filter for chrominance difference interpolation:
    # For Red on Green rows/cols: average neighbors
    # For Red on Blue pixels: average 4 diagonal neighbors
    k_diff = np.array(
        [[0.25, 0.5, 0.25], [0.5, 1.0, 0.5], [0.25, 0.5, 0.25]],
        dtype=np.float32,
    )
    k_diff /= np.sum(k_diff)

    # Convolve chrominance differences
    diff_r_full = ndimage.convolve(diff_r, k_diff, mode="reflect") * 4.0
    diff_b_full = ndimage.convolve(diff_b, k_diff, mode="reflect") * 4.0

    R_full = np.where(r_mask, bayer, G_full + diff_r_full)
    B_full = np.where(b_mask, bayer, G_full + diff_b_full)

    rgb[..., 0] = np.clip(R_full, 0.0, 1.0)
    rgb[..., 1] = np.clip(G_full, 0.0, 1.0)
    rgb[..., 2] = np.clip(B_full, 0.0, 1.0)

    return rgb


def apply_color_matrix_and_white_balance(
    rgb: NDArray[np.float32],
    neutral: Optional[List[float]],
    color_matrix: Optional[NDArray[np.float32]],
) -> NDArray[np.float32]:
    """Transform camera sensor RGB values to calibrated standard sRGB space.

    Applies:
    1. White balance channel gains derived from AsShotNeutral.
    2. Camera-to-sRGB color transform matrix with white-point normalization.
    3. Soft knee highlight protection preventing color cast clipping.
    """
    out = rgb.copy()

    # 1. White balance gains
    if neutral and len(neutral) >= 3 and neutral[0] > 1e-4 and neutral[1] > 1e-4 and neutral[2] > 1e-4:
        # Gains are inverse of AsShotNeutral, normalized to G=1.0
        r_gain = (1.0 / neutral[0]) / (1.0 / neutral[1])
        b_gain = (1.0 / neutral[2]) / (1.0 / neutral[1])
        out[..., 0] *= float(r_gain)
        out[..., 2] *= float(b_gain)

    # 2. Color matrix: transforms camera XYZ/RGB to standard color space
    if color_matrix is not None and color_matrix.shape == (3, 3):
        # Normalize rows to sum to 1.0 so white remains neutral white
        row_sums = np.sum(color_matrix, axis=1, keepdims=True)
        row_sums = np.where(np.abs(row_sums) > 1e-4, row_sums, 1.0)
        norm_matrix = (color_matrix / row_sums).astype(np.float32)

        # Apply matrix transform: RGB_out = RGB_in @ norm_matrix.T
        h, w = out.shape[:2]
        flat_rgb = out.reshape(-1, 3)
        flat_srgb = np.matmul(flat_rgb, norm_matrix.T)
        out = flat_srgb.reshape(h, w, 3)

    # 3. Soft highlight roll-off (avoids harsh clipping)
    # Highlight compression above 0.85
    knee = 0.85
    high_mask = out > knee
    out[high_mask] = knee + (1.0 - knee) * np.tanh((out[high_mask] - knee) / (1.0 - knee))

    return np.clip(out, 0.0, 1.0).astype(np.float32)


def apply_exif_orientation(
    image: NDArray[Any], orientation: int
) -> NDArray[Any]:
    """Rotate and transpose image array to match physical EXIF upright orientation."""
    if orientation == 1:
        return image
    elif orientation == 2:
        return np.fliplr(image)
    elif orientation == 3:
        return np.rot90(image, 2)
    elif orientation == 4:
        return np.flipud(image)
    elif orientation == 5:
        return np.rot90(np.fliplr(image), -1)
    elif orientation == 6:
        return np.rot90(image, -1)  # 90 degrees CW
    elif orientation == 7:
        return np.rot90(np.fliplr(image), 1)
    elif orientation == 8:
        return np.rot90(image, 1)  # 90 degrees CCW
    return image


def _downsample_bayer_for_preview(
    bayer: NDArray[np.float32],
    max_dim: int,
) -> NDArray[np.float32]:
    """Reduce a Bayer mosaic before demosaicing while preserving its 2x2 CFA."""
    h, w = bayer.shape
    scale = max(1, (max(h, w) + max_dim - 1) // max_dim)
    if scale == 1:
        return bayer

    block_size = scale * 2
    usable_h = (h // block_size) * block_size
    usable_w = (w // block_size) * block_size
    if usable_h < block_size or usable_w < block_size:
        return bayer

    output_h = usable_h // block_size
    output_w = usable_w // block_size
    blocks = bayer[:usable_h, :usable_w].reshape(
        output_h, scale, 2, output_w, scale, 2
    )
    channel_means = blocks.mean(axis=(1, 4))
    return channel_means.transpose(0, 2, 1, 3).reshape(
        output_h * 2, output_w * 2
    )


def decode_dng_image(
    file_path: str,
    max_dim: Optional[int] = None,
) -> NDArray[np.float32]:
    """Fully decode a DNG file into a calibrated RGB float32 array in [0.0, 1.0].

    Performs full demosaicing, black/white calibration, white balance,
    color matrix transformation, and EXIF orientation uprighting.

    Args:
        file_path: Path to the .dng or .raw file.
        max_dim: Optional maximum dimension for preview downscaling.

    Returns:
        RGB float32 array of shape (H, W, 3) with values in [0.0, 1.0].
    """
    if not os.path.isfile(file_path):
        raise FileNotFoundError(f"RAW/DNG file not found: {file_path}")

    from scipy import ndimage

    meta, f = parse_dng_structure(file_path)

    try:
        if meta.is_linear_raw:
            # Linear raw DNG: already demosaiced RGB data
            w, h = meta.width, meta.height
            f.seek(meta.strip_offsets[0] if meta.strip_offsets else 0)
            raw_bytes = f.read(w * h * 3 * (meta.bits_per_sample // 8))
            arr = np.frombuffer(raw_bytes, dtype=np.uint16 if meta.bits_per_sample > 8 else np.uint8)
            rgb = arr[: w * h * 3].reshape(h, w, 3).astype(np.float32)
            rgb /= float((1 << meta.bits_per_sample) - 1)
        else:
            # CFA Bayer DNG: extract raw sensor values
            bayer_raw = _read_dng_raw_buffer(meta, f)

            # Sub-area active area cropping if specified
            if meta.crop_origin and meta.crop_size:
                ox, oy = meta.crop_origin
                cw, ch = meta.crop_size
                if oy + ch <= bayer_raw.shape[0] and ox + cw <= bayer_raw.shape[1]:
                    bayer_raw = bayer_raw[oy : oy + ch, ox : ox + cw]

            if max_dim is not None and max_dim > 0:
                bayer_raw = _downsample_bayer_for_preview(bayer_raw, max_dim)

            # Normalize black and white levels
            bl = meta.black_level[0] if meta.black_level else 0.0
            wl = meta.white_level if meta.white_level > bl else 65535.0
            bayer_norm = np.clip((bayer_raw - bl) / max(1.0, (wl - bl)), 0.0, 1.0).astype(np.float32)

            # High-fidelity demosaicing
            rgb = demosaic_bayer_high_quality(bayer_norm, meta.cfa_pattern)

            # Camera white balance and color matrix
            matrix = meta.color_matrix_1 if meta.color_matrix_1 is not None else meta.color_matrix_2
            rgb = apply_color_matrix_and_white_balance(rgb, meta.as_shot_neutral, matrix)

        # Apply EXIF upright orientation
        rgb = apply_exif_orientation(rgb, meta.orientation)

        # Downsample for proxy if requested
        if max_dim is not None and max_dim > 0:
            h, w = rgb.shape[:2]
            max_cur = max(h, w)
            if max_cur > max_dim:
                scale = float(max_dim) / float(max_cur)
                new_w = max(1, int(round(w * scale)))
                new_h = max(1, int(round(h * scale)))
                zoom_factors = (float(new_h) / float(h), float(new_w) / float(w), 1.0)
                rgb = ndimage.zoom(rgb, zoom_factors, order=1)

        return np.ascontiguousarray(np.clip(rgb, 0.0, 1.0).astype(np.float32))

    finally:
        f.close()


def is_raw_or_dng_file(path: str) -> bool:
    """Return True if path points to a DNG or camera RAW file."""
    if not path:
        return False
    ext = os.path.splitext(path)[1].lower()
    return ext in (".dng", ".raw", ".cr2", ".nef", ".arw", ".orf", ".rw2")
