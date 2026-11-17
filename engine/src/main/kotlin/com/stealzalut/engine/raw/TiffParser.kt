package com.stealzalut.engine.raw

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

object TiffParser {

    const val TAG_IMAGE_WIDTH = 0x0100
    const val TAG_IMAGE_LENGTH = 0x0101
    const val TAG_BITS_PER_SAMPLE = 0x0102
    const val TAG_COMPRESSION = 0x0103
    const val TAG_PHOTOMETRIC_INTERPRETATION = 0x0106
    const val TAG_STRIP_OFFSETS = 0x0111
    const val TAG_ORIENTATION = 0x0112
    const val TAG_SAMPLES_PER_PIXEL = 0x0115
    const val TAG_ROWS_PER_STRIP = 0x0116
    const val TAG_STRIP_BYTE_COUNTS = 0x0117
    const val TAG_TILE_WIDTH = 0x0142
    const val TAG_TILE_LENGTH = 0x0143
    const val TAG_TILE_OFFSETS = 0x0144
    const val TAG_TILE_BYTE_COUNTS = 0x0145
    const val TAG_SUB_IFDS = 0x014A
    const val TAG_CFA_REPEAT_PATTERN_DIM = 0x828D
    const val TAG_CFA_PATTERN = 0x828E
    const val TAG_DNG_VERSION = 0xC612
    const val TAG_CFA_PLANE_COLOR = 0xC614
    const val TAG_CFA_LAYOUT = 0xC615
    const val TAG_BLACK_LEVEL_REPEAT_DIM = 0xC619
    const val TAG_BLACK_LEVEL = 0xC61A
    const val TAG_WHITE_LEVEL = 0xC61D
    const val TAG_DEFAULT_SCALE = 0xC61E
    const val TAG_DEFAULT_CROP_ORIGIN = 0xC61F
    const val TAG_DEFAULT_CROP_SIZE = 0xC620
    const val TAG_COLOR_MATRIX_1 = 0xC621
    const val TAG_COLOR_MATRIX_2 = 0xC622
    const val TAG_CAMERA_CALIBRATION_1 = 0xC623
    const val TAG_CAMERA_CALIBRATION_2 = 0xC624
    const val TAG_ANALOG_BALANCE = 0xC627
    const val TAG_AS_SHOT_NEUTRAL = 0xC628
    const val TAG_AS_SHOT_WHITE_XY = 0xC629
    const val TAG_BASELINE_EXPOSURE = 0xC62A
    const val TAG_ACTIVE_AREA = 0xC68D

    private val TYPE_SIZES = intArrayOf(
        0, // 0 unused
        1, // 1: BYTE
        1, // 2: ASCII
        2, // 3: SHORT
        4, // 4: LONG
        8, // 5: RATIONAL
        1, // 6: SBYTE
        1, // 7: UNDEFINED
        2, // 8: SSHORT
        4, // 9: SLONG
        8, // 10: SRATIONAL
        4, // 11: FLOAT
        8  // 12: DOUBLE
    )

    private fun typeSize(type: Int): Int {
        return if (type in 1..12) TYPE_SIZES[type] else 1
    }

    class TagValue(val type: Int, val count: Int, val raw: Any) {
        fun asIntList(): IntArray {
            return when (raw) {
                is IntArray -> raw
                is LongArray -> raw.map { it.toInt() }.toIntArray()
                is ShortArray -> raw.map { it.toInt() and 0xFFFF }.toIntArray()
                is ByteArray -> raw.map { it.toInt() and 0xFF }.toIntArray()
                is FloatArray -> raw.map { it.toInt() }.toIntArray()
                is DoubleArray -> raw.map { it.toInt() }.toIntArray()
                is Number -> intArrayOf(raw.toInt())
                is List<*> -> raw.map { (it as Number).toInt() }.toIntArray()
                else -> intArrayOf()
            }
        }

