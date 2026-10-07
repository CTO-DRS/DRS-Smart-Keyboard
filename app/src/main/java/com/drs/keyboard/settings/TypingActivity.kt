package com.drs.keyboard.settings

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.engine.UserLearner

/** Typing behavior switches + learned-word management, grouped in icon sections. */
class TypingActivity : Activity() {

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
            setPadding(UiKit.dp(this@TypingActivity, 18), UiKit.dp(this@TypingActivity, 14),
                UiKit.dp(this@TypingActivity, 18), UiKit.dp(this@TypingActivity, 30))
        }
        scroll.addView(root)
        setContentView(scroll)
        build()
    }

    /** Section header inside a card: icon tile + bold label. */
    private fun sectionLabel(card: LinearLayout, icon: String, tint: Int, tileColor: Int,
                             label: String) {
        val c = this
        val head = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(c, 4))
        }
        head.addView(UiKit.iconTile(c, p, icon, tint, tileColor, 36))
        head.addView(TextView(c).apply {
            text = label
            textSize = 15f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(UiKit.dp(c, 12), 0, 0, 0)
        })
        card.addView(head)
    }

    private fun build() {
        val c = this
        val tBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val tTeal = if (p.isDark) 0x262DD4BF else 0x1A0D9488
        val tViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED
        val tPink = if (p.isDark) 0x26EC4899 else 0x1ADB2777

        root.addView(UiKit.screenHeader(this, p, getString(R.string.typing_title),
            getString(R.string.feature_typing_sub), "gear", 0xFF14B8A6.toInt(), tTeal))

        // ---- smart engine ----
        val smart = UiKit.card(c, p)
        sectionLabel(smart, "spark", p.accent, tBlue, getString(R.string.section_smart))
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_autocorrect),
            getString(R.string.pref_autocorrect_sub), prefs.autocorrect) { prefs.autocorrect = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_nextword),
            getString(R.string.pref_nextword_sub), prefs.nextWord) { prefs.nextWord = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_swipe),
            getString(R.string.pref_swipe_sub), prefs.swipe) { prefs.swipe = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_shortcuts),
            getString(R.string.pref_shortcuts_sub), prefs.shortcuts) { prefs.shortcuts = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_learning),
            getString(R.string.pref_learning_sub), prefs.learning) { prefs.learning = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_perapp),
            getString(R.string.pref_perapp_sub), prefs.perAppLang) { prefs.perAppLang = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_emoji_suggest),
            getString(R.string.pref_emoji_suggest_sub), prefs.emojiSuggest) { prefs.emojiSuggest = it })
        smart.addView(divider())
        smart.addView(UiKit.switchRow(c, p, getString(R.string.pref_incognito),
            getString(R.string.pref_incognito_sub), prefs.incognito) { prefs.incognito = it })
        root.addView(smart, margins(0, 0, 0, UiKit.dp(c, 16)))

        // ---- layout & feel ----
        val feel = UiKit.card(c, p)
        sectionLabel(feel, "keyboard", 0xFF14B8A6.toInt(), tTeal, getString(R.string.section_feel))
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_autocap),
            getString(R.string.pref_autocap_sub), prefs.autoCap) { prefs.autoCap = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_double_space),
            getString(R.string.pref_double_space_sub), prefs.doubleSpace) { prefs.doubleSpace = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_autospace),
            getString(R.string.pref_autospace_sub), prefs.autoSpacePunct) { prefs.autoSpacePunct = it })
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_spacesnap),
            getString(R.string.pref_spacesnap_sub), prefs.spaceSnap) { prefs.spaceSnap = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_number_row),
            getString(R.string.pref_number_row_sub), prefs.numberRow) { prefs.numberRow = it })
        feel.addView(divider())
        feel.addView(segmentedRow(getString(R.string.pref_height), getString(R.string.pref_height_sub),
            listOf(getString(R.string.height_compact), getString(R.string.height_normal),
                getString(R.string.height_tall)), prefs.keyboardHeight) {
            prefs.keyboardHeight = it
        })
        feel.addView(divider())
        feel.addView(segmentedRow(getString(R.string.pref_longpress), getString(R.string.pref_longpress_sub),
            listOf(getString(R.string.lp_fast), getString(R.string.lp_normal),
                getString(R.string.lp_slow)), longPressIndex()) {
            prefs.longPressMs = longPressValue(it)
        })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_arabic_digits),
            getString(R.string.pref_arabic_digits_sub), prefs.arabicDigits) { prefs.arabicDigits = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_cursor),
            getString(R.string.pref_cursor_sub), prefs.cursorControl) { prefs.cursorControl = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_key_popup),
            getString(R.string.pref_key_popup_sub), prefs.keyPopup) { prefs.keyPopup = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_arrow_row),
            getString(R.string.pref_arrow_row_sub), prefs.arrowRow) { prefs.arrowRow = it })
        feel.addView(divider())
        feel.addView(UiKit.switchRow(c, p, getString(R.string.pref_tashkeel),
            getString(R.string.pref_tashkeel_sub), prefs.tashkeelRow) { prefs.tashkeelRow = it })
        feel.addView(divider())
        feel.addView(segmentedRow(getString(R.string.pref_label_size),
            getString(R.string.pref_label_size_sub),
            listOf(getString(R.string.lbl_small), getString(R.string.lbl_normal),
                getString(R.string.lbl_large)), prefs.keyTextScale) {
            prefs.keyTextScale = it
        })
        root.addView(feel, margins(0, 0, 0, UiKit.dp(c, 16)))

        // ---- feedback ----
        val feedback = UiKit.card(c, p)
        sectionLabel(feedback, "emoji", 0xFFEC4899.toInt(), tPink, getString(R.string.section_feedback))
        feedback.addView(UiKit.switchRow(c, p, getString(R.string.pref_sound),
            "", prefs.sound) { prefs.sound = it })
        feedback.addView(divider())
        feedback.addView(UiKit.switchRow(c, p, getString(R.string.pref_vibrate),
            "", prefs.vibrate) { prefs.vibrate = it })
        feedback.addView(divider())
        feedback.addView(segmentedRow(getString(R.string.pref_vib_strength),
            getString(R.string.pref_vib_strength_sub),
            listOf(getString(R.string.vib_light), getString(R.string.vib_normal),
                getString(R.string.vib_strong)), prefs.vibrateStrength) {
            prefs.vibrateStrength = it
        })
        feedback.addView(divider())
        feedback.addView(segmentedRow(getString(R.string.pref_sound_style),
            getString(R.string.pref_sound_style_sub),
            listOf(getString(R.string.snd_soft), getString(R.string.snd_normal),
                getString(R.string.snd_clear)), prefs.soundStyle) {
            prefs.soundStyle = it
        })
        feedback.addView(divider())
        feedback.addView(UiKit.switchRow(c, p, getString(R.string.clipboard_enable),
            getString(R.string.clipboard_enable_sub), prefs.clipboardEnabled) { prefs.clipboardEnabled = it })
        feedback.addView(divider())
        feedback.addView(UiKit.switchRow(c, p, getString(R.string.pref_clip_autoclear),
            getString(R.string.pref_clip_autoclear_sub), prefs.clipAutoClear) { prefs.clipAutoClear = it })
        root.addView(feedback, margins(0, 0, 0, UiKit.dp(c, 16)))

        // ---- learned data ----
        val privacy = UiKit.card(c, p)
        sectionLabel(privacy, "trash", 0xFF8B5CF6.toInt(), tViolet, getString(R.string.clear_learned))
        privacy.addView(UiKit.body(c, p, getString(R.string.pref_learning_sub)).apply {
            setPadding(0, 0, 0, UiKit.dp(this@TypingActivity, 12))
        })
        privacy.addView(UiKit.button(c, p, getString(R.string.clear_learned), false) {
            UserLearner.get(this).clear()
            Toast.makeText(this, R.string.clear_learned_done, Toast.LENGTH_SHORT).show()
        })
        privacy.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@TypingActivity, 10))
        })
        privacy.addView(UiKit.button(c, p, getString(R.string.learned_manage), false) {
            startActivity(android.content.Intent(this, LearnedWordsActivity::class.java))
        })
        root.addView(privacy, margins(0, 0, 0, UiKit.dp(c, 14)))
    }

    private fun divider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@TypingActivity, 1))
        setBackgroundColor(p.cardStroke)
    }

    // ---- segmented control ------------------------------------------------

    private fun longPressIndex(): Int = when (prefs.longPressMs) {
        250 -> 0
        550 -> 2
        else -> 1
    }

    private fun longPressValue(i: Int): Int = when (i) {
        0 -> 250
        2 -> 550
        else -> 380
    }

    /** Title + subtitle + a row of pill options; the selected pill is accent-filled. */
    private fun segmentedRow(title: String, sub: String, options: List<String>,
                             selectedIndex: Int, onPick: (Int) -> Unit): View {
        val c = this
        val box = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, UiKit.dp(c, 12), 0, UiKit.dp(c, 12))
        }
        box.addView(TextView(c).apply {
            text = title
            textSize = 15f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        box.addView(TextView(c).apply {
            text = sub
            textSize = 12.5f
            setTextColor(p.subtext)
            setPadding(0, UiKit.dp(c, 2), 0, UiKit.dp(c, 10))
        })
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        options.forEachIndexed { i, label ->
            val pill = TextView(c).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(c, 9), 0, UiKit.dp(c, 9))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                applyPillStyle(this, i == selectedIndex)
                background = pillBackground(i == selectedIndex)
                setOnClickListener {
                    onPick(i)
                    // restyle every sibling pill
                    for (j in 0 until row.childCount) {
                        val v = row.getChildAt(j) as TextView
                        val sel = j == i
                        v.background = pillBackground(sel)
                        applyPillStyle(v, sel)
                    }
                }
            }
            row.addView(pill, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, if (i < options.size - 1) UiKit.dp(c, 7) else 0, 0)
            })
        }
        box.addView(row)
        return box
    }

    private fun pillBackground(selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = UiKit.dp(this@TypingActivity, 13).toFloat()
            setColor(if (selected) p.accent
            else 0x14000000 or (p.text and 0x00FFFFFF))
        }

    private fun applyPillStyle(tv: TextView, selected: Boolean) {
        tv.setTextColor(if (selected) 0xFFFFFFFF.toInt() else p.subtext)
        tv.alpha = if (selected) 1f else 0.92f
    }

    private fun margins(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }
}
