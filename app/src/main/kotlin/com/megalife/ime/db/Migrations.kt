package com.megalife.ime.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create saved_credentials table for password saver/autofill
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `saved_credentials` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `domain` TEXT NOT NULL,
                    `username` TEXT NOT NULL,
                    `password` TEXT NOT NULL,
                    `display_name` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_credentials_domain` ON `saved_credentials` (`domain`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS phrases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    phrase TEXT NOT NULL,
                    language TEXT NOT NULL,
                    frequency INTEGER NOT NULL DEFAULT 1
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_phrases_language_phrase ON phrases(language, phrase)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS app_language_preferences (
                    packageName TEXT PRIMARY KEY NOT NULL,
                    languageCode TEXT NOT NULL
                )
            """)
        }
    }
}
