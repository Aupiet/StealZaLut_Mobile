"""Baseline benchmark for Python kernels:
1. Trilinear 3D LUT application (1024x768 proxy & 24 MP 6000x4000)
2. 2D Gaussian blur (24 MP 6000x4000, sigma=3.0)
Measures execution time and memory. Exports inputs and outputs for parity verification.
"""

import sys
import time
import os
import tracemalloc
import numpy as np
import scipy.ndimage

# Add python source to path
sys.path.insert(0, os.path.abspath("app/src/main/python"))
from stealzalut.core.lut_engine import apply_lut_trilinear, generate_identity_grid
from stealzalut.core.spatial_filters import gaussian_blur_2d

def make_test_lut(size=33):
    grid = generate_identity_grid(size)
    # Apply a non-linear contrast and warm tint transformation to the LUT
    lut = np.copy(grid)
    lut[..., 0] = np.clip(lut[..., 0] ** 0.85 * 1.05, 0.0, 1.0)
    lut[..., 1] = np.clip(lut[..., 1] ** 1.0, 0.0, 1.0)
    lut[..., 2] = np.clip(lut[..., 2] ** 1.15 * 0.95, 0.0, 1.0)
    return lut.astype(np.float32)

def benchmark():
    np.random.seed(42)
    lut33 = make_test_lut(33)
    lut65 = make_test_lut(65)
    
    print("=== BENCHMARK PYTHON (NumPy / SciPy) ===")
    
    # 1. Proxy 1024x768 LUT 33
    proxy_w, proxy_h = 1024, 768
    proxy_img = np.random.uniform(0.0, 1.0, (proxy_h, proxy_w, 3)).astype(np.float32)
    
    # Warmup
    _ = apply_lut_trilinear(proxy_img, lut33)
    
    times = []
    for _ in range(5):
        t0 = time.perf_counter()
        _ = apply_lut_trilinear(proxy_img, lut33)
        times.append((time.perf_counter() - t0) * 1000.0)
    print(f"Proxy 1024x768 LUT 33: avg={np.mean(times):.2f} ms (min={np.min(times):.2f} ms)")

    # 2. 24 MP image (6000x4000)
    w, h = 6000, 4000
    print(f"\nGenerating 24 MP synthetic image ({w}x{h})...")
    # Deterministic gradient pattern to test accuracy and edge cases
    x = np.linspace(0.0, 1.0, w, dtype=np.float32)
    y = np.linspace(0.0, 1.0, h, dtype=np.float32)
    xx, yy = np.meshgrid(x, y)
    img_24mp = np.stack([xx, yy, 1.0 - xx * 0.5 - yy * 0.5], axis=-1).astype(np.float32)
    
    # Save input sample (small crop or deterministic stats) for parity verification
    np.save("bench/input_24mp_crop.npy", img_24mp[:100, :100])
    np.save("bench/lut33.npy", lut33)

    # 3. 24 MP LUT 33 apply
    tracemalloc.start()
    t0 = time.perf_counter()
    out_lut33 = apply_lut_trilinear(img_24mp, lut33)
    t_lut = (time.perf_counter() - t0) * 1000.0
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    print(f"24 MP LUT 33: time={t_lut:.2f} ms, peak memory={peak / (1024*1024):.1f} MB")
    np.save("bench/py_out_lut_crop.npy", out_lut33[:100, :100])

    # 4. 24 MP LUT 65 apply
    t0 = time.perf_counter()
    _ = apply_lut_trilinear(img_24mp, lut65)
    t_lut65 = (time.perf_counter() - t0) * 1000.0
    print(f"24 MP LUT 65: time={t_lut65:.2f} ms")

    # 5. 24 MP Gaussian blur (sigma=3.0)
    tracemalloc.start()
    t0 = time.perf_counter()
    out_blur = gaussian_blur_2d(img_24mp, sigma=3.0)
    t_blur = (time.perf_counter() - t0) * 1000.0
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    print(f"24 MP Gaussian blur (sigma=3.0): time={t_blur:.2f} ms, peak memory={peak / (1024*1024):.1f} MB")
    np.save("bench/py_out_blur_crop.npy", out_blur[:100, :100])
    
    print("\nBenchmark completed. Reference outputs saved to bench/.")

if __name__ == "__main__":
    benchmark()
