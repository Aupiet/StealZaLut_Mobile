package com.stealzalut.mobile.ui.viewmodel.delegate

import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * StudioFlowDelegate: Manages navigation, wizard steps, and step validation in the Studio Funnel.
 */
class StudioFlowDelegate(
    private val stateFlow: MutableStateFlow<StealZaLutUiState>
) {
    fun navigateTo(destination: AppDestination) {
        stateFlow.update { it.copy(currentDestination = destination) }
    }

    fun setWizardStep(step: Int) {
        stateFlow.update { state ->
            val canProceedToStep2 = state.sourceBitmap != null
            val canProceedToStep3 = (state.styleBSourceMode == 0 && state.customReferenceBitmap != null) ||
                    (state.styleBSourceMode == 1 && state.selectedTheme != null)

            val isAllowed = when (step) {
                AppConstants.STEP_1_SOURCE -> true
                AppConstants.STEP_2_STYLE -> canProceedToStep2
                AppConstants.STEP_3_PARAMS -> canProceedToStep2 && canProceedToStep3
                AppConstants.STEP_4_COMPUTE -> canProceedToStep2 && canProceedToStep3
                AppConstants.STEP_5_RESULT -> canProceedToStep2 && canProceedToStep3
                else -> false
            }

            if (isAllowed || step < state.wizardStep) {
                state.copy(wizardStep = step)
            } else {
                state
            }
        }
    }

    fun setStyleBSourceMode(mode: Int) {
        stateFlow.update { it.copy(styleBSourceMode = mode) }
    }

    fun openSaveThemeDialog() {
        stateFlow.update { it.copy(isSavingThemeDialogVisible = true) }
    }

    fun dismissSaveThemeDialog() {
        stateFlow.update { it.copy(isSavingThemeDialogVisible = false) }
    }

    fun resetWizard() {
        stateFlow.update {
            it.copy(
                wizardStep = AppConstants.STEP_1_SOURCE,
                sourceUri = null,
                sourceBitmap = null,
                sourcePath = null,
                customReferenceUri = null,
                customReferenceBitmap = null,
                customReferencePath = null,
                customReferencePalette = kotlinx.collections.immutable.persistentListOf(),
                basePreviewBitmap = null,
                previewPath = null,
                exportedCubePath = null,
                exportedImagePath = null,
                isGradedImageSaved = false,
                isAutoEditActive = false,
                editedPreviewBitmap = null,
                editedPreviewPath = null,
                editExposure = 0.0f,
                editContrast = 0.0f,
                editHighlights = 0.0f,
                editShadows = 0.0f,
                editWhites = 0.0f,
                editBlacks = 0.0f,
                editVibrance = 0.0f,
                editSaturation = 0.0f
            )
        }
    }

    fun setSplitPosition(position: Float) {
        stateFlow.update { it.copy(splitPosition = position.coerceIn(0f, 1f)) }
    }

    fun toggleSplitWipeMode() {
        stateFlow.update { it.copy(isSplitWipeMode = !it.isSplitWipeMode) }
    }
}
