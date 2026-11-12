"""Zone correspondence table and edit operations."""

from __future__ import annotations

from dataclasses import dataclass, field, replace
from typing import Literal

Level = Literal["-large", "-medium", "-small", "0", "+small", "+medium", "+large"]
Origin = Literal["auto", "user", "ai"]

LEVELS: tuple[Level, ...] = ("-large", "-medium", "-small", "0", "+small", "+medium", "+large")


@dataclass(frozen=True)
class Intent:
    lightness: Level = "0"
    saturation: Level = "0"
    warmth: Level = "0"
    tint: Level = "0"

    @property
    def is_neutral(self) -> bool:
        return self == Intent()


@dataclass(frozen=True)
class Link:
    source_zone: int
    targets: dict[int, float] = field(default_factory=dict)
    strength: float = 1.0
    luminance_strength: float | None = None
    chroma_strength: float | None = None
    locked: bool = False
    intent: Intent = Intent()
    origin: Origin = "auto"
    rationale: str = ""

    def __hash__(self) -> int:
        return hash((self.source_zone, tuple(sorted(self.targets.items())), self.strength))

    @property
    def is_identity(self) -> bool:
        return self.locked

    @property
    def keeps_colours(self) -> bool:
        return self.locked or ((not self.targets or self.strength == 0.0) and self.intent.is_neutral)


@dataclass(frozen=True)
class CorrespondenceTable:
    links: tuple[Link, ...]

    @classmethod
    def from_matching(cls, matching: dict[int, dict[int, float]]) -> CorrespondenceTable:
        return cls(links=tuple(Link(source_zone=zone_id, targets=dict(targets), origin="auto") for zone_id, targets in sorted(matching.items())))

    def link(self, source_zone: int) -> Link:
        for link in self.links:
            if link.source_zone == source_zone:
                return link
        raise KeyError(f"No link for source zone {source_zone}")

    def with_link(self, new_link: Link) -> CorrespondenceTable:
        self.link(new_link.source_zone)
        return replace(
            self,
            links=tuple(new_link if link.source_zone == new_link.source_zone else link for link in self.links),
        )

    @property
    def has_manual_links(self) -> bool:
        return any(link.origin != "auto" for link in self.links)

    def is_compatible(self, source_ids: tuple[int, ...], reference_ids: tuple[int, ...]) -> bool:
        refs = set(reference_ids)
        return tuple(link.source_zone for link in self.links) == tuple(source_ids) and all(set(link.targets) <= refs for link in self.links)


def retarget(table: CorrespondenceTable, source_zone: int, reference_zone: int, *, add: bool = False, origin: Origin = "user") -> CorrespondenceTable:
    link = table.link(source_zone)
    if add and link.targets:
        if reference_zone in link.targets:
            return table
        share = 1.0 / (len(link.targets) + 1)
        targets = {k: w * (1.0 - share) for k, w in link.targets.items()}
        targets[reference_zone] = share
    else:
        targets = {reference_zone: 1.0}
    return table.with_link(replace(link, targets=targets, locked=False, origin=origin, rationale=""))


def remove_target(table: CorrespondenceTable, source_zone: int, reference_zone: int, origin: Origin = "user") -> CorrespondenceTable:
    link = table.link(source_zone)
    new_link = link_without_target(link, reference_zone, origin)
    return table if new_link is link else table.with_link(new_link)


def link_without_target(link: Link, reference_zone: int, origin: Origin = "user") -> Link:
    if reference_zone not in link.targets or len(link.targets) < 2:
        return link
    rest = {k: w for k, w in link.targets.items() if k != reference_zone}
    total = sum(rest.values())
    targets = {k: w / total for k, w in rest.items()} if total > 0 else _equal(rest)
    return replace(link, targets=targets, origin=origin)


def set_target_weight(link: Link, reference_zone: int, weight: float) -> Link:
    if reference_zone not in link.targets or len(link.targets) < 2:
        return link
    weight = min(max(weight, 0.0), 1.0)
    others = {k: w for k, w in link.targets.items() if k != reference_zone}
    total = sum(others.values())
    scaled = {k: w / total * (1.0 - weight) for k, w in others.items()} if total > 0 else {k: (1.0 - weight) / len(others) for k in others}
    targets = {k: (weight if k == reference_zone else scaled[k]) for k in link.targets}
    return replace(link, targets=targets, origin="user")


def set_locked(table: CorrespondenceTable, source_zone: int, locked: bool) -> CorrespondenceTable:
    link = table.link(source_zone)
    if link.locked == locked:
        return table
    return table.with_link(replace(link, locked=locked, origin="user"))


def reveal(table: CorrespondenceTable, source_zone: int, origin: Origin = "user") -> CorrespondenceTable:
    link = table.link(source_zone)
    if not link.locked:
        return table
    return table.with_link(replace(link, locked=False, origin=origin))


def _equal(mapping: dict[int, float]) -> dict[int, float]:
    if not mapping:
        return {}
    share = 1.0 / len(mapping)
    return {k: share for k in mapping}
