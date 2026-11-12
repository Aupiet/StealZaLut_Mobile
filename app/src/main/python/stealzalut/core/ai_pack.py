"""Portable AI analysis pack for zone-aware colour transfers.

The app captures the source/reference analysis, builds a compact summary, and
exports the current zone mapping to a folder that a user can send to their AI
assistant. The format is intentionally lightweight and deliberately avoids any
extra dependency beyond Pillow and NumPy.
"""

from __future__ import annotations

import json
import zipfile
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw

from stealzalut.core.color_space import oklab_to_oklch
from stealzalut.core.palette import ZonePalette
from stealzalut.core.zone_transfer import ZoneTransferModel

ANALYSIS_FORMAT = "stealzalut.analysis"
ANALYSIS_VERSION = 1
PACK_MAX_SIZE = 1024
ORIGINAL_MAX_SIZE = 1600
PACK_FILES = (
    "1_source.jpg",
    "2_reference.jpg",
    "3_source_zones.png",
    "4_reference_zones.png",
    "5_source_overview.png",
    "6_reference_overview.png",
    "analysis.json",
    "PROMPT.md",
)


def _lightness_band(lightness: float) -> str:
    if lightness < 0.45:
        return "shadows"
    if lightness < 0.72:
        return "midtones"
    return "highlights"


def _zone_payload(palette: ZonePalette) -> list[dict[str, Any]]:
    payload: list[dict[str, Any]] = []
    for zone in palette.zones:
        l, c, h = oklab_to_oklch(zone.mean[None, :])[0]
        payload.append(
            {
                "id": int(zone.id),
                "hex": zone.hex,
                "area_pct": round(float(zone.area * 100.0), 1),
                "oklch": [round(float(l), 3), round(float(c), 3), round(float(h), 1)],
                "lightness_band": _lightness_band(float(zone.mean[0])),
                "centroid_pct": [round(float(v), 1) for v in zone.centroid_xy],
                "bbox_pct": [round(float(v), 1) for v in zone.bbox],
            }
        )
    return payload


def _colours_payload(stats: Any) -> dict[str, Any]:
    if stats is None:
        return {}
    return {
        "hue_concentration": round(float(stats.concentration), 2),
        "colourful_pct": round(float(stats.colourful * 100.0), 1),
        "dominant_hue": round(float(stats.hue), 1),
        "monochrome": bool(stats.is_monochrome),
    }


def suggested_mode(model: ZoneTransferModel) -> str:
    """Prefer ambiance for a single-dominant-colour reference; otherwise use zones."""
    stats = model.reference_colours
    return "ambiance" if stats is not None and stats.is_monochrome else "zones"


def analysis_dict(
    model: ZoneTransferModel,
    source_size: tuple[int, int],
    reference_size: tuple[int, int],
    zone_count: int,
) -> dict[str, Any]:
    return {
        "format": ANALYSIS_FORMAT,
        "version": ANALYSIS_VERSION,
        "analysis_id": model.analysis_id,
        "zone_count": int(zone_count),
        "suggested_mode": suggested_mode(model),
        "source": {
            "size": list(source_size),
            **_colours_payload(model.source_colours),
            "zones": _zone_payload(model.source),
        },
        "reference": {
            "size": list(reference_size),
            **_colours_payload(model.reference_colours),
            "zones": _zone_payload(model.reference),
        },
        "auto_matching": [
            {
                "source_zone": int(link.source_zone),
                "targets": {str(int(k)): round(float(v), 3) for k, v in sorted(link.targets.items())},
            }
            for link in model.table.links
        ],
    }


def _resize_to_limit(image_rgb: np.ndarray, max_size: int) -> np.ndarray:
    img = Image.fromarray(image_rgb.astype(np.uint8)).convert("RGB")
    if max(img.size) <= max_size:
        return np.asarray(img, dtype=np.uint8)
    scale = max_size / float(max(img.size))
    new_size = (max(1, int(round(img.width * scale))), max(1, int(round(img.height * scale))))
    return np.asarray(img.resize(new_size, Image.Resampling.LANCZOS), dtype=np.uint8)


