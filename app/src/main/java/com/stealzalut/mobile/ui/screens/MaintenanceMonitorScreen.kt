package com.stealzalut.mobile.ui.screens

import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.chaquo.python.Python
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.core.diagnostics.CrashReporter
import com.stealzalut.mobile.core.diagnostics.LogEntry
import com.stealzalut.mobile.core.diagnostics.LogLevel
import com.stealzalut.mobile.data.cache.LruCacheManager
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private data class MaintenanceFile(
    val file: File,
    val isCrashReport: Boolean
)

private data class MaintenanceSnapshot(
    val appVersion: String,
    val device: String,
    val androidVersion: String,
    val javaHeap: String,
    val nativeHeap: String,
    val availableMemory: String,
    val cacheSize: String,
    val freeStorage: String,
    val pythonStatus: String,
    val files: List<MaintenanceFile>,
    val logs: List<LogEntry>
)

@Composable
fun MaintenanceMonitorScreen(
    viewModel: StealZaLutViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as? Application
    val scope = rememberCoroutineScope()
    var refreshToken by remember { mutableStateOf(0) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showClearCacheConfirmation by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val snapshot by produceState<MaintenanceSnapshot?>(initialValue = null, refreshToken) {
        value = withContext(Dispatchers.IO) { collectMaintenanceSnapshot(context) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { importMaintenanceFile(context, uri) }
                    feedback = context.getString(R.string.maintenance_import_success)
                    refreshToken++
                } catch (error: Exception) {
                    AppLog.log(LogLevel.ERROR, "Maintenance", "Could not import diagnostic file.", error)
                    feedback = context.getString(R.string.maintenance_import_failed)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(AppDimens.SpacingSpacious),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AppDimens.DialogElevation
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(AppDimens.SpacingNormal),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.maintenance_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.maintenance_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    OutlinedButton(
                        onClick = { refreshToken++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.maintenance_refresh))
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.maintenance_import))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentSnapshot = snapshot
                            if (application == null || currentSnapshot == null) {
                                feedback = context.getString(R.string.maintenance_share_failed)
                            } else {
                                scope.launch {
                                    try {
                                        val report = withContext(Dispatchers.IO) {
                                            writeDiagnosticsSnapshot(application, currentSnapshot)
                                        }
                                        shareMaintenanceFile(context, application, report)
                                    } catch (error: Exception) {
                                        AppLog.log(LogLevel.ERROR, "Maintenance", "Could not create diagnostics summary.", error)
                                        feedback = context.getString(R.string.maintenance_share_failed)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.maintenance_share_summary))
                    }
                    OutlinedButton(
                        onClick = { showClearCacheConfirmation = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.maintenance_clear_preview_cache))
                    }
                }

                feedback?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.maintenance_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                snapshot?.let { currentSnapshot ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                    ) {
                        item {
                            MaintenanceSection(title = stringResource(R.string.maintenance_health_title)) {
                                MetricRow(stringResource(R.string.maintenance_app), currentSnapshot.appVersion)
                                MetricRow(stringResource(R.string.maintenance_device), currentSnapshot.device)
                                MetricRow(stringResource(R.string.maintenance_android), currentSnapshot.androidVersion)
                                MetricRow(stringResource(R.string.maintenance_python), currentSnapshot.pythonStatus)
                                MetricRow(stringResource(R.string.maintenance_java_heap), currentSnapshot.javaHeap)
                                MetricRow(stringResource(R.string.maintenance_native_heap), currentSnapshot.nativeHeap)
                                MetricRow(stringResource(R.string.maintenance_available_memory), currentSnapshot.availableMemory)
                                MetricRow(stringResource(R.string.maintenance_cache), currentSnapshot.cacheSize)
                                MetricRow(stringResource(R.string.maintenance_free_storage), currentSnapshot.freeStorage)
                            }
                        }
                        item {
                            MaintenanceSection(
                                title = stringResource(
                                    R.string.maintenance_files_title,
                                    currentSnapshot.files.count { it.isCrashReport },
                                    currentSnapshot.files.count { !it.isCrashReport }
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.maintenance_native_crash_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (currentSnapshot.files.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.maintenance_files_empty),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        items(currentSnapshot.files, key = { it.file.absolutePath }) { item ->
                            MaintenanceFileCard(
                                item = item,
                                onView = {
                                    scope.launch {
                                        try {
                                            selectedFileContent = withContext(Dispatchers.IO) {
                                                readPreviewText(item.file)
                                            }
                                            selectedFile = item.file
                                        } catch (error: IOException) {
                                            AppLog.log(LogLevel.ERROR, "Maintenance", "Could not read diagnostic file.", error)
                                            feedback = context.getString(R.string.maintenance_read_failed)
                                        }
                                    }
                                },
                                onShare = {
                                    if (application != null) {
                                        try {
                                            shareMaintenanceFile(context, application, item.file)
                                        } catch (error: Exception) {
                                            AppLog.log(LogLevel.ERROR, "Maintenance", "Could not share diagnostic file.", error)
                                            feedback = context.getString(R.string.maintenance_share_failed)
                                        }
                                    }
                                },
                                onDelete = {
                                    fileToDelete = item.file
                                }
                            )
                        }
                        item {
                            MaintenanceSection(title = stringResource(R.string.maintenance_recent_events)) {
                                if (currentSnapshot.logs.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.maintenance_events_empty),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    currentSnapshot.logs.asReversed().forEach { entry ->
                                        Text(
                                            text = "${DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(entry.timestampMs))} · ${entry.level} · ${entry.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: Text(stringResource(R.string.maintenance_loading))
            }
        }
    }

    selectedFile?.let { file ->
        AlertDialog(
            onDismissRequest = {
                selectedFile = null
                selectedFileContent = null
            },
            title = { Text(file.name) },
            text = {
                Text(
                    text = selectedFileContent.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 20,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (application != null) {
                        try {
                            shareMaintenanceFile(context, application, file)
                        } catch (error: Exception) {
                            AppLog.log(LogLevel.ERROR, "Maintenance", "Could not share diagnostic file.", error)
                            feedback = context.getString(R.string.maintenance_share_failed)
                        }
                    }
                }) {
                    Text(stringResource(R.string.maintenance_share))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedFile = null
                    selectedFileContent = null
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showClearCacheConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmation = false },
            title = { Text(stringResource(R.string.maintenance_clear_cache_title)) },
            text = { Text(stringResource(R.string.maintenance_clear_cache_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirmation = false
                    scope.launch {
                        val cleared = viewModel.clearPreviewCache()
                        refreshToken++
                        feedback = context.getString(
                            if (cleared) R.string.maintenance_cache_cleared
                            else R.string.maintenance_cache_clear_failed
                        )
                    }
                }) {
                    Text(stringResource(R.string.maintenance_clear_preview_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.maintenance_delete_title)) },
            text = { Text(stringResource(R.string.maintenance_delete_message, file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    fileToDelete = null
                    scope.launch {
                        val deleted = withContext(Dispatchers.IO) { file.delete() }
                        feedback = context.getString(
                            if (deleted) R.string.maintenance_delete_success
                            else R.string.maintenance_delete_failed
                        )
                        if (deleted) refreshToken++
                    }
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun MaintenanceSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MaintenanceFileCard(
    item: MaintenanceFile,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
        ) {
            Text(item.file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${formatFileSize(item.file.length())} · ${DateFormat.getDateTimeInstance().format(Date(item.file.lastModified()))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                TextButton(onClick = onView) { Text(stringResource(R.string.maintenance_view)) }
                TextButton(onClick = onShare) { Text(stringResource(R.string.maintenance_share)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
            }
        }
    }
}

private fun collectMaintenanceSnapshot(context: Context): MaintenanceSnapshot {
    val application = context.applicationContext as? Application
    val packageInfo = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (error: Exception) {
        AppLog.log(LogLevel.WARN, "Maintenance", "Could not read app version.", error)
        null
    }
    val runtime = Runtime.getRuntime()
    val memoryInfo = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memoryInfo)

    val crashFiles = application?.let(CrashReporter::allReports).orEmpty().map {
        MaintenanceFile(it, isCrashReport = true)
    }
    val importedFiles = File(context.filesDir, IMPORT_DIRECTORY).listFiles()
        .orEmpty()
        .filter { it.isFile }
        .map { MaintenanceFile(it, isCrashReport = false) }
    val persistedLog = AppLog.persistedLogFile(context)
        .takeIf(File::isFile)
        ?.let { MaintenanceFile(it, isCrashReport = false) }

    return MaintenanceSnapshot(
        appVersion = packageInfo?.versionName ?: context.getString(R.string.maintenance_unknown),
        device = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        javaHeap = "${formatFileSize(runtime.totalMemory() - runtime.freeMemory())} / ${formatFileSize(runtime.maxMemory())}",
        nativeHeap = formatFileSize(Debug.getNativeHeapAllocatedSize()),
        availableMemory = formatFileSize(memoryInfo.availMem),
        cacheSize = formatFileSize(
            folderSize(File(context.cacheDir, LruCacheManager.PREVIEWS_DIR_NAME))
        ),
        freeStorage = formatFileSize(context.filesDir.usableSpace),
        pythonStatus = if (Python.isStarted()) context.getString(R.string.maintenance_python_ready)
            else context.getString(R.string.maintenance_python_not_started),
        files = (crashFiles + importedFiles + listOfNotNull(persistedLog))
            .sortedByDescending { it.file.lastModified() },
        logs = AppLog.current()?.recentEntries(40).orEmpty()
    )
}

private fun importMaintenanceFile(context: Context, uri: Uri): File {
    val directory = File(context.filesDir, IMPORT_DIRECTORY)
    if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
        throw IOException("Unable to create the diagnostics folder.")
    }
    val originalName = queryDisplayName(context, uri)
    val safeName = originalName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
        .takeLast(80)
        .ifBlank { "diagnostic.txt" }
    val destination = File(directory, "${UUID.randomUUID()}_$safeName")
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Unable to open the selected file.")
    try {
        input.use { source ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val bytesRead = source.read(buffer)
                    if (bytesRead < 0) break
                    copied += bytesRead
                    if (copied > MAX_IMPORTED_FILE_BYTES) {
                        throw IOException("Diagnostic file exceeds the import size limit.")
                    }
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    } catch (error: Exception) {
        destination.delete()
        throw error
    }
    return destination
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    if (uri.scheme != "content") return uri.lastPathSegment ?: "diagnostic.txt"
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0) cursor.getString(nameColumn) else null
        } else null
    } ?: "diagnostic.txt"
}

private fun readPreviewText(file: File): String {
    if (!file.isFile) throw IOException("The selected diagnostic file is unavailable.")
    val previewBytes = java.io.ByteArrayOutputStream()
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = MAX_PREVIEW_BYTES
        while (remaining > 0) {
            val bytesRead = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (bytesRead < 0) break
            previewBytes.write(buffer, 0, bytesRead)
            remaining -= bytesRead
        }
    }
    val text = previewBytes.toString(Charsets.UTF_8.name())
    return if (file.length() > MAX_PREVIEW_BYTES) {
        "$text\n\n${file.length() - MAX_PREVIEW_BYTES} bytes omitted from preview."
    } else text
}

private fun writeDiagnosticsSnapshot(application: Application, snapshot: MaintenanceSnapshot): File {
    val directory = File(application.filesDir, IMPORT_DIRECTORY)
    if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
        throw IOException("Unable to create the diagnostics folder.")
    }
    val report = File(directory, "diagnostics_${System.currentTimeMillis()}.txt")
    report.writeText(
        buildString {
            appendLine("StealZaLUT maintenance snapshot")
            appendLine("Generated: ${Date()}")
            appendLine("App: ${snapshot.appVersion}")
            appendLine("Device: ${snapshot.device}")
            appendLine("Android: ${snapshot.androidVersion}")
            appendLine("Python runtime: ${snapshot.pythonStatus}")
            appendLine("Java heap: ${snapshot.javaHeap}")
            appendLine("Native heap: ${snapshot.nativeHeap}")
            appendLine("Available memory: ${snapshot.availableMemory}")
            appendLine("App cache: ${snapshot.cacheSize}")
            appendLine("Free app storage: ${snapshot.freeStorage}")
            appendLine()
            appendLine("Recent app events:")
            snapshot.logs.forEach { entry ->
                appendLine("${Date(entry.timestampMs)} ${entry.level}/${entry.tag}: ${entry.message}")
            }
        },
        Charsets.UTF_8
    )
    return report
}

private fun shareMaintenanceFile(context: Context, application: Application, file: File) {
    if (!file.isFile || !file.canonicalPath.startsWith(application.filesDir.canonicalPath + File.separator)) {
        throw IOException("The selected file is outside the app diagnostics directory.")
    }
    val uri = FileProvider.getUriForFile(
        application,
        "${application.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.maintenance_share_title)))
}

private fun folderSize(folder: File): Long =
    folder.listFiles().orEmpty().sumOf { file -> if (file.isDirectory) folderSize(file) else file.length() }

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var amount = bytes.toDouble()
    var unitIndex = -1
    do {
        amount /= 1024.0
        unitIndex++
    } while (amount >= 1024.0 && unitIndex < units.lastIndex)
    return String.format(java.util.Locale.getDefault(), "%.1f %s", amount, units[unitIndex])
}

private const val IMPORT_DIRECTORY = "maintenance_files"
private const val MAX_PREVIEW_BYTES = 512 * 1024
private const val MAX_IMPORTED_FILE_BYTES = 25L * 1024L * 1024L
