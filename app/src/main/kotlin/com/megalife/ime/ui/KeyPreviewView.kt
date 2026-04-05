package com.megalife.ime.ui

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Shows a key preview popup above the pressed key.
 * Appears briefly to confirm which key the user tapped.
 */
class KeyPreviewView(private val context: Context) {

    private var popupWindow: PopupWindow? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    /**
     * Show a preview of the character above the key.
     * Auto-dismisses after a short delay.
     */
    fun show(anchorView: View, label: String, keyRect: RectF) {
        dismiss()

        val previewSize = dpToPx(48)

        val textView = TextView(context).apply {
            text = label
            val theme = KeyboardTheme.getTheme(context)
            setTextColor(theme.keyTextColor)
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundColor(theme.keyPressedColor)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            elevation = dpToPx(8).toFloat()
            minWidth = previewSize
            minHeight = previewSize
        }

        popupWindow = PopupWindow(textView, previewSize, previewSize, false).apply {
            isOutsideTouchable = false
            isTouchable = false
            animationStyle = 0
        }

        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val x = (keyRect.centerX() - previewSize / 2).toInt().coerceAtLeast(0)
        val y = (keyRect.top - previewSize - dpToPx(8)).toInt()

        try {
            popupWindow?.showAtLocation(
                anchorView, Gravity.NO_GRAVITY,
                location[0] + x, location[1] + y
            )
        } catch (e: Exception) {
            // View may not be attached yet
        }

        // Auto dismiss after 80ms
        dismissRunnable = Runnable { dismiss() }
        handler.postDelayed(dismissRunnable!!, 80)
    }

    fun dismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics
        ).toInt()
    }
}
