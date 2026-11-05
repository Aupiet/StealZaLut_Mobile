package com.stealzalut.mobile.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LightroomSerializerTest {

    @Test
    fun `toJson and fromJson preserves all 10 Lightroom panels and bypass flags`() {
        val original = LightroomDevelopModel(
            isBasicEnabled = true,
            isToneCurveEnabled = false,
            isColorMixerEnabled = true,
            isColorGradingEnabled = false,
            isDetailEnabled = true,
            isLensCorrectionsEnabled = false,
            isTransformEnabled = true,
            isLensBlurEnabled = false,
            isEffectsEnabled = true,
            isCalibrationEnabled = false,
            basic = BasicAdjustmentsModel(
                temp = 12.5f,
                tint = -8.0f,
                exposure = 1.25f,
                contrast = 20.0f,
                highlights = -35.0f,
                shadows = 40.0f,
                whites = 10.0f,
                blacks = -15.0f,
                texture = 18.0f,
                clarity = 22.0f,
                dehaze = 15.0f,
                vibrance = 25.0f,
                saturation = -5.0f
            ),
            toneCurve = ToneCurveModel(
                mode = "parametric",
                highlights = 15.0f,
                lights = 10.0f,
                darks = -10.0f,
                shadows = -20.0f,
                shadowSplit = 30.0f,
                midtoneSplit = 55.0f,
                highlightSplit = 80.0f,
                rgbPoints = listOf(0.0f to 0.0f, 0.5f to 0.55f, 1.0f to 1.0f)
            ),
            colorMixer = ColorMixerModel(
                mode = "hsl",
                hueRed = 10.0f,
                satOrange = -15.0f,
                lumBlue = 20.0f,
                pointActive = true,
                pointTargetR = 0.8f,
                pointTargetG = 0.3f,
                pointTargetB = 0.2f,
                pointHueShift = 12.0f,
                pointSatShift = -25.0f,
                pointLumShift = 8.0f,
                pointRange = 65.0f
            ),
            colorGrading = ColorGradingModel(
                shadowHue = 210.0f,
                shadowSat = 30.0f,
                shadowLum = -10.0f,
                midtoneHue = 45.0f,
                midtoneSat = 15.0f,
                midtoneLum = 5.0f,
                highlightHue = 35.0f,
                highlightSat = 25.0f,
                highlightLum = 12.0f,
                globalHue = 0.0f,
                globalSat = 0.0f,
                globalLum = 0.0f,
                blending = 60.0f,
                balance = -15.0f
            ),
            detail = DetailModel(
                sharpenAmount = 65.0f,
                sharpenRadius = 1.2f,
                sharpenDetail = 30.0f,
                sharpenMasking = 25.0f,
                nrLuminance = 20.0f,
                nrLumDetail = 50.0f,
                nrColor = 25.0f
            ),
            lensCorrections = LensCorrectionsModel(
                distortion = 12.0f,
                removeCa = true,
                caRedCyan = 5.0f,
                caBlueYellow = -4.0f,
                vignetteAmount = -25.0f,
                vignetteMidpoint = 45.0f
            ),
            transform = TransformModel(
                vertical = -8.0f,
                horizontal = 6.0f,
                rotate = 1.5f,
                aspect = -5.0f,
                scale = 105.0f,
                offsetX = 2.0f,
                offsetY = -3.0f
            ),
            lensBlur = LensBlurModel(
                enabled = true,
                blurAmount = 45.0f,
                bokehStyle = "blade7",
                bokehBoost = 35.0f,
                focalRange = 60.0f
            ),
            effects = EffectsModel(
                vignetteAmount = -30.0f,
                vignetteMidpoint = 40.0f,
                vignetteRoundness = 10.0f,
                vignetteFeather = 60.0f,
                vignetteHighlights = 15.0f,
                grainAmount = 35.0f,
                grainSize = 40.0f,
                grainRoughness = 60.0f
            ),
            calibration = CalibrationModel(
                shadowTint = 8.0f,
                redHue = -10.0f,
                redSat = 25.0f,
                greenHue = 15.0f,
                greenSat = -5.0f,
                blueHue = -8.0f,
                blueSat = 30.0f
            )
        )

        val json = LightroomSerializer.toJson(original)
        val deserialized = LightroomSerializer.fromJson(json)

        // Verify flags
        assertTrue(deserialized.isBasicEnabled)
        assertFalse(deserialized.isToneCurveEnabled)
        assertTrue(deserialized.isColorMixerEnabled)
        assertFalse(deserialized.isColorGradingEnabled)
        assertTrue(deserialized.isDetailEnabled)
        assertFalse(deserialized.isLensCorrectionsEnabled)
        assertTrue(deserialized.isTransformEnabled)
        assertFalse(deserialized.isLensBlurEnabled)
        assertTrue(deserialized.isEffectsEnabled)
        assertFalse(deserialized.isCalibrationEnabled)

        // Verify values
        assertEquals(12.5f, deserialized.basic.temp, 0.001f)
        assertEquals(1.25f, deserialized.basic.exposure, 0.001f)
        assertEquals("parametric", deserialized.toneCurve.mode)
        assertEquals(15.0f, deserialized.toneCurve.highlights, 0.001f)
        assertEquals(3, deserialized.toneCurve.rgbPoints.size)
        assertEquals(0.5f, deserialized.toneCurve.rgbPoints[1].first, 0.001f)
        assertEquals(0.55f, deserialized.toneCurve.rgbPoints[1].second, 0.001f)
        assertEquals(10.0f, deserialized.colorMixer.hueRed, 0.001f)
        assertEquals(65.0f, deserialized.colorMixer.pointRange, 0.001f)
        assertEquals(210.0f, deserialized.colorGrading.shadowHue, 0.001f)
        assertEquals(60.0f, deserialized.colorGrading.blending, 0.001f)
        assertEquals(65.0f, deserialized.detail.sharpenAmount, 0.001f)
        assertEquals(12.0f, deserialized.lensCorrections.distortion, 0.001f)
        assertTrue(deserialized.lensCorrections.removeCa)
        assertEquals(1.5f, deserialized.transform.rotate, 0.001f)
        assertTrue(deserialized.lensBlur.enabled)
        assertEquals("blade7", deserialized.lensBlur.bokehStyle)
        assertEquals(-30.0f, deserialized.effects.vignetteAmount, 0.001f)
        assertEquals(35.0f, deserialized.effects.grainAmount, 0.001f)
        assertEquals(8.0f, deserialized.calibration.shadowTint, 0.001f)
        assertEquals(-10.0f, deserialized.calibration.redHue, 0.001f)
    }
}