def _zone_mask(palette: ZonePalette, zone_id: int, size: tuple[int, int]) -> np.ndarray:
    labels = np.asarray(palette.labels, dtype=np.int32)
    mask = (labels == zone_id).astype(np.uint8)
    if mask.shape[:2] != size:
        mask = np.asarray(
            Image.fromarray(mask * 255, mode="L").resize(size, Image.Resampling.NEAREST),
            dtype=np.uint8,
        )
    return mask > 0


def zone_sheet(
    image_rgb: np.ndarray,
    palette: ZonePalette,
    prefix: str,
    columns: int = 4,
) -> np.ndarray:
    """Build a compact grid of tiles, one per zone, with the selected zone in colour and the rest greyed."""
    img = Image.fromarray(np.asarray(image_rgb, dtype=np.uint8)).convert("RGB")
    if max(img.size) > PACK_MAX_SIZE:
        scale = PACK_MAX_SIZE / float(max(img.size))
        img = img.resize((max(1, int(round(img.width * scale))), max(1, int(round(img.height * scale)))), Image.Resampling.LANCZOS)
    width, height = img.size
    rows = max(1, int(np.ceil(len(palette.zones) / max(1, columns))))
    col_count = min(columns, len(palette.zones))
    tile_w = max(1, width)
    tile_h = max(1, height)
    gap = 10
    canvas = Image.new(
        "RGB",
        (tile_w * col_count + gap * (col_count - 1), tile_h * rows + gap * (rows - 1) + 24),
        (30, 30, 34),
    )
    for index, zone in enumerate(palette.zones):
        row, col = divmod(index, columns)
        x0 = col * (tile_w + gap)
        y0 = row * (tile_h + gap)
        mask = _zone_mask(palette, zone.id, (width, height))
        base = np.asarray(img, dtype=np.uint8)
        grey = np.full_like(base, 44, dtype=np.uint8)
        zone_view = np.where(mask[..., None], base, grey)
        tile = Image.fromarray(zone_view.astype(np.uint8), mode="RGB")
        label_box = Image.new("RGB", tile.size, (20, 20, 26))
        label_box.paste(tile, (0, 0))
        draw_ctx = ImageDraw.Draw(label_box)
        draw_ctx.rectangle((0, 0, label_box.width - 1, label_box.height - 1), outline=(255, 255, 255), width=1)
        draw_ctx.rounded_rectangle((8, 8, 92, 30), radius=5, fill=(18, 18, 22))
        draw_ctx.text((18, 12), f"{prefix}{zone.id}", fill=(245, 245, 250))
        canvas.paste(label_box, (x0, y0))
    return np.asarray(canvas, dtype=np.uint8)


def zone_overview(image_rgb: np.ndarray, palette: ZonePalette, prefix: str) -> np.ndarray:
    img = Image.fromarray(np.asarray(image_rgb, dtype=np.uint8)).convert("RGB")
    if max(img.size) > PACK_MAX_SIZE:
        scale = PACK_MAX_SIZE / float(max(img.size))
        img = img.resize((max(1, int(round(img.width * scale))), max(1, int(round(img.height * scale)))), Image.Resampling.LANCZOS)
    canvas = img.copy()
    draw = ImageDraw.Draw(canvas)
    for zone in palette.zones:
        x = int(zone.centroid_xy[0] / 100.0 * canvas.width)
        y = int(zone.centroid_xy[1] / 100.0 * canvas.height)
        label = f"{prefix}{zone.id}"
        draw.rectangle((x - 12, y - 12, x + 12, y + 12), outline=(255, 255, 255), width=2)
        draw.rounded_rectangle((x + 16, y - 12, x + 86, y + 14), radius=5, fill=(18, 18, 22))
        draw.text((x + 24, y - 8), label, fill=(245, 245, 250))
    return np.asarray(canvas, dtype=np.uint8)


