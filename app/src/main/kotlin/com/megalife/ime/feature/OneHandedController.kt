package com.megalife.ime.feature

import android.content.Context
import android.view.View
import androidx.preference.PreferenceManager

/**
 * Controls one-handed mode.
 * Shrinks keyboard to 75% width, anchored to left or right side.
 * Suggestion bar stays full width.
 */
class OneHandedController(private val context: Context) {

    enum class Anchor { NONE, LEFT, RIGHT }

    companion object {
        private const val PREF_ONE_HANDED = "pref_one_handed_anchor"
        private const val SCALE = 0.75f
    }

    var anchor: Anchor = Anchor.NONE
        private set

    fun initialize() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        anchor = when (prefs.getString(PREF_ONE_HANDED, "none")) {
            "left" -> Anchor.LEFT
            "right" -> Anchor.RIGHT
            else -> Anchor.NONE
        }
    }

    /** Toggle one-handed mode. Cycles: NONE -> LEFT -> RIGHT -> NONE */
    fun toggle(): Anchor {
        anchor = when (anchor) {
            Anchor.NONE -> Anchor.LEFT
            Anchor.LEFT -> Anchor.RIGHT
            Anchor.RIGHT -> Anchor.NONE
        }
        persist()
        return anchor
    }

    /** Switch anchor side (left <-> right) */
    fun switchSide(): Anchor {
        anchor = when (anchor) {
            Anchor.LEFT -> Anchor.RIGHT
            Anchor.RIGHT -> Anchor.LEFT
            Anchor.NONE -> Anchor.NONE
        }
        persist()
        return anchor
    }

    /** Return to full width */
    fun disable(): Anchor {
        anchor = Anchor.NONE
        persist()
        return anchor
    }

    /** Apply one-handed transform to a keyboard view */
    fun applyToView(keyboardView: View) {
        when (anchor) {
            Anchor.NONE -> {
                keyboardView.scaleX = 1f
                keyboardView.scaleY = 1f
                keyboardView.translationX = 0f
                keyboardView.pivotX = keyboardView.width / 2f
            }
            Anchor.LEFT -> {
                keyboardView.scaleX = SCALE
                keyboardView.scaleY = SCALE
                keyboardView.pivotX = 0f
                keyboardView.pivotY = keyboardView.height.toFloat()
                keyboardView.translationX = 0f
            }
            Anchor.RIGHT -> {
                keyboardView.scaleX = SCALE
                keyboardView.scaleY = SCALE
                keyboardView.pivotX = keyboardView.width.toFloat()
                keyboardView.pivotY = keyboardView.height.toFloat()
                keyboardView.translationX = 0f
            }
        }
    }

    val isEnabled: Boolean get() = anchor != Anchor.NONE

    private fun persist() {
        val value = when (anchor) {
            Anchor.LEFT -> "left"
            Anchor.RIGHT -> "right"
            Anchor.NONE -> "none"
        }
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_ONE_HANDED, value).apply()
    }
}
