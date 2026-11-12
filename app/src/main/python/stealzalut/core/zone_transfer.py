"""Zone-based colour transfer with correspondence tables."""

from __future__ import annotations

from dataclasses import dataclass, replace
from itertools import combinations
from typing import cast

import numpy as np
from numpy.typing import NDArray

from stealzalut.core.color_transfer import (
    OKLAB_SCALE_LIMITS,
    OklabTransferModel,
    TransferModel,
    _bound_symmetric,
    _compute_mkl_matrix,
    fit_transfer,
)
from stealzalut.core.color_space import gamut_compress, oklab_to_rgb, rgb_to_oklab, srgb_to_oklab
from stealzalut.core.correspondence import CorrespondenceTable, Link, Level
from stealzalut.core.matching import match_zones
from stealzalut.core.palette import (
    DEFAULT_ZONE_COUNT,
    ColourStats,
    Zone,
    ZonePalette,
    analysis_id,
    analyze_zones,
    colour_stats,
)

F64 = NDArray[np.float64]

INTENT_STEPS: dict[str, dict[str, float]] = {
    "lightness": {"small": 0.02, "medium": 0.05, "large": 0.10},
    "saturation_up": {"small": 1.08, "medium": 1.20, "large": 1.40},
    "saturation_down": {"small": 0.92, "medium": 0.82, "large": 0.65},
    "warmth": {"small": 0.010, "medium": 0.020, "large": 0.040},
    "tint": {"small": 0.008, "medium": 0.016, "large": 0.030},
}
WARM_AXIS = np.array([0.29, 0.96]) / np.hypot(0.29, 0.96)
CONFLICT_OVERLAP = 0.3
CONFLICT_DISPLACEMENT = 0.05


@dataclass(frozen=True)
class ZoneBakeParams:
    tau: float = 1.0
    sigma0: float = 0.02
    beta: float = 0.3
    background_logit: float = -4.5


@dataclass(frozen=True)
class Conflict:
    zone_a: int
    zone_b: int
    overlap: float
    displacement_gap: float


@dataclass(frozen=True)
class ZoneTransferModel(TransferModel):
    source: ZonePalette
    reference: ZonePalette
    table: CorrespondenceTable
    global_model: OklabTransferModel
    params: ZoneBakeParams = ZoneBakeParams()
    analysis_id: str = ""
    source_colours: ColourStats | None = None
    reference_colours: ColourStats | None = None

    @property
    def method(self) -> str:
        return "zones"

    def fold_reference(self) -> TransferModel:
        return self.global_model

    def with_table(self, table: CorrespondenceTable) -> ZoneTransferModel:
        return replace(self, table=table)

    def with_params(self, params: ZoneBakeParams) -> ZoneTransferModel:
        return replace(self, params=params)

    def apply(self, colors: F64) -> F64:
        lab = srgb_to_oklab(colors)
        out = lab + self.displacement(lab, colors)
        return oklab_to_rgb(gamut_compress(out))

    def apply_lab(self, lab: F64) -> F64:
        return lab + self.displacement(lab)

    def displacement(self, lab: F64, srgb: F64 | None = None) -> F64:
        if srgb is None:
            srgb = oklab_to_rgb(gamut_compress(lab))
        zones = self.source.zones
        n, k = lab.shape[0], len(zones)
        logits = np.empty((n, k + 1))
        deltas = np.empty((k + 1, n, 3))
        global_delta = srgb_to_oklab(self.global_model.apply(srgb)) - lab
        logits[:, 0] = self.params.background_logit
        deltas[0] = global_delta

        for i, zone in enumerate(zones, start=1):
            logits[:, i] = self._zone_logit(lab, zone)
            deltas[i] = zone_delta(lab, zone, self.table.link(zone.id), self.reference, global_delta)
        logits -= logits.max(axis=1, keepdims=True)
        weights = np.exp(logits)
        weights /= weights.sum(axis=1, keepdims=True)
        return cast("F64", np.einsum("nk,knd->nd", weights, deltas))

    def _zone_logit(self, lab: F64, zone: Zone) -> F64:
        cov = zone.cov + self.params.sigma0**2 * np.eye(3)
        diff = lab - zone.mean
        mahalanobis2 = np.einsum("ni,ij,nj->n", diff, np.linalg.inv(cov), diff)
        return cast(
            "F64",
            -0.5 * mahalanobis2 / self.params.tau + self.params.beta * np.log(zone.area),
        )


def _target_moments(reference: ZonePalette, targets: dict[int, float]) -> tuple[F64, F64]:
    weights = np.array(list(targets.values()), dtype=np.float64)
    weights /= weights.sum()
    zones = [reference.zone(zone_id) for zone_id in targets]
    mean = sum((w * z.mean for w, z in zip(weights, zones)), np.zeros(3))
    cov = np.zeros((3, 3), dtype=np.float64)
    for w, z in zip(weights, zones):
        d = (z.mean - mean)[:, None]
        cov += w * (z.cov + d @ d.T)
    return mean, cov


def _signed_step(level: Level, kind: str) -> float:
    if level == "0":
        return 0.0
    step = INTENT_STEPS[kind][level[1:]]
    return (1.0 if level[0] == "+" else -1.0) * step


def _saturation_factor(level: Level) -> float:
    if level == "0":
        return 1.0
    table = INTENT_STEPS["saturation_up" if level[0] == "+" else "saturation_down"]
    return table[level[1:]]


