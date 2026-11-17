package com.stealzalut.engine.raw

import java.io.File
import kotlin.math.roundToInt
import kotlin.math.tanh

object RawDecoder {

    /**
     * Fully decode a DNG file into a calibrated RGB float32 array in [0.0, 1.0].
     *
     * @param file File pointing to .dng
     * @param maxDim Optional maximum dimension for preview downscaling.
     * @return Triple(rgb: FloatArray, width: Int, height: Int)
     */
    fun decodeDngImage(
        file: File,
        maxDim: Int? = null
    ): DecodedImage {
        require(file.exists()) { "RAW/DNG file not found: ${file.absolutePath}" }

        val (meta, raf) = TiffParser.parseDngStructure(file)

        try {
            if (meta.isLinearRaw) {
                val w = meta.width
                val h = meta.height
                val bps = meta.bitsPerSample
                val byteCount = w * h * 3 * (bps / 8)
                val off = if (meta.stripOffsets.isNotEmpty()) meta.stripOffsets[0].toLong() and 0xFFFFFFFFL else 0L
                raf.seek(off)
                val rawBytes = ByteArray(byteCount)
                raf.readFully(rawBytes)

                val rgb = FloatArray(w * h * 3)
                val maxVal = ((1 shl bps) - 1).toFloat()
                if (bps > 8) {
                    val buf = java.nio.ByteBuffer.wrap(rawBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until w * h * 3) {
                        rgb[i] = ((buf.short.toInt() and 0xFFFF) / maxVal).coerceIn(0.0f, 1.0f)
                    }
                } else {
                    for (i in 0 until w * h * 3) {
                        rgb[i] = ((rawBytes[i].toInt() and 0xFF) / maxVal).coerceIn(0.0f, 1.0f)
                    }
                }
                val oriented = applyExifOrientation(rgb, w, h, meta.orientation)
                return oriented
            } else {
                var w = meta.width
                var h = meta.height
                var bayer = TiffParser.readDngRawBuffer(meta, raf)

                // Sub-area active area cropping if specified
                if (meta.cropOrigin != null && meta.cropSize != null) {
                    val ox = meta.cropOrigin!!.first
                    val oy = meta.cropOrigin!!.second
                    val cw = meta.cropSize!!.first
                    val ch = meta.cropSize!!.second
                    if (oy + ch <= h && ox + cw <= w) {
                        val cropped = FloatArray(cw * ch)
                        for (r in 0 until ch) {
                            System.arraycopy(bayer, (oy + r) * w + ox, cropped, r * cw, cw)
                        }
                        bayer = cropped
                        w = cw
                        h = ch
                    }
                }

                // Downsample for preview if requested
                if (maxDim != null && maxDim > 0) {
                    val down = downsampleBayerForPreview(bayer, w, h, maxDim)
                    bayer = down.bayer
                    w = down.width
                    h = down.height
                }

                // Normalize black and white levels
                val bl = if (meta.blackLevel.isNotEmpty()) meta.blackLevel[0] else 0.0f
                val wl = if (meta.whiteLevel > bl) meta.whiteLevel else 65535.0f
                val range = maxOf(1.0f, wl - bl)
                for (i in bayer.indices) {
                    bayer[i] = ((bayer[i] - bl) / range).coerceIn(0.0f, 1.0f)
                }

                // High-fidelity demosaicing
                val rgb = FloatArray(w * h * 3)
                Demosaic.demosaicBayerHighQuality(bayer, w, h, meta.cfaPattern, rgb)

                // Camera white balance and color matrix
                val matrix = meta.colorMatrix1 ?: meta.colorMatrix2
                applyColorMatrixAndWhiteBalance(rgb, w * h, meta.asShotNeutral, matrix)

                // Apply EXIF upright orientation
                var result = applyExifOrientation(rgb, w, h, meta.orientation)

                // Downsample if still larger than maxDim
                if (maxDim != null && maxDim > 0) {
                    val curMax = maxOf(result.width, result.height)
                    if (curMax > maxDim) {
                        val scale = maxDim.toFloat() / curMax.toFloat()
                        val newW = maxOf(1, (result.width * scale).roundToInt())
                        val newH = maxOf(1, (result.height * scale).roundToInt())
                        result = bilinearZoom(result.rgb, result.width, result.height, newW, newH)
                    }
                }

                return result
            }
        } finally {
            raf.close()
        }
    }

    fun isRawOrDngFile(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val dot = path.lastIndexOf('.')
        if (dot < 0) return false
        val ext = path.substring(dot).lowercase()
        return ext in listOf(".dng", ".raw", ".cr2", ".nef", ".arw", ".orf", ".rw2")
    }

