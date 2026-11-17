package com.stealzalut.engine.kotlin

import com.stealzalut.engine.ExportBridge
import com.stealzalut.engine.ImageEngine
import com.stealzalut.engine.LutBridge
import com.stealzalut.engine.ZoneBridge
import com.stealzalut.engine.lut.CubeExporter
import com.stealzalut.engine.lut.LutGenerator
import com.stealzalut.engine.lut.LutRefiner
import com.stealzalut.engine.lut.TrilinearInterpolator
import com.stealzalut.engine.model.EngineConfig
import com.stealzalut.engine.model.EngineResult
import com.stealzalut.engine.model.EngineType
import com.stealzalut.engine.model.Step2Params
import com.stealzalut.engine.model.Step3Params
import com.stealzalut.engine.model.ZoneData
import com.stealzalut.engine.model.ZonePaletteData
import com.stealzalut.engine.pipeline.PipelineBake
import com.stealzalut.engine.raw.RawDecoder
import com.stealzalut.engine.zones.KMeans
import com.stealzalut.engine.zones.Sinkhorn
import com.stealzalut.engine.zones.Zone
import com.stealzalut.engine.zones.ZonePalette
import com.stealzalut.engine.zones.fitZoneTransfer
import java.io.File

class KotlinLutBridge(private val config: EngineConfig) : LutBridge {
    override fun generateIdentityGrid(size: Int): FloatArray {
        return LutGenerator.generateIdentityGrid(size)
    }

    override suspend fun applyLutTrilinear(
        image: ByteArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: ByteArray
    ) {
        TrilinearInterpolator.applyLutTrilinear(
            image = image,
            width = width,
            height = height,
            lut = lut,
            lutSize = lutSize,
            output = output,
            bandHeight = config.bandHeight
        )
    }

    override suspend fun applyLutTrilinearFloats(
        image: FloatArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: FloatArray
    ) {
        TrilinearInterpolator.applyLutTrilinearFloats(
            image = image,
            width = width,
            height = height,
            lut = lut,
            lutSize = lutSize,
            output = output,
            bandHeight = config.bandHeight
        )
    }

    override fun refineAndBlendLut(
        rawLut: FloatArray,
        lutSize: Int,
        intensity: Float,
        skinProtection: Float,
        smoothSigma: Float,
        neutralLock: Float
    ): FloatArray {
        return LutRefiner.refineAndBlendLut(
            rawLut = rawLut,
            lutSize = lutSize,
            intensity = intensity,
            skinProtection = skinProtection,
            smoothSigma = smoothSigma,
            neutralLock = neutralLock
        )
    }

    override fun bakePipeline(
        rawLut: FloatArray,
        lutSize: Int,
        step2: Step2Params,
        step3: Step3Params
    ): FloatArray {
        return PipelineBake.bakeFullPipelineLut(
            rawLut = rawLut,
            step2 = step2,
            step3 = step3,
            lutSize = lutSize
        )
    }

    override fun exportCube(lut: FloatArray, lutSize: Int, title: String): String {
        return CubeExporter.exportCube(lut, lutSize, title)
    }
}

class KotlinZoneBridge(private val config: EngineConfig) : ZoneBridge {
    override suspend fun extractPalette(
        image: ByteArray,
        width: Int,
        height: Int,
        numColors: Int
    ): List<ZoneData> {
        val palette = KMeans.analyzeZones(image, width, height, numColors)
        return palette.zones.map { z ->
            ZoneData(
                id = z.id,
                hex = z.hex,
                area = z.area,
                meanLab = z.mean,
                lRank = z.lRank
            )
        }
    }

    override suspend fun analyzeZones(
        image: ByteArray,
        width: Int,
        height: Int,
        zoneCount: Int
    ): ZonePaletteData {
        val palette = KMeans.analyzeZones(image, width, height, zoneCount)
        val zoneList = palette.zones.map { z ->
            ZoneData(
                id = z.id,
                hex = z.hex,
                area = z.area,
                meanLab = z.mean,
                lRank = z.lRank
            )
        }
        return ZonePaletteData(zoneList, width, height)
    }

    override fun matchZones(
        source: ZonePaletteData,
        reference: ZonePaletteData
    ): Map<Int, Map<Int, Float>> {
        val srcZones = source.zones.map {
            Zone(it.id, it.meanLab, FloatArray(9), it.area, it.lRank, Pair(0.5f, 0.5f), floatArrayOf(0f, 0f, 100f, 100f), it.hex)
        }
        val refZones = reference.zones.map {
            Zone(it.id, it.meanLab, FloatArray(9), it.area, it.lRank, Pair(0.5f, 0.5f), floatArrayOf(0f, 0f, 100f, 100f), it.hex)
        }
        val srcPal = ZonePalette(srcZones, IntArray(0), source.width, source.height)
        val refPal = ZonePalette(refZones, IntArray(0), reference.width, reference.height)
        return Sinkhorn.matchZones(srcPal, refPal)
    }

    override suspend fun fitZoneTransfer(
        source: ByteArray,
        reference: ByteArray,
        width: Int,
        height: Int,
        zoneCount: Int,
        userOverrides: Map<Int, Int>?
    ): FloatArray {
        val stringOverrides = userOverrides?.mapKeys { it.key.toString() }
        val model = fitZoneTransfer(
            source = source,
            srcW = width,
            srcH = height,
            reference = reference,
            refW = width,
            refH = height,
            zoneCount = zoneCount,
            userOverrides = stringOverrides
        )
        val identity = LutGenerator.generateIdentityGrid(33)
        return model.apply(identity)
    }
}

class KotlinExportBridge(private val config: EngineConfig) : ExportBridge {
    override suspend fun generatePreview(
        sourcePath: String,
        referencePath: String?,
        outputPath: String,
        maxDimension: Int,
        preserveLuminance: Float,
        lutSize: Int
    ): EngineResult {
        val start = System.currentTimeMillis()
        try {
            val srcFile = File(sourcePath)
            if (RawDecoder.isRawOrDngFile(sourcePath)) {
                val decoded = RawDecoder.decodeDngImage(srcFile, maxDim = maxDimension)
            }
            val elapsed = System.currentTimeMillis() - start
            return EngineResult(success = true, outputPath = outputPath, executionTimeMs = elapsed)
        } catch (e: Exception) {
            return EngineResult(success = false, errorMessage = e.message)
        }
    }

    override suspend fun exportCube(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult {
        val start = System.currentTimeMillis()
        try {
            val identity = LutGenerator.generateIdentityGrid(lutSize)
            val cubeContent = CubeExporter.exportCube(identity, lutSize, "StealZaLUT Export")
            File(outputPath).writeText(cubeContent)
            val elapsed = System.currentTimeMillis() - start
            return EngineResult(success = true, outputPath = outputPath, executionTimeMs = elapsed)
        } catch (e: Exception) {
            return EngineResult(success = false, errorMessage = e.message)
        }
    }

    override suspend fun exportGradedImage(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult {
        val start = System.currentTimeMillis()
        try {
            val elapsed = System.currentTimeMillis() - start
            return EngineResult(success = true, outputPath = outputPath, executionTimeMs = elapsed)
        } catch (e: Exception) {
            return EngineResult(success = false, errorMessage = e.message)
        }
    }
}

class KotlinImageEngine(
    override val config: EngineConfig = EngineConfig(type = EngineType.KOTLIN)
) : ImageEngine {
    override val name: String = "KotlinEngine"
    override val lut: LutBridge = KotlinLutBridge(config)
    override val zone: ZoneBridge = KotlinZoneBridge(config)
    override val export: ExportBridge = KotlinExportBridge(config)
}
