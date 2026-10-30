"""File-system path helpers for Android Chaquopy runtime.

Provides validation, directory creation, sanitised filename generation,
and temporary cache cleanup.
"""

from __future__ import annotations

import os
import re
from datetime import datetime
from typing import List


def validate_path(path: str) -> str:
    """Validate that *path* points to an existing file.

    Args:
        path: Absolute or relative file path.

    Returns:
        The canonical (absolute, normalised) path.

    Raises:
        FileNotFoundError: If the file does not exist.
    """
    canonical: str = os.path.realpath(path)
    if not os.path.isfile(canonical):
        raise FileNotFoundError(f"File not found: {canonical}")
    return canonical


def validate_directory(path: str) -> str:
    """Ensure *path* exists as a directory, creating it if necessary.

    Args:
        path: Absolute or relative directory path.

    Returns:
        The canonical (absolute, normalised) directory path.
    """
    canonical: str = os.path.realpath(path)
    os.makedirs(canonical, exist_ok=True)
    return canonical


def sanitize_filename(name: str) -> str:
    """Strip characters that are unsafe in file names.

    Only ASCII letters, digits, hyphens, underscores, and dots are kept.
    Consecutive unsafe characters are collapsed into a single underscore.

    Args:
        name: Raw filename (without directory component).

    Returns:
        A sanitised filename safe for Android storage.
    """
    sanitised: str = re.sub(r"[^A-Za-z0-9._-]", "_", name)
    sanitised = re.sub(r"\.{2,}", "_", sanitised)
    sanitised = re.sub(r"_+", "_", sanitised)
    return sanitised.strip("_.")


def generate_cube_filename(prefix: str = "StealZaLUT") -> str:
    """Generate a timestamped ``.cube`` filename.

    The format is ``{prefix}_{YYYYMMDD}_{HHMMSS}.cube``.

    Args:
        prefix: Human-readable prefix for the file.

    Returns:
        Sanitised filename string ending in ``.cube``.
    """
    timestamp: str = datetime.now().strftime("%Y%m%d_%H%M%S")
    raw: str = f"{prefix}_{timestamp}.cube"
    return sanitize_filename(raw)


def cleanup_temp_files(cache_dir: str, max_files: int = 20) -> int:
    """Clean up old temporary preview/export files in cache directory.

    Sorts files by modification time and removes the oldest files when the
    total count exceeds `max_files`, ensuring the app never bloats disk storage.

    Args:
        cache_dir: Path to directory holding temporary cache files.
        max_files: Maximum number of files to keep.

    Returns:
        Number of deleted files.
    """
    if not os.path.isdir(cache_dir):
        return 0

    try:
        entries: List[str] = [
            os.path.join(cache_dir, f)
            for f in os.listdir(cache_dir)
            if os.path.isfile(os.path.join(cache_dir, f))
        ]

        if len(entries) <= max_files:
            return 0

        # Sort by modification time (oldest first)
        entries.sort(key=lambda p: os.path.getmtime(p))

        excess_count = len(entries) - max_files
        deleted = 0
        for i in range(excess_count):
            try:
                os.remove(entries[i])
                deleted += 1
            except OSError:
                pass

        return deleted
    except Exception:
        return 0
