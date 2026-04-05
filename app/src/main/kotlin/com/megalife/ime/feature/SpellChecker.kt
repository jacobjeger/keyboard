package com.megalife.ime.feature

import com.megalife.ime.db.dao.DictionaryDao
import com.megalife.ime.db.dao.LearnedWordDao

/**
 * Spell checking and autocorrect engine.
 * Supports all languages with dictionaries.
 */
class SpellChecker(
    private val dictionaryDao: DictionaryDao,
    private val learnedWordDao: LearnedWordDao
) {
    companion object {
        private const val AUTOCORRECT_CONFIDENCE_THRESHOLD = 0.85f
    }

    /** Map Hebrew final forms to their base letter for edit distance normalization */
    private val hebrewFinalToBase = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')

    private fun normalizeHebrew(word: String): String {
        return word.map { hebrewFinalToBase[it] ?: it }.joinToString("")
    }

    /** Check if a word exists in the dictionary */
    suspend fun isSpellingCorrect(word: String, language: String): Boolean {
        if (word.isBlank()) return true
        if (word.all { it.isUpperCase() }) return true // ALL CAPS = abbreviation
        if (word.any { it.isDigit() }) return true // Contains numbers

        return dictionaryDao.wordExists(language, word) ||
                learnedWordDao.wordExists(language, word)
    }

    /** Get spelling suggestions for a misspelled word */
    suspend fun getSuggestions(word: String, language: String): List<String> {
        val candidates = dictionaryDao.findSimilarLengthWords(language, word, 100)
        val normalizedInput = normalizeHebrew(word.lowercase())
        return candidates
            .map { it.word to EditDistanceCalculator.distance(normalizedInput, normalizeHebrew(it.word.lowercase())) }
            .filter { it.second <= 2 } // Max 2 edits
            .sortedWith(compareBy({ it.second }, { -(candidates.find { c -> c.word == it.first }?.frequency ?: 0) }))
            .take(3)
            .map { it.first }
    }

    /**
     * Try to autocorrect a word. Returns the corrected word if confidence > 85%.
     * Returns null if no correction should be applied.
     */
    suspend fun autoCorrect(word: String, language: String): String? {
        if (word.isBlank()) return null
        if (word[0].isUpperCase()) return null // Don't correct proper nouns
        if (isSpellingCorrect(word, language)) return null

        val suggestions = getSuggestions(word, language)
        if (suggestions.isEmpty()) return null

        val best = suggestions.first()
        val confidence = EditDistanceCalculator.confidence(word, best)
        return if (confidence >= AUTOCORRECT_CONFIDENCE_THRESHOLD) best else null
    }
}
