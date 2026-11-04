package com.stealzalut.mobile.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.ui.components.ImagePickerCard
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File
import kotlin.math.roundToInt

/**
 * Standalone Photo Editor (Mode Éditeur Pur / Photo Studio).
 * Enables direct non-destructive photo editing and preset creation without reference style B.
 */
@Composable
fun StandaloneEditorScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEditorSourceImage(it) }
    }

    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setEditorCustomLut(it, "Custom_LUT.cube")
        }
    }

    val exportCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveExportedCubeToUri(it) }
    }

    var presetNameInput by remember { mutableStateOf("") }

    if (uiState.isSaveEditingPresetDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveEditingPresetDialog() },
            title = {
                Text(
                    text = stringResource(R.string.dialog_save_preset_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
                    Text(
                        text = stringResource(R.string.dialog_preset_name_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        placeholder = { Text(stringResource(R.string.dialog_preset_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = presetNameInput.ifBlank { "Mon Preset" }
                        viewModel.saveCurrentEditingPreset(name)
                        presetNameInput = ""
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveEditingPresetDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        if (uiState.editorSourceBitmap == null) {
            // Empty State: Select photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpacingLarge)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.SpacingExtraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.editor_select_image),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.editor_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Editor Active: Viewport + Controls
            EditorViewport(
                sourceBitmap = uiState.editorSourceBitmap,
                previewBitmap = uiState.editorPreviewBitmap,
                isProcessing = uiState.isEditorProcessing,
                executionTimeMs = uiState.editorExecutionTimeMs,
                lutName = uiState.editorCustomLutName,
                onChangePhoto = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            // Tabs Header
            ScrollableTabRow(
                selectedTabIndex = uiState.editorSelectedTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.editorSelectedTab == 0,
                    onClick = { viewModel.setEditorTab(0) },
                    text = { Text(stringResource(R.string.editor_tab_presets)) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) }
                )
                Tab(
                    selected = uiState.editorSelectedTab == 1,
                    onClick = { viewModel.setEditorTab(1) },
                    text = { Text(stringResource(R.string.editor_tab_calibrate)) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) }
                )
                Tab(
                    selected = uiState.editorSelectedTab == 2,
                    onClick = { viewModel.setEditorTab(2) },
                    text = { Text(stringResource(R.string.editor_tab_light)) },
                    icon = { Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) }
                )
                Tab(
                    selected = uiState.editorSelectedTab == 3,
                    onClick = { viewModel.setEditorTab(3) },
                    text = { Text(stringResource(R.string.editor_tab_color)) },
                    icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) }
                )
                Tab(
                    selected = uiState.editorSelectedTab == 4,
                    onClick = { viewModel.setEditorTab(4) },
                    text = { Text(stringResource(R.string.editor_tab_effects)) },
                    icon = { Icon(Icons.Default.BlurOn, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.editorSelectedTab) {
                    0 -> PresetsAndLutsTab(
                        savedPresets = uiState.savedEditingPresets,
                        activeLutName = uiState.editorCustomLutName,
                        onImportLut = { lutPickerLauncher.launch("*/*") },
                        onClearLut = { viewModel.clearEditorCustomLut() },
                        onApplyPreset = { viewModel.applyEditingPreset(it) },
                        onDeletePreset = { viewModel.deleteEditingPreset(it) }
                    )
                    1 -> LutCalibrationTab(
                        hasLut = uiState.editorCustomLutPath != null,
                        intensity = uiState.step2Intensity,
                        onIntensityChange = { viewModel.setStep2Intensity(it) },
                        lumaLock = uiState.step2LumaLock,
                        onLumaLockChange = { viewModel.setStep2LumaLock(it) },
                        neutralLock = uiState.step2NeutralAxisLock,
                        onNeutralLockChange = { viewModel.setStep2NeutralAxisLock(it) },
                        skinToneLock = uiState.step2SkinToneLock,
                        onSkinToneLockChange = { viewModel.setStep2SkinToneLock(it) },
                        extremesRolloff = uiState.step2ExtremesRolloff,
                        onExtremesRolloffChange = { viewModel.setStep2ExtremesRolloff(it) },
                        spatial3d = uiState.step2Spatial3d,
                        onSpatial3dChange = { viewModel.setStep2Spatial3d(it) },
                        onResetTab = { viewModel.resetStep2Params() }
                    )
                    2 -> LightTab(
                        exposure = uiState.step3Exposure,
                        onExposureChange = { viewModel.setStep3Exposure(it) },
                        contrast = uiState.step3Contrast,
                        onContrastChange = { viewModel.setStep3Contrast(it) },
                        highlights = uiState.step3Highlights,
                        onHighlightsChange = { viewModel.setStep3Highlights(it) },
                        shadows = uiState.step3Shadows,
                        onShadowsChange = { viewModel.setStep3Shadows(it) }
                    )
                    3 -> ColorTab(
                        temperature = uiState.step3Temperature,
                        onTemperatureChange = { viewModel.setStep3Temperature(it) },
                        tint = uiState.step3Tint,
                        onTintChange = { viewModel.setStep3Tint(it) },
                        vibrance = uiState.step3Vibrance,
                        onVibranceChange = { viewModel.setStep3Vibrance(it) },
                        saturation = uiState.step3Saturation,
                        onSaturationChange = { viewModel.setStep3Saturation(it) }
                    )
                    4 -> EffectsTab(
                        clarity = uiState.step3Clarity,
                        onClarityChange = { viewModel.setStep3Clarity(it) },
                        sharpen = uiState.step3Sharpen,
                        onSharpenChange = { viewModel.setStep3Sharpen(it) },
                        grain = uiState.step3Grain,
                        onGrainChange = { viewModel.setStep3Grain(it) }
                    )
                }
            }

            // Bottom Actions Bar
            EditorBottomBar(
                isExportingCube = uiState.isEditorExportingCube,
                isExportingImage = uiState.isEditorExportingImage,
                onSavePreset = { viewModel.openSaveEditingPresetDialog() },
                onExportHd = { viewModel.exportEditorImage() },
                onExportCube = {
                    viewModel.exportEditorCube { cubePath ->
                        exportCubeLauncher.launch(File(cubePath).name)
                    }
                },
                onResetAll = { viewModel.resetAllEditorParams() }
            )
        }
    }
}

// ============================================================================
// VIEWPORT AVEC TOUCH-HOLD COMPARISON ET PINCH-TO-ZOOM
// ============================================================================

@Composable
private fun EditorViewport(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    isProcessing: Boolean,
    executionTimeMs: Double?,
    lutName: String?,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHoldingCompare by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val displayBitmap = if (isHoldingCompare && sourceBitmap != null) sourceBitmap else (previewBitmap ?: sourceBitmap)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerRadiusCard)),
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        isHoldingCompare = true
                        waitForUpOrCancellation()
                        isHoldingCompare = false
                    }
                }
        ) {
            displayBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }

            // Compare badge overlay
            if (isHoldingCompare) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AppDimens.SpacingMedium)
                ) {
                    Text(
                        text = "ORIGINAL (Avant)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AppDimens.SpacingSmall)
                ) {
                    Text(
                        text = stringResource(R.string.editor_hold_compare),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = 2.dp)
                    )
                }
            }

            // Top-left LUT status badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppDimens.SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                    color = if (lutName != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = lutName ?: stringResource(R.string.editor_no_lut_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lutName != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = 2.dp)
                    )
                }
            }

            // Top-right controls (Change photo, processing indicator)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                        strokeWidth = AppDimens.BorderThick,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                }
                FilledTonalIconButton(
                    onClick = onChangePhoto,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.editor_change_image),
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                }
            }
        }
    }
}

