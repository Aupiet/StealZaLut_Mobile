package com.stealzalut.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

/**
 * Formal contract defining interchangeable typography font families in StealZaLUT Mobile.
 *
 * @param title Font family for titles, display headers, and screen designations (Playfair Display).
 * @param body Font family for standard interface labels, body text, and dialogs (Plus Jakarta Sans).
 * @param mono Font family for numerical slider values, metrics, and histograms (JetBrains Mono).
 */
@Immutable
data class FontSpec(
    val title: FontFamily = PlayfairFontFamily,
    val body: FontFamily = PlusJakartaSansFontFamily,
    val mono: FontFamily = JetBrainsMonoFontFamily,
    val monoFont: FontFamily = mono
)

val LocalFontSpec = staticCompositionLocalOf { FontSpec() }

val SzlTheme.fonts: FontSpec
    @Composable
    @ReadOnlyComposable
    get() = LocalFontSpec.current
