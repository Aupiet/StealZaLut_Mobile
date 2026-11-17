package com.stealzalut.engine.autoedit

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.model.BasicSettings
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

object AutoEdit {

    private const val REC2020_R = 0.2627f
    private const val REC2020_G = 0.6780f
    private const val REC2020_B = 0.0593f

    private const val REF_MIDDLE_GRAY = 0.18f
    private const val EPSILON_LUM = 1e-6f
    private const val EXPOSURE_SLOPE = -0.41f
    private const val EXPOSURE_INTERCEPT = -0.58f
    private const val EXPOSURE_MIN = -2.0f
    private const val EXPOSURE_MAX = 3.0f

    const val DEFAULT_AUTO_CONTRAST = 68.0f
    const val DEFAULT_AUTO_HIGHLIGHTS = -66.0f
    const val DEFAULT_AUTO_SHADOWS = 52.0f
    const val DEFAULT_AUTO_WHITES = 0.0f
    const val DEFAULT_AUTO_BLACKS = 0.0f
    const val DEFAULT_AUTO_VIBRANCE = 30.0f
    const val DEFAULT_AUTO_SATURATION = 0.0f

    private val LN2 = ln(2.0).toFloat()

    fun calculateAutoExposure(
        image: FloatArray,
        width: Int,
        height: Int,
        maxDim: Int = 512
    ): Float {
        val maxSide = max(width, height)
        val step = if (maxSide > maxDim) {
            val scale = maxDim.toFloat() / maxSide.toFloat()
            max(1, (1.0f / scale).roundToInt())
        } else {
            1
        }

        var sumV = 0.0
        var count = 0

        var y = 0
        while (y < height) {
            val rowOffset = y * width * 3
            var x = 0
            while (x < width) {
                val px = rowOffset + x * 3
                val r = ColorSpace.srgbToLinear(image[px])
                val g = ColorSpace.srgbToLinear(image[px + 1])
                val b = ColorSpace.srgbToLinear(image[px + 2])

                val lum = REC2020_R * r + REC2020_G * g + REC2020_B * b
                val lumClamped = max(lum, EPSILON_LUM)
                val v = (ln((lumClamped / REF_MIDDLE_GRAY).toDouble()) / ln(2.0)).toFloat()
                sumV += v
                count++
                x += step
            }
            y += step
        }

        if (count == 0) return 0.0f
        val vMean = (sumV / count).toFloat()
        val expVal = EXPOSURE_SLOPE * vMean + EXPOSURE_INTERCEPT
        val expClamped = expVal.coerceIn(EXPOSURE_MIN, EXPOSURE_MAX)
        return (expClamped * 100.0f).roundToInt() / 100.0f
    }

    fun getAutoEditSettings(exposure: Float): BasicSettings {
        return BasicSettings(
            exposure = exposure,
            contrast = DEFAULT_AUTO_CONTRAST,
            highlights = DEFAULT_AUTO_HIGHLIGHTS,
            shadows = DEFAULT_AUTO_SHADOWS,
            whites = DEFAULT_AUTO_WHITES,
            blacks = DEFAULT_AUTO_BLACKS,
            vibrance = DEFAULT_AUTO_VIBRANCE,
            saturation = DEFAULT_AUTO_SATURATION
        )
    }
}
