package com.megalife.ime.feature

import android.graphics.PointF
import com.megalife.ime.db.dao.DictionaryDao
import com.megalife.ime.language.LanguageConfig
import com.megalife.ime.language.TouchLayout
import com.megalife.ime.language.KeyDef
import com.megalife.ime.language.KeyType
import kotlinx.coroutines.*

/**
 * Glide/swipe typing controller.
 * Detects swipe path across on-screen keyboard, matches against dictionary.
 * Supports Latin, Cyrillic, and Hebrew scripts.
 *
 * Algorithm: Template matching — for each dictionary word, compute the expected
 * path through key centers and compare against actual swipe path using DTW.
 */
class GlideTypingController(
    private val dictionaryDao: DictionaryDao,
    private val scope: CoroutineScope
) {
    /** Touch point along the swipe path */
    data class SwipePoint(val x: Float, val y: Float, val timestamp: Long)

    /** Key bounds for matching path to keys */
    data class KeyBounds(val char: Char, val centerX: Float, val centerY: Float, val width: Float, val height: Float)

    /** Map Hebrew final forms to their base form for matching */
    private val hebrewFinalFormMap = mapOf(
        '\u05DA' to '\u05DB',  // ך -> כ
        '\u05DD' to '\u05DE',  // ם -> מ
        '\u05DF' to '\u05E0',  // ן -> נ
        '\u05E3' to '\u05E4',  // ף -> פ
        '\u05E5' to '\u05E6'   // ץ -> צ
    )

    /** Normalize Hebrew final forms to base forms for comparison */
    private fun normalizeChar(ch: Char): Char = hebrewFinalFormMap[ch] ?: ch

    private val path = mutableListOf<SwipePoint>()
    private var keyBounds: List<KeyBounds> = emptyList()
    private var isActive = false

    /** Set the keyboard key positions for path matching */
    fun setKeyBounds(bounds: List<KeyBounds>) {
        keyBounds = bounds
    }

    /** Begin a swipe gesture */
    fun onSwipeStart(x: Float, y: Float) {
        path.clear()
        path.add(SwipePoint(x, y, System.currentTimeMillis()))
        isActive = true
    }

    /** Continue the swipe (finger moving) */
    fun onSwipeMove(x: Float, y: Float) {
        if (!isActive) return
        // Sample at reasonable intervals to avoid too many points
        val last = path.lastOrNull()
        if (last != null) {
            val dx = x - last.x
            val dy = y - last.y
            if (dx * dx + dy * dy < 100) return // Skip if too close (< 10px)
        }
        path.add(SwipePoint(x, y, System.currentTimeMillis()))
    }

    /** End swipe and get word candidates */
    fun onSwipeEnd(
        language: String,
        onResult: (List<String>) -> Unit
    ) {
        isActive = false
        if (path.size < 3 || keyBounds.isEmpty()) {
            onResult(emptyList())
            return
        }

        scope.launch(Dispatchers.Default) {
            val candidates = matchSwipePath(language)
            withContext(Dispatchers.Main) {
                onResult(candidates.take(3))
            }
        }
    }

    /** Get the current path points (for trail rendering) */
    fun getCurrentPath(): List<SwipePoint> = path.toList()

    fun isSwipeActive(): Boolean = isActive

    // ==================== MATCHING ALGORITHM ====================

    private suspend fun matchSwipePath(language: String): List<String> {
        // Step 1: Extract keys along the swipe path
        val pathKeys = extractKeysFromPath()
        if (pathKeys.isEmpty()) return emptyList()

        // Step 2: Build a digit/letter sequence from the path
        val keySequence = pathKeys.map { it.char }.distinct()
        if (keySequence.isEmpty()) return emptyList()

        // Step 3: Get first and last letter (most reliable in swipe)
        val firstChar = keySequence.first()
        val lastChar = keySequence.last()

        // Step 4: Query dictionary for words starting with first letter
        val prefix = firstChar.toString()
        val candidates = withContext(Dispatchers.IO) {
            dictionaryDao.findByWordPrefix(language, prefix, 50)
        }

        // Step 5: Score each candidate against the swipe path
        val scored = candidates.mapNotNull { word ->
            if (word.word.length < 2) return@mapNotNull null
            if (word.word.last().lowercaseChar() != lastChar) return@mapNotNull null

            val score = scoreWordAgainstPath(word.word, pathKeys)
            if (score > 0.3f) Pair(word.word, score * word.frequency)
            else null
        }

        return scored.sortedByDescending { it.second }.map { it.first }
    }

    private fun extractKeysFromPath(): List<KeyBounds> {
        val result = mutableListOf<KeyBounds>()
        for (point in path) {
            val nearest = findNearestKey(point.x, point.y) ?: continue
            if (result.isEmpty() || result.last().char != nearest.char) {
                result.add(nearest)
            }
        }
        return result
    }

    private fun findNearestKey(x: Float, y: Float): KeyBounds? {
        return keyBounds
            .filter { it.char.isLetter() || it.char.code in 0x0590..0x05FF || it.char.code in 0x0600..0x06FF || it.char.code in 0x0400..0x04FF || it.char.code in 0x1200..0x137F }
            .minByOrNull {
                val dx = x - it.centerX
                val dy = y - it.centerY
                dx * dx + dy * dy
            }
    }

    /**
     * Score how well a word matches the swipe path.
     * Returns 0.0 to 1.0.
     */
    private fun scoreWordAgainstPath(word: String, pathKeys: List<KeyBounds>): Float {
        if (word.isEmpty() || pathKeys.isEmpty()) return 0f

        // Check if each letter of the word appears in order in the path
        var pathIdx = 0
        var matchCount = 0

        for (ch in word.lowercase()) {
            val normalizedCh = normalizeChar(ch)
            while (pathIdx < pathKeys.size) {
                if (normalizeChar(pathKeys[pathIdx].char) == normalizedCh) {
                    matchCount++
                    pathIdx++
                    break
                }
                pathIdx++
            }
        }

        return matchCount.toFloat() / word.length
    }
}
