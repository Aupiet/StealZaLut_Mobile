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

---

## ADR-06 : Contrat d'Export Unique & Vérification d'Intégrité des 3D LUT (.CUBE)

- **Contexte :** Lors de l'exportation d'un fichier `.cube`, le sélecteur Android SAF (`CreateDocument`) pré-créait un document vide (0 octet). La méthode `saveExportedCubeToUri` utilisait une référence privée `pendingExportCubePath` désynchronisée, et `openOutputStream(uri)?.use {}` ignorait silencieusement un flux nul sans vérifier la taille écrite.
- **Décision :**
  1. Modèle d'export unique et universel `PendingExport(sourcePath, suggestedFileName, exportType)` intégré dans `StealZaLutUiState`, alimenté par tous les points d'export (Studio Étape 5, Éditeur Autonome, Presets de Bibliothèque).
  2. Routine d'enregistrement robuste `savePendingExportToUri(destinationUri)` : vérification stricte `bytesCopied == expectedBytes > 0`. En cas d'anomalie ou de flux nul, suppression automatique du fichier placeholder créé via `DocumentsContract.deleteDocument(contentResolver, destinationUri)` et émission d'un `UiText` explicite.
  3. Validation d'intégrité stricte côté Python dans `lut_export.py` / `export_cube()` : vérification que le fichier écrit existe, sa taille $> 0$, l'en-tête `LUT_3D_SIZE` est valide et contient exactement $N^3$ triplets RVB valides (prouvé par `test_lut_export.py`).
  4. L'exportation d'une 3D LUT depuis un preset de bibliothèque requiert une photo source pour régénérer la table avec parité absolue.

---

## ADR-07 : Décodage Biaisé/Proxy Anti-Crash & Isolation Mémoire (Palettes & Auto-Edit)

- **Contexte :** L'extraction de palettes sur des clichés très haute résolution (50+ MP) provoquait des crashs natifs non interceptables par `try/catch` lors du décodage bitmap pleine résolution en mémoire vive (multiples copies volumineuses de dizaines de mégaoctets avant sous-échantillonnage).
- **Décision :**
  1. Implémentation de `load_proxy_image(path, max_dim)` dans `stealzalut/utils/image_io.py` exploitant la méthode native de Pillow `Image.draft("RGB", (max_dim, max_dim))` pour décoder immédiatement à basse résolution sans instancier le tenseur complet.
  2. Utilisation systématique de `load_proxy_image` pour l'extraction de palettes (`PALETTE_RESIZE_DIM = 512`), l'Auto-Edit (`MAX_ANALYSIS_DIMENSION = 1024`), et la mire de calibrage (`generate_color_card`).
  3. Sécurisation de l'extraction de palette côté Python : renvoi d'un code d'erreur structuré `ERR_EMPTY_PALETTE` au lieu d'un faux succès lorsque les dominantes sont vides.
  4. Côté Kotlin : encapsulation dans `paletteExceptionHandler` (`CoroutineExceptionHandler`), décodage borné à 512 px et gestion des erreurs avec `R.string.err_palette_extraction_failed` dans les 3 langues (FR, EN, ES).

---

## ADR-08 : Refonte du Flux Studio ↔ Éditeur & Clarification des Rôles (WYSIWYG)

- **Contexte :** Le Studio contenait des doublons de retouche (bouton Auto-Edit aux étapes 1 et 3, panneau d'édition rétractable inline à l'étape 5) qui parasitaient l'étalonnage et risquaient de violer la règle d'or WYSIWYG si les ajustements d'exposition n'étaient pas répercutés dans la 3D LUT.
- **Décision :**
  1. **Spécialisation nette :** Le Studio est dédié à 100 % au transfert colorimétrique par 3D LUT. L'éditeur autonome est dédié à la retouche photographique fine et aux finitions de style.
  2. **Nettoyage complet :** Suppression de l'Auto-Edit des étapes 1 et 3 du Studio, suppression du panneau d'édition inline et des curseurs d'ajustement de l'étape 5. Suppression propre du code mort (`applyAutoEdit`, sliders inline, callbacks et chaînes associées).
  3. **Studio Étape 3 :** Viewport d'aperçu compact épinglé en haut de l'écran avec comparaison rideau (`DoubleViewComparison` en mode `SPLIT_WIPE`), restant visible pendant le défilement de tous les curseurs de réglage. Préservation de l'ancien aperçu durant les calculs pour éliminer tout clignotement.
  4. **Studio Étape 2 :** Affichage de miniatures asynchrones bornées (`StudioPresetCard`) pour les styles enregistrés, avec repli élégant sur les pastilles de couleurs.
  5. **Studio Étape 5 :** Ajout du bouton pro « Retoucher dans l'éditeur » qui exporte la 3D LUT calculée en cache (`.cube`), précharge l'éditeur avec la photo source et cette LUT en base (`editorCustomLutPath`), initialise tous les curseurs à 0 (neutres), et offre un affichage instantané à 0 délai de recalcul avec parité WYSIWYG prouvée ($\Delta \le 1$ niveau 8 bits, validé par `TestStudioToEditorRelayParity` et `GradingRelayTest`).
  6. **Bouton « Terminer » :** Renommage du bouton de réinitialisation en « Terminer » avec dialogue de confirmation en cas de modifications non sauvegardées et vidage des caches Python via `clear_caches()`.

