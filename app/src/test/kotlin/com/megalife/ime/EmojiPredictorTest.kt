package com.megalife.ime

import com.megalife.ime.feature.EmojiPredictor
import com.megalife.ime.feature.KosherEmojiFilter
import org.junit.Assert.*
import org.junit.Test

class EmojiPredictorTest {

    @Test
    fun `predict returns emoji for known English keyword`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val result = predictor.predict("happy", "en")
        assertTrue(result.isNotEmpty())
        assertTrue(result.size <= 3)
        assertTrue(result.contains("😊"))
    }

    @Test
    fun `predict returns emoji for known Hebrew keyword`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val result = predictor.predict("שלום", "he")
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("👋"))
    }

    @Test
    fun `predict returns empty list for unknown word`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val result = predictor.predict("xyznonexistent", "en")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `predict returns empty list for blank input`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        assertTrue(predictor.predict("", "en").isEmpty())
        assertTrue(predictor.predict("   ", "en").isEmpty())
    }

    @Test
    fun `predict is case insensitive`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val lower = predictor.predict("happy", "en")
        val upper = predictor.predict("HAPPY", "en")
        val mixed = predictor.predict("Happy", "en")
        assertEquals(lower, upper)
        assertEquals(lower, mixed)
    }

    @Test
    fun `predict trims whitespace`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val trimmed = predictor.predict("happy", "en")
        val untrimmed = predictor.predict("  happy  ", "en")
        assertEquals(trimmed, untrimmed)
    }

    @Test
    fun `predict returns at most 3 results`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        // "happy" maps to 4 emoji, but predict should return at most 3
        val result = predictor.predict("happy", "en")
        assertTrue(result.size <= 3)
    }

    @Test
    fun `predict filters blocked emoji via kosher filter`() {
        val predictor = EmojiPredictor(kosherFilter = KosherEmojiFilter)
        // All normal keywords should still return results (their emoji are not blocked)
        val result = predictor.predict("happy", "en")
        assertTrue(result.isNotEmpty())
        // Verify none of the results are in the blocked list
        for (emoji in result) {
            assertFalse(
                "Emoji $emoji should not be blocked",
                KosherEmojiFilter.isBlocked(emoji)
            )
        }
    }

    @Test
    fun `predict without kosher filter returns unfiltered results`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val result = predictor.predict("love", "en")
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("❤️"))
    }

    @Test
    fun `multiple keywords return different emoji sets`() {
        val predictor = EmojiPredictor(kosherFilter = null)
        val happy = predictor.predict("happy", "en")
        val sad = predictor.predict("sad", "en")
        assertNotEquals(happy, sad)
    }

    @Test
    fun `predict works for various categories`() {
        val predictor = EmojiPredictor(kosherFilter = null)

        // Emotions
        assertTrue(predictor.predict("love", "en").isNotEmpty())
        // Animals
        assertTrue(predictor.predict("dog", "en").isNotEmpty())
        // Food
        assertTrue(predictor.predict("pizza", "en").isNotEmpty())
        // Weather
        assertTrue(predictor.predict("rain", "en").isNotEmpty())
        // Gestures
        assertTrue(predictor.predict("ok", "en").isNotEmpty())
        // Hebrew
        assertTrue(predictor.predict("תודה", "he").isNotEmpty())
    }
}
