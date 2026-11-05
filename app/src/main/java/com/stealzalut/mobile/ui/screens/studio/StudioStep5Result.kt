package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.components.IntensitySlider
import com.stealzalut.mobile.ui.components.SzlSectionHeader
import java.util.Locale

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
    isAutoEditActive: Boolean = false,
    autoExposure: Float = 0.0f,
    autoContrast: Float = 68.0f,
    autoHighlights: Float = -66.0f,
    autoShadows: Float = 52.0f,
    autoVibrance: Float = 30.0f,
    isEditPanelVisible: Boolean = false,
    isComputingEdit: Boolean = false,
    editExposure: Float = 0.0f,
    editContrast: Float = 0.0f,
    editHighlights: Float = 0.0f,
    editShadows: Float = 0.0f,
    editWhites: Float = 0.0f,
    editBlacks: Float = 0.0f,
    editVibrance: Float = 0.0f,
    editSaturation: Float = 0.0f,
    onToggleEditPanel: () -> Unit = {},
    onAutoEdit: () -> Unit = {},
    onResetEdits: () -> Unit = {},
    onExposureChange: (Float) -> Unit = {},
    onContrastChange: (Float) -> Unit = {},
    onHighlightsChange: (Float) -> Unit = {},
    onShadowsChange: (Float) -> Unit = {},
    onWhitesChange: (Float) -> Unit = {},
    onBlacksChange: (Float) -> Unit = {},
    onVibranceChange: (Float) -> Unit = {},
    onSaturationChange: (Float) -> Unit = {},
    onIntensityChange: (Float) -> Unit,
    onSaveGradedImage: () -> Unit,
    onSaveStyleToProfile: () -> Unit,
    onExportCube: () -> Unit,
    onShareCube: (() -> Unit)? = null,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditActive = editExposure != 0f || editContrast != 0f || editHighlights != 0f ||
            editShadows != 0f || editWhites != 0f || editBlacks != 0f ||
            editVibrance != 0f || editSaturation != 0f

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

        // 120 FPS GPU Accelerated Dual View (Side-by-Side & Split-Wipe)
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

        // LightCraft Edit Action Button
        OutlinedButton(
            onClick = onToggleEditPanel,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            colors = if (isEditActive) {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
            border = BorderStroke(
                width = AppDimens.BorderThin,
                color = if (isEditActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = if (isEditPanelVisible) stringResource(R.string.btn_close_edit) else stringResource(R.string.btn_edit_image),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (isEditActive) {
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = stringResource(R.string.edit_active_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isEditPanelVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }

        // LightCraft Collapsible Edit Panel
        AnimatedVisibility(visible = isEditPanelVisible) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                ) {
                    SzlSectionHeader(
                        title = stringResource(R.string.edit_panel_title),
                        icon = Icons.Default.Tune
                    ) {
                        Button(
                            onClick = onAutoEdit,
                            enabled = !isComputingEdit,
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isComputingEdit) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(AppDimens.IconSmall),
                                    strokeWidth = AppDimens.BorderThick,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                                Text(
                                    text = stringResource(R.string.btn_auto_edit),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onResetEdits,
                            enabled = !isComputingEdit && isEditActive,
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Text(
                                text = stringResource(R.string.btn_reset_edits),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Section: Lumière (Light)
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.SpacingNormal),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Brightness6,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                Text(
                                    text = stringResource(R.string.edit_section_light),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_exposure),
                                value = editExposure,
                                onValueChange = onExposureChange,
                                min = -2.0f,
                                max = 3.0f,
                                valueText = String.format(Locale.US, "%+.2f EV", editExposure),
                                onResetToZero = { onExposureChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_contrast),
                                value = editContrast,
                                onValueChange = onContrastChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editContrast),
                                onResetToZero = { onContrastChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_highlights),
                                value = editHighlights,
                                onValueChange = onHighlightsChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editHighlights),
                                onResetToZero = { onHighlightsChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_shadows),
                                value = editShadows,
                                onValueChange = onShadowsChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editShadows),
                                onResetToZero = { onShadowsChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_whites),
                                value = editWhites,
                                onValueChange = onWhitesChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editWhites),
                                onResetToZero = { onWhitesChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_blacks),
                                value = editBlacks,
                                onValueChange = onBlacksChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editBlacks),
                                onResetToZero = { onBlacksChange(0f) }
                            )
                        }
                    }

                    // Section: Couleur (Color)
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.SpacingNormal),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                Text(
                                    text = stringResource(R.string.edit_section_color),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_vibrance),
                                value = editVibrance,
                                onValueChange = onVibranceChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editVibrance),
                                onResetToZero = { onVibranceChange(0f) }
                            )

                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_saturation),
                                value = editSaturation,
                                onValueChange = onSaturationChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(Locale.US, "%+.0f", editSaturation),
                                onResetToZero = { onSaturationChange(0f) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        // Action 1: Save Graded Image
        Button(
            onClick = onSaveGradedImage,
            enabled = !isExportingImage && !isGradedImageSaved,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            colors = if (isGradedImageSaved) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            if (isExportingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                    strokeWidth = AppDimens.BorderThick,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_4_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = if (isGradedImageSaved) Icons.Default.CheckCircle else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = if (isGradedImageSaved)
                        stringResource(R.string.wizard_step_5_saved_badge)
                    else
                        stringResource(R.string.wizard_step_5_btn_save_gallery),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action 2: Export Adobe .cube 3D LUT
        OutlinedButton(
            onClick = onExportCube,
            enabled = !isExportingCube && !isExportingImage,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            if (isExportingCube) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                    strokeWidth = AppDimens.BorderThick,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_3_exporting_cube),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
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

        // Action 2b: Share .cube 3D LUT
        if (exportedCubePath != null && onShareCube != null) {
            OutlinedButton(
                onClick = onShareCube,
                enabled = !isExportingCube && !isExportingImage,
                modifier = Modifier
                    .fillMaxWidth()
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
                    text = stringResource(R.string.common_share) + " " + stringResource(R.string.palettes_btn_export_cube),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action 3: Save Preset Recipe
        Button(
            onClick = onSaveStyleToProfile,
            enabled = !isExportingImage && !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

        // Action 4: Reset & Restart
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(R.string.wizard_step_5_btn_reset),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}

@Composable
private fun EditParameterSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    valueText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onResetToZero: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                color = if (value != 0f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable(enabled = onResetToZero != null && value != 0f) {
                    onResetToZero?.invoke()
                }
            ) {
                Text(
                    text = valueText,
                    modifier = Modifier.padding(
                        horizontal = AppDimens.SpacingSmall,
                        vertical = AppDimens.SpacingMicro
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (value != 0f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.SpacingLarge),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
