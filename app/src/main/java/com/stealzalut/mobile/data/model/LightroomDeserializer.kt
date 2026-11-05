package com.stealzalut.mobile.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Deserialization helper for LightroomDevelopModel.
 */
object LightroomDeserializer {

    fun fromJson(jsonStr: String): LightroomDevelopModel {
        return try {
            val root = JSONObject(jsonStr)

            val basicObj = root.optJSONObject("basic")
            val basic = if (basicObj != null) {
                BasicAdjustmentsModel(
                    temp = basicObj.optDouble("temp", 0.0).toFloat(),
                    tint = basicObj.optDouble("tint", 0.0).toFloat(),
                    exposure = basicObj.optDouble("exposure", 0.0).toFloat(),
                    contrast = basicObj.optDouble("contrast", 0.0).toFloat(),
                    highlights = basicObj.optDouble("highlights", 0.0).toFloat(),
                    shadows = basicObj.optDouble("shadows", 0.0).toFloat(),
                    whites = basicObj.optDouble("whites", 0.0).toFloat(),
                    blacks = basicObj.optDouble("blacks", 0.0).toFloat(),
                    texture = basicObj.optDouble("texture", 0.0).toFloat(),
                    clarity = basicObj.optDouble("clarity", 0.0).toFloat(),
                    dehaze = basicObj.optDouble("dehaze", 0.0).toFloat(),
                    vibrance = basicObj.optDouble("vibrance", 0.0).toFloat(),
                    saturation = basicObj.optDouble("saturation", 0.0).toFloat()
                )
            } else BasicAdjustmentsModel()

            fun jsonToPoints(arr: JSONArray?): List<Pair<Float, Float>> {
                if (arr == null || arr.length() == 0) return listOf(0.0f to 0.0f, 1.0f to 1.0f)
                val list = mutableListOf<Pair<Float, Float>>()
                for (i in 0 until arr.length()) {
                    val pt = arr.optJSONArray(i)
                    if (pt != null && pt.length() >= 2) {
                        list.add(pt.optDouble(0, 0.0).toFloat() to pt.optDouble(1, 0.0).toFloat())
                    }
                }
                return if (list.isEmpty()) listOf(0.0f to 0.0f, 1.0f to 1.0f) else list
            }

            val tcObj = root.optJSONObject("tone_curve")
            val tc = if (tcObj != null) {
                ToneCurveModel(
                    mode = tcObj.optString("mode", "parametric"),
                    highlights = tcObj.optDouble("highlights", 0.0).toFloat(),
                    lights = tcObj.optDouble("lights", 0.0).toFloat(),
                    darks = tcObj.optDouble("darks", 0.0).toFloat(),
                    shadows = tcObj.optDouble("shadows", 0.0).toFloat(),
                    shadowSplit = tcObj.optDouble("shadow_split", 25.0).toFloat(),
                    midtoneSplit = tcObj.optDouble("midtone_split", 50.0).toFloat(),
                    highlightSplit = tcObj.optDouble("highlight_split", 75.0).toFloat(),
                    rgbPoints = jsonToPoints(tcObj.optJSONArray("rgb_points")),
                    redPoints = jsonToPoints(tcObj.optJSONArray("red_points")),
                    greenPoints = jsonToPoints(tcObj.optJSONArray("green_points")),
                    bluePoints = jsonToPoints(tcObj.optJSONArray("blue_points"))
                )
            } else ToneCurveModel()

            val cmObj = root.optJSONObject("color_mixer")
            val cm = if (cmObj != null) {
                ColorMixerModel(
                    mode = cmObj.optString("mode", "hsl"),
                    hueRed = cmObj.optDouble("hue_red", 0.0).toFloat(),
                    hueOrange = cmObj.optDouble("hue_orange", 0.0).toFloat(),
                    hueYellow = cmObj.optDouble("hue_yellow", 0.0).toFloat(),
                    hueGreen = cmObj.optDouble("hue_green", 0.0).toFloat(),
                    hueAqua = cmObj.optDouble("hue_aqua", 0.0).toFloat(),
                    hueBlue = cmObj.optDouble("hue_blue", 0.0).toFloat(),
                    huePurple = cmObj.optDouble("hue_purple", 0.0).toFloat(),
                    hueMagenta = cmObj.optDouble("hue_magenta", 0.0).toFloat(),
                    satRed = cmObj.optDouble("sat_red", 0.0).toFloat(),
                    satOrange = cmObj.optDouble("sat_orange", 0.0).toFloat(),
                    satYellow = cmObj.optDouble("sat_yellow", 0.0).toFloat(),
                    satGreen = cmObj.optDouble("sat_green", 0.0).toFloat(),
                    satAqua = cmObj.optDouble("sat_aqua", 0.0).toFloat(),
                    satBlue = cmObj.optDouble("sat_blue", 0.0).toFloat(),
                    satPurple = cmObj.optDouble("sat_purple", 0.0).toFloat(),
                    satMagenta = cmObj.optDouble("sat_magenta", 0.0).toFloat(),
                    lumRed = cmObj.optDouble("lum_red", 0.0).toFloat(),
                    lumOrange = cmObj.optDouble("lum_orange", 0.0).toFloat(),
                    lumYellow = cmObj.optDouble("lum_yellow", 0.0).toFloat(),
                    lumGreen = cmObj.optDouble("lum_green", 0.0).toFloat(),
                    lumAqua = cmObj.optDouble("lum_aqua", 0.0).toFloat(),
                    lumBlue = cmObj.optDouble("lum_blue", 0.0).toFloat(),
                    lumPurple = cmObj.optDouble("lum_purple", 0.0).toFloat(),
                    lumMagenta = cmObj.optDouble("lum_magenta", 0.0).toFloat(),
                    pointActive = cmObj.optBoolean("point_active", false),
                    pointTargetR = cmObj.optDouble("point_target_r", 0.5).toFloat(),
                    pointTargetG = cmObj.optDouble("point_target_g", 0.5).toFloat(),
                    pointTargetB = cmObj.optDouble("point_target_b", 0.5).toFloat(),
                    pointHueShift = cmObj.optDouble("point_hue_shift", 0.0).toFloat(),
                    pointSatShift = cmObj.optDouble("point_sat_shift", 0.0).toFloat(),
                    pointLumShift = cmObj.optDouble("point_lum_shift", 0.0).toFloat(),
                    pointRange = cmObj.optDouble("point_range", 50.0).toFloat()
                )
            } else ColorMixerModel()

            val cgObj = root.optJSONObject("color_grading")
            val cg = if (cgObj != null) {
                ColorGradingModel(
                    shadowHue = cgObj.optDouble("shadow_hue", 0.0).toFloat(),
                    shadowSat = cgObj.optDouble("shadow_sat", 0.0).toFloat(),
                    shadowLum = cgObj.optDouble("shadow_lum", 0.0).toFloat(),
                    midtoneHue = cgObj.optDouble("midtone_hue", 0.0).toFloat(),
                    midtoneSat = cgObj.optDouble("midtone_sat", 0.0).toFloat(),
                    midtoneLum = cgObj.optDouble("midtone_lum", 0.0).toFloat(),
                    highlightHue = cgObj.optDouble("highlight_hue", 0.0).toFloat(),
                    highlightSat = cgObj.optDouble("highlight_sat", 0.0).toFloat(),
                    highlightLum = cgObj.optDouble("highlight_lum", 0.0).toFloat(),
                    globalHue = cgObj.optDouble("global_hue", 0.0).toFloat(),
                    globalSat = cgObj.optDouble("global_sat", 0.0).toFloat(),
                    globalLum = cgObj.optDouble("global_lum", 0.0).toFloat(),
                    blending = cgObj.optDouble("blending", 50.0).toFloat(),
                    balance = cgObj.optDouble("balance", 0.0).toFloat()
                )
            } else ColorGradingModel()

            val detailObj = root.optJSONObject("detail")
            val detail = if (detailObj != null) {
                DetailModel(
                    sharpenAmount = detailObj.optDouble("sharpen_amount", 0.0).toFloat(),
                    sharpenRadius = detailObj.optDouble("sharpen_radius", 1.0).toFloat(),
                    sharpenDetail = detailObj.optDouble("sharpen_detail", 25.0).toFloat(),
                    sharpenMasking = detailObj.optDouble("sharpen_masking", 0.0).toFloat(),
                    nrLuminance = detailObj.optDouble("nr_luminance", 0.0).toFloat(),
                    nrLumDetail = detailObj.optDouble("nr_lum_detail", 50.0).toFloat(),
                    nrColor = detailObj.optDouble("nr_color", 0.0).toFloat()
                )
            } else DetailModel()

            val lcObj = root.optJSONObject("lens_corrections")
            val lc = if (lcObj != null) {
                LensCorrectionsModel(
                    distortion = lcObj.optDouble("distortion", 0.0).toFloat(),
                    removeCa = lcObj.optBoolean("remove_ca", false),
                    caRedCyan = lcObj.optDouble("ca_red_cyan", 0.0).toFloat(),
                    caBlueYellow = lcObj.optDouble("ca_blue_yellow", 0.0).toFloat(),
                    vignetteAmount = lcObj.optDouble("vignette_amount", 0.0).toFloat(),
                    vignetteMidpoint = lcObj.optDouble("vignette_midpoint", 50.0).toFloat()
                )
            } else LensCorrectionsModel()

            val tfObj = root.optJSONObject("transform")
            val tf = if (tfObj != null) {
                TransformModel(
                    vertical = tfObj.optDouble("vertical", 0.0).toFloat(),
                    horizontal = tfObj.optDouble("horizontal", 0.0).toFloat(),
                    rotate = tfObj.optDouble("rotate", 0.0).toFloat(),
                    aspect = tfObj.optDouble("aspect", 0.0).toFloat(),
                    scale = tfObj.optDouble("scale", 100.0).toFloat(),
                    offsetX = tfObj.optDouble("offset_x", 0.0).toFloat(),
                    offsetY = tfObj.optDouble("offset_y", 0.0).toFloat()
                )
            } else TransformModel()

            val lbObj = root.optJSONObject("lens_blur")
            val lb = if (lbObj != null) {
                LensBlurModel(
                    enabled = lbObj.optBoolean("enabled", false),
                    blurAmount = lbObj.optDouble("blur_amount", 0.0).toFloat(),
                    bokehStyle = lbObj.optString("bokeh_style", "circle"),
                    bokehBoost = lbObj.optDouble("bokeh_boost", 0.0).toFloat(),
                    focalRange = lbObj.optDouble("focal_range", 50.0).toFloat()
                )
            } else LensBlurModel()

            val fxObj = root.optJSONObject("effects")
            val fx = if (fxObj != null) {
                EffectsModel(
                    vignetteAmount = fxObj.optDouble("vignette_amount", 0.0).toFloat(),
                    vignetteMidpoint = fxObj.optDouble("vignette_midpoint", 50.0).toFloat(),
                    vignetteRoundness = fxObj.optDouble("vignette_roundness", 0.0).toFloat(),
                    vignetteFeather = fxObj.optDouble("vignette_feather", 50.0).toFloat(),
                    vignetteHighlights = fxObj.optDouble("vignette_highlights", 0.0).toFloat(),
                    grainAmount = fxObj.optDouble("grain_amount", 0.0).toFloat(),
                    grainSize = fxObj.optDouble("grain_size", 25.0).toFloat(),
                    grainRoughness = fxObj.optDouble("grain_roughness", 50.0).toFloat()
                )
            } else EffectsModel()

            val calibObj = root.optJSONObject("calibration")
            val calib = if (calibObj != null) {
                CalibrationModel(
                    shadowTint = calibObj.optDouble("shadow_tint", 0.0).toFloat(),
                    redHue = calibObj.optDouble("red_hue", 0.0).toFloat(),
                    redSat = calibObj.optDouble("red_sat", 0.0).toFloat(),
                    greenHue = calibObj.optDouble("green_hue", 0.0).toFloat(),
                    greenSat = calibObj.optDouble("green_sat", 0.0).toFloat(),
                    blueHue = calibObj.optDouble("blue_hue", 0.0).toFloat(),
                    blueSat = calibObj.optDouble("blue_sat", 0.0).toFloat()
                )
            } else CalibrationModel()

            LightroomDevelopModel(
                isBasicEnabled = root.optBoolean("is_basic_enabled", true),
                isToneCurveEnabled = root.optBoolean("is_tone_curve_enabled", true),
                isColorMixerEnabled = root.optBoolean("is_color_mixer_enabled", true),
                isColorGradingEnabled = root.optBoolean("is_color_grading_enabled", true),
                isDetailEnabled = root.optBoolean("is_detail_enabled", true),
                isLensCorrectionsEnabled = root.optBoolean("is_lens_corrections_enabled", true),
                isTransformEnabled = root.optBoolean("is_transform_enabled", true),
                isLensBlurEnabled = root.optBoolean("is_lens_blur_enabled", true),
                isEffectsEnabled = root.optBoolean("is_effects_enabled", true),
                isCalibrationEnabled = root.optBoolean("is_calibration_enabled", true),
                basic = basic,
                toneCurve = tc,
                colorMixer = cm,
                colorGrading = cg,
                detail = detail,
                lensCorrections = lc,
                transform = tf,
                lensBlur = lb,
                effects = fx,
                calibration = calib
            )
        } catch (_: Exception) {
            LightroomDevelopModel()
        }
    }
}
