package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.EffectsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Effects Develop Panel.
 * Zero vertical scroll architecture with horizontal parameter pills:
 * Covers Post-Crop Vignetting (Amount, Midpoint, Roundness, Feather, Highlights)
 * and Photographic Film Grain (Amount, Size, Roughness).
 */
@Composable
fun EditorEffectsPanel(
    effects: EffectsModel,
    onUpdateEffects: ((EffectsModel) -> EffectsModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillScrollState = rememberScrollState()
    var selectedParamIndex by remember { mutableIntStateOf(0) }

    val params = listOf(
        Pair(R.string.slider_vignette, effects.vignetteAmount),
        Pair(R.string.slider_midpoint, effects.vignetteMidpoint),
        Pair(R.string.slider_roundness, effects.vignetteRoundness),
        Pair(R.string.slider_feather, effects.vignetteFeather),
        Pair(R.string.slider_highlights, effects.vignetteHighlights),
        Pair(R.string.slider_grain, effects.grainAmount),
        Pair(R.string.slider_grain_size, effects.grainSize),
        Pair(R.string.slider_grain_roughness, effects.grainRoughness)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingMicro),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. Horizontal Parameter Selector Pills ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(pillScrollState)
                .padding(horizontal = AppDimens.SpacingExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            params.forEachIndexed { index, (labelRes, rawVal) ->
                val isSelected = selectedParamIndex == index
                val isModified = when (index) {
                    1 -> rawVal != 50.0f
                    3 -> rawVal != 50.0f
                    6 -> rawVal != 25.0f
                    7 -> rawVal != 50.0f
                    else -> rawVal != 0.0f
                }

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier.clickable { selectedParamIndex = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMicro),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isModified) {
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingMicro)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Active Tactile Slider (Zero Vertical Scroll) ---
        when (selectedParamIndex) {
            0 -> SzlBipolarSlider(
                title = stringResource(R.string.slider_vignette),
                value = effects.vignetteAmount,
                onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteAmount = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            1 -> SzlSlider(
                title = stringResource(R.string.slider_midpoint),
                value = effects.vignetteMidpoint / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteMidpoint = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            2 -> SzlBipolarSlider(
                title = stringResource(R.string.slider_roundness),
                value = effects.vignetteRoundness,
                onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteRoundness = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            3 -> SzlSlider(
                title = stringResource(R.string.slider_feather),
                value = effects.vignetteFeather / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteFeather = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            4 -> SzlSlider(
                title = stringResource(R.string.slider_highlights),
                value = effects.vignetteHighlights / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteHighlights = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            5 -> SzlSlider(
                title = stringResource(R.string.slider_grain),
                value = effects.grainAmount / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(grainAmount = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            6 -> SzlSlider(
                title = stringResource(R.string.slider_grain_size),
                value = effects.grainSize / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(grainSize = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            else -> SzlSlider(
                title = stringResource(R.string.slider_grain_roughness),
                value = effects.grainRoughness / 100.0f,
                onValueChange = { newVal -> onUpdateEffects { it.copy(grainRoughness = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
        }
    }
}
