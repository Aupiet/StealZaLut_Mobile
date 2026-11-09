"""Tests for LightCraft Auto Edit feature in stealzalut.core.auto_edit."""

import os
import numpy as np
from PIL import Image

from stealzalut.core.auto_edit import (
    calculate_auto_exposure,
    get_auto_edit_settings,
    apply_auto_edit,
    generate_auto_edit_lut,
    srgb_to_linear,
    linear_to_srgb,
    DEFAULT_AUTO_CONTRAST,
    DEFAULT_AUTO_HIGHLIGHTS,
    DEFAULT_AUTO_SHADOWS,
    DEFAULT_AUTO_WHITES,
    DEFAULT_AUTO_BLACKS,
    DEFAULT_AUTO_VIBRANCE,
    DEFAULT_AUTO_SATURATION,
)
from stealzalut.bridge import compute_auto_edit, export_auto_edit_cube


def test_srgb_linear_roundtrip():
    """Verify sRGB <-> linear conversion invertibility."""
    vals = np.linspace(0.0, 1.0, 100, dtype=np.float32)
    lin = srgb_to_linear(vals)
    srgb = linear_to_srgb(lin)
    np.testing.assert_allclose(srgb, vals, atol=1e-5)


def test_auto_exposure_gray_points():
    """Verify calculated auto exposure matches reverse-engineered LightCraft values."""
    test_cases = [
        (16, 1.52),
        (32, 0.91),
        (64, 0.16),
        (96, -0.33),
        (128, -0.69),
        (160, -0.98),
        (192, -1.22),
        (224, -1.42),
        (255, -1.59),
    ]

    for val_byte, expected_ev in test_cases:
        arr = np.full((64, 64, 3), val_byte, dtype=np.uint8)
        exp = calculate_auto_exposure(arr)
        assert abs(exp - expected_ev) <= 0.02, (
            f"For val={val_byte}: expected {expected_ev}, got {exp}"
        )


def test_auto_exposure_clamping():
    """Verify auto exposure respects min/max clamp limits [-2.0, 3.0]."""
    # Pure black
    black = np.zeros((32, 32, 3), dtype=np.uint8)
    exp_black = calculate_auto_exposure(black)
    assert exp_black == 3.0

    # Extremely bright
    white = np.full((32, 32, 3), 255, dtype=np.uint8)
    exp_white = calculate_auto_exposure(white)
    assert -2.0 <= exp_white <= 3.0


def test_get_auto_edit_settings():
    """Verify the default parameter set returned by get_auto_edit_settings."""
    img = np.full((32, 32, 3), 128, dtype=np.uint8)
    settings = get_auto_edit_settings(img)

    assert "exposure" in settings
    assert settings["contrast"] == DEFAULT_AUTO_CONTRAST
    assert settings["highlights"] == DEFAULT_AUTO_HIGHLIGHTS
    assert settings["shadows"] == DEFAULT_AUTO_SHADOWS
    assert settings["whites"] == DEFAULT_AUTO_WHITES
    assert settings["blacks"] == DEFAULT_AUTO_BLACKS
    assert settings["vibrance"] == DEFAULT_AUTO_VIBRANCE
    assert settings["saturation"] == DEFAULT_AUTO_SATURATION


def test_apply_auto_edit_range():
    """Verify apply_auto_edit produces valid float32 values in [0.0, 1.0]."""
    img = np.random.uniform(0.0, 1.0, (64, 64, 3)).astype(np.float32)
    edited = apply_auto_edit(img)

    assert edited.shape == img.shape
    assert edited.dtype == np.float32
    assert np.min(edited) >= 0.0
    assert np.max(edited) <= 1.0


def test_generate_auto_edit_lut():
    """Verify 3D LUT generation produces a valid cubic grid."""
    size = 17
    img = np.full((32, 32, 3), 100, dtype=np.uint8)
    lut = generate_auto_edit_lut(img, lut_size=size)

    assert lut.shape == (size, size, size, 3)
    assert lut.dtype == np.float32
    assert np.min(lut) >= 0.0
    assert np.max(lut) <= 1.0


def test_bridge_compute_and_export_auto_edit(tmp_path):
    """Test full bridge integration for computing auto edit and exporting .cube."""
    src_file = str(tmp_path / "test_src.png")
    preview_file = str(tmp_path / "preview.jpg")
    cube_file = str(tmp_path / "auto.cube")

    arr = np.full((100, 100, 3), 120, dtype=np.uint8)
    Image.fromarray(arr).save(src_file)

    # 1. Compute auto edit preview
    res_preview = compute_auto_edit(src_file, output_path=preview_file)
    assert res_preview["status"] == "success"
    assert os.path.isfile(preview_file)
    assert "exposure" in res_preview
    assert res_preview["contrast"] == 68.0

    # 2. Export auto edit 3D LUT (.cube)
    res_cube = export_auto_edit_cube(src_file, output_cube_path=cube_file, lut_size=17)
    assert res_cube["status"] == "success"
    assert os.path.isfile(cube_file)

    # Verify .cube file content
    with open(cube_file, "r", encoding="utf-8") as f:
        content = f.read()
    assert "LUT_3D_SIZE 17" in content
    assert "TITLE" in content

    # 3. Test apply_image_edits and compute_auto_settings
    from stealzalut.bridge import apply_image_edits, compute_auto_settings

    auto_res = compute_auto_settings(src_file)
    assert auto_res["status"] == "success"
    assert "exposure" in auto_res

    edit_res = apply_image_edits(
        src_file,
        output_path=os.path.join(str(tmp_path), "edited.jpg"),
        exposure=0.5,
        contrast=68.0,
        highlights=-66.0,
        shadows=52.0,
        vibrance=30.0,
    )
    assert edit_res["status"] == "success"
    assert os.path.isfile(edit_res["edited_path"])


if __name__ == "__main__":
    import tempfile
    from pathlib import Path

    print("Running auto_edit unit tests...")
    test_srgb_linear_roundtrip()
    print("[OK] test_srgb_linear_roundtrip passed")
    test_auto_exposure_gray_points()
    print("[OK] test_auto_exposure_gray_points passed")
    test_auto_exposure_clamping()
    print("[OK] test_auto_exposure_clamping passed")
    test_get_auto_edit_settings()
    print("[OK] test_get_auto_edit_settings passed")
    test_apply_auto_edit_range()
    print("[OK] test_apply_auto_edit_range passed")
    test_generate_auto_edit_lut()
    print("[OK] test_generate_auto_edit_lut passed")

    with tempfile.TemporaryDirectory() as td:
        test_bridge_compute_and_export_auto_edit(Path(td))
    print("[OK] test_bridge_compute_and_export_auto_edit passed")
    print("All tests successfully passed!")
