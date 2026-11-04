"""Tests for ``stealzalut.core.lut_export``.

Validates .cube file structure: header fields, data-line count, value ranges,
UTF-8 encoding, parametric size coverage, intensity baking into .cube files,
and pixel-for-pixel WYSIWYG consistency between preview and exported .cube.
"""

from pathlib import Path
import numpy as np
from PIL import Image
import pytest

try:
    from stealzalut.core.lut_export import export_cube  # type: ignore[import-untyped]
    from stealzalut.core.lut_engine import (  # type: ignore[import-untyped]
        apply_lut,
        generate_identity_lattice,
        refine_and_blend_lut,
    )
    from stealzalut.workers.export_worker import (  # type: ignore[import-untyped]
        run_export_pipeline,
        run_image_export_pipeline,
    )
    from stealzalut.workers.transfer_worker import (  # type: ignore[import-untyped]
        clear_lut_cache,
        get_or_load_source_proxy,
        run_transfer_pipeline,
    )
    from stealzalut.utils.image_io import load_image  # type: ignore[import-untyped]

    _HAS_LUT_EXPORT = True
except ImportError:
    _HAS_LUT_EXPORT = False

pytestmark = pytest.mark.skipif(
    not _HAS_LUT_EXPORT,
    reason="stealzalut.core.lut_export is not importable (missing dependency?)",
)


# ------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------


def _export_identity_cube(
    tmp_path: Path, size: int = 17, title: str = "Test"
) -> Path:
    """Generate an identity LUT and export it as a .cube file."""
    lut = generate_identity_lattice(size)
    out_path = tmp_path / f"identity_{size}.cube"
    export_cube(lut, str(out_path), size=size, title=title)
    return out_path


def _parse_cube(path: Path) -> tuple[list[str], list[list[float]]]:
    """Return (header_lines, data_rows) from a .cube file."""
    header: list[str] = []
    data: list[list[float]] = []
    with open(path, "r", encoding="utf-8") as fh:
        for line in fh:
            stripped = line.strip()
            if stripped == "" or stripped.startswith("#"):
                header.append(stripped)
                continue
            parts = stripped.split()
            if len(parts) == 3:
                try:
                    vals = [float(p) for p in parts]
                    data.append(vals)
                    continue
                except ValueError:
                    pass
            header.append(stripped)
    return header, data


# ------------------------------------------------------------------
# Header tests
# ------------------------------------------------------------------


class TestCubeFileHeader:
    """The exported .cube file must contain required header fields."""

    def test_cube_file_header(self, tmp_path: Path) -> None:
        cube_path = _export_identity_cube(tmp_path, size=17, title="MyLUT")
        text = cube_path.read_text(encoding="utf-8")

        assert "TITLE" in text, "Missing TITLE in .cube header"
        assert "LUT_3D_SIZE" in text, "Missing LUT_3D_SIZE in .cube header"
        assert "DOMAIN_MIN" in text, "Missing DOMAIN_MIN in .cube header"
        assert "DOMAIN_MAX" in text, "Missing DOMAIN_MAX in .cube header"


# ------------------------------------------------------------------
# Data line count
# ------------------------------------------------------------------


class TestCubeFileLineCount:
    """The number of data lines must equal size³."""

    def test_cube_file_line_count(self, tmp_path: Path) -> None:
        size = 17
        cube_path = _export_identity_cube(tmp_path, size=size)
        _, data_rows = _parse_cube(cube_path)

        expected = size**3
        assert len(data_rows) == expected, (
            f"Expected {expected} data lines, found {len(data_rows)}"
        )


# ------------------------------------------------------------------
# Value range
# ------------------------------------------------------------------


class TestCubeFileValues:
    """All data values must lie within [0.0, 1.0]."""

    def test_cube_file_values_in_range(self, tmp_path: Path) -> None:
        cube_path = _export_identity_cube(tmp_path, size=17)
        _, data_rows = _parse_cube(cube_path)

        all_values = np.array(data_rows)
        assert all_values.min() >= 0.0, (
            f"Value below 0.0 found: {all_values.min()}"
        )
        assert all_values.max() <= 1.0, (
            f"Value above 1.0 found: {all_values.max()}"
        )


# ------------------------------------------------------------------
# UTF-8 encoding
# ------------------------------------------------------------------


