package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.components.IntensitySlider

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
    onShareCube: (() -> Unit)? = null,
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // Status / Feedback Banner
        Surface(
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            color = if (isGradedImageSaved)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(AppDimens.SpacingNormal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGradedImageSaved) Icons.Default.CheckCircle else Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconMedium),
                    tint = if (isGradedImageSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Column {
                    Text(
                        text = stringResource(R.string.wizard_step_5_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isGradedImageSaved) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGradedImageSaved)
                            stringResource(R.string.wizard_step_5_saved_badge)
                        else
                            stringResource(R.string.wizard_step_5_autosave_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGradedImageSaved)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = AppConstants.OPACITY_CONTAINER_HEAVY)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 120 FPS GPU Accelerated Dual View (Curtain Comparison)
        DoubleViewComparison(
            sourceBitmap = sourceBitmap,
            previewBitmap = previewBitmap,
            intensity = intensity,
            executionTimeMs = executionTimeMs
        )

        // Real-time GPU Opacity Slider
        IntensitySlider(
            intensity = intensity,
            onIntensityChange = onIntensityChange
        )

        // Primary Action: Retoucher dans l'éditeur (Phase C3 Relay)
        Button(
            onClick = onOpenInEditor,
            enabled = !isExportingImage && !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(R.string.btn_edit_in_editor),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Action 1: Save Full-Res Graded Image to Gallery
        FilledTonalButton(
            onClick = onSaveGradedImage,
            enabled = !isExportingImage && !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            if (isExportingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.IconNormal),
                    strokeWidth = AppDimens.BorderThick,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_5_exporting_image),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_5_btn_save_gallery),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action 2: Export 3D LUT (.CUBE) & Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            OutlinedButton(
                onClick = onExportCube,
                enabled = !isExportingImage && !isExportingCube,
                modifier = Modifier
                    .weight(if (exportedCubePath != null && onShareCube != null) 1.2f else 1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                if (isExportingCube) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.IconNormal),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_5_exporting_cube),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_5_btn_export_cube),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (exportedCubePath != null && onShareCube != null) {
                OutlinedButton(
                    onClick = onShareCube,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.common_share),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Action 3: Save Style Recipe to Presets
        OutlinedButton(
            onClick = onSaveStyleToProfile,
            enabled = !isExportingImage && !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(R.string.wizard_step_5_btn_save_theme),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Action 4: Terminer (Finish Session with confirmation if unsaved)
        OutlinedButton(
            onClick = {
                if (isGradedImageSaved) {
                    onReset()
                } else {
                    showFinishConfirmDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(R.string.wizard_step_5_btn_reset),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
