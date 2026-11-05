package com.stealzalut.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HISTOGRAM_BINS = 64

data class HistogramData(
    val redBins: FloatArray = FloatArray(HISTOGRAM_BINS),
    val greenBins: FloatArray = FloatArray(HISTOGRAM_BINS),
    val blueBins: FloatArray = FloatArray(HISTOGRAM_BINS)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return redBins.contentEquals(other.redBins) &&
                greenBins.contentEquals(other.greenBins) &&
                blueBins.contentEquals(other.blueBins)
    }

    override fun hashCode(): Int {
        var result = redBins.contentHashCode()
        result = 31 * result + greenBins.contentHashCode()
        result = 31 * result + blueBins.contentHashCode()
        return result
    }
}

/**
 * Real-time RGB Histogram component showing Red, Green, and Blue distribution.
 * Efficiently downsamples large preview bitmaps on Dispatchers.Default.
 */
@Composable
fun SzlRgbHistogram(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val histogramData = produceState<HistogramData?>(initialValue = null, bitmap) {
        if (bitmap == null || bitmap.isRecycled) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            computeHistogram(bitmap)
        }
    }

    val data = histogramData.value ?: return

    Box(
        modifier = modifier
            .width(AppDimens.HistogramWidth)
            .height(AppDimens.HistogramHeight)
            .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
            .background(ColorCardReference.NeutralDarkBackground.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH))
            .border(
                width = AppDimens.BorderThin,
                color = Color.White.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
            )
            .padding(AppDimens.SpacingMicro)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            fun drawChannel(bins: FloatArray, color: Color) {
                val path = Path()
                val stepX = width / (bins.size - 1)

                bins.forEachIndexed { index, normalizedVal ->
                    val x = index * stepX
                    val y = height - (normalizedVal * height)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = AppDimens.BorderThin.toPx())
                )
            }

            drawChannel(data.redBins, ColorCardReference.HistogramRed)
            drawChannel(data.greenBins, ColorCardReference.HistogramGreen)
            drawChannel(data.blueBins, ColorCardReference.HistogramBlue)
        }
    }
}

private fun computeHistogram(bitmap: Bitmap): HistogramData {
    val rBins = IntArray(HISTOGRAM_BINS)
    val gBins = IntArray(HISTOGRAM_BINS)
    val bBins = IntArray(HISTOGRAM_BINS)

    val width = bitmap.width
    val height = bitmap.height
    val step = maxOf(1, maxOf(width, height) / 160)

    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            val pixel = bitmap.getPixel(x, y)
            val r = (AndroidColor.red(pixel) * (HISTOGRAM_BINS - 1)) / 255
            val g = (AndroidColor.green(pixel) * (HISTOGRAM_BINS - 1)) / 255
            val b = (AndroidColor.blue(pixel) * (HISTOGRAM_BINS - 1)) / 255

            rBins[r]++
            gBins[g]++
            bBins[b]++
        }
    }

    val maxVal = maxOf(
        rBins.maxOrNull() ?: 1,
        gBins.maxOrNull() ?: 1,
        bBins.maxOrNull() ?: 1,
        1
    ).toFloat()

    val normR = FloatArray(HISTOGRAM_BINS) { rBins[it] / maxVal }
    val normG = FloatArray(HISTOGRAM_BINS) { gBins[it] / maxVal }
    val normB = FloatArray(HISTOGRAM_BINS) { bBins[it] / maxVal }

    return HistogramData(normR, normG, normB)
}
