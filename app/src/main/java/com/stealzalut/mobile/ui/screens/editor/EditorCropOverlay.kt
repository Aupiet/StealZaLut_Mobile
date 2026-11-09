package com.stealzalut.mobile.ui.screens.editor

import com.stealzalut.mobile.data.model.CropRegion

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
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
    LANDSCAPE_16_9(16.0f / 9.0f, R.string.crop_widescreen)
}

/**
 * Interactive touch overlay superimposed directly over the image in the Viewport (FE-005).
 * Features:
 * - Rule of thirds dashed grid guidelines.
 * - Semi-transparent dark scrim outside the crop area.
 * - 48dp tactile corner handles complying with WCAG AA (FE-019).
 * - Touch drag gestures to resize crop bounds.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropRegion) {
                detectDragGestures(
                    onDragEnd = { onCropFinished?.invoke() }
                ) { change, dragAmount ->
                    change.consume()
                    val newRight = (cropRegion.right + dragAmount.x / size.width).coerceIn(cropRegion.left + 0.1f, 1.0f)
                    val newBottom = (cropRegion.bottom + dragAmount.y / size.height).coerceIn(cropRegion.top + 0.1f, 1.0f)
                    onCropRegionChange(cropRegion.copy(right = newRight, bottom = newBottom))
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
        }
    }
}

/**
 * Bottom drawer panel controls for Aspect Ratio selection and Horizon Straightening.
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
        // Ratio Pills Row
        val ratioScrollState = androidx.compose.foundation.rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(ratioScrollState),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally)
        ) {
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
                                val newBottom = (cropRegion.top + (cropRegion.right - cropRegion.left) / r).coerceIn(0.1f, 1.0f)
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
