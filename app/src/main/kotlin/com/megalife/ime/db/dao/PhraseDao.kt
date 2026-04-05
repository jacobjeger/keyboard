package com.megalife.ime.db.dao

import androidx.room.*
import com.megalife.ime.db.entity.PhraseEntry

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases WHERE language = :language AND phrase LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun findByPrefix(language: String, prefix: String, limit: Int = 5): List<PhraseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: PhraseEntry)

    @Query("UPDATE phrases SET frequency = frequency + 1 WHERE language = :language AND phrase = :phrase")
    suspend fun incrementFrequency(language: String, phrase: String)
}
