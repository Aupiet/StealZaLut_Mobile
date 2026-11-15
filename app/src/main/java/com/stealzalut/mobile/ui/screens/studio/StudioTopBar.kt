package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors

/**
 * Modern compact TopBar for Studio steps (Step 1 to Step 5) when an image is loaded.
 * Styled symmetrically with StandaloneEditorScreen's top bar:
 * - Left: Back button (accessible 48dp touch target)
 * - Center: Compare / Mask Toggle Pill or Step Badge
 * - Right: Session actions dropdown menu (...) and Primary Action button
 */
@Composable
fun StudioTopBar(
    currentStep: Int,
    isZoneMaskActive: Boolean = false,
    canProceed: Boolean = true,
    isComputing: Boolean = false,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onToggleZoneMaskActive: () -> Unit = {},
    onSaveSession: () -> Unit = {},
    onRestoreSession: () -> Unit = {},
    onResetParameters: () -> Unit = {},
    onCloseSession: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(AppDimens.TopAppBarHeight),
        color = MaterialTheme.szlColors.photoDarkroom,
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button (accessible 48dp target)
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.actionBack,
                    contentDescription = stringResource(R.string.wizard_step_3_btn_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }

            // Center: Step Title or Indicator
            when (currentStep) {
                AppConstants.STEP_3_PARAMS -> {
                    Text(
                        text = stringResource(R.string.studio_step_3_lut_params_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                AppConstants.STEP_5_RESULT -> {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppDimens.ButtonHeightCompact)
                            .clickable { onToggleZoneMaskActive() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Icon(
                                imageVector = MaterialTheme.icons.actionVisibilityOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Text(
                                text = stringResource(R.string.editor_compare),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                else -> {
                    val stepTitle = when (currentStep) {
                        AppConstants.STEP_1_SOURCE -> stringResource(R.string.wizard_step_1_title)
                        AppConstants.STEP_2_STYLE -> stringResource(R.string.wizard_step_2_title)
                        AppConstants.STEP_4_COMPUTE -> stringResource(R.string.wizard_step_4_title)
                        else -> ""
                    }
                    val stepIcon = when (currentStep) {
                        AppConstants.STEP_1_SOURCE -> MaterialTheme.icons.actionAddPhoto
                        AppConstants.STEP_2_STYLE -> MaterialTheme.icons.toolColorMixer
                        AppConstants.STEP_4_COMPUTE -> MaterialTheme.icons.toolEffects
                        else -> MaterialTheme.icons.toolBasic
                    }
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = AppDimens.ButtonHeightCompact)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Text(
                                text = stepTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Right: Menu (...) & Primary Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.actionMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_menu_save_session)) },
                            onClick = {
                                showMenu = false
                                onSaveSession()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionSave,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_menu_restore_session)) },
                            onClick = {
                                showMenu = false
                                onRestoreSession()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionRefresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_menu_reset)) },
                            onClick = {
                                showMenu = false
                                onResetParameters()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionReset,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.editor_btn_close_image)) },
                            onClick = {
                                showMenu = false
                                onCloseSession()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionClose,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                            }
                        )
                    }
                }

                // Primary Amber Action Button (48dp target)
                val actionIcon = when (currentStep) {
                    AppConstants.STEP_1_SOURCE, AppConstants.STEP_2_STYLE -> MaterialTheme.icons.statusArrowForward
                    AppConstants.STEP_3_PARAMS -> MaterialTheme.icons.toolEffects
                    AppConstants.STEP_5_RESULT -> MaterialTheme.icons.statusCheck
                    else -> MaterialTheme.icons.statusArrowForward
                }
                val isButtonEnabled = when (currentStep) {
                    AppConstants.STEP_1_SOURCE, AppConstants.STEP_2_STYLE -> canProceed
                    AppConstants.STEP_4_COMPUTE -> false
                    else -> true
                }

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                    color = if (isButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(AppDimens.MinInteractiveTarget)
                        .clickable(enabled = isButtonEnabled) { onNext() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isComputing || currentStep == AppConstants.STEP_4_COMPUTE) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimens.IconSmall),
                                strokeWidth = AppDimens.BorderMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                tint = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(AppDimens.IconNormal)
                            )
                        }
                    }
                }
            }
        }
    }
}
