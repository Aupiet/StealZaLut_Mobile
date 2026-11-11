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
| C4 | Partial | Added a bounded, redacted local log buffer and next-launch crash-report writer/share prompt. Python-specific structured logging and release-device validation remain. | WP2 |
| C5 | Partial | Python calls remain synchronous on a shared single-thread dispatcher; coroutine cancellation cannot stop an already-running Chaquopy call. Timeout/queue behavior remains unresolved. | WP3 |
| C6 | Corrected | Suspend repository and delegate paths use cancellation-preserving `suspendRunCatching`; safe launches log failures and emit localized diagnostics. | WP2 |
| C7 | Corrected | Incoming image intents validate URI scheme and readability and report access failures instead of navigating with an invalid URI. | WP2 |
| C8 | Confirmed | `GpuPreviewEngine.kt:904-905` updates uniforms and creates the render effect outside the constructor's fallback `try/catch`. | WP6 |
| C9 | Partial | `ProjectRepository` uses a temporary-file rename for one write path, while other repositories write JSON directly and parsing paths require further review. | WP5 |
| C10 | Corrected | Removed the audited non-null assertions; the app's main Kotlin sources currently contain no `!!` assertions. | WP4 |
| C11 | Confirmed; corrected | Removed the machine-specific `org.gradle.java.home`; documented a user-level override in `README.md`. The Gradle daemon toolchain remains Java 25. | WP1 |
| C12 | Partial | `assembleRelease` succeeds with R8 enabled; runtime behavior on a release device remains unverified. | WP8 |

Latest validation after the diagnostics, runtime-startup, and sampled-bitmap changes:
`.\gradlew.bat compileDebugKotlin testDebugUnitTest` — BUILD SUCCESSFUL.

No device or emulator was available for reproducing the reported NumPy native crash or validating the large-image export path on the affected Redmi device.
