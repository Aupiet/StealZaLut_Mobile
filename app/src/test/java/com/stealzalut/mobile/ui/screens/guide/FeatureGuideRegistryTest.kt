package com.stealzalut.mobile.ui.screens.guide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureGuideRegistryTest {

    @Test
    fun `registry contains exactly 12 photographic tools`() {
        assertEquals(12, FeatureGuideRegistry.items.size)
    }

    @Test
    fun `all tool IDs are unique`() {
        val ids = FeatureGuideRegistry.items.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    @Test
    fun `every tool has valid non-zero resource IDs for title, descriptions, and parameters`() {
        for (item in FeatureGuideRegistry.items) {
            assertTrue(item.titleRes != 0, "Item ${item.id} must have a valid titleRes")
            assertTrue(item.shortDescRes != 0, "Item ${item.id} must have a valid shortDescRes")
            assertTrue(item.detailedExplanationRes != 0, "Item ${item.id} must have a valid detailedExplanationRes")
            assertTrue(item.keyParameters.isNotEmpty(), "Item ${item.id} must have key parameters")
            for (param in item.keyParameters) {
                assertTrue(param.nameRes != 0, "Param in ${item.id} must have a valid nameRes")
                assertTrue(param.descRes != 0, "Param in ${item.id} must have a valid descRes")
            }
        }
    }

    @Test
    fun `every category in FeatureCategory is covered by at least one tool`() {
        val categories = FeatureGuideRegistry.items.map { it.category }.toSet()
        for (cat in FeatureCategory.entries) {
            assertTrue(categories.contains(cat), "Category $cat must be covered in the registry")
        }
    }

    @Test
    fun `filter by category narrows results accurately`() {
        val geometryItems = FeatureGuideRegistry.filter(category = FeatureCategory.GEOMETRY)
        assertTrue(geometryItems.isNotEmpty())
        assertTrue(geometryItems.all { it.category == FeatureCategory.GEOMETRY })

        val colorItems = FeatureGuideRegistry.filter(category = FeatureCategory.COLOR)
        assertTrue(colorItems.isNotEmpty())
        assertTrue(colorItems.all { it.category == FeatureCategory.COLOR })
    }

    @Test
    fun `filter by search query matches tool ID and content`() {
        val cropResults = FeatureGuideRegistry.filter(query = "crop")
        assertEquals(1, cropResults.size)
        assertEquals("crop", cropResults[0].id)

        val lutResults = FeatureGuideRegistry.filter(query = "lut")
        assertEquals(1, lutResults.size)
        assertEquals("lut", lutResults[0].id)
    }

    @Test
    fun `getById returns correct item or null for unknown id`() {
        val curves = FeatureGuideRegistry.getById("curves")
        assertNotNull(curves)
        assertEquals("curves", curves?.id)
        assertEquals(FeatureCategory.LIGHT, curves?.category)

        val unknown = FeatureGuideRegistry.getById("non_existent_tool")
        assertNull(unknown)
    }
}
