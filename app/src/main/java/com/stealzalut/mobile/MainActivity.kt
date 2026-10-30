package com.stealzalut.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stealzalut.mobile.ui.screens.MainColorGradingScreen
import com.stealzalut.mobile.ui.theme.StealZaLut_MobileTheme
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

class MainActivity : AppCompatActivity() {

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
