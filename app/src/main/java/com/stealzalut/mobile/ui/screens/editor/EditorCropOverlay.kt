package com.stealzalut.mobile.ui.screens.editor

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.theme.szlColors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Photographic standard crop ratios conforming to professional photography workflows.
 */
enum class CropAspectRatio(val ratio: Float?, @param:StringRes val labelRes: Int) {
    ORIGINAL(null, R.string.crop_original),
    FREE(null, R.string.crop_free),
    SQUARE_1_1(1.0f, R.string.crop_square),
    RATIO_4_3(4.0f / 3.0f, R.string.crop_4_3),
    RATIO_3_2(1.5f, R.string.crop_3_2),
    LANDSCAPE_16_9(16.0f / 9.0f, R.string.crop_16_9),
    PORTRAIT_9_16(9.0f / 16.0f, R.string.crop_9_16),
    RATIO_5_4(5.0f / 4.0f, R.string.crop_5_4),
    PORTRAIT_4_5(0.8f, R.string.crop_portrait)
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

    /**
     * Adapts the current crop region to a new target aspect ratio around its current center,
     * preserving the user's framing instead of resetting unnecessarily to the full image.
     */
    fun adaptCropToRatio(
        crop: CropRegion,
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int
    ): CropRegion {
        if (ratio == CropAspectRatio.FREE || imageWidth <= 0 || imageHeight <= 0) {
            return crop
        }
        val targetRatio = if (ratio == CropAspectRatio.ORIGINAL) {
            imageWidth.toFloat() / imageHeight.toFloat()
        } else {
            ratio.ratio ?: return crop
        }

        val k = imageWidth.toFloat() / (imageHeight.toFloat() * targetRatio)
        val cx = ((crop.left + crop.right) / 2f).coerceIn(0.05f, 0.95f)
        val cy = ((crop.top + crop.bottom) / 2f).coerceIn(0.05f, 0.95f)

        val curW = (crop.right - crop.left).coerceAtLeast(0.05f)
        val curH = (crop.bottom - crop.top).coerceAtLeast(0.05f)

        val maxWCenter = min(cx, 1f - cx) * 2f
        val maxHCenter = min(cy, 1f - cy) * 2f
        val maxW = min(maxWCenter, maxHCenter / k)

        var targetW = max(curW, curH / k).coerceIn(0.05f, maxW)
        var targetH = targetW * k

        if (targetH > maxHCenter) {
            targetH = maxHCenter
            targetW = targetH / k
        }

        val newLeft = (cx - targetW / 2f).coerceIn(0f, 1f - targetW)
        val newTop = (cy - targetH / 2f).coerceIn(0f, 1f - targetH)

        return crop.copy(
            left = newLeft,
            top = newTop,
            right = newLeft + targetW,
            bottom = newTop + targetH
        )
    }

    /**
     * Rotates crop framing by 90 degrees clockwise while preserving bounds.
     */
    fun rotate90(crop: CropRegion): CropRegion {
        val newL = (1.0f - crop.bottom).coerceIn(0f, 1f)
        val newT = crop.left.coerceIn(0f, 1f)
        val newR = (1.0f - crop.top).coerceIn(0f, 1f)
        val newB = crop.right.coerceIn(0f, 1f)
        return crop.copy(
            left = min(newL, newR),
            top = min(newT, newB),
            right = max(newL, newR),
            bottom = max(newT, newB)
        )
    }

    /**
     * Calculates the current magnification factor of the crop selection.
     * Full frame is 1.0x.
     */
    fun computeCurrentZoom(crop: CropRegion): Float {
        val w = crop.right - crop.left
        val h = crop.bottom - crop.top
        val maxDim = max(w, h)
        return if (maxDim > 0.001f) (1.0f / maxDim).coerceIn(1.0f, 10.0f) else 1.0f
    }

    /**
     * Multiplies current zoom level by [factor] around the center of the crop framing.
     * factor > 1.0 zooms in (tighter crop box).
     * factor < 1.0 zooms out (larger crop box).
     */
    fun zoomBy(
        crop: CropRegion,
        factor: Float,
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int,
        minDim: Float = 0.05f
    ): CropRegion {
        if (factor <= 0f) return crop
        val cx = (crop.left + crop.right) / 2f
        val cy = (crop.top + crop.bottom) / 2f
        return applyPinchZoomPan(
            crop = crop,
            zoomFactor = factor,
            panDx = 0f,
            panDy = 0f,
            centroidX = cx,
            centroidY = cy,
            ratio = ratio,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            minDim = minDim
        )
    }

