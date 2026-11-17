package com.stealzalut.engine.zones

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.transfer.ColorTransfer
import kotlin.math.*

val WARM_AXIS_0 = (0.29 / hypot(0.29, 0.96)).toFloat()
val WARM_AXIS_1 = (0.96 / hypot(0.29, 0.96)).toFloat()

data class ZoneBakeParams(
    val tau: Float = 1.0f,
    val sigma0: Float = 0.02f,
    val beta: Float = 0.3f,
    val backgroundLogit: Float = -4.5f
)

data class Conflict(
    val zoneA: Int,
    val zoneB: Int,
    val overlap: Float,
    val displacementGap: Float
)

data class OklabTransferModel(
    val srcMean: FloatArray,
    val refMean: FloatArray,
    val lightnessScale: Float,
    val chromaMatrix: FloatArray // 2x2: [m00, m01, m10, m11]
) {
    fun applyLab(lab: FloatArray): FloatArray {
        val n = lab.size / 3
        val out = FloatArray(lab.size)
        val m00 = chromaMatrix[0]
        val m01 = chromaMatrix[1]
        val m10 = chromaMatrix[2]
        val m11 = chromaMatrix[3]

        for (i in 0 until n) {
            val idx = i * 3
            out[idx] = refMean[0] + lightnessScale * (lab[idx] - srcMean[0])
            val da = lab[idx + 1] - srcMean[1]
            val db = lab[idx + 2] - srcMean[2]
            out[idx + 1] = refMean[1] + da * m00 + db * m01
            out[idx + 2] = refMean[2] + da * m10 + db * m11
        }
        return out
    }

    fun apply(rgb: FloatArray): FloatArray {
        val n = rgb.size / 3
        val lab = FloatArray(rgb.size)
        for (i in 0 until n) {
            ColorSpace.rgbToOklab(rgb[i * 3], rgb[i * 3 + 1], rgb[i * 3 + 2], lab, i * 3)
        }
        val outLab = applyLab(lab)
        ColorSpace.gamutCompress(outLab)
        val outRgb = FloatArray(rgb.size)
        for (i in 0 until n) {
            ColorSpace.oklabToRgb(outLab[i * 3].coerceIn(0f, 1f), outLab[i * 3 + 1], outLab[i * 3 + 2], outRgb, i * 3)
        }
        return outRgb
    }
}

