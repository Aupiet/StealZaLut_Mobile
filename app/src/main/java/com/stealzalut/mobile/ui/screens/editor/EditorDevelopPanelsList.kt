package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlAccordionSection
import com.stealzalut.mobile.ui.viewmodel.StealZaLutUiState
import com.stealzalut.mobile.ui.viewmodel.delegate.EditorLightroomDelegate

/**
 * Lightroom Classic 10 Develop Accordion Panels.
 * Rendered in strict Lightroom Classic sequence with independent Eye-toggle bypass,
 * Expand/Collapse state, and double-tap / reset actions.
 */
@Composable
fun EditorDevelopPanelsList(
    uiState: StealZaLutUiState,
    delegate: EditorLightroomDelegate,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = AppDimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        val lrParams = uiState.editorLightroomParams

        // 1. Basic Adjustments
        SzlAccordionSection(
            title = stringResource(R.string.section_basic),
            isExpanded = uiState.isBasicExpanded,
            isSectionEnabled = lrParams.isBasicEnabled,
            onToggleExpand = { delegate.toggleBasicExpanded() },
            onToggleBypass = { delegate.toggleBasicEnabled() },
            onResetSection = { delegate.resetBasic() }
        ) {
            EditorBasicPanel(
                basic = lrParams.basic,
                onUpdateBasic = { delegate.updateBasic(it) }
            )
        }

        // 2. Tone Curve
        SzlAccordionSection(
            title = stringResource(R.string.section_tone_curve),
            isExpanded = uiState.isToneCurveExpanded,
            isSectionEnabled = lrParams.isToneCurveEnabled,
            onToggleExpand = { delegate.toggleToneCurveExpanded() },
            onToggleBypass = { delegate.toggleToneCurveEnabled() },
            onResetSection = { delegate.resetToneCurve() }
        ) {
            EditorToneCurvePanel(
                toneCurve = lrParams.toneCurve,
                onUpdateToneCurve = { delegate.updateToneCurve(it) }
            )
        }

        // 3. Color Mixer / TSL
        SzlAccordionSection(
            title = stringResource(R.string.section_color_mixer),
            isExpanded = uiState.isColorMixerExpanded,
            isSectionEnabled = lrParams.isColorMixerEnabled,
            onToggleExpand = { delegate.toggleColorMixerExpanded() },
            onToggleBypass = { delegate.toggleColorMixerEnabled() },
            onResetSection = { delegate.resetColorMixer() }
        ) {
            EditorColorMixerPanel(
                colorMixer = lrParams.colorMixer,
                onUpdateColorMixer = { delegate.updateColorMixer(it) }
            )
        }

        // 4. Color Grading
        SzlAccordionSection(
            title = stringResource(R.string.section_color_grading),
            isExpanded = uiState.isColorGradingExpanded,
            isSectionEnabled = lrParams.isColorGradingEnabled,
            onToggleExpand = { delegate.toggleColorGradingExpanded() },
            onToggleBypass = { delegate.toggleColorGradingEnabled() },
            onResetSection = { delegate.resetColorGrading() }
        ) {
            EditorColorGradingPanel(
                colorGrading = lrParams.colorGrading,
                onUpdateColorGrading = { delegate.updateColorGrading(it) }
            )
        }

        // 5. Detail
        SzlAccordionSection(
            title = stringResource(R.string.section_detail),
            isExpanded = uiState.isDetailExpanded,
            isSectionEnabled = lrParams.isDetailEnabled,
            onToggleExpand = { delegate.toggleDetailExpanded() },
            onToggleBypass = { delegate.toggleDetailEnabled() },
            onResetSection = { delegate.resetDetail() }
        ) {
            EditorDetailPanel(
                detail = lrParams.detail,
                onUpdateDetail = { delegate.updateDetail(it) }
            )
        }

        // 6. Lens Corrections
        SzlAccordionSection(
            title = stringResource(R.string.section_lens_corrections),
            isExpanded = uiState.isLensCorrectionsExpanded,
            isSectionEnabled = lrParams.isLensCorrectionsEnabled,
            onToggleExpand = { delegate.toggleLensCorrectionsExpanded() },
            onToggleBypass = { delegate.toggleLensCorrectionsEnabled() },
            onResetSection = { delegate.resetLensCorrections() }
        ) {
            EditorLensCorrectionsPanel(
                lensCorrections = lrParams.lensCorrections,
                onUpdateLensCorrections = { delegate.updateLensCorrections(it) }
            )
        }

        // 7. Transform
        SzlAccordionSection(
            title = stringResource(R.string.section_transform),
            isExpanded = uiState.isTransformExpanded,
            isSectionEnabled = lrParams.isTransformEnabled,
            onToggleExpand = { delegate.toggleTransformExpanded() },
            onToggleBypass = { delegate.toggleTransformEnabled() },
            onResetSection = { delegate.resetTransform() }
        ) {
            EditorTransformPanel(
                transform = lrParams.transform,
                onUpdateTransform = { delegate.updateTransform(it) }
            )
        }

        // 8. Lens Blur
        SzlAccordionSection(
            title = stringResource(R.string.section_lens_blur),
            isExpanded = uiState.isLensBlurExpanded,
            isSectionEnabled = lrParams.isLensBlurEnabled,
            onToggleExpand = { delegate.toggleLensBlurExpanded() },
            onToggleBypass = { delegate.toggleLensBlurEnabled() },
            onResetSection = { delegate.resetLensBlur() }
        ) {
            EditorLensBlurPanel(
                lensBlur = lrParams.lensBlur,
                onUpdateLensBlur = { delegate.updateLensBlur(it) }
            )
        }

        // 9. Effects
        SzlAccordionSection(
            title = stringResource(R.string.section_effects),
            isExpanded = uiState.isEffectsExpanded,
            isSectionEnabled = lrParams.isEffectsEnabled,
            onToggleExpand = { delegate.toggleEffectsExpanded() },
            onToggleBypass = { delegate.toggleEffectsEnabled() },
            onResetSection = { delegate.resetEffects() }
        ) {
            EditorEffectsPanel(
                effects = lrParams.effects,
                onUpdateEffects = { delegate.updateEffects(it) }
            )
        }

        // 10. Calibration
        SzlAccordionSection(
            title = stringResource(R.string.section_calibration),
            isExpanded = uiState.isCalibrationExpanded,
            isSectionEnabled = lrParams.isCalibrationEnabled,
            onToggleExpand = { delegate.toggleCalibrationExpanded() },
            onToggleBypass = { delegate.toggleCalibrationEnabled() },
            onResetSection = { delegate.resetCalibration() }
        ) {
            EditorCalibrationPanel(
                calibration = lrParams.calibration,
                onUpdateCalibration = { delegate.updateCalibration(it) }
            )
        }
    }
}
