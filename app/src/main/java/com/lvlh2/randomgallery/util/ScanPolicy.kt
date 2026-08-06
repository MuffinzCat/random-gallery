package com.lvlh2.randomgallery.util

import com.lvlh2.randomgallery.model.ImageItem
import kotlin.random.Random

/**
 * Decides what the grid shows after a re-scan.
 *
 * When the scanned URIs are unchanged, the previously displayed order is kept
 * (names refreshed from the fresh scan) — warm resumes don't make the grid
 * jump. When the content changed (images added/removed), the fresh list is
 * shuffled. A cold start always shuffles, because `previous` is empty then.
 */
fun applyScanResult(
    previous: List<ImageItem>,
    fresh: List<ImageItem>,
    random: Random = Random.Default,
): List<ImageItem> {
    if (fresh.size == previous.size && fresh.mapTo(mutableSetOf()) { it.uri } == previous.mapTo(mutableSetOf()) { it.uri }) {
        val freshByUri = fresh.associateBy { it.uri }
        return previous.map { old -> freshByUri[old.uri] ?: old }
    }
    return fisherYatesShuffle(fresh, random)
}
