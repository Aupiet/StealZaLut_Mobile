package com.stealzalut.mobile.ui.viewmodel.delegate

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StandaloneEditorDebounceRequestTest {
    @Test
    fun `identity preview does not require Python image processing`() {
        assertFalse(defaultRequest().requiresImageProcessing)
    }

    @Test
    fun `changed image settings require Python image processing`() {
        assertTrue(defaultRequest().copy(exposure = 0.25f).requiresImageProcessing)
        assertTrue(defaultRequest().copy(customLutPath = "look.cube").requiresImageProcessing)
        assertTrue(defaultRequest().copy(intensity = 0.8f).requiresImageProcessing)
    }

    private fun defaultRequest() = StandaloneEditorDebounceRequest(
        srcPath = "source.jpg",
        intensity = 1.0f,
        lumaLock = 0.0f,
        neutralAxisLock = 0.0f,
        skinToneLock = 0.0f,
        extremesRolloff = 0.0f,
        spatial3d = 0.0f,
        exposure = 0.0f,
        contrast = 0.0f,
        highlights = 0.0f,
        shadows = 0.0f,
        temperature = 0.0f,
        tint = 0.0f,
        vibrance = 0.0f,
        saturation = 0.0f,
        clarity = 0.0f,
        sharpen = 0.0f,
        grain = 0.0f,
        customLutPath = null
    )
}
