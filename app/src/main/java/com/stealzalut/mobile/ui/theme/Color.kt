package com.stealzalut.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw Primitive Palette Tokens for StealZaLUT Mobile.
 *
 * NOTE: Screens MUST NOT reference these primitive colors directly.
 * Use [androidx.compose.material3.MaterialTheme.colorScheme] or
 * [com.stealzalut.mobile.ui.theme.SzlTheme.colors] for all UI rendering.
 */
internal object ColorPrimitives {

    // --- Amber Noir Primitive Palette (Default Studio) ---
    val AmberPrimary = Color(0xFFFFB300)
    val AmberOnPrimary = Color(0xFF1E1400)
    val AmberContainer = Color(0xFF3D2D00)
    val AmberOnContainer = Color(0xFFFFDF9E)
    val AmberSecondary = Color(0xFFFFC107)
    val AmberOnSecondary = Color(0xFF261900)
    val AmberSecondaryContainer = Color(0xFF4A3400)
    val AmberOnSecondaryContainer = Color(0xFFFFE082)
    val AmberTertiary = Color(0xFFFFB74D)
    val AmberTertiaryContainer = Color(0xFF4E2600)
    val AmberOnTertiaryContainer = Color(0xFFFFD199)

    val NoirBackground = Color(0xFF121417)
    val NoirSurface = Color(0xFF1A1D24)
    val NoirSurfaceVariant = Color(0xFF262A34)
    val NoirOutline = Color(0xFF383E4D)
    val NoirOutlineVariant = Color(0xFF2E3440)
    val NoirTextPrimary = Color(0xFFF0F2F5)
    val NoirTextSecondary = Color(0xFFA0A7B5)

    // --- Cyan Studio Primitive Palette ---
    val CyanPrimary = Color(0xFF00E5FF)
    val CyanOnPrimary = Color(0xFF00363D)
    val CyanContainer = Color(0xFF004D56)
    val CyanOnContainer = Color(0xFFB8EAFF)
    val CyanSecondary = Color(0xFF4DD0E1)
    val CyanOnSecondary = Color(0xFF00363D)
    val CyanSecondaryContainer = Color(0xFF004D56)
    val CyanOnSecondaryContainer = Color(0xFFE0F7FA)
    val CyanTertiary = Color(0xFF80DEEA)
    val CyanTertiaryContainer = Color(0xFF00363D)
    val CyanOnTertiaryContainer = Color(0xFFE0F7FA)

    val CyanBackground = Color(0xFF0B131E)
    val CyanSurface = Color(0xFF131C2A)
    val CyanSurfaceVariant = Color(0xFF1C293D)
    val CyanOutline = Color(0xFF2F4360)
    val CyanOutlineVariant = Color(0xFF223249)
    val CyanTextPrimary = Color(0xFFEDF2F7)
    val CyanTextSecondary = Color(0xFF94A3B8)

    // --- OLED Pure Black Primitive Palette ---
    val OledBackground = Color(0xFF000000)
    val OledSurface = Color(0xFF0D0E11)
    val OledSurfaceVariant = Color(0xFF16181D)
    val OledOutline = Color(0xFF2E323B)
    val OledOutlineVariant = Color(0xFF20232A)
    val OledPrimary = Color(0xFFFFC107)
    val OledOnPrimary = Color(0xFF1F1600)
    val OledContainer = Color(0xFF332300)
    val OledOnContainer = Color(0xFFFFE082)
    val OledSecondary = Color(0xFFFFD54F)
    val OledOnSecondary = Color(0xFF261B00)
    val OledSecondaryContainer = Color(0xFF3D2C00)
    val OledOnSecondaryContainer = Color(0xFFFFECB3)
    val OledTertiary = Color(0xFFFFE082)
    val OledTertiaryContainer = Color(0xFF473300)
    val OledOnTertiaryContainer = Color(0xFFFFF8E1)
    val OledTextPrimary = Color(0xFFFFFFFF)
    val OledTextSecondary = Color(0xFFA6ABB4)

    // --- Daylight Light Primitive Palette ---
    val LightBackground = Color(0xFFF8F9FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE9ECEF)
    val LightOutline = Color(0xFFCED4DA)
    val LightOutlineVariant = Color(0xFFDEE2E6)
    val LightPrimary = Color(0xFFD97706)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightContainer = Color(0xFFFEF3C7)
    val LightOnContainer = Color(0xFF78350F)
    val LightSecondary = Color(0xFFB45309)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFFDE68A)
    val LightOnSecondaryContainer = Color(0xFF78350F)
    val LightTertiary = Color(0xFF92400E)
    val LightTertiaryContainer = Color(0xFFFDF0CD)
    val LightOnTertiaryContainer = Color(0xFF451A03)
    val LightTextPrimary = Color(0xFF1F2937)
    val LightTextSecondary = Color(0xFF6B7280)

    // --- Universal Status / Feedback Primitives ---
    val SuccessGreen = Color(0xFF22C55E)
    val OnSuccessDark = Color(0xFF052E16)
    val SuccessContainerDark = Color(0xFF14532D)
    val OnSuccessContainerDark = Color(0xFFBBF7D0)

    val WarningOrange = Color(0xFFF59E0B)
    val OnWarningDark = Color(0xFF451A03)
    val WarningContainerDark = Color(0xFF78350F)
    val OnWarningContainerDark = Color(0xFFFDE68A)

    val InfoBlue = Color(0xFF3B82F6)
    val OnInfoDark = Color(0xFF172554)
    val InfoContainerDark = Color(0xFF1E3A8A)
    val OnInfoContainerDark = Color(0xFFBFDBFE)

    val ErrorRed = Color(0xFFEF4444)
    val OnErrorDark = Color(0xFF450A0A)
    val ErrorContainerDark = Color(0xFF7F1D1D)
    val OnErrorContainerDark = Color(0xFFFECACA)

    val CompareBadgeDark = Color(0xFFB91C1C)
    val OnCompareBadgeLight = Color(0xFFFFFFFF)

    // --- Darkroom & Curve Inspection Primitives ---
    val DarkroomBlack = Color(0xFF0E0E11)
    val DarkroomBlackLight = Color(0xFF141418)
    val ToneCurveRed = Color(0xFFFF5252)
    val ToneCurveGreen = Color(0xFF69F0AE)
    val ToneCurveBlue = Color(0xFF448AFF)
    val ToneCurveRgb = Color(0xFFF0F2F5)
}
