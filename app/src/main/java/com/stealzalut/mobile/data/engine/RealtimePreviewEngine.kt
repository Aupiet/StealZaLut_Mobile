package com.stealzalut.mobile.data.engine

import android.graphics.Bitmap
import android.graphics.RenderEffect
import com.stealzalut.mobile.data.model.LightroomDevelopModel

/**
 * Decoupled interface contract for real-time GPU/AGSL preview rendering.
 * Frontend views and Viewport consume this engine without direct coupling to C++/Python kernels.
 */
interface RealtimePreviewEngine {
    /**
     * Produces an AGSL/Hardware RenderEffect for Android 13+ (API 33+) or null if not supported.
     */
    fun createRenderEffect(model: LightroomDevelopModel): RenderEffect?

    /**
     * Software or fallback bitmap rendering hook for older Android APIs or offline processing.
     */
    suspend fun renderPreviewBitmap(source: Bitmap, model: LightroomDevelopModel): Bitmap
}

/**
 * Default CPU/software preview engine implementation used when hardware shader acceleration is unavailable.
 */
class DefaultSoftwarePreviewEngine : RealtimePreviewEngine {
    override fun createRenderEffect(model: LightroomDevelopModel): RenderEffect? = null

    override suspend fun renderPreviewBitmap(source: Bitmap, model: LightroomDevelopModel): Bitmap {
        // Transparent passthrough or software fallback
        return source
    }
}
