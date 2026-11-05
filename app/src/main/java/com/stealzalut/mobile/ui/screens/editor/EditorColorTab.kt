package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlBipolarSlider

@Composable
fun EditorColorTab(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    tint: Float,
    onTintChange: (Float) -> Unit,
    vibrance: Float,
    onVibranceChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        SzlBipolarSlider(
            title = stringResource(R.string.param_temperature_title),
            value = temperature,
            onValueChange = onTemperatureChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.param_tint_title),
            value = tint,
            onValueChange = onTintChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.edit_param_vibrance),
            value = vibrance,
            onValueChange = onVibranceChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.edit_param_saturation),
            value = saturation,
            onValueChange = onSaturationChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
