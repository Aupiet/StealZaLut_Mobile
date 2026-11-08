package com.stealzalut.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Main theme composable for StealZaLUT Mobile.
 *
 * Supports dynamic hot-switching across themes defined in [AppThemes],
 * system light/dark theme tracking (FE-021), Material You dynamic colors (FE-020),
 * interchangeable typography ([FontSpec]) and icons ([SzlIconSet]) without modifying screens.
 */
@Composable
fun StealZaLut_MobileTheme(
    themeId: String? = AppThemes.DEFAULT_THEME_ID,
    iconSetId: String? = "rounded",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val themeSpec = AppThemes.getThemeSpec(
        themeId = themeId,
        isSystemDark = darkTheme,
        dynamicColorScheme = dynamicColorScheme
    )
    val colorScheme = themeSpec.colorScheme
    val szlColors = themeSpec.szlColors
    val iconSet = when (iconSetId) {
        "outlined" -> OutlinedSzlIconSet
        else -> themeSpec.icons
    }
    val fontSpec = themeSpec.fontSpec
    val typography = themeSpec.typography
    val shapes = themeSpec.shapes

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !themeSpec.isDark
                insetsController.isAppearanceLightNavigationBars = !themeSpec.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalSzlColors provides szlColors,
        LocalSzlIcons provides iconSet,
        LocalSzlIconSet provides iconSet,
        LocalFontSpec provides fontSpec
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
