package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.stealzalut.mobile.ui.theme.icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.ZonePaletteAnalysis
import com.stealzalut.mobile.data.model.ZoneTransferSummary
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.components.LutSizeSelector
import com.stealzalut.mobile.ui.components.MethodSelector
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.LocalFontSpec


/**
 * Step 3: Studio Parameter Adjustments (WYSIWYG Live Workstation).
 * Features:
 * - Live WYSIWYG dual-view comparison card pinned at the top.
 * - Categorized thumb-zone tool pills (Intensity & Light, Color & Skin, Algorithm, 3D Lattice Size).
 * - High-fidelity Sliders for ALL metrics with JetBrains Mono tabular figures & amber active tracks.
 * - Pinned bottom action bar (Back, Quick Export .cube, Run LUT Render).
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
    isAnalyzingZones: Boolean = false,
    hasZoneAnalysis: Boolean = false,
    sourceZoneAnalysis: ZonePaletteAnalysis? = null,
    referenceZoneAnalysis: ZonePaletteAnalysis? = null,
    zoneTransfer: ZoneTransferSummary? = null,
    selectedZoneIndex: Int = -1,
    maskPath: String? = null,
    maskMode: String = "bw",
    onZoneCorrespondenceChanged: (srcIdx: Int, refIdx: Int) -> Unit = { _, _ -> },
    onResetCorrespondences: () -> Unit = {},
    onZoneSelected: (zoneIndex: Int) -> Unit = {},
    onToggleMaskMode: () -> Unit = {},
    onAnalyzeZones: () -> Unit = {},
    isExportingCube: Boolean = false,
    onExportCube: () -> Unit = {},
    onBack: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableIntStateOf(0) } // 0 = Method, 1 = Grid, 2 = Intensity, 3 = Color, 4 = Advanced
    val pillScrollState = rememberScrollState()
    val paramsScrollState = rememberScrollState()
    val monoFont = LocalFontSpec.current.mono

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. PINNED LIVE WYSIWYG PREVIEW ---
        DoubleViewComparison(
            sourceBitmap = sourceBitmap,
            previewBitmap = previewBitmap ?: sourceBitmap,
            executionTimeMs = executionTimeMs,
            fillAvailableHeight = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )

        // --- 2. CATEGORY PILLS (Thumb Zone Navigation: Algorithm -> 3D Grid -> Light -> Color) ---
        val categories = listOf(
            Triple(0, stringResource(R.string.studio_tab_algorithm), Icons.Default.AutoAwesome),
            Triple(1, stringResource(R.string.studio_tab_grid), Icons.Default.GridOn),
            Triple(2, stringResource(R.string.studio_tab_light), Icons.Default.Tune),
            Triple(3, stringResource(R.string.studio_tab_color), Icons.Default.Face),
            Triple(4, stringResource(R.string.studio_tab_advanced), Icons.Default.AutoAwesome)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.SpacingMicro),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            categories.forEach { (index, title, icon) ->
                val isSelected = activeCategory == index
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "pillBg" // GUARD_ALLOW
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "pillText" // GUARD_ALLOW
                )

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = animatedBg,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .clickable { activeCategory = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro, Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = animatedTextColor,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }
            }
        }

        // --- 3. DYNAMIC ADJUSTMENT CARD WITH SLIDERS FOR ALL METRICS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)),
            border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(paramsScrollState)
                    .padding(AppDimens.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                AnimatedContent(
                    targetState = activeCategory,
                    transitionSpec = { fadeIn(tween(140)).togetherWith(fadeOut(tween(100))) },
                    label = "paramCategoryTransition" // GUARD_ALLOW
                ) { category ->
                    when (category) {
                        0 -> {
                            // Category 0: Color Transfer Algorithm (Cinematic HM-MKL-HM, MKL, HM, Reinhard, MVGD)
                            MethodSelector(
                                selectedMethod = selectedMethod,
                                onMethodSelected = onMethodSelected
                            )
                        }
                        1 -> {
                            // Category 1: 3D LUT Lattice Size (17x17x17, 33x33x33, 64x64x64)
                            LutSizeSelector(
                                selectedSize = selectedLutSize,
                                onSizeSelected = onLutSizeSelected
                            )
                        }
                        2 -> {
                            // Category 2: Intensity & Luminance Lock
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                            ) {
                                SzlSlider(
                                    title = stringResource(R.string.wizard_step_3_intensity_title),
                                    value = intensity,
                                    onValueChange = onIntensityChange,
                                    min = 0f,
                                    max = 1f,
                                    step = 0.01f,
                                    defaultValue = 1f
                                )

                                SzlSlider(
                                    title = stringResource(R.string.param_luma_lock_title),
                                    value = preserveLuminance,
                                    onValueChange = onPreserveLuminanceChange,
                                    min = 0f,
                                    max = 1f,
                                    step = 0.01f,
                                    defaultValue = 0.8f
                                )
                            }
                        }
                        3 -> {
                            // Category 3: Color, Skin Protection, Neutral Lock & 3D Spatial Smoothing
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                            ) {
                                SzlSlider(
                                    title = stringResource(R.string.param_skin_prot_title),
                                    value = skinProtection,
                                    onValueChange = onSkinProtectionChange,
                                    min = 0f,
                                    max = 1f,
                                    step = 0.01f,
                                    defaultValue = 0.8f
                                )

                                SzlSlider(
                                    title = stringResource(R.string.param_neutral_lock_title),
                                    value = neutralLock,
                                    onValueChange = onNeutralLockChange,
                                    min = 0f,
                                    max = 1f,
                                    step = 0.01f,
                                    defaultValue = 0.5f
                                )

                                SzlSlider(
                                    title = stringResource(R.string.param_smooth_title),
                                    value = smoothSigma,
                                    onValueChange = onSmoothSigmaChange,
                                    min = 0f,
                                    max = 1f,
                                    step = 0.01f,
                                    defaultValue = 0.0f
                                )
                            }
                        }
                        4 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                            ) {
                                Text(
                                    text = stringResource(R.string.studio_zones_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                ZonePaletteVisualization(
                                    source = sourceZoneAnalysis,
                                    reference = referenceZoneAnalysis,
                                    zoneTransfer = zoneTransfer,
                                    selectedZoneIndex = selectedZoneIndex,
                                    maskPath = maskPath,
                                    maskMode = maskMode,
                                    onZoneCorrespondenceChanged = onZoneCorrespondenceChanged,
                                    onResetCorrespondences = onResetCorrespondences,
                                    onZoneSelected = onZoneSelected,
                                    onToggleMaskMode = onToggleMaskMode
                                )
                                if (sourceZoneAnalysis != null || referenceZoneAnalysis != null) {
                                    Text(
                                        text = listOfNotNull(
                                            sourceZoneAnalysis?.let { "source ${it.zoneCount}" },
                                            referenceZoneAnalysis?.let { "reference ${it.zoneCount}" }
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = onAnalyzeZones,
                                    enabled = !isAnalyzingZones,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
                                ) {
                                    if (isAnalyzingZones) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(AppDimens.IconNormal),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(AppDimens.IconNormal)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                    Text(
                                        text = if (hasZoneAnalysis) {
                                            stringResource(R.string.studio_zones_reanalyze)
                                        } else {
                                            stringResource(R.string.studio_zones_analyze)
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Export Cube Icon Button
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE))
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
                            imageVector = MaterialTheme.icons.actionShare,
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
