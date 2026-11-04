"""Editing Preset Manager (.szlpreset / JSON).

Handles serialization, loading, listing, and validation of editing presets
capturing the complete state of Stage 2 (Filter Refinement) and Stage 3 (Post-Grading),
along with optional 3D LUT references and base64 preview thumbnails.
"""

from __future__ import annotations

from dataclasses import asdict
import json
import os
import time
from typing import Any, Dict, List, Optional
import uuid

from stealzalut.core.pipeline import Step2Params, Step3Params


DEFAULT_PRESETS_DIR_NAME: str = "szl_presets"


def get_default_presets_dir() -> str:
    """Return default directory path for presets storage."""
    base_dir = os.environ.get("SZL_PRESETS_DIR", "")
    if not base_dir:
        # Fallback to user home / current working directory
        base_dir = os.path.join(os.path.expanduser("~"), ".stealzalut", DEFAULT_PRESETS_DIR_NAME)
    os.makedirs(base_dir, exist_ok=True)
    return os.path.abspath(base_dir)


def create_preset(
    name: str,
    step2_params: Optional[Step2Params | Dict[str, Any]] = None,
    step3_params: Optional[Step3Params | Dict[str, Any]] = None,
    lut_reference: Optional[str] = None,
    thumbnail_base64: Optional[str] = None,
    preset_id: Optional[str] = None,
) -> Dict[str, Any]:
    """Create a standardized editing preset dictionary.

    Args:
        name: User-facing preset name.
        step2_params: Stage 2 parameters (object or dict).
        step3_params: Stage 3 parameters (object or dict).
        lut_reference: Path or relative identifier to active 3D LUT (or None).
        thumbnail_base64: Base64-encoded thumbnail string (or None).
        preset_id: Optional UUID string (generated if not provided).

    Returns:
        Structured preset dictionary conforming to the .szlpreset schema.
    """
    p2 = Step2Params.from_dict(step2_params if isinstance(step2_params, dict) else asdict(step2_params) if step2_params else None)
    p3 = Step3Params.from_dict(step3_params if isinstance(step3_params, dict) else asdict(step3_params) if step3_params else None)

    return {
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
    p_id = preset_data.get("id") or str(uuid.uuid4())
    preset_data["id"] = p_id

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
    """Load and validate an editing preset from disk.

    Args:
        preset_path_or_id: File path or preset ID.
        directory: Directory to search in when a preset ID is provided.

    Returns:
        Validated preset dictionary.

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

    # Schema validation & normalisation
    if "id" not in data or "name" not in data:
        raise ValueError("Preset is missing required 'id' or 'name' fields.")

    # Normalize step2 and step3 params
    data["step2_params"] = Step2Params.from_dict(data.get("step2_params")).to_dict()
    data["step3_params"] = Step3Params.from_dict(data.get("step3_params")).to_dict()

    return data


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
                p["_file_path"] = fpath
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
