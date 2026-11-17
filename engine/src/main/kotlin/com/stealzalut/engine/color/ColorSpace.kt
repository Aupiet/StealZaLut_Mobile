package com.stealzalut.engine.color

import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorSpace {

    const val EPSILON: Float = 1e-6f

    // sRGB to Linear
    fun srgbToLinear(x: Float): Float {
        val clamped = x.coerceIn(0.0f, 1.0f)
        return if (clamped <= 0.04045f) {
            clamped / 12.92f
        } else {
            ((clamped + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    // Linear to sRGB
    fun linearToSrgb(x: Float): Float {
        val clamped = max(0.0f, x)
        return if (clamped <= 0.0031308f) {
            clamped * 12.92f
        } else {
            1.055f * clamped.pow(1.0f / 2.4f) - 0.055f
        }
    }

    // Hermite smoothstep
    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val diff = max(edge1 - edge0, EPSILON)
        val t = ((x - edge0) / diff).coerceIn(0.0f, 1.0f)
        return t * t * (3.0f - 2.0f * t)
    }

    // OKLab LMS forward matrices
    // M_SRGB_TO_LMS: [ [0.4122214708, 0.5363325363, 0.0514459929],
    //                  [0.2119034982, 0.6806995451, 0.1073969566],
    //                  [0.0883024619, 0.2817188376, 0.6299787005] ]
    // M_LMS_TO_OKLAB: [ [0.2104542553, 0.7936177850, -0.0040720468],
    //                   [1.9779984951, -2.4285922050, 0.4505937099],
    //                   [0.0259040371, 0.7827717662, -0.8086757660] ]

    fun rgbToOklab(r: Float, g: Float, b: Float, out: FloatArray, outOffset: Int = 0) {
        val linR = srgbToLinear(r)
        val linG = srgbToLinear(g)
        val linB = srgbToLinear(b)

        val l0 = 0.4122214708f * linR + 0.5363325363f * linG + 0.0514459929f * linB
        val m0 = 0.2119034982f * linR + 0.6806995451f * linG + 0.1073969566f * linB
        val s0 = 0.0883024619f * linR + 0.2817188376f * linG + 0.6299787005f * linB

        val lCbrt = cbrt(max(0.0f, l0))
        val mCbrt = cbrt(max(0.0f, m0))
        val sCbrt = cbrt(max(0.0f, s0))

        val lOut = 0.2104542553f * lCbrt + 0.7936177850f * mCbrt - 0.0040720468f * sCbrt
        val aOut = 1.9779984951f * lCbrt - 2.4285922050f * mCbrt + 0.4505937099f * sCbrt
        val bOut = 0.0259040371f * lCbrt + 0.7827717662f * mCbrt - 0.8086757660f * sCbrt

        out[outOffset] = lOut
        out[outOffset + 1] = aOut
        out[outOffset + 2] = bOut
    }

    // M_OKLAB_TO_LMS: [ [1.0, +0.3963377774, +0.2158037573],
    //                   [1.0, -0.1055613458, -0.0638541728],
    //                   [1.0, -0.0894841775, -1.2914855480] ]
    // M_LMS_TO_SRGB: [ [+4.0767439362, -3.3077115913, +0.2309699294],
    //                  [-1.2684380046, +2.6097574011, -0.3413193965],
    //                  [-0.0041960863, -0.7034186147, +1.7076147010] ]

    fun oklabToRgb(l: Float, a: Float, b: Float, out: FloatArray, outOffset: Int = 0) {
        val lCbrt = l + 0.3963377774f * a + 0.2158037573f * b
        val mCbrt = l - 0.1055613458f * a - 0.0638541728f * b
        val sCbrt = l - 0.0894841775f * a - 1.2914855480f * b

        val l0 = lCbrt * lCbrt * lCbrt
        val m0 = mCbrt * mCbrt * mCbrt
        val s0 = sCbrt * sCbrt * sCbrt

        val linR = +4.0767439362f * l0 - 3.3077115913f * m0 + 0.2309699294f * s0
        val linG = -1.2684380046f * l0 + 2.6097574011f * m0 - 0.3413193965f * s0
        val linB = -0.0041960863f * l0 - 0.7034186147f * m0 + 1.7076147010f * s0

        out[outOffset] = linearToSrgb(linR).coerceIn(0.0f, 1.0f)
        out[outOffset + 1] = linearToSrgb(linG).coerceIn(0.0f, 1.0f)
        out[outOffset + 2] = linearToSrgb(linB).coerceIn(0.0f, 1.0f)
    }
}
