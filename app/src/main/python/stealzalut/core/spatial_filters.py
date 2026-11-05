"""Pure NumPy and SciPy spatial 2D filtering routines.

Provides hardware-portable, vectorized 2D filters without OpenCV dependency:
- Gaussian blur
- Min filter (grayscale erosion)
- Bilinear remap coordinate interpolation
- Spatial 2D convolution
- Sobel gradient magnitude
- Laplacian filtering
- Bilinear zooming/resizing
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray
import scipy.ndimage


def gaussian_blur_2d(arr: NDArray[np.float32], sigma: float) -> NDArray[np.float32]:
    """2D Gaussian blur across spatial dimensions with reflect boundary."""
    if sigma <= 0.01:
        return arr
    if arr.ndim == 3:
        return scipy.ndimage.gaussian_filter(arr, sigma=(sigma, sigma, 0.0), mode="reflect")
    return scipy.ndimage.gaussian_filter(arr, sigma=sigma, mode="reflect")


def min_filter_2d(arr: NDArray[np.float32], size: int) -> NDArray[np.float32]:
    """2D Minimum filter (grayscale erosion) with reflect boundary."""
    k_size = max(1, size)
    return scipy.ndimage.minimum_filter(arr, size=k_size, mode="reflect")


def remap_2d(arr: NDArray[np.float32], map_x: NDArray[np.float32], map_y: NDArray[np.float32]) -> NDArray[np.float32]:
    """Bilinear spatial interpolation from 2D coordinate maps."""
    coords = [map_y, map_x]
    if arr.ndim == 3:
        channels = [
            scipy.ndimage.map_coordinates(arr[..., c], coords, order=1, mode="reflect")
            for c in range(arr.shape[2])
        ]
        return np.stack(channels, axis=-1).astype(np.float32)
    return scipy.ndimage.map_coordinates(arr, coords, order=1, mode="reflect").astype(np.float32)


def filter_2d(arr: NDArray[np.float32], kernel: NDArray[np.float32]) -> NDArray[np.float32]:
    """2D Spatial convolution across image channels."""
    if arr.ndim == 3:
        channels = [
            scipy.ndimage.convolve(arr[..., c], kernel, mode="reflect")
            for c in range(arr.shape[2])
        ]
        return np.stack(channels, axis=-1).astype(np.float32)
    return scipy.ndimage.convolve(arr, kernel, mode="reflect").astype(np.float32)


def sobel_magnitude_2d(gray: NDArray[np.float32]) -> NDArray[np.float32]:
    """Compute 2D Sobel gradient magnitude for edge masking."""
    gx = scipy.ndimage.sobel(gray, axis=1, mode="reflect") / 8.0
    gy = scipy.ndimage.sobel(gray, axis=0, mode="reflect") / 8.0
    return np.hypot(gx, gy).astype(np.float32)


def laplacian_2d(arr: NDArray[np.float32]) -> NDArray[np.float32]:
    """2D Laplacian filter for high-frequency roughness."""
    if arr.ndim == 3:
        channels = [
            scipy.ndimage.laplace(arr[..., c], mode="reflect")
            for c in range(arr.shape[2])
        ]
        return np.stack(channels, axis=-1).astype(np.float32)
    return scipy.ndimage.laplace(arr, mode="reflect").astype(np.float32)


def resize_2d(arr: NDArray[np.float32], out_w: int, out_h: int) -> NDArray[np.float32]:
    """Bilinear resize using SciPy zoom."""
    in_h, in_w = arr.shape[:2]
    if in_h == out_h and in_w == out_w:
        return arr
    scale_y = out_h / in_h
    scale_x = out_w / in_w
    if arr.ndim == 3:
        return scipy.ndimage.zoom(arr, (scale_y, scale_x, 1.0), order=1).astype(np.float32)
    return scipy.ndimage.zoom(arr, (scale_y, scale_x), order=1).astype(np.float32)