data class ZoneTransferModel(
    val source: ZonePalette,
    val reference: ZonePalette,
    val table: CorrespondenceTable,
    val globalModel: OklabTransferModel,
    val params: ZoneBakeParams = ZoneBakeParams(),
    val analysisId: String = "",
    val sourceColours: ColourStats? = null,
    val referenceColours: ColourStats? = null
) {
    fun withTable(newTable: CorrespondenceTable) = copy(table = newTable)
    fun withParams(newParams: ZoneBakeParams) = copy(params = newParams)

    fun apply(colors: FloatArray): FloatArray {
        val n = colors.size / 3
        val lab = FloatArray(colors.size)
        for (i in 0 until n) {
            ColorSpace.rgbToOklab(colors[i * 3], colors[i * 3 + 1], colors[i * 3 + 2], lab, i * 3)
        }
        val disp = displacement(lab, colors)
        val outLab = FloatArray(colors.size) { i -> lab[i] + disp[i] }
        ColorSpace.gamutCompress(outLab)

        val outRgb = FloatArray(colors.size)
        for (i in 0 until n) {
            ColorSpace.oklabToRgb(outLab[i * 3].coerceIn(0f, 1f), outLab[i * 3 + 1], outLab[i * 3 + 2], outRgb, i * 3)
        }
        return outRgb
    }

    fun displacement(lab: FloatArray, srgbIn: FloatArray? = null): FloatArray {
        val n = lab.size / 3
        val srgb = srgbIn ?: run {
            val res = FloatArray(lab.size)
            val comp = lab.clone()
            ColorSpace.gamutCompress(comp)
            for (i in 0 until n) {
                ColorSpace.oklabToRgb(comp[i * 3].coerceIn(0f, 1f), comp[i * 3 + 1], comp[i * 3 + 2], res, i * 3)
            }
            res
        }

        val zones = source.zones
        val k = zones.size

        // global delta: srgb_to_oklab(global_model.apply(srgb)) - lab
        val globalAppliedRgb = globalModel.apply(srgb)
        val globalDelta = FloatArray(lab.size)
        for (i in 0 until n) {
            val idx = i * 3
            ColorSpace.rgbToOklab(globalAppliedRgb[idx], globalAppliedRgb[idx + 1], globalAppliedRgb[idx + 2], globalDelta, idx)
            globalDelta[idx] -= lab[idx]
            globalDelta[idx + 1] -= lab[idx + 1]
            globalDelta[idx + 2] -= lab[idx + 2]
        }

        val logits = Array(n) { FloatArray(k + 1) }
        val deltas = Array(k + 1) { FloatArray(n * 3) }

        // slot 0: background logit and global delta
        for (i in 0 until n) {
            logits[i][0] = params.backgroundLogit
        }
        System.arraycopy(globalDelta, 0, deltas[0], 0, n * 3)

        // slots 1..k: zone logit and zone delta
        for (zi in zones.indices) {
            val zone = zones[zi]
            val slot = zi + 1
            val zLogits = zoneLogit(lab, zone, params)
            for (i in 0 until n) logits[i][slot] = zLogits[i]

            val zDelta = zoneDelta(lab, zone, table.link(zone.id), reference, globalDelta)
            System.arraycopy(zDelta, 0, deltas[slot], 0, n * 3)
        }

        // Softmax per pixel
        val weights = Array(n) { FloatArray(k + 1) }
        for (i in 0 until n) {
            var maxLogit = Float.NEGATIVE_INFINITY
            for (s in 0..k) if (logits[i][s] > maxLogit) maxLogit = logits[i][s]
            var sumExp = 0.0f
            for (s in 0..k) {
                val e = exp(logits[i][s] - maxLogit)
                weights[i][s] = e
                sumExp += e
            }
            val denom = max(sumExp, 1e-30f)
            for (s in 0..k) weights[i][s] /= denom
        }

        // Blend deltas
        val outDisp = FloatArray(n * 3)
        for (i in 0 until n) {
            val idx = i * 3
            var d0 = 0.0f
            var d1 = 0.0f
            var d2 = 0.0f
            for (s in 0..k) {
                val w = weights[i][s]
                d0 += w * deltas[s][idx]
                d1 += w * deltas[s][idx + 1]
                d2 += w * deltas[s][idx + 2]
            }
            outDisp[idx] = d0
            outDisp[idx + 1] = d1
            outDisp[idx + 2] = d2
        }
        return outDisp
    }
}

fun invert3x3(m: FloatArray): FloatArray {
    val a00 = m[0]; val a01 = m[1]; val a02 = m[2]
    val a10 = m[3]; val a11 = m[4]; val a12 = m[5]
    val a20 = m[6]; val a21 = m[7]; val a22 = m[8]

    val c00 = a11 * a22 - a12 * a21
    val c01 = a12 * a20 - a10 * a22
    val c02 = a10 * a21 - a11 * a20
    val det = a00 * c00 + a01 * c01 + a02 * c02
    val invDet = if (abs(det) > 1e-12f) 1.0f / det else 0.0f

    val inv = FloatArray(9)
    inv[0] = c00 * invDet
    inv[1] = (a02 * a21 - a01 * a22) * invDet
    inv[2] = (a01 * a12 - a02 * a11) * invDet
    inv[3] = c01 * invDet
    inv[4] = (a00 * a22 - a02 * a20) * invDet
    inv[5] = (a02 * a10 - a00 * a12) * invDet
    inv[6] = c02 * invDet
    inv[7] = (a01 * a20 - a00 * a21) * invDet
    inv[8] = (a00 * a11 - a01 * a10) * invDet
    return inv
}

fun det3x3(m: FloatArray): Float {
    val a00 = m[0]; val a01 = m[1]; val a02 = m[2]
    val a10 = m[3]; val a11 = m[4]; val a12 = m[5]
    val a20 = m[6]; val a21 = m[7]; val a22 = m[8]
    return a00 * (a11 * a22 - a12 * a21) - a01 * (a10 * a22 - a12 * a20) + a02 * (a10 * a21 - a11 * a20)
}

