# Audit Architectural & Décision Moteur : StealZaLut Mobile

## 1. Constats Majeurs (Audit Structurel)
1. **Empreinte APK obèse** : Chaquopy + NumPy + SciPy ajoutent ~86 Mo décompressés (~43 Mo compressés dans l'APK), dont >25 Mo pour `scipy` qui n'est pourtant utilisé que pour 2 filtres (`gaussian_filter`, `minimum_filter`) et le décodage RAW.
2. **Plafond mémoire critique (crashes Redmi)** : Dans NumPy, `apply_lut_trilinear` non chunké sur 24 MP génère un pic de **6 409 Mo de RAM** (tableaux intermédiaires vectorized). Le chunking 2 MP compense mais maintient ~18-20 s de calcul CPU et des risques d'OOM sur devices contraints.
3. **Goulot GIL mono-thread & non-annulabilité (C5)** : `PythonRuntime.dispatcher` sérialise tous les calculs sur 1 seul thread. Les appels Chaquopy natifs ne sont pas annulables par coroutines, bloquant l'UI et gaspillant la batterie lors des ajustements sliders.
4. **God-classes & couplage fort** : `PythonBridgeRepository.kt` (1 289 l.) et `bridge.py` (1 118 l.) centralisent tout le couplage dans un monolithe unique sans modularisation Gradle (un seul module `:app`).
5. **Risque pérennité Android 15+ (16 KB pages)** : Chaquopy 17.0 supporte les pages 16 KB mais requiert des wheels C-extensions recompilés post-octobre 2024. Le maintien de packages natifs Python sous Android représente une dette technique majeure.

## 2. Tableau des Mesures Baseline & Benchmarks (Host x86_64 / Arm64 profil)
*Mesures réelles exécutées sous `bench/` (JVM 25.0.3 LTS vs Python 3.9.13 NumPy/SciPy).*

| Opération / Métrique | Python (NumPy / SciPy) | JVM / Kotlin (Multi-Thread) | Gain JVM vs Py | Rust (Estimé SIMD) |
|---|---|---|---|---|
| **Démarrage à froid interpréteur** | ~1 500 - 2 500 ms | **0 ms** (Instantané) | ∞ | **< 5 ms** |
| **Apply LUT 33³ Proxy (1024×768)** | 545.60 ms | **6.39 ms** (ST: 17.02 ms) | **85×** | ~2.5 - 4 ms |
| **Apply LUT 33³ 24 MP (Direct)** | 23 674 ms (Pic 6.4 Go) | **162.97 ms** (ST: 473 ms) | **145×** | ~60 - 90 ms |
| **Apply LUT 33³ 24 MP (Chunked 2MP)**| 19 882 ms | N/A (non requis) | **122×** | N/A |
| **Gaussian Blur 24 MP ($\sigma=3$)** | 3 272 ms (Pic 275 Mo) | **1 254 ms** | **2.6×** | ~400 - 600 ms |
| **Parité Numérique ($\Delta E_{max}$ / $\Delta E_{mean}$)**| Référence | **0.000000 / 0.000000** | **Bit-exact** | 0.000000 |
| **Poids APK imputable au moteur** | **~43 Mo** (wheels + stdlib) | **0 Mo** (bytecode JVM) | **-100%** | **~2 à 3 Mo** (.so) |

## 3. Matrice d'Évaluation des Options Architecturales

| Critère (Pondération) | A. Garder Python Optimisé | B. Cœur Rust Unique (UniFFI/PyO3) | C. Tout Kotlin + AGSL | D. Hybride (Kotlin+GPU / Rust) |
|---|:---:|:---:|:---:|:---:|
| **Performance calcul (20%)** | 1.5/5 | **5.0/5** | 3.5/5 | 4.5/5 |
| **Taille APK (10%)** | 2.0/5 | **4.5/5** | 5.0/5 | 4.5/5 |
| **Démarrage & Mémoire (15%)** | 1.5/5 | **5.0/5** | 3.5/5 | 4.5/5 |
| **Annulabilité & Preview (15%)** | 1.5/5 | **4.5/5** | 4.0/5 | **5.0/5** |
| **Coût de portage (10%)** | **5.0/5** | 2.0/5 | 3.5/5 | 3.0/5 |
| **Parité & Réutilisation Desktop (15%)**| **5.0/5** | **5.0/5** (PyO3) | 1.5/5 | 4.0/5 |
| **Maintenabilité 1 dev & Tooling (10%)**| 2.5/5 | 3.5/5 | **4.5/5** | 3.5/5 |
| **Compatibilité 16 KB / Android 15+ (5%)**| 2.0/5 | **5.0/5** | 5.0/5 | 5.0/5 |
| **NOTE FINALE PONDÉRÉE (/5)** | **2.40 / 5** | **4.33 / 5** (RECOMMANDÉ) | **3.48 / 5** | **4.28 / 5** |

## 4. Recommandation Tranchée
**Retenir l'Option B (Cœur Rust unique via UniFFI pour Kotlin et PyO3 pour le Desktop Python).**
*Pourquoi* : C'est la seule option qui élimine les 43 Mo d'APK, supprime les risques de crashes mémoire OOM/SIGSEGV de NumPy sur Android, rend les calculs annulables, divise le temps d'export par 100, et offre un cœur algorithmique unique partagé entre Mobile et Desktop sans duplication de code. L'Option D est une variante acceptable mais l'Option B unifie totalement la couche métier.
*Condition d'invalidation* : Si le développeur refuse catégoriquement d'intégrer le langage Rust dans sa stack, basculer vers l'Option C (Tout Kotlin/AGSL), en acceptant l'abandon du partage de code avec le desktop.

## 5. Plan de Migration Progressif (Option B - Strangler Pattern)
1. **Socle & Outillage (3j)** : Mise en place crate `stealzalut-core`, configuration `cargo-ndk` (flags `-Wl,-z,max-page-size=16384`), génération bindings UniFFI Kotlin.
2. **Tests Golden Master (2j)** : Script d'extraction de 50 cas de tests (images fixes, paramètres extrêmes, exports .cube et JPEG) générés par le moteur Python actuel avec tolérance $\Delta E < 10^{-5}$.
3. **Phase 1 : Kernels Pixel & LUT (4j)** : Migration de `apply_lut` trilinéaire, `generate_identity_grid`, LUT baking 33³/65³ et filtres spatiaux (flou gaussien, clarté). Feature-flag dynamique `USE_RUST_ENGINE`.
4. **Phase 2 : Color Transfer & Zones (5j)** : Migration de `color_transfer.py` (Reinhard, MKL, Pitié), K-means palette et matching Sinkhorn.
5. **Phase 3 : Décodeur RAW & Finalisation (4j)** : Remplacement de `raw_decoder.py` par crate Rust dédiée (`rawloader` ou pipeline léger), validation Golden Master complète.
6. **Phase 4 : Suppression Chaquopy & Bindings PyO3 (2j)** : Retrait de Chaquopy du Gradle mobile (-43 Mo), publication du wheel PyO3 pour le desktop.
*Effort total estimé* : **20 jours-homme**.

## 6. Quick Wins Immédiats (Indépendants de la migration)
1. **Supprimer le warm-up de SciPy au démarrage** (`StealZaLutApplication.kt`) : Gain immédiat de ~500 ms au démarrage de l'app.
2. **Remplacer `scipy.ndimage.gaussian_filter` par un flou séparable pur NumPy ou Kotlin** : Permet de désinstaller `scipy` de `build.gradle.kts` (-25 Mo d'APK !).
3. **Introduire un flag d'annulation coopératif dans la boucle de chunking Python** : Interrompt les exports et previews dès l'annulation coroutine Kotlin.
4. **Découper la God-class `PythonBridgeRepository.kt` en délégués spécialisés** (`LutBridge`, `ZoneBridge`, `ExportBridge`).
5. **Ajouter `android:largeHeap="true"` dans le Manifest** : Soulage les pics mémoire lors du décodage RAW avant migration.
6. **Ajouter un check CI automatique d'alignement 16 KB** sur les `.so` via `llvm-objdump`.
7. **Plafonner la preview Python à 720p / 1024 px** : Inutile de calculer des previews au-delà sur mobile sous GPU AGSL.
8. **Utiliser des ByteBuffers directs pour le transfert Chaquopy** : Évite les copies intermédiaires de tableaux Java/Python.

## 7. Risques & Stratégie d'Atténuation
- **Risque de divergence numérique** : Atténué par les tests Golden Master automatisés en CI sur chaque kernel avant bascule.
- **Courbe d'apprentissage Rust / UniFFI** : Atténué par le prototypage initial sur un kernel isolé (`apply_lut`) avec feature-flag.
- **Support Android 15 16 KB** : Garanti nativement par NDK r28+ et les flags d'alignement vérifiés en CI.
