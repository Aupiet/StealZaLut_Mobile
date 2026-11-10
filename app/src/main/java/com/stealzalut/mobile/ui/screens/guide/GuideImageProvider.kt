package com.stealzalut.mobile.ui.screens.guide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.stealzalut.mobile.R
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance lightweight bitmap cache and provider for the Feature Guide.
 * Caches pre-rendered effect demonstrations so images are decoded only once and reused instantly.
 */
object GuideImageProvider {

    private val bitmapCache = ConcurrentHashMap<Int, Bitmap>()

    @DrawableRes
    fun getBeforeDrawableRes(toolId: String): Int {
        return R.drawable.guide_sample_depart
    }

    @DrawableRes
    fun getAfterDrawableRes(toolId: String): Int {
        return when (toolId) {
            "crop" -> R.drawable.guide_demo_crop
            "curves" -> R.drawable.guide_demo_curves
            "mixer" -> R.drawable.guide_demo_mixer
            "grading" -> R.drawable.guide_demo_grading
            "calibration" -> R.drawable.guide_demo_calibration
            "detail" -> R.drawable.guide_demo_detail
            "lens" -> R.drawable.guide_demo_optics
            "transform" -> R.drawable.guide_demo_geometry
            "blur" -> R.drawable.guide_demo_blur
            "effects" -> R.drawable.guide_demo_effects
            "lut" -> R.drawable.guide_demo_lut
            "transfer" -> R.drawable.guide_sample_graded
            "lut_algorithms" -> R.drawable.guide_sample_graded
            "lut_protections" -> R.drawable.guide_sample_graded
            "lut_lattice" -> R.drawable.guide_sample_graded
            else -> R.drawable.guide_demo_effects
        }
    }

    fun getBeforeBitmap(context: Context, toolId: String): Bitmap? {
        val resId = getBeforeDrawableRes(toolId)
        return loadCachedBitmap(context, resId)
    }

    fun getAfterBitmap(context: Context, toolId: String): Bitmap? {
        val resId = getAfterDrawableRes(toolId)
        return loadCachedBitmap(context, resId)
    }

    fun getReferenceBitmap(context: Context): Bitmap? {
        return loadCachedBitmap(context, R.drawable.guide_sample_reference)
    }

    private fun loadCachedBitmap(context: Context, @DrawableRes resId: Int): Bitmap? {
        val cached = bitmapCache[resId]
        if (cached != null && !cached.isRecycled) {
            return cached
        }
        return try {
            val decoded = BitmapFactory.decodeResource(context.resources, resId)
            if (decoded != null) {
                bitmapCache[resId] = decoded
            }
            decoded
        } catch (t: Throwable) {
            null
        }
    }

    fun clearCache() {
        bitmapCache.clear()
    }
}
