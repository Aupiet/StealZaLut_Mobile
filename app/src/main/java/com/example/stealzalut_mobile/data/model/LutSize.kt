package com.example.stealzalut_mobile.data.model

/**
 * 3D LUT lattice dimensions supported for Adobe .cube export.
 *
 * @property size Node count per RGB axis
 * @property label Display label in Compose UI
 * @property nodeCountText Formatted node count string
 */
enum class LutSize(
    val size: Int,
    val label: String,
    val nodeCountText: String
) {
    FAST(
        size = 17,
        label = "17³ Rapide",
        nodeCountText = "4 913 nœuds"
    ),
    STANDARD(
        size = 33,
        label = "33³ Standard",
        nodeCountText = "35 937 nœuds (Recommandé)"
    ),
    HIGH(
        size = 65,
        label = "65³ Précision",
        nodeCountText = "274 625 nœuds"
    );

    companion object {
        fun fromSize(size: Int): LutSize =
            entries.firstOrNull { it.size == size } ?: STANDARD
    }
}
