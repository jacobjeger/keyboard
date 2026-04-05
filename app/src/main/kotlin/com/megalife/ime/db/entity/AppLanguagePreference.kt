package com.megalife.ime.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_language_preferences")
data class AppLanguagePreference(
    @PrimaryKey val packageName: String,
    val languageCode: String
)
