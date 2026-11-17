package com.stealzalut.engine.raw

/**
 * High-precision edge-adaptive demosaicing engine using Malvar-He-Cutler
 * chrominance difference interpolation.
 */
object Demosaic {

    /**
     * Demosaics a normalized Bayer 2D grid into an interleaved RGB float array.
     *
     * @param bayer Flat 2D array of size [height * width] normalized to [0.0f, 1.0f].
     * @param width Width of sensor image.
     * @param height Height of sensor image.
     * @param pattern 4-element Bayer pattern array [p00, p01, p10, p11] where 0=R, 1=G, 2=B.
     * @param outRgb Output float array of size [height * width * 3] with values in [0.0f, 1.0f].
     */
    fun demosaicBayerHighQuality(
        bayer: FloatArray,
        width: Int,
        height: Int,
        pattern: IntArray,
        outRgb: FloatArray
    ) {
        val p = if (pattern.size >= 4) pattern else intArrayOf(0, 1, 1, 2)
        var ry = 0
        var rx = 0
        var by = 0
        var bx = 0
        val gy = IntArray(2)
        val gx = IntArray(2)
        var gCount = 0

        for (r in 0..1) {
            for (c in 0..1) {
                when (p[r * 2 + c]) {
                    0 -> { ry = r; rx = c }
                    2 -> { by = r; bx = c }
                    1 -> {
                        if (gCount < 2) {
                            gy[gCount] = r
                            gx[gCount] = c
                            gCount++
                        }
                    }
                }
            }
        }

        fun isG(y: Int, x: Int): Boolean {
            val yr = y and 1
            val xr = x and 1
            return (yr == gy[0] && xr == gx[0]) || (yr == gy[1] && xr == gx[1])
        }

        fun isR(y: Int, x: Int): Boolean {
            return (y and 1 == ry) && (x and 1 == rx)
        }

        fun isB(y: Int, x: Int): Boolean {
            return (y and 1 == by) && (x and 1 == bx)
        }

        val stripSize = 256
        val margin = 2

        var y0 = 0
        while (y0 < height) {
            val y1 = minOf(height, y0 + stripSize)
            val subY0 = maxOf(0, y0 - margin)
            val subY1 = minOf(height, y1 + margin)
            val subH = subY1 - subY0

            // 1. Initial G buffer on sub-band
            val gSub = FloatArray(subH * width)
            for (sy in 0 until subH) {
                val y = subY0 + sy
                val rowOffset = y * width
                val subOffset = sy * width
                for (x in 0 until width) {
                    if (isG(y, x)) {
                        gSub[subOffset + x] = bayer[rowOffset + x]
                    }
                }
            }

            // 2. G_full on sub-band
            val gFull = FloatArray(subH * width)
            for (sy in 0 until subH) {
                val y = subY0 + sy
                val subOffset = sy * width
                val rowOffset = y * width

                val yTop = (y - 1).coerceIn(0, height - 1)
                val yBot = (y + 1).coerceIn(0, height - 1)
                val syTop = (yTop - subY0).coerceIn(0, subH - 1)
                val syBot = (yBot - subY0).coerceIn(0, subH - 1)
                val topOff = syTop * width
                val botOff = syBot * width

                for (x in 0 until width) {
                    if (isG(y, x)) {
                        gFull[subOffset + x] = bayer[rowOffset + x]
                    } else {
                        val xLeft = (x - 1).coerceIn(0, width - 1)
                        val xRight = (x + 1).coerceIn(0, width - 1)
                        val top = gSub[topOff + x]
                        val bot = gSub[botOff + x]
                        val left = gSub[subOffset + xLeft]
                        val right = gSub[subOffset + xRight]
                        gFull[subOffset + x] = 0.25f * (top + bot + left + right)
                    }
                }
            }

            // 3. Chrominance differences (R - G) and (B - G) on sub-band
            val diffR = FloatArray(subH * width)
            val diffB = FloatArray(subH * width)
            for (sy in 0 until subH) {
                val y = subY0 + sy
                val rowOffset = y * width
                val subOffset = sy * width
                for (x in 0 until width) {
                    val gVal = gFull[subOffset + x]
                    if (isR(y, x)) {
                        diffR[subOffset + x] = bayer[rowOffset + x] - gVal
                    }
                    if (isB(y, x)) {
                        diffB[subOffset + x] = bayer[rowOffset + x] - gVal
                    }
                }
            }

            // 4. Convolve chrominance differences and write output for y in [y0, y1)
            for (y in y0 until y1) {
                val sy = y - subY0
                val subOffset = sy * width
                val rowOffset = y * width

                val yTop = (y - 1).coerceIn(0, height - 1)
                val yBot = (y + 1).coerceIn(0, height - 1)
                val syTop = (yTop - subY0).coerceIn(0, subH - 1)
                val syBot = (yBot - subY0).coerceIn(0, subH - 1)
                val topOff = syTop * width
                val botOff = syBot * width

                for (x in 0 until width) {
                    val xLeft = (x - 1).coerceIn(0, width - 1)
                    val xRight = (x + 1).coerceIn(0, width - 1)

                    val gVal = gFull[subOffset + x]
                    val bayerVal = bayer[rowOffset + x]

                    val rOut: Float = if (isR(y, x)) {
                        bayerVal
                    } else {
                        val c = diffR[subOffset + x]
                        val t = diffR[topOff + x]
                        val b = diffR[botOff + x]
                        val l = diffR[subOffset + xLeft]
                        val r = diffR[subOffset + xRight]
                        val tl = diffR[topOff + xLeft]
                        val tr = diffR[topOff + xRight]
                        val bl = diffR[botOff + xLeft]
                        val br = diffR[botOff + xRight]
                        val conv = 1.0f * c + 0.5f * (t + b + l + r) + 0.25f * (tl + tr + bl + br)
                        gVal + conv
                    }

                    val bOut: Float = if (isB(y, x)) {
                        bayerVal
                    } else {
                        val c = diffB[subOffset + x]
                        val t = diffB[topOff + x]
                        val b = diffB[botOff + x]
                        val l = diffB[subOffset + xLeft]
                        val r = diffB[subOffset + xRight]
                        val tl = diffB[topOff + xLeft]
                        val tr = diffB[topOff + xRight]
                        val bl = diffB[botOff + xLeft]
                        val br = diffB[botOff + xRight]
                        val conv = 1.0f * c + 0.5f * (t + b + l + r) + 0.25f * (tl + tr + bl + br)
                        gVal + conv
                    }

                    val outIdx = (rowOffset + x) * 3
                    outRgb[outIdx] = rOut.coerceIn(0.0f, 1.0f)
                    outRgb[outIdx + 1] = gVal.coerceIn(0.0f, 1.0f)
                    outRgb[outIdx + 2] = bOut.coerceIn(0.0f, 1.0f)
                }
            }

            y0 = y1
        }
    }
}
