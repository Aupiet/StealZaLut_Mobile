code = '''package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.LocalFontSpec
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3-Way Color Grading Panel (Figma Node 12:1031).
 * Strictly conforms to Figma [APRÈS] Éditeur / Outil 4 3-Way:
 * - 3 Color Wheels visible simultaneously in a single row:
 *     1. Ombres (Shadows) with cyan readout (e.g. "Teal 195°")
 *     2. Tons Moyens (Midtones) with amber readout (e.g. "Warm 42°")
 *     3. Hautes Lum. (Highlights) with gold readout (e.g. "Gold 35°")
 * - Bipolar Slider below: "Balance globale" with mono readout (e.g. "+10")
 */
@Composable
fun EditorColorGradingPanel(
    colorGrading: ColorGradingModel,
    onUpdateColorGrading: ((ColorGradingModel) -> ColorGradingModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        // --- 1. Row of 3 Wheels (Figma Node 12:1071) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wheel 1: Ombres
            MiniColorWheel(
                title = "Ombres",
                hue = if (colorGrading.shadowSat > 0f) colorGrading.shadowHue else 195f,
                sat = if (colorGrading.shadowSat > 0f) colorGrading.shadowSat else 40f,
                labelColor = Color(0xFF38BDF8),
                labelText = if (colorGrading.shadowSat > 0f) "Teal ${colorGrading.shadowHue.roundToInt()}°" else "Teal 195°",
                onHueSatChange = { h, s ->
                    onUpdateColorGrading { it.copy(shadowHue = h, shadowSat = s) }
                }
            )

            // Wheel 2: Tons Moyens
            MiniColorWheel(
                title = "Tons Moyens",
                hue = if (colorGrading.midtoneSat > 0f) colorGrading.midtoneHue else 42f,
                sat = if (colorGrading.midtoneSat > 0f) colorGrading.midtoneSat else 35f,
                labelColor = Color(0xFFE5A93C),
                labelText = if (colorGrading.midtoneSat > 0f) "Warm ${colorGrading.midtoneHue.roundToInt()}°" else "Warm 42°",
                onHueSatChange = { h, s ->
                    onUpdateColorGrading { it.copy(midtoneHue = h, midtoneSat = s) }
                }
            )

            // Wheel 3: Hautes Lum.
            MiniColorWheel(
                title = "Hautes Lum.",
                hue = if (colorGrading.highlightSat > 0f) colorGrading.highlightHue else 35f,
                sat = if (colorGrading.highlightSat > 0f) colorGrading.highlightSat else 45f,
                labelColor = Color(0xFFF59E0B),
                labelText = if (colorGrading.highlightSat > 0f) "Gold ${colorGrading.highlightHue.roundToInt()}°" else "Gold 35°",
                onHueSatChange = { h, s ->
                    onUpdateColorGrading { it.copy(highlightHue = h, highlightSat = s) }
                }
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))

        // --- 2. Balance Globale Bipolar Slider (Figma Node 12:1090) ---
        SzlBipolarSlider(
            title = "Balance globale",
            value = colorGrading.balance,
            onValueChange = { newVal ->
                onUpdateColorGrading { it.copy(balance = newVal) }
            },
            min = -100f,
            max = 100f,
            defaultValue = 0f,
            unit = ""
        )
    }
}

@Composable
private fun MiniColorWheel(
    title: String,
    hue: Float,
    sat: Float,
    labelColor: Color,
    labelText: String,
    onHueSatChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val monoFont = LocalFontSpec.current.mono
    val wheelSize = 84.dp

    val chromaticSpectrum = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .size(wheelSize)
                .clip(CircleShape)
                .background(Color(0xFF18181B))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHueSatChange(0f, 0f)
                        },
                        onTap = { offset ->
                            val radius = size.width / 2f
                            val dx = offset.x - radius
                            val dy = offset.y - radius
                            val dist = hypot(dx, dy)
                            if (dist <= radius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                val newSat = ((dist / radius) * 100f).coerceIn(0f, 100f)
                                onHueSatChange(angle, newSat)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val radius = size.width / 2f
                        val dx = change.position.x - radius
                        val dy = change.position.y - radius
                        val dist = hypot(dx, dy)
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        val newSat = ((dist / radius) * 100f).coerceIn(0f, 100f)
                        onHueSatChange(angle, newSat)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(wheelSize)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 4.dp.toPx()

                // Spectrum Outer Ring
                drawCircle(
                    brush = Brush.sweepGradient(chromaticSpectrum, center = center),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Inner Crosshairs
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(center.x - radius * 0.7f, center.y),
                    end = Offset(center.x + radius * 0.7f, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(center.x, center.y - radius * 0.7f),
                    end = Offset(center.x, center.y + radius * 0.7f),
                    strokeWidth = 1.dp.toPx()
                )

                // Puck Indicator
                val angleRad = Math.toRadians(hue.toDouble())
                val puckDist = (sat / 100f) * radius
                val puckX = center.x + (puckDist * cos(angleRad)).toFloat()
                val puckY = center.y + (puckDist * sin(angleRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(puckX, puckY)
                )
                drawCircle(
                    color = labelColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(puckX, puckY)
                )
            }
        }

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}
'''

path = "app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorColorGradingPanel.kt"
with open(path, "w", encoding="utf-8") as f:
    f.write(code)
print("Updated EditorColorGradingPanel.kt to 3-wheel row matching Figma 12:1031")
