"""Minimal recipe serialization for zone-aware AI handoff.

This keeps the project's mobile-friendly architecture while exposing the
same JSON contract used by the desktop reference: a portable recipe file,
writeable from a free-form AI answer and readable back into a structured object.
"""

from __future__ import annotations

import json
import math
import re
from dataclasses import dataclass, field
from typing import Any

RECIPE_FORMAT = "stealzalut.recipe"
RECIPE_VERSION = 2
RECIPE_SUFFIX = ".szl"
RECIPE_MODES = ("zones", "ambiance", "look")
SEPARATION_WORDS: dict[str, float] = {"sharp": 0.5, "medium": 1.0, "soft": 2.0}
STRENGTH_WORDS: dict[str, float] = {
    "off": 0.0,
    "subtle": 0.3,
    "medium": 0.6,
    "strong": 0.85,
    "full": 1.0,
}
FENCE_RE = re.compile(r"```[ \t]*([A-Za-z0-9_-]*)[ \t]*\r?\n(.*?)```", re.DOTALL)


class RecipeError(ValueError):
    """Raised when a recipe payload is invalid."""


@dataclass(frozen=True)
class RecipeLink:
    source_zone: int
    targets: dict[int, float] | None = None
    strength: float | None = None
    luminance_strength: float | None = None
    chroma_strength: float | None = None
    keep: bool = False
    intent: dict[str, str] | None = None
    rationale: str = ""
    origin: str | None = None


@dataclass(frozen=True)
class Recipe:
    analysis_id: str | None = None
    name: str = ""
    description: str = ""
    grading: dict[str, float] = field(default_factory=dict)
    tau: float | None = None
    zone_count: int | None = None
    links: tuple[RecipeLink, ...] = ()
    finishing: dict[str, float] | None = None
    finishing_keys: frozenset[str] = frozenset()
    mode: str | None = None
    separation: float | None = None
    protect_vivid: float | None = None
    focus: dict[int, float] = field(default_factory=dict)


def _is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(float(value))


def _as_float(value: Any, *, where: str, lo: float = 0.0, hi: float = 1.0) -> float:
    if not _is_number(value):
        raise RecipeError(f"{where} must be numeric.")
    val = float(value)
    if not lo <= val <= hi:
        raise RecipeError(f"{where} must be between {lo} and {hi}.")
    return val


def _normalise_targets(raw: Any, *, where: str) -> dict[int, float]:
    if raw is None:
        return {}
    if not isinstance(raw, dict):
        raise RecipeError(f"{where} targets must be an object.")
    parsed: dict[int, float] = {}
    for key, weight in raw.items():
        zone_id = _as_zone_id(key, where=f"{where} target {key}")
        value = _as_float(weight, where=f"{where} weight for zone {zone_id}", lo=0.0, hi=1.0)
        if value <= 0.0:
            raise RecipeError(f"{where} target {zone_id} has an invalid weight (must be > 0).")
        parsed[zone_id] = value
    total = sum(parsed.values())
    if total <= 0.0:
        raise RecipeError(f"{where} targets must contain at least one positive weight.")
    return {zone_id: weight / total for zone_id, weight in parsed.items()}


def _as_zone_id(value: Any, *, where: str) -> int:
    if isinstance(value, bool):
        raise RecipeError(f"{where} is not a valid zone identifier.")
    if isinstance(value, int):
        return value
    if isinstance(value, str):
        token = value.strip()
        if token.startswith("S") or token.startswith("R"):
            token = token[1:]
        if token.startswith("#"):
            token = token[1:]
        try:
            return int(token)
        except ValueError:
            raise RecipeError(f"{where} is not a valid zone identifier.") from None
    if isinstance(value, float) and value.is_integer():
        return int(value)
    raise RecipeError(f"{where} is not a valid zone identifier.")


def _parse_strength(value: Any, *, where: str) -> float | None:
    if value is None:
        return None
    if isinstance(value, str):
        key = value.strip().lower()
        if key in STRENGTH_WORDS:
            return STRENGTH_WORDS[key]
        try:
            return _as_float(float(key), where=where, lo=0.0, hi=1.0)
        except ValueError as exc:
            raise RecipeError(f"{where} strength '{value}' is invalid.") from exc
    return _as_float(value, where=where, lo=0.0, hi=1.0)


