package com.stealzalut.engine.lut

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * High-performance pure-Kotlin trilinear 3D LUT interpolator.
 * Memory-safe band processing with cooperative coroutine cancellation.
 */
object TrilinearInterpolator {

    suspend fun applyLutTrilinear(
        image: ByteArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: ByteArray,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bandHeight: Int = 128
    ) = coroutineScope {
        val scale = (lutSize - 1).toFloat()
        val n2 = lutSize * lutSize
        val maxCoord = lutSize - 2
        val rowStride = width * 3

        val numThreads = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
        val bandSize = maxOf(bandHeight, (height + numThreads - 1) / numThreads)

        val jobs = (0 until height step bandSize).map { yStart ->
            val yEnd = minOf(yStart + bandSize, height)
            launch(dispatcher) {
                for (y in yStart until yEnd) {
                    ensureActive()
                    val rowOffset = y * rowStride
                    for (x in 0 until width) {
                        val px = rowOffset + x * 3

                        val rNorm = (image[px].toInt() and 0xFF) * (1.0f / 255.0f)
                        val gNorm = (image[px + 1].toInt() and 0xFF) * (1.0f / 255.0f)
                        val bNorm = (image[px + 2].toInt() and 0xFF) * (1.0f / 255.0f)

                        val rScaled = rNorm * scale
                        val gScaled = gNorm * scale
                        val bScaled = bNorm * scale

                        var x0 = rScaled.toInt()
                        if (x0 < 0) x0 = 0 else if (x0 > maxCoord) x0 = maxCoord
                        val dx = rScaled - x0

                        var y0 = gScaled.toInt()
                        if (y0 < 0) y0 = 0 else if (y0 > maxCoord) y0 = maxCoord
                        val dy = gScaled - y0

                        var z0 = bScaled.toInt()
                        if (z0 < 0) z0 = 0 else if (z0 > maxCoord) z0 = maxCoord
                        val dz = bScaled - z0

                        val base = (z0 * n2 + y0 * lutSize + x0) * 3
                        val basePlus1 = base + 3
                        val basePlusN = (z0 * n2 + (y0 + 1) * lutSize + x0) * 3
                        val basePlusN1 = basePlusN + 3
                        val basePlusN2 = ((z0 + 1) * n2 + y0 * lutSize + x0) * 3
                        val basePlusN21 = basePlusN2 + 3
                        val basePlusN2N = ((z0 + 1) * n2 + (y0 + 1) * lutSize + x0) * 3
                        val basePlusN2N1 = basePlusN2N + 3

                        val oneMinusDx = 1.0f - dx
                        val oneMinusDy = 1.0f - dy
                        val oneMinusDz = 1.0f - dz

                        for (c in 0 until 3) {
                            val c00 = oneMinusDx * lut[base + c] + dx * lut[basePlus1 + c]
                            val c01 = oneMinusDx * lut[basePlusN + c] + dx * lut[basePlusN1 + c]
                            val c10 = oneMinusDx * lut[basePlusN2 + c] + dx * lut[basePlusN21 + c]
                            val c11 = oneMinusDx * lut[basePlusN2N + c] + dx * lut[basePlusN2N1 + c]

                            val c0 = oneMinusDy * c00 + dy * c01
                            val c1 = oneMinusDy * c10 + dy * c11

                            val v = oneMinusDz * c0 + dz * c1
                            val clamped = when {
                                v <= 0.0f -> 0
                                v >= 1.0f -> 255
                                else -> (v * 255.0f + 0.5f).toInt()
                            }
                            output[px + c] = clamped.toByte()
                        }
                    }
                }
            }
        }
        jobs.forEach { it.join() }
    }

    suspend fun applyLutTrilinearFloats(
        image: FloatArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: FloatArray,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bandHeight: Int = 128
    ) = coroutineScope {
        val scale = (lutSize - 1).toFloat()
        val n2 = lutSize * lutSize
        val maxCoord = lutSize - 2
        val rowStride = width * 3

        val numThreads = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
        val bandSize = maxOf(bandHeight, (height + numThreads - 1) / numThreads)

        val jobs = (0 until height step bandSize).map { yStart ->
            val yEnd = minOf(yStart + bandSize, height)
            launch(dispatcher) {
                for (y in yStart until yEnd) {
                    ensureActive()
                    val rowOffset = y * rowStride
                    for (x in 0 until width) {
                        val px = rowOffset + x * 3

                        val rScaled = image[px] * scale
                        val gScaled = image[px + 1] * scale
                        val bScaled = image[px + 2] * scale

                        var x0 = rScaled.toInt()
                        if (x0 < 0) x0 = 0 else if (x0 > maxCoord) x0 = maxCoord
                        val dx = rScaled - x0

                        var y0 = gScaled.toInt()
                        if (y0 < 0) y0 = 0 else if (y0 > maxCoord) y0 = maxCoord
                        val dy = gScaled - y0

                        var z0 = bScaled.toInt()
                        if (z0 < 0) z0 = 0 else if (z0 > maxCoord) z0 = maxCoord
                        val dz = bScaled - z0

                        val base = (z0 * n2 + y0 * lutSize + x0) * 3
                        val basePlus1 = base + 3
                        val basePlusN = (z0 * n2 + (y0 + 1) * lutSize + x0) * 3
                        val basePlusN1 = basePlusN + 3
                        val basePlusN2 = ((z0 + 1) * n2 + y0 * lutSize + x0) * 3
                        val basePlusN21 = basePlusN2 + 3
                        val basePlusN2N = ((z0 + 1) * n2 + (y0 + 1) * lutSize + x0) * 3
                        val basePlusN2N1 = basePlusN2N + 3

                        val oneMinusDx = 1.0f - dx
                        val oneMinusDy = 1.0f - dy
                        val oneMinusDz = 1.0f - dz

                        for (c in 0 until 3) {
                            val c00 = oneMinusDx * lut[base + c] + dx * lut[basePlus1 + c]
                            val c01 = oneMinusDx * lut[basePlusN + c] + dx * lut[basePlusN1 + c]
                            val c10 = oneMinusDx * lut[basePlusN2 + c] + dx * lut[basePlusN21 + c]
                            val c11 = oneMinusDx * lut[basePlusN2N + c] + dx * lut[basePlusN2N1 + c]

                            val c0 = oneMinusDy * c00 + dy * c01
                            val c1 = oneMinusDy * c10 + dy * c11

                            val v = oneMinusDz * c0 + dz * c1
                            output[px + c] = when {
                                v <= 0.0f -> 0.0f
                                v >= 1.0f -> 1.0f
                                else -> v
                            }
                        }
                    }
                }
            }
        }
        jobs.forEach { it.join() }
    }
}
