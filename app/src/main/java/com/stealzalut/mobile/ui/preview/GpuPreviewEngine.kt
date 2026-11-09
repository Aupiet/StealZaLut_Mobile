package com.stealzalut.mobile.ui.preview

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Normalized parameter deltas between the live UI state and the last
 * Python-committed preview rendering.
 *
 * When [isZero] is true, the GPU shader operates as a mathematical identity,
 * displaying the reference Python rendering without any pixel alteration.
 */
data class GpuPreviewDeltas(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val temp: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f,
    val vignette: Float = 0f
) {
    val isZero: Boolean
        get() = abs(exposure) < 1e-4f &&
                abs(contrast) < 1e-4f &&
                abs(temp) < 1e-4f &&
                abs(tint) < 1e-4f &&
                abs(highlights) < 1e-4f &&
                abs(shadows) < 1e-4f &&
                abs(whites) < 1e-4f &&
                abs(blacks) < 1e-4f &&
                abs(vibrance) < 1e-4f &&
                abs(saturation) < 1e-4f &&
                abs(vignette) < 1e-4f
}

/**
 * Visual effects container for the real-time GPU preview tier.
 */
data class GpuPreviewState(
    val colorFilter: ColorFilter? = null,
    val renderEffect: androidx.compose.ui.graphics.RenderEffect? = null
)

/**
 * 2-Tier Real-Time Preview Engine.
 *
 * Tier 1 (GPU / < 16ms): Real-time AGSL RuntimeShader on Android 13+ (API 33+)
 * with seamless Compose ColorMatrix fallback on Android API 24-32.
 * Tier 2 (Reference / Python Chaquopy): Asynchronous background high-fidelity pass
 * triggered on 200ms debounce or gesture completion.
 */
object GpuPreviewEngine {

