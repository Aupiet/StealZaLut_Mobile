package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.core.MonotoneCubicSpline
import com.stealzalut.mobile.data.model.ToneCurveModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Professional Zero-Scroll Tone Curve Develop Panel inspired by Lightroom.
 * Features:
 * - Mathematical Fritsch-Carlson monotone cubic spline interpolation identical to Python backend.
 * - Relative delta-based dragging preventing abrupt teleportation.
 * - Strict X-monotonicity constraint: neighbor order is strictly preserved.
 * - Tactile capture radius (32dp) for point selection.
 * - Double-tap deletion & drag-off-canvas deletion of intermediate points.
 * - Real-time In/Out coordinate readout (0-255).
 * - Light haptic feedback on touch, selection, and mutation.
 */
@Composable
fun EditorToneCurvePanel(
    toneCurve: ToneCurveModel,
    onUpdateToneCurve: ((ToneCurveModel) -> ToneCurveModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val captureRadiusPx = with(density) { 32.dp.toPx() }
    val offCanvasMarginPx = with(density) { 36.dp.toPx() }

    var curveSubMode by remember { mutableIntStateOf(0) } // 0 = Point Curve Canvas, 1 = Zonal Sliders
    var selectedChannel by remember { mutableIntStateOf(0) } // 0 = RGB, 1 = Red, 2 = Green, 3 = Blue
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    val activePoints = when (selectedChannel) {
        1 -> toneCurve.redPoints
        2 -> toneCurve.greenPoints
        3 -> toneCurve.bluePoints
        else -> toneCurve.rgbPoints
    }

    val channelColor = when (selectedChannel) {
        1 -> MaterialTheme.szlColors.toneCurveRed
        2 -> MaterialTheme.szlColors.toneCurveGreen
        3 -> MaterialTheme.szlColors.toneCurveBlue
        else -> MaterialTheme.szlColors.toneCurveRgb
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

    fun addPoint(xNorm: Float, yNorm: Float) {
        val updated = activePoints.toMutableList()
        val minNeighborDist = 0.04f
        val isTooClose = updated.any { kotlin.math.abs(it.first - xNorm) < minNeighborDist }
        if (!isTooClose) {
            val newPt = xNorm.coerceIn(0.02f, 0.98f) to yNorm.coerceIn(0f, 1f)
            updated.add(newPt)
            val sorted = updated.sortedBy { it.first }
            selectedPointIndex = sorted.indexOf(newPt)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            updatePoints(sorted)
        }
    }

    fun removePointAtIndex(index: Int) {
        val updated = activePoints.toMutableList()
        if (updated.size <= 2) return
        if (index > 0 && index < updated.size - 1) {
            updated.removeAt(index)
            selectedPointIndex = null
            draggingIndex = null
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            updatePoints(updated.sortedBy { it.first })
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingMicro),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
    ) {
        // --- 1. Mode Switcher & Quick Controls Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpacingExtraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Pills: [ Courbe ] [ Zones ]
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = if (curveSubMode == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        if (curveSubMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier.clickable { curveSubMode = 0 }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMicro),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.toolToneCurve,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconMicro),
                            tint = if (curveSubMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                        Text(
                            text = stringResource(R.string.tone_curve_mode_curve),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (curveSubMode == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (curveSubMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = if (curveSubMode == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        if (curveSubMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier.clickable { curveSubMode = 1 }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMicro),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.toolBasic,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconMicro),
                            tint = if (curveSubMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                        Text(
                            text = stringResource(R.string.tone_curve_mode_zones),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (curveSubMode == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (curveSubMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-Time Coordinates Readout or Reset
            Row(verticalAlignment = Alignment.CenterVertically) {
                val currentSelected = selectedPointIndex?.let { activePoints.getOrNull(it) }
                if (currentSelected != null && curveSubMode == 0) {
                    val inVal = (currentSelected.first * 255f).roundToInt().coerceIn(0, 255)
                    val outVal = (currentSelected.second * 255f).roundToInt().coerceIn(0, 255)
                    Text(
                        text = stringResource(R.string.tone_curve_coordinates, inVal, outVal),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPointIndex = null
                        draggingIndex = null
                        updatePoints(listOf(0.0f to 0.0f, 1.0f to 1.0f))
                    },
                    modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                ) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionReset,
                        contentDescription = stringResource(R.string.btn_reset_section),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = curveSubMode,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "toneCurveSubModeTransition" // GUARD_ALLOW
        ) { mode ->
            if (mode == 0) {
                // --- Mode 0: Point Curve Canvas (Zero Vertical Scroll) ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                ) {
                    // Channel Selector Pills (RGB, Rouge, Vert, Bleu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val channels = listOf(
                            Triple(0, R.string.channel_rgb, MaterialTheme.szlColors.toneCurveRgb),
                            Triple(1, R.string.channel_red, MaterialTheme.szlColors.toneCurveRed),
                            Triple(2, R.string.channel_green, MaterialTheme.szlColors.toneCurveGreen),
                            Triple(3, R.string.channel_blue, MaterialTheme.szlColors.toneCurveBlue)
                        )

                        channels.forEach { (index, labelRes, dotColor) ->
                            val isSelected = selectedChannel == index
                            Surface(
                                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(AppDimens.BorderThin, dotColor) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedChannel = index
                                        selectedPointIndex = null
                                        draggingIndex = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(AppDimens.SpacingSmall)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Tone Curve Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.ToneCurveZeroScrollHeight)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                            .background(ColorCardReference.NeutralDarkBackground)
                            .border(
                                width = AppDimens.BorderThin,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                            )
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(selectedChannel, activePoints) {
                                    detectTapGestures(
                                        onDoubleTap = { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val closestIdx = activePoints.indices.minByOrNull { i ->
                                                val ptPxX = activePoints[i].first * w
                                                val ptPxY = h - (activePoints[i].second * h)
                                                hypot(offset.x - ptPxX, offset.y - ptPxY)
                                            }
                                            if (closestIdx != null) {
                                                val ptPxX = activePoints[closestIdx].first * w
                                                val ptPxY = h - (activePoints[closestIdx].second * h)
                                                if (hypot(offset.x - ptPxX, offset.y - ptPxY) <= captureRadiusPx) {
                                                    removePointAtIndex(closestIdx)
                                                }
                                            }
                                        },
                                        onTap = { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectTapGestures

                                            val closestIdx = activePoints.indices.minByOrNull { i ->
                                                val ptPxX = activePoints[i].first * w
                                                val ptPxY = h - (activePoints[i].second * h)
                                                hypot(offset.x - ptPxX, offset.y - ptPxY)
                                            }

                                            if (closestIdx != null) {
                                                val ptPxX = activePoints[closestIdx].first * w
                                                val ptPxY = h - (activePoints[closestIdx].second * h)
                                                if (hypot(offset.x - ptPxX, offset.y - ptPxY) <= captureRadiusPx) {
                                                    selectedPointIndex = closestIdx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    return@detectTapGestures
                                                }
                                            }

                                            // If not near any existing point, create new point on tap
                                            val xNorm = (offset.x / w).coerceIn(0f, 1f)
                                            val yNorm = (1f - (offset.y / h)).coerceIn(0f, 1f)
                                            addPoint(xNorm, yNorm)
                                        }
                                    )
                                }
                                .pointerInput(selectedChannel, activePoints) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectDragGestures

                                            val closestIdx = activePoints.indices.minByOrNull { i ->
                                                val ptPxX = activePoints[i].first * w
                                                val ptPxY = h - (activePoints[i].second * h)
                                                hypot(startOffset.x - ptPxX, startOffset.y - ptPxY)
                                            }

                                            if (closestIdx != null) {
                                                val ptPxX = activePoints[closestIdx].first * w
                                                val ptPxY = h - (activePoints[closestIdx].second * h)
                                                if (hypot(startOffset.x - ptPxX, startOffset.y - ptPxY) <= captureRadiusPx) {
                                                    draggingIndex = closestIdx
                                                    selectedPointIndex = closestIdx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else {
                                                    draggingIndex = null
                                                }
                                            } else {
                                                draggingIndex = null
                                            }
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val idx = draggingIndex ?: return@detectDragGestures
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            if (w <= 0f || h <= 0f) return@detectDragGestures

                                            // Check off-canvas deletion for intermediate points
                                            val pos = change.position
                                            if (idx > 0 && idx < activePoints.size - 1) {
                                                if (pos.y < -offCanvasMarginPx || pos.y > h + offCanvasMarginPx ||
                                                    pos.x < -offCanvasMarginPx || pos.x > w + offCanvasMarginPx) {
                                                    removePointAtIndex(idx)
                                                    return@detectDragGestures
                                                }
                                            }

                                            val currentPoint = activePoints[idx]
                                            val dx = dragAmount.x / w
                                            val dy = -dragAmount.y / h // Invert Y because canvas Y is top-down

                                            val eps = 0.02f
                                            val minX = if (idx == 0) 0f else activePoints[idx - 1].first + eps
                                            val maxX = if (idx == activePoints.size - 1) 1f else activePoints[idx + 1].first - eps

                                            val newX = when (idx) {
                                                0 -> 0f
                                                activePoints.size - 1 -> 1f
                                                else -> (currentPoint.first + dx).coerceIn(minX, maxX)
                                            }
                                            val newY = (currentPoint.second + dy).coerceIn(0f, 1f)

                                            val updated = activePoints.toMutableList()
                                            updated[idx] = newX to newY
                                            updatePoints(updated)
                                        }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Background soft watermark histogram hill
                            val histoPath = Path()
                            histoPath.moveTo(0f, h)
                            val simulatedHisto = floatArrayOf(0.05f, 0.12f, 0.35f, 0.58f, 0.72f, 0.65f, 0.42f, 0.22f, 0.08f)
                            val stepH = w / (simulatedHisto.size - 1)
                            simulatedHisto.forEachIndexed { sIdx, v ->
                                val hx = sIdx * stepH
                                val hy = h - (v * h * 0.45f)
                                histoPath.lineTo(hx, hy)
                            }
                            histoPath.lineTo(w, h)
                            histoPath.close()
                            drawPath(
                                path = histoPath,
                                color = ColorCardReference.WhiteStep.copy(alpha = 0.07f)
                            )

                            // 4x4 Grid zones
                            val gridColor = ColorCardReference.WhiteStep.copy(alpha = 0.10f)
                            for (i in 1..3) {
                                drawLine(gridColor, Offset(w * (i / 4.0f), 0f), Offset(w * (i / 4.0f), h), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, h * (i / 4.0f)), Offset(w, h * (i / 4.0f)), strokeWidth = 1f)
                            }

                            // Dotted 45° reference diagonal
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            val diagPath = Path().apply {
                                moveTo(0f, h)
                                lineTo(w, 0f)
                            }
                            drawPath(
                                path = diagPath,
                                color = ColorCardReference.WhiteStep.copy(alpha = 0.25f),
                                style = Stroke(width = 1.5f, pathEffect = dashEffect)
                            )

                            // Evaluated Fritsch-Carlson Monotone Cubic Spline (exact parity with Python backend)
                            val sorted = activePoints.sortedBy { it.first }
                            if (sorted.isNotEmpty()) {
                                val splineSamples = MonotoneCubicSpline.evaluateCurve(sorted, steps = 120)
                                if (splineSamples.isNotEmpty()) {
                                    val curvePath = Path()
                                    val firstPt = splineSamples.first()
                                    curvePath.moveTo(firstPt.first * w, h - (firstPt.second * h))
                                    for (i in 1 until splineSamples.size) {
                                        val sample = splineSamples[i]
                                        curvePath.lineTo(sample.first * w, h - (sample.second * h))
                                    }
                                    drawPath(path = curvePath, color = channelColor, style = Stroke(width = 3.5f))
                                }
                            }

                            // Tactile Control Point Handles with active halo
                            for ((idx, pt) in activePoints.withIndex()) {
                                val center = Offset(pt.first * w, h - (pt.second * h))
                                val isSelected = idx == selectedPointIndex || idx == draggingIndex

                                if (isSelected) {
                                    drawCircle(channelColor.copy(alpha = 0.35f), radius = 14f, center = center)
                                }
                                drawCircle(ColorCardReference.NeutralDarkBackground, radius = 9f, center = center)
                                drawCircle(channelColor, radius = 7f, center = center)
                                drawCircle(ColorCardReference.WhiteStep, radius = 4f, center = center)
                            }
                        }
                    }

                    // 4-Zone Tonal Legend & Quick S-Curve Contrast Chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingExtraSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val zones = listOf(
                                R.string.tone_curve_zone_shadows,
                                R.string.tone_curve_zone_darks,
                                R.string.tone_curve_zone_lights,
                                R.string.tone_curve_zone_highlights
                            )
                            zones.forEach { zoneRes ->
                                Text(
                                    text = stringResource(zoneRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))

                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPointIndex = null
                                updatePoints(listOf(0.0f to 0.0f, 0.25f to 0.20f, 0.75f to 0.80f, 1.0f to 1.0f))
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.slider_contrast),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                            )
                        }
                    }
                }
            } else {
                // --- Mode 1: Zonal Sliders (Highlights & Shadows) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingExtraSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    SzlBipolarSlider(
                        title = stringResource(R.string.slider_highlights),
                        value = toneCurve.highlights,
                        onValueChange = { newVal -> onUpdateToneCurve { it.copy(highlights = newVal) } },
                        min = -100.0f,
                        max = 100.0f,
                        defaultValue = 0.0f
                    )
                    SzlBipolarSlider(
                        title = stringResource(R.string.slider_shadows),
                        value = toneCurve.shadows,
                        onValueChange = { newVal -> onUpdateToneCurve { it.copy(shadows = newVal) } },
                        min = -100.0f,
                        max = 100.0f,
                        defaultValue = 0.0f
                    )
                }
            }
        }
    }
}
