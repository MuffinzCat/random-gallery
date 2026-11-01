package com.lvlh2.randomgallery.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerIndexTest {
    @Test
    fun `deleting a middle image keeps the current index`() {
        assertEquals(1, nextIndexAfterDelete(currentIndex = 1, size = 5))
    }

    @Test
    fun `deleting the first image stays at the same index`() {
        assertEquals(0, nextIndexAfterDelete(currentIndex = 0, size = 5))
    }

    @Test
    fun `deleting the last image lands on the new last image`() {
        assertEquals(3, nextIndexAfterDelete(currentIndex = 4, size = 5))
    }

    @Test
    fun `deleting the last of two images lands on the first`() {
        assertEquals(0, nextIndexAfterDelete(currentIndex = 1, size = 2))
    }

    @Test
    fun `deleting the only image returns the exit sentinel`() {
        assertEquals(-1, nextIndexAfterDelete(currentIndex = 0, size = 1))
    }

    @Test
    fun `result is always in bounds for every current index and size`() {
        for (size in 1..20) {
            for (current in 0 until size) {
                val next = nextIndexAfterDelete(current, size)
                if (size == 1) {
                    assertEquals("size=1 must exit", -1, next)
                } else {
                    assertEquals("size=$size current=$current", 0, next.coerceIn(0, size - 2) - next)
                }
            }
        }
    }

    @Test
    fun `return target follows the viewed URI when it still exists`() {
        assertEquals(2, returnIndexForImage("uri-c", 0, listOf("uri-a", "uri-b", "uri-c")))
    }

    @Test
    fun `return target uses the fallback index when viewed image was deleted`() {
        assertEquals(1, returnIndexForImage("uri-deleted", 1, listOf("uri-a", "uri-c")))
    }

    @Test
    fun `return target clamps a deleted last image to the new last index`() {
        assertEquals(1, returnIndexForImage("uri-deleted", 2, listOf("uri-a", "uri-b")))
    }

    @Test
    fun `return target clamps a negative fallback to the first image`() {
        assertEquals(0, returnIndexForImage(null, -1, listOf("uri-a", "uri-b")))
    }

    @Test
    fun `return target is absent when no images survive`() {
        assertEquals(null, returnIndexForImage("uri-a", 0, emptyList()))
    }
}
