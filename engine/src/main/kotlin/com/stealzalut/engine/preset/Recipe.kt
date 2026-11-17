package com.stealzalut.engine.preset

const val RECIPE_FORMAT = "stealzalut.recipe"
const val RECIPE_VERSION = 2

data class RecipeLink(
    val sourceZone: Int,
    val targets: Map<Int, Float>? = null,
    val strength: Float? = null,
    val luminanceStrength: Float? = null,
    val chromaStrength: Float? = null,
    val keep: Boolean = false,
    val intent: Map<String, String>? = null,
    val rationale: String = "",
    val origin: String? = null
)

data class Recipe(
    val analysisId: String? = null,
    val name: String = "",
    val description: String = "",
    val grading: Map<String, Float> = emptyMap(),
    val tau: Float? = null,
    val zoneCount: Int? = null,
    val links: List<RecipeLink> = emptyList(),
    val mode: String? = null
) {
    fun toMap(): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>()
        payload["format"] = RECIPE_FORMAT
        payload["version"] = RECIPE_VERSION
        if (analysisId != null) payload["analysis_id"] = analysisId
        if (name.isNotEmpty()) payload["name"] = name
        if (description.isNotEmpty()) payload["description"] = description
        if (grading.isNotEmpty()) payload["global"] = grading
        if (mode != null) payload["mode"] = mode
        if (zoneCount != null) payload["zone_count"] = zoneCount
        if (links.isNotEmpty()) {
            payload["links"] = links.map { link ->
                val lMap = mutableMapOf<String, Any?>()
                lMap["source_zone"] = link.sourceZone
                if (link.targets != null && link.targets.isNotEmpty()) {
                    val tMap = mutableMapOf<String, Float>()
                    for ((k, v) in link.targets) tMap[k.toString()] = v
                    lMap["targets"] = tMap
                }
                if (link.strength != null) lMap["strength"] = link.strength
                if (link.luminanceStrength != null) lMap["luminance_strength"] = link.luminanceStrength
                if (link.chromaStrength != null) lMap["chroma_strength"] = link.chromaStrength
                if (link.keep) lMap["keep"] = true
                if (link.intent != null && link.intent.isNotEmpty()) lMap["intent"] = link.intent
                if (link.rationale.isNotEmpty()) lMap["rationale"] = link.rationale
                if (link.origin != null) lMap["origin"] = link.origin
                lMap
            }
        }
        return payload
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any?>): Recipe {
            val format = data["format"] as? String ?: RECIPE_FORMAT
            require(format == RECIPE_FORMAT) { "Unsupported recipe format: $format" }

            val analysisId = data["analysis_id"] as? String
            val name = data["name"] as? String ?: ""
            val description = data["description"] as? String ?: ""
            val mode = data["mode"] as? String
            val zoneCount = (data["zone_count"] as? Number)?.toInt()
            val tau = (data["tau"] as? Number)?.toFloat()

            val grading = mutableMapOf<String, Float>()
            val globalRaw = data["global"] as? Map<String, Any?>
            if (globalRaw != null) {
                for ((k, v) in globalRaw) {
                    if (v is Number) grading[k] = v.toFloat()
                }
            }

            val linksList = mutableListOf<RecipeLink>()
            val linksRaw = data["links"] as? List<Map<String, Any?>>
            if (linksRaw != null) {
                for (l in linksRaw) {
                    val sz = (l["source_zone"] as? Number)?.toInt() ?: 1
                    val targetsRaw = l["targets"] as? Map<String, Any?>
                    val targets = targetsRaw?.mapNotNull { (k, v) ->
                        val kInt = k.toIntOrNull()
                        val vF = (v as? Number)?.toFloat()
                        if (kInt != null && vF != null) Pair(kInt, vF) else null
                    }?.toMap()

                    val strength = (l["strength"] as? Number)?.toFloat()
                    val lumStrength = (l["luminance_strength"] as? Number)?.toFloat()
                    val chromaStrength = (l["chroma_strength"] as? Number)?.toFloat()
                    val keep = l["keep"] as? Boolean ?: false
                    val intent = l["intent"] as? Map<String, String>
                    val rationale = l["rationale"] as? String ?: ""
                    val origin = l["origin"] as? String

                    linksList.add(
                        RecipeLink(
                            sourceZone = sz,
                            targets = targets,
                            strength = strength,
                            luminanceStrength = lumStrength,
                            chromaStrength = chromaStrength,
                            keep = keep,
                            intent = intent,
                            rationale = rationale,
                            origin = origin
                        )
                    )
                }
            }

            return Recipe(
                analysisId = analysisId,
                name = name,
                description = description,
                grading = grading,
                tau = tau,
                zoneCount = zoneCount,
                links = linksList,
                mode = mode
            )
        }

        fun parse(jsonStr: String): Recipe {
            @Suppress("UNCHECKED_CAST")
            val parsed = SimpleJson.parse(jsonStr) as? Map<String, Any?>
                ?: throw IllegalArgumentException("Invalid recipe JSON")
            return fromMap(parsed)
        }

        fun dump(recipe: Recipe): String {
            return SimpleJson.serialize(recipe.toMap(), indent = 2)
        }
    }
}
