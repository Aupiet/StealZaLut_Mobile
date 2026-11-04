"""Unit tests for the Preset Manager and Presets Bridge API (.szlpreset / JSON).

Validates:
- Preset structure creation and serialization conforming to JSON schema.
- Saving to disk, listing, loading, and deletion.
- Schema normalisation and fallback handling.
- Bridge functions: save_preset_bridge, load_preset_bridge, list_presets_bridge, delete_preset_bridge.
"""

from pathlib import Path
import pytest

from stealzalut.bridge import (
    delete_preset_bridge,
    list_presets_bridge,
    load_preset_bridge,
    save_preset_bridge,
)
from stealzalut.core.pipeline import Step2Params, Step3Params
from stealzalut.core.preset_manager import (
    create_preset,
    delete_preset,
    list_presets,
    load_preset,
    save_preset,
)


class TestPresetManager:
    """Validate core preset creation, persistence, and querying."""

    def test_create_preset_structure(self) -> None:
        p2 = Step2Params(global_intensity=0.85, luma_lock=0.30, skin_tone_lock=0.50)
        p3 = Step3Params(exposure=0.25, contrast=0.15, vibrance=0.40)

        preset = create_preset(
            name="Cinematic Teal",
            step2_params=p2,
            step3_params=p3,
            lut_reference="/path/to/lut.cube",
        )

        assert "id" in preset and len(preset["id"]) > 0
        assert preset["name"] == "Cinematic Teal"
        assert preset["lut_reference"] == "/path/to/lut.cube"
        assert preset["step2_params"]["global_intensity"] == 0.85
        assert preset["step2_params"]["luma_lock"] == 0.30
        assert preset["step3_params"]["exposure"] == 0.25
        assert preset["step3_params"]["contrast"] == 0.15

    def test_save_load_preset_lifecycle(self, tmp_path: Path) -> None:
        p2 = {"global_intensity": 0.70, "extremes_rolloff": 0.40}
        p3 = {"exposure": -0.5, "highlights": -0.3, "temperature": 0.2}

        preset = create_preset(
            name="Moody Film",
            step2_params=p2,
            step3_params=p3,
        )

        saved_path = save_preset(preset, directory=str(tmp_path))
        assert Path(saved_path).is_file()

        # Load back by path
        loaded = load_preset(saved_path)
        assert loaded["id"] == preset["id"]
        assert loaded["name"] == "Moody Film"
        assert loaded["step2_params"]["extremes_rolloff"] == 0.40
        assert loaded["step3_params"]["temperature"] == 0.20

        # Load back by ID
        loaded_by_id = load_preset(preset["id"], directory=str(tmp_path))
        assert loaded_by_id["id"] == preset["id"]

        # List presets in directory
        all_presets = list_presets(str(tmp_path))
        assert len(all_presets) >= 1
        assert any(p["id"] == preset["id"] for p in all_presets)

        # Delete preset
        deleted = delete_preset(preset["id"], directory=str(tmp_path))
        assert deleted is True
        assert not Path(saved_path).exists()


class TestPresetBridge:
    """Validate Chaquopy Kotlin-facing Preset Bridge API."""

    def test_bridge_preset_crud(self, tmp_path: Path) -> None:
        target_dir = str(tmp_path / "presets")

        # 1. Save
        res_save = save_preset_bridge(
            name="Vintage 70s",
            step2_params={"global_intensity": 0.9, "neutral_axis_lock": 0.6},
            step3_params={"contrast": 0.3, "grain": 0.4, "temperature": 0.15},
            output_dir=target_dir,
        )
        assert res_save["status"] == "success"
        preset_id = res_save["preset_id"]
        assert len(preset_id) > 0

        # 2. List
        res_list = list_presets_bridge(directory=target_dir)
        assert res_list["status"] == "success"
        assert res_list["count"] == 1
        assert res_list["presets"][0]["name"] == "Vintage 70s"

        # 3. Load
        res_load = load_preset_bridge(path_or_id=preset_id, directory=target_dir)
        assert res_load["status"] == "success"
        assert res_load["preset"]["step3_params"]["grain"] == 0.4

        # 4. Delete
        res_del = delete_preset_bridge(path_or_id=preset_id, directory=target_dir)
        assert res_del["status"] == "success"
        assert res_del["deleted"] is True

        res_list_after = list_presets_bridge(directory=target_dir)
        assert res_list_after["count"] == 0
