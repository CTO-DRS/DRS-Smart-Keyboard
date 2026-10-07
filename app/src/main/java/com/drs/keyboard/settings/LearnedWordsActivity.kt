package com.drs.keyboard.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.engine.UserLearner

/**
 * Learned-words manager: see every word the keyboard learned from your
 * typing, search it, remove one by one or wipe all — full auditability,
 * fully on-device.
 */
class LearnedWordsActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var learner: UserLearner
    private lateinit var adapter: WordsAdapter
    private lateinit var countView: TextView
    private lateinit var emptyView: TextView
    private lateinit var listCard: LinearLayout
    private lateinit var root: LinearLayout
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        learner = UserLearner.get(this)
        UiKit.applySystemBars(this, p)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(p.bg)
            setPadding(UiKit.dp(this@LearnedWordsActivity, 18), UiKit.dp(this@LearnedWordsActivity, 14),
                UiKit.dp(this@LearnedWordsActivity, 18), UiKit.dp(this@LearnedWordsActivity, 20))
        }
        setContentView(root)
        build()
    }

    private fun build() {
        val c = this
        val tViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED
        root.removeAllViews()

        root.addView(UiKit.screenHeader(c, p, getString(R.string.learned_title),
            getString(R.string.learned_sub), "book", 0xFF8B5CF6.toInt(), tViolet))

        // search + count
        val searchCard = UiKit.card(c, p)
        val field = EditText(c).apply {
            hint = getString(R.string.learned_search)
            textSize = 14f
            setTextColor(p.text)
            setHintTextColor(p.faint)
            setBackgroundResource(android.R.color.transparent)
            setPadding(0, UiKit.dp(c, 6), 0, UiKit.dp(c, 6))
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString()?.trim() ?: ""
                    adapter.refresh()
                }
            })
        }
        searchCard.addView(field)
        countView = UiKit.body(c, p, "").apply {
            setPadding(0, UiKit.dp(c, 4), 0, 0)
        }
        searchCard.addView(countView)
        root.addView(searchCard, pad(0, 0, 0, UiKit.dp(c, 12)))

        // list
        listCard = UiKit.card(c, p)
        adapter = WordsAdapter()
        val list = ListView(c).apply {
            adapter = this@LearnedWordsActivity.adapter
            divider = null
            selector = null
        }
        emptyView = TextView(c).apply {
            text = getString(R.string.learned_empty)
            setTextColor(p.subtext)
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(c, 24), 0, UiKit.dp(c, 24))
        }
        listCard.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 420)))
        list.emptyView = emptyView
        root.addView(listCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, UiKit.dp(c, 8))
        })
        root.addView(emptyView)

        // add a word manually (names, slang — autocorrect will respect it)
        root.addView(UiKit.button(c, p, getString(R.string.learned_add), false) { showAddDialog() })

        // bulk import: a plain .txt list, one word per line (optionally "word,count")
        root.addView(UiKit.button(c, p, getString(R.string.learned_import), false) { pickWordFile() })

        root.addView(UiKit.button(c, p, getString(R.string.learned_clear_all), false) {
            learner.clear()
            adapter.refresh()
            Toast.makeText(this, R.string.clear_learned_done, Toast.LENGTH_SHORT).show()
        })

        adapter.refresh()
    }

    private fun pickWordFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/csv", "application/octet-stream"))
        }
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.learned_import)), 5001
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 5001 || resultCode != RESULT_OK || data?.data == null) return
        val imported = importWordList(data.data!!)
        if (imported < 0) {
            Toast.makeText(this, R.string.learned_import_bad, Toast.LENGTH_SHORT).show()
        } else {
            adapter.refresh()
            Toast.makeText(this, getString(R.string.learned_imported, imported), Toast.LENGTH_SHORT).show()
        }
    }

    /** Parse "one word per line" text (word / word,count / word<TAB>count),
     *  merge into the learned dictionary with the given weight. Returns the
     *  number of NEW words added, or -1 when the file could not be read. */
    private fun importWordList(uri: Uri): Int {
        val lines = try {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.readLines() ?: return -1
        } catch (e: Exception) {
            return -1
        }
        var added = 0
        var dirty = false
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split(",", "\t", ";")
            val word = parts[0].trim()
            val count = if (parts.size > 1) {
                parts[1].trim().filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 99) ?: 1
            } else 1
            // sanity: 1–40 chars, no control characters, not pure punctuation
            if (word.length !in 1..40) continue
            if (word.any { it.code < 32 }) continue
            if (word.none { it.isLetterOrDigit() }) continue
            val before = learner.freqOf(word)
            learner.boost(word, count)
            dirty = true
            if (before == 0) added++
        }
        if (dirty) learner.persist()
        return added
    }

    /** Small dialog: type a word, it lands in the learned dictionary with a strong weight. */
    private fun showAddDialog() {
        val c = this
        val input = EditText(c).apply {
            hint = getString(R.string.learned_add_hint)
            textSize = 15f
            setTextColor(p.text)
            setHintTextColor(p.faint)
            setSingleLine()
        }
        val wrap = FrameLayout(c).apply {
            setPadding(UiKit.dp(c, 22), UiKit.dp(c, 10), UiKit.dp(c, 22), 0)
            addView(input)
        }
        AlertDialog.Builder(c)
            .setTitle(R.string.learned_add)
            .setMessage(R.string.learned_add_msg)
            .setView(wrap)
            .setPositiveButton(R.string.learned_add_ok) { _, _ ->
                val w = input.text.toString().trim()
                if (w.length in 2..24 && w.all { it.isLetter() }) {
                    learner.boost(w, 5)
                    learner.persist()
                    adapter.refresh()
                    Toast.makeText(c, getString(R.string.learned_added, w), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c, R.string.learned_add_invalid, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun entries(): List<Pair<String, Int>> {
        val all = learner.entries()
        if (query.isEmpty()) return all
        return all.filter { it.first.contains(query, ignoreCase = true) }
    }

    private fun pad(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }

    private inner class WordsAdapter : BaseAdapter() {
        private var rows: List<Pair<String, Int>> = emptyList()

        inner class Holder(val word: TextView, val count: TextView)

        fun refresh() {
            rows = entries()
            countView.text = getString(R.string.learned_count, rows.size)
            notifyDataSetChanged()
            listCard.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
            emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }

        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Pair<String, Int> = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val c = this@LearnedWordsActivity
            val (word, count) = rows[position]
            val row: LinearLayout
            val holder: Holder
            if (convertView is LinearLayout && convertView.tag is Holder) {
                row = convertView
                holder = convertView.tag as Holder
            } else {
                row = LinearLayout(c).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(UiKit.dp(c, 16), UiKit.dp(c, 11), UiKit.dp(c, 16), UiKit.dp(c, 11))
                    background = UiKit.ripple(c)
                    isClickable = true
                }
                val w = TextView(c).apply {
                    textSize = 15f
                    setTextColor(p.text)
                    layoutParams = LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val n = TextView(c).apply {
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setPadding(UiKit.dp(c, 8), 0, 0, 0)
                }
                row.addView(w)
                row.addView(n)
                holder = Holder(w, n)
                row.tag = holder
            }
            holder.word.text = word
            holder.count.text = "×$count"
            holder.count.setTextColor(if (count >= 5) p.green else p.faint)
            // tap = forget this word
            row.setOnClickListener {
                learner.remove(word)
                refresh()
            }
            return row
        }
    }
}
