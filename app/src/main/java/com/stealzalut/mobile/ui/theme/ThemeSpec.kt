package com.stealzalut.mobile.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable

/**
 * Formal contract defining an interchangeable theme in StealZaLUT Mobile.
 * Adding a new theme requires implementing a single ThemeSpec instance.
 */
@Immutable
data class ThemeSpec(
    val id: String,
    @get:StringRes val nameRes: Int,
    val colorScheme: ColorScheme,
    val szlColors: SzlColors,
    val isDark: Boolean
)
