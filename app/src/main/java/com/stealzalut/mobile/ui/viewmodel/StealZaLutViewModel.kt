package com.stealzalut.mobile.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.model.AutoEditResult
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.EditResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.data.model.Step2Model
import com.stealzalut.mobile.data.model.Step3Model
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.data.repository.SettingsRepository
import com.stealzalut.mobile.ui.navigation.AppDestination
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class PreviewDebounceRequest(
    val srcPath: String,
    val refPath: String,
    val method: ColorTransferMethod,
    val intensity: Float,
    val preserveLuminance: Float,
    val skinProtection: Float,
    val smoothSigma: Float,
    val neutralLock: Float
)

private data class EditDebounceRequest(
    val imagePath: String,
    val exposure: Float,
    val contrast: Float,
    val highlights: Float,
    val shadows: Float,
    val whites: Float,
    val blacks: Float,
    val vibrance: Float,
    val saturation: Float
)

private data class StandaloneEditorDebounceRequest(
    val srcPath: String,
    val intensity: Float,
    val lumaLock: Float,
    val neutralAxisLock: Float,
    val skinToneLock: Float,
    val extremesRolloff: Float,
    val spatial3d: Float,
    val exposure: Float,
    val contrast: Float,
    val highlights: Float,
    val shadows: Float,
    val temperature: Float,
    val tint: Float,
    val vibrance: Float,
    val saturation: Float,
    val clarity: Float,
    val sharpen: Float,
    val grain: Float,
    val customLutPath: String?
)

@OptIn(FlowPreview::class)
class StealZaLutViewModel(application: Application) : AndroidViewModel(application) {

