# Rapport d'Audit Exhaustif — StealZaLUT Mobile

**Date :** 5 novembre 2026  
**Auteur :** Antigravity Engine (Senior Android / Imagerie Couleur / UI-UX)  
**Version de référence :** `StealZaLut_Mobile` master  

---

## 1. Arborescence Réelle vs `ARCHITECTURE.md §7`

L'arborescence documentée dans `ARCHITECTURE.md §7` est incomplète et omet de nombreux composants clés du projet :

| Chemin / Composant Réel | Présent dans `ARCHITECTURE.md §7` ? | Rôle & Statut |
| :--- | :---: | :--- |
| `stealzalut/core/color_card.py` | ❌ Omis | Génération des mires d'étalonnage ColorChecker |
| `stealzalut/utils/` (`image_io.py`, `paths.py`, `__init__.py`) | ❌ Omis | I/O Pillow, downsampling proxys, conversion uint8/float32, extraction de palettes |
| `tests/` (`test_auto_edit.py`, `test_bridge.py`, `test_color_transfer.py`, `test_colorset_worker.py`) | ❌ Omis | 4 suites de tests sur 8 omises (seuls 4 fichiers sont listés) |
| `core/AppConstants.kt`, `core/AppDimens.kt` | ❌ Omis | Références centralisées des dimensions et logiques |
| `data/cache/LruCacheManager.kt` | ❌ Omis | Cache mémoire LRU côté Kotlin |
| `data/model/` (`AppSettings.kt`, `ColorTransferMethod.kt`, `LutSize.kt`, `ProcessResult.kt`, `SavedGradedImage.kt`) | ❌ Omis | Modèles de données de configuration, résultat et bibliothèque |
| `data/repository/` (`SavedImageRepository.kt`, `SettingsRepository.kt`) | ❌ Omis | Persistance Room/SharedPreferences des images sauvées et réglages |
| `ui/components/` (9 composants : `DoubleViewComparison`, `ExportActionBar`, `ImageDetailDialog`, `ImagePickerCard`, `IntensitySlider`, `LutSizeSelector`, `MethodSelector`, `PaletteSwatchesView`, `SavePresetDialog`) | ❌ Omis | Bibliothèque entière de composants UI réutilisables |
| `ui/screens/` (`MetricsInfoScreen.kt`, `PalettesScreen.kt`, `ProfileScreen.kt`, `SettingsScreen.kt`) | ❌ Omis | 4 écrans principaux sur 7 omis |
| `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`) | ❌ Omis | Tokens de design system actuels |
| `ui/viewmodel/` (`ColorGradingViewModel.kt`, `SettingsViewModel.kt`) | ❌ Omis | ViewModel legacy (`ColorGradingViewModel`) et ViewModel des réglages |
| `app/src/test/` et `app/src/androidTest/` | ❌ Omis | Tests unitaires Kotlin et tests instrumentés Compose/Chaquopy |

---

## 2. Vérification des Affirmations du README vs Code Réel

| Affirmation README | Réalité Code | Écart & Verdict |
| :--- | :--- | :--- |
| **Badge API 26+ (Oreo → 15)** | `app/build.gradle.kts` spécifie `minSdk = 24` | ⚠️ **Obsolète / Faux** : Le badge indique API 26+ alors que le support réel est dès Android 7.0 Nougat (`minSdk = 24`). |
| **Export Adobe 3D LUT (`.cube`) / HaldCLUT** | `core/lut_export.py` exporte uniquement `.cube` 1.0. Aucune génération HaldCLUT PNG n'existe dans le codebase. | ⚠️ **Inexact** : HaldCLUT n'est pas implémenté dans le code mobile. |
| **Double-tap Reset sur sliders** | `DoubleTapSlider` dans `StandaloneEditorScreen.kt` (l.638) utilise un simple `.clickable { onValueChange(defaultValue) }` | 🔴 **Bug B5 / Trompeur** : Un simple tap réinitialise la valeur au lieu d'un double-tap. |
| **120 FPS Real-time GPU Blending** | `DoubleViewComparison.kt` compose deux Bitmaps avec `graphicsLayer { alpha = intensity }` | 🟠 **Risque WYSIWYG B9** : Le mélange GPU en direct n'est pas synchronisé avec `previewExecutionTimeMs` (durée Python) et peut provoquer une double application d'intensité si non cadré. |
| **83 tests unitaires** | `python -m pytest tests/` exécuté avec Python 3.10 Chaquopy | ✅ **Vrai** : 83 tests Pytest passent avec succès en 2.03s. |
| **Licence MIT (`LICENSE`)** | Fichier `LICENSE` absent de la racine du dépôt | ⚠️ **Manquant** : Le badge et le README pointent vers `LICENSE`, mais le fichier n'existe pas. |
| **CI / Automatisation** | Dossier `.github/` absent | ⚠️ **Manquant** : Aucune pipeline CI/CD GitHub Actions n'est configurée. |
| **Versionning `.idea`** | Fichiers de configuration IDE partiellement versionnés dans git | ⚠️ **À nettoyer** : Présence de `.idea/compiler.xml`, `misc.xml`, `runConfigurations.xml` dans l'arborescence git. |

