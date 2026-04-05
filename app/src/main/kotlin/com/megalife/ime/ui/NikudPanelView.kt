package com.megalife.ime.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.ime.feature.NikudManager

/**
 * Panel for selecting Hebrew nikud (vowel marks).
 * Shows all nikud characters in a grid with Hebrew names.
 */
class NikudPanelView(context: Context) : LinearLayout(context) {

    var onNikudSelected: ((Char) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private val theme = KeyboardTheme.getTheme(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.bgColor)

        // Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundColor(theme.keyColor)
        }

        val title = TextView(context).apply {
            text = "נקודות"  // "Nikud" in Hebrew
            setTextColor(theme.keyTextColor)
            textSize = 14f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(theme.accentColor)
            textSize = 18f
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
            setOnClickListener { onClose?.invoke() }
        }
        header.addView(closeBtn)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Nikud grid
        val recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = NikudGridAdapter { nikud ->
                onNikudSelected?.invoke(nikud)
            }
            clipToPadding = false
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(200)))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private class NikudGridAdapter(
        private val onSelected: (Char) -> Unit
    ) : RecyclerView.Adapter<NikudGridAdapter.ViewHolder>() {

        private val nikudItems = NikudManager.allNikud

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val size = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 60f, parent.resources.displayMetrics
                ).toInt()
                layoutParams = ViewGroup.LayoutParams(size, size)
                setBackgroundColor(KeyboardTheme.getTheme(parent.context).keyColor)
                setPadding(4, 4, 4, 4)
            }
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val nikud = nikudItems[position]
            val layout = holder.itemView as LinearLayout
            layout.removeAllViews()

            // Show the nikud on a sample consonant (bet)
            val nikudDisplay = TextView(layout.context).apply {
                text = "ב${nikud.unicode}"  // bet + nikud mark
                setTextColor(KeyboardTheme.getTheme(layout.context).keyTextColor)
                textSize = 22f
                gravity = Gravity.CENTER
            }
            layout.addView(nikudDisplay)

            // Hebrew name below
            val nameView = TextView(layout.context).apply {
                text = nikud.hebrewName
                setTextColor(Color.GRAY)
                textSize = 9f
                gravity = Gravity.CENTER
                maxLines = 1
            }
            layout.addView(nameView)

            holder.itemView.setOnClickListener { onSelected(nikud.unicode) }
        }

        override fun getItemCount() = nikudItems.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
