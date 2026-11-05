"""Editing Preset Manager (.szlpreset / JSON).

Handles serialization, loading, listing, validation, migration, duplicating,
and renaming of editing presets capturing the complete state of Stage 2
(Filter Refinement) and Stage 3 (Post-Grading), along with optional 3D LUT
references and base64 preview thumbnails. Conforms to schema_version 2.
"""

from __future__ import annotations

from dataclasses import asdict
import json
import os
import shutil
import time
from typing import Any, Dict, List, Optional
import uuid

from stealzalut.core.pipeline import Step2Params, Step3Params


DEFAULT_PRESETS_DIR_NAME: str = "szl_presets"
SCHEMA_VERSION: int = 2


def get_default_presets_dir() -> str:
    """Return default directory path for presets storage."""
    base_dir = os.environ.get("SZL_PRESETS_DIR", "")
    if not base_dir:
        # Fallback to user home / current working directory
        base_dir = os.path.join(os.path.expanduser("~"), ".stealzalut", DEFAULT_PRESETS_DIR_NAME)
    os.makedirs(base_dir, exist_ok=True)
    return os.path.abspath(base_dir)


def migrate_preset(data: Dict[str, Any]) -> Dict[str, Any]:
    """Migrate legacy preset schema (v1 or unversioned) to current schema v2.

    Ensures all parameters conform to Step2Params and Step3Params defaults,
    timestamps are valid, and schema_version is normalized to SCHEMA_VERSION.

    Args:
        data: Raw preset dictionary.

    Returns:
        Migrated and validated preset dictionary conforming to schema v2.
    """
    version = data.get("schema_version", 1)
    data["schema_version"] = SCHEMA_VERSION

    if "id" not in data or not data["id"]:
        data["id"] = str(uuid.uuid4())

    if "name" not in data or not str(data["name"]).strip():
        data["name"] = "Sans titre"
    else:
        data["name"] = str(data["name"]).strip()

    if "created_at" not in data or not isinstance(data["created_at"], (int, float)):
        data["created_at"] = int(time.time() * 1000)

    # Normalize parameters via dataclasses (ensuring robust defaults)
    data["step2_params"] = Step2Params.from_dict(data.get("step2_params")).to_dict()
    data["step3_params"] = Step3Params.from_dict(data.get("step3_params")).to_dict()

    if "lut_reference" not in data:
        data["lut_reference"] = None

    if "thumbnail_base64" not in data:
        data["thumbnail_base64"] = None

    return data


def create_preset(
    name: str,
    step2_params: Optional[Step2Params | Dict[str, Any]] = None,
    step3_params: Optional[Step3Params | Dict[str, Any]] = None,
    lut_reference: Optional[str] = None,
    thumbnail_base64: Optional[str] = None,
    preset_id: Optional[str] = None,
    schema_version: int = SCHEMA_VERSION,
) -> Dict[str, Any]:
    """Create a standardized editing preset dictionary.

    Args:
        name: User-facing preset name.
        step2_params: Stage 2 parameters (object or dict).
        step3_params: Stage 3 parameters (object or dict).
        lut_reference: Path or relative identifier to active 3D LUT (or None).
        thumbnail_base64: Base64-encoded thumbnail string (or None).
        preset_id: Optional UUID string (generated if not provided).
        schema_version: Schema version number (default: SCHEMA_VERSION).

    Returns:
        Structured preset dictionary conforming to the .szlpreset schema v2.
    """
    p2 = Step2Params.from_dict(step2_params if isinstance(step2_params, dict) else asdict(step2_params) if step2_params else None)
    p3 = Step3Params.from_dict(step3_params if isinstance(step3_params, dict) else asdict(step3_params) if step3_params else None)

    return {
        "schema_version": schema_version,
        "id": preset_id or str(uuid.uuid4()),
        "name": name.strip() or "Sans titre",
        "created_at": int(time.time() * 1000),
        "thumbnail_base64": thumbnail_base64,
        "lut_reference": lut_reference,
        "step2_params": p2.to_dict(),
        "step3_params": p3.to_dict(),
    }


def save_preset(
    preset_data: Dict[str, Any],
    filepath: Optional[str] = None,
    directory: Optional[str] = None,
) -> str:
    """Persist a preset dictionary to disk as a .szlpreset / .json file.

    Args:
        preset_data: Preset dictionary to serialize.
        filepath: Direct output file path (if specified).
        directory: Directory where file will be written if filepath is not given.

    Returns:
        Absolute path of the saved preset file.
    """
    preset_data = migrate_preset(preset_data)
    p_id = preset_data["id"]

    if filepath:
        out_path = os.path.abspath(filepath)
    else:
        target_dir = os.path.abspath(directory) if directory else get_default_presets_dir()
        os.makedirs(target_dir, exist_ok=True)
        safe_name = "".join(c for c in preset_data.get("name", "preset") if c.isalnum() or c in ("-", "_")).strip()
        filename = f"{safe_name}_{p_id}.szlpreset"
        out_path = os.path.join(target_dir, filename)

    os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump(preset_data, fh, indent=2, ensure_ascii=False)

    return out_path


