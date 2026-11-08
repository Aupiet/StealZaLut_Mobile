# Design System StealZaLUT Mobile

Le Design System de StealZaLUT Mobile est un système de design modulaire, accessible et hautement scalable conçu pour découpler entièrement les écrans métier de l'application de la couche de rendu visuel (couleurs, formes, typographies, icônes).

---

## 1. Architecture en 3 Couches de Tokens

Le système repose sur une hiérarchie stricte en 3 couches. **Les écrans ne consomment jamais de primitives brutes.**

```
┌────────────────────────────────────────────────────────┐
│ 1. PRIMITIVES (Valeurs brutes, non exposées aux vues)  │
│    ColorPrimitives, AppDimens (valeurs de base)        │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ 2. SÉMANTIQUES (Rôles fonctionnels & d'accessibilité)   │
│    MaterialTheme.colorScheme, SzlTheme.colors,         │
│    SzlTheme.icons, AppDimens.MinInteractiveTarget...   │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ 3. COMPOSANTS (Widgets Szl* & Écrans applicatifs)      │
│    SzlSlider, SzlBipolarSlider, SzlIcon, EditorCrop... │
└────────────────────────────────────────────────────────┘
```

### Couche 1 — Primitives
- **Couleurs :** Définies dans `ColorPrimitives.kt` (`AmberPrimary = Color(0xFFFFB300)`, `NoirBackground = Color(0xFF101216)`, `CyanPrimary = Color(0xFF00E5FF)`...).
- **Dimensions :** Échelle de pas (Micro 2dp, ExtraSmall 4dp, Medium 8dp, Spacious 16dp, Massive 32dp).
- **Polices :** Familles brutes encapsulées dans `FontSpec`.

### Couche 2 — Sémantiques
- **Palette sémantique :** `MaterialTheme.colorScheme` (Material 3 standard) et extension dédiée `SzlColors` (`photoDarkroom`, `sliderActive`, `compareBadge`, `clippingShadows`, `clippingHighlights`, `cropOverlay`, `cropHandle`...).
- **Typographie sémantique :** Rôles M3 (`displayLarge`, `titleMedium`, `bodyMedium`, `labelSmall` avec plancher à 12 sp minimum).
- **Icônes sémantiques :** Interface `SzlIconSet` exposant les intentions (`toolBasic`, `toolCrop`, `actionExport`, `actionUndo`...).

### Couche 3 — Composants
- Les composants réutilisables `SzlSlider`, `SzlBipolarSlider`, `SzlIcon`, `SzlEmptyState`, `DoubleViewComparison` consomment exclusivement des tokens sémantiques.

---

## 2. Table de Correspondance 1:1 (Figma ↔ Jetpack Compose)

Chaque variable ou composant défini dans Figma porte **strictement le même nom** dans le code Kotlin :

### Couleurs & Tokens Sémantiques

| Catégorie Figma | Nom Variable Figma | Équivalent Kotlin Compose | Valeur par défaut (Amber Noir) |
|---|---|---|---|
| **Surface** | `surface/background` | `MaterialTheme.colorScheme.background` | `#101216` |
| **Surface** | `surface/main` | `MaterialTheme.colorScheme.surface` | `#171A21` |
| **Surface** | `surface/variant` | `MaterialTheme.colorScheme.surfaceVariant` | `#222631` |
| **Darkroom** | `darkroom/canvas` | `SzlTheme.colors.photoDarkroom` | `#0E0E11` |
| **Brand Accent** | `accent/primary` | `MaterialTheme.colorScheme.primary` | `#FFB300` (Ambre Signature) |
| **Brand Accent** | `accent/onPrimary` | `MaterialTheme.colorScheme.onPrimary` | `#1E1400` |
| **Brand Accent** | `accent/container` | `MaterialTheme.colorScheme.primaryContainer` | `#3D2D00` |
| **Brand Accent** | `accent/onContainer`| `MaterialTheme.colorScheme.onPrimaryContainer`| `#FFDF9E` |
| **Texte** | `text/primary` | `MaterialTheme.colorScheme.onSurface` | `#F0F2F5` |
| **Texte** | `text/secondary` | `MaterialTheme.colorScheme.onSurfaceVariant` | `#A0A7B5` |
| **Bordure** | `outline/default` | `MaterialTheme.colorScheme.outline` | `#383E4D` |
| **Bordure** | `outline/variant` | `MaterialTheme.colorScheme.outlineVariant` | `#2E3440` |
| **Statut** | `status/error` | `MaterialTheme.colorScheme.error` | `#EF4444` |
| **Statut** | `status/success` | `SzlTheme.colors.success` | `#22C55E` |
| **Statut** | `status/compareBadge`| `SzlTheme.colors.compareBadge` | `#B91C1C` |
| **Contrôles** | `slider/trackActive` | `SzlTheme.colors.sliderActive` | `#FFB300` |
| **Contrôles** | `slider/trackInactive`| `SzlTheme.colors.sliderInactive`| `#262A34` |
| **Contrôles** | `slider/thumb` | `SzlTheme.colors.sliderThumb` | `#FFB300` |
| **Chambre Noire**| `clipping/shadows` | `SzlTheme.colors.clippingShadows` | `#2563EB` (Bleu cobalt) |
| **Chambre Noire**| `clipping/highlights`| `SzlTheme.colors.clippingHighlights`| `#DC2626` (Rouge vif) |
| **Recadrage** | `crop/overlay` | `SzlTheme.colors.cropOverlay` | `#B3000000` (Opacité 70%) |
| **Recadrage** | `crop/handle` | `SzlTheme.colors.cropHandle` | `#FFB300` |
| **Recadrage** | `crop/grid` | `SzlTheme.colors.cropGrid` | `#66FFFFFF` (Tiers 40%) |

