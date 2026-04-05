package com.megalife.ime.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.megalife.ime.db.entity.ClipboardItem

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_items ORDER BY is_pinned DESC, copied_at DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 10): List<ClipboardItem>

    @Insert
    suspend fun insert(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE clipboard_items SET is_pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    /** Delete unpinned items older than given timestamp */
    @Query("DELETE FROM clipboard_items WHERE is_pinned = 0 AND copied_at < :beforeTimestamp")
    suspend fun clearExpired(beforeTimestamp: Long)

    /** Delete all unpinned items */
    @Query("DELETE FROM clipboard_items WHERE is_pinned = 0")
    suspend fun clearUnpinned()

    /** Delete all items */
    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()

    /** Keep only the most recent N unpinned items */
    @Query("""
        DELETE FROM clipboard_items WHERE is_pinned = 0 AND id NOT IN (
            SELECT id FROM clipboard_items WHERE is_pinned = 0
            ORDER BY copied_at DESC LIMIT :keep
        )
    """)
    suspend fun trimToSize(keep: Int = 10)
}
