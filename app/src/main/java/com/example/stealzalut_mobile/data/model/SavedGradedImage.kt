package com.example.stealzalut_mobile.data.model

import java.util.UUID

/**
 * Metadata for a graded image exported and stored in the user's profile folder.
 *
 * @property id Unique identifier
 * @property filePath Local absolute file path
 * @property timestamp Creation timestamp in milliseconds
 * @property width Image width in pixels
 * @property height Image height in pixels
 * @property themeName Name of the theme/look used to grade this photo
 */
data class SavedGradedImage(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val themeName: String? = null
)
