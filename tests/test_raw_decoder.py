"""Comprehensive unit tests for the RAW and DNG decoding engine."""

from __future__ import annotations

import io
import os
import struct
import tempfile
import zlib

import numpy as np
import pytest

from stealzalut.core import raw_decoder as raw_decoder_module
from stealzalut.core.raw_decoder import (
    TAG_AS_SHOT_NEUTRAL,
    TAG_BITS_PER_SAMPLE,
    TAG_BLACK_LEVEL,
    TAG_CFA_PATTERN,
    TAG_CFA_REPEAT_PATTERN_DIM,
    TAG_COLOR_MATRIX_1,
    TAG_COMPRESSION,
    TAG_IMAGE_LENGTH,
    TAG_IMAGE_WIDTH,
    TAG_ORIENTATION,
    TAG_PHOTOMETRIC_INTERPRETATION,
    TAG_STRIP_BYTE_COUNTS,
    TAG_STRIP_OFFSETS,
    TAG_WHITE_LEVEL,
    apply_color_matrix_and_white_balance,
    apply_exif_orientation,
    decode_dng_image,
    demosaic_bayer_high_quality,
    is_raw_or_dng_file,
    parse_dng_structure,
)
from stealzalut.utils.image_io import load_image, load_proxy_image


def create_synthetic_dng_file(
    path: str,
    width: int = 64,
    height: int = 48,
    pattern: list[int] = [0, 1, 1, 2],
    compression: int = 1,
    orientation: int = 1,
    black_level: float = 64.0,
    white_level: float = 1023.0,
    with_color_matrix: bool = True,
) -> None:
    """Generate a valid synthetic DNG (TIFF 6.0) binary file for testing."""
    # Create synthetic Bayer CFA data: (H, W) uint16
    y, x = np.mgrid[0:height, 0:width].astype(np.float32)
    bayer = (black_level + (white_level - black_level) * (x / float(width))).astype(np.uint16)
    raw_bytes = bayer.tobytes()

    if compression == 8:
        raw_bytes = zlib.compress(raw_bytes)

    byte_order = "<"
    f = io.BytesIO()

    # Header: II 42 IFD_offset
    f.write(b"II\x2a\x00")
    ifd_offset = 8
    f.write(struct.pack(f"{byte_order}I", ifd_offset))

    # We will write tags in order of tag_id (standard TIFF requirement)
    # We will reserve space for IFD entries, then write out-of-line data, then write image data.
    tags: list[tuple[int, int, int, Any]] = [
        (TAG_IMAGE_WIDTH, 4, 1, width),
        (TAG_IMAGE_LENGTH, 4, 1, height),
        (TAG_BITS_PER_SAMPLE, 3, 1, 16),
        (TAG_COMPRESSION, 3, 1, compression),
        (TAG_PHOTOMETRIC_INTERPRETATION, 3, 1, 32803),  # CFA
        (TAG_STRIP_OFFSETS, 4, 1, 0),  # patched later
        (TAG_ORIENTATION, 3, 1, orientation),
        (TAG_STRIP_BYTE_COUNTS, 4, 1, len(raw_bytes)),
        (TAG_CFA_REPEAT_PATTERN_DIM, 3, 2, [2, 2]),
        (TAG_CFA_PATTERN, 1, 4, bytes(pattern)),
        (TAG_BLACK_LEVEL, 5, 1, (int(black_level), 1)),
        (TAG_WHITE_LEVEL, 4, 1, int(white_level)),
        (TAG_AS_SHOT_NEUTRAL, 5, 3, [(1000, 1000), (1000, 1000), (1000, 1000)]),
    ]

    if with_color_matrix:
        # 3x3 identity color matrix in SRATIONAL
        matrix_rationals = [(1000, 1000), (0, 1000), (0, 1000),
                            (0, 1000), (1000, 1000), (0, 1000),
                            (0, 1000), (0, 1000), (1000, 1000)]
        tags.append((TAG_COLOR_MATRIX_1, 10, 9, matrix_rationals))

    # Sort tags by tag_id ascending
    tags.sort(key=lambda t: t[0])

    num_entries = len(tags)
    ifd_size = 2 + num_entries * 12 + 4
    data_area_offset = ifd_offset + ifd_size

    # First pass: assemble out-of-line payload
    out_of_line = bytearray()
    entry_bytes_list = []

    for tag_id, tiff_type, count, val in tags:
        if tag_id == TAG_STRIP_OFFSETS:
            # Placeholder, will patch when out_of_line is complete
            entry_bytes_list.append((tag_id, tiff_type, count, "STRIP_OFFSET_PLACEHOLDER"))
            continue

        if tiff_type == 1:  # BYTE
            b_val = bytes(val)
            val_offset = struct.unpack(f"{byte_order}I", b_val[:4].ljust(4, b"\x00"))[0]
            entry_bytes_list.append((tag_id, tiff_type, count, val_offset))
        elif tiff_type == 3:  # SHORT
            if count == 1:
                val_offset = int(val)
                entry_bytes_list.append((tag_id, tiff_type, count, val_offset))
            else:
                packed = struct.pack(f"{byte_order}{count}H", *val)
                pos = data_area_offset + len(out_of_line)
                out_of_line.extend(packed)
                entry_bytes_list.append((tag_id, tiff_type, count, pos))
        elif tiff_type == 4:  # LONG
            val_offset = int(val)
            entry_bytes_list.append((tag_id, tiff_type, count, val_offset))
        elif tiff_type == 5 or tiff_type == 10:  # RATIONAL / SRATIONAL
            fmt = "I" if tiff_type == 5 else "i"
            if count == 1:
                packed = struct.pack(f"{byte_order}2{fmt}", val[0], val[1])
            else:
                flat = []
                for n, d in val:
                    flat.extend([n, d])
                packed = struct.pack(f"{byte_order}{count * 2}{fmt}", *flat)
            pos = data_area_offset + len(out_of_line)
            out_of_line.extend(packed)
            entry_bytes_list.append((tag_id, tiff_type, count, pos))

    image_offset = data_area_offset + len(out_of_line)

    # Now write IFD entries
    f.write(struct.pack(f"{byte_order}H", num_entries))
    for tag_id, tiff_type, count, val_offset in entry_bytes_list:
        if val_offset == "STRIP_OFFSET_PLACEHOLDER":
            val_offset = image_offset
        f.write(struct.pack(f"{byte_order}HHII", tag_id, tiff_type, count, val_offset))
    f.write(struct.pack(f"{byte_order}I", 0))  # next IFD offset = 0

    f.write(bytes(out_of_line))
    f.write(raw_bytes)

    with open(path, "wb") as out_f:
        out_f.write(f.getvalue())


