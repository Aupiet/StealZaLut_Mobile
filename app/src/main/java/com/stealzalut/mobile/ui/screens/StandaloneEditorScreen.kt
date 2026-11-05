package com.stealzalut.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.screens.editor.EditorBasicPanel
import com.stealzalut.mobile.ui.screens.editor.EditorCalibrationPanel
import com.stealzalut.mobile.ui.screens.editor.EditorColorGradingPanel
import com.stealzalut.mobile.ui.screens.editor.EditorColorMixerPanel
import com.stealzalut.mobile.ui.screens.editor.EditorDetailPanel
import com.stealzalut.mobile.ui.screens.editor.EditorEffectsPanel
import com.stealzalut.mobile.ui.screens.editor.EditorEmptyState
import com.stealzalut.mobile.ui.screens.editor.EditorPresetsTab
import com.stealzalut.mobile.ui.screens.editor.EditorSavePresetDialog
import com.stealzalut.mobile.ui.screens.editor.EditorToneCurvePanel
import com.stealzalut.mobile.ui.screens.editor.EditorToolbar
import com.stealzalut.mobile.ui.screens.editor.EditorViewport
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

/**
 * Modern Lightroom-inspired Standalone Photo Editor.
 * Maximizes photo viewport real estate (filling all remaining space)
 * with a horizontal bottom tool strip and compact, horizontal adjustment drawers.
 */
@Composable
fun StandaloneEditorScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val delegate = viewModel.lightroomDelegate
    val lrParams = uiState.editorLightroomParams

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEditorSourceImage(it) }
    }

    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEditorCustomLut(it, "Custom_LUT.cube") }
    }

    val presetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) viewModel.savePendingExportToUri(uri) else viewModel.cancelPendingExport()
    }

    val presetImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importEditingPresetFromUri(it) }
    }

    if (uiState.isSaveEditingPresetDialogVisible) {
        EditorSavePresetDialog(
            onDismiss = { viewModel.dismissSaveEditingPresetDialog() },
            onConfirm = { name -> viewModel.saveCurrentEditingPreset(name) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            if (uiState.editorSourceBitmap == null) {
                EditorEmptyState(
                    onSelectImage = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // --- 1. Large Photo Viewport (Maximized to take all available height) ---
                EditorViewport(
                    sourceBitmap = uiState.editorSourceBitmap,
                    previewBitmap = uiState.editorPreviewBitmap,
                    isProcessing = uiState.isEditorProcessing,
                    executionTimeMs = if (uiState.settings.developerMode) uiState.editorExecutionTimeMs else null,
                    lutName = uiState.editorCustomLutName,
                    onChangePhoto = { imagePickerLauncher.launch("image/*") },
                    isHistogramExpanded = uiState.isHistogramExpanded,
                    onToggleHistogram = { delegate.toggleHistogramExpanded() },
                    isComputingAutoEdit = uiState.isComputingEdit,
                    onApplyAutoEdit = { viewModel.applyEditorAutoEdit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // --- 2. Compact Active Tool Panel Container ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = AppDimens.EditorPanelMaxHeight),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.SpacingSmall)
                    ) {
                        // Section Header with Bypass & Reset for develop panels (tools 0..6)
                        if (uiState.editorActiveTool in 0..6) {
                            val (titleRes, isEnabled, onToggleBypass, onReset) = when (uiState.editorActiveTool) {
                                0 -> Quad(R.string.section_basic, lrParams.isBasicEnabled, { delegate.toggleBasicEnabled() }, { delegate.resetBasic() })
                                1 -> Quad(R.string.section_tone_curve, lrParams.isToneCurveEnabled, { delegate.toggleToneCurveEnabled() }, { delegate.resetToneCurve() })
                                2 -> Quad(R.string.section_color_mixer, lrParams.isColorMixerEnabled, { delegate.toggleColorMixerEnabled() }, { delegate.resetColorMixer() })
                                3 -> Quad(R.string.section_color_grading, lrParams.isColorGradingEnabled, { delegate.toggleColorGradingEnabled() }, { delegate.resetColorGrading() })
                                4 -> Quad(R.string.section_detail, lrParams.isDetailEnabled, { delegate.toggleDetailEnabled() }, { delegate.resetDetail() })
                                5 -> Quad(R.string.section_effects, lrParams.isEffectsEnabled, { delegate.toggleEffectsEnabled() }, { delegate.resetEffects() })
                                else -> Quad(R.string.section_calibration, lrParams.isCalibrationEnabled, { delegate.toggleCalibrationEnabled() }, { delegate.resetCalibration() })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onToggleBypass,
                                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                                    ) {
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = stringResource(R.string.desc_toggle_section),
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(AppDimens.IconSmall)
                                        )
                                    }
                                    IconButton(
                                        onClick = onReset,
                                        modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = stringResource(R.string.btn_reset_section),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(AppDimens.IconSmall)
                                        )
                                    }
                                }
                            }
                        }

                        // Tool Adjustment Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            when (uiState.editorActiveTool) {
                                0 -> EditorBasicPanel(
                                    basic = lrParams.basic,
                                    onUpdateBasic = { delegate.updateBasic(it) }
                                )
                                1 -> EditorToneCurvePanel(
                                    toneCurve = lrParams.toneCurve,
                                    onUpdateToneCurve = { delegate.updateToneCurve(it) }
                                )
                                2 -> EditorColorMixerPanel(
                                    colorMixer = lrParams.colorMixer,
                                    onUpdateColorMixer = { delegate.updateColorMixer(it) }
                                )
                                3 -> EditorColorGradingPanel(
                                    colorGrading = lrParams.colorGrading,
                                    onUpdateColorGrading = { delegate.updateColorGrading(it) }
                                )
                                4 -> EditorDetailPanel(
                                    detail = lrParams.detail,
                                    onUpdateDetail = { delegate.updateDetail(it) }
                                )
                                5 -> EditorEffectsPanel(
                                    effects = lrParams.effects,
                                    onUpdateEffects = { delegate.updateEffects(it) }
                                )
                                6 -> EditorCalibrationPanel(
                                    calibration = lrParams.calibration,
                                    onUpdateCalibration = { delegate.updateCalibration(it) }
                                )
                                else -> EditorPresetsTab(
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
                            }
                        }
                    }
                }

                // --- 3. Horizontal Scrollable Lightroom-style Toolbar ---
                EditorToolbar(
                    activeTool = uiState.editorActiveTool,
                    onSelectTool = { delegate.setEditorActiveTool(it) }
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