        fun asFloatList(): FloatArray {
            return when (raw) {
                is FloatArray -> raw
                is DoubleArray -> raw.map { it.toFloat() }.toFloatArray()
                is IntArray -> raw.map { it.toFloat() }.toFloatArray()
                is LongArray -> raw.map { it.toFloat() }.toFloatArray()
                is ShortArray -> raw.map { it.toFloat() }.toFloatArray()
                is ByteArray -> raw.map { (it.toInt() and 0xFF).toFloat() }.toFloatArray()
                is Number -> floatArrayOf(raw.toFloat())
                is List<*> -> raw.map { (it as Number).toFloat() }.toFloatArray()
                else -> floatArrayOf()
            }
        }

        fun asInt(defaultVal: Int = 0): Int {
            val list = asIntList()
            return if (list.isNotEmpty()) list[0] else defaultVal
        }

        fun asFloat(defaultVal: Float = 0f): Float {
            val list = asFloatList()
            return if (list.isNotEmpty()) list[0] else defaultVal
        }
    }

    private fun readTiffValue(
        raf: RandomAccessFile,
        order: ByteOrder,
        tiffType: Int,
        count: Int,
        valOrOffsetBytes: ByteArray,
        valOrOffsetInt: Long
    ): TagValue {
        val elemSize = typeSize(tiffType)
        val totalBytes = elemSize * count

        val dataBytes: ByteArray
        if (totalBytes <= 4) {
            dataBytes = valOrOffsetBytes.copyOf(totalBytes)
        } else {
            val savedPos = raf.filePointer
            raf.seek(valOrOffsetInt)
            dataBytes = ByteArray(totalBytes)
            raf.readFully(dataBytes)
            raf.seek(savedPos)
        }

        val buf = ByteBuffer.wrap(dataBytes).order(order)
        val raw: Any = when (tiffType) {
            1, 7 -> { // BYTE, UNDEFINED
                val arr = ByteArray(count)
                buf.get(arr)
                arr
            }
            2 -> { // ASCII
                val arr = ByteArray(count)
                buf.get(arr)
                String(arr).trimEnd('\u0000')
            }
            3 -> { // SHORT
                val arr = IntArray(count)
                for (i in 0 until count) {
                    arr[i] = buf.short.toInt() and 0xFFFF
                }
                arr
            }
            4 -> { // LONG
                val arr = LongArray(count)
                for (i in 0 until count) {
                    arr[i] = buf.int.toLong() and 0xFFFFFFFFL
                }
                arr
            }
            5, 10 -> { // RATIONAL, SRATIONAL
                val arr = FloatArray(count)
                for (i in 0 until count) {
                    val num = if (tiffType == 5) buf.int.toLong() and 0xFFFFFFFFL else buf.int.toLong()
                    val den = if (tiffType == 5) buf.int.toLong() and 0xFFFFFFFFL else buf.int.toLong()
                    arr[i] = num.toFloat() / den.toFloat().coerceAtLeast(1.0f)
                }
                arr
            }
            8 -> { // SSHORT
                val arr = ShortArray(count)
                for (i in 0 until count) arr[i] = buf.short
                arr
            }
            9 -> { // SLONG
                val arr = IntArray(count)
                for (i in 0 until count) arr[i] = buf.int
                arr
            }
            11 -> { // FLOAT
                val arr = FloatArray(count)
                for (i in 0 until count) arr[i] = buf.float
                arr
            }
            12 -> { // DOUBLE
                val arr = DoubleArray(count)
                for (i in 0 until count) arr[i] = buf.double
                arr
            }
            else -> dataBytes
        }

        return TagValue(tiffType, count, raw)
    }

