package com.stealzalut.mobile.core.image

import com.stealzalut.mobile.core.AppConstants
import kotlin.math.floor
import kotlin.math.sqrt

internal data class ExportMemoryDecision(
    val maxDimension: Int,
    val wasReducedForMemory: Boolean
)

internal object ExportMemoryGuard {
    fun resolve(
        width: Int,
        height: Int,
        requestedMaxDimension: Int,
        availableMemoryBytes: Long
    ): ExportMemoryDecision? {
        if (width <= 0 || height <= 0 || availableMemoryBytes <= 0L) return null

        val sourceMaxDimension = maxOf(width, height)
        val requestedLimit = if (requestedMaxDimension > 0) {
            minOf(requestedMaxDimension, sourceMaxDimension)
        } else {
            sourceMaxDimension
        }
        val sourcePixels = width.toDouble() * height.toDouble()
        val allowedPixels = (
            availableMemoryBytes.toDouble() *
                AppConstants.EXPORT_MEMORY_BUDGET_FRACTION /
                AppConstants.EXPORT_ESTIMATED_BYTES_PER_PIXEL
            ).toLong()
        if (allowedPixels <= 0L) return null

        val memoryLimit = if (sourcePixels <= allowedPixels.toDouble()) {
            sourceMaxDimension
        } else {
            floor(sourceMaxDimension * sqrt(allowedPixels / sourcePixels)).toInt()
        }
        val safeDimension = minOf(requestedLimit, memoryLimit)
        if (safeDimension < AppConstants.EXPORT_MINIMUM_SAFE_DIMENSION) return null

        return ExportMemoryDecision(
            maxDimension = safeDimension,
            wasReducedForMemory = safeDimension < requestedLimit
        )
    }

    fun fallback(requestedMaxDimension: Int): ExportMemoryDecision {
        val fallbackDimension = AppConstants.EXPORT_UNKNOWN_DIMENSION_FALLBACK
        val requestedLimit = requestedMaxDimension.takeIf { it > 0 }
        return ExportMemoryDecision(
            maxDimension = requestedLimit?.let { minOf(it, fallbackDimension) } ?: fallbackDimension,
            wasReducedForMemory = requestedLimit == null || requestedLimit > fallbackDimension
        )
    }
}
