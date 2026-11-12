package com.stealzalut.mobile.data.bridge

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.chaquo.python.PyObject
import com.stealzalut.mobile.data.cache.LruCacheManager
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutDomainError
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.model.RecipePayload
import com.stealzalut.mobile.data.model.Step2Model
import com.stealzalut.mobile.data.model.Step3Model
import com.stealzalut.mobile.data.model.AnalysisPackExport
import com.stealzalut.mobile.data.model.ZoneDescriptor
import com.stealzalut.mobile.data.model.ZoneLink
import com.stealzalut.mobile.data.model.ZoneMatching
import com.stealzalut.mobile.data.model.ZonePaletteAnalysis
import com.stealzalut.mobile.data.model.ZoneTransferSummary
import com.stealzalut.mobile.core.diagnostics.suspendRunCatching
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.image.ExportMemoryDecision
import com.stealzalut.mobile.core.image.ExportMemoryGuard
import com.stealzalut.mobile.data.model.LightroomDevelopModel
import com.stealzalut.mobile.data.model.LightroomDeserializer
import com.stealzalut.mobile.data.model.LightroomSerializer
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Repository interfacing with Chaquopy Python backend for offline color grading.
 * Sequential execution via `pythonDispatcher` ensures strict GIL protection.
 * Image exports save directly to native Android MediaStore.
 */
class PythonBridgeRepository(private val context: Context) {

    // Dedicated single-threaded dispatcher protecting the Python Global Interpreter Lock (GIL)
    private val pythonDispatcher = PythonRuntime.dispatcher

    val lruCacheManager: LruCacheManager = LruCacheManager(
        File(context.cacheDir, LruCacheManager.PREVIEWS_DIR_NAME)
    )

    // Dedicated directory for active source/reference inputs, isolated from auto-pruned previews
    val inputsDir: File = File(context.cacheDir, "stealzalut_inputs").apply { mkdirs() }

