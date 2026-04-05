package com.megalife.ime.feature

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Manages a list of words that should never be suggested.
 * Stored in SharedPreferences as a StringSet (migrated from comma-separated).
 * Users can add words by long-pressing a suggestion and selecting "Block".
 */
class WordBlocklist(context: Context) {

    companion object {
        private const val PREF_KEY = "word_blocklist"
        private const val PREF_KEY_SET = "word_blocklist_set"
        private const val PREF_MIGRATED = "word_blocklist_migrated_to_set"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val blockedWords: MutableSet<String> = loadBlocklist()

    private fun loadBlocklist(): MutableSet<String> {
        // Migrate from comma-separated string if needed
        if (!prefs.getBoolean(PREF_MIGRATED, false)) {
            val raw = prefs.getString(PREF_KEY, "") ?: ""
            if (raw.isNotEmpty()) {
                val words = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                prefs.edit()
                    .putStringSet(PREF_KEY_SET, words)
                    .remove(PREF_KEY)
                    .putBoolean(PREF_MIGRATED, true)
                    .apply()
                return words.toMutableSet()
            }
            prefs.edit().putBoolean(PREF_MIGRATED, true).apply()
        }

        return prefs.getStringSet(PREF_KEY_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveBlocklist() {
        // Pass a copy to avoid SharedPreferences holding a reference to the mutable set
        prefs.edit().putStringSet(PREF_KEY_SET, blockedWords.toSet()).apply()
    }

    /** Check if a word is blocked */
    fun isBlocked(word: String): Boolean {
        return word.lowercase() in blockedWords
    }

    /** Block a word from appearing in suggestions */
    fun blockWord(word: String) {
        blockedWords.add(word.lowercase())
        saveBlocklist()
    }

    /** Unblock a word */
    fun unblockWord(word: String) {
        blockedWords.remove(word.lowercase())
        saveBlocklist()
    }

    /** Get all blocked words */
    fun getAll(): Set<String> = blockedWords.toSet()

    /** Clear the entire blocklist */
    fun clearAll() {
        blockedWords.clear()
        saveBlocklist()
    }

    /** Filter a list of suggestions, removing blocked words */
    fun filterSuggestions(suggestions: List<String>): List<String> {
        return suggestions.filter { !isBlocked(it) }
    }
}