    fun parseDngStructure(file: File): Pair<DngMetadata, RandomAccessFile> {
        val raf = RandomAccessFile(file, "r")
        val header = ByteArray(4)
        raf.readFully(header)
        val order = if (header[0] == 'I'.code.toByte() && header[1] == 'I'.code.toByte()) {
            ByteOrder.LITTLE_ENDIAN
        } else if (header[0] == 'M'.code.toByte() && header[1] == 'M'.code.toByte()) {
            ByteOrder.BIG_ENDIAN
        } else {
            raf.close()
            throw IllegalArgumentException("Invalid TIFF/DNG file header: ${String(header.copyOf(2))}")
        }

        val hBuf = ByteBuffer.wrap(header, 2, 2).order(order)
        val version = hBuf.short.toInt() and 0xFFFF
        if (version != 42 && version != 0x55) {
            raf.close()
            throw IllegalArgumentException("Unsupported TIFF version: $version")
        }

        val ifdOffsetBytes = ByteArray(4)
        raf.readFully(ifdOffsetBytes)
        val firstIfdOffset = ByteBuffer.wrap(ifdOffsetBytes).order(order).int.toLong() and 0xFFFFFFFFL

        fun readIfd(offset: Long): MutableMap<Int, TagValue> {
            val tags = mutableMapOf<Int, TagValue>()
            if (offset == 0L || offset >= raf.length()) return tags
            raf.seek(offset)
            val countBytes = ByteArray(2)
            raf.readFully(countBytes)
            val numEntries = ByteBuffer.wrap(countBytes).order(order).short.toInt() and 0xFFFF
            val entriesData = ByteArray(numEntries * 12)
            raf.readFully(entriesData)

            val entryBuf = ByteBuffer.wrap(entriesData).order(order)
            for (i in 0 until numEntries) {
                val tagId = entryBuf.short.toInt() and 0xFFFF
                val tiffType = entryBuf.short.toInt() and 0xFFFF
                val count = entryBuf.int.toLong() and 0xFFFFFFFFL
                val valOffsetBytes = ByteArray(4)
                entryBuf.get(valOffsetBytes)
                val valOffsetInt = ByteBuffer.wrap(valOffsetBytes).order(order).int.toLong() and 0xFFFFFFFFL

                val value = readTiffValue(raf, order, tiffType, count.toInt(), valOffsetBytes, valOffsetInt)
                tags[tagId] = value
            }
            return tags
        }

        val ifd0 = readIfd(firstIfdOffset)
        val allIfds = mutableListOf(ifd0)

        // SubIFDs
        val subIfdTag = ifd0[TAG_SUB_IFDS]
        if (subIfdTag != null) {
            val subOffsets = subIfdTag.asIntList()
            for (off in subOffsets) {
                val subIfd = readIfd(off.toLong() and 0xFFFFFFFFL)
                if (subIfd.isNotEmpty()) allIfds.add(subIfd)
            }
        }

        // Selected IFD
        var selectedIfd = ifd0
        var bestPixelCount = 0

        for (ifd in allIfds) {
            val w = ifd[TAG_IMAGE_WIDTH]?.asInt(0) ?: 0
            val h = ifd[TAG_IMAGE_LENGTH]?.asInt(0) ?: 0
            val pixels = w * h
            val photo = ifd[TAG_PHOTOMETRIC_INTERPRETATION]?.asInt(0) ?: 0

            if (photo == 32803 && pixels >= bestPixelCount) {
                bestPixelCount = pixels
                selectedIfd = ifd
            } else if (pixels > bestPixelCount && (photo == 32803 || (selectedIfd[TAG_PHOTOMETRIC_INTERPRETATION]?.asInt(0) ?: 0) != 32803)) {
                bestPixelCount = pixels
                selectedIfd = ifd
            }
        }

        val meta = DngMetadata()
        meta.width = selectedIfd[TAG_IMAGE_WIDTH]?.asInt() ?: ifd0[TAG_IMAGE_WIDTH]?.asInt() ?: 0
        meta.height = selectedIfd[TAG_IMAGE_LENGTH]?.asInt() ?: ifd0[TAG_IMAGE_LENGTH]?.asInt() ?: 0
        meta.bitsPerSample = selectedIfd[TAG_BITS_PER_SAMPLE]?.asInt() ?: ifd0[TAG_BITS_PER_SAMPLE]?.asInt() ?: 16
        meta.compression = selectedIfd[TAG_COMPRESSION]?.asInt() ?: ifd0[TAG_COMPRESSION]?.asInt() ?: 1
        meta.photometric = selectedIfd[TAG_PHOTOMETRIC_INTERPRETATION]?.asInt() ?: ifd0[TAG_PHOTOMETRIC_INTERPRETATION]?.asInt() ?: 32803
        meta.isLinearRaw = meta.photometric in listOf(1, 2, 34892)
        meta.orientation = ifd0[TAG_ORIENTATION]?.asInt() ?: 1

        val cfaRep = selectedIfd[TAG_CFA_REPEAT_PATTERN_DIM]?.asIntList() ?: ifd0[TAG_CFA_REPEAT_PATTERN_DIM]?.asIntList()
        if (cfaRep != null && cfaRep.size >= 2) {
            meta.cfaRepeatDim = Pair(cfaRep[0], cfaRep[1])
        }

        val cfaPat = selectedIfd[TAG_CFA_PATTERN]?.asIntList() ?: ifd0[TAG_CFA_PATTERN]?.asIntList()
        if (cfaPat != null && cfaPat.isNotEmpty()) {
            meta.cfaPattern = cfaPat
        }

        val bl = selectedIfd[TAG_BLACK_LEVEL]?.asFloatList() ?: ifd0[TAG_BLACK_LEVEL]?.asFloatList()
        if (bl != null && bl.isNotEmpty()) {
            meta.blackLevel = bl
        }

        val wl = selectedIfd[TAG_WHITE_LEVEL]?.asFloat() ?: ifd0[TAG_WHITE_LEVEL]?.asFloat() ?: ((1 shl meta.bitsPerSample) - 1).toFloat()
        meta.whiteLevel = wl

        val asn = ifd0[TAG_AS_SHOT_NEUTRAL]?.asFloatList() ?: selectedIfd[TAG_AS_SHOT_NEUTRAL]?.asFloatList()
        if (asn != null && asn.size >= 3) {
            meta.asShotNeutral = asn.copyOf(3)
        }

        fun to3x3(arr: FloatArray?): Array<FloatArray>? {
            if (arr == null || arr.size < 9) return null
            return Array(3) { r ->
                FloatArray(3) { c -> arr[r * 3 + c] }
            }
        }

        meta.colorMatrix1 = to3x3(ifd0[TAG_COLOR_MATRIX_1]?.asFloatList() ?: selectedIfd[TAG_COLOR_MATRIX_1]?.asFloatList())
        meta.colorMatrix2 = to3x3(ifd0[TAG_COLOR_MATRIX_2]?.asFloatList() ?: selectedIfd[TAG_COLOR_MATRIX_2]?.asFloatList())

        val cropOrig = selectedIfd[TAG_DEFAULT_CROP_ORIGIN]?.asIntList() ?: ifd0[TAG_DEFAULT_CROP_ORIGIN]?.asIntList()
        if (cropOrig != null && cropOrig.size >= 2) {
            meta.cropOrigin = Pair(cropOrig[0], cropOrig[1])
        }

        val cropSz = selectedIfd[TAG_DEFAULT_CROP_SIZE]?.asIntList() ?: ifd0[TAG_DEFAULT_CROP_SIZE]?.asIntList()
        if (cropSz != null && cropSz.size >= 2) {
            meta.cropSize = Pair(cropSz[0], cropSz[1])
        }

        meta.stripOffsets = selectedIfd[TAG_STRIP_OFFSETS]?.asIntList() ?: intArrayOf()
        meta.stripByteCounts = selectedIfd[TAG_STRIP_BYTE_COUNTS]?.asIntList() ?: intArrayOf()
        meta.tileOffsets = selectedIfd[TAG_TILE_OFFSETS]?.asIntList() ?: intArrayOf()
        meta.tileByteCounts = selectedIfd[TAG_TILE_BYTE_COUNTS]?.asIntList() ?: intArrayOf()
        meta.tileWidth = selectedIfd[TAG_TILE_WIDTH]?.asInt() ?: 0
        meta.tileLength = selectedIfd[TAG_TILE_LENGTH]?.asInt() ?: 0

        return Pair(meta, raf)
    }

