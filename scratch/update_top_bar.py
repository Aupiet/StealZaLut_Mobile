path = r'app/src/main/java/com/stealzalut/mobile/ui/screens/MainColorGradingScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

target = '''            if (uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        // Left: Undo / Redo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                        ) {
                            IconButton(
                                onClick = { viewModel.undoEditor() },
                                enabled = viewModel.canUndoEditor(),
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionUndo,
                                    contentDescription = stringResource(R.string.common_undo),
                                    tint = if (viewModel.canUndoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(AppDimens.IconNormal)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redoEditor() },
                                enabled = viewModel.canRedoEditor(),
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionRedo,
                                    contentDescription = stringResource(R.string.common_redo),
                                    tint = if (viewModel.canRedoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(AppDimens.IconNormal)
                                )
                            }
                        }

                        // Center: Comparer Pill
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                            border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)),
                            modifier = Modifier.clickable {
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

                        // Right: Actions Dropdown & Primary Amber Export
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

                            Surface(
                                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(AppDimens.ButtonHeightCompact)
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
            }'''

replacement = '''            if (uiState.currentDestination == AppDestination.EDITOR && uiState.editorSourceBitmap != null) {
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
                                enabled = viewModel.canUndoEditor(),
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionUndo,
                                    contentDescription = stringResource(R.string.common_undo),
                                    tint = if (viewModel.canUndoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    modifier = Modifier.size(AppDimens.IconNormal)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redoEditor() },
                                enabled = viewModel.canRedoEditor(),
                                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
                            ) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionRedo,
                                    contentDescription = stringResource(R.string.common_redo),
                                    tint = if (viewModel.canRedoEditor()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
            }'''

# Normalize newlines for matching
c_norm = c.replace('\r\n', '\n')
t_norm = target.replace('\r\n', '\n')
r_norm = replacement.replace('\r\n', '\n')

if t_norm in c_norm:
    c_norm = c_norm.replace(t_norm, r_norm)
    # Ensure statusBarsPadding import exists
    if 'statusBarsPadding' not in c_norm:
        c_norm = c_norm.replace('import androidx.compose.foundation.layout.padding', 'import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.statusBarsPadding\nimport androidx.compose.foundation.layout.defaultMinSize')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c_norm)
    print("Replaced top bar successfully")
else:
    print("Target not found")
