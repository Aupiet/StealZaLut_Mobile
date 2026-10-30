package com.stealzalut.mobile.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val cacheSizeMb: Double = 0.0,
    val currentLanguageTag: String = "en",
    val userMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface SettingsUiEffect {
    data class ShowSnackbar(val message: String) : SettingsUiEffect
}

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            settings = settingsRepository.getSettings(),
            currentLanguageTag = getCurrentLanguageTag()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        refreshCacheSize()
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update {
            it.copy(
                settings = newSettings,
                userMessage = "Paramètres enregistrés."
            )
        }
        viewModelScope.launch {
            _effects.send(SettingsUiEffect.ShowSnackbar("Paramètres enregistrés."))
        }
    }

    /**
     * Changes app language dynamically using Android 13+ Per-App Language API
     * backed by AppCompatDelegate.setApplicationLocales().
     */
    fun setLanguage(languageTag: String) {
        val appLocales = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocales)
        _uiState.update { it.copy(currentLanguageTag = languageTag) }
        viewModelScope.launch {
            _effects.send(SettingsUiEffect.ShowSnackbar("Langue changée / Language updated"))
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val sizeMb = settingsRepository.getCacheSizeMb()
            _uiState.update { it.copy(cacheSizeMb = sizeMb) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cleared = settingsRepository.clearCache()
            refreshCacheSize()
            val msg = if (cleared) "Cache temporaire vidé." else "Erreur vidage cache."
            _uiState.update { it.copy(userMessage = msg) }
            _effects.send(SettingsUiEffect.ShowSnackbar(msg))
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }

    private fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales.get(0)?.language ?: "en"
        } else {
            java.util.Locale.getDefault().language
        }
    }
}
