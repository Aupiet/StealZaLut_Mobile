package com.stealzalut.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens

/**
 * Standard Collapsible Accordion Section with Section Bypass (Eye icon),
 * Title, Expand/Collapse Arrow, and optional Reset button.
 */
@Composable
fun SzlAccordionSection(
    title: String,
    isExpanded: Boolean,
    isSectionEnabled: Boolean,
    onToggleExpand: () -> Unit,
    onToggleBypass: () -> Unit,
    onResetSection: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.CornerRadiusMedium)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget)
                    .clickable { onToggleExpand() }
                    .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Eye Toggle Bypass
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleBypass,
                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                    ) {
                        Icon(
                            imageVector = if (isSectionEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(R.string.desc_toggle_section),
                            tint = if (isSectionEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppConstants.OPACITY_DISABLED_LABEL)
                            },
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSectionEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppConstants.OPACITY_DISABLED_LABEL)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onToggleExpand() }
                    )
                }

                // Right: Optional Reset + Expand Arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro)
                ) {
                    if (onResetSection != null && isExpanded) {
                        IconButton(
                            onClick = onResetSection,
                            modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.btn_reset_section),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowLeft,
                            contentDescription = if (isExpanded) {
                                stringResource(R.string.desc_collapse_section)
                            } else {
                                stringResource(R.string.desc_expand_section)
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall)
                        .alpha(if (isSectionEnabled) 1.0f else AppConstants.OPACITY_DISABLED_CONTAINER)
                ) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_DIVIDER),
                        thickness = AppDimens.DividerThickness,
                        modifier = Modifier.padding(bottom = AppDimens.SpacingSmall)
                    )
                    content()
                }
            }
        }
    }
}
