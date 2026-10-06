package com.drs.keyboard.settings

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.engine.ShortcutEngine

/** Shortcuts manager: add / edit (tap) / delete (long-press) text expansions. */
class ShortcutsActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var engine: ShortcutEngine
    private lateinit var adapter: ShortsAdapter
    private lateinit var emptyView: TextView
    private lateinit var listCard: LinearLayout
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        engine = ShortcutEngine.get(this).also { it.ensureDefaults() }
        window.statusBarColor = p.bg
        window.navigationBarColor = p.bg

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(p.bg)
            setPadding(UiKit.dp(this@ShortcutsActivity, 18), UiKit.dp(this@ShortcutsActivity, 14),
                UiKit.dp(this@ShortcutsActivity, 18), UiKit.dp(this@ShortcutsActivity, 20))
        }

        root.addView(UiKit.screenHeader(this, p, getString(R.string.shortcuts_title),
            getString(R.string.shortcuts_hint), "bolt", 0xFFF59E0B.toInt(),
            if (p.isDark) 0x26F59E0B else 0x1AD97706))

        listCard = UiKit.card(this, p)
        adapter = ShortsAdapter()
        val list = ListView(this).apply {
            adapter = this@ShortcutsActivity.adapter
            divider = null
            selector = null
        }
        emptyView = TextView(this).apply {
            text = getString(R.string.shortcuts_empty)
            setTextColor(p.subtext)
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@ShortcutsActivity, 24), 0, UiKit.dp(this@ShortcutsActivity, 24))
        }
        listCard.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 380)))
        list.emptyView = emptyView
        root.addView(listCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, UiKit.dp(this@ShortcutsActivity, 16))
        })
        root.addView(emptyView)

        root.addView(UiKit.button(this, p, getString(R.string.shortcuts_add), true) {
            showEditor(null, null)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))

        setContentView(root)
    }

    private fun showEditor(short: String?, expansion: String?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@ShortcutsActivity, 22), UiKit.dp(this@ShortcutsActivity, 16),
                UiKit.dp(this@ShortcutsActivity, 22), 0)
        }
        val shortField = EditText(this).apply {
            hint = getString(R.string.shortcuts_short)
            setText(short ?: "")
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        val longField = EditText(this).apply {
            hint = getString(R.string.shortcuts_long)
            setText(expansion ?: "")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1
            maxLines = 4
        }
        container.addView(shortField)
        container.addView(longField)

        AlertDialog.Builder(this)
            .setTitle(if (short == null) R.string.shortcuts_add else R.string.shortcuts_title)
            .setView(container)
            .setPositiveButton(R.string.shortcuts_add_btn) { _, _ ->
                val s = shortField.text.toString().trim()
                val l = longField.text.toString().trim()
                if (s.isEmpty() || l.isEmpty()) return@setPositiveButton
                val existing = engine.all()
                if ((short == null || !s.equals(short, ignoreCase = true)) && existing.containsKey(s.lowercase())) {
                    Toast.makeText(this, R.string.shortcuts_duplicate, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                engine.put(s, l)
                adapter.refresh()
            }
            .setNegativeButton(R.string.picker_cancel, null)
            .show()
    }

    private inner class ShortsAdapter : BaseAdapter() {
        private var items: List<Pair<String, String>> = engine.all().map { it.key to it.value }

        fun refresh() {
            items = engine.all().map { it.key to it.value }
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Pair<String, String> = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val c = this@ShortcutsActivity
            val row = convertView as? LinearLayout ?: LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(UiKit.dp(c, 6), UiKit.dp(c, 10), UiKit.dp(c, 6), UiKit.dp(c, 10))

                // chip with the short form
                val shortChip = TextView(c).apply {
                    id = View.generateViewId()
                    textSize = 13.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    background = UiKit.rounded(
                        p.accent and 0x2EFFFFFF or (p.accent and 0xFF000000.toInt()),
                        UiKit.dp(c, 12).toFloat())
                    setTextColor(p.accent)
                }
                addView(shortChip, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(c, 38)))
                shortChip.minWidth = UiKit.dp(c, 74)

                val texts = LinearLayout(c).apply {
                    id = View.generateViewId()
                    orientation = LinearLayout.VERTICAL
                    setPadding(UiKit.dp(c, 12), 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val long = TextView(c).apply {
                    id = View.generateViewId()
                    textSize = 13.5f
                }
                texts.addView(long)
                addView(texts)

                // tap hint chevron
                val chev = object : View(c) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = p.faint
                        style = android.graphics.Paint.Style.STROKE
                    }
                    override fun onDraw(canvas: android.graphics.Canvas) {
                        val size = width * 0.55f
                        paint.strokeWidth = size * 0.12f
                        paint.strokeCap = android.graphics.Paint.Cap.ROUND
                        paint.strokeJoin = android.graphics.Paint.Join.ROUND
                        canvas.save()
                        if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
                            canvas.scale(-1f, 1f, width / 2f, height / 2f)
                        SettingsIcons.draw(canvas, "chevron", width / 2f, height / 2f, size, paint)
                        canvas.restore()
                    }
                }
                chev.layoutParams = LinearLayout.LayoutParams(UiKit.dp(c, 18), UiKit.dp(c, 18))
                addView(chev)
            }
            val shortChip = row.getChildAt(0) as TextView
            val texts = row.getChildAt(1) as LinearLayout
            val long = texts.getChildAt(0) as TextView
            shortChip.text = item.first
            long.text = "→  " + item.second
            long.setTextColor(p.text)
            row.background = UiKit.ripple(c)
            row.isClickable = true
            row.isFocusable = true
            row.setOnClickListener { showEditor(item.first, item.second) }
            row.setOnLongClickListener {
                AlertDialog.Builder(this@ShortcutsActivity)
                    .setMessage(item.first + " → " + item.second)
                    .setPositiveButton(R.string.shortcuts_delete) { _, _ ->
                        engine.remove(item.first)
                        adapter.refresh()
                    }
                    .setNegativeButton(R.string.picker_cancel, null)
                    .show()
                true
            }
            return row
        }
    }
}
