package com.stealzalut.mobile.core.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppLoggerPersistenceTest {
    @Test
    fun `restores persisted events after logger is recreated`() {
        val directory = Files.createTempDirectory("szl-app-logs").toFile()
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory
            every { context.applicationInfo } returns ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_DEBUGGABLE
            }

            val original = AndroidAppLogger(context)
            original.log(
                level = LogLevel.ERROR,
                tag = "test",
                message = "event\tbefore\ncrash",
                throwable = IllegalStateException("failure"),
                fields = mapOf("phase" to "loading\timage")
            )

            val restored = AndroidAppLogger(context).recentEntries().single()

            assertEquals(LogLevel.ERROR, restored.level)
            assertEquals("event\tbefore\ncrash", restored.message)
            assertEquals("java.lang.IllegalStateException: failure", restored.throwable?.lineSequence()?.first())
            assertEquals(mapOf("phase" to "loading\timage"), restored.fields)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `keeps only the configured number of persisted events`() {
        val directory = Files.createTempDirectory("szl-app-log-capacity").toFile()
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory
            every { context.applicationInfo } returns ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_DEBUGGABLE
            }

            val original = AndroidAppLogger(context, capacity = 2)
            repeat(3) { index ->
                original.log(LogLevel.INFO, "test", "event-$index")
            }

            assertEquals(
                listOf("event-1", "event-2"),
                AndroidAppLogger(context, capacity = 2).recentEntries().map { it.message }
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `compacts oversized journals and preserves the newest events`() {
        val directory = Files.createTempDirectory("szl-app-log-compaction").toFile()
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory
            every { context.applicationInfo } returns ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_DEBUGGABLE
            }

            val logger = AndroidAppLogger(context, capacity = 20)
            val message = "x".repeat(4_000)
            repeat(160) { index ->
                logger.log(LogLevel.INFO, "test", "$index$message")
            }

            val journal = AppLog.persistedLogFile(context)
            assertTrue(journal.length() < AndroidAppLogger.MAX_JOURNAL_BYTES)
            assertEquals(
                "159$message",
                AndroidAppLogger(context, capacity = 20).recentEntries().last().message
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `discards an incomplete trailing record before appending new events`() {
        val directory = Files.createTempDirectory("szl-app-log-recovery").toFile()
        try {
            val context = mockk<Context>()
            every { context.filesDir } returns directory
            every { context.applicationInfo } returns ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_DEBUGGABLE
            }

            AndroidAppLogger(context).log(LogLevel.INFO, "test", "before-crash")
            AppLog.persistedLogFile(context).appendText("incomplete")
            val restored = AndroidAppLogger(context)
            restored.log(LogLevel.INFO, "test", "after-restart")

            assertEquals(
                listOf("before-crash", "after-restart"),
                AndroidAppLogger(context).recentEntries().map { it.message }
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}
