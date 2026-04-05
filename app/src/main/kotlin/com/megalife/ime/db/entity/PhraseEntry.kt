package com.megalife.ime.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class PhraseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phrase: String,
    val language: String,
    val frequency: Int = 1
)
