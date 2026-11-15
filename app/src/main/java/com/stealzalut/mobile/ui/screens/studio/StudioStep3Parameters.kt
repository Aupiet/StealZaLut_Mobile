package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    isGeneratingZoneMask: Boolean = false,
    hasZoneAnalysis: Boolean = false,
    sourceZoneAnalysis: ZonePaletteAnalysis? = null,
    referenceZoneAnalysis: ZonePaletteAnalysis? = null,
    zoneTransfer: ZoneTransferSummary? = null,
    selectedZoneIndex: Int = -1,
    maskPath: String? = null,
    maskMode: String = "bw",
    isZoneMaskActive: Boolean = false,
    onZoneCorrespondenceChanged: (srcIdx: Int, refIdx: Int) -> Unit = { _, _ -> },
    onResetCorrespondences: () -> Unit = {},
    onZoneSelected: (zoneIndex: Int) -> Unit = {},
    onToggleMaskMode: () -> Unit = {},
    onToggleZoneMaskActive: () -> Unit = {},
    onSaveSession: () -> Unit = {},
    onRestoreSession: () -> Unit = {},
    onResetParameters: () -> Unit = {},
    onAnalyzeZones: () -> Unit = {},
    isExportingCube: Boolean = false,
    onExportCube: () -> Unit = {},
    onBack: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableIntStateOf(0) } // 0 = Method, 1 = Grid, 2 = Intensity, 3 = Color, 4 = Advanced
    var isDrawerExpanded by remember { mutableStateOf(true) }
    val paramsScrollState = rememberScrollState()

    // When zone mask is active and path exists, decode and display on main viewport (isolation on black)
    val maskBitmap = remember(maskPath, isZoneMaskActive) {
        if (isZoneMaskActive && maskPath != null) {
            try { BitmapFactory.decodeFile(maskPath) } catch (_: Exception) { null }
        } else null
    }
    val effectivePreview = maskBitmap ?: previewBitmap ?: sourceBitmap

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.szlColors.photoDarkroom)
    ) {
        val totalHeight = maxHeight
        val imageMaxHeight = if (isDrawerExpanded) totalHeight * 0.48f else totalHeight * 0.60f

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. Edge-to-Edge Maximized WYSIWYG Viewport (Dynamic & Capped - Zero Scroll) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DoubleViewComparison(
                    sourceBitmap = sourceBitmap,
                    previewBitmap = effectivePreview,
                    executionTimeMs = executionTimeMs,
                    fillAvailableHeight = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = imageMaxHeight)
                )
            }

            // --- 2. Bottom Drawer & Category Toolbar (Editor Style) ---
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDrawerExpanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = totalHeight * 0.46f),
                        shape = RoundedCornerShape(
                            topStart = AppDimens.CornerRadiusEditorSheet,
                            topEnd = AppDimens.CornerRadiusEditorSheet
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        )
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall)
                    ) {
                        // Collapse Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                                .clickable { isDrawerExpanded = false }
                                .padding(vertical = AppDimens.SpacingMicro),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = AppDimens.DrawerHandleWidth, height = AppDimens.DrawerHandleHeight)
                                    .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                        }

                        // Adjustment content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(paramsScrollState)
                                .padding(bottom = AppDimens.SpacingSmall)
                        ) {
                            AnimatedContent(
                                targetState = activeCategory,
                                transitionSpec = { fadeIn(tween(140)).togetherWith(fadeOut(tween(100))) },
                                label = "paramCategoryTransition" // GUARD_ALLOW
                            ) { category ->
                                when (category) {
                                    0 -> MethodSelector(
                                        selectedMethod = selectedMethod,
                                        onMethodSelected = onMethodSelected
                                    )
                                    1 -> LutSizeSelector(
                                        selectedSize = selectedLutSize,
                                        onSizeSelected = onLutSizeSelected
                                    )
                                    2 -> Column(
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
                                    3 -> Column(
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
                                    4 -> Column(
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
                                            isAnalyzingZones = isAnalyzingZones,
                                            isGeneratingZoneMask = isGeneratingZoneMask,
                                            onZoneCorrespondenceChanged = onZoneCorrespondenceChanged,
                                            onResetCorrespondences = onResetCorrespondences,
                                            onZoneSelected = onZoneSelected,
                                            onToggleMaskMode = onToggleMaskMode
                                        )

                                        // Progress indicator while color zones are analyzing
                                        if (isAnalyzingZones) {
                                            Surface(
                                                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                                                border = BorderStroke(
                                                    AppDimens.BorderThin,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.studio_zones_analyzing),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.studio_zones_analyze),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                                                    LinearProgressIndicator(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(AppDimens.BorderThick),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = onAnalyzeZones,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                                                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
                                            ) {
                                                Icon(
                                                    imageVector = MaterialTheme.icons.toolEffects,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(AppDimens.IconNormal)
                                                )
                                                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                                Text(
                                                    text = stringResource(R.string.studio_zones_analyze),
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
                }
            }

            // Category Bar (Lightroom-style Bottom Toolbar)
            val categories = listOf(
                Triple(0, stringResource(R.string.studio_tab_algorithm), MaterialTheme.icons.toolEffects),
                Triple(1, stringResource(R.string.studio_tab_grid), MaterialTheme.icons.actionGrid),
                Triple(2, stringResource(R.string.studio_tab_light), MaterialTheme.icons.toolBasic),
                Triple(3, stringResource(R.string.studio_tab_color), MaterialTheme.icons.toolColorGrading),
                Triple(4, stringResource(R.string.studio_tab_advanced), MaterialTheme.icons.toolCalibration)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingMicro),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    categories.forEach { (index, title, icon) ->
                        val isSelected = activeCategory == index && isDrawerExpanded
                        val animatedBg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                            animationSpec = tween(180),
                            label = "catBg" // GUARD_ALLOW
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "catTint" // GUARD_ALLOW
                        )

                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = animatedBg,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                                .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                                .clickable {
                                    if (activeCategory == index && isDrawerExpanded) {
                                        isDrawerExpanded = false
                                    } else {
                                        activeCategory = index
                                        isDrawerExpanded = true
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = AppDimens.SpacingExtraSmall),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = iconTint,
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
}
}
