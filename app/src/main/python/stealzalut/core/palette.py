"""Colour-zone analysis for the mobile Lua/Chaquopy pipeline."""

from __future__ import annotations

import hashlib
from dataclasses import dataclass
from typing import cast

import numpy as np
from numpy.typing import NDArray
from PIL import Image

from stealzalut.core.color_space import gamut_compress, oklab_to_rgb, rgb_to_oklab
from stealzalut.utils.image_io import create_proxy_image

F64 = NDArray[np.float64]

ANALYSIS_MAX_SIZE = 256
DEFAULT_ZONE_COUNT = 8
MIN_ZONE_COUNT = 2
MAX_ZONE_COUNT = 16
MIN_ZONE_AREA = 0.01
COV_REGULARIZATION = 1e-5
ALGORITHM_VERSION = "zones-2"


@dataclass(frozen=True)
class Zone:
    id: int
    mean: F64
    cov: F64
    area: float
    l_rank: float
    centroid_xy: tuple[float, float]
    bbox: tuple[float, float, float, float]
    hex: str


@dataclass(frozen=True)
class ZonePalette:
    zones: tuple[Zone, ...]
    labels: NDArray[np.int32]

    def zone(self, zone_id: int) -> Zone:
        if not 1 <= zone_id <= len(self.zones):
            raise KeyError(f"No zone {zone_id} (zones are 1..{len(self.zones)})")
        return self.zones[zone_id - 1]

    @property
    def ids(self) -> tuple[int, ...]:
        return tuple(z.id for z in self.zones)


@dataclass(frozen=True)
class ColourStats:
    concentration: float
    colourful: float
    hue: float

    @property
    def is_monochrome(self) -> bool:
        return self.concentration >= 0.85


def _thumbnail(image_rgb: NDArray[np.uint8]) -> NDArray[np.uint8]:
    return create_proxy_image(image_rgb, max_dim=ANALYSIS_MAX_SIZE)


def analysis_oklab(image_rgb: NDArray[np.uint8]) -> tuple[F64, int, int]:
    small = _thumbnail(image_rgb)
    h, w = small.shape[:2]
    flat = small.reshape(-1, 3).astype(np.float64) / 255.0
    return rgb_to_oklab(flat), int(h), int(w)


def _merge_small_zones(lab: F64, labels: NDArray[np.int64], min_area: float) -> NDArray[np.int64]:
    labels = labels.copy()
    total = labels.size
    while True:
        present, counts = np.unique(labels, return_counts=True)
        if len(present) <= 1:
            return labels
        smallest_idx = int(np.argmin(counts))
        if counts[smallest_idx] / total >= min_area:
            return labels
        means = np.array([lab[labels == z].mean(axis=0) for z in present], dtype=np.float64)
        dist = np.linalg.norm(means - means[smallest_idx], axis=1)
        dist[smallest_idx] = np.inf
        target = present[int(np.argmin(dist))]
        labels[labels == present[smallest_idx]] = target


def _to_hex(lab_mean: F64) -> str:
    rgb = oklab_to_rgb(gamut_compress(lab_mean[None, :]))[0]
    r, g, b = (int(round(float(c) * 255.0)) for c in rgb)
    return f"#{r:02x}{g:02x}{b:02x}"


def color_stats(image_rgb: NDArray[np.uint8]) -> ColourStats:
    lab, _h, _w = analysis_oklab(image_rgb)
    chroma = np.hypot(lab[:, 1], lab[:, 2])
    coloured = chroma >= 0.05
    fraction = float(coloured.mean())
    if fraction < 0.05:
        return ColourStats(0.0, fraction, 0.0)
    a_sum = float(lab[coloured, 1].sum())
    b_sum = float(lab[coloured, 2].sum())
    resultant = float(np.hypot(a_sum, b_sum) / chroma[coloured].sum())
    hue = float(np.degrees(np.arctan2(b_sum, a_sum)) % 360.0)
    return ColourStats(resultant, fraction, hue)


colour_stats = color_stats


