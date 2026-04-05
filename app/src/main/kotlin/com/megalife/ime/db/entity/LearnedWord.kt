package com.megalife.ime.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learned_words",
    indices = [
        Index(value = ["language", "digit_sequence"]),
        Index(value = ["language", "word"], unique = true)
    ]
)
data class LearnedWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "digit_sequence") val digitSequence: String,
    @ColumnInfo(name = "use_count") val useCount: Int = 1,
    @ColumnInfo(name = "last_used") val lastUsed: Long = System.currentTimeMillis()
)
