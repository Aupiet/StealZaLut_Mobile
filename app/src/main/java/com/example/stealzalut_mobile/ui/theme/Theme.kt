package com.example.stealzalut_mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CinematicColorScheme = darkColorScheme(
    primary = AccentAmber,
    onPrimary = Color(0xFF1E1400),
    primaryContainer = AccentAmberContainer,
    onPrimaryContainer = Color(0xFFFFDF9E),

    secondary = AccentCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = AccentCyanContainer,
    onSecondaryContainer = Color(0xFFB8EAFF),

    tertiary = AccentCyan,
    tertiaryContainer = AccentCyanContainer,
    onTertiaryContainer = Color(0xFFB8EAFF),

    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f)
)

@Composable
fun StealZaLut_MobileTheme(
    darkTheme: Boolean = true, // Default to dark cinematic mode for color-grading accuracy
    content: @Composable () -> Unit
) {
    val colorScheme = CinematicColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
