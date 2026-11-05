package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.LensBlurModel
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Lens Blur Develop Panel.
 * Supports Synthetic Depth Blur, Bokeh Aperture Styles (Circle, Bubble, 5-Blade, 7-Blade, Ring),
 * Bokeh Boost, and Focal Plane Range.
 */
@Composable
fun EditorLensBlurPanel(
    lensBlur: LensBlurModel,
    onUpdateLensBlur: ((LensBlurModel) -> LensBlurModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Activation Switch ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chk_enable_lens_blur),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = lensBlur.enabled,
                onCheckedChange = { checked -> onUpdateLensBlur { it.copy(enabled = checked) } }
            )
        }

        if (lensBlur.enabled) {
            // --- 2. Blur Amount ---
            SzlSlider(
                title = stringResource(R.string.slider_blur_amount),
                value = lensBlur.blurAmount / 100.0f,
                onValueChange = { newVal -> onUpdateLensBlur { it.copy(blurAmount = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )

            // --- 3. Bokeh Style Selector ---
            Text(
                text = stringResource(R.string.label_bokeh_style),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val styles = listOf(
                "circle" to R.string.bokeh_circle,
                "bubble" to R.string.bokeh_bubble,
                "blade5" to R.string.bokeh_blade5,
                "blade7" to R.string.bokeh_blade7,
                "ring" to R.string.bokeh_ring
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                styles.forEach { (styleKey, labelRes) ->
                    FilterChip(
                        selected = lensBlur.bokehStyle.equals(styleKey, ignoreCase = true),
                        onClick = { onUpdateLensBlur { it.copy(bokehStyle = styleKey) } },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            // --- 4. Bokeh Boost ---
            SzlSlider(
                title = stringResource(R.string.slider_bokeh_boost),
                value = lensBlur.bokehBoost / 100.0f,
                onValueChange = { newVal -> onUpdateLensBlur { it.copy(bokehBoost = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )

            // --- 5. Focal Range ---
            SzlSlider(
                title = stringResource(R.string.slider_focal_range),
                value = lensBlur.focalRange / 100.0f,
                onValueChange = { newVal -> onUpdateLensBlur { it.copy(focalRange = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
        }
    }
}
