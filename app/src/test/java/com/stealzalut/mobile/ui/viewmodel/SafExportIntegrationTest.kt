package com.stealzalut.mobile.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.cache.LruCacheManager
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.repository.SavedImageRepository
import com.stealzalut.mobile.ui.viewmodel.delegate.GradingPipelineDelegate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SafExportIntegrationTest {

    private val context: Context = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk(relaxed = true)
    private val bridgeRepository: PythonBridgeRepository = mockk(relaxed = true)
    private val savedImageRepository: SavedImageRepository = mockk(relaxed = true)
    private val lruCacheManager: LruCacheManager = mockk(relaxed = true)
    private val stateFlow = MutableStateFlow(StealZaLutUiState())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var delegate: GradingPipelineDelegate
    private lateinit var tempCubeFile: File
    private lateinit var tempEmptyCubeFile: File
    private lateinit var tempTruncatedCubeFile: File

    @BeforeEach
    fun setUp() {
        every { context.contentResolver } returns contentResolver
        every { bridgeRepository.lruCacheManager } returns lruCacheManager

        // Create a valid LUT file with LUT_3D_SIZE 2 (8 triplets)
        tempCubeFile = File.createTempFile("valid_lut_", ".cube").apply {
            writeText(
                """
                # Created by StealZaLUT
                TITLE ValidLUT
                LUT_3D_SIZE 2
                DOMAIN_MIN 0.0 0.0 0.0
                DOMAIN_MAX 1.0 1.0 1.0
                0.0 0.0 0.0
                1.0 0.0 0.0
                0.0 1.0 0.0
                1.0 1.0 0.0
                0.0 0.0 1.0
                1.0 0.0 1.0
                0.0 1.0 1.0
                1.0 1.0 1.0
                """.trimIndent()
            )
        }

        // Empty 0-byte cube file
        tempEmptyCubeFile = File.createTempFile("empty_lut_", ".cube")

        // Truncated cube file (specifies LUT_3D_SIZE 2 but only provides 3 lines)
        tempTruncatedCubeFile = File.createTempFile("trunc_lut_", ".cube").apply {
            writeText(
                """
                # Created by StealZaLUT
                TITLE Truncated
                LUT_3D_SIZE 2
                0.0 0.0 0.0
                1.0 0.0 0.0
                0.0 1.0 0.0
                """.trimIndent()
            )
        }

        delegate = GradingPipelineDelegate(
            context = context,
            bridgeRepository = bridgeRepository,
            savedImageRepository = savedImageRepository,
            stateFlow = stateFlow,
            scope = testScope,
            decodeSampledBitmap = { _, _ -> null },
            ioDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        tempCubeFile.delete()
        tempEmptyCubeFile.delete()
        tempTruncatedCubeFile.delete()
    }

    @Test
    fun `savePendingExportToUri succeeds when writing and verifying valid CUBE_LUT`() = runTest {
        val destUri = mockk<Uri>(relaxed = true)
        val writtenBytes = ByteArrayOutputStream()

        every { contentResolver.openOutputStream(destUri, "wt") } returns writtenBytes
        every { contentResolver.openOutputStream(destUri) } returns writtenBytes
        every { contentResolver.openInputStream(destUri) } answers {
            ByteArrayInputStream(writtenBytes.toByteArray())
        }

        stateFlow.value = StealZaLutUiState(
            pendingExport = PendingExport(
                sourcePath = tempCubeFile.canonicalPath,
                suggestedFileName = "MyLUT.cube",
                exportType = ExportType.CUBE_LUT
            )
        )

        delegate.savePendingExportToUri(destUri).join()

        // Verify success state
        val state = stateFlow.value
        assertNull(state.pendingExport)
        assertNull(state.errorMessage)
        assertNotNull(state.userMessage)
        assertTrue(writtenBytes.size() > 0)
        assertEquals(tempCubeFile.length(), writtenBytes.size().toLong())
    }

    @Test
    fun `savePendingExportToUri rejects and reports error for 0-byte source file`() = runTest {
        val destUri = mockk<Uri>(relaxed = true)

        stateFlow.value = StealZaLutUiState(
            pendingExport = PendingExport(
                sourcePath = tempEmptyCubeFile.canonicalPath,
                suggestedFileName = "Empty.cube",
                exportType = ExportType.CUBE_LUT
            )
        )

        delegate.savePendingExportToUri(destUri).join()

        val state = stateFlow.value
        assertNull(state.pendingExport)
        assertNotNull(state.errorMessage)
        assertNull(state.userMessage)
    }

    @Test
    fun `savePendingExportToUri rejects truncated cube stream during verification`() = runTest {
        val destUri = mockk<Uri>(relaxed = true)
        val writtenBytes = ByteArrayOutputStream()

        every { contentResolver.openOutputStream(destUri, "wt") } returns writtenBytes
        every { contentResolver.openOutputStream(destUri) } returns writtenBytes
        every { contentResolver.openInputStream(destUri) } answers {
            ByteArrayInputStream(writtenBytes.toByteArray())
        }

        stateFlow.value = StealZaLutUiState(
            pendingExport = PendingExport(
                sourcePath = tempTruncatedCubeFile.canonicalPath,
                suggestedFileName = "Trunc.cube",
                exportType = ExportType.CUBE_LUT
            )
        )

        delegate.savePendingExportToUri(destUri).join()

        val state = stateFlow.value
        assertNull(state.pendingExport)
        assertNotNull(state.errorMessage)
        assertNull(state.userMessage)
    }

    @Test
    fun `savePendingExportToUri reports error when destination output stream cannot be opened`() = runTest {
        val destUri = mockk<Uri>(relaxed = true)

        every { contentResolver.openOutputStream(destUri, "wt") } throws IOException("Access denied")
        every { contentResolver.openOutputStream(destUri) } returns null

        stateFlow.value = StealZaLutUiState(
            pendingExport = PendingExport(
                sourcePath = tempCubeFile.canonicalPath,
                suggestedFileName = "MyLUT.cube",
                exportType = ExportType.CUBE_LUT
            )
        )

        delegate.savePendingExportToUri(destUri).join()

        val state = stateFlow.value
        assertNull(state.pendingExport)
        assertNotNull(state.errorMessage)
        assertNull(state.userMessage)
    }

    @Test
    fun `cancelPendingExport clears pending export without displaying success message`() = runTest {
        stateFlow.value = StealZaLutUiState(
            pendingExport = PendingExport(
                sourcePath = tempCubeFile.canonicalPath,
                suggestedFileName = "MyLUT.cube",
                exportType = ExportType.CUBE_LUT
            ),
            userMessage = null
        )

        delegate.cancelPendingExport()

        assertNull(stateFlow.value.pendingExport)
        assertNull(stateFlow.value.userMessage)
        assertNull(stateFlow.value.errorMessage)
    }

    @Test
    fun `setSourceImage protects file in LRU cache and replaces previous file cleanly`() = runTest {
        val uri1 = mockk<Uri>(relaxed = true)
        val uri2 = mockk<Uri>(relaxed = true)

        val tempPhoto1 = File.createTempFile("test_photo_1_", ".jpg").apply { writeText("photo1") }
        val tempPhoto2 = File.createTempFile("test_photo_2_", ".jpg").apply { writeText("photo2") }

        coEvery { bridgeRepository.cacheImageFromUri(uri1, any()) } returns Result.success(tempPhoto1)
        coEvery { bridgeRepository.cacheImageFromUri(uri2, any()) } returns Result.success(tempPhoto2)

        delegate.setSourceImage(uri1)

        assertEquals(tempPhoto1.canonicalPath, stateFlow.value.sourcePath)
        assertTrue(tempPhoto1.exists())

        // Now select a second photo: the first file should be unprotected and deleted
        delegate.setSourceImage(uri2)

        assertEquals(tempPhoto2.canonicalPath, stateFlow.value.sourcePath)
        assertTrue(tempPhoto2.exists())
        assertFalse(tempPhoto1.exists())

        tempPhoto2.delete()
    }

    @Test
    fun `exportCube respects concurrency guard when already exporting`() = runTest {
        stateFlow.value = StealZaLutUiState(
            isExportingCube = true,
            sourcePath = tempCubeFile.canonicalPath,
            customReferencePath = tempCubeFile.canonicalPath
        )

        var readyCalled = false
        delegate.exportCube { readyCalled = true }

        assertFalse(readyCalled)
    }
}