### Dimensions & Espacements

| Nom Token Figma | Équivalent Kotlin | Valeur dp | Usage recommandé |
|---|---|:---:|---|
| `spacing/micro` | `AppDimens.SpacingMicro` | 2 dp | Micro-séparateurs |
| `spacing/extraSmall` | `AppDimens.SpacingExtraSmall` | 4 dp | Écarts internes d'icônes |
| `spacing/small` | `AppDimens.SpacingSmall` | 6 dp | Paddings compacts |
| `spacing/medium` | `AppDimens.SpacingMedium` | 8 dp | Écart standard inter-éléments |
| `spacing/normal` | `AppDimens.SpacingNormal` | 10 dp | Espacement de cartes |
| `spacing/spacious` | `AppDimens.SpacingSpacious` | 16 dp | Padding latéral d'écran |
| `spacing/huge` | `AppDimens.SpacingHuge` | 24 dp | Séparation de grands blocs |
| `touch/interactiveTarget` | `AppDimens.MinInteractiveTarget` | **48 dp** | Cible tactile minimale WCAG AA |
| `touch/buttonStandard` | `AppDimens.ButtonHeightStandard` | 52 dp | Hauteur boutons d'action |
| `radius/card` | `AppDimens.CornerRadiusCard` | 12 dp | Cartes et conteneurs |
| `radius/sheet` | `AppDimens.CornerRadiusEditorSheet`| 24 dp | Tiroir rétractable éditeur |
| `radius/pill` | `AppDimens.CornerRadiusPill` | 999 dp | Pilules et indicateurs |

### Typographie (Polices Embarquées & Rôles)

Trois familles typographiques libres (licences OFL) sont embarquées directement dans `app/src/main/res/font/` et gouvernées par `FontSpec` :
- **Titres et En-têtes :** `Playfair Display` (`playfair_display_bold.ttf`, `playfair_display_regular.ttf`) pour un rendu éditorial et photographique raffiné.
- **Interface & Corps de texte :** `Plus Jakarta Sans` (`plus_jakarta_sans_regular.ttf`, `plus_jakarta_sans_medium.ttf`, `plus_jakarta_sans_bold.ttf`) pour une lisibilité parfaite des contrôles et menus.
- **Valeurs Numériques & EXIF :** `JetBrains Mono` (`jetbrains_mono_regular.ttf`, `jetbrains_mono_bold.ttf`) pour des chiffres tabulaires alignés au pixel près.

| Rôle Typographique Figma | Police | Équivalent Kotlin | Taille (sp) | Graisse | Usage |
|---|---|---|:---:|:---:|---|
| `typography/displayLarge` | Playfair Display | `MaterialTheme.typography.displayLarge` | 32 sp | Bold | Titres immersifs |
| `typography/titleLarge` | Playfair Display | `MaterialTheme.typography.titleLarge` | 20 sp | Bold | Titres de panneaux |
| `typography/titleMedium` | Plus Jakarta Sans | `MaterialTheme.typography.titleMedium` | 16 sp | SemiBold | TopAppBar & Sections |
| `typography/bodyLarge` | Plus Jakarta Sans | `MaterialTheme.typography.bodyLarge` | 16 sp | Normal | Texte de dialogue |
| `typography/bodyMedium` | Plus Jakarta Sans | `MaterialTheme.typography.bodyMedium` | 14 sp | Normal | Texte courant interface |
| `typography/bodySmall` | Plus Jakarta Sans | `MaterialTheme.typography.bodySmall` | 12 sp | Normal | Sous-titres et légendes |
| `typography/labelLarge` | Plus Jakarta Sans | `MaterialTheme.typography.labelLarge` | 14 sp | SemiBold | Libellés de boutons |
| `typography/labelSmall` | JetBrains Mono | `MaterialTheme.typography.labelSmall` | **12 sp** | Normal / Bold | Badges, pourcentages, valeurs tabulaires |
| `typography/bodyMedium` | `MaterialTheme.typography.bodyMedium` | 14 sp | Normal | Texte courant interface |
| `typography/bodySmall` | `MaterialTheme.typography.bodySmall` | 12 sp | Normal | Sous-titres et légendes |
| `typography/labelLarge` | `MaterialTheme.typography.labelLarge` | 14 sp | SemiBold | Libellés de boutons |
| `typography/labelSmall` | `MaterialTheme.typography.labelSmall` | **12 sp** | Normal | Badges, pastilles de curseurs |

