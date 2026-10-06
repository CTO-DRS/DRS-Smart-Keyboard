package com.drs.keyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.drs.keyboard.theme.KeyboardTheme

/**
 * Emoji picker: category tabs + scrollable grid + recents row.
 * Pure framework widgets, themed with glass tokens.
 */
class EmojiPanel(context: Context) : LinearLayout(context) {

    interface Callback {
        fun onEmojiPicked(emoji: String)
        fun onDismiss()
    }

    var callback: Callback? = null

    private var theme: KeyboardTheme? = null
    private lateinit var tabsScroll: HorizontalScrollView
    private lateinit var tabsRow: LinearLayout
    private lateinit var gridScroll: ScrollView
    private lateinit var grid: GridLayout
    private val tabViews = HashMap<String, TextView>()

    private val categoryLabels = mapOf(
        "favorites" to "⭐", "recent" to "🕘", "smileys" to "😀", "people" to "👋",
        "animals" to "🐶", "food" to "🍔", "activity" to "⚽", "travel" to "🚗",
        "objects" to "💡", "symbols" to "❤️"
    )

    private var current = "smileys"
    private var searching = false
    private var suppressWatcher = false
    private lateinit var searchBox: EditText
    private lateinit var searchShell: FrameLayout

    init {
        // load the catalog + persisted recents/favorites (idempotent, cheap)
        EmojiRepo.load(context)
        orientation = VERTICAL
        tabsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        tabsRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        tabsScroll.addView(tabsRow)
        buildTabs()
        gridScroll = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        grid = GridLayout(context).apply { columnCount = 8 }
        gridScroll.addView(grid)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val close = TextView(context).apply {
                text = "✕"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setOnClickListener { callback?.onDismiss() }
            }
            addView(close)
            val title = TextView(context).apply {
                text = context.getString(com.drs.keyboard.R.string.emoji_title)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(4), dp(10), dp(10), dp(10))
            }
            addView(title)
        }
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        buildSearchRow()
        addView(tabsScroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(gridScroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        select(current)
    }

    /** Glass search field: offline EN+AR keyword lookup across the catalog. */
    private fun buildSearchRow() {
        val c = context
        searchShell = FrameLayout(c).apply {
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        searchBox = EditText(c).apply {
            hint = c.getString(com.drs.keyboard.R.string.emoji_search_hint)
            textSize = 14f
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (suppressWatcher) return
                    filter(s?.toString() ?: "")
                }
            })
        }
        searchShell.addView(searchBox, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        addView(searchShell, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    /** Switch the grid between category mode and search results. */
    private fun filter(query: String) {
        val t = theme
        if (query.isBlank()) {
            if (searching) {
                searching = false
                select(current)
            }
            return
        }
        searching = true
        for ((_, tv) in tabViews) tv.background = null
        grid.removeAllViews()
        val results = EmojiRepo.search(query)
        if (results.isEmpty()) {
            val empty = TextView(context).apply {
                text = context.getString(com.drs.keyboard.R.string.emoji_no_results)
                textSize = 13f
                setTextColor(t?.keyTextColor ?: Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(30), 0, dp(30))
            }
            grid.addView(empty, GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(0, 8)
            })
            return
        }
        for (em in results) grid.addView(makeCell(em, fromFavorites = false), cellParams())
    }

    fun setTheme(t: KeyboardTheme) {
        theme = t
        setBackgroundColor(Color.TRANSPARENT)
        val d = resources.displayMetrics.density
        background = GradientDrawable().apply {
            setColor(t.bgColor)
            cornerRadius = t.cornerRadiusDp * d
        }
        for ((_, tv) in tabViews) tv.setTextColor(t.keyTextColor)
        // restyle the search field
        searchBox.setTextColor(t.keyTextColor)
        val hintAlpha = (t.keyTextColor ushr 24) * 45 / 100
        searchBox.setHintTextColor((hintAlpha shl 24) or (t.keyTextColor and 0x00FFFFFF))
        searchBox.background = GradientDrawable().apply {
            setColor(t.candidateBg)
            cornerRadius = 14 * d
        }
        select(current)
    }

    /** Rebuilds the tab row; hides favorites/recents tabs when empty. */
    private fun buildTabs() {
        tabsRow.removeAllViews()
        tabViews.clear()
        for (cat in EmojiRepo.CATEGORY_ORDER) {
            if (cat == "recent" && !EmojiRepo.hasRecents()) continue
            if (cat == "favorites" && !EmojiRepo.hasFavorites()) continue
            val tv = TextView(context).apply {
                text = categoryLabels[cat] ?: cat
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener { select(cat) }
            }
            tabViews[cat] = tv
            tabsRow.addView(tv)
        }
    }

    private fun select(cat: String) {
        val t = theme
        // dynamic categories can empty out — fall back and drop their tab
        var target = cat
        if (target == "favorites" && !EmojiRepo.hasFavorites()) target = "smileys"
        if (target == "recent" && !EmojiRepo.hasRecents()) target = "smileys"
        current = target
        // picking a category ends search mode
        if (searchBox.text.isNotEmpty()) {
            suppressWatcher = true
            searchBox.setText("")
            suppressWatcher = false
        }
        searching = false
        if ((tabViews.containsKey("favorites")) != EmojiRepo.hasFavorites() ||
            (tabViews.containsKey("recent")) != EmojiRepo.hasRecents()) {
            buildTabs()
        }
        for ((c, tv) in tabViews) {
            tv.background = if (c == target) {
                GradientDrawable().apply {
                    setColor(t?.pressColor ?: 0)
                    cornerRadius = 12 * resources.displayMetrics.density
                }
            } else null
        }
        grid.removeAllViews()
        val d = resources.displayMetrics.density
        val emojis = EmojiRepo.category(target)
        if (emojis.isEmpty() && target == "recent") {
            val empty = TextView(context).apply {
                text = context.getString(com.drs.keyboard.R.string.emoji_none)
                textSize = 13f
                setTextColor(t?.keyTextColor ?: Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(30), 0, dp(30))
            }
            grid.addView(empty, GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(0, 8)
            })
        }
        for (em in emojis) grid.addView(makeCell(em, target == "favorites"), cellParams())
    }

    private fun cellParams(): GridLayout.LayoutParams = GridLayout.LayoutParams().apply {
        width = dp(42)
        height = dp(46)
    }

    /** One tappable emoji cell; long-press toggles the favorite star. */
    private fun makeCell(em: String, fromFavorites: Boolean): TextView {
        return TextView(context).apply {
            text = em
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(4), dp(2), dp(4))
            setOnClickListener {
                EmojiRepo.recordRecent(context, em)
                callback?.onEmojiPicked(em)
            }
            markFavorite(this, EmojiRepo.isFavorite(em))
            setOnLongClickListener {
                val nowFav = EmojiRepo.toggleFavorite(context, em)
                markFavorite(this, nowFav)
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                if (fromFavorites && !nowFav) {
                    // removed from the favorites tab: rebuild the grid
                    select("favorites")
                }
                true
            }
        }
    }

    /** Favorites wear a soft accent-tinted rounded badge. */
    private fun markFavorite(cell: TextView, fav: Boolean) {
        val t = theme
        cell.background = if (fav) {
            GradientDrawable().apply {
                setColor(t?.pressColor ?: 0)
                cornerRadius = 10 * resources.displayMetrics.density
            }
        } else null
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
