package com.megalife.ime

import com.megalife.ime.feature.GematriaCalculator
import org.junit.Assert.*
import org.junit.Test

class GematriaCalculatorTest {

    @Test
    fun `single letters have correct values`() {
        assertEquals(1, GematriaCalculator.calculate("א"))
        assertEquals(2, GematriaCalculator.calculate("ב"))
        assertEquals(3, GematriaCalculator.calculate("ג"))
        assertEquals(10, GematriaCalculator.calculate("י"))
        assertEquals(100, GematriaCalculator.calculate("ק"))
        assertEquals(400, GematriaCalculator.calculate("ת"))
    }

    @Test
    fun `word shalom`() {
        // ש=300, ל=30, ו=6, ם=40 → 376
        assertEquals(376, GematriaCalculator.calculate("שלום"))
    }

    @Test
    fun `word chai`() {
        // ח=8, י=10 → 18
        assertEquals(18, GematriaCalculator.calculate("חי"))
    }

    @Test
    fun `final forms have same value as base forms`() {
        // ם (final mem) should be 40 like מ
        // ן (final nun) should be 50 like נ
        // ך (final kaf) should be 20 like כ
        // ף (final pe) should be 80 like פ
        // ץ (final tsade) should be 90 like צ
        assertEquals(40, GematriaCalculator.calculate("ם"))
        assertEquals(50, GematriaCalculator.calculate("ן"))
        assertEquals(20, GematriaCalculator.calculate("ך"))
        assertEquals(80, GematriaCalculator.calculate("ף"))
        assertEquals(90, GematriaCalculator.calculate("ץ"))
    }

    @Test
    fun `empty string returns 0`() {
        assertEquals(0, GematriaCalculator.calculate(""))
    }

    @Test
    fun `non-hebrew characters are ignored`() {
        assertEquals(0, GematriaCalculator.calculate("hello"))
        assertEquals(0, GematriaCalculator.calculate("123"))
    }

    @Test
    fun `mixed hebrew and non-hebrew`() {
        // Only Hebrew chars count
        assertEquals(1, GematriaCalculator.calculate("אa"))
    }

    @Test
    fun `containsHebrew detects hebrew`() {
        assertTrue(GematriaCalculator.containsHebrew("שלום"))
        assertTrue(GematriaCalculator.containsHebrew("hello שלום"))
        assertFalse(GematriaCalculator.containsHebrew("hello"))
        assertFalse(GematriaCalculator.containsHebrew(""))
    }
}
