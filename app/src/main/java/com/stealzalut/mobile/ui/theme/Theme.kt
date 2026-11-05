package com.stealzalut.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Main theme composable for StealZaLUT Mobile.
 *
 * Supports dynamic hot-switching across themes defined in [AppThemes]
 * with seamless preservation of compose tree state and zero artifacts.
 */
@Composable
fun StealZaLut_MobileTheme(
    themeId: String? = AppThemes.DEFAULT_THEME_ID,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeSpec = AppThemes.getThemeSpec(themeId)
    val colorScheme = themeSpec.colorScheme
    val szlColors = themeSpec.szlColors

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

    CompositionLocalProvider(LocalSzlColors provides szlColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
