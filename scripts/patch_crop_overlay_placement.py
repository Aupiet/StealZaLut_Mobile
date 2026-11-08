with open('app/src/main/java/com/stealzalut/mobile/ui/screens/StandaloneEditorScreen.kt', 'rb') as f:
    text = f.read().decode('utf-8')

# normalize newlines for searching
has_crlf = '\r\n' in text
data = text.replace('\r\n', '\n')

old_viewport = """                // --- 1. Edge-to-Edge Maximized Photo Viewport (Zero Dead Space) ---
                EditorViewport(
                    sourceBitmap = uiState.editorSourceBitmap,
                    previewBitmap = uiState.editorPreviewBitmap,
                    isProcessing = uiState.isEditorProcessing,
                    executionTimeMs = if (uiState.settings.developerMode) uiState.editorExecutionTimeMs else null,
                    lutName = uiState.editorCustomLutName,
                    onChangePhoto = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, // GUARD_ALLOW
                    isHistogramExpanded = uiState.isHistogramExpanded,
                    onToggleHistogram = { delegate.toggleHistogramExpanded() },
                    isComputingAutoEdit = uiState.isComputingEdit,
                    onApplyAutoEdit = { viewModel.applyEditorAutoEdit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )"""

new_viewport = """                // --- 1. Edge-to-Edge Maximized Photo Viewport (Zero Dead Space) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    EditorViewport(
                        sourceBitmap = uiState.editorSourceBitmap,
                        previewBitmap = uiState.editorPreviewBitmap,
                        isProcessing = uiState.isEditorProcessing,
                        executionTimeMs = if (uiState.settings.developerMode) uiState.editorExecutionTimeMs else null,
                        lutName = uiState.editorCustomLutName,
                        onChangePhoto = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, // GUARD_ALLOW
                        isHistogramExpanded = uiState.isHistogramExpanded,
                        onToggleHistogram = { delegate.toggleHistogramExpanded() },
                        isComputingAutoEdit = uiState.isComputingEdit,
                        onApplyAutoEdit = { viewModel.applyEditorAutoEdit() },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Superimpose interactive Crop & Straighten overlay directly over photo
                    if (uiState.editorActiveTool == 0) {
                        EditorCropOverlay(
                            cropRegion = CropRegion(),
                            onCropRegionChange = { /* Crop region updated */ },
                            onCropFinished = { viewModel.pushUndoSnapshot() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }"""

if old_viewport in data:
    data = data.replace(old_viewport, new_viewport)
    if has_crlf:
        data = data.replace('\n', '\r\n')
    with open('app/src/main/java/com/stealzalut/mobile/ui/screens/StandaloneEditorScreen.kt', 'wb') as f:
        f.write(data.encode('utf-8'))
    print('Updated StandaloneEditorScreen successfully')
else:
    print('Still not found in normalized text')
