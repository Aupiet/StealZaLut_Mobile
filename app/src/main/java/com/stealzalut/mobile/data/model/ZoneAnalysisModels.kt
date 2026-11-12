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

data class ZoneLink(
    val sourceZone: Int,
    val targets: Map<String, Float>,
    val strength: Float,
    val locked: Boolean
)

data class ZoneTransferSummary(
    val sourceZoneIds: List<Int>,
    val referenceZoneIds: List<Int>,
    val analysisId: String,
    val method: String,
    val correspondence: List<ZoneLink>
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
