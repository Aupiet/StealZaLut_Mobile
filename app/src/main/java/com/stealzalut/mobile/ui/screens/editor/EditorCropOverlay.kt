package com.stealzalut.mobile.ui.screens.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.szlColors

/**
 * Photographic standard crop ratios.
 */
enum class CropAspectRatio(val ratio: Float?, @param:StringRes val labelRes: Int) {
    FREE(null, R.string.crop_free),
    ORIGINAL(null, R.string.crop_original),
    SQUARE_1_1(1.0f, R.string.crop_square),
    PORTRAIT_4_5(0.8f, R.string.crop_portrait),
    RATIO_3_2(1.5f, R.string.crop_3_2),
    LANDSCAPE_16_9(16.0f / 9.0f, R.string.crop_widescreen)
}

private enum class DragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT_EDGE,
    RIGHT_EDGE,
    TOP_EDGE,
    BOTTOM_EDGE,
    INSIDE_MOVE
}

/**
 * Interactive touch overlay superimposed directly over the image in the Viewport.
 * Features:
 * - Rule of thirds dashed grid guidelines.
 * - Semi-transparent dark scrim outside the crop area.
 * - Corner and edge handles complying with WCAG AA tactile targets.
 * - Full multi-directional drag gestures: resize corners/edges or pan entire frame.
 */
