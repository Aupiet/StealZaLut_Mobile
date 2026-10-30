package com.stealzalut.mobile.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import app.cash.turbine.test
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.repository.PresetRepository
import com.stealzalut.mobile.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ColorGradingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val application: Application = mockk(relaxed = true)
    private val bridgeRepository: PythonBridgeRepository = mockk(relaxed = true)
    private val presetRepository: PresetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.getSettings() } returns AppSettings(
            defaultLutSize = LutSize.STANDARD,
            defaultMethod = ColorTransferMethod.HM_MKL_HM
        )
        coEvery { presetRepository.getAllPresets() } returns listOf(
            ColorimetryPreset(id = "test_preset", name = "Test Look", method = ColorTransferMethod.HM_MKL_HM)
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects defaults and cannot process without images`() = runTest(testDispatcher) {
        val viewModel = ColorGradingViewModel(
            application = application,
            bridgeRepository = bridgeRepository,
            presetRepository = presetRepository,
            settingsRepository = settingsRepository
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ColorTransferMethod.HM_MKL_HM, state.method)
            assertEquals(LutSize.STANDARD, state.lutSize)
            assertEquals(1.0f, state.intensity)
            assertFalse(state.canProcess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setting intensity updates immediately in state without calling Python bridge`() = runTest(testDispatcher) {
        val viewModel = ColorGradingViewModel(
            application = application,
            bridgeRepository = bridgeRepository,
            presetRepository = presetRepository,
            settingsRepository = settingsRepository
        )

        viewModel.setIntensity(0.75f)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0.75f, state.intensity, 0.001f)
            // Python bridge generatePreview should NOT be called for real-time intensity updates
            coVerify(exactly = 0) {
                bridgeRepository.generatePreview(any(), any(), any(), any())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rapid method changes are debounced before triggering Python recalculation`() = runTest(testDispatcher) {
        val viewModel = ColorGradingViewModel(
            application = application,
            bridgeRepository = bridgeRepository,
            presetRepository = presetRepository,
            settingsRepository = settingsRepository
        )

        // Switch method multiple times in rapid succession (< 150ms)
        viewModel.setMethod(ColorTransferMethod.MKL)
        advanceTimeBy(50L)
        viewModel.setMethod(ColorTransferMethod.HM)
        advanceTimeBy(50L)
        viewModel.setMethod(ColorTransferMethod.REINHARD)

        // Not yet dispatched because 150ms debounce has not elapsed
        advanceTimeBy(100L)

        // Now complete the debounce window
        advanceTimeBy(100L)
        advanceUntilIdle()

        // Only the final method (REINHARD) is in state
        assertEquals(ColorTransferMethod.REINHARD, viewModel.uiState.value.method)
    }
}
