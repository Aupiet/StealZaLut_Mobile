package com.stealzalut.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Standard ColorChecker 24-patch target and calibration step wedge references.
 * These are physical reference calibration data (ground truth), NOT theme colors.
 */
object ColorCardReference {
    val NeutralDarkBackground = Color(0xFF202228)
    val WhiteStep = Color(0xFFF0F0F0)
    val GrayMiddle1 = Color(0xFF767676)
    val GrayMiddle2 = Color(0xFF808080)

    val NeutralRamp = listOf(
        Color(0xFF000000),
        Color(0xFF333333),
        Color(0xFF666666),
        Color(0xFF999999),
        Color(0xFFCCCCCC),
        Color(0xFFFFFFFF)
    )

    val NeutralRampFive = listOf(
        Color(0xFF000000),
        Color(0xFF333333),
        Color(0xFF666666),
        Color(0xFF999999),
        Color(0xFFFFFFFF)
    )

    val ColorCheckerPatches = listOf(
        Color(0xFFCD9B84), // Light skin
        Color(0xFF9B6E55), // Dark skin
        Color(0xFF5F3E2D), // Blue sky / Brown
        Color(0xFFE12D2D), // Red
        Color(0xFF2DB937), // Green
        Color(0xFF2D55E1), // Blue
        Color(0xFF2DD7E1), // Cyan
        Color(0xFFD72DD7), // Magenta
        Color(0xFFEBD723), // Yellow
        Color(0xFF4B7DB9), // Moderate red / Blue flower
        Color(0xFF559B55), // Foliage
        Color(0xFFD77D2D)  // Orange
    )

    val PrimaryCardPatches = listOf(
        Color(0xFFCD9B84),
        Color(0xFF9B6E55),
        Color(0xFF5F3E2D),
        Color(0xFFE12D2D),
        Color(0xFF2DB937),
        Color(0xFF2D55E1)
    )

    // Scientific Histogram Channel References
    val HistogramRed = Color(0xCCEF5350)
    val HistogramGreen = Color(0xCC66BB6A)
    val HistogramBlue = Color(0xCC42A5F5)
    val HistogramLuma = Color(0xCCE0E0E0)

    // Lightroom Color Mixer 8-Zone Spectrum References
    val HslRed = Color(0xFFEF5350)
    val HslOrange = Color(0xFFFFA726)
    val HslYellow = Color(0xFFFFEE58)
    val HslGreen = Color(0xFF66BB6A)
    val HslAqua = Color(0xFF26C6DA)
    val HslBlue = Color(0xFF42A5F5)
    val HslPurple = Color(0xFFAB47BC)
    val HslMagenta = Color(0xFFEC407A)
}
