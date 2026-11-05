package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.EffectsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Effects Develop Panel.
 * Covers Post-Crop Vignetting (Amount, Midpoint, Roundness, Feather, Highlights)
 * and Photographic Film Grain (Amount, Size, Roughness).
 */
@Composable
fun EditorEffectsPanel(
    effects: EffectsModel,
    onUpdateEffects: ((EffectsModel) -> EffectsModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Post-Crop Vignette ---
        Text(
            text = stringResource(R.string.group_post_crop_vignette),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_sharpen_amount),
            value = effects.vignetteAmount,
            onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteAmount = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlSlider(
            title = stringResource(R.string.slider_midpoint),
            value = effects.vignetteMidpoint / 100.0f,
            onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteMidpoint = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_roundness),
            value = effects.vignetteRoundness,
            onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteRoundness = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlSlider(
            title = stringResource(R.string.slider_feather),
            value = effects.vignetteFeather / 100.0f,
            onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteFeather = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_highlights),
            value = effects.vignetteHighlights / 100.0f,
            onValueChange = { newVal -> onUpdateEffects { it.copy(vignetteHighlights = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        // --- 2. Film Grain ---
        Text(
            text = stringResource(R.string.group_grain),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SzlSlider(
            title = stringResource(R.string.slider_sharpen_amount),
            value = effects.grainAmount / 100.0f,
            onValueChange = { newVal -> onUpdateEffects { it.copy(grainAmount = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_grain_size),
            value = effects.grainSize / 100.0f,
            onValueChange = { newVal -> onUpdateEffects { it.copy(grainSize = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
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
