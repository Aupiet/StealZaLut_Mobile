package com.stealzalut.mobile.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.GuideDemoColors
import com.stealzalut.mobile.ui.theme.szlColors
import kotlin.math.roundToInt

/**
 * Interactive split before/after tactile slider demonstrating photographic transformations.
 */
@Composable
fun InteractiveBeforeAfterSlider(
    demoType: FeatureDemoType,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.FeatureDemoHeight)
            .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
            .background(MaterialTheme.szlColors.photoDarkroom)
            .border(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                RoundedCornerShape(AppDimens.CornerRadiusLarge)
            )
            .clipToBounds()
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()

        // 1. Full "After" background layer (graded/transformed)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GuideDemoColors.getAfterBrush(demoType))
        ) {
            // "AFTER" label (top right)
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.SpacingSmall)
            ) {
                Text(
                    text = stringResource(R.string.guide_demo_after),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                )
            }
        }

        // 2. Clipped "Before" foreground layer (original/neutral)
        val beforeWidth = (maxWidth * splitFraction)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(beforeWidth)
                .clipToBounds()
                .background(GuideDemoColors.getBeforeBrush(demoType))
        ) {
            // "BEFORE" label (top left)
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppDimens.SpacingSmall)
            ) {
                Text(
                    text = stringResource(R.string.guide_demo_before),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                )
            }
        }

        // 3. Draggable Split Divider Line & Thumb Handle
        val thumbOffsetPx = (totalWidthPx * splitFraction)
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt() - 1, 0) }
                .fillMaxHeight()
                .width(AppDimens.FeatureDemoDividerWidth)
                .background(GuideDemoColors.DividerWhite)
        )

        // Center Grab Thumb Pill (touch target)
        val density = LocalDensity.current
        val thumbRadiusPx = with(density) { (AppDimens.FeatureDemoThumbTouchTarget / 2).roundToPx() }
        val thumbCenterYPx = with(density) { (AppDimens.FeatureDemoHeight / 2 - AppDimens.FeatureDemoThumbTouchTarget / 2).roundToPx() }

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt() - thumbRadiusPx, thumbCenterYPx) }
                .size(AppDimens.FeatureDemoThumbTouchTarget)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newFraction = (splitFraction + dragAmount.x / totalWidthPx).coerceIn(0.05f, 0.95f)
                        splitFraction = newFraction
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = GuideDemoColors.DividerWhite,
                shadowElevation = AppDimens.ElevationMedium,
                modifier = Modifier.size(AppDimens.FeatureDemoThumbSize)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(AppDimens.FeatureDemoDividerWidth, AppDimens.FeatureParamHeight).background(GuideDemoColors.ThumbDarkGray))
                    Spacer(modifier = Modifier.width(AppDimens.FeatureParamSpacing))
                    Box(modifier = Modifier.size(AppDimens.FeatureDemoDividerWidth, AppDimens.FeatureParamHeight).background(GuideDemoColors.ThumbDarkGray))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
