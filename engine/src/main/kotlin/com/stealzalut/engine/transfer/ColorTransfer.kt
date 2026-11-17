package com.stealzalut.engine.transfer

import com.stealzalut.engine.color.CieLab
import com.stealzalut.engine.color.ColorSpace
import java.util.Arrays
import kotlin.math.*

object ColorTransfer {

    const val MIN_STD_RATIO = 0.5f
    const val MAX_STD_RATIO = 2.0f
    const val SOFT_CLIP_SHADOW_THRESHOLD = 0.05f
    const val SOFT_CLIP_HIGHLIGHT_THRESHOLD = 0.95f

    /**
     * Classical Jacobi eigenvalue algorithm for symmetric NxN matrices (N <= 3).
     * Returns eigenvalues (sorted ascending) and corresponding orthonormal eigenvectors.
     */
    fun jacobiEigh(aIn: DoubleArray, n: Int, maxSweeps: Int = 50, tol: Double = 1e-15): Pair<DoubleArray, Array<DoubleArray>> {
        val a = Array(n) { r -> DoubleArray(n) { c -> aIn[r * n + c] } }
        val v = Array(n) { r -> DoubleArray(n) { c -> if (r == c) 1.0 else 0.0 } }

        for (iter in 0 until maxSweeps) {
            var off = 0.0
            for (p in 0 until n) {
                for (q in p + 1 until n) {
                    off += abs(a[p][q])
                }
            }
            if (off < tol) break

            for (p in 0 until n) {
                for (q in p + 1 until n) {
                    val apq = a[p][q]
                    if (abs(apq) < 1e-15) continue
                    val theta = (a[q][q] - a[p][p]) / (2.0 * apq)
                    val t = (if (theta >= 0.0) 1.0 else -1.0) / (abs(theta) + sqrt(theta * theta + 1.0))
                    val c = 1.0 / sqrt(t * t + 1.0)
                    val s = t * c
                    val tau = s / (1.0 + c)
                    val h = t * apq
                    a[p][p] -= h
                    a[q][q] += h
                    a[p][q] = 0.0
                    a[q][p] = 0.0

                    for (r in 0 until n) {
                        if (r != p && r != q) {
                            val arp = a[r][p]
                            val arq = a[r][q]
                            a[r][p] = arp - s * (arq + tau * arp)
                            a[p][r] = a[r][p]
                            a[r][q] = arq + s * (arp - tau * arq)
                            a[q][r] = a[r][q]
                        }
                    }
                    for (r in 0 until n) {
                        val vrp = v[r][p]
                        val vrq = v[r][q]
                        v[r][p] = vrp - s * (vrq + tau * vrp)
                        v[r][q] = vrq + s * (vrp - tau * vrq)
                    }
                }
            }
        }

        val w = DoubleArray(n) { i -> a[i][i] }
        val order = Array(n) { it }
        order.sortWith(Comparator { i1, i2 -> w[i1].compareTo(w[i2]) })

        val sortedW = DoubleArray(n) { i -> w[order[i]] }
        val sortedV = Array(n) { r -> DoubleArray(n) { c -> v[r][order[c]] } }
        return Pair(sortedW, sortedV)
    }

