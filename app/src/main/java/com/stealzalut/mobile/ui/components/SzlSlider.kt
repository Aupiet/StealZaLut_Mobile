package com.stealzalut.mobile.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import kotlin.math.roundToInt

/**
 * Standard unipolar slider component with [-]/[+] buttons and track-aligned presets.
 *
 * Resolves Bug B7: Preset chips (0%, 25%, 50%, 75%, 100%) align precisely with the slider track
 * by factoring in the 48dp IconButton width and spacing.
 */
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
    val percentage = (value * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
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
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$percentage %",
                    modifier = Modifier.padding(
                        horizontal = AppDimens.SpacingMedium,
                        vertical = AppDimens.SpacingMicro
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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

            // Slider Track
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = sliderDesc },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Plus button
            FilledTonalIconButton(
                onClick = {
                    val next = (value + step).coerceAtMost(max)
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

        // Quick Preset Chips precisely aligned with the Slider Track (Bug B7 fix)
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
                        onClick = { onValueChange(valFloat) },
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
