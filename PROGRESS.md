# Suivi d'Avancement StealZaLUT Mobile

## Métriques Avant / Après
- Violations garde : 37 → 0
- Tests Pytest : 134 → 142 passés
- Tests unitaires Kotlin : 117 → 120 passés
- Latence preview perçue : 200 ms (debounce fixe) → < 1 ms (GPU) / 80-600 ms adaptatif (trailing)

## Tâches Réalisées
- [x] 3a. Fix double application post-LUT (`apply_step3_spatial_effects`)
- [x] 1. Zones overrides (`zoneUserOverrides` propagé, hash cache `_RAW_LUT_CACHE`, `with_table`)
- [x] 2. Courbe & HSL GPU (AGSL 1D LUT $256\times 1$ Fritsch-Carlson, Point Color, eye toggles)
- [x] 3b-c. Auto-edit idempotence (`editorSourcePath`, suppression duplication `step3*`, 1 undo)
- [x] 4. Synchro preview (`AdaptiveThrottleLatest`, `requestId` anti-obsolescence, delta $\Delta = P_n - P_k$)
- [x] 5. Audit UI (tokens sémantiques, 0 violation garde, zero-scroll responsive)
- [x] 6. Analyse comparative (`ambiance` transfer mood Oklab, presse-papier `EditorClipboardHolder`)
- [x] 7. Nettoyage code mort (suppression doublon `colorspace.py`, imports `color_space.py`)
- [x] 8. Documentation & ADRs (ADR-08 à ADR-23, arborescence §7, chiffres mesurés, untrack `.idea`)