---

## 3. Inventaire des Violations de Charte

### 3.1 Couleurs brutes hors thème (`Color(0x...)`) — 33 occurrences dans `ui/screens`
- `ProfileScreen.kt` : **20 occurrences**
  - Fond de carte sombre : `Color(0xFF202228)`
  - Mire de calibrage & dégradé : `Color(0xFFF0F0F0)`, `Color(0xFF333333)`, `Color(0xFF666666)`, `Color(0xFF999999)`, `Color(0xFFCCCCCC)`, `Color(0xFF767676)`, `Color(0xFF808080)`
  - Patchs ColorChecker : `Color(0xFFCD9B84)`, `Color(0xFF9B6E55)`, `Color(0xFF5F3E2D)`, `Color(0xFFE12D2D)`, `Color(0xFF2DB937)`, `Color(0xFF2D55E1)`, `Color(0xFF2DD7E1)`, `Color(0xFFD72DD7)`, `Color(0xFFEBD723)`, `Color(0xFF4B7DB9)`, `Color(0xFF559B55)`, `Color(0xFFD77D2D)`
- `PalettesScreen.kt` : **13 occurrences**
  - Mire & fond : `Color(0xFF202228)`, `Color(0xFFF0F0F0)`, `Color(0xFF333333)`, `Color(0xFF666666)`, `Color(0xFF999999)`, `Color(0xFF767676)`, `Color(0xFF808080)`
  - Patchs ColorChecker : `Color(0xFFCD9B84)`, `Color(0xFF9B6E55)`, `Color(0xFF5F3E2D)`, `Color(0xFFE12D2D)`, `Color(0xFF2DB937)`, `Color(0xFF2D55E1)`
- `ui/theme/Color.kt` & `Theme.kt` : Noms par teinte (`AccentAmber`, `AccentCyan`) au lieu de rôles sémantiques.

### 3.2 Dimensions littérales (`\d+.dp`) — 62 occurrences dans l'UI
- `ProfileScreen.kt` : **24 occurrences** (`3.dp`, `2.dp`, `4.dp`, etc.)
- `StandaloneEditorScreen.kt` : **19 occurrences** (`260.dp` pour le viewport fixe, `4.dp`, `16.dp`, `2.dp`, etc.)
- `PalettesScreen.kt` : **16 occurrences** (`2.dp`, `4.dp`, etc.)
- `ImageDetailDialog.kt` : **3 occurrences**

### 3.3 Typographie littérale (`\d+.sp` / `fontSize`)
- `ProfileScreen.kt` (l.319) : `fontSize = 10.sp` en dur sur le libellé de miniature.
- `Type.kt` : 9 valeurs `.sp` concentrées dans un seul style `bodyLarge`.

### 3.4 Textes et chaînes en dur (Non-i18n)
- `StandaloneEditorScreen.kt` :
  - `"ORIGINAL (Avant)"` (l.537)
  - `"Table 3D LUT de base (.CUBE)"` (l.708)
  - `"Aucune LUT chargée (Mode étalonnage direct)"` (l.718)
  - `"Mes Presets d'Édition (.szlpreset)"` (l.751)
  - `"Appliquer"` (l.792)
  - `"Supprimer"` (l.806)
  - `"Retirer LUT"` (l.830)
  - `"Réinitialiser tout"` (l.837, l.1112)
- `StealZaLutViewModel.kt` : **17 occurrences** de `userMessage = "..."` en français codé en dur (ex : `"Preset '${preset.name}' sauvegardé !"`, `"Preset supprimé."`, etc.) au lieu d'identifiants de ressource ou `UiText`.
- `SettingsViewModel.kt` & `ColorGradingViewModel.kt` : 3 messages d'erreur/succès en dur.

