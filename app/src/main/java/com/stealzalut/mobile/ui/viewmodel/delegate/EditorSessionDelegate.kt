package com.stealzalut.mobile.ui.viewmodel.delegate

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.model.EditorParamsSnapshot
import com.stealzalut.mobile.data.model.EditorProject
import kotlinx.coroutines.delay
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LightroomDevelopModel
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.data.repository.ProjectRepository
import kotlinx.collections.immutable.toPersistentList
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
    val lightroomParams: LightroomDevelopModel = LightroomDevelopModel(),
    val cropRegion: CropRegion = CropRegion()
)

// EditorParamsSnapshot and EditorProject are imported from com.stealzalut.mobile.data.model

@OptIn(FlowPreview::class)
class EditorSessionDelegate(
    private val context: Context,
    private val bridgeRepository: PythonBridgeRepository,
    private val savedImageRepository: SavedImageRepository,
    private val projectRepository: ProjectRepository,
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
    private var gestureStartSnapshot: EditorParamsSnapshot? = null
    private var gestureDebounceJob: Job? = null

    val lightroomDelegate: EditorLightroomDelegate = EditorLightroomDelegate(
        stateFlow = stateFlow,
        onScheduleUpdate = { scheduleEditorPreviewUpdate() },
        onBeginGesture = { beginEditGesture() },
        onEndGesture = { endEditGesture() },
        onCommitUndo = { commitCurrentStateToUndo() }
    )

    private fun updateUndoRedoState() {
        stateFlow.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    private var autoSaveJob: Job? = null

    init {
        loadProjects()
        scope.launch {
            editorDebounceFlow
                .debounce(200L)
                .distinctUntilChanged()
                .collect { req ->
                    computeStandaloneEditorPreview(req)
                }
        }
    }

    fun loadProjects() {
        scope.launch {
            val list = projectRepository.getAllProjects()
            stateFlow.update { it.copy(editorProjects = list.toPersistentList()) }
        }
    }

    fun openProject(project: EditorProject) {
        scope.launch {
            activeEditorSourceJob?.cancel()
            activeStandaloneJob?.cancel()
            autoSaveJob?.cancel()
            undoStack.clear()
            redoStack.clear()
            gestureStartSnapshot = null
            gestureDebounceJob?.cancel()
            gestureDebounceJob = null
            updateUndoRedoState()

            val sourceFile = File(project.sourceImagePath)
            if (!sourceFile.exists()) {
                stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_image_load_failed, listOf("Fichier source introuvable"))) }
                return@launch
            }

            currentEditorSourceFile = sourceFile
            val thumbnail = withContext(Dispatchers.IO) {
                decodeSampledBitmap(sourceFile.absolutePath, 1080)
            }

            stateFlow.update {
                it.copy(
                    currentProjectId = project.id,
                    editorSourceUri = Uri.fromFile(sourceFile),
                    editorSourcePath = sourceFile.absolutePath,
                    editorSourceBitmap = thumbnail,
                    editorPreviewBitmap = thumbnail,
                    editorPreviewPath = sourceFile.absolutePath,
                    isEditorProcessing = false
                )
            }

            applySnapshot(project.snapshot)
            stateFlow.update { it.copy(editorCommittedSnapshot = project.snapshot) }
        }
    }

    fun deleteProject(id: String) {
        scope.launch {
            projectRepository.deleteProject(id)
            if (stateFlow.value.currentProjectId == id) {
                closeEditorPhoto()
            }
            loadProjects()
        }
    }

    fun renameProject(id: String, newName: String) {
        scope.launch {
            projectRepository.renameProject(id, newName)
            loadProjects()
        }
    }

    fun duplicateProject(id: String) {
        scope.launch {
            projectRepository.duplicateProject(id)
            loadProjects()
        }
    }

    fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(2000L) // Debounce >= 2 seconds
            performSaveCurrentProject()
        }
    }

    fun saveCurrentProjectNow() {
        autoSaveJob?.cancel()
        scope.launch {
            performSaveCurrentProject()
        }
    }

    private suspend fun performSaveCurrentProject() {
        val srcPath = stateFlow.value.editorSourcePath ?: return
        val srcFile = File(srcPath)
        if (!srcFile.exists()) return

        val curProjId = stateFlow.value.currentProjectId
        val previewPath = stateFlow.value.editorPreviewPath
        val previewFile = previewPath?.let { File(it) }?.takeIf { it.exists() }

        if (curProjId == null) {
            val newProj = projectRepository.createProject(
                sourceFile = srcFile,
                snapshot = currentSnapshot(),
                previewFile = previewFile
            )
            stateFlow.update { it.copy(currentProjectId = newProj.id) }
        } else {
            val existing = projectRepository.getProject(curProjId)
            if (existing != null) {
                val updated = existing.copy(
                    updatedAt = System.currentTimeMillis(),
                    snapshot = currentSnapshot(),
                    previewImagePath = previewFile?.absolutePath ?: existing.previewImagePath
                )
                projectRepository.saveProject(updated)
            }
        }
        loadProjects()
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
            lightroomParams = s.editorLightroomParams,
            cropRegion = s.editorCropRegion
        )
    }

    fun beginEditGesture() {
        if (gestureStartSnapshot == null) {
            gestureStartSnapshot = currentSnapshot()
        }
        gestureDebounceJob?.cancel()
        gestureDebounceJob = scope.launch {
            delay(500L)
            endEditGesture()
        }
    }

    fun endEditGesture() {
        gestureDebounceJob?.cancel()
        gestureDebounceJob = null
        val start = gestureStartSnapshot ?: return
        gestureStartSnapshot = null
        val current = currentSnapshot()
        if (start != current) {
            if (undoStack.peek() != start) {
                undoStack.push(start)
                if (undoStack.size > 50) {
                    undoStack.removeLast()
                }
                redoStack.clear()
                updateUndoRedoState()
            }
        }
    }

    fun commitCurrentStateToUndo() {
        endEditGesture()
        val current = currentSnapshot()
        if (undoStack.peek() != current) {
            undoStack.push(current)
            if (undoStack.size > 50) {
                undoStack.removeLast()
            }
            redoStack.clear()
            updateUndoRedoState()
        }
    }

    fun pushUndoSnapshot() {
        commitCurrentStateToUndo()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        endEditGesture()
        if (undoStack.isEmpty()) return
        val current = currentSnapshot()
        redoStack.push(current)
        val previous = undoStack.pop()
        applySnapshot(previous)
        updateUndoRedoState()
    }

    fun redo() {
        endEditGesture()
        if (redoStack.isEmpty()) return
        val current = currentSnapshot()
        undoStack.push(current)
        if (undoStack.size > 50) {
            undoStack.removeLast()
        }
        val next = redoStack.pop()
        applySnapshot(next)
        updateUndoRedoState()
    }

    fun updateCropRegion(region: CropRegion) {
        beginEditGesture()
        stateFlow.update { it.copy(editorCropRegion = region) }
        scheduleEditorPreviewUpdate()
    }

    fun onCropFinished() {
        endEditGesture()
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
                editorLightroomParams = snapshot.lightroomParams,
                editorCropRegion = snapshot.cropRegion
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun setEditorSourceImage(uri: Uri) {
        activeEditorSourceJob?.cancel()
        activeStandaloneJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        gestureStartSnapshot = null
        gestureDebounceJob?.cancel()
        gestureDebounceJob = null
        updateUndoRedoState()
        activeEditorSourceJob = scope.launch {
            stateFlow.update { it.copy(isEditorProcessing = true, errorMessage = null) }
            val ext = bridgeRepository.resolveExtensionFromUri(uri)
            val filename = "editor_source_${java.util.UUID.randomUUID().toString().take(8)}.$ext"
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
                    gestureStartSnapshot = null
                    gestureDebounceJob?.cancel()
                    gestureDebounceJob = null
                    updateUndoRedoState()
                    stateFlow.update {
                        it.copy(
                            editorSourceUri = uri,
                            editorSourcePath = file.absolutePath,
                            editorSourceBitmap = thumbnail,
                            editorPreviewBitmap = thumbnail,
                            editorPreviewPath = file.absolutePath,
                            isEditorProcessing = false,
                            editorCommittedSnapshot = currentSnapshot()
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
        beginEditGesture()
        stateFlow.update { it.copy(step2Intensity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2LumaLock(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step2LumaLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2NeutralAxisLock(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step2NeutralAxisLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2SkinToneLock(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step2SkinToneLock = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2ExtremesRolloff(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step2ExtremesRolloff = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep2Spatial3d(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step2Spatial3d = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    // --- Stage 3 Setters (Post-Grading) ---
    fun setStep3Exposure(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Exposure = v.coerceIn(-2.0f, 2.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Contrast(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Contrast = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Highlights(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Highlights = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Shadows(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Shadows = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Temperature(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Temperature = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Tint(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Tint = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Vibrance(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Vibrance = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Saturation(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Saturation = v.coerceIn(-1.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Clarity(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Clarity = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Sharpen(v: Float) {
        beginEditGesture()
        stateFlow.update { it.copy(step3Sharpen = v.coerceIn(0.0f, 1.0f)) }
        scheduleEditorPreviewUpdate()
    }

    fun setStep3Grain(v: Float) {
        beginEditGesture()
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

                    val currentLr = stateFlow.value.editorLightroomParams
                    val updatedBasic = currentLr.basic.copy(
                        exposure = autoSettings.exposure.coerceIn(-5.0f, 5.0f),
                        contrast = autoSettings.contrast.coerceIn(-100.0f, 100.0f),
                        highlights = autoSettings.highlights.coerceIn(-100.0f, 100.0f),
                        shadows = autoSettings.shadows.coerceIn(-100.0f, 100.0f),
                        whites = autoSettings.whites.coerceIn(-100.0f, 100.0f),
                        blacks = autoSettings.blacks.coerceIn(-100.0f, 100.0f),
                        vibrance = autoSettings.vibrance.coerceIn(-100.0f, 100.0f),
                        saturation = autoSettings.saturation.coerceIn(-100.0f, 100.0f)
                    )
                    val updatedLr = currentLr.copy(basic = updatedBasic, isBasicEnabled = true)

                    pushUndoSnapshot()
                    stateFlow.update {
                        it.copy(
                            isComputingEdit = false,
                            isAutoEditActive = true,
                            editorLightroomParams = updatedLr,
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
        commitCurrentStateToUndo()
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
                step3Grain = 0.0f,
                editorLightroomParams = LightroomDevelopModel(),
                editorCropRegion = CropRegion()
            )
        }
        scheduleEditorPreviewUpdate()
    }

    fun scheduleEditorPreviewUpdate() {
        val src = stateFlow.value.editorSourcePath ?: return
        val effectiveCrop = if (stateFlow.value.editorActiveTool == 0) {
            com.stealzalut.mobile.data.model.CropRegion()
        } else {
            stateFlow.value.editorCropRegion
        }
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
            lightroomParams = stateFlow.value.editorLightroomParams,
            cropRegion = effectiveCrop
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
                lightroomParams = req.lightroomParams,
                cropRegion = req.cropRegion
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
                            isEditorProcessing = false,
                            editorCommittedSnapshot = currentSnapshot()
                        )
                    }
                    triggerAutoSave()
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
                        onSuccess(target.canonicalPath)
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

    fun exportEditorImage(
        format: String? = null,
        quality: Int? = null,
        maxDim: Int? = null,
        onSavedImageRecorded: () -> Unit
    ) {
        if (stateFlow.value.isEditorExportingImage) return
        val src = stateFlow.value.editorSourcePath ?: return
        val effFormat = format ?: stateFlow.value.settings.exportFormat
        val effQuality = quality ?: stateFlow.value.settings.exportQuality
        val effMaxDim = maxDim ?: stateFlow.value.settings.maxExportDim
        scope.launch {
            stateFlow.update { it.copy(isEditorExportingImage = true, errorMessage = null) }
            val res = bridgeRepository.exportEditorImage(
                srcPath = src,
                maxExportDim = effMaxDim,
                format = effFormat,
                quality = effQuality,
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
                lightroomParams = stateFlow.value.editorLightroomParams,
                cropRegion = stateFlow.value.editorCropRegion
            )

            when (res) {
                is ImageExportResult.Success -> {
                    val displayName = "StealZaLUT_Editor_${System.currentTimeMillis()}.${if (effFormat.equals("PNG", true)) "png" else "jpg"}"
                    val mediaStoreResult = bridgeRepository.saveImageToMediaStore(
                        imagePath = res.imagePath,
                        displayName = displayName,
                        mimeType = if (effFormat.equals("PNG", true)) "image/png" else "image/jpeg"
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
        saveCurrentProjectNow()
        undoStack.clear()
        redoStack.clear()
        gestureStartSnapshot = null
        gestureDebounceJob?.cancel()
        gestureDebounceJob = null
        updateUndoRedoState()
        stateFlow.update {
            it.copy(
                currentProjectId = null,
                editorSourceUri = null,
                editorSourcePath = null,
                editorSourceBitmap = null,
                editorPreviewBitmap = null,
                editorPreviewPath = null,
                editorCustomLutPath = null,
                editorCustomLutName = null,
                selectedEditingPreset = null,
                editorLightroomParams = LightroomDevelopModel(),
                editorCropRegion = CropRegion()
            )
        }
        loadProjects()
    }
}
