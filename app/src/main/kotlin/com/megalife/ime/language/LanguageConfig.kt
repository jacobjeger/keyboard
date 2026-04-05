package com.megalife.ime.language

import java.util.Locale

/**
 * Complete configuration for a supported language.
 * Each language defines its T9 key mapping, on-screen touch layout,
 * script properties, and feature flags.
 */
data class LanguageConfig(
    val code: String,
    val locale: Locale,
    val displayName: String,
    val nativeDisplayName: String,
    val scriptType: ScriptType,
    val textDirection: TextDirection,
    val t9KeyMap: T9KeyMap,
    val touchLayout: TouchLayout,
    val dictionaryAsset: String,
    val hasT9Support: Boolean = true,
    val hasGlideSupport: Boolean = scriptType.supportsGlideTyping,
    val hasAutoCorrect: Boolean = scriptType.supportsAutoCorrect,
    val hasSpellCheck: Boolean = scriptType.supportsSpellCheck,
    /** Hebrew final forms: maps base char to final form */
    val finalForms: Map<Char, Char> = emptyMap(),
    /** Azure Speech locale string for voice input */
    val voiceLocale: String = code
)
