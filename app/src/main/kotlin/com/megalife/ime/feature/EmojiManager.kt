package com.megalife.ime.feature

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.json.JSONObject

/**
 * Manages emoji state: recent emoji, skin tone preference, search.
 * Stores recent emojis in SharedPreferences (last 30).
 */
class EmojiManager(private val context: Context) {

    companion object {
        private const val PREF_RECENT_EMOJI = "recent_emojis"
        private const val PREF_SKIN_TONE = "emoji_skin_tone"
        private const val PREF_SKIN_TONES_PER_EMOJI = "emoji_skin_tones_per"
        private const val MAX_RECENT = 30
    }

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /** Get recently used emojis */
    fun getRecent(): List<String> {
        val saved = prefs.getString(PREF_RECENT_EMOJI, "") ?: ""
        return if (saved.isEmpty()) emptyList()
        else saved.split("|").filter { it.isNotEmpty() }
    }

    /** Record an emoji as used */
    fun recordUsage(emoji: String) {
        val recent = getRecent().toMutableList()
        recent.remove(emoji)
        recent.add(0, emoji)
        val trimmed = recent.take(MAX_RECENT)
        prefs.edit().putString(PREF_RECENT_EMOJI, trimmed.joinToString("|")).apply()
    }

    /** Get preferred skin tone index (0 = default yellow) */
    fun getSkinTone(): Int {
        return prefs.getInt(PREF_SKIN_TONE, 0)
    }

    /** Set preferred skin tone index */
    fun setSkinTone(index: Int) {
        prefs.edit().putInt(PREF_SKIN_TONE, index).apply()
    }

    /** Clear recent emoji history */
    fun clearRecent() {
        prefs.edit().remove(PREF_RECENT_EMOJI).apply()
    }

    /** Search emoji by name */
    fun search(query: String): List<EmojiData.Emoji> {
        return EmojiData.search(query)
    }

    /** Get all emojis for a category, with Recent prepended */
    fun getForCategory(category: EmojiData.EmojiCategory): List<EmojiData.Emoji> {
        if (category == EmojiData.EmojiCategory.RECENT) {
            return getRecent().mapNotNull { emoji ->
                EmojiData.allEmojis.find { it.emoji == emoji }
            }
        }
        return EmojiData.getByCategory(category)
    }

    // ==================== Per-Emoji Skin Tone Memory ====================

    /**
     * Get the remembered skin tone for a specific emoji.
     * Falls back to the global skin tone preference if no per-emoji override exists.
     */
    fun getSkinToneForEmoji(baseEmoji: String): Int {
        val json = prefs.getString(PREF_SKIN_TONES_PER_EMOJI, "{}") ?: "{}"
        return try {
            val obj = JSONObject(json)
            if (obj.has(baseEmoji)) obj.getInt(baseEmoji) else getSkinTone()
        } catch (e: Exception) {
            getSkinTone()
        }
    }

    /**
     * Remember the skin tone choice for a specific emoji.
     * This overrides the global default for this particular base emoji.
     */
    fun setSkinToneForEmoji(baseEmoji: String, tone: Int) {
        val json = prefs.getString(PREF_SKIN_TONES_PER_EMOJI, "{}") ?: "{}"
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            JSONObject()
        }
        obj.put(baseEmoji, tone)
        prefs.edit().putString(PREF_SKIN_TONES_PER_EMOJI, obj.toString()).apply()
    }

    /**
     * Clear all per-emoji skin tone overrides (revert to global default).
     */
    fun clearPerEmojiSkinTones() {
        prefs.edit().remove(PREF_SKIN_TONES_PER_EMOJI).apply()
    }
}
