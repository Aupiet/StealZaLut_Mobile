package com.stealzalut.engine.golden

import com.stealzalut.engine.raw.RawDecoder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

class K5GoldenMasterTest {

    private val goldenDir = File("../tests/golden").let {
        if (it.exists()) it else File("tests/golden")
    }

    private fun compareRgb(
        name: String,
        actual: FloatArray,
        expected: FloatArray,
        atol: Float = 1e-4f,
        meanTol: Float = 0.005f
    ) {
        assertEquals(expected.size, actual.size, "Size mismatch for $name")
        var sumDiff = 0.0
        var maxDiff = 0.0f
        for (i in actual.indices) {
            val diff = abs(actual[i] - expected[i])
            if (diff > maxDiff) maxDiff = diff
            sumDiff += diff
        }
        val meanDiff = (sumDiff / actual.size).toFloat()
        println("[$name] meanDiff = $meanDiff, maxDiff = $maxDiff")
        println("[$name] actual[:10]   = ${actual.take(10).joinToString()}")
        println("[$name] expected[:10] = ${expected.take(10).joinToString()}")
        assertTrue(meanDiff <= meanTol, "[$name] meanDiff $meanDiff exceeds tolerance $meanTol")
        assertTrue(maxDiff <= atol, "[$name] maxDiff $maxDiff exceeds atol $atol")
    }

    @Test
    fun testRggbDngDecoding() {
        val dngFile = File(goldenDir, "k5_test_rggb.dng")
        val npzFile = File(goldenDir, "k5_decoded_rggb.npz")
        val expected = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val decoded = RawDecoder.decodeDngImage(dngFile)
        assertEquals(48, decoded.height)
        assertEquals(64, decoded.width)
        compareRgb("K5 RGGB", decoded.rgb, expected)
    }

    @Test
    fun testBggrDngDecoding() {
        val dngFile = File(goldenDir, "k5_test_bggr.dng")
        val npzFile = File(goldenDir, "k5_decoded_bggr.npz")
        val expected = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val decoded = RawDecoder.decodeDngImage(dngFile)
        assertEquals(48, decoded.height)
        assertEquals(64, decoded.width)
        compareRgb("K5 BGGR", decoded.rgb, expected)
    }

    @Test
    fun testDeflateDngDecoding() {
        val dngFile = File(goldenDir, "k5_test_deflate.dng")
        val npzFile = File(goldenDir, "k5_decoded_deflate.npz")
        val expected = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val decoded = RawDecoder.decodeDngImage(dngFile)
        assertEquals(48, decoded.height)
        assertEquals(64, decoded.width)
        compareRgb("K5 Deflate", decoded.rgb, expected)
    }

    @Test
    fun testOrientation6DngDecoding() {
        val dngFile = File(goldenDir, "k5_test_orient6.dng")
        val npzFile = File(goldenDir, "k5_decoded_orient6.npz")
        val expected = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val decoded = RawDecoder.decodeDngImage(dngFile)
        assertEquals(64, decoded.height)
        assertEquals(48, decoded.width)
        compareRgb("K5 Orientation 6", decoded.rgb, expected)
    }

    @Test
    fun testPreviewDngDecoding() {
        val dngFile = File(goldenDir, "k5_test_rggb.dng")
        val npzFile = File(goldenDir, "k5_decoded_preview.npz")
        val expected = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val decoded = RawDecoder.decodeDngImage(dngFile, maxDim = 32)
        assertEquals(24, decoded.height)
        assertEquals(32, decoded.width)
        compareRgb("K5 Preview", decoded.rgb, expected)
    }
}
