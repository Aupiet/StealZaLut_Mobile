# Architecture Système de StealZaLUT Mobile

Ce document présente l'architecture technique, mathématique et logicielle de **StealZaLUT Mobile**, solution professionnelle d'étalonnage colorimétrique cinématographique et d'édition photographique sur Android.

---

## 1. Alignement Architectural Desktop (`stealzalut`) vs Mobile (`StealZaLut_Mobile`)

StealZaLUT repose sur un principe d'alignement strict entre la version Desktop de référence (Python / Qt / NumPy / SciPy) et la version Mobile (Jetpack Compose / Chaquopy Python 3.10) :

| Composant | Référence Desktop (`src/stealzalut/`) | Implémentation Mobile (`StealZaLut_Mobile`) | Rôle & Invariants |
| :--- | :--- | :--- | :--- |
| **Color Transfer** | `core/color_transfer.py` | `app/src/main/python/stealzalut/core/color_transfer.py` | Algorithmes de transfert colorimétrique (Reinhard, MKL, HM-MKL-HM, Wasserstein). Découplage CIE LAB, soft-clipping tanh. |
| **LUT Engine** | `core/lut_engine.py` | `app/src/main/python/stealzalut/core/lut_engine.py` | Génération du treillis d'identité $N \times N \times N$, interpolation trilinéaire vectorisée sans boucle Python, lissage spatial 3D séparable. |
| **LUT Export** | `core/lut_export.py` | `app/src/main/python/stealzalut/core/lut_export.py` | Sérialisation conforme Adobe 3D LUT (`.cube` 1.0) ordonnée $B \to G \to R$. Clamping strict $[0.0, 1.0]$, validation NaN/Inf. |
| **Pipeline 3 Étapes** | `core/pipeline.py` | `app/src/main/python/stealzalut/core/pipeline.py` | Moteur unifié OKLab non-destructif : Étape 1 (Filtre Pur), Étape 2 (Retravail & Masques), Étape 3 (Post-Grading & Baking). |
| **Preset Manager** | `core/preset_manager.py` | `app/src/main/python/stealzalut/core/preset_manager.py` | Gestionnaire de presets d'édition `.szlpreset` / JSON. Sérialisation complète des paramètres Étapes 2 & 3 et référence LUT. |
| **Auto Edit** | `core/auto_edit.py` | `app/src/main/python/stealzalut/core/auto_edit.py` | Rétro-ingénierie LightCraft (Rec. 2020 log2 deviation from 18% middle gray card). |
| **Workers** | `workers/transfer_worker.py`<br>`workers/export_worker.py` | `workers/transfer_worker.py`<br>`workers/export_worker.py` | Caches LRU double niveau (`_RAW_LUT_CACHE`, `_SRC_PROXY_CACHE`), réactivité slider $< 5\text{ ms}$, baking WYSIWYG complet. |
| **Bridge / JNI** | N/A (Appels Qt directs) | `stealzalut/bridge.py`<br>`PythonBridgeRepository.kt` | Façade Chaquopy JNI thread-safe sous `pythonDispatcher` (`limitedParallelism(1)`) pour protéger le GIL Python. |
| **UI Layer** | PySide6 / QML | Jetpack Compose (Material 3) | 120 FPS UI réactive, Flow MVI, DoubleView comparateur split-wipe, pinch-to-zoom tactile, Mode Éditeur Pur. |

---

## 2. Pipeline de Traitement d'Image en 3 Étapes Non Destructives

Le cœur du traitement est unifié entre l'image affichée à l'écran et la 3D LUT exportée (parité WYSIWYG absolue). Aucun réglage en aval ne relance les étapes coûteuses en amont.