fun zoneLogit(lab: FloatArray, zone: Zone, params: ZoneBakeParams): FloatArray {
    val n = lab.size / 3
    val cov = zone.cov.clone()
    val s2 = params.sigma0 * params.sigma0
    cov[0] += s2
    cov[4] += s2
    cov[8] += s2
    val invCov = invert3x3(cov)

    val out = FloatArray(n)
    val mu0 = zone.mean[0]
    val mu1 = zone.mean[1]
    val mu2 = zone.mean[2]

    val i00 = invCov[0]; val i01 = invCov[1]; val i02 = invCov[2]
    val i10 = invCov[3]; val i11 = invCov[4]; val i12 = invCov[5]
    val i20 = invCov[6]; val i21 = invCov[7]; val i22 = invCov[8]

    val areaTerm = params.beta * ln(max(zone.area, 1e-12f))
    val inv2Tau = -0.5f / params.tau

    for (i in 0 until n) {
        val idx = i * 3
        val d0 = lab[idx] - mu0
        val d1 = lab[idx + 1] - mu1
        val d2 = lab[idx + 2] - mu2

        val v0 = d0 * i00 + d1 * i10 + d2 * i20
        val v1 = d0 * i01 + d1 * i11 + d2 * i21
        val v2 = d0 * i02 + d1 * i12 + d2 * i22

        val mahal2 = d0 * v0 + d1 * v1 + d2 * v2
        out[i] = inv2Tau * mahal2 + areaTerm
    }
    return out
}

fun targetMoments(reference: ZonePalette, targets: Map<Int, Float>): Pair<FloatArray, FloatArray> {
    var totalW = 0.0f
    for (w in targets.values) totalW += w
    val mean = FloatArray(3)
    for ((zId, w) in targets) {
        val z = reference.zone(zId)
        val nw = w / totalW
        mean[0] += nw * z.mean[0]
        mean[1] += nw * z.mean[1]
        mean[2] += nw * z.mean[2]
    }

    val cov = FloatArray(9)
    for ((zId, w) in targets) {
        val z = reference.zone(zId)
        val nw = w / totalW
        val d0 = z.mean[0] - mean[0]
        val d1 = z.mean[1] - mean[1]
        val d2 = z.mean[2] - mean[2]

        cov[0] += nw * (z.cov[0] + d0 * d0)
        cov[1] += nw * (z.cov[1] + d0 * d1)
        cov[2] += nw * (z.cov[2] + d0 * d2)
        cov[3] += nw * (z.cov[3] + d1 * d0)
        cov[4] += nw * (z.cov[4] + d1 * d1)
        cov[5] += nw * (z.cov[5] + d1 * d2)
        cov[6] += nw * (z.cov[6] + d2 * d0)
        cov[7] += nw * (z.cov[7] + d2 * d1)
        cov[8] += nw * (z.cov[8] + d2 * d2)
    }
    return Pair(mean, cov)
}

private val INTENT_STEPS = mapOf(
    "lightness" to mapOf("small" to 0.02f, "medium" to 0.05f, "large" to 0.10f),
    "saturation_up" to mapOf("small" to 1.08f, "medium" to 1.20f, "large" to 1.40f),
    "saturation_down" to mapOf("small" to 0.92f, "medium" to 0.82f, "large" to 0.65f),
    "warmth" to mapOf("small" to 0.010f, "medium" to 0.020f, "large" to 0.040f),
    "tint" to mapOf("small" to 0.008f, "medium" to 0.016f, "large" to 0.030f)
)

private fun signedStep(level: String, kind: String): Float {
    if (level == "0" || level.isEmpty()) return 0.0f
    val suffix = level.substring(1)
    val step = INTENT_STEPS[kind]?.get(suffix) ?: 0.0f
    return if (level.startsWith("+")) step else -step
}

private fun saturationFactor(level: String): Float {
    if (level == "0" || level.isEmpty()) return 1.0f
    val kind = if (level.startsWith("+")) "saturation_up" else "saturation_down"
    val suffix = level.substring(1)
    return INTENT_STEPS[kind]?.get(suffix) ?: 1.0f
}

