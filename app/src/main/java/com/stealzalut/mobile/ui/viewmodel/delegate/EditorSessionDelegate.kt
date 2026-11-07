package com.stealzalut.mobile.ui.viewmodel.delegate

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LightroomDevelopModel
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.ExportType
import com.stealzalut.mobile.ui.viewmodel.PendingExport
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

data class StandaloneEditorDebounceRequest(
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
    val customLutPath: String?,
    val lightroomParams: LightroomDevelopModel = LightroomDevelopModel()
)

data class EditorParamsSnapshot(
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
    val customLutPath: String?,
    val customLutName: String?,
    val lightroomParams: LightroomDevelopModel = LightroomDevelopModel()
)

@OptIn(FlowPreview::class)
class EditorSessionDelegate(
    private val context: Context,
    private val bridgeRepository: PythonBridgeRepository,
    private val savedImageRepository: SavedImageRepository,
    private val stateFlow: MutableStateFlow<StealZaLutUiState>,
    private val scope: CoroutineScope,
    private val decodeSampledBitmap: (path: String, maxDim: Int) -> Bitmap?
) {
    private val editorDebounceFlow = MutableSharedFlow<StandaloneEditorDebounceRequest>(replay = 1)
    private var activeStandaloneJob: Job? = null
    private var activeEditorSourceJob: Job? = null
    private var currentEditorSourceFile: File? = null

    // Undo / Redo Stacks (Phase 4 UX Pro)
    private val undoStack = ArrayDeque<EditorParamsSnapshot>()
    private val redoStack = ArrayDeque<EditorParamsSnapshot>()

    val lightroomDelegate: EditorLightroomDelegate = EditorLightroomDelegate(
        stateFlow = stateFlow,
        onScheduleUpdate = { scheduleEditorPreviewUpdate() }
    )

    init {
        scope.launch {
            editorDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { req ->
                    computeStandaloneEditorPreview(req)
                }
        }
    }

    private fun currentSnapshot(): EditorParamsSnapshot {
        val s = stateFlow.value
        return EditorParamsSnapshot(
            intensity = s.step2Intensity,
            lumaLock = s.step2LumaLock,
            neutralAxisLock = s.step2NeutralAxisLock,
            skinToneLock = s.step2SkinToneLock,
            extremesRolloff = s.step2ExtremesRolloff,
            spatial3d = s.step2Spatial3d,
            exposure = s.step3Exposure,
            contrast = s.step3Contrast,
            highlights = s.step3Highlights,
            shadows = s.step3Shadows,
            temperature = s.step3Temperature,
            tint = s.step3Tint,
            vibrance = s.step3Vibrance,
            saturation = s.step3Saturation,
            clarity = s.step3Clarity,
            sharpen = s.step3Sharpen,
            grain = s.step3Grain,
            customLutPath = s.editorCustomLutPath,
            customLutName = s.editorCustomLutName,
            lightroomParams = s.editorLightroomParams
        )
    }

    private fun pushUndoSnapshot() {
        undoStack.push(currentSnapshot())
        if (undoStack.size > 50) {
            undoStack.removeLast()
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.push(currentSnapshot())
        val previous = undoStack.pop()
        applySnapshot(previous)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.push(currentSnapshot())
        val next = redoStack.pop()
        applySnapshot(next)
    }

    private fun applySnapshot(snapshot: EditorParamsSnapshot) {
        stateFlow.update {
            it.copy(
                step2Intensity = snapshot.intensity,
                step2LumaLock = snapshot.lumaLock,
                step2NeutralAxisLock = snapshot.neutralAxisLock,
                step2SkinToneLock = snapshot.skinToneLock,
                step2ExtremesRolloff = snapshot.extremesRolloff,
                step2Spatial3d = snapshot.spatial3d,
                step3Exposure = snapshot.exposure,
                step3Contrast = snapshot.contrast,
                step3Highlights = snapshot.highlights,
                step3Shadows = snapshot.shadows,
                step3Temperature = snapshot.temperature,
                step3Tint = snapshot.tint,
                step3Vibrance = snapshot.vibrance,
                step3Saturation = snapshot.saturation,
                step3Clarity = snapshot.clarity,
                step3Sharpen = snapshot.sharpen,
                step3Grain = snapshot.grain,
                editorCustomLutPath = snapshot.customLutPath,
                editorCustomLutName = snapshot.customLutName,
                editorLightroomParams = snapshot.lightroomParams
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun setEditorSourceImage(uri: Uri) {
        activeEditorSourceJob?.cancel()
        activeStandaloneJob?.cancel()
        activeEditorSourceJob = scope.launch {
            stateFlow.update { it.copy(isEditorProcessing = true, errorMessage = null) }
            val filename = "editor_source_${java.util.UUID.randomUUID().toString().take(8)}.jpg"
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, filename)
            cacheResult.fold(
                onSuccess = { file ->
                    bridgeRepository.lruCacheManager.protectFile(file.absolutePath)
                    val oldFile = currentEditorSourceFile
                    currentEditorSourceFile = file
                    if (oldFile != null && oldFile.absolutePath != file.absolutePath) {
                        bridgeRepository.lruCacheManager.unprotectFile(oldFile.absolutePath)
                        oldFile.delete()
                    }

                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    undoStack.clear()
                    redoStack.clear()
                    stateFlow.update {
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
                    stateFlow.update {
                        it.copy(
                            isEditorProcessing = false,
                            errorMessage = UiText.Resource(R.string.err_image_load_failed, listOf(error.localizedMessage ?: ""))
                        )
                    }
                }
            )
        }
    }

    fun setEditorCustomLut(uri: Uri, name: String) {
        scope.launch {
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "custom_lut_${System.currentTimeMillis()}.cube")
            cacheResult.fold(
                onSuccess = { file ->
                    pushUndoSnapshot()
                    stateFlow.update {
                        it.copy(
                            editorCustomLutPath = file.absolutePath,
                            editorCustomLutName = name
                        )
                    }
                    scheduleEditorPreviewUpdate()
                },
                onFailure = { error ->
                    stateFlow.update {
                        it.copy(errorMessage = UiText.Resource(R.string.err_lut_import_failed, listOf(error.localizedMessage ?: "")))
                    }
                }
            )
        }
    }

    fun clearEditorCustomLut() {
        pushUndoSnapshot()
        stateFlow.update {
            it.copy(
                editorCustomLutPath = null,
                editorCustomLutName = null
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun setEditorTab(tab: Int) {
        stateFlow.update { it.copy(editorSelectedTab = tab) }
    }

    // --- Stage 2 Setters (Filter Refinement) ---
    fun setStep2Intensity(v: Float) {
        stateFlow.update { it.copy(step2Intensity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2LumaLock(v: Float) {
        stateFlow.update { it.copy(step2LumaLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2NeutralAxisLock(v: Float) {
        stateFlow.update { it.copy(step2NeutralAxisLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2SkinToneLock(v: Float) {
        stateFlow.update { it.copy(step2SkinToneLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2ExtremesRolloff(v: Float) {
        stateFlow.update { it.copy(step2ExtremesRolloff = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2Spatial3d(v: Float) {
        stateFlow.update { it.copy(step2Spatial3d = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    // --- Stage 3 Setters (Post-Grading) ---
    fun setStep3Exposure(v: Float) {
        stateFlow.update { it.copy(step3Exposure = v.coerceIn(-2.0f, 2.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Contrast(v: Float) {
        stateFlow.update { it.copy(step3Contrast = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Highlights(v: Float) {
        stateFlow.update { it.copy(step3Highlights = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Shadows(v: Float) {
        stateFlow.update { it.copy(step3Shadows = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Temperature(v: Float) {
        stateFlow.update { it.copy(step3Temperature = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Tint(v: Float) {
        stateFlow.update { it.copy(step3Tint = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Vibrance(v: Float) {
        stateFlow.update { it.copy(step3Vibrance = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Saturation(v: Float) {
        stateFlow.update { it.copy(step3Saturation = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Clarity(v: Float) {
        stateFlow.update { it.copy(step3Clarity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Sharpen(v: Float) {
        stateFlow.update { it.copy(step3Sharpen = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Grain(v: Float) {
        stateFlow.update { it.copy(step3Grain = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    // --- Reset Actions ---
    fun resetStep2Params() {
        pushUndoSnapshot()
        stateFlow.update {
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
        pushUndoSnapshot()
        stateFlow.update {
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

    fun applyAutoEdit() {
        val current = stateFlow.value
        val imagePath = current.editorPreviewPath ?: current.editorSourcePath ?: return

        scope.launch {
            stateFlow.update { it.copy(isComputingEdit = true, errorMessage = null) }
            val autoSettings = bridgeRepository.computeAutoSettings(imagePath)
            when (autoSettings) {
                is AutoSettingsResult.Success -> {
                    val expNorm = autoSettings.exposure.coerceIn(-2.0f, 2.0f)
                    val contrastNorm = (autoSettings.contrast / 100.0f).coerceIn(-1.0f, 1.0f)
                    val highlightsNorm = (autoSettings.highlights / 100.0f).coerceIn(-1.0f, 1.0f)
                    val shadowsNorm = (autoSettings.shadows / 100.0f).coerceIn(-1.0f, 1.0f)
                    val vibranceNorm = (autoSettings.vibrance / 100.0f).coerceIn(-1.0f, 1.0f)

                    pushUndoSnapshot()
                    stateFlow.update {
                        it.copy(
                            isComputingEdit = false,
                            isAutoEditActive = true,
                            step3Exposure = expNorm,
                            step3Contrast = contrastNorm,
                            step3Highlights = highlightsNorm,
                            step3Shadows = shadowsNorm,
                            step3Vibrance = vibranceNorm,
                            userMessage = UiText.Resource(
                                R.string.msg_auto_edit_applied,
                                listOf(expNorm, contrastNorm * 100f, shadowsNorm * 100f)
                            )
                        )
                    }
                    scheduleEditorPreviewUpdate()
                }
                is AutoSettingsResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isComputingEdit = false,
                            errorMessage = autoSettings.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun resetEditingParameters() {
        pushUndoSnapshot()
        stateFlow.update {
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
                step3Grain = 0.0f
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun resetAllEditorParams() {
        pushUndoSnapshot()
        stateFlow.update {
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

    fun scheduleEditorPreviewUpdate() {
        val src = stateFlow.value.editorSourcePath ?: return
        val req = StandaloneEditorDebounceRequest(
            srcPath = src,
            intensity = stateFlow.value.step2Intensity,
            lumaLock = stateFlow.value.step2LumaLock,
            neutralAxisLock = stateFlow.value.step2NeutralAxisLock,
            skinToneLock = stateFlow.value.step2SkinToneLock,
            extremesRolloff = stateFlow.value.step2ExtremesRolloff,
            spatial3d = stateFlow.value.step2Spatial3d,
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
            grain = stateFlow.value.step3Grain,
            customLutPath = stateFlow.value.editorCustomLutPath,
            lightroomParams = stateFlow.value.editorLightroomParams
        )
        editorDebounceFlow.tryEmit(req)
    }

    private suspend fun computeStandaloneEditorPreview(req: StandaloneEditorDebounceRequest) {
        activeStandaloneJob?.cancel()
        activeStandaloneJob = scope.launch {
            stateFlow.update { it.copy(isEditorProcessing = true) }
            val res = bridgeRepository.generateEditorPreview(
                srcPath = req.srcPath,
                lutSize = stateFlow.value.lutSize,
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
                customLutPath = req.customLutPath,
                lightroomParams = req.lightroomParams
            )

            when (res) {
                is PreviewResult.Success -> {
                    val bitmap = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(res.previewPath, 1080)
                    }
                    stateFlow.update {
                        it.copy(
                            editorPreviewBitmap = bitmap,
                            editorPreviewPath = res.previewPath,
                            editorExecutionTimeMs = res.executionTimeMs,
                            isEditorProcessing = false
                        )
                    }
                }
                is PreviewResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isEditorProcessing = false,
                            errorMessage = if (res.customMessage != null) UiText.DynamicString(res.customMessage) else res.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun exportEditorCube(onSuccess: (String) -> Unit) {
        if (stateFlow.value.isEditorExportingCube) return
        val src = stateFlow.value.editorSourcePath ?: return
        scope.launch {
            stateFlow.update { it.copy(isEditorExportingCube = true, errorMessage = null) }
            val res = bridgeRepository.exportEditorCube(
                srcPath = src,
                lutSize = stateFlow.value.lutSize,
                intensity = stateFlow.value.step2Intensity,
                lumaLock = stateFlow.value.step2LumaLock,
                neutralAxisLock = stateFlow.value.step2NeutralAxisLock,
                skinToneLock = stateFlow.value.step2SkinToneLock,
                extremesRolloff = stateFlow.value.step2ExtremesRolloff,
                spatial3d = stateFlow.value.step2Spatial3d,
                exposure = stateFlow.value.step3Exposure,
                contrast = stateFlow.value.step3Contrast,
                highlights = stateFlow.value.step3Highlights,
                shadows = stateFlow.value.step3Shadows,
                temperature = stateFlow.value.step3Temperature,
                tint = stateFlow.value.step3Tint,
                vibrance = stateFlow.value.step3Vibrance,
                saturation = stateFlow.value.step3Saturation,
                customLutPath = stateFlow.value.editorCustomLutPath,
                lightroomParams = stateFlow.value.editorLightroomParams
            )

            when (res) {
                is CubeExportResult.Success -> {
                    val target = File(res.cubePath)
                    if (!target.exists() || !target.isFile || target.length() <= 0L) {
                        stateFlow.update {
                            it.copy(
                                isEditorExportingCube = false,
                                errorMessage = UiText.Resource(R.string.err_export_io, listOf("Fichier .cube généré vide ou introuvable"))
                            )
                        }
                    } else {
                        val fileName = target.name
                        stateFlow.update {
                            it.copy(
                                isEditorExportingCube = false,
                                exportedCubePath = target.canonicalPath,
                                pendingExport = PendingExport(
                                    sourcePath = target.canonicalPath,
                                    suggestedFileName = fileName,
                                    exportType = ExportType.CUBE_LUT
                                )
                            )
                        }
                        onSuccess(fileName)
                    }
                }
                is CubeExportResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isEditorExportingCube = false,
                            errorMessage = res.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun exportEditorImage(onSavedImageRecorded: () -> Unit) {
        if (stateFlow.value.isEditorExportingImage) return
        val src = stateFlow.value.editorSourcePath ?: return
        scope.launch {
            stateFlow.update { it.copy(isEditorExportingImage = true, errorMessage = null) }
            val res = bridgeRepository.exportEditorImage(
                srcPath = src,
                maxExportDim = stateFlow.value.settings.maxExportDim,
                quality = stateFlow.value.settings.exportQuality,
                lutSize = stateFlow.value.lutSize,
                intensity = stateFlow.value.step2Intensity,
                lumaLock = stateFlow.value.step2LumaLock,
                neutralAxisLock = stateFlow.value.step2NeutralAxisLock,
                skinToneLock = stateFlow.value.step2SkinToneLock,
                extremesRolloff = stateFlow.value.step2ExtremesRolloff,
                spatial3d = stateFlow.value.step2Spatial3d,
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
                grain = stateFlow.value.step3Grain,
                customLutPath = stateFlow.value.editorCustomLutPath,
                lightroomParams = stateFlow.value.editorLightroomParams
            )

            when (res) {
                is ImageExportResult.Success -> {
                    val displayName = "StealZaLUT_Editor_${System.currentTimeMillis()}.${if (stateFlow.value.settings.exportFormat.equals("PNG", true)) "png" else "jpg"}"
                    val mediaStoreResult = bridgeRepository.saveImageToMediaStore(
                        imagePath = res.imagePath,
                        displayName = displayName,
                        mimeType = if (stateFlow.value.settings.exportFormat.equals("PNG", true)) "image/png" else "image/jpeg"
                    )

                    mediaStoreResult.fold(
                        onSuccess = {
                            savedImageRepository.recordGradedImage(
                                sourceFilePath = res.imagePath,
                                width = res.width,
                                height = res.height,
                                themeName = stateFlow.value.editorCustomLutName ?: "Édition Personnalisée",
                                method = "3-stage-pipeline",
                                intensity = stateFlow.value.step2Intensity,
                                referenceImagePath = stateFlow.value.editorCustomLutPath,
                                paletteHexes = emptyList()
                            )
                            onSavedImageRecorded()
                            stateFlow.update {
                                it.copy(
                                    isEditorExportingImage = false,
                                    userMessage = UiText.Resource(R.string.msg_photo_saved)
                                )
                            }
                        },
                        onFailure = { error ->
                            stateFlow.update {
                                it.copy(
                                    isEditorExportingImage = false,
                                    errorMessage = UiText.Resource(R.string.err_mediastore, listOf(error.localizedMessage ?: ""))
                                )
                            }
                        }
                    )
                }
                is ImageExportResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isEditorExportingImage = false,
                            errorMessage = res.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun closeEditorPhoto() {
        undoStack.clear()
        redoStack.clear()
        stateFlow.update {
            it.copy(
                editorSourceUri = null,
                editorSourcePath = null,
                editorSourceBitmap = null,
                editorPreviewBitmap = null,
                editorPreviewPath = null,
                editorCustomLutPath = null,
                editorCustomLutName = null,
                selectedEditingPreset = null
            )
        }
    }
}
