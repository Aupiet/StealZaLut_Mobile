package com.example.stealzalut_mobile.data.model

/**
 * Result model returned when generating a color-transfer preview.
 */
sealed interface PreviewResult {
    data class Success(
        val previewPath: String,
        val executionTimeMs: Double
    ) : PreviewResult

    data class Error(
        val message: String
    ) : PreviewResult
}

/**
 * Result model returned when generating an Adobe .cube 3D LUT file.
 */
sealed interface CubeExportResult {
    data class Success(
        val cubePath: String,
        val lutSize: Int,
        val executionTimeMs: Double
    ) : CubeExportResult

    data class Error(
        val message: String
    ) : CubeExportResult
}

/**
 * Result model returned when directly exporting a high-resolution graded image.
 */
sealed interface ImageExportResult {
    data class Success(
        val imagePath: String,
        val width: Int,
        val height: Int,
        val executionTimeMs: Double
    ) : ImageExportResult

    data class Error(
        val message: String
    ) : ImageExportResult
}

/**
 * Result model returned when generating the calibration test card & color set.
 */
sealed interface ColorSetResult {
    data class Success(
        val colorSetPath: String,
        val refPalette: List<String>,
        val gradedPalette: List<String>,
        val executionTimeMs: Double
    ) : ColorSetResult

    data class Error(
        val message: String
    ) : ColorSetResult
}
