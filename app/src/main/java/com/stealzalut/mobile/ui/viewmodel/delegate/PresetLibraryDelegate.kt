package com.stealzalut.mobile.ui.viewmodel.delegate

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import com.stealzalut.mobile.ui.viewmodel.ExportType
import com.stealzalut.mobile.ui.viewmodel.PendingExport
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var activePaletteJob: Job? = null
    private var currentPaletteFile: File? = null

    private val paletteExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PresetLibraryDelegate", "Palette extraction failed", throwable)
        stateFlow.update {
            it.copy(
                isExtractingPalette = false,
                errorMessage = UiText.Resource(R.string.err_palette_extraction_failed)
            )
        }
    }

    fun setPalettePhoto(uri: Uri) {
        activePaletteJob?.cancel()
        activePaletteJob = scope.launch(paletteExceptionHandler) {
            stateFlow.update { it.copy(isExtractingPalette = true, errorMessage = null) }
            val filename = "palette_${java.util.UUID.randomUUID().toString().take(8)}.jpg"
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, filename)
            cacheResult.fold(
                onSuccess = { file ->
                    bridgeRepository.lruCacheManager.protectFile(file.absolutePath)
                    val oldFile = currentPaletteFile
                    currentPaletteFile = file
                    if (oldFile != null && oldFile.absolutePath != file.absolutePath) {
                        bridgeRepository.lruCacheManager.unprotectFile(oldFile.absolutePath)
                        oldFile.delete()
                    }

                    val thumbnail = withContext(Dispatchers.IO) {
                        runCatching {
                            decodeSampledBitmap(file.absolutePath, 512)
                        }.getOrNull()
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
        scope.launch(paletteExceptionHandler) {
            stateFlow.update { it.copy(isExtractingPalette = true, errorMessage = null) }
            val palette = try {
                bridgeRepository.extractPalette(imagePath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
            } catch (t: Throwable) {
                Log.e("PresetLibraryDelegate", "Palette extraction bridge error", t)
                emptyList()
            }
            stateFlow.update {
                if (palette.isNotEmpty()) {
                    it.copy(
                        isExtractingPalette = false,
                        extractedPalette = palette.toImmutableList(),
                        userMessage = UiText.Resource(R.string.msg_palette_extracted)
                    )
                } else {
                    it.copy(
                        isExtractingPalette = false,
                        errorMessage = UiText.Resource(R.string.err_palette_extraction_failed)
                    )
                }
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

        // Use source photo if available, or extract directly from reference image using neutral calibration lattice
        val candidateSource = stateFlow.value.sourcePath ?: stateFlow.value.editorSourcePath
        val sourcePhoto = if (candidateSource != null && File(candidateSource).exists()) candidateSource else ""

        scope.launch {
            stateFlow.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = sourcePhoto,
                refPath = refPath,
                lutSize = preset.lutSize,
                method = preset.method,
                intensity = preset.intensity
            )
            when (result) {
                is CubeExportResult.Success -> {
                    val target = File(result.cubePath)
                    if (!target.exists() || !target.isFile || target.length() <= 0L) {
                        stateFlow.update {
                            it.copy(
                                isExportingCube = false,
                                errorMessage = UiText.Resource(R.string.err_export_io, listOf("Fichier .cube généré vide ou introuvable"))
                            )
                        }
                    } else {
                        val fileName = target.name
                        stateFlow.update {
                            it.copy(
                                isExportingCube = false,
                                exportedCubePath = target.canonicalPath,
                                pendingExport = PendingExport(
                                    sourcePath = target.canonicalPath,
                                    suggestedFileName = fileName,
                                    exportType = ExportType.CUBE_LUT
                                )
                            )
                        }
                        onReadyToSave(target.canonicalPath)
                    }
                }
                is CubeExportResult.Error -> {
                    stateFlow.update {
                        it.copy(isExportingCube = false, errorMessage = result.error.uiText)
                    }
                }
            }
        }
    }

    fun exportCubeFromPaletteImage(onReadyToSave: (String) -> Unit) {
        if (stateFlow.value.isExportingCube) return
        val refPath = stateFlow.value.palettePhotoPath
        if (refPath == null || !File(refPath).exists()) {
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_preset_no_ref)) }
            return
        }

        val candidateSource = stateFlow.value.sourcePath ?: stateFlow.value.editorSourcePath
        val sourcePhoto = if (candidateSource != null && File(candidateSource).exists()) candidateSource else ""

        scope.launch {
            stateFlow.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = sourcePhoto,
                refPath = refPath,
                lutSize = stateFlow.value.lutSize,
                method = stateFlow.value.method,
                intensity = stateFlow.value.intensity
            )
            when (result) {
                is CubeExportResult.Success -> {
                    val target = File(result.cubePath)
                    if (!target.exists() || !target.isFile || target.length() <= 0L) {
                        stateFlow.update {
                            it.copy(
                                isExportingCube = false,
                                errorMessage = UiText.Resource(R.string.err_export_io, listOf("Fichier .cube généré vide ou introuvable"))
                            )
                        }
                    } else {
                        val fileName = target.name
                        stateFlow.update {
                            it.copy(
                                isExportingCube = false,
                                exportedCubePath = target.canonicalPath,
                                pendingExport = PendingExport(
                                    sourcePath = target.canonicalPath,
                                    suggestedFileName = fileName,
                                    exportType = ExportType.CUBE_LUT
                                )
                            )
                        }
                        onReadyToSave(target.canonicalPath)
                    }
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
        val resolvedLutPath: String? = preset.lutReference?.let { lutPath ->
            val direct = File(lutPath)
            if (direct.exists() && direct.isFile && direct.length() > 0L) {
                direct.absolutePath
            } else {
                val fileName = direct.name
                val imported = File(File(application.filesDir, "imported_luts"), fileName)
                if (imported.exists() && imported.isFile && imported.length() > 0L) {
                    imported.absolutePath
                } else {
                    val inLuts = File(File(application.filesDir, "luts"), fileName)
                    if (inLuts.exists() && inLuts.isFile && inLuts.length() > 0L) {
                        inLuts.absolutePath
                    } else {
                        null
                    }
                }
            }
        }

        val lutMissingWarning = preset.lutReference != null && resolvedLutPath == null

        stateFlow.update {
            it.copy(
                selectedEditingPreset = preset,
                editorCustomLutPath = resolvedLutPath,
                editorCustomLutName = resolvedLutPath?.let { p -> File(p).name },
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
                editorLightroomParams = preset.lightroomParams,
                userMessage = if (lutMissingWarning) {
                    UiText.DynamicString("Preset « ${preset.name} » appliqué (la LUT 3D d'origine est introuvable sur cet appareil)")
                } else {
                    UiText.Resource(R.string.msg_preset_applied, listOf(preset.name))
                }
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

    fun exportEditingPreset(preset: EditPreset, onReadyToSave: (String) -> Unit) {
        scope.launch {
            val safeName = preset.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_").trim()
            val filename = "${if (safeName.isNotEmpty()) safeName else "preset"}.szlpreset"
            val tempFile = File(application.cacheDir, filename)
            val exportedPath = bridgeRepository.exportEditingPresetBridge(preset.id, tempFile.absolutePath)
            if (exportedPath != null && File(exportedPath).length() > 0) {
                stateFlow.update {
                    it.copy(
                        pendingExport = PendingExport(
                            sourcePath = exportedPath,
                            suggestedFileName = filename,
                            exportType = ExportType.PRESET_JSON
                        )
                    )
                }
                onReadyToSave(filename)
            } else {
                stateFlow.update {
                    it.copy(
                        errorMessage = UiText.Resource(
                            R.string.err_export_io,
                            listOf("Export .szlpreset error")
                        )
                    )
                }
            }
        }
    }

    fun importEditingPresetFromUri(uri: Uri) {
        scope.launch {
            val tempFile = File(application.cacheDir, "import_preset_${System.currentTimeMillis()}.szlpreset")
            try {
                application.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    stateFlow.update {
                        it.copy(errorMessage = UiText.Resource(R.string.err_preset_import_failed))
                    }
                    return@launch
                }
                val ok = bridgeRepository.importEditingPresetBridge(tempFile.absolutePath)
                if (ok) {
                    loadEditingPresets()
                    stateFlow.update {
                        it.copy(
                            userMessage = UiText.Resource(
                                R.string.msg_preset_imported,
                                listOf(tempFile.nameWithoutExtension)
                            )
                        )
                    }
                } else {
                    stateFlow.update {
                        it.copy(errorMessage = UiText.Resource(R.string.err_preset_import_failed))
                    }
                }
            } catch (t: Throwable) {
                Log.e("PresetLibraryDelegate", "Failed to import preset from uri", t)
                stateFlow.update {
                    it.copy(errorMessage = UiText.Resource(R.string.err_preset_import_failed))
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
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
