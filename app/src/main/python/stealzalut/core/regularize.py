"""Regularisation of 3D LUT displacement fields.

This mirrors the desktop regularization path used to smooth folded displacement
fields and measure how much a LUT violates monotonicity in luminance.
"""

from __future__ import annotations

from typing import cast

import numpy as np
from numpy.typing import NDArray

F64 = NDArray[np.float64]
FOLD_TOLERANCE = 1e-3


def _gaussian_kernel(sigma: float) -> F64:
    """Create a 1D Gaussian kernel for separable smoothing."""
    radius = max(1, int(np.ceil(3.0 * sigma)))
    x = np.arange(-radius, radius + 1, dtype=np.float64)
    kernel = np.exp(-(x**2) / (2.0 * sigma**2))
    kernel /= kernel.sum()
    return cast("F64", kernel)


def smooth_field(field: F64, size: int, sigma_cells: float) -> F64:
    """Apply a separable Gaussian blur to a per-node 3D field."""
    if sigma_cells <= 0.0:
        return field.copy()

    channels = field.shape[1]
    volume = field.reshape(size, size, size, channels)
    kernel = _gaussian_kernel(sigma_cells)
    radius = (len(kernel) - 1) // 2

    for axis in range(3):
        pad = [(0, 0)] * 4
        pad[axis] = (radius, radius)
        padded = np.pad(volume, pad, mode="edge")
        acc = np.zeros_like(volume)
        for i, weight in enumerate(kernel):
            idx = np.arange(i, i + size)
            acc += weight * np.take(padded, idx, axis=axis)
        volume = acc

    return cast("F64", volume.reshape(-1, channels))


def fold_fraction(lightness: F64, size: int, tolerance: float = FOLD_TOLERANCE) -> float:
    """Return the fraction of grid edges where lightness decreases."""
    volume = lightness.reshape(size, size, size)
    violations = 0
    total = 3 * size * size * (size - 1)

    for axis in range(3):
        violations += int((np.diff(volume, axis=axis) < -tolerance).sum())

    if total == 0:
        return 0.0
    return float(violations / total)
