package com.stealzalut.mobile.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable

/**
 * Formal contract defining an interchangeable theme in StealZaLUT Mobile.
 * Adding a new theme requires implementing a single ThemeSpec instance.
 *
 * Encapsulates:
 * - Semantic ColorScheme (Material 3)
 * - Extended SzlColors (Domain photographic roles)
 * - Typography (Bound to interchangeable FontSpec)
 * - Shapes (Corner radii tokens)
 * - Icons (Semantic SzlIconSet)
 */
@Immutable
data class ThemeSpec(
    val id: String,
    @get:StringRes val nameRes: Int,
    val colorScheme: ColorScheme,
    val szlColors: SzlColors,
    val fontSpec: FontSpec = FontSpec(),
    val icons: SzlIconSet = RoundedSzlIconSet,
    val typography: Typography = buildTypography(fontSpec),
    val shapes: Shapes = Shapes,
    val isDark: Boolean = true
)