def _kmeans_oklab(points: F64, k: int) -> NDArray[np.int64]:
    """Run deterministic k-means++ with the same bounds as the desktop engine."""
    rng = np.random.default_rng(0x5A17)
    best_labels: NDArray[np.int64] | None = None
    best_inertia = np.inf

    for _attempt in range(3):
        centers = np.empty((k, 3), dtype=np.float64)
        first = int(rng.integers(points.shape[0]))
        centers[0] = points[first]
        closest = np.sum((points - centers[0]) ** 2, axis=1)
        for index in range(1, k):
            total = float(closest.sum())
            if total <= 0.0:
                centers[index:] = points[rng.integers(points.shape[0], size=k - index)]
                break
            threshold = float(rng.random()) * total
            selected = int(np.searchsorted(np.cumsum(closest), threshold, side="right"))
            centers[index] = points[min(selected, points.shape[0] - 1)]
            distance = np.sum((points - centers[index]) ** 2, axis=1)
            closest = np.minimum(closest, distance)

        labels = np.zeros(points.shape[0], dtype=np.int64)
        for _iteration in range(50):
            distances = np.sum((points[:, None, :] - centers[None, :, :]) ** 2, axis=2)
            new_labels = np.argmin(distances, axis=1)
            new_centers = centers.copy()
            for cluster in range(k):
                mask = new_labels == cluster
                if mask.any():
                    new_centers[cluster] = points[mask].mean(axis=0)
            labels = new_labels
            if float(np.max(np.abs(new_centers - centers))) <= 1e-5:
                centers = new_centers
                break
            centers = new_centers

        inertia = float(np.sum((points - centers[labels]) ** 2))
        if inertia < best_inertia:
            best_inertia = inertia
            best_labels = labels.copy()

    return best_labels if best_labels is not None else np.zeros(points.shape[0], dtype=np.int64)


def analyze_zones(image_rgb: NDArray[np.uint8], zone_count: int = DEFAULT_ZONE_COUNT) -> ZonePalette:
    k = int(np.clip(zone_count, MIN_ZONE_COUNT, MAX_ZONE_COUNT))
    lab, h, w = analysis_oklab(image_rgb)
    k = min(k, lab.shape[0])
    if k <= 1:
        labels = np.zeros(lab.shape[0], dtype=np.int64)
        zone_map = labels.reshape(h, w)
        return ZonePalette(zones=_zone_stats(lab, zone_map), labels=zone_map)

    labels = _kmeans_oklab(lab, k)

    labels = _merge_small_zones(lab, labels, MIN_ZONE_AREA)
    present = np.unique(labels)
    if present.size == 1:
        zone_map = np.zeros((h, w), dtype=np.int32)
        zone_map[labels.reshape(h, w) == present[0]] = 1
        return ZonePalette(zones=_zone_stats(lab, zone_map), labels=zone_map)

    mean_l = np.array([lab[labels == z, 0].mean() for z in present], dtype=np.float64)
    order = present[np.argsort(mean_l, kind="stable")]
    remap = np.zeros(int(present.max()) + 1, dtype=np.int32)
    remap[order] = np.arange(1, len(order) + 1, dtype=np.int32)
    zone_map = remap[labels].reshape(h, w)
    return ZonePalette(zones=_zone_stats(lab, zone_map), labels=zone_map)


def _zone_stats(lab: F64, zone_map: NDArray[np.int32]) -> tuple[Zone, ...]:
    h, w = zone_map.shape
    flat = zone_map.ravel()
    count = int(flat.max())
    areas = np.array([(flat == z).sum() / flat.size for z in range(1, count + 1)], dtype=np.float64)
    ranks = np.cumsum(areas) - areas / 2.0
    zones: list[Zone] = []
    for i, zone_id in enumerate(range(1, count + 1)):
        mask = flat == zone_id
        pixels = lab[mask]
        if pixels.shape[0] > 1:
            cov = np.asarray(np.cov(pixels, rowvar=False), dtype=np.float64)
        else:
            cov = np.zeros((3, 3), dtype=np.float64)
        ys, xs = np.divmod(np.flatnonzero(mask), w)
        zones.append(
            Zone(
                id=zone_id,
                mean=pixels.mean(axis=0),
                cov=cov + COV_REGULARIZATION * np.eye(3),
                area=float(areas[i]),
                l_rank=float(ranks[i]),
                centroid_xy=(float(xs.mean() / w * 100.0), float(ys.mean() / h * 100.0)),
                bbox=(
                    float(xs.min() / w * 100.0),
                    float(ys.min() / h * 100.0),
                    float((xs.max() + 1) / w * 100.0),
                    float((ys.max() + 1) / h * 100.0),
                ),
                hex=_to_hex(pixels.mean(axis=0)),
            )
        )
    return tuple(zones)


def analysis_id(source_rgb: NDArray[np.uint8], reference_rgb: NDArray[np.uint8], zone_count: int) -> str:
    k = int(np.clip(zone_count, MIN_ZONE_COUNT, MAX_ZONE_COUNT))
    digest = hashlib.sha256()
    for image in (source_rgb, reference_rgb):
        small = _thumbnail(image)
        digest.update(str(small.shape).encode())
        digest.update(np.ascontiguousarray(small).tobytes())
    digest.update(f"{k}|{ALGORITHM_VERSION}".encode())
    return digest.hexdigest()[:8]


def zone_mask(palette: ZonePalette, zone_id: int, size: tuple[int, int]) -> NDArray[np.bool_]:
    resized = Image.fromarray(palette.labels.astype(np.int32)).resize(size, resample=Image.Resampling.NEAREST)
    resized_arr = np.asarray(resized)
    return cast("NDArray[np.bool_]", resized_arr == zone_id)
