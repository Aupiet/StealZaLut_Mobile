package com.stealzalut.mobile.data.repository

import android.content.Context
import android.os.Build
import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.model.EditorParamsSnapshot
import com.stealzalut.mobile.data.model.EditorProject
import com.stealzalut.mobile.data.model.LightroomDeserializer
import com.stealzalut.mobile.data.model.LightroomSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository for non-destructive photo editing project drafts.
 * Uses atomic JSON sidecar persistence in dedicated project folders:
 * `filesDir/projects/<id>/project.json`
 */
class ProjectRepository(private val context: Context) {

    private val mutex = Mutex()

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    private fun getProjectDir(id: String): File =
        File(projectsRoot, id).apply { mkdirs() }

    /**
     * Retrieves all saved projects, sorted newest first by last modified time.
     */
    suspend fun getAllProjects(): List<EditorProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dirs = projectsRoot.listFiles()?.filter { it.isDirectory } ?: emptyList()
            val list = mutableListOf<EditorProject>()

            for (dir in dirs) {
                val projFile = File(dir, "project.json")
                if (projFile.exists() && projFile.isFile) {
                    val proj = readProjectFile(projFile)
                    if (proj != null) {
                        list.add(proj)
                    }
                }
            }

            list.sortByDescending { it.updatedAt }
            list
        }
    }

    /**
     * Retrieves a single project by ID.
     */
    suspend fun getProject(id: String): EditorProject? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(projectsRoot, id)
            if (!dir.exists() || !dir.isDirectory) return@withLock null
            val projFile = File(dir, "project.json")
            if (!projFile.exists() || !projFile.isFile) return@withLock null
            readProjectFile(projFile)
        }
    }

    /**
     * Atomically saves or updates an editing project.
     */
    suspend fun saveProject(project: EditorProject): EditorProject = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = getProjectDir(project.id)
            val projFile = File(dir, "project.json")
            val jsonString = projectToJson(project)
            writeAtomically(projFile, jsonString)
            project
        }
    }

    private fun writeAtomically(targetFile: File, content: String) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        tempFile.writeText(content, StandardCharsets.UTF_8)

        val moved = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }

        if (!moved) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        }
    }

    /**
     * Creates a new project copying the source photo to permanent storage.
     */
    suspend fun createProject(
        sourceFile: File,
        name: String? = null,
        snapshot: EditorParamsSnapshot = EditorParamsSnapshot(),
        previewFile: File? = null
    ): EditorProject = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val dir = getProjectDir(id)

        // Permanent copy of source image
        val sourceExt = sourceFile.extension.ifBlank { "jpg" }
        val permanentSource = File(dir, "source.$sourceExt")
        sourceFile.copyTo(permanentSource, overwrite = true)

        // Permanent copy of preview if available
        var permanentPreviewPath: String? = null
        if (previewFile != null && previewFile.exists()) {
            val permPreview = File(dir, "preview.jpg")
            previewFile.copyTo(permPreview, overwrite = true)
            permanentPreviewPath = permPreview.absolutePath
        }

        val projName = name ?: run {
            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            "Projet $dateStr"
        }

        val project = EditorProject(
            id = id,
            name = projName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            sourceImagePath = permanentSource.absolutePath,
            previewImagePath = permanentPreviewPath,
            snapshot = snapshot
        )

        saveProject(project)
    }

    /**
     * Deletes a project and all associated files.
     */
    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(projectsRoot, id)
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                false
            }
        }
    }

    /**
     * Renames an existing project.
     */
    suspend fun renameProject(id: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val existing = getProject(id) ?: return@withContext false
        val updated = existing.copy(name = newName.trim(), updatedAt = System.currentTimeMillis())
        saveProject(updated)
        true
    }

    /**
     * Duplicates a project with a new ID and independent files.
     */
    suspend fun duplicateProject(id: String, newNamePrefix: String = "Copie de "): EditorProject? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val newDir = getProjectDir(newId)

        val origSource = File(original.sourceImagePath)
        val newSource = File(newDir, origSource.name)
        if (origSource.exists()) {
            origSource.copyTo(newSource, overwrite = true)
        }

        var newPreviewPath: String? = null
        if (original.previewImagePath != null) {
            val origPreview = File(original.previewImagePath)
            if (origPreview.exists()) {
                val newPreview = File(newDir, "preview.jpg")
                origPreview.copyTo(newPreview, overwrite = true)
                newPreviewPath = newPreview.absolutePath
            }
        }

        val duplicate = EditorProject(
            id = newId,
            name = "$newNamePrefix${original.name}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            sourceImagePath = newSource.absolutePath,
            previewImagePath = newPreviewPath,
            snapshot = original.snapshot,
            width = original.width,
            height = original.height
        )

        saveProject(duplicate)
    }

    private fun readProjectFile(file: File): EditorProject? {
        return try {
            if (!file.exists() || !file.isFile) return null
            val jsonStr = file.readText(StandardCharsets.UTF_8)
            jsonToProject(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    private fun projectToJson(p: EditorProject): String {
        val root = JSONObject()
        root.put("id", p.id)
        root.put("name", p.name)
        root.put("createdAt", p.createdAt)
        root.put("updatedAt", p.updatedAt)
        root.put("sourceImagePath", p.sourceImagePath)
        root.put("previewImagePath", p.previewImagePath ?: JSONObject.NULL)
        root.put("width", p.width)
        root.put("height", p.height)

        val s = p.snapshot
        val snap = JSONObject().apply {
            put("intensity", s.intensity.toDouble())
            put("lumaLock", s.lumaLock.toDouble())
            put("neutralAxisLock", s.neutralAxisLock.toDouble())
            put("skinToneLock", s.skinToneLock.toDouble())
            put("extremesRolloff", s.extremesRolloff.toDouble())
            put("spatial3d", s.spatial3d.toDouble())
            put("exposure", s.exposure.toDouble())
            put("contrast", s.contrast.toDouble())
            put("highlights", s.highlights.toDouble())
            put("shadows", s.shadows.toDouble())
            put("temperature", s.temperature.toDouble())
            put("tint", s.tint.toDouble())
            put("vibrance", s.vibrance.toDouble())
            put("saturation", s.saturation.toDouble())
            put("clarity", s.clarity.toDouble())
            put("sharpen", s.sharpen.toDouble())
            put("grain", s.grain.toDouble())
            put("customLutPath", s.customLutPath ?: JSONObject.NULL)
            put("customLutName", s.customLutName ?: JSONObject.NULL)
            put("lightroomJson", LightroomSerializer.toJson(s.lightroomParams, s.cropRegion))
        }
        root.put("snapshot", snap)

        return root.toString(2)
    }

    private fun jsonToProject(jsonStr: String): EditorProject {
        val root = JSONObject(jsonStr)
        val id = root.getString("id")
        val name = root.optString("name", "Projet")
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = root.optLong("updatedAt", createdAt)
        val sourceImagePath = root.getString("sourceImagePath")
        val previewImagePath = if (root.isNull("previewImagePath")) null else root.optString("previewImagePath")
        val width = root.optInt("width", 0)
        val height = root.optInt("height", 0)

        val snapObj = root.optJSONObject("snapshot")
        val snapshot = if (snapObj != null) {
            val lrJson = snapObj.optString("lightroomJson", "")
            val lrModel = if (lrJson.isNotBlank()) LightroomDeserializer.fromJson(lrJson) else com.stealzalut.mobile.data.model.LightroomDevelopModel()
            val crop = if (lrJson.isNotBlank()) LightroomDeserializer.extractCropRegion(lrJson) else CropRegion()

            EditorParamsSnapshot(
                intensity = snapObj.optDouble("intensity", 1.0).toFloat(),
                lumaLock = snapObj.optDouble("lumaLock", 0.0).toFloat(),
                neutralAxisLock = snapObj.optDouble("neutralAxisLock", 0.0).toFloat(),
                skinToneLock = snapObj.optDouble("skinToneLock", 0.0).toFloat(),
                extremesRolloff = snapObj.optDouble("extremesRolloff", 0.0).toFloat(),
                spatial3d = snapObj.optDouble("spatial3d", 0.0).toFloat(),
                exposure = snapObj.optDouble("exposure", 0.0).toFloat(),
                contrast = snapObj.optDouble("contrast", 0.0).toFloat(),
                highlights = snapObj.optDouble("highlights", 0.0).toFloat(),
                shadows = snapObj.optDouble("shadows", 0.0).toFloat(),
                temperature = snapObj.optDouble("temperature", 0.0).toFloat(),
                tint = snapObj.optDouble("tint", 0.0).toFloat(),
                vibrance = snapObj.optDouble("vibrance", 0.0).toFloat(),
                saturation = snapObj.optDouble("saturation", 0.0).toFloat(),
                clarity = snapObj.optDouble("clarity", 0.0).toFloat(),
                sharpen = snapObj.optDouble("sharpen", 0.0).toFloat(),
                grain = snapObj.optDouble("grain", 0.0).toFloat(),
                customLutPath = if (snapObj.isNull("customLutPath")) null else snapObj.optString("customLutPath"),
                customLutName = if (snapObj.isNull("customLutName")) null else snapObj.optString("customLutName"),
                lightroomParams = lrModel,
                cropRegion = crop
            )
        } else EditorParamsSnapshot()

        return EditorProject(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            sourceImagePath = sourceImagePath,
            previewImagePath = previewImagePath,
            snapshot = snapshot,
            width = width,
            height = height
        )
    }
}
