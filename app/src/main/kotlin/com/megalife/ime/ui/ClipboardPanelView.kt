package com.megalife.ime.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.ime.db.entity.ClipboardItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clipboard history panel. Shows recent clipboard items.
 * Tap to paste, long-press to pin/delete.
 */
class ClipboardPanelView(context: Context) : LinearLayout(context) {

    var onPaste: ((String) -> Unit)? = null
    var onPin: ((Long) -> Unit)? = null
    var onDelete: ((Long) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private val adapter: ClipboardAdapter
    private val recyclerView: RecyclerView
    private val emptyText: TextView
    private var allItems: List<ClipboardItem> = emptyList()

    private val theme = KeyboardTheme.getTheme(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.bgColor)

        // Header with search
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundColor(theme.keyColor)
        }

        val title = TextView(context).apply {
            text = "Clipboard"
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

        // Search field
        val searchField = EditText(context).apply {
            hint = "Search clipboard..."
            setTextColor(theme.keyTextColor)
            setHintTextColor(Color.GRAY)
            textSize = 13f
            setBackgroundColor(theme.keySpecialColor)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            isSingleLine = true
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    filterItems(s?.toString() ?: "")
                }
            })
        }
        addView(searchField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Empty state
        emptyText = TextView(context).apply {
            text = "No clipboard items"
            setTextColor(Color.GRAY)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
            visibility = View.GONE
        }
        addView(emptyText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // List
        adapter = ClipboardAdapter(
            onPaste = { item -> onPaste?.invoke(item.clipText) },
            onPin = { item -> onPin?.invoke(item.id) },
            onDelete = { item -> onDelete?.invoke(item.id) }
        )
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@ClipboardPanelView.adapter
            clipToPadding = false
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(200)))
    }

    fun setItems(items: List<ClipboardItem>) {
        allItems = items
        adapter.submitList(items)
        emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun filterItems(query: String) {
        if (query.isEmpty()) {
            adapter.submitList(allItems)
            emptyText.visibility = if (allItems.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (allItems.isEmpty()) View.GONE else View.VISIBLE
        } else {
            val filtered = allItems.filter { it.clipText.contains(query, ignoreCase = true) }
            adapter.submitList(filtered)
            emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private class ClipboardAdapter(
        private val onPaste: (ClipboardItem) -> Unit,
        private val onPin: (ClipboardItem) -> Unit,
        private val onDelete: (ClipboardItem) -> Unit
    ) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

        private var items: List<ClipboardItem> = emptyList()
        private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun submitList(newItems: List<ClipboardItem>) {
            val oldItems = items
            items = newItems
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos].id == newItems[newPos].id
                override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos] == newItems[newPos]
            })
            diff.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(parent.context, 12), dpToPx(parent.context, 8),
                    dpToPx(parent.context, 12), dpToPx(parent.context, 8))
                setBackgroundColor(KeyboardTheme.getTheme(parent.context).keyColor)
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dpToPx(parent.context, 4)
                layoutParams = lp
            }

            val pinIcon = TextView(parent.context).apply {
                textSize = 14f
                setPadding(0, 0, dpToPx(parent.context, 8), 0)
                tag = "pin"
            }
            layout.addView(pinIcon)

            val textView = TextView(parent.context).apply {
                setTextColor(KeyboardTheme.getTheme(parent.context).keyTextColor)
                textSize = 13f
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                tag = "text"
            }
            layout.addView(textView)

            val timeView = TextView(parent.context).apply {
                setTextColor(Color.GRAY)
                textSize = 11f
                setPadding(dpToPx(parent.context, 8), 0, 0, 0)
                tag = "time"
            }
            layout.addView(timeView)

            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val layout = holder.itemView as LinearLayout

            val pinIcon = layout.findViewWithTag<TextView>("pin")
            pinIcon.text = if (item.isPinned) "\uD83D\uDCCC" else ""

            val textView = layout.findViewWithTag<TextView>("text")
            textView.text = item.clipText.take(100)

            val timeView = layout.findViewWithTag<TextView>("time")
            timeView.text = dateFormat.format(Date(item.copiedAt))

            holder.itemView.setOnClickListener { onPaste(item) }
            holder.itemView.setOnLongClickListener {
                if (item.isPinned) onDelete(item) else onPin(item)
                true
            }
        }

        override fun getItemCount() = items.size

        private fun dpToPx(context: Context, dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics
            ).toInt()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
