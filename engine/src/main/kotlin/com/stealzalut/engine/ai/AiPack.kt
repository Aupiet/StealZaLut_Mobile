package com.stealzalut.engine.ai

import com.stealzalut.engine.color.ColorSpace
import com.stealzalut.engine.preset.SimpleJson
import com.stealzalut.engine.zones.ZonePalette
import com.stealzalut.engine.zones.ZoneTransferModel
import kotlin.math.round

object AiPack {

    const val ANALYSIS_FORMAT = "stealzalut.analysis"
    const val ANALYSIS_VERSION = 1

    val PACK_FILES = listOf(
        "1_source.jpg",
        "2_reference.jpg",
        "3_source_zones.png",
        "4_reference_zones.png",
        "5_source_overview.png",
        "6_reference_overview.png",
        "analysis.json",
        "PROMPT.md"
    )

    private fun lightnessBand(lightness: Float): String {
        return if (lightness < 0.45f) "shadows" else if (lightness < 0.72f) "midtones" else "highlights"
    }

    private fun zonePayload(palette: ZonePalette): List<Map<String, Any?>> {
        val oklch = FloatArray(3)
        return palette.zones.map { z ->
            ColorSpace.oklabToOklch(z.mean[0], z.mean[1], z.mean[2], oklch)
            mapOf(
                "id" to z.id,
                "hex" to z.hex,
                "area_pct" to round(z.area * 1000.0f) / 10.0f,
                "oklch" to listOf(
                    round(oklch[0] * 1000.0f) / 1000.0f,
                    round(oklch[1] * 1000.0f) / 1000.0f,
                    round(oklch[2] * 10.0f) / 10.0f
                ),
                "lightness_band" to lightnessBand(z.mean[0]),
                "centroid_pct" to listOf(
                    round(z.centroidXy.first * 10.0f) / 10.0f,
                    round(z.centroidXy.second * 10.0f) / 10.0f
                ),
                "bbox_pct" to listOf(
                    round(z.bbox[0] * 10.0f) / 10.0f,
                    round(z.bbox[1] * 10.0f) / 10.0f,
                    round(z.bbox[2] * 10.0f) / 10.0f,
                    round(z.bbox[3] * 10.0f) / 10.0f
                )
            )
        }
    }

    fun suggestedMode(model: ZoneTransferModel): String {
        val stats = model.referenceColours
        return if (stats != null && stats.isMonochrome) "ambiance" else "zones"
    }

    fun analysisDict(
        model: ZoneTransferModel,
        srcW: Int,
        srcH: Int,
        refW: Int,
        refH: Int,
        zoneCount: Int
    ): Map<String, Any?> {
        val srcStats = model.sourceColours
        val refStats = model.referenceColours

        return mapOf(
            "format" to ANALYSIS_FORMAT,
            "version" to ANALYSIS_VERSION,
            "analysis_id" to model.analysisId,
            "zone_count" to zoneCount,
            "suggested_mode" to suggestedMode(model),
            "source" to mapOf(
                "size" to listOf(srcH, srcW),
                "hue_concentration" to (if (srcStats != null) round(srcStats.concentration * 100.0f) / 100.0f else 0f),
                "colourful_pct" to (if (srcStats != null) round(srcStats.colourful * 1000.0f) / 10.0f else 0f),
                "dominant_hue" to (if (srcStats != null) round(srcStats.hue * 10.0f) / 10.0f else 0f),
                "monochrome" to (srcStats?.isMonochrome ?: false),
                "zones" to zonePayload(model.source)
            ),
            "reference" to mapOf(
                "size" to listOf(refH, refW),
                "hue_concentration" to (if (refStats != null) round(refStats.concentration * 100.0f) / 100.0f else 0f),
                "colourful_pct" to (if (refStats != null) round(refStats.colourful * 1000.0f) / 10.0f else 0f),
                "dominant_hue" to (if (refStats != null) round(refStats.hue * 10.0f) / 10.0f else 0f),
                "monochrome" to (refStats?.isMonochrome ?: false),
                "zones" to zonePayload(model.reference)
            )
        )
    }

    fun buildPrompt(analysisId: String, srcZonesCount: Int, refZonesCount: Int): String {
        val srcStr = (1..srcZonesCount).joinToString(", ") { "S$it" }
        val refStr = (1..refZonesCount).joinToString(", ") { "R$it" }
        return """
You are grading the source image toward the reference style.

Analysis ID: $analysisId
Source zones: $srcStr
Reference zones: $refStr

Rules:
- Match each source zone Sx to one or more reference zones Rx.
- Keep any zone that is clearly equivalent locked when appropriate.
- MULTIPLIENT: the final dose is the product of all strengths; a full intensity is the default, and the usual value for every link is "full".
- Garde 1.0 par défaut pour luminance_strength et chroma_strength;
""".trimIndent()
    }
}