```
       [Source Image I_orig]             [Reference Image I_ref]
                 │                                   │
                 └─────────────────┬─────────────────┘
                                   ▼
        ┌────────────────────────────────────────────────────────┐
        │        ÉTAPE 1 : GÉNÉRATION DU "FILTRE PUR"           │
        │  - Transfert colorimétrique à 100% (Reinhard/MKL/HM)   │
        │  - Calcul unique exécuté sur Lattice Identité (33³)    │
        │  - Mise en cache mémoire vive LRU                      │
        └────────────────────────────────────────────────────────┘
                                   │
             ┌─────────────────────┴─────────────────────┐
             ▼                                           ▼
      [ Raw 3D LUT_pure ]                         [ Image I_pure ]
             │                                           │
             └─────────────────────┬─────────────────────┘
                                   ▼
        ┌────────────────────────────────────────────────────────┐
        │       ÉTAPE 2 : MODULE DE RETRAVAIL DU FILTRE          │
        │           (Espace Perceptuel OKLab L, a, b)            │
        │  1. Spatial 3D : Lissage Gaussien 3D sur LUT_pure      │
        │  2. Luma Lock : Interpole L_out vers L_orig            │
        │  3. Neutral Axis Lock : Masque C_orig <= 0.08          │
        │  4. Skin Tone Lock : Masque elliptique gaussien peau   │
        │  5. Extremes Roll-off : Protection ombres/highlights   │
        │  6. Global Intensity : Blend final vers identité/orig   │
        └────────────────────────────────────────────────────────┘
                                   │
                                   ▼
                            [ Résultat Étape 2 ]
                                   │
                                   ▼
        ┌────────────────────────────────────────────────────────┐
        │         ÉTAPE 3 : MODULE DE POST-GRADING               │
        │  A. Effets Colorimétriques Globaux (Baking dans LUT) : │
        │     - Exposition (Linéaire EV), Contraste (Sigmoïde)   │
        │     - Hautes Lumières / Ombres (Épaules/Talons)        │
        │     - Température / Teinte (Balance chromatique)       │
        │     - Vibrance (Smart skin-safe) / Saturation          │
        │  B. Effets Spatiaux 2D (Appliqués sur image seule) :   │
        │     - Clarity (Contraste local passe-haut)             │
        │     - Sharpen (Masque flou / Unsharp mask)             │
        │     - Grain (Grain argentique modulé par luminance)    │
        └────────────────────────────────────────────────────────┘
                                   │
             ┌─────────────────────┴─────────────────────┐
             ▼                                           ▼
┌───────────────────────────────┐       ┌───────────────────────────────┐
│     Aperçu Écran / Export HD  │       │       Adobe 3D LUT (.cube)    │
│  - Interpolation trilinéaire  │       │  - Baking Étapes 1, 2, 3 (A)  │
│  - Effets spatiaux 2D (B)     │       │  - Ordre B -> G -> R          │
│  - Sauvegarde Galerie / Media │       │  - Clamping strict [0.0, 1.0] │
└───────────────────────────────┘       └───────────────────────────────┘
```

### 2.1 Caches Mémoire et Réactivité Mobile
- `_RAW_LUT_CACHE` : Associe la clé `(src_path, ref_path, method, lut_size, preserve_luma, soft_clip)` au tableau NumPy $33 \times 33 \times 33 \times 3$ ($35\,937$ triplets float32, soit environ $420\text{ Ko}$).
- `_SRC_PROXY_CACHE` : Conserve le proxy de prévisualisation (bord long $\le 1024\text{ px}$) pour éviter tout décodage JPEG lors du déplacement des curseurs.
- Grâce à cette architecture, toute modification des 6 curseurs de l'Étape 2 ou des 11 curseurs de l'Étape 3 s'exécute en **moins de 5 ms** sur le lattice ou le proxy, garantissant une fluidité de 60 à 120 FPS sur smartphone.

---

## 3. Spécifications Mathématiques des Étapes 2 et 3

### 3.1 Espace Perceptuel Uniforme OKLab
La conversion $sRGB \leftrightarrow OKLab$ est utilisée pour tous les masques colorimétriques :
$$\begin{pmatrix} l \\ m \\ s \end{pmatrix} = \mathbf{M}_1 \begin{pmatrix} R_{lin} \\ G_{lin} \\ B_{lin} \end{pmatrix}, \quad \begin{pmatrix} L \\ a \\ b \end{pmatrix} = \mathbf{M}_2 \begin{pmatrix} l^{1/3} \\ m^{1/3} \\ s^{1/3} \end{pmatrix}$$
où $L \in [0.0, 1.0]$ est la clarté perçue, et $(a, b)$ sont les coordonnées chromatiques orthogonales.

### 3.2 Formules du Module de Retravail (Étape 2)

