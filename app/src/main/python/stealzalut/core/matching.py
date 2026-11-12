"""Automatic matching between source and reference zones."""

from __future__ import annotations

from typing import cast

import numpy as np
from numpy.typing import NDArray

from stealzalut.core.palette import ZonePalette

F64 = NDArray[np.float64]

LIGHTNESS_WEIGHT = 2.0
CHROMA_WEIGHT = 0.5
SINKHORN_EPSILON = 0.05
SINKHORN_ITERATIONS = 500
MIN_TARGET_WEIGHT = 0.1


def zone_cost(
    source: ZonePalette,
    reference: ZonePalette,
    lightness_weight: float = LIGHTNESS_WEIGHT,
    chroma_weight: float = CHROMA_WEIGHT,
) -> F64:
    l_src = np.array([z.l_rank for z in source.zones], dtype=np.float64)
    l_ref = np.array([z.l_rank for z in reference.zones], dtype=np.float64)
    ab_src = np.array([z.mean[1:] for z in source.zones], dtype=np.float64)
    ab_ref = np.array([z.mean[1:] for z in reference.zones], dtype=np.float64)

    all_ab = np.vstack([ab_src, ab_ref])
    ab_scale = max(float(np.mean(np.sum((all_ab - all_ab.mean(axis=0)) ** 2, axis=1))), 1e-4)
    d_l = (l_src[:, None] - l_ref[None, :]) ** 2
    d_ab = np.sum((ab_src[:, None, :] - ab_ref[None, :, :]) ** 2, axis=2) / ab_scale
    return cast("F64", lightness_weight * d_l + chroma_weight * d_ab)


def _logsumexp(x: F64, axis: int) -> F64:
    peak = np.max(x, axis=axis, keepdims=True)
    total = peak + np.log(np.sum(np.exp(x - peak), axis=axis, keepdims=True))
    return cast("F64", total.squeeze(axis=axis))


def sinkhorn(
    cost: F64,
    a: F64,
    b: F64,
    epsilon: float = SINKHORN_EPSILON,
    iterations: int = SINKHORN_ITERATIONS,
) -> F64:
    eps = max(float(epsilon), 1e-6)
    log_a, log_b = np.log(a), np.log(b)
    f = np.zeros_like(a)
    g = np.zeros_like(b)
    for _ in range(iterations):
        f = eps * (log_a - _logsumexp((g[None, :] - cost) / eps, axis=1))
        g = eps * (log_b - _logsumexp((f[:, None] - cost) / eps, axis=0))
    return cast("F64", np.exp((f[:, None] + g[None, :] - cost) / eps))


def match_zones(source: ZonePalette, reference: ZonePalette, epsilon: float = SINKHORN_EPSILON) -> dict[int, dict[int, float]]:
    a = np.array([z.area for z in source.zones], dtype=np.float64)
    b = np.array([z.area for z in reference.zones], dtype=np.float64)
    plan = sinkhorn(zone_cost(source, reference), a / a.sum(), b / b.sum(), epsilon)
    result: dict[int, dict[int, float]] = {}
    for i, src_zone in enumerate(source.zones):
        row = plan[i] / max(float(plan[i].sum()), 1e-300)
        keep = row >= MIN_TARGET_WEIGHT
        keep[int(np.argmax(row))] = True
        total = float(row[keep].sum())
        result[src_zone.id] = {
            reference.zones[j].id: float(row[j] / total)
            for j in np.flatnonzero(keep)
        }
    return result
