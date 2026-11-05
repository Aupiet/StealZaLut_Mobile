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
import com.stealzalut.mobile.ui.components.SzlSlider

@Composable
fun EditorEffectsTab(
    clarity: Float,
    onClarityChange: (Float) -> Unit,
    sharpen: Float,
    onSharpenChange: (Float) -> Unit,
    grain: Float,
    onGrainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        SzlSlider(
            title = stringResource(R.string.param_clarity_title),
            value = clarity,
            onValueChange = onClarityChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_sharpen_title),
            value = sharpen,
            onValueChange = onSharpenChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_grain_title),
            value = grain,
            onValueChange = onGrainChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
