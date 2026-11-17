package com.stealzalut.engine.spatial

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.exp

object SeparableGaussianBlur {

    suspend fun blur(
        input: FloatArray,
        width: Int,
        height: Int,
        sigma: Float,
        output: FloatArray,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bandHeight: Int = 128
    ) = withContext(dispatcher) {
        if (sigma <= 1e-4f) {
            System.arraycopy(input, 0, output, 0, input.size)
            return@withContext
        }

        val radius = ceil(4.0f * sigma).toInt()
        val kSize = 2 * radius + 1
        val kernel = FloatArray(kSize)
        var sum = 0.0f
        val twoSigmaSq = 2.0f * sigma * sigma

        for (i in 0 until kSize) {
            val d = (i - radius).toFloat()
            val w = exp(-(d * d) / twoSigmaSq)
            kernel[i] = w
            sum += w
        }
        val invSum = 1.0f / sum
        for (i in 0 until kSize) {
            kernel[i] *= invSum
        }

        val temp = FloatArray(input.size)

        // Pass 1: Horizontal blur in bands
        var yStart = 0
        while (yStart < height) {
            coroutineContext.ensureActive()
            val yEnd = minOf(yStart + bandHeight, height)

            for (y in yStart until yEnd) {
                val rowOffset = y * width * 3
                for (x in 0 until width) {
                    val outIdx = rowOffset + x * 3
                    var rSum = 0.0f
                    var gSum = 0.0f
                    var bSum = 0.0f

                    for (k in -radius..radius) {
                        var ix = x + k
                        if (ix < 0) ix = -ix - 1
                        else if (ix >= width) ix = 2 * width - ix - 1
                        if (ix < 0) ix = 0 else if (ix >= width) ix = width - 1

                        val inIdx = rowOffset + ix * 3
                        val wgt = kernel[k + radius]
                        rSum += input[inIdx] * wgt
                        gSum += input[inIdx + 1] * wgt
                        bSum += input[inIdx + 2] * wgt
                    }
                    temp[outIdx] = rSum
                    temp[outIdx + 1] = gSum
                    temp[outIdx + 2] = bSum
                }
            }
            yStart = yEnd
        }

        // Pass 2: Vertical blur in bands
        yStart = 0
        while (yStart < height) {
            coroutineContext.ensureActive()
            val yEnd = minOf(yStart + bandHeight, height)

            for (y in yStart until yEnd) {
                val rowOffset = y * width * 3
                for (x in 0 until width) {
                    val outIdx = rowOffset + x * 3
                    var rSum = 0.0f
                    var gSum = 0.0f
                    var bSum = 0.0f

                    for (k in -radius..radius) {
                        var iy = y + k
                        if (iy < 0) iy = -iy - 1
                        else if (iy >= height) iy = 2 * height - iy - 1
                        if (iy < 0) iy = 0 else if (iy >= height) iy = height - 1

                        val inIdx = (iy * width + x) * 3
                        val wgt = kernel[k + radius]
                        rSum += temp[inIdx] * wgt
                        gSum += temp[inIdx + 1] * wgt
                        bSum += temp[inIdx + 2] * wgt
                    }
                    output[outIdx] = rSum
                    output[outIdx + 1] = gSum
                    output[outIdx + 2] = bSum
                }
            }
            yStart = yEnd
        }
    }
}
