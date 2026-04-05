package com.megalife.ime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.megalife.ime.db.entity.LearnedWord

@Dao
interface LearnedWordDao {

    @Query("""
        SELECT * FROM learned_words
        WHERE language = :lang AND digit_sequence = :digits
        ORDER BY use_count DESC
        LIMIT :limit
    """)
    suspend fun findByDigitSequence(lang: String, digits: String, limit: Int = 10): List<LearnedWord>

    @Query("""
        SELECT * FROM learned_words
        WHERE language = :lang AND word LIKE :prefix || '%'
        ORDER BY use_count DESC
        LIMIT :limit
    """)
    suspend fun findByWordPrefix(lang: String, prefix: String, limit: Int = 10): List<LearnedWord>

    @Query("SELECT EXISTS(SELECT 1 FROM learned_words WHERE language = :lang AND word = :word COLLATE NOCASE)")
    suspend fun wordExists(lang: String, word: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: LearnedWord)

    @Query("""
        UPDATE learned_words
        SET use_count = use_count + 1, last_used = :timestamp
        WHERE language = :lang AND word = :word
    """)
    suspend fun incrementUseCount(lang: String, word: String, timestamp: Long = System.currentTimeMillis())

    /** Upsert: insert or increment (atomic) */
    @Transaction
    suspend fun upsert(word: String, language: String, digitSequence: String) {
        if (wordExists(language, word)) {
            incrementUseCount(language, word)
        } else {
            insert(LearnedWord(word = word, language = language, digitSequence = digitSequence))
        }
    }

    @Query("SELECT * FROM learned_words ORDER BY use_count DESC")
    suspend fun getAll(): List<LearnedWord>

    @Query("DELETE FROM learned_words")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM learned_words WHERE language = :lang")
    suspend fun countWords(lang: String): Int
}
