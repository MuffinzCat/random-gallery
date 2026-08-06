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

    @Test
    fun `primary volume doc id maps to a MediaStore relative path with trailing slash`() {
        assertEquals("DCIM/Camera/", primaryVolumeRelativePath("primary:DCIM/Camera"))
    }

    @Test
    fun `nested primary path maps to the full relative path`() {
        assertEquals("Pictures/2024/Holiday/", primaryVolumeRelativePath("primary:Pictures/2024/Holiday"))
    }

    @Test
    fun `input with a trailing slash is normalized`() {
        assertEquals("DCIM/Camera/", primaryVolumeRelativePath("primary:DCIM/Camera/"))
    }

    @Test
    fun `volume root is not mappable`() {
        assertEquals(null, primaryVolumeRelativePath("primary:"))
    }

    @Test
    fun `SD-card volume is not mappable`() {
        assertEquals(null, primaryVolumeRelativePath("AAAA-1234:DCIM"))
    }

    @Test
    fun `other volume prefixes are not mappable`() {
        assertEquals(null, primaryVolumeRelativePath("secondary:DCIM/Camera"))
    }

    @Test
    fun `relative path and name rebuild the primary doc id`() {
        assertEquals("primary:DCIM/Camera/IMG_1.jpg", primaryDocId("DCIM/Camera/", "IMG_1.jpg"))
    }

    @Test
    fun `rebuild works when the relative path lacks a trailing slash`() {
        assertEquals("primary:DCIM/Camera/IMG_1.jpg", primaryDocId("DCIM/Camera", "IMG_1.jpg"))
    }
}
