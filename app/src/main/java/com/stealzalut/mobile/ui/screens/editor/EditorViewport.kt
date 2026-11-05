package com.stealzalut.mobile.ui.screens.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlImageBadge
import com.stealzalut.mobile.ui.components.SzlRgbHistogram

@Composable
fun EditorViewport(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    isProcessing: Boolean,
    executionTimeMs: Double?,
    lutName: String?,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    isHistogramExpanded: Boolean = false,
    onToggleHistogram: () -> Unit = {},
    isComputingAutoEdit: Boolean = false,
    onApplyAutoEdit: () -> Unit = {}
) {
    var isHoldingCompare by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val displayBitmap = if (isHoldingCompare && sourceBitmap != null) sourceBitmap else (previewBitmap ?: sourceBitmap)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerRadiusCard)),
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isHoldActive = false
                        try {
                            withTimeout(400L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        return@withTimeout
                                    }
                                    val distance = (change.position - down.position).getDistance()
                                    if (event.changes.size > 1 || distance > 18f) {
                                        return@withTimeout
                                    }
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            isHoldActive = true
                        }

                        if (isHoldActive) {
                            try {
                                isHoldingCompare = true
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                }
                            } finally {
                                isHoldingCompare = false
                            }
                        }
                    }
                }
        ) {
            displayBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }

            // Compare badge overlay (semantic compareBadge role, zero errorContainer hijack)
            if (isHoldingCompare) {
                SzlImageBadge(
                    text = stringResource(R.string.editor_compare_original_badge),
                    alignment = Alignment.TopCenter,
                    isCompareMode = true
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AppDimens.SpacingSmall)
                ) {
                    Text(
                        text = stringResource(R.string.editor_hold_compare),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    )
                }
            }

            // Top-left LUT status badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppDimens.SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = if (lutName != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(AppDimens.SpacingMicro)
                ) {
                    Text(
                        text = lutName ?: stringResource(R.string.editor_no_lut_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lutName != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    )
                }
            }

            // Top-right controls (Histogram toggle, Change photo, processing indicator)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProcessing && !isComputingAutoEdit) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                }
                FilledTonalIconButton(
                    onClick = onApplyAutoEdit,
                    enabled = !isComputingAutoEdit && !isProcessing,
                    modifier = Modifier.size(AppDimens.ViewportButtonSize)
                ) {
                    if (isComputingAutoEdit) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppDimens.IconSmall),
                            strokeWidth = AppDimens.BorderThick,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = stringResource(R.string.btn_auto_edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                FilledTonalIconButton(
                    onClick = onToggleHistogram,
                    modifier = Modifier.size(AppDimens.ViewportButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.section_histogram),
                        tint = if (isHistogramExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                FilledTonalIconButton(
                    onClick = onChangePhoto,
                    modifier = Modifier.size(AppDimens.ViewportButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.editor_change_image),
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                }
            }

            // Bottom Histogram overlay inside viewport
            if (isHistogramExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(AppDimens.SpacingSmall)
                ) {
                    SzlRgbHistogram(
                        bitmap = previewBitmap ?: sourceBitmap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.HistogramHeight)
                            .padding(AppDimens.SpacingMicro)
                    )
                }
            }
        }
    }
}
