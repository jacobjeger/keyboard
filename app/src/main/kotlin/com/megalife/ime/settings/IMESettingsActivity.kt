package com.megalife.ime.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.megalife.ime.R
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings activity for the MegaLife Keyboard.
 * No launcher icon — accessed via long-press language key or Android Settings.
 */
class IMESettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.settings_title)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, MainSettingsFragment())
                .commit()
        }
    }

    class MainSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            // ===== TYPING =====
            val typingCategory = PreferenceCategory(context).apply {
                title = getString(R.string.settings_typing)
            }
            screen.addPreference(typingCategory)

            typingCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.DEFAULT_MODE
                title = getString(R.string.pref_default_mode)
                entries = arrayOf("T9", "On-screen")
                entryValues = arrayOf("t9", "keyboard")
                setDefaultValue("t9")
            })

            typingCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.DPAD_MODE
                title = getString(R.string.pref_dpad_mode)
                entries = arrayOf("App navigation", "Keyboard navigation")
                entryValues = arrayOf("app_nav", "keyboard_nav")
                setDefaultValue("app_nav")
            })

            typingCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.T9_TIMEOUT
                title = getString(R.string.pref_t9_timeout)
                entries = arrayOf("600ms", "800ms", "1000ms")
                entryValues = arrayOf("600", "800", "1000")
                setDefaultValue("800")
            })

            typingCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.LONG_PRESS_DURATION
                title = getString(R.string.pref_long_press_duration)
                entries = arrayOf("300ms", "500ms")
                entryValues = arrayOf("300", "500")
                setDefaultValue("500")
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.AUTOCORRECT
                title = getString(R.string.pref_autocorrect)
                setDefaultValue(false) // Off by default
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.SPELL_CHECK
                title = getString(R.string.pref_spell_check)
                setDefaultValue(true)
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.AUTO_CAPITALIZE
                title = getString(R.string.pref_auto_capitalize)
                setDefaultValue(true)
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.SMART_PUNCTUATION
                title = getString(R.string.pref_smart_punctuation)
                setDefaultValue(true)
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.WORD_LEARNING
                title = getString(R.string.pref_word_learning)
                setDefaultValue(true)
            })

            typingCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.GLIDE_TYPING
                title = getString(R.string.pref_glide_typing)
                setDefaultValue(true)
            })

            // ===== APPEARANCE =====
            val appearanceCategory = PreferenceCategory(context).apply {
                title = getString(R.string.settings_appearance)
            }
            screen.addPreference(appearanceCategory)

            appearanceCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.KEY_HEIGHT
                title = getString(R.string.pref_key_height)
                entries = arrayOf("Small", "Medium", "Large")
                entryValues = arrayOf("small", "medium", "large")
                setDefaultValue("medium")
            })

            appearanceCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.KEY_BORDER
                title = getString(R.string.pref_key_border)
                entries = arrayOf("None", "Subtle", "Bold")
                entryValues = arrayOf("none", "subtle", "bold")
                setDefaultValue("subtle")
            })

            appearanceCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.KEY_RADIUS
                title = getString(R.string.pref_key_radius)
                entries = arrayOf("Sharp", "Rounded", "Pill")
                entryValues = arrayOf("sharp", "rounded", "pill")
                setDefaultValue("rounded")
            })

            appearanceCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.KEY_TEXT_SIZE
                title = getString(R.string.pref_key_text_size)
                entries = arrayOf("Small", "Medium", "Large")
                entryValues = arrayOf("small", "medium", "large")
                setDefaultValue("medium")
            })

            appearanceCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.NUMBER_ROW
                title = getString(R.string.pref_number_row)
                setDefaultValue(false)
            })

            appearanceCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.THEME
                title = "Theme"
                entries = arrayOf("Dark", "Light", "Auto", "Custom")
                entryValues = arrayOf("dark", "light", "auto", "custom")
                setDefaultValue("dark")
            })

            appearanceCategory.addPreference(Preference(context).apply {
                title = "Customize Theme Colors"
                summary = "Set custom colors for keyboard elements"
                setOnPreferenceClickListener {
                    val intent = android.content.Intent(context, ThemePickerActivity::class.java)
                    startActivity(intent)
                    true
                }
            })

            appearanceCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.HIGH_CONTRAST
                title = "High Contrast Mode"
                summary = "Use high contrast colors for better visibility"
                setDefaultValue(false)
            })

            // ===== ACCESSIBILITY =====
            val accessibilityCategory = PreferenceCategory(context).apply {
                title = "Accessibility"
            }
            screen.addPreference(accessibilityCategory)

            accessibilityCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.VIBRATION_STRENGTH
                title = "Vibration Strength"
                entries = arrayOf("Off", "Light", "Medium", "Strong")
                entryValues = arrayOf("off", "light", "medium", "strong")
                setDefaultValue("medium")
            })

            accessibilityCategory.addPreference(ListPreference(context).apply {
                key = PreferenceKeys.KEY_PRESS_DELAY
                title = "Key Press Delay"
                summary = "Minimum time between key presses (motor accessibility)"
                entries = arrayOf("None", "100ms", "200ms", "500ms")
                entryValues = arrayOf("0", "100", "200", "500")
                setDefaultValue("0")
            })

            accessibilityCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.FLOATING_KEYBOARD
                title = "Floating Keyboard"
                summary = "Allow keyboard to be repositioned on screen"
                setDefaultValue(false)
            })

            // ===== HEBREW =====
            val hebrewCategory = PreferenceCategory(context).apply {
                title = getString(R.string.settings_hebrew)
            }
            screen.addPreference(hebrewCategory)

            hebrewCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.NIKUD
                title = getString(R.string.pref_nikud)
                setDefaultValue(false)
            })

            // ===== FEATURES =====
            val featuresCategory = PreferenceCategory(context).apply {
                title = getString(R.string.settings_features)
            }
            screen.addPreference(featuresCategory)

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.SUGGESTION_BAR
                title = getString(R.string.pref_suggestion_bar)
                setDefaultValue(true)
            })

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.CLIPBOARD_HISTORY
                title = getString(R.string.pref_clipboard_history)
                setDefaultValue(true)
            })

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.EMOJI_KEYBOARD
                title = getString(R.string.pref_emoji_keyboard)
                setDefaultValue(true)
            })

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.VOICE_INPUT
                title = getString(R.string.pref_voice_input)
                setDefaultValue(true)
            })

            // ===== VOICE INPUT CONFIG =====
            val voiceCategory = PreferenceCategory(context).apply {
                title = "Voice Input Configuration"
            }
            screen.addPreference(voiceCategory)

            val secureStorage = SecureStorage.getInstance(context)

            voiceCategory.addPreference(EditTextPreference(context).apply {
                key = "azure_speech_key_display"
                title = "Azure Speech API Key"
                summary = if (secureStorage.getAzureKey().isNotEmpty()) "Key configured" else "Required for voice input"
                dialogTitle = "Enter Azure Speech API Key"
                text = secureStorage.getAzureKey()
                setOnPreferenceChangeListener { _, newValue ->
                    secureStorage.setAzureKey(newValue as String)
                    summary = if (newValue.isNotEmpty()) "Key configured" else "Required for voice input"
                    true
                }
            })

            voiceCategory.addPreference(EditTextPreference(context).apply {
                key = "azure_speech_region_display"
                title = "Azure Speech Region"
                summary = "e.g., westeurope, eastus"
                dialogTitle = "Enter Azure Speech Region"
                text = secureStorage.getAzureRegion()
                setOnPreferenceChangeListener { _, newValue ->
                    secureStorage.setAzureRegion(newValue as String)
                    true
                }
            })

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.VIBRATION
                title = getString(R.string.pref_vibration)
                setDefaultValue(true)
            })

            featuresCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.KEY_SOUND
                title = getString(R.string.pref_key_sound)
                setDefaultValue(false)
            })

            // ===== PASSWORD MANAGER =====
            val passwordCategory = PreferenceCategory(context).apply {
                title = "Password Manager"
            }
            screen.addPreference(passwordCategory)

            passwordCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.AUTOFILL_ENABLED
                title = "Enable Autofill"
                summary = "Offer to save and fill passwords"
                setDefaultValue(true)
            })

            passwordCategory.addPreference(SwitchPreferenceCompat(context).apply {
                key = PreferenceKeys.AUTOFILL_BIOMETRIC
                title = "Require Biometric"
                summary = "Authenticate before showing saved passwords"
                setDefaultValue(true)
            })

            passwordCategory.addPreference(Preference(context).apply {
                title = "Manage Saved Passwords"
                summary = "View, edit, and delete saved credentials"
                setOnPreferenceClickListener {
                    val intent = android.content.Intent(context, PasswordManagerActivity::class.java)
                    startActivity(intent)
                    true
                }
            })

            // ===== PRIVACY =====
            val privacyCategory = PreferenceCategory(context).apply {
                title = getString(R.string.settings_privacy)
            }
            screen.addPreference(privacyCategory)

            privacyCategory.addPreference(Preference(context).apply {
                title = getString(R.string.pref_clear_learned)
                setOnPreferenceClickListener {
                    showClearConfirmation(getString(R.string.pref_clear_learned)) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            MegaLifeDatabase.getInstance(requireContext()).learnedWordDao().clearAll()
                        }
                    }
                    true
                }
            })

            privacyCategory.addPreference(Preference(context).apply {
                title = getString(R.string.pref_clear_clipboard)
                setOnPreferenceClickListener {
                    showClearConfirmation(getString(R.string.pref_clear_clipboard)) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            MegaLifeDatabase.getInstance(requireContext()).clipboardDao().clearAll()
                        }
                    }
                    true
                }
            })

            preferenceScreen = screen
        }

        private fun showClearConfirmation(title: String, onConfirm: () -> Unit) {
            AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(R.string.confirm_clear)
                .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
