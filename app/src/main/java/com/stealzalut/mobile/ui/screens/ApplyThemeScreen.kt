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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SavePresetDialog
import com.stealzalut.mobile.ui.screens.studio.StudioBottomBar
import com.stealzalut.mobile.ui.screens.studio.StudioLandingMenu
import com.stealzalut.mobile.ui.screens.studio.StudioStep1Source
import com.stealzalut.mobile.ui.screens.studio.StudioStep2Style
import com.stealzalut.mobile.ui.screens.studio.StudioStep3Parameters
import com.stealzalut.mobile.ui.screens.studio.StudioStep4Computing
import com.stealzalut.mobile.ui.screens.studio.StudioStep5Result
import com.stealzalut.mobile.ui.screens.studio.StudioStepper
import com.stealzalut.mobile.ui.screens.studio.StudioTopBar
import com.stealzalut.mobile.ui.theme.szlColors
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File

@Composable
/**
 * ApplyThemeScreen: Lightweight Studio Wizard Orchestrator (Steps 1 to 5).
 * Coordinates navigation and state transitions between modular Studio steps.
 */
fun ApplyThemeScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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

    var showExitStudioConfirmDialog by remember { mutableStateOf(false) }

    if (showExitStudioConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitStudioConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.studio_exit_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.studio_exit_dialog_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitStudioConfirmDialog = false
                        viewModel.resetWizard()
                    }
                ) {
                    Text(stringResource(R.string.studio_exit_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitStudioConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val hasImage = uiState.sourceBitmap != null

    if (hasImage) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.szlColors.photoDarkroom)
        ) {
            StudioTopBar(
                currentStep = uiState.wizardStep,
                isZoneMaskActive = uiState.isZoneMaskActive,
                canProceed = when (uiState.wizardStep) {
                    AppConstants.STEP_1_SOURCE -> uiState.canProceedToStep2
                    AppConstants.STEP_2_STYLE -> uiState.canProceedToStep3
                    else -> true
                },
                isComputing = uiState.computeStageIndex >= 0 && uiState.wizardStep == AppConstants.STEP_4_COMPUTE,
                onBack = {
                    when {
                        uiState.wizardStep == AppConstants.STEP_5_RESULT -> viewModel.setWizardStep(AppConstants.STEP_3_PARAMS)
                        uiState.wizardStep > 1 -> viewModel.setWizardStep(uiState.wizardStep - 1)
                        else -> showExitStudioConfirmDialog = true
                    }
                },
                onNext = {
                    when (uiState.wizardStep) {
                        AppConstants.STEP_1_SOURCE -> viewModel.setWizardStep(AppConstants.STEP_2_STYLE)
                        AppConstants.STEP_2_STYLE -> viewModel.setWizardStep(AppConstants.STEP_3_PARAMS)
                        AppConstants.STEP_3_PARAMS -> viewModel.runGradingFunnel()
                        AppConstants.STEP_5_RESULT -> viewModel.resetWizard()
                    }
                },
                onToggleZoneMaskActive = {
                    if (uiState.wizardStep == AppConstants.STEP_3_PARAMS) {
                        viewModel.onSetZoneMaskActive(!uiState.isZoneMaskActive)
                    }
                },
                onSaveSession = { viewModel.saveStudioSession() },
                onRestoreSession = { viewModel.restoreStudioSession() },
                onResetParameters = {
                    viewModel.setIntensity(1.0f)
                    viewModel.setPreserveLuminance(0.8f)
                    viewModel.setSkinProtection(0.8f)
                    viewModel.setSmoothSigma(0.0f)
                    viewModel.setNeutralLock(0.5f)
                    viewModel.onResetZoneCorrespondences()
                },
                onCloseSession = { showExitStudioConfirmDialog = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = uiState.wizardStep,
                    modifier = Modifier.fillMaxSize(),
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical)
                            ) {
                                StudioStep1Source(
                                    sourceBitmap = uiState.sourceBitmap,
                                    onImageSelected = { uri -> viewModel.setSourceImage(uri) },
                                    canProceed = uiState.canProceedToStep2,
                                    onReset = { viewModel.resetWizard() },
                                    onNext = { viewModel.setWizardStep(AppConstants.STEP_2_STYLE) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        AppConstants.STEP_2_STYLE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical)
                            ) {
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
                                    onNext = { viewModel.setWizardStep(AppConstants.STEP_3_PARAMS) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
                                isAnalyzingZones = uiState.isAnalyzingZones,
                                isGeneratingZoneMask = uiState.isGeneratingZoneMask,
                                hasZoneAnalysis = uiState.zoneAnalysis != null && uiState.referenceZoneAnalysis != null,
                                sourceZoneAnalysis = uiState.zoneAnalysis,
                                referenceZoneAnalysis = uiState.referenceZoneAnalysis,
                                zoneTransfer = uiState.zoneTransfer,
                                selectedZoneIndex = uiState.selectedZoneIndex,
                                maskPath = uiState.zoneMaskPath,
                                maskMode = uiState.zoneMaskMode,
                                isZoneMaskActive = uiState.isZoneMaskActive,
                                onZoneCorrespondenceChanged = { src, ref -> viewModel.onZoneCorrespondenceChanged(src, ref) },
                                onResetCorrespondences = { viewModel.onResetZoneCorrespondences() },
                                onZoneSelected = { viewModel.onZoneSelected(it) },
                                onToggleMaskMode = { viewModel.onToggleZoneMaskMode() },
                                onToggleZoneMaskActive = { viewModel.onSetZoneMaskActive(!uiState.isZoneMaskActive) },
                                onSaveSession = { viewModel.saveStudioSession() },
                                onRestoreSession = { viewModel.restoreStudioSession() },
                                onResetParameters = {
                                    viewModel.setIntensity(1.0f)
                                    viewModel.setPreserveLuminance(0.8f)
                                    viewModel.setSkinProtection(0.8f)
                                    viewModel.setSmoothSigma(0.0f)
                                    viewModel.setNeutralLock(0.5f)
                                    viewModel.onResetZoneCorrespondences()
                                },
                                onAnalyzeZones = { viewModel.analyzeCurrentZones() },
                                isExportingCube = uiState.isExportingCube,
                                onExportCube = {
                                    viewModel.exportAndShareCube(context)
                                },
                                onBack = { viewModel.setWizardStep(AppConstants.STEP_2_STYLE) },
                                onRun = { viewModel.runGradingFunnel() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        AppConstants.STEP_4_COMPUTE -> {
                            StudioStep4Computing(
                                stageIndex = uiState.computeStageIndex,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        AppConstants.STEP_5_RESULT -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical)
                            ) {
                                StudioStep5Result(
                                    sourceBitmap = uiState.sourceBitmap,
                                    previewBitmap = uiState.effectivePreviewBitmap,
                                    executionTimeMs = if (uiState.settings.developerMode) uiState.previewExecutionTimeMs else null,
                                    intensity = uiState.intensity,
                                    isGradedImageSaved = uiState.isGradedImageSaved,
                                    isExportingImage = uiState.isExportingImage,
                                    isExportingCube = uiState.isExportingCube,
                                    exportedCubePath = uiState.exportedCubePath,
                                    onIntensityChange = { viewModel.setIntensity(it) },
                                    onOpenInEditor = { viewModel.openInEditorFromStudio() },
                                    onSaveGradedImage = { viewModel.saveGradedImageToGallery() },
                                    onSaveStyleToProfile = { viewModel.openSaveThemeDialog() },
                                    onExportCube = {
                                        viewModel.exportAndShareCube(context)
                                    },
                                    onShareCube = {
                                        viewModel.exportAndShareCube(context)
                                    },
                                    onReset = { viewModel.resetWizard() }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.wizardStep != AppConstants.STEP_4_COMPUTE) {
                StudioBottomBar(
                    currentStep = uiState.wizardStep,
                    canProceed = when (uiState.wizardStep) {
                        AppConstants.STEP_1_SOURCE -> uiState.canProceedToStep2
                        AppConstants.STEP_2_STYLE -> uiState.canProceedToStep3
                        else -> true
                    },
                    onBack = {
                        when {
                            uiState.wizardStep == AppConstants.STEP_5_RESULT -> viewModel.setWizardStep(AppConstants.STEP_3_PARAMS)
                            uiState.wizardStep > 1 -> viewModel.setWizardStep(uiState.wizardStep - 1)
                            else -> showExitStudioConfirmDialog = true
                        }
                    },
                    onNext = {
                        when (uiState.wizardStep) {
                            AppConstants.STEP_1_SOURCE -> viewModel.setWizardStep(AppConstants.STEP_2_STYLE)
                            AppConstants.STEP_2_STYLE -> viewModel.setWizardStep(AppConstants.STEP_3_PARAMS)
                            AppConstants.STEP_3_PARAMS -> viewModel.runGradingFunnel()
                            AppConstants.STEP_5_RESULT -> viewModel.resetWizard()
                        }
                    }
                )
            }
        }
    } else {
        StudioLandingMenu(
            onImageSelected = { uri -> viewModel.setSourceImage(uri) },
            savedPresets = uiState.savedPresets,
            modifier = modifier
        )
    }
}
