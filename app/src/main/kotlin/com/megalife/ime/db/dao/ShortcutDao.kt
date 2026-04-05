package com.megalife.ime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megalife.ime.db.entity.TextShortcut

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM text_shortcuts ORDER BY use_count DESC")
    suspend fun getAll(): List<TextShortcut>

    @Query("SELECT * FROM text_shortcuts WHERE shortcut = :trigger")
    suspend fun findByTrigger(trigger: String): TextShortcut?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: TextShortcut)

    @Query("DELETE FROM text_shortcuts WHERE shortcut = :trigger")
    suspend fun delete(trigger: String)

    @Query("UPDATE text_shortcuts SET use_count = use_count + 1 WHERE shortcut = :trigger")
    suspend fun incrementUseCount(trigger: String)

    @Query("SELECT COUNT(*) FROM text_shortcuts")
    suspend fun count(): Int
}
