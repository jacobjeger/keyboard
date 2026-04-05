package com.megalife.ime.feature

/**
 * Computes Levenshtein (edit) distance between two strings.
 * Used for spell check and autocorrect.
 */
object EditDistanceCalculator {

    /** Compute the Levenshtein distance between two strings */
    fun distance(a: String, b: String): Int {
        val m = a.length
        val n = b.length

        // Optimize: early exit for trivial cases
        if (m == 0) return n
        if (n == 0) return m
        if (a == b) return 0

        // Use two rows instead of full matrix for O(min(m,n)) space
        var prevRow = IntArray(n + 1) { it }
        var currRow = IntArray(n + 1)

        for (i in 1..m) {
            currRow[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                currRow[j] = minOf(
                    prevRow[j] + 1,      // deletion
                    currRow[j - 1] + 1,  // insertion
                    prevRow[j - 1] + cost // substitution
                )
            }
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[n]
    }

    /**
     * Compute confidence that word2 is a correction of word1.
     * Returns 0.0 to 1.0 where 1.0 = perfect match.
     */
    fun confidence(original: String, candidate: String): Float {
        val dist = distance(original.lowercase(), candidate.lowercase())
        val maxLen = maxOf(original.length, candidate.length)
        if (maxLen == 0) return 1f
        return 1f - (dist.toFloat() / maxLen)
    }
}
