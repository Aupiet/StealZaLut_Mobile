package com.stealzalut.mobile.data.repository

import android.content.Context
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.model.EditorParamsSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ProjectRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var context: Context
    private lateinit var repository: ProjectRepository

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("szl_projects_test").toFile()
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir
        repository = ProjectRepository(context)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `create and load project lifecycle persists all parameters`() = runTest {
        val dummySource = File(tempDir, "temp_photo.jpg").apply { writeText("fake-image-bytes") }
        val snapshot = EditorParamsSnapshot(
            exposure = 0.75f,
            contrast = 0.30f,
            cropRegion = CropRegion(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f, angle = 2.5f)
        )

        val project = repository.createProject(
            sourceFile = dummySource,
            name = "Test Landscape Project",
            snapshot = snapshot
        )

        assertNotNull(project.id)
        assertEquals("Test Landscape Project", project.name)
        assertTrue(File(project.sourceImagePath).exists())

        // Retrieve all
        val all = repository.getAllProjects()
        assertEquals(1, all.size)
        assertEquals(project.id, all[0].id)
        assertEquals(0.75f, all[0].snapshot.exposure)
        assertEquals(0.30f, all[0].snapshot.contrast)
        assertEquals(2.5f, all[0].snapshot.cropRegion.angle)

        // Retrieve single
        val single = repository.getProject(project.id)
        assertNotNull(single)
        assertEquals(project.id, single?.id)
        assertEquals("Test Landscape Project", single?.name)
    }

    @Test
    fun `saveProject updates parameters and timestamp`() = runTest {
        val dummySource = File(tempDir, "source.jpg").apply { writeText("img") }
        val project = repository.createProject(dummySource, "My Project")

        val updated = project.copy(
            name = "My Project v2",
            updatedAt = project.updatedAt + 5000L,
            snapshot = project.snapshot.copy(exposure = -1.2f)
        )
        repository.saveProject(updated)

        val reloaded = repository.getProject(project.id)
        assertNotNull(reloaded)
        assertEquals("My Project v2", reloaded?.name)
        assertEquals(-1.2f, reloaded?.snapshot?.exposure)
    }

    @Test
    fun `renameProject modifies name correctly`() = runTest {
        val dummySource = File(tempDir, "source.jpg").apply { writeText("img") }
        val project = repository.createProject(dummySource, "Original Name")

        val success = repository.renameProject(project.id, "Renamed Name")
        assertTrue(success)

        val reloaded = repository.getProject(project.id)
        assertEquals("Renamed Name", reloaded?.name)
    }

    @Test
    fun `duplicateProject creates independent copy with new id`() = runTest {
        val dummySource = File(tempDir, "source.jpg").apply { writeText("original-image") }
        val dummyPreview = File(tempDir, "preview.jpg").apply { writeText("original-preview") }
        val project = repository.createProject(
            sourceFile = dummySource,
            name = "Master",
            snapshot = EditorParamsSnapshot(saturation = 0.5f),
            previewFile = dummyPreview
        )

        val duplicate = repository.duplicateProject(project.id)
        assertNotNull(duplicate)
        assertTrue(duplicate!!.id != project.id)
        assertEquals("Copie de Master", duplicate.name)
        assertEquals(0.5f, duplicate.snapshot.saturation)
        assertTrue(File(duplicate.sourceImagePath).exists())
        assertTrue(duplicate.sourceImagePath != project.sourceImagePath)

        val all = repository.getAllProjects()
        assertEquals(2, all.size)
    }

    @Test
    fun `deleteProject removes directory and cleans manifest`() = runTest {
        val dummySource = File(tempDir, "source.jpg").apply { writeText("img") }
        val project = repository.createProject(dummySource, "To Delete")

        assertEquals(1, repository.getAllProjects().size)
        val success = repository.deleteProject(project.id)
        assertTrue(success)

        assertEquals(0, repository.getAllProjects().size)
        assertNull(repository.getProject(project.id))
    }

    @Test
    fun `corrupted project json is skipped gracefully without crashing`() = runTest {
        val dummySource = File(tempDir, "valid.jpg").apply { writeText("valid") }
        val valid = repository.createProject(dummySource, "Valid Project")

        // Create corrupt directory
        val corruptDir = File(File(tempDir, "projects"), "corrupt_proj").apply { mkdirs() }
        File(corruptDir, "project.json").writeText("{ this is definitely not valid json }")

        val all = repository.getAllProjects()
        // Should only return the 1 valid project
        assertEquals(1, all.size)
        assertEquals(valid.id, all[0].id)
    }
}
