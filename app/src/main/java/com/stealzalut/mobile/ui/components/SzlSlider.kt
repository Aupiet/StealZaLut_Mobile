package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.fonts
import com.stealzalut.mobile.ui.theme.icons
import kotlin.math.roundToInt

/**
 * Standard photographic slider matching Figma 2.0 Design System.
 * Features:
 * - Clean, solid 4dp track with amber active portion and large round thumb.
 * - Value badge formatted with JetBrains Mono (FontSpec.mono) tabular figures.
 * - Double-tap / tap on badge to reset to defaultValue.
 * - Zero intrusive tick marks or clunky buttons by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SzlSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    defaultValue: Float = 0.0f,
    description: String? = null,
    icon: ImageVector? = null,
    min: Float = 0.0f,
    max: Float = 1.0f,
    step: Float = 0.05f,
    presets: List<Float>? = null,
    showIncrementButtons: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val percentage = (value * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt()
    val isModified = value > min

    Column(modifier = modifier.fillMaxWidth()) {
        // Header: Title and Interactive Reset Percentage Badge (JetBrains Mono)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                color = if (isModified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    if (isModified) MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
                    .clickable(
                        role = Role.Button,
                        enabled = isModified
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(defaultValue)
                    }
            ) {
                Text(
                    text = "$percentage %",
                    modifier = Modifier.padding(
                        horizontal = AppDimens.SpacingMedium,
                        vertical = AppDimens.SpacingMicro
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = com.stealzalut.mobile.ui.theme.SzlTheme.fonts.mono),
                    fontWeight = FontWeight.Bold,
                    color = if (isModified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (description != null) {
            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            val decDesc = stringResource(R.string.intensity_decrease_desc)
            val incDesc = stringResource(R.string.intensity_increase_desc)
            val sliderDesc = "$title: $percentage%"

            if (showIncrementButtons) {
                FilledTonalIconButton(
                    onClick = {
                        val next = (value - step).coerceAtLeast(min)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange((next * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt() / AppConstants.INTENSITY_PERCENT_MULTIPLIER)
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = AppDimens.MinTouchTarget, minHeight = AppDimens.MinTouchTarget)
                        .semantics { contentDescription = decDesc },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionRemove,
                        contentDescription = decDesc,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                }
            }

            val trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant
            val trackActiveColor = MaterialTheme.colorScheme.primary

            val thumbSurfaceColor = MaterialTheme.colorScheme.surface
            val thumbPrimaryColor = MaterialTheme.colorScheme.primary

            Slider(
                value = value,
                onValueChange = { newVal ->
                    if ((newVal == min && value != min) || (newVal == max && value != max)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onValueChange(newVal)
                },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = min..max,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                    .semantics { contentDescription = sliderDesc },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.EditorSliderThumbSize)
                            .shadow(AppDimens.ElevationLow, CircleShape)
                            .clip(CircleShape)
                            .background(thumbSurfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AppDimens.EditorSliderThumbSize)
                                .clip(CircleShape)
                                .background(thumbPrimaryColor)
                        )
                    }
                },
                track = { _ ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.EditorSliderTrackHeight)
                    ) {
                        val trackHeight = size.height
                        val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                        // 1. Base full track
                        drawRoundRect(
                            color = trackInactiveColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, trackHeight),
                            cornerRadius = cornerRadius
                        )

                        // 2. Active full amber fill
                        val range = max - min
                        val frac = if (range > 0f) ((value - min) / range).coerceIn(0f, 1f) else 0f
                        val fillWidth = frac * size.width
                        if (fillWidth > 0f) {
                            drawRoundRect(
                                color = trackActiveColor,
                                topLeft = Offset(0f, 0f),
                                size = Size(fillWidth, trackHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            )

            if (showIncrementButtons) {
                FilledTonalIconButton(
                    onClick = {
                        val next = (value + step).coerceAtMost(max)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange((next * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt() / AppConstants.INTENSITY_PERCENT_MULTIPLIER)
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = AppDimens.MinTouchTarget, minHeight = AppDimens.MinTouchTarget)
                        .semantics { contentDescription = incDesc },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionAdd,
                        contentDescription = incDesc,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                }
            }
        }
    }
}
