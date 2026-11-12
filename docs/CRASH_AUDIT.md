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
| C1 | Mitigated; device validation pending | Project thumbnails now decode off the UI thread through a shared bounds-first loader capped at 600 px. Profile, preset, palette, and detail images use the same sampled loader. | WP4 |
| C2 | Confirmed; mitigation added, device validation pending | `export_worker.py` previously loaded the full image and skipped pre-downscaling when `max_export_dim == 0`. Exports now estimate memory headroom, bound the size, pre-decode only when memory requires it, and notify the user. | WP4 |
| C3 | Mitigated | `PythonRuntime.ensureStarted` serializes Chaquopy startup, and bridge calls await it before retrieving the Python instance. | WP3 |
| C4 | Improved; device validation pending | Added a bounded, redacted local event journal synced to private storage as events are logged and reloaded on the next launch, plus a next-launch crash-report prompt. The maintenance screen can inspect/share the journal and crash reports, import and inspect tombstones/logcat, review runtime memory/storage/Python status, share a local diagnostics snapshot, and clear preview cache. | WP2 |
| C5 | Partial | Preview requests now use a conflated latest-request state and a shared single-thread dispatcher. Coroutine cancellation still cannot stop an already-running synchronous Chaquopy call; no unsafe timeout is claimed. | WP3 |
| C6 | Corrected | Suspend repository and delegate paths use cancellation-preserving `suspendRunCatching`; safe launches log failures and emit localized diagnostics. | WP2 |
| C7 | Corrected | Incoming image intents validate URI scheme and readability and report access failures instead of navigating with an invalid URI. | WP2 |
| C8 | Mitigated; device validation pending | Shader construction, uniform updates, and render-effect creation now fall back to the existing color-matrix path and log the failure. | WP6 |
| C9 | Mitigated | Project, preset, and saved-image JSON now use synchronized temporary writes, safe replacement, backup recovery, and corrupt-file quarantine. | WP5 |
| C10 | Corrected | Removed the audited non-null assertions; the app's main Kotlin sources currently contain no `!!` assertions. | WP4 |
| C11 | Confirmed; corrected | Removed the machine-specific `org.gradle.java.home`; documented a user-level override in `README.md`. The Gradle daemon toolchain remains Java 25. | WP1 |
| C12 | Build verified; device validation pending | Release minification and resource shrinking are checked by `assembleRelease` locally and in CI. Runtime behavior on the affected Redmi device remains unverified. | WP8 |

## Latest verification

- `.\gradlew.bat compileDebugKotlin testDebugUnitTest` — BUILD SUCCESSFUL.
- `PYTHONPATH=app/src/main/python python -m pytest tests/ -q` — 120 passed.
- `python scripts/check_guard_violations.py` — 53 violations, unchanged from the 53-violation baseline.
- `.\gradlew.bat assembleRelease` — BUILD SUCCESSFUL in 6m 6s, including R8 and resource shrinking.
- CI now uses the configured JDK 25 daemon and builds/tests both Debug and Release.

No device or emulator was available for reproducing the reported NumPy native crash or validating the large-image export path on the affected Redmi device.

The maintenance screen explicitly notes that native signals such as `SIGSEGV` may not be captured by the in-process Java/Kotlin uncaught-exception handler. Application events are persisted locally as they are logged, so the recent event history remains available after a process crash. Users can import a tombstone or logcat file from Android's document picker; files remain local unless explicitly shared.
