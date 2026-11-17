package com.stealzalut.engine.lightroom

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.model.BasicSettings
import com.stealzalut.engine.model.CalibrationSettings
import com.stealzalut.engine.model.ColorGradingSettings
import com.stealzalut.engine.model.ColorMixerSettings
import com.stealzalut.engine.model.ToneCurveSettings
import com.stealzalut.engine.spline.SplineInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

object LightroomColorPipeline {

    private const val EPSILON = 1e-6f

    fun applyBasicAdjustments(
        rgb: FloatArray,
        count: Int,
        s: BasicSettings,
        output: FloatArray = FloatArray(count * 3)
    ): FloatArray {
        for (i in 0 until count * 3) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }

        // A. Exposition en espace linéaire EV
        if (abs(s.exposure) > 1e-4f) {
            val expFactor = 2.0f.pow(s.exposure)
            for (i in 0 until count * 3) {
                val lin = ColorSpace.srgbToLinear(output[i]) * expFactor
                output[i] = ColorSpace.linearToSrgb(lin).coerceIn(0.0f, 1.0f)
            }
        }

        // B. Balance des blancs
        if (abs(s.temp) > 1e-4f || abs(s.tint) > 1e-4f) {
            val t = s.temp / 100.0f
            val ti = s.tint / 100.0f
            val rMult = 1.0f + 0.28f * t + 0.08f * ti
            val gMult = 1.0f - 0.18f * ti
            val bMult = 1.0f - 0.28f * t + 0.08f * ti

            var idx = 0
            for (p in 0 until count) {
                output[idx] = (output[idx] * rMult).coerceIn(0.0f, 1.0f)
                output[idx + 1] = (output[idx + 1] * gMult).coerceIn(0.0f, 1.0f)
                output[idx + 2] = (output[idx + 2] * bMult).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        // C. Tonalité
        val hasContrast = abs(s.contrast) > 1e-4f
        val hasHighlights = abs(s.highlights) > 1e-4f
        val hasShadows = abs(s.shadows) > 1e-4f
        val hasWhites = abs(s.whites) > 1e-4f
        val hasBlacks = abs(s.blacks) > 1e-4f

        if (hasContrast || hasHighlights || hasShadows || hasWhites || hasBlacks) {
            val cVal = s.contrast / 100.0f
            val cFactor = 1.0f + cVal * 0.9f
            val cNorm = 2.0f * tanh(0.75f * cFactor)
            val cSoft = 1.0f + cVal * 0.6f

            val hlMult = (s.highlights / 100.0f) * 0.32f
            val shMult = (s.shadows / 100.0f) * 0.30f
            val whMult = (s.whites / 100.0f) * 0.25f
            val blMult = (s.blacks / 100.0f) * 0.25f

            var idx = 0
            for (p in 0 until count) {
                var r = output[idx]
                var g = output[idx + 1]
                var b = output[idx + 2]

                // Contrast
                if (hasContrast) {
                    if (cVal > 0.0f) {
                        r = 0.5f + tanh((r - 0.5f) * 1.5f * cFactor) / cNorm
                        g = 0.5f + tanh((g - 0.5f) * 1.5f * cFactor) / cNorm
                        b = 0.5f + tanh((b - 0.5f) * 1.5f * cFactor) / cNorm
                    } else {
                        r = 0.5f + (r - 0.5f) * cSoft
                        g = 0.5f + (g - 0.5f) * cSoft
                        b = 0.5f + (b - 0.5f) * cSoft
                    }
                    r = r.coerceIn(0.0f, 1.0f)
                    g = g.coerceIn(0.0f, 1.0f)
                    b = b.coerceIn(0.0f, 1.0f)
                }

                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b

                // Highlights
                if (hasHighlights) {
                    val hWeight = ColorSpace.smoothstep(0.35f, 0.95f, luma).pow(1.3f)
                    val factor = hlMult * hWeight
                    r = (r + factor * r).coerceIn(0.0f, 1.0f)
                    g = (g + factor * g).coerceIn(0.0f, 1.0f)
                    b = (b + factor * b).coerceIn(0.0f, 1.0f)
                }

                // Shadows
                if (hasShadows) {
                    val sWeight = (1.0f - ColorSpace.smoothstep(0.05f, 0.65f, luma)).pow(1.3f)
                    val factor = shMult * sWeight
                    r = (r + factor * (1.0f - r)).coerceIn(0.0f, 1.0f)
                    g = (g + factor * (1.0f - g)).coerceIn(0.0f, 1.0f)
                    b = (b + factor * (1.0f - b)).coerceIn(0.0f, 1.0f)
                }

                // Whites
                if (hasWhites) {
                    val wWeight = ColorSpace.smoothstep(0.7f, 1.0f, luma).pow(2.0f)
                    val factor = whMult * wWeight
                    r = (r + factor * (1.0f - r)).coerceIn(0.0f, 1.0f)
                    g = (g + factor * (1.0f - g)).coerceIn(0.0f, 1.0f)
                    b = (b + factor * (1.0f - b)).coerceIn(0.0f, 1.0f)
                }

                // Blacks
                if (hasBlacks) {
                    val bWeight = (1.0f - ColorSpace.smoothstep(0.0f, 0.3f, luma)).pow(2.0f)
                    val factor = blMult * bWeight
                    r = (r + factor * r).coerceIn(0.0f, 1.0f)
                    g = (g + factor * g).coerceIn(0.0f, 1.0f)
                    b = (b + factor * b).coerceIn(0.0f, 1.0f)
                }

                output[idx] = r
                output[idx + 1] = g
                output[idx + 2] = b
                idx += 3
            }
        }

        // D. Vibrance & Saturation
        if (abs(s.vibrance) > 1e-4f || abs(s.saturation) > 1e-4f) {
            val vMult = (s.vibrance / 100.0f) * 0.65f
            val sMult = (s.saturation / 100.0f) * 0.55f

            var idx = 0
            for (p in 0 until count) {
                val r = output[idx]
                val g = output[idx + 1]
                val b = output[idx + 2]

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val delta = maxC - minC
                val sat = if (maxC > 1e-5f) delta / maxC else 0.0f
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                val vBoost = vMult * (1.0f - sat)
                val totalBoost = 1.0f + vBoost + sMult

                output[idx] = (lum + (r - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                output[idx + 1] = (lum + (g - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                output[idx + 2] = (lum + (b - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        return output
    }

    fun applyToneCurve(
        rgb: FloatArray,
        count: Int,
        s: ToneCurveSettings,
        output: FloatArray = FloatArray(count * 3)
    ): FloatArray {
        for (i in 0 until count * 3) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }

        if (s.mode == "parametric") {
            val sSplit = (s.shadowSplit / 100.0f).coerceIn(0.05f, 0.45f)
            val mSplit = (s.midtoneSplit / 100.0f).coerceIn(sSplit + 0.05f, 0.85f)
            val hSplit = (s.highlightSplit / 100.0f).coerceIn(mSplit + 0.05f, 0.95f)

            val dShadows = (s.shadows / 100.0f) * 0.15f
            val dDarks = (s.darks / 100.0f) * 0.18f
            val dLights = (s.lights / 100.0f) * 0.18f
            val dHighlights = (s.highlights / 100.0f) * 0.15f

            val xNodes = floatArrayOf(0.0f, sSplit, mSplit, hSplit, 1.0f)
            val yNodes = floatArrayOf(
                0.0f,
                (sSplit + dShadows + 0.5f * dDarks).coerceIn(0.01f, 0.90f),
                (mSplit + 0.5f * dDarks + 0.5f * dLights).coerceIn(0.05f, 0.95f),
                (hSplit + 0.5f * dLights + dHighlights).coerceIn(0.10f, 0.99f),
                1.0f
            )

            SplineInterpolator.evaluateMonotoneCubicSpline(xNodes, yNodes, output, output)
        } else if (s.mode == "point") {
            // Composite RGB
            if (s.rgbPoints.size >= 2) {
                val sorted = s.rgbPoints.sortedBy { it.x }
                val xPts = FloatArray(sorted.size) { sorted[it].x }
                val yPts = FloatArray(sorted.size) { sorted[it].y }
                SplineInterpolator.evaluateMonotoneCubicSpline(xPts, yPts, output, output)
            }

            // Individual R, G, B channels
            val channels = listOf(s.redPoints, s.greenPoints, s.bluePoints)
            for (ch in 0 until 3) {
                val pts = channels[ch]
                if (pts.size >= 2) {
                    val sorted = pts.sortedBy { it.x }
                    val xPts = FloatArray(sorted.size) { sorted[it].x }
                    val yPts = FloatArray(sorted.size) { sorted[it].y }

                    val chInput = FloatArray(count) { output[it * 3 + ch] }
                    val chOutput = FloatArray(count)
                    SplineInterpolator.evaluateMonotoneCubicSpline(xPts, yPts, chInput, chOutput)
                    for (i in 0 until count) {
                        output[i * 3 + ch] = chOutput[i]
                    }
                }
            }
        }

        return output
    }

    fun applyColorMixer(
        rgb: FloatArray,
        count: Int,
        s: ColorMixerSettings,
        output: FloatArray = FloatArray(count * 3)
    ): FloatArray {
        for (i in 0 until count * 3) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }

        val centers = floatArrayOf(0.0f, 30.0f, 60.0f, 120.0f, 180.0f, 240.0f, 280.0f, 320.0f)
        val hues = floatArrayOf(s.hueRed, s.hueOrange, s.hueYellow, s.hueGreen, s.hueAqua, s.hueBlue, s.huePurple, s.hueMagenta)
        val sats = floatArrayOf(s.satRed, s.satOrange, s.satYellow, s.satGreen, s.satAqua, s.satBlue, s.satPurple, s.satMagenta)
        val lums = floatArrayOf(s.lumRed, s.lumOrange, s.lumYellow, s.lumGreen, s.lumAqua, s.lumBlue, s.lumPurple, s.lumMagenta)

        val sigma = 22.0f

        var idx = 0
        for (p in 0 until count) {
            val r = output[idx]
            val g = output[idx + 1]
            val b = output[idx + 2]

            // RGB to HSL
            val cmax = max(r, max(g, b))
            val cmin = min(r, min(g, b))
            val delta = cmax - cmin

            var lum = 0.5f * (cmax + cmin)
            var sat = 0.0f
            val denom = 1.0f - abs(2.0f * lum - 1.0f)
            if (denom > EPSILON && delta > EPSILON) {
                sat = delta / denom
            }

            var hue = 0.0f
            if (delta > EPSILON) {
                if (cmax == r) {
                    hue = 60.0f * (((g - b) / delta) % 6.0f)
                } else if (cmax == g) {
                    hue = 60.0f * (((b - r) / delta) + 2.0f)
                } else {
                    hue = 60.0f * (((r - g) / delta) + 4.0f)
                }
                if (hue < 0.0f) hue += 360.0f
            }

            if (s.mode == "hsl") {
                var totalDh = 0.0f
                var totalDs = 0.0f
                var totalDl = 0.0f
                var totalW = 0.0f

                for (k in 0 until 8) {
                    val dhVal = hues[k]
                    val dsVal = sats[k]
                    val dlVal = lums[k]
                    if (abs(dhVal) < 1e-4f && abs(dsVal) < 1e-4f && abs(dlVal) < 1e-4f) continue

                    var angDiff = abs(hue - centers[k])
                    if (angDiff > 180.0f) angDiff = 360.0f - angDiff

                    val weight = exp(-0.5f * (angDiff / sigma) * (angDiff / sigma))
                    totalDh += weight * (dhVal / 100.0f) * 30.0f
                    totalDs += weight * (dsVal / 100.0f)
                    totalDl += weight * (dlVal / 100.0f) * 0.4f
                    totalW += weight
                }

                val norm = max(totalW, 1.0f)
                hue = (hue + totalDh / norm) % 360.0f
                if (hue < 0.0f) hue += 360.0f
                sat = (sat + (totalDs / norm) * sat).coerceIn(0.0f, 1.0f)
                lum = (lum + (totalDl / norm) * lum * (1.0f - lum) * 2.0f).coerceIn(0.0f, 1.0f)
            }

            // HSL to RGB
            val c = (1.0f - abs(2.0f * lum - 1.0f)) * sat
            val hPrime = hue / 60.0f
            val x = c * (1.0f - abs((hPrime % 2.0f) - 1.0f))
            val m = lum - c * 0.5f

            var rOut = 0.0f
            var gOut = 0.0f
            var bOut = 0.0f

            when {
                hPrime < 1.0f -> { rOut = c; gOut = x; bOut = 0.0f }
                hPrime < 2.0f -> { rOut = x; gOut = c; bOut = 0.0f }
                hPrime < 3.0f -> { rOut = 0.0f; gOut = c; bOut = x }
                hPrime < 4.0f -> { rOut = 0.0f; gOut = x; bOut = c }
                hPrime < 5.0f -> { rOut = x; gOut = 0.0f; bOut = c }
                else -> { rOut = c; gOut = 0.0f; bOut = x }
            }

            output[idx] = (rOut + m).coerceIn(0.0f, 1.0f)
            output[idx + 1] = (gOut + m).coerceIn(0.0f, 1.0f)
            output[idx + 2] = (bOut + m).coerceIn(0.0f, 1.0f)
            idx += 3
        }

        return output
    }

    fun applyColorGrading(
        rgb: FloatArray,
        count: Int,
        s: ColorGradingSettings,
        output: FloatArray = FloatArray(count * 3)
    ): FloatArray {
        for (i in 0 until count * 3) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }

        val pivot = 0.5f + 0.25f * (s.balance / 100.0f)
        val blendWidth = max(0.1f, (s.blending / 100.0f) * 0.5f)

        fun getTintVector(hueDeg: Float, satPercent: Float): FloatArray {
            if (satPercent < 1e-4f) return FloatArray(3)
            val rad = Math.toRadians(hueDeg.toDouble()).toFloat()
            val intensity = (satPercent / 100.0f) * 0.35f
            val twoPiThirds = (2.0 * Math.PI / 3.0).toFloat()
            val rTint = cos(rad)
            val gTint = cos(rad - twoPiThirds)
            val bTint = cos(rad - 2.0f * twoPiThirds)
            return floatArrayOf(rTint * intensity, gTint * intensity, bTint * intensity)
        }

        val vShad = getTintVector(s.shadowHue, s.shadowSat)
        val vMid = getTintVector(s.midtoneHue, s.midtoneSat)
        val vHigh = getTintVector(s.highlightHue, s.highlightSat)
        val vGlob = getTintVector(s.globalHue, s.globalSat)

        val shadLumFactor = (s.shadowLum / 100.0f) * 0.3f
        val midLumFactor = (s.midtoneLum / 100.0f) * 0.3f
        val highLumFactor = (s.highlightLum / 100.0f) * 0.3f
        val globLumFactor = (s.globalLum / 100.0f) * 0.25f

        var idx = 0
        for (p in 0 until count) {
            val r = output[idx]
            val g = output[idx + 1]
            val b = output[idx + 2]

            val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b

            val wShad = ((pivot - lum) / blendWidth).coerceIn(0.0f, 1.0f).pow(1.5f)
            val wHigh = ((lum - pivot) / blendWidth).coerceIn(0.0f, 1.0f).pow(1.5f)
            val wMid = (1.0f - wShad - wHigh).coerceIn(0.0f, 1.0f)

            val tintR = wShad * vShad[0] + wMid * vMid[0] + wHigh * vHigh[0] + vGlob[0]
            val tintG = wShad * vShad[1] + wMid * vMid[1] + wHigh * vHigh[1] + vGlob[1]
            val tintB = wShad * vShad[2] + wMid * vMid[2] + wHigh * vHigh[2] + vGlob[2]

            var newR = (r + tintR * r * (1.0f - r) * 4.0f).coerceIn(0.0f, 1.0f)
            var newG = (g + tintG * g * (1.0f - g) * 4.0f).coerceIn(0.0f, 1.0f)
            var newB = (b + tintB * b * (1.0f - b) * 4.0f).coerceIn(0.0f, 1.0f)

            val lumDelta = wShad * shadLumFactor + wMid * midLumFactor + wHigh * highLumFactor + globLumFactor
            newR = (newR + lumDelta * newR * (1.0f - newR) * 2.5f).coerceIn(0.0f, 1.0f)
            newG = (newG + lumDelta * newG * (1.0f - newG) * 2.5f).coerceIn(0.0f, 1.0f)
            newB = (newB + lumDelta * newB * (1.0f - newB) * 2.5f).coerceIn(0.0f, 1.0f)

            output[idx] = newR
            output[idx + 1] = newG
            output[idx + 2] = newB
            idx += 3
        }

        return output
    }

    fun applyCalibration(
        rgb: FloatArray,
        count: Int,
        s: CalibrationSettings,
        output: FloatArray = FloatArray(count * 3)
    ): FloatArray {
        for (i in 0 until count * 3) {
            output[i] = rgb[i].coerceIn(0.0f, 1.0f)
        }

        // Shadow tint
        if (abs(s.shadowTint) > 1e-4f) {
            val tintVal = (s.shadowTint / 100.0f) * 0.12f
            var idx = 0
            for (p in 0 until count) {
                val r = output[idx]
                val g = output[idx + 1]
                val b = output[idx + 2]
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val shadowMask = 1.0f - ColorSpace.smoothstep(0.0f, 0.25f, lum)

                output[idx] = (r + tintVal * shadowMask * 0.5f).coerceIn(0.0f, 1.0f)
                output[idx + 1] = (g - tintVal * shadowMask).coerceIn(0.0f, 1.0f)
                output[idx + 2] = (b + tintVal * shadowMask * 0.5f).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        // Linear RGB primary rotation and saturation
        val drH = (s.redHue / 100.0f) * 0.20f
        val drS = (s.redSat / 100.0f) * 0.40f
        val dgH = (s.greenHue / 100.0f) * 0.20f
        val dgS = (s.greenSat / 100.0f) * 0.40f
        val dbH = (s.blueHue / 100.0f) * 0.20f
        val dbS = (s.blueSat / 100.0f) * 0.40f

        // Matrix:
        // row 0: [ 1 + drS*0.25, dgH*0.15, dbH*0.15 ]
        // row 1: [ drH*0.15, 1 + dgS*0.25, 0 ]
        // row 2: [ 0, 0, 1 + dbS*0.25 ]
        var m00 = 1.0f + drS * 0.25f
        var m01 = dgH * 0.15f
        var m02 = dbH * 0.15f

        var m10 = drH * 0.15f
        var m11 = 1.0f + dgS * 0.25f
        var m12 = 0.0f

        var m20 = 0.0f
        var m21 = 0.0f
        var m22 = 1.0f + dbS * 0.25f

        // Normalize rows:
        val rSum0 = max(m00 + m01 + m02, EPSILON)
        val rSum1 = max(m10 + m11 + m12, EPSILON)
        val rSum2 = max(m20 + m21 + m22, EPSILON)

        m00 /= rSum0; m01 /= rSum0; m02 /= rSum0
        m10 /= rSum1; m11 /= rSum1; m12 /= rSum1
        m20 /= rSum2; m21 /= rSum2; m22 /= rSum2

        var idx = 0
        for (p in 0 until count) {
            val linR = ColorSpace.srgbToLinear(output[idx])
            val linG = ColorSpace.srgbToLinear(output[idx + 1])
            val linB = ColorSpace.srgbToLinear(output[idx + 2])

            val calR = (m00 * linR + m01 * linG + m02 * linB).coerceIn(0.0f, 1.0f)
            val calG = (m10 * linR + m11 * linG + m12 * linB).coerceIn(0.0f, 1.0f)
            val calB = (m20 * linR + m21 * linG + m22 * linB).coerceIn(0.0f, 1.0f)

            output[idx] = ColorSpace.linearToSrgb(calR).coerceIn(0.0f, 1.0f)
            output[idx + 1] = ColorSpace.linearToSrgb(calG).coerceIn(0.0f, 1.0f)
            output[idx + 2] = ColorSpace.linearToSrgb(calB).coerceIn(0.0f, 1.0f)
            idx += 3
        }

        return output
    }
}
