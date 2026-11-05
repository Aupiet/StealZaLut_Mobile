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
import com.stealzalut.mobile.data.model.CalibrationModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * Lightroom Classic Calibration Develop Panel.
 * Supports Shadows Tint and Primary Color Chromaticity Matrices (Red, Green, Blue Primary Hue & Saturation).
 */
@Composable
fun EditorCalibrationPanel(
    calibration: CalibrationModel,
    onUpdateCalibration: ((CalibrationModel) -> CalibrationModel) -> Unit,
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
        // --- 1. Shadows Tint ---
        SzlBipolarSlider(
            title = stringResource(R.string.slider_shadows_tint),
            value = calibration.shadowTint,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(shadowTint = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        // --- 2. Red Primary ---
        Text(
            text = stringResource(R.string.group_red_primary),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorCardReference.HslRed
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_hue),
            value = calibration.redHue,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(redHue = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_sat),
            value = calibration.redSat,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(redSat = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        // --- 3. Green Primary ---
        Text(
            text = stringResource(R.string.group_green_primary),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorCardReference.HslGreen
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_hue),
            value = calibration.greenHue,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(greenHue = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_sat),
            value = calibration.greenSat,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(greenSat = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        // --- 4. Blue Primary ---
        Text(
            text = stringResource(R.string.group_blue_primary),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorCardReference.HslBlue
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_hue),
            value = calibration.blueHue,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(blueHue = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.tab_sat),
            value = calibration.blueSat,
            onValueChange = { newVal -> onUpdateCalibration { it.copy(blueSat = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )
    }
}
