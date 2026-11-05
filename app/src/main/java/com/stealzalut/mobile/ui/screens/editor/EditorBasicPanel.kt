package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider

/**
 * Horizontal, compact Basic Adjustments Develop Panel.
 * Uses a horizontal parameter tab strip and prominent active slider,
 * minimizing vertical screen usage to keep the photo preview as large as possible.
 */
@Composable
fun EditorBasicPanel(
    basic: BasicAdjustmentsModel,
    onUpdateBasic: ((BasicAdjustmentsModel) -> BasicAdjustmentsModel) -> Unit,
    modifier: Modifier = Modifier
) {
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

    fun resetSelectedValue() = updateSelectedValue(0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. Horizontal Parameter Selector Strip ---
        ScrollableTabRow(
            selectedTabIndex = selectedParamIndex,
            edgePadding = AppDimens.SpacingNone,
            modifier = Modifier.fillMaxWidth()
        ) {
            params.forEachIndexed { index, (labelRes, value) ->
                val hasAdjustment = kotlin.math.abs(value) > 0.001f
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

        // --- 2. Active Parameter Slider & Reset Button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (selectedParamIndex == 0) {
                    SzlBipolarSlider(
                        title = stringResource(R.string.slider_exposure),
                        value = basic.exposure,
                        onValueChange = { updateSelectedValue(it) },
                        min = -5.0f,
                        max = 5.0f,
                        defaultValue = 0.0f,
                        unit = "EV",
                        decimals = 2
                    )
                } else {
                    SzlBipolarSlider(
                        title = stringResource(params[selectedParamIndex].first),
                        value = params[selectedParamIndex].second,
                        onValueChange = { updateSelectedValue(it) },
                        min = -100.0f,
                        max = 100.0f,
                        defaultValue = 0.0f
                    )
                }
            }

            IconButton(
                onClick = { resetSelectedValue() },
                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.btn_reset_section),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppDimens.IconSmall)
                )
            }
        }
    }
}