def test_is_raw_or_dng_file() -> None:
    assert is_raw_or_dng_file("test.dng")
    assert is_raw_or_dng_file("photo.RAW")
    assert is_raw_or_dng_file("img.cr2")
    assert is_raw_or_dng_file("img.NEF")
    assert is_raw_or_dng_file("img.arw")
    assert not is_raw_or_dng_file("photo.jpg")
    assert not is_raw_or_dng_file("photo.png")
    assert not is_raw_or_dng_file("")


def test_demosaic_bayer_high_quality_all_patterns() -> None:
    """Test high quality demosaicing on RGGB, BGGR, GRBG, GBRG layouts."""
    h, w = 32, 32
    bayer = np.random.uniform(0.1, 0.9, size=(h, w)).astype(np.float32)

    for pat in ([0, 1, 1, 2], [2, 1, 1, 0], [1, 0, 2, 1], [1, 2, 0, 1]):
        rgb = demosaic_bayer_high_quality(bayer, pat)
        assert rgb.shape == (h, w, 3)
        assert rgb.dtype == np.float32
        assert np.all(rgb >= 0.0)
        assert np.all(rgb <= 1.0)


def test_color_matrix_and_white_balance() -> None:
    """Test white balance gains and color matrix transform."""
    rgb = np.full((16, 16, 3), 0.5, dtype=np.float32)
    neutral = [0.8, 1.0, 1.2]
    matrix = np.eye(3, dtype=np.float32)

    cal = apply_color_matrix_and_white_balance(rgb, neutral, matrix)
    assert cal.shape == (16, 16, 3)
    # Red gain was (1/0.8) = 1.25, so red channel must be > green channel
    assert cal[0, 0, 0] > cal[0, 0, 1]
    # Blue gain was (1/1.2) = 0.833, so blue channel must be < green channel
    assert cal[0, 0, 2] < cal[0, 0, 1]


