package com.stealzalut.mobile.data.model

data class ZoneDescriptor(
    val id: Int,
    val area: Float,
    val lRank: Float,
    val mean: List<Float>,
    val hex: String,
    val centroidXY: List<Float>,
    val bbox: List<Float>
)

data class ZonePaletteAnalysis(
    val zoneCount: Int,
    val zones: List<ZoneDescriptor>,
    val labelsShape: List<Int>
)

data class ZoneMatchWeight(
    val sourceZoneId: Int,
    val referenceZoneId: Int,
    val weight: Float
)

data class ZoneMatching(
    val matching: Map<String, Map<String, Float>>
)

/** Slim per-zone descriptor embedded in [ZoneTransferSummary]. */
data class ZoneInfo(
    val id: Int,
    /** Mean colour as #RRGGBB hex string. */
    val hex: String,
    /** Fraction of image pixels in [0, 1]. */
    val area: Float,
    /** Lightness rank in [0, 1]. */
    val lRank: Float,
    /** Mean colour in Oklab [L, a, b]. */
    val meanOklab: List<Float>,
    val name: String = "",
    val pixelPercentage: Float = 0f,
    val dominantHue: String = "",
    val describeZone: String = ""
)

/**
 * Correspondence link from one source zone to its target reference zone(s).
 *
 * @property origin "auto" | "user" | "ai" — who set this link.
 * @property isOverridden True when the user has manually re-wired this link.
 * @property primaryTarget The reference-zone id with the highest weight, or -1 if no targets.
 */
data class ZoneLink(
    val sourceZone: Int,
    val targets: Map<String, Float>,
    val strength: Float,
    val locked: Boolean,
    val origin: String = "auto",
    val isOverridden: Boolean = false,
    val primaryTarget: Int = -1,
    val similarity: Float = 1.0f,
    val describeChange: String = ""
)

/**
 * Full result of [PythonBridgeRepository.fitZoneTransfer].
 *
 * @property userOverrides Current user-driven source→reference rewiring,
 *   used to repopulate the UI and to send back on the next recalculate call.
 */
data class ZoneTransferSummary(
    val sourceZoneIds: List<Int>,
    val referenceZoneIds: List<Int>,
    val analysisId: String,
    val method: String,
    val correspondence: List<ZoneLink>,
    val sourceZones: List<ZoneInfo> = emptyList(),
    val referenceZones: List<ZoneInfo> = emptyList(),
    val userOverrides: Map<String, Int> = emptyMap()
)

/** Result of [PythonBridgeRepository.getZoneMask]. */
data class ZoneMaskResult(
    val maskPath: String,
    val zoneId: Int,
    val zoneIndex: Int,
    val mode: String,
    val area: Float,
    val hex: String
)

data class RecipePayload(
    val analysisId: String?,
    val mode: String?,
    val zoneCount: Int?,
    val recipe: Map<String, Any>,
    val json: String
)

data class AnalysisPackExport(
    val packDir: String,
    val packName: String,
    val analysisId: String,
    val files: List<String>,
    val prompt: String
)
