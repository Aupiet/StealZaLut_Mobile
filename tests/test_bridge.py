"""
Unit tests for stealzalut.bridge.

Validates that all bridge functions handle errors gracefully without unhandled crashes,
and always return a structured dictionary with status, error_code, and message.
"""

import os
import sys
import tempfile
from pathlib import Path

# Ensure stealzalut package is importable
sys.path.insert(
    0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python")
)

import numpy as np
from PIL import Image

from stealzalut.bridge import (
    export_cube_file,
    export_graded_image,
    extract_palette,
    process_preview,
)


def _create_sample_image(path: str, color=(120, 80, 200), size=(64, 64)):
    img = Image.new("RGB", size, color)
    img.save(path, format="JPEG")


def test_missing_files_error_codes():
    """Verify that nonexistent input files return structured errors with proper codes."""
    nonexistent_src = "C:/nonexistent/source_file_12345.jpg"
    nonexistent_ref = "C:/nonexistent/ref_file_12345.jpg"

    # 1. process_preview
    r1 = process_preview(src_path=nonexistent_src, ref_path=nonexistent_ref)
    assert r1["status"] == "error"
    assert r1["error_code"] == "SOURCE_NOT_FOUND"
    assert "message" in r1

    with tempfile.TemporaryDirectory() as tmp_dir:
        valid_src = os.path.join(tmp_dir, "valid_src.jpg")
        _create_sample_image(valid_src)

        r2 = process_preview(src_path=valid_src, ref_path=nonexistent_ref)
        assert r2["status"] == "error"
        assert r2["error_code"] == "REFERENCE_NOT_FOUND"

        # 2. export_cube_file
        out_cube = os.path.join(tmp_dir, "test.cube")
        r3 = export_cube_file(src_path=nonexistent_src, ref_path=valid_src, output_cube_path=out_cube)
        assert r3["status"] == "error"
        assert r3["error_code"] == "SOURCE_NOT_FOUND"

        r4 = export_cube_file(src_path=valid_src, ref_path=nonexistent_ref, output_cube_path=out_cube)
        assert r4["status"] == "error"
        assert r4["error_code"] == "REFERENCE_NOT_FOUND"

        # 3. export_graded_image
        out_img = os.path.join(tmp_dir, "graded.jpg")
        r5 = export_graded_image(src_path=nonexistent_src, ref_path=valid_src, output_image_path=out_img)
        assert r5["status"] == "error"
        assert r5["error_code"] == "SOURCE_NOT_FOUND"

        r6 = export_graded_image(src_path=valid_src, ref_path=nonexistent_ref, output_image_path=out_img)
        assert r6["status"] == "error"
        assert r6["error_code"] == "REFERENCE_NOT_FOUND"

        # 4. extract_palette
        r8 = extract_palette(image_path=nonexistent_src)
        assert r8["status"] == "error"
        assert r8["error_code"] == "SOURCE_NOT_FOUND"
        assert r8["palette"] == []


def test_corrupted_file_handling():
    """Verify that unreadable or corrupted files return error dictionaries instead of crashing."""
    with tempfile.TemporaryDirectory() as tmp_dir:
        corrupted_path = os.path.join(tmp_dir, "corrupted.jpg")
        with open(corrupted_path, "wb") as f:
            f.write(b"NOT_A_VALID_JPEG_HEADER_RANDOM_GARBAGE_1234567890")

        valid_src = os.path.join(tmp_dir, "valid_src.jpg")
        _create_sample_image(valid_src)

        # Corrupted source image
        r1 = process_preview(src_path=corrupted_path, ref_path=valid_src)
        assert r1["status"] == "error"
        assert "error_code" in r1
        assert "message" in r1

        # Corrupted in extract_palette
        r2 = extract_palette(image_path=corrupted_path)
        assert r2["status"] == "error"
        assert "error_code" in r2
        assert r2["palette"] == []


def test_nominal_bridge_calls():
    """Verify nominal execution of all bridge operations."""
    with tempfile.TemporaryDirectory() as tmp_dir:
        src_path = os.path.join(tmp_dir, "source.jpg")
        ref_path = os.path.join(tmp_dir, "ref.jpg")
        _create_sample_image(src_path, color=(40, 100, 200))
        _create_sample_image(ref_path, color=(220, 140, 50))

        # 1. process_preview
        out_preview = os.path.join(tmp_dir, "preview.jpg")
        r_prev = process_preview(src_path=src_path, ref_path=ref_path, output_preview_path=out_preview)
        assert r_prev["status"] == "success"
        assert os.path.isfile(r_prev["preview_path"])

        # 2. export_cube_file (17^3)
        out_cube = os.path.join(tmp_dir, "lut.cube")
        r_cube = export_cube_file(src_path=src_path, ref_path=ref_path, output_cube_path=out_cube, lut_size=17)
        assert r_cube["status"] == "success"
        assert os.path.isfile(r_cube["cube_path"])
        with open(r_cube["cube_path"], "r", encoding="utf-8") as f:
            content = f.read()
            assert "LUT_3D_SIZE 17" in content

        # 3. export_graded_image
        out_img = os.path.join(tmp_dir, "graded.jpg")
        r_img = export_graded_image(src_path=src_path, ref_path=ref_path, output_image_path=out_img)
        assert r_img["status"] == "success"
        assert os.path.isfile(r_img["image_path"])

        # 4. extract_palette
        r_pal = extract_palette(image_path=src_path, num_colors=6)
        assert r_pal["status"] == "success"
        assert len(r_pal["palette"]) == 6


if __name__ == "__main__":
    test_missing_files_error_codes()
    test_corrupted_file_handling()
    test_nominal_bridge_calls()
    print("All bridge tests passed successfully!")
