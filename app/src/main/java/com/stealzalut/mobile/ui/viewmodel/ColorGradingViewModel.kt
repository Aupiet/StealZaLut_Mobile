package com.stealzalut.mobile.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutDomainError
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SettingsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Immutable
data class ColorGradingUiState(
    val sourceUri: Uri? = null,
    val sourcePath: String? = null,
    val sourceBitmap: Bitmap? = null,
    val referenceUri: Uri? = null,
    val referencePath: String? = null,
    val referenceBitmap: Bitmap? = null,
    val selectedTheme: ColorimetryPreset? = null,
    val method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val intensity: Float = 1.0f,
    val lutSize: LutSize = LutSize.STANDARD,
    val isProcessingPreview: Boolean = false,
    val isExportingCube: Boolean = false,
    val isExportingImage: Boolean = false,
    val basePreviewBitmap: Bitmap? = null, // Raw transfer at alpha = 1.0
    val displayPreviewBitmap: Bitmap? = null, // Blended with intensity
    val previewPath: String? = null,
    val previewExecutionTimeMs: Double? = null,
    val exportedCubePath: String? = null,
    val exportedImagePath: String? = null,
    val availableThemes: ImmutableList<ColorimetryPreset> = persistentListOf(),
    val splitPosition: Float = 0.5f,
    val isSplitWipeMode: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val canProcess: Boolean
        get() = sourcePath != null && effectiveReferencePath != null &&
                !isProcessingPreview && !isExportingCube && !isExportingImage

    val effectiveReferencePath: String?
        get() = referencePath ?: selectedTheme?.referenceImagePath
}

sealed interface ColorGradingUiEffect {
    data class ShowSnackbar(val message: String) : ColorGradingUiEffect
    data class ShowError(val error: LutDomainError) : ColorGradingUiEffect
    data class ShareIntentReady(val intent: Intent) : ColorGradingUiEffect
    data class CubeReadyToSave(val filePath: String) : ColorGradingUiEffect
    data class ImageReadyToSave(val filePath: String) : ColorGradingUiEffect
}

private data class RecalculationParams(
    val srcPath: String,
    val refPath: String,
    val method: ColorTransferMethod
)

