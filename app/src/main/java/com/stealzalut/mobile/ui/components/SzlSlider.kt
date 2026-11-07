package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.roundToInt

/**
 * Standard unipolar slider component with tactile [-]/[+] buttons,
 * mechanical ruler tick-marks, and track-aligned presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SzlSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    min: Float = 0.0f,
    max: Float = 1.0f,
    step: Float = 0.05f,
    presets: List<Float>? = listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
) {
    val haptic = LocalHapticFeedback.current
    val percentage = (value * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt()
    val isModified = value > min

    Column(modifier = modifier.fillMaxWidth()) {
        // Header: Title and Interactive Reset Percentage Badge
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
                        onValueChange(min)
                    }
            ) {
                Text(
                    text = "$percentage %",
                    modifier = Modifier.padding(
                        horizontal = AppDimens.SpacingMedium,
                        vertical = AppDimens.SpacingMicro
                    ),
                    style = MaterialTheme.typography.labelMedium,
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

        // Accessible Slider with [-] and [+] buttons (>= 48dp touch targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            val decDesc = stringResource(R.string.intensity_decrease_desc)
            val incDesc = stringResource(R.string.intensity_increase_desc)
            val sliderDesc = "$title: $percentage%"

            // Minus button
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
                    imageVector = Icons.Default.Remove,
                    contentDescription = decDesc,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }

            val trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant
            val trackActiveColor = MaterialTheme.colorScheme.primary
            val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

            // Graduated Slider Track
            Slider(
                value = value,
                onValueChange = { newVal ->
                    if ((newVal == min && value != min) || (newVal == max && value != max)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onValueChange(newVal)
                },
                valueRange = min..max,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = sliderDesc },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                track = { _ ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.EditorSliderTrackHeight)
                    ) {
                        val trackHeight = size.height
                        val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                        // Base track
                        drawRoundRect(
                            color = trackInactiveColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, trackHeight),
                            cornerRadius = cornerRadius
                        )

                        // Mechanical Ruler Ticks (every 10%)
                        val totalTicks = 10
                        for (i in 1 until totalTicks) {
                            val tickX = (i / totalTicks.toFloat()) * size.width
                            drawLine(
                                color = tickColor,
                                start = Offset(tickX, trackHeight * 0.2f),
                                end = Offset(tickX, trackHeight * 0.8f),
                                strokeWidth = 1.5f
                            )
                        }

                        // Active fill
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

            // Plus button
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
                    imageVector = Icons.Default.Add,
                    contentDescription = incDesc,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
        }

        // Quick Preset Chips precisely aligned with the Slider Track
        if (presets != null && presets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.MinTouchTarget),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                presets.forEach { valFloat ->
                    val label = "${(valFloat * AppConstants.INTENSITY_PERCENT_MULTIPLIER).toInt()}%"
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(valFloat)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinInteractiveTarget),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (value == valFloat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (value == valFloat) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
