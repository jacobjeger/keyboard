package com.megalife.ime.core

import android.view.KeyEvent
import com.megalife.ime.input.InputMode

/**
 * Routes hardware key events to the appropriate handler.
 * Maps Android KeyEvent codes to semantic actions.
 */
class KeyRouter(
    private val inputEngine: InputEngine,
    private val onToggleMode: () -> Unit,
    private val onToggleDPadMode: () -> Unit,
    private val onShowSettings: () -> Unit,
    private val onDPadEvent: (keyCode: Int, isLongPress: Boolean) -> Boolean
) {
    companion object {
        /** Map KEYCODE_X to logical key number 0-9 */
        fun keyCodeToNumber(keyCode: Int): Int? {
            return when (keyCode) {
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> 0
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> 1
                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> 2
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> 3
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> 4
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> 5
                KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> 6
                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> 7
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> 8
                KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> 9
                else -> null
            }
        }

        fun isDPad(keyCode: Int): Boolean {
            return keyCode in listOf(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER
            )
        }

        fun isStar(keyCode: Int) = keyCode == KeyEvent.KEYCODE_STAR
        fun isPound(keyCode: Int) = keyCode == KeyEvent.KEYCODE_POUND
    }

    /**
     * Route a key down event. Returns true if consumed.
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isLongPress = event.repeatCount > 0

        // In T9 Predictive mode, D-pad left/right cycle through word candidates
        if (inputEngine.inputMode == InputMode.T9Predictive && !isLongPress) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                inputEngine.nextPredictiveCandidate()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                inputEngine.prevPredictiveCandidate()
                return true
            }
        }

        // D-pad keys
        if (isDPad(keyCode)) {
            return onDPadEvent(keyCode, isLongPress)
        }

        // Star (*) key: cycle language
        if (isStar(keyCode)) {
            if (isLongPress) {
                // Long press star: open settings
                onShowSettings()
            } else {
                inputEngine.nextLanguage()
            }
            return true
        }

        // Pound (#) key: toggle T9 / on-screen mode
        if (isPound(keyCode)) {
            if (isLongPress) {
                // Long press #: toggle D-pad mode
                onToggleDPadMode()
            } else {
                onToggleMode()
            }
            return true
        }

        // Number keys
        val number = keyCodeToNumber(keyCode)
        if (number != null) {
            return routeNumberKey(number, isLongPress)
        }

        // Backspace
        if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
            if (isLongPress) {
                inputEngine.onBackspaceLongPress()
            } else {
                inputEngine.onBackspace()
            }
            return true
        }

        // Enter
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            inputEngine.onEnter()
            return false // Let the IME service handle the editor action
        }

        // Volume down = undo (when keyboard visible)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // Handled by the IME service
            return false
        }

        return false
    }

    private fun routeNumberKey(number: Int, isLongPress: Boolean): Boolean {
        when (number) {
            0 -> {
                inputEngine.onSpace()
            }
            1 -> {
                // Punctuation key
                val chars = inputEngine.currentLanguage.t9KeyMap.key1Chars
                if (chars.isNotEmpty()) {
                    // In T9 mode, cycle through punctuation
                    inputEngine.onT9KeyPress(1)
                }
            }
            in 2..9 -> {
                if (isLongPress) {
                    // Long press: insert the number digit
                    inputEngine.commitComposingWord()
                    inputEngine.onCharacterInput(number.toString())
                } else {
                    inputEngine.onT9KeyPress(number)
                }
            }
        }
        return true
    }
}
