package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * Metadata for a graded image exported and stored in the user's profile library.
 *
 * @property id Unique identifier
 * @property filePath Local absolute file path
 * @property timestamp Creation timestamp in milliseconds
 * @property width Image width in pixels
 * @property height Image height in pixels
 * @property themeName Name of the theme/preset used to grade this photo
 * @property method Color transfer algorithm used
 * @property intensity Intensity blend in [0.0, 1.0]
 * @property referenceImagePath Path to the style reference image used
 * @property paletteHexes Dominant color swatches associated with this look
 */
@Immutable
data class SavedGradedImage(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val themeName: String? = null,
    val method: String? = "hm-mkl-hm",
    val intensity: Float = 1.0f,
    val referenceImagePath: String? = null,
    val paletteHexes: List<String> = emptyList()
)
