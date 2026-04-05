package com.megalife.ime.navigation

/**
 * Manages the D-pad focus ring state for keyboard navigation mode.
 * Tracks which key is currently focused and draws the 2dp accent border.
 */
class KeyboardFocusManager {

    var focusedRow: Int = 0
        private set
    var focusedCol: Int = 0
        private set
    var isActive: Boolean = false
        private set

    fun activate() {
        isActive = true
        focusedRow = 0
        focusedCol = 0
    }

    fun deactivate() {
        isActive = false
    }

    fun moveRight(maxCols: Int) {
        if (focusedCol < maxCols - 1) focusedCol++
    }

    fun moveLeft() {
        if (focusedCol > 0) focusedCol--
    }

    fun moveDown(maxRows: Int) {
        if (focusedRow < maxRows - 1) focusedRow++
    }

    fun moveUp() {
        if (focusedRow > 0) focusedRow--
    }

    fun reset() {
        focusedRow = 0
        focusedCol = 0
    }
}
