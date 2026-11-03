package com.stealzalut.mobile.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class SavedImageRepositoryTest {

    private lateinit var tempDir: File
    private val context: Context = mockk()
    private lateinit var repository: SavedImageRepository

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("saved_image_test_dir").toFile()
        every { context.filesDir } returns tempDir
        repository = SavedImageRepository(context)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `getSavedImages returns empty list initially`() = runTest {
        val images = repository.getSavedImages()
        assertTrue(images.isEmpty())
    }

    @Test
    fun `recordGradedImage saves metadata and persists it`() = runTest {
        val fakeSource = File(tempDir, "temp_src.jpg").apply {
            writeText("fake image bytes")
        }

        val result = repository.recordGradedImage(
            sourceFilePath = fakeSource.absolutePath,
            width = 1920,
            height = 1080,
            themeName = "Cyberpunk Neo",
            method = "reinhard",
            intensity = 0.85f,
            paletteHexes = listOf("#FF0055", "#00FFAA")
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()
        assertNotNull(saved)
        assertEquals("Cyberpunk Neo", saved?.themeName)
        assertEquals(1920, saved?.width)
        assertEquals(1080, saved?.height)
        assertEquals("reinhard", saved?.method)
        assertEquals(0.85f, saved?.intensity)
        assertEquals(listOf("#FF0055", "#00FFAA"), saved?.paletteHexes)

        val allImages = repository.getSavedImages()
        assertEquals(1, allImages.size)
        assertEquals(saved?.id, allImages.first().id)
        assertEquals("reinhard", allImages.first().method)
        assertEquals(listOf("#FF0055", "#00FFAA"), allImages.first().paletteHexes)
        assertTrue(File(allImages.first().filePath).exists())
    }

    @Test
    fun `deleteSavedImage removes image from manifest and disk`() = runTest {
        val fakeSource = File(tempDir, "temp_src2.jpg").apply {
            writeText("fake image bytes 2")
        }

        val recordResult = repository.recordGradedImage(
            sourceFilePath = fakeSource.absolutePath,
            width = 800,
            height = 600,
            themeName = "Temporary Look"
        )
        val imageId = recordResult.getOrThrow().id

        assertEquals(1, repository.getSavedImages().size)

        val deleteResult = repository.deleteSavedImage(imageId)
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.getSavedImages()
        assertFalse(remaining.any { it.id == imageId })
        assertTrue(remaining.isEmpty())
    }
}