---

## ADR-09 : Ergonomie de l'Éditeur Autonome & Single Global Reset avec Undo

- **Contexte :** L'éditeur autonome comportait des boutons de réinitialisation multiples et ambigus (bouton "Reset" dans le bandeau Auto-Edit en haut, bouton dans la barre basse), une zone d'aperçu à hauteur fixe (260 dp) qui écrasait les curseurs sur petits écrans, et un risque de superposition avec la barre gestuelle Android.
- **Décision :**
  1. Suppression du bouton de réinitialisation redondant du bandeau Auto-Edit pour unifier l'action sur un bouton unique « Réinitialiser tout ».
  2. Implémentation d'une confirmation fluide par `SnackbarHost` avec action « Annuler » (`viewModel.undoEditor()`) restaurant instantanément l'état précédent.
  3. Zone d'aperçu dynamique à hauteur proportionnelle (~40 % de la hauteur utile de l'écran via `BoxWithConstraints`), allouant 60 % de l'espace aux onglets et curseurs de réglage.
  4. Barre d'outils accessible avec gestion native des insets de navigation gestuelle Android (`navigationBarsPadding()`).
  5. Correction de l'appui maintenu comparateur (`EditorViewport`) avec détection stationnaire de 400 ms sur le conteneur complet et relâchement garanti via `finally { isHoldingCompare = false }`.
  6. Intégration complète de l'import/export SAF de presets `.szlpreset` dans `EditorPresetsTab` avec prévention des ID en doublon.

---

## ADR-10 : Refonte UI/UX Complète, Ergonomie Zero-Scroll & Stabilisation Profil

- **Contexte :**
  1. La page Profil provoquait un crash immédiat au lancement à cause d'une erreur de formatage (`IllegalFormatConversionException`) dans les pastilles de statistiques (`R.string.profile_tab_photos` attendant `%1$d` et recevant `""`).
  2. L'éditeur autonome nécessitait trop de défilement vertical, particulièrement dans les panneaux Courbe de tonalité (`EditorToneCurvePanel`) et Étalonnage des couleurs (`EditorColorGradingPanel`), masquant la photo en cours d'édition.
  3. Dans le Studio, l'Étape 3 manquait de lisibilité avec ses cartes de paramètres empilées verticalement sans hiérarchie claire.
  4. L'affichage de l'extraction de palette prenait trop de place verticalement avec des champs de texte verbeux au détriment des nuanciers visuels.
