package com.stealzalut.engine.lut

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.roundToInt

data class ParsedCube(val size: Int, val data: FloatArray)

object CubeParser {

    fun parse(input: InputStream): ParsedCube {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        var lutSize = 0
        val floatList = ArrayList<Float>(33 * 33 * 33 * 3)

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                val parts = line.split("\\s+".toRegex())
                if (parts[0].equals("LUT_3D_SIZE", ignoreCase = true) && parts.size >= 2) {
                    lutSize = parts[1].toIntOrNull() ?: 0
                    continue
                }

                if (parts.size == 3) {
                    val r = parts[0].toFloatOrNull()
                    val g = parts[1].toFloatOrNull()
                    val b = parts[2].toFloatOrNull()
                    if (r != null && g != null && b != null) {
                        floatList.add(r.coerceIn(0.0f, 1.0f))
                        floatList.add(g.coerceIn(0.0f, 1.0f))
                        floatList.add(b.coerceIn(0.0f, 1.0f))
                    }
                }
            }
        }

        val totalRgb = floatList.size / 3
        if (lutSize <= 0) {
            val inferred = Math.cbrt(totalRgb.toDouble()).roundToInt()
            if (inferred > 1 && inferred * inferred * inferred == totalRgb) {
                lutSize = inferred
            } else {
                throw IllegalArgumentException("Could not determine valid LUT_3D_SIZE (total entries: $totalRgb)")
            }
        }

        val expected = lutSize * lutSize * lutSize
        require(totalRgb == expected) {
            "Expected $expected entries for size $lutSize, but parsed $totalRgb"
        }

        val array = FloatArray(floatList.size)
        for (i in floatList.indices) {
            array[i] = floatList[i]
        }
        return ParsedCube(lutSize, array)
    }
}
