package com.stealzalut.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.delegate.EditorSessionDelegate
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EditorUndoRedoStackTest {

    private val context: Context = mockk(relaxed = true)
    private val bridgeRepository: PythonBridgeRepository = mockk(relaxed = true)
    private val savedImageRepository: SavedImageRepository = mockk(relaxed = true)
    private val projectRepository: com.stealzalut.mobile.data.repository.ProjectRepository = mockk(relaxed = true)
    private val stateFlow = MutableStateFlow(StealZaLutUiState())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var delegate: EditorSessionDelegate

    @BeforeEach
    fun setUp() {
        stateFlow.value = StealZaLutUiState()
        delegate = EditorSessionDelegate(
            context = context,
            bridgeRepository = bridgeRepository,
            savedImageRepository = savedImageRepository,
            projectRepository = projectRepository,
            stateFlow = stateFlow,
            scope = testScope,
            decodeSampledBitmap = { _, _ -> null }
        )
    }

    @Test
    fun `initial state has canUndo and canRedo false`() {
        assertFalse(delegate.canUndo())
        assertFalse(delegate.canRedo())
        assertFalse(stateFlow.value.canUndo)
        assertFalse(stateFlow.value.canRedo)
    }

    @Test
    fun `single gesture push and undo restores previous value and updates state`() {
        assertEquals(0.0f, stateFlow.value.step3Exposure)

        // User starts dragging exposure slider
        delegate.setStep3Exposure(0.5f)
        delegate.setStep3Exposure(1.0f)
        delegate.setStep3Exposure(1.5f)
        // End of drag gesture
        delegate.endEditGesture()

        assertEquals(1.5f, stateFlow.value.step3Exposure)
        assertTrue(delegate.canUndo())
        assertTrue(stateFlow.value.canUndo)
        assertFalse(delegate.canRedo())
        assertFalse(stateFlow.value.canRedo)

        // Undo gesture
        delegate.undo()
        assertEquals(0.0f, stateFlow.value.step3Exposure)
        assertFalse(delegate.canUndo())
        assertFalse(stateFlow.value.canUndo)
        assertTrue(delegate.canRedo())
        assertTrue(stateFlow.value.canRedo)

        // Redo gesture
        delegate.redo()
        assertEquals(1.5f, stateFlow.value.step3Exposure)
        assertTrue(delegate.canUndo())
        assertTrue(stateFlow.value.canUndo)
        assertFalse(delegate.canRedo())
        assertFalse(stateFlow.value.canRedo)
    }

    @Test
    fun `coalescing combines slider micro-ticks into exactly one undo step`() {
        // First gesture: Contrast
        delegate.setStep3Contrast(0.2f)
        delegate.setStep3Contrast(0.4f)
        delegate.setStep3Contrast(0.6f)
        delegate.endEditGesture()

        // Second gesture: Saturation
        delegate.setStep3Saturation(0.1f)
        delegate.setStep3Saturation(0.3f)
        delegate.endEditGesture()

        assertEquals(0.6f, stateFlow.value.step3Contrast)
        assertEquals(0.3f, stateFlow.value.step3Saturation)

        // First undo should only revert saturation
        delegate.undo()
        assertEquals(0.6f, stateFlow.value.step3Contrast)
        assertEquals(0.0f, stateFlow.value.step3Saturation)

        // Second undo should revert contrast
        delegate.undo()
        assertEquals(0.0f, stateFlow.value.step3Contrast)
        assertEquals(0.0f, stateFlow.value.step3Saturation)
        assertFalse(delegate.canUndo())
    }

    @Test
    fun `stack caps at 50 items and drops oldest`() {
        for (i in 1..60) {
            delegate.setStep3Exposure(if (i % 2 == 0) 0.5f else -0.5f)
            delegate.endEditGesture()
        }

        // We should be able to undo at most 50 times
        var undoCount = 0
        while (delegate.canUndo()) {
            delegate.undo()
            undoCount++
        }
        assertEquals(50, undoCount)
        assertFalse(delegate.canUndo())
    }

    @Test
    fun `new edit action clears redo stack`() {
        delegate.setStep3Exposure(1.0f)
        delegate.endEditGesture()

        delegate.undo()
        assertTrue(delegate.canRedo())

        // New edit while redo was available
        delegate.setStep3Highlights(0.5f)
        delegate.endEditGesture()

        assertFalse(delegate.canRedo())
        assertFalse(stateFlow.value.canRedo)
    }

    @Test
    fun `crop region is tracked by undo and redo`() {
        val originalCrop = stateFlow.value.editorCropRegion
        val newCrop = CropRegion(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f, angle = 5.0f)

        delegate.updateCropRegion(newCrop)
        delegate.onCropFinished()

        assertEquals(newCrop, stateFlow.value.editorCropRegion)
        assertTrue(delegate.canUndo())

        delegate.undo()
        assertEquals(originalCrop, stateFlow.value.editorCropRegion)

        delegate.redo()
        assertEquals(newCrop, stateFlow.value.editorCropRegion)
    }

    @Test
    fun `resetAllEditorParams is undoable and restores lightroom and crop`() {
        delegate.lightroomDelegate.updateBasic { it.copy(clarity = 30f) }
        delegate.lightroomDelegate.onGestureFinished()
        val customCrop = CropRegion(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)
        delegate.updateCropRegion(customCrop)
        delegate.onCropFinished()

        delegate.resetAllEditorParams()

        assertEquals(0f, stateFlow.value.editorLightroomParams.basic.clarity)
        assertEquals(CropRegion(), stateFlow.value.editorCropRegion)
        assertTrue(delegate.canUndo())

        // Undo the reset
        delegate.undo()
        assertEquals(30f, stateFlow.value.editorLightroomParams.basic.clarity)
        assertEquals(customCrop, stateFlow.value.editorCropRegion)
    }

    @Test
    fun `setEditorSourceImage clears undo and redo stacks`() = runTest {
        delegate.setStep3Exposure(1.0f)
        delegate.endEditGesture()
        assertTrue(delegate.canUndo())

        val mockFile = File.createTempFile("test_editor", ".jpg")
        coEvery { bridgeRepository.cacheImageFromUri(any(), any()) } returns Result.success(mockFile)

        val uri: Uri = mockk(relaxed = true)
        delegate.setEditorSourceImage(uri)

        assertFalse(delegate.canUndo())
        assertFalse(delegate.canRedo())
        assertFalse(stateFlow.value.canUndo)
        assertFalse(stateFlow.value.canRedo)

        mockFile.delete()
    }
}
