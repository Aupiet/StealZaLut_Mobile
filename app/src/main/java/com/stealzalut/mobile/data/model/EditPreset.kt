package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Step2Model(
    val globalIntensity: Float = 1.0f,
    val lumaLock: Float = 0.0f,
    val neutralAxisLock: Float = 0.0f,
    val skinToneLock: Float = 0.0f,
    val extremesRolloff: Float = 0.0f,
    val spatial3d: Float = 0.0f
)

@Immutable
data class Step3Model(
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
    val grain: Float = 0.0f
)

@Immutable
data class EditPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailBase64: String? = null,
    val lutReference: String? = null,
    val step2Params: Step2Model = Step2Model(),
    val step3Params: Step3Model = Step3Model(),
    val filePath: String? = null
)
