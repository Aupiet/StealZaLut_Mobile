package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.szlColors
import java.util.Locale
import kotlin.math.abs

/**
 * Professional bipolar slider with center-fill, 0-point snapping, and haptic feedback.
 *
 * Resolves Bug B5 (double-tap reset), Bug B6 (center fill, unified formatting, no 20 cluttered ticks),
 * and Bug B7 (proper center marker).
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
    defaultValue: Float = 0f,
    unit: String = "",
    decimals: Int = 0,
    icon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val isModified = abs(value - defaultValue) > 0.001f

    val formattedValue = when {
        unit.equals("EV", ignoreCase = true) -> String.format(Locale.US, "%+.2f EV", value)
        decimals > 0 -> String.format(Locale.US, "%+." + decimals + "f" + unit, value)
        value > 0f -> String.format(Locale.US, "+%.0f%s", value, unit)
        value < 0f -> String.format(Locale.US, "%.0f%s", value, unit)
        else -> String.format(Locale.US, "0%s", unit)
    }

    val resetDesc = stringResource(R.string.editor_btn_reset_all)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header: Title with double-tap reset support & Value badge
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
                    Spacer(modifier = Modifier.size(AppDimens.SpacingSmall))
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = if (isModified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isModified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    )
                }

                if (isModified) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(defaultValue)
                        },
                        modifier = Modifier
                            .size(AppDimens.IconLarge)
                            .semantics { contentDescription = resetDesc }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = resetDesc,
                            modifier = Modifier.size(AppDimens.IconMicro),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        val trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant
        val trackActiveColor = MaterialTheme.colorScheme.primary
        val centerMarkerColor = MaterialTheme.szlColors.sliderThumb

        // Bipolar Canvas-Rendered Slider Track (Fills from center 0 to value)
        Slider(
            value = value,
            onValueChange = { newVal ->
                // Small tactile vibration when crossing zero
                if ((value < 0f && newVal >= 0f) || (value > 0f && newVal <= 0f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onValueChange(newVal)
            },
            valueRange = min..max,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            track = { sliderState ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.SpacingSmall)
                ) {
                    val trackHeight = size.height
                    val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                    // 1. Draw entire inactive base track
                    drawRoundRect(
                        color = trackInactiveColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, trackHeight),
                        cornerRadius = cornerRadius
                    )

                    // 2. Compute Center and Current Value positions
                    val totalRange = max - min
                    val zeroFraction = ((0f - min) / totalRange).coerceIn(0f, 1f)
                    val valueFraction = ((value - min) / totalRange).coerceIn(0f, 1f)

                    val zeroX = zeroFraction * size.width
                    val valueX = valueFraction * size.width

                    // 3. Draw active fill from zero to value
                    val fillLeft = minOf(zeroX, valueX)
                    val fillRight = maxOf(zeroX, valueX)
                    val fillWidth = fillRight - fillLeft

                    if (fillWidth > 0f) {
                        drawRoundRect(
                            color = trackActiveColor,
                            topLeft = Offset(fillLeft, 0f),
                            size = Size(fillWidth, trackHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 4. Draw Center Zero Marker
                    drawCircle(
                        color = centerMarkerColor,
                        radius = trackHeight / 1.5f,
                        center = Offset(zeroX, trackHeight / 2f)
                    )
                }
            }
        )
    }
}
