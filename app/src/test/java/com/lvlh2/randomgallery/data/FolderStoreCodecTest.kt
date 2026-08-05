package com.lvlh2.randomgallery.data

import com.lvlh2.randomgallery.model.SavedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderStoreCodecTest {
    private fun folder(
        id: String,
        name: String,
        enabled: Boolean = true,
    ) = SavedFolder(id = id, uri = "content://tree/$id", name = name, enabled = enabled)

    @Test
    fun `encode then decode round-trips an empty list`() {
        assertEquals(emptyList<SavedFolder>(), FolderCodec.decode(FolderCodec.encode(emptyList())))
    }

    @Test
    fun `encode then decode round-trips a single folder`() {
        val folders = listOf(folder("1", "Camera"))
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }

    @Test
    fun `encode then decode round-trips many folders with unicode names and disabled flags`() {
        val folders =
            listOf(
                folder("1", "照片 📷"),
                folder("2", "DCIM", enabled = false),
                folder("3", "Screenshots"),
            )
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }

    @Test
    fun `broken json decodes to an empty list`() {
        assertTrue(FolderCodec.decode("{broken").isEmpty())
    }

    @Test
    fun `non-array json decodes to an empty list`() {
        assertTrue(FolderCodec.decode("not-an-array").isEmpty())
    }

    @Test
    fun `a plain string decodes to an empty list`() {
        assertTrue(FolderCodec.decode("\"a string\"").isEmpty())
    }

    @Test
    fun `unknown fields in stored json are ignored`() {
        val legacy = """[{"id":"1","uri":"content://tree/1","name":"Camera","enabled":true,"oldField":42}]"""
        assertEquals(listOf(folder("1", "Camera")), FolderCodec.decode(legacy))
    }
}
