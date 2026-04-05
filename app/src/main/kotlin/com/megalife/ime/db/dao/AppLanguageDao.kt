package com.megalife.ime.db.dao

import androidx.room.*
import com.megalife.ime.db.entity.AppLanguagePreference

@Dao
interface AppLanguageDao {
    @Query("SELECT * FROM app_language_preferences WHERE packageName = :pkg")
    suspend fun findByPackage(pkg: String): AppLanguagePreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: AppLanguagePreference)

    @Query("SELECT * FROM app_language_preferences ORDER BY packageName")
    suspend fun getAll(): List<AppLanguagePreference>

    @Delete
    suspend fun delete(pref: AppLanguagePreference)
}
