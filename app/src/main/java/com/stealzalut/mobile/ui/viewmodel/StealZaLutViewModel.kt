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
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.model.SavedGradedImage
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

@OptIn(FlowPreview::class)
class StealZaLutViewModel(application: Application) : AndroidViewModel(application) {

    private val bridgeRepository = PythonBridgeRepository(application.applicationContext)
    private val presetRepository = PresetRepository(application.applicationContext)
    private val savedImageRepository = SavedImageRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StealZaLutUiState())
    val uiState: StateFlow<StealZaLutUiState> = _uiState.asStateFlow()

    private val previewDebounceFlow = MutableSharedFlow<PreviewDebounceRequest>(replay = 1)
    private var activeGradingJob: Job? = null
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
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
        when (destination) {
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
        _uiState.update {
            it.copy(
                wizardStep = AppConstants.STEP_1_SOURCE,
                computeStageIndex = 1,
                basePreviewBitmap = null,
                previewPath = null
            )
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
        val srcPath = current.sourcePath ?: return
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
                neutralLock = current.neutralLock
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
        val refPath = current.effectiveReferencePath ?: return

        viewModelScope.launch {
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
        val srcPath = current.sourcePath
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
                neutralLock = current.neutralLock
            )

            when (result) {
                is CubeExportResult.Success -> {
                    pendingExportCubePath = result.cubePath
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = "LUT ${result.lutSize}³ générée avec succès !"
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
        val refPath = current.palettePhotoPath ?: return

        viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = "LUT ${result.lutSize}³ générée avec succès !"
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