### 3.5 Nombres magiques Python
- `utils/image_io.py` (`extract_dominant_palette`) : Redimensionnement fixe `(128, 128)`, quantification median-cut sans filtrage chrominance/luminance, duplication de la dernière couleur en cas de dégénérescence.
- `StealZaLutViewModel.kt` : Naming de preset par modulo temporel `"Preset ${System.currentTimeMillis() % 1000}"`.

---

## 4. Fichiers Monolithes et Plan de Découpage

| Fichier Monolithe | Lignes | Responsabilités Mélangées | Plan de Découpage Cible (Sans Changement de Comportement) |
| :--- | :---: | :--- | :--- |
| `StealZaLutViewModel.kt` | **1 864** | StateFlow global, flux Studio 5 étapes, flux Standalone Editor, gestion des presets CRUD, export .cube et HD, caches | Découper en délégués de domaine :<br>• `StudioDelegate` (flux 5 étapes, détection styles)<br>• `EditorDelegate` (standalone editor, retouche 2D/3D)<br>• `PresetDelegate` (persistance, import/export)<br>Tout en conservant `StealZaLutViewModel` comme façade UDF unique et `StealZaLutUiState` immuable. |
| `ApplyThemeScreen.kt` | **1 680** | Écran hôte, Stepper 5 étapes, Step 1, Step 2, Step 3, Step 4, Step 5, Panneau LightCraft, Dialogues | Isoler dans `ui/screens/studio/` :<br>• `StudioStep1Source.kt`<br>• `StudioStep2Palette.kt`<br>• `StudioStep3Method.kt`<br>• `StudioStep4Review.kt`<br>• `StudioStep5Result.kt`<br>• `StudioLightCraftPanel.kt` |
| `StandaloneEditorScreen.kt` | **1 100** | Viewport interactif, Tabs presets, Tab retouche, Sliders, Export bar | Isoler dans `ui/screens/editor/` :<br>• `EditorViewport.kt`<br>• `EditorPresetsTab.kt`<br>• `EditorCalibrationTab.kt`<br>• `EditorGradingTabs.kt`<br>• `EditorBottomBar.kt` |
| `PythonBridgeRepository.kt` | **1 009** | Encodage base64, conversion JSON, décodage Bitmap, gestion de cache disque, appels Chaquopy | Découper en passerelles spécialisées :<br>• `TransferGateway.kt` (transfert & LUT brute)<br>• `ExportGateway.kt` (.cube et images HD)<br>• `PresetGateway.kt` (sérialisation .szlpreset) |

---

## 5. Code Mort et Duplication : `ColorGradingViewModel`

- `ColorGradingViewModel.kt` fait 387 lignes et possède son propre test unitaire `ColorGradingViewModelTest.kt` (116 lignes).
- **Audit de dépendances :** Aucun composant de l'application, aucun écran, et `MainActivity.kt` n'injectent ni n'utilisent `ColorGradingViewModel`. L'ensemble de la navigation et des écrans utilise exclusivement `StealZaLutViewModel`.
- **Conclusion :** `ColorGradingViewModel.kt` est un doublon legacy obsolète créé au tout début du projet avant la fusion dans le flux UDF centralisé.
- **Action recommandée :** Suppression sécurisée de `ColorGradingViewModel.kt` et mise à jour / archivage de son test unitaire.

---

## 6. Audit du Pipeline Colorimétrique & WYSIWYG

1. **Parité Aperçu / Export `.cube` / Export HD :**
   - Les tests Pytest actuels (`test_lut_export.py`) valident que `run_transfer_pipeline` et `apply_lut(exported_cube)` présentent une divergence maximale $\le 1$ niveau 8 bits.
   - **Faille critique constatée (Bug B9) :** Dans l'étape 5 de l'UI (`ApplyThemeScreen.kt`), le curseur d'opacité applique un mélange alpha GPU sur `previewBitmap` (`alpha = intensity`), alors que `previewBitmap` a déjà été calculé avec `global_intensity` dans Python. Cela entraîne un double affaiblissement du rendu à l'écran qui diverge de l'export HD et du fichier `.cube`.
2. **Gestion de l'orientation EXIF :**
   - À l'importation : `image_io.py::load_image` applique correctement `ImageOps.exif_transpose(img)`.
   - À l'exportation : `save_image` crée une nouvelle image Pillow via `Image.fromarray(graded)` sans réinjecter les tags EXIF d'origine. Les métadonnées d'appareil, date, et géolocalisation sont donc perdues.
