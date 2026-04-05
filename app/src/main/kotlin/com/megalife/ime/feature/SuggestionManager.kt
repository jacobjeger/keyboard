package com.megalife.ime.feature

import com.megalife.ime.db.dao.BigramDao
import com.megalife.ime.db.dao.DictionaryDao
import com.megalife.ime.db.dao.LearnedWordDao
import com.megalife.ime.db.dao.ShortcutDao
import java.util.Collections
import kotlinx.coroutines.*

/**
 * Orchestrates the word prediction pipeline.
 * Queries dictionary, learned words, and bigrams to produce ranked suggestions.
 * Debounces queries by 50ms for performance.
 */
class SuggestionManager(
    private val dictionaryDao: DictionaryDao,
    private val learnedWordDao: LearnedWordDao,
    private val bigramDao: BigramDao,
    private val shortcutDao: ShortcutDao,
    private val scope: CoroutineScope,
    var wordBlocklist: WordBlocklist? = null
) {
    /** Optional emoji predictor — when set, emoji suggestions are appended with "emoji:" prefix */
    var emojiPredictor: EmojiPredictor? = null

    /** Optional nikud suggester — when set, nikud variants are added for Hebrew words */
    var nikudSuggester: NikudSuggester? = null

    private var currentJob: Job? = null
    private val debounceMs = 10L  // Minimal debounce for responsiveness

    // Thread-safe LRU cache for recent queries
    private val cache: MutableMap<String, List<String>> = Collections.synchronizedMap(
        LinkedHashMap<String, List<String>>(32, 0.75f, true)
    )
    private val maxCacheSize = 100

    /**
     * Request suggestions asynchronously.
     * Results delivered via callback on main thread.
     */
    fun requestSuggestions(
        language: String,
        digitSequence: String,
        wordPrefix: String,
        previousWord: String,
        isT9Mode: Boolean,
        maxResults: Int = 3,
        onResult: (List<String>) -> Unit
    ) {
        currentJob?.cancel()

        if (digitSequence.isEmpty() && wordPrefix.isEmpty()) {
            // No input: show next-word predictions if previous word exists
            if (previousWord.isNotEmpty()) {
                currentJob = scope.launch {
                    val predictions = getNextWordPredictions(language, previousWord)
                    onResult(predictions)
                }
            } else {
                onResult(emptyList())
            }
            return
        }

        val cacheKey = "$language:$digitSequence:$wordPrefix"
        synchronized(cache) {
            cache[cacheKey]?.let {
                onResult(it)
                return
            }
        }

        currentJob = scope.launch {
            delay(debounceMs) // Debounce

            val suggestions = withContext(Dispatchers.IO) {
                buildSuggestions(language, digitSequence, wordPrefix, previousWord, isT9Mode, maxResults)
            }

            // Append emoji predictions if available
            val withEmoji = if (wordPrefix.isNotEmpty() && emojiPredictor != null) {
                val emojiSuggestions = emojiPredictor!!.predict(wordPrefix, language)
                    .map { "emoji:$it" }
                suggestions + emojiSuggestions
            } else {
                suggestions
            }

            // Cache the result
            synchronized(cache) {
                if (cache.size >= maxCacheSize) {
                    cache.remove(cache.keys.first())
                }
                cache[cacheKey] = withEmoji
            }

            onResult(withEmoji)
        }
    }

    private suspend fun buildSuggestions(
        language: String,
        digitSequence: String,
        wordPrefix: String,
        previousWord: String,
        isT9Mode: Boolean,
        maxResults: Int = 3
    ): List<String> {
        val results = mutableListOf<ScoredWord>()

        if (isT9Mode && digitSequence.isNotEmpty()) {
            // T9 mode: query by digit sequence
            val dictWords = dictionaryDao.findByDigitSequence(language, digitSequence, 10)
            dictWords.forEach {
                results.add(ScoredWord(it.word, it.frequency.toFloat()))
            }

            val learnedWords = learnedWordDao.findByDigitSequence(language, digitSequence, 5)
            learnedWords.forEach {
                results.add(ScoredWord(it.word, it.useCount * 3f)) // Boost learned words
            }
        } else if (wordPrefix.isNotEmpty()) {
            // On-screen mode: query by word prefix
            val dictWords = dictionaryDao.findByWordPrefix(language, wordPrefix, 10)
            dictWords.forEach {
                results.add(ScoredWord(it.word, it.frequency.toFloat()))
            }

            val learnedWords = learnedWordDao.findByWordPrefix(language, wordPrefix, 5)
            learnedWords.forEach {
                results.add(ScoredWord(it.word, it.useCount * 3f))
            }
        }

        // Bigram boost: if previous word exists, boost words that commonly follow it
        if (previousWord.isNotEmpty()) {
            val bigrams = bigramDao.findByFirstWord(language, previousWord, 5)
            for (bigram in bigrams) {
                val existing = results.find { it.word == bigram.word2 }
                if (existing != null) {
                    existing.score += bigram.frequency * 2f
                } else {
                    results.add(ScoredWord(bigram.word2, bigram.frequency * 2f))
                }
            }
        }

        // Check for text shortcuts
        if (wordPrefix.isNotEmpty()) {
            val shortcut = shortcutDao.findByTrigger(wordPrefix)
            if (shortcut != null) {
                results.add(0, ScoredWord(shortcut.expansion, Float.MAX_VALUE))
            }
        }

        // Add nikud suggestions for Hebrew words
        if (nikudSuggester != null && wordPrefix.isNotEmpty()) {
            val nikudSuggestions = nikudSuggester!!.suggest(wordPrefix)
            for (ns in nikudSuggestions) {
                if (ns !in results.map { it.word }) {
                    results.add(ScoredWord(ns, Float.MAX_VALUE / 2))
                }
            }
        }

        // Deduplicate, filter blocklist, and sort by score
        val filtered = results
            .groupBy { it.word.lowercase() }
            .map { (_, words) -> words.maxByOrNull { it.score }!! }
            .sortedByDescending { it.score }
            .map { it.word }

        val blocklist = wordBlocklist
        return if (blocklist != null) {
            blocklist.filterSuggestions(filtered).take(maxResults)
        } else {
            filtered.take(maxResults)
        }
    }

    private suspend fun getNextWordPredictions(language: String, previousWord: String): List<String> {
        val bigrams = bigramDao.findByFirstWord(language, previousWord, 3)
        return bigrams.map { it.word2 }
    }

    data class ScoredWord(val word: String, var score: Float)
}
