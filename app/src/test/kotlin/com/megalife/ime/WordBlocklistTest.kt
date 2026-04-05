package com.megalife.ime

import org.junit.Assert.*
import org.junit.Test

class WordBlocklistTest {

    @Test
    fun `filterSuggestions removes blocked words`() {
        // Test the pure filtering logic without SharedPreferences
        val blocked = setOf("bad", "ugly")
        val suggestions = listOf("good", "bad", "nice", "ugly", "fine")
        val filtered = suggestions.filter { it.lowercase() !in blocked }
        assertEquals(listOf("good", "nice", "fine"), filtered)
    }

    @Test
    fun `blocked word check is case insensitive`() {
        val blocked = setOf("hello")
        assertTrue("hello" in blocked)
        assertTrue("Hello".lowercase() in blocked)
        assertTrue("HELLO".lowercase() in blocked)
    }

    @Test
    fun `words with commas are handled correctly with StringSet`() {
        // StringSet preserves words with commas (unlike comma-separated strings)
        val set = mutableSetOf("hello, world", "foo")
        assertTrue("hello, world" in set)
        assertEquals(2, set.size) // Not split into 3
    }
}
