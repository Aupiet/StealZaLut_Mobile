package com.stealzalut.mobile.ui.screens.editor

import com.stealzalut.mobile.data.model.CropRegion
import com.stealzalut.mobile.data.model.LightroomDeserializer
import com.stealzalut.mobile.data.model.LightroomDevelopModel
import com.stealzalut.mobile.data.model.LightroomSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CropRegionTest {

    @Test
    fun `default crop region is default and hasCrop is false`() {
        val crop = CropRegion()
        assertEquals(0.0f, crop.left)
        assertEquals(0.0f, crop.top)
        assertEquals(1.0f, crop.right)
        assertEquals(1.0f, crop.bottom)
        assertEquals(0.0f, crop.angle)
        assertTrue(crop.isDefault)
        assertFalse(crop.hasCrop)
    }

    @Test
    fun `custom crop boundary sets hasCrop to true`() {
        val crop = CropRegion(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        assertFalse(crop.isDefault)
        assertTrue(crop.hasCrop)
    }

    @Test
    fun `custom angle sets hasCrop to true`() {
        val crop = CropRegion(angle = 5.0f)
        assertFalse(crop.isDefault)
        assertTrue(crop.hasCrop)
    }

    @Test
    fun `aspect ratio values match standard ratios`() {
        assertEquals(1.0f, CropAspectRatio.SQUARE_1_1.ratio)
        assertEquals(0.8f, CropAspectRatio.PORTRAIT_4_5.ratio)
        assertEquals(1.5f, CropAspectRatio.RATIO_3_2.ratio)
        assertEquals(16.0f / 9.0f, CropAspectRatio.LANDSCAPE_16_9.ratio)
    }

    @Test
    fun `serialization and deserialization roundtrip preserves crop settings`() {
        val model = LightroomDevelopModel()
        val customCrop = CropRegion(left = 0.15f, top = 0.25f, right = 0.85f, bottom = 0.75f, angle = -3.5f)

        val json = LightroomSerializer.toJson(model, customCrop)
        assertTrue(json.contains("\"crop\""))
        assertTrue(json.contains("\"left\":0.15"))
        assertTrue(json.contains("\"angle\":-3.5"))

        val extractedCrop = LightroomDeserializer.extractCropRegion(json)
        assertEquals(0.15f, extractedCrop.left, 1e-4f)
        assertEquals(0.25f, extractedCrop.top, 1e-4f)
        assertEquals(0.85f, extractedCrop.right, 1e-4f)
        assertEquals(0.75f, extractedCrop.bottom, 1e-4f)
        assertEquals(-3.5f, extractedCrop.angle, 1e-4f)
    }

    @Test
    fun `serialization without crop does not include crop object`() {
        val model = LightroomDevelopModel()
        val defaultCrop = CropRegion()

        val json = LightroomSerializer.toJson(model, defaultCrop)
        assertFalse(json.contains("\"crop\""))

        val extracted = LightroomDeserializer.extractCropRegion(json)
        assertTrue(extracted.isDefault)
    }

    // --- Pure Crop Physics & Fluid Interaction Tests ---

    @Test
    fun `computeFittedCropRegion for 1 to 1 on landscape photo centers vertically`() {
        // Landscape image: 2000 x 1000 -> targetRatio = 1.0 -> k = 2000 / (1000 * 1.0) = 2.0 > 1.0
        // Expected: w = 1 / 2.0 = 0.5, centered horizontally -> left = 0.25, right = 0.75, top = 0.0, bottom = 1.0
        val crop = CropPhysics.computeFittedCropRegion(CropAspectRatio.SQUARE_1_1, 2000, 1000)
        assertEquals(0.25f, crop.left, 1e-4f)
        assertEquals(0.0f, crop.top, 1e-4f)
        assertEquals(0.75f, crop.right, 1e-4f)
        assertEquals(1.0f, crop.bottom, 1e-4f)
    }

    @Test
    fun `computeFittedCropRegion for 1 to 1 on portrait photo centers horizontally`() {
        // Portrait image: 1000 x 2000 -> targetRatio = 1.0 -> k = 1000 / (2000 * 1.0) = 0.5 <= 1.0
        // Expected: h = 0.5, centered vertically -> top = 0.25, bottom = 0.75, left = 0.0, right = 1.0
        val crop = CropPhysics.computeFittedCropRegion(CropAspectRatio.SQUARE_1_1, 1000, 2000)
        assertEquals(0.0f, crop.left, 1e-4f)
        assertEquals(0.25f, crop.top, 1e-4f)
        assertEquals(1.0f, crop.right, 1e-4f)
        assertEquals(0.75f, crop.bottom, 1e-4f)
    }

    @Test
    fun `applyDrag INSIDE_MOVE translates crop box within bounds`() {
        val initial = CropRegion(left = 0.2f, top = 0.2f, right = 0.6f, bottom = 0.6f)
        val moved = CropPhysics.applyDrag(
            crop = initial,
            handle = DragHandle.INSIDE_MOVE,
            dx = 0.1f,
            dy = -0.05f,
            ratio = CropAspectRatio.FREE,
            imageWidth = 1000,
            imageHeight = 1000
        )
        assertEquals(0.3f, moved.left, 1e-4f)
        assertEquals(0.15f, moved.top, 1e-4f)
        assertEquals(0.7f, moved.right, 1e-4f)
        assertEquals(0.55f, moved.bottom, 1e-4f)
    }

    @Test
    fun `applyDrag INSIDE_MOVE clamps to edges`() {
        val initial = CropRegion(left = 0.8f, top = 0.8f, right = 1.0f, bottom = 1.0f)
        val moved = CropPhysics.applyDrag(
            crop = initial,
            handle = DragHandle.INSIDE_MOVE,
            dx = 0.5f,
            dy = 0.5f,
            ratio = CropAspectRatio.FREE,
            imageWidth = 1000,
            imageHeight = 1000
        )
        assertEquals(0.8f, moved.left, 1e-4f)
        assertEquals(0.8f, moved.top, 1e-4f)
        assertEquals(1.0f, moved.right, 1e-4f)
        assertEquals(1.0f, moved.bottom, 1e-4f)
    }

    @Test
    fun `applyDrag BOTTOM_RIGHT with locked ratio maintains aspect ratio`() {
        // Square image (1000x1000) with SQUARE_1_1 -> k = 1.0
        val initial = CropRegion(left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f)
        val dragged = CropPhysics.applyDrag(
            crop = initial,
            handle = DragHandle.BOTTOM_RIGHT,
            dx = 0.2f,
            dy = 0.0f, // dy is ignored or reconciled via k
            ratio = CropAspectRatio.SQUARE_1_1,
            imageWidth = 1000,
            imageHeight = 1000
        )
        assertEquals(0.1f, dragged.left, 1e-4f)
        assertEquals(0.1f, dragged.top, 1e-4f)
        assertEquals(0.7f, dragged.right, 1e-4f)
        assertEquals(0.7f, dragged.bottom, 1e-4f)
    }

    @Test
    fun `applyDrag TOP_LEFT with free ratio moves top and left independently`() {
        val initial = CropRegion(left = 0.3f, top = 0.3f, right = 0.8f, bottom = 0.8f)
        val dragged = CropPhysics.applyDrag(
            crop = initial,
            handle = DragHandle.TOP_LEFT,
            dx = -0.1f,
            dy = 0.05f,
            ratio = CropAspectRatio.FREE,
            imageWidth = 1000,
            imageHeight = 1000
        )
        assertEquals(0.2f, dragged.left, 1e-4f)
        assertEquals(0.35f, dragged.top, 1e-4f)
        assertEquals(0.8f, dragged.right, 1e-4f)
        assertEquals(0.8f, dragged.bottom, 1e-4f)
    }

    @Test
    fun `applyDrag enforces minimum dimension to prevent inversion`() {
        val initial = CropRegion(left = 0.4f, top = 0.4f, right = 0.5f, bottom = 0.5f)
        val dragged = CropPhysics.applyDrag(
            crop = initial,
            handle = DragHandle.BOTTOM_RIGHT,
            dx = -0.3f,
            dy = -0.3f,
            ratio = CropAspectRatio.FREE,
            imageWidth = 1000,
            imageHeight = 1000,
            minDim = 0.05f
        )
        assertTrue(dragged.right >= dragged.left + 0.05f)
        assertTrue(dragged.bottom >= dragged.top + 0.05f)
    }
}
