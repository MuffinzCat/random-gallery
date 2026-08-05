package com.lvlh2.randomgallery.util

import kotlin.random.Random

/**
 * Fisher-Yates shuffle returning a NEW list — the input is never mutated.
 * Mirrors the old Expo app's fisherYatesShuffle().
 */
fun <T> fisherYatesShuffle(
    list: List<T>,
    random: Random = Random.Default,
): List<T> {
    val result = list.toMutableList()
    for (i in result.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = result[i]
        result[i] = result[j]
        result[j] = tmp
    }
    return result
}
