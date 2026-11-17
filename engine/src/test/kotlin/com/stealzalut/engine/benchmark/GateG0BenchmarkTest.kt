package com.stealzalut.engine.benchmark

import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.lut.TrilinearInterpolator
import com.stealzalut.engine.spatial.SeparableGaussianBlur
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

class GateG0BenchmarkTest {

    @Test
    fun benchmarkGateG0() = runBlocking {
        println("=== GATE G0 BENCHMARK START ===")
        val lut33 = LutGenerator.generateIdentityGrid(33)

        // 1. Preview 1024x768 (≈ 0.8 MP)
        val previewW = 1024
        val previewH = 768
        val previewBytes = ByteArray(previewW * previewH * 3) { (it % 256).toByte() }
        val previewOut = ByteArray(previewBytes.size)

        // Froid LUT Preview
        val tLutPreviewColdMs = measureNanoTime {
            TrilinearInterpolator.applyLutTrilinear(previewBytes, previewW, previewH, lut33, 33, previewOut)
        } / 1_000_000.0

        // Chaud LUT Preview
        var tLutPreviewWarmMs = 0.0
        repeat(5) {
            val t = measureNanoTime {
                TrilinearInterpolator.applyLutTrilinear(previewBytes, previewW, previewH, lut33, 33, previewOut)
            } / 1_000_000.0
            tLutPreviewWarmMs += t / 5.0
        }
        println("Preview 1024x768 LUT 33^3: Froid = ${"%.2f".format(tLutPreviewColdMs)} ms, Chaud = ${"%.2f".format(tLutPreviewWarmMs)} ms (Seuil: <= 50 ms)")

        // 2. 12 MP (4000x3000)
        val w12 = 4000
        val h12 = 3000
        val bytes12mp = ByteArray(w12 * h12 * 3) { (it % 256).toByte() }
        val out12mp = ByteArray(bytes12mp.size)
        val tLut12mpMs = measureNanoTime {
            TrilinearInterpolator.applyLutTrilinear(bytes12mp, w12, h12, lut33, 33, out12mp)
        } / 1_000_000.0
        println("12 MP (4000x3000) LUT 33^3: ${"%.2f".format(tLut12mpMs)} ms")

        // 3. 24 MP (6000x4000)
        val runtime = Runtime.getRuntime()
        System.gc()
        val memBefore24 = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val w24 = 6000
        val h24 = 4000
        val bytes24mp = ByteArray(w24 * h24 * 3) { (it % 256).toByte() }
        val out24mp = ByteArray(bytes24mp.size)

        val tLut24mpMs = measureNanoTime {
            TrilinearInterpolator.applyLutTrilinear(bytes24mp, w24, h24, lut33, 33, out24mp)
        } / 1_000_000.0
        val memAfter24Lut = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        println("24 MP (6000x4000) LUT 33^3: ${"%.2f".format(tLut24mpMs)} ms (Seuil: <= 1500 ms)")
        println("24 MP LUT Memoire: Before=${memBefore24} MB, After=${memAfter24Lut} MB, Delta=${memAfter24Lut - memBefore24} MB")

        // 4. Flou Gaussien Preview (sigma = 3.0)
        val previewFloats = FloatArray(previewW * previewH * 3) { 0.5f }
        val previewBlurOut = FloatArray(previewFloats.size)
        val tBlurPreviewColdMs = measureNanoTime {
            SeparableGaussianBlur.blur(previewFloats, previewW, previewH, 3.0f, previewBlurOut, channels = 3)
        } / 1_000_000.0
        var tBlurPreviewWarmMs = 0.0
        repeat(5) {
            val t = measureNanoTime {
                SeparableGaussianBlur.blur(previewFloats, previewW, previewH, 3.0f, previewBlurOut, channels = 3)
            } / 1_000_000.0
            tBlurPreviewWarmMs += t / 5.0
        }
        println("Preview 1024x768 Flou sigma=3: Froid = ${"%.2f".format(tBlurPreviewColdMs)} ms, Chaud = ${"%.2f".format(tBlurPreviewWarmMs)} ms")

        // 5. Flou Gaussien 12 MP (sigma = 3.0)
        val floats12mp = FloatArray(w12 * h12 * 3) { 0.5f }
        val blurOut12mp = FloatArray(floats12mp.size)
        val tBlur12mpMs = measureNanoTime {
            SeparableGaussianBlur.blur(floats12mp, w12, h12, 3.0f, blurOut12mp, channels = 3)
        } / 1_000_000.0
        println("12 MP (4000x3000) Flou sigma=3: ${"%.2f".format(tBlur12mpMs)} ms")

        // 6. Flou Gaussien 24 MP (sigma = 3.0)
        System.gc()
        val memBefore24Blur = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val floats24mp = FloatArray(w24 * h24 * 3) { 0.5f }
        val blurOut24mp = FloatArray(floats24mp.size)
        val tBlur24mpMs = measureNanoTime {
            SeparableGaussianBlur.blur(floats24mp, w24, h24, 3.0f, blurOut24mp, channels = 3)
        } / 1_000_000.0
        val memAfter24Blur = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        println("24 MP (6000x4000) Flou sigma=3: ${"%.2f".format(tBlur24mpMs)} ms (Seuil: <= 4000 ms)")
        println("24 MP Flou Memoire: Delta=${memAfter24Blur - memBefore24Blur} MB")

        println("=== GATE G0 BENCHMARK END ===")
    }
}
