package com.megalife.ime.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.json.JSONObject

/**
 * Activity for customizing keyboard theme colors.
 * Allows users to set hex color values for each theme component.
 * Saves the custom theme as JSON to SharedPreferences.
 */
class ThemePickerActivity : AppCompatActivity() {

    private data class ColorField(
        val jsonKey: String,
        val label: String,
        val defaultColor: String
    )

    private val colorFields = listOf(
        ColorField("bgColor", "Background Color", "#1A1A2E"),
        ColorField("keyColor", "Key Color", "#2A2A3E"),
        ColorField("keyPressedColor", "Key Pressed Color", "#3A3A5E"),
        ColorField("keySpecialColor", "Special Key Color", "#252540"),
        ColorField("keyTextColor", "Text Color", "#FFFFFF"),
        ColorField("keyTextSecondary", "Secondary Text Color", "#99FFFFFF"),
        ColorField("accentColor", "Accent Color", "#6C63FF"),
        ColorField("accentCapsLock", "Caps Lock Color", "#FF6B6B"),
        ColorField("focusBorderColor", "Focus Border Color", "#6C63FF"),
        ColorField("suggestionBg", "Suggestion Bar Color", "#222236")
    )

    private val colorInputs = mutableMapOf<String, EditText>()
    private val colorPreviews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val existingJson = prefs.getString(PreferenceKeys.CUSTOM_THEME_JSON, null)
        val existingObj = if (existingJson != null) {
            try { JSONObject(existingJson) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }

        val scrollView = ScrollView(this)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "Customize Theme"
            textSize = 22f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleText)

        // Color fields
        for (field in colorFields) {
            val currentValue = existingObj.optString(field.jsonKey, field.defaultColor)

            val fieldLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val label = TextView(this).apply {
                text = field.label
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            fieldLayout.addView(label)

            val preview = View(this).apply {
                val size = (40 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (12 * resources.displayMetrics.density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6 * resources.displayMetrics.density
                    setStroke((1 * resources.displayMetrics.density).toInt(), Color.GRAY)
                }
                setColorPreview(this, currentValue)
            }
            fieldLayout.addView(preview)
            colorPreviews[field.jsonKey] = preview

            val input = EditText(this).apply {
                setText(currentValue)
                textSize = 13f
                filters = arrayOf(InputFilter.LengthFilter(9))
                layoutParams = LinearLayout.LayoutParams(
                    (120 * resources.displayMetrics.density).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val colorView = colorPreviews[field.jsonKey]
                        if (colorView != null) {
                            setColorPreview(colorView, text.toString())
                        }
                    }
                }
            }
            fieldLayout.addView(input)
            colorInputs[field.jsonKey] = input

            rootLayout.addView(fieldLayout)
        }

        // Button row
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 32, 0, 16)
        }

        val resetButton = Button(this).apply {
            text = "Reset to Default"
            setOnClickListener { resetToDefaults() }
        }
        buttonRow.addView(resetButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (16 * resources.displayMetrics.density).toInt() })

        val saveButton = Button(this).apply {
            text = "Save"
            setOnClickListener { saveTheme() }
        }
        buttonRow.addView(saveButton)

        rootLayout.addView(buttonRow)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
        title = "Theme Customization"
    }

    private fun setColorPreview(view: View, hexColor: String) {
        try {
            val color = Color.parseColor(hexColor)
            val bg = view.background as? GradientDrawable
            bg?.setColor(color)
        } catch (e: Exception) {
            // Invalid color — leave preview unchanged
        }
    }

    private fun resetToDefaults() {
        for (field in colorFields) {
            colorInputs[field.jsonKey]?.setText(field.defaultColor)
            val preview = colorPreviews[field.jsonKey]
            if (preview != null) {
                setColorPreview(preview, field.defaultColor)
            }
        }
    }

    private fun saveTheme() {
        val json = JSONObject()
        for (field in colorFields) {
            val value = colorInputs[field.jsonKey]?.text?.toString() ?: field.defaultColor
            // Validate the color string
            try {
                Color.parseColor(value)
                json.put(field.jsonKey, value)
            } catch (e: Exception) {
                json.put(field.jsonKey, field.defaultColor)
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putString(PreferenceKeys.CUSTOM_THEME_JSON, json.toString())
            .putString(PreferenceKeys.THEME, "custom")
            .apply()

        Toast.makeText(this, "Custom theme saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
