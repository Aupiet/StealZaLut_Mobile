package com.stealzalut.mobile.ui.components

import android.app.Application
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.core.diagnostics.CrashReporter
import com.stealzalut.mobile.core.diagnostics.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CrashReportPrompt() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        report = withContext(Dispatchers.IO) {
            (context.applicationContext as? Application)?.let(CrashReporter::unreadReports)?.firstOrNull()
        }
    }

    val pendingReport = report ?: return
    AlertDialog(
        onDismissRequest = {
            CrashReporter.markHandled(pendingReport)
            report = null
        },
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = { Text(stringResource(R.string.crash_report_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    val application = context.applicationContext as? Application
                    val intent = application?.let { CrashReporter.shareIntent(it, pendingReport) }
                    if (intent == null) {
                        AppLog.log(LogLevel.WARN, "CrashReporter", "Crash report sharing is unavailable.")
                    } else {
                        try {
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.crash_report_share)))
                        } catch (error: Throwable) {
                            AppLog.log(LogLevel.ERROR, "CrashReporter", "Could not open share sheet.", error)
                        }
                    }
                    CrashReporter.markHandled(pendingReport)
                    report = null
                }
            ) {
                Text(stringResource(R.string.crash_report_share))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    CrashReporter.markHandled(pendingReport)
                    report = null
                }
            ) {
                Text(stringResource(R.string.crash_report_ignore))
            }
        }
    )
}
