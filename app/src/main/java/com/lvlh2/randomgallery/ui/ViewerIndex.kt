package com.lvlh2.randomgallery.ui

/**
 * Index the pager should land on after deleting the image at [currentIndex] from a list
 * of [size] images. Returns -1 when deleting the only image (the viewer exits).
 *
 * Mirrors the old app's 4d3c1c8 fix: newIndex = min(current, newSize - 1) — deleting the
 * last image lands on the new last image.
 */
fun nextIndexAfterDelete(
    currentIndex: Int,
    size: Int,
): Int {
    if (size <= 1) return -1
    val newSize = size - 1
    return currentIndex.coerceAtMost(newSize - 1).coerceAtLeast(0)
}

/**
 * Resolves the grid item to reveal after the viewer closes.
 *
 * URI identity wins because list positions can change after deletion or a rescan.
 * If the viewed URI no longer exists, [fallbackIndex] is clamped to the nearest
 * surviving item. Returns null when the collection is empty.
 */
fun returnIndexForImage(
    viewedUri: String?,
    fallbackIndex: Int,
    imageUris: List<String>,
): Int? {
    if (imageUris.isEmpty()) return null

    val uriIndex = viewedUri?.let(imageUris::indexOf) ?: -1
    if (uriIndex >= 0) return uriIndex

    return fallbackIndex.coerceIn(0, imageUris.lastIndex)
}
