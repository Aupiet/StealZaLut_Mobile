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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens

/**
 * Action bar for exporting and sharing color-grading assets.
 * Zero hardcoded strings, zero hardcoded numbers, and zero emojis.
 */
@Composable
fun ExportActionBar(
    canProcess: Boolean,
    isProcessingPreview: Boolean,
    isExportingCube: Boolean,
    isExportingImage: Boolean,
    hasExportedCube: Boolean,
    hasExportedImage: Boolean,
    onGeneratePreview: () -> Unit,
    onExportGradedImage: () -> Unit,
    onExportCube: () -> Unit,
    onSaveColorimetry: () -> Unit,
    onShare: () -> Unit,
    onShareCube: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val previewDesc = stringResource(R.string.wizard_step_3_btn_run)
    val exportDesc = stringResource(R.string.wizard_step_5_btn_save_gallery)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // Row 1: Fast Preview + Direct Graded Image Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Button
            OutlinedButton(
                onClick = onGeneratePreview,
                enabled = canProcess,
                modifier = Modifier
                    .weight(AppConstants.WEIGHT_SECONDARY_ACTION)
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget)
                    .semantics {
                        contentDescription = previewDesc
                    },
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                if (isProcessingPreview) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_3_btn_run),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Direct Graded Image Export Button
            Button(
                onClick = onExportGradedImage,
                enabled = canProcess,
                modifier = Modifier
                    .weight(AppConstants.WEIGHT_PRIMARY_ACTION)
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget)
                    .semantics {
                        contentDescription = exportDesc
                    },
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isExportingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.common_save),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 2: .cube LUT Export + Recipe Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Export .cube
            OutlinedButton(
                onClick = onExportCube,
                enabled = canProcess,
                modifier = Modifier
                    .weight(AppConstants.WEIGHT_EQUAL)
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                if (isExportingCube) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_5_btn_export_cube),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Save recipe
            OutlinedButton(
                onClick = onSaveColorimetry,
                enabled = canProcess,
                modifier = Modifier
                    .weight(AppConstants.WEIGHT_EQUAL)
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_5_btn_save_theme),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
