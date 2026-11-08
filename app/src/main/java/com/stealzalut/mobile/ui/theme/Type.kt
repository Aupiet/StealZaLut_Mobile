package com.stealzalut.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stealzalut.mobile.core.AppDimens

/**
 * Creates a fully configured Material 3 Typography instance bound to an interchangeable [FontSpec].
 * Adheres strictly to the 12sp minimum font size rule (FE-023) and maximum 3 font families rule (Method 5).
 */
fun buildTypography(fontSpec: FontSpec = FontSpec()): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Bold,
            fontSize = AppDimens.FontDisplayLarge,
            lineHeight = 40.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = AppDimens.FontDisplayMedium,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Medium,
            fontSize = AppDimens.FontDisplaySmall,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Bold,
            fontSize = AppDimens.FontDisplaySmall,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = AppDimens.FontTitleLarge,
            lineHeight = 26.sp,
            letterSpacing = 0.15.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Medium,
            fontSize = AppDimens.FontTitleMedium,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Bold,
            fontSize = AppDimens.FontTitleLarge,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = AppDimens.FontTitleMedium,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontSpec.title,
            fontWeight = FontWeight.Bold,
            fontSize = AppDimens.FontTitleSmall,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.Normal,
            fontSize = AppDimens.FontBodyLarge,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.Normal,
            fontSize = AppDimens.FontBodyMedium,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.Normal,
            fontSize = AppDimens.FontBodySmall,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.Bold,
            fontSize = AppDimens.FontLabelLarge,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.SemiBold,
            fontSize = AppDimens.FontLabelMedium,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp
        ),
        // Enforce minimum 12sp font size for labelSmall (FE-023)
        labelSmall = TextStyle(
            fontFamily = fontSpec.body,
            fontWeight = FontWeight.Normal,
            fontSize = AppDimens.FontLabelSmall,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

fun createSzlTypography(fontSpec: FontSpec = FontSpec()): Typography = buildTypography(fontSpec)

val Typography = buildTypography(FontSpec())
