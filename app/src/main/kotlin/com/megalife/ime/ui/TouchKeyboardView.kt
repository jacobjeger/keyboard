package com.megalife.ime.ui

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.preference.PreferenceManager
import com.megalife.ime.feature.GlideTypingController
import com.megalife.ime.input.ShiftState
import com.megalife.ime.language.*
import com.megalife.ime.settings.PreferenceKeys

/**
 * Custom View that draws the on-screen touch keyboard.
 * Handles touch events, key rendering, RTL layouts, D-pad focus navigation,
 * glide/swipe typing detection, and cursor movement via space long-press.
 */
class TouchKeyboardView(
    context: Context,
    private var language: LanguageConfig
) : View(context) {

    // Callbacks
    var onKeyPress: ((KeyDef) -> Unit)? = null
    var onKeyLongPress: ((KeyDef) -> Unit)? = null
    var onGlideWord: ((List<String>) -> Unit)? = null
    var onCursorMove: ((direction: Int) -> Unit)? = null  // -1 = left, 1 = right
    var onSwipeBackspace: (() -> Unit)? = null  // swipe left from backspace = delete word
    var onDismissKeyboard: (() -> Unit)? = null  // swipe down gesture
    var onOpenSettings: (() -> Unit)? = null  // swipe up from bottom row area

    // Layout computation
    val keyRects = mutableListOf<KeyRect>()
    private var layout: TouchLayout = language.touchLayout
    private var shiftState: ShiftState = ShiftState.LOWER

    // Enter key label (dynamic per IME action)
    var enterKeyLabel: String = "↵"
        private set

    // Mode badge (shows current input mode on the keyboard)
    var modeBadge: String = ""
        private set

    // Touch tracking
    private var pressedKeyIndex: Int = -1
    private var longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private val longPressDelay = 500L

    // Key preview popup
    var keyPreviewView: KeyPreviewView? = null

    // Glide typing
    var glideController: GlideTypingController? = null
    var glideTrailView: GlideTrailView? = null
    private var isGlideActive = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val glideThreshold = 30f  // pixels before swipe activates

    // Cursor movement via space long-press
    private var cursorMoveActive = false
    private var cursorMoveStartX = 0f
    private var cursorMoveAccumulator = 0f
    private val cursorMoveStep = 30f  // pixels per cursor step

    // Swipe backspace tracking
    private var backspaceSwipeActive = false
    private val backspaceSwipeThreshold = 60f

    // Swipe gesture tracking (dismiss keyboard / open settings)
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var isSwipeGesture = false
    private val swipeGestureThreshold = 100f  // minimum pixels for a swipe gesture

    // Key repeat on hold (for CHARACTER keys)
    private val repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private var repeatingKey: KeyDef? = null
    private val keyRepeatInterval = 50L  // milliseconds between repeated key events

    // Motor accessibility: key press delay filter
    private var lastKeyPressTime = 0L
    private var keyPressDelay = 0L

    // D-pad focus
    private var focusedKeyIndex: Int = -1
    var focusEnabled: Boolean = false

    // Runtime-configurable dimensions (read from preferences)
    private var keyHeightDp = KeyboardTheme.getKeyHeightDp(context)
    private var keyTextSizeSp = KeyboardTheme.getKeyTextSizeSp(context)
    private var keyCornerRadiusDp = KeyboardTheme.getKeyRadiusDp(context)
    private var keyBorderDp = KeyboardTheme.getKeyBorderDp(context)
    private var showNumberRow = KeyboardTheme.isNumberRowEnabled(context)

    // Theme colors (resolved from preferences)
    private var theme = KeyboardTheme.getTheme(context)

    // Paints (pre-allocated for performance, theme-aware)
    private var keyPaint = KeyboardTheme.createKeyPaint(theme)
    private var keyTextPaint = KeyboardTheme.createKeyTextPaint(theme, spToPx(keyTextSizeSp))
    private var subTextPaint = KeyboardTheme.createSubTextPaint(theme, spToPx(KeyboardTheme.KEY_SUB_TEXT_SIZE_SP))
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.focusBorderColor
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(KeyboardTheme.FOCUS_BORDER_WIDTH_DP)
    }
    private var pressedPaint = KeyboardTheme.createPressedPaint(theme)
    private var specialPaint = KeyboardTheme.createSpecialPaint(theme)
    private var accentTextPaint = KeyboardTheme.createAccentTextPaint(theme, spToPx(keyTextSizeSp))
    private var keyBorderPaint: Paint? = if (keyBorderDp > 0) {
        KeyboardTheme.createKeyBorderPaint(dpToPx(keyBorderDp))
    } else null

    // Pre-allocated badge paints (avoid allocation in onDraw)
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentColor
        textSize = spToPx(9f)
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.keySpecialColor
        style = Paint.Style.FILL
    }

    private var cornerRadius = dpToPx(keyCornerRadiusDp)
    private val keySpacing = dpToPx(KeyboardTheme.KEY_SPACING_DP)

    data class KeyRect(
        val rect: RectF,
        val keyDef: KeyDef,
        val row: Int,
        val col: Int
    )

    init {
        // No background set — parent handles it. Keyboard draws its own bg in onDraw.
        isFocusable = false
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        loadAccessibilityPrefs()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        stopKeyRepeat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val effectiveLayout = getEffectiveLayout()
        val keyHeight = dpToPx(keyHeightDp)
        val height = (effectiveLayout.rows.size * (keyHeight + keySpacing) + keySpacing).toInt()
        setMeasuredDimension(width, height)
    }

    /** Get the layout with optional number row prepended */
    private fun getEffectiveLayout(): TouchLayout {
        if (!showNumberRow) return layout
        // Only add number row to letter layouts (not symbols/number pads which already have numbers)
        if (layout.rows.firstOrNull()?.keys?.all { it.primaryChar.length == 1 && it.primaryChar[0].isDigit() } == true) {
            return layout
        }
        val numberRow = KeyRow(listOf(
            KeyDef("1"), KeyDef("2"), KeyDef("3"), KeyDef("4"), KeyDef("5"),
            KeyDef("6"), KeyDef("7"), KeyDef("8"), KeyDef("9"), KeyDef("0")
        ))
        return layout.copy(rows = listOf(numberRow) + layout.rows)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeKeyRects(w.toFloat(), h.toFloat())
        updateGlideKeyBounds()
    }

    /** Feed key positions to glide controller for swipe path matching */
    private fun updateGlideKeyBounds() {
        val controller = glideController ?: return
        val bounds = keyRects
            .filter { it.keyDef.type == KeyType.CHARACTER && it.keyDef.primaryChar.length == 1 }
            .map { kr ->
                GlideTypingController.KeyBounds(
                    char = kr.keyDef.primaryChar[0],
                    centerX = kr.rect.centerX(),
                    centerY = kr.rect.centerY(),
                    width = kr.rect.width(),
                    height = kr.rect.height()
                )
            }
        controller.setKeyBounds(bounds)
    }

    private fun computeKeyRects(width: Float, height: Float) {
        keyRects.clear()
        val effectiveLayout = getEffectiveLayout()
        val rowCount = effectiveLayout.rows.size
        val keyHeight = (height - keySpacing * (rowCount + 1)) / rowCount

        var y = keySpacing

        for ((rowIndex, row) in effectiveLayout.rows.withIndex()) {
            val totalWeight = row.keys.sumOf { it.widthWeight.toDouble() }.toFloat()
            val paddingWeight = row.leftPadding
            val availableWidth = width - keySpacing * (row.keys.size + 1)

            var x = keySpacing + (availableWidth * paddingWeight / (totalWeight + paddingWeight * 2))

            val keys = if (effectiveLayout.isRtl) row.keys.reversed() else row.keys

            for ((colIndex, keyDef) in keys.withIndex()) {
                val keyWidth = availableWidth * keyDef.widthWeight / (totalWeight + paddingWeight * 2)
                val rect = RectF(x, y, x + keyWidth, y + keyHeight)
                keyRects.add(KeyRect(rect, keyDef, rowIndex, colIndex))
                x += keyWidth + keySpacing
            }

            y += keyHeight + keySpacing
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for ((index, keyRect) in keyRects.withIndex()) {
            val (rect, keyDef, _, _) = keyRect

            // Choose paint based on key state
            val paint = when {
                index == pressedKeyIndex -> pressedPaint
                keyDef.type.isSpecial() -> specialPaint
                else -> keyPaint
            }

            // Draw key background
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            // Draw key label
            val label = getDisplayLabel(keyDef)
            val textPaint = if (keyDef.type == KeyType.SHIFT && shiftState.isUpperCase) {
                accentTextPaint
            } else {
                keyTextPaint
            }

            val textX = rect.centerX()
            val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(label, textX, textY, textPaint)

            // Draw sub-label (T9 letter hints) if present
            if (keyDef.subLabel.isNotEmpty()) {
                val subY = rect.bottom - dpToPx(4f)
                canvas.drawText(keyDef.subLabel, textX, subY, subTextPaint)
            }

            // Draw key border if enabled
            keyBorderPaint?.let { borderPaint ->
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            }

            // Draw focus ring if focused
            if (focusEnabled && index == focusedKeyIndex) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, focusPaint)
            }
        }

        // Draw mode badge in top-right corner
        if (modeBadge.isNotEmpty()) {
            val badgeX = width.toFloat() - dpToPx(6f)
            val badgeY = dpToPx(12f)
            val textWidth = badgePaint.measureText(modeBadge)
            canvas.drawRoundRect(
                badgeX - textWidth - dpToPx(4f), badgeY - dpToPx(9f),
                badgeX + dpToPx(4f), badgeY + dpToPx(3f),
                dpToPx(3f), dpToPx(3f), badgeBgPaint
            )
            canvas.drawText(modeBadge, badgeX, badgeY, badgePaint)
        }
    }

    private fun getDisplayLabel(keyDef: KeyDef): String {
        return when (keyDef.type) {
            KeyType.SPACE -> keyDef.label
            KeyType.BACKSPACE -> "⌫"
            KeyType.ENTER -> enterKeyLabel
            KeyType.SHIFT -> when (shiftState) {
                ShiftState.LOWER -> "⇧"
                ShiftState.UPPER_NEXT -> "⇧"
                ShiftState.CAPS_LOCK -> "⇧🔒"
            }
            KeyType.CHARACTER -> {
                if (shiftState.isUpperCase && language.scriptType.supportsUpperCase) {
                    keyDef.label.uppercase()
                } else {
                    keyDef.label
                }
            }
            else -> keyDef.label
        }
    }

    fun setEnterLabel(label: String) {
        enterKeyLabel = label
        invalidate()
    }

    fun setModeBadge(badge: String) {
        modeBadge = badge
        invalidate()
    }

    // ==================== TOUCH HANDLING ====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle cursor movement mode (space long-press slide)
        if (cursorMoveActive) {
            return handleCursorMoveTouch(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                swipeStartX = event.x
                swipeStartY = event.y
                isGlideActive = false
                isSwipeGesture = false

                val index = findKeyAt(event.x, event.y)
                if (index >= 0) {
                    pressedKeyIndex = index
                    invalidate()

                    // Start long-press timer
                    longPressRunnable = Runnable {
                        if (pressedKeyIndex >= 0) {
                            val keyDef = keyRects[pressedKeyIndex].keyDef
                            // If space key long-press, enter cursor move mode
                            if (keyDef.type == KeyType.SPACE) {
                                cursorMoveActive = true
                                cursorMoveStartX = event.x
                                cursorMoveAccumulator = 0f
                                pressedKeyIndex = -1
                                invalidate()
                                onKeyLongPress?.invoke(keyDef)
                                return@Runnable
                            }
                            onKeyLongPress?.invoke(keyDef)
                            // Start key repeat for CHARACTER keys after long-press fires
                            if (keyDef.type == KeyType.CHARACTER) {
                                startKeyRepeat(keyDef)
                            }
                            pressedKeyIndex = -1
                            invalidate()
                        }
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, longPressDelay)
                }

                // Start glide trail
                if (language.hasGlideSupport) {
                    glideController?.onSwipeStart(event.x, event.y)
                    glideTrailView?.startTrail(event.x, event.y)
                }

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Check for swipe-left on backspace key → delete word
                if (!backspaceSwipeActive && pressedKeyIndex >= 0 &&
                    keyRects[pressedKeyIndex].keyDef.type == KeyType.BACKSPACE &&
                    dx < -backspaceSwipeThreshold
                ) {
                    backspaceSwipeActive = true
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    onSwipeBackspace?.invoke()
                    pressedKeyIndex = -1
                    invalidate()
                    return true
                }

                // Check if this is a glide gesture
                if (!isGlideActive && !backspaceSwipeActive && language.hasGlideSupport && distance > glideThreshold) {
                    isGlideActive = true
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    pressedKeyIndex = -1
                    invalidate()
                }

                if (isGlideActive) {
                    glideController?.onSwipeMove(event.x, event.y)
                    glideTrailView?.addPoint(event.x, event.y)
                } else if (!backspaceSwipeActive) {
                    val index = findKeyAt(event.x, event.y)
                    if (index != pressedKeyIndex) {
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        pressedKeyIndex = index
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                stopKeyRepeat()
                backspaceSwipeActive = false

                // Check for vertical swipe gestures (dismiss keyboard / open settings)
                val swipeDx = event.x - swipeStartX
                val swipeDy = event.y - swipeStartY
                val absSwipeDx = Math.abs(swipeDx)
                val absSwipeDy = Math.abs(swipeDy)
                val keyboardHeight = height.toFloat()
                val gestureStartedBelowTop20 = swipeStartY > keyboardHeight * 0.2f

                if (gestureStartedBelowTop20 && absSwipeDy > swipeGestureThreshold && absSwipeDy > absSwipeDx) {
                    // Vertical swipe detected (more vertical than horizontal)
                    if (swipeDy > swipeGestureThreshold) {
                        // Swipe down — dismiss keyboard
                        onDismissKeyboard?.invoke()
                        pressedKeyIndex = -1
                        invalidate()
                        return true
                    } else if (swipeDy < -swipeGestureThreshold) {
                        // Swipe up from bottom row area — open settings
                        val bottomRowTop = keyboardHeight * 0.75f
                        if (swipeStartY > bottomRowTop) {
                            onOpenSettings?.invoke()
                            pressedKeyIndex = -1
                            invalidate()
                            return true
                        }
                    }
                }

                if (isGlideActive) {
                    // End glide — get word candidates
                    isGlideActive = false
                    glideTrailView?.endTrail()
                    glideController?.onSwipeEnd(language.code) { words ->
                        onGlideWord?.invoke(words)
                    }
                } else if (pressedKeyIndex >= 0 && shouldAcceptKeyPress()) {
                    val kr = keyRects[pressedKeyIndex]
                    // Show key preview for character keys
                    if (kr.keyDef.type == KeyType.CHARACTER) {
                        val label = getDisplayLabel(kr.keyDef)
                        keyPreviewView?.show(this, label, kr.rect)
                    }
                    announceKeyForAccessibility(kr.keyDef)
                    onKeyPress?.invoke(kr.keyDef)
                }
                pressedKeyIndex = -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                stopKeyRepeat()
                pressedKeyIndex = -1
                isGlideActive = false
                glideTrailView?.clearTrail()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleCursorMoveTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val delta = event.x - cursorMoveStartX - cursorMoveAccumulator
                if (Math.abs(delta) >= cursorMoveStep) {
                    val steps = (delta / cursorMoveStep).toInt()
                    val direction = if (steps > 0) 1 else -1
                    repeat(Math.abs(steps)) {
                        onCursorMove?.invoke(direction)
                    }
                    cursorMoveAccumulator += steps * cursorMoveStep
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cursorMoveActive = false
                return true
            }
        }
        return true
    }

    private fun findKeyAt(x: Float, y: Float): Int {
        for ((index, keyRect) in keyRects.withIndex()) {
            if (keyRect.rect.contains(x, y)) return index
        }
        return -1
    }

    // ==================== D-PAD FOCUS NAVIGATION ====================

    fun moveFocus(direction: Int) {
        if (keyRects.isEmpty()) return

        if (focusedKeyIndex < 0) {
            focusedKeyIndex = 0
            invalidate()
            return
        }

        val current = keyRects[focusedKeyIndex]
        val newIndex = when (direction) {
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> findNextInRow(current.row, focusedKeyIndex, 1)
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> findNextInRow(current.row, focusedKeyIndex, -1)
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> findInAdjacentRow(current, 1)
            android.view.KeyEvent.KEYCODE_DPAD_UP -> findInAdjacentRow(current, -1)
            else -> focusedKeyIndex
        }

        if (newIndex != focusedKeyIndex) {
            focusedKeyIndex = newIndex
            invalidate()
        }
    }

    fun selectFocusedKey() {
        if (focusedKeyIndex >= 0 && focusedKeyIndex < keyRects.size) {
            onKeyPress?.invoke(keyRects[focusedKeyIndex].keyDef)
        }
    }

    fun longPressFocusedKey() {
        if (focusedKeyIndex >= 0 && focusedKeyIndex < keyRects.size) {
            onKeyLongPress?.invoke(keyRects[focusedKeyIndex].keyDef)
        }
    }

    private fun findNextInRow(row: Int, currentIndex: Int, step: Int): Int {
        val rowKeys = keyRects.withIndex().filter { it.value.row == row }
        val posInRow = rowKeys.indexOfFirst { it.index == currentIndex }
        val nextPos = posInRow + step
        return if (nextPos in rowKeys.indices) rowKeys[nextPos].index else currentIndex
    }

    private fun findInAdjacentRow(current: KeyRect, rowStep: Int): Int {
        val targetRow = current.row + rowStep
        val rowKeys = keyRects.filter { it.row == targetRow }
        if (rowKeys.isEmpty()) return focusedKeyIndex

        val centerX = current.rect.centerX()
        return keyRects.indexOf(
            rowKeys.minByOrNull { Math.abs(it.rect.centerX() - centerX) }
        ).coerceAtLeast(0)
    }

    // ==================== STATE UPDATES ====================

    fun updateShiftState(state: ShiftState) {
        shiftState = state
        invalidate()
    }

    fun setLanguage(lang: LanguageConfig) {
        language = lang
        layout = lang.touchLayout
        shiftState = ShiftState.LOWER
        focusedKeyIndex = -1
        requestLayout()
    }

    /** Reload appearance preferences (call after returning from settings) */
    fun reloadPreferences() {
        keyHeightDp = KeyboardTheme.getKeyHeightDp(context)
        keyTextSizeSp = KeyboardTheme.getKeyTextSizeSp(context)
        keyCornerRadiusDp = KeyboardTheme.getKeyRadiusDp(context)
        keyBorderDp = KeyboardTheme.getKeyBorderDp(context)
        showNumberRow = KeyboardTheme.isNumberRowEnabled(context)

        // Refresh theme colors
        theme = KeyboardTheme.getTheme(context)
        keyPaint = KeyboardTheme.createKeyPaint(theme)
        keyTextPaint = KeyboardTheme.createKeyTextPaint(theme, spToPx(keyTextSizeSp))
        subTextPaint = KeyboardTheme.createSubTextPaint(theme, spToPx(KeyboardTheme.KEY_SUB_TEXT_SIZE_SP))
        pressedPaint = KeyboardTheme.createPressedPaint(theme)
        specialPaint = KeyboardTheme.createSpecialPaint(theme)
        accentTextPaint = KeyboardTheme.createAccentTextPaint(theme, spToPx(keyTextSizeSp))
        focusPaint.color = theme.focusBorderColor
        badgePaint.color = theme.accentColor
        badgeBgPaint.color = theme.keySpecialColor
        cornerRadius = dpToPx(keyCornerRadiusDp)
        keyBorderPaint = if (keyBorderDp > 0) {
            KeyboardTheme.createKeyBorderPaint(dpToPx(keyBorderDp))
        } else null

        // Reload accessibility preferences
        loadAccessibilityPrefs()

        requestLayout()
    }

    // ==================== KEY REPEAT ====================

    /**
     * Start repeating a CHARACTER key after long-press fires.
     * Repeatedly invokes onKeyPress every [keyRepeatInterval] ms until stopped.
     */
    private fun startKeyRepeat(key: KeyDef) {
        if (key.type != KeyType.CHARACTER) return
        repeatingKey = key
        repeatRunnable = object : Runnable {
            override fun run() {
                repeatingKey?.let { onKeyPress?.invoke(it) }
                repeatHandler.postDelayed(this, keyRepeatInterval)
            }
        }
        repeatHandler.postDelayed(repeatRunnable!!, keyRepeatInterval)
    }

    /** Stop the key repeat loop */
    private fun stopKeyRepeat() {
        repeatRunnable?.let { repeatHandler.removeCallbacks(it) }
        repeatRunnable = null
        repeatingKey = null
    }

    // ==================== ACCESSIBILITY ====================

    private fun loadAccessibilityPrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        keyPressDelay = prefs.getString(PreferenceKeys.KEY_PRESS_DELAY, "0")?.toLongOrNull() ?: 0L
    }

    private fun shouldAcceptKeyPress(): Boolean {
        if (keyPressDelay <= 0) return true
        val now = System.currentTimeMillis()
        if (now - lastKeyPressTime < keyPressDelay) return false
        lastKeyPressTime = now
        return true
    }

    private fun announceKeyForAccessibility(key: KeyDef) {
        if (!isAccessibilityEnabled()) return
        val desc = key.getAccessibilityDescription()
        announceForAccessibility(desc)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled
    }

    // ==================== UTILITY ====================

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }

    private fun KeyType.isSpecial(): Boolean {
        return this in listOf(KeyType.SHIFT, KeyType.BACKSPACE, KeyType.ENTER, KeyType.SYMBOLS, KeyType.EMOJI)
    }
}
