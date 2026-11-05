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
fun EditorLightTab(
    exposure: Float,
    onExposureChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    highlights: Float,
    onHighlightsChange: (Float) -> Unit,
    shadows: Float,
    onShadowsChange: (Float) -> Unit,
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
            title = stringResource(R.string.edit_param_exposure),
            value = exposure,
            onValueChange = onExposureChange,
            min = -2.0f,
            max = 2.0f,
            defaultValue = 0.0f,
            unit = "EV",
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.edit_param_contrast),
            value = contrast,
            onValueChange = onContrastChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.edit_param_highlights),
            value = highlights,
            onValueChange = onHighlightsChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        SzlBipolarSlider(
            title = stringResource(R.string.edit_param_shadows),
            value = shadows,
            onValueChange = onShadowsChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            decimals = 2
        )

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
