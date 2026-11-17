package com.stealzalut.engine.zones

import com.stealzalut.engine.color.ColorSpace
import java.security.MessageDigest
import java.util.Random
import kotlin.math.*

object KMeans {

    const val ANALYSIS_MAX_SIZE = 256
    const val DEFAULT_ZONE_COUNT = 8
    const val MIN_ZONE_COUNT = 2
    const val MAX_ZONE_COUNT = 16
    const val MIN_ZONE_AREA = 0.01f
    const val COV_REGULARIZATION = 1e-5f
    const val ALGORITHM_VERSION = "zones-2"

    fun toHex(labMean: FloatArray): String {
        val rgb = FloatArray(3)
        val comp = ColorSpace.gamutCompress(labMean.copyOf())
        ColorSpace.oklabToSrgb(comp[0], comp[1], comp[2], rgb, 0)
        val r = round(rgb[0] * 255.0f).toInt().coerceIn(0, 255)
        val g = round(rgb[1] * 255.0f).toInt().coerceIn(0, 255)
        val b = round(rgb[2] * 255.0f).toInt().coerceIn(0, 255)
        return String.format("#%02x%02x%02x", r, g, b)
    }

    fun resizeBilinear(src: ByteArray, w: Int, h: Int, targetW: Int, targetH: Int): ByteArray {
        if (w == targetW && h == targetH) return src
        val out = ByteArray(targetW * targetH * 3)
        val xRatio = (w - 1).toFloat() / max(1, targetW - 1)
        val yRatio = (h - 1).toFloat() / max(1, targetH - 1)

        for (y in 0 until targetH) {
            val yf = y * yRatio
            val y0 = yf.toInt()
            val y1 = min(y0 + 1, h - 1)
            val dy = yf - y0

            for (x in 0 until targetW) {
                val xf = x * xRatio
                val x0 = xf.toInt()
                val x1 = min(x0 + 1, w - 1)
                val dx = xf - x0

                val idx00 = (y0 * w + x0) * 3
                val idx10 = (y0 * w + x1) * 3
                val idx01 = (y1 * w + x0) * 3
                val idx11 = (y1 * w + x1) * 3

                val outIdx = (y * targetW + x) * 3
                for (c in 0 until 3) {
                    val p00 = src[idx00 + c].toInt() and 0xFF
                    val p10 = src[idx10 + c].toInt() and 0xFF
                    val p01 = src[idx01 + c].toInt() and 0xFF
                    val p11 = src[idx11 + c].toInt() and 0xFF

                    val v0 = p00 * (1.0f - dx) + p10 * dx
                    val v1 = p01 * (1.0f - dx) + p11 * dx
                    val v = v0 * (1.0f - dy) + v1 * dy
                    out[outIdx + c] = round(v).toInt().coerceIn(0, 255).toByte()
                }
            }
        }
        return out
    }

    fun analysisOklab(imageRgb: ByteArray, w: Int, h: Int): Triple<FloatArray, Int, Int> {
        val maxDim = max(w, h)
        val (smallRgb, smallW, smallH) = if (maxDim > ANALYSIS_MAX_SIZE) {
            val scale = ANALYSIS_MAX_SIZE.toFloat() / maxDim
            val targetW = max(1, round(w * scale).toInt())
            val targetH = max(1, round(h * scale).toInt())
            Triple(resizeBilinear(imageRgb, w, h, targetW, targetH), targetW, targetH)
        } else {
            Triple(imageRgb, w, h)
        }

        val n = smallW * smallH
        val lab = FloatArray(n * 3)
        for (i in 0 until n) {
            val r = (smallRgb[i * 3].toInt() and 0xFF) / 255.0f
            val g = (smallRgb[i * 3 + 1].toInt() and 0xFF) / 255.0f
            val b = (smallRgb[i * 3 + 2].toInt() and 0xFF) / 255.0f
            ColorSpace.rgbToOklab(r, g, b, lab, i * 3)
        }
        return Triple(lab, smallW, smallH)
    }

