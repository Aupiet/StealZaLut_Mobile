"""
Tests for ``stealzalut.core.lut_export``.

Validates .cube file structure: header fields, data-line count,
value ranges, UTF-8 encoding, and parametric size coverage.
"""

import numpy as np
import pytest
from pathlib import Path

# Graceful degradation
try:
    from stealzalut.core.lut_export import export_cube  # type: ignore[import-untyped]
    from stealzalut.core.lut_engine import generate_identity_lattice  # type: ignore[import-untyped]

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


def _export_identity_cube(tmp_path: Path, size: int = 17, title: str = "Test") -> Path:
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
            # Try to parse as a data line (three floats)
            parts = stripped.split()
            if len(parts) == 3:
                try:
                    vals = [float(p) for p in parts]
                    data.append(vals)
                    continue
                except ValueError:
                    pass
            # Otherwise it's a header/metadata line
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

        expected = size ** 3
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
        assert all_values.min() >= 0.0, f"Value below 0.0 found: {all_values.min()}"
        assert all_values.max() <= 1.0, f"Value above 1.0 found: {all_values.max()}"


# ------------------------------------------------------------------
# UTF-8 encoding
# ------------------------------------------------------------------


class TestCubeFileEncoding:
    """The file must be valid UTF-8."""

    def test_cube_file_encoding_utf8(self, tmp_path: Path) -> None:
        cube_path = _export_identity_cube(tmp_path, size=17)
        # If this succeeds without UnicodeDecodeError, the file is valid UTF-8.
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
    def test_export_with_different_sizes(self, tmp_path: Path, size: int) -> None:
        cube_path = _export_identity_cube(tmp_path, size=size)
        _, data_rows = _parse_cube(cube_path)

        expected = size ** 3
        assert len(data_rows) == expected, (
            f"Size {size}: expected {expected} data lines, got {len(data_rows)}"
        )

        all_values = np.array(data_rows)
        assert all_values.min() >= 0.0
        assert all_values.max() <= 1.0
