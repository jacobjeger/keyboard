package com.megalife.ime

import com.megalife.ime.language.T9KeyMap
import com.megalife.ime.language.locale.ArabicConfig
import com.megalife.ime.language.locale.EnglishConfig
import com.megalife.ime.language.locale.FrenchConfig
import com.megalife.ime.language.locale.HebrewConfig
import com.megalife.ime.language.locale.RussianConfig
import org.junit.Assert.*
import org.junit.Test

class T9KeyMapTest {

    @Test
    fun `english word to digit sequence`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertEquals("43556", keyMap.wordToDigitSequence("hello"))
        assertEquals("96753", keyMap.wordToDigitSequence("world"))
        assertEquals("843", keyMap.wordToDigitSequence("the"))
    }

    @Test
    fun `english uppercase word to digit sequence`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertEquals("43556", keyMap.wordToDigitSequence("Hello"))
        assertEquals("43556", keyMap.wordToDigitSequence("HELLO"))
    }

    @Test
    fun `english get chars for key`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertEquals(listOf('a', 'b', 'c'), keyMap.getCharsForKey(2))
        assertEquals(listOf('d', 'e', 'f'), keyMap.getCharsForKey(3))
        assertEquals(listOf('p', 'q', 'r', 's'), keyMap.getCharsForKey(7))
        assertEquals(listOf('w', 'x', 'y', 'z'), keyMap.getCharsForKey(9))
    }

    @Test
    fun `english key 0 returns space`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertEquals(listOf(' '), keyMap.getCharsForKey(0))
    }

    @Test
    fun `english key 1 returns punctuation`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertTrue(keyMap.getCharsForKey(1).contains('.'))
        assertTrue(keyMap.getCharsForKey(1).contains(','))
    }

    @Test
    fun `hebrew word to digit sequence`() {
        val keyMap = HebrewConfig.t9KeyMap
        // שלום = ש(8) ל(5) ו(3) ם(6) → "8536"
        assertEquals("8536", keyMap.wordToDigitSequence("שלום"))
    }

    @Test
    fun `hebrew get chars for key`() {
        val keyMap = HebrewConfig.t9KeyMap
        assertEquals(listOf('א', 'ב', 'ג'), keyMap.getCharsForKey(2))
    }

    @Test
    fun `has mapping returns true for valid keys`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertTrue(keyMap.hasMapping(2))
        assertTrue(keyMap.hasMapping(9))
        assertFalse(keyMap.hasMapping(0))
        assertFalse(keyMap.hasMapping(1))
    }

    @Test
    fun `empty word returns empty sequence`() {
        val keyMap = EnglishConfig.t9KeyMap
        assertEquals("", keyMap.wordToDigitSequence(""))
    }

    // === Arabic T9 tests ===

    @Test
    fun `arabic word to digit sequence`() {
        val keyMap = ArabicConfig.t9KeyMap
        // في = ف(7) ي(9) → "79"
        assertEquals("79", keyMap.wordToDigitSequence("\u0641\u064A"))
    }

    @Test
    fun `arabic get chars for key 2`() {
        val keyMap = ArabicConfig.t9KeyMap
        // Key 2 = ا ب ت ث
        assertEquals(
            listOf('\u0627', '\u0628', '\u062A', '\u062B'),
            keyMap.getCharsForKey(2)
        )
    }

    @Test
    fun `arabic get chars for key 9`() {
        val keyMap = ArabicConfig.t9KeyMap
        // Key 9 = ه و ي
        assertEquals(
            listOf('\u0647', '\u0648', '\u064A'),
            keyMap.getCharsForKey(9)
        )
    }

    @Test
    fun `arabic has mapping for valid keys`() {
        val keyMap = ArabicConfig.t9KeyMap
        assertTrue(keyMap.hasMapping(2))
        assertTrue(keyMap.hasMapping(9))
        assertFalse(keyMap.hasMapping(0))
        assertFalse(keyMap.hasMapping(1))
    }

    // === Russian T9 tests ===

    @Test
    fun `russian word to digit sequence`() {
        val keyMap = RussianConfig.t9KeyMap
        // да = д(3) а(2) → "32"
        assertEquals("32", keyMap.wordToDigitSequence("\u0434\u0430"))
    }

    @Test
    fun `russian get chars for key 2`() {
        val keyMap = RussianConfig.t9KeyMap
        // Key 2 = а б в г
        assertEquals(
            listOf('\u0430', '\u0431', '\u0432', '\u0433'),
            keyMap.getCharsForKey(2)
        )
    }

    @Test
    fun `russian word net to digit sequence`() {
        val keyMap = RussianConfig.t9KeyMap
        // нет = н(5) е(3) т(6) → "536"
        assertEquals("536", keyMap.wordToDigitSequence("\u043D\u0435\u0442"))
    }

    @Test
    fun `russian has mapping for valid keys`() {
        val keyMap = RussianConfig.t9KeyMap
        assertTrue(keyMap.hasMapping(2))
        assertTrue(keyMap.hasMapping(9))
        assertFalse(keyMap.hasMapping(0))
    }

    // === French AZERTY layout tests ===

    @Test
    fun `french uses same T9 mapping as english`() {
        val frMap = FrenchConfig.t9KeyMap
        val enMap = EnglishConfig.t9KeyMap
        // French uses the same Latin T9 mapping
        assertEquals("43556", frMap.wordToDigitSequence("hello"))
        assertEquals(enMap.wordToDigitSequence("hello"), frMap.wordToDigitSequence("hello"))
    }

    @Test
    fun `french touch layout is AZERTY`() {
        val layout = FrenchConfig.touchLayout
        // First row first key should be "a" (AZERTY), not "q" (QWERTY)
        val firstRowFirstKey = layout.rows[0].keys[0]
        assertEquals("a", firstRowFirstKey.primaryChar)
        // Second key should be "z"
        val firstRowSecondKey = layout.rows[0].keys[1]
        assertEquals("z", firstRowSecondKey.primaryChar)
    }

    @Test
    fun `french layout is not RTL`() {
        assertFalse(FrenchConfig.touchLayout.isRtl)
    }

    @Test
    fun `french e key has accent popups`() {
        val layout = FrenchConfig.touchLayout
        val eKey = layout.rows[0].keys[2]
        assertEquals("e", eKey.primaryChar)
        assertTrue(eKey.popupChars.contains("\u00E9"))  // é
        assertTrue(eKey.popupChars.contains("\u00E8"))  // è
        assertTrue(eKey.popupChars.contains("\u00EA"))  // ê
        assertTrue(eKey.popupChars.contains("\u00EB"))  // ë
    }
}
