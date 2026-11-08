package com.stealzalut.mobile.ui.screens.editor

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.SzlIconSet
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors

data class EditorToolItem(
    val id: Int,
    val iconProvider: (SzlIconSet) -> ImageVector,
    @param:StringRes val labelRes: Int
)

/**
 * Ergonomic one-hand Lightroom-style bottom tool bar conforming to Figma [APRÈS] Éditeur / Viewport.
 * Features:
 * - 10 dedicated photo develop tools with vertical icon-over-short-label layout.
 * - Fixed 68dp item width showing 5 tools + peeking 6th tool on standard 390dp screens.
 * - Active amber indicator pill and modified status dot indicator.
 * - Full WCAG-compliant touch targets (>= 48dp) with horizontal fluid scrolling.
 */
@Composable
fun EditorToolbar(
    activeTool: Int,
    onSelectTool: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeModifications: Set<Int> = emptySet()
) {
    val scrollState = rememberScrollState()
    val iconSet = MaterialTheme.icons

    val tools = remember {
        listOf(
            EditorToolItem(0, { it.toolCrop }, R.string.tool_label_crop),
            EditorToolItem(1, { it.toolBasic }, R.string.tool_label_basic),
            EditorToolItem(2, { it.toolToneCurve }, R.string.tool_label_curves),
            EditorToolItem(3, { it.toolColorMixer }, R.string.tool_label_hsl),
            EditorToolItem(4, { it.toolColorGrading }, R.string.tool_label_3way),
            EditorToolItem(5, { it.toolDetail }, R.string.tool_label_detail),
            EditorToolItem(6, { it.toolEffects }, R.string.tool_label_effects),
            EditorToolItem(7, { it.toolCalibration }, R.string.tool_label_calibration),
            EditorToolItem(8, { it.toolGeometry }, R.string.tool_label_geometry),
            EditorToolItem(9, { it.toolLens }, R.string.tool_label_optics)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
        border = androidx.compose.foundation.BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tools.forEach { tool ->
                val isSelected = activeTool == tool.id
                val hasModifications = tool.id in activeModifications

                val targetBgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    Color.Transparent
                }
                val targetContentColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                val animatedBg by animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(durationMillis = 180),
                    label = "toolBg" // GUARD_ALLOW
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = tween(durationMillis = 180),
                    label = "toolContent" // GUARD_ALLOW
                )

                Column(
                    modifier = Modifier
                        .width(AppDimens.EditorToolItemWidth)
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
                        .background(animatedBg)
                        .clickable { onSelectTool(tool.id) }
                        .padding(horizontal = AppDimens.SpacingMicro, vertical = AppDimens.SpacingExtraSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = tool.iconProvider(iconSet),
                            contentDescription = stringResource(tool.labelRes),
                            tint = animatedContentColor,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        if (hasModifications) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingSmall)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.szlColors.toolActiveDot)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                    Text(
                        text = stringResource(tool.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = animatedContentColor
                    )
                }
            }
        }
    }
}