@Composable
fun EditorCropOverlay(
    cropRegion: CropRegion,
    onCropRegionChange: (CropRegion) -> Unit,
    modifier: Modifier = Modifier,
    onCropFinished: (() -> Unit)? = null
) {
    val cropOverlayColor = MaterialTheme.szlColors.cropOverlay
    val cropHandleColor = MaterialTheme.szlColors.cropHandle
    val cropGridColor = MaterialTheme.szlColors.cropGrid
    val density = LocalDensity.current
    val touchHandleRadiusPx = with(density) { 36.dp.toPx() }

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropRegion) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val leftPx = cropRegion.left * w
                        val topPx = cropRegion.top * h
                        val rightPx = cropRegion.right * w
                        val bottomPx = cropRegion.bottom * h

                        val x = startOffset.x
                        val y = startOffset.y

                        fun dist(px: Float, py: Float) = kotlin.math.hypot(x - px, y - py)

                        activeHandle = when {
                            dist(leftPx, topPx) <= touchHandleRadiusPx -> DragHandle.TOP_LEFT
                            dist(rightPx, topPx) <= touchHandleRadiusPx -> DragHandle.TOP_RIGHT
                            dist(leftPx, bottomPx) <= touchHandleRadiusPx -> DragHandle.BOTTOM_LEFT
                            dist(rightPx, bottomPx) <= touchHandleRadiusPx -> DragHandle.BOTTOM_RIGHT
                            kotlin.math.abs(x - leftPx) <= touchHandleRadiusPx && y in topPx..bottomPx -> DragHandle.LEFT_EDGE
                            kotlin.math.abs(x - rightPx) <= touchHandleRadiusPx && y in topPx..bottomPx -> DragHandle.RIGHT_EDGE
                            kotlin.math.abs(y - topPx) <= touchHandleRadiusPx && x in leftPx..rightPx -> DragHandle.TOP_EDGE
                            kotlin.math.abs(y - bottomPx) <= touchHandleRadiusPx && x in leftPx..rightPx -> DragHandle.BOTTOM_EDGE
                            x in leftPx..rightPx && y in topPx..bottomPx -> DragHandle.INSIDE_MOVE
                            else -> DragHandle.NONE
                        }
                    },
                    onDragEnd = {
                        activeHandle = DragHandle.NONE
                        onCropFinished?.invoke()
                    },
                    onDragCancel = {
                        activeHandle = DragHandle.NONE
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectDragGestures

                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h
                    val minDim = 0.08f

                    when (activeHandle) {
                        DragHandle.TOP_LEFT -> {
                            val newLeft = (cropRegion.left + dx).coerceIn(0f, cropRegion.right - minDim)
                            val newTop = (cropRegion.top + dy).coerceIn(0f, cropRegion.bottom - minDim)
                            onCropRegionChange(cropRegion.copy(left = newLeft, top = newTop))
                        }
                        DragHandle.TOP_RIGHT -> {
                            val newRight = (cropRegion.right + dx).coerceIn(cropRegion.left + minDim, 1f)
                            val newTop = (cropRegion.top + dy).coerceIn(0f, cropRegion.bottom - minDim)
                            onCropRegionChange(cropRegion.copy(right = newRight, top = newTop))
                        }
                        DragHandle.BOTTOM_LEFT -> {
                            val newLeft = (cropRegion.left + dx).coerceIn(0f, cropRegion.right - minDim)
                            val newBottom = (cropRegion.bottom + dy).coerceIn(cropRegion.top + minDim, 1f)
                            onCropRegionChange(cropRegion.copy(left = newLeft, bottom = newBottom))
                        }
                        DragHandle.BOTTOM_RIGHT -> {
                            val newRight = (cropRegion.right + dx).coerceIn(cropRegion.left + minDim, 1f)
                            val newBottom = (cropRegion.bottom + dy).coerceIn(cropRegion.top + minDim, 1f)
                            onCropRegionChange(cropRegion.copy(right = newRight, bottom = newBottom))
                        }
                        DragHandle.LEFT_EDGE -> {
                            val newLeft = (cropRegion.left + dx).coerceIn(0f, cropRegion.right - minDim)
                            onCropRegionChange(cropRegion.copy(left = newLeft))
                        }
                        DragHandle.RIGHT_EDGE -> {
                            val newRight = (cropRegion.right + dx).coerceIn(cropRegion.left + minDim, 1f)
                            onCropRegionChange(cropRegion.copy(right = newRight))
                        }
                        DragHandle.TOP_EDGE -> {
                            val newTop = (cropRegion.top + dy).coerceIn(0f, cropRegion.bottom - minDim)
                            onCropRegionChange(cropRegion.copy(top = newTop))
                        }
                        DragHandle.BOTTOM_EDGE -> {
                            val newBottom = (cropRegion.bottom + dy).coerceIn(cropRegion.top + minDim, 1f)
                            onCropRegionChange(cropRegion.copy(bottom = newBottom))
                        }
                        DragHandle.INSIDE_MOVE -> {
                            val boxW = cropRegion.right - cropRegion.left
                            val boxH = cropRegion.bottom - cropRegion.top
                            var newLeft = cropRegion.left + dx
                            var newTop = cropRegion.top + dy

                            if (newLeft < 0f) newLeft = 0f
                            if (newLeft + boxW > 1f) newLeft = 1f - boxW
                            if (newTop < 0f) newTop = 0f
                            if (newTop + boxH > 1f) newTop = 1f - boxH

                            onCropRegionChange(cropRegion.copy(
                                left = newLeft,
                                top = newTop,
                                right = newLeft + boxW,
                                bottom = newTop + boxH
                            ))
                        }
                        DragHandle.NONE -> Unit
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val leftPx = cropRegion.left * canvasW
            val topPx = cropRegion.top * canvasH
            val rightPx = cropRegion.right * canvasW
            val bottomPx = cropRegion.bottom * canvasH
            val cropW = rightPx - leftPx
            val cropH = bottomPx - topPx

            // Semi-transparent scrim outside crop rect
            drawRect(color = cropOverlayColor, size = Size(canvasW, topPx))
            drawRect(color = cropOverlayColor, topLeft = Offset(0f, topPx), size = Size(leftPx, cropH))
            drawRect(color = cropOverlayColor, topLeft = Offset(rightPx, topPx), size = Size(canvasW - rightPx, cropH))
            drawRect(color = cropOverlayColor, topLeft = Offset(0f, bottomPx), size = Size(canvasW, canvasH - bottomPx))

            // Crop border
            drawRect(
                color = cropHandleColor,
                topLeft = Offset(leftPx, topPx),
                size = Size(cropW, cropH),
                style = Stroke(width = AppDimens.BorderMedium.toPx())
            )

            // Rule-of-thirds grid
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val oneThirdW = cropW / 3f
            val oneThirdH = cropH / 3f

            drawLine(
                color = cropGridColor,
                start = Offset(leftPx + oneThirdW, topPx),
                end = Offset(leftPx + oneThirdW, bottomPx),
                strokeWidth = AppDimens.BorderThin.toPx(),
                pathEffect = dash
            )
            drawLine(
                color = cropGridColor,
                start = Offset(leftPx + 2f * oneThirdW, topPx),
                end = Offset(leftPx + 2f * oneThirdW, bottomPx),
                strokeWidth = AppDimens.BorderThin.toPx(),
                pathEffect = dash
            )
            drawLine(
                color = cropGridColor,
                start = Offset(leftPx, topPx + oneThirdH),
                end = Offset(rightPx, topPx + oneThirdH),
                strokeWidth = AppDimens.BorderThin.toPx(),
                pathEffect = dash
            )
            drawLine(
                color = cropGridColor,
                start = Offset(leftPx, topPx + 2f * oneThirdH),
                end = Offset(rightPx, topPx + 2f * oneThirdH),
                strokeWidth = AppDimens.BorderThin.toPx(),
                pathEffect = dash
            )

            // Tactical corner handles (48dp interaction footprint)
            val handleLen = AppDimens.IconMedium.toPx()
            val handleThickness = AppDimens.BorderThick.toPx()

            // Top-Left
            drawLine(cropHandleColor, Offset(leftPx, topPx), Offset(leftPx + handleLen, topPx), handleThickness)
            drawLine(cropHandleColor, Offset(leftPx, topPx), Offset(leftPx, topPx + handleLen), handleThickness)

            // Top-Right
            drawLine(cropHandleColor, Offset(rightPx, topPx), Offset(rightPx - handleLen, topPx), handleThickness)
            drawLine(cropHandleColor, Offset(rightPx, topPx), Offset(rightPx, topPx + handleLen), handleThickness)

            // Bottom-Left
            drawLine(cropHandleColor, Offset(leftPx, bottomPx), Offset(leftPx + handleLen, bottomPx), handleThickness)
            drawLine(cropHandleColor, Offset(leftPx, bottomPx), Offset(leftPx, bottomPx - handleLen), handleThickness)

            // Bottom-Right
            drawLine(cropHandleColor, Offset(rightPx, bottomPx), Offset(rightPx - handleLen, bottomPx), handleThickness)
            drawLine(cropHandleColor, Offset(rightPx, bottomPx), Offset(rightPx, bottomPx - handleLen), handleThickness)

            // Mid-Edge tactile notches
            val midW = leftPx + cropW / 2f
            val midH = topPx + cropH / 2f
            val edgeNotchLen = handleLen * 0.6f

            // Top edge
            drawLine(cropHandleColor, Offset(midW - edgeNotchLen / 2f, topPx), Offset(midW + edgeNotchLen / 2f, topPx), handleThickness)
            // Bottom edge
            drawLine(cropHandleColor, Offset(midW - edgeNotchLen / 2f, bottomPx), Offset(midW + edgeNotchLen / 2f, bottomPx), handleThickness)
            // Left edge
            drawLine(cropHandleColor, Offset(leftPx, midH - edgeNotchLen / 2f), Offset(leftPx, midH + edgeNotchLen / 2f), handleThickness)
            // Right edge
            drawLine(cropHandleColor, Offset(rightPx, midH - edgeNotchLen / 2f), Offset(rightPx, midH + edgeNotchLen / 2f), handleThickness)
        }
    }
}

