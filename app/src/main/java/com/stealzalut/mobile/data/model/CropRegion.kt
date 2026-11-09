package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable

/**
 * Normalized crop bounding box coordinates [0.0f .. 1.0f] and rotation angle in degrees.
 * Decoupled contract aligned with Adobe Camera Raw / Lightroom specifications
 * (crs:CropLeft, crs:CropTop, crs:CropRight, crs:CropBottom, crs:CropAngle).
 */
@Immutable
data class CropRegion(
    val left: Float = 0.0f,
    val top: Float = 0.0f,
    val right: Float = 1.0f,
    val bottom: Float = 1.0f,
    val angle: Float = 0.0f
) {
    val isDefault: Boolean
        get() = left == 0.0f && top == 0.0f && right == 1.0f && bottom == 1.0f && angle == 0.0f

    val hasCrop: Boolean
        get() = !isDefault
}
