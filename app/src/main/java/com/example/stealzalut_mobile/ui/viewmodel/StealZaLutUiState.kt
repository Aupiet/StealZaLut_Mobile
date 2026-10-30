package com.example.stealzalut_mobile.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import com.example.stealzalut_mobile.data.model.AppSettings
import com.example.stealzalut_mobile.data.model.ColorTransferMethod
import com.example.stealzalut_mobile.data.model.ColorimetryPreset
import com.example.stealzalut_mobile.data.model.LutSize
import com.example.stealzalut_mobile.data.model.SavedGradedImage
import com.example.stealzalut_mobile.ui.navigation.AppDestination

/**
 * Global UI state for StealZaLUT Pro, structured around 3 main features:
 * 1. Extraction d'un Thème à partir d'une photo
 * 2. Application d'un Thème connu sur une photo (Visualisation Double Vue)
 * 3. Profil & Bibliothèque (Dossier Thèmes & Dossier Images Étalonnées)
 */
data class StealZaLutUiState(
    val currentDestination: AppDestination = AppDestination.APPLY_THEME,

    // ========================================================================
    // FEATURE 1 : EXTRACTION D'UN THÈME DEPUIS UNE IMAGE
    // ========================================================================
    val extractPhotoUri: Uri? = null,
    val extractPhotoBitmap: Bitmap? = null,
    val extractPhotoPath: String? = null,
    val isExtractingTheme: Boolean = false,
    val extractedPalette: List<String> = emptyList(),
    val extractedColorSetBitmap: Bitmap? = null,
    val extractedColorSetPath: String? = null,
    val extractedExecutionTimeMs: Double? = null,

    // ========================================================================
    // FEATURE 2 : APPLICATION D'UN THÈME (VISUALISATION DOUBLE VUE SEULEMENT)
    // ========================================================================
    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val sourcePath: String? = null,

    val selectedTheme: ColorimetryPreset? = null,
    val customReferenceUri: Uri? = null,
    val customReferenceBitmap: Bitmap? = null,
    val customReferencePath: String? = null,

    val method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val intensity: Float = 1.0f,
    val lutSize: LutSize = LutSize.STANDARD,

    val isProcessingPreview: Boolean = false,
    val isExportingCube: Boolean = false,
    val isExportingImage: Boolean = false,

    val previewPath: String? = null,
    val previewBitmap: Bitmap? = null,
    val previewExecutionTimeMs: Double? = null,

    val exportedCubePath: String? = null,
    val exportedImagePath: String? = null,

    // ========================================================================
    // FEATURE 3 : PROFIL & BIBLIOTHÈQUE (DOSSIERS THÈMES & IMAGES)
    // ========================================================================
    val savedThemes: List<ColorimetryPreset> = emptyList(),
    val savedImages: List<SavedGradedImage> = emptyList(),

    // Detail dialog for inspecting & exporting a theme's mire from library
    val selectedMirePreset: ColorimetryPreset? = null,
    val selectedMireBitmap: Bitmap? = null,

    // Settings & Cache
    val settings: AppSettings = AppSettings(),
    val cacheSizeMb: Double = 0.0,

    // Messages
    val userMessage: String? = null,
    val errorMessage: String? = null
) {
    val canExtract: Boolean
        get() = extractPhotoPath != null && !isExtractingTheme

    val effectiveReferencePath: String?
        get() = customReferencePath ?: selectedTheme?.referenceImagePath

    val canApply: Boolean
        get() = sourcePath != null && effectiveReferencePath != null && !isProcessingPreview && !isExportingCube && !isExportingImage
}
