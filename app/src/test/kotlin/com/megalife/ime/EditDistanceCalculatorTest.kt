package com.megalife.ime

import com.megalife.ime.feature.EditDistanceCalculator
import org.junit.Assert.*
import org.junit.Test

class EditDistanceCalculatorTest {

    @Test
    fun `identical strings have distance 0`() {
        assertEquals(0, EditDistanceCalculator.distance("hello", "hello"))
    }

    @Test
    fun `empty strings have distance 0`() {
        assertEquals(0, EditDistanceCalculator.distance("", ""))
    }

    @Test
    fun `one empty string returns length of other`() {
        assertEquals(5, EditDistanceCalculator.distance("hello", ""))
        assertEquals(5, EditDistanceCalculator.distance("", "hello"))
    }

    @Test
    fun `single substitution`() {
        assertEquals(1, EditDistanceCalculator.distance("cat", "bat"))
    }

    @Test
    fun `single insertion`() {
        assertEquals(1, EditDistanceCalculator.distance("cat", "cats"))
    }

    @Test
    fun `single deletion`() {
        assertEquals(1, EditDistanceCalculator.distance("cats", "cat"))
    }

    @Test
    fun `multiple edits`() {
        assertEquals(3, EditDistanceCalculator.distance("kitten", "sitting"))
    }

    @Test
    fun `confidence for identical strings is 1`() {
        assertEquals(1.0f, EditDistanceCalculator.confidence("hello", "hello"), 0.001f)
    }

    @Test
    fun `confidence for completely different strings is 0`() {
        assertEquals(0.0f, EditDistanceCalculator.confidence("abc", "xyz"), 0.001f)
    }

    @Test
    fun `confidence for one edit on 5-char word`() {
        // distance=1, maxLen=5, confidence = 1 - 1/5 = 0.8
        assertEquals(0.8f, EditDistanceCalculator.confidence("hello", "hallo"), 0.001f)
    }
}