    /**
     * Android Graphics Shading Language (AGSL) program for real-time adjustments.
     * Evaluates exact mathematical counterparts to [lightroom_color.py] in < 1ms per frame.
     */
    val AGSL_SHADER_SRC: String = """
        uniform shader image;
        uniform float uDeltaExposure;
        uniform float uDeltaContrast;
        uniform float uDeltaTemp;
        uniform float uDeltaTint;
        uniform float uDeltaHighlights;
        uniform float uDeltaShadows;
        uniform float uDeltaWhites;
        uniform float uDeltaBlacks;
        uniform float uDeltaVibrance;
        uniform float uDeltaSaturation;
        uniform float uDeltaVignette;

        float srgbToLinear(float c) {
            c = clamp(c, 0.0, 1.0);
            return (c <= 0.04045) ? (c / 12.92) : pow((c + 0.055) / 1.055, 2.4);
        }
        float3 srgbToLinear3(float3 c) {
            return float3(srgbToLinear(c.r), srgbToLinear(c.g), srgbToLinear(c.b));
        }
        float linearToSrgb(float c) {
            c = max(c, 0.0);
            return (c <= 0.0031308) ? (c * 12.92) : (1.055 * pow(c, 1.0 / 2.4) - 0.055);
        }
        float3 linearToSrgb3(float3 c) {
            return float3(linearToSrgb(c.r), linearToSrgb(c.g), linearToSrgb(c.b));
        }

        float safeTanh(float x) {
            float ex = exp(clamp(x, -10.0, 10.0));
            float emx = exp(clamp(-x, -10.0, 10.0));
            return (ex - emx) / (ex + emx);
        }
        float3 safeTanh3(float3 v) {
            return float3(safeTanh(v.r), safeTanh(v.g), safeTanh(v.b));
        }

        half4 main(float2 fragCoord) {
            half4 color = image.eval(fragCoord);
            if (color.a == 0.0) {
                return color;
            }

            // Early-out if zero deltas (idle identity)
            if (abs(uDeltaExposure) < 0.0001 &&
                abs(uDeltaContrast) < 0.0001 &&
                abs(uDeltaTemp) < 0.0001 &&
                abs(uDeltaTint) < 0.0001 &&
                abs(uDeltaHighlights) < 0.0001 &&
                abs(uDeltaShadows) < 0.0001 &&
                abs(uDeltaWhites) < 0.0001 &&
                abs(uDeltaBlacks) < 0.0001 &&
                abs(uDeltaVibrance) < 0.0001 &&
                abs(uDeltaSaturation) < 0.0001) {
                return color;
            }

            float3 rgb = clamp(color.rgb, 0.0, 1.0);

            // 1. Exposure in linear EV space
            if (abs(uDeltaExposure) > 0.0001) {
                float3 lin = srgbToLinear3(rgb);
                lin *= exp2(uDeltaExposure);
                rgb = linearToSrgb3(lin);
                rgb = clamp(rgb, 0.0, 1.0);
            }

            // 2. White Balance (Temperature & Tint)
            if (abs(uDeltaTemp) > 0.0001 || abs(uDeltaTint) > 0.0001) {
                float t = uDeltaTemp / 100.0;
                float ti = uDeltaTint / 100.0;
                rgb.r *= (1.0 + 0.28 * t + 0.08 * ti);
                rgb.g *= (1.0 - 0.18 * ti);
                rgb.b *= (1.0 - 0.28 * t + 0.08 * ti);
                rgb = clamp(rgb, 0.0, 1.0);
            }

            // 3. Contrast (Sigmoidal tanh)
            if (abs(uDeltaContrast) > 0.0001) {
                float c_val = uDeltaContrast / 100.0;
                if (c_val > 0.0) {
                    float factor = 1.0 + c_val * 0.9;
                    float norm = 2.0 * safeTanh(0.75 * factor);
                    rgb = 0.5 + safeTanh3((rgb - 0.5) * 1.5 * factor) / norm;
                } else {
                    rgb = 0.5 + (rgb - 0.5) * (1.0 + c_val * 0.6);
                }
                rgb = clamp(rgb, 0.0, 1.0);
            }

            // 4. Highlights, Shadows, Whites, Blacks
            float luma = 0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b;
            if (abs(uDeltaHighlights) > 0.0001) {
                float h_weight = pow(smoothstep(0.35, 0.95, luma), 1.3);
                rgb += (uDeltaHighlights / 100.0) * 0.32 * h_weight * rgb;
                rgb = clamp(rgb, 0.0, 1.0);
            }
            if (abs(uDeltaShadows) > 0.0001) {
                float s_weight = pow(1.0 - smoothstep(0.05, 0.65, luma), 1.3);
                rgb += (uDeltaShadows / 100.0) * 0.30 * s_weight * (1.0 - rgb);
                rgb = clamp(rgb, 0.0, 1.0);
            }
            if (abs(uDeltaWhites) > 0.0001) {
                float w_weight = pow(smoothstep(0.7, 1.0, luma), 2.0);
                rgb += (uDeltaWhites / 100.0) * 0.25 * w_weight * (1.0 - rgb);
                rgb = clamp(rgb, 0.0, 1.0);
            }
            if (abs(uDeltaBlacks) > 0.0001) {
                float b_weight = pow(1.0 - smoothstep(0.0, 0.3, luma), 2.0);
                rgb += (uDeltaBlacks / 100.0) * 0.25 * b_weight * rgb;
                rgb = clamp(rgb, 0.0, 1.0);
            }

            // 5. Vibrance & Saturation
            if (abs(uDeltaVibrance) > 0.0001 || abs(uDeltaSaturation) > 0.0001) {
                float max_c = max(max(rgb.r, rgb.g), rgb.b);
                float min_c = min(min(rgb.r, rgb.g), rgb.b);
                float delta = max_c - min_c;
                float sat = (max_c > 0.00001) ? (delta / max_c) : 0.0;
                float lum = 0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b;

                float v_boost = (uDeltaVibrance / 100.0) * (1.0 - sat) * 0.65;
                float s_boost = (uDeltaSaturation / 100.0) * 0.55;
                float total_boost = 1.0 + v_boost + s_boost;

                rgb = lum + (rgb - lum) * total_boost;
                rgb = clamp(rgb, 0.0, 1.0);
            }

            return half4(rgb, color.a);
        }
    """.trimIndent()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun updateShaderUniforms(shader: RuntimeShader, deltas: GpuPreviewDeltas) {
        shader.setFloatUniform("uDeltaExposure", deltas.exposure)
        shader.setFloatUniform("uDeltaContrast", deltas.contrast)
        shader.setFloatUniform("uDeltaTemp", deltas.temp)
        shader.setFloatUniform("uDeltaTint", deltas.tint)
        shader.setFloatUniform("uDeltaHighlights", deltas.highlights)
        shader.setFloatUniform("uDeltaShadows", deltas.shadows)
        shader.setFloatUniform("uDeltaWhites", deltas.whites)
        shader.setFloatUniform("uDeltaBlacks", deltas.blacks)
        shader.setFloatUniform("uDeltaVibrance", deltas.vibrance)
        shader.setFloatUniform("uDeltaSaturation", deltas.saturation)
        shader.setFloatUniform("uDeltaVignette", deltas.vignette)
    }

