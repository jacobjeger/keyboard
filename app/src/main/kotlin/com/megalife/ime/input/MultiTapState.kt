package com.megalife.ime.input

import com.megalife.ime.language.LanguageConfig

/**
 * Tracks the current state of multi-tap T9 input.
 */
class MultiTapState {
    /** The key currently being cycled (2-9) */
    var currentKey: Int = -1
        private set

    /** Index into the character list for the current key */
    var charIndex: Int = 0
        private set

    /** Whether we're in the middle of a multi-tap cycle */
    val isActive: Boolean get() = currentKey in 2..9

    /** The currently selected character, or null if no key is active */
    fun getCurrentChar(language: LanguageConfig): Char? {
        if (!isActive) return null
        val chars = language.t9KeyMap.getCharsForKey(currentKey)
        return if (chars.isNotEmpty()) chars[charIndex % chars.size] else null
    }

    /** Get all characters for the current key */
    fun getKeyChars(language: LanguageConfig): List<Char> {
        if (!isActive) return emptyList()
        return language.t9KeyMap.getCharsForKey(currentKey)
    }

    /**
     * Process a key press. Returns true if this is a cycle (same key pressed again).
     */
    fun onKeyPress(key: Int, language: LanguageConfig): Boolean {
        val chars = language.t9KeyMap.getCharsForKey(key)
        if (chars.isEmpty()) return false

        return if (key == currentKey) {
            // Same key: cycle to next character
            charIndex = (charIndex + 1) % chars.size
            true
        } else {
            // Different key: start new cycle
            currentKey = key
            charIndex = 0
            false
        }
    }

    /** Reset the state (after commit) */
    fun reset() {
        currentKey = -1
        charIndex = 0
    }
}
