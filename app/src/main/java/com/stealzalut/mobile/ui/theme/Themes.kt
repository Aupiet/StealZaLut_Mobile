package com.stealzalut.mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.stealzalut.mobile.R

/**
 * Registry of available visual themes in StealZaLUT Mobile.
 *
 * Scalable architecture:
 * To add a new theme, define an entry here and append it to [AppThemes.allThemes].
 */
object AppThemes {

    const val DEFAULT_THEME_ID = "amber_noir"
    const val THEME_CYAN_STUDIO = "cyan_studio"
    const val THEME_OLED_BLACK = "oled_black"
    const val THEME_DAYLIGHT = "daylight"

    val AmberNoir = ThemeSpec(
        id = DEFAULT_THEME_ID,
        nameRes = R.string.theme_amber_noir,
        isDark = true,
        colorScheme = darkColorScheme(
            primary = ColorPrimitives.AmberPrimary,
            onPrimary = ColorPrimitives.AmberOnPrimary,
            primaryContainer = ColorPrimitives.AmberContainer,
            onPrimaryContainer = ColorPrimitives.AmberOnContainer,
            secondary = ColorPrimitives.AmberSecondary,
            onSecondary = ColorPrimitives.AmberOnSecondary,
            secondaryContainer = ColorPrimitives.AmberSecondaryContainer,
            onSecondaryContainer = ColorPrimitives.AmberOnSecondaryContainer,
            tertiary = ColorPrimitives.AmberTertiary,
            onTertiary = ColorPrimitives.AmberOnPrimary,
            tertiaryContainer = ColorPrimitives.AmberTertiaryContainer,
            onTertiaryContainer = ColorPrimitives.AmberOnTertiaryContainer,
            background = ColorPrimitives.NoirBackground,
            onBackground = ColorPrimitives.NoirTextPrimary,
            surface = ColorPrimitives.NoirSurface,
            onSurface = ColorPrimitives.NoirTextPrimary,
            surfaceVariant = ColorPrimitives.NoirSurfaceVariant,
            onSurfaceVariant = ColorPrimitives.NoirTextSecondary,
            outline = ColorPrimitives.NoirOutline,
            outlineVariant = ColorPrimitives.NoirOutlineVariant,
            error = ColorPrimitives.ErrorRed,
            onError = ColorPrimitives.OnErrorDark,
            errorContainer = ColorPrimitives.ErrorContainerDark,
            onErrorContainer = ColorPrimitives.OnErrorContainerDark
        ),
        szlColors = SzlColors(
            success = ColorPrimitives.SuccessGreen,
            onSuccess = ColorPrimitives.OnSuccessDark,
            successContainer = ColorPrimitives.SuccessContainerDark,
            onSuccessContainer = ColorPrimitives.OnSuccessContainerDark,
            warning = ColorPrimitives.WarningOrange,
            onWarning = ColorPrimitives.OnWarningDark,
            warningContainer = ColorPrimitives.WarningContainerDark,
            onWarningContainer = ColorPrimitives.OnWarningContainerDark,
            info = ColorPrimitives.InfoBlue,
            onInfo = ColorPrimitives.OnInfoDark,
            infoContainer = ColorPrimitives.InfoContainerDark,
            onInfoContainer = ColorPrimitives.OnInfoContainerDark,
            overlayOnImage = Color(0xA6000000),
            onOverlayOnImage = Color.White,
            scrim = Color(0xB3000000),
            sliderActive = ColorPrimitives.AmberPrimary,
            sliderInactive = ColorPrimitives.NoirSurfaceVariant,
            sliderThumb = ColorPrimitives.AmberPrimary,
            sliderBipolarFill = ColorPrimitives.AmberPrimary,
            chipSelected = ColorPrimitives.AmberContainer,
            onChipSelected = ColorPrimitives.AmberOnContainer,
            navIndicator = ColorPrimitives.AmberContainer,
            compareBadge = ColorPrimitives.CompareBadgeDark,
            onCompareBadge = ColorPrimitives.OnCompareBadgeLight
        )
    )

