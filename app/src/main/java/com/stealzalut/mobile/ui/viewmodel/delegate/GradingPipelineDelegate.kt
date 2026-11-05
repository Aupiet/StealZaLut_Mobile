package com.stealzalut.mobile.ui.viewmodel.delegate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.AutoEditResult
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.EditResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(FlowPreview::class)
class GradingPipelineDelegate(
    private val context: Context,
    private val bridgeRepository: PythonBridgeRepository,
    private val savedImageRepository: SavedImageRepository,
    private val stateFlow: MutableStateFlow<StealZaLutUiState>,
    private val scope: CoroutineScope,
    private val decodeSampledBitmap: (path: String, maxDim: Int) -> Bitmap?
) {
    private val previewDebounceFlow = MutableSharedFlow<PreviewDebounceRequest>(replay = 1)
    private val editDebounceFlow = MutableSharedFlow<EditDebounceRequest>(replay = 1)
    private var activeGradingJob: Job? = null
    private var activeEditJob: Job? = null
    private var pendingExportCubePath: String? = null

    init {
        scope.launch {
            previewDebounceFlow
                .debounce(200L)
                .distinctUntilChanged()
                .collect { req ->
                    computePythonPreview(req)
                }
        }

        scope.launch {
            editDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { req ->
                    computeEditedPreview(req)
                }
        }
    }

    fun setSourceImage(uri: Uri) {
        scope.launch {
            stateFlow.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "source_input.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    stateFlow.update {
                        it.copy(
                            sourceUri = uri,
                            sourcePath = file.absolutePath,
                            sourceBitmap = thumbnail,
                            isProcessingPreview = false,
                            basePreviewBitmap = null,
                            previewPath = null
                        )
                    }
                    if (stateFlow.value.effectiveReferencePath != null) {
                        triggerDebouncedPreview()
                    }
                },
                onFailure = { error ->
                    stateFlow.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = UiText.Resource(R.string.err_source_load, listOf(error.localizedMessage ?: ""))
                        )
                    }
                }
            )
        }
    }

    fun selectThemeToApply(preset: ColorimetryPreset) {
        stateFlow.update {
            it.copy(
                selectedTheme = preset,
                styleBSourceMode = 1,
                customReferencePath = null,
                customReferenceBitmap = null,
                customReferenceUri = null,
                method = preset.method,
                intensity = preset.intensity,
                lutSize = preset.lutSize,
                userMessage = UiText.Resource(R.string.msg_preset_selected, listOf(preset.name))
            )
        }
        if (stateFlow.value.sourcePath != null && stateFlow.value.effectiveReferencePath != null) {
            triggerDebouncedPreview()
        }
    }

    fun setCustomReferenceForApply(uri: Uri) {
        scope.launch {
            stateFlow.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, "custom_ref_apply.jpg")
            cacheResult.fold(
                onSuccess = { file ->
                    val thumbnail = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file.absolutePath, 1080)
                    }
                    val palette = withContext(Dispatchers.IO) {
                        bridgeRepository.extractPalette(file.absolutePath, AppConstants.DEFAULT_PALETTE_COLOR_COUNT)
                    }
                    stateFlow.update {
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
                    if (stateFlow.value.sourcePath != null) {
                        triggerDebouncedPreview()
                    }
                },
                onFailure = { error ->
                    stateFlow.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = UiText.Resource(R.string.err_reference_load, listOf(error.localizedMessage ?: ""))
                        )
                    }
                }
            )
        }
    }

    fun setMethod(method: ColorTransferMethod) {
        if (stateFlow.value.method == method) return
        stateFlow.update { it.copy(method = method) }
        if (stateFlow.value.canApply) triggerDebouncedPreview()
    }

    fun setIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0f, 1f)
        if (stateFlow.value.intensity == clamped) return
        stateFlow.update {
            it.copy(intensity = clamped, isGradedImageSaved = false)
        }
        triggerDebouncedPreview()
    }

    fun setPreserveLuminance(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (stateFlow.value.preserveLuminance == clamped) return
        stateFlow.update {
            it.copy(preserveLuminance = clamped, isGradedImageSaved = false)
        }
        triggerDebouncedPreview()
    }

    fun setSkinProtection(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (stateFlow.value.skinProtection == clamped) return
        stateFlow.update {
            it.copy(skinProtection = clamped, isGradedImageSaved = false)
        }
        triggerDebouncedPreview()
    }

    fun setSmoothSigma(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (stateFlow.value.smoothSigma == clamped) return
        stateFlow.update {
            it.copy(smoothSigma = clamped, isGradedImageSaved = false)
        }
        triggerDebouncedPreview()
    }

    fun setNeutralLock(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (stateFlow.value.neutralLock == clamped) return
        stateFlow.update {
            it.copy(neutralLock = clamped, isGradedImageSaved = false)
        }
        triggerDebouncedPreview()
    }

    fun setLutSize(size: LutSize) {
        stateFlow.update { it.copy(lutSize = size) }
    }

    fun triggerDebouncedPreview() {
        val current = stateFlow.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return
        scope.launch {
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

    private suspend fun computePythonPreview(req: PreviewDebounceRequest) {
        activeGradingJob?.cancel()
        activeGradingJob = scope.launch {
            stateFlow.update { it.copy(isProcessingPreview = true, errorMessage = null) }
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
                    stateFlow.update {
                        it.copy(
                            isProcessingPreview = false,
                            previewPath = result.previewPath,
                            basePreviewBitmap = rawTransferredBmp,
                            previewExecutionTimeMs = result.executionTimeMs,
                            isGradedImageSaved = false
                        )
                    }
                }
                is PreviewResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = result.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun runGradingFunnel() {
        val current = stateFlow.value
        val srcPath = current.sourcePath ?: return
        val refPath = current.effectiveReferencePath ?: return

        activeGradingJob?.cancel()
        activeGradingJob = scope.launch {
            stateFlow.update {
                it.copy(
                    wizardStep = AppConstants.STEP_4_COMPUTE,
                    computeStageIndex = 1,
                    isProcessingPreview = true,
                    errorMessage = null
                )
            }

            delay(AppConstants.COMPUTE_STAGE_DELAY_MS)
            stateFlow.update { it.copy(computeStageIndex = 2) }

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

            delay(AppConstants.COMPUTE_STAGE_DELAY_MS)
            stateFlow.update { it.copy(computeStageIndex = 3) }

            when (result) {
                is PreviewResult.Success -> {
                    val rawTransferredBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1080)
                    }

                    stateFlow.update {
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
                    stateFlow.update {
                        it.copy(
                            isProcessingPreview = false,
                            errorMessage = result.error.uiText,
                            wizardStep = AppConstants.STEP_3_PARAMS
                        )
                    }
                }
            }
        }
    }

    fun applyAutoEdit() {
        val current = stateFlow.value
        val srcPath = current.sourcePath
        if (srcPath.isNullOrBlank()) {
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_select_source_first)) }
            return
        }

        activeGradingJob?.cancel()
        activeGradingJob = scope.launch {
            stateFlow.update {
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

                    stateFlow.update {
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
                            userMessage = UiText.Resource(
                                R.string.msg_auto_edit_applied,
                                listOf(result.exposure, 68.0f, 52.0f)
                            )
                        )
                    }
                }
                is AutoEditResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isComputingAutoEdit = false,
                            isProcessingPreview = false,
                            errorMessage = result.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun toggleEditPanel() {
        stateFlow.update { it.copy(isEditPanelVisible = !it.isEditPanelVisible) }
    }

    fun setEditExposure(value: Float) {
        val clamped = value.coerceIn(-2.0f, 3.0f)
        if (stateFlow.value.editExposure == clamped) return
        stateFlow.update { it.copy(editExposure = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditContrast(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editContrast == clamped) return
        stateFlow.update { it.copy(editContrast = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditHighlights(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editHighlights == clamped) return
        stateFlow.update { it.copy(editHighlights = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditShadows(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editShadows == clamped) return
        stateFlow.update { it.copy(editShadows = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditWhites(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editWhites == clamped) return
        stateFlow.update { it.copy(editWhites = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditBlacks(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editBlacks == clamped) return
        stateFlow.update { it.copy(editBlacks = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditVibrance(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editVibrance == clamped) return
        stateFlow.update { it.copy(editVibrance = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    fun setEditSaturation(value: Float) {
        val clamped = value.coerceIn(-100f, 100f)
        if (stateFlow.value.editSaturation == clamped) return
        stateFlow.update { it.copy(editSaturation = clamped, isGradedImageSaved = false) }
        triggerDebouncedEdit()
    }

    private fun triggerDebouncedEdit() {
        val current = stateFlow.value
        val imagePath = current.previewPath ?: current.sourcePath ?: return
        scope.launch {
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

    fun resetEdits() {
        activeEditJob?.cancel()
        stateFlow.update {
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
            stateFlow.update {
                it.copy(
                    isComputingEdit = false,
                    editedPreviewBitmap = null,
                    editedPreviewPath = null
                )
            }
            return
        }

        activeEditJob?.cancel()
        activeEditJob = scope.launch {
            stateFlow.update { it.copy(isComputingEdit = true, errorMessage = null) }

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
                    stateFlow.update {
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
                    stateFlow.update {
                        it.copy(
                            isComputingEdit = false,
                            errorMessage = result.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun applyAutoInEdit(isStandalone: Boolean = false, onTriggerStandalonePreview: (() -> Unit)? = null) {
        val current = stateFlow.value
        val imagePath = if (isStandalone) {
            current.editorPreviewPath ?: current.editorSourcePath ?: return
        } else {
            current.previewPath ?: current.sourcePath ?: return
        }

        activeEditJob?.cancel()
        activeEditJob = scope.launch {
            stateFlow.update { it.copy(isComputingEdit = true, errorMessage = null) }
            val autoSettings = bridgeRepository.computeAutoSettings(imagePath)
            when (autoSettings) {
                is AutoSettingsResult.Success -> {
                    val expNorm = autoSettings.exposure.coerceIn(-2.0f, 2.0f)
                    val contrastNorm = (autoSettings.contrast / 100.0f).coerceIn(-1.0f, 1.0f)
                    val highlightsNorm = (autoSettings.highlights / 100.0f).coerceIn(-1.0f, 1.0f)
                    val shadowsNorm = (autoSettings.shadows / 100.0f).coerceIn(-1.0f, 1.0f)
                    val vibranceNorm = (autoSettings.vibrance / 100.0f).coerceIn(-1.0f, 1.0f)

                    stateFlow.update {
                        it.copy(
                            isComputingEdit = false,
                            isAutoEditActive = true,
                            editExposure = autoSettings.exposure,
                            editContrast = autoSettings.contrast,
                            editHighlights = autoSettings.highlights,
                            editShadows = autoSettings.shadows,
                            editWhites = autoSettings.whites,
                            editBlacks = autoSettings.blacks,
                            editVibrance = autoSettings.vibrance,
                            editSaturation = autoSettings.saturation,
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

                    if (isStandalone) {
                        onTriggerStandalonePreview?.invoke()
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

    fun saveGradedImageToGallery(onSavedImageRecorded: () -> Unit) {
        val current = stateFlow.value
        if (current.isExportingImage) return
        val srcPath = current.sourcePath ?: return

        if (current.isEditActive) {
            val previewPath = current.effectivePreviewPath ?: current.previewPath
            if (previewPath == null || !File(previewPath).exists()) {
                stateFlow.update { it.copy(isExportingImage = false, errorMessage = UiText.Resource(R.string.err_image_load_failed, listOf("Preview not found"))) }
                return
            }
            scope.launch {
                stateFlow.update { it.copy(isExportingImage = true, errorMessage = null) }
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
                        onSavedImageRecorded()
                        stateFlow.update {
                            it.copy(
                                isExportingImage = false,
                                exportedImagePath = previewPath,
                                isGradedImageSaved = true,
                                userMessage = UiText.Resource(R.string.msg_photo_saved)
                            )
                        }
                    },
                    onFailure = { error ->
                        stateFlow.update {
                            it.copy(
                                isExportingImage = false,
                                errorMessage = UiText.Resource(R.string.err_mediastore, listOf(error.localizedMessage ?: ""))
                            )
                        }
                    }
                )
            }
            return
        }

        val refPath = current.effectiveReferencePath ?: return

        scope.launch {
            stateFlow.update { it.copy(isExportingImage = true, errorMessage = null) }

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
                                themeName = current.selectedTheme?.name ?: "Look Personnalisé",
                                method = current.method.name,
                                intensity = current.intensity,
                                referenceImagePath = refPath,
                                paletteHexes = current.customReferencePalette
                            )
                            onSavedImageRecorded()
                            stateFlow.update {
                                it.copy(
                                    isExportingImage = false,
                                    exportedImagePath = result.imagePath,
                                    isGradedImageSaved = true,
                                    userMessage = UiText.Resource(R.string.msg_photo_hd_saved)
                                )
                            }
                        },
                        onFailure = { error ->
                            stateFlow.update {
                                it.copy(
                                    isExportingImage = false,
                                    errorMessage = UiText.Resource(R.string.err_mediastore, listOf(error.localizedMessage ?: ""))
                                )
                            }
                        }
                    )
                }
                is ImageExportResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isExportingImage = false,
                            errorMessage = result.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun exportCube(onReadyToSave: (String) -> Unit) {
        val current = stateFlow.value
        val src = current.sourcePath
        val ref = current.effectiveReferencePath

        if (src == null || ref == null) {
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_select_source_and_ref)) }
            return
        }

        scope.launch {
            stateFlow.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = src,
                refPath = ref,
                method = current.method,
                lutSize = current.lutSize,
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
                    stateFlow.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath
                        )
                    }
                    onReadyToSave(result.cubePath)
                }
                is CubeExportResult.Error -> {
                    stateFlow.update {
                        it.copy(
                            isExportingCube = false,
                            errorMessage = result.error.uiText
                        )
                    }
                }
            }
        }
    }

    fun saveExportedCubeToUri(destinationUri: Uri) {
        val sourcePath = pendingExportCubePath ?: stateFlow.value.exportedCubePath ?: return
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_lut_path_not_found)) }
            return
        }

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                        sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
                stateFlow.update {
                    it.copy(userMessage = UiText.Resource(R.string.msg_lut_cube_saved))
                }
            } catch (e: Exception) {
                stateFlow.update {
                    it.copy(errorMessage = UiText.Resource(R.string.err_save_failed, listOf(e.localizedMessage ?: "")))
                }
            }
        }
    }

    fun createShareCubeIntent(cubeFilePath: String? = null): Intent? {
        val path = cubeFilePath ?: stateFlow.value.exportedCubePath ?: pendingExportCubePath ?: return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "LUT 3D StealZaLUT (.cube)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getShareIntent(filePath: String): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {
            null
        }
    }
}
