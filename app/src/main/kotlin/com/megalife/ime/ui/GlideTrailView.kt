package com.megalife.ime.ui

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View

/**
 * Overlay view that draws the glide/swipe trail on top of the keyboard.
 * Shows a fading gradient path following the user's finger during swipe typing.
 */
class GlideTrailView(context: Context) : View(context) {

    private val theme = KeyboardTheme.getTheme(context)

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentColor
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentColor
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Pre-allocated dot paint (avoid allocation in onDraw)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentColor
        style = Paint.Style.FILL
        alpha = 180
    }

    private val trailPath = Path()
    private val points = mutableListOf<PointF>()
    private var isActive = false

    // Pre-computed dp values for draw loop
    private val minStrokeWidthPx = dpToPx(1.5f)
    private val strokeWidthRangePx = dpToPx(2f)
    private val dotRadiusPx = dpToPx(5f)

    // Fading effect: older points fade out
    private val maxPoints = 100

    // Fade-out runnable (stored for cancellation)
    private var fadeRunnable: Runnable? = null

    fun startTrail(x: Float, y: Float) {
        fadeRunnable?.let { removeCallbacks(it) }
        points.clear()
        points.add(PointF(x, y))
        isActive = true
        invalidate()
    }

    fun addPoint(x: Float, y: Float) {
        if (!isActive) return
        points.add(PointF(x, y))
        if (points.size > maxPoints) {
            points.removeAt(0)
        }
        invalidate()
    }

    fun endTrail() {
        isActive = false
        fadeRunnable?.let { removeCallbacks(it) }
        fadeRunnable = Runnable {
            points.clear()
            invalidate()
        }
        postDelayed(fadeRunnable!!, 200)
    }

    fun clearTrail() {
        fadeRunnable?.let { removeCallbacks(it) }
        points.clear()
        isActive = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fadeRunnable?.let { removeCallbacks(it) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        // Draw trail with gradient opacity
        for (i in 1 until points.size) {
            val ratio = i.toFloat() / points.size
            val alpha = (ratio * 200).toInt().coerceIn(30, 200)
            val width = minStrokeWidthPx + ratio * strokeWidthRangePx

            fadePaint.alpha = alpha
            fadePaint.strokeWidth = width
            canvas.drawLine(
                points[i - 1].x, points[i - 1].y,
                points[i].x, points[i].y,
                fadePaint
            )
        }

        // Draw a dot at current position
        if (isActive && points.isNotEmpty()) {
            val last = points.last()
            canvas.drawCircle(last.x, last.y, dotRadiusPx, dotPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }
}
