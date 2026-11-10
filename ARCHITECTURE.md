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
| **GPU Preview** | N/A (OpenGL Qt) | `ui/preview/GpuPreviewEngine.kt` | Moteur AGSL RuntimeShader instantané (< 1 ms) avec repli ColorMatrix, deltas différentiels relatifs et parité $\Delta E \le 0.5$. |
| **Crop Physics** | N/A (QGraphicsItem) | `ui/screens/editor/EditorCropOverlay.kt` | Physique mathématique pure (`CropPhysics`), verrouillage de ratio d'aspect strict, poignées L-brackets et redressement 120 FPS. |
| **Project Storage** | N/A (Fichiers locaux) | `data/repository/ProjectRepository.kt` | Persistance sidecar atomique non-destructive des sessions d'édition (`project.json`, photo locale et vignette). |
| **Feature Guide** | N/A | `ui/screens/guide/` | Centre d'aide interactif des 12 outils avec comparateur unifié `DoubleViewComparison` et cache d'effets pré-rendus. |

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

### 2.2 Architecture de Prévisualisation GPU Hybride à 2 Étages (AGSL / ColorMatrix & Chaquopy)
Pour concilier l'exigence de réactivité tactile absolue (60–120 FPS sans saccade) et la règle d'or WYSIWYG (ADR-08), l'application implémente un pipeline hybride différentiel à deux étages (`GpuPreviewEngine.kt`) :
1. **Étage 1 (GPU / < 1 ms par trame) :**
   - Évaluation instantanée des ajustements sur GPU via AGSL `RuntimeShader` (`android.graphics.RuntimeShader`) sur Android 13+ (API 33+), avec repli matériel transparent sur `ColorFilter.colorMatrix` pour Android 7.0–12 (API 24–32).
   - Couverture intégrale des 10 modules d'édition : Exposition (espace linéaire $2^{\Delta EV}$), balance des blancs vectorielle, contraste sigmoïdal $\tanh$, ombres/hautes lumières smoothstep, courbes de tonalité paramétriques, mixeur HSL 8 bandes (fenêtres gaussiennes $\sigma = 22^\circ$), roues 3-way color grading, étalonnage de profil capteur, vignettage radial, netteté et grain.
   - Les shaders opèrent sur des **deltas relatifs** ($\Delta = \text{valeur actuelle} - \text{valeur Python}$).
2. **Étage 2 (Fidélité Maximale en Tâche de Fond) :**
   - Déclenchement automatique de la passe de référence Python Chaquopy dès le relâchement du geste tactile ou après 200 ms d'inactivité.
   - À réception du nouveau bitmap développé, le delta s'annule ($\Delta = 0$), le shader redevient une identité mathématique neutre : transition sans clignotement et parité $\Delta E \le 0.5$ niveau 8 bits.

### 2.3 Moteur Géométrique de Recadrage & Redressement (`CropPhysics`)
Le recadrage et le redressement d'horizon (`EditorCropOverlay.kt`) reposent sur un modèle géométrique pur conforme à la spécification Adobe Camera Raw (`crs:Crop*`) :
- **Verrouillage strict des ratios :**
  $$k = \frac{W_{\text{image}}}{H_{\text{image}} \cdot R_{\text{target}}}$$
  Permet de contraindre rigoureusement les proportions physiques réelles (Libre, Original, 1:1, 4:5, 3:2, 16:9).
- **8 Poignées & Panoramique Biaisé :** 4 coins en équerre L-brackets haute visibilité (3 dp blancs avec ombre portée) et 4 encoches médianes, avec détection de contact généreuse (44 dp) et garde-fou d'inversion (`minDim = 0.05f`).
- **Alignement Sub-pixel au Viewport :** Projection exacte sur le rectangle de l'image letterboxée (`ContentScale.Fit`) avec gestion du Darkroom.
- **Découplage de l'Aperçu & Redressement 120 FPS :** Dans l'outil Recadrer (`editorActiveTool == 0`), le buffer d'aperçu Python conserve l'image complète non tronquée, évitant tout effet de rétrécissement récursif. La rotation de redressement est appliquée en temps réel sur GPU (`graphicsLayer { rotationZ = -cropAngle }`) sous la grille de tiers fixe. Lors de la validation ou de l'export, Python exécute la découpe et la rotation finales.