    /**
     * Sets target zoom level directly (e.g. from 1.0x to 4.0x) maintaining the current center.
     */
    fun setZoom(
        crop: CropRegion,
        targetZoom: Float,
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int
    ): CropRegion {
        val currentZoom = computeCurrentZoom(crop)
        if (currentZoom <= 0.01f || targetZoom <= 0.01f) return crop
        val factor = targetZoom / currentZoom
        return zoomBy(crop, factor, ratio, imageWidth, imageHeight)
    }

    /**
     * Translates (nudges) the crop framing by [deltaX] and [deltaY] in normalized coordinates.
     */
    fun nudge(crop: CropRegion, deltaX: Float, deltaY: Float): CropRegion {
        val w = crop.right - crop.left
        val h = crop.bottom - crop.top
        val newL = (crop.left + deltaX).coerceIn(0f, 1f - w)
        val newT = (crop.top + deltaY).coerceIn(0f, 1f - h)
        return crop.copy(
            left = newL,
            top = newT,
            right = newL + w,
            bottom = newT + h
        )
    }

    /**
     * Centers the crop framing within the image boundaries without altering its size.
     */
    fun center(crop: CropRegion): CropRegion {
        val w = crop.right - crop.left
        val h = crop.bottom - crop.top
        val newL = ((1.0f - w) / 2f).coerceIn(0f, 1f - w)
        val newT = ((1.0f - h) / 2f).coerceIn(0f, 1f - h)
        return crop.copy(
            left = newL,
            top = newT,
            right = newL + w,
            bottom = newT + h
        )
    }