    fun computeMklMatrix(covSrc: DoubleArray, covRef: DoubleArray, n: Int = 3): FloatArray {
        val eps = 1e-6
        val covS = DoubleArray(n * n) { i -> covSrc[i] + if (i % (n + 1) == 0) eps else 0.0 }
        val covR = DoubleArray(n * n) { i -> covRef[i] + if (i % (n + 1) == 0) eps else 0.0 }

        val (eigValR, eigVecR) = jacobiEigh(covS, n)
        val valR = DoubleArray(n) { i -> sqrt(max(eigValR[i], 0.0)) }
        val invR = DoubleArray(n) { i -> 1.0 / (valR[i] + 1e-7) }

        val matC = DoubleArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    for (l in 0 until n) {
                        sum += eigVecR[k][i] * covR[k * n + l] * eigVecR[l][j]
                    }
                }
                matC[i * n + j] = valR[i] * sum * valR[j]
            }
        }

        val (eigValC, eigVecC) = jacobiEigh(matC, n)
        val valC = DoubleArray(n) { i -> sqrt(max(eigValC[i], 0.0)) }

        val tMat = FloatArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    for (l in 0 until n) {
                        var mKl = 0.0
                        for (m in 0 until n) {
                            mKl += eigVecC[k][m] * valC[m] * eigVecC[l][m]
                        }
                        sum += eigVecR[i][k] * invR[k] * mKl * invR[l] * eigVecR[j][l]
                    }
                }
                tMat[i * n + j] = sum.toFloat()
            }
        }
        return tMat
    }

    fun computeMvgdMatrix(covSrc: DoubleArray, covRef: DoubleArray, n: Int = 3): FloatArray {
        val eps = 1e-6
        val covS = DoubleArray(n * n) { i -> covSrc[i] + if (i % (n + 1) == 0) eps else 0.0 }
        val covR = DoubleArray(n * n) { i -> covRef[i] + if (i % (n + 1) == 0) eps else 0.0 }

        val (valS, vecS) = jacobiEigh(covS, n)
        val invSqrtS = DoubleArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    val invSqrt = 1.0 / sqrt(max(valS[k], 0.0) + 1e-7)
                    sum += vecS[i][k] * invSqrt * vecS[j][k]
                }
                invSqrtS[i * n + j] = sum
            }
        }

        val (valR, vecR) = jacobiEigh(covR, n)
        val sqrtR = DoubleArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    val sq = sqrt(max(valR[k], 0.0))
                    sum += vecR[i][k] * sq * vecR[j][k]
                }
                sqrtR[i * n + j] = sum
            }
        }

        val tMat = FloatArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    sum += sqrtR[i * n + k] * invSqrtS[k * n + j]
                }
                tMat[i * n + j] = sum.toFloat()
            }
        }
        return tMat
    }

    fun boundSymmetric(matrix: FloatArray, n: Int, lo: Float, hi: Float): FloatArray {
        val sym = DoubleArray(n * n) { idx ->
            val r = idx / n
            val c = idx % n
            0.5 * (matrix[r * n + c] + matrix[c * n + r]).toDouble()
        }
        val (eigvals, eigvecs) = jacobiEigh(sym, n)
        val out = FloatArray(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until n) {
                    val clipped = eigvals[k].coerceIn(lo.toDouble(), hi.toDouble())
                    sum += eigvecs[i][k] * clipped * eigvecs[j][k]
                }
                out[i * n + j] = sum.toFloat()
            }
        }
        return out
    }

    fun applySoftClip(x: FloatArray, shadowTh: Float = SOFT_CLIP_SHADOW_THRESHOLD, highlightTh: Float = SOFT_CLIP_HIGHLIGHT_THRESHOLD) {
        val mHi = 1.0f - highlightTh
        val mLo = shadowTh
        for (i in x.indices) {
            val v = x[i]
            if (v > highlightTh) {
                x[i] = (highlightTh + mHi * tanh((v - highlightTh) / mHi)).coerceIn(0f, 1f)
            } else if (v < shadowTh) {
                x[i] = (shadowTh - mLo * tanh((shadowTh - v) / mLo)).coerceIn(0f, 1f)
            } else {
                x[i] = v.coerceIn(0f, 1f)
            }
        }
    }

    fun histogramMatching1D(src: FloatArray, ref: FloatArray, target: FloatArray): FloatArray {
        val nSrc = src.size / 3
        val nRef = ref.size / 3
        val nTgt = target.size / 3
        val out = FloatArray(target.size)

        val sC = FloatArray(nSrc)
        val rC = FloatArray(nRef)
        val sVals = FloatArray(256)
        val rVals = FloatArray(256)

        for (ch in 0 until 3) {
            for (i in 0 until nSrc) sC[i] = src[i * 3 + ch].coerceIn(0f, 1f)
            for (i in 0 until nRef) rC[i] = ref[i * 3 + ch].coerceIn(0f, 1f)
            Arrays.sort(sC)
            Arrays.sort(rC)

            for (k in 0 until 256) {
                val q = k / 255.0

                val idxS = q * (nSrc - 1)
                val iS = idxS.toInt()
                val fS = (idxS - iS).toFloat()
                sVals[k] = if (iS >= nSrc - 1) sC[nSrc - 1] else ((1f - fS) * sC[iS] + fS * sC[iS + 1])

                val idxR = q * (nRef - 1)
                val iR = idxR.toInt()
                val fR = (idxR - iR).toFloat()
                rVals[k] = if (iR >= nRef - 1) rC[nRef - 1] else ((1f - fR) * rC[iR] + fR * rC[iR + 1])
            }

            for (i in 0 until nTgt) {
                val v = target[i * 3 + ch].coerceIn(0f, 1f)
                val mapped = if (v <= sVals[0]) {
                    rVals[0]
                } else if (v >= sVals[255]) {
                    rVals[255]
                } else {
                    var idx = Arrays.binarySearch(sVals, v)
                    if (idx >= 0) {
                        rVals[idx]
                    } else {
                        val ins = -idx - 1
                        val k = (ins - 1).coerceIn(0, 254)
                        val denom = sVals[k + 1] - sVals[k]
                        val u = if (abs(denom) > 1e-12f) (v - sVals[k]) / denom else 0f
                        (1f - u) * rVals[k] + u * rVals[k + 1]
                    }
                }
                out[i * 3 + ch] = mapped.coerceIn(0f, 1f)
            }
        }
        return out
    }

    fun reinhardLabTransfer(
        src: FloatArray,
        ref: FloatArray,
        target: FloatArray,
        preserveLuminance: Float = 0f
    ): FloatArray {
        val nSrc = src.size / 3
        val nRef = ref.size / 3
        val nTgt = target.size / 3

        val srcLab = FloatArray(src.size)
        val refLab = FloatArray(ref.size)
        val tgtLab = FloatArray(target.size)

        CieLab.rgbToLab(src, srcLab)
        CieLab.rgbToLab(ref, refLab)
        CieLab.rgbToLab(target, tgtLab)

        val muS = DoubleArray(3)
        val muR = DoubleArray(3)
        for (i in 0 until nSrc) {
            muS[0] += srcLab[i * 3]
            muS[1] += srcLab[i * 3 + 1]
            muS[2] += srcLab[i * 3 + 2]
        }
        for (c in 0 until 3) muS[c] /= nSrc.toDouble()

        for (i in 0 until nRef) {
            muR[0] += refLab[i * 3]
            muR[1] += refLab[i * 3 + 1]
            muR[2] += refLab[i * 3 + 2]
        }
        for (c in 0 until 3) muR[c] /= nRef.toDouble()

        val stdS = DoubleArray(3)
        val stdR = DoubleArray(3)
        for (i in 0 until nSrc) {
            for (c in 0 until 3) {
                val d = srcLab[i * 3 + c] - muS[c]
                stdS[c] += d * d
            }
        }
        for (c in 0 until 3) stdS[c] = sqrt(stdS[c] / nSrc.toDouble())

        for (i in 0 until nRef) {
            for (c in 0 until 3) {
                val d = refLab[i * 3 + c] - muR[c]
                stdR[c] += d * d
            }
        }
        for (c in 0 until 3) stdR[c] = sqrt(stdR[c] / nRef.toDouble())

        val stdRatio = FloatArray(3) { c ->
            (stdR[c] / (stdS[c] + 1e-6)).toFloat().coerceIn(MIN_STD_RATIO, MAX_STD_RATIO)
        }

        val outLab = FloatArray(target.size)
        val clampedPreserve = preserveLuminance.coerceIn(0f, 1f)

        for (i in 0 until nTgt) {
            val idx = i * 3
            var l = (tgtLab[idx] - muS[0].toFloat()) * stdRatio[0] + muR[0].toFloat()
            val a = (tgtLab[idx + 1] - muS[1].toFloat()) * stdRatio[1] + muR[1].toFloat()
            val b = (tgtLab[idx + 2] - muS[2].toFloat()) * stdRatio[2] + muR[2].toFloat()

            if (clampedPreserve > 0f) {
                l = clampedPreserve * tgtLab[idx] + (1f - clampedPreserve) * l
            }
            outLab[idx] = l.coerceIn(0f, 100f)
            outLab[idx + 1] = a
            outLab[idx + 2] = b
        }

        val outRgb = FloatArray(target.size)
        CieLab.labToRgb(outLab, outRgb)
        for (i in outRgb.indices) outRgb[i] = outRgb[i].coerceIn(0f, 1f)
        return outRgb
    }

    fun oklabTransfer(
        src: FloatArray,
        ref: FloatArray,
        target: FloatArray,
        preserveLuminance: Float = 0f
    ): FloatArray {
        val nSrc = src.size / 3
        val nRef = ref.size / 3
        val nTgt = target.size / 3

        val srcLab = FloatArray(src.size)
        val refLab = FloatArray(ref.size)
        val tgtLab = FloatArray(target.size)

        for (i in 0 until nSrc) ColorSpace.srgbToOklab(src[i * 3], src[i * 3 + 1], src[i * 3 + 2], srcLab, i * 3)
        for (i in 0 until nRef) ColorSpace.srgbToOklab(ref[i * 3], ref[i * 3 + 1], ref[i * 3 + 2], refLab, i * 3)
        for (i in 0 until nTgt) ColorSpace.srgbToOklab(target[i * 3], target[i * 3 + 1], target[i * 3 + 2], tgtLab, i * 3)

        val muS = DoubleArray(3)
        val muR = DoubleArray(3)
        for (i in 0 until nSrc) {
            muS[0] += srcLab[i * 3]
            muS[1] += srcLab[i * 3 + 1]
            muS[2] += srcLab[i * 3 + 2]
        }
        for (c in 0 until 3) muS[c] /= nSrc.toDouble()

        for (i in 0 until nRef) {
            muR[0] += refLab[i * 3]
            muR[1] += refLab[i * 3 + 1]
            muR[2] += refLab[i * 3 + 2]
        }
        for (c in 0 until 3) muR[c] /= nRef.toDouble()

        val stdS = DoubleArray(3)
        val stdR = DoubleArray(3)
        for (i in 0 until nSrc) {
            for (c in 0 until 3) {
                val d = srcLab[i * 3 + c] - muS[c]
                stdS[c] += d * d
            }
        }
        for (c in 0 until 3) stdS[c] = sqrt(stdS[c] / nSrc.toDouble())

        for (i in 0 until nRef) {
            for (c in 0 until 3) {
                val d = refLab[i * 3 + c] - muR[c]
                stdR[c] += d * d
            }
        }
        for (c in 0 until 3) stdR[c] = sqrt(stdR[c] / nRef.toDouble())

        val scale = FloatArray(3) { c ->
            ((stdR[c] + 1e-6) / (stdS[c] + 1e-6)).toFloat().coerceIn(MIN_STD_RATIO, MAX_STD_RATIO)
        }

        val outLab = FloatArray(target.size)
        val clampedPreserve = preserveLuminance.coerceIn(0f, 1f)

        for (i in 0 until nTgt) {
            val idx = i * 3
            var l = (tgtLab[idx] - muS[0].toFloat()) * scale[0] + muR[0].toFloat()
            val a = (tgtLab[idx + 1] - muS[1].toFloat()) * scale[1] + muR[1].toFloat()
            val b = (tgtLab[idx + 2] - muS[2].toFloat()) * scale[2] + muR[2].toFloat()

            if (clampedPreserve > 0f) {
                l = clampedPreserve * tgtLab[idx] + (1f - clampedPreserve) * l
            }
            outLab[idx] = l.coerceIn(0f, 1f)
            outLab[idx + 1] = a
            outLab[idx + 2] = b
        }

        ColorSpace.gamutCompress(outLab)
        for (i in 0 until nTgt) outLab[i * 3] = outLab[i * 3].coerceIn(0f, 1f)

        val outRgb = FloatArray(target.size)
        for (i in 0 until nTgt) ColorSpace.oklabToSrgb(outLab[i * 3], outLab[i * 3 + 1], outLab[i * 3 + 2], outRgb, i * 3)
        return outRgb
    }

    fun computeCovariance(points: FloatArray, mu: DoubleArray): DoubleArray {
        val n = points.size / 3
        val cov = DoubleArray(9)
        if (n <= 1) return cov
        val factor = 1.0 / (n - 1).toDouble()
        for (i in 0 until n) {
            val d0 = points[i * 3] - mu[0]
            val d1 = points[i * 3 + 1] - mu[1]
            val d2 = points[i * 3 + 2] - mu[2]

            cov[0] += d0 * d0
            cov[1] += d0 * d1
            cov[2] += d0 * d2
            cov[3] += d1 * d0
            cov[4] += d1 * d1
            cov[5] += d1 * d2
            cov[6] += d2 * d0
            cov[7] += d2 * d1
            cov[8] += d2 * d2
        }
        for (k in 0 until 9) cov[k] *= factor
        return cov
    }

    fun mklTransfer(
        src: FloatArray,
        ref: FloatArray,
        target: FloatArray,
        method: String = "mkl"
    ): FloatArray {
        val nSrc = src.size / 3
        val nRef = ref.size / 3
        val nTgt = target.size / 3

        val muS = DoubleArray(3)
        val muR = DoubleArray(3)
        for (i in 0 until nSrc) {
            muS[0] += src[i * 3]
            muS[1] += src[i * 3 + 1]
            muS[2] += src[i * 3 + 2]
        }
        for (c in 0 until 3) muS[c] /= nSrc.toDouble()

        for (i in 0 until nRef) {
            muR[0] += ref[i * 3]
            muR[1] += ref[i * 3 + 1]
            muR[2] += ref[i * 3 + 2]
        }
        for (c in 0 until 3) muR[c] /= nRef.toDouble()

        val covS = computeCovariance(src, muS)
        val covR = computeCovariance(ref, muR)

        val tMat = if (method.lowercase() == "mvgd") computeMvgdMatrix(covS, covR) else computeMklMatrix(covS, covR)

        val out = FloatArray(target.size)
        val m00 = tMat[0]
        val m01 = tMat[1]
        val m02 = tMat[2]
        val m10 = tMat[3]
        val m11 = tMat[4]
        val m12 = tMat[5]
        val m20 = tMat[6]
        val m21 = tMat[7]
        val m22 = tMat[8]

        val muS0 = muS[0].toFloat()
        val muS1 = muS[1].toFloat()
        val muS2 = muS[2].toFloat()
        val muR0 = muR[0].toFloat()
        val muR1 = muR[1].toFloat()
        val muR2 = muR[2].toFloat()

        for (i in 0 until nTgt) {
            val d0 = target[i * 3] - muS0
            val d1 = target[i * 3 + 1] - muS1
            val d2 = target[i * 3 + 2] - muS2

            out[i * 3] = (d0 * m00 + d1 * m01 + d2 * m02 + muR0).coerceIn(0f, 1f)
            out[i * 3 + 1] = (d0 * m10 + d1 * m11 + d2 * m12 + muR1).coerceIn(0f, 1f)
            out[i * 3 + 2] = (d0 * m20 + d1 * m21 + d2 * m22 + muR2).coerceIn(0f, 1f)
        }
        return out
    }

    fun fitAndApplyTransfer(
        src: FloatArray,
        ref: FloatArray,
        target: FloatArray? = null,
        method: String = "reinhard",
        preserveLuminance: Float = 0f,
        softClip: Boolean = false
    ): FloatArray {
        val tgt = target ?: src
        val meth = method.lowercase()
        var result: FloatArray = when (meth) {
            "reinhard" -> reinhardLabTransfer(src, ref, tgt, preserveLuminance)
            "oklab" -> oklabTransfer(src, ref, tgt, preserveLuminance)
            "hm" -> histogramMatching1D(src, ref, tgt)
            "mkl", "mvgd" -> mklTransfer(src, ref, tgt, meth)
            "hm-mkl-hm", "hm-mvgd-hm" -> {
                val sHm1 = histogramMatching1D(src, ref, src)
                val tHm1 = histogramMatching1D(src, ref, tgt)
                val subMeth = if (meth == "hm-mkl-hm") "mkl" else "mvgd"
                val sMkl = mklTransfer(sHm1, ref, sHm1, subMeth)
                val tMkl = mklTransfer(sHm1, ref, tHm1, subMeth)
                histogramMatching1D(sMkl, ref, tMkl)
            }
            else -> reinhardLabTransfer(src, ref, tgt, preserveLuminance)
        }

        val clampedPreserve = preserveLuminance.coerceIn(0f, 1f)
        if (clampedPreserve > 0f && meth != "reinhard") {
            val tgtLab = FloatArray(tgt.size)
            val resLab = FloatArray(result.size)
            CieLab.rgbToLab(tgt, tgtLab)
            CieLab.rgbToLab(result, resLab)
            val n = tgt.size / 3
            for (i in 0 until n) {
                resLab[i * 3] = clampedPreserve * tgtLab[i * 3] + (1f - clampedPreserve) * resLab[i * 3]
            }
            CieLab.labToRgb(resLab, result)
        }

        if (softClip) {
            applySoftClip(result)
        } else {
            for (i in result.indices) result[i] = result[i].coerceIn(0f, 1f)
        }
        return result
    }

    fun fitAndApplyTransferBytes(
        src: ByteArray,
        ref: ByteArray,
        target: ByteArray? = null,
        method: String = "reinhard",
        preserveLuminance: Float = 0f,
        softClip: Boolean = false
    ): ByteArray {
        val nSrc = src.size / 3
        val nRef = ref.size / 3
        val nTgt = (target?.size ?: src.size) / 3

        val srcF = FloatArray(src.size) { i -> (src[i].toInt() and 0xFF) / 255.0f }
        val refF = FloatArray(ref.size) { i -> (ref[i].toInt() and 0xFF) / 255.0f }
        val tgtF = if (target == null) null else FloatArray(target.size) { i -> (target[i].toInt() and 0xFF) / 255.0f }

        val resF = fitAndApplyTransfer(srcF, refF, tgtF, method, preserveLuminance, softClip)
        val out = ByteArray(resF.size)
        for (i in resF.indices) {
            out[i] = (round(resF[i] * 255.0f).toInt().coerceIn(0, 255)).toByte()
        }
        return out
    }
}
