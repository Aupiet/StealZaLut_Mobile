package com.stealzalut.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import kotlin.math.roundToInt

/**
 * Dedicated Interactive Curtain Comparison component (Studio Split Wipe).
 * Features an interactive split curtain divider with synchronized zoom and pan.
 */
@Composable
fun DoubleViewComparison(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    intensity: Float = 1.0f,
    executionTimeMs: Double? = null,
    height: androidx.compose.ui.unit.Dp = AppDimens.DoubleViewHeight,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var splitFraction by remember { mutableFloatStateOf(AppConstants.SPLIT_WIPE_DEFAULT_FRACTION) }

    // Synchronized Zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(AppConstants.ZOOM_SCALE_MIN) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingLarge)) {
            // Header with Execution Time & Zoom Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.comparison_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (zoomScale > AppConstants.ZOOM_THRESHOLD_RESET) {
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(end = AppDimens.SpacingSmall)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        zoomScale = AppConstants.ZOOM_SCALE_MIN
                                        panOffset = Offset.Zero
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionZoomIn,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconMicro),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingMicro))
                                Text(
                                    text = stringResource(R.string.comparison_zoom_reset, (zoomScale * AppConstants.INTENSITY_PERCENT_MULTIPLIER).toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (executionTimeMs != null) {
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.statusSpeed,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconMicro),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingMicro))
                                Text(
                                    text = stringResource(R.string.comparison_time_format, executionTimeMs.toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            // RIDEAU INTERACTIF (SPLIT WIPE SLIDER) AVEC COMPOSITION GPU
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusCard))
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE))
                    .border(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM),
                        RoundedCornerShape(AppDimens.CornerRadiusCard)
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(AppConstants.ZOOM_SCALE_MIN, AppConstants.ZOOM_SCALE_MAX)
                            panOffset = if (zoomScale <= AppConstants.ZOOM_SCALE_MIN) Offset.Zero else panOffset + pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                val newFraction = (offset.x / size.width.toFloat()).coerceIn(0.01f, 0.99f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                splitFraction = newFraction
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (size.width > 0) {
                                val delta = dragAmount / size.width.toFloat()
                                val newFraction = (splitFraction + delta).coerceIn(0.01f, 0.99f)
                                if ((splitFraction < 0.5f && newFraction >= 0.5f) || (splitFraction > 0.5f && newFraction <= 0.5f)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                splitFraction = newFraction
                            }
                        }
                    }
            ) {
                val containerWidthPx = constraints.maxWidth.toFloat()
                val splitX = containerWidthPx * splitFraction
                val handleDesc = stringResource(R.string.comparison_handle_desc)

                if (sourceBitmap != null) {
                    // Layer 1: GPU Graded Image (Under layer)
                    GpuBlendedImage(
                        sourceBitmap = sourceBitmap,
                        transferredBitmap = previewBitmap,
                        intensity = intensity,
                        zoomScale = zoomScale,
                        panOffset = panOffset,
                        contentDescription = stringResource(R.string.comparison_lut_label)
                    )

                    // Layer 2: Original Image clipped to [0, splitX]
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.comparison_original_label),
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentScale = ContentScale.Crop
                    )

                    // Vertical Divider Line
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(splitX.roundToInt(), 0) }
                            .width(AppDimens.BorderThick)
                            .fillMaxHeight()
                            .background(Color.White)
                    )

                    // Center Circular Handle with Vector Icon
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (splitX - AppDimens.CurtainHandleSize.toPx() / 2).roundToInt(),
                                    (constraints.maxHeight / 2 - AppDimens.CurtainHandleSize.toPx() / 2).roundToInt()
                                )
                            }
                            .size(AppDimens.MinTouchTarget)
                            .semantics {
                                contentDescription = handleDesc
                                stateDescription = "${(splitFraction * AppConstants.INTENSITY_PERCENT_MULTIPLIER).toInt()}%"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = AppDimens.SpacingSmall,
                            modifier = Modifier.size(AppDimens.CurtainHandleSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionCompare,
                                    contentDescription = handleDesc,
                                    modifier = Modifier.size(AppDimens.IconNormal),
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    // Badges matching Figma Node 12:405 / 12:407 (AVANT / APRÈS)
                    SzlImageBadge(
                        text = "AVANT",
                        alignment = Alignment.TopStart,
                        isCompareMode = false
                    )
                    SzlImageBadge(
                        text = "APRÈS",
                        alignment = Alignment.TopEnd,
                        isCompareMode = true
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.comparison_no_images),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the color-graded preview output computed from the unique final 3D LUT.
 *
 * Resolves Bug B9 (WYSIWYG parity):
 * Python preview rendering already incorporates the global intensity blend into the 3D LUT
 * (refine_and_blend_lut -> apply_lut). Rendering [transferredBitmap] directly at alpha = 1.0f
 * guarantees strict pixel parity (delta <= 1 8-bit level) between the preview viewport,
 * the exported .cube LUT, and the HD photo export, preventing double-attenuation.
 */
@Composable
private fun GpuBlendedImage(
    sourceBitmap: Bitmap?,
    transferredBitmap: Bitmap?,
    intensity: Float,
    zoomScale: Float,
    panOffset: Offset,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val bitmapToRender = transferredBitmap ?: sourceBitmap
        if (bitmapToRender != null) {
            Image(
                bitmap = bitmapToRender.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}