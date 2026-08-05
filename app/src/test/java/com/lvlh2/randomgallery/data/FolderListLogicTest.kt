package com.lvlh2.randomgallery.data

import com.lvlh2.randomgallery.model.SavedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderListLogicTest {
    private fun folder(
        id: String,
        enabled: Boolean = true,
    ) = SavedFolder(id = id, uri = "content://tree/$id", name = "Folder $id", enabled = enabled)

    @Test
    fun `addFolder appends an enabled folder with a generated id`() {
        val result = addFolder(emptyList(), "content://tree/cam", "Camera") { "id-1" }
        assertEquals(1, result.size)
        assertEquals(SavedFolder("id-1", "content://tree/cam", "Camera"), result[0])
    }

    @Test
    fun `addFolder generates a unique id per call`() {
        var counter = 0
        val list =
            emptyList<SavedFolder>()
                .let { addFolder(it, "content://tree/1", "A") { "id-${++counter}" } }
                .let { addFolder(it, "content://tree/2", "B") { "id-${++counter}" } }
        assertEquals(listOf("id-1", "id-2"), list.map { it.id })
    }

    @Test
    fun `toggleFolder flips only the target folder`() {
        val original = listOf(folder("1"), folder("2"))
        val result = toggleFolder(original, "2")
        assertTrue(result[0].enabled)
        assertEquals(false, result[1].enabled)
        assertEquals(original[0], result[0]) // untouched folder keeps its original instance
        assertEquals(original[1].copy(enabled = false), result[1])
    }

    @Test
    fun `toggleFolder on an unknown id is a no-op`() {
        val original = listOf(folder("1"))
        assertEquals(original, toggleFolder(original, "nope"))
    }

    @Test
    fun `removeFolder removes by id and keeps the rest`() {
        val original = listOf(folder("1"), folder("2"), folder("3"))
        val result = removeFolder(original, "2")
        assertEquals(listOf(folder("1"), folder("3")), result)
    }

    @Test
    fun `removeFolder on an unknown id is a no-op`() {
        val original = listOf(folder("1"))
        assertEquals(original, removeFolder(original, "nope"))
    }

    @Test
    fun `removeFolder on an empty list is a no-op`() {
        assertEquals(emptyList<SavedFolder>(), removeFolder(emptyList(), "nope"))
    }

    @Test
    fun `toggleFolder returns new object instances for the toggled folder`() {
        val result = toggleFolder(listOf(folder("1")), "1")
        assertNotEquals(folder("1"), result[0])
    }
}
