package com.megalife.ime.settings

import android.content.Context
import android.os.Environment
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.feature.TypingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DataExportManager(private val context: Context) {

    suspend fun exportToJson(): File = withContext(Dispatchers.IO) {
        val db = MegaLifeDatabase.getInstance(context)
        val export = JSONObject()

        // Export learned words
        val learnedWords = db.learnedWordDao().getAll()
        val learnedArray = JSONArray()
        for (w in learnedWords) {
            learnedArray.put(JSONObject().apply {
                put("word", w.word)
                put("language", w.language)
                put("useCount", w.useCount)
            })
        }
        export.put("learnedWords", learnedArray)

        // Export text shortcuts
        val shortcuts = db.shortcutDao().getAll()
        val shortcutArray = JSONArray()
        for (s in shortcuts) {
            shortcutArray.put(JSONObject().apply {
                put("trigger", s.shortcut)
                put("expansion", s.expansion)
            })
        }
        export.put("textShortcuts", shortcutArray)

        // Export typing stats
        val stats = TypingStats(context)
        val weeklyStats = stats.getWeeklyStats()
        val statsArray = JSONArray()
        for (s in weeklyStats) {
            statsArray.put(JSONObject().apply {
                put("date", s.date)
                put("wordsTyped", s.wordsTyped)
                put("charsTyped", s.charsTyped)
                put("corrections", s.correctionsApplied)
                put("emojis", s.emojisUsed)
            })
        }
        export.put("typingStats", statsArray)

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "megalife_data_export.json"
        )
        file.writeText(export.toString(2))
        file
    }
}
