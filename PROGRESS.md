# Suivi d'Avancement StealZaLUT Mobile

## Métriques Avant / Après
- Violations garde : 0 (baseline figée)
- Tests Pytest : 143 → 159 passés (dont 16 suites / 44 cas golden master)
- Tests unitaires Kotlin : 120 passés
- APK Release arm64-v8a : 100.6 Mo → 88.7 Mo (-11.9 Mo via QW-1 color-matcher)
- Démarrage à froid Chaquopy : ~1500-2500 ms (0 device adb connecté)

## Fait
- [x] Tag `pre-kotlin-engine` posé sur `review`
- [x] Baseline mesurée (APK 100.6 Mo, 0 violation, 159 tests py, 120 tests kt)
- [x] Étape 0 : Golden master figé (44 cas K1-K5) sur `test/golden-master`
- [x] Tag `pre-golden-master` & merge --no-ff de `test/golden-master` dans `review`
- [x] QW-1 : Suppression dépendance morte `color-matcher` (-11.9 Mo APK) mergé
- [x] QW-2 & QW-3 : Absence warm-up SciPy & preview plafonnée 1024 px validés

## En cours
- [ ] Étape 1 : Architecture `:engine` (Kotlin/JVM pur, plugin `kotlin("jvm")`, `ImageEngine`)

## Reste
- [ ] Gate G0 : Benchmark ART release (apply_lut + flou gaussien sur appareil réel)
- [ ] K1-K5 Portage sur branches `kt/*` vers `integration/kotlin-engine`
- [ ] G3 : Bascule flag ON après parité golden master
- [ ] Préparation `chore/remove-chaquopy` (non mergée)

## Décisions
- ADR-09b (Option C) : Moteur 100 % Kotlin JVM pur par bandes + AGSL GPU
- Baseline lint configurée via `app/lint-baseline.xml` pour garder AGP strict sur nouveaux codes
- Golden master stocké sous `tests/golden/` avec tolérances (float ≤ 1e-4, uint8 ≤ 1, raw ΔE ≤ 0.5)
