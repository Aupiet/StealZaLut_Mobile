package com.stealzalut.engine

import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.lut.TrilinearInterpolator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrilinearInterpolatorTest {

    @Test
    fun `identity lut leaves image unchanged`() = runBlocking {
        val width = 64
        val height = 64
        val lutSize = 33
        val identity = LutGenerator.generateIdentityGrid(lutSize)

        val img = FloatArray(width * height * 3)
        for (i in img.indices) {
            img[i] = (i % 100) / 100.0f
        }
        val out = FloatArray(img.size)

        TrilinearInterpolator.applyLutTrilinearFloats(img, width, height, identity, lutSize, out)

        for (i in img.indices) {
            assertEquals(img[i], out[i], 1e-4f, "Difference at index $i")
        }
    }

    @Test
    fun `trilinear interpolation byte array bounds are preserved`() = runBlocking {
        val width = 32
        val height = 32
        val lutSize = 17
        val identity = LutGenerator.generateIdentityGrid(lutSize)

        val img = ByteArray(width * height * 3) { (it % 256).toByte() }
        val out = ByteArray(img.size)

        TrilinearInterpolator.applyLutTrilinear(img, width, height, identity, lutSize, out)

        for (i in img.indices) {
            val original = img[i].toInt() and 0xFF
            val result = out[i].toInt() and 0xFF
            assertTrue(kotlin.math.abs(original - result) <= 1, "Off by more than 1 at $i: $original vs $result")
        }
    }
}
