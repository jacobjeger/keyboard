package com.megalife.ime.language

/**
 * T9 key mapping for a language.
 * Maps physical keys 0-9 to lists of characters for multi-tap cycling.
 * Also provides reverse mapping: character -> digit for dictionary lookup.
 */
class T9KeyMap(
    /** Key 0-9 -> list of characters. Index is the key number. */
    val keyChars: Map<Int, List<Char>>,
    /** Characters for key 1 (punctuation/special) */
    val key1Chars: List<Char> = listOf('.', ',', '!', '?', '\'', '-', '@'),
    /** Characters for key 0 (space + extras) */
    val key0Chars: List<Char> = listOf(' '),
    /** Extra character mappings (e.g., Hebrew final forms ם→same key as מ) */
    val extraCharMappings: Map<Char, Char> = emptyMap()
) {
    /** Reverse map: character -> digit key */
    private val charToDigit: Map<Char, Int> by lazy {
        buildMap {
            keyChars.forEach { (key, chars) ->
                chars.forEach { ch ->
                    put(ch.lowercaseChar(), key)
                    if (ch.isLetter()) put(ch.uppercaseChar(), key)
                }
            }
            // Add extra mappings (final forms map to same key as their base form)
            extraCharMappings.forEach { (extra, base) ->
                val key = get(base)
                if (key != null) {
                    put(extra, key)
                }
            }
        }
    }

    /** Convert a word to its T9 digit sequence for dictionary lookup */
    fun wordToDigitSequence(word: String): String {
        val sb = StringBuilder()
        for (ch in word) {
            val digit = charToDigit[ch] ?: charToDigit[ch.lowercaseChar()] ?: continue
            sb.append(digit)
        }
        return sb.toString()
    }

    /** Get the characters for a given key number */
    fun getCharsForKey(key: Int): List<Char> {
        return when (key) {
            0 -> key0Chars
            1 -> key1Chars
            else -> keyChars[key] ?: emptyList()
        }
    }

    /** Check if a key has T9 character mapping */
    fun hasMapping(key: Int): Boolean {
        return key in 2..9 && keyChars.containsKey(key)
    }
}
