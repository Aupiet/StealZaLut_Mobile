package com.stealzalut.mobile.data.model

import androidx.annotation.StringRes
import com.stealzalut.mobile.R

/**
 * Supported color-transfer algorithms in StealZaLUT with localized string resources
 * for professional colorists and standard users alike.
 *
 * @property key Python bridge identifier
 * @property displayNameRes Resource ID for user-friendly name
 * @property technicalSubtitleRes Resource ID for technical algorithm subtitle
 * @property descriptionRes Resource ID for algorithm explanatory text
 */
enum class ColorTransferMethod(
    val key: String,
    @StringRes val displayNameRes: Int,
    @StringRes val technicalSubtitleRes: Int,
    @StringRes val descriptionRes: Int
) {
    HM_MKL_HM(
        key = "hm-mkl-hm",
        displayNameRes = R.string.method_hm_mkl_hm_name,
        technicalSubtitleRes = R.string.method_hm_mkl_hm_sub,
        descriptionRes = R.string.method_hm_mkl_hm_desc
    ),
    MKL(
        key = "mkl",
        displayNameRes = R.string.method_mkl_name,
        technicalSubtitleRes = R.string.method_mkl_sub,
        descriptionRes = R.string.method_mkl_desc
    ),
    HM(
        key = "hm",
        displayNameRes = R.string.method_hm_name,
        technicalSubtitleRes = R.string.method_hm_sub,
        descriptionRes = R.string.method_hm_desc
    ),
    REINHARD(
        key = "reinhard",
        displayNameRes = R.string.method_reinhard_name,
        technicalSubtitleRes = R.string.method_reinhard_sub,
        descriptionRes = R.string.method_reinhard_desc
    );

    companion object {
        fun fromKey(key: String): ColorTransferMethod =
            entries.firstOrNull { it.key == key } ?: HM_MKL_HM
    }
}