    fun colorStats(imageRgb: ByteArray, w: Int, h: Int): ColourStats {
        val (lab, _, _) = analysisOklab(imageRgb, w, h)
        val n = lab.size / 3
        var colouredCount = 0
        var aSum = 0.0
        var bSum = 0.0
        var chromaSum = 0.0

        for (i in 0 until n) {
            val a = lab[i * 3 + 1]
            val b = lab[i * 3 + 2]
            val ch = sqrt(a * a + b * b)
            if (ch >= 0.05f) {
                colouredCount++
                aSum += a
                bSum += b
                chromaSum += ch
            }
        }

        val fraction = colouredCount.toFloat() / n.toFloat()
        if (fraction < 0.05f || chromaSum <= 0.0) {
            return ColourStats(0.0f, fraction, 0.0f)
        }

        val resultant = (hypot(aSum, bSum) / chromaSum).toFloat()
        var hue = Math.toDegrees(atan2(bSum, aSum)).toFloat() % 360.0f
        if (hue < 0.0f) hue += 360.0f
        return ColourStats(resultant, fraction, hue)
    }

    fun kmeansOklab(points: FloatArray, k: Int, seed: Long = 0x5A17L): IntArray {
        val n = points.size / 3
        val rng = Random(seed)

        var bestLabels: IntArray? = null
        var bestInertia = Double.POSITIVE_INFINITY

        val centers = FloatArray(k * 3)
        val newCenters = FloatArray(k * 3)
        val clusterCounts = IntArray(k)
        val closest = DoubleArray(n)
        val labels = IntArray(n)

        for (attempt in 0 until 3) {
            // First center chosen uniformly at random
            val first = rng.nextInt(n)
            centers[0] = points[first * 3]
            centers[1] = points[first * 3 + 1]
            centers[2] = points[first * 3 + 2]

            for (i in 0 until n) {
                val dl = points[i * 3] - centers[0]
                val da = points[i * 3 + 1] - centers[1]
                val db = points[i * 3 + 2] - centers[2]
                closest[i] = (dl * dl + da * da + db * db).toDouble()
            }

            // K-means++ initialization
            for (index in 1 until k) {
                var total = 0.0
                for (i in 0 until n) total += closest[i]
                if (total <= 0.0) {
                    val sel = rng.nextInt(n)
                    centers[index * 3] = points[sel * 3]
                    centers[index * 3 + 1] = points[sel * 3 + 1]
                    centers[index * 3 + 2] = points[sel * 3 + 2]
                    continue
                }
                val threshold = rng.nextDouble() * total
                var cum = 0.0
                var selected = n - 1
                for (i in 0 until n) {
                    cum += closest[i]
                    if (cum >= threshold) {
                        selected = i
                        break
                    }
                }
                centers[index * 3] = points[selected * 3]
                centers[index * 3 + 1] = points[selected * 3 + 1]
                centers[index * 3 + 2] = points[selected * 3 + 2]

                for (i in 0 until n) {
                    val dl = points[i * 3] - centers[index * 3]
                    val da = points[i * 3 + 1] - centers[index * 3 + 1]
                    val db = points[i * 3 + 2] - centers[index * 3 + 2]
                    val dist = (dl * dl + da * da + db * db).toDouble()
                    if (dist < closest[i]) closest[i] = dist
                }
            }

            // Lloyd iterations (up to 50)
            for (iter in 0 until 50) {
                // Assign each point to closest center
                for (i in 0 until n) {
                    val pL = points[i * 3]
                    val pA = points[i * 3 + 1]
                    val pB = points[i * 3 + 2]

                    var minDist = Double.POSITIVE_INFINITY
                    var minIdx = 0
                    for (c in 0 until k) {
                        val dl = pL - centers[c * 3]
                        val da = pA - centers[c * 3 + 1]
                        val db = pB - centers[c * 3 + 2]
                        val dist = (dl * dl + da * da + db * db).toDouble()
                        if (dist < minDist) {
                            minDist = dist
                            minIdx = c
                        }
                    }
                    labels[i] = minIdx
                }

                // Update centers
                newCenters.fill(0f)
                clusterCounts.fill(0)
                for (i in 0 until n) {
                    val c = labels[i]
                    clusterCounts[c]++
                    newCenters[c * 3] += points[i * 3]
                    newCenters[c * 3 + 1] += points[i * 3 + 1]
                    newCenters[c * 3 + 2] += points[i * 3 + 2]
                }
                var maxShift = 0f
                for (c in 0 until k) {
                    if (clusterCounts[c] > 0) {
                        val cnt = clusterCounts[c].toFloat()
                        newCenters[c * 3] /= cnt
                        newCenters[c * 3 + 1] /= cnt
                        newCenters[c * 3 + 2] /= cnt
                    } else {
                        newCenters[c * 3] = centers[c * 3]
                        newCenters[c * 3 + 1] = centers[c * 3 + 1]
                        newCenters[c * 3 + 2] = centers[c * 3 + 2]
                    }
                    val dl = abs(newCenters[c * 3] - centers[c * 3])
                    val da = abs(newCenters[c * 3 + 1] - centers[c * 3 + 1])
                    val db = abs(newCenters[c * 3 + 2] - centers[c * 3 + 2])
                    maxShift = max(maxShift, max(dl, max(da, db)))
                }
                System.arraycopy(newCenters, 0, centers, 0, k * 3)
                if (maxShift <= 1e-5f) break
            }

            var inertia = 0.0
            for (i in 0 until n) {
                val c = labels[i]
                val dl = points[i * 3] - centers[c * 3]
                val da = points[i * 3 + 1] - centers[c * 3 + 1]
                val db = points[i * 3 + 2] - centers[c * 3 + 2]
                inertia += (dl * dl + da * da + db * db).toDouble()
            }

            if (inertia < bestInertia) {
                bestInertia = inertia
                bestLabels = labels.clone()
            }
        }
        return bestLabels ?: IntArray(n)
    }

