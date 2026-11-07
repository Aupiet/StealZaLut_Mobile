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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens

data class EditorToolItem(
    val id: Int,
    val icon: ImageVector,
    @param:StringRes val labelRes: Int
)

/**
 * Ergonomic one-hand Lightroom-style bottom tool bar.
 * Features:
 * - Animated tactile pills with smooth color and elevation transitions.
 * - Glowing modification dot indicators showing which panels have active edits.
 * - Full WCAG-compliant touch targets with horizontal fluid scrolling.
 */
@Composable
fun EditorToolbar(
    activeTool: Int,
    onSelectTool: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeModifications: Set<Int> = emptySet()
) {
    val scrollState = rememberScrollState()

    val tools = listOf(
        EditorToolItem(0, Icons.Default.Tune, R.string.section_basic),
        EditorToolItem(1, Icons.AutoMirrored.Filled.ShowChart, R.string.section_tone_curve),
        EditorToolItem(2, Icons.Default.Palette, R.string.section_color_mixer),
        EditorToolItem(3, Icons.Default.ColorLens, R.string.section_color_grading),
        EditorToolItem(4, Icons.Default.Details, R.string.section_detail),
        EditorToolItem(5, Icons.Default.AutoAwesome, R.string.section_effects),
        EditorToolItem(6, Icons.Default.Tune, R.string.section_calibration)
    )

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
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tools.forEach { tool ->
                val isSelected = activeTool == tool.id
                val hasModifications = tool.id in activeModifications

                val targetBgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
                val targetContentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                val animatedBg by animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "toolBg" // GUARD_ALLOW
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "toolContent" // GUARD_ALLOW
                )

                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .background(animatedBg)
                        .clickable { onSelectTool(tool.id) }
                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                tint = animatedContentColor,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                            Text(
                                text = stringResource(tool.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = animatedContentColor
                            )
                        }

                        // Glowing modification indicator dot under the tool label
                        if (hasModifications) {
                            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SpacingExtraSmall)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
