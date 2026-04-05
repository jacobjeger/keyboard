package com.megalife.ime.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.megalife.ime.db.dao.*
import com.megalife.ime.db.entity.*

@Database(
    entities = [
        DictionaryWord::class,
        LearnedWord::class,
        BigramEntry::class,
        ClipboardItem::class,
        TextShortcut::class,
        SavedCredential::class,
        PhraseEntry::class,
        AppLanguagePreference::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MegaLifeDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): DictionaryDao
    abstract fun learnedWordDao(): LearnedWordDao
    abstract fun bigramDao(): BigramDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun credentialDao(): CredentialDao
    abstract fun phraseDao(): PhraseDao
    abstract fun appLanguageDao(): AppLanguageDao

    companion object {
        @Volatile
        private var INSTANCE: MegaLifeDatabase? = null

        fun getInstance(context: Context): MegaLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MegaLifeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MegaLifeDatabase::class.java,
                "megalife_ime.db"
            )
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                .build()
        }
    }
}
