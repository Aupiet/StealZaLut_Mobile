package com.stealzalut.engine.lut

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure Kotlin LUT post-processing matching stealzalut.core.lut_engine.refine_and_blend_lut.
 * Bit-exact with NumPy operations on 3D LUT volumes.
 */
object LutRefiner {

    const val DEFAULT_INTENSITY: Float = 1.0f
    const val DEFAULT_SKIN_PROTECTION: Float = 0.0f
    const val DEFAULT_SMOOTH_SIGMA: Float = 0.0f
    const val DEFAULT_NEUTRAL_LOCK: Float = 0.0f

    const val SKIN_CB_CENTER: Float = 0.40f
    const val SKIN_CR_CENTER: Float = 0.60f
    const val SKIN_CB_SIGMA: Float = 0.08f
    const val SKIN_CR_SIGMA: Float = 0.08f
    const val NEUTRAL_LOCK_SIGMA: Float = 0.08f

    fun refineAndBlendLut(
        rawLut: FloatArray,
        lutSize: Int,
        intensity: Float = DEFAULT_INTENSITY,
        skinProtection: Float = DEFAULT_SKIN_PROTECTION,
        smoothSigma: Float = DEFAULT_SMOOTH_SIGMA,
        neutralLock: Float = DEFAULT_NEUTRAL_LOCK
    ): FloatArray {
        val count = lutSize * lutSize * lutSize
        require(rawLut.size == count * 3) {
            "LUT size mismatch: expected ${count * 3} floats, got ${rawLut.size}"
        }

        val identity = LutGenerator.generateIdentityGrid(lutSize)
        val clampedIntensity = intensity.coerceIn(0.0f, 1.0f)
        if (clampedIntensity == 0.0f) {
            return identity
        }

        var lut3d = rawLut.copyOf()

        // 2. Protection of Skin Tones
        val clampedSkin = skinProtection.coerceIn(0.0f, 1.0f)
        if (clampedSkin > 0.0f) {
            val step = if (lutSize > 1) 1.0f / (lutSize - 1).toFloat() else 0.0f
            var idx = 0
            for (b in 0 until lutSize) {
                val bVal = b * step
                for (g in 0 until lutSize) {
                    val gVal = g * step
                    for (r in 0 until lutSize) {
                        val rVal = r * step

                        // BT.601 YCbCr
                        val y = 0.299f * rVal + 0.587f * gVal + 0.114f * bVal
                        val cb = -0.168736f * rVal - 0.331264f * gVal + 0.5f * bVal + 0.5f
                        val cr = 0.5f * rVal - 0.418688f * gVal - 0.081312f * bVal + 0.5f

                        val dCb = (cb - SKIN_CB_CENTER) / SKIN_CB_SIGMA
                        val dCr = (cr - SKIN_CR_CENTER) / SKIN_CR_SIGMA
                        val chromaMask = exp(-0.5f * (dCb * dCb + dCr * dCr))

                        val rgSmooth = ((rVal - gVal) / 0.05f).coerceIn(0.0f, 1.0f)
                        val gbSmooth = ((gVal - bVal) / 0.05f).coerceIn(0.0f, 1.0f)
                        val yWindow = ((y - 0.08f) / 0.08f).coerceIn(0.0f, 1.0f) *
                                ((0.98f - y) / 0.08f).coerceIn(0.0f, 1.0f)

                        val skinMask = chromaMask * rgSmooth * gbSmooth * yWindow
                        val skinBlend = (clampedSkin * skinMask).coerceIn(0.0f, 1.0f)
                        val oneMinusBlend = 1.0f - skinBlend

                        lut3d[idx] = oneMinusBlend * lut3d[idx] + skinBlend * identity[idx]
                        lut3d[idx + 1] = oneMinusBlend * lut3d[idx + 1] + skinBlend * identity[idx + 1]
                        lut3d[idx + 2] = oneMinusBlend * lut3d[idx + 2] + skinBlend * identity[idx + 2]

                        idx += 3
                    }
                }
            }
        }

        // 3. Neutral Axis / Black & White Lock
        val clampedLock = neutralLock.coerceIn(0.0f, 1.0f)
        if (clampedLock > 0.0f) {
            val step = if (lutSize > 1) 1.0f / (lutSize - 1).toFloat() else 0.0f
            val sqrt3 = sqrt(3.0f)
            var idx = 0
            for (b in 0 until lutSize) {
                val bVal = b * step
                for (g in 0 until lutSize) {
                    val gVal = g * step
                    for (r in 0 until lutSize) {
                        val rVal = r * step

                        val dBlack = sqrt(rVal * rVal + gVal * gVal + bVal * bVal) / sqrt3
                        val oneMinusR = 1.0f - rVal
                        val oneMinusG = 1.0f - gVal
                        val oneMinusB = 1.0f - bVal
                        val dWhite = sqrt(oneMinusR * oneMinusR + oneMinusG * oneMinusG + oneMinusB * oneMinusB) / sqrt3

                        val wBlack = exp(-0.5f * (dBlack / NEUTRAL_LOCK_SIGMA) * (dBlack / NEUTRAL_LOCK_SIGMA))
                        val wWhite = exp(-0.5f * (dWhite / NEUTRAL_LOCK_SIGMA) * (dWhite / NEUTRAL_LOCK_SIGMA))
                        val wNeutral = (clampedLock * max(wBlack, wWhite)).coerceIn(0.0f, 1.0f)
                        val oneMinusW = 1.0f - wNeutral

                        lut3d[idx] = oneMinusW * lut3d[idx] + wNeutral * identity[idx]
                        lut3d[idx + 1] = oneMinusW * lut3d[idx + 1] + wNeutral * identity[idx + 1]
                        lut3d[idx + 2] = oneMinusW * lut3d[idx + 2] + wNeutral * identity[idx + 2]

                        idx += 3
                    }
                }
            }
        }

        // 4. 3D Spatial Grid Smoothing
        if (smoothSigma > 1e-4f) {
            lut3d = gaussianSmooth3D(lut3d, lutSize, smoothSigma)
        }

        // 5. Direct Intensity Integration
        val oneMinusIntensity = 1.0f - clampedIntensity
        val result = FloatArray(count * 3)
        for (i in 0 until count * 3) {
            val v = oneMinusIntensity * identity[i] + clampedIntensity * lut3d[i]
            result[i] = v.coerceIn(0.0f, 1.0f)
        }

        return result
    }

