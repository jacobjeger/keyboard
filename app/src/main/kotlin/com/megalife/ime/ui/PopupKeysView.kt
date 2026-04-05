package com.megalife.ime.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Popup window showing alternate characters on long-press.
 * Appears above the pressed key. Finger slides to select.
 */
class PopupKeysView(private val context: Context) {

    private var popupWindow: PopupWindow? = null
    private var selectedIndex: Int = -1
    private var charViews: List<TextView> = emptyList()
    var onCharSelected: ((String) -> Unit)? = null

    /**
     * Show popup with the given characters, anchored above the given view at the key rect.
     */
    fun show(
        anchorView: View,
        chars: List<String>,
        keyRect: RectF
    ) {
        dismiss()
        if (chars.isEmpty()) return

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(KeyboardTheme.getTheme(context).keySpecialColor)
            setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6))
            elevation = dpToPx(4).toFloat()
        }

        charViews = chars.map { ch ->
            TextView(context).apply {
                text = ch
                setTextColor(KeyboardTheme.getTheme(context).keyTextColor)
                textSize = 20f
                gravity = Gravity.CENTER
                val size = dpToPx(40)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                minWidth = size
                minHeight = size
            }
        }

        charViews.forEach { container.addView(it) }

        val totalWidth = chars.size * dpToPx(40) + dpToPx(8)
        val popupHeight = dpToPx(52)

        popupWindow = PopupWindow(container, totalWidth, popupHeight, false).apply {
            isOutsideTouchable = true
            isTouchable = true
            animationStyle = 0
            setTouchInterceptor { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        updateSelection(event.x)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        updateSelection(event.x)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val selected = commitSelection()
                        if (selected != null) {
                            onCharSelected?.invoke(selected)
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        // Position above the key
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val x = (keyRect.centerX() - totalWidth / 2).toInt().coerceAtLeast(0)
        val y = (keyRect.top - popupHeight - dpToPx(4)).toInt()

        popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY,
            location[0] + x, location[1] + y)
    }

    /**
     * Update selection based on finger position (called during ACTION_MOVE).
     * Returns the currently highlighted character index.
     */
    fun updateSelection(touchX: Float): Int {
        if (charViews.isEmpty()) return -1

        val cellWidth = dpToPx(40)
        val startX = dpToPx(4) // padding
        selectedIndex = ((touchX - startX) / cellWidth).toInt().coerceIn(0, charViews.size - 1)

        // Highlight selected
        charViews.forEachIndexed { i, tv ->
            tv.setBackgroundColor(
                if (i == selectedIndex) KeyboardTheme.getTheme(context).accentColor
                else Color.TRANSPARENT
            )
        }

        return selectedIndex
    }

    /**
     * Commit the selection and dismiss.
     */
    fun commitSelection(): String? {
        val result = if (selectedIndex >= 0 && selectedIndex < charViews.size) {
            charViews[selectedIndex].text.toString()
        } else null

        dismiss()
        return result
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
        selectedIndex = -1
        charViews = emptyList()
    }

    fun isShowing(): Boolean = popupWindow?.isShowing == true

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics
        ).toInt()
    }
}