> [!IMPORTANT]
> **Valeurs par défaut des protections (Parité Desktop à 100%) :**
> Toutes les protections de l'Étape 2 (`luma_lock`, `neutral_axis_lock`, `skin_tone_lock`, `extremes_rolloff`, `spatial_3d`) sont fixées à **`0.0` par défaut** (avec `global_intensity = 1.0`). Cela garantit que le transfert initial lors de l'application d'un thème transmet immédiatement l'intégralité du contraste et de l'ambiance chromatique de la référence (ex: ciel de nuit cyan/émeraude, néons vifs) sans être étouffé par des masques préventifs. L'utilisateur peut ensuite activer et doser ces protections à sa convenance ou via l'option Auto du menu d'édition.

1. **Spatial 3D (`spatial_3d` $\in [0.0, 1.0]$)** :
   $$LUT_{smooth} = (1 - \alpha_{spatial}) \cdot LUT_{pure} + \alpha_{spatial} \cdot \text{GaussianBlur3D}(LUT_{pure}, \sigma)$$
   Appliqué directement sur le volume tridimensionnel pour éliminer toute cassure de dégradé.
2. **Préservation de la Lumière (`luma_lock` $\in [0.0, 1.0]$)** :
   $$L_{out} = (1 - \text{luma\_lock}) \cdot L_{pure} + \text{luma\_lock} \cdot L_{orig}$$
3. **Protection de l'Axe Neutre (`neutral_axis_lock` $\in [0.0, 1.0]$)** :
   $$C_{orig} = \sqrt{a_{orig}^2 + b_{orig}^2}, \quad M_{neutral} = 1.0 - \text{smoothstep}(0.0, 0.08, C_{orig})$$
   $$W_{neutral} = \text{neutral\_axis\_lock} \cdot M_{neutral}$$
4. **Protection des Tons Chair (`skin_tone_lock` $\in [0.0, 1.0]$)** :
   $$H_{orig} = \text{atan2}(b_{orig}, a_{orig})$$
   $$M_{skin} = \exp\left(-\frac{(H_{orig} - H_{skin})^2}{2\sigma_H^2} - \frac{(C_{orig} - C_{skin})^2}{2\sigma_C^2}\right)$$
   avec $H_{skin} \approx 0.86\text{ rad}$, $C_{skin} \approx 0.092$, $\sigma_H = 0.35$, $\sigma_C = 0.045$.
   $$W_{skin} = \text{skin\_tone\_lock} \cdot M_{skin}$$
5. **Protection des Extrêmes (`extremes_rolloff` $\in [0.0, 1.0]$)** :
   $$M_{extremes} = (1.0 - \text{smoothstep}(0.0, 0.15, L_{orig})) + \text{smoothstep}(0.85, 1.0, L_{orig})$$
   $$W_{extremes} = \text{extremes\_rolloff} \cdot \text{clamp}(M_{extremes}, 0.0, 1.0)$$
6. **Combinaison des Masques et Intensité Globale (`global_intensity` $\in [0.0, 1.0]$)** :
   $$W_{protect} = 1.0 - (1.0 - W_{neutral}) \cdot (1.0 - W_{skin}) \cdot (1.0 - W_{extremes})$$
   $$(a_{blended}, b_{blended}) = (1.0 - W_{protect}) \cdot (a_{pure}, b_{pure}) + W_{protect} \cdot (a_{orig}, b_{orig})$$
   Conversion OKLab $\to$ RGB de $(L_{out}, a_{blended}, b_{blended}) \implies I_{masked}$, puis :
   $$I_{step2} = (1.0 - \text{global\_intensity}) \cdot I_{orig} + \text{global\_intensity} \cdot I_{masked}$$

---

## 4. Spécification d'Export Adobe 3D LUT (`.cube`) et Baking

Le format Adobe Cube LUT 1.0 est le standard de l'industrie cinématographique.

### 4.1 Ordre des Données
L'ordre canonique impose que l'axe **Rouge (R)** varie le plus vite (boucle interne), suivi de l'axe **Vert (G)**, puis de l'axe **Bleu (B)** le plus lent (boucle externe) :
```
for b_idx in range(size):
    for g_idx in range(size):
        for r_idx in range(size):
            write(r, g, b)
```

### 4.2 En-tête Obligatoire
```text
TITLE "StealZaLut_Export"
LUT_3D_SIZE 33
DOMAIN_MIN 0.0 0.0 0.0
DOMAIN_MAX 1.0 1.0 1.0
```

