package com.stealzalut.mobile.ui.screens.guide

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.DoubleViewComparison

/**
 * Interactive split before/after tactile slider demonstrating photographic transformations.
 * Reuses the battle-tested Studio [DoubleViewComparison] component with lightweight cached bitmaps.
 */
@Composable
fun InteractiveBeforeAfterSlider(
    toolId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val beforeBitmap = remember(toolId) { GuideImageProvider.getBeforeBitmap(context, toolId) }
    val afterBitmap = remember(toolId) { GuideImageProvider.getAfterBitmap(context, toolId) }

    DoubleViewComparison(
        sourceBitmap = beforeBitmap,
        previewBitmap = afterBitmap,
        height = AppDimens.DoubleViewHeight,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun InteractiveBeforeAfterSlider(
    demoType: FeatureDemoType,
    modifier: Modifier = Modifier
) {
    val toolId = remember(demoType) {
        when (demoType) {
            FeatureDemoType.CROP -> "crop"
            FeatureDemoType.TONE_CURVE -> "curves"
            FeatureDemoType.COLOR_MIXER -> "mixer"
            FeatureDemoType.COLOR_GRADING -> "grading"
            FeatureDemoType.CALIBRATION -> "calibration"
            FeatureDemoType.DETAIL -> "detail"
            FeatureDemoType.LENS_CORRECTIONS -> "lens"
            FeatureDemoType.TRANSFORM -> "transform"
            FeatureDemoType.LENS_BLUR -> "blur"
            FeatureDemoType.EFFECTS -> "effects"
            FeatureDemoType.LUT_EXPORT -> "lut"
            FeatureDemoType.STYLE_TRANSFER -> "transfer"
        }
    }
    InteractiveBeforeAfterSlider(toolId = toolId, modifier = modifier)
}
