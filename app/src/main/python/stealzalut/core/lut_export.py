"""Adobe .cube LUT file exporter and parser.

Writes 3-D LUT data in the industry-standard Adobe Cube LUT 1.0
format understood by DaVinci Resolve, Adobe Premiere, Final Cut Pro,
Lightroom, and all professional color grading software.
"""

from __future__ import annotations

import os
from typing import Tuple, Union

import numpy as np
from numpy.typing import NDArray


DEFAULT_CUBE_TITLE: str = "StealZaLut_Export"


def export_cube(
    lut_data: NDArray[Union[np.float32, np.float64]],
    output_path: str,
    size: int = 33,
    title: str = DEFAULT_CUBE_TITLE,
) -> str:
    """Export 3D LUT data as an industry-standard Adobe ``.cube`` file.

    Follows the canonical Adobe Cube LUT specification:
    - Domain bounds: [0.0 0.0 0.0] to [1.0 1.0 1.0].
    - Iteration order: Red (fastest / innermost), Green (middle), Blue (slowest / outermost).
    - Floating point values formatted to 6 decimal places.
    - Strict validation: checks for size^3 entries, absence of NaN/Inf, and bounds clipping.

    Args:
        lut_data: Array of shape ``(size ** 3, 3)`` or ``(size, size, size, 3)`` in [0.0, 1.0].
        output_path: Destination file path for the ``.cube`` file.
        size: LUT grid dimension that was used to produce *lut_data*.
        title: Embedded TITLE header string.

    Returns:
        The absolute path of the written ``.cube`` file.

    Raises:
        ValueError: If *lut_data* dimension does not match size^3 entries.
    """
    expected_rows: int = size ** 3

    if lut_data.ndim == 4:
        if lut_data.shape != (size, size, size, 3):
            raise ValueError(
                f"lut_data 4D shape {lut_data.shape} does not match expected "
                f"({size}, {size}, {size}, 3)."
            )
        # Flattening 4D [b, g, r, :] in C order guarantees R varies fastest
        flat_data = lut_data.reshape(-1, 3)
    elif lut_data.ndim == 2:
        if lut_data.shape != (expected_rows, 3):
            raise ValueError(
                f"lut_data shape {lut_data.shape} does not match expected "
                f"({expected_rows}, 3) for LUT size {size}."
            )
        flat_data = lut_data
    else:
        raise ValueError(
            f"Invalid lut_data shape {lut_data.shape}; expected 2D ({expected_rows}, 3) "
            f"or 4D ({size}, {size}, {size}, 3)."
        )

    # Sanitize: replace NaNs and Infs, then clamp strictly to [0.0, 1.0]
    sanitized = np.nan_to_num(flat_data, copy=True, nan=0.0, posinf=1.0, neginf=0.0)
    clamped_data = np.clip(sanitized, 0.0, 1.0).astype(np.float32)

    # Standard Adobe .cube header
    header_lines: list[str] = [
        f'TITLE "{title}"',
        "# Created by StealZaLUT Mobile",
        f"LUT_3D_SIZE {size}",
        "DOMAIN_MIN 0.0 0.0 0.0",
        "DOMAIN_MAX 1.0 1.0 1.0",
        "",
    ]
    header: str = "\n".join(header_lines)

    os.makedirs(os.path.dirname(os.path.abspath(output_path)) or ".", exist_ok=True)

    with open(output_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(header)
        np.savetxt(fh, clamped_data, fmt="%.6f", delimiter=" ")

    # Verification: file exists, is non-empty, and conforms strictly to specification
    abs_path = os.path.abspath(output_path)
    if not os.path.isfile(abs_path) or os.path.getsize(abs_path) == 0:
        raise OSError(f"Exported .cube file is missing or empty: {output_path}")

    # Lightweight streaming verification to avoid heavy list/float allocations on mobile devices
    header_found = False
    data_lines_count = 0
    with open(abs_path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            if stripped.startswith("LUT_3D_SIZE"):
                parts = stripped.split()
                if len(parts) >= 2 and int(parts[1]) == size:
                    header_found = True
                continue
            if stripped.startswith("TITLE") or stripped.startswith("DOMAIN_"):
                continue
            parts = stripped.split()
            if len(parts) == 3:
                data_lines_count += 1

    if not header_found or data_lines_count != expected_rows:
        raise ValueError(
            f"Integrity check failed for {output_path}: expected {expected_rows} lines "
            f"for size {size}, but parsed {data_lines_count} lines (header valid: {header_found})."
        )

    return abs_path


def load_cube_file(cube_path: str) -> Tuple[int, NDArray[np.float32]]:
    """Parse and load an Adobe ``.cube`` 3D LUT file.

    Args:
        cube_path: Path to the .cube file.

    Returns:
        Tuple of (lut_size, lut_data) where lut_data is float32 of shape (size ** 3, 3)
        with values clamped in [0.0, 1.0].

    Raises:
        FileNotFoundError: If the file does not exist.
        ValueError: If parsing fails or LUT_3D_SIZE is missing.
    """
    if not os.path.isfile(cube_path):
        raise FileNotFoundError(f"LUT cube file not found: {cube_path}")

    lut_size: int = 0
    data_lines: list[list[float]] = []

    with open(cube_path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue

            parts = stripped.split()
            if parts[0].upper() == "LUT_3D_SIZE" and len(parts) >= 2:
                lut_size = int(parts[1])
                continue

            if len(parts) == 3:
                try:
                    r, g, b = float(parts[0]), float(parts[1]), float(parts[2])
                    data_lines.append([r, g, b])
                except ValueError:
                    continue

    if lut_size <= 0:
        # Infer size from row count
        total = len(data_lines)
        if total == 0:
            raise ValueError(f"Empty or header-only .cube file with no data: {cube_path}")
        inferred = int(round(total ** (1.0 / 3.0)))
        if inferred > 1 and inferred**3 == total:
            lut_size = inferred
        else:
            raise ValueError(f"Could not determine valid LUT_3D_SIZE from file: {cube_path}")

    expected = lut_size**3
    if expected == 0 or len(data_lines) != expected:
        raise ValueError(
            f"Expected {expected} entries for size {lut_size}, but parsed {len(data_lines)} in {cube_path}."
        )

    lut_arr = np.array(data_lines, dtype=np.float32)
    lut_arr = np.nan_to_num(lut_arr, copy=False, nan=0.0, posinf=1.0, neginf=0.0)
    return lut_size, np.clip(lut_arr, 0.0, 1.0)
