package com.stealzalut.mobile.ui.screens

import com.stealzalut.mobile.ui.screens.guide.FeatureGuideScreen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.screens.editor.EditorActionsDropdownToolbar
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import com.stealzalut.mobile.core.diagnostics.DiagnosticUiEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainColorGradingScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val msgEditsReset = stringResource(R.string.msg_edits_reset)
    val commonUndo = stringResource(R.string.common_undo)
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitEditorConfirmDialog by remember { mutableStateOf(false) }
    var showMaintenanceMonitor by remember { mutableStateOf(false) }

    // Intercept hardware/gesture Back to preserve unsaved drafts and prevent accidental exits
    val isEditorWithPhoto = uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null
    val isStudioWizardOngoing = uiState.currentDestination == AppDestination.STUDIO && uiState.wizardStep > 1
    val isNotStudio = uiState.currentDestination != AppDestination.STUDIO

    BackHandler(enabled = isEditorWithPhoto || isStudioWizardOngoing || isNotStudio) {
        when {
            isEditorWithPhoto -> {
                showExitEditorConfirmDialog = true
            }
            uiState.currentDestination == AppDestination.GUIDE -> {
                viewModel.navigateBackFromGuide()
            }
            isStudioWizardOngoing -> {
                viewModel.setWizardStep(uiState.wizardStep - 1)
            }
            isNotStudio -> {
                viewModel.navigateTo(AppDestination.STUDIO)
            }
        }
    }

    if (showExitEditorConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitEditorConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.editor_exit_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.editor_exit_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitEditorConfirmDialog = false
                        viewModel.openSaveEditingPresetDialog()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.editor_btn_save_draft),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExitEditorConfirmDialog = false
                            viewModel.navigateTo(AppDestination.STUDIO)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.editor_btn_leave),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = {
                            showExitEditorConfirmDialog = false
                            viewModel.closeEditorPhoto()
                            viewModel.navigateTo(AppDestination.EDITOR)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.editor_btn_close_image),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    // React to user messages and errors
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context), duration = SnackbarDuration.Short)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err.asString(context), duration = SnackbarDuration.Long)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(snackbarHostState) {
        DiagnosticUiEvents.events.collect { event ->
            snackbarHostState.showSnackbar(event.asString(context), duration = SnackbarDuration.Long)
        }
    }



    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(AppDimens.TopAppBarHeight),
                    color = MaterialTheme.szlColors.photoDarkroom,
                    border = BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppDimens.SpacingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Undo / Redo (accessible 48dp target & contrast)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                        ) {
                            IconButton(
                                onClick = { viewModel.undoEditor() },
                                enabled = uiState.canUndo,
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionUndo,
                                    contentDescription = stringResource(R.string.common_undo),
                                    tint = if (uiState.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(AppDimens.IconNormal)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redoEditor() },
                                enabled = uiState.canRedo,
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionRedo,
                                    contentDescription = stringResource(R.string.common_redo),
                                    tint = if (uiState.canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(AppDimens.IconNormal)
                                )
                            }
                        }

                        // Center: Comparer Pill
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                            border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)),
                            modifier = Modifier
                                .defaultMinSize(minHeight = AppDimens.ButtonHeightCompact)
                                .clickable {
                                    // Direct comparison toggle
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionVisibilityOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Text(
                                    text = stringResource(R.string.editor_compare),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Right: Actions Dropdown (⋮) & Primary Amber Export (48dp target)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            EditorActionsDropdownToolbar(
                                isExportingImage = uiState.isEditorExportingImage,
                                isExportingCube = uiState.isEditorExportingCube,
                                onExportHd = { viewModel.openExportDialog() },
                                onExportCube = {
                                    viewModel.exportAndShareEditorCube(context)
                                },
                                onSavePreset = { viewModel.openSaveEditingPresetDialog() },
                                onAutoEditAi = { viewModel.applyEditorAutoEdit() },
                                onOpenPresets = { viewModel.openEditorPresetsSheet() },
                                onOpenGuide = { viewModel.navigateTo(AppDestination.GUIDE) },
                                onResetAll = {
                                    viewModel.resetAllEditorParams()
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = msgEditsReset,
                                            actionLabel = commonUndo,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoEditor()
                                        }
                                    }
                                }
                            )

                            Surface(
                                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(AppDimens.MinInteractiveTarget)
                                    .clickable { viewModel.openExportDialog() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = MaterialTheme.icons.actionExport,
                                        contentDescription = stringResource(R.string.export_dialog_title),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(AppDimens.IconNormal)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!(uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) && uiState.currentDestination != AppDestination.GUIDE) {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.HorizontalDivider(
                        thickness = AppDimens.DividerThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = AppDimens.SpacingNone
                    ) {
                        val iconSet = MaterialTheme.icons
                        AppDestination.entries.filter { it.isBottomBarItem }.forEach { destination ->
                            val isSelected = uiState.currentDestination == destination
                            val label = stringResource(destination.labelRes)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.iconProvider(iconSet),
                                        contentDescription = label,
                                        modifier = Modifier.size(AppDimens.IconMedium)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (uiState.settings.developerMode) {
                            val label = stringResource(R.string.maintenance_nav)
                            NavigationBarItem(
                                selected = showMaintenanceMonitor,
                                onClick = { showMaintenanceMonitor = true },
                                icon = {
                                    Icon(
                                        imageVector = iconSet.statusHistogram,
                                        contentDescription = label,
                                        modifier = Modifier.size(AppDimens.IconMedium)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (uiState.currentDestination) {
            AppDestination.STUDIO -> {
                ApplyThemeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.EDITOR -> {
                StandaloneEditorScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.PALETTES -> {
                PalettesScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.PROFILE -> {
                ProfileScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.GUIDE -> {
                FeatureGuideScreen(
                    onNavigateBack = { viewModel.navigateBackFromGuide() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showMaintenanceMonitor) {
        MaintenanceMonitorScreen(
            viewModel = viewModel,
            onDismiss = { showMaintenanceMonitor = false }
        )
    }
}
