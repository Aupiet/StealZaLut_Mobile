package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Modern Editor-inspired Studio Step 3 Workstation.
 * Features:
 * - Pinned live WYSIWYG dual-view comparison at the top.
 * - Categorized thumb-zone tool pills (Intensity & Light, Color & Skin, Method, 3D Grid).
 * - Unified tactile adjustment card with smooth animated transitions.
 * - Pinned primary action bar (Back, Export Cube, Run 3D LUT synthesis).
 */
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
    onBack: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableIntStateOf(0) } // 0 = Intensity/Lum, 1 = Color/Skin, 2 = Method, 3 = Grid
    val pillScrollState = rememberScrollState()
    val paramsScrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. PINNED LIVE WYSIWYG PREVIEW CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
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
                                    vertical = AppDimens.SpacingMicro
                                )
                            )
                        }
                    }
                }

                DoubleViewComparison(
                    sourceBitmap = sourceBitmap,
                    previewBitmap = previewBitmap,
                    intensity = intensity,
                    executionTimeMs = executionTimeMs,
                    height = AppDimens.StudioPinnedPreviewHeight
                )
            }
        }

        // --- 2. EDITOR-STYLE CATEGORIZED TOOLBAR PILLS ---
        val categories = listOf(
            Triple(0, Icons.Default.Tune, R.string.studio_cat_intensity),
            Triple(1, Icons.Default.Face, R.string.studio_cat_protection),
            Triple(2, Icons.Default.AutoAwesome, R.string.studio_cat_method),
            Triple(3, Icons.Default.GridOn, R.string.studio_cat_lut_size)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
            border = androidx.compose.foundation.BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(pillScrollState)
                    .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { (catId, icon, labelRes) ->
                    val isSelected = activeCategory == catId
                    val targetBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val targetContent = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(180), label = "catBg") // GUARD_ALLOW
                    val animatedContent by animateColorAsState(targetValue = targetContent, animationSpec = tween(180), label = "catContent") // GUARD_ALLOW

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppDimens.StudioStep3PillHeight)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                            .background(animatedBg)
                            .clickable { activeCategory = catId }
                            .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingExtraSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = animatedContent,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = animatedContent
                            )
                        }
                    }
                }
            }
        }

        // --- 3. DYNAMIC CATEGORY PANEL CARD (Scrollable independently) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingNormal)
            ) {
                AnimatedContent(
                    targetState = activeCategory,
                    transitionSpec = { fadeIn(tween(160)).togetherWith(fadeOut(tween(120))) },
                    label = "studioCategoryTransition" // GUARD_ALLOW
                ) { cat ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(paramsScrollState),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                    ) {
                        when (cat) {
                            0 -> {
                                // Category 0: Overall Intensity & Luminance
                                ParameterSlider(
                                    title = stringResource(R.string.param_intensity_title),
                                    value = intensity,
                                    onValueChange = onIntensityChange,
                                    icon = Icons.Default.Tune,
                                    min = AppConstants.INTENSITY_MIN,
                                    max = AppConstants.INTENSITY_MAX,
                                    presets = null
                                )
                                ParameterSlider(
                                    title = stringResource(R.string.param_preserve_lum_title),
                                    value = preserveLuminance,
                                    onValueChange = onPreserveLuminanceChange,
                                    icon = Icons.Default.Tune,
                                    min = AppConstants.PARAM_MIN,
                                    max = AppConstants.PARAM_MAX,
                                    presets = null
                                )
                            }
                            1 -> {
                                // Category 1: Color & Skin Protection Controls
                                ParameterSlider(
                                    title = stringResource(R.string.param_skin_prot_title),
                                    value = skinProtection,
                                    onValueChange = onSkinProtectionChange,
                                    icon = Icons.Default.Face,
                                    min = AppConstants.PARAM_MIN,
                                    max = AppConstants.PARAM_MAX,
                                    presets = null
                                )
                                ParameterSlider(
                                    title = stringResource(R.string.param_neutral_lock_title),
                                    value = neutralLock,
                                    onValueChange = onNeutralLockChange,
                                    icon = Icons.Default.Tune,
                                    min = AppConstants.PARAM_MIN,
                                    max = AppConstants.PARAM_MAX,
                                    presets = null
                                )
                                ParameterSlider(
                                    title = stringResource(R.string.param_smooth_title),
                                    value = smoothSigma,
                                    onValueChange = onSmoothSigmaChange,
                                    icon = Icons.Default.Tune,
                                    min = AppConstants.PARAM_MIN,
                                    max = AppConstants.PARAM_MAX,
                                    presets = null
                                )
                            }
                            2 -> {
                                // Category 2: Color Transfer Method
                                MethodSelector(
                                    selectedMethod = selectedMethod,
                                    onMethodSelected = onMethodSelected
                                )
                            }
                            3 -> {
                                // Category 3: 3D LUT Lattice Size
                                LutSizeSelector(
                                    selectedSize = selectedLutSize,
                                    onSizeSelected = onLutSizeSelected
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. PINNED BOTTOM ACTION BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.SpacingMicro),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
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

            // Quick Export Cube Icon Button
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE))
            ) {
                IconButton(
                    onClick = onExportCube,
                    enabled = !isExportingCube,
                    modifier = Modifier.size(AppDimens.ButtonHeightStandard)
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
                            contentDescription = stringResource(R.string.wizard_step_5_btn_export_cube),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                    }
                }
            }

            Button(
                onClick = onRun,
                modifier = Modifier
                    .weight(1.6f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
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
