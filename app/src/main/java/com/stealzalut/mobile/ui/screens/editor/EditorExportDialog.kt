package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.szlColors
import java.util.Locale
import kotlin.math.roundToInt

enum class ExportFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png")
}

/**
 * Modal Export Options Dialog (FE-013) conforming to Figma frame [APRÈS] Éditeur / Dialogue Export HD.
 * Uses 20-24dp rounded corners, subtle amber border, width ~92%, and full-width buttons.
 */
@Composable
fun EditorExportDialog(
    isExporting: Boolean,
    exportProgress: Float,
    imageWidth: Int,
    imageHeight: Int,
    onDismiss: () -> Unit,
    onConfirmExport: (format: ExportFormat, quality: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.JPEG) }
    var qualityPercent by remember { mutableFloatStateOf(0.95f) }

    val estimatedMb = remember(selectedFormat, qualityPercent, imageWidth, imageHeight) {
        val w = if (imageWidth > 0) imageWidth else 3840
        val h = if (imageHeight > 0) imageHeight else 2160
        val rawBytes = w.toDouble() * h.toDouble() * 3.0
        val compressedBytes = if (selectedFormat == ExportFormat.JPEG) {
            rawBytes * (qualityPercent * 0.12).coerceIn(0.03, 0.35)
        } else {
            rawBytes * 0.55 // PNG lossless compression
        }
        val mb = compressedBytes / (1024.0 * 1024.0)
        String.format(Locale.US, "~ %.1f Mo", mb)
    }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = AppDimens.SpacingLarge),
            shape = RoundedCornerShape(AppDimens.CornerRadiusDialog),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            ),
            shadowElevation = AppDimens.DialogElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingHuge),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingStandard)
            ) {
                // Header: Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro)) {
                    Text(
                        text = stringResource(R.string.export_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.export_dialog_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Format Selector (JPEG vs PNG)
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
                    Text(
                        text = stringResource(R.string.export_dialog_format),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                    ) {
                        ExportFormat.entries.forEach { format ->
                            val isSelected = selectedFormat == format
                            Surface(
                                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    AppDimens.BorderThin,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(AppDimens.ButtonHeightCompact)
                                    .clickable(enabled = !isExporting) { selectedFormat = format }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (format == ExportFormat.JPEG) {
                                            stringResource(R.string.export_dialog_format_jpeg)
                                        } else {
                                            stringResource(R.string.export_dialog_format_png)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Quality Slider (for JPEG)
                if (selectedFormat == ExportFormat.JPEG) {
                    SzlSlider(
                        title = stringResource(R.string.export_dialog_quality),
                        value = qualityPercent,
                        onValueChange = { qualityPercent = it },
                        min = 0.50f,
                        max = 1.0f,
                        step = 0.05f,
                        presets = listOf(0.70f, 0.85f, 0.95f, 1.0f)
                    )
                }

                // Resolution and Estimated Size Card
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingStandard, vertical = AppDimens.SpacingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.export_dialog_resolution),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val w = if (imageWidth > 0) imageWidth else 3840
                            val h = if (imageHeight > 0) imageHeight else 2160
                            Text(
                                text = "${w} × ${h} px",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.export_dialog_filesize_est),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = estimatedMb,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Tiled Export Progress Bar
                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                    ) {
                        Text(
                            text = stringResource(R.string.export_dialog_progress),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.ProgressBarHeight),
                            color = MaterialTheme.szlColors.exportProgress,
                            trackColor = MaterialTheme.szlColors.exportTrack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))

                // Action Buttons: Confirm Full Width & Cancel Full Width
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Button(
                        onClick = {
                            onConfirmExport(selectedFormat, (qualityPercent * 100).roundToInt())
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.ButtonHeightStandard)
                    ) {
                        Text(
                            text = stringResource(R.string.export_dialog_confirm),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    if (!isExporting) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.ButtonHeightStandard)
                        ) {
                            Text(
                                text = stringResource(R.string.common_cancel),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
