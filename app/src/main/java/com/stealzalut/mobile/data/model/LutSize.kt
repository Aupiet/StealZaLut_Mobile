package com.stealzalut.mobile.data.model

import androidx.annotation.StringRes
import com.stealzalut.mobile.R

/**
 * 3D LUT lattice dimensions supported for Adobe .cube export with localized string resources.
 *
 * @property size Node count per RGB axis
 * @property labelRes Display label in Compose UI
 * @property nodeCountRes Formatted node count string resource
 */
enum class LutSize(
    val size: Int,
    @StringRes val labelRes: Int,
    @StringRes val nodeCountRes: Int
) {
    FAST(
        size = 17,
        labelRes = R.string.lut_size_17_label,
        nodeCountRes = R.string.lut_size_17_nodes
    ),
    STANDARD(
        size = 33,
        labelRes = R.string.lut_size_33_label,
        nodeCountRes = R.string.lut_size_33_nodes
    ),
    HIGH(
        size = 65,
        labelRes = R.string.lut_size_65_label,
        nodeCountRes = R.string.lut_size_65_nodes
    );

    companion object {
        fun fromSize(size: Int): LutSize =
            entries.firstOrNull { it.size == size } ?: STANDARD
    }
}
