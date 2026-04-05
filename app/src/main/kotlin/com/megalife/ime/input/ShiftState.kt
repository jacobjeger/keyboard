package com.megalife.ime.input

/**
 * Shift/caps lock state machine.
 * LOWER -> tap shift -> UPPER_NEXT (one letter) -> types letter -> LOWER
 * LOWER -> double-tap shift -> CAPS_LOCK -> tap shift -> LOWER
 */
enum class ShiftState {
    LOWER,
    UPPER_NEXT,
    CAPS_LOCK;

    /** Apply shift to a character */
    fun applyToChar(ch: Char): Char = when (this) {
        LOWER -> ch.lowercaseChar()
        UPPER_NEXT, CAPS_LOCK -> ch.uppercaseChar()
    }

    /** State transition after a character is typed */
    fun afterCharTyped(): ShiftState = when (this) {
        UPPER_NEXT -> LOWER
        else -> this
    }

    /** State transition on shift key tap */
    fun onShiftTap(): ShiftState = when (this) {
        LOWER -> UPPER_NEXT
        UPPER_NEXT -> LOWER
        CAPS_LOCK -> LOWER
    }

    /** State transition on shift key double-tap */
    fun onShiftDoubleTap(): ShiftState = CAPS_LOCK

    val isUpperCase: Boolean get() = this != LOWER
    val isCapsLock: Boolean get() = this == CAPS_LOCK
}
