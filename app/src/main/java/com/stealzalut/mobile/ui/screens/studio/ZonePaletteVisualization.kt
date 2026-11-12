package com.stealzalut.mobile.ui.screens.studio

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ZoneInfo
import com.stealzalut.mobile.data.model.ZoneLink
import com.stealzalut.mobile.data.model.ZonePaletteAnalysis
import com.stealzalut.mobile.data.model.ZoneTransferSummary
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.SzlTheme

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

private const val SPARSE_ALPHA = 0.35f

/**
 * Full "Zone Colours" feature widget — CorrespondenceView + ZoneInspector.
 */
@Composable
fun ZonePaletteVisualization(
    source: ZonePaletteAnalysis?,
    reference: ZonePaletteAnalysis?,
    zoneTransfer: ZoneTransferSummary? = null,
    selectedZoneIndex: Int = -1,
    maskPath: String? = null,
    maskMode: String = "bw",
    onZoneCorrespondenceChanged: (srcIdx: Int, refIdx: Int) -> Unit = { _, _ -> },
    onResetCorrespondences: () -> Unit = {},
    onZoneSelected: (zoneIndex: Int) -> Unit = {},
    onToggleMaskMode: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = AppDimens.SpacingSpacious
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(stringResource(R.string.zone_tab_correspondences)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(stringResource(R.string.zone_tab_inspector)) }
            )
        }

        Spacer(Modifier.height(AppDimens.SpacingMedium))

        when (activeTab) {
            0 -> CorrespondenceGraphTab(
                source = zoneTransfer?.sourceZones ?: emptyList(),
                reference = zoneTransfer?.referenceZones ?: emptyList(),
                correspondence = zoneTransfer?.correspondence ?: emptyList(),
                userOverrides = zoneTransfer?.userOverrides ?: emptyMap(),
                onLinkChanged = onZoneCorrespondenceChanged,
                onReset = onResetCorrespondences
            )
            1 -> ZoneInspectorTab(
                source = source,
                zoneTransfer = zoneTransfer,
                selectedZoneIndex = selectedZoneIndex,
                maskPath = maskPath,
                maskMode = maskMode,
                onZoneSelected = onZoneSelected,
                onToggleMaskMode = onToggleMaskMode
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 0 — Correspondence Graph
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CorrespondenceGraphTab(
    source: List<ZoneInfo>,
    reference: List<ZoneInfo>,
    correspondence: List<ZoneLink>,
    userOverrides: Map<String, Int>,
    onLinkChanged: (srcIdx: Int, refIdx: Int) -> Unit,
    onReset: () -> Unit
) {
    if (source.isEmpty() || reference.isEmpty()) {
        ZonePlaceholder(stringResource(R.string.zone_placeholder_load))
        return
    }

    var dragSrcId by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }

    val colorAuto = MaterialTheme.colorScheme.primary
    val colorCrossed = SzlTheme.colors.warning
    val colorDrag = SzlTheme.colors.toolActiveDot
    val nodeBorder = MaterialTheme.colorScheme.outline
    val nodeSelected = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    val nodeWidthDp = AppDimens.ZoneNodeWidth
    val nodeHeightDp = AppDimens.ZoneNodeHeight
    val nodeRadiusDp = AppDimens.CornerRadiusMedium
    val marginDp = AppDimens.SpacingSpacious
    val gapDp = AppDimens.SpacingSmall
    val minStrokeDp = AppDimens.ZoneCurveStrokeMin
    val maxStrokeDp = AppDimens.ZoneCurveStrokeMax
    val dragStrokeDp = AppDimens.ZoneCurveDragStroke
    val borderMediumDp = AppDimens.BorderMedium
    val borderSelectedDp = AppDimens.ZoneBorderSelected

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppDimens.ZoneGraphMinHeight)
                .background(surfaceColor, RoundedCornerShape(AppDimens.CornerRadiusCard))
                .padding(AppDimens.SpacingMedium)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(source, reference, correspondence) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val srcRects = computeNodeRects(
                                    zones = source,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat(),
                                    side = Side.SOURCE,
                                    nodeWidthPx = nodeWidthDp.toPx(),
                                    nodeHeightPx = nodeHeightDp.toPx(),
                                    marginPx = marginDp.toPx(),
                                    gapPx = gapDp.toPx()
                                )
                                dragSrcId = srcRects.entries.firstOrNull { (_, r) ->
                                    offset.x >= r.left && offset.x <= r.right &&
                                            offset.y >= r.top && offset.y <= r.bottom
                                }?.key ?: -1
                            },
                            onDrag = { change, _ ->
                                if (dragSrcId >= 0) dragPos = change.position
                            },
                            onDragEnd = {
                                if (dragSrcId >= 0 && dragPos != null) {
                                    val refRects = computeNodeRects(
                                        zones = reference,
                                        canvasWidth = size.width.toFloat(),
                                        canvasHeight = size.height.toFloat(),
                                        side = Side.REFERENCE,
                                        nodeWidthPx = nodeWidthDp.toPx(),
                                        nodeHeightPx = nodeHeightDp.toPx(),
                                        marginPx = marginDp.toPx(),
                                        gapPx = gapDp.toPx()
                                    )
                                    val hit = refRects.entries.firstOrNull { (_, r) ->
                                        val p = dragPos
                                        p != null && p.x >= r.left && p.x <= r.right && p.y >= r.top && p.y <= r.bottom
                                    }
                                    if (hit != null) onLinkChanged(dragSrcId, hit.key)
                                }
                                dragSrcId = -1
                                dragPos = null
                            },
                            onDragCancel = { dragSrcId = -1; dragPos = null }
                        )
                    }
            ) {
                val nodeWidthPx = nodeWidthDp.toPx()
                val nodeHeightPx = nodeHeightDp.toPx()
                val marginPx = marginDp.toPx()
                val gapPx = gapDp.toPx()
                val radiusPx = nodeRadiusDp.toPx()

                val srcRects = computeNodeRects(source, size.width, size.height, Side.SOURCE, nodeWidthPx, nodeHeightPx, marginPx, gapPx)
                val refRects = computeNodeRects(reference, size.width, size.height, Side.REFERENCE, nodeWidthPx, nodeHeightPx, marginPx, gapPx)

                // ── Draw Bézier links ─────────────────────────────────────────
                for (link in correspondence) {
                    val srcRect = srcRects[link.sourceZone] ?: continue
                    val primaryRefId = link.primaryTarget
                    val refRect = refRects[primaryRefId] ?: continue

                    val isDiagonal = (link.sourceZone == primaryRefId)
                    val isOverridden = link.isOverridden || link.origin == "user"
                    val weight = link.similarity.coerceIn(0f, 1f)
                    val strokePx = minStrokeDp.toPx() + (maxStrokeDp.toPx() - minStrokeDp.toPx()) * weight

                    val curveColor = when {
                        isOverridden || !isDiagonal -> colorCrossed
                        else -> colorAuto
                    }

                    drawBezierLink(
                        start = Offset(srcRect.right, (srcRect.top + srcRect.bottom) / 2f),
                        end = Offset(refRect.left, (refRect.top + refRect.bottom) / 2f),
                        color = curveColor,
                        strokePx = strokePx,
                        dashed = weight < 0.3f
                    )
                }

                // ── Draw active drag curve ────────────────────────────────────
                val dp = dragPos
                if (dragSrcId >= 0 && dp != null) {
                    val srcRect = srcRects[dragSrcId]
                    if (srcRect != null) {
                        drawBezierLink(
                            start = Offset(srcRect.right, (srcRect.top + srcRect.bottom) / 2f),
                            end = dp,
                            color = colorDrag,
                            strokePx = dragStrokeDp.toPx(),
                            dashed = false
                        )
                    }
                }

                // ── Draw source nodes ─────────────────────────────────────────
                for (zone in source) {
                    val rect = srcRects[zone.id] ?: continue
                    val zoneFill = zone.hex.toComposeColor(surfaceColor)
                    val alpha = if (zone.area < 0.01f) SPARSE_ALPHA else 1f
                    val borderColor = if (zone.id == dragSrcId) nodeSelected else nodeBorder
                    val borderStroke = if (zone.id == dragSrcId) borderSelectedDp.toPx() else borderMediumDp.toPx()
                    drawZoneNode(rect, zoneFill, borderColor, borderStroke, radiusPx, alpha)
                }

                // ── Draw reference nodes ──────────────────────────────────────
                for (zone in reference) {
                    val rect = refRects[zone.id] ?: continue
                    val zoneFill = zone.hex.toComposeColor(surfaceColor)
                    val alpha = if (zone.area < 0.01f) SPARSE_ALPHA else 1f
                    drawZoneNode(rect, zoneFill, nodeBorder, borderMediumDp.toPx(), radiusPx, alpha)
                }
            }
        }

        Spacer(Modifier.height(AppDimens.SpacingMedium))

        // Reset button
        if (userOverrides.isNotEmpty()) {
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.zone_reset_correspondences))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1 — Zone Inspector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZoneInspectorTab(
    source: ZonePaletteAnalysis?,
    zoneTransfer: ZoneTransferSummary?,
    selectedZoneIndex: Int,
    maskPath: String?,
    maskMode: String,
    onZoneSelected: (Int) -> Unit,
    onToggleMaskMode: () -> Unit
) {
    if (source == null) {
        ZonePlaceholder(stringResource(R.string.zone_placeholder_load))
        return
    }

    val zones = source.zones
    val activeLink = if (selectedZoneIndex >= 0 && zoneTransfer != null) {
        zoneTransfer.correspondence.getOrNull(selectedZoneIndex)
    } else null

    var zoneStrength by remember(selectedZoneIndex) { mutableFloatStateOf(activeLink?.strength ?: 1.0f) }
    var zoneChroma by remember(selectedZoneIndex) { mutableFloatStateOf(1.0f) }
    var zoneLumaShift by remember(selectedZoneIndex) { mutableFloatStateOf(0.0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpacingSpacious),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // ── 7-card Zone Selection Band ────────────────────────────────────
        Text(
            stringResource(R.string.zone_inspector_select_zone),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            zones.forEachIndexed { index, zone ->
                val isSelected = index == selectedZoneIndex
                val zoneFill = zone.hex.toComposeColor(MaterialTheme.colorScheme.surfaceVariant)
                val textColor = if (zone.hex.luminance() > 0.5f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val pctText = "${(zone.area * 100f).toInt()}%"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                        .background(zoneFill)
                        .border(
                            width = if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                        )
                        .alpha(if (zone.area < 0.01f) SPARSE_ALPHA else 1f)
                        .clickable { onZoneSelected(index) }
                        .padding(AppDimens.SpacingExtraSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pctText, // GUARD_ALLOW
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        // ── Soft Weight Mask Preview (B&W or Color Heatmap) ───────────────
        if (selectedZoneIndex >= 0 && maskPath != null) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                Text(
                    stringResource(R.string.zone_inspector_mask_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.ZoneMaskPreviewHeight)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusCard))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val bitmap = remember(maskPath) {
                        try { BitmapFactory.decodeFile(maskPath) } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.zone_inspector_mask_preview),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    FilledTonalButton(onClick = onToggleMaskMode) {
                        Text(
                            if (maskMode == "bw")
                                stringResource(R.string.zone_mask_mode_color)
                            else
                                stringResource(R.string.zone_mask_mode_bw)
                        )
                    }
                }
            }
        }

        // ── Transformation Description & Sliders per Zone ─────────────────
        if (activeLink != null) {
            val selectedZone = zones.getOrNull(selectedZoneIndex)
            if (selectedZone != null) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    Text(
                        stringResource(
                            R.string.zone_inspector_zone_title,
                            (selectedZone.area * 100f).toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val changeText = activeLink.describeChange.ifEmpty {
                        when (activeLink.origin) {
                            "user" -> stringResource(R.string.zone_link_origin_user)
                            "ai"   -> stringResource(R.string.zone_link_origin_ai)
                            else   -> stringResource(R.string.zone_link_origin_auto)
                        }
                    }

                    Text(
                        text = changeText, // GUARD_ALLOW
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeLink.isOverridden) {
                        Text(
                            stringResource(R.string.zone_link_overridden),
                            style = MaterialTheme.typography.bodySmall,
                            color = SzlTheme.colors.warning
                        )
                    }

                    // Sliders for targeted zone overrides
                    SzlSlider(
                        title = stringResource(R.string.zone_slider_strength_title),
                        value = zoneStrength,
                        onValueChange = { zoneStrength = it },
                        min = 0f,
                        max = 1f,
                        defaultValue = 1f
                    )

                    SzlSlider(
                        title = stringResource(R.string.zone_slider_chroma_title),
                        value = zoneChroma,
                        onValueChange = { zoneChroma = it },
                        min = 0f,
                        max = 1f,
                        defaultValue = 1f
                    )

                    SzlBipolarSlider(
                        title = stringResource(R.string.zone_slider_luma_title),
                        value = zoneLumaShift,
                        onValueChange = { zoneLumaShift = it },
                        min = -1f,
                        max = 1f,
                        defaultValue = 0f
                    )
                }
            }
        }

        if (selectedZoneIndex < 0) {
            Text(
                stringResource(R.string.zone_inspector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legacy overload
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ZonePaletteVisualization(
    source: ZonePaletteAnalysis?,
    reference: ZonePaletteAnalysis?
) {
    ZonePaletteVisualization(
        source = source,
        reference = reference,
        zoneTransfer = null
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Placeholder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZonePlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.ZonePlaceholderHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message, // GUARD_ALLOW
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Geometry helpers
// ─────────────────────────────────────────────────────────────────────────────

private enum class Side { SOURCE, REFERENCE }

private data class NodeRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun computeNodeRects(
    zones: List<ZoneInfo>,
    canvasWidth: Float,
    canvasHeight: Float,
    side: Side,
    nodeWidthPx: Float,
    nodeHeightPx: Float,
    marginPx: Float,
    gapPx: Float
): Map<Int, NodeRect> {
    val k = zones.size
    if (k == 0) return emptyMap()
    val totalHeight = k * nodeHeightPx + (k - 1) * gapPx
    val startY = (canvasHeight - totalHeight) / 2f
    val x = if (side == Side.SOURCE) marginPx else canvasWidth - marginPx - nodeWidthPx
    val result = mutableMapOf<Int, NodeRect>()
    zones.forEachIndexed { index, zone ->
        val top = startY + index * (nodeHeightPx + gapPx)
        result[zone.id] = NodeRect(x, top, x + nodeWidthPx, top + nodeHeightPx)
    }
    return result
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas draw helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawBezierLink(
    start: Offset,
    end: Offset,
    color: Color,
    strokePx: Float,
    dashed: Boolean
) {
    val dx = ((end.x - start.x) * 0.45f).coerceAtLeast(20f)
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + dx, start.y,
            end.x - dx, end.y,
            end.x, end.y
        )
    }
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = effect
        )
    )
}

private fun DrawScope.drawZoneNode(
    rect: NodeRect,
    fill: Color,
    border: Color,
    borderStrokePx: Float,
    radiusPx: Float,
    alpha: Float
) {
    val half = borderStrokePx / 2f
    val adjustedLeft = rect.left + half
    val adjustedTop = rect.top + half
    val adjustedRight = rect.right - half
    val adjustedBottom = rect.bottom - half
    val w = adjustedRight - adjustedLeft
    val h = adjustedBottom - adjustedTop

    drawRoundRect(
        color = fill.copy(alpha = fill.alpha * alpha),
        topLeft = Offset(adjustedLeft, adjustedTop),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx)
    )
    drawRoundRect(
        color = border.copy(alpha = alpha),
        topLeft = Offset(adjustedLeft, adjustedTop),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx),
        style = Stroke(width = borderStrokePx)
    )
}

private fun String.toComposeColor(fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(this))
} catch (_: Exception) {
    fallback
}

private fun String.luminance(): Float = try {
    val c = android.graphics.Color.parseColor(this)
    val r = android.graphics.Color.red(c) / 255f
    val g = android.graphics.Color.green(c) / 255f
    val b = android.graphics.Color.blue(c) / 255f
    0.2126f * r + 0.7152f * g + 0.0722f * b
} catch (_: Exception) {
    0.5f
}
