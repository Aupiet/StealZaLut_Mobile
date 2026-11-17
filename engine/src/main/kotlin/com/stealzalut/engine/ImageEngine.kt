package com.stealzalut.engine

import com.stealzalut.engine.model.EngineConfig
import com.stealzalut.engine.model.EngineResult
import com.stealzalut.engine.model.Step2Params
import com.stealzalut.engine.model.Step3Params
import com.stealzalut.engine.model.ZoneData
import com.stealzalut.engine.model.ZonePaletteData

interface LutBridge {
    fun generateIdentityGrid(size: Int): FloatArray
    suspend fun applyLutTrilinear(
        image: ByteArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: ByteArray
    )
    suspend fun applyLutTrilinearFloats(
        image: FloatArray,
        width: Int,
        height: Int,
        lut: FloatArray,
        lutSize: Int,
        output: FloatArray
    )
    fun refineAndBlendLut(
        rawLut: FloatArray,
        lutSize: Int,
        intensity: Float = 1.0f,
        skinProtection: Float = 0.0f,
        smoothSigma: Float = 0.0f,
        neutralLock: Float = 0.0f
    ): FloatArray
    fun bakePipeline(
        rawLut: FloatArray,
        lutSize: Int,
        step2: Step2Params,
        step3: Step3Params
    ): FloatArray
    fun exportCube(lut: FloatArray, lutSize: Int, title: String): String
}

interface ZoneBridge {
    suspend fun extractPalette(image: ByteArray, width: Int, height: Int, numColors: Int): List<ZoneData>
    suspend fun analyzeZones(image: ByteArray, width: Int, height: Int, zoneCount: Int): ZonePaletteData
    fun matchZones(source: ZonePaletteData, reference: ZonePaletteData): Map<Int, Map<Int, Float>>
    suspend fun fitZoneTransfer(
        source: ByteArray,
        reference: ByteArray,
        width: Int,
        height: Int,
        zoneCount: Int,
        userOverrides: Map<Int, Int>?
    ): FloatArray
}

interface ExportBridge {
    suspend fun generatePreview(
        sourcePath: String,
        referencePath: String?,
        outputPath: String,
        maxDimension: Int,
        preserveLuminance: Float,
        lutSize: Int
    ): EngineResult

    suspend fun exportCube(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult

    suspend fun exportGradedImage(
        sourcePath: String,
        referencePath: String,
        outputPath: String,
        lutSize: Int,
        preserveLuminance: Float
    ): EngineResult
}

interface ImageEngine {
    val name: String
    val config: EngineConfig
    val lut: LutBridge
    val zone: ZoneBridge
    val export: ExportBridge
}
