# Phase R — Portage des fonctionnalités de zones du desktop vers mobile

Date : 2026-11-11
Statut : recherche uniquement, aucune implémentation de code ni build exécuté.

## 1. Limites et contexte de la recherche

- Vérifié (glob `reference/**`, 2026-11-11) : le dépôt de référence desktop n’est pas présent dans ce workspace courant (`reference/desktop/` est absent). La phase d’implémentation ne peut commencer qu’après l’ajout d’une copie lecture seule de ce dépôt, conformément au prompt.
- Vérifié (README.md, 2026-11-11) : le projet mobile cible Python 3.10+ via Chaquopy et précise `NumPy`, `SciPy` et `Color-Matcher` dans la stack scientifique.
- Vérifié (`app/build.gradle.kts`, 2026-11-11) : la configuration Chaquopy installe explicitement `numpy`, `Pillow`, `scipy` et `color-matcher`; il n’y a pas de dépendance `opencv-python-headless` ni de paquet OpenCV dans le build actuel.
- Vérifié (`app/src/main/python/stealzalut/utils/image_io.py`, 2026-11-11) : le moteur mobile actuel exploite déjà une palette dominante basée sur `Pillow` + `quantize(..., method=MEDIANCUT)`, donc le mode « palette / median-cut » est déjà présent et la phase zones doit s’appuyer dessus comme base de comparaison, pas comme duplication aveugle.
- Vérifié (`app/src/main/python/stealzalut/core/color_space.py`, 2026-11-11) : le projet mobile dispose déjà de conversions sRGB ⇄ OKLab et de `smoothstep`, ce qui donne une base solide pour deux éléments du portage desktop : Oklab et régularisation douce.
- Vérifié (`app/src/main/AndroidManifest.xml`, 2026-11-11) : le `FileProvider` est bien déclaré via `androidx.core.content.FileProvider` avec le fichier XML `app/src/main/res/xml/file_paths.xml`.
- Vérifié (`app/src/main/java/com/stealzalut/mobile/data/bridge/PythonBridgeRepository.kt`, 2026-11-11) : le repo possède déjà une API de partage de fichiers via `createShareIntent(...)` fondée sur `FileProvider`.

## 2. Table de décision

