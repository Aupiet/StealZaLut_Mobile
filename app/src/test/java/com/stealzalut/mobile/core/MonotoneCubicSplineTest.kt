package com.stealzalut.mobile.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonotoneCubicSplineTest {

    @Test
    fun `numeric parity with python evaluate_monotone_cubic_spline`() {
        val points = listOf(
            0.0f to 0.0f,
            0.25f to 0.15f,
            0.75f to 0.85f,
            1.0f to 1.0f
        )

        val y0 = MonotoneCubicSpline.evaluate(points, 0.1f)
        val y1 = MonotoneCubicSpline.evaluate(points, 0.5f)
        val y2 = MonotoneCubicSpline.evaluate(points, 0.9f)

        // Python output: [0.0504, 0.5, 0.9496]
        assertEquals(0.0504f, y0, 1e-3f)
        assertEquals(0.5f, y1, 1e-3f)
        assertEquals(0.9496f, y2, 1e-3f)
    }

    @Test
    fun `linear default diagonal evaluates identity`() {
        val diagonal = listOf(0.0f to 0.0f, 1.0f to 1.0f)
        for (i in 0..10) {
            val x = i / 10.0f
            assertEquals(x, MonotoneCubicSpline.evaluate(diagonal, x), 1e-5f)
        }
    }

    @Test
    fun `strictly preserves monotonicity without overshoots`() {
        val points = listOf(
            0.0f to 0.0f,
            0.2f to 0.1f,
            0.4f to 0.5f,
            0.8f to 0.9f,
            1.0f to 1.0f
        )

        var prevY = 0.0f
        for (i in 0..100) {
            val x = i / 100.0f
            val y = MonotoneCubicSpline.evaluate(points, x)
            assertTrue(y in 0.0f..1.0f, "Value y=$y must stay in [0, 1]")
            assertTrue(y >= prevY - 1e-6f, "Spline must be monotonic non-decreasing: $y >= $prevY at x=$x")
            prevY = y
        }
    }
}
