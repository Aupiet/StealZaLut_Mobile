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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ToneCurveModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlin.math.abs
import kotlin.math.sin

/**
 * Unified Tone Curve Develop Panel.
 * Merges Point Curves and Parametric Zones into a single cohesive interface,
 * with an Edit Lock toggle button to prevent accidental touches.
 */
@Composable
fun EditorToneCurvePanel(
    toneCurve: ToneCurveModel,
    onUpdateToneCurve: ((ToneCurveModel) -> ToneCurveModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableIntStateOf(0) } // 0 = RGB, 1 = Red, 2 = Green, 3 = Blue
    var isEditMode by remember { mutableStateOf(false) }

    val activePoints = when (selectedChannel) {
        1 -> toneCurve.redPoints
        2 -> toneCurve.greenPoints
        3 -> toneCurve.bluePoints
        else -> toneCurve.rgbPoints
    }

    fun updatePoints(sorted: List<Pair<Float, Float>>) {
        onUpdateToneCurve { tc ->
            when (selectedChannel) {
                1 -> tc.copy(redPoints = sorted)
                2 -> tc.copy(greenPoints = sorted)
                3 -> tc.copy(bluePoints = sorted)
                else -> tc.copy(rgbPoints = sorted)
            }
        }
    }

    fun handlePointTouch(xNorm: Float, yNorm: Float, allowAdd: Boolean) {
        val updated = activePoints.toMutableList()
        val closestIndex = updated.indices.minByOrNull { i ->
            abs(updated[i].first - xNorm) + abs(updated[i].second - yNorm)
        }
        val minDist = closestIndex?.let { i ->
            abs(updated[i].first - xNorm) + abs(updated[i].second - yNorm)
        } ?: 1f

        if (closestIndex != null && (minDist < 0.15f || !allowAdd)) {
            val newX = if (closestIndex == 0) 0f else if (closestIndex == updated.size - 1) 1f else xNorm
            updated[closestIndex] = newX to yNorm
        } else if (allowAdd) {
            updated.add(xNorm to yNorm)
        }
        updatePoints(updated.sortedBy { it.first })
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- Channel Selector & Edit Mode Toggle ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedChannel,
                edgePadding = AppDimens.SpacingNone,
                modifier = Modifier.weight(1f)
            ) {
                Tab(selected = selectedChannel == 0, onClick = { selectedChannel = 0 }, text = { Text(stringResource(R.string.channel_rgb)) })
                Tab(selected = selectedChannel == 1, onClick = { selectedChannel = 1 }, text = { Text(stringResource(R.string.channel_red)) })
                Tab(selected = selectedChannel == 2, onClick = { selectedChannel = 2 }, text = { Text(stringResource(R.string.channel_green)) })
                Tab(selected = selectedChannel == 3, onClick = { selectedChannel = 3 }, text = { Text(stringResource(R.string.channel_blue)) })
            }

            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))

            // Edit Lock / Unlock Button (Prevents accidental tactile modifications)
            FilterChip(
                selected = isEditMode,
                onClick = { isEditMode = !isEditMode },
                leadingIcon = {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconMicro)
                    )
                },
                label = {
                    Text(
                        text = if (isEditMode) stringResource(R.string.tone_curve_edit_active) else stringResource(R.string.tone_curve_edit_locked),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }

        // --- Interactive Visualizer Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ToneCurveCanvasHeight)
                .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                .background(ColorCardReference.NeutralDarkBackground)
                .border(
                    width = if (isEditMode) AppDimens.BorderThick else AppDimens.BorderThin,
                    color = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                )
        ) {
            val strokeColor = when (selectedChannel) {
                1 -> ColorCardReference.HistogramRed
                2 -> ColorCardReference.HistogramGreen
                3 -> ColorCardReference.HistogramBlue
                else -> ColorCardReference.WhiteStep
            }

            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (isEditMode) {
                            Modifier
                                .pointerInput(selectedChannel, activePoints) {
                                    detectTapGestures { offset ->
                                        val xNorm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val yNorm = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                        handlePointTouch(xNorm, yNorm, allowAdd = true)
                                    }
                                }
                                .pointerInput(selectedChannel, activePoints) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val xNorm = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val yNorm = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                                        handlePointTouch(xNorm, yNorm, allowAdd = false)
                                    }
                                }
                        } else {
                            Modifier
                        }
                    )
            ) {
                val w = size.width
                val h = size.height

                // 4x4 Grid and diagonal guide
                val gridColor = ColorCardReference.WhiteStep.copy(alpha = 0.12f)
                for (i in 1..3) {
                    drawLine(gridColor, Offset(w * (i / 4.0f), 0f), Offset(w * (i / 4.0f), h), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h * (i / 4.0f)), Offset(w, h * (i / 4.0f)), strokeWidth = 1f)
                }
                drawLine(ColorCardReference.WhiteStep.copy(alpha = 0.2f), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                // Render unified curve path
                val path = Path()
                val sorted = activePoints.sortedBy { it.first }
                if (sorted.isNotEmpty()) {
                    for (i in sorted.indices) {
                        val px = sorted[i].first * w
                        val py = h - (sorted[i].second * h)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                }
                drawPath(path = path, color = strokeColor, style = Stroke(width = 3f))

                // Control point handles
                for (pt in activePoints) {
                    val center = Offset(pt.first * w, h - (pt.second * h))
                    drawCircle(ColorCardReference.NeutralDarkBackground, radius = 7f, center = center)
                    drawCircle(strokeColor, radius = 5f, center = center)
                }
            }
        }

        // --- Merged Quick Tonal Sliders & Presets ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            FilterChip(
                selected = false,
                onClick = { updatePoints(listOf(0.0f to 0.0f, 1.0f to 1.0f)) },
                label = { Text(stringResource(R.string.btn_reset_section)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = false,
                onClick = { updatePoints(listOf(0.0f to 0.0f, 0.25f to 0.20f, 0.75f to 0.80f, 1.0f to 1.0f)) },
                label = { Text(stringResource(R.string.slider_contrast)) },
                modifier = Modifier.weight(1f)
            )
        }

        SzlBipolarSlider(
            title = stringResource(R.string.slider_highlights),
            value = toneCurve.highlights,
            onValueChange = { newVal -> onUpdateToneCurve { it.copy(highlights = newVal) } },
            min = -100.0f,
            max = 100.0f
        )
        SzlBipolarSlider(
            title = stringResource(R.string.slider_shadows),
            value = toneCurve.shadows,
            onValueChange = { newVal -> onUpdateToneCurve { it.copy(shadows = newVal) } },
            min = -100.0f,
            max = 100.0f
        )
    }
}