| Besoin | Options | Choix retenu | Preuve / source | Risque principal |
| --- | --- | --- | --- | --- |
| R1 — OpenCV ou SciPy ? | 1) `opencv-python-headless`, 2) `scipy` + Pillow + NumPy | Choix par défaut : `SciPy` sans OpenCV | Vérifié (`app/build.gradle.kts`, 2026-11-11) : le build n’installe ni `opencv` ni `cv2`. Supposé : `scipy.ndimage` + `scipy.cluster.vq.kmeans2` restent disponibles sous Chaquopy 3.10 Android/arm64. | Le pire cas est une régression Chaquopy sur arm64 ou un manque de `scipy.cluster` si la version exacte diffère. |
| R2 — Sinkhorn | 1) dépendance externe (`pot`), 2) code NumPy porté du desktop | Choix : Port direct de la logique NumPy du desktop, sans nouvelle dépendance | Vérifié (`app/build.gradle.kts`, 2026-11-11) : `pot` n’est pas installé. Supposé : le desktop a déjà un code NumPy compact et testable. | Si la version desktop est absente ou si le code est trop spécifique à son contexte, le port peut nécessiter un petit refactoring avant intégration. |
| R3 — Rendu du pack d’images | 1) Pillow / `ImageDraw`, 2) Canvas Android | Choix : Canvas Android pour la vue interactive + export composite simple ; Pillow seulement en secours si besoin de génération pure Python | Vérifié : le projet est déjà orienté Compose/Android UI et possède `FileProvider`, donc l’UI de partage est native. Supposé : le pack de zones est plus simple à générer dans Kotlin que dans un processus Python à télécharger. | Risque faible mais réel de temps de rendu sur grand pack ; il faut imposer un max 2048 px et un mode “miniature agrégée”. |
| R4 — Diagramme de correspondances | 1) dépendance Sankey, 2) `Canvas` + `Path.cubicTo` | Choix : `Canvas` pur, sans dépendance | Vérifié : le projet n’a pas de dépendance de graphes de liens dans la stack actuelle. Supposé : le besoin visuel est couvert par `Path.cubicTo` + `pointerInput` avec pastilles circulaires. | Risque faible ; le coût principal est le calcul d’ordonnancement du layout sur petits écrans tactiles. |
| R5 — Envoi à l’IA de l’utilisateur | 1) plusieurs images, 2) image composite unique + prompt + JSON | Choix : repli par défaut “une seule image composite” + prompt texte + `analysis.json` | Vérifié (AndroidManifest + FileProvider, 2026-11-11) : le partage de fichiers est déjà compatible. Supposé : beaucoup d’IA n’acceptent pas bien plusieurs images dans `ACTION_SEND_MULTIPLE`, donc le mode unique est le plus robuste. | Le risque est la qualité du choix d’image composite ; le bon compromis est 2048 px max, zones visibles, contours et identifiants lisibles. |
| R6 — État de l’art | 1) zones + match optimal, 2) palette globale seulement | Choix : zones + transport optimal (Sinkhorn) | Vérifié : le repo considère déjà les palettes moyennes comme un niveau plus faible que le moteur de transfert global. Supposé : le desktop a identifié le point de rupture “une seule distribution / aucune notion de quoi va où”. | Risque de sur-ingénierie si le moteur mobile n’est pas prêt à gérer des K zones de manière déterministe et testable. |
| R7 — Réutilisation interne | 1) réécrire des briques, 2) réutiliser les composants mobiles existants | Choix : réutiliser le pipeline existant et la pile d’éditeur/undo/export | Vérifié (`app/src/main/python/stealzalut/core/...`, 2026-11-11) : `color_space.py`, `pipeline.py`, `bridge.py`, `PendingExport` et la pile d’undo existent déjà. | Risque de couplage si la zone transfer se branche mal dans le pipeline `bake_full_pipeline_lut` et on double-implémente l’étape 2. |
| R8 — Licences | 1) ajouter une dépendance lourde, 2) rester dans les dépendances MIT/BSD/Apache | Choix : aucune dépendance native ou modèle non permis ; aucun poids de modèle externe avant validation | Vérifié : le build actuel ne contenait qu’outils déjà autorisés (`numpy`, `Pillow`, `scipy`, `color-matcher`). Supposé : les futures entrées de modèle segmentation sémantique devront être vérifiées avant usage. | Risque juridique/poids APK si un modèle non permissif est utilisé malgré le contrat du prompt. |

## 3. Mini-spikes jetables (hors `app/`)

### R1 — Spike : OpenCV vs SciPy sur Chaquopy 3.10 / arm64

But : vérifier si le port des zones peut se faire sans dépendance native ni risque ABI.

- Vérifié (`app/build.gradle.kts`, 2026-11-11) : le build installe `numpy`, `Pillow`, `scipy` et `color-matcher`, mais pas `opencv-python-headless`.
- Vérifié (`app/src/main/python/stealzalut/core/spatial_filters.py`, 2026-11-11) : le code mobile utilise déjà `scipy.ndimage` pour les filtres spatiaux (`gaussian_filter`, `sobel`, `convolve`, `zoom`, etc.), donc le dépôt est déjà aligné avec la voie SciPy.
- Supposé (car non exécuté ici) : `scipy.cluster.vq.kmeans2` et `scipy.ndimage` sont suffisamment stables sous Chaquopy 3.10 pour exécuter du k-means déterministe sur des images de taille 512–1024 px et du lissage 2D/3D.
- Choix recommandé : utiliser `scipy.cluster.vq.kmeans2(minit='++', seed=...)` pour la partition Oklab, `scipy.ndimage.label` / `distance_transform_edt` / `median_filter` pour la régularisation et les masques, et `Pillow.ImageDraw` pour le rendu des contours et libellés.
- Impact estimé : pas de nouvelle dépendance native, pas d’augmentation de taille d’APK en dehors du code Python. Risque : à vérifier sur arm64 réel avant validation de phase.

