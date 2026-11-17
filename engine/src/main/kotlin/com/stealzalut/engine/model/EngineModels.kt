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
