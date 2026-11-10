package com.stealzalut.mobile.ui.screens.editor

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

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

enum class DragHandle {
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
 * High-performance geometric physics calculations for photo cropping,
 * ensuring strict aspect ratio preservation, boundary clamping, and zero distortion.
 */
object CropPhysics {

    fun computeFittedCropRegion(
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int
    ): CropRegion {
        if (ratio == CropAspectRatio.FREE || ratio == CropAspectRatio.ORIGINAL || imageWidth <= 0 || imageHeight <= 0) {
            return CropRegion() // Reset to full uncropped frame
        }
        val targetRatio = ratio.ratio ?: return CropRegion()
        val k = imageWidth.toFloat() / (imageHeight.toFloat() * targetRatio)

        return if (k <= 1.0f) {
            val h = k
            val top = (1.0f - h) / 2.0f
            CropRegion(left = 0f, top = top, right = 1f, bottom = top + h)
        } else {
            val w = 1.0f / k
            val left = (1.0f - w) / 2.0f
            CropRegion(left = left, top = 0f, right = left + w, bottom = 1f)
        }
    }

    fun applyDrag(
        crop: CropRegion,
        handle: DragHandle,
        dx: Float,
        dy: Float,
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int,
        minDim: Float = 0.05f
    ): CropRegion {
        val k: Float? = when (ratio) {
            CropAspectRatio.FREE -> null
            CropAspectRatio.ORIGINAL -> 1.0f
            else -> ratio.ratio?.let { r ->
                if (imageHeight > 0 && r > 0f) (imageWidth.toFloat() / (imageHeight.toFloat() * r)) else null
            }
        }

        return when (handle) {
            DragHandle.NONE -> crop
            DragHandle.INSIDE_MOVE -> {
                val w = crop.right - crop.left
                val h = crop.bottom - crop.top
                val newL = (crop.left + dx).coerceIn(0f, 1f - w)
                val newT = (crop.top + dy).coerceIn(0f, 1f - h)
                crop.copy(left = newL, top = newT, right = newL + w, bottom = newT + h)
            }
            DragHandle.BOTTOM_RIGHT -> {
                if (k == null) {
                    val newR = (crop.right + dx).coerceIn(crop.left + minDim, 1f)
                    val newB = (crop.bottom + dy).coerceIn(crop.top + minDim, 1f)
                    crop.copy(right = newR, bottom = newB)
                } else {
                    val maxW = min(1f - crop.left, (1f - crop.top) / k)
                    val targetW = (crop.right + dx - crop.left).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(right = crop.left + targetW, bottom = crop.top + targetH)
                }
            }
            DragHandle.TOP_LEFT -> {
                if (k == null) {
                    val newL = (crop.left + dx).coerceIn(0f, crop.right - minDim)
                    val newT = (crop.top + dy).coerceIn(0f, crop.bottom - minDim)
                    crop.copy(left = newL, top = newT)
                } else {
                    val maxW = min(crop.right, crop.bottom / k)
                    val targetW = (crop.right - (crop.left + dx)).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(left = crop.right - targetW, top = crop.bottom - targetH)
                }
            }
            DragHandle.TOP_RIGHT -> {
                if (k == null) {
                    val newR = (crop.right + dx).coerceIn(crop.left + minDim, 1f)
                    val newT = (crop.top + dy).coerceIn(0f, crop.bottom - minDim)
                    crop.copy(right = newR, top = newT)
                } else {
                    val maxW = min(1f - crop.left, crop.bottom / k)
                    val targetW = (crop.right + dx - crop.left).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(right = crop.left + targetW, top = crop.bottom - targetH)
                }
            }
            DragHandle.BOTTOM_LEFT -> {
                if (k == null) {
                    val newL = (crop.left + dx).coerceIn(0f, crop.right - minDim)
                    val newB = (crop.bottom + dy).coerceIn(crop.top + minDim, 1f)
                    crop.copy(left = newL, bottom = newB)
                } else {
                    val maxW = min(crop.right, (1f - crop.top) / k)
                    val targetW = (crop.right - (crop.left + dx)).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(left = crop.right - targetW, bottom = crop.top + targetH)
                }
            }
            DragHandle.LEFT_EDGE -> {
                if (k == null) {
                    val newL = (crop.left + dx).coerceIn(0f, crop.right - minDim)
                    crop.copy(left = newL)
                } else {
                    val centerV = (crop.top + crop.bottom) / 2f
                    val maxHByCenter = min(centerV, 1f - centerV) * 2f
                    val maxW = min(crop.right, maxHByCenter / k)
                    val targetW = (crop.right - (crop.left + dx)).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(
                        left = crop.right - targetW,
                        top = centerV - targetH / 2f,
                        bottom = centerV + targetH / 2f
                    )
                }
            }
            DragHandle.RIGHT_EDGE -> {
                if (k == null) {
                    val newR = (crop.right + dx).coerceIn(crop.left + minDim, 1f)
                    crop.copy(right = newR)
                } else {
                    val centerV = (crop.top + crop.bottom) / 2f
                    val maxHByCenter = min(centerV, 1f - centerV) * 2f
                    val maxW = min(1f - crop.left, maxHByCenter / k)
                    val targetW = (crop.right + dx - crop.left).coerceIn(minDim, maxW)
                    val targetH = targetW * k
                    crop.copy(
                        right = crop.left + targetW,
                        top = centerV - targetH / 2f,
                        bottom = centerV + targetH / 2f
                    )
                }
            }
            DragHandle.TOP_EDGE -> {
                if (k == null) {
                    val newT = (crop.top + dy).coerceIn(0f, crop.bottom - minDim)
                    crop.copy(top = newT)
                } else {
                    val centerU = (crop.left + crop.right) / 2f
                    val maxWByCenter = min(centerU, 1f - centerU) * 2f
                    val maxH = min(crop.bottom, maxWByCenter * k)
                    val targetH = (crop.bottom - (crop.top + dy)).coerceIn(minDim * k, maxH)
                    val targetW = targetH / k
                    crop.copy(
                        top = crop.bottom - targetH,
                        left = centerU - targetW / 2f,
                        right = centerU + targetW / 2f
                    )
                }
            }
            DragHandle.BOTTOM_EDGE -> {
                if (k == null) {
                    val newB = (crop.bottom + dy).coerceIn(crop.top + minDim, 1f)
                    crop.copy(bottom = newB)
                } else {
                    val centerU = (crop.left + crop.right) / 2f
                    val maxWByCenter = min(centerU, 1f - centerU) * 2f
                    val maxH = min(1f - crop.top, maxWByCenter * k)
                    val targetH = (crop.bottom + dy - crop.top).coerceIn(minDim * k, maxH)
                    val targetW = targetH / k
                    crop.copy(
                        bottom = crop.top + targetH,
                        left = centerU - targetW / 2f,
                        right = centerU + targetW / 2f
                    )
                }
            }
        }
    }
}

/**
 * Interactive touch overlay superimposed directly over the image in the Viewport.
 * Features:
 * - Fluid tactile corner and edge tracking with generous hit targets.
 * - Dynamic Rule of Thirds grid that fades in smoothly during active drag gestures.
 * - Strict aspect ratio locking based on physical pixel dimensions.
 * - High-contrast corner L-brackets with drop shadows for pro visibility.
 * - Hardware accelerated real-time straighten rotation.
 */
@Composable
fun EditorCropOverlay(
    cropRegion: CropRegion,
    onCropRegionChange: (CropRegion) -> Unit,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    modifier: Modifier = Modifier,
    onCropFinished: (() -> Unit)? = null
) {
    val cropOverlayColor = MaterialTheme.szlColors.cropOverlay
    val cropHandleColor = MaterialTheme.szlColors.cropHandle
    val cropGridColor = MaterialTheme.szlColors.cropGrid
    val darkroomColor = MaterialTheme.szlColors.photoDarkroom
    val density = LocalDensity.current

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    val isDragging = activeHandle != DragHandle.NONE

    val gridAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "cropGridAlpha" // GUARD_ALLOW
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropRegion, selectedRatio, imageWidth, imageHeight) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val paddingH = with(density) { AppDimens.SpacingSmall.toPx() }
                        val paddingV = with(density) { AppDimens.SpacingMicro.toPx() }
                        val availW = (size.width - 2f * paddingH).coerceAtLeast(1f)
                        val availH = (size.height - 2f * paddingV).coerceAtLeast(1f)