### R2 — Spike : implémentation Sinkhorn sans `pot`

But : confirmer que la table de correspondance peut être calculée en NumPy pur et rester déterministe.

- Vérifié (`app/build.gradle.kts`, 2026-11-11) : `pot` n’est pas installé. Toute dépendance supplémentaire serait un écart de conception.
- Supposé : la référence desktop code le Sinkhorn en une dizaine de lignes NumPy, ce qui est compatible avec le port attendu ; le coût de transport peut être calculé sur un tableau d’assignation `C` de taille `(K, K)` ou `(K_src, K_ref)` avec normalisations alternées `row/col` jusqu’à convergence.
- Recommandation : porter la version desktop telle quelle dans `core/matching.py` et la garder purement NumPy ; ne pas introduire d’abstraction `pot` tant que le spike n’a pas démontré un gain net.

### R3 — Spike : rendu du pack d’images

But : décider si les étiquettes / contours / vues de zones sont dessinés côté Python ou côté Kotlin.

- Vérifié (AndroidManifest + `FileProvider`, 2026-11-11) : l’application sait déjà partager des fichiers générés.
- Supposé : la génération de pack complet dans Python sera plus lente et plus risquée qu’un rendu “composite” dans le UI layer, surtout si on veut de l’édition tactile et des contours surlignés.
- Recommandation : le "pack" partagé à l’IA doit être un agrégat composite (visuel source + référence + contours de zones + labels S1…Sk) rendu en Kotlin à partir d’un bitmap 2K max ; le texte/labels seront dessinés avec les polices système, pas avec un paquet de police externe. Cela évite à la fois la licence de font et le risque de dépendance non autorisée.

### R4 — Spike : diagramme de correspondances sans bibliothèque tierce

But : vérifier si une dépendance supplémentaire est inutile.

- Vérifié : le projet ne dépend actuellement d’aucune bibliothèque de graphes ou de Sankey.
- Supposé : les blocs source / cible peuvent être représentés comme deux colonnes de pastilles avec un `Canvas` et des courbes `Path.cubicTo`; chaque lien porte poids, couleur et statut (auto / utilisateur / IA). Cela reste tactile et compatible Compose sans rupture de design system.
- Recommandation : garder le composant autonome et léger : pas de bibliothèque, pas de new dependency.

### R5 — Spike : partage IA / collage / repli

But : sécuriser le circuit “préparer -> envoyer -> coller retour → parse → appliquer”.

- Vérifié : le `FileProvider` est déjà présent et la création d’`Intent.ACTION_SEND` est déjà utilisée (`PythonBridgeRepository.kt`).
- Supposé : la majorité des apps d’IA acceptera mieux une image composite unique plutôt qu’un pack multipart avec plusieurs fichiers, notamment sur Android. Donc le repli recommandé est : image composite (zones visibles, 2048 px max) + prompt texte + `analysis.json` dans le cache local ; l’export IA ne doit pas dépendre d’un partage multi-fichiers.
- Supposé : la lecture du presse-papiers doit rester explicite (`LocalClipboard` / bouton “Coller la réponse de l’IA”), car les sytèmes Android récents affichent déjà un avis de collage ; il faut éviter une lecture automatique au lancement du composant.

## 4. Défis et points d’attention des interfaces, Python et Android

### 4.1 Python : garde-fou sans dépendance native supplémentaire

- Vérifié : le projet mobile a déjà démontré un modèle mathématique vectorisé en OKLab (`core/color_space.py`) et des filtres de spatialisation basés sur `scipy.ndimage` (`spatial_filters.py`).
- Recommandation : conserver le chemin SciPy et éviter tout ajout de `opencv` tant que le spike `kmeans2 + ndimage` ne montre pas de baisse de qualité ou de perf clairement supérieure. La dépendance native ajoute du poids APK, de la variance ABI et un risque de crash ARM64, ce qui va contre les exigences explicitement citées dans le prompt.

