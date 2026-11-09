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
import com.stealzalut.mobile.ui.theme.icons
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ToneCurveModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.szlColors
import kotlin.math.abs

/**
 * Professional Zero-Scroll Tone Curve Develop Panel inspired by Lightroom.
 * Features:
 * - Two ultra-clean modes: Point Curve (spline canvas) and Zonal Sliders (Highlights/Shadows).
 * - Smooth cubic spline curve rendering with large tactile grab points (>= 48dp touch targets).
 * - Soft watermark histogram distribution in background.
 * - Fine dashed 45° reference diagonal and 4x4 zone grid.
 * - Explicit 4-zone tonal labels (Shadows, Darks, Lights, Highlights).
 * - Styled channel tabs with dedicated color swatches (White, Red #FF5252, Green #69F0AE, Blue #448AFF).
 * - Guaranteed zero vertical scrolling inside the editor drawer.
 */
@Composable
fun EditorToneCurvePanel(
    toneCurve: ToneCurveModel,
    onUpdateToneCurve: ((ToneCurveModel) -> ToneCurveModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var curveSubMode by remember { mutableIntStateOf(0) } // 0 = Point Curve Canvas, 1 = Zonal Sliders
    var selectedChannel by remember { mutableIntStateOf(0) } // 0 = RGB, 1 = Red, 2 = Green, 3 = Blue
    var isEditMode by remember { mutableStateOf(false) }
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
        val isTooClose = updated.any { abs(it.first - xNorm) < 0.05f }
        if (!isTooClose) {
            updated.add(xNorm to yNorm)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            updatePoints(updated.sortedBy { it.first })
        }
    }

    fun removeClosestIntermediatePoint(xNorm: Float, yNorm: Float) {
        val updated = activePoints.toMutableList()
        if (updated.size <= 2) return
        val closestIndex = updated.indices.minByOrNull { i ->
            abs(updated[i].first - xNorm) + abs(updated[i].second - yNorm)
        } ?: return
        if (closestIndex > 0 && closestIndex < updated.size - 1) {
            updated.removeAt(closestIndex)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            updatePoints(updated.sortedBy { it.first })
        }
    }

    fun moveClosestPoint(xNorm: Float, yNorm: Float, targetIndex: Int? = null) {
        val updated = activePoints.toMutableList()
        val indexToMove = targetIndex ?: updated.indices.minByOrNull { i ->
            abs(updated[i].first - xNorm) + abs(updated[i].second - yNorm)
        } ?: return

        val newX = when (indexToMove) {
            0 -> 0f
            updated.size - 1 -> 1f
            else -> xNorm.coerceIn(0.01f, 0.99f)
        }
        updated[indexToMove] = newX to yNorm.coerceIn(0f, 1f)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        updatePoints(updated.sortedBy { it.first })
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

            // Quick Reset & Edit Lock in Curve Mode
            if (curveSubMode == 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isEditMode = !isEditMode },
                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) MaterialTheme.icons.actionEdit else MaterialTheme.icons.statusLock,
                            contentDescription = null,
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    .clickable { selectedChannel = index }
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
                                width = if (isEditMode) AppDimens.BorderThick else AppDimens.BorderThin,
                                color = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                            )
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .then(
                                    if (isEditMode) {
                                        Modifier
                                            .pointerInput(selectedChannel, activePoints) {
                                                detectTapGestures(
                                                    onLongPress = { offset ->
                                                        val xNorm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val yNorm = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                                        addPoint(xNorm, yNorm)
                                                    },
                                                    onDoubleTap = { offset ->
                                                        val xNorm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val yNorm = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                                        removeClosestIntermediatePoint(xNorm, yNorm)
                                                    },
                                                    onTap = { offset ->
                                                        val xNorm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val yNorm = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                                        moveClosestPoint(xNorm, yNorm)
                                                    }
                                                )
                                            }
                                            .pointerInput(selectedChannel, activePoints) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        val xNorm = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val yNorm = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                                        draggingIndex = activePoints.indices.minByOrNull { i ->
                                                            abs(activePoints[i].first - xNorm) + abs(activePoints[i].second - yNorm)
                                                        }
                                                    },
                                                    onDragEnd = { draggingIndex = null },
                                                    onDragCancel = { draggingIndex = null },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        val xNorm = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                        val yNorm = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                                                        moveClosestPoint(xNorm, yNorm, draggingIndex)
                                                    }
                                                )
                                            }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val w = size.width
                            val h = size.height

                            // Background soft watermark histogram hill
                            val histoPath = Path()
                            histoPath.moveTo(0f, h)
                            val simulatedHisto = floatArrayOf(0.05f, 0.12f, 0.35f, 0.58f, 0.72f, 0.65f, 0.42f, 0.22f, 0.08f)
                            val stepH = w / (simulatedHisto.size - 1)
                            simulatedHisto.forEachIndexed { idx, v ->
                                val hx = idx * stepH
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

                            // Smooth Spline Tone Curve
                            val sorted = activePoints.sortedBy { it.first }
                            if (sorted.isNotEmpty()) {
                                val curvePath = Path()
                                val p0 = sorted.first()
                                curvePath.moveTo(p0.first * w, h - (p0.second * h))

                                if (sorted.size == 2) {
                                    val p1 = sorted.last()
                                    curvePath.lineTo(p1.first * w, h - (p1.second * h))
                                } else {
                                    for (i in 0 until sorted.size - 1) {
                                        val cur = sorted[i]
                                        val next = sorted[i + 1]
                                        val prev = if (i > 0) sorted[i - 1] else cur
                                        val nextNext = if (i + 2 < sorted.size) sorted[i + 2] else next

                                        val curX = cur.first * w
                                        val curY = h - (cur.second * h)
                                        val nextX = next.first * w
                                        val nextY = h - (next.second * h)

                                        val cp1x = curX + (nextX - (prev.first * w)) / 5f
                                        val cp1y = curY + (nextY - (h - (prev.second * h))) / 5f
                                        val cp2x = nextX - ((nextNext.first * w) - curX) / 5f
                                        val cp2y = nextY - ((h - (nextNext.second * h)) - curY) / 5f

                                        curvePath.cubicTo(cp1x, cp1y, cp2x, cp2y, nextX, nextY)
                                    }
                                }

                                drawPath(path = curvePath, color = channelColor, style = Stroke(width = 3.5f))
                            }

                            // Tactile Control Point Handles
                            for (pt in activePoints) {
                                val center = Offset(pt.first * w, h - (pt.second * h))
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
