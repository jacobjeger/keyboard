package com.megalife.ime.ui

import android.view.View
import android.widget.TextView
import com.megalife.ime.R
import com.megalife.ime.input.InputMode
import com.megalife.ime.input.ShiftState
import com.megalife.ime.language.LanguageConfig

/**
 * Manages the compact T9 status/indicator bar shown in T9 mode.
 * Shows: mode label, CAPS indicator, composing text, letter options.
 */
class T9StatusView(private val view: View) {

    private val modeLabel: TextView = view.findViewById(R.id.t9_mode_label)
    private val capsIndicator: TextView = view.findViewById(R.id.caps_indicator)
    private val composeText: TextView = view.findViewById(R.id.t9_compose_text)
    private val letterOptions: TextView = view.findViewById(R.id.t9_letter_options)

    fun updateMode(mode: InputMode) {
        modeLabel.text = mode.displayName
    }

    fun updateLanguage(lang: LanguageConfig) {
        // Could show language indicator
    }

    fun updateComposingText(text: String) {
        composeText.text = text
    }

    /**
     * Update the letter options display for the current multi-tap key.
     * Highlights the currently selected character.
     * Example: pressed 4 twice → "G [H] I"
     */
    fun updateLetterOptions(key: Int, charIndex: Int, chars: List<Char>) {
        if (key < 0 || chars.isEmpty()) {
            letterOptions.text = ""
            return
        }

        val sb = StringBuilder()
        for ((i, ch) in chars.withIndex()) {
            if (i > 0) sb.append(" ")
            if (i == charIndex) {
                sb.append("[${ch.uppercase()}]")
            } else {
                sb.append(ch.uppercase())
            }
        }
        letterOptions.text = sb.toString()
    }

    fun updateCapsIndicator(state: ShiftState) {
        capsIndicator.visibility = if (state.isCapsLock) View.VISIBLE else View.GONE
    }
}