### 4.2 Couche zones : recycler les briques existent

- Vérifié : le moteur mobile possède déjà `rgb_to_oklab`, `oklab_to_rgb`, `smoothstep`, le pipeline 3 étapes, les protections neutres / peau / luminance et le pipeline export `.cube`.
- Recommandation : la zone transfer doit produire une LUT brute qui alimente `bake_full_pipeline_lut` sans “doubler” les protections déjà présentes dans l’étape 2. C’est précisément l’intégration imposée dans le prompt et elle doit être respectée pour rester WYSIWYG.

### 4.3 UI : garder la pile existante et la rendre tactile

- Vérifié : le repo a déjà une pile d’undo/redo, un `StealZaLutUiState` immuable et un `activeGradingJob` unique et annulable.
- Recommandation : `ZonesDelegate` est la bonne abstraction pour l’UI de correspondances et la logique d’état, en gardant un seul `pythonDispatcher` et un seul flux de travail. Cela est cohérent avec ADR-02 et la séparation déjà en place.

### 4.4 Export et partage : même cadre, même règles

- Vérifié : `PendingExport` est déjà un point d’entrée sûr et `FileProvider` existe déjà.
- Recommandation : l’export `.szl`, `.szlpreset` et le partage IA doivent passer par le même contrat de sécurité et par le même mécanisme d’URI SAFE Android. On garde le chemin d’export unique et on évite de créer un second mécanisme ad hoc.

## 5. Décisions sur lesquelles le port reste incertain (à trancher à GO)

- Décision 1 — `SciPy` vs `cv2` : la recommandation est `SciPy`, mais cela reste un point à valider au moment d’une exécution réelle sur arm64.
- Décision 2 — Rendu visuel du pack IA : le plus robuste est une image composite unique, mais le besoin exact d’un mode “pack complet” dépendra de la qualité du retour utilisateur dans les tests réels.
- Décision 3 — Choix d’écran pour les correspondances : plein écran vs feuille modale. L’architecture actuelle favorise les feuilles modales et les composants isolés, mais un écran plein cadre peut être plus lisible pour les comparaisons de zones sur petit écran.
- Décision 4 — Traçage des identifiants de zones dans la recette IA : conserver `analysis_id` et le déterminisme du tri `(K, luminance, centroid)` est nécessaire, mais la politique exacte “verrouillage / auto / retrait” reste à décider avec le design utilisateur.
- Décision 5 — Composants strictement permis en licence : aucune dépendance non MIT/BSD/Apache. Le point “SegFormer / ONNX / modèle sémantique” est hors périmètre tant que la licence n’a pas été vérifiée.

## 6. Conclusion de la phase R

La voie la plus prudente pour le portage desktop → mobile est la suivante :

1. Reprendre les briques existantes dans le dépôt mobile (OKLab, `smoothstep`, `pipeline.py`, `bridge.py`, `FileProvider`, `PendingExport`, `undo/redo`).
2. Ajouter les zones uniquement via une couche Python pure, sans dépendance native, en privilégiant `SciPy` + `Pillow` + NumPy.
3. Garder le moteur de zones hors des protections existantes du pipeline 2 mobile, et faire alimenter la LUT brute dans `bake_full_pipeline_lut` avec mécanisme de sortie unique WYSIWYG.
4. Construire le pont IA sur un mode de repli simple, robuste et lisible par les applications d’IA, en évitant tout `ACTION_SEND_MULTIPLE` fragile.
5. N’ajouter aucune dépendance supplémentaire avant validation de compatibilité Android/Chaquopy/arm64.

Le rapport est prêt pour validation. À partir de ce point, la suite doit être validée par un GO explicite avant la phase de codage.
