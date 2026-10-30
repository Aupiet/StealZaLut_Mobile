package com.stealzalut.mobile.core

/**
 * Centralized business, algorithm, timing, and threshold constants for StealZaLUT Pro.
 * Eliminates magic numbers across the entire codebase.
 */
object AppConstants {

    // --- Intensity Slider ---
    const val INTENSITY_MIN: Float = 0.0f
    const val INTENSITY_MAX: Float = 1.0f
    const val INTENSITY_DEFAULT: Float = 1.0f
    const val INTENSITY_STEP: Float = 0.05f
    const val INTENSITY_SLIDER_STEPS: Int = 19 // (1.0 - 0.0) / 0.05 - 1
    const val INTENSITY_PERCENT_MULTIPLIER: Float = 100.0f

    // Quick preset values for intensity
    val INTENSITY_PRESETS: List<Float> = listOf(0.25f, 0.50f, 0.75f, 1.0f)

    // --- Zoom & Pan (Double View Comparison) ---
    const val ZOOM_SCALE_MIN: Float = 1.0f
    const val ZOOM_SCALE_MAX: Float = 5.0f
    const val ZOOM_SCALE_DOUBLE_TAP: Float = 2.5f
    const val ZOOM_THRESHOLD_RESET: Float = 1.05f

    // --- Split Wipe Comparison ---
    const val SPLIT_WIPE_DEFAULT_FRACTION: Float = 0.5f
    const val SPLIT_WIPE_MIN_FRACTION: Float = 0.0f
    const val SPLIT_WIPE_MAX_FRACTION: Float = 1.0f
    const val SPLIT_WIPE_DIVIDER_ALPHA: Float = 0.9f

    // --- Timing & Debounce ---
    const val DEBOUNCE_METHOD_SWITCH_MS: Long = 150L
    const val COMPUTE_STAGE_DELAY_MS: Long = 400L
    const val SNACKBAR_MESSAGE_RESET_DELAY_MS: Long = 3000L

    // --- Image Processing & Dimensions ---
    const val PRESET_THUMBNAIL_MAX_DIMENSION: Int = 256
    const val PRESET_THUMBNAIL_QUALITY: Int = 85
    const val PREVIEW_MAX_DIMENSION: Int = 1024
    const val PALETTE_EXTRACTION_MAX_DIMENSION: Int = 128
    const val DEFAULT_PALETTE_COLOR_COUNT: Int = 5

    // --- Cache & Storage Policies ---
    const val MAX_CACHE_SIZE_BYTES: Long = 50L * 1024L * 1024L // 50 MB
    const val CACHE_EXPIRATION_MILLIS: Long = 24L * 60L * 60L * 1000L // 24 Hours
    const val BYTES_PER_MEGABYTE: Double = 1024.0 * 1024.0

    // --- Export Defaults ---
    const val DEFAULT_JPEG_QUALITY: Int = 92
    const val DEFAULT_PROXY_DIMENSION: Int = 1024
    const val DEFAULT_EXPORT_DIMENSION: Int = 2048

    // --- Alpha / Opacity Values ---
    const val OPACITY_CONTAINER_SUBTLE: Float = 0.3f
    const val OPACITY_CONTAINER_MEDIUM: Float = 0.5f
    const val OPACITY_CONTAINER_HIGH: Float = 0.7f
    const val OPACITY_CONTAINER_HEAVY: Float = 0.85f
    const val OPACITY_BORDER_SUBTLE: Float = 0.4f
    const val OPACITY_BORDER_MEDIUM: Float = 0.5f

    // --- Stepper Funnel Steps ---
    const val STEP_1_SOURCE: Int = 1
    const val STEP_2_STYLE: Int = 2
    const val STEP_3_PARAMS: Int = 3
    const val STEP_4_COMPUTE: Int = 4
    const val STEP_5_RESULT: Int = 5
    const val TOTAL_WIZARD_STEPS: Int = 5

    // --- Profile Screen Grid ---
    const val PROFILE_GRID_COLUMNS: Int = 3
    const val DATE_FORMAT_DISPLAY: String = "dd/MM/yyyy HH:mm"

    // --- Layout Weights ---
    const val WEIGHT_PRIMARY_ACTION: Float = 1.3f
    const val WEIGHT_SECONDARY_ACTION: Float = 1.0f
    const val WEIGHT_EQUAL: Float = 1.0f

    // --- MIME Types ---
    const val MIME_TYPE_JPEG: String = "image/jpeg"
    const val MIME_TYPE_OCTET_STREAM: String = "application/octet-stream"

    // --- Language Tags ---
    const val LANG_TAG_FR: String = "fr"
    const val LANG_TAG_EN: String = "en"
    const val LANG_TAG_ES: String = "es"

    // --- Export Dimensions ---
    const val EXPORT_DIM_1080P: Int = 1080
    const val EXPORT_DIM_2K: Int = 2048
    const val EXPORT_DIM_4K: Int = 3840
    const val EXPORT_DIM_NATIVE: Int = 0

    // --- Export Formats ---
    const val EXPORT_FORMAT_JPEG: String = "JPEG"
    const val EXPORT_FORMAT_PNG: String = "PNG"

    // --- JPEG Quality Slider ---
    const val QUALITY_MIN: Float = 75.0f
    const val QUALITY_MAX: Float = 100.0f
    const val QUALITY_STEPS: Int = 5
}
