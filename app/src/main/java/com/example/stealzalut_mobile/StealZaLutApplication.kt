package com.example.stealzalut_mobile

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

/**
 * Application class for StealZaLUT.
 * Initializes the Chaquopy Python runtime offline environment on application startup.
 */
class StealZaLutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
