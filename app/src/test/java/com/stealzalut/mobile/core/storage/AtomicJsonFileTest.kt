package com.stealzalut.mobile.core.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class AtomicJsonFileTest {
    @Test
    fun writesAndReplacesTextWithoutLeavingTemporaryFiles() {
        val directory = Files.createTempDirectory("atomic_json_file").toFile()
        try {
            val file = File(directory, "settings.json")
            AtomicJsonFile.writeText(file, """{"version":1}""")
            AtomicJsonFile.writeText(file, """{"version":2}""")

            assertEquals("""{"version":2}""", AtomicJsonFile.readText(file))
            assertFalse(File(directory, "settings.json.tmp").exists())
            assertFalse(File(directory, "settings.json.bak").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun restoresBackupWhenTargetIsMissing() {
        val directory = Files.createTempDirectory("atomic_json_recovery").toFile()
        try {
            val file = File(directory, "manifest.json")
            File(directory, "manifest.json.bak").writeText("""{"recovered":true}""")

            assertEquals("""{"recovered":true}""", AtomicJsonFile.readText(file))
            assertTrue(file.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun quarantinePreservesTheUnreadableFile() {
        val directory = Files.createTempDirectory("atomic_json_quarantine").toFile()
        try {
            val file = File(directory, "presets.json").apply { writeText("not json") }
            AtomicJsonFile.quarantineCorrupt(file, IllegalArgumentException("Invalid JSON"))

            assertFalse(file.exists())
            assertEquals("not json", directory.listFiles().orEmpty().single().readText())
            assertTrue(directory.listFiles().orEmpty().single().name.startsWith("presets.json.corrupt_"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
