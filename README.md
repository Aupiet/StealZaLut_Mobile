# StealZaLUT Mobile

[![Android API 24+](https://img.shields.io/badge/Android-API%2024%2B%20(Nougat%20%E2%86%92%2015)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Python 3.10+ (Chaquopy)](https://img.shields.io/badge/Backend-Python%203.10%20%7C%20Chaquopy%2017.2.0-3776AB?logo=python&logoColor=white)](https://chaquo.com/chaquopy/)
[![NumPy & SciPy](https://img.shields.io/badge/Math-NumPy%20%7C%20SciPy%20%7C%20Color--Matcher-013243?logo=scipy&logoColor=white)](https://scipy.org/)
[![Tests: 113 Pytest + 87 Kotlin](https://img.shields.io/badge/Tests-113%20Pytest%20%2B%2087%20Kotlin-brightgreen.svg)](tests/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Architecture: ARCHITECTURE.md](https://img.shields.io/badge/Doc-ARCHITECTURE.md-blue.svg)](ARCHITECTURE.md)

> **Application mobile Android 100% hors-ligne d'étalonnage cinématographique et d'édition photo studio, avec pipeline non-destructif en 3 étapes, Auto-Edit intelligent, gestionnaire de presets (`.szlpreset`) et export Adobe 3D LUT (`.cube`) de qualité professionnelle.**

---

## 1. Présentation du Projet & Parité Desktop

**StealZaLUT Mobile** est l'équivalent mobile complet de la version de référence desktop [**StealZaLUT Desktop**](https://github.com/whoismathieu/stealzalut.git). L'application transpose la puissance colorimétrique professionnelle des stations de travail de post-production cinématographique directement dans le creux de la main, sur smartphone et tablette Android.

### Parité Visuelle à 100% avec StealZaLUT Desktop
Sur Mobile comme sur Desktop, l'algorithme garantit un transfert colorimétrique dynamique et éclatant :
- **Dynamique nocturne et urbaine préservée** : Les ambiances riches (ex: ciel cyan/émeraude de nuit) et les petits détails très saturés (néons rouges et jaunes, balises d'immeubles, éclairages urbains) sont fidèlement capturés grâce à un échantillonnage haute fidélité ($512\text{ px}$ en interpolation `LANCZOS`) et à l'élimination des compressions destructrices.
- **Rendu initial pur à 100%** : Toutes les protections du filtre (verrouillage de luminance, protection de l'axe neutre, roll-off des extrêmes) sont fixées à `0.0` par défaut afin de délivrer immédiatement à l'application d'un style l'effet cinématographique pur de l'image de référence. L'utilisateur affine ensuite précisément son rendu à l'aide des curseurs de réglage.
- **Principe WYSIWYG Universel** : Ce que vous voyez sur l'écran OLED 120 Hz de votre smartphone correspond au pixel près à ce qui est encodé dans le fichier exporté ou injecté dans votre timeline vidéo desktop.

---

## 2. Fonctionnalités Majeures

### 2.1 Pipeline Non-Destructif en 3 Étapes

Le cœur du traitement repose sur un pipeline vectorisé modulaire :

```mermaid
graph LR
    subgraph Etape1 ["Étape 1 : Pure Filter"]
        PF["Transfert Colorimétrique Pur\n(HM-MKL-HM, MVGD, Reinhard, HM)\nou LUT 3D importée"]
    end

    subgraph Etape2 ["Étape 2 : Retravail du Filtre"]
        RF["Affinement Ciblé (OKLab)\n• Intensité (0-100%)\n• Luma Lock (Lumière source)\n• Extremes Roll-off\n• Neutral Axis Lock\n• Skin Tone Lock\n• Lissage Spatial 3D"]
    end

    subgraph Etape3 ["Étape 3 : Post-Grading"]
        PG["Laboratoire Studio\n• Exposition (EV), Contraste, Ombres, Hautes Lumières\n• Température, Teinte, Vibrance, Saturation\n• Clarté, Netteté, Grain argentique"]
    end

    Etape1 --> Etape2 --> Etape3
```

1. **Étape 1 : Pure Filter**
   - Implémente les algorithmes de transfert optimal de distributions : `hm-mkl-hm`, `mkl`, `hm-mvgd-hm`, `mvgd`, `reinhard` (CIE $L^*a^*b^*$ D65) et `hm` (Histogram Matching 1D par canal).
   - Construit une table de correspondance 3D unifiée (`compute_lut` / `generate_lut_from_pair`) à partir d'échantillons conservant l'aspect ratio et les détails lumineux fins.
2. **Étape 2 : Retravail du Filtre (Calibration)**
   - **Intensité globale** : Interpolation linéaire fluide entre l'image originale et le filtre pur.
   - **Luma Lock** : Verrouillage de la luminance perçue dans l'espace perceptuel uniforme OKLab.
   - **Extremes Roll-off** : Préservation progressive des blancs spéculaires ($L > 0.85$) et des ombres profondes ($L < 0.15$) via lissage polynomial `smoothstep`.
   - **Neutral Axis Lock** : Protection des gris neutres et des tons achromatiques ($C < 0.03$) contre les dominantes parasites.
   - **Skin Tone Lock** : Masquage gaussien perceptuel ciblé sur les carnations humaines ($h \in [20^\circ, 65^\circ]$, $C \in [0.03, 0.25]$).
   - **Lissage Spatial 3D** : Convolutions gaussiennes 3D séparables garantissant des dégradés réguliers sans bandes ni postérisation.
3. **Étape 3 : Post-Grading & Effets Studio**
   - **Lumière** : Exposition calibrée en EV ($2^{EV}$ en espace linéaire), Contraste pondéré, Hautes Lumières et Ombres.
   - **Couleur** : Balance des blancs (Température Kelvin et Teinte vert-magenta), Saturation globale et Vibrance intelligente (amplification prioritaire des tons désaturés sans sursaturer les visages).
   - **Effets** : Clarté (contraste local par filtre passe-haut), Netteté (masque flou unsharp mask) et Grain photo procédural (bruit gaussien modulé selon la luminance).

---

### 2.2 Relais Studio ↔ Éditeur & Spécialisation des Rôles (Règle WYSIWYG)

Pour garantir une expérience professionnelle sans ambiguïté et un respect absolu de la règle d'or WYSIWYG :
- **Studio (Étalonnage Pur)** : Se concentre à 100% sur le transfert colorimétrique par 3D LUT (`HM_MKL_HM`, `MKL`, `Reinhard`, `HM`). Les étapes 1 et 3 sont épurées de tout doublon d'édition. L'étape 3 propose une organisation sans défilement en 4 onglets optimisés (**Algorithme**, **Grille 3D LUT**, **Lumière** avec Verrouillage de Luminance, **Couleur** avec Protection des Tons Chairs, Axe Neutre et Lissage Spatial 3D). L'étape 5 intègre un bouton d'enregistrement direct de la photo étalonnée en galerie (`Enregistrer la photo dans la galerie`).
- **Support Universel RAW & DNG** : Intégration d'un sélecteur dédié (« Ouvrir RAW / DNG ») via le sélecteur de fichiers système Android (SAF), décodage des aperçus EXIF intégrés et prise en charge des négatifs numériques DNG/RAW via le plugin TIFF de Pillow.
- **Guide des Outils Interactif Étendu (15 Outils & Création de LUT)** :
  - Intègre l'ensemble des fonctionnalités majeures de création de LUT par image de référence (Algorithmes de transfert MKL/HM/Reinhard/Cinématique, Masques & Protections OKLab, Résolution de grille 3D 17³/33³/64³, Exportation `.cube`).
  - Démonstration interactive avant/après basée sur les photos d'exemple réelles (Photo de départ, Photo étalonnée, Palette de référence).
- **Dialogue d'Exportation Haute Fidélité Réglable** : Choix direct dans la boîte de dialogue de la résolution cible (1080p FHD, 1440p QHD, 4K UHD, Natif), du format (JPEG, PNG) et de la qualité de compression avec estimation en temps réel du poids en Mo et des dimensions en pixels.
- **Bouton « Retoucher dans l'éditeur » (Étape 5)** : Permet de basculer instantanément le résultat de l'étalonnage Studio vers l'éditeur autonome en exportant la 3D LUT en cache (`editorCustomLutPath`). L'éditeur s'ouvre avec la photo source, la LUT préchargée, tous les curseurs à zéro et un affichage instantané sans délai de recalcul (parité WYSIWYG prouvée avec $\Delta \le 1$ niveau 8 bits).
- **Auto-Edit « LightCraft » dans l'Éditeur** : L'analyse optique logarithmique Rec. 2020 calibrée sur carte 18% est directement intégrée dans l'éditeur autonome, ajustant dynamiquement l'exposition, le contraste, les ombres, les hautes lumières et la vibrance.

---

### 2.3 Mode "Éditeur Photo Seul" (Standalone Photo Studio) & Ergonomie Mobile

Un espace de création dédié accessible directement depuis la barre de navigation principale :
- **Retouche autonome** : Modifiez n'importe quelle photo de votre galerie sans nécessiter d'image de référence B.
- **Importation de LUTs externes & Presets dynamiques** : Chargez n'importe quel fichier `.cube` tiers et appliquez-lui le pipeline complet (Retravail Étape 2 + Grading Étape 3) ou piochez dans vos presets personnalisés sans dépendance de styles en dur.
- **Prévisualisation GPU Instantanée à 2 Étages (AGSL RuntimeShader & ColorMatrix)** :
  - *Étage 1 (GPU / < 1 ms, 60–120 FPS)* : Rendu instantané sur 100% des outils (Exposition, Contraste, Balance des blancs, Courbes de tonalité, Mixeur HSL 8 bandes, Roues Color Grading 3 voies, Étalonnage capteur, Vignettage, Grain et Netteté). Le calcul opère par deltas relatifs ($\Delta = \text{actuel} - \text{Python}$), éliminant toute latence perçue au toucher.
  - *Étage 2 (Fidélité maximale en arrière-plan)* : Déclenchement automatique de la passe complète Python Chaquopy dès le relâchement du geste ou après une inactivité de 200 ms, garantissant une parité visuelle absolue ($\Delta E \le 0.5$ niveau 8 bits).
- **Physique de Recadrage & Redressement Fluide (`CropPhysics`)** :
  - *Verrouillage strict des ratios* : Libre, Original, Carré 1:1, Portrait 4:5, Photo 3:2, Cinéma 16:9 calculés sur les pixels physiques réels ($k = \frac{W_{\text{image}}}{H_{\text{image}} \cdot R_{\text{target}}}$).
  - *Ergonomie tactile pro* : 8 poignées (équerres L-brackets blanches de 3 dp avec ombre portée, encoches centrales) et zones de capture généreuses (44 dp).
  - *Grille des tiers dynamique* : Fading fluide de la grille (`gridAlpha`) visible uniquement pendant le déplacement.
  - *Redressement d'horizon GPU 120 FPS* : Rotation en temps réel de l'image sous la grille de niveau fixe, avec découplage du buffer de prévisualisation (zéro rétrécissement récursif).
- **Courbes de Tonalité Spline Monotone (Fritsch-Carlson)** :
  - *Parité mathématique 100%* : Algorithme Hermite monotone garantissant l'absence totale d'oscillations et d'inversions de pente.
  - *Manipulation tactile par delta relatif* : Zéro saut visuel au toucher, création/sélection intuitive, retour haptique et coordonnées RVB dynamiques [0, 255].
- **Gestionnaire de Projets d'Édition Non-Destructifs** :
  - *Persistance Sidecar Atomique* : Sauvegarde automatique débouncée des sessions dans `filesDir/projects/<id>/` (`project.json`, photo locale et vignette d'aperçu).
  - *Écran d'accueil Pro* : Historique des projets récents avec vignettes, métadonnées relatives, renommage, duplication et suppression sécurisée.
- **Pile Annuler / Rétablir (Undo/Redo) Profonde & Coalescence de Gestes** :
  - Coalescence des micro-ticks de curseurs (un seul snapshot par geste tactile), plafond FIFO de 50 états, couverture intégrale de tous les réglages Étape 2/3, modèles Lightroom et recadrage.
- **Partage Avancé & Fiche Recette** :
  - Dialogue d'exportation étendu permettant de partager la 3D LUT `.cube`, la vignette haute fidélité `.png` et la fiche recette de développement au format JSON.
- **Expérience tactile haut de gamme** :
  - **Comparaison tactile robuste** : Appui maintenu (`HOLD_TO_COMPARE`) avec détection stationnaire de 400 ms sur toute la surface et relâchement garanti sans blocage.
  - **Pinch-to-zoom & Panoramique synchronisés** : Inspectez les détails jusqu'à $4\times$, réinitialisable par double-tap ($1\times$).
  - **Double-tap Reset** : Double-cliquez sur n'importe quel curseur pour le ramener à sa valeur neutre.
  - **Histogramme RVB temps réel** : Analyse dynamique des canaux Rouge, Vert, Bleu superposable à l'image, agrandissable en HUD étendu.

---

### 2.4 Système de Presets d'Édition (`.szlpreset` v2)

- **Format ouvert et versionné (Schéma v2)** : Fichiers JSON légers (`.szlpreset`) enregistrant la totalité de vos réglages (Étape 2, Étape 3, métadonnées, chemin de la LUT associée et miniature encodée) avec migration automatique et rétro-compatibilité totale avec les anciens formats non versionnés.
- **Opérations étendues** :
  - Sauvegarder, appliquer, supprimer, dupliquer et renommer des presets.
  - Importer et exporter des fichiers `.szlpreset` autonomes pour le partage entre appareils.

---

### 2.5 Qualité d'Image HD & Exportation Conforme Adobe 3D LUT (`.cube`)

- **Conformité stricte Adobe** : Ordre de parcours $B \to G \to R$ (le canal Rouge varie le plus rapidement dans le fichier texte).
- **Assainissement des données** : Clamping rigoureux dans $[0.0, 1.0]$, remplacement des valeurs `NaN` et `Inf`, en-têtes standardisés reconnus par DaVinci Resolve, Premiere Pro, Final Cut Pro, Photoshop, CapCut et les moniteurs de tournage SmallHD / Atomos.
- **Baking complet des 3 Étapes** : Tous les réglages colorimétriques de l'Étape 1, de l'Étape 2 et de l'Étape 3 sont fusionnés dans la table `.cube` exportée (avec contournement automatique des effets 2D spatiaux non-linéaires).
- **Tiling Mémoire Anti-OOM (Images > 12 MP)** : Traitement vectorisé par blocs/tranches de rangées (plafond 2 Mpx par tranche) garantissant une empreinte RAM minimale ($< 100\text{ Mo}$) et zéro artefact de raccord aux bords de tuiles, même sur des photographies de 50 à 100 Mpx.
- **Préservation Intégrale des Métadonnées EXIF** : Conservation automatique de l'orientation (normalisée à 1 pour éviter toute double rotation), de l'appareil, de l'objectif, de l'exposition et des données de capture.

---

### 2.6 Design System Multi-Thèmes & Accessibilité (WCAG AA)

- **4 Thèmes interchangeables** :
  - *Ambre Noir* : Esthétique studio cinématographique sombre d'origine.
  - *Cyan Studio* : Ambiance chambre noire professionnelle cyan profond.
  - *OLED Noir Absolu* : Fond 100% noir pour économie de batterie sur écrans AMOLED.
  - *Lumière du Jour* : Thème clair à fort contraste pour utilisation en extérieur sous le soleil.
- **Zéro Flash au Démarrage & Zéro Artefact** : Démarrage synchronisé avec `windowBackground`, bascule à chaud sans perte d'état et tests de contraste WCAG AA validés par test JVM.

---

### 2.7 Centre d'Aide & Guide Interactif des 12 Outils Photographiques

Pour accompagner les photographes sans encombrer le flux d'édition :
- **Catalogue complet des 12 outils** : Fiches détaillées pour le Recadrage & Redressement, Courbes de tonalité, Mixeur HSL, Color Grading, Profils capteur, Détails & Netteté, Corrections optiques, Déformation géométrique, Flou d'objectif bokeh, Effets & Grain, LUTs 3D, et Transfert de style.
- **Comparateur Tactile Unifié `DoubleViewComparison`** : Utilise le même composant d'excellence que le Studio avec les modes Split-Wipe (rideau draggable), Side-by-Side (côte à côte) et Hold-to-Compare (appui maintenu).
- **Démonstrations Réelles & Effets Pré-rendus en Cache** : Intégration de photographies réelles d'étalonnage (`guide_sample_depart.jpg`, `guide_sample_etalonnee.jpg`, `guide_sample_reference.jpg`) avec génération et mise en cache mémoire d'effets contextuels fluides pour chaque outil.
- **Navigation Hiérarchique Sans Faille** : Le retour arrière depuis un outil ramène directement à la liste des outils sans quitter inopportunément la section d'aide.

## 3. Architecture Technique

StealZaLUT Mobile s'appuie sur une Clean Architecture réactive liant l'UI déclarative Jetpack Compose au moteur vectorisé NumPy/SciPy via Chaquopy :

```mermaid
graph TD
    subgraph UI ["Couche Présentation Jetpack Compose"]
        ApplyScreen["Studio de Gradation\n(ApplyThemeScreen.kt)"]
        EditorScreen["Éditeur Photo Seul\n(StandaloneEditorScreen.kt)"]
        PalettesScreen["Bibliothèque & Palettes\n(PalettesScreen.kt)"]
    end

    subgraph VM ["Couche Métier Android"]
        ViewModel["StealZaLutViewModel.kt\n(StateFlow, Debounce 150ms, Coroutines)"]
        BridgeRepo["PythonBridgeRepository.kt\n(Façade Chaquopy thread-safe)"]
    end

    subgraph CorePython ["Moteur Vectorisé Python (Chaquopy 17.2.0 / Python 3.10)"]
        Bridge["bridge.py (Points d'entrée)"]
        Pipeline["core/pipeline.py (Pipeline 3 Étapes)"]
        ColorSpace["core/color_space.py (sRGB / OKLab / Linear)"]
        ColorTransfer["core/color_transfer.py (MKL, MVGD, Reinhard, HM)"]
        LutEngine["core/lut_engine.py (Lattice 3D, Trilinear)"]
        LutExport["core/lut_export.py (Adobe .cube)"]
        AutoEdit["core/auto_edit.py (LightCraft Tone Mapping)"]
        PresetManager["core/preset_manager.py (.szlpreset CRUD)"]
    end

    UI --> ViewModel
    ViewModel --> BridgeRepo
    BridgeRepo --> Bridge
    Bridge --> Pipeline & ColorTransfer & LutEngine & LutExport & AutoEdit & PresetManager
    Pipeline --> ColorSpace & LutEngine
```

---

## 4. Tests et Instructions de Build

### Prérequis
- **Android Studio** : Ladybug ou supérieur
- **JDK** : Version 17 ou supérieure
- **Python** : 3.10 ou supérieur (avec `numpy`, `scipy`, `pillow`, `pytest`)

### Exécuter les tests unitaires Python (Core mathématique)
**PowerShell (Windows) :**
```powershell
$env:PYTHONPATH="app/src/main/python"
pytest tests/ -q
```
**Bash (Linux / macOS) :**
```bash
PYTHONPATH=app/src/main/python pytest tests/ -q
```
> **Résultat attendu :** 113 tests passés avec succès (validation des conversions OKLab, transport optimal MKL/MVGD, génération de LUTs, baking 3 étapes, format Adobe `.cube`, tiling mémoire anti-OOM, préservation EXIF, persistance presets v2 et parité GPU preview).

### Exécuter les tests unitaires Android (Kotlin JVM)
**PowerShell (Windows) :**
```powershell
.\gradlew.bat testDebugUnitTest
```
**Bash (Linux / macOS) :**
```bash
./gradlew testDebugUnitTest
```
> **Résultat attendu :** 85 tests unitaires JVM passés avec succès (garde du Design System, contrastes WCAG AA, relais Studio ↔ Éditeur, persistance JSON, exports SAF, physique de recadrage CropPhysics, interpolation Fritsch-Carlson, gestionnaire de projets, parité GPU preview, pile Undo/Redo et registre du guide). L'application comprend également 2 tests instrumentés (`androidTest`) pour un total de 87 tests Kotlin.

### Lancer l'analyse statique Android Lint
**PowerShell (Windows) :**
```powershell
.\gradlew.bat lintDebug
```
**Bash (Linux / macOS) :**
```bash
./gradlew lintDebug
```

### Compiler le projet Android
**PowerShell (Windows) :**
```powershell
.\gradlew.bat compileDebugKotlin
```
**Bash (Linux / macOS) :**
```bash
./gradlew compileDebugKotlin
```

---

## 5. Licence
Projet sous licence [MIT](LICENSE).
Inspiré par le projet original [StealZaLUT Desktop](https://github.com/whoismathieu/stealzalut.git) par Mathieu.

*Dernière mise à jour : 10 novembre 2026*
