package com.megalife.ime

import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.megalife.ime.core.*
import com.megalife.ime.db.DictionaryLoader
import com.megalife.ime.db.MegaLifeDatabase
import com.megalife.ime.feature.*
import com.megalife.ime.input.InputMode
import com.megalife.ime.input.ShiftState
import com.megalife.ime.language.*
import com.megalife.ime.navigation.DPadNavigator
import com.megalife.ime.settings.IMESettingsActivity
import com.megalife.ime.ui.*
import kotlinx.coroutines.*

class MegaLifeIME : InputMethodService() {

    // Core components
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var inputEngine: InputEngine
    private lateinit var keyRouter: KeyRouter
    private lateinit var dPadNavigator: DPadNavigator
    private lateinit var suggestionManager: SuggestionManager
    private lateinit var undoManager: UndoManager

    // Feature components
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager
    private lateinit var oneHandedController: OneHandedController
    private lateinit var spellChecker: SpellChecker
    private lateinit var shortcutEngine: TextShortcutEngine
    private lateinit var emojiManager: EmojiManager
    private lateinit var wordBlocklist: WordBlocklist
    private var voiceInputManager: VoiceInputManager? = null
    private var glideTypingController: GlideTypingController? = null

    // Database
    private val database: MegaLifeDatabase by lazy {
        MegaLifeDatabase.getInstance(this)
    }

    // Views
    private var keyboardView: View? = null
    private var keyboardContainer: FrameLayout? = null
    private var viewManager: KeyboardViewManager? = null
    private var t9StatusView: T9StatusView? = null
    private var suggestionBarView: SuggestionBarView? = null
    private var popupKeysView: PopupKeysView? = null
    private var keyPreviewView: KeyPreviewView? = null

