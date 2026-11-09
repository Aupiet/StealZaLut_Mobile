package com.stealzalut.mobile.ui.screens.guide

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.stealzalut.mobile.R
import com.stealzalut.mobile.ui.theme.SzlIconSet

/**
 * Functional categories for tools in the Feature Guide.
 */
enum class FeatureCategory(
    @StringRes val labelRes: Int,
    val iconProvider: (SzlIconSet) -> ImageVector
) {
    LIGHT(R.string.guide_cat_light, { it.toolBasic }),
    COLOR(R.string.guide_cat_color, { it.toolColorGrading }),
    GEOMETRY(R.string.guide_cat_geometry, { it.toolCrop }),
    EFFECTS(R.string.guide_cat_effects, { it.toolEffects }),
    LUTS(R.string.guide_cat_luts, { it.actionAutoEdit })
}

/**
 * Interactive demonstration type for each tool card.
 */
enum class FeatureDemoType {
    CROP,
    TONE_CURVE,
    COLOR_MIXER,
    COLOR_GRADING,
    CALIBRATION,
    DETAIL,
    LENS_CORRECTIONS,
    TRANSFORM,
    LENS_BLUR,
    EFFECTS,
    LUT_EXPORT,
    STYLE_TRANSFER
}

/**
 * A single key parameter entry for a tool.
 */
data class FeatureParam(
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int
)

/**
 * Metadata and content for an individual tool in the Feature Guide.
 */
data class FeatureGuideItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val shortDescRes: Int,
    @StringRes val detailedExplanationRes: Int,
    val category: FeatureCategory,
    val iconProvider: (SzlIconSet) -> ImageVector,
    val keyParameters: List<FeatureParam>,
    val demoType: FeatureDemoType
)
