package com.stealzalut.mobile.ui.screens.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlImageBadge
import com.stealzalut.mobile.ui.components.SzlRgbHistogram
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors

/**
 * High-end Darkroom Photo Viewport inspired by Lightroom and Halide.
 * Features:
 * - Neutral darkroom canvas background (#0E0E11) preventing color perception skew.
 * - Minimalist floating translucent frosted-glass control strip.
 * - Compact floating animated Glassmorphism RGB Histogram overlay.
 * - Tactile hold-to-compare with high-visibility original badge.
 * - Synchronized 1x..4x pinch-to-zoom and pan.
 */
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
    var isHistogramEnlarged by remember { mutableStateOf(false) }

    val displayBitmap = if (isHoldingCompare && sourceBitmap != null) sourceBitmap else (previewBitmap ?: sourceBitmap)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.szlColors.photoDarkroom)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
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
        // --- 1. Photo Canvas with Darkroom Rounded Frame (Figma parity) ---
        displayBitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }
        }

        // --- 2. Minimalist Translucent Floating Top Toolbar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingSmall)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pill: Active LUT indicator
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    ColorCardReference.WhiteStep.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.SpacingSmall)
                            .clip(CircleShape)
                            .background(if (lutName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = lutName ?: stringResource(R.string.editor_no_lut_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Pill Group: Histogram, Import Photo
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    ColorCardReference.WhiteStep.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingMicro),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Histogram Toggle Button
                    IconButton(
                        onClick = onToggleHistogram,
                        modifier = Modifier.size(AppDimens.ViewportButtonSize)
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.statusHistogram,
                            contentDescription = stringResource(R.string.section_histogram),
                            tint = if (isHistogramExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                    }

                    // Change Photo Button
                    IconButton(
                        onClick = onChangePhoto,
                        modifier = Modifier.size(AppDimens.ViewportButtonSize)
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.actionAddPhoto,
                            contentDescription = stringResource(R.string.editor_change_image),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                    }
                }
            }
        }

        // --- 3. Hold-to-Compare Feedback Overlay ---
        if (isHoldingCompare) {
            SzlImageBadge(
                text = stringResource(R.string.editor_compare_original_badge),
                alignment = Alignment.TopCenter,
                isCompareMode = true
            )
        } else {
            // Subtle unobtrusive hint pill at the bottom
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.SpacingSmall)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMicro),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionCompare,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconMicro),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                    Text(
                        text = stringResource(R.string.editor_hold_compare),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- 4. Processing Progress Spinner ---
        if (isProcessing && !isComputingAutoEdit) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(AppDimens.AvatarSize),
                strokeWidth = AppDimens.BorderMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- 5. Compact Floating Animated Glassmorphism RGB Histogram (TopStart parity) ---
        AnimatedVisibility(
            visible = isHistogramExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = AppDimens.SpacingNormal, top = AppDimens.TopAppBarHeight + AppDimens.SpacingSmall)
        ) {
            SzlRgbHistogram(
                bitmap = previewBitmap ?: sourceBitmap,
                isEnlarged = isHistogramEnlarged,
                onClick = { isHistogramEnlarged = !isHistogramEnlarged }
            )
        }
    }
}
