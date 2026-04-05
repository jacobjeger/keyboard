package com.megalife.ime.language

import android.content.Context
import androidx.preference.PreferenceManager
import com.megalife.ime.language.locale.AmharicConfig
import com.megalife.ime.language.locale.ArabicConfig
import com.megalife.ime.language.locale.EnglishConfig
import com.megalife.ime.language.locale.FrenchConfig
import com.megalife.ime.language.locale.HebrewConfig
import com.megalife.ime.language.locale.RussianConfig
import com.megalife.ime.language.locale.YiddishConfig

/**
 * Singleton registry of all supported languages.
 * Manages enabled languages, language cycling, and persistence.
 * All public methods are synchronized for thread safety.
 */
object LanguageRegistry {

    private const val PREF_CURRENT_LANGUAGE = "current_language"
    private const val PREF_ENABLED_LANGUAGES = "enabled_languages"
    private const val PREF_ENABLED_LANGUAGES_ORDERED = "enabled_languages_ordered"

    /** All available languages in order */
    val allLanguages: List<LanguageConfig> = listOf(
        EnglishConfig.config,
        HebrewConfig.config,
        ArabicConfig.config,
        YiddishConfig.config,
        RussianConfig.config,
        FrenchConfig.config,
        AmharicConfig.config
    )

    private val languageMap: Map<String, LanguageConfig> = allLanguages.associateBy { it.code }

    private var enabledLanguageCodes: MutableList<String> = mutableListOf("en", "he", "ar", "yi", "ru", "fr", "am")
    private var currentIndex: Int = 0

    val currentLanguage: LanguageConfig
        @Synchronized get() {
            val enabledLangs = enabledLanguages
            return if (enabledLangs.isNotEmpty()) {
                enabledLangs[currentIndex.coerceIn(0, enabledLangs.size - 1)]
            } else {
                EnglishConfig.config
            }
        }

    val enabledLanguages: List<LanguageConfig>
        @Synchronized get() = enabledLanguageCodes.mapNotNull { languageMap[it] }

    @Synchronized
    fun initialize(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Use ordered comma-separated string for deterministic ordering
        val orderedSaved = prefs.getString(PREF_ENABLED_LANGUAGES_ORDERED, null)
        if (orderedSaved != null) {
            enabledLanguageCodes = orderedSaved.split(",")
                .filter { it.isNotEmpty() }
                .toMutableList()
        } else {
            // Migrate from unordered StringSet if present
            val saved = prefs.getStringSet(PREF_ENABLED_LANGUAGES, null)
            if (saved != null) {
                enabledLanguageCodes = saved.sorted().toMutableList()
                // Persist as ordered string
                prefs.edit()
                    .putString(PREF_ENABLED_LANGUAGES_ORDERED, enabledLanguageCodes.joinToString(","))
                    .apply()
            }
        }

        // Ensure at least English is enabled
        if (enabledLanguageCodes.isEmpty()) enabledLanguageCodes.add("en")

        val savedLang = prefs.getString(PREF_CURRENT_LANGUAGE, "en") ?: "en"
        currentIndex = enabledLanguageCodes.indexOf(savedLang).coerceAtLeast(0)
    }

    /** Cycle to the next enabled language. Returns the new current language. */
    @Synchronized
    fun nextLanguage(context: Context): LanguageConfig {
        val enabled = enabledLanguages
        if (enabled.size <= 1) return currentLanguage
        currentIndex = (currentIndex + 1) % enabled.size
        persistCurrentLanguage(context)
        return currentLanguage
    }

    /** Set a specific language as current */
    @Synchronized
    fun setLanguage(code: String, context: Context) {
        val idx = enabledLanguageCodes.indexOf(code)
        if (idx >= 0) {
            currentIndex = idx
            persistCurrentLanguage(context)
        }
    }

    fun getLanguage(code: String): LanguageConfig? = languageMap[code]

    private fun persistCurrentLanguage(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_CURRENT_LANGUAGE, currentLanguage.code).apply()
    }
}
