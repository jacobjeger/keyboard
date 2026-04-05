package com.megalife.ime.language

/** Definition of an on-screen keyboard layout */
data class TouchLayout(
    val rows: List<KeyRow>,
    val isRtl: Boolean = false
)

data class KeyRow(
    val keys: List<KeyDef>,
    /** Offset from left edge as fraction of key width (for staggered rows) */
    val leftPadding: Float = 0f
)

data class KeyDef(
    val primaryChar: String,
    val label: String = primaryChar,
    /** Characters shown on long-press popup */
    val popupChars: List<String> = emptyList(),
    /** Width relative to a standard key (1.0 = normal) */
    val widthWeight: Float = 1f,
    val type: KeyType = KeyType.CHARACTER,
    /** Sub-label shown below main label (e.g., T9 letters) */
    val subLabel: String = ""
)

enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    LANGUAGE,
    SYMBOLS,
    EMOJI,
    PERIOD,
    COMMA
}

/**
 * Returns an accessibility description for the key, suitable for TalkBack announcements.
 * If the key has an explicit accessibilityLabel, that is used; otherwise a sensible
 * default is derived from the key type and primary character.
 */
fun KeyDef.getAccessibilityDescription(): String {
    return when (type) {
        KeyType.SHIFT -> "Shift"
        KeyType.BACKSPACE -> "Delete"
        KeyType.ENTER -> "Enter"
        KeyType.SPACE -> "Space"
        KeyType.LANGUAGE -> "Switch language"
        KeyType.SYMBOLS -> "Symbols"
        KeyType.EMOJI -> "Emoji"
        KeyType.PERIOD -> "Period"
        KeyType.COMMA -> "Comma"
        else -> primaryChar
    }
}
