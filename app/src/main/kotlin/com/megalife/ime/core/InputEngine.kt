package com.megalife.ime.core

import android.content.Context
import android.view.inputmethod.InputConnection
import androidx.preference.PreferenceManager
import com.megalife.ime.core.InputConnectionHelper.charBeforeCursor
import com.megalife.ime.core.InputConnectionHelper.deleteWordBackward
import com.megalife.ime.core.InputConnectionHelper.isStartOfSentence
import com.megalife.ime.core.InputConnectionHelper.safeCommitText
import com.megalife.ime.core.InputConnectionHelper.safeDeleteSurrounding
import com.megalife.ime.core.InputConnectionHelper.safeFinishComposing
import com.megalife.ime.core.InputConnectionHelper.safeSetComposing
import com.megalife.ime.core.InputConnectionHelper.insertDirectionMarker
import com.megalife.ime.db.dao.BigramDao
import com.megalife.ime.db.dao.LearnedWordDao
import com.megalife.ime.feature.SpellChecker
import com.megalife.ime.feature.TextShortcutEngine
import com.megalife.ime.input.*
import com.megalife.ime.language.LanguageConfig
import com.megalife.ime.language.LanguageRegistry
import com.megalife.ime.language.ScriptType
import com.megalife.ime.language.TextDirection
import kotlinx.coroutines.*

/**
 * Core input engine. Manages:
 * - Current input mode (T9 multi-tap, direct input, numeric)
 * - Multi-tap state and timing
 * - Composing text (the word being built)
 * - Shift state
 * - Smart punctuation (double-space = period + space)
 * - Auto-capitalization
 * - Autocorrect on word commit (Latin only)
 * - Word learning (records typed words)
 * - Bigram recording (tracks word pairs)
 * - Hebrew final forms replacement
 * - Text shortcut expansion
 */
