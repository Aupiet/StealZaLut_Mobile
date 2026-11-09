package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.theme.icons

/**
 * Step 5: Colorimetric Result Showcase (Figma Node 12:380).
 * Strictly conforms to Figma [APRÈS] Studio / Étape 5 Résultat:
 * - Full-frame Before/After comparator with AVANT / APRÈS badges
 * - Primary Action: "Ouvrir dans l'Éditeur Pro →" (Amber, full-width)
 * - Row of 2 secondary actions: "Exporter .cube" and "Enregistrer Preset"
 */
@Composable
fun StudioStep5Result(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    executionTimeMs: Double?,
    intensity: Float,
    isGradedImageSaved: Boolean,
    isExportingImage: Boolean,
    isExportingCube: Boolean,
    exportedCubePath: String? = null,
    onIntensityChange: (Float) -> Unit,
    onOpenInEditor: () -> Unit,
    onSaveGradedImage: () -> Unit,
    onSaveStyleToProfile: () -> Unit,
    onExportCube: () -> Unit,
    onShareCube: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_finish_studio_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_finish_studio_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirmDialog = false
                        onReset()
                    }
                ) {
                    Text(stringResource(R.string.dialog_finish_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Before / After Interactive Curtain Comparison (Figma node 12:400) ---
        DoubleViewComparison(
            sourceBitmap = sourceBitmap,
            previewBitmap = previewBitmap,
            intensity = intensity,
            executionTimeMs = executionTimeMs,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        // --- 2. Primary Action Button: "Ouvrir dans l'Éditeur Pro →" (Figma node 12:409) ---
        Button(
            onClick = onOpenInEditor,
            enabled = !isExportingImage && !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.btn_edit_in_editor),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- 3. Row of 2 Secondary Action Buttons (Figma node 12:412) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: "Exporter .cube"
            OutlinedButton(
                onClick = onShareCube,
                enabled = !isExportingImage && !isExportingCube,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                if (isExportingCube) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.IconSmall),
                        strokeWidth = AppDimens.BorderMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = MaterialTheme.icons.actionShare,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_5_btn_export_cube),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Button 2: "Enregistrer Preset"
            OutlinedButton(
                onClick = onSaveStyleToProfile,
                enabled = !isExportingImage && !isExportingCube,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.actionSave,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_5_btn_save_theme),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
