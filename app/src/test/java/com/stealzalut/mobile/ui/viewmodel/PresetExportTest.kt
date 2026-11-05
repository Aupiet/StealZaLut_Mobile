package com.stealzalut.mobile.ui.viewmodel

import android.app.Application
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.delegate.PresetLibraryDelegate
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PresetExportTest {

    private val application: Application = mockk(relaxed = true)
    private val bridgeRepository: PythonBridgeRepository = mockk(relaxed = true)
    private val presetRepository: PresetRepository = mockk(relaxed = true)
    private val savedImageRepository: SavedImageRepository = mockk(relaxed = true)
    private val stateFlow = MutableStateFlow(StealZaLutUiState())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var delegate: PresetLibraryDelegate
    private lateinit var tempRefFile: File

    @BeforeEach
    fun setUp() {
        tempRefFile = File.createTempFile("test_ref_", ".jpg")
        tempRefFile.writeText("fake image bytes")

        delegate = PresetLibraryDelegate(
            application = application,
            bridgeRepository = bridgeRepository,
            presetRepository = presetRepository,
            savedImageRepository = savedImageRepository,
            stateFlow = stateFlow,
            scope = testScope,
            decodeSampledBitmap = { _, _ -> null },
            onTriggerEditorPreviewCallback = {}
        )
    }

    @Test
    fun `exportCubeFromPreset succeeds even when sourcePhoto is null (reference-alone LUT extraction)`() = runTest {
        val preset = ColorimetryPreset(
            id = "preset_test",
            name = "Cinematic Look",
            method = ColorTransferMethod.HM_MKL_HM,
            intensity = 1.0f,
            lutSize = LutSize.STANDARD,
            referenceImagePath = tempRefFile.absolutePath
        )

        val outCubePath = "/data/user/0/com.stealzalut/cache/test_preset.cube"
        coEvery {
            bridgeRepository.exportCube(
                srcPath = "",
                refPath = tempRefFile.absolutePath,
                lutSize = LutSize.STANDARD,
                method = ColorTransferMethod.HM_MKL_HM,
                intensity = 1.0f
            )
        } returns CubeExportResult.Success(cubePath = outCubePath, lutSize = 33, executionTimeMs = 25.0)

        var readyPath: String? = null
        delegate.exportCubeFromPreset(preset) { path ->
            readyPath = path
        }

        assertEquals(outCubePath, readyPath)
        assertNull(stateFlow.value.errorMessage)
        assertNotNull(stateFlow.value.pendingExport)
        assertEquals(ExportType.CUBE_LUT, stateFlow.value.pendingExport?.exportType)
        assertEquals(outCubePath, stateFlow.value.pendingExport?.sourcePath)

        tempRefFile.delete()
    }

    @Test
    fun `exportCubeFromPaletteImage extracts and exports 3D LUT from palette photo`() = runTest {
        stateFlow.value = StealZaLutUiState(
            palettePhotoPath = tempRefFile.absolutePath,
            lutSize = LutSize.STANDARD,
            method = ColorTransferMethod.HM_MKL_HM,
            intensity = 1.0f
        )

        val outCubePath = "/data/user/0/com.stealzalut/cache/test_palette.cube"
        coEvery {
            bridgeRepository.exportCube(
                srcPath = "",
                refPath = tempRefFile.absolutePath,
                lutSize = LutSize.STANDARD,
                method = ColorTransferMethod.HM_MKL_HM,
                intensity = 1.0f
            )
        } returns CubeExportResult.Success(cubePath = outCubePath, lutSize = 33, executionTimeMs = 30.0)

        var readyPath: String? = null
        delegate.exportCubeFromPaletteImage { path ->
            readyPath = path
        }

        assertEquals(outCubePath, readyPath)
        assertNull(stateFlow.value.errorMessage)
        assertNotNull(stateFlow.value.pendingExport)
        assertEquals(ExportType.CUBE_LUT, stateFlow.value.pendingExport?.exportType)

        tempRefFile.delete()
    }
}
