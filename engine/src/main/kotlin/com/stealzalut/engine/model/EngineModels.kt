package com.stealzalut.engine.model

enum class EngineType {
    PYTHON,
    KOTLIN;

    companion object {
        fun fromString(value: String?): EngineType {
            return when (value?.lowercase()) {
                "kotlin", "kt" -> KOTLIN
                else -> PYTHON
            }
        }
    }
}

data class EngineConfig(
    val type: EngineType = EngineType.PYTHON,
    val parallelism: Int = maxOf(1, Runtime.getRuntime().availableProcessors() - 1),
    val bandHeight: Int = 256
)

data class Step2Params(
    val globalIntensity: Float = 1.0f,
    val lumaLock: Float = 0.0f,
    val neutralAxisLock: Float = 0.0f,
    val skinToneLock: Float = 0.0f,
    val extremesRolloff: Float = 0.0f,
    val spatial3d: Float = 0.0f
)

data class Step3Params(
    val exposure: Float = 0.0f,
    val contrast: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val whites: Float = 0.0f,
    val blacks: Float = 0.0f,
    val temperature: Float = 0.0f,
    val tint: Float = 0.0f,
    val vibrance: Float = 0.0f,
    val saturation: Float = 0.0f,
    val clarity: Float = 0.0f
)

data class ZoneData(
    val id: Int,
    val hex: String,
    val area: Float,
    val meanLab: FloatArray,
    val lRank: Float = 0.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZoneData) return false
        return id == other.id && hex == other.hex && area == other.area && meanLab.contentEquals(other.meanLab)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + hex.hashCode()
        result = 31 * result + area.hashCode()
        result = 31 * result + meanLab.contentHashCode()
        return result
    }
}

data class ZonePaletteData(
    val zones: List<ZoneData>,
    val width: Int,
    val height: Int
)

data class EngineResult(
    val success: Boolean,
    val outputPath: String? = null,
    val executionTimeMs: Long = 0,
    val errorMessage: String? = null
)

data class BasicSettings(
    val exposure: Float = 0.0f,
    val contrast: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val whites: Float = 0.0f,
    val blacks: Float = 0.0f,
    val temp: Float = 0.0f,
    val tint: Float = 0.0f,
    val vibrance: Float = 0.0f,
    val saturation: Float = 0.0f
)

data class Point2D(val x: Float, val y: Float)

data class ToneCurveSettings(
    val mode: String = "point", // "parametric" or "point"
    val shadowSplit: Float = 25.0f,
    val midtoneSplit: Float = 50.0f,
    val highlightSplit: Float = 75.0f,
    val shadows: Float = 0.0f,
    val darks: Float = 0.0f,
    val lights: Float = 0.0f,
    val highlights: Float = 0.0f,
    val rgbPoints: List<Point2D> = emptyList(),
    val redPoints: List<Point2D> = emptyList(),
    val greenPoints: List<Point2D> = emptyList(),
    val bluePoints: List<Point2D> = emptyList()
)

data class ColorMixerSettings(
    val mode: String = "hsl", // "hsl" or "point_color"
    val hueRed: Float = 0.0f,
    val satRed: Float = 0.0f,
    val lumRed: Float = 0.0f,
    val hueOrange: Float = 0.0f,
    val satOrange: Float = 0.0f,
    val lumOrange: Float = 0.0f,
    val hueYellow: Float = 0.0f,
    val satYellow: Float = 0.0f,
    val lumYellow: Float = 0.0f,
    val hueGreen: Float = 0.0f,
    val satGreen: Float = 0.0f,
    val lumGreen: Float = 0.0f,
    val hueAqua: Float = 0.0f,
    val satAqua: Float = 0.0f,
    val lumAqua: Float = 0.0f,
    val hueBlue: Float = 0.0f,
    val satBlue: Float = 0.0f,
    val lumBlue: Float = 0.0f,
    val huePurple: Float = 0.0f,
    val satPurple: Float = 0.0f,
    val lumPurple: Float = 0.0f,
    val hueMagenta: Float = 0.0f,
    val satMagenta: Float = 0.0f,
    val lumMagenta: Float = 0.0f,
    val pointActive: Boolean = false,
    val pointTargetR: Float = 0.5f,
    val pointTargetG: Float = 0.5f,
    val pointTargetB: Float = 0.5f,
    val pointRange: Float = 50.0f,
    val pointHueShift: Float = 0.0f,
    val pointSatShift: Float = 0.0f,
    val pointLumShift: Float = 0.0f
)

data class ColorGradingSettings(
    val shadowHue: Float = 0.0f,
    val shadowSat: Float = 0.0f,
    val shadowLum: Float = 0.0f,
    val midtoneHue: Float = 0.0f,
    val midtoneSat: Float = 0.0f,
    val midtoneLum: Float = 0.0f,
    val highlightHue: Float = 0.0f,
    val highlightSat: Float = 0.0f,
    val highlightLum: Float = 0.0f,
    val globalHue: Float = 0.0f,
    val globalSat: Float = 0.0f,
    val globalLum: Float = 0.0f,
    val balance: Float = 0.0f,
    val blending: Float = 50.0f
)

data class CalibrationSettings(
    val shadowTint: Float = 0.0f,
    val redHue: Float = 0.0f,
    val redSat: Float = 0.0f,
    val greenHue: Float = 0.0f,
    val greenSat: Float = 0.0f,
    val blueHue: Float = 0.0f,
    val blueSat: Float = 0.0f
)