def build_prompt(model: ZoneTransferModel) -> str:
    zones = ", ".join(f"S{zone.id}" for zone in model.source.zones)
    ref_zones = ", ".join(f"R{zone.id}" for zone in model.reference.zones)
    return f"""You are grading the source image toward the reference style.

Analysis ID: {model.analysis_id}
Source zones: {zones}
Reference zones: {ref_zones}

Rules:
- Match each source zone Sx to one or more reference zones Rx.
- Keep any zone that is clearly equivalent locked when appropriate.
- MULTIPLIENT: the final dose is the product of all strengths; a full intensity is the default, and the usual value for every link is "full".
- Garde 1.0 par défaut pour luminance_strength et chroma_strength; si you need to soften, prefer ≤ 0.5.
- N'invente AUCUNE valeur de couleur.
- Use a recipe format based on a JSON object with the field `format: "stealzalut.recipe"` and `version: 2`.
- The AI should emit a `links` array like {{"source_zone": 1, "targets": {{"2": 0.7, "3": 0.3}}, "strength": "full", "luminance_strength": 1.0, "chroma_strength": 1.0}}.
- If the source zone should remain untouched, use `{{"source_zone": 3, "keep": true}}`.
- PLEIN effet = "full"; if a tone must be less assertive, use "strong" or "medium".

```json
{{
  "format": "stealzalut.recipe",
  "version": 2,
  "analysis_id": "{model.analysis_id}",
  "mode": "zones",
  "links": [
    {{"source_zone": 1, "targets": {{"1": 1.0}}, "strength": "full", "luminance_strength": 1.0, "chroma_strength": 1.0}}
  ],
  "finishing": {{"saturation": 0.0, "vibrance": 0.0}}
}}
```
"""


def _write_image(path: Path, arr: np.ndarray, *, quality: int = 92, fmt: str = "JPEG") -> None:
    img = Image.fromarray(arr.astype(np.uint8)).convert("RGB")
    if fmt.upper() == "JPEG":
        img.save(path, format="JPEG", quality=quality, subsampling=0)
    else:
        img.save(path, format="PNG")


def write_pack(
    destination: str | Path,
    pack_name: str,
    source: np.ndarray,
    reference: np.ndarray,
    model: ZoneTransferModel,
    zone_count: int,
) -> Path:
    folder = Path(destination) / f"{pack_name}_stealzalut_pack"
    folder.mkdir(parents=True, exist_ok=True)
    for path in list(folder.iterdir()):
        if path.is_file():
            path.unlink()

    source_clean = _resize_to_limit(np.asarray(source, dtype=np.uint8), ORIGINAL_MAX_SIZE)
    ref_clean = _resize_to_limit(np.asarray(reference, dtype=np.uint8), ORIGINAL_MAX_SIZE)

    _write_image(folder / "1_source.jpg", source_clean, quality=92, fmt="JPEG")
    _write_image(folder / "2_reference.jpg", ref_clean, quality=92, fmt="JPEG")

    source_sheet = zone_sheet(source_clean, model.source, "S")
    reference_sheet = zone_sheet(ref_clean, model.reference, "R")
    source_overview = zone_overview(source_clean, model.source, "S")
    reference_overview = zone_overview(ref_clean, model.reference, "R")

    Image.fromarray(source_sheet.astype(np.uint8)).save(folder / "3_source_zones.png")
    Image.fromarray(reference_sheet.astype(np.uint8)).save(folder / "4_reference_zones.png")
    Image.fromarray(source_overview.astype(np.uint8)).save(folder / "5_source_overview.png")
    Image.fromarray(reference_overview.astype(np.uint8)).save(folder / "6_reference_overview.png")

    analysis = analysis_dict(model, tuple(source_clean.shape[1::-1]), tuple(ref_clean.shape[1::-1]), zone_count)
    (folder / "analysis.json").write_text(json.dumps(analysis, ensure_ascii=False, indent=2), encoding="utf-8")
    (folder / "PROMPT.md").write_text(build_prompt(model), encoding="utf-8")

    with zipfile.ZipFile(folder.with_suffix(".zip"), "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name in PACK_FILES:
            archive.write(folder / name, arcname=name)

    return folder
