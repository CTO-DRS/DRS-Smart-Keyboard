package com.drs.keyboard.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.drs.keyboard.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Main settings screen — Glass Depth 2.0. Gradient hero, live status pill,
 * smart 3-step wizard with completion checks, stat grid, icon navigation
 * cards and a privacy shield panel.
 */
class SettingsActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var root: LinearLayout
    private lateinit var pillHost: LinearLayout
    private lateinit var wizardStatus: TextView
    private var step1Done = false
    private var step2Done = false
    private val step3Done get() = Prefs(this).themeEditorJustSaved

    /** Counted once per activity life — shared by the stat grid and the privacy chip. */
    private val liveStatsCache: IntArray by lazy { liveStats() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        UiKit.applySystemBars(this, p)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@SettingsActivity, 18), UiKit.dp(this@SettingsActivity, 20),
                UiKit.dp(this@SettingsActivity, 18), UiKit.dp(this@SettingsActivity, 28))
        }
        scroll.addView(root)
        setContentView(scroll)
        buildUi(animate = true)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi(animate: Boolean) {
        val c = this
        val d = UiKit.dp(c, 14)

        buildHero()
        buildWizard()
        buildStats()
        buildFeatures()
        buildPrivacy()
        buildFooter()
        if (animate) {
            // staggered fade-and-rise — plays on first open only
            for (i in 0 until root.childCount) {
                UiKit.animateIn(root.getChildAt(i), 45L * i)
            }
        }
    }

    // ---------- hero ----------

    private fun buildHero() {
        val c = this
        val hero = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(c, 22), UiKit.dp(c, 22), UiKit.dp(c, 22), UiKit.dp(c, 22))
            background = UiKit.heroDrawable(c, p)
        }

        // top row: glyph + status pill
        val topRow = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val glyphHost = FrameLayout(c).apply {
            background = GradientDrawable().apply {
                cornerRadius = UiKit.dp(c, 18).toFloat()
                setColor(0x1FFFFFFF)
                setStroke(UiKit.dp(c, 1), 0x33FFFFFF)
            }
            setPadding(UiKit.dp(c, 9), UiKit.dp(c, 9), UiKit.dp(c, 9), UiKit.dp(c, 9))
        }
        glyphHost.addView(UiKit.glowGlyphView(c, p, 34))
        topRow.addView(glyphHost)

        pillHost = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.END
        }
        topRow.addView(pillHost)
        hero.addView(topRow)

        hero.addView(TextView(c).apply {
            text = getString(R.string.ime_name)
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, UiKit.dp(c, 16), 0, 0)
        })

        hero.addView(TextView(c).apply {
            text = "“" + getString(R.string.about_slogan) + "”"
            textSize = 13.5f
            setTextColor(0xD9FFFFFF.toInt())
            // synthetic italic slants Arabic glyphs awkwardly — keep upright in RTL
            if (!UiKit.isRtl(c)) typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            setPadding(0, UiKit.dp(c, 4), 0, 0)
        })

        hero.addView(UiKit.body(c, p, getString(R.string.setup_subtitle)).apply {
            setTextColor(0x99FFFFFF.toInt())
            setPadding(0, UiKit.dp(c, 10), 0, 0)
        })

        root.addView(hero, linear(UiKit.dp(c, 24)))
    }

    // ---------- wizard ----------

    private fun buildWizard() {
        val c = this

        root.addView(UiKit.sectionHeader(c, p, getString(R.string.section_setup).uppercase()),
            linear(UiKit.dp(c, 10)))

        val wizard = UiKit.card(c, p)
        wizardStatus = UiKit.body(c, p, "").apply {
            setPadding(0, 0, 0, UiKit.dp(c, 2))
        }
        wizard.addView(wizardStatus)

        wizard.addView(wizardStep("1", getString(R.string.step1_title), getString(R.string.step1_desc)) {
            runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        wizard.addView(wizardDivider())
        wizard.addView(wizardStep("2", getString(R.string.step2_title), getString(R.string.step2_desc)) {
            runCatching { inputMethodManager().showInputMethodPicker() }
        })
        wizard.addView(wizardDivider())
        wizard.addView(wizardStep("3", getString(R.string.step3_title), getString(R.string.step3_desc)) {
            startActivity(Intent(c, ThemeEditorActivity::class.java))
        })
        root.addView(wizard, linear(UiKit.dp(c, 24)))
    }

    private fun wizardStep(number: String, titleText: String, subText: String,
                           onClick: () -> Unit): LinearLayout {
        val c = this
        val done = when (number) {
            "1" -> step1Done
            "2" -> step2Done
            else -> step3Done
        }
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, UiKit.dp(c, 13), 0, UiKit.dp(c, 13))
            background = UiKit.ripple(c)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        row.addView(UiKit.stepBadge(c, p, number, done))
        val texts = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(UiKit.dp(c, 14), 0, UiKit.dp(c, 14), 0)
        }
        texts.addView(TextView(c).apply {
            text = if (done) "$titleText  ·  ${getString(R.string.setup_done)}" else titleText
            textSize = 15.5f
            setTextColor(if (done) p.green else p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        texts.addView(TextView(c).apply {
            text = subText
            textSize = 12.5f
            setTextColor(p.subtext)
            setPadding(0, UiKit.dp(c, 3), 0, 0)
        })
        row.addView(texts)
        row.addView(UiKit.chevron(c, p))
        return row
    }

    private fun wizardDivider(): View = UiKit.divider(this, p)

    // ---------- stats grid ----------

    private fun buildStats() {
        val c = this
        root.addView(UiKit.sectionHeader(c, p, getString(R.string.section_stats).uppercase()),
            linear(UiKit.dp(c, 10)))

        val grid = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        val cellLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val cardBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val cardViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED
        val cardTeal = if (p.isDark) 0x262DD4BF else 0x1A0D9488
        val cardAmber = if (p.isDark) 0x26F59E0B else 0x1AD97706

        // live inventory, computed from the real assets — never frozen marketing numbers
        val loc = Locale.getDefault()
        val fmt = { n: Int -> String.format(loc, "%,d", n) }
        val (words, emoji, themes, sensitive) = liveStatsCache
        row1.addView(UiKit.statCard(c, p, "book", fmt(words),
            getString(R.string.stats_words_label), p.accent, cardBlue), cellLp)
        row1.addView(space(10))
        row1.addView(UiKit.statCard(c, p, "emoji", fmt(emoji),
            getString(R.string.stats_emoji_label), 0xFFEC4899.toInt(), cardViolet), cellLp)
        row2.addView(UiKit.statCard(c, p, "palette", fmt(themes),
            getString(R.string.stats_themes_label), 0xFF14B8A6.toInt(), cardTeal), cellLp)
        row2.addView(space(10))
        row2.addView(UiKit.statCard(c, p, "shield", fmt(sensitive),
            getString(R.string.stats_permissions_label), 0xFFF59E0B.toInt(), cardAmber), cellLp)

        grid.addView(row1, linear(UiKit.dp(c, 10)))
        grid.addView(row2, linear(0))
        root.addView(grid, linear(UiKit.dp(c, 24)))
    }

    private fun space(w: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@SettingsActivity, w), 1)
    }

    /**
     * Real inventory, counted on-device from what the keyboard actually ships:
     *  - words  = entries in the bundled en + ar frequency dictionaries
     *  - emoji  = rows in the bundled emoji catalog
     *  - themes = presets currently registered in ThemeRepo
     *  - sensitive = requested permissions in the dangerous groups (always 0 here)
     * Everything is derived live so the numbers can never drift from reality.
     */
    private fun liveStats(): IntArray {
        var words = 0
        var emoji = 0
        runCatching {
            for (name in listOf("dict/en_freq.txt", "dict/ar_freq.txt")) {
                assets.open(name).use { s ->
                    BufferedReader(InputStreamReader(s, Charsets.UTF_8)).forEachLine { line ->
                        if (line.indexOf('\t') > 0) words++
                    }
                }
            }
            assets.open("dict/emoji.txt").use { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).forEachLine { line ->
                    if (line.indexOf('\t') > 0) emoji++
                }
            }
        }
        val themes = runCatching { com.drs.keyboard.theme.ThemeRepo.presets().size }.getOrDefault(16)
        var sensitive = 0
        runCatching {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val dangerous = setOf(
                "CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS",
                "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CALL_LOG", "WRITE_CALL_LOG",
                "CALL_PHONE", "READ_PHONE_STATE", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
                "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_PHONE_NUMBERS")
            for (perm in info.requestedPermissions ?: emptyArray()) {
                val bare = perm.substringAfterLast('.')
                if (bare in dangerous) sensitive++
            }
        }
        return intArrayOf(words, emoji, themes, sensitive)
    }

    // ---------- features ----------

    private fun buildFeatures() {
        val c = this
        root.addView(UiKit.sectionHeader(c, p, getString(R.string.section_features).uppercase()),
            linear(UiKit.dp(c, 10)))

        val card = UiKit.card(c, p)
        val tBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val tViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED
        val tTeal = if (p.isDark) 0x262DD4BF else 0x1A0D9488
        val tAmber = if (p.isDark) 0x26F59E0B else 0x1AD97706
        val tGreen = if (p.isDark) 0x2634D399 else 0x1A059669

        card.addView(UiKit.navRow(c, p, "globe", p.accent, tBlue,
            getString(R.string.languages_title), getString(R.string.feature_lang_sub)) {
            startActivity(Intent(c, LanguagesActivity::class.java))
        })
        card.addView(wizardDivider())
        card.addView(UiKit.navRow(c, p, "spark", 0xFF14B8A6.toInt(), tTeal,
            getString(R.string.stats_title), getString(R.string.feature_stats_sub)) {
            startActivity(Intent(c, StatsActivity::class.java))
        })
        card.addView(wizardDivider())
        card.addView(UiKit.navRow(c, p, "palette", 0xFF8B5CF6.toInt(), tViolet,
            getString(R.string.appearance_title), getString(R.string.feature_theme_sub)) {
            startActivity(Intent(c, ThemeEditorActivity::class.java))
        })
        card.addView(wizardDivider())
        card.addView(UiKit.navRow(c, p, "gear", 0xFF14B8A6.toInt(), tTeal,
            getString(R.string.typing_title), getString(R.string.feature_typing_sub)) {
            startActivity(Intent(c, TypingActivity::class.java))
        })
        card.addView(wizardDivider())
        card.addView(UiKit.navRow(c, p, "bolt", 0xFFF59E0B.toInt(), tAmber,
            getString(R.string.shortcuts_title), getString(R.string.feature_shortcuts_sub)) {
            startActivity(Intent(c, ShortcutsActivity::class.java))
        })
        card.addView(wizardDivider())
        card.addView(UiKit.navRow(c, p, "shield", p.green, tGreen,
            getString(R.string.backup_title), getString(R.string.feature_backup_sub)) {
            startActivity(Intent(c, BackupActivity::class.java))
        })
        root.addView(card, linear(UiKit.dp(c, 24)))
    }

    // ---------- privacy ----------

    private fun buildPrivacy() {
        val c = this
        root.addView(UiKit.sectionHeader(c, p, getString(R.string.section_privacy).uppercase()),
            linear(UiKit.dp(c, 10)))

        val card = UiKit.card(c, p)
        val top = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tGreen = if (p.isDark) 0x2634D399 else 0x1A059669
        top.addView(UiKit.iconTile(c, p, "shield", p.green, tGreen, 44))
        top.addView(TextView(c).apply {
            text = getString(R.string.privacy_title)
            textSize = 16.5f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(UiKit.dp(c, 14), 0, UiKit.dp(c, 14), 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(UiKit.chip(c, p,
            String.format(Locale.getDefault(), "%,d", liveStatsCache[3]), p.green))
        card.addView(top)

        card.addView(UiKit.body(c, p, getString(R.string.about_body)).apply {
            setPadding(0, UiKit.dp(c, 12), 0, 0)
        })

        val chips = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, UiKit.dp(c, 14), 0, 0)
        }
        chips.addView(UiKit.chip(c, p, getString(R.string.privacy_chip_offline), p.accent))
        chips.addView(space(8))
        chips.addView(UiKit.chip(c, p, getString(R.string.privacy_chip_telemetry), 0xFF8B5CF6.toInt()))
        chips.addView(space(8))
        chips.addView(UiKit.chip(c, p, getString(R.string.privacy_chip_permissions), p.green))
        card.addView(chips)
        root.addView(card, linear(UiKit.dp(c, 24)))
    }

    // ---------- footer ----------

    private fun buildFooter() {
        val c = this
        val footer = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(c, 6), 0, 0)
        }
        footer.addView(TextView(c).apply {
            text = "DRS · " + getString(R.string.about_version) + " " + versionName()
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(p.faint)
        })
        footer.addView(TextView(c).apply {
            text = getString(R.string.footer_made)
            textSize = 11.5f
            setTextColor(p.faint)
            setPadding(0, UiKit.dp(c, 4), 0, 0)
        })
        root.addView(footer)
    }

    // ---------- state ----------

    private fun inputMethodManager(): InputMethodManager =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    }.getOrDefault("")

    private fun refreshStatus() {
        val imm = inputMethodManager()
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val default = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        step1Done = enabled
        step2Done = default.contains(packageName)
        // full rebuild keeps every embedded state (pill, badges, status) in sync
        root.removeAllViews()
        buildUi(animate = false)
        renderStatus()
    }

    private fun renderStatus() {
        val active = step1Done && step2Done
        pillHost.addView(UiKit.statusPill(this, p,
            getString(if (active) R.string.hero_pill_active else R.string.hero_pill_setup), active))

        val allDone = step1Done && step2Done && step3Done
        wizardStatus.text = getString(when {
            allDone -> R.string.wizard_status_ready
            step2Done -> R.string.status_active
            else -> R.string.status_enabled
        })
        wizardStatus.setTextColor(if (step2Done) p.green else p.subtext)
        wizardStatus.visibility = if (step1Done) View.VISIBLE else View.GONE
    }

    private fun linear(bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, bottom)
        }
}
