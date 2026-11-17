package com.stealzalut.engine.zones

import kotlin.math.*

object Sinkhorn {

    const val LIGHTNESS_WEIGHT = 2.0
    const val CHROMA_WEIGHT = 0.5
    const val SINKHORN_EPSILON = 0.05
    const val SINKHORN_ITERATIONS = 500
    const val MIN_TARGET_WEIGHT = 0.1

    fun zoneCost(
        source: ZonePalette,
        reference: ZonePalette,
        lightnessWeight: Double = LIGHTNESS_WEIGHT,
        chromaWeight: Double = CHROMA_WEIGHT
    ): Array<DoubleArray> {
        val nSrc = source.zones.size
        val nRef = reference.zones.size
        val nTotal = nSrc + nRef

        var meanA = 0.0
        var meanB = 0.0
        for (z in source.zones) {
            meanA += z.mean[1]
            meanB += z.mean[2]
        }
        for (z in reference.zones) {
            meanA += z.mean[1]
            meanB += z.mean[2]
        }
        meanA /= nTotal.toDouble()
        meanB /= nTotal.toDouble()

        var varSum = 0.0
        for (z in source.zones) {
            val da = z.mean[1] - meanA
            val db = z.mean[2] - meanB
            varSum += da * da + db * db
        }
        for (z in reference.zones) {
            val da = z.mean[1] - meanA
            val db = z.mean[2] - meanB
            varSum += da * da + db * db
        }
        val abScale = max(varSum / nTotal.toDouble(), 1e-4)

        val cost = Array(nSrc) { DoubleArray(nRef) }
        for (i in 0 until nSrc) {
            val lSrc = source.zones[i].lRank.toDouble()
            val aSrc = source.zones[i].mean[1].toDouble()
            val bSrc = source.zones[i].mean[2].toDouble()

            for (j in 0 until nRef) {
                val lRef = reference.zones[j].lRank.toDouble()
                val aRef = reference.zones[j].mean[1].toDouble()
                val bRef = reference.zones[j].mean[2].toDouble()

                val dl = lSrc - lRef
                val da = aSrc - aRef
                val db = bSrc - bRef

                val dL = dl * dl
                val dAb = (da * da + db * db) / abScale
                cost[i][j] = lightnessWeight * dL + chromaWeight * dAb
            }
        }
        return cost
    }

    private fun logsumexpRow(row: DoubleArray): Double {
        var peak = Double.NEGATIVE_INFINITY
        for (v in row) if (v > peak) peak = v
        var sum = 0.0
        for (v in row) sum += exp(v - peak)
        return peak + ln(sum)
    }

    fun sinkhorn(
        cost: Array<DoubleArray>,
        a: DoubleArray,
        b: DoubleArray,
        epsilon: Double = SINKHORN_EPSILON,
        iterations: Int = SINKHORN_ITERATIONS
    ): Array<DoubleArray> {
        val nSrc = a.size
        val nRef = b.size
        val eps = max(epsilon, 1e-6)

        val logA = DoubleArray(nSrc) { i -> ln(max(a[i], 1e-300)) }
        val logB = DoubleArray(nRef) { j -> ln(max(b[j], 1e-300)) }

        var f = DoubleArray(nSrc)
        var g = DoubleArray(nRef)

        val tmpRow = DoubleArray(nRef)
        val tmpCol = DoubleArray(nSrc)

        for (iter in 0 until iterations) {
            // Update f: f_i = eps * (logA_i - logsumexp_j((g_j - cost_ij) / eps))
            val newF = DoubleArray(nSrc)
            for (i in 0 until nSrc) {
                for (j in 0 until nRef) {
                    tmpRow[j] = (g[j] - cost[i][j]) / eps
                }
                newF[i] = eps * (logA[i] - logsumexpRow(tmpRow))
            }
            f = newF

            // Update g: g_j = eps * (logB_j - logsumexp_i((f_i - cost_ij) / eps))
            val newG = DoubleArray(nRef)
            for (j in 0 until nRef) {
                for (i in 0 until nSrc) {
                    tmpCol[i] = (f[i] - cost[i][j]) / eps
                }
                newG[j] = eps * (logB[j] - logsumexpRow(tmpCol))
            }
            g = newG
        }

        val plan = Array(nSrc) { DoubleArray(nRef) }
        for (i in 0 until nSrc) {
            for (j in 0 until nRef) {
                plan[i][j] = exp((f[i] + g[j] - cost[i][j]) / eps)
            }
        }
        return plan
    }

    fun matchZones(
        source: ZonePalette,
        reference: ZonePalette,
        epsilon: Double = SINKHORN_EPSILON
    ): Map<Int, Map<Int, Float>> {
        val a = DoubleArray(source.zones.size) { i -> source.zones[i].area.toDouble() }
        val b = DoubleArray(reference.zones.size) { j -> reference.zones[j].area.toDouble() }

        var aSum = 0.0
        for (v in a) aSum += v
        for (i in a.indices) a[i] /= aSum

        var bSum = 0.0
        for (v in b) bSum += v
        for (j in b.indices) b[j] /= bSum

        val cost = zoneCost(source, reference)
        val plan = sinkhorn(cost, a, b, epsilon)

        val result = mutableMapOf<Int, Map<Int, Float>>()
        for (i in source.zones.indices) {
            val srcZone = source.zones[i]
            val row = plan[i]
            var rowSum = 0.0
            for (v in row) rowSum += v
            val denom = max(rowSum, 1e-300)

            val normRow = DoubleArray(reference.zones.size) { j -> row[j] / denom }
            var maxIdx = 0
            var maxVal = normRow[0]
            for (j in normRow.indices) {
                if (normRow[j] > maxVal) {
                    maxVal = normRow[j]
                    maxIdx = j
                }
            }

            val keep = BooleanArray(reference.zones.size) { j -> normRow[j] >= MIN_TARGET_WEIGHT }
            keep[maxIdx] = true

            var keepSum = 0.0
            for (j in keep.indices) {
                if (keep[j]) keepSum += normRow[j]
            }

            val zoneTargets = mutableMapOf<Int, Float>()
            for (j in keep.indices) {
                if (keep[j]) {
                    val refId = reference.zones[j].id
                    zoneTargets[refId] = (normRow[j] / keepSum).toFloat()
                }
            }
            result[srcZone.id] = zoneTargets
        }
        return result
    }
}
