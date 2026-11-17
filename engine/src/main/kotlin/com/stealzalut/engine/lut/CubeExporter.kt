package com.stealzalut.engine.lut

import java.io.StringWriter

object CubeExporter {
    fun exportCube(lut: FloatArray, lutSize: Int, title: String = "StealZaLUT Export"): String {
        val totalEntries = lutSize * lutSize * lutSize
        require(lut.size >= totalEntries * 3) {
            "LUT array size ${lut.size} does not match expected size ${totalEntries * 3}"
        }

        val writer = StringWriter(totalEntries * 30)
        writer.append("# Created by StealZaLUT Kotlin Engine\n")
        writer.append("TITLE \"").append(title).append("\"\n")
        writer.append("LUT_3D_SIZE ").append(lutSize.toString()).append("\n")
        writer.append("DOMAIN_MIN 0.0 0.0 0.0\n")
        writer.append("DOMAIN_MAX 1.0 1.0 1.0\n\n")

        var idx = 0
        for (i in 0 until totalEntries) {
            val r = maxOf(0.0f, minOf(1.0f, lut[idx]))
            val g = maxOf(0.0f, minOf(1.0f, lut[idx + 1]))
            val b = maxOf(0.0f, minOf(1.0f, lut[idx + 2]))
            writer.append(String.format(java.util.Locale.US, "%.6f %.6f %.6f\n", r, g, b))
            idx += 3
        }
        return writer.toString()
    }
}
