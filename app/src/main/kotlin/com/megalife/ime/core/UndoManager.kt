package com.megalife.ime.core

/**
 * Manages undo/redo stack for typing operations.
 * Stores the last 10 operations.
 */
class UndoManager(private val maxSize: Int = 10) {

    data class TextState(
        val text: String,
        val cursorPosition: Int,
        val hasAutoSpace: Boolean = false
    )

    private val undoStack = ArrayDeque<TextState>()
    private val redoStack = ArrayDeque<TextState>()

    /** Record a new text state */
    fun record(text: String, cursorPosition: Int, hasAutoSpace: Boolean = false) {
        undoStack.addLast(TextState(text, cursorPosition, hasAutoSpace))
        if (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        // New input clears redo stack
        redoStack.clear()
    }

    /**
     * Undo: returns the state that was just committed (the word to remove),
     * or null if no history.
     */
    fun undo(): TextState? {
        if (undoStack.isEmpty()) return null
        val removed = undoStack.removeLast()
        redoStack.addLast(removed)
        return removed
    }

    /** Redo: returns the next state, or null if no redo available */
    fun redo(): TextState? {
        val state = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(state)
        return state
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
