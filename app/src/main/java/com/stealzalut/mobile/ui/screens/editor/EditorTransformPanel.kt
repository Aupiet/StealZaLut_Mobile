package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.TransformModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Transform / Geometry Develop Panel.
 * Supports Perspective corrections (Vertical, Horizontal, Rotate, Aspect, Scale, Offset X, Offset Y).
 */
@Composable
fun EditorTransformPanel(
    transform: TransformModel,
    onUpdateTransform: ((TransformModel) -> TransformModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        SzlBipolarSlider(
            title = stringResource(R.string.slider_vertical),
            value = transform.vertical,
            onValueChange = { newVal -> onUpdateTransform { it.copy(vertical = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_horizontal),
            value = transform.horizontal,
            onValueChange = { newVal -> onUpdateTransform { it.copy(horizontal = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_rotate),
            value = transform.rotate,
            onValueChange = { newVal -> onUpdateTransform { it.copy(rotate = newVal) } },
            min = -45.0f,
            max = 45.0f,
            defaultValue = 0.0f,
            decimals = 1
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_aspect),
            value = transform.aspect,
            onValueChange = { newVal -> onUpdateTransform { it.copy(aspect = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlSlider(
            title = stringResource(R.string.slider_scale),
            value = (transform.scale - 50.0f) / 100.0f,
            onValueChange = { newVal -> onUpdateTransform { it.copy(scale = 50.0f + newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_offset_x),
            value = transform.offsetX,
            onValueChange = { newVal -> onUpdateTransform { it.copy(offsetX = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_offset_y),
            value = transform.offsetY,
            onValueChange = { newVal -> onUpdateTransform { it.copy(offsetY = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )
    }
}
