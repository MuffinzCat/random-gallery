package com.lvlh2.randomgallery.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeUriUtilsTest {
    private fun treeUri(encodedId: String) = "content://com.android.externalstorage.documents/tree/$encodedId"

    @Test
    fun `derives the folder name from a primary volume path`() {
        assertEquals("Camera", deriveFolderName(treeUri("primary%3ADCIM%2FCamera")))
    }

    @Test
    fun `derives the last segment of a nested path`() {
        assertEquals("2024", deriveFolderName(treeUri("primary%3ADCIM%2FCamera%2F2024")))
    }

    @Test
    fun `derives the folder name from an SD-card volume`() {
        assertEquals("DCIM", deriveFolderName(treeUri("AAAA-1234%3ADCIM")))
    }

    @Test
    fun `falls back to Folder for a volume root`() {
        assertEquals("Folder", deriveFolderName(treeUri("primary%3A")))
    }

    @Test
    fun `decodes percent-encoded names with spaces`() {
        assertEquals("My Photos", deriveFolderName(treeUri("primary%3ADCIM%2FMy%20Photos")))
    }

    @Test
    fun `getTreeDocumentId returns the decoded document id`() {
        assertEquals("primary:DCIM/Camera", getTreeDocumentId(treeUri("primary%3ADCIM%2FCamera")))
    }
}
