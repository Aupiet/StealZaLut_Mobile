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
import com.stealzalut.mobile.data.model.DetailModel
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Detail Develop Panel.
 * Covers Sharpening (Amount, Radius, Detail, Masking)
 * and Noise Reduction (Luminance, Luminance Detail, Color).
 */
@Composable
fun EditorDetailPanel(
    detail: DetailModel,
    onUpdateDetail: ((DetailModel) -> DetailModel) -> Unit,
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
        // --- 1. Sharpening ---
        Text(
            text = stringResource(R.string.group_sharpening),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SzlSlider(
            title = stringResource(R.string.slider_sharpen_amount),
            value = detail.sharpenAmount / 150.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenAmount = newVal * 150.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_sharpen_radius),
            value = (detail.sharpenRadius - 0.5f) / 2.5f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenRadius = 0.5f + newVal * 2.5f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_sharpen_detail),
            value = detail.sharpenDetail / 100.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenDetail = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_sharpen_masking),
            value = detail.sharpenMasking / 100.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenMasking = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        // --- 2. Noise Reduction ---
        Text(
            text = stringResource(R.string.group_noise_reduction),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        SzlSlider(
            title = stringResource(R.string.slider_nr_luminance),
            value = detail.nrLuminance / 100.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(nrLuminance = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_nr_lum_detail),
            value = detail.nrLumDetail / 100.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(nrLumDetail = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.slider_nr_color),
            value = detail.nrColor / 100.0f,
            onValueChange = { newVal -> onUpdateDetail { it.copy(nrColor = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )
    }
}
