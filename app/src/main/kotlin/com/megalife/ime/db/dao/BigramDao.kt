package com.megalife.ime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.megalife.ime.db.entity.BigramEntry

@Dao
interface BigramDao {

    @Query("""
        SELECT * FROM bigram_entries
        WHERE language = :lang AND word1 = :word1
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findByFirstWord(lang: String, word1: String, limit: Int = 5): List<BigramEntry>

    @Query("""
        UPDATE bigram_entries
        SET frequency = frequency + 1
        WHERE language = :lang AND word1 = :word1 AND word2 = :word2
    """)
    suspend fun incrementFrequency(lang: String, word1: String, word2: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bigram_entries WHERE language = :lang AND word1 = :word1 AND word2 = :word2)")
    suspend fun exists(lang: String, word1: String, word2: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: BigramEntry)

    /** Record a word pair: insert or increment frequency (atomic) */
    @Transaction
    suspend fun recordPair(lang: String, word1: String, word2: String) {
        if (exists(lang, word1, word2)) {
            incrementFrequency(lang, word1, word2)
        } else {
            insert(BigramEntry(word1 = word1, word2 = word2, language = lang))
        }
    }

    @Query("DELETE FROM bigram_entries")
    suspend fun clearAll()
}
