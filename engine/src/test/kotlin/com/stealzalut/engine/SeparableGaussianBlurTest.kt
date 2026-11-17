package com.stealzalut.engine

import com.stealzalut.engine.spatial.SeparableGaussianBlur
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeparableGaussianBlurTest {

    @Test
    fun `zero sigma leaves input unchanged`() = runBlocking {
        val width = 16
        val height = 16
        val input = FloatArray(width * height * 3) { it.toFloat() / (width * height * 3) }
        val output = FloatArray(input.size)

        SeparableGaussianBlur.blur(input, width, height, sigma = 0.0f, output = output)

        for (i in input.indices) {
            assertEquals(input[i], output[i], 1e-6f)
        }
    }

    @Test
    fun `uniform image blur preserves mean value`() = runBlocking {
        val width = 32
        val height = 32
        val input = FloatArray(width * height * 3) { 0.5f }
        val output = FloatArray(input.size)

        SeparableGaussianBlur.blur(input, width, height, sigma = 2.0f, output = output)

        for (i in output.indices) {
            assertEquals(0.5f, output[i], 1e-4f)
        }
    }
}
