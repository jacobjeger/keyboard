package com.megalife.ime.feature

import android.content.ClipboardManager
import android.content.Context
import com.megalife.ime.db.dao.ClipboardDao
import com.megalife.ime.db.entity.ClipboardItem
import com.megalife.ime.security.KeystoreEncryption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Monitors the system clipboard and stores history in Room.
 * Items expire after 1 hour unless pinned. Max 10 items.
 */
class ClipboardHistoryManager(
    private val context: Context,
    private val clipboardDao: ClipboardDao,
    private val scope: CoroutineScope
) {
    companion object {
        private const val EXPIRY_MS = 60 * 60 * 1000L // 1 hour
        private const val MAX_ITEMS = 10
    }

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipboardChanged()
    }

    fun startMonitoring() {
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    fun stopMonitoring() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    private fun onClipboardChanged() {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).text?.toString() ?: return
        if (text.isBlank()) return

        scope.launch(Dispatchers.IO) {
            val encrypted = KeystoreEncryption.encrypt(text)
            clipboardDao.insert(ClipboardItem(clipText = encrypted))
            clipboardDao.trimToSize(MAX_ITEMS)
            clipboardDao.clearExpired(System.currentTimeMillis() - EXPIRY_MS)
        }
    }

    suspend fun getItems(): List<ClipboardItem> {
        return clipboardDao.getAll(MAX_ITEMS).mapNotNull { item ->
            val decrypted = KeystoreEncryption.decrypt(item.clipText) ?: return@mapNotNull null
            item.copy(clipText = decrypted)
        }
    }

    suspend fun pinItem(id: Long) {
        clipboardDao.setPinned(id, true)
    }

    suspend fun unpinItem(id: Long) {
        clipboardDao.setPinned(id, false)
    }

    suspend fun deleteItem(id: Long) {
        clipboardDao.delete(id)
    }

    suspend fun clearAll() {
        clipboardDao.clearUnpinned()
    }
}