@OptIn(FlowPreview::class)
class ColorGradingViewModel(
    application: Application,
    private val bridgeRepository: PythonBridgeRepository = PythonBridgeRepository(application.applicationContext),
    private val presetRepository: PresetRepository = PresetRepository(application.applicationContext),
    private val settingsRepository: SettingsRepository = SettingsRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ColorGradingUiState())
    val uiState: StateFlow<ColorGradingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ColorGradingUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val recalculationFlow = MutableSharedFlow<RecalculationParams>(replay = 1)
    private var activeProcessingJob: Job? = null

    init {
        val initialSettings = settingsRepository.getSettings()
        _uiState.update {
            it.copy(
                method = initialSettings.defaultMethod,
                lutSize = initialSettings.defaultLutSize
            )
        }

        viewModelScope.launch {
            val themes = presetRepository.getAllPresets()
            _uiState.update {
                it.copy(
                    availableThemes = themes.toImmutableList(),
                    selectedTheme = themes.firstOrNull()
                )
            }
        }

        // Debounced Python recalculation flow for method & image switches (150ms)
        viewModelScope.launch {
            recalculationFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { params ->
                    executePythonTransfer(params.srcPath, params.refPath, params.method)
                }
        }
    }

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
                            basePreviewBitmap = null,
                            displayPreviewBitmap = null,
                            isProcessingPreview = false
                        )
                    }
                    triggerRecalculationIfPossible()
                },
                onFailure = { error ->
                    val domainError = LutDomainError.ExportIoError(error.localizedMessage ?: error.toString())
                    _uiState.update { it.copy(isProcessingPreview = false, errorMessage = domainError.userMessage) }
                    _effects.send(ColorGradingUiEffect.ShowError(domainError))
                }
            )
        }
    }

    fun selectTheme(theme: ColorimetryPreset) {
        _uiState.update {
            it.copy(
                selectedTheme = theme,
                referencePath = null,
                referenceBitmap = null,
                referenceUri = null,
                method = theme.method,
                intensity = theme.intensity,
                lutSize = theme.lutSize
            )
        }
        triggerRecalculationIfPossible()
    }

    fun setCustomReferenceImage(uri: Uri) {
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
                            referenceUri = uri,
                            referencePath = file.absolutePath,
                            referenceBitmap = thumbnail,
                            selectedTheme = null,
                            isProcessingPreview = false
                        )
                    }
                    triggerRecalculationIfPossible()
                },
                onFailure = { error ->
                    val domainError = LutDomainError.ExportIoError(error.localizedMessage ?: error.toString())
                    _uiState.update { it.copy(isProcessingPreview = false, errorMessage = domainError.userMessage) }
                    _effects.send(ColorGradingUiEffect.ShowError(domainError))
                }
            )
        }
    }

    fun setMethod(method: ColorTransferMethod) {
        if (_uiState.value.method == method) return
        _uiState.update { it.copy(method = method) }
        triggerRecalculationIfPossible()
    }

    /**
     * 120 FPS Real-Time Intensity Interpolation.
     * Purely updates UI state intensity (alpha) for GPU hardware composition.
     * Zero Python bridge calls, zero disk I/O, zero bitmap allocations.
     */
    fun setIntensity(newIntensity: Float) {
        val clamped = newIntensity.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(intensity = clamped)
        }
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

    fun triggerPreview() {
        val current = _uiState.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return
        viewModelScope.launch {
            executePythonTransfer(src, ref, current.method)
        }
    }

    private fun triggerRecalculationIfPossible() {
        val current = _uiState.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return
        viewModelScope.launch {
            recalculationFlow.emit(RecalculationParams(src, ref, current.method))
        }
    }

    /**
     * Calculates transfer at full intensity (alpha = 1.0) via Python once,
     * then performs immediate alpha blending in Kotlin memory.
     */
    private suspend fun executePythonTransfer(srcPath: String, refPath: String, method: ColorTransferMethod) {
        activeProcessingJob?.cancel()
        activeProcessingJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPreview = true, errorMessage = null) }

            // Python computes 100% transfer once
            val result = bridgeRepository.generatePreview(
                srcPath = srcPath,
                refPath = refPath,
                method = method,
                intensity = 1.0f
            )

            when (result) {
                is PreviewResult.Success -> {
                    val rawTransferredBmp = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(result.previewPath, 1024)
                    }
                    _uiState.update {
                        it.copy(
                            isProcessingPreview = false,
                            previewPath = result.previewPath,
                            basePreviewBitmap = rawTransferredBmp,
                            displayPreviewBitmap = rawTransferredBmp,
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
                    _effects.send(ColorGradingUiEffect.ShowError(result.error))
                }
            }
        }
    }

    fun exportCube(onReadyToSave: ((String) -> Unit)? = null) {
        val current = _uiState.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingCube = true, errorMessage = null) }
            val result = bridgeRepository.exportCube(
                srcPath = src,
                refPath = ref,
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
                    onReadyToSave?.invoke(result.cubePath)
                    _effects.send(ColorGradingUiEffect.CubeReadyToSave(result.cubePath))
                    _effects.send(ColorGradingUiEffect.ShowSnackbar("LUT exportée : ${result.lutSize}³"))
                }
                is CubeExportResult.Error -> {
                    _uiState.update {
                        it.copy(isExportingCube = false, errorMessage = result.error.userMessage)
                    }
                    _effects.send(ColorGradingUiEffect.ShowError(result.error))
                }
            }
        }
    }

    fun exportGradedImage(onReadyToSave: ((String) -> Unit)? = null) {
        val current = _uiState.value
        val src = current.sourcePath ?: return
        val ref = current.effectiveReferencePath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExportingImage = true, errorMessage = null) }
            val settings = settingsRepository.getSettings()

            val result = bridgeRepository.exportGradedImage(
                srcPath = src,
                refPath = ref,
                method = current.method,
                intensity = current.intensity,
                maxExportDim = settings.maxExportDim,
                format = settings.exportFormat,
                quality = settings.exportQuality
            )

            when (result) {
                is ImageExportResult.Success -> {
                    val displayName = "StealZaLUT_${System.currentTimeMillis()}.${if (settings.exportFormat.equals("PNG", true)) "png" else "jpg"}"
                    bridgeRepository.saveImageToMediaStore(
                        imagePath = result.imagePath,
                        displayName = displayName,
                        mimeType = if (settings.exportFormat.equals("PNG", true)) "image/png" else "image/jpeg"
                    )

                    _uiState.update {
                        it.copy(
                            isExportingImage = false,
                            exportedImagePath = result.imagePath,
                            userMessage = "Image (${result.width}×${result.height}) enregistrée dans la Galerie !"
                        )
                    }
                    onReadyToSave?.invoke(result.imagePath)
                    _effects.send(ColorGradingUiEffect.ImageReadyToSave(result.imagePath))
                    _effects.send(ColorGradingUiEffect.ShowSnackbar("Image enregistrée dans Pictures/StealZaLUT"))
                }
                is ImageExportResult.Error -> {
                    _uiState.update {
                        it.copy(isExportingImage = false, errorMessage = result.error.userMessage)
                    }
                    _effects.send(ColorGradingUiEffect.ShowError(result.error))
                }
            }
        }
    }

    fun shareCube() {
        val cubePath = _uiState.value.exportedCubePath ?: return
        val intent = bridgeRepository.createShareIntent(cubePath, "application/octet-stream")
        viewModelScope.launch {
            _effects.send(ColorGradingUiEffect.ShareIntentReady(intent))
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
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (_: Exception) {
            null
        }
    }
}