fun zoneDelta(
    lab: FloatArray,
    zone: Zone,
    link: Link,
    reference: ZonePalette,
    globalDelta: FloatArray
): FloatArray {
    val n = lab.size / 3
    val delta = FloatArray(lab.size)
    if (link.locked) return delta

    for (i in 0 until n) {
        delta[i * 3] = globalDelta[i * 3]
    }

    if (link.targets.isNotEmpty() && link.strength > 0.0f) {
        val (tMean, tCov) = targetMoments(reference, link.targets)
        val sigmaS = sqrt(max(zone.cov[0], 0f))
        val sigmaT = sqrt(max(tCov[0], 0f))
        val scale = (sigmaT / max(sigmaS, 1e-6f)).coerceIn(0.5f, 2.0f)

        val covS2 = doubleArrayOf(zone.cov[4].toDouble(), zone.cov[5].toDouble(), zone.cov[7].toDouble(), zone.cov[8].toDouble())
        val covT2 = doubleArrayOf(tCov[4].toDouble(), tCov[5].toDouble(), tCov[7].toDouble(), tCov[8].toDouble())

        val mkl2 = ColorTransfer.computeMklMatrix(covS2, covT2, 2)
        val chromaMap = ColorTransfer.boundSymmetric(mkl2, 2, 0.5f, 2.0f)

        val lum = link.luminanceStrength ?: 0.0f
        val chroma = link.chromaStrength ?: 1.0f
        val strength = link.strength

        val c00 = chromaMap[0]; val c01 = chromaMap[1]
        val c10 = chromaMap[2]; val c11 = chromaMap[3]

        for (i in 0 until n) {
            val idx = i * 3
            val newL = tMean[0] + scale * (lab[idx] - zone.mean[0])
            val da = lab[idx + 1] - zone.mean[1]
            val db = lab[idx + 2] - zone.mean[2]

            val newA = tMean[1] + da * c00 + db * c01
            val newB = tMean[2] + da * c10 + db * c11

            val zoneDl = newL - lab[idx]
            delta[idx] += strength * lum * (zoneDl - globalDelta[idx])
            delta[idx + 1] = strength * chroma * (newA - lab[idx + 1])
            delta[idx + 2] = strength * chroma * (newB - lab[idx + 2])
        }
    }

    val intent = link.intent
    if (!intent.isNeutral) {
        val dl = signedStep(intent.lightness, "lightness")
        val sat = saturationFactor(intent.saturation) - 1.0f
        val warmth = signedStep(intent.warmth, "warmth")
        val tint = signedStep(intent.tint, "tint")

        for (i in 0 until n) {
            val idx = i * 3
            delta[idx] += dl
            val aNow = lab[idx + 1] + delta[idx + 1]
            val bNow = lab[idx + 2] + delta[idx + 2]

            delta[idx + 1] += sat * aNow + warmth * WARM_AXIS_0 + tint
            delta[idx + 2] += sat * bNow + warmth * WARM_AXIS_1
        }
    }
    return delta
}

fun bhattacharyyaCoefficient(meanA: FloatArray, covA: FloatArray, meanB: FloatArray, covB: FloatArray): Float {
    val cov = FloatArray(9) { i -> 0.5f * (covA[i] + covB[i]) }
    val diff = floatArrayOf(meanA[0] - meanB[0], meanA[1] - meanB[1], meanA[2] - meanB[2])

    val invCov = invert3x3(cov)
    val v0 = diff[0] * invCov[0] + diff[1] * invCov[3] + diff[2] * invCov[6]
    val v1 = diff[0] * invCov[1] + diff[1] * invCov[4] + diff[2] * invCov[7]
    val v2 = diff[0] * invCov[2] + diff[1] * invCov[5] + diff[2] * invCov[8]
    val termMean = 0.125f * (diff[0] * v0 + diff[1] * v1 + diff[2] * v2)

    val detCov = max(det3x3(cov), 1e-30f)
    val detA = max(det3x3(covA), 1e-30f)
    val detB = max(det3x3(covB), 1e-30f)
    val termCov = 0.5f * (ln(detCov) - 0.5f * (ln(detA) + ln(detB)))
    return exp(-(termMean + termCov))
}

