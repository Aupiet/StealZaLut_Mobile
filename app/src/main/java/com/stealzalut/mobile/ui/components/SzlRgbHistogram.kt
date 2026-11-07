package com.stealzalut.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HISTOGRAM_BINS = 64

data class HistogramData(
    val redBins: FloatArray = FloatArray(HISTOGRAM_BINS),
    val greenBins: FloatArray = FloatArray(HISTOGRAM_BINS),
    val blueBins: FloatArray = FloatArray(HISTOGRAM_BINS),
    val lumBins: FloatArray = FloatArray(HISTOGRAM_BINS)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return redBins.contentEquals(other.redBins) &&
                greenBins.contentEquals(other.greenBins) &&
                blueBins.contentEquals(other.blueBins) &&
                lumBins.contentEquals(other.lumBins)
    }

    override fun hashCode(): Int {
        var result = redBins.contentHashCode()
        result = 31 * result + greenBins.contentHashCode()
        result = 31 * result + blueBins.contentHashCode()
        result = 31 * result + lumBins.contentHashCode()
        return result
    }
}

/**
 * High-end Glassmorphism RGB Histogram.
 * Supports both a compact floating overlay pill and an enlarged HUD view with
 * channel spectrum curves, zone grid lines, and clipping alerts.
 */
@Composable
fun SzlRgbHistogram(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    isEnlarged: Boolean = false,
    onClick: (() -> Unit)? = null
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

    val targetWidth = if (isEnlarged) AppDimens.HistogramEnlargedWidth else AppDimens.HistogramOverlayWidth
    val targetHeight = if (isEnlarged) AppDimens.HistogramEnlargedHeight else AppDimens.HistogramOverlayHeight

    Box(
        modifier = modifier
            .width(targetWidth)
            .height(targetHeight)
            .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
            .background(ColorCardReference.NeutralDarkBackground.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH))
            .border(
                width = AppDimens.BorderThin,
                color = ColorCardReference.WhiteStep.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(AppDimens.SpacingMicro)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isEnlarged) {
                // Header with channel indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.editor_enlarged_histogram_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("L", ColorCardReference.WhiteStep, "Luma"),
                            Triple("R", ColorCardReference.HistogramRed, "Red"),
                            Triple("G", ColorCardReference.HistogramGreen, "Green"),
                            Triple("B", ColorCardReference.HistogramBlue, "Blue")
                        ).forEach { (label, dotColor, _) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(AppDimens.SpacingExtraSmall)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingMicro))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height

                    if (isEnlarged) {
                        // 25%, 50%, 75% dashed vertical grid lines
                        val gridLineColor = ColorCardReference.WhiteStep.copy(alpha = 0.12f)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (i in 1..3) {
                            val x = width * (i / 4.0f)
                            drawLine(
                                color = gridLineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f,
                                pathEffect = dashEffect
                            )
                        }
                    }

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
                            style = Stroke(width = if (isEnlarged) AppDimens.BorderMedium.toPx() else AppDimens.BorderThin.toPx())
                        )
                    }

                    drawChannel(data.lumBins, ColorCardReference.WhiteStep.copy(alpha = 0.45f))
                    drawChannel(data.redBins, ColorCardReference.HistogramRed)
                    drawChannel(data.greenBins, ColorCardReference.HistogramGreen)
                    drawChannel(data.blueBins, ColorCardReference.HistogramBlue)
                }
            }
        }
    }
}

private fun computeHistogram(bitmap: Bitmap): HistogramData {
    val rBins = IntArray(HISTOGRAM_BINS)
    val gBins = IntArray(HISTOGRAM_BINS)
    val bBins = IntArray(HISTOGRAM_BINS)
    val lBins = IntArray(HISTOGRAM_BINS)

    val width = bitmap.width
    val height = bitmap.height
    val step = maxOf(1, maxOf(width, height) / 160)

    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            val pixel = bitmap.getPixel(x, y)
            val rVal = AndroidColor.red(pixel)
            val gVal = AndroidColor.green(pixel)
            val bVal = AndroidColor.blue(pixel)
            val lVal = (0.2126f * rVal + 0.7152f * gVal + 0.0722f * bVal).toInt().coerceIn(0, 255)

            val r = (rVal * (HISTOGRAM_BINS - 1)) / 255
            val g = (gVal * (HISTOGRAM_BINS - 1)) / 255
            val b = (bVal * (HISTOGRAM_BINS - 1)) / 255
            val l = (lVal * (HISTOGRAM_BINS - 1)) / 255

            rBins[r]++
            gBins[g]++
            bBins[b]++
            lBins[l]++
        }
    }

    val maxVal = maxOf(
        rBins.maxOrNull() ?: 1,
        gBins.maxOrNull() ?: 1,
        bBins.maxOrNull() ?: 1,
        lBins.maxOrNull() ?: 1,
        1
    ).toFloat()

    val normR = FloatArray(HISTOGRAM_BINS) { rBins[it] / maxVal }
    val normG = FloatArray(HISTOGRAM_BINS) { gBins[it] / maxVal }
    val normB = FloatArray(HISTOGRAM_BINS) { bBins[it] / maxVal }
    val normL = FloatArray(HISTOGRAM_BINS) { lBins[it] / maxVal }

    return HistogramData(normR, normG, normB, normL)
}
