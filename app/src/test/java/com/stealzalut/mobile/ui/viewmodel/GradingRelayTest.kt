package com.stealzalut.mobile.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.delegate.GradingPipelineDelegate
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GradingRelayTest {

    private val context: Context = mockk(relaxed = true)
    private val bridgeRepository: PythonBridgeRepository = mockk(relaxed = true)
    private val savedImageRepository: SavedImageRepository = mockk(relaxed = true)
    private val stateFlow = MutableStateFlow(StealZaLutUiState())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var delegate: GradingPipelineDelegate

    @BeforeEach
    fun setUp() {
        delegate = GradingPipelineDelegate(
            context = context,
            bridgeRepository = bridgeRepository,
            savedImageRepository = savedImageRepository,
            stateFlow = stateFlow,
            scope = testScope,
            decodeSampledBitmap = { _, _ -> null }
        )
    }

    @Test
    fun `openInEditorFromStudio sets error when source or ref is missing`() = runTest {
        stateFlow.value = StealZaLutUiState(sourcePath = null, customReferencePath = null)
        delegate.openInEditorFromStudio()

        assertNotNull(stateFlow.value.errorMessage)
        assertEquals(AppDestination.STUDIO, stateFlow.value.currentDestination)
    }

    @Test
    fun `openInEditorFromStudio preloads LUT and switches destination to EDITOR`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val mockUri = mockk<Uri>(relaxed = true)
        val cubePath = "/data/user/0/com.stealzalut/cache/test_export.cube"

        stateFlow.value = StealZaLutUiState(
            sourcePath = "/data/user/0/com.stealzalut/cache/src.jpg",
            customReferencePath = "/data/user/0/com.stealzalut/cache/ref.jpg",
            sourceBitmap = mockBitmap,
            sourceUri = mockUri,
            basePreviewBitmap = mockBitmap,
            method = ColorTransferMethod.HM_MKL_HM,
            intensity = 0.9f,
            lutSize = LutSize.STANDARD,
            currentDestination = AppDestination.STUDIO
        )

        coEvery {
            bridgeRepository.exportCube(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns CubeExportResult.Success(cubePath = cubePath, lutSize = 33, executionTimeMs = 15.0)

        delegate.openInEditorFromStudio()

        val state = stateFlow.value
        assertEquals(AppDestination.EDITOR, state.currentDestination)
        assertEquals(cubePath, state.editorCustomLutPath)
        assertEquals("Studio Grade", state.editorCustomLutName)
        assertEquals(mockBitmap, state.editorPreviewBitmap)
        assertEquals(1.0f, state.step2Intensity)
        assertEquals(0.0f, state.step3Exposure)
        assertEquals(0.0f, state.step3Contrast)
        assertEquals(0.0f, state.step3Highlights)
        assertEquals(0.0f, state.step3Shadows)
        assertNull(state.errorMessage)
    }
}
