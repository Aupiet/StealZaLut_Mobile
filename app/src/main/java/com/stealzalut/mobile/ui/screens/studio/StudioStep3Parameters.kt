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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.components.LutSizeSelector
import com.stealzalut.mobile.ui.components.MethodSelector
import com.stealzalut.mobile.ui.components.ParameterSlider

@Composable
fun StudioStep3Parameters(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    isProcessingPreview: Boolean,
    executionTimeMs: Double?,
    selectedMethod: ColorTransferMethod,
    onMethodSelected: (ColorTransferMethod) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    preserveLuminance: Float,
    onPreserveLuminanceChange: (Float) -> Unit,
    skinProtection: Float,
    onSkinProtectionChange: (Float) -> Unit,
    smoothSigma: Float,
    onSmoothSigmaChange: (Float) -> Unit,
    neutralLock: Float,
    onNeutralLockChange: (Float) -> Unit,
    selectedLutSize: LutSize,
    onLutSizeSelected: (LutSize) -> Unit,
    isExportingCube: Boolean = false,
    onExportCube: () -> Unit = {},
    isComputingAutoEdit: Boolean = false,
    onAutoEdit: () -> Unit = {},
    onBack: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)
    ) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_3_title),
            subtitle = stringResource(R.string.wizard_step_3_subtitle)
        )

        // Live WYSIWYG Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.SpacingNormal),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                        Text(
                            text = stringResource(R.string.wizard_step_3_preview_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isProcessingPreview) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimens.IconSmall),
                                strokeWidth = AppDimens.BorderThin,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.wizard_step_3_computing_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (executionTimeMs != null) {
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${executionTimeMs.toInt()} ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.SpacingSmall,
                                    vertical = AppDimens.SpacingExtraSmall
                                )
                            )
                        }
                    }
                }

                DoubleViewComparison(
                    sourceBitmap = sourceBitmap,
                    previewBitmap = previewBitmap,
                    intensity = intensity,
                    executionTimeMs = executionTimeMs
                )
            }
        }

        // Independent Parameter Controls Card with Integrated Auto-Edit Action
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.wizard_step_3_independent_params_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = onAutoEdit,
                        enabled = !isComputingAutoEdit,
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isComputingAutoEdit) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimens.IconSmall),
                                strokeWidth = AppDimens.BorderThick,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
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
                }

                // 1. Overall Strength
                ParameterSlider(
                    title = stringResource(R.string.param_intensity_title),
                    description = stringResource(R.string.param_intensity_desc),
                    value = intensity,
                    onValueChange = onIntensityChange,
                    icon = Icons.Default.Tune,
                    min = AppConstants.INTENSITY_MIN,
                    max = AppConstants.INTENSITY_MAX
                )

                // 2. Luminance Preservation
                ParameterSlider(
                    title = stringResource(R.string.param_preserve_lum_title),
                    description = stringResource(R.string.param_preserve_lum_desc),
                    value = preserveLuminance,
                    onValueChange = onPreserveLuminanceChange,
                    icon = Icons.Default.Brightness6,
                    min = AppConstants.PARAM_MIN,
                    max = AppConstants.PARAM_MAX
                )

                // 3. Skin Tone Protection
                ParameterSlider(
                    title = stringResource(R.string.param_skin_prot_title),
                    description = stringResource(R.string.param_skin_prot_desc),
                    value = skinProtection,
                    onValueChange = onSkinProtectionChange,
                    icon = Icons.Default.Face,
                    min = AppConstants.PARAM_MIN,
                    max = AppConstants.PARAM_MAX
                )

                // 4. Neutral Axis Lock
                ParameterSlider(
                    title = stringResource(R.string.param_neutral_lock_title),
                    description = stringResource(R.string.param_neutral_lock_desc),
                    value = neutralLock,
                    onValueChange = onNeutralLockChange,
                    icon = Icons.Default.Contrast,
                    min = AppConstants.PARAM_MIN,
                    max = AppConstants.PARAM_MAX
                )

                // 5. 3D Spatial Smoothing
                ParameterSlider(
                    title = stringResource(R.string.param_smooth_title),
                    description = stringResource(R.string.param_smooth_desc),
                    value = smoothSigma,
                    onValueChange = onSmoothSigmaChange,
                    icon = Icons.Default.BlurOn,
                    min = AppConstants.PARAM_MIN,
                    max = AppConstants.PARAM_MAX
                )
            }
        }

        // Method Selector
        MethodSelector(
            selectedMethod = selectedMethod,
            onMethodSelected = onMethodSelected
        )

        // LUT Size Selector
        LutSizeSelector(
            selectedSize = selectedLutSize,
            onSizeSelected = onLutSizeSelected
        )

        // Direct Action: Export 3D LUT (.CUBE) directly from Step 3
        OutlinedButton(
            onClick = onExportCube,
            enabled = !isExportingCube,
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

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_3_btn_back),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Button(
                onClick = onRun,
                modifier = Modifier
                    .weight(1.5f)
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
                    text = stringResource(R.string.wizard_step_3_btn_run),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