def _parse_separation(value: Any, *, where: str) -> float:
    if isinstance(value, str):
        key = value.strip().lower()
        if key in SEPARATION_WORDS:
            return SEPARATION_WORDS[key]
    if _is_number(value):
        val = float(value)
        if 0.25 <= val <= 3.0:
            return val
    raise RecipeError(f"{where} must be one of {sorted(SEPARATION_WORDS)} or a number between 0.25 and 3.0.")


def _parse_link(item: Any, index: int) -> RecipeLink:
    if not isinstance(item, dict):
        raise RecipeError(f"Link #{index} must be an object.")
    source_zone = item.get("source_zone")
    if source_zone is None:
        raise RecipeError(f"Link #{index} is missing source_zone.")
    source_id = _as_zone_id(source_zone, where=f"Link #{index} source_zone")
    targets = item.get("targets")
    parsed_targets = _normalise_targets(targets, where=f"Link #{index}") if targets is not None else None

    strength = _parse_strength(item.get("strength"), where=f"Link #{index} strength") if "strength" in item else None
    luminance_strength = _parse_strength(item.get("luminance_strength"), where=f"Link #{index} luminance_strength") if "luminance_strength" in item else None
    chroma_strength = _parse_strength(item.get("chroma_strength"), where=f"Link #{index} chroma_strength") if "chroma_strength" in item else None

    intent = item.get("intent")
    if intent is not None and not isinstance(intent, dict):
        raise RecipeError(f"Link #{index} intent must be an object.")

    rationale = str(item.get("rationale") or "")
    origin = item.get("origin")
    if origin is not None:
        origin = str(origin)
    return RecipeLink(
        source_zone=source_id,
        targets=parsed_targets,
        strength=strength,
        luminance_strength=luminance_strength,
        chroma_strength=chroma_strength,
        keep=bool(item.get("keep", False)),
        intent=intent,
        rationale=rationale,
        origin=origin,
    )


def extract_json(text: str) -> str:
    """Return the last JSON object found in free-form AI output."""
    blocks = [match.group(2).strip() for match in FENCE_RE.finditer(text)]
    for candidate in reversed(blocks):
        if candidate.startswith("{"):
            return candidate
    stripped = text.strip()
    if stripped.startswith("{"):
        return stripped
    start = stripped.find("{")
    end = stripped.rfind("}")
    if start != -1 and end > start:
        return stripped[start : end + 1]
    raise RecipeError("No JSON block found. The response must end with a ```json ... ``` block.")


def _parse_global(global_data: Any) -> tuple[dict[str, float], float | None]:
    grading: dict[str, float] = {}
    tau: float | None = None
    if global_data is None:
        return grading, tau
    if not isinstance(global_data, dict):
        raise RecipeError("'global' must be an object or null.")
    for key, value in global_data.items():
        if key in {"zone_separation", "separation", "tau"}:
            tau = _parse_separation(value, where=f"global.{key}")
            continue
        if isinstance(value, str):
            lowered = value.strip().lower()
            if lowered in STRENGTH_WORDS:
                grading[str(key)] = STRENGTH_WORDS[lowered]
                continue
        if _is_number(value):
            grading[str(key)] = float(value)
            continue
        raise RecipeError(f"global.{key} must be numeric or a valid strength word.")
    return grading, tau


def _parse_look(look_data: Any) -> tuple[float | None, float | None, dict[int, float]]:
    if look_data is None:
        return None, None, {}
    if not isinstance(look_data, dict):
        raise RecipeError("'look' must be an object or null.")
    separation = None
    protect_vivid = None
    focus: dict[int, float] = {}
    if "separation" in look_data:
        separation = _parse_separation(look_data["separation"], where="look.separation")
    if "protect_vivid" in look_data:
        protect_vivid = _as_float(look_data["protect_vivid"], where="look.protect_vivid", lo=0.0, hi=1.0)
    for key, value in (look_data.get("focus") or {}).items():
        focus[_as_zone_id(key, where=f"look.focus.{key}")] = _as_float(value, where=f"look.focus.{key}", lo=0.0, hi=1.0)
    return separation, protect_vivid, focus


