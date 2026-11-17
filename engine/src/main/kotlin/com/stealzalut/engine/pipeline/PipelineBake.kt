package com.stealzalut.engine.pipeline

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.model.Step2Params
import com.stealzalut.engine.model.Step3Params
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

object PipelineBake {

    private const val SKIN_H_CENTER = 0.86f
    private const val SKIN_C_CENTER = 0.092f
    private const val SKIN_H_SIGMA = 0.35f
    private const val SKIN_C_SIGMA = 0.045f
    private const val NEUTRAL_CHROMA_THRESHOLD = 0.08f
    private val TWO_PI = (2.0 * Math.PI).toFloat()
    private val PI = Math.PI.toFloat()

    fun bakeFullPipelineLut(
        rawLut: FloatArray,
        step2: Step2Params,
        step3: Step3Params,
        lutSize: Int = 33
    ): FloatArray {
        val count = lutSize * lutSize * lutSize
        val identity = LutGenerator.generateIdentityGrid(lutSize)
        val pureLut = if (rawLut.isEmpty()) identity else rawLut

        // Stage 2
        val refined = applyStep2(identity, pureLut, count, step2)

        // Stage 3
        val baked = applyStep3(refined, count, step3)

        val output = FloatArray(count * 3)
        for (i in 0 until count * 3) {
            val v = baked[i]
            output[i] = if (v.isNaN()) 0.0f else v.coerceIn(0.0f, 1.0f)
        }
        return output
    }

    fun applyStep2(
        origRgb: FloatArray,
        pureRgb: FloatArray,
        count: Int,
        params: Step2Params
    ): FloatArray {
        if (params.globalIntensity <= 1e-6f) {
            return origRgb.copyOf()
        }

        val output = FloatArray(count * 3)
        val oklabOrig = FloatArray(3)
        val oklabPure = FloatArray(3)
        val oklabRefined = FloatArray(3)

        var idx = 0
        for (p in 0 until count) {
            val redOrig = origRgb[idx]
            val greenOrig = origRgb[idx + 1]
            val blueOrig = origRgb[idx + 2]

            val redPure = pureRgb[idx]
            val greenPure = pureRgb[idx + 1]
            val bluePure = pureRgb[idx + 2]

            ColorSpace.rgbToOklab(redOrig, greenOrig, blueOrig, oklabOrig, 0)
            ColorSpace.rgbToOklab(redPure, greenPure, bluePure, oklabPure, 0)

            val lOrig = oklabOrig[0]
            val aOrig = oklabOrig[1]
            val bOrig = oklabOrig[2]

            val lPure = oklabPure[0]
            val aPure = oklabPure[1]
            val bPure = oklabPure[2]

            // 2. Luma Lock
            val lOut = if (params.lumaLock > 1e-4f) {
                (1.0f - params.lumaLock) * lPure + params.lumaLock * lOrig
            } else {
                lPure
            }

            // 3. Neutral Axis Lock
            val cOrig = sqrt(aOrig * aOrig + bOrig * bOrig)
            val wNeutral = if (params.neutralAxisLock > 1e-4f) {
                val mNeutral = 1.0f - ColorSpace.smoothstep(0.0f, NEUTRAL_CHROMA_THRESHOLD, cOrig)
                params.neutralAxisLock * mNeutral
            } else 0.0f

            // 4. Skin Tone Lock
            val wSkin = if (params.skinToneLock > 1e-4f) {
                val hOrig = atan2(bOrig, aOrig)
                val dh = (hOrig - SKIN_H_CENTER + PI) % TWO_PI - PI
                val dSkinSq = (dh / SKIN_H_SIGMA) * (dh / SKIN_H_SIGMA) +
                        ((cOrig - SKIN_C_CENTER) / SKIN_C_SIGMA) * ((cOrig - SKIN_C_CENTER) / SKIN_C_SIGMA)
                val mSkin = exp(-0.5f * dSkinSq)
                params.skinToneLock * mSkin
            } else 0.0f

            // 5. Extremes Rolloff
            val wExtremes = if (params.extremesRolloff > 1e-4f) {
                val mShadows = 1.0f - ColorSpace.smoothstep(0.0f, 0.15f, lOrig)
                val mHighlights = ColorSpace.smoothstep(0.85f, 1.0f, lOrig)
                val mExtremes = (mShadows + mHighlights).coerceIn(0.0f, 1.0f)
                params.extremesRolloff * mExtremes
            } else 0.0f

            // 6. Combined Protection Mask
            val wProtect = (1.0f - (1.0f - wNeutral) * (1.0f - wSkin) * (1.0f - wExtremes)).coerceIn(0.0f, 1.0f)
            val aBlended = (1.0f - wProtect) * aPure + wProtect * aOrig
            val bBlended = (1.0f - wProtect) * bPure + wProtect * bOrig

            ColorSpace.oklabToRgb(lOut, aBlended, bBlended, oklabRefined, 0)

            // 7. Global Intensity
            val intensity = params.globalIntensity
            output[idx] = ((1.0f - intensity) * redOrig + intensity * oklabRefined[0]).coerceIn(0.0f, 1.0f)
            output[idx + 1] = ((1.0f - intensity) * greenOrig + intensity * oklabRefined[1]).coerceIn(0.0f, 1.0f)
            output[idx + 2] = ((1.0f - intensity) * blueOrig + intensity * oklabRefined[2]).coerceIn(0.0f, 1.0f)

            idx += 3
        }

        return output
    }

