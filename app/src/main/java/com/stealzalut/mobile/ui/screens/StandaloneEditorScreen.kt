package com.stealzalut.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.BasicAdjustmentsModel
import com.stealzalut.mobile.data.model.CalibrationModel
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.data.model.ColorMixerModel
import com.stealzalut.mobile.data.model.DetailModel
import com.stealzalut.mobile.data.model.EffectsModel
import com.stealzalut.mobile.data.model.ToneCurveModel
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
import com.stealzalut.mobile.ui.theme.szlColors
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

/**
 * Modern Lightroom-inspired Standalone Photo Editor.
 * Zero dead space: the photo viewport extends edge-to-edge on a neutral darkroom canvas (#0E0E11),
 * leaving maximum visual space for critical color judgment.
 * The ergonomic adjustment drawer and toolbar occupy only the lower thumb zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneEditorScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val delegate = viewModel.lightroomDelegate
    val lrParams = uiState.editorLightroomParams

    val activeModifications = remember(lrParams) {
        buildSet {
            if (lrParams.basic != BasicAdjustmentsModel()) add(0)
            if (lrParams.toneCurve != ToneCurveModel()) add(1)
            if (lrParams.colorMixer != ColorMixerModel()) add(2)
            if (lrParams.colorGrading != ColorGradingModel()) add(3)
            if (lrParams.detail != DetailModel()) add(4)
            if (lrParams.effects != EffectsModel()) add(5)
            if (lrParams.calibration != CalibrationModel()) add(6)
        }
    }

    var isDrawerExpanded by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEditorSourceImage(it) }
    }

    val lutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEditorCustomLut(it, "Custom_LUT.cube") } // GUARD_ALLOW
    }

    val presetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json") // GUARD_ALLOW
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

    if (uiState.isEditorPresetsSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEditorPresetsSheet() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall)
            ) {
                EditorPresetsTab(
                    savedPresets = uiState.savedEditingPresets,
                    activeLutName = uiState.editorCustomLutName,
                    onImportLut = { lutPickerLauncher.launch("*/*") }, // GUARD_ALLOW
                    onClearLut = { viewModel.clearEditorCustomLut() },
                    onApplyPreset = {
                        viewModel.applyEditingPreset(it)
                        viewModel.dismissEditorPresetsSheet()
                    },
                    onDeletePreset = { viewModel.deleteEditingPreset(it) },
                    onDuplicatePreset = { viewModel.duplicateEditingPreset(it) },
                    onImportPreset = { presetImportLauncher.launch("*/*") }, // GUARD_ALLOW
                    onExportPreset = { preset ->
                        viewModel.exportEditingPreset(preset) { suggestedName ->
                            presetExportLauncher.launch(suggestedName)
                        }
                    }
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.szlColors.photoDarkroom)
    ) {
        if (uiState.editorSourceBitmap == null) {
            EditorEmptyState(
                onSelectImage = { imagePickerLauncher.launch("image/*") }, // GUARD_ALLOW
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimens.SpacingSpacious)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- 1. Edge-to-Edge Maximized Photo Viewport (Zero Dead Space) ---
                EditorViewport(
                    sourceBitmap = uiState.editorSourceBitmap,
                    previewBitmap = uiState.editorPreviewBitmap,
                    isProcessing = uiState.isEditorProcessing,
                    executionTimeMs = if (uiState.settings.developerMode) uiState.editorExecutionTimeMs else null,
                    lutName = uiState.editorCustomLutName,
                    onChangePhoto = { imagePickerLauncher.launch("image/*") }, // GUARD_ALLOW
                    isHistogramExpanded = uiState.isHistogramExpanded,
                    onToggleHistogram = { delegate.toggleHistogramExpanded() },
                    isComputingAutoEdit = uiState.isComputingEdit,
                    onApplyAutoEdit = { viewModel.applyEditorAutoEdit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // --- 2. Compact Bottom Thumb-Zone: Drawer & Toolbar ---
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Unified Active Tool Panel Card with Rounded Top Corners (24dp)
                    AnimatedVisibility(
                        visible = isDrawerExpanded,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = AppDimens.EditorPanelMinHeight + AppDimens.SpacingHuge),
                            shape = RoundedCornerShape(
                                topStart = AppDimens.CornerRadiusEditorSheet,
                                topEnd = AppDimens.CornerRadiusEditorSheet
                            ),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            border = androidx.compose.foundation.BorderStroke(
                                AppDimens.BorderThin,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall)
                            ) {
                                // Tactile Drag / Collapse Pill Handle
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                                        .clickable { isDrawerExpanded = false }
                                        .padding(vertical = AppDimens.SpacingMicro),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = AppDimens.DrawerHandleWidth, height = AppDimens.DrawerHandleHeight)
                                            .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    )
                                }

                                // Section Header with Bypass, Single Reset & Collapse for develop panels (tools 0..6)
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppDimens.SpacingExtraSmall),
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
                                            IconButton(
                                                onClick = { isDrawerExpanded = false },
                                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = stringResource(R.string.editor_collapse_panel),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(AppDimens.IconMedium)
                                                )
                                            }
                                        }
                                    }
                                }

                            // Fluid Animated Content Transition between develop tools
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                            ) {
                                AnimatedContent(
                                    targetState = uiState.editorActiveTool,
                                    transitionSpec = {
                                        (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 6 })
                                            .togetherWith(fadeOut(tween(140)))
                                    },
                                    label = "editorPanelTransition" // GUARD_ALLOW
                                ) { activeTool ->
                                    when (activeTool) {
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
                                        else -> EditorBasicPanel(
                                            basic = lrParams.basic,
                                            onUpdateBasic = { delegate.updateBasic(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    // Horizontal Scrollable Lightroom-style Toolbar with Active Indicator Dots
                    EditorToolbar(
                        activeTool = uiState.editorActiveTool,
                        onSelectTool = { toolId ->
                            if (uiState.editorActiveTool == toolId && isDrawerExpanded) {
                                isDrawerExpanded = false
                            } else {
                                isDrawerExpanded = true
                                delegate.setEditorActiveTool(toolId)
                            }
                        },
                        activeModifications = activeModifications,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    )
                }
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
