package com.lvlh2.randomgallery.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleTest {
    @Test
    fun `shuffle preserves length`() {
        val input = (0 until 100).toList()
        assertEquals(input.size, fisherYatesShuffle(input).size)
    }

    @Test
    fun `shuffle preserves the multiset of elements`() {
        val input = (0 until 100).toList()
        assertEquals(input.sorted(), fisherYatesShuffle(input).sorted())
    }

    @Test
    fun `shuffle does not mutate the input`() {
        val input = (0 until 100).toList()
        val original = input.toList()
        fisherYatesShuffle(input)
        assertEquals(original, input)
    }

    @Test
    fun `shuffle returns a new list`() {
        val input = listOf(1, 2, 3, 4, 5)
        val result = fisherYatesShuffle(input)
        // Always a fresh instance (assertNotEquals compares contents — and content
        // equality is a 1-in-120 flake on a 5-element Fisher-Yates).
        assertNotSame(input, result)
        assertEquals(input.sorted(), result.sorted())
    }

    @Test
    fun `empty and single-element lists are stable`() {
        assertEquals(emptyList<Int>(), fisherYatesShuffle(emptyList<Int>()))
        assertEquals(listOf(42), fisherYatesShuffle(listOf(42)))
    }

    @Test
    fun `two-element list produces both orders over many runs`() {
        val orders = mutableSetOf<List<Int>>()
        repeat(50) {
            orders += fisherYatesShuffle(listOf(1, 2))
        }
        assertEquals(setOf(listOf(1, 2), listOf(2, 1)), orders)
    }

    @Test
    fun `distribution over 6000 runs is roughly uniform`() {
        val counts = mutableMapOf<List<Int>, Int>()
        val random = Random(42)
        repeat(6000) {
            val order = fisherYatesShuffle(listOf(1, 2, 3), random)
            counts[order] = (counts[order] ?: 0) + 1
        }
        assertEquals(6, counts.size)
        counts.forEach { (_, count) ->
            assertTrue("count $count too far from expected 1000", count in 700..1300)
        }
    }

    @Test
    fun `10000 elements smoke test`() {
        val input = (0 until 10000).toList()
        val result = fisherYatesShuffle(input)
        assertEquals(input.sorted(), result.sorted())
    }

    @Test
    fun `works for arbitrary object lists`() {
        data class Thing(
            val name: String,
        )
        val input = listOf(Thing("a"), Thing("b"), Thing("c"))
        val result = fisherYatesShuffle(input)
        assertEquals(input.toSet(), result.toSet())
    }
}
