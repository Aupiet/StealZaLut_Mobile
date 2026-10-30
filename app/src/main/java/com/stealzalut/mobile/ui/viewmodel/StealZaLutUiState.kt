package com.stealzalut.mobile.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.navigation.AppDestination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Global UI state for StealZaLUT Mobile, structured around 3 functional pillars:
 * 1. STUDIO   : Color grading funnel (Source + Reference -> 120 FPS GPU Preview -> Export .cube / Gallery Image)
 * 2. PALETTES : Palette extraction and saved presets library
 * 3. SETTINGS : Application preferences, algorithm defaults, and cache maintenance
 */
@Immutable
data class StealZaLutUiState(
    val currentDestination: AppDestination = AppDestination.STUDIO,

    // ========================================================================
    // 1. STUDIO DE GRADATION (WIZARD FUNNEL)
    // ========================================================================
    val wizardStep: Int = AppConstants.STEP_1_SOURCE,
    val computeStageIndex: Int = 1,
    val isSavingThemeDialogVisible: Boolean = false,
    val styleBSourceMode: Int = 0, // 0 = Custom Photo B, 1 = Saved Preset

    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val sourcePath: String? = null,

    val selectedTheme: ColorimetryPreset? = null,
    val customReferenceUri: Uri? = null,
    val customReferenceBitmap: Bitmap? = null,
    val customReferencePath: String? = null,
    val customReferencePalette: ImmutableList<String> = persistentListOf(),

    val method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val intensity: Float = AppConstants.INTENSITY_DEFAULT,
    val lutSize: LutSize = LutSize.STANDARD,

    val isProcessingPreview: Boolean = false,
    val isExportingCube: Boolean = false,
    val isExportingImage: Boolean = false,

    // Raw transferred preview computed at alpha = 1.0 by Python
    val basePreviewBitmap: Bitmap? = null,
    val previewPath: String? = null,
    val previewExecutionTimeMs: Double? = null,

    val exportedCubePath: String? = null,
    val exportedImagePath: String? = null,
    val isGradedImageSaved: Boolean = false,

    // Split Wipe slider position in [0.0, 1.0]
    val splitPosition: Float = AppConstants.SPLIT_WIPE_DEFAULT_FRACTION,
    val isSplitWipeMode: Boolean = false,

    // ========================================================================
    // 2. PALETTES & PRESETS
    // ========================================================================
    val palettePhotoUri: Uri? = null,
    val palettePhotoBitmap: Bitmap? = null,
    val palettePhotoPath: String? = null,
    val isExtractingPalette: Boolean = false,
    val extractedPalette: ImmutableList<String> = persistentListOf(),
    val savedPresets: ImmutableList<ColorimetryPreset> = persistentListOf(),

    // ========================================================================
    // 3. SETTINGS & CACHE
    // ========================================================================
    val settings: AppSettings = AppSettings(),
    val cacheSizeMb: Double = 0.0,
    val currentLanguageTag: String = "en",

    // Feedback
    val userMessage: String? = null,
    val errorMessage: String? = null
) {
    val effectiveReferencePath: String?
        get() = if (styleBSourceMode == 0) customReferencePath else (selectedTheme?.referenceImagePath ?: customReferencePath)

    val canProceedToStep2: Boolean
        get() = sourcePath != null

    val canProceedToStep3: Boolean
        get() = effectiveReferencePath != null

    val canApply: Boolean
        get() = sourcePath != null && effectiveReferencePath != null && !isProcessingPreview && !isExportingCube && !isExportingImage
}
