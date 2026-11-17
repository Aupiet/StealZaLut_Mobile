package com.stealzalut.engine.raw

/**
 * Parsed DNG metadata required for proper demosaicing and color reconstruction.
 */
data class DngMetadata(
    var width: Int = 0,
    var height: Int = 0,
    var bitsPerSample: Int = 16,
    var compression: Int = 1, // 1 = uncompressed, 8 = deflate
    var photometric: Int = 32803, // 32803 = CFA
    var orientation: Int = 1,
    var cfaRepeatDim: Pair<Int, Int> = Pair(2, 2),
    var cfaPattern: IntArray = intArrayOf(0, 1, 1, 2), // Default RGGB (0=R, 1=G, 2=B)
    var blackLevel: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f),
    var whiteLevel: Float = 65535.0f,
    var asShotNeutral: FloatArray? = null,
    var colorMatrix1: Array<FloatArray>? = null,
    var colorMatrix2: Array<FloatArray>? = null,
    var cropOrigin: Pair<Int, Int>? = null,
    var cropSize: Pair<Int, Int>? = null,
    var stripOffsets: IntArray = intArrayOf(),
    var stripByteCounts: IntArray = intArrayOf(),
    var tileOffsets: IntArray = intArrayOf(),
    var tileByteCounts: IntArray = intArrayOf(),
    var tileWidth: Int = 0,
    var tileLength: Int = 0,
    var isLinearRaw: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DngMetadata
        return width == other.width &&
                height == other.height &&
                bitsPerSample == other.bitsPerSample &&
                compression == other.compression &&
                photometric == other.photometric &&
                orientation == other.orientation &&
                cfaRepeatDim == other.cfaRepeatDim &&
                cfaPattern.contentEquals(other.cfaPattern) &&
                blackLevel.contentEquals(other.blackLevel) &&
                whiteLevel == other.whiteLevel &&
                (asShotNeutral?.contentEquals(other.asShotNeutral) ?: (other.asShotNeutral == null)) &&
                cropOrigin == other.cropOrigin &&
                cropSize == other.cropSize &&
                stripOffsets.contentEquals(other.stripOffsets) &&
                stripByteCounts.contentEquals(other.stripByteCounts) &&
                tileOffsets.contentEquals(other.tileOffsets) &&
                tileByteCounts.contentEquals(other.tileByteCounts) &&
                tileWidth == other.tileWidth &&
                tileLength == other.tileLength &&
                isLinearRaw == other.isLinearRaw
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bitsPerSample
        result = 31 * result + compression
        result = 31 * result + photometric
        result = 31 * result + orientation
        result = 31 * result + cfaRepeatDim.hashCode()
        result = 31 * result + cfaPattern.contentHashCode()
        result = 31 * result + blackLevel.contentHashCode()
        result = 31 * result + whiteLevel.hashCode()
        result = 31 * result + (asShotNeutral?.contentHashCode() ?: 0)
        result = 31 * result + (cropOrigin?.hashCode() ?: 0)
        result = 31 * result + (cropSize?.hashCode() ?: 0)
        result = 31 * result + stripOffsets.contentHashCode()
        result = 31 * result + stripByteCounts.contentHashCode()
        result = 31 * result + tileOffsets.contentHashCode()
        result = 31 * result + tileByteCounts.contentHashCode()
        result = 31 * result + tileWidth
        result = 31 * result + tileLength
        result = 31 * result + isLinearRaw.hashCode()
        return result
    }
}