// ============================================================================
// DOUBLE-TAP RESET SLIDER COMPONENT
// ============================================================================

@Composable
private fun DoubleTapSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float = 0.0f,
    max: Float = 1.0f,
    defaultValue: Float = 0.0f,
    valueFormat: String = "%.2f",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, _ -> }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Double tap / click to reset
                    onValueChange(defaultValue)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(java.util.Locale.US, valueFormat, value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (value != defaultValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (value != defaultValue) FontWeight.Bold else FontWeight.Normal
                )
                if (value != defaultValue) {
                    IconButton(
                        onClick = { onValueChange(defaultValue) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// TAB 0: PRESETS & LUTS
// ============================================================================

@Composable
private fun PresetsAndLutsTab(
    savedPresets: List<EditPreset>,
    activeLutName: String?,
    onImportLut: () -> Unit,
    onClearLut: () -> Unit,
    onApplyPreset: (EditPreset) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // LUT Import / Active Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpacingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Table 3D LUT de base (.CUBE)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeLutName ?: "Aucune LUT chargée (Mode étalonnage direct)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        if (activeLutName != null) {
                            IconButton(onClick = onClearLut) {
                                Icon(Icons.Default.Close, contentDescription = "Retirer LUT", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedButton(onClick = onImportLut) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.editor_import_cube))
                        }
                    }
                }
            }
        }

        // Saved Presets List
        item {
            Text(
                text = "Mes Presets d\'Édition (.szlpreset)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedPresets.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.editor_no_presets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AppDimens.SpacingLarge)
                )
            }
        } else {
            items(savedPresets, key = { it.id }) { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyPreset(preset) },
                    shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.SpacingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (preset.lutReference != null) {
                                Text(
                                    text = "LUT: ${File(preset.lutReference).name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onApplyPreset(preset) }) {
                                Text("Appliquer")
                            }
                            IconButton(onClick = { onDeletePreset(preset.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 1: LUT CALIBRATION (STAGE 2)
// ============================================================================

@Composable
private fun LutCalibrationTab(
    hasLut: Boolean,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    lumaLock: Float,
    onLumaLockChange: (Float) -> Unit,
    neutralLock: Float,
    onNeutralLockChange: (Float) -> Unit,
    skinToneLock: Float,
    onSkinToneLockChange: (Float) -> Unit,
    extremesRolloff: Float,
    onExtremesRolloffChange: (Float) -> Unit,
    spatial3d: Float,
    onSpatial3dChange: (Float) -> Unit,
    onResetTab: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        if (!hasLut) {
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppDimens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(AppDimens.SpacingNormal))
                    Text(
                        text = "Les masques de calibrage opèrent sur une LUT chargée ou un filtre actif.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        DoubleTapSlider(
            title = stringResource(R.string.param_global_intensity_title),
            value = intensity,
            onValueChange = onIntensityChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 1.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_luma_lock_title),
            value = lumaLock,
            onValueChange = onLumaLockChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_neutral_axis_lock_title),
            value = neutralLock,
            onValueChange = onNeutralLockChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_skin_tone_lock_title),
            value = skinToneLock,
            onValueChange = onSkinToneLockChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_extremes_rolloff_title),
            value = extremesRolloff,
            onValueChange = onExtremesRolloffChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_spatial_3d_title),
            value = spatial3d,
            onValueChange = onSpatial3dChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )
    }
}

