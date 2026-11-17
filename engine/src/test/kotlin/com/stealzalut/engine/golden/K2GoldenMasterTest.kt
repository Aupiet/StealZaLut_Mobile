package com.stealzalut.engine.golden

import com.stealzalut.engine.autoedit.AutoEdit
import com.stealzalut.engine.lightroom.LightroomColorPipeline
import com.stealzalut.engine.lut.CubeParser
import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.model.BasicSettings
import com.stealzalut.engine.model.CalibrationSettings
import com.stealzalut.engine.model.ColorGradingSettings
import com.stealzalut.engine.model.ColorMixerSettings
import com.stealzalut.engine.model.Point2D
import com.stealzalut.engine.model.Step2Params
import com.stealzalut.engine.model.Step3Params
import com.stealzalut.engine.model.ToneCurveSettings
import com.stealzalut.engine.pipeline.PipelineBake
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

class K2GoldenMasterTest {

    private val goldenDir: File
        get() {
            val root = File(".").canonicalFile
            val dir = if (File(root, "tests/golden").exists()) {
                File(root, "tests/golden")
            } else {
                File(root.parentFile, "tests/golden")
            }
            assertTrue(dir.exists(), "Golden master dir not found: ${dir.absolutePath}")
            return dir
        }

    private fun assertAllClose(actual: FloatArray, expected: FloatArray, atol: Float = 1e-4f, label: String = "") {
        assertEquals(expected.size, actual.size, "Size mismatch for $label")
        var maxDiff = 0.0f
        for (i in actual.indices) {
            val diff = abs(actual[i] - expected[i])
            if (diff > maxDiff) maxDiff = diff
            assertTrue(diff <= atol, "$label diverged at $i: actual=${actual[i]} expected=${expected[i]} diff=$diff")
        }
        println("$label PASSED: maxDiff = $maxDiff (atol = $atol)")
    }

    @Test
    fun testK2BasicAdjustments() {
        // Exposure pos
        val dPos = File(goldenDir, "k2_exposure_pos.npz")
        val inPos = NpyReader.readFloatArrayFromNpz(dPos, "input")
        val expPos = NpyReader.readFloatArrayFromNpz(dPos, "output")
        val outPos = LightroomColorPipeline.applyBasicAdjustments(inPos, inPos.size / 3, BasicSettings(exposure = 2.0f))
        assertAllClose(outPos, expPos, label = "Exposure +2.0")

        // Exposure neg
        val dNeg = File(goldenDir, "k2_exposure_neg.npz")
        val inNeg = NpyReader.readFloatArrayFromNpz(dNeg, "input")
        val expNeg = NpyReader.readFloatArrayFromNpz(dNeg, "output")
        val outNeg = LightroomColorPipeline.applyBasicAdjustments(inNeg, inNeg.size / 3, BasicSettings(exposure = -2.0f))
        assertAllClose(outNeg, expNeg, label = "Exposure -2.0")

        // Contrast
        val dCt = File(goldenDir, "k2_contrast.npz")
        val inCt = NpyReader.readFloatArrayFromNpz(dCt, "input")
        val expCt = NpyReader.readFloatArrayFromNpz(dCt, "out_pos")
        val outCt = LightroomColorPipeline.applyBasicAdjustments(inCt, inCt.size / 3, BasicSettings(contrast = 50.0f))
        assertAllClose(outCt, expCt, label = "Contrast +50")

        // White Balance
        val dWb = File(goldenDir, "k2_white_balance.npz")
        val inWb = NpyReader.readFloatArrayFromNpz(dWb, "input")
        val expWb = NpyReader.readFloatArrayFromNpz(dWb, "output")
        val outWb = LightroomColorPipeline.applyBasicAdjustments(inWb, inWb.size / 3, BasicSettings(temp = 30.0f, tint = -20.0f))
        assertAllClose(outWb, expWb, label = "White Balance (30, -20)")

        // Tone adjustments
        val dTone = File(goldenDir, "k2_tone_adjustments.npz")
        val inTone = NpyReader.readFloatArrayFromNpz(dTone, "input")
        val expTone = NpyReader.readFloatArrayFromNpz(dTone, "output")
        val outTone = LightroomColorPipeline.applyBasicAdjustments(
            inTone, inTone.size / 3,
            BasicSettings(highlights = -60.0f, shadows = 40.0f, whites = 20.0f, blacks = -20.0f)
        )
        assertAllClose(outTone, expTone, label = "Tone adjustments")

        // Vibrance & Saturation
        val dVs = File(goldenDir, "k2_vibrance_sat.npz")
        val inVs = NpyReader.readFloatArrayFromNpz(dVs, "input")
        val expVs = NpyReader.readFloatArrayFromNpz(dVs, "output")
        val outVs = LightroomColorPipeline.applyBasicAdjustments(
            inVs, inVs.size / 3,
            BasicSettings(vibrance = 40.0f, saturation = -30.0f)
        )
        assertAllClose(outVs, expVs, label = "Vibrance & Saturation")
    }

