package com.stealzalut.mobile.data.repository

import android.content.Context
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.core.diagnostics.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Repository tracking graded images exported to the user's permanent profile library.
 * Preserves a copy of the exported photo in a dedicated permanent directory
 * so it is never deleted by temporary cache sweeps.
 */
class SavedImageRepository(private val context: Context) {

    private val trackingFile: File
        get() = File(context.filesDir, "saved_images_manifest.json")

    private val exportsDir: File
        get() = File(context.filesDir, "exports").apply { mkdirs() }

    private val mutex = Mutex()

    /**
     * Retrieves all saved graded images, sorted newest first.
     */
    suspend fun getSavedImages(): List<SavedGradedImage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val list = loadManifest().toMutableList()
            // Synchronize with actual files on disk
            val existingFiles = exportsDir.listFiles()?.filter {
                it.isFile && (it.extension.equals("jpg", true) || it.extension.equals("jpeg", true) || it.extension.equals("png", true))
            } ?: emptyList()

            // Remove manifest entries whose files were deleted
            val synced = list.filter { item -> existingFiles.any { it.absolutePath == item.filePath } }.toMutableList()

            // Add any files in exportsDir that were not yet in manifest
            existingFiles.forEach { file ->
                if (synced.none { it.filePath == file.absolutePath }) {
                    synced.add(
                        SavedGradedImage(
                            filePath = file.absolutePath,
                            timestamp = file.lastModified(),
                            themeName = "Look StealZaLUT"
                        )
                    )
                }
            }

            synced.sortByDescending { it.timestamp }
            writeManifest(synced)
            synced
        }
    }

    /**
     * Registers a newly exported graded image. Copies the image from the temporary
     * preview/cache directory to the permanent library storage.
     * Guarantees idempotency via mutex locking and duplicate prevention.
     */
    suspend fun recordGradedImage(
        sourceFilePath: String,
        width: Int,
        height: Int,
        themeName: String?,
        method: String? = "hm-mkl-hm",
        intensity: Float = 1.0f,
        referenceImagePath: String? = null,
        paletteHexes: List<String> = emptyList()
    ): Result<SavedGradedImage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            suspendRunCatching {
                val src = File(sourceFilePath)
                require(src.exists()) { "Source file does not exist: $sourceFilePath" }

                val current = loadManifest().toMutableList()
                val now = System.currentTimeMillis()
                val effectiveTheme = themeName?.ifBlank { "Look StealZaLUT" } ?: "Look StealZaLUT"
                val effectiveMethod = method ?: "hm-mkl-hm"

                // Idempotency: prevent rapid duplicate insertion within 3 seconds
                val existingRecent = current.firstOrNull {
                    it.width == width &&
                    it.height == height &&
                    it.themeName == effectiveTheme &&
                    it.method == effectiveMethod &&
                    kotlin.math.abs(it.intensity - intensity) < 0.01f &&
                    (now - it.timestamp) < 3000L &&
                    File(it.filePath).exists()
                }
                if (existingRecent != null) {
                    return@suspendRunCatching existingRecent
                }

                val finalPath = if (src.parentFile?.absolutePath == exportsDir.absolutePath) {
                    src.absolutePath
                } else {
                    val ext = src.extension.ifBlank { "jpg" }
                    val permanentFile = File(exportsDir, "graded_${System.currentTimeMillis()}.$ext")
                    FileInputStream(src).use { input ->
                        FileOutputStream(permanentFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    permanentFile.absolutePath
                }

                val newItem = SavedGradedImage(
                    filePath = finalPath,
                    timestamp = System.currentTimeMillis(),
                    width = width,
                    height = height,
                    themeName = effectiveTheme,
                    method = effectiveMethod,
                    intensity = intensity,
                    referenceImagePath = referenceImagePath,
                    paletteHexes = paletteHexes
                )

                current.add(0, newItem)
                writeManifest(current)
                newItem
            }
        }
    }

    /**
     * Deletes a saved image from permanent storage and manifest.
     */
    suspend fun deleteSavedImage(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            suspendRunCatching {
                val current = loadManifest().toMutableList()
                val item = current.firstOrNull { it.id == id }
                if (item != null) {
                    File(item.filePath).delete()
                    current.remove(item)
                    writeManifest(current)
                }
            }
        }
    }

    private fun loadManifest(): List<SavedGradedImage> {
        if (!trackingFile.exists()) return emptyList()
        return try {
            val json = JSONArray(trackingFile.readText(Charsets.UTF_8))
            val list = mutableListOf<SavedGradedImage>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val paletteList = mutableListOf<String>()
                val paletteArray = obj.optJSONArray("paletteHexes")
                if (paletteArray != null) {
                    for (j in 0 until paletteArray.length()) {
                        paletteList.add(paletteArray.getString(j))
                    }
                }
                list.add(
                    SavedGradedImage(
                        id = obj.optString("id"),
                        filePath = obj.optString("filePath"),
                        timestamp = obj.optLong("timestamp"),
                        width = obj.optInt("width"),
                        height = obj.optInt("height"),
                        themeName = obj.optString("themeName").takeIf { it.isNotBlank() },
                        method = obj.optString("method", "hm-mkl-hm"),
                        intensity = obj.optDouble("intensity", 1.0).toFloat(),
                        referenceImagePath = obj.optString("referenceImagePath").takeIf { it.isNotBlank() },
                        paletteHexes = paletteList
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeManifest(list: List<SavedGradedImage>) {
        val json = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("filePath", item.filePath)
                put("timestamp", item.timestamp)
                put("width", item.width)
                put("height", item.height)
                put("themeName", item.themeName ?: "")
                put("method", item.method ?: "hm-mkl-hm")
                put("intensity", item.intensity.toDouble())
                put("referenceImagePath", item.referenceImagePath ?: "")
                put("paletteHexes", JSONArray(item.paletteHexes))
            }
            json.put(obj)
        }
        trackingFile.writeText(json.toString(2), Charsets.UTF_8)
    }
}
