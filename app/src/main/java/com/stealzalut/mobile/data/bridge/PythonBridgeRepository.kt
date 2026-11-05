package com.stealzalut.mobile.data.bridge

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.stealzalut.mobile.data.cache.LruCacheManager
import com.stealzalut.mobile.data.model.AutoEditResult
import com.stealzalut.mobile.data.model.AutoSettingsResult
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.EditPreset
import com.stealzalut.mobile.data.model.EditResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutDomainError
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
import com.stealzalut.mobile.data.model.Step2Model
import com.stealzalut.mobile.data.model.Step3Model
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
    private val pythonDispatcher = Dispatchers.IO.limitedParallelism(1)

    val lruCacheManager: LruCacheManager = LruCacheManager(
        File(context.cacheDir, LruCacheManager.PREVIEWS_DIR_NAME)
    )

    /**
     * Resolves an Android Content Uri by caching the stream into internal storage
     * so Python can read a POSIX path.
     */
    suspend fun cacheImageFromUri(uri: Uri, cacheFilename: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val previewsDir = lruCacheManager.previewDir
                val targetFile = File(previewsDir, cacheFilename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Impossible d'ouvrir le flux pour l'URI: $uri")
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
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.filesDir, "luts").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val cubeFile = File(outputDir, "StealZaLUT_${timestamp}_${lutSize.size}.cube")

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
                val size = pyResult.callAttr("get", "lut_size")?.toInt() ?: lutSize.size
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                CubeExportResult.Success(cubePath = path, lutSize = size, executionTimeMs = timeMs)
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
        runCatching {
            val py = Python.getInstance()
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
                maxExportDim,
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
                saturation.toDouble()
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "image_path")?.toString()
                    ?: imageFile.absolutePath
                val w = pyResult.callAttr("get", "width")?.toInt() ?: 0
                val h = pyResult.callAttr("get", "height")?.toInt() ?: 0
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                ImageExportResult.Success(imagePath = path, width = w, height = h, executionTimeMs = timeMs)
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
     * Executes the reverse-engineered LightCraft Auto Edit feature on a source image.
     * Computes exposure, tone curves, and vibrance, and writes a WYSIWYG preview file.
     */
    suspend fun generateAutoEditPreview(srcPath: String): AutoEditResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val previewOutputFile = lruCacheManager.createPreviewFile("auto_edit_preview_")
            val pyResult: PyObject = bridge.callAttr(
                "compute_auto_edit",
                srcPath,
                previewOutputFile.absolutePath,
                1024,
                33
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "preview_path")?.toString() ?: previewOutputFile.absolutePath
                val exposure = pyResult.callAttr("get", "exposure")?.toDouble()?.toFloat() ?: 0.0f
                val contrast = pyResult.callAttr("get", "contrast")?.toDouble()?.toFloat() ?: 68.0f
                val highlights = pyResult.callAttr("get", "highlights")?.toDouble()?.toFloat() ?: -66.0f
                val shadows = pyResult.callAttr("get", "shadows")?.toDouble()?.toFloat() ?: 52.0f
                val whites = pyResult.callAttr("get", "whites")?.toDouble()?.toFloat() ?: 0.0f
                val blacks = pyResult.callAttr("get", "blacks")?.toDouble()?.toFloat() ?: 0.0f
                val vibrance = pyResult.callAttr("get", "vibrance")?.toDouble()?.toFloat() ?: 30.0f
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0

                AutoEditResult.Success(
                    previewPath = path,
                    exposure = exposure,
                    contrast = contrast,
                    highlights = highlights,
                    shadows = shadows,
                    whites = whites,
                    blacks = blacks,
                    vibrance = vibrance,
                    executionTimeMs = timeMs
                )
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                AutoEditResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            AutoEditResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Exports the LightCraft Auto Edit grade as an Adobe .cube 3D LUT file.
     */
    suspend fun exportAutoEditCube(
        srcPath: String,
        lutSize: LutSize = LutSize.STANDARD
    ): CubeExportResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.filesDir, "luts").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val cubeFile = File(outputDir, "StealZaLUT_Auto_${timestamp}_${lutSize.size}.cube")

            val pyResult: PyObject = bridge.callAttr(
                "export_auto_edit_cube",
                srcPath,
                cubeFile.absolutePath,
                lutSize.size
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "cube_path")?.toString()
                    ?: pyResult.callAttr("get", "lut_path")?.toString()
                    ?: cubeFile.absolutePath
                val size = pyResult.callAttr("get", "lut_size")?.toInt() ?: lutSize.size
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                CubeExportResult.Success(cubePath = path, lutSize = size, executionTimeMs = timeMs)
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
     * Applies LightCraft edit adjustments (Light & Color) to an image file.
     */
    suspend fun applyImageEdits(
        imagePath: String,
        exposure: Float = 0.0f,
        contrast: Float = 0.0f,
        highlights: Float = 0.0f,
        shadows: Float = 0.0f,
        whites: Float = 0.0f,
        blacks: Float = 0.0f,
        vibrance: Float = 0.0f,
        saturation: Float = 0.0f
    ): EditResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.cacheDir, "edits").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val outputFile = File(outputDir, "edit_${timestamp}.jpg")

            val pyResult: PyObject = bridge.callAttr(
                "apply_image_edits",
                imagePath,
                outputFile.absolutePath,
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
                val path = pyResult.callAttr("get", "edited_path")?.toString() ?: outputFile.absolutePath
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                EditResult.Success(editedPath = path, executionTimeMs = timeMs)
            } else {
                val errorCode = pyResult.callAttr("get", "error_code")?.toString()
                val message = pyResult.callAttr("get", "message")?.toString()
                val domainError = LutDomainError.fromPythonResponse(errorCode, message)
                EditResult.Error(error = domainError, customMessage = message)
            }
        }.getOrElse { error ->
            val domainError = LutDomainError.fromPythonResponse(null, error.localizedMessage ?: error.toString())
            EditResult.Error(error = domainError, customMessage = error.localizedMessage)
        }
    }

    /**
     * Computes LightCraft auto exposure and tone settings for an image.
     */
    suspend fun computeAutoSettings(imagePath: String): AutoSettingsResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
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
            runCatching {
                val py = Python.getInstance()
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
     * Generates a visual calibration color card showcasing how the reference style
     * transforms highlights, grayscale ramp, midtones, skin tones, and gamut primaries.
     */
    suspend fun generateColorCard(
        refPath: String,
        outputCardPath: String,
        method: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
        intensity: Float = 1.0f
    ): Result<String> = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
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
     * Saves a generated .cube LUT file into the user's public Downloads directory
     * via MediaStore on Android 10+ (API 29+) with resilient fallback to public storage.
     */
    suspend fun saveCubeToDownloads(
        cubePath: String,
        displayName: String = File(cubePath).name
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(cubePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Fichier LUT introuvable: $cubePath")
            }

            // Adobe .cube is an ASCII text format; text/plain prevents Android from appending .bin
            val mimeType = "text/plain"
            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StealZaLUT")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { output ->
                            FileInputStream(sourceFile).use { input ->
                                input.copyTo(output)
                            }
                        } ?: throw IllegalStateException("Impossible d'écrire dans MediaStore URI: $uri")

                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        return@runCatching uri
                    }
                } catch (_: Exception) {
                    // Fallback to legacy downloads directory if MediaStore insert fails on specific OEM ROMs
                }
            }

            @Suppress("DEPRECATION")
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "StealZaLUT"
            ).apply { mkdirs() }
            val targetFile = File(downloadsDir, displayName)
            sourceFile.copyTo(targetFile, overwrite = true)
            Uri.fromFile(targetFile)
        }
    }

    /**
     * Generates a synthetic reference sample image (e.g. Cinematic Teal & Orange, Moody, Sunset).
     */
    suspend fun createSampleImage(style: String): File = withContext(Dispatchers.IO) {
        val fileName = "sample_${style.lowercase().replace(" ", "_")}.jpg"
        val sampleFile = File(lruCacheManager.previewDir, fileName)
        if (sampleFile.exists()) return@withContext sampleFile

        val width = 720
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            "Teal & Orange" -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(18, 85, 110), Color.rgb(220, 110, 45), Color.rgb(70, 30, 20)),
                    floatArrayOf(0.0f, 0.65f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            "Moody Forest" -> {
                val gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(20, 35, 30), Color.rgb(40, 75, 55), Color.rgb(15, 20, 25)),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            else -> {
                val gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(240, 20, 120), Color.rgb(30, 10, 60), Color.rgb(0, 230, 240)),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        FileOutputStream(sampleFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
        sampleFile
    }

    /**
     * Copies a generated file to a user-selected destination URI via SAF.
     */
    suspend fun copyFileToDestinationUri(sourceFilePath: String, destinationUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceFile = File(sourceFilePath)
                if (!sourceFile.exists()) {
                    throw IllegalStateException("Le fichier source n'existe pas : $sourceFilePath")
                }
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                    output.flush()
                } ?: throw IllegalStateException("Impossible d'écrire dans la destination choisie.")
                Unit
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
        customLutPath: String? = null
    ): PreviewResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
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
                customLutPath
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
        customLutPath: String? = null
    ): CubeExportResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.filesDir, "luts").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val cubeFile = File(outputDir, "StealZaLUT_Editor_${timestamp}_${lutSize.size}.cube")

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
                customLutPath
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "lut_path")?.toString() ?: cubeFile.absolutePath
                val size = pyResult.callAttr("get", "lut_size")?.toInt() ?: lutSize.size
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                CubeExportResult.Success(cubePath = path, lutSize = size, executionTimeMs = timeMs)
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
        customLutPath: String? = null
    ): ImageExportResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.cacheDir, "exports_temp").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val imageFile = File(outputDir, "StealZaLUT_Editor_Graded_${timestamp}.jpg")

            val pyResult: PyObject = bridge.callAttr(
                "export_graded_image",
                srcPath,
                "",  // No reference
                imageFile.absolutePath,
                "reinhard",
                intensity.toDouble(),
                maxExportDim,
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
                customLutPath
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "image_path")?.toString() ?: imageFile.absolutePath
                val w = pyResult.callAttr("get", "width")?.toInt() ?: 0
                val h = pyResult.callAttr("get", "height")?.toInt() ?: 0
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                ImageExportResult.Success(imagePath = path, width = w, height = h, executionTimeMs = timeMs)
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
        runCatching {
            val py = Python.getInstance()
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

            val pyResult = bridge.callAttr(
                "save_preset_bridge",
                preset.name,
                step2Map,
                step3Map,
                preset.lutReference,
                preset.thumbnailBase64,
                presetsDir.absolutePath
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
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets").apply { mkdirs() }

            val pyResult = bridge.callAttr("list_presets_bridge", presetsDir.absolutePath)
            val status = pyResult.callAttr("get", "status")?.toString()
            if (status != "success") return@runCatching emptyList<EditPreset>()

            val pyList = pyResult.callAttr("get", "presets") ?: return@runCatching emptyList<EditPreset>()
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

                list.add(
                    EditPreset(
                        id = id,
                        name = name,
                        createdAt = createdAt,
                        thumbnailBase64 = thumb,
                        lutReference = lutRef,
                        step2Params = s2,
                        step3Params = s3,
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
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
            val py = Python.getInstance()
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
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")
            val presetsDir = File(context.filesDir, "szl_presets")
            val pyResult = bridge.callAttr("import_preset_bridge", importPath, presetsDir.absolutePath)
            pyResult.callAttr("get", "status")?.toString() == "success"
        }.getOrDefault(false)
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
