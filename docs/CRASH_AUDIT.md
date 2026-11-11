# Crash and stability audit

## Baseline (2026-11-11)

| Check | Result |
|---|---|
| `PYTHONPATH=app/src/main/python python -m pytest tests/ -v` | 119 passed in 12.49s (Python 3.9.13) |
| `.\gradlew.bat compileDebugKotlin testDebugUnitTest` | BUILD SUCCESSFUL in 14s |
| `python scripts/check_guard_violations.py` | 53 violations reported before changes |
| Redmi Android tombstone reproduction | Not available; the supplied excerpt contains only a PC in `_multiarray_umath.so`, without signal, fault address, or surrounding frames |
| Post-change Python suite | 120 passed in 10.95s |
| Post-change Kotlin compile and unit tests | BUILD SUCCESSFUL; includes `ExportMemoryGuardTest` |
| Post-change design-system guard | 53 pre-existing violations; no new violations |
| `.\gradlew.bat assembleRelease` | BUILD SUCCESSFUL in 5m 47s |

The excerpt identifies a native NumPy frame, but does not establish whether the fault is an out-of-memory condition, invalid native access, or another cause. It is not sufficient to attribute the crash to NumPy itself.

## Findings

| ID | Status | Evidence | Work package |
|---|---|---|---|
| C1 | Confirmed | `EditorEmptyState.kt:332` decodes a full-resolution bitmap in a remembered UI value. | WP4 |
| C2 | Confirmed; mitigation added, device validation pending | `export_worker.py` previously loaded the full image and skipped pre-downscaling when `max_export_dim == 0`. Exports now estimate memory headroom, bound the size, pre-decode only when memory requires it, and notify the user. | WP4 |
| C3 | Confirmed | `StealZaLutApplication.kt:29-37` starts Python asynchronously; repository calls use `Python.getInstance()` without awaiting startup. | WP3 |
| C4 | Confirmed | Startup has ad hoc Android logs, but no global crash reporter or structured Python logger was found in the initial audit. | WP2 |
| C5 | Partial | Python calls run synchronously on a single-thread dispatcher; coroutine cancellation cannot stop an already-running Chaquopy call. No call timeout was found. | WP3 |
| C6 | Confirmed | `runCatching` is used in suspend repository and delegate paths, so cancellation can be consumed. | WP2 |
| C7 | Confirmed | `MainActivity.handleIncomingIntent` reads intent data and navigates without handling URI/access exceptions or validating that the URI can be read. | WP2 |
| C8 | Confirmed | `GpuPreviewEngine.kt:904-905` updates uniforms and creates the render effect outside the constructor's fallback `try/catch`. | WP6 |
| C9 | Partial | `ProjectRepository` uses a temporary-file rename for one write path, while other repositories write JSON directly and parsing paths require further review. | WP5 |
| C10 | Confirmed | Five `!!` sites were found: `PythonBridgeRepository.kt:74`, `ProfileScreen.kt:275,395`, `PalettesScreen.kt:370`, `ImageDetailDialog.kt:160`, and `GradingPipelineDelegate.kt:745`. | WP4 |
| C11 | Confirmed; corrected | Removed the machine-specific `org.gradle.java.home`; documented a user-level override in `README.md`. The Gradle daemon toolchain remains Java 25. | WP1 |
| C12 | Partial | `assembleRelease` succeeds with R8 enabled; runtime behavior on a release device remains unverified. | WP8 |

No device or emulator was available for reproducing the reported NumPy native crash or validating the large-image export path on the affected Redmi device.
