package com.example.stealzalut_mobile.data.repository

import android.content.Context
import com.example.stealzalut_mobile.data.model.SavedGradedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository tracking graded images exported to the user's local folder.
 */
class SavedImageRepository(private val context: Context) {

    private val trackingFile: File
        get() = File(context.filesDir, "saved_images_manifest.json")

    private val exportsDir: File
        get() = File(context.filesDir, "exports").apply { mkdirs() }

    /**
     * Retrieves all saved graded images, sorted newest first.
     */
    suspend fun getSavedImages(): List<SavedGradedImage> = withContext(Dispatchers.IO) {
        val list = loadManifest().toMutableList()
        // Synchronize with actual files on disk
        val existingFiles = exportsDir.listFiles()?.filter { it.isFile && (it.extension == "jpg" || it.extension == "jpeg" || it.extension == "png") } ?: emptyList()
        
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

    /**
     * Registers a newly exported graded image.
     */
    suspend fun recordGradedImage(
        filePath: String,
        width: Int,
        height: Int,
        themeName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = loadManifest().toMutableList()
            current.removeAll { it.filePath == filePath }
            current.add(
                0,
                SavedGradedImage(
                    filePath = filePath,
                    timestamp = System.currentTimeMillis(),
                    width = width,
                    height = height,
                    themeName = themeName ?: "Thème Personnalisé"
                )
            )
            writeManifest(current)
        }
    }

    /**
     * Deletes a saved image from storage and manifest.
     */
    suspend fun deleteSavedImage(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val current = loadManifest().toMutableList()
            val item = current.firstOrNull { it.id == id }
            if (item != null) {
                File(item.filePath).delete()
                current.remove(item)
                writeManifest(current)
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
                list.add(
                    SavedGradedImage(
                        id = obj.optString("id"),
                        filePath = obj.optString("filePath"),
                        timestamp = obj.optLong("timestamp"),
                        width = obj.optInt("width"),
                        height = obj.optInt("height"),
                        themeName = obj.optString("themeName")
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
            }
            json.put(obj)
        }
        trackingFile.writeText(json.toString(2), Charsets.UTF_8)
    }
}
