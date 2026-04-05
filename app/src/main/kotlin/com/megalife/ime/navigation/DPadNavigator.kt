package com.megalife.ime.navigation

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.preference.PreferenceManager

/**
 * Handles D-pad navigation in two modes:
 * - APP_NAV: D-pad controls the app, D-pad UP closes keyboard
 * - KEYBOARD_NAV: D-pad navigates keyboard keys with focus ring
 */
class DPadNavigator(
    private val context: Context,
    private val onCloseKeyboard: () -> Unit,
    private val onKeyboardFocusMove: (direction: Int) -> Unit,
    private val onKeyboardFocusSelect: () -> Unit,
    private val onKeyboardFocusLongSelect: () -> Unit
) {
    var mode: DPadMode = DPadMode.APP_NAV
        private set

    fun initialize() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val saved = prefs.getString("pref_dpad_mode", "app_nav")
        mode = if (saved == "keyboard_nav") DPadMode.KEYBOARD_NAV else DPadMode.APP_NAV
    }

    fun toggleMode() {
        mode = when (mode) {
            DPadMode.APP_NAV -> DPadMode.KEYBOARD_NAV
            DPadMode.KEYBOARD_NAV -> DPadMode.APP_NAV
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(
            "pref_dpad_mode",
            if (mode == DPadMode.KEYBOARD_NAV) "keyboard_nav" else "app_nav"
        ).apply()

        val msg = when (mode) {
            DPadMode.APP_NAV -> "D-pad: App navigation"
            DPadMode.KEYBOARD_NAV -> "D-pad: Keyboard navigation"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handle a D-pad key event. Returns true if consumed.
     */
    fun onDPadEvent(keyCode: Int, isLongPress: Boolean): Boolean {
        return when (mode) {
            DPadMode.APP_NAV -> handleAppNavMode(keyCode)
            DPadMode.KEYBOARD_NAV -> handleKeyboardNavMode(keyCode, isLongPress)
        }
    }

    private fun handleAppNavMode(keyCode: Int): Boolean {
        // In app nav mode, D-pad UP closes the keyboard
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            onCloseKeyboard()
            return true
        }
        // All other D-pad events pass through to the app
        return false
    }

    private fun handleKeyboardNavMode(keyCode: Int, isLongPress: Boolean): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                onCloseKeyboard()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                onKeyboardFocusMove(KeyEvent.KEYCODE_DPAD_DOWN)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                onKeyboardFocusMove(KeyEvent.KEYCODE_DPAD_LEFT)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                onKeyboardFocusMove(KeyEvent.KEYCODE_DPAD_RIGHT)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (isLongPress) {
                    onKeyboardFocusLongSelect()
                } else {
                    onKeyboardFocusSelect()
                }
                return true
            }
        }
        return false
    }
}
