package com.stealzalut.engine.color

import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow

/**
 * Standard D65 sRGB to CIE L*a*b* conversions (IEC 61966-2-1).
 * Pure Kotlin JVM implementation matching stealzalut.core.color_transfer.
 */
object CieLab {
    // Standard D65 sRGB to XYZ transformation matrix (IEC 61966-2-1)
    val M_SRGB_TO_XYZ = floatArrayOf(
        0.4124564f, 0.3575761f, 0.1804375f,
        0.2126729f, 0.7151522f, 0.0721750f,
        0.0193339f, 0.1191920f, 0.9503041f
    )

    // Standard XYZ to sRGB inverse matrix
    val M_XYZ_TO_SRGB = floatArrayOf(
        3.2404542f, -1.5371385f, -0.4985314f,
        -0.9692660f, 1.8760108f, 0.0415560f,
        0.0556434f, -0.2040259f, 1.0572252f
    )

    // CIE 1931 2° D65 standard illuminant reference white
    val D65_WHITE = floatArrayOf(0.95047f, 1.00000f, 1.08883f)

    const val CIE_DELTA = 6.0f / 29.0f
    const val CIE_DELTA_CUBED = CIE_DELTA * CIE_DELTA * CIE_DELTA
    const val CIE_3_DELTA_SQUARED = 3.0f * (CIE_DELTA * CIE_DELTA)

    fun rgbToLab(r: Float, g: Float, b: Float, outLab: FloatArray, outOffset: Int = 0) {
        val rClip = r.coerceIn(0.0f, 1.0f)
        val gClip = g.coerceIn(0.0f, 1.0f)
        val bClip = b.coerceIn(0.0f, 1.0f)

        // Linearize sRGB gamma
        val rLin = if (rClip > 0.04045f) ((rClip + 0.055f) / 1.055f).pow(2.4f) else (rClip / 12.92f)
        val gLin = if (gClip > 0.04045f) ((gClip + 0.055f) / 1.055f).pow(2.4f) else (gClip / 12.92f)
        val bLin = if (bClip > 0.04045f) ((bClip + 0.055f) / 1.055f).pow(2.4f) else (bClip / 12.92f)

        // sRGB -> XYZ
        val x = rLin * M_SRGB_TO_XYZ[0] + gLin * M_SRGB_TO_XYZ[1] + bLin * M_SRGB_TO_XYZ[2]
        val y = rLin * M_SRGB_TO_XYZ[3] + gLin * M_SRGB_TO_XYZ[4] + bLin * M_SRGB_TO_XYZ[5]
        val z = rLin * M_SRGB_TO_XYZ[6] + gLin * M_SRGB_TO_XYZ[7] + bLin * M_SRGB_TO_XYZ[8]

        val xn = x / D65_WHITE[0]
        val yn = y / D65_WHITE[1]
        val zn = z / D65_WHITE[2]

        val fx = if (xn > CIE_DELTA_CUBED) cbrt(xn) else (xn / CIE_3_DELTA_SQUARED + 4.0f / 29.0f)
        val fy = if (yn > CIE_DELTA_CUBED) cbrt(yn) else (yn / CIE_3_DELTA_SQUARED + 4.0f / 29.0f)
        val fz = if (zn > CIE_DELTA_CUBED) cbrt(zn) else (zn / CIE_3_DELTA_SQUARED + 4.0f / 29.0f)

        outLab[outOffset] = 116.0f * fy - 16.0f
        outLab[outOffset + 1] = 500.0f * (fx - fy)
        outLab[outOffset + 2] = 200.0f * (fy - fz)
    }

    fun labToRgb(l: Float, a: Float, b: Float, outRgb: FloatArray, outOffset: Int = 0) {
        val fy = (l + 16.0f) / 116.0f
        val fx = fy + a / 500.0f
        val fz = fy - b / 200.0f

        val xn = if (fx > CIE_DELTA) fx * fx * fx else CIE_3_DELTA_SQUARED * (fx - 4.0f / 29.0f)
        val yn = if (fy > CIE_DELTA) fy * fy * fy else CIE_3_DELTA_SQUARED * (fy - 4.0f / 29.0f)
        val zn = if (fz > CIE_DELTA) fz * fz * fz else CIE_3_DELTA_SQUARED * (fz - 4.0f / 29.0f)

        val x = xn * D65_WHITE[0]
        val y = yn * D65_WHITE[1]
        val z = zn * D65_WHITE[2]

        // XYZ -> Linear sRGB
        val rLin = max(0.0f, x * M_XYZ_TO_SRGB[0] + y * M_XYZ_TO_SRGB[1] + z * M_XYZ_TO_SRGB[2])
        val gLin = max(0.0f, x * M_XYZ_TO_SRGB[3] + y * M_XYZ_TO_SRGB[4] + z * M_XYZ_TO_SRGB[5])
        val bLin = max(0.0f, x * M_XYZ_TO_SRGB[6] + y * M_XYZ_TO_SRGB[7] + z * M_XYZ_TO_SRGB[8])

        // Apply sRGB gamma
        val r = if (rLin > 0.0031308f) 1.055f * rLin.pow(1.0f / 2.4f) - 0.055f else 12.92f * rLin
        val g = if (gLin > 0.0031308f) 1.055f * gLin.pow(1.0f / 2.4f) - 0.055f else 12.92f * gLin
        val bOut = if (bLin > 0.0031308f) 1.055f * bLin.pow(1.0f / 2.4f) - 0.055f else 12.92f * bLin

        outRgb[outOffset] = r.coerceIn(0.0f, 1.0f)
        outRgb[outOffset + 1] = g.coerceIn(0.0f, 1.0f)
        outRgb[outOffset + 2] = bOut.coerceIn(0.0f, 1.0f)
    }

    fun rgbToLab(rgb: FloatArray, lab: FloatArray) {
        val n = rgb.size / 3
        for (i in 0 until n) {
            val idx = i * 3
            rgbToLab(rgb[idx], rgb[idx + 1], rgb[idx + 2], lab, idx)
        }
    }

    fun labToRgb(lab: FloatArray, rgb: FloatArray) {
        val n = lab.size / 3
        for (i in 0 until n) {
            val idx = i * 3
            labToRgb(lab[idx], lab[idx + 1], lab[idx + 2], rgb, idx)
        }
    }
}
