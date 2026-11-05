package com.stealzalut.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stealzalut.mobile.ui.screens.MainColorGradingScreen
import com.stealzalut.mobile.ui.theme.StealZaLut_MobileTheme
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: StealZaLutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            StealZaLut_MobileTheme(themeId = uiState.settings.selectedThemeId) {
                MainColorGradingScreen(viewModel = viewModel)
            }
        }
    }
}
