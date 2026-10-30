package com.stealzalut.mobile.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.stealzalut.mobile.R

/**
 * Top-level functional navigation destinations in StealZaLUT Mobile:
 * 1. STUDIO   : Fast color grading (Source + Reference -> 120 FPS GPU Preview -> Export .cube / Image)
 * 2. PALETTES : Color palette extractor and saved presets library
 * 3. SETTINGS : Engine defaults (LUT size, algorithm, language, cache cleanup)
 */
enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector
) {
    STUDIO(
        route = "studio",
        labelRes = R.string.nav_studio,
        titleRes = R.string.app_top_bar_title,
        subtitleRes = R.string.dest_subtitle_studio,
        icon = Icons.Default.Tune
    ),
    PALETTES(
        route = "palettes",
        labelRes = R.string.nav_palettes,
        titleRes = R.string.palettes_header_title,
        subtitleRes = R.string.dest_subtitle_palettes,
        icon = Icons.Default.Palette
    ),
    SETTINGS(
        route = "settings",
        labelRes = R.string.nav_settings,
        titleRes = R.string.settings_header_title,
        subtitleRes = R.string.dest_subtitle_settings,
        icon = Icons.Default.Settings
    )
}
