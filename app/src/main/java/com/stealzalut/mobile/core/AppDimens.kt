package com.stealzalut.mobile.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions, paddings, corner radii, touch targets, and elevations.
 * Guarantees zero hardcoded dp/sp values across Compose UI screens.
 */
object AppDimens {

    // --- Spacing & Padding ---
    val SpacingNone: Dp = 0.dp
    val SpacingMicro: Dp = 2.dp
    val SpacingExtraSmall: Dp = 4.dp
    val SpacingSmall: Dp = 6.dp
    val SpacingMedium: Dp = 8.dp
    val SpacingNormal: Dp = 10.dp
    val SpacingLarge: Dp = 12.dp
    val SpacingStandard: Dp = 14.dp
    val SpacingSpacious: Dp = 16.dp
    val SpacingExtraLarge: Dp = 20.dp
    val SpacingHuge: Dp = 24.dp
    val SpacingMassive: Dp = 32.dp

    // --- Screen Paddings ---
    val ScreenPaddingHorizontal: Dp = 16.dp
    val ScreenPaddingVertical: Dp = 8.dp
    val BottomBarElevation: Dp = 8.dp

    // --- Corner Radii ---
    val CornerRadiusMicro: Dp = 4.dp
    val CornerRadiusSmall: Dp = 6.dp
    val CornerRadiusMedium: Dp = 8.dp
    val CornerRadiusNormal: Dp = 10.dp
    val CornerRadiusCard: Dp = 12.dp
    val CornerRadiusLarge: Dp = 16.dp
    val CornerRadiusPill: Dp = 999.dp

    // --- Touch Targets (WCAG 2.1 AA Compliance >= 48dp) ---
    val MinTouchTarget: Dp = 48.dp
    val MinInteractiveTarget: Dp = 44.dp
    val ButtonHeightStandard: Dp = 52.dp

    // --- Border & Stroke Widths ---
    val BorderThin: Dp = 1.dp
    val BorderMedium: Dp = 1.5.dp
    val BorderThick: Dp = 2.dp
    val DividerThickness: Dp = 1.dp

    // --- Icon Sizes ---
    val IconMicro: Dp = 14.dp
    val IconSmall: Dp = 18.dp
    val IconNormal: Dp = 20.dp
    val IconMedium: Dp = 24.dp
    val IconLarge: Dp = 32.dp
    val IconExtraLarge: Dp = 48.dp
    val IconJumbo: Dp = 64.dp

    // --- Component Heights & Sizes ---
    val ImagePickerHeight: Dp = 160.dp
    val ImagePickerCollapsedHeight: Dp = 120.dp
    val DoubleViewHeight: Dp = 260.dp
    val MireBoxHeight: Dp = 220.dp
    val MireThumbnailWidth: Dp = 72.dp
    val MireThumbnailHeight: Dp = 48.dp
    val SwatchSize: Dp = 28.dp
    val SwatchHeight: Dp = 44.dp
    val CurtainHandleSize: Dp = 36.dp
    val AvatarSize: Dp = 52.dp
    val ThumbnailCardSize: Dp = 80.dp
    val StepperIndicatorHeight: Dp = 6.dp
    val ComputeLoaderSize: Dp = 120.dp
    val ProgressIndicatorSize: Dp = 20.dp
    val PhotoDetailImageMaxHeight: Dp = 320.dp
}
