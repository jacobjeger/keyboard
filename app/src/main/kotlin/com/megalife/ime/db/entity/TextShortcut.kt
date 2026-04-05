package com.megalife.ime.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_shortcuts")
data class TextShortcut(
    @PrimaryKey
    @ColumnInfo(name = "shortcut") val shortcut: String,
    @ColumnInfo(name = "expansion") val expansion: String,
    @ColumnInfo(name = "use_count") val useCount: Int = 0
)
