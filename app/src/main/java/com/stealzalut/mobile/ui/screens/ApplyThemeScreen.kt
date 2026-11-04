package com.stealzalut.mobile.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.ui.components.DoubleViewComparison
import com.stealzalut.mobile.ui.components.ImagePickerCard
import com.stealzalut.mobile.ui.components.IntensitySlider
import com.stealzalut.mobile.ui.components.LutSizeSelector
import com.stealzalut.mobile.ui.components.MethodSelector
import com.stealzalut.mobile.ui.components.PaletteSwatchesView
import com.stealzalut.mobile.ui.components.ParameterSlider
import com.stealzalut.mobile.ui.components.SavePresetDialog
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File

/**
 * Gamified Color Grading Studio Screen (Apply Theme Flow):
 * - Step 1: Upload Source Photo A
 * - Step 2: Choose Reference Photo B or Saved Preset
 * - Step 3: Configure Algorithm & Intensity
 * - Step 4: High-Tech Offline Python Computing Screen
 * - Step 5: 120 FPS Real-time GPU Blending, Direct MediaStore Export, and .cube Export
 */
@Composable
fun ApplyThemeScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val exportCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        if (destinationUri != null) {
            viewModel.saveExportedCubeToUri(destinationUri)
        }
    }

    if (uiState.isSavingThemeDialogVisible) {
        val paletteToSave = if (uiState.customReferencePalette.isNotEmpty()) {
            uiState.customReferencePalette
        } else {
            uiState.selectedTheme?.paletteHexes ?: emptyList()
        }

        SavePresetDialog(
            method = uiState.method,
            intensity = uiState.intensity,
            lutSize = uiState.lutSize,
            paletteHexes = paletteToSave,
            onDismiss = { viewModel.dismissSaveThemeDialog() },
            onSave = { name, notes ->
                viewModel.saveCurrentStyleAsPreset(name, notes)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSpacious)
    ) {
        // --- 1. Stepper Progress Header ---
        StepperProgressHeader(
            currentStep = uiState.wizardStep,
            onStepClick = { step ->
                if (step < uiState.wizardStep) {
                    viewModel.setWizardStep(step)
                }
            }
        )

        // --- 2. Dynamic Step Content ---
        AnimatedContent(
            targetState = uiState.wizardStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "WizardFunnelTransition"
        ) { step ->
            when (step) {
                AppConstants.STEP_1_SOURCE -> {
                    WizardStep1Source(
                        sourceBitmap = uiState.sourceBitmap,
                        onImageSelected = { uri -> viewModel.setSourceImage(uri) },
                        canProceed = uiState.canProceedToStep2,
                        isComputingAutoEdit = uiState.isComputingAutoEdit,
                        onAutoEdit = { viewModel.applyAutoEdit() },
                        onNext = { viewModel.setWizardStep(AppConstants.STEP_2_STYLE) }
                    )
                }

                AppConstants.STEP_2_STYLE -> {
                    WizardStep2Style(
                        sourceMode = uiState.styleBSourceMode,
                        onSourceModeChanged = { mode -> viewModel.setStyleBSourceMode(mode) },
                        customReferenceBitmap = uiState.customReferenceBitmap,
                        customReferencePalette = uiState.customReferencePalette,
                        onCustomRefSelected = { uri -> viewModel.setCustomReferenceForApply(uri) },
                        savedPresets = uiState.savedPresets,
                        selectedTheme = uiState.selectedTheme,
                        onThemeSelected = { preset -> viewModel.selectThemeToApply(preset) },
                        canProceed = uiState.canProceedToStep3,
                        onBack = { viewModel.setWizardStep(AppConstants.STEP_1_SOURCE) },
                        onNext = { viewModel.setWizardStep(AppConstants.STEP_3_PARAMS) }
                    )
                }

                AppConstants.STEP_3_PARAMS -> {
                    WizardStep3Parameters(
                        sourceBitmap = uiState.sourceBitmap,
                        previewBitmap = uiState.basePreviewBitmap,
                        isProcessingPreview = uiState.isProcessingPreview,
                        executionTimeMs = uiState.previewExecutionTimeMs,
                        selectedMethod = uiState.method,
                        onMethodSelected = { viewModel.setMethod(it) },
                        intensity = uiState.intensity,
                        onIntensityChange = { viewModel.setIntensity(it) },
                        preserveLuminance = uiState.preserveLuminance,
                        onPreserveLuminanceChange = { viewModel.setPreserveLuminance(it) },
                        skinProtection = uiState.skinProtection,
                        onSkinProtectionChange = { viewModel.setSkinProtection(it) },
                        smoothSigma = uiState.smoothSigma,
                        onSmoothSigmaChange = { viewModel.setSmoothSigma(it) },
                        neutralLock = uiState.neutralLock,
                        onNeutralLockChange = { viewModel.setNeutralLock(it) },
                        selectedLutSize = uiState.lutSize,
                        onLutSizeSelected = { viewModel.setLutSize(it) },
                        isExportingCube = uiState.isExportingCube,
                        onExportCube = {
                            viewModel.exportCube { cubePath ->
                                exportCubeLauncher.launch(File(cubePath).name)
                            }
                        },
                        isComputingAutoEdit = uiState.isComputingAutoEdit,
                        onAutoEdit = { viewModel.applyAutoEdit() },
                        onBack = { viewModel.setWizardStep(AppConstants.STEP_2_STYLE) },
                        onRun = { viewModel.runGradingFunnel() }
                    )
                }

                AppConstants.STEP_4_COMPUTE -> {
                    WizardStep4Computing(
                        stageIndex = uiState.computeStageIndex
                    )
                }

                AppConstants.STEP_5_RESULT -> {
                    WizardStep5Result(
                        sourceBitmap = uiState.sourceBitmap,
                        previewBitmap = uiState.effectivePreviewBitmap,
                        executionTimeMs = uiState.previewExecutionTimeMs,
                        intensity = uiState.intensity,
                        isGradedImageSaved = uiState.isGradedImageSaved,
                        isExportingImage = uiState.isExportingImage,
                        isExportingCube = uiState.isExportingCube,
                        exportedCubePath = uiState.exportedCubePath,
                        isAutoEditActive = uiState.isAutoEditActive,
                        autoExposure = uiState.autoExposure,
                        autoContrast = uiState.autoContrast,
                        autoHighlights = uiState.autoHighlights,
                        autoShadows = uiState.autoShadows,
                        autoVibrance = uiState.autoVibrance,
                        isEditPanelVisible = uiState.isEditPanelVisible,
                        isComputingEdit = uiState.isComputingEdit,
                        editExposure = uiState.editExposure,
                        editContrast = uiState.editContrast,
                        editHighlights = uiState.editHighlights,
                        editShadows = uiState.editShadows,
                        editWhites = uiState.editWhites,
                        editBlacks = uiState.editBlacks,
                        editVibrance = uiState.editVibrance,
                        editSaturation = uiState.editSaturation,
                        onToggleEditPanel = { viewModel.toggleEditPanel() },
                        onAutoEdit = { viewModel.applyAutoInEdit() },
                        onResetEdits = { viewModel.resetEdits() },
                        onExposureChange = { viewModel.setEditExposure(it) },
                        onContrastChange = { viewModel.setEditContrast(it) },
                        onHighlightsChange = { viewModel.setEditHighlights(it) },
                        onShadowsChange = { viewModel.setEditShadows(it) },
                        onWhitesChange = { viewModel.setEditWhites(it) },
                        onBlacksChange = { viewModel.setEditBlacks(it) },
                        onVibranceChange = { viewModel.setEditVibrance(it) },
                        onSaturationChange = { viewModel.setEditSaturation(it) },
                        onIntensityChange = { viewModel.setIntensity(it) },
                        onSaveGradedImage = { viewModel.saveGradedImageToGallery() },
                        onSaveStyleToProfile = { viewModel.openSaveThemeDialog() },
                        onExportCube = {
                            viewModel.exportCube { cubePath ->
                                exportCubeLauncher.launch(File(cubePath).name)
                            }
                        },
                        onShareCube = {
                            viewModel.createShareCubeIntent()?.let { intent ->
                                context.startActivity(Intent.createChooser(intent, "Partager la LUT (.cube)"))
                            }
                        },
                        onReset = { viewModel.resetWizard() }
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEPPER PROGRESS HEADER
// ============================================================================

@Composable
private fun StepperProgressHeader(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..AppConstants.TOTAL_WIZARD_STEPS) {
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                val circleBg = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .size(AppDimens.SwatchSize)
                        .clip(CircleShape)
                        .background(circleBg)
                        .clickable(enabled = isCompleted) { onStepClick(step) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall),
                            tint = contentColor
                        )
                    } else {
                        Text(
                            text = "$step",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                if (step < AppConstants.TOTAL_WIZARD_STEPS) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(AppDimens.BorderThick)
                            .padding(horizontal = AppDimens.SpacingExtraSmall)
                            .background(
                                if (step < currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            )
                    )
                }
            }
        }
    }
}

// ============================================================================
// WIZARD STEP 1: PHOTO SOURCE A
// ============================================================================

@Composable
private fun WizardStep1Source(
    sourceBitmap: android.graphics.Bitmap?,
    onImageSelected: (android.net.Uri) -> Unit,
    canProceed: Boolean,
    isComputingAutoEdit: Boolean = false,
    onAutoEdit: () -> Unit = {},
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_1_title),
            subtitle = stringResource(R.string.wizard_step_1_subtitle)
        )

        ImagePickerCard(
            title = stringResource(R.string.wizard_step_1_title),
            subtitle = stringResource(R.string.wizard_step_1_empty),
            badgeText = stringResource(R.string.wizard_step_1_badge),
            bitmap = sourceBitmap,
            onImageSelected = onImageSelected
        )

        if (sourceBitmap != null) {
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                border = BorderStroke(
                    width = AppDimens.BorderThin,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpacingNormal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Brightness6,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.IconSmall),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                            Text(
                                text = stringResource(R.string.btn_auto_edit),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                        Text(
                            text = stringResource(R.string.auto_edit_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = AppConstants.OPACITY_CONTAINER_HEAVY)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

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
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Text(
                text = stringResource(R.string.wizard_step_1_btn_next),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }
    }
}

// ============================================================================
// WIZARD STEP 2: STYLE REFERENCE B (PHOTO B OU PRESET SAUVEGARDÉ)
// ============================================================================

@Composable
private fun WizardStep2Style(
    sourceMode: Int,
    onSourceModeChanged: (Int) -> Unit,
    customReferenceBitmap: android.graphics.Bitmap?,
    customReferencePalette: List<String>,
    onCustomRefSelected: (android.net.Uri) -> Unit,
    savedPresets: List<ColorimetryPreset>,
    selectedTheme: ColorimetryPreset?,
    onThemeSelected: (ColorimetryPreset) -> Unit,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_2_title),
            subtitle = stringResource(R.string.wizard_step_2_subtitle)
        )

        TabRow(
            selectedTabIndex = sourceMode,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
        ) {
            Tab(
                selected = sourceMode == 0,
                onClick = { onSourceModeChanged(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.wizard_step_2_tab_photo_b),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
            Tab(
                selected = sourceMode == 1,
                onClick = { onSourceModeChanged(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = "${stringResource(R.string.wizard_step_2_tab_saved)} (${savedPresets.size})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }

        if (sourceMode == 0) {
            ImagePickerCard(
                title = stringResource(R.string.wizard_step_2_tab_photo_b),
                subtitle = stringResource(R.string.wizard_step_2_ref_empty),
                badgeText = stringResource(R.string.wizard_step_2_ref_badge),
                bitmap = customReferenceBitmap,
                onImageSelected = onCustomRefSelected
            )

            if (customReferencePalette.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                        PaletteSwatchesView(
                            title = stringResource(R.string.palettes_swatches_title),
                            colors = customReferencePalette
                        )
                    }
                }
            }
        } else {
            if (savedPresets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE))
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimens.SpacingSpacious),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconExtraLarge),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.wizard_step_2_no_saved),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedPresets, key = { it.id }) { preset ->
                        val isSelected = selectedTheme?.id == preset.id
                        Card(
                            onClick = { onThemeSelected(preset) },
                            modifier = Modifier.width(AppDimens.DoubleViewHeight),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            )
                        ) {
                            Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                                Text(
                                    text = stringResource(preset.method.displayNameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (preset.paletteHexes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                                    PaletteSwatchesView(
                                        title = "",
                                        colors = preset.paletteHexes.take(AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_2_btn_back),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .weight(1.3f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Text(
                    text = stringResource(R.string.wizard_step_2_btn_next),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
        }
    }
}

// ============================================================================
// WIZARD STEP 3: ENGINE PARAMETERS & ALGORITHMS
// ============================================================================

@Composable
private fun WizardStep3Parameters(
    sourceBitmap: android.graphics.Bitmap?,
    previewBitmap: android.graphics.Bitmap?,
    isProcessingPreview: Boolean,
    executionTimeMs: Double?,
    selectedMethod: com.stealzalut.mobile.data.model.ColorTransferMethod,
    onMethodSelected: (com.stealzalut.mobile.data.model.ColorTransferMethod) -> Unit,
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
    selectedLutSize: com.stealzalut.mobile.data.model.LutSize,
    onLutSizeSelected: (com.stealzalut.mobile.data.model.LutSize) -> Unit,
    isExportingCube: Boolean = false,
    onExportCube: () -> Unit = {},
    isComputingAutoEdit: Boolean = false,
    onAutoEdit: () -> Unit = {},
    onBack: () -> Unit,
    onRun: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)) {
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
                    imageVector = Icons.Default.ArrowBack,
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

// ============================================================================
// WIZARD STEP 4: COMPUTING SCREEN (PROGRESS ANIMATION)
// ============================================================================

@Composable
private fun WizardStep4Computing(
    stageIndex: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingMassive),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppDimens.ComputeLoaderSize),
                strokeWidth = AppDimens.BorderThick,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingExtraLarge))

            Text(
                text = stringResource(R.string.wizard_step_4_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            val stageText = when (stageIndex) {
                1 -> stringResource(R.string.wizard_step_4_stage_1)
                2 -> stringResource(R.string.wizard_step_4_stage_2)
                else -> stringResource(R.string.wizard_step_4_stage_3)
            }

            Text(
                text = stageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_4_tech_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

// ============================================================================
// WIZARD STEP 5: RESULT & 120 FPS GPU INTENSITY & LIGHTCRAFT EDIT
// ============================================================================

@Composable
private fun EditParameterSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    valueText: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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

@Composable
private fun WizardStep5Result(
    sourceBitmap: android.graphics.Bitmap?,
    previewBitmap: android.graphics.Bitmap?,
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
    onReset: () -> Unit
) {
    val isEditActive = editExposure != 0f || editContrast != 0f || editHighlights != 0f ||
            editShadows != 0f || editWhites != 0f || editBlacks != 0f ||
            editVibrance != 0f || editSaturation != 0f

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
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

        // Real-time GPU Opacity Slider (Zero Chaquopy calls, Zero disk writes)
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
                    // Header: Title + [Auto] + [Reset] Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.IconMedium)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                            Text(
                                text = stringResource(R.string.edit_panel_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // [Auto] button
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

                            // [Reset] button
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

                            // 1. Exposure (-2.00 to +3.00 EV)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_exposure),
                                value = editExposure,
                                onValueChange = onExposureChange,
                                min = -2.0f,
                                max = 3.0f,
                                valueText = String.format(java.util.Locale.US, "%+.2f EV", editExposure),
                                onResetToZero = { onExposureChange(0f) }
                            )

                            // 2. Contrast (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_contrast),
                                value = editContrast,
                                onValueChange = onContrastChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editContrast),
                                onResetToZero = { onContrastChange(0f) }
                            )

                            // 3. Highlights (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_highlights),
                                value = editHighlights,
                                onValueChange = onHighlightsChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editHighlights),
                                onResetToZero = { onHighlightsChange(0f) }
                            )

                            // 4. Shadows (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_shadows),
                                value = editShadows,
                                onValueChange = onShadowsChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editShadows),
                                onResetToZero = { onShadowsChange(0f) }
                            )

                            // 5. Whites (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_whites),
                                value = editWhites,
                                onValueChange = onWhitesChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editWhites),
                                onResetToZero = { onWhitesChange(0f) }
                            )

                            // 6. Blacks (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_blacks),
                                value = editBlacks,
                                onValueChange = onBlacksChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editBlacks),
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

                            // 1. Vibrance (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_vibrance),
                                value = editVibrance,
                                onValueChange = onVibranceChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editVibrance),
                                onResetToZero = { onVibranceChange(0f) }
                            )

                            // 2. Saturation (-100 to +100)
                            EditParameterSlider(
                                title = stringResource(R.string.edit_param_saturation),
                                value = editSaturation,
                                onValueChange = onSaturationChange,
                                min = -100f,
                                max = 100f,
                                valueText = String.format(java.util.Locale.US, "%+.0f", editSaturation),
                                onResetToZero = { onSaturationChange(0f) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        // Action 1: Save Graded Image directly to Android Gallery via MediaStore
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

        // Action 2b: Share .cube 3D LUT (if already generated or available)
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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
    }
}

// ============================================================================
// SECTION TITLE BANNER
// ============================================================================

@Composable
private fun SectionTitleBanner(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
