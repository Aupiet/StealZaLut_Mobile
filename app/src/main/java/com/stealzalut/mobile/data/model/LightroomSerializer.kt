package com.stealzalut.mobile.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialization and Deserialization helpers for LightroomDevelopModel.
 */
object LightroomSerializer {

    fun toJson(model: LightroomDevelopModel, cropRegion: CropRegion? = null): String {
        val root = JSONObject()

        // Section bypass flags
        root.put("is_basic_enabled", model.isBasicEnabled)
        root.put("is_tone_curve_enabled", model.isToneCurveEnabled)
        root.put("is_color_mixer_enabled", model.isColorMixerEnabled)
        root.put("is_color_grading_enabled", model.isColorGradingEnabled)
        root.put("is_detail_enabled", model.isDetailEnabled)
        root.put("is_lens_corrections_enabled", model.isLensCorrectionsEnabled)
        root.put("is_transform_enabled", model.isTransformEnabled)
        root.put("is_lens_blur_enabled", model.isLensBlurEnabled)
        root.put("is_effects_enabled", model.isEffectsEnabled)
        root.put("is_calibration_enabled", model.isCalibrationEnabled)

        // 1. Basic
        val basic = JSONObject().apply {
            put("temp", model.basic.temp.toDouble())
            put("tint", model.basic.tint.toDouble())
            put("exposure", model.basic.exposure.toDouble())
            put("contrast", model.basic.contrast.toDouble())
            put("highlights", model.basic.highlights.toDouble())
            put("shadows", model.basic.shadows.toDouble())
            put("whites", model.basic.whites.toDouble())
            put("blacks", model.basic.blacks.toDouble())
            put("texture", model.basic.texture.toDouble())
            put("clarity", model.basic.clarity.toDouble())
            put("dehaze", model.basic.dehaze.toDouble())
            put("vibrance", model.basic.vibrance.toDouble())
            put("saturation", model.basic.saturation.toDouble())
        }
        root.put("basic", basic)

        // 2. Tone Curve
        val tc = JSONObject().apply {
            put("mode", model.toneCurve.mode)
            put("highlights", model.toneCurve.highlights.toDouble())
            put("lights", model.toneCurve.lights.toDouble())
            put("darks", model.toneCurve.darks.toDouble())
            put("shadows", model.toneCurve.shadows.toDouble())
            put("shadow_split", model.toneCurve.shadowSplit.toDouble())
            put("midtone_split", model.toneCurve.midtoneSplit.toDouble())
            put("highlight_split", model.toneCurve.highlightSplit.toDouble())

            fun pointsToJson(points: List<Pair<Float, Float>>): JSONArray {
                val arr = JSONArray()
                points.forEach { (x, y) ->
                    val ptArr = JSONArray().apply {
                        put(x.toDouble())
                        put(y.toDouble())
                    }
                    arr.put(ptArr)
                }
                return arr
            }
            put("rgb_points", pointsToJson(model.toneCurve.rgbPoints))
            put("red_points", pointsToJson(model.toneCurve.redPoints))
            put("green_points", pointsToJson(model.toneCurve.greenPoints))
            put("blue_points", pointsToJson(model.toneCurve.bluePoints))
        }
        root.put("tone_curve", tc)

        // 3. Color Mixer
        val cm = JSONObject().apply {
            put("mode", model.colorMixer.mode)
            put("hue_red", model.colorMixer.hueRed.toDouble())
            put("hue_orange", model.colorMixer.hueOrange.toDouble())
            put("hue_yellow", model.colorMixer.hueYellow.toDouble())
            put("hue_green", model.colorMixer.hueGreen.toDouble())
            put("hue_aqua", model.colorMixer.hueAqua.toDouble())
            put("hue_blue", model.colorMixer.hueBlue.toDouble())
            put("hue_purple", model.colorMixer.huePurple.toDouble())
            put("hue_magenta", model.colorMixer.hueMagenta.toDouble())

            put("sat_red", model.colorMixer.satRed.toDouble())
            put("sat_orange", model.colorMixer.satOrange.toDouble())
            put("sat_yellow", model.colorMixer.satYellow.toDouble())
            put("sat_green", model.colorMixer.satGreen.toDouble())
            put("sat_aqua", model.colorMixer.satAqua.toDouble())
            put("sat_blue", model.colorMixer.satBlue.toDouble())
            put("sat_purple", model.colorMixer.satPurple.toDouble())
            put("sat_magenta", model.colorMixer.satMagenta.toDouble())

            put("lum_red", model.colorMixer.lumRed.toDouble())
            put("lum_orange", model.colorMixer.lumOrange.toDouble())
            put("lum_yellow", model.colorMixer.lumYellow.toDouble())
            put("lum_green", model.colorMixer.lumGreen.toDouble())
            put("lum_aqua", model.colorMixer.lumAqua.toDouble())
            put("lum_blue", model.colorMixer.lumBlue.toDouble())
            put("lum_purple", model.colorMixer.lumPurple.toDouble())
            put("lum_magenta", model.colorMixer.lumMagenta.toDouble())

            put("point_active", model.colorMixer.pointActive)
            put("point_target_r", model.colorMixer.pointTargetR.toDouble())
            put("point_target_g", model.colorMixer.pointTargetG.toDouble())
            put("point_target_b", model.colorMixer.pointTargetB.toDouble())
            put("point_hue_shift", model.colorMixer.pointHueShift.toDouble())
            put("point_sat_shift", model.colorMixer.pointSatShift.toDouble())
            put("point_lum_shift", model.colorMixer.pointLumShift.toDouble())
            put("point_range", model.colorMixer.pointRange.toDouble())
        }
        root.put("color_mixer", cm)

        // 4. Color Grading
        val cg = JSONObject().apply {
            put("shadow_hue", model.colorGrading.shadowHue.toDouble())
            put("shadow_sat", model.colorGrading.shadowSat.toDouble())
            put("shadow_lum", model.colorGrading.shadowLum.toDouble())
            put("midtone_hue", model.colorGrading.midtoneHue.toDouble())
            put("midtone_sat", model.colorGrading.midtoneSat.toDouble())
            put("midtone_lum", model.colorGrading.midtoneLum.toDouble())
            put("highlight_hue", model.colorGrading.highlightHue.toDouble())
            put("highlight_sat", model.colorGrading.highlightSat.toDouble())
            put("highlight_lum", model.colorGrading.highlightLum.toDouble())
            put("global_hue", model.colorGrading.globalHue.toDouble())
            put("global_sat", model.colorGrading.globalSat.toDouble())
            put("global_lum", model.colorGrading.globalLum.toDouble())
            put("blending", model.colorGrading.blending.toDouble())
            put("balance", model.colorGrading.balance.toDouble())
        }
        root.put("color_grading", cg)

        // 5. Detail
        val detail = JSONObject().apply {
            put("sharpen_amount", model.detail.sharpenAmount.toDouble())
            put("sharpen_radius", model.detail.sharpenRadius.toDouble())
            put("sharpen_detail", model.detail.sharpenDetail.toDouble())
            put("sharpen_masking", model.detail.sharpenMasking.toDouble())
            put("nr_luminance", model.detail.nrLuminance.toDouble())
            put("nr_lum_detail", model.detail.nrLumDetail.toDouble())
            put("nr_color", model.detail.nrColor.toDouble())
        }
        root.put("detail", detail)

        // 6. Lens Corrections
        val lc = JSONObject().apply {
            put("distortion", model.lensCorrections.distortion.toDouble())
            put("remove_ca", model.lensCorrections.removeCa)
            put("ca_red_cyan", model.lensCorrections.caRedCyan.toDouble())
            put("ca_blue_yellow", model.lensCorrections.caBlueYellow.toDouble())
            put("vignette_amount", model.lensCorrections.vignetteAmount.toDouble())
            put("vignette_midpoint", model.lensCorrections.vignetteMidpoint.toDouble())
        }
        root.put("lens_corrections", lc)

        // 7. Transform
        val tf = JSONObject().apply {
            put("vertical", model.transform.vertical.toDouble())
            put("horizontal", model.transform.horizontal.toDouble())
            put("rotate", model.transform.rotate.toDouble())
            put("aspect", model.transform.aspect.toDouble())
            put("scale", model.transform.scale.toDouble())
            put("offset_x", model.transform.offsetX.toDouble())
            put("offset_y", model.transform.offsetY.toDouble())
        }
        root.put("transform", tf)

        // 8. Lens Blur
        val lb = JSONObject().apply {
            put("enabled", model.lensBlur.enabled)
            put("blur_amount", model.lensBlur.blurAmount.toDouble())
            put("bokeh_style", model.lensBlur.bokehStyle)
            put("bokeh_boost", model.lensBlur.bokehBoost.toDouble())
            put("focal_range", model.lensBlur.focalRange.toDouble())
        }
        root.put("lens_blur", lb)

        // 9. Effects
        val fx = JSONObject().apply {
            put("vignette_amount", model.effects.vignetteAmount.toDouble())
            put("vignette_midpoint", model.effects.vignetteMidpoint.toDouble())
            put("vignette_roundness", model.effects.vignetteRoundness.toDouble())
            put("vignette_feather", model.effects.vignetteFeather.toDouble())
            put("vignette_highlights", model.effects.vignetteHighlights.toDouble())
            put("grain_amount", model.effects.grainAmount.toDouble())
            put("grain_size", model.effects.grainSize.toDouble())
            put("grain_roughness", model.effects.grainRoughness.toDouble())
        }
        root.put("effects", fx)

        // 10. Calibration
        val calib = JSONObject().apply {
            put("shadow_tint", model.calibration.shadowTint.toDouble())
            put("red_hue", model.calibration.redHue.toDouble())
            put("red_sat", model.calibration.redSat.toDouble())
            put("green_hue", model.calibration.greenHue.toDouble())
            put("green_sat", model.calibration.greenSat.toDouble())
            put("blue_hue", model.calibration.blueHue.toDouble())
            put("blue_sat", model.calibration.blueSat.toDouble())
        }
        root.put("calibration", calib)

        if (cropRegion != null && cropRegion.hasCrop) {
            val cropObj = JSONObject().apply {
                put("left", cropRegion.left.toDouble())
                put("top", cropRegion.top.toDouble())
                put("right", cropRegion.right.toDouble())
                put("bottom", cropRegion.bottom.toDouble())
                put("angle", cropRegion.angle.toDouble())
            }
            root.put("crop", cropObj)
        }

        return root.toString()
    }

    fun fromJson(jsonStr: String): LightroomDevelopModel = LightroomDeserializer.fromJson(jsonStr)
}
