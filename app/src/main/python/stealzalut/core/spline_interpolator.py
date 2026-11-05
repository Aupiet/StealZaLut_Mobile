"""Monotone Cubic Hermite Spline Interpolator (Fritsch-Carlson).

Provides strictly monotonic spline evaluation for Lightroom Classic Tone Curves,
preventing non-monotonic overshoot and banding in 3D LUT lattice generation.
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

EPSILON: float = 1e-6


def evaluate_monotone_cubic_spline(
    x_nodes: NDArray[np.float32],
    y_nodes: NDArray[np.float32],
    x_eval: NDArray[np.float32],
) -> NDArray[np.float32]:
    """Evaluate a strictly monotone cubic spline interpolating given nodes.

    Prevents overshoot and oscillations, ensuring LUT values remain monotonic.
    """
    n = len(x_nodes)
    if n < 2:
        return x_eval.copy()

    # Differences & slopes
    dx = np.diff(x_nodes)
    dy = np.diff(y_nodes)
    slopes = dy / np.maximum(dx, EPSILON)

    # Tangents at internal nodes
    m = np.zeros(n, dtype=np.float32)
    m[0] = slopes[0]
    m[-1] = slopes[-1]
    for i in range(1, n - 1):
        if slopes[i - 1] * slopes[i] <= 0:
            m[i] = 0.0
        else:
            m[i] = (slopes[i - 1] + slopes[i]) / 2.0

    # Fritsch & Carlson monotonicity adjustments
    for i in range(n - 1):
        if abs(slopes[i]) < EPSILON:
            m[i] = 0.0
            m[i + 1] = 0.0
        else:
            alpha = m[i] / slopes[i]
            beta = m[i + 1] / slopes[i]
            dist = alpha**2 + beta**2
            if dist > 9.0:
                tau = 3.0 / np.sqrt(dist)
                m[i] = tau * alpha * slopes[i]
                m[i + 1] = tau * beta * slopes[i]

    # Evaluate piecewise Hermite polynomials
    flat_x = np.clip(x_eval.ravel(), x_nodes[0], x_nodes[-1])
    indices = np.searchsorted(x_nodes, flat_x) - 1
    indices = np.clip(indices, 0, n - 2)

    x0 = x_nodes[indices]
    x1 = x_nodes[indices + 1]
    y0 = y_nodes[indices]
    y1 = y_nodes[indices + 1]
    m0 = m[indices]
    m1 = m[indices + 1]
    h = x1 - x0

    t = (flat_x - x0) / np.maximum(h, EPSILON)
    t2 = t * t
    t3 = t2 * t

    h00 = 2.0 * t3 - 3.0 * t2 + 1.0
    h10 = t3 - 2.0 * t2 + t
    h01 = -2.0 * t3 + 3.0 * t2
    h11 = t3 - t2

    res = h00 * y0 + h10 * h * m0 + h01 * y1 + h11 * h * m1
    return np.clip(res.reshape(x_eval.shape), 0.0, 1.0).astype(np.float32)
