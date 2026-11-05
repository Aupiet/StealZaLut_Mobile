# Registre des Décisions d'Architecture et de Conception (ADR)

Ce document consigne les décisions structurantes prises pour professionnaliser, stabiliser et faire évoluer **StealZaLUT Mobile** dans le respect strict des invariants de la charte (`CODING_STANDARDS.md`, `ARCHITECTURE.md`).

---

## ADR-01 : Design System Multi-Thèmes & Rôles Sémantiques Étendus

- **Contexte :** L'ancien système de couleurs nommait les teintes par couleur brute (`AccentAmber`, `AccentCyan`), rendant impossible un basculement de thème cohérent. Un seul thème sombre existait, le paramètre `darkTheme` était ignoré, et un risque de flash blanc existait au démarrage.
- **Décision :**
  1. Séparation stricte entre données de référence physique (mire ColorChecker dans `ColorCardReference.kt`) et jetons d'interface.
  2. Création de `SzlColors` via `CompositionLocalProvider` pour les rôles d'imagerie spécifiques (`compareBadge`, `overlayOnImage`, `scrim`, `sliderActive`, `sliderBipolarFill`).
  3. Registre de 4 thèmes complets (`AmberNoir`, `CyanStudio`, `OledBlack`, `Daylight`) implémentant le contrat `ThemeSpec`.
  4. Élimination du flash de démarrage via `windowBackground` synchronisé dans `styles.xml` / `themes.xml`.
  5. Test JVM automatisé de garde (`DesignSystemGuardTest`) et test de conformité contraste WCAG AA (`ThemeContrastTest`) sur tous les thèmes.

---

## ADR-02 : Décomposition des Monolithes par Domaine et Délégués

- **Contexte :** Quatre fichiers dépassaient 1 000 lignes (`StealZaLutViewModel.kt` ~2 022 l., `ApplyThemeScreen.kt` ~1 770 l., `StandaloneEditorScreen.kt` ~1 150 l., `PythonBridgeRepository.kt` ~1 090 l.).
- **Décision :**
  1. `ApplyThemeScreen.kt` (196 lignes) orchestre 7 sous-composants dédiés dans `ui/screens/studio/` (`StudioStepper`, `StudioStep1Source` à `StudioStep5Result`, `StudioComponents`).
  2. `StandaloneEditorScreen.kt` (310 lignes) orchestre 6 onglets/vues dans `ui/screens/editor/` (`EditorViewport`, `EditorPresetsTab`, `EditorCalibrationTab`, `EditorLightTab`, `EditorColorTab`, `EditorEffectsTab`).
  3. `StealZaLutViewModel.kt` (360 lignes) délègue sa logique métier à 4 délégués spécialisés sous `ui/viewmodel/delegate/` (`StudioFlowDelegate`, `GradingPipelineDelegate`, `EditorSessionDelegate`, `PresetLibraryDelegate`) en conservant l'immuabilité UDF (`StateFlow<StealZaLutUiState>`).
  4. Suppression du code mort dupliqué (`ColorGradingViewModel.kt` et son test).

---

## ADR-03 : Ergonomie Tactile Pro & Comparateur Triple Mode

- **Contexte :** La comparaison d'image était limitée et l'éditeur ne permettait pas de revenir en arrière facilement sur mobile.
- **Décision :**
  1. `DoubleViewComparison.kt` unifié supporte désormais 3 modes d'inspection :
     - `SPLIT_WIPE` (rideau interactif tactile).
     - `SIDE_BY_SIDE` (côte à côte).
     - `HOLD_TO_COMPARE` (appui maintenu avec badge sémantique `SzlImageBadge`).
  2. Panoramique et Zoom synchronisés de $1\times$ à $4\times$ avec réinitialisation par double-tap.
  3. Pile d'annulation/rétablissement (Undo/Redo) dans la barre supérieure de l'éditeur autonome.
  4. Ajout d'un histogramme RVB temps réel superposable (`SzlRgbHistogram`).

---

## ADR-04 : Schéma de Presets v2, Rétro-compatibilité & CRUD Étendu

- **Contexte :** Les fichiers `.szlpreset` n'étaient pas versionnés et risquaient de planter si des paramètres manquaient.
- **Décision :**
  1. Spécification de `SCHEMA_VERSION = 2` dans `preset_manager.py`.
  2. Implémentation de `migrate_preset` garantissant une mise à niveau transparente et rétro-compatible des anciens fichiers non versionnés ou v1.
  3. Ajout des fonctionnalités Duplication, Renommage, Export et Import de fichiers `.szlpreset` dans le moteur Python, la passerelle Chaquopy et l'interface utilisateur.

---

## ADR-05 : Tiling Mémoire Anti-OOM pour Export HD (> 12 MP) & Préservation EXIF

- **Contexte :** L'export d'images haute résolution (12 MP à 100 MP) générait des allocations float32 volumineuses risquant de déclencher un Out-Of-Memory (OOM) sur mobile. Les métadonnées EXIF (orientation, date, appareil) étaient également perdues.
- **Décision :**
  1. `apply_lut` découpe automatiquement le traitement en tranches de rangées contiguës plafonnées à 2 Mpx (`LUT_CHUNK_PIXEL_LIMIT`), limitant la mémoire vive crête à $< 100\text{ Mo}$.
  2. L'interpolation trilinéaire étant une opération strictement ponctuelle, le découpage garantit une équivalence binaire à 100% avec zéro artefact de raccord aux bords de tuiles.
  3. Extraction systématique des métadonnées EXIF du cliché source via `get_clean_exif_bytes`, normalisation du tag d'orientation à 1 (`Normal`) pour éviter toute double rotation, et réinjection dans l'image JPEG finale.
