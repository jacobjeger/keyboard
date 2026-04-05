package com.megalife.ime.settings

/** All SharedPreference key constants */
object PreferenceKeys {
    // Typing
    const val DEFAULT_MODE = "pref_default_mode"
    const val DPAD_MODE = "pref_dpad_mode"
    const val T9_TIMEOUT = "pref_t9_timeout"
    const val LONG_PRESS_DURATION = "pref_long_press_duration"
    const val AUTOCORRECT = "pref_autocorrect"
    const val SPELL_CHECK = "pref_spell_check"
    const val AUTO_CAPITALIZE = "pref_auto_capitalize"
    const val SMART_PUNCTUATION = "pref_smart_punctuation"
    const val WORD_LEARNING = "pref_word_learning"
    const val GLIDE_TYPING = "pref_glide_typing"

    // Appearance
    const val KEY_HEIGHT = "pref_key_height"
    const val KEY_BORDER = "pref_key_border"
    const val KEY_RADIUS = "pref_key_radius"
    const val KEY_TEXT_SIZE = "pref_key_text_size"
    const val NUMBER_ROW = "pref_number_row"

    // Languages
    const val ENABLED_LANGUAGES = "enabled_languages"
    const val CURRENT_LANGUAGE = "current_language"

    // Hebrew
    const val GEMATRIA = "pref_gematria"
    const val GEMATRIA_METHOD = "pref_gematria_method"
    const val NIKUD = "pref_nikud"
    const val PER_APP_LANGUAGE = "pref_per_app_language"

    // Features
    const val SUGGESTION_BAR = "pref_suggestion_bar"
    const val CLIPBOARD_HISTORY = "pref_clipboard_history"
    const val EMOJI_KEYBOARD = "pref_emoji_keyboard"
    const val EMOJI_PREDICTION = "pref_emoji_prediction"
    const val VOICE_INPUT = "pref_voice_input"
    const val VIBRATION = "pref_vibration"
    const val KEY_SOUND = "pref_key_sound"

    // Theme
    const val THEME = "pref_theme"

    // Autofill / Password Manager
    const val AUTOFILL_ENABLED = "pref_autofill_enabled"
    const val AUTOFILL_BIOMETRIC = "pref_autofill_biometric"

    // Custom Theme
    const val CUSTOM_THEME_JSON = "pref_custom_theme_json"

    // Accessibility
    const val FLOATING_KEYBOARD = "pref_floating_keyboard"
    const val HIGH_CONTRAST = "pref_high_contrast"
    const val VIBRATION_STRENGTH = "pref_vibration_strength"
    const val KEY_PRESS_DELAY = "pref_key_press_delay"

    // Privacy
    const val CLEAR_LEARNED = "pref_clear_learned"
    const val CLEAR_CLIPBOARD = "pref_clear_clipboard"
}