def load_preset(
    preset_path_or_id: str,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Load, validate, and migrate an editing preset from disk.

    Args:
        preset_path_or_id: File path or preset ID.
        directory: Directory to search in when a preset ID is provided.

    Returns:
        Validated and migrated preset dictionary conforming to schema v2.

    Raises:
        FileNotFoundError: If the preset file cannot be located.
        ValueError: If the file is not valid JSON or lacks mandatory fields.
    """
    target_path = preset_path_or_id
    if not os.path.isfile(target_path):
        # Check in specified directory or default presets directory
        search_dir = directory or get_default_presets_dir()
        found = False
        if os.path.isdir(search_dir):
            for fname in os.listdir(search_dir):
                if fname.endswith((".szlpreset", ".json")):
                    candidate = os.path.join(search_dir, fname)
                    if preset_path_or_id in fname:
                        target_path = candidate
                        found = True
                        break
                    try:
                        with open(candidate, "r", encoding="utf-8") as f_check:
                            c_data = json.load(f_check)
                            if c_data.get("id") == preset_path_or_id:
                                target_path = candidate
                                found = True
                                break
                    except Exception:
                        continue
        if not found:
            raise FileNotFoundError(f"Preset file not found: {preset_path_or_id}")

    with open(target_path, "r", encoding="utf-8") as fh:
        data = json.load(fh)

    if not isinstance(data, dict):
        raise ValueError("Invalid preset content: root must be a JSON object.")

    migrated = migrate_preset(data)
    migrated["_file_path"] = os.path.abspath(target_path)
    return migrated


def list_presets(directory: Optional[str] = None) -> List[Dict[str, Any]]:
    """List all available presets in the specified directory.

    Args:
        directory: Directory path to scan (or default if None).

    Returns:
        List of loaded preset dictionaries sorted by creation timestamp (newest first).
    """
    target_dir = os.path.abspath(directory) if directory else get_default_presets_dir()
    if not os.path.isdir(target_dir):
        return []

    presets: List[Dict[str, Any]] = []
    for fname in os.listdir(target_dir):
        if fname.endswith((".szlpreset", ".json")):
            fpath = os.path.join(target_dir, fname)
            try:
                p = load_preset(fpath)
                presets.append(p)
            except Exception:
                continue

    presets.sort(key=lambda p: p.get("created_at", 0), reverse=True)
    return presets


def delete_preset(preset_id_or_path: str, directory: Optional[str] = None) -> bool:
    """Delete a preset file from disk.

    Args:
        preset_id_or_path: File path or preset ID.
        directory: Directory to search in if an ID was passed.

    Returns:
        True if the file was deleted, False otherwise.
    """
    target_path = preset_id_or_path
    if not os.path.isfile(target_path):
        search_dir = directory or get_default_presets_dir()
        if os.path.isdir(search_dir):
            for fname in os.listdir(search_dir):
                if fname.endswith((".szlpreset", ".json")):
                    candidate = os.path.join(search_dir, fname)
                    if preset_id_or_path in fname:
                        target_path = candidate
                        break
                    try:
                        with open(candidate, "r", encoding="utf-8") as f_check:
                            c_data = json.load(f_check)
                            if c_data.get("id") == preset_id_or_path:
                                target_path = candidate
                                break
                    except Exception:
                        continue

    if os.path.isfile(target_path):
        try:
            os.remove(target_path)
            return True
        except OSError:
            return False
    return False


def duplicate_preset(
    preset_id_or_path: str,
    new_name: Optional[str] = None,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Duplicate an existing preset with a new ID and name.

    Args:
        preset_id_or_path: File path or preset ID of the source preset.
        new_name: Optional custom name (defaults to '<original_name> (Copie)').
        directory: Working directory.

    Returns:
        The newly created duplicated preset dictionary.
    """
    source = load_preset(preset_id_or_path, directory=directory)
    base_name = source.get("name", "Preset")
    copy_name = new_name.strip() if new_name and new_name.strip() else f"{base_name} (Copie)"

    new_preset = create_preset(
        name=copy_name,
        step2_params=source.get("step2_params"),
        step3_params=source.get("step3_params"),
        lut_reference=source.get("lut_reference"),
        thumbnail_base64=source.get("thumbnail_base64"),
    )
    saved_path = save_preset(new_preset, directory=directory)
    new_preset["_file_path"] = saved_path
    return new_preset


def rename_preset(
    preset_id_or_path: str,
    new_name: str,
    directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Rename an existing preset.

    Args:
        preset_id_or_path: File path or preset ID of the preset to rename.
        new_name: New name for the preset.
        directory: Working directory.

    Returns:
        The updated preset dictionary.
    """
    existing = load_preset(preset_id_or_path, directory=directory)
    old_file_path = existing.get("_file_path")

    cleaned_name = new_name.strip()
    if not cleaned_name:
        raise ValueError("Preset name cannot be empty.")

    existing["name"] = cleaned_name
    new_path = save_preset(existing, directory=directory)
    existing["_file_path"] = new_path

    # Clean up old file if name change caused a new filename
    if old_file_path and os.path.isfile(old_file_path) and os.path.abspath(old_file_path) != os.path.abspath(new_path):
        try:
            os.remove(old_file_path)
        except OSError:
            pass

    return existing


def export_preset(
    preset_id_or_path: str,
    export_path: str,
    directory: Optional[str] = None,
) -> str:
    """Export a preset to a standalone .szlpreset file at export_path.

    Args:
        preset_id_or_path: Preset identifier or path.
        export_path: Destination file path.
        directory: Directory to search preset in.

    Returns:
        Canonical absolute export path.
    """
    preset = load_preset(preset_id_or_path, directory=directory)
    # Remove internal metadata key before export
    clean_data = {k: v for k, v in preset.items() if not k.startswith("_")}

    # Bundle referenced 3D LUT directly into the preset if available on disk for cross-device portability
    lut_ref = clean_data.get("lut_reference")
    if lut_ref and os.path.isfile(lut_ref):
        try:
            with open(lut_ref, "r", encoding="utf-8", errors="replace") as fh:
                clean_data["lut_embedded_cube"] = fh.read()
            clean_data["lut_filename"] = os.path.basename(lut_ref)
        except Exception:
            pass

    os.makedirs(os.path.dirname(os.path.abspath(export_path)) or ".", exist_ok=True)
    with open(export_path, "w", encoding="utf-8") as fh:
        json.dump(clean_data, fh, indent=2, ensure_ascii=False)
    return os.path.abspath(export_path)


def import_preset(
    import_path: str,
    target_directory: Optional[str] = None,
) -> Dict[str, Any]:
    """Import a .szlpreset file from an external path into the presets directory.

    Validates schema, unbundles any embedded 3D LUT into local storage, and migrates if needed.

    Args:
        import_path: External file path to import.
        target_directory: Storage directory to import into.

    Returns:
        The imported and registered preset dictionary.
    """
    if not os.path.isfile(import_path):
        raise FileNotFoundError(f"File to import not found: {import_path}")

    preset = load_preset(import_path)
    clean_data = {k: v for k, v in preset.items() if not k.startswith("_")}
    target_dir = target_directory or get_default_presets_dir()

    # Unpack embedded 3D LUT if bundled in preset
    embedded_cube = clean_data.get("lut_embedded_cube")
    lut_filename = clean_data.get("lut_filename") or f"preset_{clean_data.get('id', 'imported')}.cube"
    if embedded_cube:
        luts_dir = os.path.join(target_dir, "imported_luts")
        os.makedirs(luts_dir, exist_ok=True)
        local_cube_path = os.path.join(luts_dir, lut_filename)
        with open(local_cube_path, "w", encoding="utf-8") as fh:
            fh.write(embedded_cube)
        clean_data["lut_reference"] = os.path.abspath(local_cube_path)
    else:
        # Check if legacy absolute lut_reference is present but points to a non-existent path on this device
        old_ref = clean_data.get("lut_reference")
        if old_ref and not os.path.isfile(old_ref):
            candidate = os.path.join(target_dir, "imported_luts", os.path.basename(old_ref))
            if os.path.isfile(candidate):
                clean_data["lut_reference"] = os.path.abspath(candidate)
            else:
                clean_data["lut_reference"] = None
                clean_data["lut_missing_reference"] = old_ref

    # Generate new ID if preset with same ID already exists in target
    existing_ids = {p["id"] for p in list_presets(target_dir)}
    if clean_data["id"] in existing_ids:
        clean_data["id"] = str(uuid.uuid4())
        clean_data["name"] = f"{clean_data['name']} (Importé)"

    saved_path = save_preset(clean_data, directory=target_dir)
    clean_data["_file_path"] = saved_path
    return clean_data
