package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.szlColors
import kotlin.math.abs

/**
 * Basic Adjustments Develop Panel matching Figma [APRÈS] Éditeur / Outil 1 Basic.
 * Features:
 * - 4 Parameter Groups (Lumière, Tons, Couleur, Texture) with indicator pills.
 * - Maximum 4 sliders visible simultaneously (no more 1-slider micro drawer).
 * - Fine 4dp full track, amber active portion, round thumb.
 * - Double-tap reset helper caption.
 * - Modified status dot on modified groups.
 */
@Composable
fun EditorBasicPanel(
    basic: BasicAdjustmentsModel,
    onUpdateBasic: ((BasicAdjustmentsModel) -> BasicAdjustmentsModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }

    val hasLightModified = abs(basic.exposure) > 0.001f || abs(basic.contrast) > 0.001f ||
            abs(basic.highlights) > 0.001f || abs(basic.shadows) > 0.001f
    val hasToneModified = abs(basic.whites) > 0.001f || abs(basic.blacks) > 0.001f || abs(basic.clarity) > 0.001f
    val hasColorModified = abs(basic.temp) > 0.001f || abs(basic.tint) > 0.001f ||
            abs(basic.vibrance) > 0.001f || abs(basic.saturation) > 0.001f
    val hasTextureModified = abs(basic.texture) > 0.001f || abs(basic.dehaze) > 0.001f

    val groups = listOf(
        Pair(R.string.group_light, hasLightModified),
        Pair(R.string.group_tone, hasToneModified),
        Pair(R.string.group_color, hasColorModified),
        Pair(R.string.group_texture, hasTextureModified)
    )

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
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Group Selector Pills ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally)
        ) {
            groups.forEachIndexed { index, (labelRes, isModified) ->
                val isSelected = selectedGroupIndex == index
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = AppDimens.ButtonHeightCompact)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .clickable { selectedGroupIndex = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingStandard, vertical = AppDimens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isModified) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingExtraSmall)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.szlColors.toolActiveDot)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))

        // --- 2. Up to 4 Sliders Visible Simultaneously ---
        when (selectedGroupIndex) {
            0 -> {
                // Light (Lumière) Group: Exposition, Contraste, Hautes lumières, Ombres (Matches Figma exactly!)
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_exposure),
                    value = basic.exposure,
                    onValueChange = { onUpdateBasic { b -> b.copy(exposure = it) } },
                    min = -5.0f,
                    max = 5.0f,
                    defaultValue = 0.0f,
                    unit = "EV",
                    decimals = 2
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_contrast),
                    value = basic.contrast,
                    onValueChange = { onUpdateBasic { b -> b.copy(contrast = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_highlights),
                    value = basic.highlights,
                    onValueChange = { onUpdateBasic { b -> b.copy(highlights = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_shadows),
                    value = basic.shadows,
                    onValueChange = { onUpdateBasic { b -> b.copy(shadows = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
            }
            1 -> {
                // Tone (Tons) Group: Blancs, Noirs, Clarté
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_whites),
                    value = basic.whites,
                    onValueChange = { onUpdateBasic { b -> b.copy(whites = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_blacks),
                    value = basic.blacks,
                    onValueChange = { onUpdateBasic { b -> b.copy(blacks = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_clarity),
                    value = basic.clarity,
                    onValueChange = { onUpdateBasic { b -> b.copy(clarity = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
            }
            2 -> {
                // Color (Couleur) Group: Température, Teinte, Vibrance, Saturation
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_temp),
                    value = basic.temp,
                    onValueChange = { onUpdateBasic { b -> b.copy(temp = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f,
                    trackGradient = tempGradient
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_tint),
                    value = basic.tint,
                    onValueChange = { onUpdateBasic { b -> b.copy(tint = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f,
                    trackGradient = tintGradient
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_vibrance),
                    value = basic.vibrance,
                    onValueChange = { onUpdateBasic { b -> b.copy(vibrance = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_saturation),
                    value = basic.saturation,
                    onValueChange = { onUpdateBasic { b -> b.copy(saturation = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
            }
            else -> {
                // Texture Group: Texture, Correction de voile
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_texture),
                    value = basic.texture,
                    onValueChange = { onUpdateBasic { b -> b.copy(texture = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
                SzlBipolarSlider(
                    title = stringResource(R.string.slider_dehaze),
                    value = basic.dehaze,
                    onValueChange = { onUpdateBasic { b -> b.copy(dehaze = it) } },
                    min = -100.0f,
                    max = 100.0f,
                    defaultValue = 0.0f
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        // --- 3. Double-tap Reset Helper Caption (matching Figma) ---
        Text(
            text = stringResource(R.string.hint_double_tap_reset),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