class TestCubeFileEncoding:
    """The file must be valid UTF-8."""

    def test_cube_file_encoding_utf8(self, tmp_path: Path) -> None:
        cube_path = _export_identity_cube(tmp_path, size=17)
        try:
            cube_path.read_text(encoding="utf-8")
        except UnicodeDecodeError as exc:
            pytest.fail(f".cube file is not valid UTF-8: {exc}")


# ------------------------------------------------------------------
# Parametric size coverage
# ------------------------------------------------------------------


class TestCubeExportSizes:
    """Export must work for various standard LUT sizes."""

    @pytest.mark.parametrize("size", [17, 33])
    def test_export_with_different_sizes(
        self, tmp_path: Path, size: int
    ) -> None:
        cube_path = _export_identity_cube(tmp_path, size=size)
        _, data_rows = _parse_cube(cube_path)

        expected = size**3
        assert len(data_rows) == expected, (
            f"Size {size}: expected {expected} data lines, got {len(data_rows)}"
        )

        all_values = np.array(data_rows)
        assert all_values.min() >= 0.0
        assert all_values.max() <= 1.0


# ------------------------------------------------------------------
# Intensity integration in exported .cube
# ------------------------------------------------------------------


class TestCubeIntensityIntegration:
    """With intensity = 0.5, exported .cube values must be exactly midway between identity and 1.0."""

    def test_cube_values_at_intensity_half(
        self,
        tmp_path: Path,
        sample_src_path: Path,
        sample_ref_path: Path,
    ) -> None:
        clear_lut_cache()
        cube_00 = str(tmp_path / "lut_00.cube")
        cube_05 = str(tmp_path / "lut_05.cube")
        cube_10 = str(tmp_path / "lut_10.cube")

        # Export at intensity 0.0 (identity)
        run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=cube_00,
            lut_size=17,
            intensity=0.0,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )

        # Export at intensity 0.5
        run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=cube_05,
            lut_size=17,
            intensity=0.5,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )

        # Export at intensity 1.0
        run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=cube_10,
            lut_size=17,
            intensity=1.0,
            skin_protection=0.0,
            smooth_sigma=0.0,
            neutral_lock=0.0,
        )

        _, data_00 = _parse_cube(Path(cube_00))
        _, data_05 = _parse_cube(Path(cube_05))
        _, data_10 = _parse_cube(Path(cube_10))

        arr_00 = np.array(data_00, dtype=np.float32)
        arr_05 = np.array(data_05, dtype=np.float32)
        arr_10 = np.array(data_10, dtype=np.float32)

        expected_midpoint = (arr_00 + arr_10) / 2.0
        np.testing.assert_allclose(arr_05, expected_midpoint, atol=1e-5)


# ------------------------------------------------------------------
# WYSIWYG consistency between preview and exported .cube
# ------------------------------------------------------------------


class TestWysiwygPreviewCubeConsistency:
    """Preview image must correspond pixel-for-pixel to applying the exported .cube to source."""

    def test_preview_matches_exported_cube_application(
        self,
        tmp_path: Path,
        sample_src_path: Path,
        sample_ref_path: Path,
    ) -> None:
        clear_lut_cache()

        preview_path = str(tmp_path / "preview.png")
        cube_path = str(tmp_path / "grade.cube")

        # 1. Generate preview image
        res_prev = run_transfer_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_path=preview_path,
            intensity=0.75,
            preserve_luminance=0.70,
            skin_protection=0.50,
            smooth_sigma=0.60,
            neutral_lock=0.80,
            lut_size=33,
            max_dimension=512,
            quality=100,
        )
        assert res_prev["status"] == "success"

        # 2. Export .cube with the exact same parameters
        res_cube = run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=cube_path,
            lut_size=33,
            intensity=0.75,
            preserve_luminance=0.70,
            skin_protection=0.50,
            smooth_sigma=0.60,
            neutral_lock=0.80,
        )
        assert res_cube["status"] == "success"

        # 3. Load the exported .cube file and parse into a 3D LUT
        _, cube_data = _parse_cube(Path(cube_path))
        cube_lut = np.array(cube_data, dtype=np.float32)

        # 4. Load the preview source proxy image
        src_work = get_or_load_source_proxy(str(sample_src_path), max_dimension=512)

        # 5. Apply the exported .cube to the source image
        cube_graded_image = apply_lut(src_work, cube_lut)

        # 6. Load the generated preview image
        generated_preview = load_image(preview_path)

        # 7. Compare pixel for pixel: with lossless PNG, maximum difference is 0 or <= 1 uint8 level
        diff = np.abs(cube_graded_image.astype(int) - generated_preview.astype(int))
        max_diff = int(np.max(diff))
        assert max_diff <= 1, (
            f"Preview does not match .cube: max pixel error = {max_diff}"
        )


