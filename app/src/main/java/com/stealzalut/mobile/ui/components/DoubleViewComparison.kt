package com.stealzalut.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import kotlin.math.roundToInt

enum class ComparisonViewMode {
    SIDE_BY_SIDE,
    SPLIT_WIPE
}

/**
 * Dedicated Double View & Interactive Split Wipe Slider comparison component.
 * Features 120 FPS real-time GPU alpha blending:
 *   I_displayed(alpha) = (1 - alpha) * I_src + alpha * I_trans
 * Handled natively by Android RenderThread / GPU hardware overlays with zero Chaquopy calls during drag.
 */
@Composable
fun DoubleViewComparison(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    intensity: Float = 1.0f,
    executionTimeMs: Double? = null,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ComparisonViewMode.SIDE_BY_SIDE) }
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
            // Header with Execution Time
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
                                imageVector = Icons.Default.Speed,
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

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            // Mode Selector (Side-by-Side vs Split Wipe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewMode == ComparisonViewMode.SIDE_BY_SIDE,
                    onClick = { viewMode = ComparisonViewMode.SIDE_BY_SIDE },
                    label = { Text(stringResource(R.string.comparison_side_by_side)) },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = viewMode == ComparisonViewMode.SPLIT_WIPE,
                    onClick = { viewMode = ComparisonViewMode.SPLIT_WIPE },
                    label = { Text(stringResource(R.string.comparison_split_wipe)) },
                    colors = FilterChipDefaults.filterChipColors()
                )
                if (zoomScale > AppConstants.ZOOM_THRESHOLD_RESET) {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(start = AppDimens.SpacingExtraSmall)
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
                                imageVector = Icons.Default.ZoomIn,
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
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

            if (viewMode == ComparisonViewMode.SIDE_BY_SIDE) {
                // MODE 1 : CÔTE À CÔTE AVEC ZOOM ET PAN SYNCHRONISÉS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.DoubleViewHeight)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(AppConstants.ZOOM_SCALE_MIN, AppConstants.ZOOM_SCALE_MAX)
                                panOffset = if (zoomScale <= AppConstants.ZOOM_SCALE_MIN) Offset.Zero else panOffset + pan
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    zoomScale = if (zoomScale > AppConstants.ZOOM_SCALE_MIN) AppConstants.ZOOM_SCALE_MIN else AppConstants.ZOOM_SCALE_DOUBLE_TAP
                                    panOffset = Offset.Zero
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                ) {
                    // Left: Original
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusCard))
                            .clipToBounds()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE))
                            .border(
                                AppDimens.BorderThin,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM),
                                RoundedCornerShape(AppDimens.CornerRadiusCard)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sourceBitmap != null) {
                            Image(
                                bitmap = sourceBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.comparison_original_label),
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
                            PillLabel(
                                text = stringResource(R.string.comparison_original_label),
                                alignment = Alignment.BottomStart,
                                containerColor = Color.Black.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
                                contentColor = Color.White
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(AppDimens.SpacingMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconLarge),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                                Text(
                                    text = stringResource(R.string.wizard_step_1_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Right: Graded (Hardware GPU Composition)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusCard))
                            .clipToBounds()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE))
                            .border(
                                AppDimens.BorderThin,
                                if (previewBitmap != null) MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM),
                                RoundedCornerShape(AppDimens.CornerRadiusCard)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null || sourceBitmap != null) {
                            GpuBlendedImage(
                                sourceBitmap = sourceBitmap,
                                transferredBitmap = previewBitmap,
                                intensity = intensity,
                                zoomScale = zoomScale,
                                panOffset = panOffset,
                                contentDescription = stringResource(R.string.comparison_lut_label)
                            )
                            PillLabel(
                                text = stringResource(R.string.comparison_lut_label),
                                alignment = Alignment.BottomStart,
                                containerColor = if (previewBitmap != null) MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.SPLIT_WIPE_DIVIDER_ALPHA) else Color.Black.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
                                contentColor = if (previewBitmap != null) MaterialTheme.colorScheme.onPrimary else Color.White
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(AppDimens.SpacingMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconLarge),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                                Text(
                                    text = stringResource(R.string.comparison_lut_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // MODE 2 : RIDEAU INTERACTIF (SPLIT WIPE SLIDER) AVEC COMPOSITION GPU
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.DoubleViewHeight)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusCard))
                        .clipToBounds()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE))
                        .border(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM),
                            RoundedCornerShape(AppDimens.CornerRadiusCard)
                        )
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
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = (splitX + dragAmount.x).coerceIn(0f, containerWidthPx)
                                        splitFraction = (newX / containerWidthPx).coerceIn(0.02f, 0.98f)
                                    }
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
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = handleDesc,
                                        modifier = Modifier.size(AppDimens.IconNormal),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        // Labels
                        PillLabel(
                            text = stringResource(R.string.comparison_original_label),
                            alignment = Alignment.BottomStart,
                            containerColor = Color.Black.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
                            contentColor = Color.White
                        )
                        PillLabel(
                            text = stringResource(R.string.comparison_lut_label),
                            alignment = Alignment.BottomEnd,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_CONTAINER_HEAVY),
                            contentColor = Color.White
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
}

/**
 * Superimposes original image and 100% transferred image with GPU hardware graphicsLayer alpha.
 * Interpolates pixel by pixel directly on GPU at constant 120 FPS:
 *   I_displayed(alpha) = (1 - alpha) * I_src + alpha * I_trans
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
        // Base: Original Source (1 - alpha)
        if (sourceBitmap != null) {
            Image(
                bitmap = sourceBitmap.asImageBitmap(),
                contentDescription = null,
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
        // Overlay: 100% Transferred Image with GPU graphicsLayer alpha
        if (transferredBitmap != null) {
            Image(
                bitmap = transferredBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = intensity.coerceIn(0f, 1f)
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

@Composable
private fun PillLabel(
    text: String,
    alignment: Alignment,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.padding(AppDimens.SpacingSmall),
            shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
            color = containerColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
