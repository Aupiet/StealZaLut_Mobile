package com.stealzalut.mobile.core.image

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SampledBitmapLoaderTest {
    @Test
    fun keepsFullResolutionWhenAlreadyWithinLimit() {
        assertEquals(1, SampledBitmapLoader.calculateInSampleSize(480, 320, 600))
    }

    @Test
    fun selectsPowerOfTwoSampleToBoundLargeDimension() {
        assertEquals(8, SampledBitmapLoader.calculateInSampleSize(4000, 3000, 500))
    }

    @Test
    fun usesLongestSideForPortraitImages() {
        assertEquals(4, SampledBitmapLoader.calculateInSampleSize(2000, 5000, 1440))
    }
}