3. **Espace Colorimétrique / Profil ICC :**
   - Le moteur opère sous l'hypothèse implicite d'un espace colorimétrique standard sRGB (D65).
   - Les fichiers exportés ne balisent pas de profil ICC sRGB explicite, ce qui peut entraîner des dérives sur des visualiseurs externes Wide Color Gamut (Display P3).
4. **Gestion Mémoire sur Export HD (> 12 MP) :**
   - `apply_lut_trilinear` traite l'image entière de façon vectorisée en float32. Pour une photo de 12 à 24 MP, les tableaux intermédiaires (`dx, dy, dz`, 4 lignes d'interpolation `c00..c11`) génèrent une empreinte crête de **1.2 à 1.5 Go de RAM**, risquant un crash OOM sur mobile.
   - **Solution :** Implémenter un traitement par tuiles/tranches dans `export_worker.py` (ex. tranches de 512 lignes).
5. **Résolution d'Export par Défaut :**
   - `AppSettings.maxExportDim` est initialisé à `2048`, tandis que `config.py::MAX_EXPORT_DIMENSION` vaut `3840`. Le bouton mentionne "HD". Il est impératif d'harmoniser ces valeurs et d'offrir l'option "Résolution Native / Originale".

---

## 7. Métriques Initiales Mesurées

- **Tests Python (Pytest) :** 83 tests exécutés avec succès en **2.03 s**.
- **Tests Kotlin JVM :** Réussite complète en **25 s**.
- **Compilation Kotlin Debug :** Réussite complète en **20 s**.
- **Taille APK Debug (Universel arm64-v8a + x86_64) :** **115.6 Mo** (121 294 688 octets).
- **Temps de pipeline aperçu (Proxy 512px) :** ~15 à 35 ms pour le calcul de base, < 5 ms pour les ajustements d'étapes 2 et 3 sur le lattice.

---

## 8. Synthèse Exécutive de l'Audit (15 Lignes)

1. L'application dispose d'une base mathématique saine (83 tests Pytest verts) et compile sans erreur sous Kotlin 2.2 / Gradle 9.6 / Chaquopy 3.10.
2. L'arborescence réelle est nettement plus riche que documentée dans `ARCHITECTURE.md §7` (9 composants UI, 4 écrans, `utils/` et 4 suites de tests omis).
3. Les affirmations du README sur HaldCLUT sont inexistantes dans le code, le badge API 26+ contredit `minSdk = 24`, et `LICENSE` est absent.
4. L'UI souffre de 33 couleurs brutes hors-thème, 62 dimensions littérales en dp, 17 messages ViewModel codés en dur en français et un thème sombre unique non interchangeable.
5. `ColorGradingViewModel.kt` est un fichier mort de 387 lignes sans aucun appel dans l'application.
6. 4 monolithes totalisant 5 653 lignes nécessitent un découpage modulaire en délégués spécialisés.
7. Le bug B9 brise la règle d'or WYSIWYG : le slider d'opacité GPU en étape 5 double l'intensité appliquée par rapport à l'export `.cube`/HD.
8. Les sliders bipolaires se remplissent anormalement depuis la gauche au lieu du centre 0, et un tap réinitialise la valeur (fausse implémentation double-tap B5).
9. La barre inférieure de l'éditeur écrase 4 boutons sur 360 dp avec textes coupés (B1), et le bandeau Auto Edit déborde (B2).
10. La hauteur fixe de 260 dp du viewport éditeur étouffe les onglets sur petits écrans (B3), et tout toucher déclenche le mode comparaison (B4).
11. L'extraction de palette utilise une quantification median-cut RGB 128px produisant des pastilles quasi noires sur les photos de nuit (B11).
12. Les listes et grilles tronquent leurs derniers éléments par manque de padding bas (B14).
13. L'export HD réinitialise les EXIFs et consomme > 1 Go de RAM sur images 12 MP+, risquant l'OOM sans tuilage.
14. Les barres système utilisent des APIs dépréciées et `themes.xml` sans `windowBackground` risque un flash blanc au démarrage.
15. Plan d'action validé : Déploiement immédiat de la Phase 1 (Tokens, Thèmes, Test de garde), puis résolution ordonnée des bugs B1 à B17.
