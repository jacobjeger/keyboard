package com.megalife.ime.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.preference.PreferenceManager
import com.megalife.ime.settings.PreferenceKeys
import org.json.JSONObject

/**
 * Theme for keyboard visual design. Supports dark and light themes.
 * Reads preferences for customizable dimensions and active theme.
 */
object KeyboardTheme {

    // ==================== DARK THEME ====================
    const val BG_COLOR = 0xFF1A1A2E.toInt()
    const val KEY_COLOR = 0xFF2A2A3E.toInt()
    const val KEY_PRESSED_COLOR = 0xFF3A3A5E.toInt()
    const val KEY_SPECIAL_COLOR = 0xFF252540.toInt()
    const val KEY_TEXT_COLOR = Color.WHITE
    const val KEY_TEXT_SECONDARY = 0x99FFFFFF.toInt()
    const val ACCENT_COLOR = 0xFF6C63FF.toInt()
    const val ACCENT_CAPS_LOCK = 0xFFFF6B6B.toInt()
    const val FOCUS_BORDER_COLOR = 0xFF6C63FF.toInt()
    const val SUGGESTION_BG = 0xFF222236.toInt()

    // ==================== LIGHT THEME ====================
    const val LIGHT_BG_COLOR = 0xFFECEFF1.toInt()
    const val LIGHT_KEY_COLOR = 0xFFFFFFFF.toInt()
    const val LIGHT_KEY_PRESSED_COLOR = 0xFFD6D6D6.toInt()
    const val LIGHT_KEY_SPECIAL_COLOR = 0xFFB0BEC5.toInt()
    const val LIGHT_KEY_TEXT_COLOR = 0xFF212121.toInt()
    const val LIGHT_KEY_TEXT_SECONDARY = 0x99000000.toInt()
    const val LIGHT_SUGGESTION_BG = 0xFFE0E0E0.toInt()
    const val LIGHT_ACCENT_COLOR = 0xFF5C54E0.toInt()

    // ==================== DIMENSIONS ====================
    const val KEY_SPACING_DP = 3f
    const val KEY_CORNER_RADIUS_DP = 4f
    const val KEY_TEXT_SIZE_SP = 20f
    const val KEY_SUB_TEXT_SIZE_SP = 11f
    const val FOCUS_BORDER_WIDTH_DP = 2f

    const val BORDER_NONE = 0f
    const val BORDER_SUBTLE = 0.5f
    const val BORDER_BOLD = 1.5f

    // ==================== THEME STATE ====================

    data class ThemeColors(
        val bgColor: Int,
        val keyColor: Int,
        val keyPressedColor: Int,
        val keySpecialColor: Int,
        val keyTextColor: Int,
        val keyTextSecondary: Int,
        val accentColor: Int,
        val accentCapsLock: Int,
        val focusBorderColor: Int,
        val suggestionBg: Int
    )

    val darkTheme = ThemeColors(
        bgColor = BG_COLOR,
        keyColor = KEY_COLOR,
        keyPressedColor = KEY_PRESSED_COLOR,
        keySpecialColor = KEY_SPECIAL_COLOR,
        keyTextColor = KEY_TEXT_COLOR,
        keyTextSecondary = KEY_TEXT_SECONDARY,
        accentColor = ACCENT_COLOR,
        accentCapsLock = ACCENT_CAPS_LOCK,
        focusBorderColor = FOCUS_BORDER_COLOR,
        suggestionBg = SUGGESTION_BG
    )

    val lightTheme = ThemeColors(
        bgColor = LIGHT_BG_COLOR,
        keyColor = LIGHT_KEY_COLOR,
        keyPressedColor = LIGHT_KEY_PRESSED_COLOR,
        keySpecialColor = LIGHT_KEY_SPECIAL_COLOR,
        keyTextColor = LIGHT_KEY_TEXT_COLOR,
        keyTextSecondary = LIGHT_KEY_TEXT_SECONDARY,
        accentColor = LIGHT_ACCENT_COLOR,
        accentCapsLock = ACCENT_CAPS_LOCK,
        focusBorderColor = LIGHT_ACCENT_COLOR,
        suggestionBg = LIGHT_SUGGESTION_BG
    )

