package com.megalife.ime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megalife.ime.db.entity.DictionaryWord

@Dao
interface DictionaryDao {

    /** Core T9 lookup: exact digit sequence match, ordered by frequency. Target: <50ms */
    @Query("""
        SELECT * FROM dictionary_words
        WHERE language = :lang AND digit_sequence = :digits
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findByDigitSequence(lang: String, digits: String, limit: Int = 20): List<DictionaryWord>

    /** T9 prefix lookup for partial input */
    @Query("""
        SELECT * FROM dictionary_words
        WHERE language = :lang AND digit_sequence LIKE :prefix || '%'
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findByDigitPrefix(lang: String, prefix: String, limit: Int = 20): List<DictionaryWord>

    /** On-screen keyboard autocomplete: word prefix match */
    @Query("""
        SELECT * FROM dictionary_words
        WHERE language = :lang AND word LIKE :prefix || '%'
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findByWordPrefix(lang: String, prefix: String, limit: Int = 10): List<DictionaryWord>

    /** Check if a word exists in the dictionary (for spell check) */
    @Query("SELECT EXISTS(SELECT 1 FROM dictionary_words WHERE language = :lang AND word = :word COLLATE NOCASE)")
    suspend fun wordExists(lang: String, word: String): Boolean

    /** Find similar words for spell check suggestions (Levenshtein done in code) */
    @Query("""
        SELECT * FROM dictionary_words
        WHERE language = :lang AND LENGTH(word) BETWEEN LENGTH(:word) - 2 AND LENGTH(:word) + 2
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findSimilarLengthWords(lang: String, word: String, limit: Int = 100): List<DictionaryWord>

    /** Bulk insert for dictionary loading */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<DictionaryWord>)

    /** Update frequency for a word */
    @Query("UPDATE dictionary_words SET frequency = frequency + 1 WHERE language = :lang AND word = :word")
    suspend fun incrementFrequency(lang: String, word: String)

    /** Add user word to dictionary */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: DictionaryWord)

    /** Count words per language */
    @Query("SELECT COUNT(*) FROM dictionary_words WHERE language = :lang")
    suspend fun countWords(lang: String): Int

    /** Clear all words for a language (for dictionary reload) */
    @Query("DELETE FROM dictionary_words WHERE language = :lang AND user_added = 0")
    suspend fun clearLanguageDictionary(lang: String)
}
