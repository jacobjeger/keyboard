package com.megalife.ime.core

import android.view.inputmethod.InputConnection

/**
 * Extension functions for safe InputConnection operations.
 */
object InputConnectionHelper {

    /** Safely commit text, handling null InputConnection */
    fun InputConnection?.safeCommitText(text: CharSequence, newCursorPosition: Int = 1): Boolean {
        return this?.commitText(text, newCursorPosition) ?: false
    }

    /** Safely delete surrounding text */
    fun InputConnection?.safeDeleteSurrounding(before: Int, after: Int): Boolean {
        return this?.deleteSurroundingText(before, after) ?: false
    }

    /** Safely set composing text */
    fun InputConnection?.safeSetComposing(text: CharSequence, newCursorPosition: Int = 1): Boolean {
        return this?.setComposingText(text, newCursorPosition) ?: false
    }

    /** Safely finish composing */
    fun InputConnection?.safeFinishComposing(): Boolean {
        return this?.finishComposingText() ?: false
    }

    /** Get text before cursor, safely */
    fun InputConnection?.safeGetTextBefore(length: Int): CharSequence? {
        return this?.getTextBeforeCursor(length, 0)
    }

    /** Get text after cursor, safely */
    fun InputConnection?.safeGetTextAfter(length: Int): CharSequence? {
        return this?.getTextAfterCursor(length, 0)
    }

    /** Check if the character before cursor matches */
    fun InputConnection?.charBeforeCursor(): Char? {
        val text = this?.getTextBeforeCursor(1, 0)
        return if (text != null && text.isNotEmpty()) text[0] else null
    }

    /** Check if we're at the start of a sentence (after . ! ? or empty) */
    fun InputConnection?.isStartOfSentence(): Boolean {
        val text = this?.getTextBeforeCursor(2, 0) ?: return true
        if (text.isEmpty()) return true
        val trimmed = text.toString().trimEnd()
        if (trimmed.isEmpty()) return true
        return trimmed.last() in listOf('.', '!', '?')
    }

    /** Insert Unicode direction marker when switching between RTL and LTR text */
    fun InputConnection?.insertDirectionMarker(isRtl: Boolean) {
        val marker = if (isRtl) "\u200F" else "\u200E"  // RLM or LRM
        this?.commitText(marker, 1)
    }

    /** Delete one word backwards */
    fun InputConnection?.deleteWordBackward() {
        if (this == null) return
        val text = getTextBeforeCursor(50, 0)?.toString() ?: return
        if (text.isEmpty()) return

        var i = text.length - 1
        // Skip trailing spaces
        while (i >= 0 && text[i] == ' ') i--
        // Skip word characters
        while (i >= 0 && text[i] != ' ') i--

        val charsToDelete = text.length - i - 1
        if (charsToDelete > 0) {
            deleteSurroundingText(charsToDelete, 0)
        }
    }
}