fun findConflicts(model: ZoneTransferModel): List<Conflict> {
    val zones = model.source.zones
    val k = zones.size
    val means = FloatArray(k * 3)
    for (i in zones.indices) {
        means[i * 3] = zones[i].mean[0]
        means[i * 3 + 1] = zones[i].mean[1]
        means[i * 3 + 2] = zones[i].mean[2]
    }

    val globalMeansApplied = model.globalModel.apply(run {
        val srgb = FloatArray(k * 3)
        val comp = means.clone()
        ColorSpace.gamutCompress(comp)
        for (i in 0 until k) ColorSpace.oklabToRgb(comp[i * 3].coerceIn(0f, 1f), comp[i * 3 + 1], comp[i * 3 + 2], srgb, i * 3)
        srgb
    })
    val globalDelta = FloatArray(k * 3)
    for (i in 0 until k) {
        val idx = i * 3
        ColorSpace.rgbToOklab(globalMeansApplied[idx], globalMeansApplied[idx + 1], globalMeansApplied[idx + 2], globalDelta, idx)
        globalDelta[idx] -= means[idx]
        globalDelta[idx + 1] -= means[idx + 1]
        globalDelta[idx + 2] -= means[idx + 2]
    }

    val meanDelta = mutableMapOf<Int, FloatArray>()
    for (i in zones.indices) {
        val z = zones[i]
        val singleLab = floatArrayOf(z.mean[0], z.mean[1], z.mean[2])
        val singleGlobal = floatArrayOf(globalDelta[i * 3], globalDelta[i * 3 + 1], globalDelta[i * 3 + 2])
        val d = zoneDelta(singleLab, z, model.table.link(z.id), model.reference, singleGlobal)
        meanDelta[z.id] = d
    }

    val conflicts = mutableListOf<Conflict>()
    for (i in 0 until k) {
        for (j in i + 1 until k) {
            val a = zones[i]
            val b = zones[j]
            val overlap = bhattacharyyaCoefficient(a.mean, a.cov, b.mean, b.cov)
            val dA = meanDelta[a.id]!!
            val dB = meanDelta[b.id]!!
            val gap = sqrt((dA[0] - dB[0]).pow(2) + (dA[1] - dB[1]).pow(2) + (dA[2] - dB[2]).pow(2))
            if (overlap > 0.3f && gap > 0.05f) {
                conflicts.add(Conflict(a.id, b.id, overlap, gap))
            }
        }
    }
    return conflicts
}

fun harmonizeConflicts(model: ZoneTransferModel, maxRounds: Int = 4): CorrespondenceTable {
    var table = model.table
    for (round in 0 until maxRounds) {
        val conflicts = findConflicts(model.withTable(table))
        var changed = false
        for (conflict in conflicts) {
            val a = table.link(conflict.zoneA)
            val b = table.link(conflict.zoneB)
            if (a.origin != "auto" || b.origin != "auto" || a.locked || b.locked) continue

            val areaA = model.source.zone(a.sourceZone).area
            val areaB = model.source.zone(b.sourceZone).area

            val merged = mutableMapOf<Int, Float>()
            for ((link, area) in listOf(Pair(a, areaA), Pair(b, areaB))) {
                for ((refId, weight) in link.targets) {
                    merged[refId] = (merged[refId] ?: 0.0f) + weight * area
                }
            }
            var total = 0.0f
            for (w in merged.values) total += w
            val normTargets = merged.mapValues { it.value / total }
            if (normTargets != a.targets || normTargets != b.targets) {
                table = table.withLink(a.copy(targets = normTargets))
                table = table.withLink(b.copy(targets = normTargets))
                changed = true
            }
        }
        if (!changed) break
    }
    return table
}