### 2.4 Persistance Sidecar et Modèle de Projets d'Édition (`EditorProject`)
L'éditeur intègre un système complet de gestion de projets d'édition non-destructifs (`ProjectRepository.kt`) :
- Chaque projet est isolé dans son répertoire `filesDir/projects/<id>/` contenant le manifeste `project.json`, une copie permanente de la photo source et la vignette d'aperçu.
- Écriture atomique avec fichier temporaire et remplacement sécurisé (`Files.move` avec `REPLACE_EXISTING`), immunisé contre les extinctions inopinées.
- Sauvegarde automatique en arrière-plan débouncée à 2 secondes lors de toute modification.
- Écran d'accueil de l'éditeur affichant l'historique chronologique des projets récents avec métadonnées relatives, duplication, renommage et suppression.

### 2.5 Centre d'Aide & Découverte des 15 Outils Photographiques et 3D LUT
L'application propose un espace dédié à l'apprentissage et à la découverte interactive des outils majeurs (`ui/screens/guide/`) :
- Modélisation typée `FeatureGuideModel` (catégories, paramètres, échelles de valeurs, conseils pratiques).
- Couverture intégrale des **outils de retouche Lightroom** (Recadrage, Courbes, Mixeur HSL, Étalonnage 3 Voies, Étalonnage Capteur, Netteté, Optique, Géométrie, Flou d'objectif, Effets & Grain) ET de la **création de 3D LUT** (Algorithmes de transfert MKL/HM/Reinhard/Cinématique, Masques & Protections OKLab, Résolution de grille 3D LUT 17³/33³/64³, Exportation `.cube`).
- Réutilisation directe du comparateur tactile haute performance du Studio `DoubleViewComparison` (Split-Wipe, Side-by-Side, Hold-to-Compare).
- Photographies réelles d'étalonnage embarquées (`guide_sample_depart.jpg`, `guide_sample_graded.jpg`, `guide_sample_reference.jpg`) avec cache mémoire d'effets pré-générés pour une démonstration instantanée sans latence.
- Navigation hiérarchique préservant le contexte du guide lors des retours arrière.

### 2.6 Prise en Charge Universelle RAW & DNG
Pour pallier la limitation du sélecteur de photos standard d'Android (`PickVisualMedia`) qui filtre les fichiers bruts :
- Intégration d'un bouton dédié « Ouvrir RAW / DNG » s'appuyant sur le Storage Access Framework Android (`ActivityResultContracts.GetContent("*/*")`).
- Résolution dynamique de l'extension de fichier originale depuis le `ContentResolver` (préservation des `.dng`, `.raw`, `.cr2`, `.nef`, `.arw`).
- Décodage résilient des aperçus : contournement de la vérification bounds de `BitmapFactory` pour les formats RAW/DNG, extraction native des vignettes EXIF intégrées via `android.media.ExifInterface.thumbnailBitmap` / `thumbnailBytes`.
- Enregistrement des extensions `.dng` et `.raw` dans le plugin TIFF de Pillow (`PIL.TiffImagePlugin`) pour une ouverture directe côté Python.

### 2.7 Dialogue d'Exportation HD et Pilotage Multi-Résolution
Le dialogue d'exportation de l'éditeur autonome (`EditorExportDialog.kt`) offre un contrôle complet :
- Sélection de la résolution cible : 1080p FHD (1920), 1440p QHD (2560), 4K UHD (3840) et Résolution Native (0).
- Sélection du format : JPEG et PNG non-destructif.
- Curseur de qualité de compression JPEG (50% à 100%) avec calcul en temps réel du poids estimé en Mo et des dimensions en pixels.
- Transmission intégrale des paramètres (`format`, `quality`, `maxDim`) vers `PythonBridgeRepository.exportEditorImage` et `export_graded_image` pour un rendu haute précision.

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

L'application respecte une séparation stricte des responsabilités par couches, des délégués de domaine spécialisés pour le ViewModel, et des composants/panneaux UI hautement modulaires :

```
StealZaLut_Mobile/
├── ARCHITECTURE.md                          Documentation architecturale globale
├── CODING_STANDARDS.md                      Charte technique et invariants WYSIWYG
├── LICENSE                                  Licence open-source MIT
├── README.md                                Présentation, guides et instructions de build
├── app/
│   └── src/
│       ├── main/
│       │   ├── python/stealzalut/
│       │   │   ├── config.py                Constantes système, algorithmes et seuils
│       │   │   ├── bridge.py                Façade unique JNI/Chaquopy (status: success/error)
│       │   │   ├── core/
│       │   │   │   ├── auto_edit.py         Analyse optique Rec. 2020 LightCraft
│       │   │   │   ├── color_card.py        Génération et détection mire ColorChecker
│       │   │   │   ├── color_space.py       Espaces perceptuels OKLab, CIE LAB, smoothstep
│       │   │   │   ├── color_transfer.py    Transferts optimaux (MKL, MVGD, Reinhard, HM)
│       │   │   │   ├── lightroom_color.py   Moteur de virage partiel et balance des blancs
│       │   │   │   ├── lightroom_models.py  Structures de données du moteur de développement
│       │   │   │   ├── lightroom_spatial.py Traitements spatiaux (clarté, netteté, grain)
│       │   │   │   ├── lut_engine.py        Lattice 3D, interpolation trilinéaire & chunking anti-OOM
│       │   │   │   ├── lut_export.py        Exportation Adobe .cube 1.0 conforme
│       │   │   │   ├── pipeline.py          Moteur Étapes 2 & 3, Baking LUT et masques
│       │   │   │   ├── preset_manager.py    Gestionnaire de presets v2, migration, duplication, export
│       │   │   │   ├── spatial_filters.py   Convolutions et filtres spatiaux optimisés
│       │   │   │   └── spline_interpolator.py Interpolation par splines cubiques pour courbes
│       │   │   ├── utils/
│       │   │   │   └── image_io.py          I/O Pillow, préservation EXIF et extraction de palettes
│       │   │   └── workers/
│       │   │       ├── export_worker.py     Orchestration baking .cube et export photos HD
│       │   │       └── transfer_worker.py   Caches LRU double niveau (_RAW_LUT_CACHE, proxy)
│       │   └── java/com/stealzalut/mobile/
│       │       ├── MainActivity.kt              Point d'entrée Android, gestion fenêtres et thèmes
│       │       ├── StealZaLutApplication.kt     Initialisation application et Chaquopy
│       │       ├── core/
│       │       │   ├── AppConstants.kt          Constantes logiques, seuils d'opacité, durées
│       │       │   ├── AppDimens.kt             Jetons de dimensions, paddings et rayons
│       │       │   └── UiText.kt                Encapsulation i18n des chaînes pour ViewModel
│       │       ├── data/
│       │       │   ├── bridge/
│       │       │   │   └── PythonBridgeRepository.kt Façade thread-safe sur pythonDispatcher
│       │       │   ├── cache/
│       │       │   │   └── LruCacheManager.kt   Cache mémoire LRU bitmaps et aperçus
│       │       │   ├── model/
│       │       │   │   ├── AppSettings.kt       Modèle des préférences utilisateur et thème
│       │       │   │   ├── ColorimetryPreset.kt Modèle preset Studio / Palettes
│       │       │   │   ├── ColorTransferMethod.kt Énumération des algorithmes de transfert
│       │       │   │   ├── EditPreset.kt        Modèle preset Éditeur (.szlpreset)
│       │       │   │   ├── LightroomDeserializer.kt Désérialisation des paramètres d'édition
│       │       │   │   ├── LightroomSerializer.kt   Sérialisation des réglages studio
│       │       │   │   ├── LutSize.kt           Dimensions de grille 3D LUT (17, 33, 65)
│       │       │   │   ├── ProcessResult.kt     Conteneurs de résultats de traitement JNI
│       │       │   │   └── SavedGradedImage.kt  Entité d'image sauvegardée en galerie
│       │       │   └── repository/
│       │       │       ├── PresetRepository.kt  Persistance JSON des presets Studio
│       │       │       ├── SavedImageRepository.kt Gestion de l'historique des exports
│       │       │       └── SettingsRepository.kt Gestion SharedPreferences
│       │       └── ui/
│       │           ├── components/
│       │           │   ├── DoubleViewComparison.kt Comparateur 3 modes (Split, Side-by-side, Hold)
│       │           │   ├── ExportActionBar.kt   Barre d'actions d'exportation
│       │           │   ├── ImageDetailDialog.kt Modal d'inspection détaillée d'une image
│       │           │   ├── ImagePickerCard.kt   Sélecteur d'images avec aperçu
│       │           │   ├── LutSizeSelector.kt   Sélecteur de précision de LUT
│       │           │   ├── MethodSelector.kt    Sélecteur d'algorithme mathématique
│       │           │   ├── PaletteSwatchesView.kt Nuancier de palettes extraites
│       │           │   ├── SavePresetDialog.kt  Dialogue d'enregistrement de style
│       │           │   ├── SzlAccordionSection.kt Section accordéon rétractable
│       │           │   ├── SzlBipolarSlider.kt  Curseur bipolaire (-100 à +100) avec point neutre
│       │           │   ├── SzlEmptyState.kt     Composant standard d'état vide
│       │           │   ├── SzlRgbHistogram.kt   Histogramme RVB temps réel superposable
│       │           │   ├── SzlScrim.kt          Voile semi-transparent de chargement
│       │           │   └── SzlSlider.kt         Curseur unilatéral fluide avec reset double-tap
│       │           ├── navigation/
│       │           │   └── AppDestination.kt    Destinations de navigation de l'application
│       │           ├── screens/
│       │           │   ├── ApplyThemeScreen.kt  Orchestrateur du Studio de gradation
│       │           │   ├── MainColorGradingScreen.kt Conteneur principal et navigation
│       │           │   ├── PalettesScreen.kt    Exploration et extraction de palettes cinéma
│       │           │   ├── ProfileScreen.kt     Galerie d'images et bibliothèque de presets
│       │           │   ├── SettingsScreen.kt    Paramètres globaux et sélecteur de thème
│       │           │   ├── StandaloneEditorScreen.kt Orchestrateur de l'Éditeur photo autonome
│       │           │   ├── editor/              Panneaux spécialisés de l'Éditeur autonome
│       │           │   │   ├── EditorActionsDropdownToolbar.kt Menu déroulant presets et Auto-Edit
│       │           │   │   ├── EditorBasicPanel.kt  Réglages de tonalité de base (EV, contraste)
│       │           │   │   ├── EditorCalibrationPanel.kt Étape 2 (Luma Lock, Skin Lock...)
│       │           │   │   ├── EditorColorGradingPanel.kt Color wheels 3 voies (ombres, tons moyens, HL)
│       │           │   │   ├── EditorColorMixerPanel.kt HSL 8 canaux sélectifs
│       │           │   │   ├── EditorCropOverlay.kt Grille de composition et recadrage
│       │           │   │   ├── EditorDetailPanel.kt Clarté, netteté et réduction de bruit
│       │           │   │   ├── EditorEffectsPanel.kt Grain argentique, vignetage
│       │           │   │   ├── EditorEmptyState.kt  Invite d'import de photo dans l'éditeur
│       │           │   │   ├── EditorExportDialog.kt Dialogue d'options d'export (.cube, JPEG)
│       │           │   │   ├── EditorLensBlurPanel.kt Flou optique d'objectif simulé
│       │           │   │   ├── EditorLensCorrectionsPanel.kt Correction distorsion et vignetage
│       │           │   │   ├── EditorPresetsTab.kt  Gestionnaire de presets et import .cube
│       │           │   │   ├── EditorSavePresetDialog.kt Dialogue d'enregistrement .szlpreset
│       │           │   │   ├── EditorToneCurvePanel.kt Courbe de tonalité paramétrique et point-curve
│       │           │   │   ├── EditorToolbar.kt     Barre d'outils tactile inférieure
│       │           │   │   ├── EditorTransformPanel.kt Redressement géométrique et perspective
│       │           │   │   └── EditorViewport.kt    Zone d'aperçu dynamique avec gestes tactiles
│       │           │   ├── guide/               Centre d'aide & découverte modulaire (12 outils)
│       │           │   │   ├── FeatureGuideModel.kt Modèle typé (catégories, paramètres, démos)
│       │           │   │   ├── FeatureGuideRegistry.kt Registre des 12 guides photographiques
│       │           │   │   ├── FeatureGuideScreen.kt Écran avec recherche, filtres et fiche détaillée
│       │           │   │   └── InteractiveBeforeAfterSlider.kt Comparateur tactile interactif
│       │           │   └── studio/              Studio de gradation par étapes
│       │           │       ├── StudioComponents.kt  Composants d'étape partagés
│       │           │       ├── StudioStep1Source.kt Étape 1 : Sélection photo source
│       │           │       ├── StudioStep2Style.kt  Étape 2 : Sélection du style de référence
│       │           │       ├── StudioStep3Parameters.kt Étape 3 : Réglages colorimétriques
│       │           │       ├── StudioStep4Computing.kt Étape 4 : Progression du calcul JNI
│       │           │       ├── StudioStep5Result.kt Étape 5 : Rendu final et relais vers l'éditeur
│       │           │       └── StudioStepper.kt     Indicateur horizontal d'étapes
│       │           ├── theme/
│       │           │   ├── Color.kt             Primitives de teintes de base
│       │           │   ├── ColorCardReference.kt Références colorimétriques ColorChecker
│       │           │   ├── FontSpec.kt          Contrat typographique (Playfair, Jakarta, Mono)
│       │           │   ├── Shape.kt             Rayons de courbure standardisés
│       │           │   ├── SzlColors.kt         Tokens sémantiques étendus (SzlTheme.colors)
│       │           │   ├── SzlIconSet.kt        Jeu d'icônes sémantiques (SzlTheme.icons)
│       │           │   ├── Theme.kt             Composant racine StealZaLut_MobileTheme
│       │           │   ├── Themes.kt            Définition des 4 thèmes (Ambre, Cyan, OLED, Daylight)
│       │           │   ├── ThemeSpec.kt         Contrat de spécification de thème
│       │           │   └── Type.kt              Définition des styles Material 3 Typography
│       │           └── viewmodel/
│       │               ├── StealZaLutUiState.kt État immuable centralisé (@Immutable)
│       │               ├── StealZaLutViewModel.kt Façade UDF unique
│       │               └── delegate/            Délégués spécialisés par domaine
│       │                   ├── EditorLightroomDelegate.kt Moteur de réglages avancés
│       │                   ├── EditorSessionDelegate.kt  Session et historique d'édition
│       │                   ├── GradingPipelineDelegate.kt Pipeline colorimétrique Chaquopy
│       │                   ├── PresetLibraryDelegate.kt   Gestion de la bibliothèque de presets
│       │                   └── StudioFlowDelegate.kt      Flux du wizard Studio 5 étapes
│       ├── test/java/com/stealzalut/mobile/
│       │   ├── core/MonotoneCubicSplineTest.kt          Test interpolation Fritsch-Carlson
│       │   ├── data/model/LightroomSerializerTest.kt     Test sérialisation des modèles de retouche
│       │   ├── data/repository/PresetRepositoryTest.kt  Test persistance JSON des presets
│       │   ├── data/repository/ProjectRepositoryTest.kt Test persistance atomique des projets
│       │   ├── data/repository/SavedImageRepositoryTest.kt Test historique des images
│       │   ├── theme/DesignSystemGuardTest.kt           Test JVM de garde (zéro primitive en dur)
│       │   ├── theme/ThemeContrastTest.kt               Test conformité contraste WCAG AA
│       │   ├── ui/preview/GpuPreviewParityTest.kt       Test parité mathématique GPU vs Python
│       │   ├── ui/screens/editor/CropRegionTest.kt      Test physique de recadrage CropPhysics
│       │   ├── ui/screens/editor/ToneCurveInteractionTest.kt Test interaction tactile courbe
│       │   ├── ui/screens/guide/FeatureGuideRegistryTest.kt  Test intégrité du registre des 12 guides
│       │   ├── ui/screens/guide/GuideImageProviderTest.kt    Test cache des effets de guide
│       │   ├── ui/viewmodel/EditorUndoRedoStackTest.kt  Test pile Undo/Redo et coalescence
│       │   ├── ui/viewmodel/GradingRelayTest.kt         Test relais Studio ↔ Éditeur
│       │   ├── ui/viewmodel/PresetExportTest.kt         Test export de presets
│       │   ├── ui/viewmodel/SafExportIntegrationTest.kt Test flux d'exportation SAF
│       │   └── ui/viewmodel/StudioFlowNavigationTest.kt Test navigation du wizard Studio
│       └── androidTest/java/com/stealzalut/mobile/
│           ├── bridge/PythonBridgeIntegrationTest.kt    Test JNI Chaquopy sur appareil/émulateur
│           └── ui/MainColorGradingScreenTest.kt         Test UI instrumenté navigation
└── tests/                                       113 tests Pytest (100% verts)
    ├── conftest.py                              Fixtures Pytest partagées
    ├── test_auto_edit.py                        Tests analyse optique LightCraft
    ├── test_bridge.py                           Tests passerelle JNI bridge.py
    ├── test_colorset_worker.py                  Tests génération de palettes de référence
    ├── test_color_transfer.py                   Tests transport optimal (MKL, MVGD, Reinhard, HM)
    ├── test_lut_engine.py                       Tests treillis 3D, interpolation et chunking
    ├── test_lut_export.py                       Tests conformité Adobe .cube et intégrité
    ├── test_pipeline.py                         Tests pipeline 3 étapes et masques OKLab
    └── test_preset_manager.py                   Tests persistance v2, CRUD et migration presets
```
