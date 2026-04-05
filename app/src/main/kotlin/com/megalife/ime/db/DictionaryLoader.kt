package com.megalife.ime.db

import android.content.Context
import android.util.Log
import com.megalife.ime.db.dao.DictionaryDao
import com.megalife.ime.db.entity.DictionaryWord
import com.megalife.ime.language.T9KeyMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads dictionary files from assets into the Room database.
 * Dictionary format: one word per line, format "word,frequency"
 * Computes digit sequences at load time for T9 lookup.
 */
class DictionaryLoader(
    private val context: Context,
    private val dictionaryDao: DictionaryDao
) {
    companion object {
        private const val TAG = "DictionaryLoader"
        private const val BATCH_SIZE = 500
    }

    /**
     * Load a dictionary file for a given language.
     * @param assetFileName e.g., "dict_en.txt"
     * @param language language code e.g., "en"
     * @param t9KeyMap the T9 key map for computing digit sequences
     */
    suspend fun loadDictionary(
        assetFileName: String,
        language: String,
        t9KeyMap: T9KeyMap
    ) = withContext(Dispatchers.IO) {
        val existingCount = dictionaryDao.countWords(language)
        if (existingCount > 0) {
            Log.d(TAG, "Dictionary for $language already loaded ($existingCount words)")
            return@withContext
        }

        Log.d(TAG, "Loading dictionary for $language from $assetFileName")
        var count = 0
        val batch = mutableListOf<DictionaryWord>()

        try {
            context.assets.open(assetFileName).bufferedReader().useLines { lines ->
                for (line in lines) {
                    val parts = line.trim().split(",", limit = 2)
                    if (parts.size < 2) continue

                    val word = parts[0].trim()
                    val frequency = parts[1].trim().toIntOrNull() ?: continue

                    if (word.isBlank()) continue

                    val digitSequence = t9KeyMap.wordToDigitSequence(word)
                    if (digitSequence.isEmpty()) continue

                    batch.add(
                        DictionaryWord(
                            word = word,
                            language = language,
                            digitSequence = digitSequence,
                            frequency = frequency
                        )
                    )

                    if (batch.size >= BATCH_SIZE) {
                        dictionaryDao.insertAll(batch)
                        count += batch.size
                        batch.clear()
                    }
                }
            }

            // Insert remaining batch
            if (batch.isNotEmpty()) {
                dictionaryDao.insertAll(batch)
                count += batch.size
            }

            Log.d(TAG, "Loaded $count words for $language")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dictionary for $language", e)
        }
    }

    /** Load default text shortcuts */
    suspend fun loadDefaultShortcuts(shortcutDao: com.megalife.ime.db.dao.ShortcutDao) =
        withContext(Dispatchers.IO) {
            if (shortcutDao.count() > 0) return@withContext

            val defaults = listOf(
                com.megalife.ime.db.entity.TextShortcut("bzh", "בעזרת השם"),
                com.megalife.ime.db.entity.TextShortcut("aiy", "אם ירצה השם"),
                com.megalife.ime.db.entity.TextShortcut("bh", "ברוך השם"),
            )
            defaults.forEach { shortcutDao.insert(it) }
            Log.d(TAG, "Loaded ${defaults.size} default shortcuts")
        }
}
