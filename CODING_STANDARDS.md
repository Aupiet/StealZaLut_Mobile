# Normes de Développement et Architecture — StealZaLUT Mobile

Ce document définit les standards officiels de code, l'architecture logicielle, les conventions numériques et les règles de qualité applicables au projet **StealZaLUT Mobile**. Tout contributeur ou agent d'ingénierie logicielle doit s'y conformer rigoureusement.

---

## 1. Principes Architecturaux Directeurs

### 1.1 Règle d'Or WYSIWYG (*What You See Is What You Get*)
> **Toute modification visuelle affichée à l'écran doit obligatoirement être le résultat direct de l'application de la LUT 3D (`apply_lut_trilinear`).**

- **Interdiction formelle des décalages aperçu / export :** Un curseur de réglage (intensité, protection peau, luminance, lissage) ne doit **jamais** être un simple mélange visuel dans l'interface sans impacter la table de correspondance 3D.
- **Unicité du pipeline :**
  1. Les paramètres utilisateurs sont injectés dans la grille 3D via `refine_and_blend_lut()`.
  2. L'aperçu écran est généré par `apply_lut(source_preview, final_lut)`.
  3. L'export `.cube` est sérialisé depuis ce même `final_lut`.
  4. L'export image haute résolution applique ce même `final_lut`.
- Cette unicité garantit une fidélité colorimétrique absolue ($0$ différence ou $\le 1$ niveau de quantification 8 bits) entre l'application mobile et les logiciels de post-production professionnels (DaVinci Resolve, Adobe Premiere Pro, Final Cut Pro).

### 1.2 Séparation Stricte des Responsabilités (Python)

Le moteur Python embarqué (`stealzalut`) est partitionné en trois couches étanches :

1. **Couche Mathématique Fondamentale (`stealzalut/core/`)** :
   - Fonctions mathématiques pures et transformations matricielles.
   - Zéro dépendance d'I/O disque ou de framework applicatif.
   - Entrées et sorties sous forme de tenseurs NumPy typés.
   - Contient : `color_transfer.py`, `lut_engine.py`, `lut_export.py`, `color_card.py`.
