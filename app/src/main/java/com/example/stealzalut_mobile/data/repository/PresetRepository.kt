package com.example.stealzalut_mobile.data.repository

import android.content.Context
import com.example.stealzalut_mobile.data.model.ColorTransferMethod
import com.example.stealzalut_mobile.data.model.ColorimetryPreset
import com.example.stealzalut_mobile.data.model.LutSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository responsible for persisting and loading colorimetry themes/presets.
 * Stores themes with their palette, recipe, and generated calibration mire (color set).
 */
class PresetRepository(private val context: Context) {

    private val presetsFile: File
        get() = File(context.filesDir, "saved_colorimetries.json")

    /**
     * Factory built-in professional colorimetry themes.
     */
    val builtInPresets: List<ColorimetryPreset> = listOf(
        ColorimetryPreset(
            id = "builtin_teal_orange",
            name = "Cinematic Teal & Orange",
            method = ColorTransferMethod.HM_MKL_HM,
            intensity = 1.0f,
            lutSize = LutSize.STANDARD,
            paletteHexes = listOf("#E07A5F", "#3D405B", "#81B29A", "#F2CC8F", "#1D3557", "#F4F1DE"),
            isBuiltIn = true,
            notes = "Standard de l'étalonnage hollywoodien : tons chair chaleureux et ombres bleu sarcelle."
        ),
        ColorimetryPreset(
            id = "builtin_kodak_portra",
            name = "Kodak Portra Film 400",
            method = ColorTransferMethod.REINHARD,
            intensity = 0.85f,
            lutSize = LutSize.STANDARD,
            paletteHexes = listOf("#F4A261", "#E76F51", "#2A9D8F", "#E9C46A", "#264653", "#FAF0CA"),
            isBuiltIn = true,
            notes = "Grain argentique chaud, compression douce des hautes lumières et verts pastels."
        ),
        ColorimetryPreset(
            id = "builtin_nordic_moody",
            name = "Nordic Cold Moody",
            method = ColorTransferMethod.MKL,
            intensity = 0.90f,
            lutSize = LutSize.STANDARD,
            paletteHexes = listOf("#2B2D42", "#8D99AE", "#EDF2F4", "#457B9D", "#1D3557", "#A8DADC"),
            isBuiltIn = true,
            notes = "Ambiance scandinave froide, ombres bleutées désaturées et contraste dramatique."
        ),
        ColorimetryPreset(
            id = "builtin_cyberpunk",
            name = "Cyberpunk Neon Drive",
            method = ColorTransferMethod.HM,
            intensity = 1.0f,
            lutSize = LutSize.HIGH,
            paletteHexes = listOf("#FF007F", "#00F0FF", "#7B2CBF", "#10002B", "#3C096C", "#9D4EDD"),
            isBuiltIn = true,
            notes = "Dédoublement néon magenta et cyan vif avec noirs profonds."
        )
    )

    /**
     * Loads all themes (user-saved + factory built-in).
     */
    suspend fun getAllPresets(): List<ColorimetryPreset> = withContext(Dispatchers.IO) {
        val userPresets = loadUserPresets()
        builtInPresets + userPresets
    }

    /**
     * Saves a new user colorimetry theme.
     */
    suspend fun savePreset(preset: ColorimetryPreset): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = loadUserPresets().toMutableList()
            val index = current.indexOfFirst { it.id == preset.id }
            if (index >= 0) {
                current[index] = preset
            } else {
                current.add(0, preset)
            }
            writeUserPresets(current)
        }
    }

    /**
     * Deletes a user preset by ID.
     */
    suspend fun deletePreset(presetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = loadUserPresets().toMutableList()
            current.removeAll { it.id == presetId }
            writeUserPresets(current)
        }
    }

    private fun loadUserPresets(): List<ColorimetryPreset> {
        if (!presetsFile.exists()) return emptyList()
        return try {
            val jsonText = presetsFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<ColorimetryPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val paletteList = mutableListOf<String>()
                val paletteArray = obj.optJSONArray("paletteHexes")
                if (paletteArray != null) {
                    for (j in 0 until paletteArray.length()) {
                        paletteList.add(paletteArray.getString(j))
                    }
                }

                list.add(
                    ColorimetryPreset(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Sans titre"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        method = ColorTransferMethod.fromKey(obj.optString("method", "hm-mkl-hm")),
                        intensity = obj.optDouble("intensity", 1.0).toFloat(),
                        lutSize = LutSize.fromSize(obj.optInt("lutSize", 33)),
                        referenceImagePath = obj.optString("referenceImagePath").takeIf { it.isNotBlank() },
                        paletteHexes = paletteList,
                        colorSetImagePath = obj.optString("colorSetImagePath").takeIf { it.isNotBlank() },
                        cubeFilePath = obj.optString("cubeFilePath").takeIf { it.isNotBlank() },
                        isBuiltIn = false,
                        notes = obj.optString("notes").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeUserPresets(presets: List<ColorimetryPreset>) {
        val jsonArray = JSONArray()
        for (p in presets) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("createdAt", p.createdAt)
                put("method", p.method.key)
                put("intensity", p.intensity.toDouble())
                put("lutSize", p.lutSize.size)
                put("referenceImagePath", p.referenceImagePath ?: "")
                put("paletteHexes", JSONArray(p.paletteHexes))
                put("colorSetImagePath", p.colorSetImagePath ?: "")
                put("cubeFilePath", p.cubeFilePath ?: "")
                put("notes", p.notes ?: "")
            }
            jsonArray.put(obj)
        }
        presetsFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
    }
}
