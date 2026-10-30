package com.example.stealzalut_mobile.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stealzalut_mobile.data.bridge.PythonBridgeRepository
import com.example.stealzalut_mobile.data.model.AppSettings
import com.example.stealzalut_mobile.data.model.ColorSetResult
import com.example.stealzalut_mobile.data.model.ColorTransferMethod
import com.example.stealzalut_mobile.data.model.ColorimetryPreset
import com.example.stealzalut_mobile.data.model.CubeExportResult
import com.example.stealzalut_mobile.data.model.ImageExportResult
import com.example.stealzalut_mobile.data.model.LutSize
import com.example.stealzalut_mobile.data.model.PreviewResult
import com.example.stealzalut_mobile.data.repository.PresetRepository
import com.example.stealzalut_mobile.data.repository.SavedImageRepository
import com.example.stealzalut_mobile.data.repository.SettingsRepository
import com.example.stealzalut_mobile.ui.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StealZaLutViewModel(application: Application) : AndroidViewModel(application) {

    private val bridgeRepository = PythonBridgeRepository(application.applicationContext)
    private val presetRepository = PresetRepository(application.applicationContext)
    private val savedImageRepository = SavedImageRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StealZaLutUiState())
    val uiState: StateFlow<StealZaLutUiState> = _uiState.asStateFlow()

    init {
        val initialSettings = settingsRepository.getSettings()
        _uiState.update {
            it.copy(
                settings = initialSettings,
                method = initialSettings.defaultMethod,
                lutSize = initialSettings.defaultLutSize
            )
        }
        loadSavedThemes()
        loadSavedImages()
        refreshCacheSize()

        // Select the first built-in theme as default
        viewModelScope.launch {
            val themes = presetRepository.getAllPresets()
            if (themes.isNotEmpty()) {
                _uiState.update { it.copy(selectedTheme = themes.first()) }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
        if (destination == AppDestination.PROFILE) {
            loadSavedThemes()
            loadSavedImages()
            refreshCacheSize()
        }
    }

    // ========================================================================
    // FEATURE 1 : EXTRAIRE UN THÈME DEPUIS UNE PHOTO
    // ========================================================================

    fun setExtractPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingTheme = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "extract_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 720)
                    }
                    _uiState.update {
                        it.copy(
                            extractPhotoUri = uri,
                            extractPhotoPath = file.absolutePath,
                            extractPhotoBitmap = thumbnail,
                            isExtractingTheme = false,
                            extractedPalette = emptyList(),
                            extractedColorSetBitmap = null,
                            extractedColorSetPath = null
                        )
                    }
                    runExtractTheme(file.absolutePath)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isExtractingTheme = false,
                            errorMessage = "Impossible de charger la photo : ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun loadExtractSample(style: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingTheme = true, errorMessage = null) }
            val sampleFile = bridgeRepository.createSampleImage(style)
            val thumbnail = withContext(Dispatchers.IO) {
                decodeSampledBitmap(sampleFile.absolutePath, 720)
            }
            _uiState.update {
                it.copy(
                    extractPhotoUri = Uri.fromFile(sampleFile),
                    extractPhotoPath = sampleFile.absolutePath,
                    extractPhotoBitmap = thumbnail,
                    isExtractingTheme = false
                )
            }
            runExtractTheme(sampleFile.absolutePath)
        }
    }

    private fun runExtractTheme(imagePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingTheme = true) }

            // 1. Dominant palette extraction
            val palette = bridgeRepository.extractPalette(imagePath, 6)

            // 2. Generate calibration mire (color set) for this theme
            val mireResult = bridgeRepository.generateColorSet(
                refPath = imagePath,
                method = _uiState.value.method,
                intensity = _uiState.value.intensity
            )

            when (mireResult) {
                is ColorSetResult.Success -> {
                    val mireBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(mireResult.colorSetPath, 1024)
                    }
                    _uiState.update {
                        it.copy(
                            isExtractingTheme = false,
                            extractedPalette = palette.ifEmpty { mireResult.refPalette },
                            extractedColorSetPath = mireResult.colorSetPath,
                            extractedColorSetBitmap = mireBmp,
                            extractedExecutionTimeMs = mireResult.executionTimeMs,
                            userMessage = "Thème extrait avec succès en ${mireResult.executionTimeMs.toInt()} ms !"
                        )
                    }
                }
                is ColorSetResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExtractingTheme = false,
                            extractedPalette = palette,
                            errorMessage = "Analyse mire : ${mireResult.message}"
                        )
                    }
                }
            }
        }
    }

    fun saveExtractedTheme(name: String, notes: String?) {
        val current = _uiState.value
        val refPath = current.extractPhotoPath ?: return

        viewModelScope.launch {
            val preset = ColorimetryPreset(
                name = name.ifBlank { "Thème ${System.currentTimeMillis() % 10000}" },
                method = current.method,
                intensity = current.intensity,
                lutSize = current.lutSize,
                referenceImagePath = refPath,
                paletteHexes = current.extractedPalette,
                colorSetImagePath = current.extractedColorSetPath,
                notes = notes
            )
            val saveResult = presetRepository.savePreset(preset)
            saveResult.fold(
                onSuccess = {
                    loadSavedThemes()
                    _uiState.update {
                        it.copy(userMessage = "Thème '${preset.name}' sauvegardé dans votre profil !")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Erreur sauvegarde thème : ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    // ========================================================================
    // FEATURE 2 : APPLIQUER UN THÈME (VISUALISATION DOUBLE VUE SEULEMENT)
    // ========================================================================

    fun setSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "source_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 720)
                    }
                    _uiState.update {
                        it.copy(
                            sourceUri = uri,
                            sourcePath = file.absolutePath,
                            sourceBitmap = thumbnail,
                            isProcessingPreview = false,
                            previewBitmap = null,
                            previewPath = null
                        )
                    }
                    if (_uiState.value.effectiveReferencePath != null) {
                        generatePreview()
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

    fun selectThemeToApply(theme: ColorimetryPreset) {
        _uiState.update {
            it.copy(
                selectedTheme = theme,
                customReferencePath = null,
                customReferenceBitmap = null,
                customReferenceUri = null,
                method = theme.method,
                intensity = theme.intensity,
                lutSize = theme.lutSize,
                userMessage = "Thème '${theme.name}' sélectionné !"
            )
        }
        if (_uiState.value.sourcePath != null && _uiState.value.effectiveReferencePath != null) {
            generatePreview()
        }
    }

    fun setCustomReferenceForApply(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "custom_ref_apply.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 720)
                    }
                    _uiState.update {
                        it.copy(
                            customReferenceUri = uri,
                            customReferencePath = file.absolutePath,
                            customReferenceBitmap = thumbnail,
                            selectedTheme = null,
                            isProcessingPreview = false
                        )
                    }
                    if (_uiState.value.sourcePath != null) {
                        generatePreview()
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
        _uiState.update { it.copy(method = method) }
        if (_uiState.value.canApply) generatePreview()
    }

    fun setIntensity(intensity: Float) {
        _uiState.update { it.copy(intensity = intensity) }
        if (_uiState.value.canApply) generatePreview()
    }

    fun setLutSize(size: LutSize) {
        _uiState.update { it.copy(lutSize = size) }
    }

    fun generatePreview() {
        val current = _uiState.value
        val srcPath = current.sourcePath ?: return
        val refPath = current.effectiveReferencePath ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessingPreview = true, errorMessage = null)
            }

            val result = bridgeRepository.generatePreview(
                srcPath = srcPath,
                refPath = refPath,
                method = current.method,
                intensity = current.intensity
            )

            when (result) {
                is PreviewResult.Success -> {
                    val previewBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1024)
                    }
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            previewPath = result.previewPath,
                            previewBitmap = previewBmp,
                            previewExecutionTimeMs = result.executionTimeMs
                        )
                    }
                }
                is PreviewResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun exportGradedImage(onReadyToSave: (String) -> Unit) {
        val current = _uiState.value
        val srcPath = current.sourcePath ?: return
        val refPath = current.effectiveReferencePath ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isExportingImage = true, errorMessage = null)
            }

            val result = bridgeRepository.exportGradedImage(
                srcPath = srcPath,
                refPath = refPath,
                method = current.method,
                intensity = current.intensity,
                maxExportDim = current.settings.maxExportDim,
                format = current.settings.exportFormat,
                quality = current.settings.exportQuality
            )

            when (result) {
                is ImageExportResult.Success -> {
                    // Record in the profile folder
                    val themeName = current.selectedTheme?.name ?: "Look StealZaLUT"
                    savedImageRepository.recordGradedImage(
                        filePath = result.imagePath,
                        width = result.width,
                        height = result.height,
                        themeName = themeName
                    )
                    loadSavedImages()

                    _uiState.update {
                        it.copy(
                            isExportingImage = false,
                            exportedImagePath = result.imagePath,
                            userMessage = "Image étalonnée (${result.width}×${result.height}) prête !"
                        )
                    }
                    onReadyToSave(result.imagePath)
                }
                is ImageExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExportingImage = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun exportCube(onReadyToSave: (String) -> Unit) {
        val current = _uiState.value
        val srcPath = current.sourcePath ?: current.extractPhotoPath ?: return
        val refPath = current.effectiveReferencePath ?: current.extractPhotoPath ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isExportingCube = true, errorMessage = null)
            }

            val result = bridgeRepository.exportCube(
                srcPath = srcPath,
                refPath = refPath,
                lutSize = current.lutSize,
                method = current.method,
                intensity = current.intensity
            )

            when (result) {
                is CubeExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = "LUT ${result.lutSize}³ générée en ${result.executionTimeMs.toInt()} ms !"
                        )
                    }
                    onReadyToSave(result.cubePath)
                }
                is CubeExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExportingCube = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    // ========================================================================
    // FEATURE 3 : PROFIL & BIBLIOTHÈQUE (DOSSIERS THÈMES & IMAGES)
    // ========================================================================

    fun loadSavedThemes() {
        viewModelScope.launch {
            val list = presetRepository.getAllPresets()
            _uiState.update { it.copy(savedThemes = list) }
        }
    }

    fun loadSavedImages() {
        viewModelScope.launch {
            val list = savedImageRepository.getSavedImages()
            _uiState.update { it.copy(savedImages = list) }
        }
    }

    /**
     * Opens the detailed calibration mire inspection dialog for a theme from the library.
     */
    fun openMireDetail(preset: ColorimetryPreset) {
        viewModelScope.launch {
            var mireBmp: Bitmap? = null

            // If the preset has an existing color set image file
            preset.colorSetImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    mireBmp = withContext(Dispatchers.IO) { decodeSampledBitmap(f.absolutePath, 1024) }
                }
            }

            // If not yet generated or missing, generate it on demand from its reference image
            if (mireBmp == null && preset.referenceImagePath != null) {
                val f = File(preset.referenceImagePath)
                if (f.exists()) {
                    val result = bridgeRepository.generateColorSet(
                        refPath = f.absolutePath,
                        method = preset.method,
                        intensity = preset.intensity
                    )
                    if (result is ColorSetResult.Success) {
                        mireBmp = withContext(Dispatchers.IO) { decodeSampledBitmap(result.colorSetPath, 1024) }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    selectedMirePreset = preset,
                    selectedMireBitmap = mireBmp
                )
            }
        }
    }

    fun closeMireDetail() {
        _uiState.update { it.copy(selectedMirePreset = null, selectedMireBitmap = null) }
    }

    fun applyThemeFromLibrary(preset: ColorimetryPreset) {
        selectThemeToApply(preset)
        navigateTo(AppDestination.APPLY_THEME)
    }

    fun deleteTheme(presetId: String) {
        viewModelScope.launch {
            presetRepository.deletePreset(presetId)
            loadSavedThemes()
            _uiState.update { it.copy(userMessage = "Thème supprimé de votre bibliothèque.") }
        }
    }

    fun deleteSavedImage(id: String) {
        viewModelScope.launch {
            savedImageRepository.deleteSavedImage(id)
            loadSavedImages()
            _uiState.update { it.copy(userMessage = "Image supprimée du dossier.") }
        }
    }

    // ========================================================================
    // STORAGE & SAF
    // ========================================================================

    fun saveExportedCubeToUri(destinationUri: Uri) {
        val cubePath = _uiState.value.exportedCubePath ?: return
        viewModelScope.launch {
            val saveResult = bridgeRepository.copyFileToDestinationUri(cubePath, destinationUri)
            saveResult.fold(
                onSuccess = {
                    _uiState.update { it.copy(userMessage = "LUT .cube sauvegardée avec succès !") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Erreur sauvegarde .cube: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun saveExportedImageToUri(destinationUri: Uri) {
        val imagePath = _uiState.value.exportedImagePath ?: return
        viewModelScope.launch {
            val saveResult = bridgeRepository.copyFileToDestinationUri(imagePath, destinationUri)
            saveResult.fold(
                onSuccess = {
                    _uiState.update { it.copy(userMessage = "Image enregistrée dans votre galerie !") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Erreur sauvegarde image: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun saveMireToUri(preset: ColorimetryPreset, destinationUri: Uri) {
        val mirePath = preset.colorSetImagePath ?: _uiState.value.extractedColorSetPath ?: return
        viewModelScope.launch {
            val saveResult = bridgeRepository.copyFileToDestinationUri(mirePath, destinationUri)
            saveResult.fold(
                onSuccess = {
                    _uiState.update { it.copy(userMessage = "Mire de calibration exportée avec succès !") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Erreur export mire: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun getShareIntent(filePath: String?): android.content.Intent? {
        val path = filePath ?: _uiState.value.exportedCubePath ?: _uiState.value.exportedImagePath ?: return null
        return bridgeRepository.createShareIntent(path)
    }

    // ========================================================================
    // SETTINGS & CACHE
    // ========================================================================

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update {
            it.copy(
                settings = newSettings,
                method = newSettings.defaultMethod,
                lutSize = newSettings.defaultLutSize,
                userMessage = "Paramètres enregistrés."
            )
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val sizeMb = settingsRepository.getCacheSizeMb()
            _uiState.update { it.copy(cacheSizeMb = sizeMb) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cleared = settingsRepository.clearCache()
            refreshCacheSize()
            _uiState.update {
                it.copy(
                    previewBitmap = null,
                    previewPath = null,
                    extractedColorSetBitmap = null,
                    extractedColorSetPath = null,
                    userMessage = if (cleared) "Cache temporaire vidé." else "Erreur vidage cache."
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }

    private fun decodeSampledBitmap(path: String, maxDim: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            var sampleSize = 1
            val maxActual = maxOf(options.outWidth, options.outHeight)
            while (maxActual / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (_: Exception) {
            null
        }
    }
}
