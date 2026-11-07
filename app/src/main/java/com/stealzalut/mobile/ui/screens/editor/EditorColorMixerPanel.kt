package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorMixerModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * Lightroom Classic Color Mixer / TSL Develop Panel.
 * Implements the horizontal 8-color spectral picker with zero vertical scroll,
 * featuring interactive H/S/L pills and a single active tactile slider.
 */
@Composable
fun EditorColorMixerPanel(
    colorMixer: ColorMixerModel,
    onUpdateColorMixer: ((ColorMixerModel) -> ColorMixerModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHsl = colorMixer.mode.equals("hsl", ignoreCase = true)
    var selectedColorIndex by remember { mutableIntStateOf(0) } // 0..7
    var activeProperty by remember { mutableIntStateOf(0) } // 0 = Hue, 1 = Sat, 2 = Lum, 3 = Range (point color only)

    val spectralColors = listOf(
        Pair(ColorCardReference.HslRed, R.string.color_red),
        Pair(ColorCardReference.HslOrange, R.string.color_orange),
        Pair(ColorCardReference.HslYellow, R.string.color_yellow),
        Pair(ColorCardReference.HslGreen, R.string.color_green),
        Pair(ColorCardReference.HslAqua, R.string.color_aqua),
        Pair(ColorCardReference.HslBlue, R.string.color_blue),
        Pair(ColorCardReference.HslPurple, R.string.color_purple),
        Pair(ColorCardReference.HslMagenta, R.string.color_magenta)
    )

    fun getCurrentHue(): Float = when (selectedColorIndex) {
        0 -> colorMixer.hueRed
        1 -> colorMixer.hueOrange
        2 -> colorMixer.hueYellow
        3 -> colorMixer.hueGreen
        4 -> colorMixer.hueAqua
        5 -> colorMixer.hueBlue
        6 -> colorMixer.huePurple
        else -> colorMixer.hueMagenta
    }

    fun getCurrentSat(): Float = when (selectedColorIndex) {
        0 -> colorMixer.satRed
        1 -> colorMixer.satOrange
        2 -> colorMixer.satYellow
        3 -> colorMixer.satGreen
        4 -> colorMixer.satAqua
        5 -> colorMixer.satBlue
        6 -> colorMixer.satPurple
        else -> colorMixer.satMagenta
    }

    fun getCurrentLum(): Float = when (selectedColorIndex) {
        0 -> colorMixer.lumRed
        1 -> colorMixer.lumOrange
        2 -> colorMixer.lumYellow
        3 -> colorMixer.lumGreen
        4 -> colorMixer.lumAqua
        5 -> colorMixer.lumBlue
        6 -> colorMixer.lumPurple
        else -> colorMixer.lumMagenta
    }

    fun updateCurrentHue(v: Float) = onUpdateColorMixer { m ->
        when (selectedColorIndex) {
            0 -> m.copy(hueRed = v)
            1 -> m.copy(hueOrange = v)
            2 -> m.copy(hueYellow = v)
            3 -> m.copy(hueGreen = v)
            4 -> m.copy(hueAqua = v)
            5 -> m.copy(hueBlue = v)
            6 -> m.copy(huePurple = v)
            else -> m.copy(hueMagenta = v)
        }
    }

    fun updateCurrentSat(v: Float) = onUpdateColorMixer { m ->
        when (selectedColorIndex) {
            0 -> m.copy(satRed = v)
            1 -> m.copy(satOrange = v)
            2 -> m.copy(satYellow = v)
            3 -> m.copy(satGreen = v)
            4 -> m.copy(satAqua = v)
            5 -> m.copy(satBlue = v)
            6 -> m.copy(satPurple = v)
            else -> m.copy(satMagenta = v)
        }
    }

    fun updateCurrentLum(v: Float) = onUpdateColorMixer { m ->
        when (selectedColorIndex) {
            0 -> m.copy(lumRed = v)
            1 -> m.copy(lumOrange = v)
            2 -> m.copy(lumYellow = v)
            3 -> m.copy(lumGreen = v)
            4 -> m.copy(lumAqua = v)
            5 -> m.copy(lumBlue = v)
            6 -> m.copy(lumPurple = v)
            else -> m.copy(lumMagenta = v)
        }
    }

    fun resetCurrentColor() = onUpdateColorMixer { m ->
        when (selectedColorIndex) {
            0 -> m.copy(hueRed = 0f, satRed = 0f, lumRed = 0f)
            1 -> m.copy(hueOrange = 0f, satOrange = 0f, lumOrange = 0f)
            2 -> m.copy(hueYellow = 0f, satYellow = 0f, lumYellow = 0f)
            3 -> m.copy(hueGreen = 0f, satGreen = 0f, lumGreen = 0f)
            4 -> m.copy(hueAqua = 0f, satAqua = 0f, lumAqua = 0f)
            5 -> m.copy(hueBlue = 0f, satBlue = 0f, lumBlue = 0f)
            6 -> m.copy(huePurple = 0f, satPurple = 0f, lumPurple = 0f)
            else -> m.copy(hueMagenta = 0f, satMagenta = 0f, lumMagenta = 0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingMicro),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 8 Spectral Color Buttons (Horizontal Row) ---
        if (isHsl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.SpacingMicro),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                spectralColors.forEachIndexed { index, (color, nameRes) ->
                    val isSelected = selectedColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(AppDimens.SwatchSize)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else ColorCardReference.WhiteStep.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { selectedColorIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingSmall)
                                    .clip(CircleShape)
                                    .background(ColorCardReference.WhiteStep)
                            )
                        }
                    }
                }
            }

            // --- Property Selector Pills: [ Teinte ] [ Saturation ] [ Luminance ] + Reset ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val properties = listOf(
                        Triple(0, R.string.tab_hue, getCurrentHue()),
                        Triple(1, R.string.tab_sat, getCurrentSat()),
                        Triple(2, R.string.tab_lum, getCurrentLum())
                    )

                    properties.forEach { (propIndex, labelRes, propValue) ->
                        val isSelected = activeProperty == propIndex
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                AppDimens.BorderThin,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            ),
                            modifier = Modifier.clickable { activeProperty = propIndex }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (propValue != 0f) {
                                    Spacer(modifier = Modifier.width(AppDimens.SpacingMicro))
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

                IconButton(
                    onClick = { resetCurrentColor() },
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

            // --- Single Active Slider (Zero Scroll) ---
            when (activeProperty) {
                0 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_hue),
                    value = getCurrentHue(),
                    onValueChange = { updateCurrentHue(it) },
                    min = -100.0f,
                    max = 100.0f
                )
                1 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_sat),
                    value = getCurrentSat(),
                    onValueChange = { updateCurrentSat(it) },
                    min = -100.0f,
                    max = 100.0f
                )
                else -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_lum),
                    value = getCurrentLum(),
                    onValueChange = { updateCurrentLum(it) },
                    min = -100.0f,
                    max = 100.0f
                )
            }
        } else {
            // --- Point Color Mode (Pipette) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.SwatchSize)
                            .clip(CircleShape)
                            .background(Color(colorMixer.pointTargetR, colorMixer.pointTargetG, colorMixer.pointTargetB))
                            .border(AppDimens.BorderMedium, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(
                        text = stringResource(R.string.eyedropper_label),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
                    listOf(
                        Triple(0, R.string.tab_hue, colorMixer.pointHueShift != 0f),
                        Triple(1, R.string.tab_sat, colorMixer.pointSatShift != 0f),
                        Triple(2, R.string.tab_lum, colorMixer.pointLumShift != 0f),
                        Triple(3, R.string.slider_range, false)
                    ).forEach { (idx, labelRes, isModified) ->
                        val isSelected = activeProperty == idx
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                AppDimens.BorderThin,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            ),
                            modifier = Modifier.clickable { activeProperty = idx }
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                            )
                        }
                    }
                }
            }

            when (activeProperty) {
                0 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_hue),
                    value = colorMixer.pointHueShift,
                    onValueChange = { newVal -> onUpdateColorMixer { it.copy(pointHueShift = newVal, pointActive = true) } },
                    min = -100.0f,
                    max = 100.0f
                )
                1 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_sat),
                    value = colorMixer.pointSatShift,
                    onValueChange = { newVal -> onUpdateColorMixer { it.copy(pointSatShift = newVal, pointActive = true) } },
                    min = -100.0f,
                    max = 100.0f
                )
                2 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_lum),
                    value = colorMixer.pointLumShift,
                    onValueChange = { newVal -> onUpdateColorMixer { it.copy(pointLumShift = newVal, pointActive = true) } },
                    min = -100.0f,
                    max = 100.0f
                )
                else -> SzlSlider(
                    title = stringResource(R.string.slider_range),
                    value = colorMixer.pointRange / 100.0f,
                    onValueChange = { newVal -> onUpdateColorMixer { it.copy(pointRange = newVal * 100.0f) } },
                    min = 0.05f,
                    max = 1.0f,
                    step = 0.01f,
                    presets = null
                )
            }
        }
    }
}
