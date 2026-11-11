package com.stealzalut.mobile.core.diagnostics

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrashReportWriterTest {
    @Test
    fun `writes crash report with stack and recent logs`() {
        val directory = Files.createTempDirectory("szl-crash-report").toFile()
        try {
            val writer = CrashReportWriter(directory)
            val report = writer.write(
                timestamp = 1234L,
                threadName = "main",
                throwable = IllegalStateException("failure"),
                metadata = mapOf("Device" to "Example"),
                recentLogs = listOf(LogEntry(1L, LogLevel.ERROR, "tag", "last event", null, emptyMap()))
            )
            val text = report.readText()
            assertTrue(text.contains("Thread: main"))
            assertTrue(text.contains("IllegalStateException: failure"))
            assertTrue(text.contains("last event"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `retains only the newest five reports`() {
        val directory = Files.createTempDirectory("szl-crash-rotation").toFile()
        try {
            val writer = CrashReportWriter(directory)
            repeat(7) { index ->
                writer.write(index.toLong() + 1L, "main", RuntimeException("failure"), emptyMap(), emptyList())
            }
            assertEquals(5, directory.listFiles()?.size)
            assertFalse(directory.listFiles().orEmpty().any { it.name.startsWith("crash_1_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
