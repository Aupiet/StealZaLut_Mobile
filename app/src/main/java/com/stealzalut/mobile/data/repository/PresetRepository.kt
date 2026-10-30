package com.stealzalut.mobile.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.LutSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Repository responsible for persisting and loading user colorimetry presets.
 * Generates and stores WebP thumbnails (quality 85%, max 256x256 px) for each preset.
 */
class PresetRepository(private val context: Context) {

    private val presetsFile: File
        get() = File(context.filesDir, "saved_colorimetries.json")

    private val thumbnailsDir: File
        get() = File(context.filesDir, "preset_thumbnails").apply { mkdirs() }

    val builtInPresets: List<ColorimetryPreset> = emptyList()

    suspend fun getAllPresets(): List<ColorimetryPreset> = withContext(Dispatchers.IO) {
        loadUserPresets()
    }

    suspend fun savePreset(preset: ColorimetryPreset): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            var updatedPreset = preset
            if (preset.thumbnailPath == null) {
                val imageToThumb = preset.referenceImagePath
                if (imageToThumb != null && File(imageToThumb).exists()) {
                    val webpThumb = generateWebpThumbnail(imageToThumb, preset.id)
                    if (webpThumb != null) {
                        updatedPreset = preset.copy(thumbnailPath = webpThumb)
                    }
                }
            }

            val current = loadUserPresets().toMutableList()
            val index = current.indexOfFirst { it.id == updatedPreset.id }
            if (index >= 0) {
                current[index] = updatedPreset
            } else {
                current.add(0, updatedPreset)
            }
            writeUserPresets(current)
        }
    }

    suspend fun deletePreset(presetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = loadUserPresets().toMutableList()
            val removed = current.firstOrNull { it.id == presetId }
            removed?.thumbnailPath?.let { File(it).delete() }
            current.removeAll { it.id == presetId }
            writeUserPresets(current)
        }
    }

    private fun generateWebpThumbnail(sourcePath: String, presetId: String): String? {
        return try {
            val srcFile = File(sourcePath)
            if (!srcFile.exists()) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourcePath, bounds)

            var sampleSize = 1
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxDim / (sampleSize * 2) >= AppConstants.PRESET_THUMBNAIL_MAX_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bmp = BitmapFactory.decodeFile(sourcePath, decodeOpts) ?: return null

            val scaled = if (maxOf(bmp.width, bmp.height) > AppConstants.PRESET_THUMBNAIL_MAX_DIMENSION) {
                val factor = AppConstants.PRESET_THUMBNAIL_MAX_DIMENSION.toFloat() / maxOf(bmp.width, bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * factor).toInt(), (bmp.height * factor).toInt(), true).also {
                    if (it != bmp) bmp.recycle()
                }
            } else bmp

            val thumbFile = File(thumbnailsDir, "preset_${presetId}.webp")
            FileOutputStream(thumbFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, AppConstants.PRESET_THUMBNAIL_QUALITY, out)
                } else {
                    @Suppress("DEPRECATION")
                    scaled.compress(Bitmap.CompressFormat.WEBP, AppConstants.PRESET_THUMBNAIL_QUALITY, out)
                }
            }
            scaled.recycle()
            thumbFile.absolutePath
        } catch (_: Exception) {
            null
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
                        thumbnailPath = obj.optString("thumbnailPath").takeIf { it.isNotBlank() },
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
                put("thumbnailPath", p.thumbnailPath ?: "")
                put("cubeFilePath", p.cubeFilePath ?: "")
                put("notes", p.notes ?: "")
            }
            jsonArray.put(obj)
        }
        presetsFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
    }
}
