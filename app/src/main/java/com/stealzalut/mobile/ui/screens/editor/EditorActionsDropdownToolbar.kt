package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.icons

/**
 * Vertical dropdown actions toolbar located in the top-left of StandaloneEditorScreen.
 * Replaces the heavy bottom bar and provides quick access to:
 * - Auto-Edit (AI)
 * - Presets & LUTs Library
 * - Save Preset
 * - Export HD Photo (modal options FE-013)
 * - Export .CUBE LUT
 * - Reset All adjustments
 */
@Composable
fun EditorActionsDropdownToolbar(
    isExportingImage: Boolean,
    isExportingCube: Boolean,
    onExportHd: () -> Unit,
    onExportCube: () -> Unit,
    onSavePreset: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
    onAutoEditAi: () -> Unit = {},
    onOpenPresets: () -> Unit = {},
    onOpenGuide: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val icons = MaterialTheme.icons

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(AppDimens.MinInteractiveTarget)
        ) {
            if (isExportingImage || isExportingCube) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.IconSmall),
                    strokeWidth = AppDimens.BorderThick,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = icons.actionMenu,
                    contentDescription = stringResource(R.string.editor_actions_menu),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSmall)
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 1. Auto-Edit (IA)
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.menu_auto_edit_ai),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionAutoEdit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                onClick = {
                    isExpanded = false
                    onAutoEditAi()
                }
            )

            // 2. Bibliothèque de Presets & LUTs
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.menu_presets_luts),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionPalette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                onClick = {
                    isExpanded = false
                    onOpenPresets()
                }
            )

            HorizontalDivider()

            // 3. Sauvegarder Preset
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.editor_save_preset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionSave,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                onClick = {
                    isExpanded = false
                    onSavePreset()
                }
            )

            HorizontalDivider()

            // 4. Exporter Photo HD
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.editor_export_hd),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionExport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                enabled = !isExportingImage && !isExportingCube,
                onClick = {
                    isExpanded = false
                    onExportHd()
                }
            )

            // 5. Partager / Exporter .CUBE LUT
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.editor_export_cube),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionShare,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                enabled = !isExportingImage && !isExportingCube,
                onClick = {
                    isExpanded = false
                    onExportCube()
                }
            )

            // 6. Guide des fonctionnalités
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.guide_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionHelp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                onClick = {
                    isExpanded = false
                    onOpenGuide()
                }
            )

            HorizontalDivider()

            // 7. Réinitialiser tout
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.editor_btn_reset_all),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icons.actionReset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                },
                onClick = {
                    isExpanded = false
                    onResetAll()
                }
            )
        }
    }
}
