package com.stealzalut.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended semantic color tokens complementing Material 3 ColorScheme.
 * Allows pure semantic coloring across all screens with zero hardcoded primitives.
 */
@Immutable
data class SzlColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val overlayOnImage: Color,
    val onOverlayOnImage: Color,
    val scrim: Color,
    val sliderActive: Color,
    val sliderInactive: Color,
    val sliderThumb: Color,
    val sliderBipolarFill: Color,
    val chipSelected: Color,
    val onChipSelected: Color,
    val navIndicator: Color,
    val compareBadge: Color,
    val onCompareBadge: Color
)

val LocalSzlColors = staticCompositionLocalOf<SzlColors> {
    error("No SzlColors provided. Ensure StealZaLut_MobileTheme is wrapping the composable hierarchy.")
}

/**
 * Convenient accessor for extended theme colors: [SzlTheme.colors]
 */
object SzlTheme {
    val colors: SzlColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSzlColors.current
}

/**
 * Extension on MaterialTheme for semantic access.
 */
val MaterialTheme.szlColors: SzlColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSzlColors.current
