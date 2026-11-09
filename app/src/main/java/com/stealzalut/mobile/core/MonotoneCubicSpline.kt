package com.stealzalut.mobile.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Monotone Cubic Hermite Spline Interpolator (Fritsch-Carlson 1980).
 * Provides mathematically identical curve interpolation between Android Compose UI and
 * Python Chaquopy backend (stealzalut.core.spline_interpolator).
 * Prevents non-monotonic overshoot, ringing, and color inversions.
 */
object MonotoneCubicSpline {

    private const val EPSILON = 1e-6f

    /**
     * Evaluates a list of sorted control points at [xEval] using Fritsch-Carlson cubic Hermite interpolation.
     */
    fun evaluate(points: List<Pair<Float, Float>>, xEval: Float): Float {
        if (points.isEmpty()) return xEval.coerceIn(0f, 1f)
        if (points.size == 1) return points[0].second.coerceIn(0f, 1f)

        val n = points.size
        val xNodes = FloatArray(n) { points[it].first }
        val yNodes = FloatArray(n) { points[it].second }

        // Differences and secular slopes
        val dx = FloatArray(n - 1) { xNodes[it + 1] - xNodes[it] }
        val dy = FloatArray(n - 1) { yNodes[it + 1] - yNodes[it] }
        val slopes = FloatArray(n - 1) { dy[it] / max(dx[it], EPSILON) }

        // Tangents at nodes
        val m = FloatArray(n)
        m[0] = slopes[0]
        m[n - 1] = slopes[n - 2]
        for (i in 1 until n - 1) {
            if (slopes[i - 1] * slopes[i] <= 0f) {
                m[i] = 0f
            } else {
                m[i] = (slopes[i - 1] + slopes[i]) / 2f
            }
        }

        // Fritsch & Carlson monotonicity adjustments
        for (i in 0 until n - 1) {
            if (abs(slopes[i]) < EPSILON) {
                m[i] = 0f
                m[i + 1] = 0f
            } else {
                val alpha = m[i] / slopes[i]
                val beta = m[i + 1] / slopes[i]
                val dist = alpha * alpha + beta * beta
                if (dist > 9f) {
                    val tau = 3f / sqrt(dist)
                    m[i] = tau * alpha * slopes[i]
                    m[i + 1] = tau * beta * slopes[i]
                }
            }
        }

        // Clamp xEval to knot range
        val x = xEval.coerceIn(xNodes[0], xNodes[n - 1])

        // Locate segment
        var k = 0
        for (i in 0 until n - 1) {
            if (x >= xNodes[i] && x <= xNodes[i + 1]) {
                k = i
                break
            }
        }

        val x0 = xNodes[k]
        val x1 = xNodes[k + 1]
        val y0 = yNodes[k]
        val y1 = yNodes[k + 1]
        val m0 = m[k]
        val m1 = m[k + 1]
        val h = x1 - x0

        val t = (x - x0) / max(h, EPSILON)
        val t2 = t * t
        val t3 = t2 * t

        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + t
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2

        val result = h00 * y0 + h10 * h * m0 + h01 * y1 + h11 * h * m1
        return result.coerceIn(0f, 1f)
    }

    /**
     * Evaluates the spline across [steps] evenly spaced points from 0f to 1f for rendering.
     */
    fun evaluateCurve(points: List<Pair<Float, Float>>, steps: Int = 100): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        val sorted = points.sortedBy { it.first }
        if (sorted.size == 2 && sorted.first().first == 0f && sorted.first().second == 0f &&
            sorted.last().first == 1f && sorted.last().second == 1f) {
            return listOf(0f to 0f, 1f to 1f)
        }

        val result = ArrayList<Pair<Float, Float>>(steps + 1)
        for (i in 0..steps) {
            val x = i.toFloat() / steps.toFloat()
            val y = evaluate(sorted, x)
            result.add(x to y)
        }
        return result
    }
}
