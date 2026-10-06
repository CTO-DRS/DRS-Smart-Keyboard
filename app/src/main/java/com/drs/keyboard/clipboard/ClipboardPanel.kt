package com.drs.keyboard.clipboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.theme.KeyboardTheme

/**
 * Clipboard hub panel: pinned items first, then history. Tap = paste,
 * long-press = pin / unpin / delete dialog. Fully local, nothing synced.
 */
class ClipboardPanel(context: Context, private val history: ClipboardHistory) :
    LinearLayout(context) {

    interface Callback {
        fun onPaste(text: String)
        fun onDismiss()
    }

    var callback: Callback? = null
    private var theme: KeyboardTheme? = null
    private val list = ListView(context)
    private val adapter = ClipsAdapter()
    private val emptyView = TextView(context)

    init {
        orientation = VERTICAL
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
        }
        val title = TextView(context).apply {
            text = context.getString(R.string.clipboard_panel_title)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)
        val clear = TextView(context).apply {
            text = context.getString(R.string.clipboard_clear_all)
            textSize = 13f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                history.clearAll()
                Toast.makeText(context, R.string.clipboard_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(clear)
        val close = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { callback?.onDismiss() }
        }
        header.addView(close)

        emptyView.text = context.getString(R.string.clipboard_empty)
        emptyView.gravity = Gravity.CENTER
        emptyView.setPadding(dp(20), dp(40), dp(20), dp(40))

        list.adapter = adapter
        list.divider = null
        list.choiceMode = ListView.CHOICE_MODE_NONE
        list.emptyView = emptyView

        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(emptyView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(list, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        history.addListener(object : ClipboardHistory.Listener {
            override fun onChanged() {
                post { adapter.refresh() }
            }
        })
    }

    fun setTheme(t: KeyboardTheme) {
        theme = t
        val d = resources.displayMetrics.density
        background = GradientDrawable().apply {
            setColor(t.bgColor)
            cornerRadius = t.cornerRadiusDp * d
        }
        adapter.applyTheme()
    }

    fun refresh() = adapter.refresh()

    private inner class ClipsAdapter : BaseAdapter() {
        private var items: List<ClipboardHistory.Item> = history.all()

        fun applyTheme() {
            notifyDataSetChanged()
        }

        fun refresh() {
            items = history.all()
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): ClipboardHistory.Item = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val t = theme
            val d = resources.displayMetrics.density
            val row = convertView as? LinearLayout ?: LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                val pin = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 13f
                    setPadding(0, 0, dp(8), 0)
                }
                addView(pin, LinearLayout.LayoutParams(dp(18), LayoutParams.WRAP_CONTENT))
                val text = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 14f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                }
                addView(text, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                val del = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 14f
                    setPadding(dp(10), 0, 0, 0)
                }
                addView(del, LinearLayout.LayoutParams(dp(30), LayoutParams.WRAP_CONTENT))
            }
            val pin = row.getChildAt(0) as TextView
            val text = row.getChildAt(1) as TextView
            val del = row.getChildAt(2) as TextView
            pin.text = if (item.pinned) "📌" else ""
            text.text = item.text
            text.setTextColor(t?.keyTextColor ?: Color.WHITE)
            pin.setTextColor(t?.accentColor ?: Color.WHITE)
            del.text = "✕"
            del.setTextColor(t?.keyTextColor ?: Color.WHITE)
            del.alpha = 0.6f
            row.background = GradientDrawable().apply {
                setColor(t?.pressColor ?: 0)
                cornerRadius = 12 * d
            }
            (row.layoutParams as? MarginLayoutParams)?.let {
                it.setMargins(dp(8), dp(3), dp(8), dp(3))
            }
            row.setOnClickListener { callback?.onPaste(item.text) }
            row.setOnLongClickListener {
                history.setPinned(item.text, !item.pinned)
                true
            }
            del.setOnClickListener { history.remove(item.text) }
            return row
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
