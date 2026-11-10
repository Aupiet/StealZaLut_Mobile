package com.stealzalut.mobile.ui.viewmodel.delegate

import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.data.model.CalibrationModel
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.data.model.ColorMixerModel
import com.stealzalut.mobile.data.model.DetailModel
import com.stealzalut.mobile.data.model.EffectsModel
import com.stealzalut.mobile.data.model.LensBlurModel
import com.stealzalut.mobile.data.model.LensCorrectionsModel
import com.stealzalut.mobile.data.model.ToneCurveModel
import com.stealzalut.mobile.data.model.TransformModel
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Specialized delegate handling Adobe Lightroom Classic develop panels state,
 * accordion collapse/expansion, bypass toggles, and slider updates.
 */
class EditorLightroomDelegate(
    private val stateFlow: MutableStateFlow<StealZaLutUiState>,
    private val onScheduleUpdate: () -> Unit,
    private val onBeginGesture: () -> Unit = {},
    private val onEndGesture: () -> Unit = {},
    private val onCommitUndo: () -> Unit = {}
) {

    // --- Toolbar & Viewport Controls ---
    fun setEditorActiveTool(tool: Int) {
        val previousTool = stateFlow.value.editorActiveTool
        stateFlow.update { it.copy(editorActiveTool = tool) }
        if (previousTool == 0 || tool == 0) {
            onScheduleUpdate()
        }
    }

    fun toggleHistogramExpanded() {
        stateFlow.update { it.copy(isHistogramExpanded = !it.isHistogramExpanded) }
    }

    // --- Section Expansion Toggles (Accordion) ---
    fun toggleBasicExpanded() = stateFlow.update { it.copy(isBasicExpanded = !it.isBasicExpanded) }
    fun toggleToneCurveExpanded() = stateFlow.update { it.copy(isToneCurveExpanded = !it.isToneCurveExpanded) }
    fun toggleColorMixerExpanded() = stateFlow.update { it.copy(isColorMixerExpanded = !it.isColorMixerExpanded) }
    fun toggleColorGradingExpanded() = stateFlow.update { it.copy(isColorGradingExpanded = !it.isColorGradingExpanded) }
    fun toggleDetailExpanded() = stateFlow.update { it.copy(isDetailExpanded = !it.isDetailExpanded) }
    fun toggleLensCorrectionsExpanded() = stateFlow.update { it.copy(isLensCorrectionsExpanded = !it.isLensCorrectionsExpanded) }
    fun toggleTransformExpanded() = stateFlow.update { it.copy(isTransformExpanded = !it.isTransformExpanded) }
    fun toggleLensBlurExpanded() = stateFlow.update { it.copy(isLensBlurExpanded = !it.isLensBlurExpanded) }
    fun toggleEffectsExpanded() = stateFlow.update { it.copy(isEffectsExpanded = !it.isEffectsExpanded) }
    fun toggleCalibrationExpanded() = stateFlow.update { it.copy(isCalibrationExpanded = !it.isCalibrationExpanded) }

    // --- Section Bypass Toggles (Eye Icon) ---
    fun toggleBasicEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isBasicEnabled = !curr.isBasicEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleToneCurveEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isToneCurveEnabled = !curr.isToneCurveEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleColorMixerEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isColorMixerEnabled = !curr.isColorMixerEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleColorGradingEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isColorGradingEnabled = !curr.isColorGradingEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleDetailEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isDetailEnabled = !curr.isDetailEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleLensCorrectionsEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isLensCorrectionsEnabled = !curr.isLensCorrectionsEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleTransformEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isTransformEnabled = !curr.isTransformEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleLensBlurEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isLensBlurEnabled = !curr.isLensBlurEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleEffectsEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isEffectsEnabled = !curr.isEffectsEnabled))
        }
        onScheduleUpdate()
    }

    fun toggleCalibrationEnabled() {
        onCommitUndo()
        stateFlow.update {
            val curr = it.editorLightroomParams
            it.copy(editorLightroomParams = curr.copy(isCalibrationEnabled = !curr.isCalibrationEnabled))
        }
        onScheduleUpdate()
    }

    // --- Parameter Mutators ---

    fun updateBasic(transform: (BasicAdjustmentsModel) -> BasicAdjustmentsModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            val updated = transform(p.basic)
            it.copy(
                editorLightroomParams = p.copy(basic = updated),
                step3Exposure = updated.exposure,
                step3Contrast = updated.contrast / 100.0f,
                step3Highlights = updated.highlights / 100.0f,
                step3Shadows = updated.shadows / 100.0f,
                step3Temperature = updated.temp / 100.0f,
                step3Tint = updated.tint / 100.0f,
                step3Vibrance = updated.vibrance / 100.0f,
                step3Saturation = updated.saturation / 100.0f
            )
        }
        onScheduleUpdate()
    }

    fun updateToneCurve(transform: (ToneCurveModel) -> ToneCurveModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(toneCurve = transform(p.toneCurve)))
        }
        onScheduleUpdate()
    }

    fun updateColorMixer(transform: (ColorMixerModel) -> ColorMixerModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(colorMixer = transform(p.colorMixer)))
        }
        onScheduleUpdate()
    }

    fun updateColorGrading(transform: (ColorGradingModel) -> ColorGradingModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(colorGrading = transform(p.colorGrading)))
        }
        onScheduleUpdate()
    }

    fun updateDetail(transform: (DetailModel) -> DetailModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            val updated = transform(p.detail)
            it.copy(
                editorLightroomParams = p.copy(detail = updated),
                step3Sharpen = updated.sharpenAmount / 150.0f
            )
        }
        onScheduleUpdate()
    }

    fun updateLensCorrections(transform: (LensCorrectionsModel) -> LensCorrectionsModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(lensCorrections = transform(p.lensCorrections)))
        }
        onScheduleUpdate()
    }

    fun updateTransform(transform: (TransformModel) -> TransformModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(transform = transform(p.transform)))
        }
        onScheduleUpdate()
    }

    fun updateLensBlur(transform: (LensBlurModel) -> LensBlurModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(lensBlur = transform(p.lensBlur)))
        }
        onScheduleUpdate()
    }

    fun updateEffects(transform: (EffectsModel) -> EffectsModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            val updated = transform(p.effects)
            it.copy(
                editorLightroomParams = p.copy(effects = updated),
                step3Grain = updated.grainAmount / 100.0f
            )
        }
        onScheduleUpdate()
    }

    fun updateCalibration(transform: (CalibrationModel) -> CalibrationModel) {
        onBeginGesture()
        stateFlow.update {
            val p = it.editorLightroomParams
            it.copy(editorLightroomParams = p.copy(calibration = transform(p.calibration)))
        }
        onScheduleUpdate()
    }

    // --- Reset Actions per Panel ---
    fun resetBasic() { onCommitUndo(); updateBasic { BasicAdjustmentsModel() }; onEndGesture() }
    fun resetToneCurve() { onCommitUndo(); updateToneCurve { ToneCurveModel() }; onEndGesture() }
    fun resetColorMixer() { onCommitUndo(); updateColorMixer { ColorMixerModel() }; onEndGesture() }
    fun resetColorGrading() { onCommitUndo(); updateColorGrading { ColorGradingModel() }; onEndGesture() }
    fun resetDetail() { onCommitUndo(); updateDetail { DetailModel() }; onEndGesture() }
    fun resetLensCorrections() { onCommitUndo(); updateLensCorrections { LensCorrectionsModel() }; onEndGesture() }
    fun resetTransform() { onCommitUndo(); updateTransform { TransformModel() }; onEndGesture() }
    fun resetLensBlur() { onCommitUndo(); updateLensBlur { LensBlurModel() }; onEndGesture() }
    fun resetEffects() { onCommitUndo(); updateEffects { EffectsModel() }; onEndGesture() }
    fun resetCalibration() { onCommitUndo(); updateCalibration { CalibrationModel() }; onEndGesture() }

    fun onGestureFinished() {
        onEndGesture()
    }
}
