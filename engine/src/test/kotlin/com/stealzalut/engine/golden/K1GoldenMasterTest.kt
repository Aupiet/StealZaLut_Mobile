package com.stealzalut.engine.golden

import com.stealzalut.engine.lut.CubeParser
import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.lut.LutRefiner
import com.stealzalut.engine.lut.TrilinearInterpolator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

class K1GoldenMasterTest {

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

    @Test
    fun testK1IdentityLuts() {
        for (size in listOf(17, 33, 65)) {
            val cubeFile = File(goldenDir, "k1_identity_$size.cube")
            assertTrue(cubeFile.exists())
            val parsed = FileInputStream(cubeFile).use { CubeParser.parse(it) }
            assertEquals(size, parsed.size)

            val generated = LutGenerator.generateIdentityGrid(size)
            assertEquals(generated.size, parsed.data.size)
            for (i in generated.indices) {
                val diff = abs(generated[i] - parsed.data[i])
                assertTrue(diff <= 1e-4f, "Identity LUT size $size diverged at $i: diff=$diff")
            }
        }
    }

    @Test
    fun testK1ApplyLutTrilinear() = runBlocking {
        val npz = File(goldenDir, "k1_apply_lut_256.npz")
        val input = NpyReader.readFloatArrayFromNpz(npz, "input")
        val lut = NpyReader.readFloatArrayFromNpz(npz, "lut")
        val expected = NpyReader.readFloatArrayFromNpz(npz, "output")

        val width = 256
        val height = 256
        val output = FloatArray(input.size)
        TrilinearInterpolator.applyLutTrilinearFloats(
            image = input,
            width = width,
            height = height,
            lut = lut,
            lutSize = 33,
            output = output
        )

        var maxDiff = 0.0f
        for (i in output.indices) {
            val diff = abs(output[i] - expected[i])
            if (diff > maxDiff) maxDiff = diff
            assertTrue(diff <= 1e-4f, "Trilinear apply diverged at $i: actual=${output[i]} expected=${expected[i]} diff=$diff")
        }
        println("K1 testK1ApplyLutTrilinear: maxDiff = $maxDiff")
    }

    @Test
    fun testK1LutLocks() {
        val npz = File(goldenDir, "k1_lut_locks.npz")
        val lut = NpyReader.readFloatArrayFromNpz(npz, "lut")
        val expected = NpyReader.readFloatArrayFromNpz(npz, "output")

        val actual = LutRefiner.refineAndBlendLut(
            rawLut = lut,
            lutSize = 33,
            intensity = 0.85f,
            skinProtection = 0.8f,
            smoothSigma = 0.5f,
            neutralLock = 1.0f
        )

        var maxDiff = 0.0f
        for (i in actual.indices) {
            val diff = abs(actual[i] - expected[i])
            if (diff > maxDiff) maxDiff = diff
            assertTrue(diff <= 1e-4f, "LUT locks diverged at $i: actual=${actual[i]} expected=${expected[i]} diff=$diff")
        }
        println("K1 testK1LutLocks: maxDiff = $maxDiff")
    }

    @Test
    fun testK1ExtremeLut() = runBlocking {
        val npz = File(goldenDir, "k1_extreme_lut.npz")
        val input = NpyReader.readFloatArrayFromNpz(npz, "input")
        val lut = NpyReader.readFloatArrayFromNpz(npz, "lut")
        val expected = NpyReader.readFloatArrayFromNpz(npz, "output")

        val width = 256
        val height = 256
        val output = FloatArray(input.size)
        TrilinearInterpolator.applyLutTrilinearFloats(
            image = input,
            width = width,
            height = height,
            lut = lut,
            lutSize = 33,
            output = output
        )

        var maxDiff = 0.0f
        for (i in output.indices) {
            val diff = abs(output[i] - expected[i])
            if (diff > maxDiff) maxDiff = diff
            assertTrue(diff <= 1e-4f, "Extreme LUT diverged at $i: actual=${output[i]} expected=${expected[i]} diff=$diff")
        }
        println("K1 testK1ExtremeLut: maxDiff = $maxDiff")
    }

    @Test
    fun testK1Probes12mpAnd24mp() = runBlocking {
        // 12 MP probes (500 pixels)
        val d12 = File(goldenDir, "k1_apply_lut_12mp_probes.npz")
        val in12 = NpyReader.readFloatArrayFromNpz(d12, "probe_in")
        val lut12 = NpyReader.readFloatArrayFromNpz(d12, "lut")
        val exp12 = NpyReader.readFloatArrayFromNpz(d12, "probe_out")

        val out12 = FloatArray(in12.size)
        TrilinearInterpolator.applyLutTrilinearFloats(
            image = in12,
            width = 500,
            height = 1,
            lut = lut12,
            lutSize = 33,
            output = out12
        )
        for (i in out12.indices) {
            val diff = abs(out12[i] - exp12[i])
            assertTrue(diff <= 1e-4f, "12MP probe diverged at $i: diff=$diff")
        }

        // 24 MP probes (1000 pixels)
        val d24 = File(goldenDir, "k1_apply_lut_24mp_probes.npz")
        val in24 = NpyReader.readFloatArrayFromNpz(d24, "probe_in")
        val lut24 = NpyReader.readFloatArrayFromNpz(d24, "lut")
        val exp24 = NpyReader.readFloatArrayFromNpz(d24, "probe_out")

        val out24 = FloatArray(in24.size)
        TrilinearInterpolator.applyLutTrilinearFloats(
            image = in24,
            width = 1000,
            height = 1,
            lut = lut24,
            lutSize = 33,
            output = out24
        )
        for (i in out24.indices) {
            val diff = abs(out24[i] - exp24[i])
            assertTrue(diff <= 1e-4f, "24MP probe diverged at $i: diff=$diff")
        }
        println("K1 testK1Probes12mpAnd24mp: PASSED")
    }
}
