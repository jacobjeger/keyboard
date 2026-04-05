package com.megalife.ime.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.ime.R

/**
 * Manages the suggestion bar RecyclerView above the keyboard.
 * Shows word suggestions with DiffUtil for smooth updates.
 * First suggestion is bold (highest confidence).
 * Supports long-press to block a word from suggestions.
 */
class SuggestionBarView(
    private val recyclerView: RecyclerView,
    private val onSuggestionSelected: (String) -> Unit
) {
    var onSuggestionLongPress: ((String) -> Unit)? = null

    private val adapter = SuggestionAdapter(
        onSelected = onSuggestionSelected,
        onLongPress = { word -> onSuggestionLongPress?.invoke(word) }
    )

    init {
        recyclerView.layoutManager = LinearLayoutManager(
            recyclerView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null // No animation for speed
        recyclerView.setHasFixedSize(true)
    }

    fun updateSuggestions(suggestions: List<String>) {
        adapter.submitList(suggestions.take(5))
    }

    fun setVisible(visible: Boolean) {
        recyclerView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun clear() {
        adapter.submitList(emptyList())
    }

    private class SuggestionAdapter(
        private val onSelected: (String) -> Unit,
        private val onLongPress: (String) -> Unit
    ) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

        private var items: List<String> = emptyList()

        fun submitList(newItems: List<String>) {
            val oldItems = items
            items = newItems

            // Use DiffUtil for efficient updates
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos] == newItems[newPos]
                override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos] == newItems[newPos]
            })
            diff.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.suggestion_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val word = items[position]
            val displayWord = if (word.startsWith("→")) {
                // Spell check correction — show with indicator
                word.substring(1)
            } else {
                word
            }

            holder.textView.text = displayWord

            // Bold the first suggestion
            holder.textView.typeface = if (position == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            // Spell check corrections get accent color
            if (word.startsWith("→")) {
                holder.textView.setTextColor(KeyboardTheme.ACCENT_COLOR)
            } else {
                holder.textView.setTextColor(KeyboardTheme.getTheme(holder.itemView.context).keyTextColor)
            }

            holder.itemView.setOnClickListener { onSelected(displayWord) }
            holder.itemView.setOnLongClickListener {
                onLongPress(displayWord)
                true
            }
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.suggestion_text)
        }
    }
}
