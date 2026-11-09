package com.stealzalut.mobile.ui.screens.guide

import com.stealzalut.mobile.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Central modular registry of all 12 photographic development and grading tools.
 */
object FeatureGuideRegistry {

    val items: ImmutableList<FeatureGuideItem> = listOf(
        // 1. Crop & Straighten
        FeatureGuideItem(
            id = "crop",
            titleRes = R.string.guide_crop_title,
            shortDescRes = R.string.guide_crop_desc,
            detailedExplanationRes = R.string.guide_crop_detail,
            category = FeatureCategory.GEOMETRY,
            iconProvider = { it.toolCrop },
            keyParameters = listOf(
                FeatureParam(R.string.guide_crop_p1_name, R.string.guide_crop_p1_desc),
                FeatureParam(R.string.guide_crop_p2_name, R.string.guide_crop_p2_desc)
            ),
            demoType = FeatureDemoType.CROP
        ),

        // 2. Tone Curves
        FeatureGuideItem(
            id = "curves",
            titleRes = R.string.guide_curves_title,
            shortDescRes = R.string.guide_curves_desc,
            detailedExplanationRes = R.string.guide_curves_detail,
            category = FeatureCategory.LIGHT,
            iconProvider = { it.toolToneCurve },
            keyParameters = listOf(
                FeatureParam(R.string.guide_curves_p1_name, R.string.guide_curves_p1_desc),
                FeatureParam(R.string.guide_curves_p2_name, R.string.guide_curves_p2_desc)
            ),
            demoType = FeatureDemoType.TONE_CURVE
        ),

        // 3. HSL Color Mixer
        FeatureGuideItem(
            id = "mixer",
            titleRes = R.string.guide_mixer_title,
            shortDescRes = R.string.guide_mixer_desc,
            detailedExplanationRes = R.string.guide_mixer_detail,
            category = FeatureCategory.COLOR,
            iconProvider = { it.toolColorMixer },
            keyParameters = listOf(
                FeatureParam(R.string.guide_mixer_p1_name, R.string.guide_mixer_p1_desc),
                FeatureParam(R.string.guide_mixer_p2_name, R.string.guide_mixer_p2_desc)
            ),
            demoType = FeatureDemoType.COLOR_MIXER
        ),

        // 4. Color Grading
        FeatureGuideItem(
            id = "grading",
            titleRes = R.string.guide_grading_title,
            shortDescRes = R.string.guide_grading_desc,
            detailedExplanationRes = R.string.guide_grading_detail,
            category = FeatureCategory.COLOR,
            iconProvider = { it.toolColorGrading },
            keyParameters = listOf(
                FeatureParam(R.string.guide_grading_p1_name, R.string.guide_grading_p1_desc),
                FeatureParam(R.string.guide_grading_p2_name, R.string.guide_grading_p2_desc)
            ),
            demoType = FeatureDemoType.COLOR_GRADING
        ),

        // 5. Camera Calibration
        FeatureGuideItem(
            id = "calibration",
            titleRes = R.string.guide_calibration_title,
            shortDescRes = R.string.guide_calibration_desc,
            detailedExplanationRes = R.string.guide_calibration_detail,
            category = FeatureCategory.COLOR,
            iconProvider = { it.toolCalibration },
            keyParameters = listOf(
                FeatureParam(R.string.guide_calibration_p1_name, R.string.guide_calibration_p1_desc),
                FeatureParam(R.string.guide_calibration_p2_name, R.string.guide_calibration_p2_desc)
            ),
            demoType = FeatureDemoType.CALIBRATION
        ),

        // 6. Detail & Sharpening
        FeatureGuideItem(
            id = "detail",
            titleRes = R.string.guide_detail_title,
            shortDescRes = R.string.guide_detail_desc,
            detailedExplanationRes = R.string.guide_detail_detail,
            category = FeatureCategory.LIGHT,
            iconProvider = { it.toolDetail },
            keyParameters = listOf(
                FeatureParam(R.string.guide_detail_p1_name, R.string.guide_detail_p1_desc),
                FeatureParam(R.string.guide_detail_p2_name, R.string.guide_detail_p2_desc)
            ),
            demoType = FeatureDemoType.DETAIL
        ),

        // 7. Lens Corrections
        FeatureGuideItem(
            id = "lens",
            titleRes = R.string.guide_lens_title,
            shortDescRes = R.string.guide_lens_desc,
            detailedExplanationRes = R.string.guide_lens_detail,
            category = FeatureCategory.GEOMETRY,
            iconProvider = { it.toolLens },
            keyParameters = listOf(
                FeatureParam(R.string.guide_lens_p1_name, R.string.guide_lens_p1_desc),
                FeatureParam(R.string.guide_lens_p2_name, R.string.guide_lens_p2_desc)
            ),
            demoType = FeatureDemoType.LENS_CORRECTIONS
        ),

        // 8. Geometric Transform
        FeatureGuideItem(
            id = "transform",
            titleRes = R.string.guide_transform_title,
            shortDescRes = R.string.guide_transform_desc,
            detailedExplanationRes = R.string.guide_transform_detail,
            category = FeatureCategory.GEOMETRY,
            iconProvider = { it.toolGeometry },
            keyParameters = listOf(
                FeatureParam(R.string.guide_transform_p1_name, R.string.guide_transform_p1_desc),
                FeatureParam(R.string.guide_transform_p2_name, R.string.guide_transform_p2_desc)
            ),
            demoType = FeatureDemoType.TRANSFORM
        ),

        // 9. Lens Blur (Bokeh)
        FeatureGuideItem(
            id = "blur",
            titleRes = R.string.guide_blur_title,
            shortDescRes = R.string.guide_blur_desc,
            detailedExplanationRes = R.string.guide_blur_detail,
            category = FeatureCategory.EFFECTS,
            iconProvider = { it.toolLens },
            keyParameters = listOf(
                FeatureParam(R.string.guide_blur_p1_name, R.string.guide_blur_p1_desc),
                FeatureParam(R.string.guide_blur_p2_name, R.string.guide_blur_p2_desc)
            ),
            demoType = FeatureDemoType.LENS_BLUR
        ),

        // 10. Creative Effects & Grain
        FeatureGuideItem(
            id = "effects",
            titleRes = R.string.guide_effects_title,
            shortDescRes = R.string.guide_effects_desc,
            detailedExplanationRes = R.string.guide_effects_detail,
            category = FeatureCategory.EFFECTS,
            iconProvider = { it.toolEffects },
            keyParameters = listOf(
                FeatureParam(R.string.guide_effects_p1_name, R.string.guide_effects_p1_desc),
                FeatureParam(R.string.guide_effects_p2_name, R.string.guide_effects_p2_desc)
            ),
            demoType = FeatureDemoType.EFFECTS
        ),

        // 11. 3D LUT Import & Sharing
        FeatureGuideItem(
            id = "lut",
            titleRes = R.string.guide_lut_title,
            shortDescRes = R.string.guide_lut_desc,
            detailedExplanationRes = R.string.guide_lut_detail,
            category = FeatureCategory.LUTS,
            iconProvider = { it.actionExport },
            keyParameters = listOf(
                FeatureParam(R.string.guide_lut_p1_name, R.string.guide_lut_p1_desc),
                FeatureParam(R.string.guide_lut_p2_name, R.string.guide_lut_p2_desc)
            ),
            demoType = FeatureDemoType.LUT_EXPORT
        ),

        // 12. AI Style Transfer
        FeatureGuideItem(
            id = "transfer",
            titleRes = R.string.guide_transfer_title,
            shortDescRes = R.string.guide_transfer_desc,
            detailedExplanationRes = R.string.guide_transfer_detail,
            category = FeatureCategory.LUTS,
            iconProvider = { it.actionAutoEdit },
            keyParameters = listOf(
                FeatureParam(R.string.guide_transfer_p1_name, R.string.guide_transfer_p1_desc),
                FeatureParam(R.string.guide_transfer_p2_name, R.string.guide_transfer_p2_desc)
            ),
            demoType = FeatureDemoType.STYLE_TRANSFER
        )
    ).toPersistentList()

    /**
     * Filters items by category and query string.
     */
    fun filter(
        category: FeatureCategory? = null,
        query: String = "",
        resolveString: (Int) -> String = { "" }
    ): List<FeatureGuideItem> {
        val trimmed = query.trim().lowercase()
        return items.filter { item ->
            val matchCategory = category == null || item.category == category
            val matchQuery = if (trimmed.isBlank()) {
                true
            } else {
                val title = resolveString(item.titleRes).lowercase()
                val desc = resolveString(item.shortDescRes).lowercase()
                title.contains(trimmed) || desc.contains(trimmed) || item.id.contains(trimmed)
            }
            matchCategory && matchQuery
        }
    }

    fun getById(id: String): FeatureGuideItem? = items.firstOrNull { it.id == id }
}