- **Décision :**
  1. **Stabilisation Profil :** Création de chaînes dédiées non paramétrées `profile_stat_photos` et `profile_stat_presets`, et contrainte des listes `LazyVerticalGrid` / `LazyColumn` via `Box(modifier = Modifier.weight(1f).fillMaxWidth())` éliminant tout crash.
  2. **Ergonomie Zero-Scroll dans l'Éditeur :**
     - *Courbe de tonalité :* Séparation nette en 2 modes dédiés : Mode Courbe à points (`ToneCurveZeroScrollHeight` 120dp) avec spline cubique, 4 zones et presets S-Curve instantanés ; et Mode Zones avec curseurs Hautes lumières et Ombres. Aucun défilement vertical requis.
     - *Color Grading :* 5 onglets thématiques (Ombres, Tons moyens, Hautes lumières, Global, Mélange/Équilibre), roue compacte 100dp (`ColorWheelZeroScrollSize`) et curseur de luminance sans aucun chevauchement ni défilement vertical.
     - *Tiroir rétractable :* Poignée tactile et chevron permettant de replier le tiroir pour laisser la photo occuper 100% de la hauteur de l'écran.
  3. **Studio Étape 3 façon Éditeur :** Remplacement de la longue liste de cartes par une barre de pilules thématiques tactiles (*Intensité & Lumière*, *Couleurs & Peau*, *Méthode Colorimétrique*, *Grille 3D LUT*), offrant un espace propre, catégorisé et cohérent avec l'Éditeur sous l'aperçu WYSIWYG épinglé.
  4. **Nuanciers Cinéma Compacts :** Resserrement des cartes de nuanciers (`CinemaSwatchCompactWidth` 76dp, `CinemaSwatchCompactHeight` 96dp), affichage `#HEX` et `%` sur une seule ligne façon Pantone/ShotDeck, et compactage du formulaire d'enregistrement de presets en une rangée de boutons unifiés.

---

## ADR-11 : Sauvegarde Session & BackHandler, Zéro Défilement Intégral, Toolbox Unifiée et Ergonomie Tactile

- **Contexte :**
  1. Lors de l'appui sur le bouton Retour physique/gestuel Android, l'application se fermait brutalement sans sauvegarder l'édition photo en cours.
  2. Plusieurs sous-menus de l'éditeur (`ColorMixer`, `Detail`, `Effects`, `Calibration`) utilisaient encore des listes verticales à défilement (`verticalScroll`) réduisant la visibilité du canevas photo.
  3. L'histogramme du viewport n'offrait pas de vue agrandie pour analyser précisément les canaux spectraux et le clipping.
  4. La page Profil comportait un en-tête volumineux avec avatar et textes génériques gaspillant l'espace visuel avant la galerie de photos et de styles.
  5. Les accès Presets et Auto-Edit (IA) surchargeaient la barre d'outils inférieure et le viewport de l'éditeur.
  6. Le toucher sur la courbe de tonalités créait des points parasites lors d'un simple toucher ou glissement.
  7. La page Palettes dépassait la hauteur d'écran avec textes encombrants et boutons d'action tronqués par la barre de navigation.
- **Décision :**
  1. **Gestion du BackHandler & Préservation de brouillon :** Intégration de `BackHandler` dans `MainColorGradingScreen` interceptant le retour dans l'éditeur pour afficher un dialogue de confirmation (Enregistrer Preset, Revenir au Studio avec session préservée en mémoire, ou Abandonner/Fermer la photo). Dans le Studio, le retour recule d'une étape (`wizardStep - 1`).
  2. **Zéro-Scroll Intégral sur 100% des panneaux de développement :**
     - `EditorColorMixerPanel` : 8 pastilles de couleur horizontales, pilules de sélection Teinte / Saturation / Luminance et un seul curseur actif.
     - `EditorDetailPanel`, `EditorEffectsPanel`, `EditorCalibrationPanel` : transformation en sélecteurs horizontaux à pilules avec curseur actif unique. Aucun défilement vertical requis sur aucun panneau de l'éditeur.
  3. **Histogramme RVB Agrandissable (HUD) :** Clic tactile sur l'histogramme pour afficher un HUD étendu (280×150dp) avec lignes de repère 25/50/75%, légendes L/R/G/B et analyse spectrale.
  4. **Allègement Profil :** Suppression de la carte d'en-tête redondante, centrant l'affichage immédiatement sur les onglets Photos et Presets.
  5. **Boîte à Outils Haut-Gauche :** Déplacement d'Auto-Edit (IA) et de la Bibliothèque de Presets dans `EditorActionsDropdownToolbar` en haut à gauche, avec ouverture des presets via `ModalBottomSheet` et libération de la barre inférieure pour les 7 outils de retouche purs.
  6. **Courbe de Tonalités Tactile :** Création de point uniquement sur appui long (`onLongPress`), déplacement du point le plus proche sur simple toucher/glissement, et suppression sur double-tap.
  7. **Page Palettes Compacte Single-Page :** Prévisualisation compacte (`ImagePickerCollapsedHeight` 120dp), masquage du bandeau d'introduction lorsque l'image est active, nuanciers avec Hex et % empilés verticalement sans chevauchement, garantissant un affichage 100% visible sans scroll.