/**
 * Bottom drawer panel controls for Aspect Ratio selection, Horizon Straightening, and Reset.
 */
@Composable
fun EditorCropPanel(
    cropRegion: CropRegion,
    onCropRegionChange: (CropRegion) -> Unit,
    modifier: Modifier = Modifier,
    onCropFinished: (() -> Unit)? = null
) {
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.ORIGINAL) }
    var currentAngle by remember { mutableFloatStateOf(cropRegion.angle) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // Ratio Pills Row with Reset button
        val ratioScrollState = androidx.compose.foundation.rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(ratioScrollState),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally)
        ) {
            // Reset Crop button
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)
                    .clickable {
                        selectedRatio = CropAspectRatio.ORIGINAL
                        currentAngle = 0.0f
                        onCropRegionChange(CropRegion())
                        onCropFinished?.invoke()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.crop_reset),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall)
                    )
                }
            }

            CropAspectRatio.entries.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)
                        .clickable {
                            selectedRatio = ratio
                            val r = ratio.ratio
                            if (r != null) {
                                val currentW = cropRegion.right - cropRegion.left
                                val newBottom = (cropRegion.top + currentW / r).coerceIn(cropRegion.top + 0.05f, 1.0f)
                                onCropRegionChange(cropRegion.copy(bottom = newBottom))
                            }
                            onCropFinished?.invoke()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(ratio.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall)
                        )
                    }
                }
            }
        }

        // Straighten Slider (-45° to +45°)
        SzlBipolarSlider(
            title = stringResource(R.string.crop_straighten_title),
            value = currentAngle,
            onValueChange = { angle ->
                currentAngle = angle
                onCropRegionChange(cropRegion.copy(angle = angle))
            },
            min = -45.0f,
            max = 45.0f,
            defaultValue = 0.0f,
            onValueChangeFinished = onCropFinished,
            unit = stringResource(R.string.unit_degrees)
        )
    }
}