fun fitOklabModel(source: ByteArray, srcW: Int, srcH: Int, reference: ByteArray, refW: Int, refH: Int): OklabTransferModel {
    val (srcLab, _, _) = KMeans.analysisOklab(source, srcW, srcH)
    val (refLab, _, _) = KMeans.analysisOklab(reference, refW, refH)

    val nSrc = srcLab.size / 3
    val nRef = refLab.size / 3

    val muS = FloatArray(3)
    val muR = FloatArray(3)
    for (i in 0 until nSrc) {
        muS[0] += srcLab[i * 3]
        muS[1] += srcLab[i * 3 + 1]
        muS[2] += srcLab[i * 3 + 2]
    }
    for (c in 0 until 3) muS[c] /= nSrc.toFloat()

    for (i in 0 until nRef) {
        muR[0] += refLab[i * 3]
        muR[1] += refLab[i * 3 + 1]
        muR[2] += refLab[i * 3 + 2]
    }
    for (c in 0 until 3) muR[c] /= nRef.toFloat()

    var varS0 = 0.0
    for (i in 0 until nSrc) {
        val d = srcLab[i * 3] - muS[0]
        varS0 += d * d
    }
    val sigmaS = sqrt(varS0 / nSrc.toDouble()).toFloat()

    var varR0 = 0.0
    for (i in 0 until nRef) {
        val d = refLab[i * 3] - muR[0]
        varR0 += d * d
    }
    val sigmaR = sqrt(varR0 / nRef.toDouble()).toFloat()

    val lightnessScale = (sigmaR / max(sigmaS, 1e-6f)).coerceIn(0.5f, 2.0f)

    // 2x2 covariance on chroma (a, b)
    val covS2 = DoubleArray(4)
    val factorS = 1.0 / max(1, nSrc - 1).toDouble()
    for (i in 0 until nSrc) {
        val da = (srcLab[i * 3 + 1] - muS[1]).toDouble()
        val db = (srcLab[i * 3 + 2] - muS[2]).toDouble()
        covS2[0] += da * da
        covS2[1] += da * db
        covS2[2] += db * da
        covS2[3] += db * db
    }
    for (k in 0..3) covS2[k] = covS2[k] * factorS + (if (k == 0 || k == 3) 1e-8 else 0.0)

    val covR2 = DoubleArray(4)
    val factorR = 1.0 / max(1, nRef - 1).toDouble()
    for (i in 0 until nRef) {
        val da = (refLab[i * 3 + 1] - muR[1]).toDouble()
        val db = (refLab[i * 3 + 2] - muR[2]).toDouble()
        covR2[0] += da * da
        covR2[1] += da * db
        covR2[2] += db * da
        covR2[3] += db * db
    }
    for (k in 0..3) covR2[k] = covR2[k] * factorR + (if (k == 0 || k == 3) 1e-8 else 0.0)

    val mkl2 = ColorTransfer.computeMklMatrix(covS2, covR2, 2)
    val chromaMatrix = ColorTransfer.boundSymmetric(mkl2, 2, 0.5f, 2.0f)

    return OklabTransferModel(
        srcMean = muS,
        refMean = muR,
        lightnessScale = lightnessScale,
        chromaMatrix = chromaMatrix
    )
}

fun fitZoneTransfer(
    source: ByteArray,
    srcW: Int,
    srcH: Int,
    reference: ByteArray,
    refW: Int,
    refH: Int,
    zoneCount: Int = KMeans.DEFAULT_ZONE_COUNT,
    params: ZoneBakeParams? = null,
    userOverrides: Map<String, Int>? = null
): ZoneTransferModel {
    val srcPal = KMeans.analyzeZones(source, srcW, srcH, zoneCount)
    val refPal = KMeans.analyzeZones(reference, refW, refH, zoneCount)
    var table = CorrespondenceTable.fromMatching(Sinkhorn.matchZones(srcPal, refPal))
    val globalModel = fitOklabModel(source, srcW, srcH, reference, refW, refH)

    var model = ZoneTransferModel(
        source = srcPal,
        reference = refPal,
        table = table,
        globalModel = globalModel,
        params = params ?: ZoneBakeParams(),
        analysisId = KMeans.analysisId(source, srcW, srcH, reference, refW, refH, zoneCount),
        sourceColours = KMeans.colorStats(source, srcW, srcH),
        referenceColours = KMeans.colorStats(reference, refW, refH)
    )

    table = harmonizeConflicts(model)
    model = model.withTable(table)

    if (userOverrides != null) {
        for ((sZ, rZ) in userOverrides) {
            val srcZId = sZ.toIntOrNull()
            if (srcZId != null) {
                try {
                    table = retarget(table, srcZId, rZ)
                } catch (ignored: Exception) {}
            }
        }
        model = model.withTable(table)
    }
    return model
}
