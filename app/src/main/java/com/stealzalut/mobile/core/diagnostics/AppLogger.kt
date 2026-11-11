package com.stealzalut.mobile.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.net.URI
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

    init {
        require(capacity > 0) { "Logger capacity must be positive." }
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

    private companion object {
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
