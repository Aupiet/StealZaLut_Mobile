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
    val SpacingHuge: Dp = 24.dp
    val SpacingMassive: Dp = 32.dp

    // --- Screen Paddings ---
    val ScreenPaddingHorizontal: Dp = 16.dp
    val ScreenPaddingVertical: Dp = 8.dp
    val ListBottomPadding: Dp = 96.dp

    // --- Corner Radii ---
    val CornerRadiusTiny: Dp = 2.dp
    val CornerRadiusMire: Dp = 3.dp
    val CornerRadiusMicro: Dp = 4.dp
    val CornerRadiusSmall: Dp = 6.dp
    val CornerRadiusMedium: Dp = 8.dp
    val CornerRadiusNormal: Dp = 10.dp
    val CornerRadiusCard: Dp = 12.dp
    val CornerRadiusLarge: Dp = 16.dp
    val CornerRadiusModal: Dp = 20.dp
    val CornerRadiusDialog: Dp = 24.dp
    val CornerRadiusEditorSheet: Dp = 24.dp
    val CornerRadiusPill: Dp = 999.dp
    val CornerRadiusFull: Dp = CornerRadiusPill

    // --- Touch Targets (WCAG 2.1 AA Compliance >= 48dp) ---
    val MinTouchTarget: Dp = 48.dp
    val MinInteractiveTarget: Dp = 48.dp
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
    val SwatchSize: Dp = 28.dp
    val CurtainHandleSize: Dp = 36.dp
    val AvatarSize: Dp = 52.dp
    val ProgressIndicatorSize: Dp = 20.dp
    val PhotoDetailImageMaxHeight: Dp = 320.dp
    val TopAppBarHeight: Dp = 56.dp
    val CalibrationCardHeight: Dp = 110.dp
    val CalibrationCardLargeHeight: Dp = 140.dp
    val CalibrationRowHeightLarge: Dp = 22.dp
    val CalibrationRowHeightStandard: Dp = 18.dp
    val CalibrationRowHeightMedium: Dp = 16.dp
    val CalibrationRowHeightSmall: Dp = 14.dp
    val CalibrationRowHeightMicro: Dp = 12.dp
    val ViewportButtonSize: Dp = 36.dp
    val ProgressBarHeight: Dp = 16.dp
    val HistogramOverlayHeight: Dp = 56.dp
    val HistogramOverlayWidth: Dp = 110.dp
    val HistogramEnlargedWidth: Dp = 280.dp
    val HistogramEnlargedHeight: Dp = 150.dp
    val EditorToolItemWidth: Dp = 68.dp
    val EditorSliderTrackHeight: Dp = 4.dp
    val EditorSliderThumbSize: Dp = 20.dp
    val CinemaSwatchCompactWidth: Dp = 76.dp
    val CinemaSwatchCompactHeight: Dp = 96.dp
    val CinemaSwatchCompactColorHeight: Dp = 58.dp
    val ToneCurveZeroScrollHeight: Dp = 120.dp
    val CinemaRibbonHeight: Dp = 14.dp
    val DrawerHandleWidth: Dp = 36.dp
    val DrawerHandleHeight: Dp = 4.dp
    val ButtonHeightCompact: Dp = 40.dp
    val FeatureDemoHeight: Dp = 200.dp
    val FeatureDemoDividerWidth: Dp = 2.dp
    val FeatureDemoThumbSize: Dp = 32.dp
    val FeatureDemoThumbTouchTarget: Dp = 44.dp
    val FeatureCategoryBadgeSize: Dp = 44.dp
    val FeatureParamSpacing: Dp = 3.dp
    val FeatureParamHeight: Dp = 12.dp
    val ElevationMedium: Dp = 4.dp

    // --- Elevations ---
    val DialogElevation: Dp = 6.dp
    val ElevationLow: Dp = 2.dp

    // --- Typography Scale Tokens ---
    val FontDisplayLarge: TextUnit = 32.sp
    val FontDisplayMedium: TextUnit = 28.sp
    val FontDisplaySmall: TextUnit = 24.sp
    val FontTitleLarge: TextUnit = 22.sp
    val FontTitleMedium: TextUnit = 18.sp
    val FontTitleSmall: TextUnit = 14.sp
    val FontBodyLarge: TextUnit = 16.sp
    val FontBodyMedium: TextUnit = 14.sp
    val FontBodySmall: TextUnit = 12.sp
    val FontLabelLarge: TextUnit = 14.sp
    val FontLabelMedium: TextUnit = 13.sp
    val FontLabelSmall: TextUnit = 12.sp
}