# ------------------------------------------------------------------
# Cube Export Creation Contract (Tâche 1 & Tâche 3)
# ------------------------------------------------------------------


class TestCubeExportCreationContract:
    """Validate that cube export generates lut_path in JSON and valid disk file."""

    def test_export_worker_generates_valid_lut_path(
        self, sample_src_path: Path, sample_ref_path: Path, tmp_path: Path
    ) -> None:
        target_cube = str(tmp_path / "final_look.cube")
        res = run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=target_cube,
            lut_size=17,
        )

        assert res["status"] == "success"
        assert "lut_path" in res, "Expected 'lut_path' key in result dictionary"
        assert "cube_path" in res, "Expected 'cube_path' key in result dictionary"
        assert res["lut_path"] == str(Path(target_cube).resolve())

        # Physical file must exist and be non-empty
        assert Path(target_cube).is_file()
        assert Path(target_cube).stat().st_size > 0

    def test_run_image_export_pipeline_native_mode(
        self, sample_src_path: Path, sample_ref_path: Path, tmp_path: Path
    ) -> None:
        """Native mode (max_export_dim = 0 or None) must preserve source dimensions."""
        src_img = load_image(str(sample_src_path))
        orig_h, orig_w = src_img.shape[:2]

        target_img = str(tmp_path / "native_export.jpg")
        res = run_image_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_image_path=target_img,
            max_export_dim=0,  # Native
        )

        assert res["status"] == "success"
        assert res["width"] == orig_w
        assert res["height"] == orig_h

        exported = load_image(target_img)
        assert exported.shape[:2] == (orig_h, orig_w)


# ------------------------------------------------------------------
# Full 3-Stage Pipeline Baking & Cube Parser
# ------------------------------------------------------------------


class TestCubeBakingAndParser:
    """Validate 3-Stage pipeline baking into .cube and load_cube_file parsing."""

    def test_cube_parser_roundtrip(self, tmp_path: Path) -> None:
        from stealzalut.core.lut_export import load_cube_file

        cube_path = _export_identity_cube(tmp_path, size=17, title="RoundtripTest")
        parsed_size, parsed_lut = load_cube_file(str(cube_path))

        assert parsed_size == 17
        assert parsed_lut.shape == (17**3, 3)
        assert parsed_lut.min() >= 0.0
        assert parsed_lut.max() <= 1.0

    def test_full_pipeline_baking_in_export_worker(
        self, sample_src_path: Path, sample_ref_path: Path, tmp_path: Path
    ) -> None:
        from stealzalut.core.lut_export import load_cube_file

        target_cube = str(tmp_path / "baked_look.cube")
        res = run_export_pipeline(
            src_path=str(sample_src_path),
            ref_path=str(sample_ref_path),
            output_cube_path=target_cube,
            lut_size=17,
            intensity=0.85,
            luma_lock=0.20,
            neutral_axis_lock=0.40,
            skin_tone_lock=0.50,
            extremes_rolloff=0.15,
            spatial_3d=0.10,
            exposure=0.30,
            contrast=0.15,
            highlights=-0.20,
            shadows=0.10,
            temperature=0.05,
            tint=-0.02,
            vibrance=0.25,
            saturation=0.05,
        )

        assert res["status"] == "success"
        assert Path(target_cube).is_file()

        parsed_size, parsed_lut = load_cube_file(target_cube)
        assert parsed_size == 17
        assert parsed_lut.shape == (17**3, 3)
        assert not np.isnan(parsed_lut).any()
        assert not np.isinf(parsed_lut).any()
        assert np.min(parsed_lut) >= 0.0
        assert np.max(parsed_lut) <= 1.0
