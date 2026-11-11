package com.stealzalut.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.screens.MainColorGradingScreen
import com.stealzalut.mobile.ui.theme.StealZaLut_MobileTheme
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import com.stealzalut.mobile.ui.components.CrashReportPrompt
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.core.diagnostics.LogLevel
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    private val viewModel: StealZaLutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            StealZaLut_MobileTheme(
                themeId = uiState.settings.selectedThemeId,
                iconSetId = uiState.settings.selectedIconSetId
            ) {
                Box(Modifier.fillMaxSize()) {
                    MainColorGradingScreen(viewModel = viewModel)
                    CrashReportPrompt()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            val type = intent.type
            if (type?.startsWith("image/") == true) {
                val imageUri: Uri? = when (action) {
                    Intent.ACTION_EDIT -> intent.data
                    Intent.ACTION_SEND -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                        }
                    }
                    else -> intent.data
                }
                if (imageUri == null || !canReadImageUri(imageUri)) {
                    viewModel.reportIncomingImageError()
                    return
                }
                viewModel.setEditorSourceImage(imageUri)
                viewModel.navigateTo(AppDestination.EDITOR)
            }
        } catch (error: SecurityException) {
            AppLog.log(LogLevel.WARN, TAG, "Incoming image permission is unavailable.")
            viewModel.reportIncomingImageError()
        } catch (error: IllegalArgumentException) {
            AppLog.log(LogLevel.WARN, TAG, "Incoming image intent is invalid.")
            viewModel.reportIncomingImageError()
        } catch (error: ClassCastException) {
            AppLog.log(LogLevel.WARN, TAG, "Incoming image extra has an invalid type.")
            viewModel.reportIncomingImageError()
        } catch (error: FileNotFoundException) {
            AppLog.log(LogLevel.WARN, TAG, "Incoming image cannot be opened.")
            viewModel.reportIncomingImageError()
        }
    }

    private fun canReadImageUri(uri: Uri): Boolean {
        return when (uri.scheme) {
            "content" -> contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            "file" -> uri.path?.let { java.io.File(it).isFile } == true
            else -> false
        }
    }

    private companion object {
        const val TAG = "StealZaLutMain"
    }
}
