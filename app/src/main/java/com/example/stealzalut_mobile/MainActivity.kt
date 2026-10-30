package com.example.stealzalut_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.stealzalut_mobile.ui.screens.MainColorGradingScreen
import com.example.stealzalut_mobile.ui.theme.StealZaLut_MobileTheme
import com.example.stealzalut_mobile.ui.viewmodel.StealZaLutViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StealZaLutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StealZaLut_MobileTheme {
                MainColorGradingScreen(viewModel = viewModel)
            }
        }
    }
}
