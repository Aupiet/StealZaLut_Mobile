package com.stealzalut.mobile.ui.viewmodel.delegate

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.data.model.Step2Model
import com.stealzalut.mobile.data.model.Step3Model
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresetLibraryDelegate(
    private val application: Application,
    private val bridgeRepository: PythonBridgeRepository,
    private val presetRepository: PresetRepository,
    private val savedImageRepository: SavedImageRepository,
    private val stateFlow: MutableStateFlow<StealZaLutUiState>,
    private val scope: CoroutineScope,
    private val decodeSampledBitmap: (path: String, maxDim: Int) -> Bitmap?,
    private val onApplyPresetToStudioCallback: (ColorimetryPreset) -> Unit,
    private val onApplyLookCallback: (SavedGradedImage) -> Unit,
    private val onTriggerEditorPreviewCallback: () -> Unit
) {
    fun loadSavedPresets() {
        scope.launch {
            val list = presetRepository.getAllPresets()
            stateFlow.update { it.copy(savedPresets = list.toImmutableList()) }
        }
    }

    fun saveCurrentStyleAsPreset(name: String, notes: String?) {
        val current = stateFlow.value
        if (current.isSavingPreset) return
        val refPath = current.effectiveReferencePath ?: return

        stateFlow.update { it.copy(isSavingPreset = true) }
        scope.launch {
            try {
                val palette = if (current.customReferencePalette.isNotEmpty()) {
                    current.customReferencePalette
                } else {
                    bridgeRepository.extractPalette(refPath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
                }

                val cardFile = File(application.filesDir, "color_card_${System.currentTimeMillis()}.jpg")
                val cardResult = bridgeRepository.generateColorCard(
                    refPath = refPath,
                    outputCardPath = cardFile.absolutePath,
                    method = current.method,
                    intensity = current.intensity
                )
                val thumbnailPath = cardResult.getOrNull() ?: cardFile.absolutePath

                val defaultPresetName = "Style " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                val preset = ColorimetryPreset(
                    name = name.ifBlank { defaultPresetName },
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
                        stateFlow.update {
                            it.copy(
                                isSavingThemeDialogVisible = false,
                                userMessage = UiText.Resource(R.string.msg_preset_saved, listOf(preset.name))
                            )
                        }
                    },
                    onFailure = { error ->
                        stateFlow.update {
                            it.copy(
                                isSavingThemeDialogVisible = false,
                                errorMessage = UiText.Resource(R.string.err_save_failed, listOf(error.localizedMessage ?: ""))
                            )
                        }
                    }
                )
            } finally {
                stateFlow.update { it.copy(isSavingPreset = false) }
            }
        }
    }

    fun deletePreset(id: String) {
        scope.launch {
            presetRepository.deletePreset(id)
            loadSavedPresets()
            stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_deleted)) }
        }
    }

    fun applyPresetToStudio(preset: ColorimetryPreset) {
        onApplyPresetToStudioCallback(preset)
    }

    fun setPalettePhoto(uri: Uri) {
        scope.launch {
            stateFlow.update { it.copy(isExtractingPalette = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "palette_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    stateFlow.update {
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
                    stateFlow.update {
                        it.copy(
                            isExtractingPalette = false,
                            errorMessage = UiText.Resource(R.string.err_image_load_failed, listOf(error.localizedMessage ?: ""))
                        )
                    }
                }
            )
        }
    }

    fun extractPaletteFromImage(imagePath: String) {
        scope.launch {
            stateFlow.update { it.copy(isExtractingPalette = true) }
            val palette = bridgeRepository.extractPalette(imagePath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
            stateFlow.update {
                it.copy(
                    isExtractingPalette = false,
                    extractedPalette = palette.toImmutableList(),
                    userMessage = UiText.Resource(R.string.msg_palette_extracted)
                )
            }
        }
    }

    fun saveExtractedPreset(name: String, notes: String?) {
        val current = stateFlow.value
        if (current.isSavingPreset) return
        val refPath = current.palettePhotoPath ?: return

        stateFlow.update { it.copy(isSavingPreset = true) }
        scope.launch {
            try {
                val cardFile = File(application.filesDir, "color_card_${System.currentTimeMillis()}.jpg")
                val cardResult = bridgeRepository.generateColorCard(
                    refPath = refPath,
                    outputCardPath = cardFile.absolutePath,
                    method = current.method,
                    intensity = current.intensity
                )
                val thumbnailPath = cardResult.getOrNull() ?: cardFile.absolutePath

                val defaultPresetName = "Style " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                val preset = ColorimetryPreset(
                    name = name.ifBlank { defaultPresetName },
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
                        stateFlow.update {
                            it.copy(userMessage = UiText.Resource(R.string.msg_preset_saved, listOf(preset.name)))
                        }
                    },
                    onFailure = { error ->
                        stateFlow.update {
                            it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf(error.localizedMessage ?: "")))
                        }
                    }
                )
            } finally {
                stateFlow.update { it.copy(isSavingPreset = false) }
            }
        }
    }

    fun setProfileTab(tabIndex: Int) {
        stateFlow.update { it.copy(profileSelectedTab = tabIndex) }
    }

    fun loadSavedImages() {
        scope.launch {
            val images = savedImageRepository.getSavedImages()
            stateFlow.update { it.copy(savedImages = images.toImmutableList()) }
        }
    }

    fun deleteSavedImage(id: String) {
        scope.launch {
            savedImageRepository.deleteSavedImage(id)
            loadSavedImages()
            stateFlow.update {
                it.copy(
                    userMessage = UiText.Resource(R.string.msg_photo_deleted),
                    selectedDetailImage = if (it.selectedDetailImage?.id == id) null else it.selectedDetailImage
                )
            }
        }
    }

    fun openImageDetail(image: SavedGradedImage) {
        stateFlow.update { it.copy(selectedDetailImage = image) }
    }

    fun dismissImageDetail() {
        stateFlow.update { it.copy(selectedDetailImage = null) }
    }

    fun applyLookFromSavedImage(image: SavedGradedImage) {
        onApplyLookCallback(image)
    }

    fun createShareImageIntent(filePath: String): Intent {
        return bridgeRepository.createShareIntent(filePath, "image/*")
    }

    fun exportCubeFromPreset(preset: ColorimetryPreset, onReadyToSave: (String) -> Unit) {
        if (stateFlow.value.isExportingCube) return
        val refPath = preset.referenceImagePath
        if (refPath == null || !File(refPath).exists()) {
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_preset_no_ref)) }
            return
        }

        scope.launch {
            stateFlow.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = refPath,
                refPath = refPath,
                lutSize = preset.lutSize,
                method = preset.method,
                intensity = preset.intensity
            )
            when (result) {
                is CubeExportResult.Success -> {
                    bridgeRepository.saveCubeToDownloads(result.cubePath, File(result.cubePath).name)
                    stateFlow.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            userMessage = UiText.Resource(R.string.msg_lut_cube_saved)
                        )
                    }
                    onReadyToSave(result.cubePath)
                }
                is CubeExportResult.Error -> {
                    stateFlow.update {
                        it.copy(isExportingCube = false, errorMessage = result.error.uiText)
                    }
                }
            }
        }
    }

    fun loadEditingPresets() {
        scope.launch {
            val list = bridgeRepository.listEditingPresetsBridge()
            stateFlow.update { it.copy(savedEditingPresets = list.toImmutableList()) }
        }
    }

    fun openSaveEditingPresetDialog() {
        stateFlow.update { it.copy(isSaveEditingPresetDialogVisible = true) }
    }

    fun dismissSaveEditingPresetDialog() {
        stateFlow.update { it.copy(isSaveEditingPresetDialogVisible = false) }
    }

    fun saveCurrentEditingPreset(name: String) {
        scope.launch {
            dismissSaveEditingPresetDialog()
            val preset = EditPreset(
                name = name,
                lutReference = stateFlow.value.editorCustomLutPath,
                step2Params = Step2Model(
                    globalIntensity = stateFlow.value.step2Intensity,
                    lumaLock = stateFlow.value.step2LumaLock,
                    neutralAxisLock = stateFlow.value.step2NeutralAxisLock,
                    skinToneLock = stateFlow.value.step2SkinToneLock,
                    extremesRolloff = stateFlow.value.step2ExtremesRolloff,
                    spatial3d = stateFlow.value.step2Spatial3d
                ),
                step3Params = Step3Model(
                    exposure = stateFlow.value.step3Exposure,
                    contrast = stateFlow.value.step3Contrast,
                    highlights = stateFlow.value.step3Highlights,
                    shadows = stateFlow.value.step3Shadows,
                    temperature = stateFlow.value.step3Temperature,
                    tint = stateFlow.value.step3Tint,
                    vibrance = stateFlow.value.step3Vibrance,
                    saturation = stateFlow.value.step3Saturation,
                    clarity = stateFlow.value.step3Clarity,
                    sharpen = stateFlow.value.step3Sharpen,
                    grain = stateFlow.value.step3Grain
                )
            )

            val res = bridgeRepository.saveEditingPresetBridge(preset)
            res.fold(
                onSuccess = {
                    loadEditingPresets()
                    stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_editing_preset_saved)) }
                },
                onFailure = { err ->
                    stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf(err.localizedMessage ?: ""))) }
                }
            )
        }
    }

    fun applyEditingPreset(preset: EditPreset) {
        stateFlow.update {
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
                userMessage = UiText.Resource(R.string.msg_preset_applied, listOf(preset.name))
            )
        }
        onTriggerEditorPreviewCallback()
    }

    fun deleteEditingPreset(presetId: String) {
        scope.launch {
            val ok = bridgeRepository.deleteEditingPresetBridge(presetId)
            if (ok) {
                loadEditingPresets()
                stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_deleted)) }
            }
        }
    }

    fun duplicatePreset(presetId: String, newName: String? = null) {
        scope.launch {
            val res = presetRepository.duplicatePreset(presetId, newName)
            res.fold(
                onSuccess = {
                    loadSavedPresets()
                    stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_duplicated)) }
                },
                onFailure = { err ->
                    stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf(err.localizedMessage ?: ""))) }
                }
            )
        }
    }

    fun renamePreset(presetId: String, newName: String) {
        scope.launch {
            val res = presetRepository.renamePreset(presetId, newName)
            res.fold(
                onSuccess = {
                    loadSavedPresets()
                    stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_renamed)) }
                },
                onFailure = { err ->
                    stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf(err.localizedMessage ?: ""))) }
                }
            )
        }
    }

    fun duplicateEditingPreset(presetId: String, newName: String? = null) {
        scope.launch {
            val ok = bridgeRepository.duplicateEditingPresetBridge(presetId, newName)
            if (ok) {
                loadEditingPresets()
                stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_duplicated)) }
            }
        }
    }

    fun renameEditingPreset(presetId: String, newName: String) {
        scope.launch {
            val ok = bridgeRepository.renameEditingPresetBridge(presetId, newName)
            if (ok) {
                loadEditingPresets()
                stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_renamed)) }
            }
        }
    }

    fun exportEditingPreset(presetId: String, exportPath: String, onSuccess: (String) -> Unit) {
        scope.launch {
            val path = bridgeRepository.exportEditingPresetBridge(presetId, exportPath)
            if (path != null) {
                onSuccess(path)
            } else {
                stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_export_io, listOf("Export .szlpreset error"))) }
            }
        }
    }

    fun importEditingPreset(importPath: String) {
        scope.launch {
            val ok = bridgeRepository.importEditingPresetBridge(importPath)
            if (ok) {
                loadEditingPresets()
                stateFlow.update { it.copy(userMessage = UiText.Resource(R.string.msg_preset_saved, listOf(File(importPath).nameWithoutExtension))) }
            } else {
                stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf("Import .szlpreset error"))) }
            }
        }
    }
}
