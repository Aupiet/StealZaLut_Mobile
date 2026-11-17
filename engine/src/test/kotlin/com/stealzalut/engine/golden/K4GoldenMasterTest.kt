package com.stealzalut.engine.golden

import com.stealzalut.engine.ai.AiPack
import com.stealzalut.engine.lut.CubeParser
import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.lut.LutRefiner
import com.stealzalut.engine.preset.Recipe
import com.stealzalut.engine.preset.SimpleJson
import com.stealzalut.engine.transfer.ColorTransfer
import com.stealzalut.engine.zones.KMeans
import com.stealzalut.engine.zones.Sinkhorn
import com.stealzalut.engine.zones.fitZoneTransfer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

class K4GoldenMasterTest {

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
    fun testReinhardTransferUint8Tolerance() {
        testTransferMethod("reinhard")
    }

    @Test
    fun testMklTransferUint8Tolerance() {
        testTransferMethod("mkl")
    }

    @Test
    fun testHmTransferUint8Tolerance() {
        testTransferMethod("hm")
    }

    @Test
    fun testMvgdTransferUint8Tolerance() {
        testTransferMethod("mvgd")
    }

    @Test
    fun testOklabTransferUint8Tolerance() {
        testTransferMethod("oklab")
    }

    private fun testTransferMethod(method: String) {
        val npzFile = File(goldenDir, "k4_transfer_$method.npz")
        assertTrue(npzFile.exists(), "NPZ file should exist: ${npzFile.path}")

        val srcU8 = NpyReader.readByteArrayFromNpz(npzFile, "source")
        val refU8 = NpyReader.readByteArrayFromNpz(npzFile, "ref")
        val expF = NpyReader.readFloatArrayFromNpz(npzFile, "output")

        val srcF = FloatArray(srcU8.size) { i -> (srcU8[i].toInt() and 0xFF) / 255.0f }
        val refF = FloatArray(refU8.size) { i -> (refU8[i].toInt() and 0xFF) / 255.0f }

        val actualF = ColorTransfer.fitAndApplyTransfer(srcF, refF, method = method)
        assertEquals(expF.size, actualF.size, "Output size should match")

        var maxDiffU8 = 0
        var diffCount = 0
        for (i in expF.indices) {
            val expVal = kotlin.math.round(expF[i] * 255.0f).toInt().coerceIn(0, 255)
            val actVal = kotlin.math.round(actualF[i] * 255.0f).toInt().coerceIn(0, 255)
            val diff = abs(expVal - actVal)
            if (diff > maxDiffU8) maxDiffU8 = diff
            if (diff > 0) diffCount++
        }

        println("K4 transfer [$method]: maxDiff = $maxDiffU8 uint8 levels, diffCount = $diffCount / ${expF.size}")
        assertTrue(maxDiffU8 <= 1, "Transfer [$method] maxDiff ($maxDiffU8) must be <= 1 uint8 level")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testPaletteKMeans5And8() {
        val f5 = File(goldenDir, "k4_palette_kmeans_5.json")
        assertTrue(f5.exists())
        val json5 = SimpleJson.parse(f5.readText()) as Map<String, Any?>
        val expCount5 = (json5["count"] as Number).toInt()
        assertEquals(5, expCount5)

        val f8 = File(goldenDir, "k4_palette_kmeans_8.json")
        assertTrue(f8.exists())
        val json8 = SimpleJson.parse(f8.readText()) as Map<String, Any?>
        val expCount8 = (json8["count"] as Number).toInt()
        assertEquals(8, expCount8)

        val npz = File(goldenDir, "k4_transfer_reinhard.npz")
        val srcU8 = NpyReader.readByteArrayFromNpz(npz, "source")
        val pal5 = KMeans.analyzeZones(srcU8, 128, 128, 5)
        assertEquals(5, pal5.zones.size)

        val pal8 = KMeans.analyzeZones(srcU8, 128, 128, 8)
        assertEquals(8, pal8.zones.size)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testSinkhornMatching() {
        val f = File(goldenDir, "k4_sinkhorn_matching.json")
        assertTrue(f.exists())
        val json = SimpleJson.parse(f.readText()) as Map<String, Any?>
        assertTrue(json.isNotEmpty())

        val npz = File(goldenDir, "k4_transfer_reinhard.npz")
        val srcU8 = NpyReader.readByteArrayFromNpz(npz, "source")
        val refU8 = NpyReader.readByteArrayFromNpz(npz, "ref")
        val palSrc = KMeans.analyzeZones(srcU8, 128, 128, 5)
        val palRef = KMeans.analyzeZones(refU8, 128, 128, 5)

        val matches = Sinkhorn.matchZones(palSrc, palRef)
        assertEquals(5, matches.size)
        for (i in 1..5) {
            assertTrue(matches.containsKey(i), "Match for zone $i must exist")
            val m = matches[i]!!
            assertTrue(m.isNotEmpty(), "Targets for zone $i must not be empty")
            var sumW = 0.0f
            for (w in m.values) sumW += w
            assertTrue(abs(sumW - 1.0f) < 1e-4f, "Weights must sum to approx 1.0")
        }
    }

    @Test
    fun testZoneTransferModel() {
        val npz = File(goldenDir, "k4_zone_transfer.npz")
        val inputD = NpyReader.readDoubleArrayFromNpz(npz, "input")
        val expectedF = NpyReader.readFloatArrayFromNpz(npz, "output")
        val inputF = FloatArray(inputD.size) { i -> inputD[i].toFloat() }

        val srcNpz = File(goldenDir, "k4_transfer_reinhard.npz")
        val srcU8 = NpyReader.readByteArrayFromNpz(srcNpz, "source")
        val refU8 = NpyReader.readByteArrayFromNpz(srcNpz, "ref")

        val ztModel = fitZoneTransfer(
            source = srcU8,
            srcW = 128,
            srcH = 128,
            reference = refU8,
            refW = 128,
            refH = 128,
            zoneCount = 5,
            userOverrides = mapOf("1" to 2)
        )

        val actualF = ztModel.apply(inputF)
        assertEquals(expectedF.size, actualF.size)

        var maxDiff = 0.0f
        for (i in expectedF.indices) {
            val d = abs(expectedF[i] - actualF[i])
            if (d > maxDiff) maxDiff = d
        }
        println("Zone transfer 3 sample points maxDiff = $maxDiff")
        assertTrue(maxDiff <= 0.05f, "Zone transfer maxDiff ($maxDiff) must be reasonable")
    }

    @Test
    fun testAmbianceLut() {
        val cubeFile = File(goldenDir, "k4_ambiance_lut.cube")
        assertTrue(cubeFile.exists())
        val parsed = cubeFile.inputStream().use { CubeParser.parse(it) }
        val size = parsed.size
        val expectedLut = parsed.data
        assertEquals(33, size)

        val srcNpz = File(goldenDir, "k4_transfer_reinhard.npz")
        val srcU8 = NpyReader.readByteArrayFromNpz(srcNpz, "source")
        val refU8 = NpyReader.readByteArrayFromNpz(srcNpz, "ref")

        val identity = LutGenerator.generateIdentityGrid(size)
        val srcF = FloatArray(srcU8.size) { i -> (srcU8[i].toInt() and 0xFF) / 255.0f }
        val refF = FloatArray(refU8.size) { i -> (refU8[i].toInt() and 0xFF) / 255.0f }

        val transferred = ColorTransfer.fitAndApplyTransfer(srcF, refF, identity, method = "reinhard")
        val smoothed = LutRefiner.gaussianSmooth3D(transferred, size, sigma = 0.25f)

        var maxDiff = 0.0f
        var diffSum = 0.0
        for (i in expectedLut.indices) {
            val d = abs(expectedLut[i] - smoothed[i])
            if (d > maxDiff) maxDiff = d
            diffSum += d
        }
        val avgDiff = (diffSum / expectedLut.size).toFloat()
        println("Ambiance LUT 33^3: maxDiff = $maxDiff, avgDiff = $avgDiff")
        assertTrue(avgDiff <= 1e-4f, "Ambiance LUT avgDiff ($avgDiff) must be <= 1e-4")
    }

    @Test
    fun testRecipeRoundtrip() {
        val f = File(goldenDir, "k4_recipe.json")
        assertTrue(f.exists())
        val jsonText = f.readText()
        val recipe = Recipe.parse(jsonText)
        assertEquals("golden42", recipe.analysisId)
        assertEquals("zones", recipe.mode)
        assertEquals(2, recipe.links.size)

        val dumped = Recipe.dump(recipe)
        val reloaded = Recipe.parse(dumped)
        assertEquals(recipe.analysisId, reloaded.analysisId)
        assertEquals(recipe.mode, reloaded.mode)
        assertEquals(recipe.links.size, reloaded.links.size)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testAiPackJson() {
        val f = File(goldenDir, "k4_ai_pack.json")
        assertTrue(f.exists())
        val json = SimpleJson.parse(f.readText()) as Map<String, Any?>
        for (fileKey in AiPack.PACK_FILES) {
            assertTrue(json.containsKey(fileKey), "AI pack should contain key: $fileKey")
        }
    }
}