    val CyanStudio = ThemeSpec(
        id = THEME_CYAN_STUDIO,
        nameRes = R.string.theme_cyan_studio,
        isDark = true,
        colorScheme = darkColorScheme(
            primary = ColorPrimitives.CyanPrimary,
            onPrimary = ColorPrimitives.CyanOnPrimary,
            primaryContainer = ColorPrimitives.CyanContainer,
            onPrimaryContainer = ColorPrimitives.CyanOnContainer,
            secondary = ColorPrimitives.CyanSecondary,
            onSecondary = ColorPrimitives.CyanOnSecondary,
            secondaryContainer = ColorPrimitives.CyanSecondaryContainer,
            onSecondaryContainer = ColorPrimitives.CyanOnSecondaryContainer,
            tertiary = ColorPrimitives.CyanTertiary,
            onTertiary = ColorPrimitives.CyanOnPrimary,
            tertiaryContainer = ColorPrimitives.CyanTertiaryContainer,
            onTertiaryContainer = ColorPrimitives.CyanOnTertiaryContainer,
            background = ColorPrimitives.CyanBackground,
            onBackground = ColorPrimitives.CyanTextPrimary,
            surface = ColorPrimitives.CyanSurface,
            onSurface = ColorPrimitives.CyanTextPrimary,
            surfaceVariant = ColorPrimitives.CyanSurfaceVariant,
            onSurfaceVariant = ColorPrimitives.CyanTextSecondary,
            outline = ColorPrimitives.CyanOutline,
            outlineVariant = ColorPrimitives.CyanOutlineVariant,
            error = ColorPrimitives.ErrorRed,
            onError = ColorPrimitives.OnErrorDark,
            errorContainer = ColorPrimitives.ErrorContainerDark,
            onErrorContainer = ColorPrimitives.OnErrorContainerDark
        ),
        szlColors = SzlColors(
            success = ColorPrimitives.SuccessGreen,
            onSuccess = ColorPrimitives.OnSuccessDark,
            successContainer = ColorPrimitives.SuccessContainerDark,
            onSuccessContainer = ColorPrimitives.OnSuccessContainerDark,
            warning = ColorPrimitives.WarningOrange,
            onWarning = ColorPrimitives.OnWarningDark,
            warningContainer = ColorPrimitives.WarningContainerDark,
            onWarningContainer = ColorPrimitives.OnWarningContainerDark,
            info = ColorPrimitives.InfoBlue,
            onInfo = ColorPrimitives.OnInfoDark,
            infoContainer = ColorPrimitives.InfoContainerDark,
            onInfoContainer = ColorPrimitives.OnInfoContainerDark,
            overlayOnImage = Color(0xA6000000),
            onOverlayOnImage = Color.White,
            scrim = Color(0xB3000000),
            sliderActive = ColorPrimitives.CyanPrimary,
            sliderInactive = ColorPrimitives.CyanSurfaceVariant,
            sliderThumb = ColorPrimitives.CyanPrimary,
            sliderBipolarFill = ColorPrimitives.CyanPrimary,
            chipSelected = ColorPrimitives.CyanContainer,
            onChipSelected = ColorPrimitives.CyanOnContainer,
            navIndicator = ColorPrimitives.CyanContainer,
            compareBadge = ColorPrimitives.CompareBadgeDark,
            onCompareBadge = ColorPrimitives.OnCompareBadgeLight
        )
    )

    val OledBlack = ThemeSpec(
        id = THEME_OLED_BLACK,
        nameRes = R.string.theme_oled_black,
        isDark = true,
        colorScheme = darkColorScheme(
            primary = ColorPrimitives.OledPrimary,
            onPrimary = ColorPrimitives.OledOnPrimary,
            primaryContainer = ColorPrimitives.OledContainer,
            onPrimaryContainer = ColorPrimitives.OledOnContainer,
            secondary = ColorPrimitives.OledSecondary,
            onSecondary = ColorPrimitives.OledOnSecondary,
            secondaryContainer = ColorPrimitives.OledSecondaryContainer,
            onSecondaryContainer = ColorPrimitives.OledOnSecondaryContainer,
            tertiary = ColorPrimitives.OledTertiary,
            onTertiary = ColorPrimitives.OledOnPrimary,
            tertiaryContainer = ColorPrimitives.OledTertiaryContainer,
            onTertiaryContainer = ColorPrimitives.OledOnTertiaryContainer,
            background = ColorPrimitives.OledBackground,
            onBackground = ColorPrimitives.OledTextPrimary,
            surface = ColorPrimitives.OledSurface,
            onSurface = ColorPrimitives.OledTextPrimary,
            surfaceVariant = ColorPrimitives.OledSurfaceVariant,
            onSurfaceVariant = ColorPrimitives.OledTextSecondary,
            outline = ColorPrimitives.OledOutline,
            outlineVariant = ColorPrimitives.OledOutlineVariant,
            error = ColorPrimitives.ErrorRed,
            onError = ColorPrimitives.OnErrorDark,
            errorContainer = ColorPrimitives.ErrorContainerDark,
            onErrorContainer = ColorPrimitives.OnErrorContainerDark
        ),
        szlColors = SzlColors(
            success = ColorPrimitives.SuccessGreen,
            onSuccess = ColorPrimitives.OnSuccessDark,
            successContainer = ColorPrimitives.SuccessContainerDark,
            onSuccessContainer = ColorPrimitives.OnSuccessContainerDark,
            warning = ColorPrimitives.WarningOrange,
            onWarning = ColorPrimitives.OnWarningDark,
            warningContainer = ColorPrimitives.WarningContainerDark,
            onWarningContainer = ColorPrimitives.OnWarningContainerDark,
            info = ColorPrimitives.InfoBlue,
            onInfo = ColorPrimitives.OnInfoDark,
            infoContainer = ColorPrimitives.InfoContainerDark,
            onInfoContainer = ColorPrimitives.OnInfoContainerDark,
            overlayOnImage = Color(0xB3000000),
            onOverlayOnImage = Color.White,
            scrim = Color(0xCC000000),
            sliderActive = ColorPrimitives.OledPrimary,
            sliderInactive = ColorPrimitives.OledSurfaceVariant,
            sliderThumb = ColorPrimitives.OledPrimary,
            sliderBipolarFill = ColorPrimitives.OledPrimary,
            chipSelected = ColorPrimitives.OledContainer,
            onChipSelected = ColorPrimitives.OledOnContainer,
            navIndicator = ColorPrimitives.OledContainer,
            compareBadge = ColorPrimitives.CompareBadgeDark,
            onCompareBadge = ColorPrimitives.OnCompareBadgeLight
        )
    )

