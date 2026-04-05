package com.megalife.ime.db.dao

import androidx.room.*
import com.megalife.ime.db.entity.SavedCredential

@Dao
interface CredentialDao {

    @Query("SELECT * FROM saved_credentials WHERE domain = :domain ORDER BY updated_at DESC")
    suspend fun findByDomain(domain: String): List<SavedCredential>

    @Query("SELECT * FROM saved_credentials WHERE domain = :domain AND username = :username LIMIT 1")
    suspend fun findByDomainAndUsername(domain: String, username: String): SavedCredential?

    @Query("SELECT * FROM saved_credentials WHERE domain LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    suspend fun searchByDomain(query: String): List<SavedCredential>

    @Query("SELECT * FROM saved_credentials ORDER BY updated_at DESC")
    suspend fun getAll(): List<SavedCredential>

    @Insert
    suspend fun insert(credential: SavedCredential): Long

    @Update
    suspend fun update(credential: SavedCredential)

    @Query("DELETE FROM saved_credentials WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM saved_credentials")
    suspend fun count(): Int

    @Query("DELETE FROM saved_credentials")
    suspend fun clearAll()
}
