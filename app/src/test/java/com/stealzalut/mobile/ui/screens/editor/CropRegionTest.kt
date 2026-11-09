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
}
