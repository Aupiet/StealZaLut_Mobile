package com.stealzalut.mobile.core.diagnostics

import android.app.Application
import android.os.Build
import android.os.Debug
import android.os.Process
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

internal class CrashReportWriter(
    private val directory: File,
    private val maxReports: Int = MAX_CRASH_REPORTS
) {
    fun write(
        timestamp: Long,
        threadName: String,
        throwable: Throwable,
        metadata: Map<String, String>,
        recentLogs: List<LogEntry>
    ): File {
        directory.mkdirs()
        val report = File(directory, "crash_${timestamp}_${sequence.incrementAndGet()}.txt")
        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()
        val content = buildString {
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT).format(Date(timestamp))}")
            appendLine("Thread: ${AppLoggerRedaction.redact(threadName)}")
            metadata.forEach { (key, value) ->
                appendLine("$key: ${AppLoggerRedaction.redact(value)}")
            }
            appendLine("Screen: unknown")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(AppLoggerRedaction.redact(stackTrace))
            appendLine()
            appendLine("Recent application logs:")
            recentLogs.takeLast(MAX_CRASH_LOG_LINES).forEach { entry ->
                append(entry.timestampMs)
                append(' ')
                append(entry.level)
                append('/')
                append(entry.tag)
                append(": ")
                appendLine(entry.message)
            }
        }
        FileOutputStream(report).use { output ->
            output.write(content.toByteArray(StandardCharsets.UTF_8))
            output.fd.sync()
        }
        rotate()
        return report
    }

    private fun rotate() {
        directory.listFiles { file -> REPORT_PATTERN.matches(file.name) }
            .orEmpty()
            .sortedByDescending { file ->
                file.name.removePrefix("crash_").substringBefore('_').toLongOrNull()
                    ?: file.lastModified()
            }
            .drop(maxReports.coerceAtLeast(1))
            .forEach { it.delete() }
    }

    companion object {
        const val MAX_CRASH_REPORTS = 5
        const val MAX_CRASH_LOG_LINES = 200
        private val sequence = AtomicLong()
        private val REPORT_PATTERN = Regex("""crash_\d+_\d+(?:\.txt|\.read)""")
    }
}

object CrashReporter {
    private const val TAG = "CrashReporter"

    fun install(application: Application) {
        try {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            val writer = CrashReportWriter(File(application.filesDir, "crash"))
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                try {
                    writer.write(
                        timestamp = System.currentTimeMillis(),
                        threadName = thread.name,
                        throwable = error,
                        metadata = collectMetadata(application),
                        recentLogs = AppLog.current()?.recentEntries(CrashReportWriter.MAX_CRASH_LOG_LINES).orEmpty()
                    )
                } catch (reportError: Throwable) {
                    AppLog.log(LogLevel.ERROR, TAG, "Could not write crash report.", reportError)
                } finally {
                    try {
                        if (previousHandler != null) {
                            previousHandler.uncaughtException(thread, error)
                        } else {
                            Process.killProcess(Process.myPid())
                            kotlin.system.exitProcess(10)
                        }
                    } catch (_: Throwable) {
                        Process.killProcess(Process.myPid())
                        kotlin.system.exitProcess(10)
                    }
                }
            }
        } catch (error: Throwable) {
            AppLog.log(LogLevel.ERROR, TAG, "Could not install crash reporter.", error)
        }
    }

    fun unreadReports(application: Application): List<File> = try {
        File(application.filesDir, "crash").listFiles { file ->
            file.name.matches(Regex("""crash_\d+_\d+\.txt"""))
        }.orEmpty().sortedByDescending(File::lastModified)
    } catch (error: Throwable) {
        AppLog.log(LogLevel.WARN, TAG, "Could not read crash reports.", error)
        emptyList()
    }

    fun markHandled(file: File) {
        try {
            val handled = File(file.parentFile, "${file.nameWithoutExtension}.read")
            if (!file.renameTo(handled)) {
                AppLog.log(LogLevel.WARN, TAG, "Could not mark crash report as handled.")
            }
        } catch (error: Throwable) {
            AppLog.log(LogLevel.WARN, TAG, "Could not mark crash report as handled.", error)
        }
    }

    fun shareIntent(application: Application, file: File): android.content.Intent? = try {
        val uri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            file
        )
        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } catch (error: Throwable) {
        AppLog.log(LogLevel.ERROR, TAG, "Could not prepare crash report sharing.", error)
        null
    }

    private fun collectMetadata(application: Application): Map<String, String> {
        val runtime = Runtime.getRuntime()
        val version = try {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        val heap = try {
            "used=${runtime.totalMemory() - runtime.freeMemory()}, max=${runtime.maxMemory()}"
        } catch (_: Throwable) {
            "unavailable"
        }
        val nativeHeap = try {
            "allocated=${Debug.getNativeHeapAllocatedSize()}, free=${Debug.getNativeHeapFreeSize()}"
        } catch (_: Throwable) {
            "unavailable"
        }
        return mapOf(
            "App version" to version,
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android SDK" to Build.VERSION.SDK_INT.toString(),
            "JVM heap bytes" to heap,
            "Native heap bytes" to nativeHeap
        )
    }
}

private object AppLoggerRedaction {
    private val pathPattern = Regex("""(?:[A-Za-z]:[\\/]|/)(?:[^\s/:]+[\\/])+[^\s/:]+""")
    private val uriPattern = Regex("""([a-zA-Z][a-zA-Z0-9+.-]*://)([^\s]+)""")

    fun redact(value: String): String {
        val uriRedacted = uriPattern.replace(value) {
            it.groupValues[1] + it.groupValues[2].substringAfterLast('/')
        }
        return pathPattern.replace(uriRedacted) {
            it.value.substringAfterLast('/').substringAfterLast('\\')
        }
    }
}
