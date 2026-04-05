package com.megalife.ime.settings

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.megalife.ime.feature.TypingStats

/**
 * Activity showing weekly typing statistics in a simple programmatic layout.
 */
class TypingStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stats = TypingStats(this)
        val todayStats = stats.getDailyStats()
        val weeklyStats = stats.getWeeklyStats()

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title
        root.addView(TextView(this).apply {
            text = "Typing Statistics"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        })

        // Today section
        root.addView(sectionHeader("Today"))
        root.addView(statRow("Words typed", todayStats.wordsTyped.toString()))
        root.addView(statRow("Characters typed", todayStats.charsTyped.toString()))
        root.addView(statRow("Corrections applied", todayStats.correctionsApplied.toString()))
        root.addView(statRow("Emojis used", todayStats.emojisUsed.toString()))

        // Spacer
        root.addView(TextView(this).apply {
            setPadding(0, dp(16), 0, 0)
        })

        // Weekly section
        root.addView(sectionHeader("Last 7 Days"))

        // Table header
        root.addView(tableRow("Date", "Words", "Chars", isHeader = true))

        // Table rows
        for (day in weeklyStats) {
            val dateLabel = if (day.date == todayStats.date) "${day.date} (today)" else day.date
            root.addView(tableRow(dateLabel, day.wordsTyped.toString(), day.charsTyped.toString()))
        }

        scrollView.addView(root)
        setContentView(scrollView)
    }

    private fun sectionHeader(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun statRow(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            addView(TextView(context).apply {
                this.text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                this.text = value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    private fun tableRow(col1: String, col2: String, col3: String, isHeader: Boolean = false): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            if (isHeader) {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
            }

            val style = if (isHeader) Typeface.BOLD else Typeface.NORMAL

            addView(TextView(context).apply {
                text = col1
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, style)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            })

            addView(TextView(context).apply {
                text = col2
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, style)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                text = col3
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, style)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
