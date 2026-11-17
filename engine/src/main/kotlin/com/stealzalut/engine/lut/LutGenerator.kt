package com.stealzalut.engine.lut

object LutGenerator {
    fun generateIdentityGrid(size: Int): FloatArray {
        val count = size * size * size
        val grid = FloatArray(count * 3)
        val step = if (size > 1) 1.0f / (size - 1).toFloat() else 0.0f

        var idx = 0
        for (b in 0 until size) {
            val bVal = b * step
            for (g in 0 until size) {
                val gVal = g * step
                for (r in 0 until size) {
                    val rVal = r * step
                    grid[idx] = rVal
                    grid[idx + 1] = gVal
                    grid[idx + 2] = bVal
                    idx += 3
                }
            }
        }
        return grid
    }
}
