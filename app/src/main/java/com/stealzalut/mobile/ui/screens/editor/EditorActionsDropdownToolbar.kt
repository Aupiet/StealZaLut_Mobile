package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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

/**
 * Vertical dropdown actions toolbar located in the top-left of StandaloneEditorScreen.
 * Replaces the heavy bottom bar and provides quick access to:
 * - Auto-Edit (AI)
 * - Presets & LUTs Library
 * - Save Preset
 * - Export HD Photo
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
    onOpenPresets: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

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
                    imageVector = Icons.Default.Menu,
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
                        imageVector = Icons.Default.AutoFixHigh,
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
                        imageVector = Icons.Default.Collections,
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
                        imageVector = Icons.Default.Save,
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

            // 2. Exporter Photo HD
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
                        imageVector = Icons.Default.FileDownload,
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

            // 3. Exporter .CUBE LUT
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
                        imageVector = Icons.Default.Tune,
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

            HorizontalDivider()

            // 4. Réinitialiser tout
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
                        imageVector = Icons.Default.RestartAlt,
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
