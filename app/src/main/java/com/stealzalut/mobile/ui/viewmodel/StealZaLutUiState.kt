package com.stealzalut.mobile.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.UiText
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.data.model.CalibrationModel
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.data.model.ColorMixerModel
import com.stealzalut.mobile.data.model.EffectsModel
import com.stealzalut.mobile.data.model.ToneCurveModel
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.EditorParamsSnapshot
import com.stealzalut.mobile.ui.preview.GpuPreviewDeltas
import com.stealzalut.mobile.data.model.EditorProject
import com.stealzalut.mobile.data.model.LightroomDevelopModel
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.ui.navigation.AppDestination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
enum class ExportType {
    CUBE_LUT,
    HD_IMAGE,
    PRESET_JSON
}

@Immutable
data class PendingExport(
    val sourcePath: String,
    val suggestedFileName: String,
    val exportType: ExportType
)

/**
 * Global UI state for StealZaLUT Mobile, structured around 3 functional pillars:
 * 1. STUDIO   : Color grading funnel (Source + Reference -> 120 FPS GPU Preview -> Export .cube / Gallery Image)
 * 2. PALETTES : Palette extraction and saved presets library
 * 3. SETTINGS : Application preferences, algorithm defaults, and cache maintenance
 */
@Immutable
data class StealZaLutUiState(
    val currentDestination: AppDestination = AppDestination.STUDIO,
    val previousDestination: AppDestination? = null,

    // ========================================================================
    // 1. STUDIO DE GRADATION (WIZARD FUNNEL)
    // ========================================================================
    val wizardStep: Int = AppConstants.STEP_1_SOURCE,
    val computeStageIndex: Int = 1,
    val isSavingThemeDialogVisible: Boolean = false,
    val styleBSourceMode: Int = 0, // 0 = Custom Photo B, 1 = Saved Preset

    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val sourcePath: String? = null,

    val selectedTheme: ColorimetryPreset? = null,
    val customReferenceUri: Uri? = null,
    val customReferenceBitmap: Bitmap? = null,
    val customReferencePath: String? = null,
    val customReferencePalette: ImmutableList<String> = persistentListOf(),

    val method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val intensity: Float = AppConstants.INTENSITY_DEFAULT,
    val preserveLuminance: Float = AppConstants.PRESERVE_LUMINANCE_DEFAULT,
    val skinProtection: Float = AppConstants.SKIN_PROTECTION_DEFAULT,
    val smoothSigma: Float = AppConstants.SMOOTH_SIGMA_DEFAULT,
    val neutralLock: Float = AppConstants.NEUTRAL_LOCK_DEFAULT,
    val lutSize: LutSize = LutSize.STANDARD,

    val isProcessingPreview: Boolean = false,
    val isExportingCube: Boolean = false,
    val isExportingImage: Boolean = false,
    val isSavingPreset: Boolean = false,

    // Editor Auto Edit state
    val isAutoEditActive: Boolean = false,
    val isComputingEdit: Boolean = false,
    val editExposure: Float = 0.0f,
    val editContrast: Float = 0.0f,
    val editHighlights: Float = 0.0f,
    val editShadows: Float = 0.0f,
    val editWhites: Float = 0.0f,
    val editBlacks: Float = 0.0f,
    val editVibrance: Float = 0.0f,
    val editSaturation: Float = 0.0f,
    val editedPreviewBitmap: Bitmap? = null,
    val editedPreviewPath: String? = null,

    // Raw transferred preview computed at alpha = 1.0 by Python
    val basePreviewBitmap: Bitmap? = null,
    val previewPath: String? = null,
    val previewExecutionTimeMs: Double? = null,

    val exportedCubePath: String? = null,
    val exportedImagePath: String? = null,
    val isGradedImageSaved: Boolean = false,
    val pendingExport: PendingExport? = null,

    // Split Wipe slider position in [0.0, 1.0]
    val splitPosition: Float = AppConstants.SPLIT_WIPE_DEFAULT_FRACTION,
    val isSplitWipeMode: Boolean = false,

    // ========================================================================
    // NOUVEAU PIPELINE 3 ÉTAPES & RETRAVAIL AVANCÉ
    // ========================================================================
    val step2Intensity: Float = 1.0f,
    val step2LumaLock: Float = 0.0f,
    val step2NeutralAxisLock: Float = 0.0f,
    val step2SkinToneLock: Float = 0.0f,
    val step2ExtremesRolloff: Float = 0.0f,
    val step2Spatial3d: Float = 0.0f,

    val step3Exposure: Float = 0.0f,
    val step3Contrast: Float = 0.0f,
    val step3Highlights: Float = 0.0f,
    val step3Shadows: Float = 0.0f,
    val step3Temperature: Float = 0.0f,
    val step3Tint: Float = 0.0f,
    val step3Vibrance: Float = 0.0f,
    val step3Saturation: Float = 0.0f,
    val step3Clarity: Float = 0.0f,
    val step3Sharpen: Float = 0.0f,
    val step3Grain: Float = 0.0f,

    // ========================================================================
    // MODE ÉDITEUR PUR (STANDALONE PHOTO STUDIO)
    // ========================================================================
    val editorSourceUri: Uri? = null,
    val editorSourceBitmap: Bitmap? = null,
    val editorSourcePath: String? = null,
    val editorPreviewBitmap: Bitmap? = null,
    val editorPreviewPath: String? = null,
    val editorCustomLutPath: String? = null,
    val editorCustomLutName: String? = null,
    val isEditorProcessing: Boolean = false,
    val isEditorExportingCube: Boolean = false,
    val isEditorExportingImage: Boolean = false,
    val exportProgress: Float = 0f,
    val isExportDialogOpen: Boolean = false,
    val editorExecutionTimeMs: Double? = null,
    val editorSelectedTab: Int = 0, // 0 = Presets & LUTs, 1 = Calibrage, 2 = Lumière, 3 = Couleur, 4 = Effets
    val savedEditingPresets: ImmutableList<EditPreset> = persistentListOf(),
    val selectedEditingPreset: EditPreset? = null,
    val isSaveEditingPresetDialogVisible: Boolean = false,
    val isEditorPresetsSheetVisible: Boolean = false,

    // Adobe Lightroom Classic Develop Engine state
    val editorLightroomParams: LightroomDevelopModel = LightroomDevelopModel(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val editorCropRegion: CropRegion = CropRegion(),
    val editorProjects: ImmutableList<EditorProject> = persistentListOf(),
    val currentProjectId: String? = null,
    val editorCommittedSnapshot: EditorParamsSnapshot = EditorParamsSnapshot(),
    val editorActiveTool: Int = 0, // 0 = Global Panels, 1 = Crop, 2 = Healing, 3 = Red Eye, 4 = Masking, 5 = Presets / Auto-edit
    val isHistogramExpanded: Boolean = true,
    val isBasicExpanded: Boolean = true,
    val isToneCurveExpanded: Boolean = false,
    val isColorMixerExpanded: Boolean = false,
    val isColorGradingExpanded: Boolean = false,
    val isDetailExpanded: Boolean = false,
    val isLensCorrectionsExpanded: Boolean = false,
    val isTransformExpanded: Boolean = false,
    val isLensBlurExpanded: Boolean = false,
    val isEffectsExpanded: Boolean = false,
    val isCalibrationExpanded: Boolean = false,

    // ========================================================================
    // 2. PALETTES & PRESETS
    // ========================================================================
    val palettePhotoUri: Uri? = null,
    val palettePhotoBitmap: Bitmap? = null,
    val palettePhotoPath: String? = null,
    val isExtractingPalette: Boolean = false,
    val extractedPalette: ImmutableList<String> = persistentListOf(),
    val savedPresets: ImmutableList<ColorimetryPreset> = persistentListOf(),

    // ========================================================================
    // 3. PROFIL & BIBLIOTHÈQUE
    // ========================================================================
    val savedImages: ImmutableList<SavedGradedImage> = persistentListOf(),
    val profileSelectedTab: Int = 0, // 0 = Photos, 1 = Thèmes / Presets
    val selectedDetailImage: SavedGradedImage? = null,

    // ========================================================================
    // 4. SETTINGS & CACHE
    // ========================================================================
    val settings: AppSettings = AppSettings(),
    val cacheSizeMb: Double = 0.0,
    val currentLanguageTag: String = "en",

    // Feedback
    val userMessage: UiText? = null,
    val errorMessage: UiText? = null
) {
    val effectiveReferencePath: String?
        get() = if (styleBSourceMode == 0) {
            selectedTheme?.referenceImagePath ?: customReferencePath
        } else {
            customReferencePath ?: selectedTheme?.referenceImagePath
        }

    val canProceedToStep2: Boolean
        get() = sourcePath != null

    val canProceedToStep3: Boolean
        get() = if (styleBSourceMode == 0) selectedTheme != null else (customReferenceBitmap != null || customReferencePath != null)

    val canApply: Boolean
        get() = sourcePath != null && effectiveReferencePath != null &&
                !isProcessingPreview && !isExportingCube && !isExportingImage && !isSavingPreset

    val isEditActive: Boolean
        get() = isAutoEditActive || editExposure != 0f || editContrast != 0f || editHighlights != 0f ||
                editShadows != 0f || editWhites != 0f || editBlacks != 0f || editVibrance != 0f || editSaturation != 0f

    val effectivePreviewBitmap: Bitmap?
        get() = if (isEditActive && editedPreviewBitmap != null) editedPreviewBitmap else basePreviewBitmap

    val editorGpuDeltas: GpuPreviewDeltas
        get() {
            val currLr = editorLightroomParams
            val commLr = editorCommittedSnapshot.lightroomParams

            val currBasic = if (currLr.isBasicEnabled) currLr.basic else BasicAdjustmentsModel()
            val commBasic = if (commLr.isBasicEnabled) commLr.basic else BasicAdjustmentsModel()

            val currTone = if (currLr.isToneCurveEnabled) currLr.toneCurve else ToneCurveModel()
            val commTone = if (commLr.isToneCurveEnabled) commLr.toneCurve else ToneCurveModel()

            val currGrading = if (currLr.isColorGradingEnabled) currLr.colorGrading else ColorGradingModel()
            val commGrading = if (commLr.isColorGradingEnabled) commLr.colorGrading else ColorGradingModel()

            val currCalib = if (currLr.isCalibrationEnabled) currLr.calibration else CalibrationModel()
            val commCalib = if (commLr.isCalibrationEnabled) commLr.calibration else CalibrationModel()

            val currMixer = if (currLr.isColorMixerEnabled) currLr.colorMixer else ColorMixerModel()
            val commMixer = if (commLr.isColorMixerEnabled) commLr.colorMixer else ColorMixerModel()

            val currEffects = if (currLr.isEffectsEnabled) currLr.effects else EffectsModel()
            val commEffects = if (commLr.isEffectsEnabled) commLr.effects else EffectsModel()

            val mixerHues = floatArrayOf(
                currMixer.hueRed - commMixer.hueRed,
                currMixer.hueOrange - commMixer.hueOrange,
                currMixer.hueYellow - commMixer.hueYellow,
                currMixer.hueGreen - commMixer.hueGreen,
                currMixer.hueAqua - commMixer.hueAqua,
                currMixer.hueBlue - commMixer.hueBlue,
                currMixer.huePurple - commMixer.huePurple,
                currMixer.hueMagenta - commMixer.hueMagenta
            )
            val mixerSats = floatArrayOf(
                currMixer.satRed - commMixer.satRed,
                currMixer.satOrange - commMixer.satOrange,
                currMixer.satYellow - commMixer.satYellow,
                currMixer.satGreen - commMixer.satGreen,
                currMixer.satAqua - commMixer.satAqua,
                currMixer.satBlue - commMixer.satBlue,
                currMixer.satPurple - commMixer.satPurple,
                currMixer.satMagenta - commMixer.satMagenta
            )
            val mixerLums = floatArrayOf(
                currMixer.lumRed - commMixer.lumRed,
                currMixer.lumOrange - commMixer.lumOrange,
                currMixer.lumYellow - commMixer.lumYellow,
                currMixer.lumGreen - commMixer.lumGreen,
                currMixer.lumAqua - commMixer.lumAqua,
                currMixer.lumBlue - commMixer.lumBlue,
                currMixer.lumPurple - commMixer.lumPurple,
                currMixer.lumMagenta - commMixer.lumMagenta
            )

            return GpuPreviewDeltas(
                exposure = (currBasic.exposure + step3Exposure) - (commBasic.exposure + editorCommittedSnapshot.exposure),
                contrast = (currBasic.contrast + step3Contrast) - (commBasic.contrast + editorCommittedSnapshot.contrast),
                temp = (currBasic.temp + step3Temperature) - (commBasic.temp + editorCommittedSnapshot.temperature),
                tint = (currBasic.tint + step3Tint) - (commBasic.tint + editorCommittedSnapshot.tint),
                highlights = (currBasic.highlights + step3Highlights) - (commBasic.highlights + editorCommittedSnapshot.highlights),
                shadows = (currBasic.shadows + step3Shadows) - (commBasic.shadows + editorCommittedSnapshot.shadows),
                whites = currBasic.whites - commBasic.whites,
                blacks = currBasic.blacks - commBasic.blacks,
                vibrance = (currBasic.vibrance + step3Vibrance) - (commBasic.vibrance + editorCommittedSnapshot.vibrance),
                saturation = (currBasic.saturation + step3Saturation) - (commBasic.saturation + editorCommittedSnapshot.saturation),

                curveHighlights = currTone.highlights - commTone.highlights,
                curveLights = currTone.lights - commTone.lights,
                curveDarks = currTone.darks - commTone.darks,
                curveShadows = currTone.shadows - commTone.shadows,

                gradeShadowHue = currGrading.shadowHue,
                gradeShadowSat = currGrading.shadowSat,
                gradeShadowLum = currGrading.shadowLum - commGrading.shadowLum,
                commGradeShadowSat = commGrading.shadowSat,
                commGradeShadowHue = commGrading.shadowHue,

                gradeMidtoneHue = currGrading.midtoneHue,
                gradeMidtoneSat = currGrading.midtoneSat,
                gradeMidtoneLum = currGrading.midtoneLum - commGrading.midtoneLum,
                commGradeMidtoneSat = commGrading.midtoneSat,
                commGradeMidtoneHue = commGrading.midtoneHue,

                gradeHighlightHue = currGrading.highlightHue,
                gradeHighlightSat = currGrading.highlightSat,
                gradeHighlightLum = currGrading.highlightLum - commGrading.highlightLum,
                commGradeHighlightSat = commGrading.highlightSat,
                commGradeHighlightHue = commGrading.highlightHue,

                gradeGlobalHue = currGrading.globalHue,
                gradeGlobalSat = currGrading.globalSat,
                gradeGlobalLum = currGrading.globalLum - commGrading.globalLum,
                commGradeGlobalSat = commGrading.globalSat,
                commGradeGlobalHue = commGrading.globalHue,

                gradeBlending = currGrading.blending,
                gradeBalance = currGrading.balance - commGrading.balance,

                calibShadowTint = currCalib.shadowTint - commCalib.shadowTint,
                calibRedHue = currCalib.redHue - commCalib.redHue,
                calibRedSat = currCalib.redSat - commCalib.redSat,
                calibGreenHue = currCalib.greenHue - commCalib.greenHue,
                calibGreenSat = currCalib.greenSat - commCalib.greenSat,
                calibBlueHue = currCalib.blueHue - commCalib.blueHue,
                calibBlueSat = currCalib.blueSat - commCalib.blueSat,

                mixerHues = mixerHues,
                mixerSats = mixerSats,
                mixerLums = mixerLums,

                vignette = currEffects.vignetteAmount - commEffects.vignetteAmount
            )
        }


}
