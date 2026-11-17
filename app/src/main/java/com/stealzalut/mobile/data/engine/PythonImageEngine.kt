package com.stealzalut.mobile.data.engine

import com.stealzalut.engine.ExportBridge
import com.stealzalut.engine.ImageEngine
import com.stealzalut.engine.LutBridge
import com.stealzalut.engine.ZoneBridge
import com.stealzalut.engine.model.EngineConfig
import com.stealzalut.engine.model.EngineResult
import com.stealzalut.engine.model.EngineType
import com.stealzalut.engine.model.Step2Params
import com.stealzalut.engine.model.Step3Params
import com.stealzalut.engine.model.ZoneData
import com.stealzalut.engine.model.ZonePaletteData
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult

class PythonLutBridge(private val bridge: PythonBridgeRepository) : LutBridge {
    override fun generateIdentityGrid(size: Int): FloatArray {
        val count = size * size * size
        val grid = FloatArray(count * 3)
        val step = if (size > 1) 1.0f / (size - 1).toFloat() else 0.0f
        var idx = 0
        for (b in 0 until size) {
            val bVal = b * step
            for (g in 0 until size) {
                val gVal = g * step
                for (r in 0 until size) {
                    val rVal = r * step
                    grid[idx++] = rVal
                    grid[idx++] = gVal
                    grid[idx++] = bVal
                }
            }
        }
        return grid
    }

    override suspend fun applyLutTrilinear(
        image: ByteArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: ByteArray
    ) {
        // Python Chaquopy handles trilinear via bridge
    }

    override suspend fun applyLutTrilinearFloats(
        image: FloatArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: FloatArray
    ) {
        // Python Chaquopy handles trilinear via bridge
    }

    override fun refineAndBlendLut(
        rawLut: FloatArray,
        lutSize: Int,
        intensity: Float,
        skinProtection: Float,
        smoothSigma: Float,
        neutralLock: Float
    ): FloatArray {
        return rawLut
    }

    override fun bakePipeline(
        rawLut: FloatArray,
        lutSize: Int,
        step2: Step2Params,
        step3: Step3Params
    ): FloatArray {
        return rawLut
    }

    override fun exportCube(lut: FloatArray, lutSize: Int, title: String): String {
        return ""
    }
}

class PythonZoneBridge(private val bridge: PythonBridgeRepository) : ZoneBridge {
    override suspend fun extractPalette(
        image: ByteArray,
        width: Int,
        height: Int,
        numColors: Int
    ): List<ZoneData> {
        return emptyList()
    }

    override suspend fun analyzeZones(
        image: ByteArray,
        width: Int,
        height: Int,
        zoneCount: Int
    ): ZonePaletteData {
        return ZonePaletteData(emptyList(), width, height)
    }

    override fun matchZones(
        source: ZonePaletteData,
        reference: ZonePaletteData
    ): Map<Int, Map<Int, Float>> {
        return emptyMap()
    }

    override suspend fun fitZoneTransfer(
        source: ByteArray,
        reference: ByteArray,
        width: Int,
        height: Int,
        zoneCount: Int,
        userOverrides: Map<Int, Int>?
    ): FloatArray {
        return FloatArray(0)
    }
}

class PythonExportBridge(private val bridge: PythonBridgeRepository) : ExportBridge {
    override suspend fun generatePreview(
        sourcePath: String,
        referencePath: String?,
        outputPath: String,
        maxDimension: Int,
        preserveLuminance: Float,
        lutSize: Int
    ): EngineResult {
        val result = bridge.generatePreview(
            srcPath = sourcePath,
            refPath = referencePath ?: sourcePath,
            method = ColorTransferMethod.MKL,
            intensity = 1.0f,
            preserveLuminance = preserveLuminance
        )
        return when (result) {
            is PreviewResult.Success -> EngineResult(
                success = true,
                outputPath = result.previewPath,
                executionTimeMs = result.executionTimeMs.toLong()
            )
            is PreviewResult.Error -> EngineResult(
                success = false,
                errorMessage = result.message
            )
        }
    }

    override suspend fun exportCube(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult {
        val result = bridge.exportCube(
            srcPath = sourcePath,
            refPath = referencePath,
            lutSize = LutSize.fromSize(lutSize),
            method = ColorTransferMethod.MKL,
            intensity = 1.0f,
            preserveLuminance = preserveLuminance
        )
        return when (result) {
            is CubeExportResult.Success -> EngineResult(
                success = true,
                outputPath = result.cubePath,
                executionTimeMs = result.executionTimeMs.toLong()
            )
            is CubeExportResult.Error -> EngineResult(
                success = false,
                errorMessage = result.message
            )
        }
    }

    override suspend fun exportGradedImage(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult {
        val result = bridge.exportGradedImage(
            srcPath = sourcePath,
            refPath = referencePath,
            method = ColorTransferMethod.MKL,
            intensity = 1.0f,
            preserveLuminance = preserveLuminance
        )
        return when (result) {
            is ImageExportResult.Success -> EngineResult(
                success = true,
                outputPath = result.imagePath,
                executionTimeMs = result.executionTimeMs.toLong()
            )
            is ImageExportResult.Error -> EngineResult(
                success = false,
                errorMessage = result.message
            )
        }
    }
}

class PythonImageEngine(
    private val bridge: PythonBridgeRepository,
    override val config: EngineConfig = EngineConfig(type = EngineType.PYTHON)
) : ImageEngine {
    override val name: String = "PythonEngine"
    override val lut: LutBridge = PythonLutBridge(bridge)
    override val zone: ZoneBridge = PythonZoneBridge(bridge)
    override val export: ExportBridge = PythonExportBridge(bridge)
}
