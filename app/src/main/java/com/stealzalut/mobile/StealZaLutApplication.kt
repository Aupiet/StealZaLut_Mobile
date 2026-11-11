package com.stealzalut.mobile

import android.app.Application
import android.util.Log
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.core.diagnostics.CrashReporter
import com.stealzalut.mobile.core.diagnostics.LogLevel
import com.stealzalut.mobile.data.bridge.PythonRuntime
import com.stealzalut.mobile.data.cache.LruCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Application class for StealZaLUT.
 * Asynchronously initializes Chaquopy Python runtime on startup and warms up
 * heavy scientific computing modules (numpy, scipy, color_matcher, stealzalut.bridge)
 * to remove the initial 1.5s latency on first user interaction.
 */
class StealZaLutApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppLog.initialize(this)
        CrashReporter.install(this)

        // Asynchronous warm-up of Python and cache maintenance
        applicationScope.launch {
            try {
                val py = PythonRuntime.ensureStarted(this@StealZaLutApplication)
                // Warm up heavy modules in background
                py.getModule("numpy")
                py.getModule("scipy")
                py.getModule("color_matcher")
                py.getModule("stealzalut.bridge")
                Log.i(TAG, "Chaquopy warm-up completed successfully.")
            } catch (e: Exception) {
                AppLog.log(LogLevel.WARN, TAG, "Chaquopy background warm-up failed.", e)
            }

            // Clean up temporary preview cache on startup (LRU policy: > 24h or > 50MB)
            try {
                val previewDir = File(cacheDir, "stealzalut_previews")
                LruCacheManager(previewDir).cleanupOldOrExcessFiles()
            } catch (e: Exception) {
                AppLog.log(LogLevel.WARN, TAG, "Initial cache cleanup failed.", e)
            }
        }
    }

    companion object {
        private const val TAG = "StealZaLutApp"
    }
}
