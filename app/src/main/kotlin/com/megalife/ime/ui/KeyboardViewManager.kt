package com.megalife.ime.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import com.megalife.ime.feature.GlideTypingController
import com.megalife.ime.input.ShiftState
import com.megalife.ime.language.KeyDef
import com.megalife.ime.language.LanguageConfig
import com.megalife.ime.language.SymbolLayouts
import com.megalife.ime.settings.IMESettingsActivity

/**
 * Manages switching between different keyboard views:
 * - Touch keyboard (letters) with glide typing overlay
 * - Symbol pages (1 & 2)
 * - Number pad / Phone pad
 * - Emoji panel
 * - Clipboard panel
 * - Nikud panel
 */
class KeyboardViewManager(
    private val context: Context,
    private val container: FrameLayout,
    private val onKeyPress: (KeyDef) -> Unit,
    private val onKeyLongPress: (KeyDef) -> Unit,
    private val onEmojiSelected: (String) -> Unit,
    private val onClipboardPaste: (String) -> Unit,
    private val onNikudSelected: (Char) -> Unit,
    private val onGlideWord: (List<String>) -> Unit = {},
    private val onCursorMove: (Int) -> Unit = {},
    private val onSwipeBackspace: () -> Unit = {},
    private val onDismissKeyboard: () -> Unit = {},
    private val onOpenSettings: () -> Unit = {}
) {
    enum class Panel {
        LETTERS, SYMBOLS_1, SYMBOLS_2, NUMBER_PAD, PHONE_PAD, EMOJI, CLIPBOARD, NIKUD
    }

    var currentPanel: Panel = Panel.LETTERS
        private set

    private var touchKeyboardView: TouchKeyboardView? = null
    private var glideTrailView: GlideTrailView? = null
    private var emojiKeyboardView: EmojiKeyboardView? = null
    private var clipboardPanelView: ClipboardPanelView? = null
    private var nikudPanelView: NikudPanelView? = null
    private var currentLanguage: LanguageConfig? = null
    private var currentShiftState: ShiftState = ShiftState.LOWER
    private var currentEnterLabel: String = "↵"

    // Glide controller and key preview (set by MegaLifeIME)
    var glideController: GlideTypingController? = null
    var keyPreviewView: KeyPreviewView? = null

    fun showLetters(language: LanguageConfig) {
        currentLanguage = language
        currentPanel = Panel.LETTERS
        container.removeAllViews()

        val tkv = TouchKeyboardView(context, language)
        tkv.onKeyPress = onKeyPress
        tkv.onKeyLongPress = onKeyLongPress
        tkv.onGlideWord = onGlideWord
        tkv.onCursorMove = onCursorMove
        tkv.onSwipeBackspace = onSwipeBackspace
        tkv.onDismissKeyboard = onDismissKeyboard
        tkv.onOpenSettings = onOpenSettings
        tkv.updateShiftState(currentShiftState)
        tkv.setEnterLabel(currentEnterLabel)

        // Wire key preview
        tkv.keyPreviewView = keyPreviewView

        // Wire glide typing
        if (language.hasGlideSupport) {
            tkv.glideController = glideController
            val trail = GlideTrailView(context)
            glideTrailView = trail
            tkv.glideTrailView = trail
            container.addView(tkv)
            // Overlay trail view on top
            container.addView(trail, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            trail.isClickable = false
            trail.isFocusable = false
        } else {
            container.addView(tkv)
        }

        touchKeyboardView = tkv
    }

    fun showSymbols1() {
        currentPanel = Panel.SYMBOLS_1
        container.removeAllViews()

        val lang = currentLanguage ?: return
        val symbolConfig = lang.copy(touchLayout = SymbolLayouts.symbolsPage1)
        val tkv = TouchKeyboardView(context, symbolConfig)
        tkv.onKeyPress = { keyDef ->
            when {
                keyDef.type == com.megalife.ime.language.KeyType.SYMBOLS && keyDef.primaryChar == "1/2" -> showSymbols2()
                keyDef.type == com.megalife.ime.language.KeyType.SYMBOLS && keyDef.primaryChar == "ABC" -> showLetters(currentLanguage!!)
                else -> onKeyPress(keyDef)
            }
        }
        tkv.onKeyLongPress = onKeyLongPress
        tkv.setEnterLabel(currentEnterLabel)
        touchKeyboardView = tkv
        container.addView(tkv)
    }

    fun showSymbols2() {
        currentPanel = Panel.SYMBOLS_2
        container.removeAllViews()

        val lang = currentLanguage ?: return
        val symbolConfig = lang.copy(touchLayout = SymbolLayouts.symbolsPage2)
        val tkv = TouchKeyboardView(context, symbolConfig)
        tkv.onKeyPress = { keyDef ->
            when {
                keyDef.type == com.megalife.ime.language.KeyType.SYMBOLS && keyDef.primaryChar == "2/2" -> showSymbols1()
                keyDef.type == com.megalife.ime.language.KeyType.SYMBOLS && keyDef.primaryChar == "ABC" -> showLetters(currentLanguage!!)
                else -> onKeyPress(keyDef)
            }
        }
        tkv.onKeyLongPress = onKeyLongPress
        tkv.setEnterLabel(currentEnterLabel)
        touchKeyboardView = tkv
        container.addView(tkv)
    }

    fun showNumberPad() {
        currentPanel = Panel.NUMBER_PAD
        container.removeAllViews()

        val lang = currentLanguage ?: return
        val numConfig = lang.copy(touchLayout = SymbolLayouts.numberPad)
        val tkv = TouchKeyboardView(context, numConfig)
        tkv.onKeyPress = onKeyPress
        tkv.onKeyLongPress = onKeyLongPress
        touchKeyboardView = tkv
        container.addView(tkv)
    }

    fun showPhonePad() {
        currentPanel = Panel.PHONE_PAD
        container.removeAllViews()

        val lang = currentLanguage ?: return
        val phoneConfig = lang.copy(touchLayout = SymbolLayouts.phonePad)
        val tkv = TouchKeyboardView(context, phoneConfig)
        tkv.onKeyPress = onKeyPress
        tkv.onKeyLongPress = onKeyLongPress
        touchKeyboardView = tkv
        container.addView(tkv)
    }

    fun showEmoji() {
        currentPanel = Panel.EMOJI
        container.removeAllViews()

        val view = EmojiKeyboardView(context)
        view.onEmojiSelected = onEmojiSelected
        view.onBackToLetters = { showLetters(currentLanguage!!) }
        emojiKeyboardView = view
        container.addView(view)
    }

    fun showClipboard(
        items: List<com.megalife.ime.db.entity.ClipboardItem>,
        onSetup: ((ClipboardPanelView) -> Unit)? = null
    ) {
        currentPanel = Panel.CLIPBOARD
        container.removeAllViews()

        val view = ClipboardPanelView(context)
        view.onPaste = onClipboardPaste
        view.onClose = { showLetters(currentLanguage!!) }
        view.setItems(items)
        onSetup?.invoke(view) // Allow caller to wire pin/delete
        clipboardPanelView = view
        container.addView(view)
    }

    fun showNikud() {
        currentPanel = Panel.NIKUD
        container.removeAllViews()

        val view = NikudPanelView(context)
        view.onNikudSelected = onNikudSelected
        view.onClose = { showLetters(currentLanguage!!) }
        nikudPanelView = view
        container.addView(view)
    }

    fun updateShiftState(state: ShiftState) {
        currentShiftState = state
        touchKeyboardView?.updateShiftState(state)
    }

    fun setEnterLabel(label: String) {
        currentEnterLabel = label
        touchKeyboardView?.setEnterLabel(label)
    }

    fun setModeBadge(badge: String) {
        touchKeyboardView?.setModeBadge(badge)
    }

    fun setLanguage(language: LanguageConfig) {
        currentLanguage = language
        if (currentPanel == Panel.LETTERS) {
            showLetters(language)
        }
    }

    fun getTouchKeyboardView(): TouchKeyboardView? = touchKeyboardView

    fun moveFocus(direction: Int) = touchKeyboardView?.moveFocus(direction)
    fun selectFocusedKey() = touchKeyboardView?.selectFocusedKey()
    fun longPressFocusedKey() = touchKeyboardView?.longPressFocusedKey()
}