def zone_delta(
    lab: F64,
    zone: Zone,
    link: Link,
    reference: ZonePalette,
    global_delta: F64,
) -> F64:
    delta = np.zeros_like(lab)
    if link.locked:
        return delta
    delta[:, 0] = global_delta[:, 0]
    if link.targets and link.strength > 0.0:
        lo, hi = OKLAB_SCALE_LIMITS
        t_mean, t_cov = _target_moments(reference, link.targets)
        sigma_s = float(np.sqrt(zone.cov[0, 0]))
        sigma_t = float(np.sqrt(t_cov[0, 0]))
        scale = float(np.clip(sigma_t / max(sigma_s, 1e-6), lo, hi))
        chroma_map = _bound_symmetric(_compute_mkl_matrix(zone.cov[1:, 1:], t_cov[1:, 1:]), lo, hi)
        new_l = t_mean[0] + scale * (lab[:, 0] - zone.mean[0])
        new_ab = t_mean[1:] + (lab[:, 1:] - zone.mean[1:]) @ chroma_map.T

        lum = 0.0 if link.luminance_strength is None else link.luminance_strength
        chroma = 1.0 if link.chroma_strength is None else link.chroma_strength
        zone_dl = new_l - lab[:, 0]
        delta[:, 0] += link.strength * lum * (zone_dl - global_delta[:, 0])
        delta[:, 1:] = link.strength * chroma * (new_ab - lab[:, 1:])

    intent = link.intent
    if not intent.is_neutral:
        delta[:, 0] += _signed_step(intent.lightness, "lightness")
        ab_now = lab[:, 1:] + delta[:, 1:]
        delta[:, 1:] += (_saturation_factor(intent.saturation) - 1.0) * ab_now
        delta[:, 1:] += _signed_step(intent.warmth, "warmth") * WARM_AXIS
        delta[:, 1] += _signed_step(intent.tint, "tint")
    return delta


def bhattacharyya_coefficient(mean_a: F64, cov_a: F64, mean_b: F64, cov_b: F64) -> float:
    cov = 0.5 * (cov_a + cov_b)
    diff = mean_a - mean_b
    term_mean = 0.125 * float(diff @ np.linalg.solve(cov, diff))
    _, logdet = np.linalg.slogdet(cov)
    _, logdet_a = np.linalg.slogdet(cov_a)
    _, logdet_b = np.linalg.slogdet(cov_b)
    term_cov = 0.5 * (logdet - 0.5 * (logdet_a + logdet_b))
    return float(np.exp(-(term_mean + term_cov)))


def find_conflicts(model: ZoneTransferModel) -> list[Conflict]:
    zones = model.source.zones
    means = np.array([z.mean for z in zones], dtype=np.float64)
    global_delta = srgb_to_oklab(model.global_model.apply(oklab_to_rgb(gamut_compress(means))))
    global_delta -= means
    mean_delta = {
        z.id: zone_delta(z.mean[None, :], z, model.table.link(z.id), model.reference, global_delta[i : i + 1])[0]
        for i, z in enumerate(zones)
    }
    conflicts: list[Conflict] = []
    for a, b in combinations(zones, 2):
        overlap = bhattacharyya_coefficient(a.mean, a.cov, b.mean, b.cov)
        gap = float(np.linalg.norm(mean_delta[a.id] - mean_delta[b.id]))
        if overlap > CONFLICT_OVERLAP and gap > CONFLICT_DISPLACEMENT:
            conflicts.append(Conflict(a.id, b.id, overlap, gap))
    return conflicts


def fit_zone_transfer(
    source: NDArray[np.uint8],
    reference: NDArray[np.uint8],
    zone_count: int = DEFAULT_ZONE_COUNT,
    params: ZoneBakeParams | None = None,
) -> ZoneTransferModel:
    src_palette = analyze_zones(source, zone_count)
    ref_palette = analyze_zones(reference, zone_count)
    table = CorrespondenceTable.from_matching(match_zones(src_palette, ref_palette))
    global_model = fit_transfer(source, reference, method="oklab")
    assert isinstance(global_model, OklabTransferModel)
    model = ZoneTransferModel(
        source=src_palette,
        reference=ref_palette,
        table=table,
        global_model=global_model,
        params=params or ZoneBakeParams(),
        analysis_id=analysis_id(source, reference, zone_count),
        source_colours=colour_stats(source),
        reference_colours=colour_stats(reference),
    )
    return model.with_table(harmonize_conflicts(model))


def harmonize_conflicts(model: ZoneTransferModel, max_rounds: int = 4) -> CorrespondenceTable:
    table = model.table
    for _ in range(max_rounds):
        conflicts = find_conflicts(model.with_table(table))
        changed = False
        for conflict in conflicts:
            a, b = table.link(conflict.zone_a), table.link(conflict.zone_b)
            if a.origin != "auto" or b.origin != "auto" or a.locked or b.locked:
                continue
            area_a = model.source.zone(a.source_zone).area
            area_b = model.source.zone(b.source_zone).area
            merged: dict[int, float] = {}
            for link, area in ((a, area_a), (b, area_b)):
                for ref_id, weight in link.targets.items():
                    merged[ref_id] = merged.get(ref_id, 0.0) + weight * area
            total = sum(merged.values())
            targets = {k: w / total for k, w in sorted(merged.items())}
            if targets != a.targets or targets != b.targets:
                table = table.with_link(replace(a, targets=targets))
                table = table.with_link(replace(b, targets=dict(targets)))
                changed = True
        if not changed:
            break
    return table
