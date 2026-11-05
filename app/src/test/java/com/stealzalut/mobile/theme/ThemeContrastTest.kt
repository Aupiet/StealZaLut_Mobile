package com.stealzalut.mobile.theme

import androidx.compose.ui.graphics.Color
import com.stealzalut.mobile.ui.theme.AppThemes
import com.stealzalut.mobile.ui.theme.ThemeSpec
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastTest {

    private fun sRGBToLinear(component: Float): Double {
        return if (component <= 0.04045f) {
            component.toDouble() / 12.92
        } else {
            ((component.toDouble() + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = sRGBToLinear(color.red)
        val g = sRGBToLinear(color.green)
        val b = sRGBToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(color1: Color, color2: Color): Double {
        val l1 = relativeLuminance(color1)
        val l2 = relativeLuminance(color2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun `all registered themes pass WCAG AA contrast for primary surfaces and typography`() {
        AppThemes.allThemes.forEach { theme: ThemeSpec ->
            val cs = theme.colorScheme

            // Text on Background (WCAG AA normal text >= 4.5:1)
            val bgContrast = contrastRatio(cs.onBackground, cs.background)
            assertTrue(
                bgContrast >= 4.5,
                "Theme ${theme.id} failed onBackground/background contrast: $bgContrast < 4.5"
            )

            // Text on Surface (WCAG AA normal text >= 4.5:1)
            val surfaceContrast = contrastRatio(cs.onSurface, cs.surface)
            assertTrue(
                surfaceContrast >= 4.5,
                "Theme ${theme.id} failed onSurface/surface contrast: $surfaceContrast < 4.5"
            )

            // Text on Primary Container (WCAG AA normal text >= 4.5:1)
            val containerContrast = contrastRatio(cs.onPrimaryContainer, cs.primaryContainer)
            assertTrue(
                containerContrast >= 4.5,
                "Theme ${theme.id} failed onPrimaryContainer/primaryContainer contrast: $containerContrast < 4.5"
            )

            // Primary Button Label (WCAG AA large/bold button text >= 3.0:1)
            val primaryBtnContrast = contrastRatio(cs.onPrimary, cs.primary)
            assertTrue(
                primaryBtnContrast >= 3.0,
                "Theme ${theme.id} failed onPrimary/primary contrast: $primaryBtnContrast < 3.0"
            )

            // Extended SzlColors compare badge (High contrast text >= 4.5:1)
            val badgeContrast = contrastRatio(theme.szlColors.onCompareBadge, theme.szlColors.compareBadge)
            assertTrue(
                badgeContrast >= 4.5,
                "Theme ${theme.id} failed compareBadge contrast: $badgeContrast < 4.5"
            )
        }
    }
}
