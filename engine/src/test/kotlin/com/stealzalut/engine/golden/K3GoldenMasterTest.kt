package com.stealzalut.engine.golden

import com.stealzalut.engine.model.BasicSettings
import com.stealzalut.engine.model.DetailSettings
import com.stealzalut.engine.spatial.SpatialFilters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

class K3GoldenMasterTest {

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
    fun testK3GaussianBlur() {
        // Sigma 1.0 (128x128x3)
        val d1 = File(goldenDir, "k3_gaussian_blur_sigma1.npz")
        val in1 = NpyReader.readFloatArrayFromNpz(d1, "input")
        val exp1 = NpyReader.readFloatArrayFromNpz(d1, "output")
        val out1 = SpatialFilters.gaussianBlur2D(in1, 128, 128, sigma = 1.0f, channels = 3)
        assertAllClose(out1, exp1, label = "Gaussian Blur sigma=1.0")

        // Sigma 3.0 (512x512x3)
        val d3 = File(goldenDir, "k3_gaussian_blur_sigma3.npz")
        val in3 = NpyReader.readFloatArrayFromNpz(d3, "input")
        val exp3 = NpyReader.readFloatArrayFromNpz(d3, "output")
        val out3 = SpatialFilters.gaussianBlur2D(in3, 512, 512, sigma = 3.0f, channels = 3)
        assertAllClose(out3, exp3, label = "Gaussian Blur sigma=3.0")

        // 24 MP patch (256x256x3)
        val dPatch = File(goldenDir, "k3_gaussian_blur_24mp_patch.npz")
        val inPatch = NpyReader.readFloatArrayFromNpz(dPatch, "input")
        val expPatch = NpyReader.readFloatArrayFromNpz(dPatch, "output")
        val outPatch = SpatialFilters.gaussianBlur2D(inPatch, 256, 256, sigma = 3.0f, channels = 3)
        assertAllClose(outPatch, expPatch, label = "Gaussian Blur 24MP patch")
    }

    @Test
    fun testK3PresenceAndDetail() {
        // Presence (Clarity, Texture, Dehaze) (128x128x3)
        val dCl = File(goldenDir, "k3_spatial_clarity.npz")
        val inCl = NpyReader.readFloatArrayFromNpz(dCl, "input")
        val expCl = NpyReader.readFloatArrayFromNpz(dCl, "output")
        val sPresence = BasicSettings(clarity = 50.0f, texture = 30.0f, dehaze = 20.0f)
        val outCl = SpatialFilters.applyPresenceSpatial(inCl, 128, 128, sPresence)
        assertAllClose(outCl, expCl, label = "Presence (Clarity, Texture, Dehaze)")

        // Detail (Sharpening) (128x128x3)
        val dDet = File(goldenDir, "k3_spatial_detail.npz")
        val inDet = NpyReader.readFloatArrayFromNpz(dDet, "input")
        val expDet = NpyReader.readFloatArrayFromNpz(dDet, "output")
        val sDetail = DetailSettings(sharpenAmount = 60.0f, sharpenRadius = 1.2f, sharpenDetail = 35.0f)
        val outDet = SpatialFilters.applyDetailSpatial(inDet, 128, 128, sDetail)
        assertAllClose(outDet, expDet, label = "Detail (Sharpening)")
    }
}
