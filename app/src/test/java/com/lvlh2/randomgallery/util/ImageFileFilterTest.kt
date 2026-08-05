package com.lvlh2.randomgallery.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFileFilterTest {
    @Test
    fun `accepts every supported extension`() {
        for (ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")) {
            assertTrue("expected $ext to be accepted", isImageFile("photo.$ext"))
        }
    }

    @Test
    fun `is case-insensitive`() {
        assertTrue(isImageFile("PHOTO.JPG"))
        assertTrue(isImageFile("Photo.PnG"))
        assertTrue(isImageFile("photo.HeIc"))
    }

    @Test
    fun `rejects unsupported extensions`() {
        for (ext in listOf("txt", "mp4", "avi", "mov", "mkv", "mp3", "pdf", "doc", "zip", "raw", "svg", "tiff")) {
            assertFalse("expected $ext to be rejected", isImageFile("file.$ext"))
        }
    }

    @Test
    fun `rejects files without an extension`() {
        assertFalse(isImageFile("photo"))
        assertFalse(isImageFile(""))
    }

    @Test
    fun `rejects dotfiles and trailing dots`() {
        assertFalse(isImageFile(".hidden"))
        assertFalse(isImageFile("photo."))
    }

    @Test
    fun `matches on the last extension only`() {
        assertTrue(isImageFile("photo.backup.jpg"))
        assertFalse(isImageFile("photo.jpg.backup"))
    }

    @Test
    fun `accepts a long SAF document display name`() {
        assertTrue(isImageFile("IMG_20240101_120000_processed_final (1).JPG"))
    }
}