    fun readDngRawBuffer(meta: DngMetadata, raf: RandomAccessFile): FloatArray {
        val w = meta.width
        val h = meta.height
        val totalPixels = w * h
        val bps = meta.bitsPerSample

        val bos = ByteArrayOutputStream()

        if (meta.tileOffsets.isNotEmpty()) {
            for (i in meta.tileOffsets.indices) {
                val off = meta.tileOffsets[i].toLong() and 0xFFFFFFFFL
                val cnt = meta.tileByteCounts[i]
                raf.seek(off)
                val chunk = ByteArray(cnt)
                raf.readFully(chunk)
                if (meta.compression == 8) {
                    bos.write(decompressDeflate(chunk))
                } else {
                    bos.write(chunk)
                }
            }
        } else if (meta.stripOffsets.isNotEmpty()) {
            for (i in meta.stripOffsets.indices) {
                val off = meta.stripOffsets[i].toLong() and 0xFFFFFFFFL
                val cnt = meta.stripByteCounts[i]
                raf.seek(off)
                val chunk = ByteArray(cnt)
                raf.readFully(chunk)
                if (meta.compression == 8) {
                    bos.write(decompressDeflate(chunk))
                } else {
                    bos.write(chunk)
                }
            }
        } else {
            throw IllegalArgumentException("DNG file has neither strip offsets nor tile offsets")
        }

        val data = bos.toByteArray()
        val bayer = FloatArray(totalPixels)

        when (bps) {
            16 -> {
                val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                val available = data.size / 2
                val limit = minOf(totalPixels, available)
                for (i in 0 until limit) {
                    bayer[i] = (buf.short.toInt() and 0xFFFF).toFloat()
                }
                // Edge pad if needed
                val lastVal = if (limit > 0) bayer[limit - 1] else 0f
                for (i in limit until totalPixels) {
                    bayer[i] = lastVal
                }
            }
            8 -> {
                val limit = minOf(totalPixels, data.size)
                for (i in 0 until limit) {
                    bayer[i] = (data[i].toInt() and 0xFF).toFloat() * 256.0f
                }
                val lastVal = if (limit > 0) bayer[limit - 1] else 0f
                for (i in limit until totalPixels) {
                    bayer[i] = lastVal
                }
            }
            12 -> {
                if (data.size >= (totalPixels * 3) / 2) {
                    var inIdx = 0
                    var outIdx = 0
                    while (outIdx < totalPixels && inIdx + 2 < data.size) {
                        val b0 = data[inIdx].toInt() and 0xFF
                        val b1 = data[inIdx + 1].toInt() and 0xFF
                        val b2 = data[inIdx + 2].toInt() and 0xFF
                        val p1 = (b0 shl 4) or (b1 ushr 4)
                        val p2 = ((b1 and 0x0F) shl 8) or b2
                        bayer[outIdx++] = p1.toFloat()
                        if (outIdx < totalPixels) {
                            bayer[outIdx++] = p2.toFloat()
                        }
                        inIdx += 3
                    }
                } else {
                    val limit = minOf(totalPixels, data.size)
                    for (i in 0 until limit) {
                        bayer[i] = (data[i].toInt() and 0xFF).toFloat() * 16.0f
                    }
                }
            }
            else -> {
                val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                val available = data.size / 2
                val limit = minOf(totalPixels, available)
                for (i in 0 until limit) {
                    bayer[i] = (buf.short.toInt() and 0xFFFF).toFloat()
                }
            }
        }

        return bayer
    }

    private fun decompressDeflate(compressed: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(compressed)
        val bos = ByteArrayOutputStream(compressed.size * 2)
        val buf = ByteArray(8192)
        try {
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buf)
                if (count == 0) break
                bos.write(buf, 0, count)
            }
        } finally {
            inflater.end()
        }
        return bos.toByteArray()
    }
}
