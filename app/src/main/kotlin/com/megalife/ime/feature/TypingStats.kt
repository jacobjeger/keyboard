package com.megalife.ime.feature

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TypingStats(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val PREF_STATS = "typing_stats_daily"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    data class DailyStats(
        val date: String,
        val wordsTyped: Int = 0,
        val charsTyped: Int = 0,
        val correctionsApplied: Int = 0,
        val emojisUsed: Int = 0
    )

    private fun today(): String = dateFormat.format(Date())

    private fun getStatsJson(): JSONObject {
        val json = prefs.getString(PREF_STATS, "{}") ?: "{}"
        return JSONObject(json)
    }

    private fun saveStats(stats: JSONObject) {
        prefs.edit().putString(PREF_STATS, stats.toString()).apply()
    }

    private fun getDayStats(date: String): DailyStats {
        val all = getStatsJson()
        val day = all.optJSONObject(date) ?: return DailyStats(date)
        return DailyStats(
            date = date,
            wordsTyped = day.optInt("words", 0),
            charsTyped = day.optInt("chars", 0),
            correctionsApplied = day.optInt("corrections", 0),
            emojisUsed = day.optInt("emojis", 0)
        )
    }

    private fun updateDayStats(date: String, update: (DailyStats) -> DailyStats) {
        val all = getStatsJson()
        val current = getDayStats(date)
        val updated = update(current)
        val dayJson = JSONObject().apply {
            put("words", updated.wordsTyped)
            put("chars", updated.charsTyped)
            put("corrections", updated.correctionsApplied)
            put("emojis", updated.emojisUsed)
        }
        all.put(date, dayJson)

        // Keep only last 30 days
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
        val cutoffStr = dateFormat.format(cutoff.time)
        val keys = all.keys().asSequence().toList()
        for (key in keys) {
            if (key < cutoffStr) all.remove(key)
        }

        saveStats(all)
    }

    fun recordWord() { updateDayStats(today()) { it.copy(wordsTyped = it.wordsTyped + 1) } }
    fun recordChars(count: Int) { updateDayStats(today()) { it.copy(charsTyped = it.charsTyped + count) } }
    fun recordCorrection() { updateDayStats(today()) { it.copy(correctionsApplied = it.correctionsApplied + 1) } }
    fun recordEmoji() { updateDayStats(today()) { it.copy(emojisUsed = it.emojisUsed + 1) } }

    fun getDailyStats(): DailyStats = getDayStats(today())

    fun getWeeklyStats(): List<DailyStats> {
        val result = mutableListOf<DailyStats>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            result.add(getDayStats(dateFormat.format(cal.time)))
        }
        return result
    }
}
