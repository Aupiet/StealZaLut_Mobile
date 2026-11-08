package com.stealzalut.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.screens.MainColorGradingScreen
import com.stealzalut.mobile.ui.theme.StealZaLut_MobileTheme
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

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
                MainColorGradingScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
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
            if (imageUri != null) {
                viewModel.setEditorSourceImage(imageUri)
                viewModel.navigateTo(AppDestination.EDITOR)
            }
        }
    }
}
