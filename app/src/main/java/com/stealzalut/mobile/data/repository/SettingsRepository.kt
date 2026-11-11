package com.stealzalut.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.stealzalut.mobile.data.cache.LruCacheManager
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.core.diagnostics.suspendRunCatching
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.theme.AppThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for managing persistent application settings and disk cache stats.
 */
class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stealzalut_settings", Context.MODE_PRIVATE)

    private val lruCacheManager = LruCacheManager(
        File(context.cacheDir, LruCacheManager.PREVIEWS_DIR_NAME)
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())

    companion object {
        private const val KEY_DEFAULT_LUT_SIZE = "key_default_lut_size"
        private const val KEY_DEFAULT_METHOD = "key_default_method"
        private const val KEY_ANALYSIS_PROXY_DIM = "key_analysis_proxy_dim"
        private const val KEY_MAX_EXPORT_DIM = "key_max_export_dim"
        private const val KEY_EXPORT_FORMAT = "key_export_format"
        private const val KEY_EXPORT_QUALITY = "key_export_quality"
        private const val KEY_SELECTED_THEME_ID = "key_selected_theme_id"
        private const val KEY_DEVELOPER_MODE = "key_developer_mode"
    }

    private fun loadSettings(): AppSettings {
        val lutSizeVal = prefs.getInt(KEY_DEFAULT_LUT_SIZE, 33)
        val methodKey = prefs.getString(KEY_DEFAULT_METHOD, "hm-mkl-hm") ?: "hm-mkl-hm"
        val proxyDim = prefs.getInt(KEY_ANALYSIS_PROXY_DIM, 512)
        val exportDim = prefs.getInt(KEY_MAX_EXPORT_DIM, 3840)
        val format = prefs.getString(KEY_EXPORT_FORMAT, "JPEG") ?: "JPEG"
        val quality = prefs.getInt(KEY_EXPORT_QUALITY, 95)
        val themeId = prefs.getString(KEY_SELECTED_THEME_ID, AppThemes.DEFAULT_THEME_ID) ?: AppThemes.DEFAULT_THEME_ID
        val devMode = prefs.getBoolean(KEY_DEVELOPER_MODE, false)

        return AppSettings(
            defaultLutSize = LutSize.fromSize(lutSizeVal),
            defaultMethod = ColorTransferMethod.fromKey(methodKey),
            analysisProxyDim = proxyDim,
            maxExportDim = exportDim,
            exportFormat = format,
            exportQuality = quality,
            selectedThemeId = themeId,
            developerMode = devMode
        )
    }

    fun getSettings(): AppSettings = _settingsFlow.value

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_DEFAULT_LUT_SIZE, settings.defaultLutSize.size)
            .putString(KEY_DEFAULT_METHOD, settings.defaultMethod.key)
            .putInt(KEY_ANALYSIS_PROXY_DIM, settings.analysisProxyDim)
            .putInt(KEY_MAX_EXPORT_DIM, settings.maxExportDim)
            .putString(KEY_EXPORT_FORMAT, settings.exportFormat)
            .putInt(KEY_EXPORT_QUALITY, settings.exportQuality)
            .putString(KEY_SELECTED_THEME_ID, settings.selectedThemeId)
            .putBoolean(KEY_DEVELOPER_MODE, settings.developerMode)
            .apply()
        _settingsFlow.value = settings
    }

    /**
     * Computes the total disk space occupied by the app's cache directory in Megabytes.
     */
    suspend fun getCacheSizeMb(): Double = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val totalBytes = calculateFolderSize(cacheDir)
        totalBytes / (1024.0 * 1024.0)
    }

    /**
     * Clears all temporary preview and calculation files from the cache directory.
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        suspendRunCatching {
            lruCacheManager.clearAll()
            context.cacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
            true
        }.getOrDefault(false)
    }

    private fun calculateFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateFolderSize(file) else file.length()
        }
        return size
    }
}