---

## 3. Guide de Modularité : Procédures d'Évolution

### 3.1 Comment ajouter un nouveau Thème visuel (1 seul objet)
Toute la déclaration d'un thème réside dans un unique objet `ThemeSpec`. Pour ajouter un thème (ex. `EmeraldVintage`) :

1. Ouvrir [`Themes.kt`](file:///c:/Users/audri/AndroidStudioProjects/StealZaLut_Mobile/app/src/main/java/com/stealzalut/mobile/ui/theme/Themes.kt).
2. Déclarer la constante d'ID et l'objet `ThemeSpec` :
```kotlin
const val THEME_EMERALD_VINTAGE = "emerald_vintage"

val EmeraldVintage = ThemeSpec(
    id = THEME_EMERALD_VINTAGE,
    nameRes = R.string.theme_emerald_vintage,
    isDark = true,
    colorScheme = darkColorScheme(
        primary = Color(0xFF10B981),
        background = Color(0xFF062018),
        surface = Color(0xFF0B2D23),
        ...
    ),
    szlColors = SzlColors(
        sliderActive = Color(0xFF10B981),
        photoDarkroom = Color(0xFF04140F),
        ...
    ),
    shapes = Shapes,
    icons = RoundedSzlIconSet,
    fontSpec = FontSpec()
)
```
3. Ajouter `EmeraldVintage` à la liste `AppThemes.allThemes`.
4. **Zéro modification requise dans les écrans !** Le thème est instantanément sélectionnable et appliqué.

### 3.2 Comment changer la Typographie de l'application
Le système typographique est gouverné par l'abstraction `FontSpec`.
Pour remplacer la police par défaut par une police sur-mesure (ex. `Plus Jakarta Sans` ou `Outfit`) :

1. Déposer les fichiers `.ttf` ou `.otf` dans `app/src/main/res/font/`.
2. Définir le `FontFamily` :
```kotlin
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)
```
3. Assigner la police dans le `ThemeSpec` :
```kotlin
fontSpec = FontSpec(
    titleFont = OutfitFontFamily,
    bodyFont = OutfitFontFamily,
    monoFont = FontFamily.Monospace
)
```
4. Tous les écrans adoptent immédiatement la nouvelle police sans retouche.

### 3.3 Comment changer le Jeu d'icônes
Le système d'icônes repose sur l'interface `SzlIconSet`. Pour basculer sur un jeu d'icônes alternatif (ex. `OutlinedSzlIconSet`) :

1. Dans le `ThemeSpec` concerné, définir :
```kotlin
icons = OutlinedSzlIconSet
```
2. Le `StealZaLut_MobileTheme` injecte automatiquement le jeu d'icônes via `LocalSzlIconSet provides themeSpec.icons`.
3. Tous les appels `SzlTheme.icons.<nom>` ou `SzlIcon(...)` restituent instantanément le nouveau style visuel.

---

## 4. Conformité Accessibilité (WCAG 2.1 AA)

- **Cibles tactiles :** 100 % des boutons, pastilles et curseurs respectent `AppDimens.MinInteractiveTarget = 48.dp` (exigence WCAG 2.5.5).
- **Contraste chromatique :** Ratio de contraste vérifié par `ThemeContrastTest` (≥ 4.5:1 sur texte normal, ≥ 3.0:1 sur boutons et titres).
- **Taille de texte :** Plancher typographique fixé à **12 sp** (`labelSmall`), garantissant la robustesse à l'agrandissement système 200%.
- **Zero-literal :** Zéro couleur brute, zéro dimension en dur, zéro texte non internationalisé (garanti par `DesignSystemGuardTest`).
