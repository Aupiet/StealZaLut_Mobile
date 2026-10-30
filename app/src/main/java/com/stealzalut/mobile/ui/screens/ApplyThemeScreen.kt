package com.stealzalut.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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

    val exportCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(AppConstants.MIME_TYPE_OCTET_STREAM)
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
                        selectedMethod = uiState.method,
                        onMethodSelected = { viewModel.setMethod(it) },
                        intensity = uiState.intensity,
                        onIntensityChange = { viewModel.setIntensity(it) },
                        selectedLutSize = uiState.lutSize,
                        onLutSizeSelected = { viewModel.setLutSize(it) },
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
                        previewBitmap = uiState.basePreviewBitmap,
                        executionTimeMs = uiState.previewExecutionTimeMs,
                        intensity = uiState.intensity,
                        isGradedImageSaved = uiState.isGradedImageSaved,
                        isExportingImage = uiState.isExportingImage,
                        isExportingCube = uiState.isExportingCube,
                        onIntensityChange = { viewModel.setIntensity(it) },
                        onSaveGradedImage = { viewModel.saveGradedImageToGallery() },
                        onSaveStyleToProfile = { viewModel.openSaveThemeDialog() },
                        onExportCube = {
                            viewModel.exportCube { cubePath ->
                                exportCubeLauncher.launch(File(cubePath).name)
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

        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

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
    selectedMethod: com.stealzalut.mobile.data.model.ColorTransferMethod,
    onMethodSelected: (com.stealzalut.mobile.data.model.ColorTransferMethod) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    selectedLutSize: com.stealzalut.mobile.data.model.LutSize,
    onLutSizeSelected: (com.stealzalut.mobile.data.model.LutSize) -> Unit,
    onBack: () -> Unit,
    onRun: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_3_title),
            subtitle = stringResource(R.string.wizard_step_3_subtitle)
        )

        MethodSelector(
            selectedMethod = selectedMethod,
            onMethodSelected = onMethodSelected
        )

        IntensitySlider(
            intensity = intensity,
            onIntensityChange = onIntensityChange
        )

        LutSizeSelector(
            selectedSize = selectedLutSize,
            onSizeSelected = onLutSizeSelected
        )

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
// WIZARD STEP 5: RESULT & 120 FPS GPU INTENSITY
// ============================================================================

@Composable
private fun WizardStep5Result(
    sourceBitmap: android.graphics.Bitmap?,
    previewBitmap: android.graphics.Bitmap?,
    executionTimeMs: Double?,
    intensity: Float,
    isGradedImageSaved: Boolean,
    isExportingImage: Boolean,
    isExportingCube: Boolean,
    onIntensityChange: (Float) -> Unit,
    onSaveGradedImage: () -> Unit,
    onSaveStyleToProfile: () -> Unit,
    onExportCube: () -> Unit,
    onReset: () -> Unit
) {
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

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        // Action 1: Save Graded Image directly to Android Gallery via MediaStore
        Button(
            onClick = onSaveGradedImage,
            enabled = !isExportingImage,
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
                    text = stringResource(R.string.settings_export_dim),
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
            enabled = !isExportingCube,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
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

        // Action 3: Save Preset Recipe
        Button(
            onClick = onSaveStyleToProfile,
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
