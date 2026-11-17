package com.stealzalut.engine.spatial

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.model.BasicSettings
import com.stealzalut.engine.model.DetailSettings
import kotlinx.coroutines.runBlocking
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object SpatialFilters {

    fun gaussianBlur2D(
        input: FloatArray,
        width: Int,
        height: Int,
        sigma: Float,
        channels: Int = 3,
        output: FloatArray = FloatArray(input.size)
    ): FloatArray {
        runBlocking {
            SeparableGaussianBlur.blur(
                input = input,
                width = width,
                height = height,
                sigma = sigma,
                output = output,
                channels = channels
            )
        }
        return output
    }

    fun minFilter2D(
        input: FloatArray,
        width: Int,
        height: Int,
        size: Int,
        output: FloatArray = FloatArray(width * height)
    ): FloatArray {
        val radius = max(1, size) / 2
        val temp = FloatArray(width * height)

        // Pass 1: Horizontal min with reflect boundary
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var minVal = Float.MAX_VALUE
                for (k in -radius..radius) {
                    var ix = x + k
                    if (ix < 0) ix = -ix - 1
                    else if (ix >= width) ix = 2 * width - ix - 1
                    if (ix < 0) ix = 0 else if (ix >= width) ix = width - 1

                    val v = input[rowOffset + ix]
                    if (v < minVal) minVal = v
                }
                temp[rowOffset + x] = minVal
            }
        }

        // Pass 2: Vertical min with reflect boundary
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var minVal = Float.MAX_VALUE
                for (k in -radius..radius) {
                    var iy = y + k
                    if (iy < 0) iy = -iy - 1
                    else if (iy >= height) iy = 2 * height - iy - 1
                    if (iy < 0) iy = 0 else if (iy >= height) iy = height - 1

                    val v = temp[iy * width + x]
                    if (v < minVal) minVal = v
                }
                output[rowOffset + x] = minVal
            }
        }

        return output
    }

    fun sobelMagnitude2D(
        gray: FloatArray,
        width: Int,
        height: Int,
        output: FloatArray = FloatArray(width * height)
    ): FloatArray {
        // SciPy Sobel:
        // Gx: Kx = [[-1, 0, 1], [-2, 0, 2], [-1, 0, 1]] / 8.0
        // Gy: Ky = [[-1, -2, -1], [0, 0, 0], [1, 2, 1]] / 8.0
        fun getReflect(x: Int, y: Int): Float {
            var rx = x
            if (rx < 0) rx = -rx - 1
            else if (rx >= width) rx = 2 * width - rx - 1
            if (rx < 0) rx = 0 else if (rx >= width) rx = width - 1

            var ry = y
            if (ry < 0) ry = -ry - 1
            else if (ry >= height) ry = 2 * height - ry - 1
            if (ry < 0) ry = 0 else if (ry >= height) ry = height - 1

            return gray[ry * width + rx]
        }

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val p00 = getReflect(x - 1, y - 1)
                val p01 = getReflect(x, y - 1)
                val p02 = getReflect(x + 1, y - 1)

                val p10 = getReflect(x - 1, y)
                val p12 = getReflect(x + 1, y)

                val p20 = getReflect(x - 1, y + 1)
                val p21 = getReflect(x, y + 1)
                val p22 = getReflect(x + 1, y + 1)

                val gx = (-p00 + p02 - 2.0f * p10 + 2.0f * p12 - p20 + p22) * 0.125f
                val gy = (-p00 - 2.0f * p01 - p02 + p20 + 2.0f * p21 + p22) * 0.125f

                output[rowOffset + x] = sqrt(gx * gx + gy * gy)
            }
        }
        return output
    }

    fun applyPresenceSpatial(
        rgb: FloatArray,
        width: Int,
        height: Int,
        s: BasicSettings,
        output: FloatArray = FloatArray(rgb.size)
    ): FloatArray {
        for (i in rgb.indices) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }
        if (min(width, height) < 16) return output

        val count = width * height

        // A. Texture
        if (abs(s.texture) > 1e-4f) {
            val blurFine = gaussianBlur2D(output, width, height, 1.2f, 3)
            val blurMedium = gaussianBlur2D(output, width, height, 3.5f, 3)
            val mult = (s.texture / 100.0f) * 0.45f
            for (i in output.indices) {
                val midFreq = blurFine[i] - blurMedium[i]
                output[i] = (output[i] + mult * midFreq).coerceIn(0.0f, 1.0f)
            }
        }

        // B. Clarity
        if (abs(s.clarity) > 1e-4f) {
            val sigmaClarity = max(3.0f, min(width, height).toFloat() / 40.0f)
            val blurClarity = gaussianBlur2D(output, width, height, sigmaClarity, 3)
            val mult = (s.clarity / 100.0f) * 0.40f

            var idx = 0
            for (p in 0 until count) {
                val r = output[idx]
                val g = output[idx + 1]
                val b = output[idx + 2]

                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val diffLum = lum - 0.5f
                val midtoneW = (1.0f - 4.0f * diffLum * diffLum).coerceIn(0.0f, 1.0f)
                val factor = mult * midtoneW

                output[idx] = (r + factor * (r - blurClarity[idx])).coerceIn(0.0f, 1.0f)
                output[idx + 1] = (g + factor * (g - blurClarity[idx + 1])).coerceIn(0.0f, 1.0f)
                output[idx + 2] = (b + factor * (b - blurClarity[idx + 2])).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        // C. Dehaze
        if (abs(s.dehaze) > 1e-4f) {
            val dVal = s.dehaze / 100.0f
            val darkCh = FloatArray(count)
            var idx = 0
            for (p in 0 until count) {
                darkCh[p] = min(output[idx], min(output[idx + 1], output[idx + 2]))
                idx += 3
            }

            val kSize = max(5, (min(width, height) / 30) or 1)
            val darkMin = minFilter2D(darkCh, width, height, kSize)

            // Percentile 99.5 per channel for atmLight
            val rVals = FloatArray(count) { output[it * 3] }
            val gVals = FloatArray(count) { output[it * 3 + 1] }
            val bVals = FloatArray(count) { output[it * 3 + 2] }
            Arrays.sort(rVals)
            Arrays.sort(gVals)
            Arrays.sort(bVals)
            val pIdx = min(count - 1, (count * 0.995f).toInt())
            val atmR = rVals[pIdx]
            val atmG = gVals[pIdx]
            val atmB = bVals[pIdx]

            idx = 0
            for (p in 0 until count) {
                val trans = (1.0f - 0.95f * darkMin[p]).coerceIn(0.1f, 1.0f)
                if (dVal > 0.0f) {
                    val denom = max(trans.pow(dVal * 0.8f), 0.15f)
                    output[idx] = ((output[idx] - atmR) / denom + atmR).coerceIn(0.0f, 1.0f)
                    output[idx + 1] = ((output[idx + 1] - atmG) / denom + atmG).coerceIn(0.0f, 1.0f)
                    output[idx + 2] = ((output[idx + 2] - atmB) / denom + atmB).coerceIn(0.0f, 1.0f)
                } else {
                    val fog = abs(dVal) * (1.0f - trans) * 0.6f
                    output[idx] = (output[idx] * (1.0f - fog) + atmR * fog).coerceIn(0.0f, 1.0f)
                    output[idx + 1] = (output[idx + 1] * (1.0f - fog) + atmG * fog).coerceIn(0.0f, 1.0f)
                    output[idx + 2] = (output[idx + 2] * (1.0f - fog) + atmB * fog).coerceIn(0.0f, 1.0f)
                }
                idx += 3
            }
        }

        return output
    }

    fun applyDetailSpatial(
        rgb: FloatArray,
        width: Int,
        height: Int,
        s: DetailSettings,
        output: FloatArray = FloatArray(rgb.size)
    ): FloatArray {
        for (i in rgb.indices) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }
        if (min(width, height) < 16) return output

        val count = width * height

        // A. Noise Reduction
        if (s.nrLuminance > 1e-4f || s.nrColor > 1e-4f) {
            val oklab = FloatArray(output.size)
            val oklabPx = FloatArray(3)
            var idx = 0
            for (p in 0 until count) {
                ColorSpace.rgbToOklab(output[idx], output[idx + 1], output[idx + 2], oklabPx, 0)
                oklab[idx] = oklabPx[0]
                oklab[idx + 1] = oklabPx[1]
                oklab[idx + 2] = oklabPx[2]
                idx += 3
            }

            if (s.nrLuminance > 1e-4f) {
                val lCh = FloatArray(count) { oklab[it * 3] }
                val sigmaS = 1.0f + (s.nrLuminance / 100.0f) * 3.5f
                val lBlur = gaussianBlur2D(lCh, width, height, sigmaS, 1)
                val threshold = 0.04f + 0.12f * (1.0f - s.nrLumDetail / 100.0f)
                val alphaL = (s.nrLuminance / 100.0f) * 0.85f

                for (p in 0 until count) {
                    val origL = lCh[p]
                    val blurL = lBlur[p]
                    val diff = abs(origL - blurL)
                    val edgeWeight = ColorSpace.smoothstep(threshold * 0.5f, threshold * 1.5f, diff)
                    oklab[p * 3] = (1.0f - alphaL * (1.0f - edgeWeight)) * origL + alphaL * (1.0f - edgeWeight) * blurL
                }
            }

            if (s.nrColor > 1e-4f) {
                val aCh = FloatArray(count) { oklab[it * 3 + 1] }
                val bCh = FloatArray(count) { oklab[it * 3 + 2] }
                val sigmaC = 1.0f + (s.nrColor / 100.0f) * 4.0f
                val aBlur = gaussianBlur2D(aCh, width, height, sigmaC, 1)
                val bBlur = gaussianBlur2D(bCh, width, height, sigmaC, 1)
                for (p in 0 until count) {
                    oklab[p * 3 + 1] = aBlur[p]
                    oklab[p * 3 + 2] = bBlur[p]
                }
            }

            val rgbPx = FloatArray(3)
            idx = 0
            for (p in 0 until count) {
                ColorSpace.oklabToRgb(oklab[idx], oklab[idx + 1], oklab[idx + 2], rgbPx, 0)
                output[idx] = rgbPx[0].coerceIn(0.0f, 1.0f)
                output[idx + 1] = rgbPx[1].coerceIn(0.0f, 1.0f)
                output[idx + 2] = rgbPx[2].coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        // B. Sharpening
        if (s.sharpenAmount > 1e-4f) {
            val rad = max(0.5f, s.sharpenRadius)
            val blur = gaussianBlur2D(output, width, height, rad, 3)

            val mask = if (s.sharpenMasking > 1e-4f) {
                val gray = FloatArray(count)
                var idx = 0
                for (p in 0 until count) {
                    gray[p] = 0.299f * output[idx] + 0.587f * output[idx + 1] + 0.114f * output[idx + 2]
                    idx += 3
                }
                val gradMag = sobelMagnitude2D(gray, width, height)
                val thresh = (s.sharpenMasking / 100.0f) * 0.4f
                FloatArray(count) { p ->
                    ColorSpace.smoothstep(thresh * 0.5f, thresh + 0.1f, gradMag[p])
                }
            } else null

            val detailWeight = 0.5f + 0.5f * (s.sharpenDetail / 100.0f)
            val gain = (s.sharpenAmount / 100.0f) * 0.70f * detailWeight

            var idx = 0
            for (p in 0 until count) {
                val m = mask?.get(p) ?: 1.0f
                val g = gain * m
                output[idx] = (output[idx] + g * (output[idx] - blur[idx])).coerceIn(0.0f, 1.0f)
                output[idx + 1] = (output[idx + 1] + g * (output[idx + 1] - blur[idx + 1])).coerceIn(0.0f, 1.0f)
                output[idx + 2] = (output[idx + 2] + g * (output[idx + 2] - blur[idx + 2])).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        return output
    }
}
