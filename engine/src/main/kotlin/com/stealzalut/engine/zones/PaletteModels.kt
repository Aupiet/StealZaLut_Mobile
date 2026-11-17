package com.stealzalut.engine.zones

data class Zone(
    val id: Int,
    val mean: FloatArray, // OKLab [L, a, b]
    val cov: FloatArray,  // 3x3 covariance matrix (length 9)
    val area: Float,
    val lRank: Float,
    val centroidXy: Pair<Float, Float>,
    val bbox: FloatArray, // [minX, minY, maxX, maxY] in %
    val hex: String
)

data class ZonePalette(
    val zones: List<Zone>,
    val labels: IntArray, // 1-indexed zone labels, length = width * height
    val width: Int,
    val height: Int
) {
    fun zone(zoneId: Int): Zone {
        val z = zones.find { it.id == zoneId }
        return z ?: throw NoSuchElementException("No zone $zoneId")
    }

    val ids: List<Int> get() = zones.map { it.id }
}

data class ColourStats(
    val concentration: Float,
    val colourful: Float,
    val hue: Float
) {
    val isMonochrome: Boolean get() = concentration >= 0.85f
}