    fun applyStep3(
        rgb: FloatArray,
        count: Int,
        params: Step3Params
    ): FloatArray {
        val out = rgb.copyOf()

        // 1. Linear Exposure
        if (abs(params.exposure) > 1e-4f) {
            val factor = 2.0f.pow(params.exposure)
            for (i in 0 until count * 3) {
                val lin = ColorSpace.srgbToLinear(out[i]) * factor
                out[i] = ColorSpace.linearToSrgb(lin).coerceIn(0.0f, 1.0f)
            }
        }

        // 2. Contrast
        if (abs(params.contrast) > 1e-4f) {
            val c = params.contrast
            if (c > 0.0f) {
                val cFactor = 1.0f + c * 0.8f
                val normFactor = 2.0f * tanh(0.75f * cFactor)
                for (i in 0 until count * 3) {
                    val centered = out[i] - 0.5f
                    out[i] = (0.5f + tanh(centered * 1.5f * cFactor) / normFactor).coerceIn(0.0f, 1.0f)
                }
            } else {
                val soften = 1.0f + c * 0.6f
                for (i in 0 until count * 3) {
                    val centered = out[i] - 0.5f
                    out[i] = (0.5f + centered * soften).coerceIn(0.0f, 1.0f)
                }
            }
        }

        // 3. Highlights
        if (abs(params.highlights) > 1e-4f) {
            val hl = params.highlights * 0.32f
            for (i in 0 until count * 3) {
                val v = out[i]
                val highWeight = ((v - 0.25f) / 0.75f).coerceIn(0.0f, 1.0f).pow(1.5f)
                out[i] = (v + hl * highWeight * v).coerceIn(0.0f, 1.0f)
            }
        }

        // 4. Shadows
        if (abs(params.shadows) > 1e-4f) {
            val sh = params.shadows * 0.28f
            for (i in 0 until count * 3) {
                val v = out[i]
                val shadowWeight = (1.0f - 2.0f * v).coerceIn(0.0f, 1.0f).pow(1.8f)
                out[i] = (v + sh * shadowWeight * (1.0f - v)).coerceIn(0.0f, 1.0f)
            }
        }

        // 5. Temperature & Tint
        if (abs(params.temperature) > 1e-4f || abs(params.tint) > 1e-4f) {
            val t = params.temperature
            val ti = params.tint
            var idx = 0
            for (p in 0 until count) {
                var r = out[idx]
                var g = out[idx + 1]
                var b = out[idx + 2]

                if (abs(t) > 1e-4f) {
                    r *= (1.0f + 0.18f * t)
                    b *= (1.0f - 0.18f * t)
                }
                if (abs(ti) > 1e-4f) {
                    g *= (1.0f - 0.14f * ti)
                    r *= (1.0f + 0.06f * ti)
                    b *= (1.0f + 0.06f * ti)
                }
                out[idx] = r.coerceIn(0.0f, 1.0f)
                out[idx + 1] = g.coerceIn(0.0f, 1.0f)
                out[idx + 2] = b.coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        // 6. Vibrance & Saturation
        if (abs(params.vibrance) > 1e-4f || abs(params.saturation) > 1e-4f) {
            val vMult = params.vibrance * 0.60f
            val sMult = params.saturation * 0.50f
            var idx = 0
            for (p in 0 until count) {
                val r = out[idx]
                val g = out[idx + 1]
                val b = out[idx + 2]

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val delta = maxC - minC
                val sat = if (maxC > 1e-5f) delta / maxC else 0.0f
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                val vBoost = vMult * (1.0f - sat)
                val totalBoost = 1.0f + vBoost + sMult

                out[idx] = (lum + (r - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                out[idx + 1] = (lum + (g - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                out[idx + 2] = (lum + (b - lum) * totalBoost).coerceIn(0.0f, 1.0f)
                idx += 3
            }
        }

        return out
    }
}