2. **Couche d'Orchestration et Cache (`stealzalut/workers/`)** :
   - Gestion des flux de travail (chargement des images d'analyse, dimensionnement des proxys, appel du calcul de base, post-traitement, sauvegarde).
   - Maintien des caches LRU en mémoire vive (`_RAW_LUT_CACHE`, `_SRC_PROXY_CACHE`) pour assurer une réactivité $< 5\text{ ms}$ lors des ajustements de paramètres.
   - Contient : `transfer_worker.py`, `export_worker.py`.
3. **Façade Passerelle JNI (`stealzalut/bridge.py`)** :
   - Point d'entrée unique exposé à Kotlin / Android via Chaquopy.
   - Validation stricte des chemins de fichiers et des paramètres.
   - Capture intégrale de toutes les exceptions pour renvoyer des dictionnaires structurés : `{"status": "success", ...}` ou `{"status": "error", "error_code": "...", "message": "..."}`.
   - Aucun crash Python ne doit se propager non contrôlé vers la JVM Android.

---

## 2. Standards de Codage Python

### 2.1 Conventions PEP 8 et Typage Statique
- **Typage systématique :** Toutes les signatures de fonctions doivent comporter des annotations de type strictes (`from __future__ import annotations`, `typing`, `numpy.typing.NDArray`).
- **Docstrings complètes :** Rédigées au format Google / NumPy Docstring, spécifiant :
  - La description claire de l'opération mathématique.
  - La liste des arguments avec leurs plages valides (ex: `intensity in [0.0, 1.0]`).
  - La dimension et le type des tableaux retournés (ex: `shape (size ** 3, 3)`, dtype `float32`).
  - Les exceptions levées (`Raises:`).

### 2.2 Zéro "Magic Number"
- Aucune constante numérique littérale ne doit être codée en dur dans les algorithmes.
- Toutes les constantes (seuils de soft-clipping, dimensions de proxys, ratios d'écart-type, paramètres de détection des tons chairs, tolérances d'écrêtage, tailles de cache) doivent être importées depuis `stealzalut.config`.

### 2.3 Vectorisation Totale et Élimination des Boucles Python
- **Aucune boucle `for` Python sur les pixels ou les cellules de la grille 3D.**
- Toutes les opérations doivent être vectorisées en tirant parti du broadcasting et des routines BLAS sous-jacentes de NumPy.
- Le lissage spatial 3D doit utiliser un filtre gaussien 3D séparable le long des axes, maintenant l'exécution sous la barre des $3\text{ ms}$ pour une grille $33 \times 33 \times 33$.
- L'interpolation trilinéaire doit utiliser l'indexation vectorisée directe pour traiter des images $1024 \times 1024$ en un temps adapté au mobile.

### 2.4 Robustesse Numérique
- Division par zéro prévenue systématiquement avec `EPSILON = 1e-6` :
  ```python
  scale = ref_std / (src_std + np.float32(EPSILON))
  ```
- Bornage systématique des ratios d'écart-type pour empêcher les explosions de contraste :
  ```python
  scale[..., 0] = np.clip(scale[..., 0], MIN_STD_RATIO, MAX_STD_RATIO)
  ```
- Utilisation de fonctions douces $C^1$ en `np.tanh` pour le roll-off des ombres et hautes lumières (`apply_soft_clip`).

---

## 3. Conventions sur la Manipulation des Tenseurs NumPy

| Contexte | Type NumPy | Plage de Valeurs | Ordre des Canaux / Axes | Description |
| :--- | :--- | :--- | :--- | :--- |
| **I/O Fichiers Images** | `np.uint8` | $[0, 255]$ | `(H, W, 3)` RGB | Décodage / encodage Pillow, transit MediaStore. |
| **Grilles 3D LUT (Volume)** | `np.float32` | $[0.0, 1.0]$ | `(N, N, N, 3)` (B, G, R) | Grille spatiale 3D. Index 0 = B, 1 = G, 2 = R. Valeur = `[r, g, b]`. |
| **Grilles 3D LUT (Flat)** | `np.float32` | $[0.0, 1.0]$ | `(N³, 3)` R-fastest | Conforme spécification Adobe `.cube`. |
| **Espace Colorimétrique LAB**| `np.float32` | $L \in [0, 100]$, $a, b \in [-128, 127]$ | `(..., 3)` CIE L\*a\*b\* | Séparation stricte de la luminance ($L$) et chrominance ($a, b$). |
| **Espace Détection Peau** | `np.float32` | $Y \in [0, 1]$, $Cb, Cr \in [0, 1]$ | `(..., 3)` YCbCr | Modèle ITU-R BT.601 centré à $0.5$ pour $Cb, Cr$. |

---

## 4. Standards de Codage Kotlin & Android

### 4.1 Architecture et Unidirectional Data Flow (UDF)
- Respect du pattern MVVM / MVI avec Jetpack Compose.
- **État unique immuable :** `StealZaLutUiState` annoté avec `@Immutable`, exposant des collections immuables (`kotlinx.collections.immutable.ImmutableList`).
- **Protection du GIL Python :** Tout appel JNI vers Chaquopy doit s'exécuter exclusivement sur le thread dédié `pythonDispatcher` configuré via :
  ```kotlin
  val pythonDispatcher = Dispatchers.IO.limitedParallelism(1)
  ```
- **Gestion des annulations :** Tout changement rapide d'algorithme ou de photo source annule le job précédent (`activeGradingJob?.cancel()`) pour éviter les files d'attente bloquantes.

### 4.2 Nommage et Cohérence
- Constantes centralisées dans `com.stealzalut.mobile.core.AppConstants`.
- Tailles, paddings, élévations et rayons de courbure centralisés dans `AppDimens`.
- Rétrocompatibilité systématique des fonctions Kotlin du repository avec des valeurs par défaut alignées sur `AppConstants`.

---

## 5. Normes de Tests Unitaires et Validation

### 5.1 Suite Python (`tests/`)
Chaque module `core` et `workers` doit disposer d'une suite de tests Pytest couvrant :
1. **Contrats de forme et de type :** Préservation des dimensions, vérification des types (`uint8`, `float32`), absence de `NaN` ou d'infinis.
2. **Invariants Mathématiques Clés :**
   - `intensity = 0.0` renvoie l'exacte LUT identité ($\Delta_{\max} < 10^{-6}$).
   - `intensity = 0.5` génère des valeurs strictement au point médian.
   - `preserve_luminance = 1.0` conserve le canal $L$ de la source ($\Delta_L < 1.5$) tout en modifiant la chrominance $A, B$.
   - Protection peau : les coordonnées correspondant aux tons chairs convergent vers l'identité en présence de `skin_protection`.
   - Verrouillage neutre : les nœuds $(0,0,0)$ et $(1,1,1)$ restent neutres sans dérive de teinte.
3. **Validation WYSIWYG Complète :**
   - Comparaison pixel pour pixel entre l'image produite par `run_transfer_pipeline` et l'application directe du `.cube` exporté sur la même image source.
   - Écart maximal toléré $\le 1$ niveau 8 bits (dû uniquement à l'arrondi décimal ASCII `%.6f` de la spécification `.cube`).

### 5.2 Exécution Continue
Les tests unitaires doivent pouvoir être exécutés hors ligne, sans réseau, en une commande :
```bash
python -m pytest tests/
```
Un code ne respectant pas 100% de réussite sur l'ensemble des tests ne doit pas être intégré.
