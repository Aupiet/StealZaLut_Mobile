package com.stealzalut.mobile.data.repository

import android.content.Context
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.LutSize
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

class PresetRepositoryTest {

    private lateinit var tempDir: File
    private val context: Context = mockk()
    private lateinit var repository: PresetRepository

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("preset_test_dir").toFile()
        every { context.filesDir } returns tempDir
        repository = PresetRepository(context)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `getAllPresets returns empty list initially as built-in themes are disabled`() = runTest {
        val presets = repository.getAllPresets()
        assertTrue(presets.isEmpty())
        assertTrue(repository.builtInPresets.isEmpty())
    }

    @Test
    fun `savePreset adds a new preset and persists it`() = runTest {
        val customPreset = ColorimetryPreset(
            id = "custom_1",
            name = "Vintage 80s Vibe",
            method = ColorTransferMethod.REINHARD,
            intensity = 0.9f,
            lutSize = LutSize.STANDARD,
            paletteHexes = listOf("#AA1122", "#334455")
        )

        val result = repository.savePreset(customPreset)
        assertTrue(result.isSuccess)

        // Reload to verify persistence
        val allPresets = repository.getAllPresets()
        val found = allPresets.firstOrNull { it.id == "custom_1" }
        assertNotNull(found)
        assertEquals("Vintage 80s Vibe", found?.name)
        assertEquals(ColorTransferMethod.REINHARD, found?.method)
        assertEquals(0.9f, found?.intensity)
        assertEquals(listOf("#AA1122", "#334455"), found?.paletteHexes)
    }

    @Test
    fun `deletePreset removes a user preset correctly`() = runTest {
        val customPreset = ColorimetryPreset(
            id = "to_delete_id",
            name = "Temporary Theme",
            method = ColorTransferMethod.HM
        )

        repository.savePreset(customPreset)
        assertTrue(repository.getAllPresets().any { it.id == "to_delete_id" })

        val deleteResult = repository.deletePreset("to_delete_id")
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.getAllPresets()
        assertFalse(remaining.any { it.id == "to_delete_id" })
        assertTrue(remaining.isEmpty())
    }
}