    /**
     * Applies pinch-to-zoom and two-finger pan to the crop region.
     * When zooming into the photo (zoomFactor > 1), the crop box in normalized image space shrinks
     * centered at the touch centroid (framing a tighter portion of the image).
     * When zooming out of the photo (zoomFactor < 1), the crop box expands towards full image bounds [0, 1].
     * Pan translates the crop box across the image in the direction of the fingers.
     * When an aspect ratio is locked, the ratio is strictly preserved.
     */
    fun applyPinchZoomPan(
        crop: CropRegion,
        zoomFactor: Float,
        panDx: Float,
        panDy: Float,
        centroidX: Float,
        centroidY: Float,
        ratio: CropAspectRatio,
        imageWidth: Int,
        imageHeight: Int,
        minDim: Float = 0.05f
    ): CropRegion {
        if (zoomFactor <= 0f) return crop

        val curW = crop.right - crop.left
        val curH = crop.bottom - crop.top

        val k: Float? = when (ratio) {
            CropAspectRatio.FREE -> null
            CropAspectRatio.ORIGINAL -> if (imageHeight > 0 && imageWidth > 0) 1.0f else null
            else -> ratio.ratio?.let { r ->
                if (imageHeight > 0 && r > 0f) (imageWidth.toFloat() / (imageHeight.toFloat() * r)) else null
            }
        }

        // Zooming in on image shrinks the crop box; zooming out expands it
        val scale = 1.0f / zoomFactor

        if (k == null) {
            var newW = (curW * scale).coerceIn(minDim, 1.0f)
            var newH = (curH * scale).coerceIn(minDim, 1.0f)

            val anchorX = if (centroidX in crop.left..crop.right) centroidX else (crop.left + curW / 2f)
            val anchorY = if (centroidY in crop.top..crop.bottom) centroidY else (crop.top + curH / 2f)

            val relX = if (curW > 0.001f) (anchorX - crop.left) / curW else 0.5f
            val relY = if (curH > 0.001f) (anchorY - crop.top) / curH else 0.5f

            var newL = anchorX - relX * newW + panDx
            var newT = anchorY - relY * newH + panDy

            if (newL < 0f) newL = 0f
            if (newL + newW > 1.0f) {
                if (newW > 1.0f) newW = 1.0f
                newL = (1.0f - newW).coerceAtLeast(0f)
            }

            if (newT < 0f) newT = 0f
            if (newT + newH > 1.0f) {
                if (newH > 1.0f) newH = 1.0f
                newT = (1.0f - newH).coerceAtLeast(0f)
            }

            return crop.copy(
                left = newL,
                top = newT,
                right = newL + newW,
                bottom = newT + newH
            )
        } else {
            val maxW = min(1.0f, 1.0f / k)
            var newW = (curW * scale).coerceIn(minDim, maxW)
            var newH = newW * k

            val anchorX = if (centroidX in crop.left..crop.right) centroidX else (crop.left + curW / 2f)
            val anchorY = if (centroidY in crop.top..crop.bottom) centroidY else (crop.top + curH / 2f)

            val relX = if (curW > 0.001f) (anchorX - crop.left) / curW else 0.5f
            val relY = if (curH > 0.001f) (anchorY - crop.top) / curH else 0.5f

            var newL = (anchorX - relX * newW + panDx).coerceIn(0f, 1f - newW)
            var newT = (anchorY - relY * newH + panDy).coerceIn(0f, 1f - newH)

            return crop.copy(
                left = newL,
                top = newT,
                right = newL + newW,
                bottom = newT + newH
            )
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
            CropAspectRatio.ORIGINAL -> if (imageHeight > 0 && imageWidth > 0) 1.0f else null
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
                    val newB = (crop.bottom + dy).coerceIn(0f, crop.bottom - minDim)
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
 * - Multitouch pinch-to-zoom and pan support without gesture conflict.
 * - Top Action Strip with Cancel, Rotate 90°, Reset, and Apply buttons.
 */
@Composable
fun EditorCropOverlay(
    cropRegion: CropRegion,
    onCropRegionChange: (CropRegion) -> Unit,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    modifier: Modifier = Modifier,
    onCropFinished: (() -> Unit)? = null,
    onCancelCrop: (() -> Unit)? = null,
    onApplyCrop: (() -> Unit)? = null,
    onRotate90: (() -> Unit)? = null,
    onResetCrop: (() -> Unit)? = null,
    onTransformImage: ((zoom: Float, pan: Offset) -> Unit)? = null
) {
    val cropOverlayColor = MaterialTheme.szlColors.cropOverlay
    val cropHandleColor = MaterialTheme.szlColors.cropHandle
    val cropGridColor = MaterialTheme.szlColors.cropGrid
    val darkroomColor = MaterialTheme.szlColors.photoDarkroom
    val density = LocalDensity.current

    val currentCropRegion by rememberUpdatedState(cropRegion)
    val currentSelectedRatio by rememberUpdatedState(selectedRatio)
    val currentImageWidth by rememberUpdatedState(imageWidth)
    val currentImageHeight by rememberUpdatedState(imageHeight)
    val currentOnCropRegionChange by rememberUpdatedState(onCropRegionChange)
    val currentOnCropFinished by rememberUpdatedState(onCropFinished)

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var isInteracting by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    val gridAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "cropGridAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(imageWidth, imageHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    val now = System.currentTimeMillis()
                    val isDoubleTap = (now - lastTapTime < 350L) && (down.position - lastTapPos).getDistance() < 50f
                    lastTapTime = now
                    lastTapPos = down.position

                    val paddingH = with(density) { AppDimens.SpacingSmall.toPx() }
                    val paddingV = with(density) { AppDimens.SpacingMicro.toPx() }
                    val availW = (size.width - 2f * paddingH).coerceAtLeast(1f)
                    val availH = (size.height - 2f * paddingV).coerceAtLeast(1f)

                    val (imgW, imgH) = if (currentImageWidth > 0 && currentImageHeight > 0) {
                        val imgAspect = currentImageWidth.toFloat() / currentImageHeight.toFloat()
                        val availAspect = availW / availH
                        if (availAspect > imgAspect) (availH * imgAspect) to availH else availW to (availW / imgAspect)
                    } else {
                        availW to availH
                    }

                    val imgLeft = paddingH + (availW - imgW) / 2f
                    val imgTop = paddingV + (availH - imgH) / 2f

                    if (isDoubleTap && imgW > 0f && imgH > 0f) {
                        // Tactile double-tap zoom toggle: 2x zoom into tap point <-> 1x reset
                        val tapNormX = ((down.position.x - imgLeft) / imgW).coerceIn(0f, 1f)
                        val tapNormY = ((down.position.y - imgTop) / imgH).coerceIn(0f, 1f)
                        val curZoom = CropPhysics.computeCurrentZoom(currentCropRegion)
                        val toggled = if (curZoom > 1.25f) {
                            CropPhysics.zoomBy(currentCropRegion, 1.0f / curZoom, currentSelectedRatio, currentImageWidth, currentImageHeight)
                        } else {
                            CropPhysics.applyPinchZoomPan(
                                crop = currentCropRegion,
                                zoomFactor = 2.0f,
                                panDx = 0f,
                                panDy = 0f,
                                centroidX = tapNormX,
                                centroidY = tapNormY,
                                ratio = currentSelectedRatio,
                                imageWidth = currentImageWidth,
                                imageHeight = currentImageHeight
                            )
                        }
                        currentOnCropRegionChange(toggled)
                        currentOnCropFinished?.invoke()
                        lastTapTime = 0L
                        return@awaitEachGesture
                    }

                    val initialCrop = currentCropRegion
                    val leftPx = imgLeft + initialCrop.left * imgW
                    val topPx = imgTop + initialCrop.top * imgH
                    val rightPx = imgLeft + initialCrop.right * imgW
                    val bottomPx = imgTop + initialCrop.bottom * imgH

                    val x = down.position.x
                    val y = down.position.y
                    fun dist(px: Float, py: Float) = hypot(x - px, y - py)

                    val cornerRadius = with(density) { 52.dp.toPx() }
                    val edgeRadius = with(density) { 42.dp.toPx() }

                    val handle = when {
                        dist(leftPx, topPx) <= cornerRadius -> DragHandle.TOP_LEFT
                        dist(rightPx, topPx) <= cornerRadius -> DragHandle.TOP_RIGHT
                        dist(leftPx, bottomPx) <= cornerRadius -> DragHandle.BOTTOM_LEFT
                        dist(rightPx, bottomPx) <= cornerRadius -> DragHandle.BOTTOM_RIGHT
                        abs(x - leftPx) <= edgeRadius && y in (topPx - edgeRadius)..(bottomPx + edgeRadius) -> DragHandle.LEFT_EDGE
                        abs(x - rightPx) <= edgeRadius && y in (topPx - edgeRadius)..(bottomPx + edgeRadius) -> DragHandle.RIGHT_EDGE
                        abs(y - topPx) <= edgeRadius && x in (leftPx - edgeRadius)..(rightPx + edgeRadius) -> DragHandle.TOP_EDGE
                        abs(y - bottomPx) <= edgeRadius && x in (leftPx - edgeRadius)..(rightPx + edgeRadius) -> DragHandle.BOTTOM_EDGE
                        // Any drag inside selection or on the photo translates the crop window
                        else -> DragHandle.INSIDE_MOVE
                    }

                    activeHandle = handle
                    isInteracting = true
                    var currentCrop = initialCrop
                    var totalDragDist = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        if (activePointers.isEmpty()) {
                            activeHandle = DragHandle.NONE
                            isInteracting = false
                            currentOnCropFinished?.invoke()
                            break
                        }

                        if (activePointers.size >= 2) {
                            // Multitouch pinch-to-zoom and two-finger pan
                            lastTapTime = 0L
                            activeHandle = DragHandle.NONE
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()

                            if (imgW > 0f && imgH > 0f) {
                                val panDx = pan.x / imgW
                                val panDy = pan.y / imgH
                                val centroidNormX = ((centroid.x - imgLeft) / imgW).coerceIn(0f, 1f)
                                val centroidNormY = ((centroid.y - imgTop) / imgH).coerceIn(0f, 1f)

                                currentCrop = CropPhysics.applyPinchZoomPan(
                                    crop = currentCrop,
                                    zoomFactor = zoom,
                                    panDx = panDx,
                                    panDy = panDy,
                                    centroidX = centroidNormX,
                                    centroidY = centroidNormY,
                                    ratio = currentSelectedRatio,
                                    imageWidth = currentImageWidth,
                                    imageHeight = currentImageHeight
                                )
                                currentOnCropRegionChange(currentCrop)
                            }
                            onTransformImage?.invoke(zoom, pan)
                            event.changes.forEach { it.consume() }
                        } else if (activePointers.size == 1) {
                            // Single pointer dragging handle or box
                            val change = activePointers.first()
                            val dragDelta = change.position - change.previousPosition
                            totalDragDist += dragDelta.getDistance()
                            if (totalDragDist > 8f) {
                                lastTapTime = 0L
                            }
                            change.consume()

                            if (imgW > 0f && imgH > 0f && (abs(dragDelta.x) > 0.001f || abs(dragDelta.y) > 0.001f)) {
                                val dx = dragDelta.x / imgW
                                val dy = dragDelta.y / imgH
                                currentCrop = CropPhysics.applyDrag(
                                    crop = currentCrop,
                                    handle = handle,
                                    dx = dx,
                                    dy = dy,
                                    ratio = currentSelectedRatio,
                                    imageWidth = currentImageWidth,
                                    imageHeight = currentImageHeight
                                )
                                currentOnCropRegionChange(currentCrop)
                            }
                        }
                    }
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
            val cropW = (rightPx - leftPx).coerceAtLeast(1f)
            val cropH = (bottomPx - topPx).coerceAtLeast(1f)

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

            // 4. Rule-of-thirds grid (fades in smoothly when interacting)
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

            // 7. Tactical Center Move Reticle (subtle indicator showing draggable selection when idle)
            if (cropW >= 100f && cropH >= 100f && gridAlpha < 0.2f) {
                val reticleRadius = 14f
                val reticleColor = cropHandleColor.copy(alpha = 0.35f)
                val reticleStroke = AppDimens.BorderThin.toPx()
                drawCircle(color = reticleColor, radius = reticleRadius, center = Offset(midW, midH), style = Stroke(width = reticleStroke))
                val tickLen = 6f
                drawLine(reticleColor, Offset(midW - reticleRadius - tickLen, midH), Offset(midW - reticleRadius, midH), reticleStroke)
                drawLine(reticleColor, Offset(midW + reticleRadius, midH), Offset(midW + reticleRadius + tickLen, midH), reticleStroke)
                drawLine(reticleColor, Offset(midW, midH - reticleRadius - tickLen), Offset(midW, midH - reticleRadius), reticleStroke)
                drawLine(reticleColor, Offset(midW, midH + reticleRadius), Offset(midW, midH + reticleRadius + tickLen), reticleStroke)
            }
        }

        // Floating Top Confirmation Bar: Cancel | Rotate 90° | Reset | Apply
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = AppDimens.SpacingNormal)
                .padding(horizontal = AppDimens.SpacingSmall),
            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                // Cancel button
                IconButton(
                    onClick = { onCancelCrop?.invoke() },
                    modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.crop_cancel),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    )
                }

                // Current Zoom Factor Badge
                val topZoom = CropPhysics.computeCurrentZoom(cropRegion)
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", topZoom),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = 4.dp)
                    )
                }

                // Rotate 90° button
                IconButton(
                    onClick = { onRotate90?.invoke() },
                    modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                        contentDescription = stringResource(R.string.crop_rotate_90),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    )
                }

                // Reset button
                IconButton(
                    onClick = { onResetCrop?.invoke() },
                    modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.crop_reset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    )
                }

                // Apply button
                Button(
                    onClick = { onApplyCrop?.invoke() },
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                        Text(
                            text = stringResource(R.string.crop_apply),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
    onCropFinished: (() -> Unit)? = null,
    onRotate90: (() -> Unit)? = null
) {
    var currentAngle by remember { mutableFloatStateOf(cropRegion.angle) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // Ratio Pills Row with Reset and Rotate 90°
        val ratioScrollState = androidx.compose.foundation.rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(ratioScrollState),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
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

            // Rotate 90° button
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)
                    .clickable {
                        onRotate90?.invoke()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "90°",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Predefined Aspect Ratio Pills
            CropAspectRatio.entries.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)
                        .clickable {
                            onRatioChange(ratio)
                            // Non-destructive adaptation around current framing center
                            val adapted = CropPhysics.adaptCropToRatio(cropRegion, ratio, imageWidth, imageHeight)
                            onCropRegionChange(adapted.copy(angle = currentAngle))
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