### 4.3 Baking Intégral
Le fichier `.cube` n'est pas une simple table brute : il encapsule le treillis d'identité $33^3$ ayant traversé :
1. Le transfert de couleurs de base (Étape 1).
2. Le lissage spatial 3D et les masques colorimétriques de l'Étape 2.
3. Les réglages colorimétriques globaux de l'Étape 3 (Exposition, Contraste, Hautes Lumières, Ombres, Température, Teinte, Vibrance, Saturation).
4. Validation : `np.nan_to_num(lut_data, nan=0.0, posinf=1.0, neginf=0.0)` et `np.clip(lut_data, 0.0, 1.0)`.

---

## 5. Schéma JSON des Presets d'Édition (`.szlpreset` / `.json`)

Les réglages d'édition sont sérialisables sous forme de preset réutilisable :

```json
{
  "id": "e4b3c8a1-7d2e-4f10-9b3a-8c5d2e1a9f04",
  "name": "Cinematic Teal Warm Skin",
  "created_at": 1730745600000,
  "thumbnail_base64": "data:image/webp;base64,...",
  "lut_reference": null,
  "step2_params": {
    "global_intensity": 0.85,
    "luma_lock": 0.30,
    "extremes_rolloff": 0.20,
    "neutral_axis_lock": 0.50,
    "skin_tone_lock": 0.65,
    "spatial_3d": 0.15
  },
  "step3_params": {
    "exposure": 0.15,
    "contrast": 0.20,
    "highlights": -0.25,
    "shadows": 0.18,
    "temperature": 0.08,
    "tint": -0.04,
    "vibrance": 0.30,
    "saturation": 0.05,
    "clarity": 0.12,
    "sharpen": 0.08,
    "grain": 0.05
  }
}
```

---

## 6. Mode Éditeur Pur / Photo Studio (Standalone Photo Editor)

Le Mode Éditeur Pur permet d'éditer une photo source **sans sélectionner d'image de référence B** :
- **Entrée directe :** Sélection unique d'une photo depuis la galerie ou prise de vue.
- **Onglet Presets & LUTs :** Permet d'importer une table `.cube` externe ou d'appliquer un `.szlpreset` existant.
- **Onglet Calibrage LUT (Étape 2) :** Actif dès qu'une LUT est appliquée.
- **Onglets Lumière, Couleur, Effets (Étape 3) :** Retouches en temps réel en direct sur la photo.
- **Exportations disponibles :**
  - Image haute résolution dans la galerie MediaStore (JPEG/PNG).
  - 3D LUT cuite (`.cube`) exportable vers DaVinci Resolve / Premiere Pro.
  - Sauvegarde en preset d'édition `.szlpreset`.

---

## 7. Arborescence Complète et Découpée des Fichiers

L'application respecte une modularité stricte (< 400 lignes par fichier, zéro monolithe) :