    private fun gaussianSmooth3D(grid: FloatArray, size: Int, sigma: Float): FloatArray {
        val radius = max(1, ceil(2.0f * sigma).toInt())
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

        var current = grid.copyOf()
        val temp = FloatArray(grid.size)

        // Axis 0 (B dimension)
        val n2 = size * size
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val outBase = (b * n2 + g * size + r) * 3
                    for (c in 0 until 3) {
                        var acc = 0.0f
                        for (k in -radius..radius) {
                            val inB = (b + k).coerceIn(0, size - 1)
                            val inIdx = (inB * n2 + g * size + r) * 3 + c
                            acc += current[inIdx] * kernel[k + radius]
                        }
                        temp[outBase + c] = acc
                    }
                }
            }
        }
        current = temp.copyOf()

        // Axis 1 (G dimension)
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val outBase = (b * n2 + g * size + r) * 3
                    for (c in 0 until 3) {
                        var acc = 0.0f
                        for (k in -radius..radius) {
                            val inG = (g + k).coerceIn(0, size - 1)
                            val inIdx = (b * n2 + inG * size + r) * 3 + c
                            acc += current[inIdx] * kernel[k + radius]
                        }
                        temp[outBase + c] = acc
                    }
                }
            }
        }
        current = temp.copyOf()

        // Axis 2 (R dimension)
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val outBase = (b * n2 + g * size + r) * 3
                    for (c in 0 until 3) {
                        var acc = 0.0f
                        for (k in -radius..radius) {
                            val inR = (r + k).coerceIn(0, size - 1)
                            val inIdx = (b * n2 + g * size + inR) * 3 + c
                            acc += current[inIdx] * kernel[k + radius]
                        }
                        temp[outBase + c] = acc
                    }
                }
            }
        }

        return temp
    }
}