    @Test
    fun testK2ToneCurves() {
        val dS = File(goldenDir, "k2_tone_curve_s.npz")
        val inS = NpyReader.readFloatArrayFromNpz(dS, "input")
        val expS = NpyReader.readFloatArrayFromNpz(dS, "output")
        val ptsS = listOf(
            Point2D(0.0f, 0.0f), Point2D(0.25f, 0.15f), Point2D(0.5f, 0.5f),
            Point2D(0.75f, 0.85f), Point2D(1.0f, 1.0f)
        )
        val outS = LightroomColorPipeline.applyToneCurve(inS, inS.size / 3, ToneCurveSettings(mode = "point", rgbPoints = ptsS))
        assertAllClose(outS, expS, label = "Tone Curve S-Curve")

        val dExt = File(goldenDir, "k2_tone_curve_extreme.npz")
        val inExt = NpyReader.readFloatArrayFromNpz(dExt, "input")
        val expExt = NpyReader.readFloatArrayFromNpz(dExt, "output")
        val ptsExt = listOf(
            Point2D(0.0f, 0.2f), Point2D(0.1f, 0.4f), Point2D(0.5f, 0.6f),
            Point2D(0.9f, 0.8f), Point2D(1.0f, 0.9f)
        )
        val outExt = LightroomColorPipeline.applyToneCurve(inExt, inExt.size / 3, ToneCurveSettings(mode = "point", rgbPoints = ptsExt))
        assertAllClose(outExt, expExt, label = "Tone Curve Extreme")
    }

    @Test
    fun testK2MixerGradingCalibration() {
        // HSL
        val dHsl = File(goldenDir, "k2_hsl_mixer.npz")
        val inHsl = NpyReader.readFloatArrayFromNpz(dHsl, "input")
        val expHsl = NpyReader.readFloatArrayFromNpz(dHsl, "output")
        val mixerSettings = ColorMixerSettings(
            hueRed = 15.0f, satRed = 25.0f, lumRed = -10.0f,
            hueOrange = -10.0f, satOrange = 30.0f, lumOrange = 5.0f,
            hueYellow = 20.0f, satYellow = -20.0f, lumYellow = 15.0f,
            hueGreen = -30.0f, satGreen = 40.0f, lumGreen = -15.0f,
            hueAqua = 10.0f, satAqua = 15.0f, lumAqua = 10.0f,
            hueBlue = -25.0f, satBlue = 35.0f, lumBlue = -20.0f,
            huePurple = 15.0f, satPurple = -15.0f, lumPurple = 5.0f,
            hueMagenta = -10.0f, satMagenta = 20.0f, lumMagenta = -5.0f
        )
        val outHsl = LightroomColorPipeline.applyColorMixer(inHsl, inHsl.size / 3, mixerSettings)
        assertAllClose(outHsl, expHsl, label = "HSL Mixer")

        // Grading
        val dGr = File(goldenDir, "k2_color_grading.npz")
        val inGr = NpyReader.readFloatArrayFromNpz(dGr, "input")
        val expGr = NpyReader.readFloatArrayFromNpz(dGr, "output")
        val gradingSettings = ColorGradingSettings(
            shadowHue = 210.0f, shadowSat = 35.0f, shadowLum = -10.0f,
            midtoneHue = 35.0f, midtoneSat = 20.0f, midtoneLum = 5.0f,
            highlightHue = 45.0f, highlightSat = 40.0f, highlightLum = 10.0f,
            balance = 15.0f, blending = 60.0f
        )
        val outGr = LightroomColorPipeline.applyColorGrading(inGr, inGr.size / 3, gradingSettings)
        assertAllClose(outGr, expGr, label = "Color Grading")

        // Calibration
        val dCal = File(goldenDir, "k2_calibration.npz")
        val inCal = NpyReader.readFloatArrayFromNpz(dCal, "input")
        val expCal = NpyReader.readFloatArrayFromNpz(dCal, "output")
        val calibSettings = CalibrationSettings(
            shadowTint = 15.0f,
            redHue = -20.0f, redSat = 25.0f,
            greenHue = 15.0f, greenSat = -10.0f,
            blueHue = 30.0f, blueSat = 30.0f
        )
        val outCal = LightroomColorPipeline.applyCalibration(inCal, inCal.size / 3, calibSettings)
        assertAllClose(outCal, expCal, label = "Calibration")
    }

    @Test
    fun testK2FullPipelineAndAutoEdit() {
        val bakedPath = File(goldenDir, "k2_full_pipeline_baked_33.cube")
        val parsed = FileInputStream(bakedPath).use { CubeParser.parse(it) }
        assertEquals(33, parsed.size)

        val rawLut = LutGenerator.generateIdentityGrid(33)
        val p2 = Step2Params(globalIntensity = 0.9f, lumaLock = 0.0f, neutralAxisLock = 1.0f, skinToneLock = 0.0f)
        val p3 = Step3Params(exposure = 0.5f, contrast = 0.2f, vibrance = 0.15f, saturation = -0.1f)
        val actualLut = PipelineBake.bakeFullPipelineLut(rawLut, p2, p3, lutSize = 33)
        assertAllClose(actualLut, parsed.data, label = "Full Pipeline Bake 33^3")

        // Auto Edit dark image
        val autoSettings = AutoEdit.getAutoEditSettings(-1.5f)
        assertEquals(-1.5f, autoSettings.exposure)
        assertEquals(68.0f, autoSettings.contrast)
        println("Auto Edit Settings: PASSED")
    }
}
