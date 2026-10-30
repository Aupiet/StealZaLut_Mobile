package com.example.stealzalut_mobile.data.model

import java.util.UUID

/**
 * Saved colorimetric theme / profile.
 *
 * @property id Unique identifier
 * @property name User-given title (e.g. "Hollywood Warm 70s")
 * @property createdAt Timestamp in milliseconds
 * @property method Color transfer algorithm used
 * @property intensity Opacity blend in [0.0, 1.0]
 * @property lutSize Default 3D LUT grid size
 * @property referenceImagePath Cached reference image path
 * @property paletteHexes Dominant color hex codes (e.g. ["#E67E22", "#2C3E50"])
 * @property colorSetImagePath Path to generated calibration test card (mire)
 * @property cubeFilePath Path to generated .cube file if already exported
 * @property isBuiltIn True if this is a factory-provided preset
 * @property notes Optional user notes/description
 */
data class ColorimetryPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val intensity: Float = 1.0f,
    val lutSize: LutSize = LutSize.STANDARD,
    val referenceImagePath: String? = null,
    val paletteHexes: List<String> = emptyList(),
    val colorSetImagePath: String? = null,
    val cubeFilePath: String? = null,
    val isBuiltIn: Boolean = false,
    val notes: String? = null
)