    val Daylight = ThemeSpec(
        id = THEME_DAYLIGHT,
        nameRes = R.string.theme_daylight,
        isDark = false,
        colorScheme = lightColorScheme(
            primary = ColorPrimitives.LightPrimary,
            onPrimary = ColorPrimitives.LightOnPrimary,
            primaryContainer = ColorPrimitives.LightContainer,
            onPrimaryContainer = ColorPrimitives.LightOnContainer,
            secondary = ColorPrimitives.LightSecondary,
            onSecondary = ColorPrimitives.LightOnSecondary,
            secondaryContainer = ColorPrimitives.LightSecondaryContainer,
            onSecondaryContainer = ColorPrimitives.LightOnSecondaryContainer,
            tertiary = ColorPrimitives.LightTertiary,
            onTertiary = ColorPrimitives.LightOnSecondary,
            tertiaryContainer = ColorPrimitives.LightTertiaryContainer,
            onTertiaryContainer = ColorPrimitives.LightOnTertiaryContainer,
            background = ColorPrimitives.LightBackground,
            onBackground = ColorPrimitives.LightTextPrimary,
            surface = ColorPrimitives.LightSurface,
            onSurface = ColorPrimitives.LightTextPrimary,
            surfaceVariant = ColorPrimitives.LightSurfaceVariant,
            onSurfaceVariant = ColorPrimitives.LightTextSecondary,
            outline = ColorPrimitives.LightOutline,
            outlineVariant = ColorPrimitives.LightOutlineVariant,
            error = ColorPrimitives.ErrorRed,
            onError = Color.White,
            errorContainer = ColorPrimitives.OnErrorContainerDark,
            onErrorContainer = ColorPrimitives.OnErrorDark
        ),
        szlColors = SzlColors(
            success = ColorPrimitives.SuccessGreen,
            onSuccess = Color.White,
            successContainer = Color(0xFFDCFCE7),
            onSuccessContainer = Color(0xFF14532D),
            warning = ColorPrimitives.WarningOrange,
            onWarning = Color.White,
            warningContainer = Color(0xFFFEF3C7),
            onWarningContainer = Color(0xFF78350F),
            info = ColorPrimitives.InfoBlue,
            onInfo = Color.White,
            infoContainer = Color(0xFFDBEAFE),
            onInfoContainer = Color(0xFF1E3A8A),
            overlayOnImage = Color(0x80000000),
            onOverlayOnImage = Color.White,
            scrim = Color(0x99000000),
            sliderActive = ColorPrimitives.LightPrimary,
            sliderInactive = ColorPrimitives.LightSurfaceVariant,
            sliderThumb = ColorPrimitives.LightPrimary,
            sliderBipolarFill = ColorPrimitives.LightPrimary,
            chipSelected = ColorPrimitives.LightContainer,
            onChipSelected = ColorPrimitives.LightOnContainer,
            navIndicator = ColorPrimitives.LightContainer,
            compareBadge = ColorPrimitives.CompareBadgeDark,
            onCompareBadge = Color.White
        )
    )

    val allThemes: List<ThemeSpec> = listOf(AmberNoir, CyanStudio, OledBlack, Daylight)

    fun getThemeSpec(themeId: String?): ThemeSpec {
        return allThemes.find { it.id == themeId } ?: AmberNoir
    }
}
