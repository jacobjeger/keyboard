package com.megalife.ime.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bigram_entries",
    indices = [
        Index(value = ["language", "word1"])
    ]
)
data class BigramEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word1") val word1: String,
    @ColumnInfo(name = "word2") val word2: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 1
)
