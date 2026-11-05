package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Step2Model(
    val globalIntensity: Float = 1.0f,
    val lumaLock: Float = 0.0f,
    val neutralAxisLock: Float = 0.0f,
    val skinToneLock: Float = 0.0f,
    val extremesRolloff: Float = 0.0f,
    val spatial3d: Float = 0.0f
)

@Immutable
data class Step3Model(
    val exposure: Float = 0.0f,
    val contrast: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val temperature: Float = 0.0f,
    val tint: Float = 0.0f,
    val vibrance: Float = 0.0f,
    val saturation: Float = 0.0f,
    val clarity: Float = 0.0f,
    val sharpen: Float = 0.0f,
    val grain: Float = 0.0f
)

@Immutable
data class BasicAdjustmentsModel(
    val temp: Float = 0.0f,
    val tint: Float = 0.0f,
    val exposure: Float = 0.0f,
    val contrast: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val whites: Float = 0.0f,
    val blacks: Float = 0.0f,
    val texture: Float = 0.0f,
    val clarity: Float = 0.0f,
    val dehaze: Float = 0.0f,
    val vibrance: Float = 0.0f,
    val saturation: Float = 0.0f
)

@Immutable
data class ToneCurveModel(
    val mode: String = "parametric",
    val highlights: Float = 0.0f,
    val lights: Float = 0.0f,
    val darks: Float = 0.0f,
    val shadows: Float = 0.0f,
    val shadowSplit: Float = 25.0f,
    val midtoneSplit: Float = 50.0f,
    val highlightSplit: Float = 75.0f,
    val rgbPoints: List<Pair<Float, Float>> = listOf(0.0f to 0.0f, 1.0f to 1.0f),
    val redPoints: List<Pair<Float, Float>> = listOf(0.0f to 0.0f, 1.0f to 1.0f),
    val greenPoints: List<Pair<Float, Float>> = listOf(0.0f to 0.0f, 1.0f to 1.0f),
    val bluePoints: List<Pair<Float, Float>> = listOf(0.0f to 0.0f, 1.0f to 1.0f)
)

@Immutable
data class ColorMixerModel(
    val mode: String = "hsl",
    val hueRed: Float = 0.0f,
    val hueOrange: Float = 0.0f,
    val hueYellow: Float = 0.0f,
    val hueGreen: Float = 0.0f,
    val hueAqua: Float = 0.0f,
    val hueBlue: Float = 0.0f,
    val huePurple: Float = 0.0f,
    val hueMagenta: Float = 0.0f,
    val satRed: Float = 0.0f,
    val satOrange: Float = 0.0f,
    val satYellow: Float = 0.0f,
    val satGreen: Float = 0.0f,
    val satAqua: Float = 0.0f,
    val satBlue: Float = 0.0f,
    val satPurple: Float = 0.0f,
    val satMagenta: Float = 0.0f,
    val lumRed: Float = 0.0f,
    val lumOrange: Float = 0.0f,
    val lumYellow: Float = 0.0f,
    val lumGreen: Float = 0.0f,
    val lumAqua: Float = 0.0f,
    val lumBlue: Float = 0.0f,
    val lumPurple: Float = 0.0f,
    val lumMagenta: Float = 0.0f,
    val pointActive: Boolean = false,
    val pointTargetR: Float = 0.5f,
    val pointTargetG: Float = 0.5f,
    val pointTargetB: Float = 0.5f,
    val pointHueShift: Float = 0.0f,
    val pointSatShift: Float = 0.0f,
    val pointLumShift: Float = 0.0f,
    val pointRange: Float = 50.0f
)

