"""Unit tests for the Preset Manager and Presets Bridge API (.szlpreset / JSON).

Validates:
- Preset structure creation and serialization conforming to JSON schema v2.
- Migration from legacy unversioned/v1 presets to schema v2.
- Duplicate, rename, export, and import operations.
- Saving to disk, listing, loading, and deletion.
- Schema normalisation and fallback handling.
- Bridge functions: save, load, list, delete, duplicate, rename, export, import.
"""

from pathlib import Path

from stealzalut.bridge import (
    delete_preset_bridge,
    duplicate_preset_bridge,
    export_preset_bridge,
    import_preset_bridge,
    list_presets_bridge,
    rename_preset_bridge,
    save_preset_bridge,
)
from stealzalut.core.pipeline import Step2Params, Step3Params
from stealzalut.core.preset_manager import (
    SCHEMA_VERSION,
    create_preset,
    delete_preset,
    duplicate_preset,
    export_preset,
    import_preset,
    list_presets,
    load_preset,
    migrate_preset,
    rename_preset,
    save_preset,
)


class TestPresetManager:
    """Validate core preset creation, persistence, querying, and migration."""

    def test_create_preset_structure(self) -> None:
        p2 = Step2Params(global_intensity=0.85, luma_lock=0.30, skin_tone_lock=0.50)
        p3 = Step3Params(exposure=0.25, contrast=0.15, vibrance=0.40)

        preset = create_preset(
            name="Cinematic Teal",
            step2_params=p2,
            step3_params=p3,
            lut_reference="/path/to/lut.cube",
        )

        assert preset["schema_version"] == SCHEMA_VERSION
        assert "id" in preset and len(preset["id"]) > 0
        assert preset["name"] == "Cinematic Teal"
        assert preset["lut_reference"] == "/path/to/lut.cube"
        assert preset["step2_params"]["global_intensity"] == 0.85
        assert preset["step2_params"]["luma_lock"] == 0.30
        assert preset["step3_params"]["exposure"] == 0.25
        assert preset["step3_params"]["contrast"] == 0.15

    def test_migration_legacy_preset(self) -> None:
        """Validate that legacy unversioned / v1 presets migrate gracefully to v2."""
        legacy_data = {
            "id": "legacy-id-123",
            "name": "Old School Look",
            "step2_params": {"global_intensity": 0.5},
            # step3_params completely omitted
        }

        migrated = migrate_preset(legacy_data)
        assert migrated["schema_version"] == 2
        assert migrated["id"] == "legacy-id-123"
        assert migrated["name"] == "Old School Look"
        assert migrated["step2_params"]["global_intensity"] == 0.5
        assert migrated["step2_params"]["luma_lock"] == 0.0  # default filled
        assert "exposure" in migrated["step3_params"]  # default filled
        assert migrated["step3_params"]["exposure"] == 0.0
        assert "created_at" in migrated

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
        assert loaded["schema_version"] == 2

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

    def test_duplicate_preset(self, tmp_path: Path) -> None:
        orig = create_preset("Original Look", step3_params={"contrast": 0.4})
        save_preset(orig, directory=str(tmp_path))

        # Duplicate with default name
        dup = duplicate_preset(orig["id"], directory=str(tmp_path))
        assert dup["id"] != orig["id"]
        assert dup["name"] == "Original Look (Copie)"
        assert dup["step3_params"]["contrast"] == 0.4

        # Duplicate with custom name
        dup2 = duplicate_preset(orig["id"], new_name="Custom Copy", directory=str(tmp_path))
        assert dup2["name"] == "Custom Copy"

    def test_rename_preset(self, tmp_path: Path) -> None:
        orig = create_preset("Pre Rename", step3_params={"exposure": 0.1})
        save_preset(orig, directory=str(tmp_path))

        renamed = rename_preset(orig["id"], "Post Rename", directory=str(tmp_path))
        assert renamed["name"] == "Post Rename"
        assert renamed["id"] == orig["id"]

    def test_export_and_import_preset(self, tmp_path: Path) -> None:
        source_dir = str(tmp_path / "source")
        target_dir = str(tmp_path / "target")
        export_file = str(tmp_path / "exported.szlpreset")

        orig = create_preset("Exportable", step3_params={"grain": 0.5})
        save_preset(orig, directory=source_dir)

        # Export
        out = export_preset(orig["id"], export_path=export_file, directory=source_dir)
        assert Path(out).is_file()

        # Import into fresh directory
        imported = import_preset(out, target_directory=target_dir)
        assert imported["name"] == "Exportable"
        assert imported["step3_params"]["grain"] == 0.5

    def test_export_and_import_preset_with_embedded_lut(self, tmp_path: Path) -> None:
        source_dir = str(tmp_path / "device_a_presets")
        target_dir = str(tmp_path / "device_b_presets")
        export_file = str(tmp_path / "shared.szlpreset")

        # Create a mock .cube file on "device A"
        cube_file = tmp_path / "device_a_storage" / "cinematic.cube"
        cube_file.parent.mkdir(parents=True, exist_ok=True)
        cube_file.write_text('TITLE "Cinematic"\nLUT_3D_SIZE 2\n0.0 0.0 0.0\n0.0 0.0 1.0\n')

        orig = create_preset("WithLUT", lut_reference=str(cube_file))
        save_preset(orig, directory=source_dir)

        # Export on device A
        out = export_preset(orig["id"], export_path=export_file, directory=source_dir)

        # Import on device B
        imported = import_preset(out, target_directory=target_dir)
        assert imported["lut_reference"] is not None
        assert Path(imported["lut_reference"]).is_file()
        assert Path(imported["lut_reference"]).read_text() == cube_file.read_text()
        # Verify it unpacked inside device B's imported_luts directory
        assert "device_b_presets" in imported["lut_reference"]


class TestPresetBridge:
    """Validate Chaquopy Kotlin-facing Preset Bridge API."""

    def test_bridge_preset_crud_and_extended_ops(self, tmp_path: Path) -> None:
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

        # 2. Duplicate
        res_dup = duplicate_preset_bridge(path_or_id=preset_id, new_name="Vintage 70s B", directory=target_dir)
        assert res_dup["status"] == "success"
        assert res_dup["preset"]["name"] == "Vintage 70s B"
        dup_id = res_dup["preset_id"]

        # 3. Rename
        res_ren = rename_preset_bridge(path_or_id=dup_id, new_name="Vintage Golden", directory=target_dir)
        assert res_ren["status"] == "success"
        assert res_ren["preset"]["name"] == "Vintage Golden"

        # 4. List
        res_list = list_presets_bridge(directory=target_dir)
        assert res_list["status"] == "success"
        assert res_list["count"] == 2

        # 5. Export
        export_target = str(tmp_path / "standalone.szlpreset")
        res_exp = export_preset_bridge(path_or_id=preset_id, export_path=export_target, directory=target_dir)
        assert res_exp["status"] == "success"
        assert Path(export_target).is_file()

        # 6. Import into SAME directory (tests duplicate ID handling)
        res_imp = import_preset_bridge(import_path=export_target, directory=target_dir)
        assert res_imp["status"] == "success"
        # Duplicate ID should trigger new UUID generation to prevent overwriting
        assert res_imp["preset"]["id"] != preset_id
        assert "Vintage 70s" in res_imp["preset"]["name"]
        assert "(Importé)" in res_imp["preset"]["name"]

        # 7. Delete
        res_del = delete_preset_bridge(path_or_id=preset_id, directory=target_dir)
        assert res_del["status"] == "success"
        assert res_del["deleted"] is True