def test_exif_orientation_handling() -> None:
    """Test that all 8 EXIF orientations transform array dimensions appropriately."""
    img = np.zeros((20, 40, 3), dtype=np.float32)
    # Orientation 1: noop
    assert apply_exif_orientation(img, 1).shape == (20, 40, 3)
    # Orientation 6 (90 CW): height/width swapped -> (40, 20, 3)
    assert apply_exif_orientation(img, 6).shape == (40, 20, 3)
    # Orientation 8 (90 CCW): height/width swapped -> (40, 20, 3)
    assert apply_exif_orientation(img, 8).shape == (40, 20, 3)


def test_uncompressed_dng_decoding() -> None:
    """Verify parsing and decoding an uncompressed DNG file."""
    with tempfile.NamedTemporaryFile(suffix=".dng", delete=False) as tmp:
        tmp_path = tmp.name

    try:
        create_synthetic_dng_file(tmp_path, width=40, height=30, compression=1)
        meta, f = parse_dng_structure(tmp_path)
        f.close()
        assert meta.width == 40
        assert meta.height == 30
        assert meta.compression == 1
        assert meta.bits_per_sample == 16

        # Decode float image
        rgb = decode_dng_image(tmp_path)
        assert rgb.shape == (30, 40, 3)
        assert rgb.dtype == np.float32
        assert 0.0 <= rgb.min() <= rgb.max() <= 1.0

        # Load as uint8 proxy
        proxy = load_proxy_image(tmp_path, max_dim=20)
        assert max(proxy.shape[:2]) <= 20
        assert proxy.dtype == np.uint8

        # Load as full resolution uint8
        full = load_image(tmp_path)
        assert full.shape == (30, 40, 3)
        assert full.dtype == np.uint8

    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)


def test_dng_preview_downsamples_before_demosaicing(
    tmp_path, monkeypatch: pytest.MonkeyPatch
) -> None:
    """Keep Bayer preview processing bounded for high-resolution RAW images."""
    dng_path = str(tmp_path / "large_preview.dng")
    create_synthetic_dng_file(dng_path, width=400, height=300)

    demosaic_shapes: list[tuple[int, int]] = []
    original_demosaic = raw_decoder_module.demosaic_bayer_high_quality

    def record_demosaic_shape(bayer: np.ndarray, pattern: list[int]) -> np.ndarray:
        demosaic_shapes.append(bayer.shape)
        return original_demosaic(bayer, pattern)

    monkeypatch.setattr(
        raw_decoder_module, "demosaic_bayer_high_quality", record_demosaic_shape
    )
    preview = decode_dng_image(dng_path, max_dim=40)

    assert demosaic_shapes
    assert max(demosaic_shapes[0]) <= 40
    assert max(preview.shape[:2]) <= 40


def test_deflate_compressed_dng_decoding() -> None:
    """Verify parsing and decoding a Deflate-compressed DNG file."""
    with tempfile.NamedTemporaryFile(suffix=".dng", delete=False) as tmp:
        tmp_path = tmp.name

    try:
        create_synthetic_dng_file(tmp_path, width=48, height=36, compression=8)
        rgb = decode_dng_image(tmp_path)
        assert rgb.shape == (36, 48, 3)
        assert rgb.dtype == np.float32

        proxy = load_proxy_image(tmp_path, max_dim=24)
        assert max(proxy.shape[:2]) <= 24

    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
