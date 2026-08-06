package com.lvlh2.randomgallery.util

import com.lvlh2.randomgallery.model.ImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ScanPolicyTest {
    private fun item(
        name: String,
        uri: String = name,
    ) = ImageItem(uri = uri, name = name)

    /** The seeded random keeps the reshuffle cases deterministic. */
    private val seeded = Random(42)

    private fun uriSet(items: List<ImageItem>) = items.mapTo(mutableSetOf()) { it.uri }

    @Test
    fun `empty previous shuffles the fresh list`() {
        val fresh = listOf(item("a.jpg"), item("b.jpg"), item("c.jpg"), item("d.jpg"))
        val result = applyScanResult(emptyList(), fresh, seeded)
        assertEquals(uriSet(fresh), uriSet(result))
        assertNotEquals(fresh, result) // cold start must actually shuffle
    }

    @Test
    fun `unchanged set keeps the previous order`() {
        val previous = listOf(item("c.jpg", "uri-c"), item("a.jpg", "uri-a"), item("b.jpg", "uri-b"))
        val fresh = listOf(item("a.jpg", "uri-a"), item("b.jpg", "uri-b"), item("c.jpg", "uri-c"))
        // Fresh is enumerated in name order; the displayed order must not jump.
        assertEquals(previous, applyScanResult(previous, fresh, seeded))
    }

    @Test
    fun `unchanged set refreshes names from the fresh scan`() {
        val previous = listOf(item("old-name", "uri-a"))
        val fresh = listOf(item("renamed.jpg", "uri-a"))
        assertEquals(listOf(ImageItem(uri = "uri-a", name = "renamed.jpg")), applyScanResult(previous, fresh, seeded))
    }

    @Test
    fun `added image reshuffles the fresh list`() {
        val previous = listOf(item("a.jpg", "uri-a"), item("b.jpg", "uri-b"), item("c.jpg", "uri-c"))
        val fresh = previous + item("new.jpg", "uri-new")
        val result = applyScanResult(previous, fresh, seeded)
        assertEquals(uriSet(fresh), uriSet(result))
        assertNotEquals(fresh, result) // content changed -> a fresh shuffle
    }

    @Test
    fun `removed image reshuffles the fresh list`() {
        val previous = listOf(item("a.jpg", "uri-a"), item("b.jpg", "uri-b"), item("c.jpg", "uri-c"))
        val fresh = listOf(item("a.jpg", "uri-a"), item("c.jpg", "uri-c"))
        val result = applyScanResult(previous, fresh, seeded)
        assertEquals(uriSet(fresh), uriSet(result))
        assertTrue(fresh != result) // content changed -> a fresh shuffle
    }

    @Test
    fun `same uris with different names still keeps the previous order`() {
        val previous = listOf(item("a.jpg", "uri-a"), item("b.jpg", "uri-b"))
        val fresh = listOf(item("b2.jpg", "uri-b"), item("a2.jpg", "uri-a"))
        val result = applyScanResult(previous, fresh, seeded)
        assertEquals(listOf("uri-a", "uri-b"), result.map { it.uri })
        assertEquals(listOf("a2.jpg", "b2.jpg"), result.map { it.name })
    }
}