    private val bridgeRepository = PythonBridgeRepository(application.applicationContext)
    private val presetRepository = PresetRepository(application.applicationContext)
    private val savedImageRepository = SavedImageRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StealZaLutUiState())
    val uiState: StateFlow<StealZaLutUiState> = _uiState.asStateFlow()

    private val previewDebounceFlow = MutableSharedFlow<PreviewDebounceRequest>(replay = 1)
    private val editDebounceFlow = MutableSharedFlow<EditDebounceRequest>(replay = 1)
    private val editorDebounceFlow = MutableSharedFlow<StandaloneEditorDebounceRequest>(replay = 1)
    private var activeGradingJob: Job? = null
    private var activeEditJob: Job? = null
    private var activeStandaloneJob: Job? = null
    private var pendingExportCubePath: String? = null

    init {
        val initialSettings = settingsRepository.getSettings()
        val detectedTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank {
            java.util.Locale.getDefault().language
        }
        _uiState.update {
            it.copy(
                settings = initialSettings,
                method = initialSettings.defaultMethod,
                lutSize = initialSettings.defaultLutSize,
                currentLanguageTag = detectedTag
            )
        }
        loadSavedPresets()
        loadSavedImages()
        loadEditingPresets()
        refreshCacheSize()

        viewModelScope.launch {
            val presets = presetRepository.getAllPresets()
            if (presets.isNotEmpty()) {
                _uiState.update { it.copy(selectedTheme = presets.first()) }
            }
        }

        // Debounced preview flow (200ms) for real-time parameter tweaking
        viewModelScope.launch {
            previewDebounceFlow
                .debounce(200L)
                .distinctUntilChanged()
                .collect { req ->
                    computePythonPreview(req)
                }
        }

        // Debounced edit flow (150ms) for LightCraft edit parameter tweaking
        viewModelScope.launch {
            editDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { req ->
                    computeEditedPreview(req)
                }
        }

        // Debounced standalone photo editor preview (150ms)
        viewModelScope.launch {
            editorDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { req ->
                    computeStandaloneEditorPreview(req)
                }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
        when (destination) {
            AppDestination.EDITOR -> loadEditingPresets()
            AppDestination.PALETTES -> loadSavedPresets()
            AppDestination.PROFILE -> {
                loadSavedPresets()
                loadSavedImages()
            }
            AppDestination.SETTINGS -> refreshCacheSize()
            else -> {}
        }
    }

    // ========================================================================
    // 1. STUDIO ÉTALONNAGE
    // ========================================================================

    fun setSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "source_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    _uiState.update {
                        it.copy(
                            sourceUri = uri,
                            sourcePath = file.absolutePath,
                            sourceBitmap = thumbnail,
                            isProcessingPreview = false,
                            basePreviewBitmap = null,
                            previewPath = null
                        )
                    }
                    if (_uiState.value.effectiveReferencePath != null) {
                        triggerDebouncedPreview()
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = "Erreur chargement source : ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun selectThemeToApply(preset: ColorimetryPreset) {
        _uiState.update {
            it.copy(
                selectedTheme = preset,
                styleBSourceMode = 1,
                customReferencePath = null,
                customReferenceBitmap = null,
                customReferenceUri = null,
                method = preset.method,
                intensity = preset.intensity,
                lutSize = preset.lutSize,
                userMessage = "Preset '${preset.name}' sélectionné !"
            )
        }
        if (_uiState.value.sourcePath != null && _uiState.value.effectiveReferencePath != null) {
            triggerDebouncedPreview()
        }
    }

    fun setCustomReferenceForApply(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "custom_ref_apply.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    val palette = withContext(Dispatchers.IO) {
                        bridgeRepository.extractPalette(file.absolutePath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
                    }
                    _uiState.update {
                        it.copy(
                            styleBSourceMode = 0,
                            customReferenceUri = uri,
                            customReferencePath = file.absolutePath,
                            customReferenceBitmap = thumbnail,
                            customReferencePalette = palette.toImmutableList(),
                            selectedTheme = null,
                            isProcessingPreview = false
                        )
                    }
                    if (_uiState.value.sourcePath != null) {
                        triggerDebouncedPreview()
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = "Erreur chargement référence : ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun setMethod(method: ColorTransferMethod) {
        if (_uiState.value.method == method) return
        _uiState.update { it.copy(method = method) }
        if (_uiState.value.canApply) triggerDebouncedPreview()
    }

    fun setIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0f, 1f)
        if (_uiState.value.intensity == clamped) return
        _uiState.update {
            it.copy(
                intensity = clamped,
                isGradedImageSaved = false
            )
        }
        triggerDebouncedPreview()
    }

    fun setPreserveLuminance(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_uiState.value.preserveLuminance == clamped) return
        _uiState.update {
            it.copy(
                preserveLuminance = clamped,
                isGradedImageSaved = false
            )
        }
        triggerDebouncedPreview()
    }

    fun setSkinProtection(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_uiState.value.skinProtection == clamped) return
        _uiState.update {
            it.copy(
                skinProtection = clamped,
                isGradedImageSaved = false
            )
        }
        triggerDebouncedPreview()
    }

    fun setSmoothSigma(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_uiState.value.smoothSigma == clamped) return
        _uiState.update {
            it.copy(
                smoothSigma = clamped,
                isGradedImageSaved = false
            )
        }
        triggerDebouncedPreview()
    }

    fun setNeutralLock(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_uiState.value.neutralLock == clamped) return
        _uiState.update {
            it.copy(
                neutralLock = clamped,
                isGradedImageSaved = false
            )
        }
        triggerDebouncedPreview()
    }

    fun setLutSize(size: LutSize) {
        _uiState.update { it.copy(lutSize = size) }
    }

    fun setSplitPosition(position: Float) {
        _uiState.update { it.copy(splitPosition = position.coerceIn(0f, 1f)) }
    }

    fun toggleSplitWipeMode() {
        _uiState.update { it.copy(isSplitWipeMode = !it.isSplitWipeMode) }
    }

    private fun triggerDebouncedPreview() {
        val current = _uiState.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return
        viewModelScope.launch {
            previewDebounceFlow.emit(
                PreviewDebounceRequest(
                    srcPath = src,
                    refPath = ref,
                    method = current.method,
                    intensity = current.intensity,
                    preserveLuminance = current.preserveLuminance,
                    skinProtection = current.skinProtection,
                    smoothSigma = current.smoothSigma,
                    neutralLock = current.neutralLock
                )
            )
        }
    }

    fun setWizardStep(step: Int) {
        _uiState.update { it.copy(wizardStep = step) }
        if (step == AppConstants.STEP_3_PARAMS) {
            triggerDebouncedPreview()
        }
    }

    fun setStyleBSourceMode(mode: Int) {
        _uiState.update { it.copy(styleBSourceMode = mode) }
    }

    fun openSaveThemeDialog() {
        _uiState.update { it.copy(isSavingThemeDialogVisible = true) }
    }

    fun dismissSaveThemeDialog() {
        _uiState.update { it.copy(isSavingThemeDialogVisible = false) }
    }

    fun resetWizard() {
        activeEditJob?.cancel()
        _uiState.update {
            it.copy(
                wizardStep = AppConstants.STEP_1_SOURCE,
                computeStageIndex = 1,
                basePreviewBitmap = null,
                previewPath = null,
                isAutoEditActive = false,
                isEditPanelVisible = false,
                isComputingEdit = false,
                editExposure = 0f,
                editContrast = 0f,
                editHighlights = 0f,
                editShadows = 0f,
                editWhites = 0f,
                editBlacks = 0f,
                editVibrance = 0f,
                editSaturation = 0f,
                editedPreviewBitmap = null,
                editedPreviewPath = null
            )
        }
    }

    fun toggleEditPanel() {
        _uiState.update { it.copy(isEditPanelVisible = !it.isEditPanelVisible) }
    }

    fun setEditExposure(value: Float) {
        val clamped = value.coerceIn(-2.0f, 3.0f)
        if (_uiState.value.editExposure == clamped) return
        _uiState.update { it.copy(editExposure = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditContrast(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editContrast == clamped) return
        _uiState.update { it.copy(editContrast = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditHighlights(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editHighlights == clamped) return
        _uiState.update { it.copy(editHighlights = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditShadows(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editShadows == clamped) return
        _uiState.update { it.copy(editShadows = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditWhites(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editWhites == clamped) return
        _uiState.update { it.copy(editWhites = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditBlacks(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editBlacks == clamped) return
        _uiState.update { it.copy(editBlacks = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditVibrance(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editVibrance == clamped) return
        _uiState.update { it.copy(editVibrance = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditSaturation(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (_uiState.value.editSaturation == clamped) return
        _uiState.update { it.copy(editSaturation = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    private fun triggerDebouncedEdit() {
        val current = _uiState.value
        val imagePath = current.previewPath ?: current.sourcePath ?: return
        viewModelScope.launch {
            editDebounceFlow.emit(
                EditDebounceRequest(
                    imagePath = imagePath,
                    exposure = current.editExposure,
                    contrast = current.editContrast,
                    highlights = current.editHighlights,
                    shadows = current.editShadows,
                    whites = current.editWhites,
                    blacks = current.editBlacks,
                    vibrance = current.editVibrance,
                    saturation = current.editSaturation
                )
            )
        }
    }

    /**
     * Triggered by the [✨ Auto] action inside the editing menu.
     * Evaluates auto tone mapping on the active photo, updates Stage 3 sliders
     * (exposure, contrast, highlights, shadows, vibrance) in real time so the user
     * can visually inspect and fine-tune them, and refreshes the preview.
     */
    fun applyAutoInEdit(isStandalone: Boolean = false) {
        val current = _uiState.value
        val imagePath = if (isStandalone) {
            current.editorPreviewPath ?: current.editorSourcePath ?: return
        } else {
            current.previewPath ?: current.sourcePath ?: return
        }

        val targetJob = if (isStandalone) activeStandaloneJob else activeEditJob
        targetJob?.cancel()

        val job = viewModelScope.launch {
            _uiState.update { it.copy(isComputingEdit = true, errorMessage = null) }
            val autoSettings = bridgeRepository.computeAutoSettings(imagePath)
            when (autoSettings) {
                is AutoSettingsResult.Success -> {
                    val expNorm = autoSettings.exposure.coerceIn(-2.0f, 2.0f)
                    val contrastNorm = (autoSettings.contrast / 100.0f).coerceIn(-1.0f, 1.0f)
                    val highlightsNorm = (autoSettings.highlights / 100.0f).coerceIn(-1.0f, 1.0f)
                    val shadowsNorm = (autoSettings.shadows / 100.0f).coerceIn(-1.0f, 1.0f)
                    val vibranceNorm = (autoSettings.vibrance / 100.0f).coerceIn(-1.0f, 1.0f)

                    _uiState.update {
                        it.copy(
                            isComputingEdit = false,
                            isAutoEditActive = true,
                            // Legacy edit state
                            editExposure = autoSettings.exposure,
                            editContrast = autoSettings.contrast,
                            editHighlights = autoSettings.highlights,
                            editShadows = autoSettings.shadows,
                            editWhites = autoSettings.whites,
                            editBlacks = autoSettings.blacks,
                            editVibrance = autoSettings.vibrance,
                            editSaturation = autoSettings.saturation,
                            // 3-Stage pipeline state (sliders in UI)
                            step3Exposure = expNorm,
                            step3Contrast = contrastNorm,
                            step3Highlights = highlightsNorm,
                            step3Shadows = shadowsNorm,
                            step3Vibrance = vibranceNorm,
                            userMessage = "Auto-Edit appliqué : Exposition %+.2f EV | Contraste %+.0f%% | Ombres %+.0f%%".format(
                                expNorm,
                                contrastNorm * 100f,
                                shadowsNorm * 100f
                            )
                        )
                    }

                    if (isStandalone) {
                        scheduleEditorPreviewUpdate()
                    } else {
                        computeEditedPreview(
                            EditDebounceRequest(
                                imagePath = imagePath,
                                exposure = autoSettings.exposure,
                                contrast = autoSettings.contrast,
                                highlights = autoSettings.highlights,
                                shadows = autoSettings.shadows,
                                whites = autoSettings.whites,
                                blacks = autoSettings.blacks,
                                vibrance = autoSettings.vibrance,
                                saturation = autoSettings.saturation
                            )
                        )
                    }
                }
                is AutoSettingsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isComputingEdit = false,
                            errorMessage = autoSettings.error.userMessage
                        )
                    }
                }
            }
        }

        if (isStandalone) {
            activeStandaloneJob = job
        } else {
            activeEditJob = job
        }
    }

    /**
     * Triggered by the [Reset] button inside the Step 5 Edit Panel.
     * Resets all edit parameters to neutral (0) and restores the base preview.
     */
    fun resetEdits() {
        activeEditJob?.cancel()
        _uiState.update {
            it.copy(
                isAutoEditActive = false,
                isComputingEdit = false,
                editExposure = 0f,
                editContrast = 0f,
                editHighlights = 0f,
                editShadows = 0f,
                editWhites = 0f,
                editBlacks = 0f,
                editVibrance = 0f,
                editSaturation = 0f,
                editedPreviewBitmap = null,
                editedPreviewPath = null,
                isGradedImageSaved = false
            )
        }
    }

    private suspend fun computeEditedPreview(req: EditDebounceRequest) {
        val isAnyEditActive = req.exposure != 0f || req.contrast != 0f || req.highlights != 0f ||
                req.shadows != 0f || req.whites != 0f || req.blacks != 0f || req.vibrance != 0f || req.saturation != 0f

        if (!isAnyEditActive) {
            _uiState.update {
                it.copy(
                    isComputingEdit = false,
                    editedPreviewBitmap = null,
                    editedPreviewPath = null
                )
            }
            return
        }

        activeEditJob?.cancel()
        activeEditJob = viewModelScope.launch {
            _uiState.update { it.copy(isComputingEdit = true, errorMessage = null) }

            val result = bridgeRepository.applyImageEdits(
                imagePath = req.imagePath,
                exposure = req.exposure,
                contrast = req.contrast,
                highlights = req.highlights,
                shadows = req.shadows,
                whites = req.whites,
                blacks = req.blacks,
                vibrance = req.vibrance,
                saturation = req.saturation
            )

            when (result) {
                is EditResult.Success -> {
                    val editedBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.editedPath, 1080)
                    }
                    _uiState.update {
                        it.copy(
                            isComputingEdit = false,
                            editedPreviewBitmap = editedBmp,
                            editedPreviewPath = result.editedPath,
                            previewExecutionTimeMs = result.executionTimeMs,
                            isGradedImageSaved = false
                        )
                    }
                }
                is EditResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isComputingEdit = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes the reverse-engineered LightCraft Auto Edit feature.
     * Evaluates linear Rec.2020 luminance against 18% middle gray to compute optimal exposure,
     * applies calibrated contrast, highlights, shadows, whites, blacks, and vibrance,
     * and displays the 120 FPS WYSIWYG result directly in Step 5.
     */
    fun applyAutoEdit() {
        val current = _uiState.value
        val srcPath = current.sourcePath
        if (srcPath.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez d'abord sélectionner une photo source.") }
            return
        }

        activeGradingJob?.cancel()
        activeGradingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isComputingAutoEdit = true,
                    isProcessingPreview = true,
                    errorMessage = null
                )
            }

            val result = bridgeRepository.generateAutoEditPreview(srcPath)

            when (result) {
                is AutoEditResult.Success -> {
                    val rawBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1080)
                    }

                    _uiState.update {
                        it.copy(
                            isComputingAutoEdit = false,
                            isProcessingPreview = false,
                            isAutoEditActive = true,
                            previewPath = result.previewPath,
                            basePreviewBitmap = rawBmp,
                            autoExposure = result.exposure,
                            autoContrast = result.contrast,
                            autoHighlights = result.highlights,
                            autoShadows = result.shadows,
                            autoWhites = result.whites,
                            autoBlacks = result.blacks,
                            autoVibrance = result.vibrance,
                            previewExecutionTimeMs = result.executionTimeMs,
                            wizardStep = AppConstants.STEP_5_RESULT,
                            userMessage = "Auto Edit appliqué : Exposition %+.2f EV | Contraste +68 | Ombres +52".format(result.exposure)
                        )
                    }
                }
                is AutoEditResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isComputingAutoEdit = false,
                            isProcessingPreview = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes color-grading computation:
     * Cancels any active computation job to safeguard Python GIL and starts funnel.
     */
    fun runGradingFunnel() {
        val current = _uiState.value
        val srcPath = current.sourcePath ?: return
        val refPath = current.effectiveReferencePath ?: return

        activeGradingJob?.cancel()
        activeGradingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    wizardStep = AppConstants.STEP_4_COMPUTE,
                    computeStageIndex = 1,
                    isProcessingPreview = true,
                    errorMessage = null
                )
            }

            kotlinx.coroutines.delay(AppConstants.COMPUTE_STAGE_DELAY_MS)
            _uiState.update { it.copy(computeStageIndex = 2) }

            val result = bridgeRepository.generatePreview(
                srcPath = srcPath,
                refPath = refPath,
                method = current.method,
                intensity = current.intensity,
                preserveLuminance = current.preserveLuminance,
                skinProtection = current.skinProtection,
                smoothSigma = current.smoothSigma,
                neutralLock = current.neutralLock
            )

            kotlinx.coroutines.delay(AppConstants.COMPUTE_STAGE_DELAY_MS)
            _uiState.update { it.copy(computeStageIndex = 3) }

            when (result) {
                is PreviewResult.Success -> {
                    val rawTransferredBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1080)
                    }

                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            previewPath = result.previewPath,
                            basePreviewBitmap = rawTransferredBmp,
                            previewExecutionTimeMs = result.executionTimeMs,
                            wizardStep = AppConstants.STEP_5_RESULT,
                            isGradedImageSaved = false
                        )
                    }
                }
                is PreviewResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = result.error.userMessage,
                            wizardStep = AppConstants.STEP_3_PARAMS
                        )
                    }
                }
            }
        }
    }

    /**
     * Saves the final graded image directly to the native Android MediaStore gallery (Pictures/StealZaLUT).
     */
    fun saveGradedImageToGallery() {
        val current = _uiState.value
        if (current.isExportingImage) return
        val srcPath = current.sourcePath ?: return

        if (current.isEditActive) {
            val previewPath = current.effectivePreviewPath ?: current.previewPath
            if (previewPath == null || !File(previewPath).exists()) {
                _uiState.update { it.copy(isExportingImage = false, errorMessage = "Aperçu édité introuvable.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isExportingImage = true, errorMessage = null) }
                val displayName = "StealZaLUT_Edited_${System.currentTimeMillis()}.${if (current.settings.exportFormat.equals("PNG", true)) "png" else "jpg"}"
                val mediaStoreResult = bridgeRepository.saveImageToMediaStore(
                    imagePath = previewPath,
                    displayName = displayName,
                    mimeType = if (current.settings.exportFormat.equals("PNG", true)) "image/png" else "image/jpeg"
                )

                mediaStoreResult.fold(
                    onSuccess = {
                        savedImageRepository.recordGradedImage(
                            sourceFilePath = previewPath,
                            width = 1080,
                            height = 1080,
                            themeName = if (current.isAutoEditActive) "Auto Edit (LightCraft)" else "Édition LightCraft",
                            method = if (current.isAutoEditActive) "auto-edit" else "lightcraft-edit",
                            intensity = current.intensity,
                            referenceImagePath = current.effectiveReferencePath,
                            paletteHexes = emptyList()
                        )
                        loadSavedImages()
                        _uiState.update {
                            it.copy(
                                isExportingImage = false,
                                exportedImagePath = previewPath,
                                isGradedImageSaved = true,
                                userMessage = "Photo enregistrée dans la Galerie et votre Bibliothèque !"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isExportingImage = false,
                                errorMessage = "Erreur MediaStore : ${error.localizedMessage}"
                            )
                        }
                    }
                )
            }
            return
        }

        val refPath = current.effectiveReferencePath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingImage = true, errorMessage = null) }

            val result = bridgeRepository.exportGradedImage(
                srcPath = srcPath,
                refPath = refPath,
                method = current.method,
                intensity = current.intensity,
                maxExportDim = current.settings.maxExportDim,
                format = current.settings.exportFormat,
                quality = current.settings.exportQuality,
                preserveLuminance = current.preserveLuminance,
                skinProtection = current.skinProtection,
                smoothSigma = current.smoothSigma,
                neutralLock = current.neutralLock,
                exposure = current.editExposure,
                contrast = current.editContrast,
                highlights = current.editHighlights,
                shadows = current.editShadows,
                whites = current.editWhites,
                blacks = current.editBlacks,
                vibrance = current.editVibrance,
                saturation = current.editSaturation
            )

            when (result) {
                is ImageExportResult.Success -> {
                    val displayName = "StealZaLUT_${System.currentTimeMillis()}.${if (current.settings.exportFormat.equals("PNG", true)) "png" else "jpg"}"
                    val mediaStoreResult = bridgeRepository.saveImageToMediaStore(
                        imagePath = result.imagePath,
                        displayName = displayName,
                        mimeType = if (current.settings.exportFormat.equals("PNG", true)) "image/png" else "image/jpeg"
                    )

                    mediaStoreResult.fold(
                        onSuccess = {
                            savedImageRepository.recordGradedImage(
                                sourceFilePath = result.imagePath,
                                width = result.width,
                                height = result.height,
                                themeName = current.selectedTheme?.name ?: "Look StealZaLUT",
                                method = current.method.key,
                                intensity = current.intensity,
                                referenceImagePath = current.effectiveReferencePath,
                                paletteHexes = current.selectedTheme?.paletteHexes ?: current.customReferencePalette
                            )
                            loadSavedImages()
                            _uiState.update {
                                it.copy(
                                    isExportingImage = false,
                                    exportedImagePath = result.imagePath,
                                    isGradedImageSaved = true,
                                    userMessage = "Photo enregistrée dans la Galerie et votre Bibliothèque !"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isExportingImage = false,
                                    errorMessage = "Erreur MediaStore : ${error.localizedMessage}"
                                )
                            }
                        }
                    )
                }
                is ImageExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExportingImage = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun saveCurrentStyleAsPreset(name: String, notes: String?) {
        val current = _uiState.value
        if (current.isSavingPreset) return
        val refPath = current.effectiveReferencePath ?: return

        _uiState.update { it.copy(isSavingPreset = true) }
        viewModelScope.launch {
            try {
                val palette = if (current.customReferencePalette.isNotEmpty()) {
                    current.customReferencePalette
                } else {
                    bridgeRepository.extractPalette(refPath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
                }

                val cardFile = File(getApplication<Application>().filesDir, "color_card_${System.currentTimeMillis()}.jpg")
                val cardResult = bridgeRepository.generateColorCard(
                    refPath = refPath,
                    outputCardPath = cardFile.absolutePath,
                    method = current.method,
                    intensity = current.intensity
                )
                val thumbnailPath = cardResult.getOrNull() ?: cardFile.absolutePath

                val preset = ColorimetryPreset(
                    name = name.ifBlank { "Preset ${System.currentTimeMillis() % 1000}" },
                    method = current.method,
                    intensity = current.intensity,
                    lutSize = current.lutSize,
                    referenceImagePath = refPath,
                    paletteHexes = palette,
                    thumbnailPath = thumbnailPath,
                    notes = notes
                )
                val saveResult = presetRepository.savePreset(preset)
                saveResult.fold(
                    onSuccess = {
                        loadSavedPresets()
                        _uiState.update {
                            it.copy(
                                isSavingThemeDialogVisible = false,
                                userMessage = "Preset '${preset.name}' sauvegardé !"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSavingThemeDialogVisible = false,
                                errorMessage = "Erreur : ${error.localizedMessage}"
                            )
                        }
                    }
                )
            } finally {
                _uiState.update { it.copy(isSavingPreset = false) }
            }
        }
    }

    private suspend fun computePythonPreview(req: PreviewDebounceRequest) {
        activeGradingJob?.cancel()
        activeGradingJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }

            val result = bridgeRepository.generatePreview(
                srcPath = req.srcPath,
                refPath = req.refPath,
                method = req.method,
                intensity = req.intensity,
                preserveLuminance = req.preserveLuminance,
                skinProtection = req.skinProtection,
                smoothSigma = req.smoothSigma,
                neutralLock = req.neutralLock
            )

            when (result) {
                is PreviewResult.Success -> {
                    val rawTransferredBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1080)
                    }

                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            previewPath = result.previewPath,
                            basePreviewBitmap = rawTransferredBmp,
                            previewExecutionTimeMs = result.executionTimeMs
                        )
                    }
                }
                is PreviewResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun exportCube(onReadyToSave: (String) -> Unit) {
        val current = _uiState.value
        if (current.isExportingCube) return
        val srcPath = current.sourcePath

        if (current.isAutoEditActive) {
            if (srcPath == null) {
                _uiState.update { it.copy(errorMessage = "Veuillez sélectionner une photo source.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isExportingCube = true, errorMessage = null) }
                val result = bridgeRepository.exportAutoEditCube(srcPath, current.lutSize)
                when (result) {
                    is CubeExportResult.Success -> {
                        pendingExportCubePath = result.cubePath
                        val downloadsResult = bridgeRepository.saveCubeToDownloads(result.cubePath, File(result.cubePath).name)
                        val feedbackMessage = if (downloadsResult.isSuccess) {
                            "LUT Auto Edit ${result.lutSize}³ enregistrée dans vos Téléchargements !"
                        } else {
                            "LUT Auto Edit ${result.lutSize}³ générée avec succès !"
                        }
                        _uiState.update {
                            it.copy(
                                isExportingCube = false,
                                exportedCubePath = result.cubePath,
                                userMessage = feedbackMessage
                            )
                        }
                        onReadyToSave(result.cubePath)
                    }
                    is CubeExportResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isExportingCube = false,
                                errorMessage = result.error.userMessage
                            )
                        }
                    }
                }
            }
            return
        }

        val refPath = current.effectiveReferencePath

        if (srcPath == null || refPath == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Impossible d'exporter : veuillez sélectionner une photo source et un style de référence."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingCube = true, errorMessage = null) }

            val result = bridgeRepository.exportCube(
                srcPath = srcPath,
                refPath = refPath,
                lutSize = current.lutSize,
                method = current.method,
                intensity = current.intensity,
                preserveLuminance = current.preserveLuminance,
                skinProtection = current.skinProtection,
                smoothSigma = current.smoothSigma,
                neutralLock = current.neutralLock,
                exposure = current.editExposure,
                contrast = current.editContrast,
                highlights = current.editHighlights,
                shadows = current.editShadows,
                whites = current.editWhites,
                blacks = current.editBlacks,
                vibrance = current.editVibrance,
                saturation = current.editSaturation
            )

            when (result) {
                is CubeExportResult.Success -> {
                    pendingExportCubePath = result.cubePath
                    val downloadsResult = bridgeRepository.saveCubeToDownloads(result.cubePath, File(result.cubePath).name)
                    val feedbackMessage = if (downloadsResult.isSuccess) {
                        "LUT ${result.lutSize}³ enregistrée dans vos Téléchargements !"
                    } else {
                        "LUT ${result.lutSize}³ générée avec succès !"
                    }
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = feedbackMessage
                        )
                    }
                    onReadyToSave(result.cubePath)
                }
                is CubeExportResult.Error -> {
                    _uiState.update {
                        it.copy(isExportingCube = false, errorMessage = result.error.userMessage)
                    }
                }
            }
        }
    }

    // ========================================================================
    // 2. PALETTES & PRESETS
    // ========================================================================

    fun setPalettePhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingPalette = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "palette_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    _uiState.update {
                        it.copy(
                            palettePhotoUri = uri,
                            palettePhotoPath = file.absolutePath,
                            palettePhotoBitmap = thumbnail,
                            isExtractingPalette = false
                        )
                    }
                    extractPaletteFromImage(file.absolutePath)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isExtractingPalette = false,
                            errorMessage = "Impossible de charger l'image : ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    private fun extractPaletteFromImage(imagePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingPalette = true) }
            val palette = bridgeRepository.extractPalette(imagePath, 6)
            _uiState.update {
                it.copy(
                    isExtractingPalette = false,
                    extractedPalette = palette.toImmutableList(),
                    userMessage = "Palette extraite avec succès !"
                )
            }
        }
    }

    fun saveExtractedPreset(name: String, notes: String?) {
        val current = _uiState.value
        if (current.isSavingPreset) return
        val refPath = current.palettePhotoPath ?: return

        _uiState.update { it.copy(isSavingPreset = true) }
        viewModelScope.launch {
            try {
                val cardFile = File(getApplication<Application>().filesDir, "color_card_${System.currentTimeMillis()}.jpg")
                val cardResult = bridgeRepository.generateColorCard(
                    refPath = refPath,
                    outputCardPath = cardFile.absolutePath,
                    method = current.method,
                    intensity = current.intensity
                )
                val thumbnailPath = cardResult.getOrNull() ?: cardFile.absolutePath

                val preset = ColorimetryPreset(
                    name = name.ifBlank { "Preset ${System.currentTimeMillis() % 1000}" },
                    method = current.method,
                    intensity = current.intensity,
                    lutSize = current.lutSize,
                    referenceImagePath = refPath,
                    paletteHexes = current.extractedPalette,
                    thumbnailPath = thumbnailPath,
                    notes = notes
                )
                val saveResult = presetRepository.savePreset(preset)
                saveResult.fold(
                    onSuccess = {
                        loadSavedPresets()
                        _uiState.update {
                            it.copy(userMessage = "Preset '${preset.name}' enregistré !")
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(errorMessage = "Erreur : ${error.localizedMessage}")
                        }
                    }
                )
            } finally {
                _uiState.update { it.copy(isSavingPreset = false) }
            }
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            presetRepository.deletePreset(id)
            loadSavedPresets()
            _uiState.update { it.copy(userMessage = "Preset supprimé.") }
        }
    }

    fun loadSavedPresets() {
        viewModelScope.launch {
            val list = presetRepository.getAllPresets()
            _uiState.update { it.copy(savedPresets = list.toImmutableList()) }
        }
    }

    fun applyPresetToStudio(preset: ColorimetryPreset) {
        selectThemeToApply(preset)
        navigateTo(AppDestination.STUDIO)
        setWizardStep(AppConstants.STEP_2_STYLE)
    }

    // ========================================================================
    // 3. PROFIL & BIBLIOTHÈQUE (SAVED IMAGES & PRESETS)
    // ========================================================================

    fun setProfileTab(tabIndex: Int) {
        _uiState.update { it.copy(profileSelectedTab = tabIndex) }
    }

    fun loadSavedImages() {
        viewModelScope.launch {
            val images = savedImageRepository.getSavedImages()
            _uiState.update { it.copy(savedImages = images.toImmutableList()) }
        }
    }

    fun deleteSavedImage(id: String) {
        viewModelScope.launch {
            savedImageRepository.deleteSavedImage(id)
            loadSavedImages()
            _uiState.update {
                it.copy(
                    userMessage = "Photo supprimée de la bibliothèque.",
                    selectedDetailImage = if (it.selectedDetailImage?.id == id) null else it.selectedDetailImage
                )
            }
        }
    }

    fun openImageDetail(image: SavedGradedImage) {
        _uiState.update { it.copy(selectedDetailImage = image) }
    }

    fun dismissImageDetail() {
        _uiState.update { it.copy(selectedDetailImage = null) }
    }

    fun applyLookFromSavedImage(image: SavedGradedImage) {
        val method = ColorTransferMethod.fromKey(image.method ?: "hm-mkl-hm")
        val customPreset = image.referenceImagePath?.let { path ->
            ColorimetryPreset(
                name = image.themeName ?: "Look StealZaLUT",
                method = method,
                intensity = image.intensity,
                referenceImagePath = path,
                paletteHexes = image.paletteHexes
            )
        }

        _uiState.update {
            it.copy(
                method = method,
                intensity = image.intensity,
                customReferencePath = image.referenceImagePath,
                selectedTheme = customPreset,
                customReferencePalette = image.paletteHexes.toImmutableList(),
                selectedDetailImage = null
            )
        }
        navigateTo(AppDestination.STUDIO)
        setWizardStep(AppConstants.STEP_2_STYLE)
    }

    fun createShareImageIntent(filePath: String): Intent {
        return bridgeRepository.createShareIntent(filePath, "image/*")
    }

    fun exportCubeFromPreset(preset: ColorimetryPreset, onReadyToSave: (String) -> Unit) {
        if (_uiState.value.isExportingCube) return
        val refPath = preset.referenceImagePath
        if (refPath == null || !File(refPath).exists()) {
            _uiState.update { it.copy(errorMessage = "Ce preset ne contient pas d'image de référence valide pour régénérer la LUT.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = refPath,
                refPath = refPath,
                lutSize = preset.lutSize,
                method = preset.method,
                intensity = preset.intensity
            )
            when (result) {
                is CubeExportResult.Success -> {
                    pendingExportCubePath = result.cubePath
                    val downloadsResult = bridgeRepository.saveCubeToDownloads(result.cubePath, File(result.cubePath).name)
                    val feedbackMessage = if (downloadsResult.isSuccess) {
                        "LUT ${result.lutSize}³ enregistrée dans vos Téléchargements !"
                    } else {
                        "LUT ${result.lutSize}³ générée avec succès !"
                    }
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = feedbackMessage
                        )
                    }
                    onReadyToSave(result.cubePath)
                }
                is CubeExportResult.Error -> {
                    _uiState.update {
                        it.copy(isExportingCube = false, errorMessage = result.error.userMessage)
                    }
                }
            }
        }
    }

    // ========================================================================
    // 4. PARAMÈTRES & CACHE
    // ========================================================================

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update {
            it.copy(
                settings = newSettings,
                lutSize = newSettings.defaultLutSize,
                method = newSettings.defaultMethod
            )
        }
    }

    fun setDefaultLutSize(size: LutSize) {
        val updated = _uiState.value.settings.copy(defaultLutSize = size)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated, lutSize = size) }
    }

    fun setDefaultMethod(method: ColorTransferMethod) {
        val updated = _uiState.value.settings.copy(defaultMethod = method)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated, method = method) }
    }

    fun setLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        _uiState.update { it.copy(currentLanguageTag = languageTag) }
    }

    fun clearCache() {
        viewModelScope.launch {
            bridgeRepository.lruCacheManager.clearAll()
            refreshCacheSize()
            _uiState.update { it.copy(userMessage = "Cache temporaire nettoyé !") }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val sizeMb = bridgeRepository.lruCacheManager.getCacheSizeMb()
            _uiState.update { it.copy(cacheSizeMb = sizeMb) }
        }
    }

    fun saveExportedCubeToUri(destinationUri: Uri) {
        val cubePath = pendingExportCubePath ?: _uiState.value.exportedCubePath
        if (cubePath == null) {
            _uiState.update { it.copy(errorMessage = "Erreur : chemin du fichier LUT introuvable.") }
            return
        }
        viewModelScope.launch {
            val result = bridgeRepository.copyFileToDestinationUri(cubePath, destinationUri)
            result.fold(
                onSuccess = { _uiState.update { it.copy(userMessage = "LUT .cube enregistrée avec succès !") } },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = "Erreur lors de l'enregistrement : ${error.localizedMessage}") } }
            )
        }
    }

    fun createShareCubeIntent(cubeFilePath: String? = null): Intent? {
        val path = cubeFilePath ?: pendingExportCubePath ?: _uiState.value.exportedCubePath ?: return null
        return try {
            bridgeRepository.createShareIntent(path, "*/*")
        } catch (_: Exception) {
            null
        }
    }

    fun getShareIntent(filePath: String): Intent? {
        return try {
            bridgeRepository.createShareIntent(filePath)
        } catch (_: Exception) {
            null
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }

    fun openMetricsInfo() {
        _uiState.update { it.copy(isInfoScreenVisible = true) }
    }

    fun dismissMetricsInfo() {
        _uiState.update { it.copy(isInfoScreenVisible = false) }
    }

    // ========================================================================
    // STANDALONE PHOTO EDITOR (MODE ÉDITEUR PUR / PHOTO STUDIO)
    // ========================================================================

    fun setEditorSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditorProcessing = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "editor_source_${System.currentTimeMillis()}.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    _uiState.update {
                        it.copy(
                            editorSourceUri = uri,
                            editorSourcePath = file.absolutePath,
                            editorSourceBitmap = thumbnail,
                            editorPreviewBitmap = thumbnail,
                            editorPreviewPath = file.absolutePath,
                            isEditorProcessing = false
                        )
                    }
                    scheduleEditorPreviewUpdate()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isEditorProcessing = false,
                            errorMessage = error.localizedMessage ?: "Impossible de charger la photo."
                        )
                    }
                }
            )
        }
    }

    fun setEditorCustomLut(uri: Uri, name: String) {
        viewModelScope.launch {
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "custom_lut_${System.currentTimeMillis()}.cube")
            cacheResult.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            editorCustomLutPath = file.absolutePath,
                            editorCustomLutName = name
                        )
                    }
                    scheduleEditorPreviewUpdate()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Erreur import LUT : ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    fun clearEditorCustomLut() {
        _uiState.update {
            it.copy(
                editorCustomLutPath = null,
                editorCustomLutName = null
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun setEditorTab(tab: Int) {
        _uiState.update { it.copy(editorSelectedTab = tab) }
    }

    // --- Stage 2 Setters (Filter Refinement) ---
    fun setStep2Intensity(v: Float) {
        _uiState.update { it.copy(step2Intensity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2LumaLock(v: Float) {
        _uiState.update { it.copy(step2LumaLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2NeutralAxisLock(v: Float) {
        _uiState.update { it.copy(step2NeutralAxisLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2SkinToneLock(v: Float) {
        _uiState.update { it.copy(step2SkinToneLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2ExtremesRolloff(v: Float) {
        _uiState.update { it.copy(step2ExtremesRolloff = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2Spatial3d(v: Float) {
        _uiState.update { it.copy(step2Spatial3d = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    // --- Stage 3 Setters (Post-Grading) ---
    fun setStep3Exposure(v: Float) {
        _uiState.update { it.copy(step3Exposure = v.coerceIn(-2.0f, 2.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Contrast(v: Float) {
        _uiState.update { it.copy(step3Contrast = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Highlights(v: Float) {
        _uiState.update { it.copy(step3Highlights = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Shadows(v: Float) {
        _uiState.update { it.copy(step3Shadows = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Temperature(v: Float) {
        _uiState.update { it.copy(step3Temperature = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Tint(v: Float) {
        _uiState.update { it.copy(step3Tint = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Vibrance(v: Float) {
        _uiState.update { it.copy(step3Vibrance = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Saturation(v: Float) {
        _uiState.update { it.copy(step3Saturation = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Clarity(v: Float) {
        _uiState.update { it.copy(step3Clarity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Sharpen(v: Float) {
        _uiState.update { it.copy(step3Sharpen = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Grain(v: Float) {
        _uiState.update { it.copy(step3Grain = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    // --- Reset Actions ---
    fun resetStep2Params() {
        _uiState.update {
            it.copy(
                step2Intensity = 1.0f,
                step2LumaLock = 0.0f,
                step2NeutralAxisLock = 0.0f,
                step2SkinToneLock = 0.0f,
                step2ExtremesRolloff = 0.0f,
                step2Spatial3d = 0.0f
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun resetStep3Params() {
        _uiState.update {
            it.copy(
                step3Exposure = 0.0f,
                step3Contrast = 0.0f,
                step3Highlights = 0.0f,
                step3Shadows = 0.0f,
                step3Temperature = 0.0f,
                step3Tint = 0.0f,
                step3Vibrance = 0.0f,
                step3Saturation = 0.0f,
                step3Clarity = 0.0f,
                step3Sharpen = 0.0f,
                step3Grain = 0.0f
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun resetEditingParameters() {
        _uiState.update {
            it.copy(
                isAutoEditActive = false,
                step2Intensity = 1.0f,
                step2LumaLock = 0.0f,
                step2NeutralAxisLock = 0.0f,
                step2SkinToneLock = 0.0f,
                step2ExtremesRolloff = 0.0f,
                step2Spatial3d = 0.0f,
                step3Exposure = 0.0f,
                step3Contrast = 0.0f,
                step3Highlights = 0.0f,
                step3Shadows = 0.0f,
                step3Temperature = 0.0f,
                step3Tint = 0.0f,
                step3Vibrance = 0.0f,
                step3Saturation = 0.0f,
                step3Clarity = 0.0f,
                step3Sharpen = 0.0f,
                step3Grain = 0.0f,
                editExposure = 0f,
                editContrast = 0f,
                editHighlights = 0f,
                editShadows = 0f,
                editWhites = 0f,
                editBlacks = 0f,
                editVibrance = 0f,
                editSaturation = 0f
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun resetAllEditorParams() {
        _uiState.update {
            it.copy(
                editorCustomLutPath = null,
                editorCustomLutName = null,
                selectedEditingPreset = null,
                step2Intensity = 1.0f,
                step2LumaLock = 0.0f,
                step2NeutralAxisLock = 0.0f,
                step2SkinToneLock = 0.0f,
                step2ExtremesRolloff = 0.0f,
                step2Spatial3d = 0.0f,
                step3Exposure = 0.0f,
                step3Contrast = 0.0f,
                step3Highlights = 0.0f,
                step3Shadows = 0.0f,
                step3Temperature = 0.0f,
                step3Tint = 0.0f,
                step3Vibrance = 0.0f,
                step3Saturation = 0.0f,
                step3Clarity = 0.0f,
                step3Sharpen = 0.0f,
                step3Grain = 0.0f
            )
        }
        scheduleEditorPreviewUpdate()
    }

    private fun scheduleEditorPreviewUpdate() {
        val src = _uiState.value.editorSourcePath ?: return
        val req = StandaloneEditorDebounceRequest(
            srcPath = src,
            intensity = _uiState.value.step2Intensity,
            lumaLock = _uiState.value.step2LumaLock,
            neutralAxisLock = _uiState.value.step2NeutralAxisLock,
            skinToneLock = _uiState.value.step2SkinToneLock,
            extremesRolloff = _uiState.value.step2ExtremesRolloff,
            spatial3d = _uiState.value.step2Spatial3d,
            exposure = _uiState.value.step3Exposure,
            contrast = _uiState.value.step3Contrast,
            highlights = _uiState.value.step3Highlights,
            shadows = _uiState.value.step3Shadows,
            temperature = _uiState.value.step3Temperature,
            tint = _uiState.value.step3Tint,
            vibrance = _uiState.value.step3Vibrance,
            saturation = _uiState.value.step3Saturation,
            clarity = _uiState.value.step3Clarity,
            sharpen = _uiState.value.step3Sharpen,
            grain = _uiState.value.step3Grain,
            customLutPath = _uiState.value.editorCustomLutPath
        )
        editorDebounceFlow.tryEmit(req)
    }

    private suspend fun computeStandaloneEditorPreview(req: StandaloneEditorDebounceRequest) {
        activeStandaloneJob?.cancel()
        activeStandaloneJob = viewModelScope.launch {
            _uiState.update { it.copy(isEditorProcessing = true) }
            val res = bridgeRepository.generateEditorPreview(
                srcPath = req.srcPath,
                lutSize = _uiState.value.lutSize,
                intensity = req.intensity,
                lumaLock = req.lumaLock,
                neutralAxisLock = req.neutralAxisLock,
                skinToneLock = req.skinToneLock,
                extremesRolloff = req.extremesRolloff,
                spatial3d = req.spatial3d,
                exposure = req.exposure,
                contrast = req.contrast,
                highlights = req.highlights,
                shadows = req.shadows,
                temperature = req.temperature,
                tint = req.tint,
                vibrance = req.vibrance,
                saturation = req.saturation,
                clarity = req.clarity,
                sharpen = req.sharpen,
                grain = req.grain,
                customLutPath = req.customLutPath
            )

            when (res) {
                is PreviewResult.Success -> {
                    val bitmap = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(res.previewPath, 1080)
                    }
                    _uiState.update {
                        it.copy(
                            editorPreviewBitmap = bitmap,
                            editorPreviewPath = res.previewPath,
                            editorExecutionTimeMs = res.executionTimeMs,
                            isEditorProcessing = false
                        )
                    }
                }
                is PreviewResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEditorProcessing = false,
                            errorMessage = res.customMessage ?: res.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun exportEditorCube(onSuccess: (String) -> Unit) {
        val src = _uiState.value.editorSourcePath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEditorExportingCube = true, errorMessage = null) }
            val res = bridgeRepository.exportEditorCube(
                srcPath = src,
                lutSize = _uiState.value.lutSize,
                intensity = _uiState.value.step2Intensity,
                lumaLock = _uiState.value.step2LumaLock,
                neutralAxisLock = _uiState.value.step2NeutralAxisLock,
                skinToneLock = _uiState.value.step2SkinToneLock,
                extremesRolloff = _uiState.value.step2ExtremesRolloff,
                spatial3d = _uiState.value.step2Spatial3d,
                exposure = _uiState.value.step3Exposure,
                contrast = _uiState.value.step3Contrast,
                highlights = _uiState.value.step3Highlights,
                shadows = _uiState.value.step3Shadows,
                temperature = _uiState.value.step3Temperature,
                tint = _uiState.value.step3Tint,
                vibrance = _uiState.value.step3Vibrance,
                saturation = _uiState.value.step3Saturation,
                customLutPath = _uiState.value.editorCustomLutPath
            )

            when (res) {
                is CubeExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isEditorExportingCube = false,
                            userMessage = "LUT 3D (.cube) exportée avec succès !"
                        )
                    }
                    onSuccess(res.cubePath)
                }
                is CubeExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEditorExportingCube = false,
                            errorMessage = res.customMessage ?: res.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun exportEditorImage() {
        val src = _uiState.value.editorSourcePath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEditorExportingImage = true, errorMessage = null) }
            val res = bridgeRepository.exportEditorImage(
                srcPath = src,
                maxExportDim = _uiState.value.settings.maxExportDim,
                quality = _uiState.value.settings.exportQuality,
                lutSize = _uiState.value.lutSize,
                intensity = _uiState.value.step2Intensity,
                lumaLock = _uiState.value.step2LumaLock,
                neutralAxisLock = _uiState.value.step2NeutralAxisLock,
                skinToneLock = _uiState.value.step2SkinToneLock,
                extremesRolloff = _uiState.value.step2ExtremesRolloff,
                spatial3d = _uiState.value.step2Spatial3d,
                exposure = _uiState.value.step3Exposure,
                contrast = _uiState.value.step3Contrast,
                highlights = _uiState.value.step3Highlights,
                shadows = _uiState.value.step3Shadows,
                temperature = _uiState.value.step3Temperature,
                tint = _uiState.value.step3Tint,
                vibrance = _uiState.value.step3Vibrance,
                saturation = _uiState.value.step3Saturation,
                clarity = _uiState.value.step3Clarity,
                sharpen = _uiState.value.step3Sharpen,
                grain = _uiState.value.step3Grain,
                customLutPath = _uiState.value.editorCustomLutPath
            )

            when (res) {
                is ImageExportResult.Success -> {
                    val mediaResult = bridgeRepository.saveImageToMediaStore(res.imagePath)
                    mediaResult.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isEditorExportingImage = false,
                                    userMessage = "Photo HD enregistrée dans la Galerie Android !"
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isEditorExportingImage = false,
                                    errorMessage = "Erreur MediaStore : ${err.localizedMessage}"
                                )
                            }
                        }
                    )
                }
                is ImageExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEditorExportingImage = false,
                            errorMessage = res.customMessage ?: res.error.userMessage
                        )
                    }
                }
            }
        }
    }

    // --- Editing Presets Management ---
    fun loadEditingPresets() {
        viewModelScope.launch {
            val list = bridgeRepository.listEditingPresetsBridge()
            _uiState.update { it.copy(savedEditingPresets = list.toImmutableList()) }
        }
    }

    fun openSaveEditingPresetDialog() {
        _uiState.update { it.copy(isSaveEditingPresetDialogVisible = true) }
    }

    fun dismissSaveEditingPresetDialog() {
        _uiState.update { it.copy(isSaveEditingPresetDialogVisible = false) }
    }

    fun saveCurrentEditingPreset(name: String) {
        viewModelScope.launch {
            dismissSaveEditingPresetDialog()
            val preset = EditPreset(
                name = name,
                lutReference = _uiState.value.editorCustomLutPath,
                step2Params = Step2Model(
                    globalIntensity = _uiState.value.step2Intensity,
                    lumaLock = _uiState.value.step2LumaLock,
                    neutralAxisLock = _uiState.value.step2NeutralAxisLock,
                    skinToneLock = _uiState.value.step2SkinToneLock,
                    extremesRolloff = _uiState.value.step2ExtremesRolloff,
                    spatial3d = _uiState.value.step2Spatial3d
                ),
                step3Params = Step3Model(
                    exposure = _uiState.value.step3Exposure,
                    contrast = _uiState.value.step3Contrast,
                    highlights = _uiState.value.step3Highlights,
                    shadows = _uiState.value.step3Shadows,
                    temperature = _uiState.value.step3Temperature,
                    tint = _uiState.value.step3Tint,
                    vibrance = _uiState.value.step3Vibrance,
                    saturation = _uiState.value.step3Saturation,
                    clarity = _uiState.value.step3Clarity,
                    sharpen = _uiState.value.step3Sharpen,
                    grain = _uiState.value.step3Grain
                )
            )

            val res = bridgeRepository.saveEditingPresetBridge(preset)
            res.fold(
                onSuccess = {
                    loadEditingPresets()
                    _uiState.update { it.copy(userMessage = "Preset d'édition sauvegardé !") }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(errorMessage = "Erreur sauvegarde : ${err.localizedMessage}") }
                }
            )
        }
    }

    fun applyEditingPreset(preset: EditPreset) {
        _uiState.update {
            it.copy(
                selectedEditingPreset = preset,
                editorCustomLutPath = preset.lutReference,
                editorCustomLutName = preset.lutReference?.let { p -> File(p).name },
                step2Intensity = preset.step2Params.globalIntensity,
                step2LumaLock = preset.step2Params.lumaLock,
                step2NeutralAxisLock = preset.step2Params.neutralAxisLock,
                step2SkinToneLock = preset.step2Params.skinToneLock,
                step2ExtremesRolloff = preset.step2Params.extremesRolloff,
                step2Spatial3d = preset.step2Params.spatial3d,
                step3Exposure = preset.step3Params.exposure,
                step3Contrast = preset.step3Params.contrast,
                step3Highlights = preset.step3Params.highlights,
                step3Shadows = preset.step3Params.shadows,
                step3Temperature = preset.step3Params.temperature,
                step3Tint = preset.step3Params.tint,
                step3Vibrance = preset.step3Params.vibrance,
                step3Saturation = preset.step3Params.saturation,
                step3Clarity = preset.step3Params.clarity,
                step3Sharpen = preset.step3Params.sharpen,
                step3Grain = preset.step3Params.grain,
                userMessage = "Preset appliqué : ${preset.name}"
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun deleteEditingPreset(presetId: String) {
        viewModelScope.launch {
            val ok = bridgeRepository.deleteEditingPresetBridge(presetId)
            if (ok) {
                loadEditingPresets()
                _uiState.update { it.copy(userMessage = "Preset supprimé.") }
            }
        }
    }

    /**
     * Efficient memory-bounded Bitmap decoding limited to 1080px max dimension.
     */
    private fun decodeSampledBitmap(path: String, maxDim: Int = 1080): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            var sampleSize = 1
            val maxActual = maxOf(options.outWidth, options.outHeight)
            while (maxActual / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (_: Exception) {
            null
        }
    }
}