    fun mergeSmallZones(lab: FloatArray, inLabels: IntArray, minArea: Float): IntArray {
        val labels = inLabels.clone()
        val total = labels.size.toFloat()

        while (true) {
            val counts = mutableMapOf<Int, Int>()
            for (l in labels) counts[l] = (counts[l] ?: 0) + 1
            if (counts.size <= 1) return labels

            var smallestLabel = -1
            var smallestCount = Int.MAX_VALUE
            for ((l, cnt) in counts) {
                if (cnt < smallestCount) {
                    smallestCount = cnt
                    smallestLabel = l
                }
            }

            if (smallestCount / total >= minArea) return labels

            // Compute OKLab means for present clusters
            val present = counts.keys.toList()
            val means = Array(present.size) { FloatArray(3) }
            for (i in labels.indices) {
                val pIdx = present.indexOf(labels[i])
                means[pIdx][0] += lab[i * 3]
                means[pIdx][1] += lab[i * 3 + 1]
                means[pIdx][2] += lab[i * 3 + 2]
            }
            for (p in present.indices) {
                val cnt = counts[present[p]]!!.toFloat()
                means[p][0] /= cnt
                means[p][1] /= cnt
                means[p][2] /= cnt
            }

            val smIdx = present.indexOf(smallestLabel)
            val smMean = means[smIdx]
            var nearestLabel = -1
            var nearestDist = Double.POSITIVE_INFINITY
            for (p in present.indices) {
                if (p == smIdx) continue
                val dl = means[p][0] - smMean[0]
                val da = means[p][1] - smMean[1]
                val db = means[p][2] - smMean[2]
                val dist = (dl * dl + da * da + db * db).toDouble()
                if (dist < nearestDist) {
                    nearestDist = dist
                    nearestLabel = present[p]
                }
            }

            for (i in labels.indices) {
                if (labels[i] == smallestLabel) {
                    labels[i] = nearestLabel
                }
            }
        }
    }

    fun analyzeZones(imageRgb: ByteArray, w: Int, h: Int, zoneCount: Int = DEFAULT_ZONE_COUNT): ZonePalette {
        val k = zoneCount.coerceIn(MIN_ZONE_COUNT, MAX_ZONE_COUNT)
        val (lab, smallW, smallH) = analysisOklab(imageRgb, w, h)
        val n = smallW * smallH

        val rawLabels = kmeansOklab(lab, min(k, n))
        val mergedLabels = mergeSmallZones(lab, rawLabels, MIN_ZONE_AREA)

        val present = mergedLabels.toSet().toList()
        if (present.size == 1) {
            val zoneMap = IntArray(n) { 1 }
            return ZonePalette(zoneStats(lab, zoneMap, smallW, smallH), zoneMap, smallW, smallH)
        }

        // Sort clusters by ascending mean L (stable sort)
        val meanL = present.map { cl ->
            var sumL = 0.0
            var cnt = 0
            for (i in 0 until n) {
                if (mergedLabels[i] == cl) {
                    sumL += lab[i * 3]
                    cnt++
                }
            }
            Pair(cl, sumL / max(1, cnt))
        }.sortedWith(Comparator { o1, o2 -> o1.second.compareTo(o2.second) })

        val remap = mutableMapOf<Int, Int>()
        for (idx in meanL.indices) {
            remap[meanL[idx].first] = idx + 1
        }

        val zoneMap = IntArray(n) { i -> remap[mergedLabels[i]] ?: 1 }
        return ZonePalette(zoneStats(lab, zoneMap, smallW, smallH), zoneMap, smallW, smallH)
    }

