package com.stealzalut.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlBottomActionBar
import com.stealzalut.mobile.ui.components.SzlSectionHeader
import com.stealzalut.mobile.ui.screens.editor.EditorCalibrationTab
import com.stealzalut.mobile.ui.screens.editor.EditorColorTab
import com.stealzalut.mobile.ui.screens.editor.EditorEffectsTab
import com.stealzalut.mobile.ui.screens.editor.EditorLightTab
import com.stealzalut.mobile.ui.screens.editor.EditorPresetsTab
import com.stealzalut.mobile.ui.screens.editor.EditorViewport
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.savePendingExportToUri(uri)
        } else {
            viewModel.cancelPendingExport()
        }
    }

    val presetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.savePendingExportToUri(uri)
        } else {
            viewModel.cancelPendingExport()
        }
    }

    val presetImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importEditingPresetFromUri(it) }
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
                        val defaultName = "Style " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                        val name = presetNameInput.ifBlank { defaultName }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val proportionalViewportHeight = (maxHeight * 0.40f).coerceIn(200.dp, 360.dp)

            Column(
                modifier = Modifier
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
                            border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant)
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
                                    modifier = Modifier.size(AppDimens.EmptyStateIconSize)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(AppDimens.CurtainHandleSize)
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
                        executionTimeMs = if (uiState.settings.developerMode) uiState.editorExecutionTimeMs else null,
                        lutName = uiState.editorCustomLutName,
                        onChangePhoto = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(proportionalViewportHeight)
                    )

                    // Integrated Auto-Edit Quick Action Banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusMedium)
                    ) {
                        SzlSectionHeader(
                            title = stringResource(R.string.auto_edit_badge),
                            icon = Icons.Default.AutoFixHigh,
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall)
                        ) {
                            Button(
                                onClick = { viewModel.applyEditorAutoEdit() },
                                enabled = !uiState.isComputingEdit && !uiState.isEditorProcessing,
                                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (uiState.isComputingEdit) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(AppDimens.IconSmall),
                                        strokeWidth = AppDimens.BorderThick,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(AppDimens.IconSmall)
                                    )
                                    Spacer(modifier = Modifier.size(AppDimens.SpacingExtraSmall))
                                    Text(
                                        text = stringResource(R.string.btn_auto_edit),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tabs Header (PrimaryScrollableTabRow with gesture compliance)
                    PrimaryScrollableTabRow(
                        selectedTabIndex = uiState.editorSelectedTab,
                        edgePadding = AppDimens.SpacingNone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = uiState.editorSelectedTab == 0,
                            onClick = { viewModel.setEditorTab(0) },
                            text = { Text(stringResource(R.string.editor_tab_presets)) },
                            icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.TabHeightCompact)
                        )
                        Tab(
                            selected = uiState.editorSelectedTab == 1,
                            onClick = { viewModel.setEditorTab(1) },
                            text = { Text(stringResource(R.string.editor_tab_calibrate)) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.TabHeightCompact)
                        )
                        Tab(
                            selected = uiState.editorSelectedTab == 2,
                            onClick = { viewModel.setEditorTab(2) },
                            text = { Text(stringResource(R.string.editor_tab_light)) },
                            icon = { Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.TabHeightCompact)
                        )
                        Tab(
                            selected = uiState.editorSelectedTab == 3,
                            onClick = { viewModel.setEditorTab(3) },
                            text = { Text(stringResource(R.string.editor_tab_color)) },
                            icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.TabHeightCompact)
                        )
                        Tab(
                            selected = uiState.editorSelectedTab == 4,
                            onClick = { viewModel.setEditorTab(4) },
                            text = { Text(stringResource(R.string.editor_tab_effects)) },
                            icon = { Icon(Icons.Default.BlurOn, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall)) },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.TabHeightCompact)
                        )
                    }

                    // Tab Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (uiState.editorSelectedTab) {
                            0 -> EditorPresetsTab(
                                savedPresets = uiState.savedEditingPresets,
                                activeLutName = uiState.editorCustomLutName,
                                onImportLut = { lutPickerLauncher.launch("*/*") },
                                onClearLut = { viewModel.clearEditorCustomLut() },
                                onApplyPreset = { viewModel.applyEditingPreset(it) },
                                onDeletePreset = { viewModel.deleteEditingPreset(it) },
                                onDuplicatePreset = { viewModel.duplicateEditingPreset(it) },
                                onImportPreset = { presetImportLauncher.launch("*/*") },
                                onExportPreset = { preset ->
                                    viewModel.exportEditingPreset(preset) { suggestedName ->
                                        presetExportLauncher.launch(suggestedName)
                                    }
                                }
                            )
                            1 -> EditorCalibrationTab(
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
                            2 -> EditorLightTab(
                                exposure = uiState.step3Exposure,
                                onExposureChange = { viewModel.setStep3Exposure(it) },
                                contrast = uiState.step3Contrast,
                                onContrastChange = { viewModel.setStep3Contrast(it) },
                                highlights = uiState.step3Highlights,
                                onHighlightsChange = { viewModel.setStep3Highlights(it) },
                                shadows = uiState.step3Shadows,
                                onShadowsChange = { viewModel.setStep3Shadows(it) }
                            )
                            3 -> EditorColorTab(
                                temperature = uiState.step3Temperature,
                                onTemperatureChange = { viewModel.setStep3Temperature(it) },
                                tint = uiState.step3Tint,
                                onTintChange = { viewModel.setStep3Tint(it) },
                                vibrance = uiState.step3Vibrance,
                                onVibranceChange = { viewModel.setStep3Vibrance(it) },
                                saturation = uiState.step3Saturation,
                                onSaturationChange = { viewModel.setStep3Saturation(it) }
                            )
                            4 -> EditorEffectsTab(
                                clarity = uiState.step3Clarity,
                                onClarityChange = { viewModel.setStep3Clarity(it) },
                                sharpen = uiState.step3Sharpen,
                                onSharpenChange = { viewModel.setStep3Sharpen(it) },
                                grain = uiState.step3Grain,
                                onGrainChange = { viewModel.setStep3Grain(it) }
                            )
                        }
                    }

                    // Bottom Actions Bar with Single Reset & Undo Snackbar
                    SzlBottomActionBar(
                        isExportingImage = uiState.isEditorExportingImage,
                        isExportingCube = uiState.isEditorExportingCube,
                        onExportHd = { viewModel.exportEditorImage() },
                        onExportCube = {
                            viewModel.exportEditorCube { cubePath ->
                                exportCubeLauncher.launch(File(cubePath).name)
                            }
                        },
                        onSavePreset = { viewModel.openSaveEditingPresetDialog() },
                        onResetAll = {
                            viewModel.resetAllEditorParams()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.msg_edits_reset),
                                    actionLabel = context.getString(R.string.common_undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoEditor()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