    fun resolveExtensionFromUri(uri: Uri): String {
        return try {
            val mime = context.contentResolver.getType(uri)
            val extFromMime = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!extFromMime.isNullOrBlank()) return extFromMime.lowercase()

            var name: String? = null
            if (uri.scheme == android.content.ContentResolver.SCHEME_CONTENT) {
                val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) name = it.getString(idx)
                    }
                }
            }
            val resolvedName = name ?: uri.path
            val dot = resolvedName?.lastIndexOf('.') ?: -1
            if (resolvedName != null && dot != -1 && dot < resolvedName.length - 1) {
                resolvedName.substring(dot + 1).lowercase()
            } else {
                "jpg"
            }
        } catch (e: Exception) {
            "jpg"
        }
    }

    /**
     * Resolves an Android Content Uri by caching the stream into internal storage
     * within [inputsDir] so Python can read a POSIX path.
     * Active input files are kept strictly isolated from the LRU previews cache.
     */
    suspend fun cacheImageFromUri(uri: Uri, cacheFilename: String): Result<File> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val targetFile = File(inputsDir, cacheFilename)
                val bytesCopied = context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Impossible d'ouvrir le flux pour l'URI: $uri")

                if (!targetFile.exists() || targetFile.length() <= 0L || bytesCopied <= 0L) {
                    targetFile.delete()
                    throw IllegalStateException("Le fichier en cache est vide ou incomplet: $cacheFilename")
                }

                // If not a LUT file, verify that it can be decoded bounds-first
                if (!cacheFilename.endsWith(".cube", ignoreCase = true)) {
                    val ext = targetFile.extension.lowercase()
                    val isRawOrDng = ext in listOf("dng", "raw", "cr2", "nef", "arw", "orf", "rw2")
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    try {
                        BitmapFactory.decodeFile(targetFile.absolutePath, opts)
                        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                            if (isRawOrDng) {
                                if (targetFile.length() < 1024L) {
                                    targetFile.delete()
                                    throw IllegalArgumentException("Fichier RAW/DNG corrompu: $cacheFilename")
                                }
                            } else {
                                targetFile.delete()
                                throw IllegalArgumentException("Format d'image non reconnu ou corrompu: $cacheFilename")
                            }
                        }
                    } catch (t: Throwable) {
                        if (!isRawOrDng) {
                            targetFile.delete()
                            throw IllegalStateException("Échec de vérification du format d'image: ${t.localizedMessage}", t)
                        }
                    }
                }

                targetFile
            }
        }

    /**
     * Invokes the Python bridge to compute and save a preview image (always at 100% intensity for GPU blending).
     */
    suspend fun generatePreview(
        srcPath: String,
        refPath: String,
        method: ColorTransferMethod,
        intensity: Float = 1.0f,
        preserveLuminance: Float = com.stealzalut.mobile.core.AppConstants.PRESERVE_LUMINANCE_DEFAULT,
        skinProtection: Float = com.stealzalut.mobile.core.AppConstants.SKIN_PROTECTION_DEFAULT,
        smoothSigma: Float = com.stealzalut.mobile.core.AppConstants.SMOOTH_SIGMA_DEFAULT,
        neutralLock: Float = com.stealzalut.mobile.core.AppConstants.NEUTRAL_LOCK_DEFAULT
    ): PreviewResult = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val previewOutputFile = lruCacheManager.createPreviewFile("preview_")
            val pyResult: PyObject = bridge.callAttr(
                "process_preview",
                srcPath,
                refPath,
                method.key,
                intensity.toDouble(),
                previewOutputFile.absolutePath,
                1024,
                preserveLuminance.toDouble(),
                skinProtection.toDouble(),
                smoothSigma.toDouble(),
                neutralLock.toDouble()
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "preview_path")?.toString()
                    ?: previewOutputFile.absolutePath
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                PreviewResult.Success(previewPath = path, executionTimeMs = timeMs)
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                PreviewResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            PreviewResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Invokes the Python bridge to generate the Adobe .cube 3D LUT file.
     */
    suspend fun exportCube(
        srcPath: String,
        refPath: String,
        lutSize: LutSize,
        method: ColorTransferMethod,
        intensity: Float,
        preserveLuminance: Float = com.stealzalut.mobile.core.AppConstants.PRESERVE_LUMINANCE_DEFAULT,
        skinProtection: Float = com.stealzalut.mobile.core.AppConstants.SKIN_PROTECTION_DEFAULT,
        smoothSigma: Float = com.stealzalut.mobile.core.AppConstants.SMOOTH_SIGMA_DEFAULT,
        neutralLock: Float = com.stealzalut.mobile.core.AppConstants.NEUTRAL_LOCK_DEFAULT,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        whites: Float = 0.0f,
        blacks: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f
    ): CubeExportResult = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.cacheDir, "shared_luts").apply { mkdirs() }
            outputDir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3600_000L) it.delete() }
            val timestamp = System.currentTimeMillis()
            val uniqueTag = java.util.UUID.randomUUID().toString().take(8)
            val cubeFile = File(outputDir, "StealZaLUT_${timestamp}_${uniqueTag}_${lutSize.size}.cube")

            val pyResult: PyObject = bridge.callAttr(
                "export_cube_file",
                srcPath,
                refPath,
                cubeFile.absolutePath,
                lutSize.size,
                method.key,
                intensity.toDouble(),
                preserveLuminance.toDouble(),
                skinProtection.toDouble(),
                smoothSigma.toDouble(),
                neutralLock.toDouble(),
                exposure.toDouble(),
                contrast.toDouble(),
                highlights.toDouble(),
                shadows.toDouble(),
                whites.toDouble(),
                blacks.toDouble(),
                vibrance.toDouble(),
                saturation.toDouble()
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "lut_path")?.toString()
                    ?: pyResult.callAttr("get", "cube_path")?.toString()
                    ?: cubeFile.absolutePath
                val target = File(path)
                if (!target.exists() || !target.isFile || target.length() <= 0L) {
                    android.util.Log.e("PythonBridgeRepository", "Python reported success but generated cube file is missing or empty: $path")
                    val domainError = LutDomainError.ExportIoError("Fichier .cube généré introuvable ou vide sur le stockage interne")
                    CubeExportResult.Error(error = domainError, customMessage = "Fichier .cube généré introuvable ou vide sur le stockage interne")
                } else {
                    val size = pyResult.callAttr("get", "lut_size")?.toInt() ?: lutSize.size
                    val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                    CubeExportResult.Success(cubePath = target.canonicalPath, lutSize = size, executionTimeMs = timeMs)
                }
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                CubeExportResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            CubeExportResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Directly exports the graded image in high resolution.
     */
    suspend fun exportGradedImage(
        srcPath: String,
        refPath: String,
        method: ColorTransferMethod,
        intensity: Float,
        maxExportDim: Int = 2048,
        format: String = "JPEG",
        quality: Int = 95,
        preserveLuminance: Float = com.stealzalut.mobile.core.AppConstants.PRESERVE_LUMINANCE_DEFAULT,
        skinProtection: Float = com.stealzalut.mobile.core.AppConstants.SKIN_PROTECTION_DEFAULT,
        smoothSigma: Float = com.stealzalut.mobile.core.AppConstants.SMOOTH_SIGMA_DEFAULT,
        neutralLock: Float = com.stealzalut.mobile.core.AppConstants.NEUTRAL_LOCK_DEFAULT,
        lutSize: LutSize = LutSize.STANDARD,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        whites: Float = 0.0f,
        blacks: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f
    ): ImageExportResult = withContext(pythonDispatcher) {
        val memoryDecision = resolveExportMemory(srcPath, maxExportDim)
            ?: return@withContext ImageExportResult.Error(LutDomainError.OutOfMemoryError)
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val ext = if (format.equals("PNG", ignoreCase = true)) "png" else "jpg"
            val outputDir = File(context.cacheDir, "exports_temp").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val imageFile = File(outputDir, "StealZaLUT_Graded_${timestamp}.$ext")

            val pyResult: PyObject = bridge.callAttr(
                "export_graded_image",
                srcPath,
                refPath,
                imageFile.absolutePath,
                method.key,
                intensity.toDouble(),
                memoryDecision.maxDimension,
                quality,
                preserveLuminance.toDouble(),
                skinProtection.toDouble(),
                smoothSigma.toDouble(),
                neutralLock.toDouble(),
                lutSize.size,
                exposure.toDouble(),
                contrast.toDouble(),
                highlights.toDouble(),
                shadows.toDouble(),
                whites.toDouble(),
                blacks.toDouble(),
                vibrance.toDouble(),
                saturation.toDouble(),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                null,
                memoryDecision.wasReducedForMemory
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "image_path")?.toString()
                    ?: imageFile.absolutePath
                val w = pyResult.callAttr("get", "width")?.toInt() ?: 0
                val h = pyResult.callAttr("get", "height")?.toInt() ?: 0
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                ImageExportResult.Success(
                    imagePath = path,
                    width = w,
                    height = h,
                    executionTimeMs = timeMs,
                    wasReducedForMemory = memoryDecision.wasReducedForMemory
                )
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                ImageExportResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            ImageExportResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    
    
    
    /**
     * Computes LightCraft auto exposure and tone settings for an image.
     */
    suspend fun computeAutoSettings(imagePath: String): AutoSettingsResult = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val pyResult: PyObject = bridge.callAttr("compute_auto_settings", imagePath)

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val exposure = pyResult.callAttr("get", "exposure")?.toDouble()?.toFloat() ?: 0.0f
                val contrast = pyResult.callAttr("get", "contrast")?.toDouble()?.toFloat() ?: 68.0f
                val highlights = pyResult.callAttr("get", "highlights")?.toDouble()?.toFloat() ?: -66.0f
                val shadows = pyResult.callAttr("get", "shadows")?.toDouble()?.toFloat() ?: 52.0f
                val whites = pyResult.callAttr("get", "whites")?.toDouble()?.toFloat() ?: 0.0f
                val blacks = pyResult.callAttr("get", "blacks")?.toDouble()?.toFloat() ?: 0.0f
                val vibrance = pyResult.callAttr("get", "vibrance")?.toDouble()?.toFloat() ?: 30.0f
                val saturation = pyResult.callAttr("get", "saturation")?.toDouble()?.toFloat() ?: 0.0f
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                AutoSettingsResult.Success(
                    exposure = exposure,
                    contrast = contrast,
                    highlights = highlights,
                    shadows = shadows,
                    whites = whites,
                    blacks = blacks,
                    vibrance = vibrance,
                    saturation = saturation,
                    executionTimeMs = timeMs
                )
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                AutoSettingsResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            AutoSettingsResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Extracts dominant colors from an image path as hex strings.
     */
    suspend fun extractPalette(imagePath: String, numColors: Int = 6): List<String> =
        withContext(pythonDispatcher) {
            suspendRunCatching {
                val py = PythonRuntime.ensureStarted(context)
                val bridge = py.getModule("stealzalut.bridge")
                val pyResult = bridge.callAttr("extract_palette", imagePath, numColors)
                val status = pyResult.callAttr("get", "status")?.toString()
                if (status == "success") {
                    val listPy = pyResult.callAttr("get", "palette")
                    val result = mutableListOf<String>()
                    if (listPy != null) {
                        val count = listPy.callAttr("__len__")?.toInt() ?: 0
                        for (i in 0 until count) {
                            listPy.callAttr("__getitem__", i)?.toString()?.let { result.add(it) }
                        }
                    }
                    result
                } else {
                    emptyList()
                }
            }.getOrDefault(emptyList())
        }

    /**
     * Analyze a source image into JSON-friendly zone descriptors using the desktop-aligned
     * zone engine, without relying on any GPU or extra native dependencies.
     */
    suspend fun analyzeZonePalette(
        imagePath: String,
        zoneCount: Int = 8
    ): Result<ZonePaletteAnalysis> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr("analyze_zone_palette_bridge", imagePath, zoneCount)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") {
                val message = pyResult.callAttr("get", "message")?.toString() ?: "Zone analysis failed"
                throw IllegalStateException(message)
            }

            fun floatList(value: PyObject): List<Float> =
                value.asList().map { item -> item.toFloat() }

            val zoneList = pyResult.callAttr("get", "zones").asList()
            val zones = zoneList.map { zoneObj ->
                val mean = floatList(zoneObj.callAttr("get", "mean"))
                val centroid = floatList(zoneObj.callAttr("get", "centroid_xy"))
                val bbox = floatList(zoneObj.callAttr("get", "bbox"))
                ZoneDescriptor(
                    id = zoneObj.callAttr("get", "id")?.toInt() ?: 0,
                    area = zoneObj.callAttr("get", "area")?.toFloat() ?: 0f,
                    lRank = zoneObj.callAttr("get", "l_rank")?.toFloat() ?: 0f,
                    mean = mean,
                    hex = zoneObj.callAttr("get", "hex")?.toString() ?: "#000000",
                    centroidXY = centroid,
                    bbox = bbox
                )
            }

            val labelsShape = (pyResult.callAttr("get", "labels_shape").asList() ?: emptyList())
                .map { item -> item.toInt() }

            ZonePaletteAnalysis(
                zoneCount = pyResult.callAttr("get", "zone_count")?.toInt() ?: zones.size,
                zones = zones,
                labelsShape = labelsShape
            )
        }
    }

    /**
     * Compute source/reference zone matching using the Sinkhorn assignment logic exposed by the
     * desktop-aligned Python zone engine.
     */
    suspend fun matchZonePalette(
        sourcePath: String,
        referencePath: String,
        zoneCount: Int = 8
    ): Result<ZoneMatching> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr("match_zone_palette_bridge", sourcePath, referencePath, zoneCount)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") {
                val message = pyResult.callAttr("get", "message")?.toString() ?: "Zone matching failed"
                throw IllegalStateException(message)
            }

            val matchingPy = pyResult.callAttr("get", "matching_entries")
            val matching = mutableMapOf<String, Map<String, Float>>()
            val sourceCount = matchingPy.callAttr("__len__")?.toInt() ?: 0
            for (i in 0 until sourceCount) {
                val sourceEntry = matchingPy.callAttr("__getitem__", i)
                val sourceKey = sourceEntry.callAttr("get", "source_zone")?.toString() ?: "0"
                val targetEntriesPy = sourceEntry.callAttr("get", "targets")
                val targetMap = mutableMapOf<String, Float>()
                val targetCount = targetEntriesPy.callAttr("__len__")?.toInt() ?: 0
                for (j in 0 until targetCount) {
                    val targetEntry = targetEntriesPy.callAttr("__getitem__", j)
                    val targetKey = targetEntry.callAttr("get", "target_zone")?.toString() ?: "0"
                    val targetValue = targetEntry.callAttr("get", "weight")?.toFloat() ?: 0f
                    targetMap[targetKey] = targetValue
                }
                matching[sourceKey] = targetMap
            }
            ZoneMatching(matching = matching)
        }
    }

    /**
     * Fit a zone-aware transfer model and summarize its source/reference correspondence.
     */
    suspend fun fitZoneTransfer(
        sourcePath: String,
        referencePath: String,
        zoneCount: Int = 8
    ): Result<ZoneTransferSummary> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr("fit_zone_transfer_bridge", sourcePath, referencePath, zoneCount)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") {
                val message = pyResult.callAttr("get", "message")?.toString() ?: "Zone transfer fit failed"
                throw IllegalStateException(message)
            }

            val correspondencePy = pyResult.callAttr("get", "correspondence")
            val correspondenceCount = correspondencePy.callAttr("__len__")?.toInt() ?: 0
            val correspondence = mutableListOf<ZoneLink>()
            for (i in 0 until correspondenceCount) {
                val linkPy = correspondencePy.callAttr("__getitem__", i)
                val targetsPy = linkPy.callAttr("get", "target_entries")
                val targets = mutableMapOf<String, Float>()
                val targetCount = targetsPy.callAttr("__len__")?.toInt() ?: 0
                for (j in 0 until targetCount) {
                    val targetEntry = targetsPy.callAttr("__getitem__", j)
                    val targetKey = targetEntry.callAttr("get", "target_zone")?.toString() ?: "0"
                    val targetValue = targetEntry.callAttr("get", "weight")?.toFloat() ?: 0f
                    targets[targetKey] = targetValue
                }
                correspondence.add(
                    ZoneLink(
                        sourceZone = linkPy.callAttr("get", "source_zone")?.toInt() ?: 0,
                        targets = targets,
                        strength = linkPy.callAttr("get", "strength")?.toFloat() ?: 0f,
                        locked = linkPy.callAttr("get", "locked")?.toBoolean() ?: false
                    )
                )
            }

            ZoneTransferSummary(
                sourceZoneIds = pyResult.callAttr("get", "source_zone_ids").let {
                    val size = it.callAttr("__len__")?.toInt() ?: 0
                    buildList {
                        for (i in 0 until size) add(it.callAttr("__getitem__", i)?.toInt() ?: 0)
                    }
                },
                referenceZoneIds = pyResult.callAttr("get", "reference_zone_ids").let {
                    val size = it.callAttr("__len__")?.toInt() ?: 0
                    buildList {
                        for (i in 0 until size) add(it.callAttr("__getitem__", i)?.toInt() ?: 0)
                    }
                },
                analysisId = pyResult.callAttr("get", "analysis_id")?.toString() ?: "",
                method = pyResult.callAttr("get", "method")?.toString() ?: "zones",
                correspondence = correspondence
            )
        }
    }

    /**
     * Parse AI-produced recipe text or raw JSON into a normalized, structured payload.
     */
    suspend fun parseRecipeBridge(recipeText: String): Result<RecipePayload> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr("parse_recipe_bridge", recipeText)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") {
                val message = pyResult.callAttr("get", "message")?.toString() ?: "Recipe parsing failed"
                throw IllegalStateException(message)
            }

            val recipeMapPy = pyResult.callAttr("get", "recipe")
            val recipeMap = mutableMapOf<String, Any>()
            val keysPy = recipeMapPy.callAttr("keys")
            val keyCount = keysPy.callAttr("__len__")?.toInt() ?: 0
            for (i in 0 until keyCount) {
                val key = keysPy.callAttr("__getitem__", i)?.toString() ?: continue
                val value = recipeMapPy.callAttr("__getitem__", key)
                recipeMap[key] = value.toString()
            }

            RecipePayload(
                analysisId = pyResult.callAttr("get", "analysis_id")?.toString(),
                mode = pyResult.callAttr("get", "mode")?.toString(),
                zoneCount = pyResult.callAttr("get", "zone_count")?.toInt(),
                recipe = recipeMap,
                json = pyResult.callAttr("get", "json")?.toString() ?: ""
            )
        }
    }

    /**
     * Build the lightweight AI analysis pack for a source/reference pair and return the folder path.
     */
    suspend fun writeAiPack(
        sourcePath: String,
        referencePath: String,
        outputDir: String,
        packName: String = "demo",
        zoneCount: Int = 8
    ): Result<AnalysisPackExport> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr("write_ai_pack_bridge", sourcePath, referencePath, outputDir, packName, zoneCount)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") {
                val message = pyResult.callAttr("get", "message")?.toString() ?: "AI pack export failed"
                throw IllegalStateException(message)
            }

            val filesPy = pyResult.callAttr("get", "files")
            val files = mutableListOf<String>()
            val fileCount = filesPy.callAttr("__len__")?.toInt() ?: 0
            for (i in 0 until fileCount) {
                files.add(filesPy.callAttr("__getitem__", i)?.toString() ?: "")
            }

            AnalysisPackExport(
                packDir = pyResult.callAttr("get", "pack_dir")?.toString() ?: outputDir,
                packName = pyResult.callAttr("get", "pack_name")?.toString() ?: packName,
                analysisId = pyResult.callAttr("get", "analysis_id")?.toString() ?: "",
                files = files,
                prompt = pyResult.callAttr("get", "prompt")?.toString() ?: ""
            )
        }
    }

    /**
     * Generates a visual calibration color card showcasing how the reference style
     * transforms highlights, grayscale ramp, midtones, skin tones, and gamut primaries.
     */
    suspend fun generateColorCard(
        refPath: String,
        outputCardPath: String,
        method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
        intensity: Float = 1.0f
    ): Result<String> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult = bridge.callAttr(
                "generate_color_card",
                refPath,
                outputCardPath,
                method.key,
                intensity.toDouble()
            )
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                pyResult.callAttr("get", "card_path")?.toString() ?: outputCardPath
            } else {
                val msg = pyResult.callAttr("get", "message")?.toString() ?: "Erreur génération mire"
                throw IllegalStateException(msg)
            }
        }
    }

    /**
     * Saves an image directly into the Android native MediaStore gallery (Pictures/StealZaLUT).
     */
    suspend fun saveImageToMediaStore(
        imagePath: String,
        displayName: String = "StealZaLUT_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val sourceFile = File(imagePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Fichier source introuvable: $imagePath")
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StealZaLUT")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Impossible d'insérer dans MediaStore")

            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Impossible d'écrire dans MediaStore URI: $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            uri
        }
    }

    
    
    
    /**
     * Generates a preview for the Standalone Photo Editor (Single photo, no reference image).
     */
    suspend fun generateEditorPreview(
        srcPath: String,
        lutSize: LutSize = LutSize.STANDARD,
        intensity: Float = 1.0f,
        lumaLock: Float = 0.0f,
        neutralAxisLock: Float = 0.0f,
        skinToneLock: Float = 0.0f,
        extremesRolloff: Float = 0.0f,
        spatial3d: Float = 0.0f,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        temperature: Float = 0.0f,
        tint: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f,
        clarity: Float = 0.0f,
        sharpen: Float = 0.0f,
        grain: Float = 0.0f,
        customLutPath: String? = null,
        lightroomParams: LightroomDevelopModel? = null,
        cropRegion: com.stealzalut.mobile.data.model.CropRegion? = null
    ): PreviewResult = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val previewOutputFile = lruCacheManager.createPreviewFile("editor_preview_")
            val pyResult: PyObject = bridge.callAttr(
                "process_editor_preview",
                srcPath,
                previewOutputFile.absolutePath,
                1024,
                lutSize.size,
                intensity.toDouble(),
                lumaLock.toDouble(),
                neutralAxisLock.toDouble(),
                skinToneLock.toDouble(),
                extremesRolloff.toDouble(),
                spatial3d.toDouble(),
                exposure.toDouble(),
                contrast.toDouble(),
                highlights.toDouble(),
                shadows.toDouble(),
                temperature.toDouble(),
                tint.toDouble(),
                vibrance.toDouble(),
                saturation.toDouble(),
                clarity.toDouble(),
                sharpen.toDouble(),
                grain.toDouble(),
                customLutPath,
                if (lightroomParams != null || (cropRegion != null && cropRegion.hasCrop)) {
                    LightroomSerializer.toJson(lightroomParams ?: LightroomDevelopModel(), cropRegion)
                } else null
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "preview_path")?.toString() ?: previewOutputFile.absolutePath
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                PreviewResult.Success(previewPath = path, executionTimeMs = timeMs)
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                PreviewResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            PreviewResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Exports an Adobe .cube 3D LUT from the Standalone Editor settings.
     */
    suspend fun exportEditorCube(
        srcPath: String,
        lutSize: LutSize,
        intensity: Float = 1.0f,
        lumaLock: Float = 0.0f,
        neutralAxisLock: Float = 0.0f,
        skinToneLock: Float = 0.0f,
        extremesRolloff: Float = 0.0f,
        spatial3d: Float = 0.0f,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        temperature: Float = 0.0f,
        tint: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f,
        customLutPath: String? = null,
        lightroomParams: LightroomDevelopModel? = null
    ): CubeExportResult = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.cacheDir, "shared_luts").apply { mkdirs() }
            outputDir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 3600_000L) it.delete() }
            val timestamp = System.currentTimeMillis()
            val uniqueTag = java.util.UUID.randomUUID().toString().take(8)
            val cubeFile = File(outputDir, "StealZaLUT_Editor_${timestamp}_${uniqueTag}_${lutSize.size}.cube")

            val pyResult: PyObject = bridge.callAttr(
                "export_cube_file",
                srcPath,
                "",  // No reference
                cubeFile.absolutePath,
                lutSize.size,
                "reinhard",
                intensity.toDouble(),
                0.0,
                0.0,
                0.0,
                0.0,
                exposure.toDouble(),
                contrast.toDouble(),
                highlights.toDouble(),
                shadows.toDouble(),
                0.0,
                0.0,
                vibrance.toDouble(),
                saturation.toDouble(),
                temperature.toDouble(),
                tint.toDouble(),
                0.0,
                0.0,
                0.0,
                lumaLock.toDouble(),
                neutralAxisLock.toDouble(),
                skinToneLock.toDouble(),
                extremesRolloff.toDouble(),
                spatial3d.toDouble(),
                customLutPath,
                lightroomParams?.let { LightroomSerializer.toJson(it) }
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "lut_path")?.toString()
                    ?: pyResult.callAttr("get", "cube_path")?.toString()
                    ?: cubeFile.absolutePath
                val target = File(path)
                if (!target.exists() || !target.isFile || target.length() <= 0L) {
                    android.util.Log.e("PythonBridgeRepository", "Python reported success but generated editor cube file is missing or empty: $path")
                    val domainError = LutDomainError.ExportIoError("Fichier .cube généré introuvable ou vide sur le stockage interne")
                    CubeExportResult.Error(error = domainError, customMessage = "Fichier .cube généré introuvable ou vide sur le stockage interne")
                } else {
                    val size = pyResult.callAttr("get", "lut_size")?.toInt() ?: lutSize.size
                    val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                    CubeExportResult.Success(cubePath = target.canonicalPath, lutSize = size, executionTimeMs = timeMs)
                }
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                CubeExportResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            CubeExportResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Exports a graded image from the Standalone Editor in high resolution.
     */
    suspend fun exportEditorImage(
        srcPath: String,
        maxExportDim: Int = 2048,
        format: String = "JPEG",
        quality: Int = 95,
        lutSize: LutSize = LutSize.STANDARD,
        intensity: Float = 1.0f,
        lumaLock: Float = 0.0f,
        neutralAxisLock: Float = 0.0f,
        skinToneLock: Float = 0.0f,
        extremesRolloff: Float = 0.0f,
        spatial3d: Float = 0.0f,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        temperature: Float = 0.0f,
        tint: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f,
        clarity: Float = 0.0f,
        sharpen: Float = 0.0f,
        grain: Float = 0.0f,
        customLutPath: String? = null,
        lightroomParams: LightroomDevelopModel? = null,
        cropRegion: com.stealzalut.mobile.data.model.CropRegion? = null
    ): ImageExportResult = withContext(pythonDispatcher) {
        val memoryDecision = resolveExportMemory(srcPath, maxExportDim)
            ?: return@withContext ImageExportResult.Error(LutDomainError.OutOfMemoryError)
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.cacheDir, "exports_temp").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val ext = if (format.equals("PNG", true)) "png" else "jpg"
            val imageFile = File(outputDir, "StealZaLUT_Editor_Graded_${timestamp}.$ext")

            val pyResult: PyObject = bridge.callAttr(
                "export_graded_image",
                srcPath,
                "",  // No reference
                imageFile.absolutePath,
                "reinhard",
                intensity.toDouble(),
                memoryDecision.maxDimension,
                quality,
                0.0,
                0.0,
                0.0,
                0.0,
                lutSize.size,
                exposure.toDouble(),
                contrast.toDouble(),
                highlights.toDouble(),
                shadows.toDouble(),
                0.0,
                0.0,
                vibrance.toDouble(),
                saturation.toDouble(),
                temperature.toDouble(),
                tint.toDouble(),
                clarity.toDouble(),
                sharpen.toDouble(),
                grain.toDouble(),
                lumaLock.toDouble(),
                neutralAxisLock.toDouble(),
                skinToneLock.toDouble(),
                extremesRolloff.toDouble(),
                spatial3d.toDouble(),
                customLutPath,
                if (lightroomParams != null || (cropRegion != null && cropRegion.hasCrop)) {
                    LightroomSerializer.toJson(lightroomParams ?: LightroomDevelopModel(), cropRegion)
                } else null,
                memoryDecision.wasReducedForMemory
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "image_path")?.toString() ?: imageFile.absolutePath
                val w = pyResult.callAttr("get", "width")?.toInt() ?: 0
                val h = pyResult.callAttr("get", "height")?.toInt() ?: 0
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                ImageExportResult.Success(
                    imagePath = path,
                    width = w,
                    height = h,
                    executionTimeMs = timeMs,
                    wasReducedForMemory = memoryDecision.wasReducedForMemory
                )
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                ImageExportResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            ImageExportResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Saves an editing preset using the Python Bridge and preset manager.
     */
    suspend fun saveEditingPresetBridge(preset: EditPreset): Result<String> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")

            val presetsDir = File(context.filesDir, "szl_presets").apply { mkdirs() }

            val step2Map = mapOf(
                "global_intensity" to preset.step2Params.globalIntensity.toDouble(),
                "luma_lock" to preset.step2Params.lumaLock.toDouble(),
                "neutral_axis_lock" to preset.step2Params.neutralAxisLock.toDouble(),
                "skin_tone_lock" to preset.step2Params.skinToneLock.toDouble(),
                "extremes_rolloff" to preset.step2Params.extremesRolloff.toDouble(),
                "spatial_3d" to preset.step2Params.spatial3d.toDouble()
            )

            val step3Map = mapOf(
                "exposure" to preset.step3Params.exposure.toDouble(),
                "contrast" to preset.step3Params.contrast.toDouble(),
                "highlights" to preset.step3Params.highlights.toDouble(),
                "shadows" to preset.step3Params.shadows.toDouble(),
                "temperature" to preset.step3Params.temperature.toDouble(),
                "tint" to preset.step3Params.tint.toDouble(),
                "vibrance" to preset.step3Params.vibrance.toDouble(),
                "saturation" to preset.step3Params.saturation.toDouble(),
                "clarity" to preset.step3Params.clarity.toDouble(),
                "sharpen" to preset.step3Params.sharpen.toDouble(),
                "grain" to preset.step3Params.grain.toDouble()
            )

            val lrJson = LightroomSerializer.toJson(preset.lightroomParams)

            val pyResult = bridge.callAttr(
                "save_preset_bridge",
                preset.name,
                step2Map,
                step3Map,
                preset.lutReference,
                preset.thumbnailBase64,
                presetsDir.absolutePath,
                lrJson
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                pyResult.callAttr("get", "preset_path")?.toString() ?: ""
            } else {
                val msg = pyResult.callAttr("get", "message")?.toString() ?: "Erreur sauvegarde preset"
                throw IllegalStateException(msg)
            }
        }
    }

    /**
     * Lists all saved editing presets (.szlpreset).
     */
    suspend fun listEditingPresetsBridge(): List<EditPreset> = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets").apply { mkdirs() }

            val pyResult = bridge.callAttr("list_presets_bridge", presetsDir.absolutePath)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") return@suspendRunCatching emptyList<EditPreset>()

            val pyList = pyResult.callAttr("get", "presets") ?: return@suspendRunCatching emptyList<EditPreset>()
            val count = pyList.callAttr("__len__")?.toInt() ?: 0
            val list = mutableListOf<EditPreset>()

            for (i in 0 until count) {
                val item = pyList.callAttr("__getitem__", i) ?: continue
                val id = item.callAttr("get", "id")?.toString() ?: ""
                val name = item.callAttr("get", "name")?.toString() ?: "Sans titre"
                val createdAt = item.callAttr("get", "created_at")?.toLong() ?: System.currentTimeMillis()
                val lutRef = item.callAttr("get", "lut_reference")?.toString()
                val thumb = item.callAttr("get", "thumbnail_base64")?.toString()
                val filePath = item.callAttr("get", "_file_path")?.toString()

                val s2Obj = item.callAttr("get", "step2_params")
                val s2 = Step2Model(
                    globalIntensity = s2Obj?.callAttr("get", "global_intensity")?.toDouble()?.toFloat() ?: 1.0f,
                    lumaLock = s2Obj?.callAttr("get", "luma_lock")?.toDouble()?.toFloat() ?: 0.0f,
                    neutralAxisLock = s2Obj?.callAttr("get", "neutral_axis_lock")?.toDouble()?.toFloat() ?: 0.0f,
                    skinToneLock = s2Obj?.callAttr("get", "skin_tone_lock")?.toDouble()?.toFloat() ?: 0.0f,
                    extremesRolloff = s2Obj?.callAttr("get", "extremes_rolloff")?.toDouble()?.toFloat() ?: 0.0f,
                    spatial3d = s2Obj?.callAttr("get", "spatial_3d")?.toDouble()?.toFloat() ?: 0.0f
                )

                val s3Obj = item.callAttr("get", "step3_params")
                val s3 = Step3Model(
                    exposure = s3Obj?.callAttr("get", "exposure")?.toDouble()?.toFloat() ?: 0.0f,
                    contrast = s3Obj?.callAttr("get", "contrast")?.toDouble()?.toFloat() ?: 0.0f,
                    highlights = s3Obj?.callAttr("get", "highlights")?.toDouble()?.toFloat() ?: 0.0f,
                    shadows = s3Obj?.callAttr("get", "shadows")?.toDouble()?.toFloat() ?: 0.0f,
                    temperature = s3Obj?.callAttr("get", "temperature")?.toDouble()?.toFloat() ?: 0.0f,
                    tint = s3Obj?.callAttr("get", "tint")?.toDouble()?.toFloat() ?: 0.0f,
                    vibrance = s3Obj?.callAttr("get", "vibrance")?.toDouble()?.toFloat() ?: 0.0f,
                    saturation = s3Obj?.callAttr("get", "saturation")?.toDouble()?.toFloat() ?: 0.0f,
                    clarity = s3Obj?.callAttr("get", "clarity")?.toDouble()?.toFloat() ?: 0.0f,
                    sharpen = s3Obj?.callAttr("get", "sharpen")?.toDouble()?.toFloat() ?: 0.0f,
                    grain = s3Obj?.callAttr("get", "grain")?.toDouble()?.toFloat() ?: 0.0f
                )

                val lrObj = item.callAttr("get", "lightroom_params")
                val lrParams = if (lrObj != null && !lrObj.toString().equals("null", ignoreCase = true)) {
                    val str = lrObj.toString()
                    val lrJson = if (str.startsWith("{")) str else {
                        // In case Chaquopy represented it as a map or json representation
                        suspendRunCatching {
                            val map = lrObj.asMap()
                            JSONObject(map.map { (k, v) -> k.toString() to v.toString() }.toMap()).toString()
                        }.getOrDefault("{}")
                    }
                    suspendRunCatching { LightroomDeserializer.fromJson(lrJson) }.getOrDefault(LightroomDevelopModel())
                } else {
                    LightroomDevelopModel()
                }

                list.add(
                    EditPreset(
                        id = id,
                        name = name,
                        createdAt = createdAt,
                        thumbnailBase64 = thumb,
                        lutReference = lutRef,
                        step2Params = s2,
                        step3Params = s3,
                        lightroomParams = lrParams,
                        filePath = filePath
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    /**
     * Deletes a saved editing preset.
     */
    suspend fun deleteEditingPresetBridge(presetIdOrPath: String): Boolean = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")

            val pyResult = bridge.callAttr("delete_preset_bridge", presetIdOrPath, presetsDir.absolutePath)
            pyResult.callAttr("get", "deleted")?.toBoolean() ?: false
        }.getOrDefault(false)
    }

    /**
     * Duplicates an existing editing preset.
     */
    suspend fun duplicateEditingPresetBridge(presetIdOrPath: String, newName: String? = null): Boolean = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")
            val pyResult = bridge.callAttr("duplicate_preset_bridge", presetIdOrPath, newName, presetsDir.absolutePath)
            pyResult.callAttr("get", "status")?.toString() == "success"
        }.getOrDefault(false)
    }

    /**
     * Renames an existing editing preset.
     */
    suspend fun renameEditingPresetBridge(presetIdOrPath: String, newName: String): Boolean = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")
            val pyResult = bridge.callAttr("rename_preset_bridge", presetIdOrPath, newName, presetsDir.absolutePath)
            pyResult.callAttr("get", "status")?.toString() == "success"
        }.getOrDefault(false)
    }

    /**
     * Exports an editing preset to a file.
     */
    suspend fun exportEditingPresetBridge(presetIdOrPath: String, exportPath: String): String? = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")
            val pyResult = bridge.callAttr("export_preset_bridge", presetIdOrPath, exportPath, presetsDir.absolutePath)
            if (pyResult.callAttr("get", "status")?.toString() == "success") {
                pyResult.callAttr("get", "export_path")?.toString()
            } else null
        }.getOrNull()
    }

    /**
     * Imports an editing preset from a file.
     */
    suspend fun importEditingPresetBridge(importPath: String): Boolean = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")
            val pyResult = bridge.callAttr("import_preset_bridge", importPath, presetsDir.absolutePath)
            pyResult.callAttr("get", "status")?.toString() == "success"
        }.getOrDefault(false)
    }

    suspend fun clearCaches(): Boolean = withContext(pythonDispatcher) {
        suspendRunCatching {
            val py = PythonRuntime.ensureStarted(context)
            val bridge = py.getModule("stealzalut.bridge")
            val pyResult: PyObject = bridge.callAttr("clear_caches")
            pyResult.callAttr("get", "status")?.toString() == "success"
        }.getOrDefault(false)
    }

    private fun resolveExportMemory(
        srcPath: String,
        requestedMaxDimension: Int
    ): ExportMemoryDecision? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return ExportMemoryGuard.fallback(requestedMaxDimension)
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return ExportMemoryGuard.fallback(requestedMaxDimension)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return ExportMemoryGuard.resolve(
            width = bounds.outWidth,
            height = bounds.outHeight,
            requestedMaxDimension = requestedMaxDimension,
            availableMemoryBytes = memoryInfo.availMem
        )
    }

    /**
     * Builds a Share Intent for any generated file using FileProvider.
     */
    fun createShareIntent(filePath: String, mimeType: String = "*/*"): Intent {
        val file = File(filePath)
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
