package com.stealzalut.engine.zones

data class Intent(
    val lightness: String = "0",
    val saturation: String = "0",
    val warmth: String = "0",
    val tint: String = "0"
) {
    val isNeutral: Boolean get() = lightness == "0" && saturation == "0" && warmth == "0" && tint == "0"
}

data class Link(
    val sourceZone: Int,
    val targets: Map<Int, Float> = emptyMap(),
    val strength: Float = 1.0f,
    val luminanceStrength: Float? = null,
    val chromaStrength: Float? = null,
    val locked: Boolean = false,
    val intent: Intent = Intent(),
    val origin: String = "auto",
    val rationale: String = ""
) {
    val isIdentity: Boolean get() = locked
    val keepsColours: Boolean get() = locked || (targets.isEmpty() || strength == 0.0f) && intent.isNeutral
}

data class CorrespondenceTable(
    val links: List<Link>
) {
    fun link(sourceZone: Int): Link {
        val l = links.find { it.sourceZone == sourceZone }
        return l ?: throw NoSuchElementException("No link for source zone $sourceZone")
    }

    fun withLink(newLink: Link): CorrespondenceTable {
        return copy(links = links.map { if (it.sourceZone == newLink.sourceZone) newLink else it })
    }

    val hasManualLinks: Boolean get() = links.any { it.origin != "auto" }

    companion object {
        fun fromMatching(matching: Map<Int, Map<Int, Float>>): CorrespondenceTable {
            val list = matching.keys.sorted().map { zId ->
                Link(sourceZone = zId, targets = matching[zId] ?: emptyMap(), origin = "auto")
            }
            return CorrespondenceTable(list)
        }
    }
}

fun retarget(
    table: CorrespondenceTable,
    sourceZone: Int,
    referenceZone: Int,
    add: Boolean = false,
    origin: String = "user"
): CorrespondenceTable {
    val link = table.link(sourceZone)
    val targets = if (add && link.targets.isNotEmpty()) {
        if (link.targets.containsKey(referenceZone)) return table
        val share = 1.0f / (link.targets.size + 1)
        val m = link.targets.mapValues { it.value * (1.0f - share) }.toMutableMap()
        m[referenceZone] = share
        m
    } else {
        mapOf(referenceZone to 1.0f)
    }
    return table.withLink(link.copy(targets = targets, locked = false, origin = origin, rationale = ""))
}