    val highContrastTheme = ThemeColors(
        bgColor = Color.BLACK,
        keyColor = Color.parseColor("#1A1A1A"),
        keyPressedColor = Color.parseColor("#333333"),
        keySpecialColor = Color.parseColor("#111111"),
        keyTextColor = Color.WHITE,
        keyTextSecondary = Color.parseColor("#CCCCCC"),
        accentColor = Color.parseColor("#FFFF00"),
        accentCapsLock = Color.parseColor("#FF0000"),
        focusBorderColor = Color.parseColor("#FFFF00"),
        suggestionBg = Color.parseColor("#0D0D0D")
    )

    fun getTheme(context: Context): ThemeColors {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // High contrast overrides everything
        val highContrast = prefs.getBoolean(PreferenceKeys.HIGH_CONTRAST, false)
        if (highContrast) return highContrastTheme

        return when (prefs.getString(PreferenceKeys.THEME, "dark")) {
            "light" -> lightTheme
            "custom" -> loadCustomTheme(context)
            "auto" -> {
                val nightMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) darkTheme
                else lightTheme
            }
            else -> darkTheme
        }
    }

    private fun loadCustomTheme(context: Context): ThemeColors {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val json = prefs.getString(PreferenceKeys.CUSTOM_THEME_JSON, null)
        if (json == null) return darkTheme
        return try {
            val obj = JSONObject(json)
            ThemeColors(
                bgColor = Color.parseColor(obj.optString("bgColor", "#1A1A2E")),
                keyColor = Color.parseColor(obj.optString("keyColor", "#2A2A3E")),
                keyPressedColor = Color.parseColor(obj.optString("keyPressedColor", "#3A3A5E")),
                keySpecialColor = Color.parseColor(obj.optString("keySpecialColor", "#252540")),
                keyTextColor = Color.parseColor(obj.optString("keyTextColor", "#FFFFFF")),
                keyTextSecondary = Color.parseColor(obj.optString("keyTextSecondary", "#99FFFFFF")),
                accentColor = Color.parseColor(obj.optString("accentColor", "#6C63FF")),
                accentCapsLock = Color.parseColor(obj.optString("accentCapsLock", "#FF6B6B")),
                focusBorderColor = Color.parseColor(obj.optString("focusBorderColor", "#6C63FF")),
                suggestionBg = Color.parseColor(obj.optString("suggestionBg", "#222236"))
            )
        } catch (e: Exception) {
            darkTheme
        }
    }

    // ==================== PREFERENCE READERS ====================

    fun getKeyHeightDp(context: Context): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString(PreferenceKeys.KEY_HEIGHT, "medium")) {
            "small" -> 40f
            "large" -> 56f
            else -> 48f
        }
    }

    fun getKeyRadiusDp(context: Context): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString(PreferenceKeys.KEY_RADIUS, "rounded")) {
            "sharp" -> 0f
            "rounded" -> 4f
            "pill" -> 12f
            else -> 4f
        }
    }

    fun getKeyTextSizeSp(context: Context): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString(PreferenceKeys.KEY_TEXT_SIZE, "medium")) {
            "small" -> 16f
            "large" -> 24f
            else -> 20f
        }
    }

    fun getKeyBorderDp(context: Context): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return when (prefs.getString(PreferenceKeys.KEY_BORDER, "none")) {
            "subtle" -> BORDER_SUBTLE
            "bold" -> BORDER_BOLD
            else -> BORDER_NONE
        }
    }

    fun isNumberRowEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PreferenceKeys.NUMBER_ROW, false)
    }

    // ==================== PAINT FACTORIES ====================

    fun createKeyPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_COLOR
        style = Paint.Style.FILL
    }

    fun createKeyTextPaint(textSizePx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_TEXT_COLOR
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    fun createSubTextPaint(textSizePx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = KEY_TEXT_SECONDARY
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    fun createFocusPaint(borderWidthPx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = FOCUS_BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = borderWidthPx
    }

    fun createKeyBorderPaint(borderWidthPx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = borderWidthPx
    }

    // ==================== THEME-AWARE PAINT FACTORIES ====================

    fun createKeyPaint(theme: ThemeColors): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keyColor
        style = Paint.Style.FILL
    }

    fun createKeyTextPaint(theme: ThemeColors, textSizePx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keyTextColor
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    fun createSubTextPaint(theme: ThemeColors, textSizePx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keyTextSecondary
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    fun createAccentTextPaint(theme: ThemeColors, textSizePx: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentColor
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    fun createPressedPaint(theme: ThemeColors): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keyPressedColor
        style = Paint.Style.FILL
    }

    fun createSpecialPaint(theme: ThemeColors): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keySpecialColor
        style = Paint.Style.FILL
    }
}
