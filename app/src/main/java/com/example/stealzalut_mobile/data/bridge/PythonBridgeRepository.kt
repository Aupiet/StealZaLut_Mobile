package com.example.stealzalut_mobile.data.bridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.example.stealzalut_mobile.data.model.ColorSetResult
import com.example.stealzalut_mobile.data.model.ColorTransferMethod
import com.example.stealzalut_mobile.data.model.CubeExportResult
import com.example.stealzalut_mobile.data.model.ImageExportResult
import com.example.stealzalut_mobile.data.model.LutSize
import com.example.stealzalut_mobile.data.model.PreviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository interfacing with Chaquopy Python backend for offline color grading.
 * All computations are executed strictly on background worker threads (Dispatchers.Default).
 */
class PythonBridgeRepository(private val context: Context) {

    /**
     * Resolves an Android Content Uri (e.g. from PhotoPicker / SAF) by copying
     * the stream into the application cache directory so Python can read a POSIX path.
     */
    suspend fun cacheImageFromUri(uri: Uri, cacheFilename: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetFile = File(context.cacheDir, cacheFilename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Impossible d'ouvrir le flux pour l'URI: $uri")
                targetFile
            }
        }

    /**
     * Invokes the Python bridge to compute and save a preview image.
     */
    suspend fun generatePreview(
        srcPath: String,
        refPath: String,
        method: ColorTransferMethod,
        intensity: Float
    ): PreviewResult = withContext(Dispatchers.Default) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val previewOutputFile = File(context.cacheDir, "stealzalut_preview.jpg")
            val pyResult: PyObject = bridge.callAttr(
                "process_preview",
                srcPath,
                refPath,
                method.key,
                intensity.toDouble(),
                previewOutputFile.absolutePath
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "preview_path")?.toString()
                    ?: previewOutputFile.absolutePath
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0
                PreviewResult.Success(previewPath = path, executionTimeMs = timeMs)
            } else {
                val message = pyResult.callAttr("get", "message")?.toString()
                    ?: "Erreur inconnue lors du calcul de l'aperçu"
                PreviewResult.Error(message = message)
            }
        }.getOrElse { error ->
            PreviewResult.Error(message = error.localizedMessage ?: error.toString())
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
    ): CubeExportResult = withContext(Dispatchers.Default) {
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
                val message = pyResult.callAttr("get", "message")?.toString()
                    ?: "Erreur inconnue lors de l'exportation du fichier .cube"
                CubeExportResult.Error(message = message)
            }
        }.getOrElse { error ->
            CubeExportResult.Error(message = error.localizedMessage ?: error.toString())
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
    ): ImageExportResult = withContext(Dispatchers.Default) {
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
                val message = pyResult.callAttr("get", "message")?.toString()
                    ?: "Erreur lors de l'export direct de l'image"
                ImageExportResult.Error(message = message)
            }
        }.getOrElse { error ->
            ImageExportResult.Error(message = error.localizedMessage ?: error.toString())
        }
    }

    /**
     * Generates a neutral calibration test pattern transformed through the look
     * and extracts dominant palettes.
     */
    suspend fun generateColorSet(
        refPath: String,
        method: ColorTransferMethod,
        intensity: Float
    ): ColorSetResult = withContext(Dispatchers.Default) {
        runCatching {
            val py = Python.getInstance()
            val bridge = py.getModule("stealzalut.bridge")

            val outputDir = File(context.filesDir, "colorsets").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val cardFile = File(outputDir, "StealZaLUT_ColorSet_${timestamp}.jpg")

            val pyResult: PyObject = bridge.callAttr(
                "generate_color_set",
                refPath,
                cardFile.absolutePath,
                method.key,
                intensity.toDouble()
            )

            val status = pyResult.callAttr("get", "status")?.toString()
            if (status == "success") {
                val path = pyResult.callAttr("get", "colorset_path")?.toString() ?: cardFile.absolutePath
                val timeMs = pyResult.callAttr("get", "execution_time_ms")?.toDouble() ?: 0.0

                val refPalPy = pyResult.callAttr("get", "ref_palette")
                val gradedPalPy = pyResult.callAttr("get", "graded_palette")

                val refList = mutableListOf<String>()
                val gradedList = mutableListOf<String>()

                if (refPalPy != null) {
                    val count = refPalPy.callAttr("__len__")?.toInt() ?: 0
                    for (i in 0 until count) {
                        refPalPy.callAttr("__getitem__", i)?.toString()?.let { refList.add(it) }
                    }
                }

                if (gradedPalPy != null) {
                    val count = gradedPalPy.callAttr("__len__")?.toInt() ?: 0
                    for (i in 0 until count) {
                        gradedPalPy.callAttr("__getitem__", i)?.toString()?.let { gradedList.add(it) }
                    }
                }

                ColorSetResult.Success(
                    colorSetPath = path,
                    refPalette = refList,
                    gradedPalette = gradedList,
                    executionTimeMs = timeMs
                )
            } else {
                val message = pyResult.callAttr("get", "message")?.toString()
                    ?: "Erreur lors de la génération de la mire colorimétrique"
                ColorSetResult.Error(message = message)
            }
        }.getOrElse { error ->
            ColorSetResult.Error(message = error.localizedMessage ?: error.toString())
        }
    }

    /**
     * Extracts dominant colors from an image path as hex strings.
     */
    suspend fun extractPalette(imagePath: String, numColors: Int = 6): List<String> =
        withContext(Dispatchers.Default) {
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
     * Generates a synthetic reference sample image (e.g. Cinematic Teal & Orange, Moody, Sunset)
     * so the user can test all features right out of the box without needing to find photos.
     */
    suspend fun createSampleImage(style: String): File = withContext(Dispatchers.IO) {
        val fileName = "sample_${style.lowercase().replace(" ", "_")}.jpg"
        val sampleFile = File(context.cacheDir, fileName)
        if (sampleFile.exists()) return@withContext sampleFile

        val width = 720
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            "Teal & Orange" -> {
                // Sky teal gradient top, orange/amber ground bottom
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
            "Cyberpunk" -> {
                val gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(240, 20, 120), Color.rgb(30, 10, 60), Color.rgb(0, 230, 240)),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            else -> {
                // Neutral portrait / street tone
                val gradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(190, 160, 140), Color.rgb(120, 100, 90), Color.rgb(50, 45, 40)),
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
