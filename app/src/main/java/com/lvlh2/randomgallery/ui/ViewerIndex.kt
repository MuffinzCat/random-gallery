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