@Immutable
data class ColorGradingModel(
    val shadowHue: Float = 0.0f,
    val shadowSat: Float = 0.0f,
    val shadowLum: Float = 0.0f,
    val midtoneHue: Float = 0.0f,
    val midtoneSat: Float = 0.0f,
    val midtoneLum: Float = 0.0f,
    val highlightHue: Float = 0.0f,
    val highlightSat: Float = 0.0f,
    val highlightLum: Float = 0.0f,
    val globalHue: Float = 0.0f,
    val globalSat: Float = 0.0f,
    val globalLum: Float = 0.0f,
    val blending: Float = 50.0f,
    val balance: Float = 0.0f
)

@Immutable
data class DetailModel(
    val sharpenAmount: Float = 0.0f,
    val sharpenRadius: Float = 1.0f,
    val sharpenDetail: Float = 25.0f,
    val sharpenMasking: Float = 0.0f,
    val nrLuminance: Float = 0.0f,
    val nrLumDetail: Float = 50.0f,
    val nrColor: Float = 0.0f
)

@Immutable
data class LensCorrectionsModel(
    val distortion: Float = 0.0f,
    val removeCa: Boolean = false,
    val caRedCyan: Float = 0.0f,
    val caBlueYellow: Float = 0.0f,
    val vignetteAmount: Float = 0.0f,
    val vignetteMidpoint: Float = 50.0f
)

@Immutable
data class TransformModel(
    val vertical: Float = 0.0f,
    val horizontal: Float = 0.0f,
    val rotate: Float = 0.0f,
    val aspect: Float = 0.0f,
    val scale: Float = 100.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f
)

@Immutable
data class LensBlurModel(
    val enabled: Boolean = false,
    val blurAmount: Float = 0.0f,
    val bokehStyle: String = "circle",
    val bokehBoost: Float = 0.0f,
    val focalRange: Float = 50.0f
)

@Immutable
data class EffectsModel(
    val vignetteAmount: Float = 0.0f,
    val vignetteMidpoint: Float = 50.0f,
    val vignetteRoundness: Float = 0.0f,
    val vignetteFeather: Float = 50.0f,
    val vignetteHighlights: Float = 0.0f,
    val grainAmount: Float = 0.0f,
    val grainSize: Float = 25.0f,
    val grainRoughness: Float = 50.0f
)

@Immutable
data class CalibrationModel(
    val shadowTint: Float = 0.0f,
    val redHue: Float = 0.0f,
    val redSat: Float = 0.0f,
    val greenHue: Float = 0.0f,
    val greenSat: Float = 0.0f,
    val blueHue: Float = 0.0f,
    val blueSat: Float = 0.0f
)

@Immutable
data class LightroomDevelopModel(
    val isBasicEnabled: Boolean = true,
    val isToneCurveEnabled: Boolean = true,
    val isColorMixerEnabled: Boolean = true,
    val isColorGradingEnabled: Boolean = true,
    val isDetailEnabled: Boolean = true,
    val isLensCorrectionsEnabled: Boolean = true,
    val isTransformEnabled: Boolean = true,
    val isLensBlurEnabled: Boolean = true,
    val isEffectsEnabled: Boolean = true,
    val isCalibrationEnabled: Boolean = true,
    val basic: BasicAdjustmentsModel = BasicAdjustmentsModel(),
    val toneCurve: ToneCurveModel = ToneCurveModel(),
    val colorMixer: ColorMixerModel = ColorMixerModel(),
    val colorGrading: ColorGradingModel = ColorGradingModel(),
    val detail: DetailModel = DetailModel(),
    val lensCorrections: LensCorrectionsModel = LensCorrectionsModel(),
    val transform: TransformModel = TransformModel(),
    val lensBlur: LensBlurModel = LensBlurModel(),
    val effects: EffectsModel = EffectsModel(),
    val calibration: CalibrationModel = CalibrationModel()
)

@Immutable
data class EditPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailBase64: String? = null,
    val lutReference: String? = null,
    val step2Params: Step2Model = Step2Model(),
    val step3Params: Step3Model = Step3Model(),
    val lightroomParams: LightroomDevelopModel = LightroomDevelopModel(),
    val filePath: String? = null
)
