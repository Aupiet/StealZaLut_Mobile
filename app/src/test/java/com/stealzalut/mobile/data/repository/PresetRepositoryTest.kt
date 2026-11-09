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
    fun `getAllPresets returns empty list initially`() = runTest {
        val presets = repository.getAllPresets()
        assertTrue(presets.isEmpty())
    }

    @Test
    fun `legacy preset with builtin prefix is safely loaded and preserved`() = runTest {
        val legacyPreset = ColorimetryPreset(
            id = "builtin_blade_runner",
            name = "Blade Runner Legacy",
            method = ColorTransferMethod.HM_MKL_HM,
            intensity = 1.0f,
            lutSize = LutSize.STANDARD,
            paletteHexes = listOf("#1E3A5F", "#F6AD55")
        )
        val saveResult = repository.savePreset(legacyPreset)
        assertTrue(saveResult.isSuccess)

        val reloaded = repository.getAllPresets()
        val found = reloaded.firstOrNull { it.id == "builtin_blade_runner" }
        assertNotNull(found)
        assertEquals("Blade Runner Legacy", found?.name)
        assertEquals("builtin_blade_runner", found?.id)
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

    @Test
    fun `savePreset with same id updates existing preset rather than duplicating`() = runTest {
        val preset = ColorimetryPreset(
            id = "preset_id_1",
            name = "Original Name",
            method = ColorTransferMethod.REINHARD
        )
        repository.savePreset(preset)
        assertEquals(1, repository.getAllPresets().size)

        // Save again with same ID but modified intensity
        val updated = preset.copy(name = "Updated Name", intensity = 0.5f)
        repository.savePreset(updated)

        val all = repository.getAllPresets()
        assertEquals(1, all.size, "Should not duplicate preset with same id")
        assertEquals("Updated Name", all.first().name)
        assertEquals(0.5f, all.first().intensity)
    }

    @Test
    fun `savePreset with identical content within deduplication window prevents duplicate entry`() = runTest {
        val preset1 = ColorimetryPreset(
            id = "uuid_1",
            name = "Same Look",
            method = ColorTransferMethod.HM_MKL_HM,
            paletteHexes = listOf("#112233", "#445566")
        )
        val preset2 = ColorimetryPreset(
            id = "uuid_2", // Different ID (e.g. rapid double tap)
            name = "Same Look",
            method = ColorTransferMethod.HM_MKL_HM,
            paletteHexes = listOf("#112233", "#445566")
        )

        repository.savePreset(preset1)
        repository.savePreset(preset2)

        val all = repository.getAllPresets()
        assertEquals(1, all.size, "Rapid identical preset saves should be deduplicated")
    }
}
