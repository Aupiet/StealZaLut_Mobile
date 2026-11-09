package com.stealzalut.mobile.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.stealzalut.mobile.ui.screens.guide.FeatureDemoType

/**
 * Color gradients and palettes for the interactive Feature Guide demonstrations.
 */
object GuideDemoColors {

    val DividerWhite = Color(0xFFFFFFFF)
    val ThumbDarkGray = Color(0xFF333333)

    fun getBeforeBrush(demoType: FeatureDemoType): Brush {
        return when (demoType) {
            FeatureDemoType.CROP -> Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF34495E)))
            FeatureDemoType.TONE_CURVE -> Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF707070)))
            FeatureDemoType.COLOR_MIXER -> Brush.linearGradient(listOf(Color(0xFF5D6D7E), Color(0xFF85929E)))
            FeatureDemoType.COLOR_GRADING -> Brush.linearGradient(listOf(Color(0xFF515A5A), Color(0xFF7B7D7D)))
            FeatureDemoType.CALIBRATION -> Brush.linearGradient(listOf(Color(0xFF5B4B49), Color(0xFF7D6B69)))
            FeatureDemoType.DETAIL -> Brush.linearGradient(listOf(Color(0xFF3B3B3B), Color(0xFF525252)))
            FeatureDemoType.LENS_CORRECTIONS -> Brush.linearGradient(listOf(Color(0xFF2E4053), Color(0xFF3E5063)))
            FeatureDemoType.TRANSFORM -> Brush.linearGradient(listOf(Color(0xFF34495E), Color(0xFF415B76)))
            FeatureDemoType.LENS_BLUR -> Brush.linearGradient(listOf(Color(0xFF1B2631), Color(0xFF283747)))
            FeatureDemoType.EFFECTS -> Brush.linearGradient(listOf(Color(0xFF424949), Color(0xFF515A5A)))
            FeatureDemoType.LUT_EXPORT -> Brush.linearGradient(listOf(Color(0xFF566573), Color(0xFF707B7C)))
            FeatureDemoType.STYLE_TRANSFER -> Brush.linearGradient(listOf(Color(0xFF4D5656), Color(0xFF626567)))
        }
    }

    fun getAfterBrush(demoType: FeatureDemoType): Brush {
        return when (demoType) {
            FeatureDemoType.CROP -> Brush.linearGradient(listOf(Color(0xFF16A085), Color(0xFF2ECC71)))
            FeatureDemoType.TONE_CURVE -> Brush.linearGradient(listOf(Color(0xFF111111), Color(0xFFF39C12), Color(0xFFFFFFFF)))
            FeatureDemoType.COLOR_MIXER -> Brush.linearGradient(listOf(Color(0xFFE74C3C), Color(0xFF3498DB)))
            FeatureDemoType.COLOR_GRADING -> Brush.linearGradient(listOf(Color(0xFF0E3D59), Color(0xFFE07A5F), Color(0xFFF4F1DE)))
            FeatureDemoType.CALIBRATION -> Brush.linearGradient(listOf(Color(0xFFC0392B), Color(0xFF27AE60), Color(0xFF2980B9)))
            FeatureDemoType.DETAIL -> Brush.linearGradient(listOf(Color(0xFF1F618D), Color(0xFF5DADE2)))
            FeatureDemoType.LENS_CORRECTIONS -> Brush.linearGradient(listOf(Color(0xFF117864), Color(0xFF48C9B0)))
            FeatureDemoType.TRANSFORM -> Brush.linearGradient(listOf(Color(0xFF1A5276), Color(0xFF2980B9)))
            FeatureDemoType.LENS_BLUR -> Brush.radialGradient(listOf(Color(0xFFE67E22), Color(0xFF1C2833)))
            FeatureDemoType.EFFECTS -> Brush.radialGradient(listOf(Color(0xFFD35400), Color(0xFF0B0C10)))
            FeatureDemoType.LUT_EXPORT -> Brush.linearGradient(listOf(Color(0xFF6C3483), Color(0xFFF1C40F)))
            FeatureDemoType.STYLE_TRANSFER -> Brush.linearGradient(listOf(Color(0xFF78281F), Color(0xFFB9770E), Color(0xFF1E8449)))
        }
    }
}
