package com.megalife.ime.language

enum class ScriptType {
    LATIN,
    HEBREW,
    CYRILLIC,
    ARABIC,
    GEEZ;

    val supportsUpperCase: Boolean
        get() = this == LATIN || this == CYRILLIC

    val supportsAutoCorrect: Boolean
        get() = this == LATIN || this == CYRILLIC || this == HEBREW

    val supportsSpellCheck: Boolean
        get() = this == LATIN || this == CYRILLIC || this == HEBREW

    val supportsGlideTyping: Boolean
        get() = this == LATIN || this == CYRILLIC || this == HEBREW
}
