package com.stealzalut.mobile.ui.screens.guide

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class GuideImageProviderTest {

    @Test
    fun `every registered tool has a valid non-zero before and after drawable resource`() {
        for (item in FeatureGuideRegistry.items) {
            val beforeRes = GuideImageProvider.getBeforeDrawableRes(item.id)
            val afterRes = GuideImageProvider.getAfterDrawableRes(item.id)

            assertNotEquals(0, beforeRes, "Before drawable for ${item.id} must not be 0")
            assertNotEquals(0, afterRes, "After drawable for ${item.id} must not be 0")
            assertNotEquals(beforeRes, afterRes, "Before and after drawables for ${item.id} must be distinct")
        }
    }
}