```
StealZaLut_Mobile/
├── ARCHITECTURE.md                          Documentation architecturale globale
├── CODING_STANDARDS.md                      Charte technique et invariants WYSIWYG
├── LICENSE                                  Licence open-source MIT
├── app/
│   └── src/
│       ├── main/
│       │   ├── python/stealzalut/
│       │   │   ├── config.py                Constantes système, algorithmes et seuils
│       │   │   ├── bridge.py                Façade unique JNI/Chaquopy (status: success/error)
│       │   │   ├── core/
│       │   │   │   ├── color_space.py       Espaces perceptuels OKLab, CIE LAB, smoothstep
│       │   │   │   ├── pipeline.py          Moteur Étapes 2 & 3, Baking LUT et masques
│       │   │   │   ├── preset_manager.py    Gestionnaire de presets v2, migration, duplication, export
│       │   │   │   ├── lut_engine.py        Lattice 3D, interpolation trilinéaire & chunking anti-OOM
│       │   │   │   ├── lut_export.py        Exportation Adobe .cube 1.0 conforme
│       │   │   │   ├── color_transfer.py    Transferts optimaux (MKL, MVGD, Reinhard, HM)
│       │   │   │   └── auto_edit.py         Analyse optique Rec. 2020 LightCraft
│       │   │   ├── utils/
│       │   │   │   └── image_io.py          I/O Pillow, préservation EXIF et extraction de palettes
│       │   │   └── workers/
│       │   │       ├── transfer_worker.py   Caches LRU double niveau (_RAW_LUT_CACHE, proxy)
│       │   │       └── export_worker.py     Orchestration baking .cube et export photos HD
│       │   └── java/com/stealzalut/mobile/
│       │       ├── core/
│       │       │   ├── AppConstants.kt      Constantes logiques, seuils d'opacité, durées
│       │       │   ├── AppDimens.kt         Jetons de dimensions, paddings et rayons
│       │       │   └── UiText.kt            Encapsulation i18n des chaînes pour ViewModel
│       │       ├── data/
│       │       │   ├── bridge/
│       │       │   │   └── PythonBridgeRepository.kt Façade thread-safe sur pythonDispatcher
│       │       │   ├── model/
│       │       │   │   ├── ColorimetryPreset.kt Modèle preset Studio / Palettes
│       │       │   │   ├── EditPreset.kt        Modèle preset Éditeur (.szlpreset)
│       │       │   │   └── AppSettings.kt       Persistance des préférences et thèmes
│       │       │   └── repository/
│       │       │       ├── PresetRepository.kt  Persistance JSON des presets Studio
│       │       │       └── SettingsRepository.kt Gestion SharedPreferences
│       │       └── ui/
│       │           ├── components/
│       │           │   ├── DoubleViewComparison.kt Comparateur 3 modes (Split, Side-by-side, Hold)
│       │           │   ├── SzlEmptyState.kt     Composant standard d'état vide
│       │           │   ├── SzlRgbHistogram.kt   Histogramme RVB temps réel superposable
│       │           │   ├── SzlImageBadge.kt     Badge sémantique sur image (compareBadge)
│       │           │   └── ...
│       │           ├── theme/
│       │           │   ├── SzlColors.kt         Rôles sémantiques étendus (CompositionLocal)
│       │           │   ├── ThemeSpec.kt         Contrat de thème (colorScheme, SzlColors)
│       │           │   ├── Themes.kt            Registre des 4 thèmes (Ambre, Cyan, OLED, Daylight)
│       │           │   ├── Theme.kt             Provider StealZaLut_MobileTheme
│       │           │   └── ColorCardReference.kt Constantes de mire ColorChecker physiques
│       │           ├── screens/
│       │           │   ├── studio/              Studio de gradation découpé en 7 étapes/vues
│       │           │   │   ├── StudioStepper.kt
│       │           │   │   ├── StudioStep1Source.kt
│       │           │   │   ├── StudioStep2Style.kt
│       │           │   │   ├── StudioStep3Parameters.kt
│       │           │   │   ├── StudioStep4Computing.kt
│       │           │   │   ├── StudioStep5Result.kt
│       │           │   │   └── StudioComponents.kt
│       │           │   ├── editor/              Éditeur photo seul découpé en 6 vues
│       │           │   │   ├── EditorViewport.kt
│       │           │   │   ├── EditorPresetsTab.kt
│       │           │   │   ├── EditorCalibrationTab.kt
│       │           │   │   ├── EditorLightTab.kt
│       │           │   │   ├── EditorColorTab.kt
│       │           │   │   └── EditorEffectsTab.kt
│       │           │   ├── ApplyThemeScreen.kt  Orchestrateur Studio (< 200 lignes)
│       │           │   ├── StandaloneEditorScreen.kt Orchestrateur Éditeur (< 320 lignes)
│       │           │   ├── ProfileScreen.kt     Galerie et presets enregistrés
│       │           │   └── SettingsScreen.kt    Sélecteur de thèmes et paramètres
│       │           └── viewmodel/
│       │               ├── StealZaLutViewModel.kt Façade UDF (< 350 lignes)
│       │               ├── StealZaLutUiState.kt État immuable (@Immutable)
│       │               └── delegate/            Délégués spécialisés par domaine
│       │                   ├── StudioFlowDelegate.kt
│       │                   ├── GradingPipelineDelegate.kt
│       │                   ├── EditorSessionDelegate.kt
│       │                   └── PresetLibraryDelegate.kt
│       └── test/java/com/stealzalut/mobile/
│           ├── DesignSystemGuardTest.kt     Test JVM de garde (zéro Color/dp/sp/text en dur)
│           ├── ThemeContrastTest.kt         Test de conformité contraste WCAG AA sur les 4 thèmes
│           └── StealZaLutViewModelTest.kt   Tests unitaires UDF du ViewModel
└── tests/                                   91 tests Pytest (100% verts)
    ├── test_auto_edit.py
    ├── test_bridge.py
    ├── test_color_transfer.py
    ├── test_colorset_worker.py
    ├── test_lut_engine.py
    ├── test_lut_export.py
    ├── test_pipeline.py
    └── test_preset_manager.py
```