                        val (imgW, imgH) = if (imageWidth > 0 && imageHeight > 0) {
                            val imgAspect = imageWidth.toFloat() / imageHeight.toFloat()
                            val availAspect = availW / availH
                            if (availAspect > imgAspect) (availH * imgAspect) to availH else availW to (availW / imgAspect)
                        } else {
                            availW to availH
                        }

                        val imgLeft = paddingH + (availW - imgW) / 2f
                        val imgTop = paddingV + (availH - imgH) / 2f

                        val leftPx = imgLeft + cropRegion.left * imgW
                        val topPx = imgTop + cropRegion.top * imgH
                        val rightPx = imgLeft + cropRegion.right * imgW
                        val bottomPx = imgTop + cropRegion.bottom * imgH

                        val x = startOffset.x
                        val y = startOffset.y
                        fun dist(px: Float, py: Float) = hypot(x - px, y - py)

                        val cornerRadius = with(density) { 44.dp.toPx() }
                        val edgeRadius = with(density) { 36.dp.toPx() }

                        activeHandle = when {
                            dist(leftPx, topPx) <= cornerRadius -> DragHandle.TOP_LEFT
                            dist(rightPx, topPx) <= cornerRadius -> DragHandle.TOP_RIGHT
                            dist(leftPx, bottomPx) <= cornerRadius -> DragHandle.BOTTOM_LEFT
                            dist(rightPx, bottomPx) <= cornerRadius -> DragHandle.BOTTOM_RIGHT
                            abs(x - leftPx) <= edgeRadius && y in (topPx - edgeRadius)..(bottomPx + edgeRadius) -> DragHandle.LEFT_EDGE
                            abs(x - rightPx) <= edgeRadius && y in (topPx - edgeRadius)..(bottomPx + edgeRadius) -> DragHandle.RIGHT_EDGE
                            abs(y - topPx) <= edgeRadius && x in (leftPx - edgeRadius)..(rightPx + edgeRadius) -> DragHandle.TOP_EDGE
                            abs(y - bottomPx) <= edgeRadius && x in (leftPx - edgeRadius)..(rightPx + edgeRadius) -> DragHandle.BOTTOM_EDGE
                            x in leftPx..rightPx && y in topPx..bottomPx -> DragHandle.INSIDE_MOVE
                            else -> {
                                val dTL = dist(leftPx, topPx)
                                val dTR = dist(rightPx, topPx)
                                val dBL = dist(leftPx, bottomPx)
                                val dBR = dist(rightPx, bottomPx)
                                val minD = min(min(dTL, dTR), min(dBL, dBR))
                                if (minD <= cornerRadius * 1.5f) {
                                    when (minD) {
                                        dTL -> DragHandle.TOP_LEFT
                                        dTR -> DragHandle.TOP_RIGHT
                                        dBL -> DragHandle.BOTTOM_LEFT
                                        else -> DragHandle.BOTTOM_RIGHT
                                    }
                                } else DragHandle.NONE
                            }
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
                    val paddingH = with(density) { AppDimens.SpacingSmall.toPx() }
                    val paddingV = with(density) { AppDimens.SpacingMicro.toPx() }
                    val availW = (size.width - 2f * paddingH).coerceAtLeast(1f)
                    val availH = (size.height - 2f * paddingV).coerceAtLeast(1f)

                    val (imgW, imgH) = if (imageWidth > 0 && imageHeight > 0) {
                        val imgAspect = imageWidth.toFloat() / imageHeight.toFloat()
                        val availAspect = availW / availH
                        if (availAspect > imgAspect) (availH * imgAspect) to availH else availW to (availW / imgAspect)
                    } else {
                        availW to availH
                    }

                    if (imgW <= 0f || imgH <= 0f) return@detectDragGestures

                    val dx = dragAmount.x / imgW
                    val dy = dragAmount.y / imgH

                    val updated = CropPhysics.applyDrag(
                        crop = cropRegion,
                        handle = activeHandle,
                        dx = dx,
                        dy = dy,
                        ratio = selectedRatio,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight
                    )
                    onCropRegionChange(updated)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val paddingH = AppDimens.SpacingSmall.toPx()
            val paddingV = AppDimens.SpacingMicro.toPx()
            val availW = (canvasW - 2f * paddingH).coerceAtLeast(1f)
            val availH = (canvasH - 2f * paddingV).coerceAtLeast(1f)

            val (imgW, imgH) = if (imageWidth > 0 && imageHeight > 0) {
                val imgAspect = imageWidth.toFloat() / imageHeight.toFloat()
                val availAspect = availW / availH
                if (availAspect > imgAspect) (availH * imgAspect) to availH else availW to (availW / imgAspect)
            } else {
                availW to availH
            }

            val imgLeft = paddingH + (availW - imgW) / 2f
            val imgTop = paddingV + (availH - imgH) / 2f

            val leftPx = imgLeft + cropRegion.left * imgW
            val topPx = imgTop + cropRegion.top * imgH
            val rightPx = imgLeft + cropRegion.right * imgW
            val bottomPx = imgTop + cropRegion.bottom * imgH
            val cropW = rightPx - leftPx
            val cropH = bottomPx - topPx

            // 1. Outside photo darkroom letterboxing
            drawRect(color = darkroomColor, topLeft = Offset(0f, 0f), size = Size(canvasW, imgTop))
            drawRect(color = darkroomColor, topLeft = Offset(0f, imgTop + imgH), size = Size(canvasW, (canvasH - (imgTop + imgH)).coerceAtLeast(0f)))
            drawRect(color = darkroomColor, topLeft = Offset(0f, imgTop), size = Size(imgLeft, imgH))
            drawRect(color = darkroomColor, topLeft = Offset(imgLeft + imgW, imgTop), size = Size((canvasW - (imgLeft + imgW)).coerceAtLeast(0f), imgH))

            // 2. Scrim inside photo boundary but outside crop rect
            drawRect(color = cropOverlayColor, topLeft = Offset(imgLeft, imgTop), size = Size(imgW, (topPx - imgTop).coerceAtLeast(0f)))
            drawRect(color = cropOverlayColor, topLeft = Offset(imgLeft, bottomPx), size = Size(imgW, (imgTop + imgH - bottomPx).coerceAtLeast(0f)))
            drawRect(color = cropOverlayColor, topLeft = Offset(imgLeft, topPx), size = Size((leftPx - imgLeft).coerceAtLeast(0f), cropH))
            drawRect(color = cropOverlayColor, topLeft = Offset(rightPx, topPx), size = Size((imgLeft + imgW - rightPx).coerceAtLeast(0f), cropH))

            // 3. Crisp crop border
            drawRect(
                color = cropHandleColor,
                topLeft = Offset(leftPx, topPx),
                size = Size(cropW, cropH),
                style = Stroke(width = AppDimens.BorderMedium.toPx())
            )

            // 4. Rule-of-thirds grid (fades in smoothly when dragging)
            if (gridAlpha > 0.01f) {
                val oneThirdW = cropW / 3f
                val oneThirdH = cropH / 3f
                val gridColorWithAlpha = cropGridColor.copy(alpha = 0.75f * gridAlpha)
                val gridStroke = AppDimens.BorderThin.toPx()

                drawLine(color = gridColorWithAlpha, start = Offset(leftPx + oneThirdW, topPx), end = Offset(leftPx + oneThirdW, bottomPx), strokeWidth = gridStroke)
                drawLine(color = gridColorWithAlpha, start = Offset(leftPx + 2f * oneThirdW, topPx), end = Offset(leftPx + 2f * oneThirdW, bottomPx), strokeWidth = gridStroke)
                drawLine(color = gridColorWithAlpha, start = Offset(leftPx, topPx + oneThirdH), end = Offset(rightPx, topPx + oneThirdH), strokeWidth = gridStroke)
                drawLine(color = gridColorWithAlpha, start = Offset(leftPx, topPx + 2f * oneThirdH), end = Offset(rightPx, topPx + 2f * oneThirdH), strokeWidth = gridStroke)
            }

            // 5. Tactical Pro L-Bracket Handles (with subtle shadow outline for high contrast)
            val handleLen = AppDimens.IconMedium.toPx()
            val handleThickness = AppDimens.BorderThick.toPx()
            val shadowThickness = handleThickness + AppDimens.BorderThin.toPx() * 2f
            val shadowColor = Color.Black.copy(alpha = 0.5f)

            fun drawLBracket(cornerX: Float, cornerY: Float, dirX: Float, dirY: Float) {
                drawLine(shadowColor, Offset(cornerX, cornerY), Offset(cornerX + dirX * handleLen, cornerY), shadowThickness)
                drawLine(shadowColor, Offset(cornerX, cornerY), Offset(cornerX, cornerY + dirY * handleLen), shadowThickness)
                drawLine(cropHandleColor, Offset(cornerX, cornerY), Offset(cornerX + dirX * handleLen, cornerY), handleThickness)
                drawLine(cropHandleColor, Offset(cornerX, cornerY), Offset(cornerX, cornerY + dirY * handleLen), handleThickness)
            }

            drawLBracket(leftPx, topPx, 1f, 1f)
            drawLBracket(rightPx, topPx, -1f, 1f)
            drawLBracket(leftPx, bottomPx, 1f, -1f)
            drawLBracket(rightPx, bottomPx, -1f, -1f)

            // 6. Mid-Edge tactile notches
            val midW = leftPx + cropW / 2f
            val midH = topPx + cropH / 2f
            val notchLen = handleLen * 0.65f

            // Top edge
            drawLine(shadowColor, Offset(midW - notchLen / 2f, topPx), Offset(midW + notchLen / 2f, topPx), shadowThickness)
            drawLine(cropHandleColor, Offset(midW - notchLen / 2f, topPx), Offset(midW + notchLen / 2f, topPx), handleThickness)
            // Bottom edge
            drawLine(shadowColor, Offset(midW - notchLen / 2f, bottomPx), Offset(midW + notchLen / 2f, bottomPx), shadowThickness)
            drawLine(cropHandleColor, Offset(midW - notchLen / 2f, bottomPx), Offset(midW + notchLen / 2f, bottomPx), handleThickness)
            // Left edge
            drawLine(shadowColor, Offset(leftPx, midH - notchLen / 2f), Offset(leftPx, midH + notchLen / 2f), shadowThickness)
            drawLine(cropHandleColor, Offset(leftPx, midH - notchLen / 2f), Offset(leftPx, midH + notchLen / 2f), handleThickness)
            // Right edge
            drawLine(shadowColor, Offset(rightPx, midH - notchLen / 2f), Offset(rightPx, midH + notchLen / 2f), shadowThickness)
            drawLine(cropHandleColor, Offset(rightPx, midH - notchLen / 2f), Offset(rightPx, midH + notchLen / 2f), handleThickness)
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
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    selectedRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
    onRatioChange: (CropAspectRatio) -> Unit = {},
    modifier: Modifier = Modifier,
    onCropFinished: (() -> Unit)? = null
) {
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
                        onRatioChange(CropAspectRatio.ORIGINAL)
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
                            onRatioChange(ratio)
                            val fitted = CropPhysics.computeFittedCropRegion(ratio, imageWidth, imageHeight)
                            onCropRegionChange(fitted.copy(angle = currentAngle))
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
