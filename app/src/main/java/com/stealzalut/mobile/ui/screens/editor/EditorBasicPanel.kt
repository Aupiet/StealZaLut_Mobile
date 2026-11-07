package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlin.math.abs

/**
 * Compact Basic Adjustments Develop Panel with horizontal parameter strip,
 * specialized color gradients for Temp and Tint, and zero redundant reset buttons.
 */
@Composable
fun EditorBasicPanel(
    basic: BasicAdjustmentsModel,
    onUpdateBasic: ((BasicAdjustmentsModel) -> BasicAdjustmentsModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedParamIndex by remember { mutableIntStateOf(0) }

    val params = listOf(
        Pair(R.string.slider_exposure, basic.exposure),
        Pair(R.string.slider_contrast, basic.contrast),
        Pair(R.string.slider_highlights, basic.highlights),
        Pair(R.string.slider_shadows, basic.shadows),
        Pair(R.string.slider_whites, basic.whites),
        Pair(R.string.slider_blacks, basic.blacks),
        Pair(R.string.slider_temp, basic.temp),
        Pair(R.string.slider_tint, basic.tint),
        Pair(R.string.slider_vibrance, basic.vibrance),
        Pair(R.string.slider_saturation, basic.saturation),
        Pair(R.string.slider_clarity, basic.clarity),
        Pair(R.string.slider_dehaze, basic.dehaze),
        Pair(R.string.slider_texture, basic.texture)
    )

    fun updateSelectedValue(v: Float) {
        onUpdateBasic { b ->
            when (selectedParamIndex) {
                0 -> b.copy(exposure = v)
                1 -> b.copy(contrast = v)
                2 -> b.copy(highlights = v)
                3 -> b.copy(shadows = v)
                4 -> b.copy(whites = v)
                5 -> b.copy(blacks = v)
                6 -> b.copy(temp = v)
                7 -> b.copy(tint = v)
                8 -> b.copy(vibrance = v)
                9 -> b.copy(saturation = v)
                10 -> b.copy(clarity = v)
                11 -> b.copy(dehaze = v)
                else -> b.copy(texture = v)
            }
        }
    }

    val tempGradient = Brush.horizontalGradient(
        listOf(
            ColorCardReference.HistogramBlue.copy(alpha = 0.6f),
            ColorCardReference.NeutralDarkBackground,
            ColorCardReference.HslOrange.copy(alpha = 0.6f)
        )
    )

    val tintGradient = Brush.horizontalGradient(
        listOf(
            ColorCardReference.HistogramGreen.copy(alpha = 0.6f),
            ColorCardReference.NeutralDarkBackground,
            ColorCardReference.HslMagenta.copy(alpha = 0.6f)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Horizontal Parameter Selector Strip ---
        ScrollableTabRow(
            selectedTabIndex = selectedParamIndex,
            edgePadding = AppDimens.SpacingNone,
            modifier = Modifier.fillMaxWidth()
        ) {
            params.forEachIndexed { index, (labelRes, value) ->
                val hasAdjustment = abs(value) > 0.001f
                Tab(
                    selected = selectedParamIndex == index,
                    onClick = { selectedParamIndex = index },
                    text = {
                        Text(
                            text = stringResource(labelRes),
                            fontWeight = if (selectedParamIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasAdjustment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }

        // --- 2. Active Parameter Slider ---
        Box(modifier = Modifier.fillMaxWidth()) {
            when (selectedParamIndex) {
                0 -> SzlBipolarSlider(
                    title = stringResource(R.string.slider_exposure),
                    value = basic.exposure,
                    onValueChange = { updateSelectedValue(it) },
                    min = -5.0f,
                    max = 5.0f,
                    defaultValue = 0.0f,
                    unit = "EV",
                    decimals = 2
                )
                6 -> SzlBipolarSlider(
                    title = stringResource(R.string.slider_temp),
                    value = basic.temp,
                    onValueChange = { updateSelectedValue(it) },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f,
                    trackGradient = tempGradient
                )
                7 -> SzlBipolarSlider(
                    title = stringResource(R.string.slider_tint),
                    value = basic.tint,
                    onValueChange = { updateSelectedValue(it) },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f,
                    trackGradient = tintGradient
                )
                else -> SzlBipolarSlider(
                    title = stringResource(params[selectedParamIndex].first),
                    value = params[selectedParamIndex].second,
                    onValueChange = { updateSelectedValue(it) },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
            }
        }
    }
}
