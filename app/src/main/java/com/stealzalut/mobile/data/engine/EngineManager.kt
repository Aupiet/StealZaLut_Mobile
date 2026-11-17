package com.stealzalut.mobile.data.engine

import android.content.Context
import com.stealzalut.engine.ImageEngine
import com.stealzalut.engine.kotlin.KotlinImageEngine
import com.stealzalut.engine.model.EngineConfig
import com.stealzalut.engine.model.EngineType
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository

object EngineManager {
    private const val PREF_KEY_ENGINE = "KEY_STEALZALUT_ENGINE"
    private var activeType: EngineType = EngineType.fromString(
        System.getProperty("ENGINE") ?: "kotlin"
    )

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("stealzalut_engine_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_ENGINE, null)
        if (saved != null) {
            activeType = EngineType.fromString(saved)
        }
    }

    fun getEngineType(): EngineType = activeType

    fun setEngineType(context: Context?, type: EngineType) {
        activeType = type
        if (context != null) {
            val prefs = context.getSharedPreferences("stealzalut_engine_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_ENGINE, type.name.lowercase()).apply()
        }
    }

    fun getEngine(context: Context, bridgeRepo: PythonBridgeRepository): ImageEngine {
        return when (activeType) {
            EngineType.KOTLIN -> KotlinImageEngine(EngineConfig(type = EngineType.KOTLIN))
            EngineType.PYTHON -> PythonImageEngine(bridgeRepo)
        }
    }
}
