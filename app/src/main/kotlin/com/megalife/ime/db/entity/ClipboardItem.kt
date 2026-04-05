package com.megalife.ime.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "clip_text") val clipText: String,
    @ColumnInfo(name = "copied_at") val copiedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false
)
