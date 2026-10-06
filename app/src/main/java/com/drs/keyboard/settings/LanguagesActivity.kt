package com.drs.keyboard.settings

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R

/** Language pack manager: enable/disable English + Arabic layouts. */
class LanguagesActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var root: LinearLayout
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        prefs = Prefs(this)
        window.statusBarColor = p.bg
        window.navigationBarColor = p.bg

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@LanguagesActivity, 18), UiKit.dp(this@LanguagesActivity, 14),
                UiKit.dp(this@LanguagesActivity, 18), UiKit.dp(this@LanguagesActivity, 30))
        }
        scroll.addView(root)
        setContentView(scroll)
        build()
    }

    private fun build() {
        val c = this
        root.addView(UiKit.screenHeader(this, p, getString(R.string.languages_title),
            getString(R.string.lang_hint), "globe", p.accent,
            if (p.isDark) 0x264D8DFF else 0x1A2E6BFF))

        root.addView(langCard(
            "EN", getString(R.string.lang_english),
            if (prefs.langEnglish) getString(R.string.lang_enabled) else getString(R.string.lang_disabled),
            prefs.langEnglish, p.accent) { on ->
            if (!on && !prefs.langArabic) {
                prefs.langEnglish = true
                Toast.makeText(this, R.string.lang_hint, Toast.LENGTH_SHORT).show()
                recreate()
                return@langCard
            }
            prefs.langEnglish = on
            recreate()
        })

        root.addView(langCard(
            "ع", getString(R.string.lang_arabic),
            if (prefs.langArabic) getString(R.string.lang_enabled) else getString(R.string.lang_disabled),
            prefs.langArabic, 0xFF14B8A6.toInt()) { on ->
            if (!on && !prefs.langEnglish) {
                prefs.langArabic = true
                Toast.makeText(this, R.string.lang_hint, Toast.LENGTH_SHORT).show()
                recreate()
                return@langCard
            }
            prefs.langArabic = on
            recreate()
        })

        // hint card
        val hint = UiKit.card(c, p)
        val hintRow = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hintRow.addView(UiKit.iconTile(c, p, "spark", 0xFFF59E0B.toInt(),
            if (p.isDark) 0x26F59E0B else 0x1AD97706, 36))
        hintRow.addView(UiKit.body(c, p, getString(R.string.lang_hint)).apply {
            setPadding(UiKit.dp(c, 12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        hint.addView(hintRow)
        root.addView(hint, margins(0, UiKit.dp(c, 16), 0, 0))
    }

    /** Premium language card: script-glyph medallion + name + status dot + switch. */
    private fun langCard(glyph: String, name: String, statusText: String,
                         enabled: Boolean, tint: Int, onToggle: (Boolean) -> Unit): View {
        val c = this
        val r = UiKit.dp(c, 22).toFloat()
        val card = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(c, 18), UiKit.dp(c, 16), UiKit.dp(c, 18), UiKit.dp(c, 16))
            background = UiKit.pressed(c, r)
            isClickable = true
            isFocusable = true
            setOnClickListener { onToggle(!enabled) }
        }

        // script glyph medallion
        val medallion = TextView(c).apply {
            text = glyph
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tint)
            gravity = Gravity.CENTER
            background = UiKit.rounded(tint and 0x2EFFFFFF or (tint and 0xFF000000.toInt()),
                UiKit.dp(c, 17).toFloat())
        }
        card.addView(medallion, LinearLayout.LayoutParams(UiKit.dp(c, 56), UiKit.dp(c, 56)))

        val texts = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(UiKit.dp(c, 15), 0, UiKit.dp(c, 15), 0)
        }
        texts.addView(TextView(c).apply {
            text = name
            textSize = 16f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        texts.addView(TextView(c).apply {
            text = (if (enabled) "● " else "○ ") + statusText
            textSize = 12.5f
            setTextColor(if (enabled) p.green else p.subtext)
            setPadding(0, UiKit.dp(c, 3), 0, 0)
        })
        card.addView(texts)

        val sw = Switch(c).apply {
            isChecked = enabled
            trackDrawable?.setTint(if (enabled) tint else p.faint)
            setOnCheckedChangeListener { _, checked -> onToggle(checked) }
        }
        card.addView(sw)
        return card
    }

    private fun margins(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }
}
