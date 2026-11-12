import json
import sys
from pathlib import Path

import numpy as np

auto_path = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python"
sys.path.insert(0, str(auto_path))

from PIL import Image

from stealzalut.core.ai_pack import PACK_FILES, analysis_dict, build_prompt, write_pack
from stealzalut.core.recipe import RecipeError, dump_recipe, parse_recipe, recipe_to_dict
from stealzalut.core.zone_transfer import fit_zone_transfer


def _stripes(colors, height=200, width=80):
    img = np.zeros((height, width * len(colors), 3), dtype=np.uint8)
    for i, color in enumerate(colors):
        img[:, i * width : (i + 1) * width] = color
    return img


def test_recipe_parse_and_round_trip():
    text = '''
    ```json
    {
      "format": "stealzalut.recipe",
      "version": 2,
      "analysis_id": "abcd1234",
      "mode": "zones",
      "global": {"zone_separation": "soft", "mix": 0.9},
      "links": [
        {"source_zone": "S1", "targets": {"2": 0.7, "3": 0.3}, "strength": "strong", "luminance_strength": 1.0, "chroma_strength": 1.0},
        {"source_zone": 3, "keep": true}
      ]
    }
    ```
    '''
    recipe = parse_recipe(text)
    assert recipe.analysis_id == "abcd1234"
    assert recipe.mode == "zones"
    assert recipe.tau == 2.0
    assert recipe.links[0].targets == {2: 0.7, 3: 0.3}
    assert recipe.links[1].keep is True
    round_trip = parse_recipe(dump_recipe(recipe))
    assert round_trip.analysis_id == recipe.analysis_id
    assert round_trip.grading.get("mix") == 0.9


def test_recipe_rejects_invalid_json():
    try:
        parse_recipe('hello world')
    except RecipeError:
        return
    raise AssertionError("parse_recipe should reject free text without JSON")


def test_ai_pack_writes_expected_files(tmp_path):
    source = _stripes([(20, 40, 60), (120, 170, 200), (220, 160, 90)])
    reference = _stripes([(10, 80, 70), (200, 140, 80), (80, 40, 30), (250, 220, 100)], height=220, width=110)
    model = fit_zone_transfer(source, reference, zone_count=4)
    folder = write_pack(tmp_path, "demo", source, reference, model, 4)

    names = sorted(p.name for p in folder.iterdir())
    assert names == sorted(PACK_FILES)
    data = json.loads((folder / "analysis.json").read_text(encoding="utf-8"))
    assert data["analysis_id"] == model.analysis_id
    prompt = (folder / "PROMPT.md").read_text(encoding="utf-8")
    assert "stealzalut.recipe" in prompt
    assert model.analysis_id in prompt

    assert len(model.source.zones) > 0
    assert len(model.reference.zones) > 0
