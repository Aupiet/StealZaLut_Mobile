package com.stealzalut.mobile.core.image

import com.stealzalut.mobile.core.AppConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportMemoryGuardTest {
    @Test
    fun nativeExportKeepsOriginalDimensionWhenMemoryIsSufficient() {
        val result = ExportMemoryGuard.resolve(
            width = 4000,
            height = 3000,
            requestedMaxDimension = AppConstants.EXPORT_DIM_NATIVE,
            availableMemoryBytes = 8L * 1024L * 1024L * 1024L
        )

        assertEquals(4000, result?.maxDimension)
        assertFalse(result?.wasReducedForMemory ?: true)
    }

    @Test
    fun nativeExportIsReducedWhenEstimatedPeakExceedsBudget() {
        val result = ExportMemoryGuard.resolve(
            width = 8000,
            height = 6000,
            requestedMaxDimension = AppConstants.EXPORT_DIM_NATIVE,
            availableMemoryBytes = 256L * 1024L * 1024L
        )

        assertNotNull(result)
        val decision = result ?: throw AssertionError("Expected a bounded export size")
        assertTrue(decision.maxDimension < 8000)
        assertTrue(decision.wasReducedForMemory)
    }

    @Test
    fun exportBelowMinimumSafeSizeIsRejected() {
        val result = ExportMemoryGuard.resolve(
            width = 8000,
            height = 6000,
            requestedMaxDimension = AppConstants.EXPORT_DIM_NATIVE,
            availableMemoryBytes = 1024L
        )

        assertNull(result)
    }

    @Test
    fun explicitDimensionIsPreservedUnlessMemoryRequiresFurtherReduction() {
        val result = ExportMemoryGuard.resolve(
            width = 6000,
            height = 4000,
            requestedMaxDimension = AppConstants.EXPORT_DIM_2K,
            availableMemoryBytes = 8L * 1024L * 1024L * 1024L
        )

        assertEquals(AppConstants.EXPORT_DIM_2K, result?.maxDimension)
        assertFalse(result?.wasReducedForMemory ?: true)
    }
}
