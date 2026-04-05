package com.megalife.ime.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_words",
    indices = [
        Index(value = ["language", "digit_sequence"]),
        Index(value = ["language", "word"])
    ]
)
data class DictionaryWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "digit_sequence") val digitSequence: String,
    @ColumnInfo(name = "frequency") val frequency: Int,
    @ColumnInfo(name = "user_added") val userAdded: Boolean = false
)
