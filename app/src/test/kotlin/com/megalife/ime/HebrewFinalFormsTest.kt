package com.megalife.ime

import org.junit.Assert.*
import org.junit.Test

class HebrewFinalFormsTest {

    // Hebrew final forms mapping (same as in HebrewConfig)
    private val finalForms = mapOf(
        'כ' to 'ך',
        'מ' to 'ם',
        'נ' to 'ן',
        'פ' to 'ף',
        'צ' to 'ץ'
    )

    /**
     * Apply Hebrew final forms: last char gets final form,
     * non-last chars get base form (un-finalized).
     */
    private fun applyFinalForms(word: String): String {
        if (finalForms.isEmpty() || word.isEmpty()) return word

        val chars = word.toCharArray()
        // Convert last char to final form if applicable
        val lastChar = chars.last()
        val finalForm = finalForms[lastChar]
        if (finalForm != null) {
            chars[chars.size - 1] = finalForm
        }

        // Un-finalize any non-last character
        val reverseFinals = finalForms.entries.associate { it.value to it.key }
        for (i in 0 until chars.size - 1) {
            val baseForm = reverseFinals[chars[i]]
            if (baseForm != null) {
                chars[i] = baseForm
            }
        }

        return String(chars)
    }

    @Test
    fun `final kaf at end of word`() {
        // כ at end → ך
        assertEquals("לך", applyFinalForms("לכ"))
    }

    @Test
    fun `final mem at end of word`() {
        // מ at end → ם
        assertEquals("שלום", applyFinalForms("שלומ").let {
            // Actually test: שלומ → שלום
            applyFinalForms("שלומ")
        })
    }

    @Test
    fun `final nun at end of word`() {
        // נ at end → ן
        assertEquals("חן", applyFinalForms("חנ"))
    }

    @Test
    fun `final pe at end of word`() {
        // פ at end → ף
        assertEquals("כף", applyFinalForms("כפ"))
    }

    @Test
    fun `final tsade at end of word`() {
        // צ at end → ץ
        assertEquals("עץ", applyFinalForms("עצ"))
    }

    @Test
    fun `non-final char in middle stays base form`() {
        // מ in middle stays מ, not ם
        val result = applyFinalForms("מלך")
        assertEquals('מ', result[0])
        assertEquals('ל', result[1])
        assertEquals('ך', result[2]) // כ→ך at end
    }

    @Test
    fun `already-final char in middle gets un-finalized`() {
        // If someone types ם in middle (shouldn't happen but handle it)
        val result = applyFinalForms("םלכ")
        assertEquals('מ', result[0]) // ם → מ (un-finalized)
        assertEquals('ל', result[1])
        assertEquals('ך', result[2]) // כ → ך (finalized)
    }

    @Test
    fun `empty string returns empty`() {
        assertEquals("", applyFinalForms(""))
    }

    @Test
    fun `single char gets finalized`() {
        assertEquals("ם", applyFinalForms("מ"))
    }

    @Test
    fun `non-final-form char unchanged`() {
        assertEquals("את", applyFinalForms("את"))
    }
}
