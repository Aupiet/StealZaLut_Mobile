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
import com.stealzalut.mobile.data.model.DetailModel
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Detail Develop Panel.
 * Zero vertical scroll architecture with horizontal parameter pills:
 * Covers Sharpening (Amount, Radius, Detail, Masking)
 * and Noise Reduction (Luminance, Luminance Detail, Color).
 */
@Composable
fun EditorDetailPanel(
    detail: DetailModel,
    onUpdateDetail: ((DetailModel) -> DetailModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillScrollState = rememberScrollState()
    var selectedParamIndex by remember { mutableIntStateOf(0) }

    val params = listOf(
        Pair(R.string.slider_sharpen_amount, detail.sharpenAmount),
        Pair(R.string.slider_sharpen_radius, detail.sharpenRadius),
        Pair(R.string.slider_sharpen_detail, detail.sharpenDetail),
        Pair(R.string.slider_sharpen_masking, detail.sharpenMasking),
        Pair(R.string.slider_nr_luminance, detail.nrLuminance),
        Pair(R.string.slider_nr_lum_detail, detail.nrLumDetail),
        Pair(R.string.slider_nr_color, detail.nrColor)
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
                    1 -> rawVal != 1.0f
                    2 -> rawVal != 25.0f
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
            0 -> SzlSlider(
                title = stringResource(R.string.slider_sharpen_amount),
                value = detail.sharpenAmount / 150.0f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenAmount = newVal * 150.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            1 -> SzlSlider(
                title = stringResource(R.string.slider_sharpen_radius),
                value = (detail.sharpenRadius - 0.5f) / 2.5f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenRadius = 0.5f + newVal * 2.5f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            2 -> SzlSlider(
                title = stringResource(R.string.slider_sharpen_detail),
                value = detail.sharpenDetail / 100.0f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenDetail = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            3 -> SzlSlider(
                title = stringResource(R.string.slider_sharpen_masking),
                value = detail.sharpenMasking / 100.0f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(sharpenMasking = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            4 -> SzlSlider(
                title = stringResource(R.string.slider_nr_luminance),
                value = detail.nrLuminance / 100.0f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(nrLuminance = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            5 -> SzlSlider(
                title = stringResource(R.string.slider_nr_lum_detail),
                value = detail.nrLumDetail / 100.0f,
                onValueChange = { newVal -> onUpdateDetail { it.copy(nrLumDetail = newVal * 100.0f) } },
                min = 0.0f,
                max = 1.0f,
                step = 0.02f,
                presets = null
            )
            else -> SzlSlider(
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
}
