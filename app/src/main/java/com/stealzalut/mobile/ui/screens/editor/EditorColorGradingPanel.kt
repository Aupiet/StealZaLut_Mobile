package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Lightroom Classic 3-Way Color Grading Develop Panel with Live Chromatic Visualization.
 * Features sweep-gradient color wheels for Shadows, Midtones, Highlights & Global,
 * with live color-tint swatch preview and luminance/blending controls.
 */
@Composable
fun EditorColorGradingPanel(
    colorGrading: ColorGradingModel,
    onUpdateColorGrading: ((ColorGradingModel) -> ColorGradingModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWheel by remember { mutableIntStateOf(0) } // 0 = Shadows, 1 = Midtones, 2 = Highlights, 3 = Global

    val currentHue = when (selectedWheel) {
        0 -> colorGrading.shadowHue
        1 -> colorGrading.midtoneHue
        2 -> colorGrading.highlightHue
        else -> colorGrading.globalHue
    }
    val currentSat = when (selectedWheel) {
        0 -> colorGrading.shadowSat
        1 -> colorGrading.midtoneSat
        2 -> colorGrading.highlightSat
        else -> colorGrading.globalSat
    }
    val currentLum = when (selectedWheel) {
        0 -> colorGrading.shadowLum
        1 -> colorGrading.midtoneLum
        2 -> colorGrading.highlightLum
        else -> colorGrading.globalLum
    }

    fun hslToColor(hue: Float, satPercent: Float, lumPercent: Float): Color {
        val s = (satPercent / 100f).coerceIn(0f, 1f)
        if (s < 0.01f) return ColorCardReference.GrayMiddle2
        val l = (0.5f + (lumPercent / 100f) * 0.25f).coerceIn(0.1f, 0.9f)
        val c = (1f - abs(2f * l - 1f)) * s
        val hPrime = (hue % 360f) / 60f
        val x = c * (1f - abs((hPrime % 2f) - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            hPrime < 1f -> Triple(c, x, 0f)
            hPrime < 2f -> Triple(x, c, 0f)
            hPrime < 3f -> Triple(0f, c, x)
            hPrime < 4f -> Triple(0f, x, c)
            hPrime < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r + m, g + m, b + m)
    }

    val activeTintColor = hslToColor(currentHue, currentSat, currentLum)

    val wheelSpectrum = listOf(
        ColorCardReference.HslRed,
        ColorCardReference.HslOrange,
        ColorCardReference.HslYellow,
        ColorCardReference.HslGreen,
        ColorCardReference.HslAqua,
        ColorCardReference.HslBlue,
        ColorCardReference.HslPurple,
        ColorCardReference.HslMagenta,
        ColorCardReference.HslRed
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- Wheel Selector Tabs with Live Tint Indicators ---
        ScrollableTabRow(
            selectedTabIndex = selectedWheel,
            edgePadding = AppDimens.SpacingNone,
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabs = listOf(
                Pair(R.string.wheel_shadows, hslToColor(colorGrading.shadowHue, colorGrading.shadowSat, colorGrading.shadowLum)),
                Pair(R.string.wheel_midtones, hslToColor(colorGrading.midtoneHue, colorGrading.midtoneSat, colorGrading.midtoneLum)),
                Pair(R.string.wheel_highlights, hslToColor(colorGrading.highlightHue, colorGrading.highlightSat, colorGrading.highlightLum)),
                Pair(R.string.wheel_global, hslToColor(colorGrading.globalHue, colorGrading.globalSat, colorGrading.globalLum))
            )

            tabs.forEachIndexed { index, (labelRes, tintCol) ->
                Tab(
                    selected = selectedWheel == index,
                    onClick = { selectedWheel = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingSmall)
                                    .clip(CircleShape)
                                    .background(tintCol)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Text(stringResource(labelRes))
                        }
                    }
                )
            }
        }

        // --- Visual Chromatic Color Wheel & Tint Preview ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(AppDimens.ColorWheelSize)
                    .pointerInput(selectedWheel) {
                        fun updateFromOffset(pos: Offset, sizePx: Float) {
                            val center = sizePx / 2f
                            val dx = pos.x - center
                            val dy = pos.y - center
                            val dist = hypot(dx, dy)
                            val radius = center
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            val sat = (dist / radius * 100f).coerceIn(0f, 100f)

                            onUpdateColorGrading { curr ->
                                when (selectedWheel) {
                                    0 -> curr.copy(shadowHue = angle, shadowSat = sat)
                                    1 -> curr.copy(midtoneHue = angle, midtoneSat = sat)
                                    2 -> curr.copy(highlightHue = angle, highlightSat = sat)
                                    else -> curr.copy(globalHue = angle, globalSat = sat)
                                }
                            }
                        }

                        detectTapGestures { offset -> updateFromOffset(offset, size.width.toFloat()) }
                    }
                    .pointerInput(selectedWheel) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = size.width / 2f
                            val dx = change.position.x - center
                            val dy = change.position.y - center
                            val dist = hypot(dx, dy)
                            val radius = center
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            val sat = (dist / radius * 100f).coerceIn(0f, 100f)

                            onUpdateColorGrading { curr ->
                                when (selectedWheel) {
                                    0 -> curr.copy(shadowHue = angle, shadowSat = sat)
                                    1 -> curr.copy(midtoneHue = angle, midtoneSat = sat)
                                    2 -> curr.copy(highlightHue = angle, highlightSat = sat)
                                    else -> curr.copy(globalHue = angle, globalSat = sat)
                                }
                            }
                        }
                    }
            ) {
                val radius = size.width / 2f
                val center = Offset(radius, radius)

                // 1. Sweep gradient: Full 360 chromatic spectrum
                drawCircle(
                    brush = Brush.sweepGradient(colors = wheelSpectrum, center = center),
                    radius = radius,
                    center = center
                )

                // 2. Radial gradient: Center desaturation to white/neutral
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorCardReference.NeutralDarkBackground, Color.Transparent),
                        center = center,
                        radius = radius * 0.95f
                    ),
                    radius = radius,
                    center = center
                )

                // Outer border ring
                drawCircle(
                    color = ColorCardReference.WhiteStep.copy(alpha = 0.4f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Reticle center dot
                drawCircle(
                    color = ColorCardReference.WhiteStep.copy(alpha = 0.6f),
                    radius = 3f,
                    center = center
                )

                // Interactive thumb indicator
                val rad = Math.toRadians(currentHue.toDouble())
                val dist = (currentSat / 100f) * radius
                val thumbX = center.x + (dist * cos(rad)).toFloat()
                val thumbY = center.y + (dist * sin(rad)).toFloat()

                drawCircle(ColorCardReference.NeutralDarkBackground, radius = 9f, center = Offset(thumbX, thumbY))
                drawCircle(activeTintColor, radius = 7f, center = Offset(thumbX, thumbY))
                drawCircle(ColorCardReference.WhiteStep, radius = 7f, center = Offset(thumbX, thumbY), style = Stroke(width = 2f))
            }

            // --- Visual Active Tint Swatch Badge ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.CurtainHandleSize)
                        .clip(CircleShape)
                        .background(activeTintColor)
                        .border(AppDimens.BorderMedium, ColorCardReference.WhiteStep.copy(alpha = 0.5f), CircleShape)
                )
                Text(
                    text = "${currentHue.toInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${currentSat.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Active Wheel Luminance Slider ---
        SzlBipolarSlider(
            title = stringResource(R.string.slider_luminance),
            value = currentLum,
            onValueChange = { newVal ->
                onUpdateColorGrading { curr ->
                    when (selectedWheel) {
                        0 -> curr.copy(shadowLum = newVal)
                        1 -> curr.copy(midtoneLum = newVal)
                        2 -> curr.copy(highlightLum = newVal)
                        else -> curr.copy(globalLum = newVal)
                    }
                }
            },
            min = -100.0f,
            max = 100.0f
        )

        // --- Global Blending and Balance ---
        SzlSlider(
            title = stringResource(R.string.slider_blending),
            value = colorGrading.blending / 100.0f,
            onValueChange = { newVal -> onUpdateColorGrading { it.copy(blending = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.01f,
            presets = null
        )

        SzlBipolarSlider(
            title = stringResource(R.string.slider_balance),
            value = colorGrading.balance,
            onValueChange = { newVal -> onUpdateColorGrading { it.copy(balance = newVal) } },
            min = -100.0f,
            max = 100.0f
        )
    }
}
