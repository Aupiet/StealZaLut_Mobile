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
import com.stealzalut.mobile.data.model.CalibrationModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * Lightroom Classic Calibration Develop Panel.
 * Zero vertical scroll architecture with horizontal parameter pills:
 * Supports Shadows Tint and Primary Color Chromaticity Matrices (Red, Green, Blue Primary Hue & Saturation).
 */
@Composable
fun EditorCalibrationPanel(
    calibration: CalibrationModel,
    onUpdateCalibration: ((CalibrationModel) -> CalibrationModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillScrollState = rememberScrollState()
    var selectedParamIndex by remember { mutableIntStateOf(0) }

    val params = listOf(
        Triple(R.string.slider_shadows_tint, calibration.shadowTint, ColorCardReference.WhiteStep),
        Triple(R.string.group_red_primary, calibration.redHue, ColorCardReference.HslRed),
        Triple(R.string.tab_sat, calibration.redSat, ColorCardReference.HslRed),
        Triple(R.string.group_green_primary, calibration.greenHue, ColorCardReference.HslGreen),
        Triple(R.string.tab_sat, calibration.greenSat, ColorCardReference.HslGreen),
        Triple(R.string.group_blue_primary, calibration.blueHue, ColorCardReference.HslBlue),
        Triple(R.string.tab_sat, calibration.blueSat, ColorCardReference.HslBlue)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingMicro),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. Horizontal Parameter Selector Pills with Color Accents ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(pillScrollState)
                .padding(horizontal = AppDimens.SpacingExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            params.forEachIndexed { index, (labelRes, rawVal, dotColor) ->
                val isSelected = selectedParamIndex == index
                val isModified = rawVal != 0.0f

                val pillText = when (index) {
                    1 -> "${stringResource(R.string.group_red_primary)} Hue"
                    2 -> "${stringResource(R.string.group_red_primary)} Sat"
                    3 -> "${stringResource(R.string.group_green_primary)} Hue"
                    4 -> "${stringResource(R.string.group_green_primary)} Sat"
                    5 -> "${stringResource(R.string.group_blue_primary)} Hue"
                    6 -> "${stringResource(R.string.group_blue_primary)} Sat"
                    else -> stringResource(labelRes)
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
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingSmall)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                        }
                        Text(
                            text = pillText,
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
            0 -> SzlBipolarSlider(
                title = stringResource(R.string.slider_shadows_tint),
                value = calibration.shadowTint,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(shadowTint = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            1 -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_red_primary)} ${stringResource(R.string.tab_hue)}",
                value = calibration.redHue,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(redHue = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            2 -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_red_primary)} ${stringResource(R.string.tab_sat)}",
                value = calibration.redSat,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(redSat = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            3 -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_green_primary)} ${stringResource(R.string.tab_hue)}",
                value = calibration.greenHue,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(greenHue = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            4 -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_green_primary)} ${stringResource(R.string.tab_sat)}",
                value = calibration.greenSat,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(greenSat = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            5 -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_blue_primary)} ${stringResource(R.string.tab_hue)}",
                value = calibration.blueHue,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(blueHue = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
            else -> SzlBipolarSlider(
                title = "${stringResource(R.string.group_blue_primary)} ${stringResource(R.string.tab_sat)}",
                value = calibration.blueSat,
                onValueChange = { newVal -> onUpdateCalibration { it.copy(blueSat = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
        }
    }
}
