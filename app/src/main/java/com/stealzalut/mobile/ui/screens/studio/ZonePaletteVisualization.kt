package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ZoneDescriptor
import com.stealzalut.mobile.data.model.ZoneInfo
import com.stealzalut.mobile.data.model.ZoneLink
import com.stealzalut.mobile.data.model.ZonePaletteAnalysis
import com.stealzalut.mobile.data.model.ZoneTransferSummary
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.SzlTheme
import com.stealzalut.mobile.ui.theme.icons

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

private const val SPARSE_ALPHA = 0.35f

private val DEFAULT_ZONE_NAMES = listOf(
    "Deep Sh.", "Shadows", "Low Mids", "Midtones", "High Mids", "Highlights", "Whites"
)

private fun ZoneDescriptor.toZoneInfo(idx: Int): ZoneInfo = ZoneInfo(
    id = id,
    hex = hex,
    area = area,
    lRank = lRank,
    meanOklab = mean,
    name = DEFAULT_ZONE_NAMES.getOrElse(idx) { "Zone $idx" },
    pixelPercentage = area * 100f
)

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
    isAnalyzingZones: Boolean = false,
    isGeneratingZoneMask: Boolean = false,
    onZoneCorrespondenceChanged: (srcIdx: Int, refIdx: Int) -> Unit = { _, _ -> },
    onResetCorrespondences: () -> Unit = {},
    onZoneSelected: (zoneIndex: Int) -> Unit = {},
    onToggleMaskMode: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) }

    val effectiveSourceZones = remember(zoneTransfer, source) {
        if (zoneTransfer?.sourceZones?.isNotEmpty() == true) {
            zoneTransfer.sourceZones
        } else {
            source?.zones?.mapIndexed { i, d -> d.toZoneInfo(i) } ?: emptyList()
        }
    }
    val effectiveRefZones = remember(zoneTransfer, reference) {
        if (zoneTransfer?.referenceZones?.isNotEmpty() == true) {
            zoneTransfer.referenceZones
        } else {
            reference?.zones?.mapIndexed { i, d -> d.toZoneInfo(i) } ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = activeTab
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

        if (isAnalyzingZones) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.ZonePlaceholderHeight),
                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
                ),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.IconLarge),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(AppDimens.SpacingMedium))
                    Text(
                        text = stringResource(R.string.studio_zones_analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (activeTab) {
                0 -> CorrespondenceGraphTab(
                    source = effectiveSourceZones,
                    reference = effectiveRefZones,
                    correspondence = zoneTransfer?.correspondence ?: emptyList(),
                    userOverrides = zoneTransfer?.userOverrides ?: emptyMap(),
                    onLinkChanged = onZoneCorrespondenceChanged,
                    onReset = onResetCorrespondences
                )
                1 -> ZoneInspectorTab(
                    zones = effectiveSourceZones,
                    zoneTransfer = zoneTransfer,
                    selectedZoneIndex = selectedZoneIndex,
                    maskPath = maskPath,
                    maskMode = maskMode,
                    isGeneratingZoneMask = isGeneratingZoneMask,
                    onZoneSelected = onZoneSelected,
                    onToggleMaskMode = onToggleMaskMode
                )
            }
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
    val marginDp = AppDimens.SpacingMedium
    val gapDp = AppDimens.SpacingSmall
    val minStrokeDp = AppDimens.ZoneCurveStrokeMin
    val maxStrokeDp = AppDimens.ZoneCurveStrokeMax
    val dragStrokeDp = AppDimens.ZoneCurveDragStroke
    val borderMediumDp = AppDimens.BorderMedium
    val borderSelectedDp = AppDimens.ZoneBorderSelected

    val density = LocalDensity.current
    val nodeWidthPx = with(density) { nodeWidthDp.toPx() }
    val nodeHeightPx = with(density) { nodeHeightDp.toPx() }
    val marginPx = with(density) { marginDp.toPx() }
    val gapPx = with(density) { gapDp.toPx() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ZoneGraphMinHeight)
                .clipToBounds()
                .background(surfaceColor, RoundedCornerShape(AppDimens.CornerRadiusCard))
                .padding(AppDimens.SpacingSmall)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(source, reference, correspondence) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val totalH = source.size * nodeHeightPx + (source.size - 1) * gapPx
                                val startY = (size.height - totalH) / 2f
                                if (offset.x in marginPx..(marginPx + nodeWidthPx)) {
                                    val relY = offset.y - startY
                                    val idx = (relY / (nodeHeightPx + gapPx)).toInt()
                                    if (idx in source.indices) {
                                        val nodeTop = startY + idx * (nodeHeightPx + gapPx)
                                        if (offset.y in nodeTop..(nodeTop + nodeHeightPx)) {
                                            dragSrcId = idx
                                            dragPos = offset
                                        }
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                if (dragSrcId >= 0) dragPos = change.position
                            },
                            onDragEnd = {
                                if (dragSrcId >= 0 && dragPos != null) {
                                    val dp = dragPos!!
                                    val totalH = reference.size * nodeHeightPx + (reference.size - 1) * gapPx
                                    val startY = (size.height - totalH) / 2f
                                    val refLeft = size.width - marginPx - nodeWidthPx
                                    if (dp.x in (refLeft - marginPx)..(size.width.toFloat())) {
                                        val relY = dp.y - startY
                                        val idx = (relY / (nodeHeightPx + gapPx)).toInt()
                                        if (idx in reference.indices) {
                                            onLinkChanged(dragSrcId, idx)
                                        }
                                    }
                                }
                                dragSrcId = -1
                                dragPos = null
                            },
                            onDragCancel = {
                                dragSrcId = -1
                                dragPos = null
                            }
                        )
                    }
            ) {
                val totalH = source.size * nodeHeightPx + (source.size - 1) * gapPx
                val startY = (size.height - totalH) / 2f

                // Draw Bézier links
                for (link in correspondence) {
                    val sIdx = link.sourceZone
                    val rIdx = link.primaryTarget
                    if (sIdx in source.indices && rIdx in reference.indices) {
                        val startPt = Offset(marginPx + nodeWidthPx, startY + sIdx * (nodeHeightPx + gapPx) + nodeHeightPx / 2f)
                        val endPt = Offset(size.width - marginPx - nodeWidthPx, startY + rIdx * (nodeHeightPx + gapPx) + nodeHeightPx / 2f)

                        val isDiagonal = (sIdx == rIdx)
                        val isOverridden = link.isOverridden || link.origin == "user"
                        val weight = link.similarity.coerceIn(0f, 1f)
                        val strokePx = minStrokeDp.toPx() + (maxStrokeDp.toPx() - minStrokeDp.toPx()) * weight
                        val curveColor = when {
                            isOverridden || !isDiagonal -> colorCrossed
                            else -> colorAuto
                        }
                        drawBezierLink(startPt, endPt, curveColor, strokePx, weight < 0.3f)
                    }
                }

                // Draw active drag line
                val dp = dragPos
                if (dragSrcId in source.indices && dp != null) {
                    val startPt = Offset(marginPx + nodeWidthPx, startY + dragSrcId * (nodeHeightPx + gapPx) + nodeHeightPx / 2f)
                    drawBezierLink(startPt, dp, colorDrag, dragStrokeDp.toPx(), false)
                }
            }

            // Left: Source nodes
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = marginDp),
                verticalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                source.forEachIndexed { idx, zone ->
                    val zoneFill = zone.hex.toComposeColor(surfaceColor)
                    val isSparse = zone.area < 0.01f
                    val isDraggingThis = dragSrcId == idx
                    val textColor = if (zone.hex.luminance() > 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface

                    Surface(
                        shape = RoundedCornerShape(nodeRadiusDp),
                        color = zoneFill.copy(alpha = if (isSparse) SPARSE_ALPHA else 1f),
                        border = BorderStroke(
                            if (isDraggingThis) borderSelectedDp else borderMediumDp,
                            if (isDraggingThis) nodeSelected else nodeBorder.copy(alpha = if (isSparse) SPARSE_ALPHA else 0.8f)
                        ),
                        modifier = Modifier
                            .width(nodeWidthDp)
                            .height(nodeHeightDp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AppDimens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DEFAULT_ZONE_NAMES.getOrElse(idx) { zone.name },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(zone.area * 100f).toInt()}%", // GUARD_ALLOW
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Right: Reference nodes
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = marginDp),
                verticalArrangement = Arrangement.spacedBy(gapDp)
            ) {
                reference.forEachIndexed { idx, zone ->
                    val zoneFill = zone.hex.toComposeColor(surfaceColor)
                    val isSparse = zone.area < 0.01f
                    val textColor = if (zone.hex.luminance() > 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface

                    Surface(
                        shape = RoundedCornerShape(nodeRadiusDp),
                        color = zoneFill.copy(alpha = if (isSparse) SPARSE_ALPHA else 1f),
                        border = BorderStroke(
                            borderMediumDp,
                            nodeBorder.copy(alpha = if (isSparse) SPARSE_ALPHA else 0.8f)
                        ),
                        modifier = Modifier
                            .width(nodeWidthDp)
                            .height(nodeHeightDp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AppDimens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DEFAULT_ZONE_NAMES.getOrElse(idx) { zone.name },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(zone.area * 100f).toInt()}%", // GUARD_ALLOW
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(AppDimens.SpacingSmall))

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
    zones: List<ZoneInfo>,
    zoneTransfer: ZoneTransferSummary?,
    selectedZoneIndex: Int,
    maskPath: String?,
    maskMode: String,
    isGeneratingZoneMask: Boolean = false,
    onZoneSelected: (Int) -> Unit,
    onToggleMaskMode: () -> Unit
) {
    if (zones.isEmpty()) {
        ZonePlaceholder(stringResource(R.string.zone_placeholder_load))
        return
    }

    val activeLink = if (selectedZoneIndex in zones.indices && zoneTransfer != null) {
        zoneTransfer.correspondence.getOrNull(selectedZoneIndex)
    } else null

    var zoneStrength by remember(selectedZoneIndex) { mutableFloatStateOf(activeLink?.strength ?: 1.0f) }
    var zoneChroma by remember(selectedZoneIndex) { mutableFloatStateOf(1.0f) }
    var zoneLumaShift by remember(selectedZoneIndex) { mutableFloatStateOf(0.0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // ── 7-card Zone Selection Band ────────────────────────────────────
        Text(
            stringResource(R.string.zone_inspector_select_zone),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
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
                val shortName = DEFAULT_ZONE_NAMES.getOrElse(index) { zone.name }

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = zoneFill,
                    border = BorderStroke(
                        width = if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.ButtonHeightCompact)
                        .alpha(if (zone.area < 0.01f) SPARSE_ALPHA else 1f)
                        .clickable { onZoneSelected(index) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.SpacingMicro, horizontal = AppDimens.SpacingExtraSmall),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = pctText, // GUARD_ALLOW
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ── Selected Zone Details & Controls ──────────────────────────────
        if (selectedZoneIndex in zones.indices) {
            val selectedZone = zones[selectedZoneIndex]
            val pctText = "${(selectedZone.area * 100f).toInt()}%"

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.IconNormal)
                                    .clip(CircleShape)
                                    .background(selectedZone.hex.toComposeColor(MaterialTheme.colorScheme.primary))
                                    .border(AppDimens.BorderThin, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Text(
                                text = "${DEFAULT_ZONE_NAMES.getOrElse(selectedZoneIndex) { "Zone" }} ($pctText)", // GUARD_ALLOW
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = onToggleMaskMode,
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.ButtonHeightCompact),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill)
                        ) {
                            Icon(
                                imageVector = MaterialTheme.icons.actionVisibilityOn,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                            Text(
                                text = if (maskMode == "bw") "Masque N&B" else "Masque Couleur", // GUARD_ALLOW
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val changeText = activeLink?.describeChange?.ifEmpty { null }
                        ?: selectedZone.describeZone.ifEmpty { null }
                        ?: "${DEFAULT_ZONE_NAMES.getOrElse(selectedZoneIndex) { "Zone" }} · Teinte et luminance analysées" // GUARD_ALLOW

                    Text(
                        text = changeText, // GUARD_ALLOW
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Zone override sliders
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
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
        } else {
            Text(
                text = stringResource(R.string.zone_inspector_hint),
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
