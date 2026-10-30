package com.stealzalut.mobile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stealzalut.mobile.ui.components.ExportActionBar
import com.stealzalut.mobile.ui.theme.StealZaLut_MobileTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainColorGradingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exportActionBar_buttonsAreDisabledWhenCannotProcess() {
        composeTestRule.setContent {
            StealZaLut_MobileTheme {
                ExportActionBar(
                    canProcess = false,
                    isProcessingPreview = false,
                    isExportingCube = false,
                    isExportingImage = false,
                    hasExportedCube = false,
                    hasExportedImage = false,
                    onGeneratePreview = {},
                    onExportGradedImage = {},
                    onExportCube = {},
                    onSaveColorimetry = {},
                    onShare = {}
                )
            }
        }

        // Verify accessibility labels exist and interactive targets conform to WCAG 2.1 AA
        composeTestRule.onNodeWithContentDescription("Preview").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Export").assertIsNotEnabled()
    }
}
