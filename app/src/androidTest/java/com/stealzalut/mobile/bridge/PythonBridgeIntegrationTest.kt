package com.stealzalut.mobile.bridge

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.stealzalut.mobile.data.bridge.PythonBridgeRepository
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.CubeExportResult
import com.stealzalut.mobile.data.model.LutSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PythonBridgeIntegrationTest {

    private lateinit var repository: PythonBridgeRepository
    private lateinit var appContext: android.content.Context

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext))
        }
        repository = PythonBridgeRepository(appContext)
    }

    @Test
    fun exportCube_createsValidAdobeCubeFileWithExactNodeCount() = runBlocking {
        // 1. Generate 64x64 source and reference test bitmaps in memory
        val srcBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(30, 80, 180)) // Deep Blue
        }
        val refBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(220, 120, 40)) // Warm Amber / Orange
        }

        val srcFile = File(appContext.cacheDir, "test_int_src.jpg")
        val refFile = File(appContext.cacheDir, "test_int_ref.jpg")

        FileOutputStream(srcFile).use { srcBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        FileOutputStream(refFile).use { refBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        // 2. Call PythonBridgeRepository.exportCube with FAST (17^3)
        val result = repository.exportCube(
            srcPath = srcFile.absolutePath,
            refPath = refFile.absolutePath,
            lutSize = LutSize.FAST,
            method = ColorTransferMethod.HM,
            intensity = 1.0f
        )

        // 3. Verify success and cube file properties
        assertTrue("Expected CubeExportResult.Success, got $result", result is CubeExportResult.Success)
        val success = result as CubeExportResult.Success

        val cubeFile = File(success.cubePath)
        assertTrue("Exported .cube file must exist on disk", cubeFile.exists())

        val lines = cubeFile.readLines()
        assertTrue("Cube file must contain header", lines.isNotEmpty())
        assertTrue("Cube file header must contain LUT_3D_SIZE 17", lines.any { it.trim() == "LUT_3D_SIZE 17" })

        // 4. Count data lines (ignoring comments and headers)
        val dataLines = lines.filter { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("TITLE") &&
                    !trimmed.startsWith("LUT_") && !trimmed.startsWith("DOMAIN_")
        }

        val expectedNodes = 17 * 17 * 17 // 4 913 nodes
        assertEquals("Cube file must contain exactly 17^3 = 4,913 RGB triplet lines", expectedNodes, dataLines.size)

        // 5. Verify all values are floats in [0.0, 1.0]
        for ((idx, line) in dataLines.withIndex()) {
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            assertEquals("Each line must contain 3 floating point numbers (R G B)", 3, parts.size)
            for (part in parts) {
                val value = part.toFloat()
                assertTrue("Lattice values must be in [0.0, 1.0], but got $value at line $idx", value in 0.0f..1.0f)
            }
        }
    }
}
