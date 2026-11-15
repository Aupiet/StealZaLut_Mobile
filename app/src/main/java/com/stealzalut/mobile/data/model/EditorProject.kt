package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * Full snapshot of active editing parameters across StealZaLUT, Adobe Lightroom develop panels,
 * custom LUTs, and non-destructive normalized crop.
 */
@Immutable
data class EditorParamsSnapshot(
    val intensity: Float = 1.0f,
    val lumaLock: Float = 0.0f,
    val neutralAxisLock: Float = 0.0f,
    val skinToneLock: Float = 0.0f,
    val extremesRolloff: Float = 0.0f,
    val spatial3d: Float = 0.0f,
    val exposure: Float = 0.0f,
    val contrast: Float = 0.0f,
    val highlights: Float = 0.0f,
    val shadows: Float = 0.0f,
    val temperature: Float = 0.0f,
    val tint: Float = 0.0f,
    val vibrance: Float = 0.0f,
    val saturation: Float = 0.0f,
    val clarity: Float = 0.0f,
    val sharpen: Float = 0.0f,
    val grain: Float = 0.0f,
    val customLutPath: String? = null,
    val customLutName: String? = null,
    val lightroomParams: LightroomDevelopModel = LightroomDevelopModel(),
    val cropRegion: CropRegion = CropRegion()
)

/**
 * Editorial non-destructive photo editing project draft persisted across app sessions.
 */
@Immutable
data class EditorProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceImagePath: String,
    val previewImagePath: String? = null,
    val snapshot: EditorParamsSnapshot = EditorParamsSnapshot(),
    val width: Int = 0,
    val height: Int = 0
)

/**
 * Snapshot of Studio Step 3 parameters and zone overrides persisted across app sessions.
 */
@Immutable
data class StudioSessionSnapshot(
    val method: String = "HM_MKL_HM",
    val intensity: Float = 1.0f,
    val preserveLuminance: Float = 0.8f,
    val skinProtection: Float = 0.8f,
    val smoothSigma: Float = 0.0f,
    val neutralLock: Float = 0.5f,
    val lutSize: Int = 33,
    val userOverrides: Map<String, Int> = emptyMap()
)

/**
 * Persisted Studio grading session.
 */
@Immutable
data class StudioSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Studio Session",
    val updatedAt: Long = System.currentTimeMillis(),
    val sourcePath: String? = null,
    val referencePath: String? = null,
    val snapshot: StudioSessionSnapshot = StudioSessionSnapshot()
)
