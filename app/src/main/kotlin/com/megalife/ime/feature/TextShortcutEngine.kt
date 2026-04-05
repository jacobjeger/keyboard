package com.megalife.ime.feature

import com.megalife.ime.db.dao.ShortcutDao
import com.megalife.ime.db.entity.TextShortcut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Watches typed text for shortcut triggers and expands them.
 * Built-in defaults: "bzh" → "בעזרת השם", "aiy" → "אם ירצה השם", "bh" → "ברוך השם"
 */
class TextShortcutEngine(
    private val shortcutDao: ShortcutDao,
    private val scope: CoroutineScope
) {
    // Cache shortcuts in memory for fast lookup
    private var shortcutCache: Map<String, String> = emptyMap()

    init {
        refreshCache()
    }

    fun refreshCache() {
        scope.launch(Dispatchers.IO) {
            val all = shortcutDao.getAll()
            shortcutCache = all.associate { it.shortcut to it.expansion }
        }
    }

    /**
     * Check if the given word is a shortcut trigger.
     * Called when space is pressed after a word.
     * Returns the expansion if it's a shortcut, null otherwise.
     */
    fun checkAndExpand(word: String): String? {
        val expansion = shortcutCache[word] ?: return null
        scope.launch(Dispatchers.IO) {
            shortcutDao.incrementUseCount(word)
        }
        return expansion
    }

    suspend fun getAllShortcuts(): List<TextShortcut> {
        return shortcutDao.getAll()
    }

    suspend fun addShortcut(trigger: String, expansion: String) {
        shortcutDao.insert(TextShortcut(shortcut = trigger, expansion = expansion))
        refreshCache()
    }

    suspend fun deleteShortcut(trigger: String) {
        shortcutDao.delete(trigger)
        refreshCache()
    }
}
