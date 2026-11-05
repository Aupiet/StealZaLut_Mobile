package com.stealzalut.mobile.ui.viewmodel.delegate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.ExportType
import com.stealzalut.mobile.ui.viewmodel.PendingExport
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
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
import java.io.IOException

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
class GradingPipelineDelegate(
    private val context: Context,
    private val bridgeRepository: PythonBridgeRepository,
    private val savedImageRepository: SavedImageRepository,
    private val stateFlow: MutableStateFlow<StealZaLutUiState>,
    private val scope: CoroutineScope,
    private val decodeSampledBitmap: (path: String, maxDim: Int) -> Bitmap?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val previewDebounceFlow = MutableSharedFlow<PreviewDebounceRequest>(replay = 1)
    private var activeGradingJob: Job? = null
    private var activeSourceJob: Job? = null
    private var activeCustomRefJob: Job? = null
    private var currentSourceFile: File? = null
    private var currentCustomRefFile: File? = null

    fun clearCaches() {
        activeGradingJob?.cancel()
        activeSourceJob?.cancel()
        activeCustomRefJob?.cancel()
        currentSourceFile?.let {
            bridgeRepository.lruCacheManager.unprotectFile(it.absolutePath)
            it.delete()
            currentSourceFile = null
        }
        currentCustomRefFile?.let {
            bridgeRepository.lruCacheManager.unprotectFile(it.absolutePath)
            it.delete()
            currentCustomRefFile = null
        }
        scope.launch {
            bridgeRepository.clearCaches()
        }
    }

    init {
        scope.launch {
            previewDebounceFlow
                .debounce(200L)
                .distinctUntilChanged()
                .collect { req ->
                    computePythonPreview(req)
                }
        }
    }

    fun setSourceImage(uri: Uri) {
        activeSourceJob?.cancel()
        activeGradingJob?.cancel()
        activeSourceJob = scope.launch {
            stateFlow.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val filename = "source_${java.util.UUID.randomUUID().toString().take(8)}.jpg"
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, filename)
            cacheResult.fold(
                onSuccess = { file ->
                    bridgeRepository.lruCacheManager.protectFile(file.absolutePath)
                    val oldFile = currentSourceFile
                    currentSourceFile = file
                    if (oldFile != null && oldFile.absolutePath != file.absolutePath) {
                        bridgeRepository.lruCacheManager.unprotectFile(oldFile.absolutePath)
                        oldFile.delete()
                    }

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
        activeCustomRefJob?.cancel()
        activeGradingJob?.cancel()
        activeCustomRefJob = scope.launch {
            stateFlow.update { it.copy(isProcessingPreview = true, errorMessage = null) }
            val filename = "custom_ref_${java.util.UUID.randomUUID().toString().take(8)}.jpg"
            val cacheResult = bridgeRepository.cacheImageFromUri(uri, filename)
            cacheResult.fold(
                onSuccess = { file ->
                    bridgeRepository.lruCacheManager.protectFile(file.absolutePath)
                    val oldFile = currentCustomRefFile
                    currentCustomRefFile = file
                    if (oldFile != null && oldFile.absolutePath != file.absolutePath) {
                        bridgeRepository.lruCacheManager.unprotectFile(oldFile.absolutePath)
                        oldFile.delete()
                    }

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

    fun saveGradedImageToGallery(onSavedImageRecorded: () -> Unit) {
        val current = stateFlow.value
        if (current.isExportingImage) return
        val srcPath = current.sourcePath ?: return
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
                exposure = 0f,
                contrast = 0f,
                highlights = 0f,
                shadows = 0f,
                whites = 0f,
                blacks = 0f,
                vibrance = 0f,
                saturation = 0f
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
        if (current.isExportingCube) return
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
                exposure = 0f,
                contrast = 0f,
                highlights = 0f,
                shadows = 0f,
                whites = 0f,
                blacks = 0f,
                vibrance = 0f,
                saturation = 0f
            )

            when (result) {
                is CubeExportResult.Success -> {
                    val fileName = File(result.cubePath).name
                    stateFlow.update {
                        it.copy(
                            isExportingCube = false,
                            exportedCubePath = result.cubePath,
                            pendingExport = PendingExport(
                                sourcePath = result.cubePath,
                                suggestedFileName = fileName,
                                exportType = ExportType.CUBE_LUT
                            )
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

    fun openInEditorFromStudio() {
        val current = stateFlow.value
        if (current.isExportingCube) return
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
                neutralLock = current.neutralLock
            )

            when (result) {
                is CubeExportResult.Success -> {
                    stateFlow.update {
                        it.copy(
                            isExportingCube = false,
                            editorSourceUri = current.sourceUri,
                            editorSourcePath = src,
                            editorSourceBitmap = current.sourceBitmap,
                            editorCustomLutPath = result.cubePath,
                            editorCustomLutName = "Studio Grade",
                            editorPreviewBitmap = current.effectivePreviewBitmap,
                            step2Intensity = 1.0f,
                            step2LumaLock = 0.0f,
                            step2NeutralAxisLock = 0.0f,
                            step2SkinToneLock = 0.0f,
                            step2ExtremesRolloff = 0.0f,
                            step2Spatial3d = 0.0f,
                            step3Exposure = 0f,
                            step3Contrast = 0f,
                            step3Highlights = 0f,
                            step3Shadows = 0f,
                            step3Temperature = 0f,
                            step3Tint = 0f,
                            step3Vibrance = 0f,
                            step3Saturation = 0f,
                            step3Clarity = 0f,
                            step3Sharpen = 0f,
                            step3Grain = 0f,
                            editorSelectedTab = 0,
                            currentDestination = AppDestination.EDITOR
                        )
                    }
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

    fun savePendingExportToUri(destinationUri: Uri): Job {
        val pending = stateFlow.value.pendingExport
        if (pending == null) {
            cleanupEmptyUri(destinationUri)
            stateFlow.update { it.copy(errorMessage = UiText.Resource(R.string.err_no_pending_export)) }
            return Job().apply { complete() }
        }

        val sourceFile = File(pending.sourcePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            cleanupEmptyUri(destinationUri)
            stateFlow.update {
                it.copy(
                    pendingExport = null,
                    errorMessage = UiText.Resource(R.string.err_lut_path_not_found)
                )
            }
            return Job().apply { complete() }
        }

        val expectedBytes = sourceFile.length()

        return scope.launch {
            try {
                val bytesCopied = withContext(ioDispatcher) {
                    val outStream = (try {
                        context.contentResolver.openOutputStream(destinationUri, "wt")
                    } catch (_: Exception) {
                        null
                    }) ?: context.contentResolver.openOutputStream(destinationUri)
                    ?: throw IOException("Failed to open output stream for URI: $destinationUri")
                    outStream.use { out ->
                        val copied = sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(out)
                        }
                        out.flush()
                        copied
                    }
                }

                if (bytesCopied != expectedBytes || bytesCopied <= 0L) {
                    cleanupEmptyUri(destinationUri)
                    stateFlow.update {
                        it.copy(
                            pendingExport = null,
                            errorMessage = UiText.Resource(R.string.err_export_io, listOf("Corrupted copy ($bytesCopied / $expectedBytes bytes)"))
                        )
                    }
                    return@launch
                }

                // Streaming post-write verification directly from destination document
                val verifyResult = verifyDestinationDocument(destinationUri, expectedBytes, pending.exportType)
                if (verifyResult.isFailure) {
                    cleanupEmptyUri(destinationUri)
                    val errorDetail = verifyResult.exceptionOrNull()?.localizedMessage ?: "Vérification document échouée"
                    stateFlow.update {
                        it.copy(
                            pendingExport = null,
                            errorMessage = UiText.Resource(R.string.err_export_io, listOf(errorDetail))
                        )
                    }
                    return@launch
                }

                stateFlow.update {
                    it.copy(
                        pendingExport = null,
                        userMessage = when (pending.exportType) {
                            ExportType.CUBE_LUT -> UiText.Resource(R.string.msg_lut_cube_saved)
                            ExportType.HD_IMAGE -> UiText.Resource(R.string.msg_image_saved)
                            ExportType.PRESET_JSON -> UiText.Resource(R.string.msg_preset_saved, listOf(pending.suggestedFileName))
                        }
                    )
                }
            } catch (e: Exception) {
                cleanupEmptyUri(destinationUri)
                stateFlow.update {
                    it.copy(
                        pendingExport = null,
                        errorMessage = UiText.Resource(R.string.err_save_failed, listOf(e.localizedMessage ?: ""))
                    )
                }
            }
        }
    }

    fun saveExportedCubeToUri(destinationUri: Uri): Job {
        return savePendingExportToUri(destinationUri)
    }

    fun cancelPendingExport() {
        stateFlow.update { it.copy(pendingExport = null) }
    }

    private fun cleanupEmptyUri(uri: Uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Ignored if document already removed or unsupported
        }
    }

    private suspend fun verifyDestinationDocument(
        uri: Uri,
        expectedBytes: Long,
        exportType: ExportType
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val inStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Impossible d'ouvrir le flux de lecture pour l'URI: $uri")

            inStream.use { stream ->
                if (exportType == ExportType.CUBE_LUT) {
                    val reader = stream.bufferedReader()
                    var lutSize: Int? = null
                    var dataCount = 0
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line!!.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("TITLE")) {
                            continue
                        }
                        if (trimmed.startsWith("LUT_3D_SIZE")) {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                lutSize = parts[1].toIntOrNull()
                            }
                            continue
                        }
                        if (trimmed.startsWith("DOMAIN_MIN") || trimmed.startsWith("DOMAIN_MAX")) {
                            continue
                        }
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size == 3 && parts[0].toFloatOrNull() != null && parts[1].toFloatOrNull() != null && parts[2].toFloatOrNull() != null) {
                            dataCount++
                        }
                    }

                    if (lutSize == null || lutSize <= 0) {
                        throw IOException("Entête LUT_3D_SIZE introuvable ou invalide dans le fichier exporté.")
                    }
                    val expectedTriplets = lutSize * lutSize * lutSize
                    if (dataCount != expectedTriplets) {
                        throw IOException("Fichier .cube incomplet : $dataCount triplets trouvés sur $expectedTriplets attendus.")
                    }
                } else {
                    var totalRead = 0L
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        totalRead += read
                    }
                    if (totalRead <= 0L || totalRead != expectedBytes) {
                        throw IOException("Vérification des octets échouée ($totalRead / $expectedBytes octets)")
                    }
                }
            }
        }
    }

    fun createShareCubeIntent(cubeFilePath: String? = null): Intent? {
        val path = cubeFilePath ?: stateFlow.value.exportedCubePath ?: stateFlow.value.pendingExport?.sourcePath ?: return null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null

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
