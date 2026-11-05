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
    SPLIT_WIPE,
    HOLD_TO_COMPARE
}

/**
 * Dedicated Double View & Interactive Comparison component (Studio + Editor).
 * Features 3 modes:
 * 1. Side-by-Side (synced 1:1 zoom/pan)
 * 2. Split Wipe (interactive curtain divider)
 * 3. Hold-to-Compare (touch anywhere on image to view original, release for graded)
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
    var isHoldingCompare by remember { mutableStateOf(false) }

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

            // Mode Selector
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
                FilterChip(
                    selected = viewMode == ComparisonViewMode.HOLD_TO_COMPARE,
                    onClick = { viewMode = ComparisonViewMode.HOLD_TO_COMPARE },
                    label = { Text(stringResource(R.string.comparison_hold_to_compare)) },
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

            when (viewMode) {
                ComparisonViewMode.SIDE_BY_SIDE -> {
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
                                SzlImageBadge(
                                    text = stringResource(R.string.comparison_original_label),
                                    alignment = Alignment.BottomStart,
                                    isCompareMode = false
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

                        // Right: Graded
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
                                SzlImageBadge(
                                    text = stringResource(R.string.comparison_lut_label),
                                    alignment = Alignment.BottomStart,
                                    isCompareMode = false
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
                }

                ComparisonViewMode.SPLIT_WIPE -> {
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
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (size.width > 0) {
                                        splitFraction = (offset.x / size.width.toFloat()).coerceIn(0.01f, 0.99f)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (size.width > 0) {
                                        val delta = dragAmount / size.width.toFloat()
                                        splitFraction = (splitFraction + delta).coerceIn(0.01f, 0.99f)
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
                                            imageVector = Icons.Default.Compare,
                                            contentDescription = handleDesc,
                                            modifier = Modifier.size(AppDimens.IconNormal),
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }

                            // Labels
                            SzlImageBadge(
                                text = stringResource(R.string.comparison_original_label),
                                alignment = Alignment.BottomStart,
                                isCompareMode = false
                            )
                            SzlImageBadge(
                                text = stringResource(R.string.comparison_lut_label),
                                alignment = Alignment.BottomEnd,
                                isCompareMode = false
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

                ComparisonViewMode.HOLD_TO_COMPARE -> {
                    // MODE 3 : APPUI LONG POUR COMPARER AVEC ZOOM & PAN
                    Box(
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
                                    },
                                    onPress = {
                                        isHoldingCompare = true
                                        tryAwaitRelease()
                                        isHoldingCompare = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val currentBmp = if (isHoldingCompare || previewBitmap == null) sourceBitmap else previewBitmap
                        if (currentBmp != null) {
                            Image(
                                bitmap = currentBmp.asImageBitmap(),
                                contentDescription = if (isHoldingCompare) stringResource(R.string.comparison_original_label) else stringResource(R.string.comparison_lut_label),
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
                            SzlImageBadge(
                                text = if (isHoldingCompare) stringResource(R.string.comparison_original_label) else stringResource(R.string.comparison_lut_label),
                                alignment = Alignment.TopCenter,
                                isCompareMode = isHoldingCompare
                            )
                        } else {
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
