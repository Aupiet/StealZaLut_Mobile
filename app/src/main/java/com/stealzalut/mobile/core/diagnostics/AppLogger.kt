package com.stealzalut.mobile.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestampMs: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String?,
    val fields: Map<String, String>
)

interface AppLogger {
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        fields: Map<String, String> = emptyMap()
    )

    fun recentEntries(limit: Int = DEFAULT_RING_BUFFER_CAPACITY): List<LogEntry>

    companion object {
        const val DEFAULT_RING_BUFFER_CAPACITY = 500
    }
}

class AndroidAppLogger(
    context: Context,
    private val capacity: Int = AppLogger.DEFAULT_RING_BUFFER_CAPACITY
) : AppLogger {
    private val isDebuggable =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val entries = ArrayDeque<LogEntry>(capacity)
    private val logFile = AppLog.persistedLogFile(context)

    init {
        require(capacity > 0) { "Logger capacity must be positive." }
        loadPersistedEntries()
    }

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        fields: Map<String, String>
    ) {
        safely {
            if (!isDebuggable && level < LogLevel.WARN) return@safely
            val safeEntry = LogEntry(
                timestampMs = System.currentTimeMillis(),
                level = level,
                tag = redact(tag),
                message = redact(message),
                throwable = throwable?.stackTraceToString()?.let(::redact),
                fields = fields.mapValues { redact(it.value) }
            )
            synchronized(entries) {
                if (entries.size == capacity) entries.removeFirst()
                entries.addLast(safeEntry)
                try {
                    persist(safeEntry)
                } catch (error: Exception) {
                    Log.e(LOG_TAG, "Could not persist application event.", error)
                }
            }
            val text = buildString {
                append(safeEntry.message)
                safeEntry.fields.forEach { (key, value) ->
                    append(" ")
                    append(key)
                    append("=")
                    append(value)
                }
            }
            when (level) {
                LogLevel.DEBUG -> Log.d(safeEntry.tag, text, throwable)
                LogLevel.INFO -> Log.i(safeEntry.tag, text, throwable)
                LogLevel.WARN -> Log.w(safeEntry.tag, text, throwable)
                LogLevel.ERROR -> Log.e(safeEntry.tag, text, throwable)
            }
        }
    }

    override fun recentEntries(limit: Int): List<LogEntry> = safely(emptyList()) {
        synchronized(entries) { entries.toList().takeLast(limit.coerceAtLeast(0)) }
    }

    private inline fun <T> safely(fallback: T, action: () -> T): T =
        try {
            action()
        } catch (_: Throwable) {
            fallback
        }

    private inline fun safely(action: () -> Unit) {
        try {
            action()
        } catch (_: Throwable) {
            // Diagnostics must not cause application failures.
        }
    }

    private fun redact(value: String): String {
        val uriRedacted = URI_PATTERN.replace(value) { match ->
            match.groupValues[1] + match.groupValues[2].substringAfterLast('/')
        }
        return PATH_PATTERN.replace(uriRedacted) { match ->
            match.value.substringAfterLast(File.separatorChar).substringAfterLast('/')
        }
    }

    private fun loadPersistedEntries() {
        try {
            recoverJournal()
            if (!logFile.isFile) return
            val loaded = logFile.readLines(StandardCharsets.UTF_8)
                .takeLast(capacity)
                .mapNotNull(::decodeEntry)
            synchronized(entries) {
                entries.addAll(loaded)
            }
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Could not load persisted application events.", error)
        }
    }

    private fun persist(entry: LogEntry) {
        val directory = logFile.parentFile
            ?: throw IOException("The diagnostics directory is unavailable.")
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create the diagnostics directory.")
        }
        FileOutputStream(logFile, true).use { output ->
            output.write(encodeEntry(entry).toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
            output.fd.sync()
        }
        if (logFile.length() > MAX_JOURNAL_BYTES) compactJournal(directory)
    }

    private fun compactJournal(directory: File) {
        val temporary = File(directory, "$JOURNAL_NAME.tmp")
        val backup = File(directory, "$JOURNAL_NAME.backup")
        FileOutputStream(temporary, false).use { output ->
            synchronized(entries) {
                entries.forEach { entry ->
                    output.write(encodeEntry(entry).toByteArray(StandardCharsets.UTF_8))
                    output.write('\n'.code)
                }
            }
            output.fd.sync()
        }
        if (backup.exists() && !backup.delete()) {
            temporary.delete()
            throw IOException("Could not remove the previous diagnostics journal backup.")
        }
        if (logFile.exists() && !logFile.renameTo(backup)) {
            temporary.delete()
            throw IOException("Could not rotate the diagnostics journal.")
        }
        if (!temporary.renameTo(logFile)) {
            backup.renameTo(logFile)
            temporary.delete()
            throw IOException("Could not replace the diagnostics journal.")
        }
        backup.delete()
    }

    private fun recoverJournal() {
        val backup = File(logFile.parentFile, "$JOURNAL_NAME.backup")
        if (!logFile.exists() && backup.exists() && !backup.renameTo(logFile)) {
            throw IOException("Could not recover the diagnostics journal.")
        }
        if (logFile.exists() && backup.exists()) backup.delete()
        if (logFile.isFile && logFile.length() > 0L) {
            RandomAccessFile(logFile, "rw").use { journal ->
                journal.seek(journal.length() - 1)
                if (journal.readByte() != '\n'.code.toByte()) {
                    var position = journal.length() - 1
                    while (position >= 0) {
                        journal.seek(position)
                        if (journal.readByte() == '\n'.code.toByte()) break
                        position--
                    }
                    journal.setLength(position + 1)
                    journal.fd.sync()
                }
            }
        }
    }

    private fun encodeEntry(entry: LogEntry): String = buildString {
        append(entry.timestampMs)
        append('\t')
        append(entry.level.name)
        append('\t')
        append(escape(entry.tag))
        append('\t')
        append(escape(entry.message))
        append('\t')
        append(escape(entry.throwable.orEmpty()))
        entry.fields.forEach { (key, value) ->
            append('\t')
            append(escape(key))
            append('\t')
            append(escape(value))
        }
    }.take(MAX_JOURNAL_LINE_CHARS)

    private fun decodeEntry(line: String): LogEntry? {
        val columns = line.split('\t')
        if (columns.size < 5) return null
        val timestamp = columns[0].toLongOrNull() ?: return null
        val level = runCatching { LogLevel.valueOf(columns[1]) }.getOrNull() ?: return null
        val fields = columns.drop(5).chunked(2)
            .filter { it.size == 2 }
            .associate { (key, value) -> unescape(key) to unescape(value) }
        return LogEntry(
            timestampMs = timestamp,
            level = level,
            tag = unescape(columns[2]),
            message = unescape(columns[3]),
            throwable = unescape(columns[4]).ifEmpty { null },
            fields = fields
        )
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun unescape(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    '\\' -> result.append('\\')
                    't' -> result.append('\t')
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    else -> result.append('\\').append(value[index + 1])
                }
                index += 2
            } else {
                result.append(character)
                index++
            }
        }
        return result.toString()
    }

    companion object {
        private const val LOG_TAG = "AppLogger"
        internal const val JOURNAL_NAME = "app-events.log"
        internal const val MAX_JOURNAL_BYTES = 512 * 1024L
        private const val MAX_JOURNAL_LINE_CHARS = 16 * 1024
        val URI_PATTERN = Regex("""([a-zA-Z][a-zA-Z0-9+.-]*://)([^\s]+)""")
        val PATH_PATTERN = Regex("""(?:[A-Za-z]:[\\/]|/)(?:[^\\s/:]+[\\/])+[^\\s/:]+""")
    }
}

object AppLog {
    @Volatile
    private var logger: AppLogger? = null

    fun initialize(context: Context) {
        if (logger == null) {
            synchronized(this) {
                if (logger == null) logger = AndroidAppLogger(context.applicationContext)
            }
        }
    }

    fun current(): AppLogger? = logger

    fun persistedLogFile(context: Context): File =
        File(File(context.filesDir, "diagnostics"), AndroidAppLogger.JOURNAL_NAME)

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        fields: Map<String, String> = emptyMap()
    ) {
        logger?.log(level, tag, message, throwable, fields)
    }
}