    /**
     * Builds a hardware ColorMatrix for devices running Android API < 33.
     */
    fun createFallbackColorMatrix(deltas: GpuPreviewDeltas): ColorMatrix {
        val matrix = ColorMatrix()
        if (deltas.isZero) return matrix

        // 1. Exposure EV scale
        val expScale = 2.0.pow(deltas.exposure.toDouble()).toFloat()

        // 2. White balance
        val t = deltas.temp / 100f
        val ti = deltas.tint / 100f
        val rScale = expScale * (1f + 0.28f * t + 0.08f * ti)
        val gScale = expScale * (1f - 0.18f * ti)
        val bScale = expScale * (1f - 0.28f * t + 0.08f * ti)

        // 3. Contrast scale & offset
        val cVal = deltas.contrast / 100f
        val cScale = if (cVal >= 0f) 1f + cVal * 0.9f else 1f + cVal * 0.6f
        val cOffset = (0.5f * (1f - cScale)) * 255f

        // 4. Saturation
        val satBoost = 1f + (deltas.saturation / 100f) * 0.55f + (deltas.vibrance / 100f) * 0.40f

        val satMatrix = ColorMatrix().apply { setToSaturation(satBoost) }
        val primaryMatrix = ColorMatrix(
            floatArrayOf(
                rScale * cScale, 0f, 0f, 0f, cOffset,
                0f, gScale * cScale, 0f, 0f, cOffset,
                0f, 0f, bScale * cScale, 0f, cOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        primaryMatrix.timesAssign(satMatrix)
        return primaryMatrix
    }

    /**
     * Exact JVM pixel evaluator matching the AGSL shader arithmetic.
     * Used for mathematical parity verification and performance benchmarking.
     */
    fun evaluatePixel(rgb: FloatArray, deltas: GpuPreviewDeltas): FloatArray {
        if (deltas.isZero) return rgb.clone()

        var r = rgb[0].coerceIn(0f, 1f)
        var g = rgb[1].coerceIn(0f, 1f)
        var b = rgb[2].coerceIn(0f, 1f)

        // 1. Exposure in linear space
        if (abs(deltas.exposure) > 1e-4f) {
            val linR = srgbToLinear(r) * 2.0.pow(deltas.exposure.toDouble()).toFloat()
            val linG = srgbToLinear(g) * 2.0.pow(deltas.exposure.toDouble()).toFloat()
            val linB = srgbToLinear(b) * 2.0.pow(deltas.exposure.toDouble()).toFloat()
            r = linearToSrgb(linR).coerceIn(0f, 1f)
            g = linearToSrgb(linG).coerceIn(0f, 1f)
            b = linearToSrgb(linB).coerceIn(0f, 1f)
        }

        // 2. White Balance
        if (abs(deltas.temp) > 1e-4f || abs(deltas.tint) > 1e-4f) {
            val t = deltas.temp / 100f
            val ti = deltas.tint / 100f
            r = (r * (1f + 0.28f * t + 0.08f * ti)).coerceIn(0f, 1f)
            g = (g * (1f - 0.18f * ti)).coerceIn(0f, 1f)
            b = (b * (1f - 0.28f * t + 0.08f * ti)).coerceIn(0f, 1f)
        }

        // 3. Contrast (Sigmoidal tanh)
        if (abs(deltas.contrast) > 1e-4f) {
            val cVal = deltas.contrast / 100f
            if (cVal > 0f) {
                val factor = 1f + cVal * 0.9f
                val norm = 2f * safeTanh(0.75f * factor)
                r = (0.5f + safeTanh((r - 0.5f) * 1.5f * factor) / norm).coerceIn(0f, 1f)
                g = (0.5f + safeTanh((g - 0.5f) * 1.5f * factor) / norm).coerceIn(0f, 1f)
                b = (0.5f + safeTanh((b - 0.5f) * 1.5f * factor) / norm).coerceIn(0f, 1f)
            } else {
                r = (0.5f + (r - 0.5f) * (1f + cVal * 0.6f)).coerceIn(0f, 1f)
                g = (0.5f + (g - 0.5f) * (1f + cVal * 0.6f)).coerceIn(0f, 1f)
                b = (0.5f + (b - 0.5f) * (1f + cVal * 0.6f)).coerceIn(0f, 1f)
            }
        }

        // 4. Highlights, Shadows, Whites, Blacks
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (abs(deltas.highlights) > 1e-4f) {
            val hWeight = smoothstep(0.35f, 0.95f, luma).pow(1.3f)
            val hBoost = (deltas.highlights / 100f) * 0.32f * hWeight
            r = (r + hBoost * r).coerceIn(0f, 1f)
            g = (g + hBoost * g).coerceIn(0f, 1f)
            b = (b + hBoost * b).coerceIn(0f, 1f)
        }
        if (abs(deltas.shadows) > 1e-4f) {
            val sWeight = (1f - smoothstep(0.05f, 0.65f, luma)).pow(1.3f)
            val sBoost = (deltas.shadows / 100f) * 0.30f * sWeight
            r = (r + sBoost * (1f - r)).coerceIn(0f, 1f)
            g = (g + sBoost * (1f - g)).coerceIn(0f, 1f)
            b = (b + sBoost * (1f - b)).coerceIn(0f, 1f)
        }
        if (abs(deltas.whites) > 1e-4f) {
            val wWeight = smoothstep(0.7f, 1.0f, luma).pow(2.0f)
            val wBoost = (deltas.whites / 100f) * 0.25f * wWeight
            r = (r + wBoost * (1f - r)).coerceIn(0f, 1f)
            g = (g + wBoost * (1f - g)).coerceIn(0f, 1f)
            b = (b + wBoost * (1f - b)).coerceIn(0f, 1f)
        }
        if (abs(deltas.blacks) > 1e-4f) {
            val bWeight = (1f - smoothstep(0.0f, 0.3f, luma)).pow(2.0f)
            val bBoost = (deltas.blacks / 100f) * 0.25f * bWeight
            r = (r + bBoost * r).coerceIn(0f, 1f)
            g = (g + bBoost * g).coerceIn(0f, 1f)
            b = (b + bBoost * b).coerceIn(0f, 1f)
        }

        // 5. Vibrance & Saturation
        if (abs(deltas.vibrance) > 1e-4f || abs(deltas.saturation) > 1e-4f) {
            val maxC = max(max(r, g), b)
            val minC = min(min(r, g), b)
            val delta = maxC - minC
            val sat = if (maxC > 1e-5f) delta / maxC else 0f
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            val vBoost = (deltas.vibrance / 100f) * (1f - sat) * 0.65f
            val sBoost = (deltas.saturation / 100f) * 0.55f
            val totalBoost = 1f + vBoost + sBoost

            r = (lum + (r - lum) * totalBoost).coerceIn(0f, 1f)
            g = (lum + (g - lum) * totalBoost).coerceIn(0f, 1f)
            b = (lum + (b - lum) * totalBoost).coerceIn(0f, 1f)
        }

        return floatArrayOf(r, g, b)
    }

    private fun srgbToLinear(c: Float): Float {
        val clamped = c.coerceIn(0f, 1f)
        return if (clamped <= 0.04045f) clamped / 12.92f else ((clamped + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(c: Float): Float {
        val clamped = max(0f, c)
        return if (clamped <= 0.0031308f) clamped * 12.92f else 1.055f * clamped.pow(1f / 2.4f) - 0.055f
    }

    private fun safeTanh(x: Float): Float {
        val ex = exp(x.coerceIn(-10f, 10f))
        val emx = exp((-x).coerceIn(-10f, 10f))
        return (ex - emx) / (ex + emx)
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

/**
 * Remembers the GPU preview state for Compose rendering.
 */
@Composable
fun rememberGpuPreviewState(deltas: GpuPreviewDeltas): GpuPreviewState {
    if (deltas.isZero) {
        return remember { GpuPreviewState() }
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(GpuPreviewEngine.AGSL_SHADER_SRC) }
        remember(deltas) {
            GpuPreviewEngine.updateShaderUniforms(shader, deltas)
            val effect = RenderEffect.createRuntimeShaderEffect(shader, "image").asComposeRenderEffect()
            GpuPreviewState(renderEffect = effect)
        }
    } else {
        remember(deltas) {
            val cm = GpuPreviewEngine.createFallbackColorMatrix(deltas)
            GpuPreviewState(colorFilter = ColorFilter.colorMatrix(cm))
        }
    }
}
