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
import com.megalife.ime.feature.EmojiData
import com.megalife.ime.feature.EmojiManager
import com.megalife.ime.feature.KosherEmojiFilter

/**
 * Emoji keyboard panel with category tabs, search bar, grid of emojis,
 * and skin tone selector. Kosher-filtered.
 */
class EmojiKeyboardView(context: Context) : LinearLayout(context) {

    var onEmojiSelected: ((String) -> Unit)? = null
    var onBackToLetters: (() -> Unit)? = null

    private val emojiManager = EmojiManager(context)
    private val tabContainer: LinearLayout
    private val emojiGrid: RecyclerView
    private val searchField: EditText
    private val skinToneBar: LinearLayout
    private val adapter: EmojiGridAdapter
    private var currentCategory = EmojiData.EmojiCategory.SMILEYS
    private var isSearchMode = false

    init {
        orientation = VERTICAL
        val theme = KeyboardTheme.getTheme(context)
        setBackgroundColor(theme.bgColor)

        // Emoji grid adapter (init first so search can reference it)
        adapter = EmojiGridAdapter { emoji ->
            emojiManager.recordUsage(emoji)
            onEmojiSelected?.invoke(emoji)
        }

        // Search bar
        searchField = EditText(context).apply {
            hint = "\uD83D\uDD0D Search emoji..."
            setTextColor(theme.keyTextColor)
            setHintTextColor(theme.keyTextSecondary)
            textSize = 13f
            setBackgroundColor(theme.keySpecialColor)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            isSingleLine = true
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val query = s?.toString() ?: ""
                    if (query.isNotEmpty()) {
                        isSearchMode = true
                        val results = KosherEmojiFilter.filter(EmojiData.search(query))
                        adapter.submitList(results)
                    } else {
                        isSearchMode = false
                        selectCategory(currentCategory)
                    }
                }
            })
        }
        addView(searchField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Category tabs row
        tabContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
        }
        val tabScroller = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(theme.keyColor)
            addView(tabContainer)
        }
        addView(tabScroller, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(36)))

        // Emoji grid
        emojiGrid = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 8)
            this.adapter = this@EmojiKeyboardView.adapter
            setBackgroundColor(theme.bgColor)
            clipToPadding = false
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for speed
        }
        addView(emojiGrid, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(180)))

        // Skin tone selector bar
        skinToneBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            setBackgroundColor(theme.keyColor)
        }
        buildSkinToneBar()
        addView(skinToneBar, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(28)))

        buildCategoryTabs()
        selectCategory(EmojiData.EmojiCategory.SMILEYS)
    }

    private fun buildSkinToneBar() {
        val tones = listOf("\uD83D\uDC4B", "\uD83D\uDC4B\uD83C\uDFFB", "\uD83D\uDC4B\uD83C\uDFFC",
            "\uD83D\uDC4B\uD83C\uDFFD", "\uD83D\uDC4B\uD83C\uDFFE", "\uD83D\uDC4B\uD83C\uDFFF")
        val currentTone = emojiManager.getSkinTone()

        for ((i, tone) in tones.withIndex()) {
            val tv = TextView(context).apply {
                text = tone
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(dpToPx(6), 0, dpToPx(6), 0)
                alpha = if (i == currentTone) 1.0f else 0.4f
                setOnClickListener {
                    emojiManager.setSkinTone(i)
                    // Update highlight
                    for (j in 0 until skinToneBar.childCount) {
                        skinToneBar.getChildAt(j).alpha = if (j == i) 1.0f else 0.4f
                    }
                }
            }
            skinToneBar.addView(tv)
        }
    }

    private fun buildCategoryTabs() {
        addTab("ABC") { onBackToLetters?.invoke() }

        val categoryIcons = mapOf(
            EmojiData.EmojiCategory.RECENT to "\uD83D\uDD58",
            EmojiData.EmojiCategory.SMILEYS to "\uD83D\uDE00",
            EmojiData.EmojiCategory.PEOPLE to "\uD83D\uDC4B",
            EmojiData.EmojiCategory.NATURE to "\uD83C\uDF3F",
            EmojiData.EmojiCategory.FOOD to "\uD83C\uDF54",
            EmojiData.EmojiCategory.TRAVEL to "\uD83D\uDE97",
            EmojiData.EmojiCategory.OBJECTS to "\uD83D\uDCA1",
            EmojiData.EmojiCategory.SYMBOLS to "\u2764\uFE0F"
        )

        for (category in EmojiData.EmojiCategory.entries) {
            val icon = categoryIcons[category] ?: category.name.take(2)
            addTab(icon) { selectCategory(category) }
        }
    }

    private fun addTab(label: String, onClick: () -> Unit) {
        val tab = TextView(context).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
            setOnClickListener {
                searchField.setText("")
                onClick()
            }
        }
        tabContainer.addView(tab)
    }

    private fun selectCategory(category: EmojiData.EmojiCategory) {
        currentCategory = category
        val emojis = KosherEmojiFilter.filter(emojiManager.getForCategory(category))
        adapter.submitList(emojis)
        emojiGrid.scrollToPosition(0)

        for (i in 1 until tabContainer.childCount) {
            val tab = tabContainer.getChildAt(i) as? TextView ?: continue
            val tabCategory = EmojiData.EmojiCategory.entries.getOrNull(i - 1)
            tab.alpha = if (tabCategory == category) 1.0f else 0.5f
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private class EmojiGridAdapter(
        private val onSelected: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiGridAdapter.ViewHolder>() {

        private var items: List<EmojiData.Emoji> = emptyList()

        fun submitList(newItems: List<EmojiData.Emoji>) {
            val oldItems = items
            items = newItems
            val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos].emoji == newItems[newPos].emoji
                override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldItems[oldPos] == newItems[newPos]
            })
            diff.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                val size = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics
                ).toInt()
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emoji = items[position]
            (holder.itemView as TextView).text = emoji.emoji
            holder.itemView.setOnClickListener { onSelected(emoji.emoji) }
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
