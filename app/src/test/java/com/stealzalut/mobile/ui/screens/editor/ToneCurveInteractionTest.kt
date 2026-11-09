package com.stealzalut.mobile.ui.screens.editor

import com.stealzalut.mobile.core.MonotoneCubicSpline
import com.stealzalut.mobile.data.model.ToneCurveModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToneCurveInteractionTest {

    @Test
    fun `default tone curve model starts with identity endpoints`() {
        val model = ToneCurveModel()
        assertEquals(2, model.rgbPoints.size)
        assertEquals(0.0f to 0.0f, model.rgbPoints.first())
        assertEquals(1.0f to 1.0f, model.rgbPoints.last())
    }

    @Test
    fun `x-monotonicity constraint forbids crossing adjacent control points`() {
        val points = mutableListOf(
            0.0f to 0.0f,
            0.3f to 0.2f,
            0.7f to 0.8f,
            1.0f to 1.0f
        )

        val eps = 0.02f
        val idx = 1 // moving point at 0.3
        val minX = points[idx - 1].first + eps // 0.02
        val maxX = points[idx + 1].first - eps // 0.68

        // Attempting to move point 1 past point 2 (e.g. to 0.85) should be constrained to maxX (0.68)
        val candidateXPastRight = 0.85f
        val constrainedXRight = candidateXPastRight.coerceIn(minX, maxX)
        assertEquals(0.68f, constrainedXRight, 1e-4f)

        // Attempting to move point 1 past point 0 (e.g. to -0.1) should be constrained to minX (0.02)
        val candidateXPastLeft = -0.1f
        val constrainedXLeft = candidateXPastLeft.coerceIn(minX, maxX)
        assertEquals(0.02f, constrainedXLeft, 1e-4f)
    }

    @Test
    fun `endpoints 0 and last cannot be deleted and maintain fixed X boundaries`() {
        val points = mutableListOf(0.0f to 0.0f, 0.5f to 0.5f, 1.0f to 1.0f)

        // Deletion rule: only index > 0 and < size - 1 can be removed
        fun canRemove(i: Int, list: List<Pair<Float, Float>>): Boolean =
            list.size > 2 && i > 0 && i < list.size - 1

        assertFalse(canRemove(0, points), "Endpoint 0 cannot be removed")
        assertFalse(canRemove(2, points), "Endpoint N-1 cannot be removed")
        assertTrue(canRemove(1, points), "Intermediate point can be removed")

        // Endpoint X-lock rule
        fun constrainX(i: Int, candidateX: Float, list: List<Pair<Float, Float>>): Float =
            when (i) {
                0 -> 0.0f
                list.size - 1 -> 1.0f
                else -> candidateX.coerceIn(list[i - 1].first + 0.02f, list[i + 1].first - 0.02f)
            }

        assertEquals(0.0f, constrainX(0, 0.45f, points))
        assertEquals(1.0f, constrainX(2, 0.65f, points))
    }

    @Test
    fun `complex S-curve evaluation maintains monotonicity and bounded output`() {
        val sCurve = listOf(
            0.0f to 0.0f,
            0.25f to 0.15f,
            0.5f to 0.5f,
            0.75f to 0.85f,
            1.0f to 1.0f
        )

        val samples = MonotoneCubicSpline.evaluateCurve(sCurve, steps = 100)
        assertEquals(101, samples.size)

        var prevY = -1.0f
        for ((x, y) in samples) {
            assertTrue(x in 0.0f..1.0f)
            assertTrue(y in 0.0f..1.0f, "Y must remain inside [0, 1] without overshoot: was $y at x=$x")
            assertTrue(y >= prevY - 1e-5f, "Spline must be monotonically non-decreasing: $y >= $prevY")
            prevY = y
        }
    }
}
