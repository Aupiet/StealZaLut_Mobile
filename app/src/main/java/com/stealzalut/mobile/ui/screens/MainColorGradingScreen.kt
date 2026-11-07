package com.stealzalut.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.screens.editor.EditorActionsDropdownToolbar
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainColorGradingScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitEditorConfirmDialog by remember { mutableStateOf(false) }

    val exportCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) viewModel.savePendingExportToUri(uri) else viewModel.cancelPendingExport()
    }

    // Intercept hardware/gesture Back to preserve unsaved drafts and prevent accidental exits
    val isEditorWithPhoto = uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null
    val isStudioWizardOngoing = uiState.currentDestination == AppDestination.STUDIO && uiState.wizardStep > 1
    val isNotStudio = uiState.currentDestination != AppDestination.STUDIO

    BackHandler(enabled = isEditorWithPhoto || isStudioWizardOngoing || isNotStudio) {
        when {
            isEditorWithPhoto -> {
                showExitEditorConfirmDialog = true
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
                            viewModel.navigateTo(AppDestination.STUDIO)
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

    if (uiState.isInfoScreenVisible) {
        MetricsInfoBottomSheet(onDismiss = { viewModel.dismissMetricsInfo() })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) {
                        EditorActionsDropdownToolbar(
                            isExportingImage = uiState.isEditorExportingImage,
                            isExportingCube = uiState.isEditorExportingCube,
                            onExportHd = { viewModel.exportEditorImage() },
                            onExportCube = {
                                viewModel.exportEditorCube { cubePath ->
                                    exportCubeLauncher.launch(File(cubePath).name)
                                }
                            },
                            onSavePreset = { viewModel.openSaveEditingPresetDialog() },
                            onAutoEditAi = { viewModel.applyEditorAutoEdit() },
                            onOpenPresets = { viewModel.openEditorPresetsSheet() },
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
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(uiState.currentDestination.subtitleRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) {
                        IconButton(
                            onClick = { viewModel.undoEditor() },
                            enabled = viewModel.canUndoEditor()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.common_undo),
                                tint = if (viewModel.canUndoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redoEditor() },
                            enabled = viewModel.canRedoEditor()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = stringResource(R.string.common_redo),
                                tint = if (viewModel.canRedoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.openMetricsInfo() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.info_screen_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = AppDimens.BottomBarElevation
            ) {
                AppDestination.entries.forEach { destination ->
                    val isSelected = uiState.currentDestination == destination
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = label,
                                modifier = Modifier.size(AppDimens.IconMedium)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
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
        }
    }
}