def parse_recipe(source: str | dict[str, Any]) -> Recipe:
    """Parse a recipe from a JSON string, raw JSON object, or free-form AI text."""
    if isinstance(source, str):
        try:
            data = json.loads(extract_json(source))
        except json.JSONDecodeError as exc:
            raise RecipeError(f"Invalid recipe JSON (line {exc.lineno}, column {exc.colno}): {exc.msg}.") from exc
    elif isinstance(source, dict):
        data = source
    else:
        raise RecipeError("Recipe payload must be a JSON object or a text string.")

    if not isinstance(data, dict):
        raise RecipeError("Recipe payload must resolve to an object.")
    fmt = data.get("format", RECIPE_FORMAT)
    if fmt != RECIPE_FORMAT:
        raise RecipeError(f"Unsupported recipe format '{fmt}'.")
    version = data.get("version", RECIPE_VERSION)
    if version != RECIPE_VERSION:
        raise RecipeError(f"Unsupported recipe version '{version}'.")

    mode = data.get("mode")
    if mode is not None and mode not in RECIPE_MODES:
        raise RecipeError(f"Unsupported recipe mode '{mode}'.")

    zone_count = data.get("zone_count")
    if zone_count is not None:
        if not _is_number(zone_count) or not float(zone_count).is_integer():
            raise RecipeError("zone_count must be an integer.")
        zone_count = int(zone_count)

    grading, tau = _parse_global(data.get("global"))
    separation, protect_vivid, focus = _parse_look(data.get("look"))

    raw_links = [] if mode == "look" else data.get("links", [])
    if not isinstance(raw_links, list):
        raise RecipeError("'links' must be a list.")

    links = tuple(_parse_link(item, idx) for idx, item in enumerate(raw_links, start=1))
    seen: set[int] = set()
    for idx, link in enumerate(links, start=1):
        if link.source_zone in seen:
            raise RecipeError(f"Link #{idx}: source zone S{link.source_zone} appears more than once.")
        seen.add(link.source_zone)

    finishing = data.get("finishing")
    if finishing is not None and not isinstance(finishing, dict):
        raise RecipeError("'finishing' must be an object or null.")
    finishing_keys = frozenset(str(k) for k in (finishing or {}).keys())

    return Recipe(
        analysis_id=str(data["analysis_id"]) if data.get("analysis_id") is not None else None,
        name=str(data.get("name") or ""),
        description=str(data.get("description") or ""),
        grading=grading,
        tau=tau if tau is not None else None,
        zone_count=zone_count,
        links=links,
        finishing=finishing,
        finishing_keys=finishing_keys,
        mode=mode,
        separation=separation,
        protect_vivid=protect_vivid,
        focus=focus,
    )


def _serialize_targets(targets: dict[int, float] | None) -> dict[str, float] | None:
    if not targets:
        return None
    return {str(zone_id): float(weight) for zone_id, weight in sorted(targets.items())}


def recipe_to_dict(recipe: Recipe) -> dict[str, Any]:
    """Serialize a Recipe to a JSON-friendly dict."""
    payload: dict[str, Any] = {
        "format": RECIPE_FORMAT,
        "version": RECIPE_VERSION,
    }
    if recipe.analysis_id is not None:
        payload["analysis_id"] = recipe.analysis_id
    if recipe.name:
        payload["name"] = recipe.name
    if recipe.description:
        payload["description"] = recipe.description
    if recipe.grading:
        payload["global"] = {str(k): float(v) for k, v in recipe.grading.items()}
    if recipe.mode is not None:
        payload["mode"] = recipe.mode
    if recipe.zone_count is not None:
        payload["zone_count"] = recipe.zone_count
    if recipe.links:
        payload["links"] = [
            {
                "source_zone": int(link.source_zone),
                **({"targets": _serialize_targets(link.targets)} if link.targets else {}),
                **({"strength": float(link.strength)} if link.strength is not None else {}),
                **({"luminance_strength": float(link.luminance_strength)} if link.luminance_strength is not None else {}),
                **({"chroma_strength": float(link.chroma_strength)} if link.chroma_strength is not None else {}),
                **({"keep": bool(link.keep)} if link.keep else {}),
                **({"intent": dict(link.intent)} if link.intent else {}),
                **({"rationale": link.rationale} if link.rationale else {}),
                **({"origin": str(link.origin)} if link.origin else {}),
            }
            for link in recipe.links
        ]
    if recipe.finishing is not None:
        payload["finishing"] = recipe.finishing
    if recipe.tau is not None and "zone_separation" not in payload.get("global", {}):
        payload.setdefault("global", {})["zone_separation"] = recipe.tau
    return payload


def dump_recipe(recipe: Recipe, *, pretty: bool = True) -> str:
    return json.dumps(recipe_to_dict(recipe), ensure_ascii=False, indent=2 if pretty else None)