    data class DecodedImage(val rgb: FloatArray, val width: Int, val height: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DecodedImage
            return width == other.width &&
                    height == other.height &&
                    rgb.contentEquals(other.rgb)
        }

        override fun hashCode(): Int {
            var result = rgb.contentHashCode()
            result = 31 * result + width
            result = 31 * result + height
            return result
        }
    }

    private data class DownsampledMosaic(val bayer: FloatArray, val width: Int, val height: Int)

    private fun downsampleBayerForPreview(
        bayer: FloatArray,
        w: Int,
        h: Int,
        maxDim: Int
    ): DownsampledMosaic {
        val scale = maxOf(1, (maxOf(h, w) + maxDim - 1) / maxDim)
        if (scale == 1) return DownsampledMosaic(bayer, w, h)

        val blockSize = scale * 2
        val usableH = (h / blockSize) * blockSize
        val usableW = (w / blockSize) * blockSize
        if (usableH < blockSize || usableW < blockSize) return DownsampledMosaic(bayer, w, h)

        val outputH = usableH / blockSize
        val outputW = usableW / blockSize
        val outH2 = outputH * 2
        val outW2 = outputW * 2
        val outBayer = FloatArray(outH2 * outW2)
        val scaleSq = (scale * scale).toFloat()

        for (oh in 0 until outputH) {
            for (ow in 0 until outputW) {
                for (cr in 0..1) {
                    for (cc in 0..1) {
                        var sum = 0.0f
                        for (sr in 0 until scale) {
                            val r = (oh * scale + sr) * 2 + cr
                            val rowOff = r * w
                            for (sc in 0 until scale) {
                                val c = (ow * scale + sc) * 2 + cc
                                sum += bayer[rowOff + c]
                            }
                        }
                        val flatIdx = ((oh * outputW + ow) * 2 + cr) * 2 + cc
                        outBayer[flatIdx] = sum / scaleSq
                    }
                }
            }
        }

        return DownsampledMosaic(outBayer, outW2, outH2)
    }

    private fun applyColorMatrixAndWhiteBalance(
        rgb: FloatArray,
        pixelCount: Int,
        neutral: FloatArray?,
        colorMatrix: Array<FloatArray>?
    ) {
        var rGain = 1.0f
        var bGain = 1.0f
        if (neutral != null && neutral.size >= 3 && neutral[0] > 1e-4f && neutral[1] > 1e-4f && neutral[2] > 1e-4f) {
            rGain = (1.0f / neutral[0]) / (1.0f / neutral[1])
            bGain = (1.0f / neutral[2]) / (1.0f / neutral[1])
        }

        val normMatrix: Array<FloatArray>?
        if (colorMatrix != null && colorMatrix.size == 3 && colorMatrix[0].size == 3) {
            normMatrix = Array(3) { r ->
                val rowSum = colorMatrix[r][0] + colorMatrix[r][1] + colorMatrix[r][2]
                val s = if (Math.abs(rowSum) > 1e-4f) rowSum else 1.0f
                FloatArray(3) { c -> colorMatrix[r][c] / s }
            }
        } else {
            normMatrix = null
        }

        val knee = 0.85f
        val oneMinusKnee = 1.0f - knee

        for (i in 0 until pixelCount) {
            val idx = i * 3
            var r = rgb[idx] * rGain
            var g = rgb[idx + 1]
            var b = rgb[idx + 2] * bGain

            if (normMatrix != null) {
                val rMat = r * normMatrix[0][0] + g * normMatrix[0][1] + b * normMatrix[0][2]
                val gMat = r * normMatrix[1][0] + g * normMatrix[1][1] + b * normMatrix[1][2]
                val bMat = r * normMatrix[2][0] + g * normMatrix[2][1] + b * normMatrix[2][2]
                r = rMat
                g = gMat
                b = bMat
            }

            if (r > knee) r = knee + oneMinusKnee * tanh(((r - knee) / oneMinusKnee).toDouble()).toFloat()
            if (g > knee) g = knee + oneMinusKnee * tanh(((g - knee) / oneMinusKnee).toDouble()).toFloat()
            if (b > knee) b = knee + oneMinusKnee * tanh(((b - knee) / oneMinusKnee).toDouble()).toFloat()

            rgb[idx] = r.coerceIn(0.0f, 1.0f)
            rgb[idx + 1] = g.coerceIn(0.0f, 1.0f)
            rgb[idx + 2] = b.coerceIn(0.0f, 1.0f)
        }
    }

    private fun applyExifOrientation(
        rgb: FloatArray,
        w: Int,
        h: Int,
        orientation: Int
    ): DecodedImage {
        when (orientation) {
            1 -> return DecodedImage(rgb, w, h)
            2 -> { // fliplr
                val out = FloatArray(rgb.size)
                for (y in 0 until h) {
                    val row = y * w
                    for (x in 0 until w) {
                        val srcIdx = (row + x) * 3
                        val dstIdx = (row + (w - 1 - x)) * 3
                        out[dstIdx] = rgb[srcIdx]
                        out[dstIdx + 1] = rgb[srcIdx + 1]
                        out[dstIdx + 2] = rgb[srcIdx + 2]
                    }
                }
                return DecodedImage(out, w, h)
            }
            3 -> { // rot180
                val out = FloatArray(rgb.size)
                for (y in 0 until h) {
                    val dstRow = (h - 1 - y) * w
                    val srcRow = y * w
                    for (x in 0 until w) {
                        val srcIdx = (srcRow + x) * 3
                        val dstIdx = (dstRow + (w - 1 - x)) * 3
                        out[dstIdx] = rgb[srcIdx]
                        out[dstIdx + 1] = rgb[srcIdx + 1]
                        out[dstIdx + 2] = rgb[srcIdx + 2]
                    }
                }
                return DecodedImage(out, w, h)
            }
            4 -> { // flipud
                val out = FloatArray(rgb.size)
                for (y in 0 until h) {
                    val dstRow = (h - 1 - y) * w
                    val srcRow = y * w
                    System.arraycopy(rgb, srcRow * 3, out, dstRow * 3, w * 3)
                }
                return DecodedImage(out, w, h)
            }
            6 -> { // 90 CW (h, w -> w, h)
                val out = FloatArray(rgb.size)
                for (y in 0 until h) {
                    val srcRow = y * w
                    for (x in 0 until w) {
                        val srcIdx = (srcRow + x) * 3
                        // new y is x, new x is h - 1 - y
                        val dstIdx = (x * h + (h - 1 - y)) * 3
                        out[dstIdx] = rgb[srcIdx]
                        out[dstIdx + 1] = rgb[srcIdx + 1]
                        out[dstIdx + 2] = rgb[srcIdx + 2]
                    }
                }
                return DecodedImage(out, h, w)
            }
            8 -> { // 90 CCW (h, w -> w, h)
                val out = FloatArray(rgb.size)
                for (y in 0 until h) {
                    val srcRow = y * w
                    for (x in 0 until w) {
                        val srcIdx = (srcRow + x) * 3
                        // new y is w - 1 - x, new x is y
                        val dstIdx = ((w - 1 - x) * h + y) * 3
                        out[dstIdx] = rgb[srcIdx]
                        out[dstIdx + 1] = rgb[srcIdx + 1]
                        out[dstIdx + 2] = rgb[srcIdx + 2]
                    }
                }
                return DecodedImage(out, h, w)
            }
            else -> return DecodedImage(rgb, w, h)
        }
    }

    private fun bilinearZoom(
        srcRgb: FloatArray,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): DecodedImage {
        val out = FloatArray(dstW * dstH * 3)
        val scaleX = srcW.toFloat() / dstW.toFloat()
        val scaleY = srcH.toFloat() / dstH.toFloat()

        for (dy in 0 until dstH) {
            val sy = dy * scaleY
            val y0 = sy.toInt().coerceIn(0, srcH - 1)
            val y1 = minOf(y0 + 1, srcH - 1)
            val yFrac = sy - y0

            val dstRow = dy * dstW
            val srcRow0 = y0 * srcW
            val srcRow1 = y1 * srcW

            for (dx in 0 until dstW) {
                val sx = dx * scaleX
                val x0 = sx.toInt().coerceIn(0, srcW - 1)
                val x1 = minOf(x0 + 1, srcW - 1)
                val xFrac = sx - x0

                val idx00 = (srcRow0 + x0) * 3
                val idx01 = (srcRow0 + x1) * 3
                val idx10 = (srcRow1 + x0) * 3
                val idx11 = (srcRow1 + x1) * 3

                val dstIdx = (dstRow + dx) * 3

                for (c in 0..2) {
                    val top = srcRgb[idx00 + c] * (1.0f - xFrac) + srcRgb[idx01 + c] * xFrac
                    val bot = srcRgb[idx10 + c] * (1.0f - xFrac) + srcRgb[idx11 + c] * xFrac
                    out[dstIdx + c] = (top * (1.0f - yFrac) + bot * yFrac).coerceIn(0.0f, 1.0f)
                }
            }
        }

        return DecodedImage(out, dstW, dstH)
    }
}
