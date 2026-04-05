package com.megalife.ime.input

/**
 * Sealed interface representing the current input mode.
 * Each mode handles key presses differently.
 */
sealed interface InputMode {
    val displayName: String

    /** T9 multi-tap: press keys to cycle through letters */
    data object MultiTap : InputMode {
        override val displayName = "T9"
    }

    /** T9 predictive: press keys to build digit sequence, system suggests whole words */
    data object T9Predictive : InputMode {
        override val displayName = "T9+"
    }

    /** On-screen keyboard direct input */
    data object DirectInput : InputMode {
        override val displayName = "ABC"
    }

    /** Number input mode */
    data object Numeric : InputMode {
        override val displayName = "123"
    }

    /** Pass-through: keys go directly to app */
    data object Passthrough : InputMode {
        override val displayName = ""
    }
}
