package com.stealzalut.mobile.ui.viewmodel

import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.delegate.StudioFlowDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StudioFlowNavigationTest {

    @Test
    fun `navigateTo updates currentDestination and preserves previousDestination`() {
        val stateFlow = MutableStateFlow(StealZaLutUiState(currentDestination = AppDestination.SETTINGS))
        val delegate = StudioFlowDelegate(stateFlow)

        delegate.navigateTo(AppDestination.GUIDE)

        assertEquals(AppDestination.GUIDE, stateFlow.value.currentDestination)
        assertEquals(AppDestination.SETTINGS, stateFlow.value.previousDestination)
    }

    @Test
    fun `navigateBackFromGuide returns to previousDestination when opened from settings`() {
        val stateFlow = MutableStateFlow(StealZaLutUiState(currentDestination = AppDestination.SETTINGS))
        val delegate = StudioFlowDelegate(stateFlow)

        delegate.navigateTo(AppDestination.GUIDE)
        assertEquals(AppDestination.GUIDE, stateFlow.value.currentDestination)

        delegate.navigateBackFromGuide()
        assertEquals(AppDestination.SETTINGS, stateFlow.value.currentDestination)
    }

    @Test
    fun `navigateBackFromGuide returns to editor when opened from editor`() {
        val stateFlow = MutableStateFlow(StealZaLutUiState(currentDestination = AppDestination.EDITOR))
        val delegate = StudioFlowDelegate(stateFlow)

        delegate.navigateTo(AppDestination.GUIDE)
        assertEquals(AppDestination.GUIDE, stateFlow.value.currentDestination)
        assertEquals(AppDestination.EDITOR, stateFlow.value.previousDestination)

        delegate.navigateBackFromGuide()
        assertEquals(AppDestination.EDITOR, stateFlow.value.currentDestination)
    }

    @Test
    fun `navigateBackFromGuide defaults to settings when previousDestination is null`() {
        val stateFlow = MutableStateFlow(StealZaLutUiState(currentDestination = AppDestination.GUIDE, previousDestination = null))
        val delegate = StudioFlowDelegate(stateFlow)

        delegate.navigateBackFromGuide()
        assertEquals(AppDestination.SETTINGS, stateFlow.value.currentDestination)
    }
}
