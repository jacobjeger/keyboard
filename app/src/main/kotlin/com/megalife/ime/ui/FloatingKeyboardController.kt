package com.megalife.ime.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Controls the floating/docked state of the keyboard.
 * In floating mode, the keyboard can be repositioned by dragging.
 * In docked mode, the keyboard stays fixed at the bottom of the screen.
 */
class FloatingKeyboardController(private val context: Context) {

    enum class Mode { DOCKED, FLOATING }

    var mode: Mode = Mode.DOCKED
        private set

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }

    // Touch handling for dragging in floating mode
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    /**
     * Toggle between docked and floating mode.
     */
    fun toggle() {
        mode = if (mode == Mode.DOCKED) Mode.FLOATING else Mode.DOCKED
    }

    /**
     * Set the mode directly.
     */
    fun setMode(newMode: Mode) {
        mode = newMode
    }

    fun isFloating(): Boolean = mode == Mode.FLOATING

    /**
     * Get layout params configured for the current mode.
     * In floating mode, the keyboard is movable.
     * In docked mode, it is anchored at the bottom.
     */
    fun getLayoutParams(): WindowManager.LayoutParams {
        return if (mode == Mode.FLOATING) {
            layoutParams.apply {
                gravity = Gravity.NO_GRAVITY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }
        } else {
            layoutParams.apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
        }
    }

    /**
     * Create a touch listener for dragging the keyboard in floating mode.
     * Attach this to the keyboard's drag handle area.
     */
    fun createDragTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            if (mode != Mode.FLOATING) return@OnTouchListener false

            val wm = windowManager ?: return@OnTouchListener false
            val fv = floatingView ?: return@OnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(fv, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Attach a view and window manager for floating mode management.
     */
    fun attach(view: View, wm: WindowManager) {
        floatingView = view
        windowManager = wm
    }

    /**
     * Detach the floating view reference.
     */
    fun detach() {
        floatingView = null
        windowManager = null
    }
}
