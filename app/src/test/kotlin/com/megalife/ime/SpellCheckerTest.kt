package com.megalife.ime

import com.megalife.ime.feature.EditDistanceCalculator
import org.junit.Assert.*
import org.junit.Test

class SpellCheckerTest {

    @Test
    fun `edit distance handles empty strings`() {
        assertEquals(0, EditDistanceCalculator.distance("", ""))
        assertEquals(3, EditDistanceCalculator.distance("abc", ""))
        assertEquals(3, EditDistanceCalculator.distance("", "abc"))
    }

    @Test
    fun `edit distance for similar words`() {
        assertEquals(1, EditDistanceCalculator.distance("hello", "hallo"))
        assertEquals(1, EditDistanceCalculator.distance("world", "worl"))
        assertEquals(1, EditDistanceCalculator.distance("kitten", "sitten"))
    }

    @Test
    fun `confidence returns valid range`() {
        val conf = EditDistanceCalculator.confidence("hello", "hallo")
        assertTrue(conf in 0f..1f)
        assertTrue(conf > 0.5f)
    }

    @Test
    fun `confidence for identical words is 1`() {
        assertEquals(1f, EditDistanceCalculator.confidence("hello", "hello"))
    }
}
