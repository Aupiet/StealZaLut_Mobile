package com.stealzalut.mobile

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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

        // Asynchronous warm-up of Python and cache maintenance
        applicationScope.launch {
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(this@StealZaLutApplication))
                }
                val py = Python.getInstance()
                // Warm up heavy modules in background
                py.getModule("numpy")
                py.getModule("scipy")
                py.getModule("color_matcher")
                py.getModule("stealzalut.bridge")
                Log.i(TAG, "Chaquopy warm-up completed successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "Chaquopy background warm-up encountered an issue: ${e.message}")
            }

            // Clean up temporary preview cache on startup (LRU policy: > 24h or > 50MB)
            try {
                val previewDir = File(cacheDir, "stealzalut_previews")
                LruCacheManager(previewDir).cleanupOldOrExcessFiles()
            } catch (e: Exception) {
                Log.w(TAG, "Initial cache cleanup encountered an issue: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "StealZaLutApp"
    }
}
