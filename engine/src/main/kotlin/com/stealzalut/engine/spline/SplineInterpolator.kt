package com.stealzalut.engine.spline

import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object SplineInterpolator {

    private const val EPSILON = 1e-6f

    fun evaluateMonotoneCubicSpline(
        xNodes: FloatArray,
        yNodes: FloatArray,
        xEval: FloatArray,
        output: FloatArray = FloatArray(xEval.size)
    ): FloatArray {
        val n = xNodes.size
        if (n < 2) {
            System.arraycopy(xEval, 0, output, 0, xEval.size)
            return output
        }

        val slopes = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            val dx = max(xNodes[i + 1] - xNodes[i], EPSILON)
            val dy = yNodes[i + 1] - yNodes[i]
            slopes[i] = dy / dx
        }

        val m = FloatArray(n)
        m[0] = slopes[0]
        m[n - 1] = slopes[n - 2]
        for (i in 1 until n - 1) {
            if (slopes[i - 1] * slopes[i] <= 0.0f) {
                m[i] = 0.0f
            } else {
                m[i] = (slopes[i - 1] + slopes[i]) * 0.5f
            }
        }

        // Fritsch & Carlson monotonicity adjustments
        for (i in 0 until n - 1) {
            if (abs(slopes[i]) < EPSILON) {
                m[i] = 0.0f
                m[i + 1] = 0.0f
            } else {
                val alpha = m[i] / slopes[i]
                val beta = m[i + 1] / slopes[i]
                val dist = alpha * alpha + beta * beta
                if (dist > 9.0f) {
                    val tau = 3.0f / sqrt(dist)
                    m[i] = tau * alpha * slopes[i]
                    m[i + 1] = tau * beta * slopes[i]
                }
            }
        }

        val minX = xNodes[0]
        val maxX = xNodes[n - 1]

        for (k in xEval.indices) {
            val flatX = xEval[k].coerceIn(minX, maxX)

            // Binary search to find interval
            var idx = Arrays.binarySearch(xNodes, flatX)
            if (idx >= 0) {
                if (idx >= n - 1) idx = n - 2
            } else {
                idx = -idx - 2
                if (idx < 0) idx = 0
                if (idx >= n - 1) idx = n - 2
            }

            val x0 = xNodes[idx]
            val x1 = xNodes[idx + 1]
            val y0 = yNodes[idx]
            val y1 = yNodes[idx + 1]
            val m0 = m[idx]
            val m1 = m[idx + 1]
            val h = max(x1 - x0, EPSILON)

            val t = (flatX - x0) / h
            val t2 = t * t
            val t3 = t2 * t

            val h00 = 2.0f * t3 - 3.0f * t2 + 1.0f
            val h10 = t3 - 2.0f * t2 + t
            val h01 = -2.0f * t3 + 3.0f * t2
            val h11 = t3 - t2

            val res = h00 * y0 + h10 * h * m0 + h01 * y1 + h11 * h * m1
            output[k] = res.coerceIn(0.0f, 1.0f)
        }

        return output
    }
}