    // State
    private var currentEditorInfo: EditorInfo? = null
    private var isInPasswordMode = false
    private var cursorMoveMode = false
    private var spaceDownX = 0f

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        loadDictionaries()
    }

    private fun initializeComponents() {
        feedbackManager = FeedbackManager(this)
        undoManager = UndoManager()
        oneHandedController = OneHandedController(this)
        oneHandedController.initialize()
        emojiManager = EmojiManager(this)
        spellChecker = SpellChecker(database.dictionaryDao(), database.learnedWordDao())
        shortcutEngine = TextShortcutEngine(database.shortcutDao(), scope)
        wordBlocklist = WordBlocklist(this)

        clipboardHistoryManager = ClipboardHistoryManager(
            this, database.clipboardDao(), scope
        )
        clipboardHistoryManager.startMonitoring()

        inputEngine = InputEngine(
            context = this,
            feedbackManager = feedbackManager,
            onComposingTextChanged = { text -> updateComposingDisplay(text) },
            onMultiTapStateChanged = { key, index, chars -> updateMultiTapDisplay(key, index, chars) },
            onSuggestionsNeeded = { digits, prefix -> requestSuggestions(digits, prefix) },
            onModeChanged = { mode -> onInputModeChanged(mode) },
            onShiftChanged = { state -> onShiftStateChanged(state) },
            onLanguageChanged = { lang -> onLanguageChanged(lang) }
        )

        // Wire in feature components
        inputEngine.spellChecker = spellChecker
        inputEngine.learnedWordDao = database.learnedWordDao()
        inputEngine.bigramDao = database.bigramDao()
        inputEngine.shortcutEngine = shortcutEngine
        inputEngine.undoManager = undoManager
        inputEngine.onAutoCorrect = { original, corrected ->
            Toast.makeText(this, "Autocorrected: $original → $corrected", Toast.LENGTH_SHORT).show()
        }
        inputEngine.onPredictiveCandidatesChanged = { candidates, _ ->
            suggestionBarView?.updateSuggestions(candidates)
        }
        inputEngine.onSpellCheckResult = { original, suggestions ->
            // Show spell check suggestions in the suggestion bar
            // Tapping a suggestion replaces the misspelled word
            val corrections = suggestions.map { "→$it" }
            suggestionBarView?.updateSuggestions(corrections)
        }
        inputEngine.initialize()

        dPadNavigator = DPadNavigator(
            context = this,
            onCloseKeyboard = { hideWindow() },
            onKeyboardFocusMove = { dir -> viewManager?.moveFocus(dir) },
            onKeyboardFocusSelect = { viewManager?.selectFocusedKey() },
            onKeyboardFocusLongSelect = { viewManager?.longPressFocusedKey() }
        )
        dPadNavigator.initialize()

        keyRouter = KeyRouter(
            inputEngine = inputEngine,
            onToggleMode = { toggleInputMode() },
            onToggleDPadMode = { dPadNavigator.toggleMode() },
            onShowSettings = { openSettings() },
            onDPadEvent = { keyCode, longPress -> dPadNavigator.onDPadEvent(keyCode, longPress) }
        )

        suggestionManager = SuggestionManager(
            dictionaryDao = database.dictionaryDao(),
            learnedWordDao = database.learnedWordDao(),
            bigramDao = database.bigramDao(),
            shortcutDao = database.shortcutDao(),
            scope = scope,
            wordBlocklist = wordBlocklist
        )

        // Voice input (optional — may not have Azure key)
        voiceInputManager = VoiceInputManager(this, scope).also {
            it.onResult = { text -> inputEngine.onClipboardPaste(text) }
            it.onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            it.onListeningChanged = { listening ->
                updateVoiceIcon(listening)
            }
        }

        // Glide typing (English only)
        glideTypingController = GlideTypingController(database.dictionaryDao(), scope)

        popupKeysView = PopupKeysView(this)
        keyPreviewView = KeyPreviewView(this)
    }

    private fun loadDictionaries() {
        scope.launch(Dispatchers.IO) {
            val loader = DictionaryLoader(this@MegaLifeIME, database.dictionaryDao())
            for (lang in LanguageRegistry.allLanguages) {
                try {
                    loader.loadDictionary(lang.dictionaryAsset, lang.code, lang.t9KeyMap)
                } catch (e: Exception) {
                    android.util.Log.w("MegaLifeIME", "Failed to load dictionary: ${lang.code}", e)
                }
            }
            loader.loadDefaultShortcuts(database.shortcutDao())
        }
    }

    // ==================== VIEW CREATION ====================

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_main, null)
        keyboardView = view
        keyboardContainer = view.findViewById(R.id.keyboard_container)

        // Initialize T9 status bar (permanent view, shown/hidden by mode)
        val t9Container = view.findViewById<View>(R.id.t9_status_container)
        if (t9Container != null) {
            t9StatusView = T9StatusView(t9Container)
        }

        // Initialize suggestion bar
        val suggestionList = view.findViewById<RecyclerView>(R.id.suggestion_list)
        suggestionBarView = SuggestionBarView(suggestionList) { word ->
            inputEngine.acceptSuggestion(word)
        }
        // Long-press suggestion to block it
        suggestionBarView?.onSuggestionLongPress = { word ->
            wordBlocklist.blockWord(word)
            Toast.makeText(this, "\"$word\" blocked from suggestions", Toast.LENGTH_SHORT).show()
        }

        // Undo button — deletes the last committed word
        val undoBtn = view.findViewById<TextView>(R.id.btn_undo)
        undoBtn?.setOnClickListener {
            val state = undoManager.undo()
            if (state != null) {
                // Delete the word that was just committed
                val ic = currentInputConnection
                ic?.deleteSurroundingText(state.text.length + if (state.hasAutoSpace) 1 else 0, 0)
                feedbackManager.onSpecialKeyPress()
            }
        }
        undoBtn?.visibility = View.VISIBLE

        // Redo button — re-inserts the undone word
        val redoBtn = view.findViewById<TextView>(R.id.btn_redo)
        redoBtn?.setOnClickListener {
            val state = undoManager.redo()
            if (state != null) {
                val ic = currentInputConnection
                ic?.commitText(state.text + " ", 1)
                feedbackManager.onSpecialKeyPress()
            }
        }
        redoBtn?.visibility = View.VISIBLE

        // Clipboard button
        val clipboardBtn = view.findViewById<ImageView>(R.id.btn_clipboard)
        clipboardBtn?.setOnClickListener { showClipboardPanel() }

        // Voice button
        val voiceBtn = view.findViewById<ImageView>(R.id.btn_voice)
        voiceBtn?.setOnClickListener { toggleVoiceInput() }
        voiceBtn?.visibility = View.VISIBLE

        // One-handed mode button
        val oneHandedBtn = view.findViewById<TextView>(R.id.btn_one_handed)
        oneHandedBtn?.setOnClickListener {
            val anchor = oneHandedController.toggle()
            applyOneHandedMode()
            oneHandedBtn.text = when (anchor) {
                OneHandedController.Anchor.LEFT -> "[<]"
                OneHandedController.Anchor.RIGHT -> "[>]"
                OneHandedController.Anchor.NONE -> "[ ]"
            }
        }

        // Drag handle for keyboard height adjustment
        val dragHandle = view.findViewById<View>(R.id.drag_handle)
        setupDragHandle(dragHandle)

        // Initialize KeyboardViewManager
        val container = keyboardContainer ?: return view
        viewManager = KeyboardViewManager(
            context = this,
            container = container,
            onKeyPress = { keyDef -> handleTouchKeyPress(keyDef) },
            onKeyLongPress = { keyDef -> handleTouchKeyLongPress(keyDef) },
            onEmojiSelected = { emoji ->
                emojiManager.recordUsage(emoji)
                inputEngine.onEmojiInput(emoji)
            },
            onClipboardPaste = { text -> inputEngine.onClipboardPaste(text) },
            onNikudSelected = { nikud -> inputEngine.onNikudInput(nikud) },
            onGlideWord = { words ->
                // Glide typing results → show as suggestions
                suggestionBarView?.updateSuggestions(words)
            },
            onCursorMove = { direction ->
                // Space long-press cursor movement
                val ic = currentInputConnection ?: return@KeyboardViewManager
                val keyCode = if (direction > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                feedbackManager.onKeyPress()
            },
            onSwipeBackspace = {
                // Swipe left on backspace → delete whole word
                inputEngine.onBackspaceLongPress()
                feedbackManager.onSpecialKeyPress()
            },
            onDismissKeyboard = {
                // Swipe down gesture → hide keyboard
                requestHideSelf(0)
            },
            onOpenSettings = {
                // Swipe up from bottom row → open settings
                val intent = Intent(this@MegaLifeIME, IMESettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        )
        viewManager?.glideController = glideTypingController
        viewManager?.keyPreviewView = keyPreviewView

        // Show the appropriate keyboard
        showKeyboardForCurrentMode()

        return view
    }

    private fun showKeyboardForCurrentMode() {
        val isT9Mode = inputEngine.inputMode == InputMode.MultiTap ||
                inputEngine.inputMode == InputMode.T9Predictive

        // Show/hide T9 status bar
        val t9Container = keyboardView?.findViewById<View>(R.id.t9_status_container)
        t9Container?.visibility = if (isT9Mode) View.VISIBLE else View.GONE

        if (isT9Mode) {
            t9StatusView?.updateMode(inputEngine.inputMode)
            t9StatusView?.updateLanguage(inputEngine.currentLanguage)
            // T9 modes: keyboard container is empty (using physical keys)
            // But keep the container — don't remove views unnecessarily
            keyboardContainer?.removeAllViews()
        }

        when (inputEngine.inputMode) {
            InputMode.MultiTap, InputMode.T9Predictive -> {
                // Physical keypad input — no touch keyboard needed
                // T9 status bar is shown above
            }
            InputMode.DirectInput -> {
                viewManager?.showLetters(inputEngine.currentLanguage)
                applyOneHandedMode()
            }
            InputMode.Numeric -> {
                val editorInfo = currentEditorInfo
                val inputType = editorInfo?.inputType?.and(InputType.TYPE_MASK_CLASS) ?: 0
                if (inputType == InputType.TYPE_CLASS_PHONE) {
                    viewManager?.showPhonePad()
                } else {
                    viewManager?.showNumberPad()
                }
            }
            InputMode.Passthrough -> {}
        }
    }

    // ==================== KEY HANDLING ====================

    private fun handleTouchKeyPress(keyDef: KeyDef) {
        when (keyDef.type) {
            KeyType.CHARACTER -> {
                inputEngine.onCharacterInput(keyDef.primaryChar)
            }
            KeyType.SHIFT -> inputEngine.toggleShift()
            KeyType.BACKSPACE -> inputEngine.onBackspace()
            KeyType.ENTER -> {
                inputEngine.onEnter()
                sendDefaultEditorAction(true)
            }
            KeyType.SPACE -> inputEngine.onSpace()
            KeyType.LANGUAGE -> inputEngine.nextLanguage()
            KeyType.SYMBOLS -> {
                if (viewManager?.currentPanel == KeyboardViewManager.Panel.SYMBOLS_1 ||
                    viewManager?.currentPanel == KeyboardViewManager.Panel.SYMBOLS_2
                ) {
                    viewManager?.showLetters(inputEngine.currentLanguage)
                } else {
                    viewManager?.showSymbols1()
                }
            }
            KeyType.EMOJI -> viewManager?.showEmoji()
            KeyType.PERIOD -> inputEngine.onPunctuation(keyDef.primaryChar)
            KeyType.COMMA -> inputEngine.onPunctuation(",")
        }
    }

    private fun handleTouchKeyLongPress(keyDef: KeyDef) {
        when (keyDef.type) {
            KeyType.BACKSPACE -> inputEngine.onBackspaceLongPress()
            KeyType.SHIFT -> inputEngine.doubleTapShift()
            KeyType.LANGUAGE -> {
                // Long-press language → show input method picker (switch keyboards)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            KeyType.SPACE -> {
                // Long press space → cursor move mode (handled in TouchKeyboardView)
                // The toast is shown here; actual slide movement handled by onCursorMove callback
                feedbackManager.onSpecialKeyPress()
            }
            KeyType.CHARACTER -> {
                if (keyDef.popupChars.isNotEmpty()) {
                    showPopupKeys(keyDef)
                }
            }
            KeyType.PERIOD -> {
                // Long-press period shows nikud panel if Hebrew
                if (inputEngine.currentLanguage.scriptType == ScriptType.HEBREW) {
                    viewManager?.showNikud()
                }
            }
            else -> {}
        }
    }

    private fun showPopupKeys(keyDef: KeyDef) {
        val tkv = viewManager?.getTouchKeyboardView() ?: return
        val keyRect = tkv.keyRects.find { it.keyDef == keyDef } ?: return

        popupKeysView?.onCharSelected = { char ->
            inputEngine.onCharacterInput(char)
        }
        popupKeysView?.show(tkv, keyDef.popupChars, keyRect.rect)
    }

    private fun applyOneHandedMode() {
        val tkv = viewManager?.getTouchKeyboardView() ?: return
        oneHandedController.applyToView(tkv)
    }

    // ==================== INPUT LIFECYCLE ====================

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentEditorInfo = attribute
        inputEngine.setInputConnection(currentInputConnection)
        inputEngine.resetForNewField()

        isInPasswordMode = false

        when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> {
                inputEngine.setNumericMode()
            }
            InputType.TYPE_CLASS_PHONE -> {
                inputEngine.setNumericMode()
            }
            InputType.TYPE_CLASS_TEXT -> {
                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                when (variation) {
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                        isInPasswordMode = true
                        inputEngine.isPasswordMode = true
                        suggestionBarView?.setVisible(false)
                    }
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> {
                        suggestionBarView?.setVisible(true)
                        // Email mode — direct input preferred
                        if (inputEngine.inputMode == InputMode.MultiTap) {
                            inputEngine.setDirectMode()
                        }
                    }
                    InputType.TYPE_TEXT_VARIATION_URI -> {
                        suggestionBarView?.setVisible(false)
                        if (inputEngine.inputMode == InputMode.MultiTap) {
                            inputEngine.setDirectMode()
                        }
                    }
                    else -> {
                        suggestionBarView?.setVisible(true)
                    }
                }
            }
        }

        // Update enter key label based on IME action
        updateEnterKeyLabel(attribute)
    }

    /** Never go fullscreen — always show the app content above the keyboard */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateExtractTextView(): View? = null

    override fun onUpdateExtractingVisibility(ei: EditorInfo) {
        setExtractViewShown(false)
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        // Tell Android the keyboard only covers from the top of our view down
        // This lets the app resize to fill the space above
        val view = keyboardView
        if (view != null) {
            val loc = IntArray(2)
            view.getLocationInWindow(loc)
            outInsets.visibleTopInsets = loc[1]
            outInsets.contentTopInsets = loc[1]
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
            outInsets.touchableRegion.setEmpty()
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputEngine.setInputConnection(currentInputConnection)

        // Re-check password mode in case onStartInput didn't fire
        if (info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            val variation = info.inputType and InputType.TYPE_MASK_VARIATION
            val isPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            isInPasswordMode = isPassword
            inputEngine.isPasswordMode = isPassword
            suggestionBarView?.setVisible(!isPassword)
        }

        // Reload prefs in case user changed settings
        viewManager?.getTouchKeyboardView()?.reloadPreferences()
        feedbackManager.reloadPrefs()
        showKeyboardForCurrentMode()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Landscape/portrait rotation — recreate the keyboard view
        // The system calls onCreateInputView again after this
    }

    override fun onFinishInput() {
        super.onFinishInput()
        inputEngine.commitComposingWord()
        popupKeysView?.dismiss()
        keyPreviewView?.dismiss()
    }

    private fun updateEnterKeyLabel(info: EditorInfo) {
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val label = when (action) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "\uD83D\uDD0D" // magnifying glass
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_PREVIOUS -> "Prev"
            else -> "↵"
        }
        viewManager?.setEnterLabel(label)
    }

    // ==================== KEY EVENT HANDLING ====================

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Volume down = undo (when keyboard is visible)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && isInputViewShown) {
            val op = undoManager.undo()
            if (op != null) {
                Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show()
                return true
            }
        }

        if (keyRouter.onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Consume * and # on key up too, so the app doesn't get them
        if (keyCode == KeyEvent.KEYCODE_STAR || keyCode == KeyEvent.KEYCODE_POUND) {
            return true
        }
        // Consume number keys in T9 mode
        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 &&
            (inputEngine.inputMode == InputMode.MultiTap ||
             inputEngine.inputMode == InputMode.T9Predictive)
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // ==================== CALLBACKS ====================

    private fun updateComposingDisplay(text: String) {
        t9StatusView?.updateComposingText(text)
    }

    private fun updateMultiTapDisplay(key: Int, charIndex: Int, chars: List<Char>) {
        t9StatusView?.updateLetterOptions(key, charIndex, chars)
    }

    private fun requestSuggestions(digitSequence: String, wordPrefix: String) {
        if (isInPasswordMode) return

        val lang = inputEngine.currentLanguage.code
        val previousWord = inputEngine.lastCommittedWord
        val isT9 = inputEngine.inputMode == InputMode.MultiTap ||
                    inputEngine.inputMode == InputMode.T9Predictive

        val maxResults = if (inputEngine.inputMode == InputMode.T9Predictive) 10 else 3

        suggestionManager.requestSuggestions(
            language = lang,
            digitSequence = digitSequence,
            wordPrefix = wordPrefix,
            previousWord = previousWord,
            isT9Mode = isT9,
            maxResults = maxResults
        ) { suggestions ->
            // In T9 Predictive mode, feed candidates back to InputEngine
            if (inputEngine.inputMode == InputMode.T9Predictive) {
                inputEngine.updatePredictiveCandidates(suggestions)
            } else {
                suggestionBarView?.updateSuggestions(suggestions)
            }
        }
    }

    private fun onInputModeChanged(mode: InputMode) {
        showKeyboardForCurrentMode()
        t9StatusView?.updateMode(mode)

        // Update mode badge on touch keyboard
        val badge = when (mode) {
            InputMode.DirectInput -> "ABC"
            InputMode.MultiTap -> "T9"
            InputMode.T9Predictive -> "T9+"
            InputMode.Numeric -> "123"
            InputMode.Passthrough -> ""
        }
        viewManager?.setModeBadge(badge)
        feedbackManager.onSpecialKeyPress()
    }

    private fun onShiftStateChanged(state: ShiftState) {
        viewManager?.updateShiftState(state)
        t9StatusView?.updateCapsIndicator(state)
    }

    private fun onLanguageChanged(lang: LanguageConfig) {
        t9StatusView?.updateLanguage(lang)
        // Only rebuild keyboard if we're in direct input (touch keyboard needs language switch)
        // T9 modes just update the status bar
        if (inputEngine.inputMode == InputMode.DirectInput) {
            viewManager?.setLanguage(lang)
        }
        feedbackManager.onSpecialKeyPress()
    }

    private fun toggleInputMode() {
        inputEngine.toggleMode()
    }

    // ==================== FEATURE ACTIONS ====================

    private fun showClipboardPanel() {
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                clipboardHistoryManager.getItems()
            }
            viewManager?.showClipboard(items) { panel ->
                // Wire pin/delete actions
                panel.onPin = { id ->
                    scope.launch(Dispatchers.IO) {
                        clipboardHistoryManager.pinItem(id)
                        val refreshed = clipboardHistoryManager.getItems()
                        withContext(Dispatchers.Main) { panel.setItems(refreshed) }
                    }
                }
                panel.onDelete = { id ->
                    scope.launch(Dispatchers.IO) {
                        clipboardHistoryManager.deleteItem(id)
                        val refreshed = clipboardHistoryManager.getItems()
                        withContext(Dispatchers.Main) { panel.setItems(refreshed) }
                    }
                }
            }
        }
    }

    private fun toggleVoiceInput() {
        val vim = voiceInputManager ?: return
        if (vim.isCurrentlyListening()) {
            vim.stopListening()
        } else {
            if (!vim.hasPermission()) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                return
            }
            vim.startListening(inputEngine.currentLanguage.code)
        }
    }

    private fun updateVoiceIcon(listening: Boolean) {
        val voiceBtn = keyboardView?.findViewById<ImageView>(R.id.btn_voice)
        voiceBtn?.alpha = if (listening) 1.0f else 0.6f
    }

    // ==================== SETTINGS ====================

    private fun openSettings() {
        val intent = Intent(this, IMESettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun setupDragHandle(handle: View?) {
        handle ?: return
        var initialY = 0f
        var initialHeight = 0
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialHeight = keyboardContainer?.height ?: 0
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val delta = (initialY - event.rawY).toInt()
                    val density = resources.displayMetrics.density
                    val minHeight = (100 * density).toInt()
                    val maxHeight = (400 * density).toInt()
                    val newHeight = (initialHeight + delta).coerceIn(minHeight, maxHeight)
                    keyboardContainer?.layoutParams?.height = newHeight
                    keyboardContainer?.requestLayout()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inputEngine.destroy()
        voiceInputManager?.destroy()
        clipboardHistoryManager.stopMonitoring()
        popupKeysView?.dismiss()
        keyPreviewView?.dismiss()
        scope.cancel()
    }
}
