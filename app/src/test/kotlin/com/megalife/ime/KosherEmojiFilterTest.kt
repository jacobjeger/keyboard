package com.megalife.ime

import com.megalife.ime.feature.EmojiData
import com.megalife.ime.feature.KosherEmojiFilter
import org.junit.Assert.*
import org.junit.Test

class KosherEmojiFilterTest {

    @Test
    fun `blocks alcohol emoji`() {
        assertTrue(KosherEmojiFilter.isBlocked("\uD83C\uDF7A")) // beer
        assertTrue(KosherEmojiFilter.isBlocked("\uD83C\uDF77")) // wine glass
        assertTrue(KosherEmojiFilter.isBlocked("\uD83C\uDF78")) // cocktail
    }

    @Test
    fun `blocks weapon emoji`() {
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDD2B")) // pistol
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDCA3")) // bomb
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDD2A")) // knife
    }

    @Test
    fun `blocks suggestive emoji`() {
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDC8B")) // kiss mark
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDC59")) // bikini
    }

    @Test
    fun `blocks occult emoji`() {
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDC80")) // skull
        assertTrue(KosherEmojiFilter.isBlocked("\uD83D\uDD2E")) // crystal ball
    }

    @Test
    fun `allows smiley emoji`() {
        assertFalse(KosherEmojiFilter.isBlocked("\uD83D\uDE00")) // grinning face
        assertFalse(KosherEmojiFilter.isBlocked("\uD83D\uDE0A")) // smiling face
    }

    @Test
    fun `allows nature emoji`() {
        assertFalse(KosherEmojiFilter.isBlocked("\uD83C\uDF3F")) // herb
        assertFalse(KosherEmojiFilter.isBlocked("\uD83C\uDF39")) // rose
    }

    @Test
    fun `blocks names containing alcohol`() {
        assertTrue(KosherEmojiFilter.isNameBlocked("beer mug"))
        assertTrue(KosherEmojiFilter.isNameBlocked("wine glass"))
        assertTrue(KosherEmojiFilter.isNameBlocked("cocktail glass"))
    }

    @Test
    fun `allows clean names`() {
        assertFalse(KosherEmojiFilter.isNameBlocked("grinning face"))
        assertFalse(KosherEmojiFilter.isNameBlocked("heart"))
        assertFalse(KosherEmojiFilter.isNameBlocked("sun"))
    }

    @Test
    fun `filter removes blocked items from list`() {
        val emojis = listOf(
            EmojiData.Emoji("\uD83D\uDE00", "grinning face", EmojiData.EmojiCategory.SMILEYS),
            EmojiData.Emoji("\uD83C\uDF7A", "beer mug", EmojiData.EmojiCategory.FOOD),
            EmojiData.Emoji("\uD83C\uDF3F", "herb", EmojiData.EmojiCategory.NATURE)
        )
        val filtered = KosherEmojiFilter.filter(emojis)
        assertEquals(2, filtered.size)
        assertEquals("grinning face", filtered[0].name)
        assertEquals("herb", filtered[1].name)
    }
}
