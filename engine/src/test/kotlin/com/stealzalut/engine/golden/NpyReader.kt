package com.stealzalut.engine.golden

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

object NpyReader {

    fun readFloatArrayFromNpz(npzFile: File, entryName: String): FloatArray {
        val zip = ZipFile(npzFile)
        val name = if (entryName.endsWith(".npy")) entryName else "$entryName.npy"
        val entry = zip.getEntry(name)
            ?: throw IllegalArgumentException("Entry $name not found in ${npzFile.name}")

        return zip.getInputStream(entry).use { stream ->
            readNpyFloatArray(stream)
        }
    }

    fun readByteArrayFromNpz(npzFile: File, entryName: String): ByteArray {
        val zip = ZipFile(npzFile)
        val name = if (entryName.endsWith(".npy")) entryName else "$entryName.npy"
        val entry = zip.getEntry(name)
            ?: throw IllegalArgumentException("Entry $name not found in ${npzFile.name}")

        return zip.getInputStream(entry).use { stream ->
            readNpyByteArray(stream)
        }
    }

    private fun readNpyFloatArray(stream: InputStream): FloatArray {
        val magicAndVersion = ByteArray(8)
        stream.readNBytes(magicAndVersion, 0, 8)
        check(magicAndVersion[0] == 0x93.toByte() && String(magicAndVersion, 1, 5) == "NUMPY") {
            "Invalid NPY magic"
        }

        val major = magicAndVersion[6].toInt()
        val headerLen = if (major >= 2) {
            val lenBytes = ByteArray(4)
            stream.readNBytes(lenBytes, 0, 4)
            ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).int
        } else {
            val b0 = stream.read() and 0xFF
            val b1 = stream.read() and 0xFF
            b0 or (b1 shl 8)
        }

        val dictBytes = ByteArray(headerLen)
        stream.readNBytes(dictBytes, 0, headerLen)
        val rawData = stream.readAllBytes()

        val floatBuffer = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val floats = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floats)
        return floats
    }

    private fun readNpyByteArray(stream: InputStream): ByteArray {
        val magicAndVersion = ByteArray(8)
        stream.readNBytes(magicAndVersion, 0, 8)
        check(magicAndVersion[0] == 0x93.toByte() && String(magicAndVersion, 1, 5) == "NUMPY") {
            "Invalid NPY magic"
        }

        val major = magicAndVersion[6].toInt()
        val headerLen = if (major >= 2) {
            val lenBytes = ByteArray(4)
            stream.readNBytes(lenBytes, 0, 4)
            ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).int
        } else {
            val b0 = stream.read() and 0xFF
            val b1 = stream.read() and 0xFF
            b0 or (b1 shl 8)
        }

        val dictBytes = ByteArray(headerLen)
        stream.readNBytes(dictBytes, 0, headerLen)
        return stream.readAllBytes()
    }
}
