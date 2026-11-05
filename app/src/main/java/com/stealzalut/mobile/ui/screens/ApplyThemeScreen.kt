package com.stealzalut.mobile.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SavePresetDialog
import com.stealzalut.mobile.ui.screens.studio.StudioStep1Source
import com.stealzalut.mobile.ui.screens.studio.StudioStep2Style
import com.stealzalut.mobile.ui.screens.studio.StudioStep3Parameters
import com.stealzalut.mobile.ui.screens.studio.StudioStep4Computing
import com.stealzalut.mobile.ui.screens.studio.StudioStep5Result
import com.stealzalut.mobile.ui.screens.studio.StudioStepper
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File

/**
 * ApplyThemeScreen: Lightweight Studio Wizard Orchestrator (Steps 1 to 5).
 * Coordinates navigation and state transitions between modular Studio steps.
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
        StudioStepper(
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
                    StudioStep1Source(
                        sourceBitmap = uiState.sourceBitmap,
                        onImageSelected = { uri -> viewModel.setSourceImage(uri) },
                        canProceed = uiState.canProceedToStep2,
                        isComputingAutoEdit = uiState.isComputingAutoEdit,
                        onAutoEdit = { viewModel.applyAutoEdit() },
                        onNext = { viewModel.setWizardStep(AppConstants.STEP_2_STYLE) }
                    )
                }

                AppConstants.STEP_2_STYLE -> {
                    StudioStep2Style(
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
                    StudioStep3Parameters(
                        sourceBitmap = uiState.sourceBitmap,
                        previewBitmap = uiState.basePreviewBitmap,
                        isProcessingPreview = uiState.isProcessingPreview,
                        executionTimeMs = if (uiState.settings.developerMode) uiState.previewExecutionTimeMs else null,
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
                    StudioStep4Computing(
                        stageIndex = uiState.computeStageIndex
                    )
                }

                AppConstants.STEP_5_RESULT -> {
                    StudioStep5Result(
                        sourceBitmap = uiState.sourceBitmap,
                        previewBitmap = uiState.effectivePreviewBitmap,
                        executionTimeMs = if (uiState.settings.developerMode) uiState.previewExecutionTimeMs else null,
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
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_cube_title)))
                            }
                        },
                        onReset = { viewModel.resetWizard() }
                    )
                }
            }
        }
    }
}
