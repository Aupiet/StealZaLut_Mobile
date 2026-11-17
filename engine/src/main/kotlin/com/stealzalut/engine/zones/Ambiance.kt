package com.stealzalut.engine.zones

import com.stealzalut.engine.color.ColorSpace
import kotlin.math.*

data class AmbianceTransferModel(
    val tintCurve: Array<FloatArray>, // 64 x 2 (a, b)
    val globalModel: OklabTransferModel,
    val mute: Float = 0.35f,
    val protectVivid: Float = 0.8f
) {
    fun tint(lightness: Float): Pair<Float, Float> {
        val t = lightness.coerceIn(0.0f, 1.0f) * 63.0f
        val idx = t.toInt().coerceIn(0, 62)
        val frac = t - idx
        val a = (1.0f - frac) * tintCurve[idx][0] + frac * tintCurve[idx + 1][0]
        val b = (1.0f - frac) * tintCurve[idx][1] + frac * tintCurve[idx + 1][1]
        return Pair(a, b)
    }

    fun applyLab(lab: FloatArray): FloatArray {
        val n = lab.size / 3
        val globalLab = globalModel.applyLab(lab)
        val out = FloatArray(lab.size)

        for (i in 0 until n) {
            val idx = i * 3
            val lOut = globalLab[idx].coerceIn(0.0f, 1.0f)
            val a = lab[idx + 1]
            val b = lab[idx + 2]

            val chroma = sqrt(a * a + b * b)
            val free = 1.0f - protectVivid * ColorSpace.smoothstep(0.04f, 0.12f, chroma)
            val (tintA, tintB) = tint(lOut)

            val aOut = a * (1.0f - mute * free) + free * tintA
            val bOut = b * (1.0f - mute * free) + free * tintB

            out[idx] = lOut
            out[idx + 1] = aOut
            out[idx + 2] = bOut
        }
        return out
    }

    fun apply(colors: FloatArray): FloatArray {
        val n = colors.size / 3
        val lab = FloatArray(colors.size)
        for (i in 0 until n) {
            ColorSpace.rgbToOklab(colors[i * 3], colors[i * 3 + 1], colors[i * 3 + 2], lab, i * 3)
        }
        val outLab = applyLab(lab)
        ColorSpace.gamutCompress(outLab)
        val outRgb = FloatArray(colors.size)
        for (i in 0 until n) {
            ColorSpace.oklabToRgb(outLab[i * 3].coerceIn(0f, 1f), outLab[i * 3 + 1], outLab[i * 3 + 2], outRgb, i * 3)
        }
        return outRgb
    }
}

object Ambiance {
    const val CURVE_SAMPLES = 64
    const val CURVE_BANDWIDTH = 0.05f
    const val CURVE_PRIOR = 25.0f

    fun referenceTintCurve(reference: ByteArray, w: Int, h: Int): Array<FloatArray> {
        val (lab, _, _) = KMeans.analysisOklab(reference, w, h)
        val n = lab.size / 3

        var meanA = 0.0
        var meanB = 0.0
        for (i in 0 until n) {
            meanA += lab[i * 3 + 1]
            meanB += lab[i * 3 + 2]
        }
        meanA /= n.toDouble()
        meanB /= n.toDouble()

        val curve = Array(CURVE_SAMPLES) { FloatArray(2) }
        val inv2Bw2 = -0.5f / (CURVE_BANDWIDTH * CURVE_BANDWIDTH)

        for (k in 0 until CURVE_SAMPLES) {
            val level = k.toFloat() / (CURVE_SAMPLES - 1).toFloat()
            var sumA = CURVE_PRIOR * meanA
            var sumB = CURVE_PRIOR * meanB
            var sumW = CURVE_PRIOR.toDouble()

            for (i in 0 until n) {
                val dl = level - lab[i * 3]
                val w_i = exp(dl * dl * inv2Bw2).toDouble()
                sumW += w_i
                sumA += w_i * lab[i * 3 + 1]
                sumB += w_i * lab[i * 3 + 2]
            }

            curve[k][0] = (sumA / sumW).toFloat()
            curve[k][1] = (sumB / sumW).toFloat()
        }
        return curve
    }

    fun fitAmbiance(
        source: ByteArray,
        srcW: Int,
        srcH: Int,
        reference: ByteArray,
        refW: Int,
        refH: Int
    ): AmbianceTransferModel {
        val globalModel = fitOklabModel(source, srcW, srcH, reference, refW, refH)
        val tintCurve = referenceTintCurve(reference, refW, refH)
        return AmbianceTransferModel(tintCurve = tintCurve, globalModel = globalModel)
    }
}
