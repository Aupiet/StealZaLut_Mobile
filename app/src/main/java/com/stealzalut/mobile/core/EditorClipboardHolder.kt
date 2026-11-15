package com.stealzalut.mobile.core

import com.stealzalut.mobile.data.model.LightroomDevelopModel

/**
 * In-memory clipboard holder for Lightroom editing parameters.
 * Enables seamless copy-pasting of color grading and tonal parameters across photos.
 */
object EditorClipboardHolder {
    @Volatile
    private var clipboardParams: LightroomDevelopModel? = null

    fun copy(params: LightroomDevelopModel) {
        clipboardParams = params.copy()
    }

    fun paste(): LightroomDevelopModel? = clipboardParams?.copy()

    fun hasCopied(): Boolean = clipboardParams != null

    fun clear() {
        clipboardParams = null
    }
}
