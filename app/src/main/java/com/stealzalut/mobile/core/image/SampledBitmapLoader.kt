package com.stealzalut.mobile.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object SampledBitmapLoader {
    fun decodeFile(file: File, maxDimension: Int): Bitmap? {
        require(maxDimension > 0) { "Maximum bitmap dimension must be positive." }
        if (!file.isFile) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    internal fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        require(width > 0 && height > 0) { "Bitmap dimensions must be positive." }
        require(maxDimension > 0) { "Maximum bitmap dimension must be positive." }

        val sourceMaxDimension = maxOf(width, height)
        var sampleSize = 1
        while (sourceMaxDimension / sampleSize > maxDimension && sampleSize <= Int.MAX_VALUE / 2) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
