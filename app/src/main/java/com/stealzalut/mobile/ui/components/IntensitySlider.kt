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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import kotlin.math.roundToInt

/**
 * Accessible real-time parameter slider with 5% increments and WCAG 2.1 AA compliance.
 */
@Composable
fun ParameterSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    min: Float = 0.0f,
    max: Float = 1.0f,
    step: Float = 0.05f,
    presets: List<Float>? = AppConstants.INTENSITY_PRESETS
) {
    val percentage = (value * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt()

    Column(modifier = modifier.fillMaxWidth()) {
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
                    color = MaterialTheme.colorScheme.onSurface
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

        // Accessible Slider with [-] and [+] buttons (48dp touch targets)
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
                    onValueChange(
                        (next * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt() / AppConstants.INTENSITY_PERCENT_MULTIPLIER
                    )
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = AppDimens.MinTouchTarget, minHeight = AppDimens.MinTouchTarget)
                    .semantics {
                        contentDescription = decDesc
                    },
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

            val sliderSteps = ((max - min) / step - 1).roundToInt().coerceAtLeast(0)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                steps = sliderSteps,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = sliderDesc
                        stateDescription = "$percentage%"
                    },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            // Plus button
            FilledTonalIconButton(
                onClick = {
                    val next = (value + step).coerceAtMost(max)
                    onValueChange(
                        (next * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt() / AppConstants.INTENSITY_PERCENT_MULTIPLIER
                    )
                },
                modifier = Modifier
                    .defaultMinSize(minWidth = AppDimens.MinTouchTarget, minHeight = AppDimens.MinTouchTarget)
                    .semantics {
                        contentDescription = incDesc
                    },
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

        // Quick Preset Chips (if provided)
        if (presets != null && presets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                presets.forEach { valFloat ->
                    val label = "${(valFloat * AppConstants.INTENSITY_PERCENT_MULTIPLIER).toInt()}%"
                    TextButton(
                        onClick = { onValueChange(valFloat) },
                        modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinInteractiveTarget),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = AppDimens.SpacingMedium,
                            vertical = AppDimens.SpacingMicro
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if ((value * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt() == (valFloat * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Accessible real-time intensity slider with 60 FPS in-memory alpha blending.
 * Retained for backwards compatibility across existing UI components.
 */
@Composable
fun IntensitySlider(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    ParameterSlider(
        title = stringResource(R.string.intensity_title),
        value = intensity,
        onValueChange = onIntensityChange,
        modifier = modifier,
        icon = MaterialTheme.icons.toolBasic
    )
}
