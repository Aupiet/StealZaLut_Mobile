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
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.ImageExportResult
import com.stealzalut.mobile.data.model.LutDomainError
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.data.model.PreviewResult
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
        intensity: Float = 1.0f
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
                1024
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
        intensity: Float
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
                intensity.toDouble()
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "cube_path")?.toString()
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
        quality: Int = 95
    ): ImageExportResult = withContext(pythonDispatcher) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val ext = if (format.equals("PNG", ignoreCase = true)) "png" else "jpg"
            val outputDir = File(context.filesDir, "exports").apply { mkdirs() }
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
                quality
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
                } ?: throw IllegalStateException("Impossible d'écrire dans la destination choisie.")
                Unit
            }
        }

    /**
     * Builds a Share Intent for any generated file using FileProvider.
     */
    fun createShareIntent(filePath: String, mimeType: String = "application/octet-stream"): Intent {
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