class InputEngine(
    private val context: Context,
    private val feedbackManager: FeedbackManager,
    private val onComposingTextChanged: (String) -> Unit,
    private val onMultiTapStateChanged: (key: Int, charIndex: Int, chars: List<Char>) -> Unit,
    private val onSuggestionsNeeded: (digitSequence: String, wordPrefix: String) -> Unit,
    private val onModeChanged: (InputMode) -> Unit,
    private val onShiftChanged: (ShiftState) -> Unit,
    private val onLanguageChanged: (LanguageConfig) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    var inputMode: InputMode = InputMode.MultiTap
        private set

    var shiftState: ShiftState = ShiftState.LOWER
        private set

    val currentLanguage: LanguageConfig
        get() = LanguageRegistry.currentLanguage

    // Multi-tap state
    private val multiTapState = MultiTapState()
    private lateinit var multiTapTimer: MultiTapTimer

    // T9 Predictive state
    private var predictiveCandidates: List<String> = emptyList()
    private var predictiveCandidateIndex: Int = 0

    // Callback for when predictive candidates change (UI should show them)
    var onPredictiveCandidatesChanged: ((candidates: List<String>, selectedIndex: Int) -> Unit)? = null

    // Composing text (the current word being built)
    private val composingBuffer = StringBuilder()

    // T9 digit sequence for predictive lookup
    private val digitSequence = StringBuilder()

    // Last committed word (for bigram prediction)
    var lastCommittedWord: String = ""
        private set

    // Smart punctuation: last space timestamp
    private var lastSpaceTime: Long = 0L

    // Caps lock reminder
    private var capsLockReminderJob: Job? = null

    private var inputConnection: InputConnection? = null

    // Optional components (set by MegaLifeIME after init)
    var spellChecker: SpellChecker? = null
    var learnedWordDao: LearnedWordDao? = null
    var bigramDao: BigramDao? = null
    var shortcutEngine: TextShortcutEngine? = null
    var undoManager: UndoManager? = null

    // Password mode flag — disables learning and autocorrect
    var isPasswordMode: Boolean = false

    // Callback for autocorrect notification
    var onAutoCorrect: ((original: String, corrected: String) -> Unit)? = null

    // Callback for spell check suggestions (word was committed but may be misspelled)
    var onSpellCheckResult: ((original: String, suggestions: List<String>) -> Unit)? = null

    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }

    fun initialize() {
        val timeoutMs = prefs.getString("pref_t9_timeout", "800")?.toLongOrNull() ?: 800L
        multiTapTimer = MultiTapTimer(scope, timeoutMs) {
            commitMultiTapChar()
        }

        // Restore mode from prefs
        val savedMode = prefs.getString("pref_default_mode", "t9")
        inputMode = when (savedMode) {
            "keyboard" -> InputMode.DirectInput
            "t9predictive" -> InputMode.T9Predictive
            "numeric" -> InputMode.Numeric
            else -> InputMode.MultiTap
        }
    }

    // ==================== T9 MULTI-TAP ====================

    /** Handle a physical number key press (2-9) */
    fun onT9KeyPress(key: Int) {
        if (key !in 2..9) return

        // Route to predictive mode if active
        if (inputMode == InputMode.T9Predictive) {
            onT9PredictiveKeyPress(key)
            return
        }

        if (inputMode != InputMode.MultiTap) return
        if (key !in 2..9) return

        val lang = currentLanguage

        // If a different key is pressed while a character is pending, commit the pending char first
        if (multiTapState.isActive && multiTapState.currentKey != key) {
            // Save the pending character before state changes
            val pendingChar = multiTapState.getCurrentChar(lang)
            if (pendingChar != null) {
                val finalPending = if (lang.scriptType.supportsUpperCase) {
                    shiftState.applyToChar(pendingChar)
                } else {
                    pendingChar
                }
                composingBuffer.append(finalPending)
                if (shiftState == ShiftState.UPPER_NEXT) {
                    shiftState = shiftState.afterCharTyped()
                    onShiftChanged(shiftState)
                }
            }
            multiTapState.reset()
            multiTapTimer.cancel()
        }

        val wasCycle = multiTapState.onKeyPress(key, lang)

        val currentChar = multiTapState.getCurrentChar(lang) ?: return
        val allChars = multiTapState.getKeyChars(lang)

        // Apply shift
        val displayChar = if (lang.scriptType.supportsUpperCase) {
            shiftState.applyToChar(currentChar)
        } else {
            currentChar
        }

        // Show composing text with current character
        val composing = composingBuffer.toString() + displayChar
        inputConnection.safeSetComposing(composing)
        onComposingTextChanged(composing)

        // Update T9 indicator
        onMultiTapStateChanged(key, multiTapState.charIndex, allChars)

        // Build digit sequence for prediction
        if (!wasCycle) {
            digitSequence.append(key)
        }
        onSuggestionsNeeded(digitSequence.toString(), "")

        // Restart timer
        multiTapTimer.restart()
        feedbackManager.onKeyPress()
    }

    /** Commit the current multi-tap character and reset */
    private fun commitMultiTapChar() {
        val lang = currentLanguage
        val ch = multiTapState.getCurrentChar(lang) ?: return

        val finalChar = if (lang.scriptType.supportsUpperCase) {
            shiftState.applyToChar(ch)
        } else {
            ch
        }

        composingBuffer.append(finalChar)
        multiTapState.reset()
        multiTapTimer.cancel()

        // Update composing
        inputConnection.safeSetComposing(composingBuffer.toString())
        onComposingTextChanged(composingBuffer.toString())
        onMultiTapStateChanged(-1, 0, emptyList())

        // Shift returns to lower after one letter in UPPER_NEXT mode
        if (shiftState == ShiftState.UPPER_NEXT) {
            shiftState = shiftState.afterCharTyped()
            onShiftChanged(shiftState)
        }
    }

    // ==================== T9 PREDICTIVE MODE ====================

    /**
     * T9 Predictive: each key press appends a digit to the sequence.
     * The system queries the dictionary for matching words and shows the best match
     * as composing text. User cycles through candidates with repeated presses of
     * the same key or D-pad left/right.
     */
    private fun onT9PredictiveKeyPress(key: Int) {
        if (key !in 2..9) return
        feedbackManager.onKeyPress()

        digitSequence.append(key)
        predictiveCandidateIndex = 0

        // Query for matching words asynchronously
        onSuggestionsNeeded(digitSequence.toString(), "")

        // Show digit sequence in the T9 indicator while loading
        onMultiTapStateChanged(-1, 0, emptyList())
        onComposingTextChanged(digitSequence.toString())

        // Set composing to digit sequence temporarily (will be replaced by candidate)
        inputConnection.safeSetComposing(digitSequence.toString())
    }

    /**
     * Called by SuggestionManager callback to update predictive candidates.
     * In T9 Predictive mode, the first candidate becomes the composing text.
     */
    fun updatePredictiveCandidates(candidates: List<String>) {
        if (inputMode != InputMode.T9Predictive) return
        predictiveCandidates = candidates
        predictiveCandidateIndex = 0

        if (candidates.isNotEmpty()) {
            val word = candidates[0]
            inputConnection.safeSetComposing(word)
            onComposingTextChanged(word)
        }
        onPredictiveCandidatesChanged?.invoke(candidates, predictiveCandidateIndex)
    }

    /** Cycle to the next T9 predictive candidate (called on D-pad right or * key) */
    fun nextPredictiveCandidate() {
        if (inputMode != InputMode.T9Predictive) return
        if (predictiveCandidates.isEmpty()) return

        predictiveCandidateIndex = (predictiveCandidateIndex + 1) % predictiveCandidates.size
        val word = predictiveCandidates[predictiveCandidateIndex]
        inputConnection.safeSetComposing(word)
        onComposingTextChanged(word)
        onPredictiveCandidatesChanged?.invoke(predictiveCandidates, predictiveCandidateIndex)
    }

    /** Cycle to the previous T9 predictive candidate */
    fun prevPredictiveCandidate() {
        if (inputMode != InputMode.T9Predictive) return
        if (predictiveCandidates.isEmpty()) return

        predictiveCandidateIndex = if (predictiveCandidateIndex == 0) {
            predictiveCandidates.size - 1
        } else {
            predictiveCandidateIndex - 1
        }
        val word = predictiveCandidates[predictiveCandidateIndex]
        inputConnection.safeSetComposing(word)
        onComposingTextChanged(word)
        onPredictiveCandidatesChanged?.invoke(predictiveCandidates, predictiveCandidateIndex)
    }

    /** Handle backspace in T9 predictive mode */
    private fun onT9PredictiveBackspace() {
        if (digitSequence.isNotEmpty()) {
            digitSequence.deleteCharAt(digitSequence.length - 1)
            predictiveCandidateIndex = 0
            if (digitSequence.isEmpty()) {
                inputConnection.safeFinishComposing()
                onComposingTextChanged("")
                predictiveCandidates = emptyList()
                onPredictiveCandidatesChanged?.invoke(emptyList(), 0)
            } else {
                onSuggestionsNeeded(digitSequence.toString(), "")
                inputConnection.safeSetComposing(digitSequence.toString())
                onComposingTextChanged(digitSequence.toString())
            }
        } else {
            inputConnection.safeDeleteSurrounding(1, 0)
        }
    }

    // ==================== DIRECT INPUT (on-screen keyboard) ====================

    /** Handle a character from the on-screen keyboard */
    fun onCharacterInput(char: String) {
        feedbackManager.onKeyPress()

        val finalChar = if (currentLanguage.scriptType.supportsUpperCase && char.length == 1) {
            shiftState.applyToChar(char[0]).toString()
        } else {
            char
        }

        composingBuffer.append(finalChar)
        inputConnection.safeSetComposing(composingBuffer.toString())
        onComposingTextChanged(composingBuffer.toString())

        // Request suggestions based on typed prefix
        onSuggestionsNeeded("", composingBuffer.toString())

        // Shift returns to lower after one letter
        if (shiftState == ShiftState.UPPER_NEXT && char.length == 1 && char[0].isLetter()) {
            shiftState = shiftState.afterCharTyped()
            onShiftChanged(shiftState)
        }
    }

    // ==================== SPACE, ENTER, BACKSPACE ====================

    /** Handle space key press */
    fun onSpace() {
        feedbackManager.onSpecialKeyPress()

        // Smart punctuation: double-space = period + space
        val smartPunctuation = prefs.getBoolean("pref_smart_punctuation", true)
        val now = System.currentTimeMillis()
        if (smartPunctuation && now - lastSpaceTime < 400 && composingBuffer.isEmpty()) {
            inputConnection.safeDeleteSurrounding(1, 0)
            inputConnection.safeCommitText(". ")
            lastSpaceTime = 0
            autoCapitalize()
            return
        }
        lastSpaceTime = now

        // Commit composing text + space (with autocorrect + learning)
        commitComposingWordWithFeatures(hasAutoSpace = true)
        inputConnection.safeCommitText(" ")
        autoCapitalize()
    }

    /** Handle enter key press */
    fun onEnter() {
        feedbackManager.onSpecialKeyPress()
        commitComposingWordWithFeatures()
    }

    /** Handle backspace */
    fun onBackspace() {
        feedbackManager.onSpecialKeyPress()

        // T9 Predictive backspace
        if (inputMode == InputMode.T9Predictive) {
            onT9PredictiveBackspace()
            return
        }

        if (multiTapState.isActive) {
            multiTapState.reset()
            multiTapTimer.cancel()
            if (digitSequence.isNotEmpty()) {
                digitSequence.deleteCharAt(digitSequence.length - 1)
            }
            inputConnection.safeSetComposing(composingBuffer.toString())
            onComposingTextChanged(composingBuffer.toString())
            onMultiTapStateChanged(-1, 0, emptyList())
            return
        }

        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (digitSequence.isNotEmpty()) {
                digitSequence.deleteCharAt(digitSequence.length - 1)
            }
            if (composingBuffer.isEmpty()) {
                inputConnection.safeFinishComposing()
            } else {
                inputConnection.safeSetComposing(composingBuffer.toString())
            }
            onComposingTextChanged(composingBuffer.toString())
            onSuggestionsNeeded(digitSequence.toString(), composingBuffer.toString())
        } else {
            inputConnection.safeDeleteSurrounding(1, 0)
        }
    }

    /** Handle long-press backspace: delete word by word */
    fun onBackspaceLongPress() {
        feedbackManager.onSpecialKeyPress()
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.clear()
            digitSequence.clear()
            inputConnection.safeFinishComposing()
            onComposingTextChanged("")
        } else {
            inputConnection.deleteWordBackward()
        }
    }

    // ==================== COMMIT AND SUGGESTIONS ====================

    /** Commit the current composing word (simple, no features) */
    fun commitComposingWord() {
        // T9 Predictive: commit the selected candidate word
        if (inputMode == InputMode.T9Predictive && digitSequence.isNotEmpty()) {
            val word = if (predictiveCandidates.isNotEmpty()) {
                predictiveCandidates[predictiveCandidateIndex]
            } else {
                digitSequence.toString() // Fallback: commit digits if no match
            }
            inputConnection.safeFinishComposing()
            inputConnection.safeCommitText(word)
            lastCommittedWord = word
            composingBuffer.clear()
            digitSequence.clear()
            predictiveCandidates = emptyList()
            predictiveCandidateIndex = 0
            onComposingTextChanged("")
            onPredictiveCandidatesChanged?.invoke(emptyList(), 0)
            return
        }

        if (composingBuffer.isNotEmpty()) {
            var word = composingBuffer.toString()
            word = applyHebrewFinalForms(word)
            inputConnection.safeCommitText(word)
            lastCommittedWord = word
            composingBuffer.clear()
            digitSequence.clear()
            multiTapState.reset()
            multiTapTimer.cancel()
            onComposingTextChanged("")
            onMultiTapStateChanged(-1, 0, emptyList())
        } else {
            inputConnection.safeFinishComposing()
        }
    }

    /**
     * Commit with all features: autocorrect, word learning, bigram recording,
     * Hebrew final forms, and shortcut expansion.
     */
    private fun commitComposingWordWithFeatures(hasAutoSpace: Boolean = false) {
        if (composingBuffer.isEmpty()) {
            inputConnection.safeFinishComposing()
            return
        }

        var word = composingBuffer.toString()

        // Apply Hebrew final forms
        word = applyHebrewFinalForms(word)

        // Check for text shortcuts
        if (!isPasswordMode) {
            val expanded = shortcutEngine?.checkAndExpand(word)
            if (expanded != null) {
                inputConnection.safeCommitText(expanded)
                lastCommittedWord = expanded
                composingBuffer.clear()
                digitSequence.clear()
                multiTapState.reset()
                multiTapTimer.cancel()
                onComposingTextChanged("")
                onMultiTapStateChanged(-1, 0, emptyList())
                return
            }
        }

        // Try autocorrect (Latin only, not in password mode)
        if (!isPasswordMode && currentLanguage.hasAutoCorrect &&
            prefs.getBoolean("pref_autocorrect", false)
        ) {
            // Snapshot and clear composing state synchronously to prevent race condition
            val snapshotWord = word
            val snapshotLang = currentLanguage.code
            composingBuffer.clear()
            digitSequence.clear()
            multiTapState.reset()
            multiTapTimer.cancel()
            onComposingTextChanged("")
            onMultiTapStateChanged(-1, 0, emptyList())

            scope.launch(Dispatchers.IO) {
                val corrected = spellChecker?.autoCorrect(snapshotWord, snapshotLang)
                withContext(Dispatchers.Main) {
                    val finalWord = corrected ?: snapshotWord
                    if (corrected != null) {
                        onAutoCorrect?.invoke(snapshotWord, corrected)
                    }
                    doCommitDirect(finalWord, hasAutoSpace)
                }
            }
        } else {
            doCommit(word, hasAutoSpace)
        }
    }

    /** Commit after autocorrect (composing state already cleared) */
    private fun doCommitDirect(word: String, hasAutoSpace: Boolean = false) {
        undoManager?.record(word, word.length, hasAutoSpace)
        inputConnection.safeCommitText(word)
        val prevWord = lastCommittedWord
        lastCommittedWord = word
        runPostCommitTasks(word, prevWord)
    }

    private fun doCommit(word: String, hasAutoSpace: Boolean = false) {
        // Record state for undo before committing
        undoManager?.record(word, word.length, hasAutoSpace)

        inputConnection.safeCommitText(word)

        val prevWord = lastCommittedWord
        lastCommittedWord = word

        composingBuffer.clear()
        digitSequence.clear()
        multiTapState.reset()
        multiTapTimer.cancel()
        onComposingTextChanged("")
        onMultiTapStateChanged(-1, 0, emptyList())

        runPostCommitTasks(word, prevWord)
    }

    private fun runPostCommitTasks(word: String, prevWord: String) {
        // Run spell check on committed word (async, shows suggestions if misspelled)
        if (!isPasswordMode && currentLanguage.hasSpellCheck &&
            prefs.getBoolean("pref_spell_check", true)
        ) {
            scope.launch(Dispatchers.IO) {
                val checker = spellChecker ?: return@launch
                if (!checker.isSpellingCorrect(word, currentLanguage.code)) {
                    val suggestions = checker.getSuggestions(word, currentLanguage.code)
                    if (suggestions.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onSpellCheckResult?.invoke(word, suggestions)
                        }
                    }
                }
            }
        }

        // Record word learning and bigram (async, non-blocking)
        if (!isPasswordMode && prefs.getBoolean("pref_word_learning", true)) {
            scope.launch(Dispatchers.IO) {
                // Learn the word
                val lang = currentLanguage
                val digits = lang.t9KeyMap.wordToDigitSequence(word.lowercase())
                learnedWordDao?.upsert(word.lowercase(), lang.code, digits)

                // Record bigram
                if (prevWord.isNotEmpty()) {
                    bigramDao?.recordPair(lang.code, prevWord.lowercase(), word.lowercase())
                }
            }
        }
    }

    /** Accept a suggestion from the suggestion bar */
    fun acceptSuggestion(word: String) {
        composingBuffer.clear()
        digitSequence.clear()
        multiTapState.reset()
        multiTapTimer.cancel()

        inputConnection.safeFinishComposing()
        inputConnection.safeCommitText(word)

        val prevWord = lastCommittedWord
        lastCommittedWord = word

        // Auto-space after suggestion
        inputConnection.safeCommitText(" ")

        onComposingTextChanged("")
        onMultiTapStateChanged(-1, 0, emptyList())
        autoCapitalize()

        // Learn from suggestion acceptance
        if (!isPasswordMode && prefs.getBoolean("pref_word_learning", true)) {
            scope.launch(Dispatchers.IO) {
                val lang = currentLanguage
                val digits = lang.t9KeyMap.wordToDigitSequence(word.lowercase())
                learnedWordDao?.upsert(word.lowercase(), lang.code, digits)
                if (prevWord.isNotEmpty()) {
                    bigramDao?.recordPair(lang.code, prevWord.lowercase(), word.lowercase())
                }
            }
        }
    }

    // ==================== HEBREW FINAL FORMS ====================

    /**
     * Replace Hebrew letters with their final forms when at the end of a word.
     * כ→ך  מ→ם  נ→ן  פ→ף  צ→ץ
     */
    private fun applyHebrewFinalForms(word: String): String {
        if (currentLanguage.scriptType != ScriptType.HEBREW) return word
        val finalForms = currentLanguage.finalForms
        if (finalForms.isEmpty() || word.isEmpty()) return word

        val chars = word.toCharArray()
        // Only the last character gets converted to final form
        val lastChar = chars.last()
        val finalForm = finalForms[lastChar]
        if (finalForm != null) {
            chars[chars.size - 1] = finalForm
        }

        // Also un-finalize any non-last character that has a final form
        val reverseFinals = finalForms.entries.associate { it.value to it.key }
        for (i in 0 until chars.size - 1) {
            val baseForm = reverseFinals[chars[i]]
            if (baseForm != null) {
                chars[i] = baseForm
            }
        }

        return String(chars)
    }

    // ==================== MODE AND LANGUAGE ====================

    // Debounce for mode toggle to prevent rapid toggling
    private var lastModeChangeTime = 0L

    /** Toggle between T9 Multi-tap → T9 Predictive → On-screen keyboard */
    fun toggleMode() {
        val now = System.currentTimeMillis()
        if (now - lastModeChangeTime < 300) return
        lastModeChangeTime = now

        commitComposingWord()
        inputMode = when (inputMode) {
            InputMode.MultiTap -> InputMode.T9Predictive
            InputMode.T9Predictive -> InputMode.DirectInput
            InputMode.DirectInput -> InputMode.MultiTap
            else -> InputMode.MultiTap
        }
        // Reset predictive state when leaving predictive mode
        if (inputMode != InputMode.T9Predictive) {
            predictiveCandidates = emptyList()
            predictiveCandidateIndex = 0
        }
        prefs.edit().putString(
            "pref_default_mode",
            when (inputMode) {
                InputMode.DirectInput -> "keyboard"
                InputMode.T9Predictive -> "t9predictive"
                else -> "t9"
            }
        ).apply()
        onModeChanged(inputMode)
    }

    /** Switch to the next enabled language */
    fun nextLanguage() {
        val previousDirection = currentLanguage.textDirection
        commitComposingWord()
        LanguageRegistry.nextLanguage(context)
        val newDirection = currentLanguage.textDirection
        // Insert direction marker when switching between RTL and LTR
        if (previousDirection != newDirection) {
            inputConnection.insertDirectionMarker(newDirection == TextDirection.RTL)
        }
        onLanguageChanged(currentLanguage)
        autoCapitalize()
    }

    /** Toggle shift state */
    fun toggleShift() {
        feedbackManager.onCapsToggle()
        shiftState = shiftState.onShiftTap()
        onShiftChanged(shiftState)
        updateCapsLockReminder()
    }

    /** Double-tap shift for caps lock */
    fun doubleTapShift() {
        feedbackManager.onCapsToggle()
        shiftState = shiftState.onShiftDoubleTap()
        onShiftChanged(shiftState)
        updateCapsLockReminder()
    }

    // ==================== AUTO FEATURES ====================

    /** Auto-capitalize at start of sentence */
    private fun autoCapitalize() {
        val enabled = prefs.getBoolean("pref_auto_capitalize", true)
        if (!enabled) return
        if (!currentLanguage.scriptType.supportsUpperCase) return
        if (shiftState == ShiftState.CAPS_LOCK) return

        if (inputConnection.isStartOfSentence()) {
            shiftState = ShiftState.UPPER_NEXT
            onShiftChanged(shiftState)
        }
    }

    /** Start/stop caps lock reminder vibration */
    private fun updateCapsLockReminder() {
        capsLockReminderJob?.cancel()
        if (shiftState.isCapsLock) {
            capsLockReminderJob = scope.launch {
                while (isActive) {
                    delay(30_000L)
                    feedbackManager.onCapsLockReminder()
                }
            }
        }
    }

    // ==================== SPECIAL INPUT ====================

    /** Handle punctuation input (triggers autocorrect + spell check) */
    fun onPunctuation(char: String) {
        feedbackManager.onKeyPress()
        commitComposingWordWithFeatures()
        inputConnection.safeCommitText(char)
        autoCapitalize()
    }

    /** Handle nikud insertion (Hebrew vowel point) */
    fun onNikudInput(nikud: Char) {
        feedbackManager.onKeyPress()
        // Nikud are combining marks — they attach to the preceding consonant
        inputConnection.safeCommitText(nikud.toString())
    }

    /** Handle emoji insertion */
    fun onEmojiInput(emoji: String) {
        commitComposingWord()
        inputConnection.safeCommitText(emoji)
    }

    /** Handle clipboard paste */
    fun onClipboardPaste(text: String) {
        commitComposingWord()
        inputConnection.safeCommitText(text)
    }

    /** Set input to numeric mode (for number fields) */
    fun setNumericMode() {
        inputMode = InputMode.Numeric
        onModeChanged(inputMode)
    }

    /** Set input to direct mode (for on-screen keyboard) */
    fun setDirectMode() {
        inputMode = InputMode.DirectInput
        onModeChanged(inputMode)
    }

    /** Reset state for new input field */
    fun resetForNewField() {
        composingBuffer.clear()
        digitSequence.clear()
        multiTapState.reset()
        multiTapTimer.cancel()
        predictiveCandidates = emptyList()
        predictiveCandidateIndex = 0
        lastCommittedWord = ""
        lastSpaceTime = 0
        isPasswordMode = false
        autoCapitalize()
    }

    fun destroy() {
        scope.cancel()
        capsLockReminderJob?.cancel()
    }
}
