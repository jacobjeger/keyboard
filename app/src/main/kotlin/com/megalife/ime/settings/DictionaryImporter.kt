package com.megalife.ime.settings

import android.content.Context
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.db.entity.DictionaryWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class DictionaryImporter(private val context: Context) {

    suspend fun importFromStream(stream: InputStream, language: String): Int = withContext(Dispatchers.IO) {
        val db = MegaLifeDatabase.getInstance(context)
        val dao = db.dictionaryDao()
        var count = 0

        stream.bufferedReader().useLines { lines ->
            val words = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.length <= 50 }
                .distinct()
                .map { line ->
                    val parts = line.split("\t")
                    val word = parts[0].trim()
                    val freq = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
                    DictionaryWord(
                        word = word,
                        language = language,
                        digitSequence = "", // Will be computed by the caller if needed
                        frequency = freq,
                        userAdded = true
                    )
                }
                .toList()

            // Batch insert
            for (batch in words.chunked(100)) {
                dao.insertAll(batch)
                count += batch.size
            }
        }
        count
    }
}
