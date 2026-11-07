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
import java.util.Locale
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.data.repository.SettingsRepository
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.delegate.EditorSessionDelegate
import com.stealzalut.mobile.ui.viewmodel.delegate.GradingPipelineDelegate
import com.stealzalut.mobile.ui.viewmodel.delegate.PresetLibraryDelegate
import com.stealzalut.mobile.ui.viewmodel.delegate.StudioFlowDelegate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StealZaLutViewModel: Single Source of Truth & UDF Façade.
 * Coordinates UI events with modular domain delegates:
 * - [StudioFlowDelegate]: 5-step funnel transitions and wizard validation.
 * - [GradingPipelineDelegate]: Chaquopy Python color transfer, live previews, and export.
 * - [EditorSessionDelegate]: Standalone photo editor session, parameters, and undo/redo.
 * - [PresetLibraryDelegate]: Presets CRUD, palette extraction, and library management.
 */
class StealZaLutViewModel(application: Application) : AndroidViewModel(application) {

    private val bridgeRepository = PythonBridgeRepository(application.applicationContext)
    private val presetRepository = PresetRepository(application.applicationContext)
    private val savedImageRepository = SavedImageRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StealZaLutUiState())
    val uiState: StateFlow<StealZaLutUiState> = _uiState.asStateFlow()

    // --- Domain Delegates ---
    private val studioFlowDelegate = StudioFlowDelegate(_uiState)

    private val gradingPipelineDelegate = GradingPipelineDelegate(
        context = application.applicationContext,
        bridgeRepository = bridgeRepository,
        savedImageRepository = savedImageRepository,
        stateFlow = _uiState,
        scope = viewModelScope,
        decodeSampledBitmap = ::decodeSampledBitmap
    )

    private val editorSessionDelegate = EditorSessionDelegate(
        context = application.applicationContext,
        bridgeRepository = bridgeRepository,
        savedImageRepository = savedImageRepository,
        stateFlow = _uiState,
        scope = viewModelScope,
        decodeSampledBitmap = ::decodeSampledBitmap
    )

    private val presetLibraryDelegate = PresetLibraryDelegate(
        application = application,
        bridgeRepository = bridgeRepository,
        presetRepository = presetRepository,
        savedImageRepository = savedImageRepository,
        stateFlow = _uiState,
        scope = viewModelScope,
        decodeSampledBitmap = ::decodeSampledBitmap,
        onTriggerEditorPreviewCallback = {
            editorSessionDelegate.scheduleEditorPreviewUpdate()
        }
    )

    init {
        val initialSettings = settingsRepository.getSettings()
        val detectedTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank {
            Locale.getDefault().language
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
    }

    // ========================================================================
    // NAVIGATION & WIZARD FLOW (StudioFlowDelegate)
    // ========================================================================

    fun navigateTo(destination: AppDestination) {
        studioFlowDelegate.navigateTo(destination)
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

    fun setWizardStep(step: Int) {
        studioFlowDelegate.setWizardStep(step)
        if (step == AppConstants.STEP_3_PARAMS) {
            gradingPipelineDelegate.triggerDebouncedPreview()
        }
    }

    fun setStyleBSourceMode(mode: Int) = studioFlowDelegate.setStyleBSourceMode(mode)
    fun openSaveThemeDialog() = studioFlowDelegate.openSaveThemeDialog()
    fun dismissSaveThemeDialog() = studioFlowDelegate.dismissSaveThemeDialog()
    fun resetWizard() {
        studioFlowDelegate.resetWizard()
        gradingPipelineDelegate.clearCaches()
    }
    fun setSplitPosition(position: Float) = studioFlowDelegate.setSplitPosition(position)
    fun toggleSplitWipeMode() = studioFlowDelegate.toggleSplitWipeMode()

    // ========================================================================
    // STUDIO GRADING & PIPELINE (GradingPipelineDelegate)
    // ========================================================================

    fun setSourceImage(uri: Uri) = gradingPipelineDelegate.setSourceImage(uri)
    fun selectThemeToApply(preset: ColorimetryPreset) = gradingPipelineDelegate.selectThemeToApply(preset)
    fun setCustomReferenceForApply(uri: Uri) = gradingPipelineDelegate.setCustomReferenceForApply(uri)
    fun setMethod(method: ColorTransferMethod) = gradingPipelineDelegate.setMethod(method)
    fun setIntensity(intensity: Float) = gradingPipelineDelegate.setIntensity(intensity)
    fun setPreserveLuminance(value: Float) = gradingPipelineDelegate.setPreserveLuminance(value)
    fun setSkinProtection(value: Float) = gradingPipelineDelegate.setSkinProtection(value)
    fun setSmoothSigma(value: Float) = gradingPipelineDelegate.setSmoothSigma(value)
    fun setNeutralLock(value: Float) = gradingPipelineDelegate.setNeutralLock(value)
    fun setLutSize(size: LutSize) = gradingPipelineDelegate.setLutSize(size)
    fun runGradingFunnel() = gradingPipelineDelegate.runGradingFunnel()
    fun applyEditorAutoEdit() = editorSessionDelegate.applyAutoEdit()

    fun saveGradedImageToGallery() = gradingPipelineDelegate.saveGradedImageToGallery { loadSavedImages() }
    fun exportCube(onReadyToSave: (String) -> Unit) = gradingPipelineDelegate.exportCube(onReadyToSave)
    fun cancelPendingExport() = gradingPipelineDelegate.cancelPendingExport()
    fun savePendingExportToUri(destinationUri: Uri) = gradingPipelineDelegate.savePendingExportToUri(destinationUri)
    fun saveExportedCubeToUri(destinationUri: Uri) = gradingPipelineDelegate.saveExportedCubeToUri(destinationUri)
    fun createShareCubeIntent(cubeFilePath: String? = null): Intent? = gradingPipelineDelegate.createShareCubeIntent(cubeFilePath)
    fun getShareIntent(filePath: String): Intent? = gradingPipelineDelegate.getShareIntent(filePath)
    fun openInEditorFromStudio() = gradingPipelineDelegate.openInEditorFromStudio()

    // ========================================================================
    // STANDALONE PHOTO EDITOR (EditorSessionDelegate)
    // ========================================================================

    fun setEditorSourceImage(uri: Uri) = editorSessionDelegate.setEditorSourceImage(uri)
    fun setEditorCustomLut(uri: Uri, name: String) = editorSessionDelegate.setEditorCustomLut(uri, name)
    fun clearEditorCustomLut() = editorSessionDelegate.clearEditorCustomLut()
    fun setEditorTab(tab: Int) = editorSessionDelegate.setEditorTab(tab)

    fun setStep2Intensity(v: Float) = editorSessionDelegate.setStep2Intensity(v)
    fun setStep2LumaLock(v: Float) = editorSessionDelegate.setStep2LumaLock(v)
    fun setStep2NeutralAxisLock(v: Float) = editorSessionDelegate.setStep2NeutralAxisLock(v)
    fun setStep2SkinToneLock(v: Float) = editorSessionDelegate.setStep2SkinToneLock(v)
    fun setStep2ExtremesRolloff(v: Float) = editorSessionDelegate.setStep2ExtremesRolloff(v)
    fun setStep2Spatial3d(v: Float) = editorSessionDelegate.setStep2Spatial3d(v)

    fun setStep3Exposure(v: Float) = editorSessionDelegate.setStep3Exposure(v)
    fun setStep3Contrast(v: Float) = editorSessionDelegate.setStep3Contrast(v)
    fun setStep3Highlights(v: Float) = editorSessionDelegate.setStep3Highlights(v)
    fun setStep3Shadows(v: Float) = editorSessionDelegate.setStep3Shadows(v)
    fun setStep3Temperature(v: Float) = editorSessionDelegate.setStep3Temperature(v)
    fun setStep3Tint(v: Float) = editorSessionDelegate.setStep3Tint(v)
    fun setStep3Vibrance(v: Float) = editorSessionDelegate.setStep3Vibrance(v)
    fun setStep3Saturation(v: Float) = editorSessionDelegate.setStep3Saturation(v)
    fun setStep3Clarity(v: Float) = editorSessionDelegate.setStep3Clarity(v)
    fun setStep3Sharpen(v: Float) = editorSessionDelegate.setStep3Sharpen(v)
    fun setStep3Grain(v: Float) = editorSessionDelegate.setStep3Grain(v)

    fun resetStep2Params() = editorSessionDelegate.resetStep2Params()
    fun resetStep3Params() = editorSessionDelegate.resetStep3Params()
    fun resetEditingParameters() = editorSessionDelegate.resetEditingParameters()
    fun resetAllEditorParams() = editorSessionDelegate.resetAllEditorParams()
    fun closeEditorPhoto() = editorSessionDelegate.closeEditorPhoto()
    fun exportEditorCube(onSuccess: (String) -> Unit) = editorSessionDelegate.exportEditorCube(onSuccess)
    fun exportEditorImage() = editorSessionDelegate.exportEditorImage { loadSavedImages() }

    // Phase 4: Undo / Redo for Editor
    fun undoEditor() = editorSessionDelegate.undo()
    fun redoEditor() = editorSessionDelegate.redo()
    fun canUndoEditor(): Boolean = editorSessionDelegate.canUndo()
    fun canRedoEditor(): Boolean = editorSessionDelegate.canRedo()
    val lightroomDelegate get() = editorSessionDelegate.lightroomDelegate

    // ========================================================================
    // PRESETS & LIBRARY (PresetLibraryDelegate)
    // ========================================================================

    fun saveCurrentStyleAsPreset(name: String, notes: String?) = presetLibraryDelegate.saveCurrentStyleAsPreset(name, notes)
    fun deletePreset(id: String) = presetLibraryDelegate.deletePreset(id)
    fun loadSavedPresets() = presetLibraryDelegate.loadSavedPresets()
    fun setPalettePhoto(uri: Uri) = presetLibraryDelegate.setPalettePhoto(uri)
    fun saveExtractedPreset(name: String, notes: String?) = presetLibraryDelegate.saveExtractedPreset(name, notes)

    fun setProfileTab(tabIndex: Int) = presetLibraryDelegate.setProfileTab(tabIndex)
    fun loadSavedImages() = presetLibraryDelegate.loadSavedImages()
    fun deleteSavedImage(id: String) = presetLibraryDelegate.deleteSavedImage(id)
    fun openImageDetail(image: SavedGradedImage) = presetLibraryDelegate.openImageDetail(image)
    fun dismissImageDetail() = presetLibraryDelegate.dismissImageDetail()
    fun createShareImageIntent(filePath: String): Intent = presetLibraryDelegate.createShareImageIntent(filePath)
    fun exportCubeFromPreset(preset: ColorimetryPreset, onReadyToSave: (String) -> Unit) =
        presetLibraryDelegate.exportCubeFromPreset(preset, onReadyToSave)
    fun exportCubeFromPaletteImage(onReadyToSave: (String) -> Unit) =
        presetLibraryDelegate.exportCubeFromPaletteImage(onReadyToSave)

    fun loadEditingPresets() = presetLibraryDelegate.loadEditingPresets()
    fun openSaveEditingPresetDialog() = presetLibraryDelegate.openSaveEditingPresetDialog()
    fun dismissSaveEditingPresetDialog() = presetLibraryDelegate.dismissSaveEditingPresetDialog()
    fun openEditorPresetsSheet() = presetLibraryDelegate.openEditorPresetsSheet()
    fun dismissEditorPresetsSheet() = presetLibraryDelegate.dismissEditorPresetsSheet()
    fun saveCurrentEditingPreset(name: String) = presetLibraryDelegate.saveCurrentEditingPreset(name)
    fun applyEditingPreset(preset: EditPreset) = presetLibraryDelegate.applyEditingPreset(preset)
    fun deleteEditingPreset(presetId: String) = presetLibraryDelegate.deleteEditingPreset(presetId)
    fun duplicatePreset(id: String, newName: String? = null) = presetLibraryDelegate.duplicatePreset(id, newName)
    fun renamePreset(id: String, newName: String) = presetLibraryDelegate.renamePreset(id, newName)
    fun duplicateEditingPreset(id: String, newName: String? = null) = presetLibraryDelegate.duplicateEditingPreset(id, newName)
    fun renameEditingPreset(id: String, newName: String) = presetLibraryDelegate.renameEditingPreset(id, newName)
    fun exportEditingPreset(id: String, exportPath: String, onSuccess: (String) -> Unit) = presetLibraryDelegate.exportEditingPreset(id, exportPath, onSuccess)
    fun exportEditingPreset(preset: EditPreset, onReadyToSave: (String) -> Unit) = presetLibraryDelegate.exportEditingPreset(preset, onReadyToSave)
    fun importEditingPreset(importPath: String) = presetLibraryDelegate.importEditingPreset(importPath)
    fun importEditingPresetFromUri(uri: Uri) = presetLibraryDelegate.importEditingPresetFromUri(uri)

    // ========================================================================
    // SETTINGS & CACHE
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
            _uiState.update { it.copy(userMessage = UiText.Resource(R.string.msg_cache_cleared)) }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val sizeMb = bridgeRepository.lruCacheManager.getCacheSizeMb()
            _uiState.update { it.copy(cacheSizeMb = sizeMb) }
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

    private fun decodeSampledBitmap(path: String, maxDim: Int = 1080): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            val maxActual = maxOf(options.outWidth, options.outHeight)
            while (maxActual / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (t: Throwable) {
            android.util.Log.e("StealZaLutViewModel", "decodeSampledBitmap error: ${t.localizedMessage}", t)
            null
        }
    }
}
