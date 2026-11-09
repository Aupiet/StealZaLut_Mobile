package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.stealzalut.mobile.ui.theme.szlColors
import java.util.Locale
import kotlin.math.abs

/**
 * Professional tactile bipolar slider inspired by Lightroom and Halide.
 * Features:
 * - Center-zero bilateral fill with subtle mechanical graduation tick marks (Ruler).
 * - Interactive value badge that resets to zero on tap with tactile haptic feedback.
 * - Double-tap reset on header and slider.
 * - Haptic feedback at center crossing (0.0) and slider boundaries.
 * - Optional custom color track gradient (e.g. Temp, Tint).
 * - Zero redundant reset icons.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SzlBipolarSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    defaultValue: Float = 0f,
    unit: String = "",
    decimals: Int = 0,
    icon: ImageVector? = null,
    trackGradient: Brush? = null
) {
    val haptic = LocalHapticFeedback.current
    val isModified = abs(value - defaultValue) > 0.001f
    val interactionSource = remember { MutableInteractionSource() }

    val formattedValue = when {
        unit.equals("EV", ignoreCase = true) -> String.format(Locale.US, "%+.2f EV", value)
        decimals > 0 -> String.format(Locale.US, "%+." + decimals + "f" + unit, value)
        value > 0f -> String.format(Locale.US, "+%.0f%s", value, unit)
        value < 0f -> String.format(Locale.US, "%.0f%s", value, unit)
        else -> String.format(Locale.US, "0%s", unit)
    }

    val resetLabel = stringResource(R.string.btn_tap_to_reset)
    val sliderDesc = "$title: $formattedValue"

    Column(modifier = modifier.fillMaxWidth()) {
        // --- Header: Title + Single Interactive Value Reset Badge ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onDoubleClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onValueChange(defaultValue)
                    }
                ),
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
                        modifier = Modifier.size(AppDimens.IconSmall),
                        tint = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isModified) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Single unified tactile value badge (tapping resets to defaultValue)
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                color = if (isModified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    if (isModified) MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = resetLabel,
                        enabled = isModified
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(defaultValue)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                ) {
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),
                        fontWeight = FontWeight.Bold,
                        color = if (isModified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant
        val trackActiveColor = MaterialTheme.colorScheme.primary
        val centerMarkerColor = MaterialTheme.szlColors.sliderThumb
        val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

        // --- Graduated Tactile Slider Track ---
        Slider(
            value = value,
            onValueChange = { newVal ->
                // Tactile feedback when crossing zero
                if ((value < 0f && newVal >= 0f) || (value > 0f && newVal <= 0f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if ((newVal == min && value != min) || (newVal == max && value != max)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onValueChange(newVal)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = min..max,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                .semantics { contentDescription = sliderDesc },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(AppDimens.EditorSliderThumbSize)
                        .shadow(AppDimens.ElevationLow, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = AppDimens.BorderMedium,
                            color = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.SpacingSmall)
                            .clip(CircleShape)
                            .background(
                                if (isModified) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
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

                    // 1. Draw inactive base track (or custom gradient if provided)
                    if (trackGradient != null) {
                        drawRoundRect(
                            brush = trackGradient,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, trackHeight),
                            cornerRadius = cornerRadius
                        )
                    } else {
                        drawRoundRect(
                            color = trackInactiveColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, trackHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 2. Draw mechanical ruler ticks (every 10% along track)
                    val totalTicks = 10
                    for (i in 1 until totalTicks) {
                        val tickFraction = i / totalTicks.toFloat()
                        val tickX = tickFraction * size.width
                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, trackHeight * 0.2f),
                            end = Offset(tickX, trackHeight * 0.8f),
                            strokeWidth = 1.5f
                        )
                    }

                    // 3. Compute Center and Current Value positions
                    val totalRange = max - min
                    val zeroFraction = ((0f - min) / totalRange).coerceIn(0f, 1f)
                    val valueFraction = ((value - min) / totalRange).coerceIn(0f, 1f)

                    val zeroX = zeroFraction * size.width
                    val valueX = valueFraction * size.width

                    // 4. Draw active fill from zero to value
                    val fillLeft = minOf(zeroX, valueX)
                    val fillRight = maxOf(zeroX, valueX)
                    val fillWidth = fillRight - fillLeft

                    if (fillWidth > 0f && trackGradient == null) {
                        drawRoundRect(
                            color = trackActiveColor,
                            topLeft = Offset(fillLeft, 0f),
                            size = Size(fillWidth, trackHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 5. Draw prominent Center Zero Marker
                    drawCircle(
                        color = centerMarkerColor,
                        radius = trackHeight / 1.4f,
                        center = Offset(zeroX, trackHeight / 2f)
                    )
                }
            }
        )
    }
}
