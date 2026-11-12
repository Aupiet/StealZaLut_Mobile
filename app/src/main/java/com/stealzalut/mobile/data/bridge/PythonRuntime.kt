package com.stealzalut.mobile.data.bridge

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object PythonRuntime {
    val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val startupMutex = Mutex()

    suspend fun ensureStarted(context: Context): Python = withContext(dispatcher) {
        startupMutex.withLock {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            Python.getInstance()
        }
    }

    suspend fun warmUp(context: Context) = withContext(dispatcher) {
        val python = ensureStarted(context)
        python.getModule("numpy")
        python.getModule("scipy")
        python.getModule("color_matcher")
        python.getModule("stealzalut.bridge")
    }
}
