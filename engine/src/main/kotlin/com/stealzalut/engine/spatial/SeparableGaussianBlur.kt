package com.stealzalut.engine.spatial

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.exp

/**
 * 2D Separable Gaussian blur matching SciPy ndimage gaussian_filter(mode="reflect").
 * Band-based multi-threaded execution with cooperative cancellation.
 */
object SeparableGaussianBlur {

    suspend fun blur(
        input: FloatArray,
        width: Int,
        height: Int,
        sigma: Float,
        output: FloatArray,
        channels: Int = 3,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bandHeight: Int = 128
    ) = coroutineScope {
        if (sigma <= 1e-4f) {
            System.arraycopy(input, 0, output, 0, input.size)
            return@coroutineScope
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

        // Pass 1: Horizontal blur in parallel bands
        val numThreads = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
        val bandSize = maxOf(bandHeight, (height + numThreads - 1) / numThreads)

        val pass1Jobs = (0 until height step bandSize).map { yStart ->
            val yEnd = minOf(yStart + bandSize, height)
            launch(dispatcher) {
                for (y in yStart until yEnd) {
                    ensureActive()
                    val rowOffset = y * width * channels
                    for (x in 0 until width) {
                        val outIdx = rowOffset + x * channels
                        for (c in 0 until channels) {
                            var chSum = 0.0f
                            for (k in -radius..radius) {
                                var ix = x + k
                                if (ix < 0) ix = -ix - 1
                                else if (ix >= width) ix = 2 * width - ix - 1
                                if (ix < 0) ix = 0 else if (ix >= width) ix = width - 1

                                val inIdx = rowOffset + ix * channels + c
                                chSum += input[inIdx] * kernel[k + radius]
                            }
                            temp[outIdx + c] = chSum
                        }
                    }
                }
            }
        }
        pass1Jobs.forEach { it.join() }

        // Pass 2: Vertical blur in parallel bands
        val pass2Jobs = (0 until height step bandSize).map { yStart ->
            val yEnd = minOf(yStart + bandSize, height)
            launch(dispatcher) {
                for (y in yStart until yEnd) {
                    ensureActive()
                    val rowOffset = y * width * channels
                    for (x in 0 until width) {
                        val outIdx = rowOffset + x * channels
                        for (c in 0 until channels) {
                            var chSum = 0.0f
                            for (k in -radius..radius) {
                                var iy = y + k
                                if (iy < 0) iy = -iy - 1
                                else if (iy >= height) iy = 2 * height - iy - 1
                                if (iy < 0) iy = 0 else if (iy >= height) iy = height - 1

                                val inIdx = (iy * width + x) * channels + c
                                chSum += temp[inIdx] * kernel[k + radius]
                            }
                            output[outIdx + c] = chSum
                        }
                    }
                }
            }
        }
        pass2Jobs.forEach { it.join() }
    }
}