// ============================================================================
// TAB 2: LIGHT (STAGE 3)
// ============================================================================

@Composable
private fun LightTab(
    exposure: Float,
    onExposureChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    highlights: Float,
    onHighlightsChange: (Float) -> Unit,
    shadows: Float,
    onShadowsChange: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        DoubleTapSlider(
            title = stringResource(R.string.edit_param_exposure),
            value = exposure,
            onValueChange = onExposureChange,
            min = -2.0f,
            max = 2.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f EV"
        )

        DoubleTapSlider(
            title = stringResource(R.string.edit_param_contrast),
            value = contrast,
            onValueChange = onContrastChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )

        DoubleTapSlider(
            title = stringResource(R.string.edit_param_highlights),
            value = highlights,
            onValueChange = onHighlightsChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )

        DoubleTapSlider(
            title = stringResource(R.string.edit_param_shadows),
            value = shadows,
            onValueChange = onShadowsChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )
    }
}

// ============================================================================
// TAB 3: COLOR (STAGE 3)
// ============================================================================

@Composable
private fun ColorTab(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    tint: Float,
    onTintChange: (Float) -> Unit,
    vibrance: Float,
    onVibranceChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        DoubleTapSlider(
            title = stringResource(R.string.param_temperature_title),
            value = temperature,
            onValueChange = onTemperatureChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_tint_title),
            value = tint,
            onValueChange = onTintChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )

        DoubleTapSlider(
            title = stringResource(R.string.edit_param_vibrance),
            value = vibrance,
            onValueChange = onVibranceChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )

        DoubleTapSlider(
            title = stringResource(R.string.edit_param_saturation),
            value = saturation,
            onValueChange = onSaturationChange,
            min = -1.0f,
            max = 1.0f,
            defaultValue = 0.0f,
            valueFormat = "%+.2f"
        )
    }
}

// ============================================================================
// TAB 4: EFFECTS (STAGE 3)
// ============================================================================

@Composable
private fun EffectsTab(
    clarity: Float,
    onClarityChange: (Float) -> Unit,
    sharpen: Float,
    onSharpenChange: (Float) -> Unit,
    grain: Float,
    onGrainChange: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        DoubleTapSlider(
            title = stringResource(R.string.param_clarity_title),
            value = clarity,
            onValueChange = onClarityChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_sharpen_title),
            value = sharpen,
            onValueChange = onSharpenChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )

        DoubleTapSlider(
            title = stringResource(R.string.param_grain_title),
            value = grain,
            onValueChange = onGrainChange,
            min = 0.0f,
            max = 1.0f,
            defaultValue = 0.0f
        )
    }
}

// ============================================================================
// BOTTOM ACTIONS BAR
// ============================================================================

@Composable
private fun EditorBottomBar(
    isExportingCube: Boolean,
    isExportingImage: Boolean,
    onSavePreset: () -> Unit,
    onExportHd: () -> Unit,
    onExportCube: () -> Unit,
    onResetAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset All
        FilledTonalIconButton(onClick = onResetAll) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Réinitialiser tout")
        }

        // Save Preset
        OutlinedButton(
            onClick = onSavePreset,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.editor_save_preset), style = MaterialTheme.typography.labelSmall)
        }

        // Export .CUBE
        OutlinedButton(
            onClick = onExportCube,
            enabled = !isExportingCube,
            modifier = Modifier.weight(1f)
        ) {
            if (isExportingCube) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.editor_export_cube), style = MaterialTheme.typography.labelSmall)
            }
        }

        // Export HD Image
        Button(
            onClick = onExportHd,
            enabled = !isExportingImage,
            modifier = Modifier.weight(1.2f)
        ) {
            if (isExportingImage) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.editor_export_hd), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