    fun zoneStats(lab: FloatArray, zoneMap: IntArray, w: Int, h: Int): List<Zone> {
        val total = zoneMap.size.toFloat()
        var maxId = 0
        for (z in zoneMap) if (z > maxId) maxId = z

        val counts = IntArray(maxId + 1)
        for (z in zoneMap) counts[z]++

        val areas = FloatArray(maxId + 1) { z -> counts[z] / total }
        val ranks = FloatArray(maxId + 1)
        var cumArea = 0f
        for (z in 1..maxId) {
            ranks[z] = cumArea + areas[z] / 2.0f
            cumArea += areas[z]
        }

        val zones = mutableListOf<Zone>()
        for (zoneId in 1..maxId) {
            val count = counts[zoneId]
            if (count == 0) continue

            val mean = FloatArray(3)
            var sumX = 0.0
            var sumY = 0.0
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE

            for (i in zoneMap.indices) {
                if (zoneMap[i] == zoneId) {
                    mean[0] += lab[i * 3]
                    mean[1] += lab[i * 3 + 1]
                    mean[2] += lab[i * 3 + 2]

                    val px = i % w
                    val py = i / w
                    sumX += px
                    sumY += py
                    if (px < minX) minX = px
                    if (py < minY) minY = py
                    if (px > maxX) maxX = px
                    if (py > maxY) maxY = py
                }
            }

            mean[0] /= count.toFloat()
            mean[1] /= count.toFloat()
            mean[2] /= count.toFloat()

            val cov = FloatArray(9)
            if (count > 1) {
                val factor = 1.0f / (count - 1).toFloat()
                for (i in zoneMap.indices) {
                    if (zoneMap[i] == zoneId) {
                        val d0 = lab[i * 3] - mean[0]
                        val d1 = lab[i * 3 + 1] - mean[1]
                        val d2 = lab[i * 3 + 2] - mean[2]

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
                }
                for (k in 0 until 9) cov[k] *= factor
            }
            // Add regularization COV_REGULARIZATION * I
            cov[0] += COV_REGULARIZATION
            cov[4] += COV_REGULARIZATION
            cov[8] += COV_REGULARIZATION

            val centroid = Pair((sumX / count / w * 100.0).toFloat(), (sumY / count / h * 100.0).toFloat())
            val bbox = floatArrayOf(
                (minX.toFloat() / w * 100.0f),
                (minY.toFloat() / h * 100.0f),
                ((maxX + 1).toFloat() / w * 100.0f),
                ((maxY + 1).toFloat() / h * 100.0f)
            )

            zones.add(
                Zone(
                    id = zoneId,
                    mean = mean,
                    cov = cov,
                    area = areas[zoneId],
                    lRank = ranks[zoneId],
                    centroidXy = centroid,
                    bbox = bbox,
                    hex = toHex(mean)
                )
            )
        }
        return zones
    }

    fun analysisId(sourceRgb: ByteArray, srcW: Int, srcH: Int, referenceRgb: ByteArray, refW: Int, refH: Int, zoneCount: Int): String {
        val k = zoneCount.coerceIn(MIN_ZONE_COUNT, MAX_ZONE_COUNT)
        val md = MessageDigest.getInstance("SHA-256")
        for ((bytes, w, h) in listOf(Triple(sourceRgb, srcW, srcH), Triple(referenceRgb, refW, refH))) {
            val maxDim = max(w, h)
            val (small, sw, sh) = if (maxDim > ANALYSIS_MAX_SIZE) {
                val scale = ANALYSIS_MAX_SIZE.toFloat() / maxDim
                val tw = max(1, round(w * scale).toInt())
                val th = max(1, round(h * scale).toInt())
                Triple(resizeBilinear(bytes, w, h, tw, th), tw, th)
            } else {
                Triple(bytes, w, h)
            }
            md.update("($sh, $sw, 3)".toByteArray())
            md.update(small)
        }
        md.update("$k|$ALGORITHM_VERSION".toByteArray())
        val digest = md.digest()
        val sb = StringBuilder()
        for (i in 0 until 4) {
            sb.append(String.format("%02x", digest[i]))
        }
        return sb.toString()
    }
}
