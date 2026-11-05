package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens

/**
 * Scalable bottom action bar for Studio & Editor.
 *
 * Resolves Bug B1: Never crushes buttons or truncates labels letter-by-letter on 360dp screens.
 * Uses a primary action with export menu/options + clear, accessible secondary buttons.
 */
@Composable
fun SzlBottomActionBar(
    isExportingImage: Boolean,
    isExportingCube: Boolean,
    onExportHd: () -> Unit,
    onExportCube: () -> Unit,
    onSavePreset: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExportMenu by remember { mutableStateOf(false) }
    val resetDesc = stringResource(R.string.editor_btn_reset_all)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Reset All (Secondary Icon Action >= 48dp)
        FilledTonalIconButton(
            onClick = onResetAll,
            modifier = Modifier
                .defaultMinSize(minWidth = AppDimens.MinTouchTarget, minHeight = AppDimens.MinTouchTarget)
                .semantics { contentDescription = resetDesc }
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = resetDesc,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }

        // 2. Save Preset (Secondary Outlined Action)
        OutlinedButton(
            onClick = onSavePreset,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconSmall)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
            Text(
                text = stringResource(R.string.editor_save_preset),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. Primary Export Action with Dropdown Selection (Resolves B1)
        Column {
            Button(
                onClick = { showExportMenu = true },
                enabled = !isExportingImage && !isExportingCube,
                modifier = Modifier
                    .defaultMinSize(minWidth = AppDimens.MinInteractiveTarget, minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isExportingImage || isExportingCube) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.IconSmall),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                    Text(
                        text = stringResource(R.string.editor_actions_export),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.editor_export_hd),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        showExportMenu = false
                        onExportHd()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.editor_export_cube),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        showExportMenu = false
                        onExportCube()
                    }
                )
            }
        }
    }
}
