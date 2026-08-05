package com.lvlh2.randomgallery.util

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")

/**
 * True for the exact extension set the old Expo app accepted (case-insensitive).
 * A leading dot ("photo.jpg.backup", ".hidden") or a missing extension is not an image.
 */
fun isImageFile(name: String): Boolean {
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex <= 0 || dotIndex == name.lastIndex) return false
    return name.substring(dotIndex + 1).lowercase() in IMAGE_EXTENSIONS
}
