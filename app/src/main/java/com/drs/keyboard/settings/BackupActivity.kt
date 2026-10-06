package com.drs.keyboard.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.engine.ShortcutEngine
import com.drs.keyboard.engine.UserLearner
import com.drs.keyboard.view.EmojiRepo
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup & restore: one JSON file carrying every setting, the learned-word
 * table, text shortcuts, emoji favorites/recents and the current glass theme.
 * Uses the Storage Access Framework — no storage permission, user picks where.
 */
class BackupActivity : Activity() {

    private lateinit var p: UiKit.Palette

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        window.statusBarColor = p.bg
        window.navigationBarColor = p.bg

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@BackupActivity, 18), UiKit.dp(this@BackupActivity, 14),
                UiKit.dp(this@BackupActivity, 18), UiKit.dp(this@BackupActivity, 30))
        }
        scroll.addView(root)
        setContentView(scroll)
        build(root)
    }

    private fun build(root: LinearLayout) {
        val c = this
        val tBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val tGreen = if (p.isDark) 0x2634D399 else 0x1A059669
        val tViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED

        root.addView(UiKit.screenHeader(c, p, getString(R.string.backup_title),
            getString(R.string.backup_sub), "shield", p.accent, tBlue))

        // ---- what's inside ----
        val info = UiKit.card(c, p)
        info.addView(UiKit.body(c, p, getString(R.string.backup_what)).apply {
            setPadding(0, 0, 0, UiKit.dp(c, 10))
        })
        for (line in listOf(
            getString(R.string.backup_item_prefs),
            getString(R.string.backup_item_words),
            getString(R.string.backup_item_shortcuts),
            getString(R.string.backup_item_emoji),
            getString(R.string.backup_item_theme)
        )) {
            info.addView(itemRow(line))
        }
        info.addView(UiKit.body(c, p, getString(R.string.backup_privacy_note)).apply {
            setTextColor(p.subtext)
            setPadding(0, UiKit.dp(c, 12), 0, 0)
        })
        root.addView(info, pad(0, 0, 0, UiKit.dp(c, 16)))

        // ---- export ----
        val export = UiKit.card(c, p)
        val exHead = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        exHead.addView(UiKit.iconTile(c, p, "check", p.green, tGreen, 44))
        exHead.addView(TextView(c).apply {
            text = getString(R.string.backup_export)
            textSize = 16.5f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(UiKit.dp(c, 14), 0, 0, 0)
        })
        export.addView(exHead)
        export.addView(UiKit.body(c, p, getString(R.string.backup_export_sub)).apply {
            setPadding(0, UiKit.dp(c, 10), 0, UiKit.dp(c, 14))
        })
        export.addView(UiKit.button(c, p, getString(R.string.backup_export_btn), true) {
            exportBackup()
        })
        root.addView(export, pad(0, 0, 0, UiKit.dp(c, 16)))

        // ---- import ----
        val import = UiKit.card(c, p)
        val imHead = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        imHead.addView(UiKit.iconTile(c, p, "spark", 0xFF8B5CF6.toInt(), tViolet, 44))
        imHead.addView(TextView(c).apply {
            text = getString(R.string.backup_import)
            textSize = 16.5f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(UiKit.dp(c, 14), 0, 0, 0)
        })
        import.addView(imHead)
        import.addView(UiKit.body(c, p, getString(R.string.backup_import_sub)).apply {
            setPadding(0, UiKit.dp(c, 10), 0, UiKit.dp(c, 14))
        })
        import.addView(UiKit.button(c, p, getString(R.string.backup_import_btn), false) {
            importBackup()
        })
        root.addView(import, pad(0, 0, 0, UiKit.dp(c, 16)))
    }

    private fun itemRow(text: String): View {
        val c = this
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, UiKit.dp(c, 5), 0, UiKit.dp(c, 5))
        }
        row.addView(View(c).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(c, 6), UiKit.dp(c, 6)).apply {
                setMargins(0, 0, UiKit.dp(c, 10), 0)
            }
            background = UiKit.rounded(p.accent, UiKit.dp(c, 3).toFloat())
        })
        row.addView(TextView(c).apply {
            setText(text)
            textSize = 13.5f
            setTextColor(p.text)
        })
        return row
    }

    private fun pad(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    private fun exportBackup() {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "drs-keyboard-backup-$stamp.json")
        }
        runCatching { startActivityForResult(intent, REQ_EXPORT) }.onFailure {
            Toast.makeText(this, R.string.backup_no_saf, Toast.LENGTH_LONG).show()
        }
    }

    private fun buildBackupJson(): JSONObject {
        val c = this
        val out = JSONObject()
        out.put("app", "DRS Keyboard")
        out.put("backup_version", 1)
        out.put("created", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))

        // every user setting
        val prefsJson = JSONObject()
        for ((k, v) in Prefs(c).exportAll()) {
            when (v) {
                is Boolean, is Int, is Long, is Float, is String -> prefsJson.put(k, v)
            }
        }
        out.put("prefs", prefsJson)

        // learned words
        val words = JSONObject()
        for ((w, n) in UserLearner.get(c).entries()) words.put(w, n)
        out.put("learned", words)

        // shortcuts
        val shorts = JSONObject()
        for ((k, v) in ShortcutEngine.get(c).also { it.ensureDefaults() }.all()) shorts.put(k, v)
        out.put("shortcuts", shorts)

        // emoji favorites + recents
        EmojiRepo.load(c)
        out.put("emoji_favorites", EmojiRepo.exportFavorites())
        out.put("emoji_recents", EmojiRepo.exportRecents())

        // current theme
        val themePrefs = getSharedPreferences("drs_theme", MODE_PRIVATE)
        out.put("theme", themePrefs.getString("current_theme_json", "") ?: "")
        return out
    }

    private fun writeBackupTo(uri: Uri): Boolean = runCatching {
        contentResolver.openOutputStream(uri, "wt")?.use { os ->
            os.write(buildBackupJson().toString().toByteArray(Charsets.UTF_8))
            os.flush()
            true
        } ?: false
    }.getOrDefault(false)

    // ------------------------------------------------------------------
    // Import
    // ------------------------------------------------------------------

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        runCatching { startActivityForResult(intent, REQ_IMPORT) }.onFailure {
            Toast.makeText(this, R.string.backup_no_saf, Toast.LENGTH_LONG).show()
        }
    }

    private fun readTextFrom(uri: Uri): String? = runCatching {
        contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { r ->
                r.readText()
            }
        }
    }.getOrNull()

    /** Applies a parsed backup. Returns a human-readable result line. */
    private fun applyBackup(obj: JSONObject): String {
        val c = this
        var words = 0; var shorts = 0; var favs = 0

        // 1) every user setting
        val prefsJson = obj.optJSONObject("prefs")
        if (prefsJson != null) {
            val map = HashMap<String, Any?>(prefsJson.length())
            for (k in prefsJson.keys()) {
                val v = prefsJson.get(k)
                if (v is Boolean || v is Int || v is Long || v is Float || v is String) {
                    map[k] = v
                }
            }
            Prefs(c).importAll(map)
        }

        // 2) learned words
        val learned = obj.optJSONObject("learned")
        if (learned != null) {
            val table = HashMap<String, Int>(learned.length())
            for (k in learned.keys()) {
                val n = learned.optInt(k, 0)
                if (n > 0) table[k] = n
            }
            UserLearner.get(c).replaceAll(table)
            words = table.size
        }

        // 3) shortcuts
        val shortsJson = obj.optJSONObject("shortcuts")
        if (shortsJson != null) {
            val engine = ShortcutEngine.get(c).also { it.ensureDefaults() }
            for (k in shortsJson.keys()) {
                val v = shortsJson.optString(k, "")
                if (v.isNotEmpty()) engine.put(k, v, persistNow = false)
            }
            engine.persist()
            shorts = shortsJson.length()
        }

        // 4) emoji favorites + recents
        EmojiRepo.load(c)
        val favStr = obj.optString("emoji_favorites", "")
        val recStr = obj.optString("emoji_recents", "")
        EmojiRepo.restore(c,
            favStr.split('\u0001').filter { it.isNotEmpty() },
            recStr.split('\u0001').filter { it.isNotEmpty() })
        favs = favStr.split('\u0001').count { it.isNotEmpty() }

        // 5) theme (validated before saving)
        val themeJson = obj.optString("theme", "")
        if (themeJson.isNotEmpty()) {
            val theme = com.drs.keyboard.theme.KeyboardTheme.fromJson(themeJson)
            if (theme != null) {
                getSharedPreferences("drs_theme", MODE_PRIVATE).edit()
                    .putString("current_theme_json", themeJson).apply()
            }
        }
        return getString(R.string.backup_done, words, shorts, favs)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        when (requestCode) {
            REQ_EXPORT -> {
                val ok = writeBackupTo(uri)
                Toast.makeText(this,
                    getString(if (ok) R.string.backup_saved else R.string.backup_failed),
                    Toast.LENGTH_LONG).show()
            }
            REQ_IMPORT -> {
                val text = readTextFrom(uri)
                val parsed = text?.let { runCatching { JSONObject(it) }.getOrNull() }
                val msg = if (parsed != null && parsed.optString("app") == "DRS Keyboard") {
                    runCatching { applyBackup(parsed) }.getOrElse { getString(R.string.backup_failed) }
                } else getString(R.string.backup_bad_file)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                if (parsed != null && parsed.optString("app") == "DRS Keyboard") {
                    // rebuild so import state is reflected
                    setContentView(buildRootAgain())
                }
            }
        }
    }

    private fun buildRootAgain(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@BackupActivity, 18), UiKit.dp(this@BackupActivity, 14),
                UiKit.dp(this@BackupActivity, 18), UiKit.dp(this@BackupActivity, 30))
        }
        scroll.addView(root)
        build(root)
        return scroll
    }

    companion object {
        private const val REQ_EXPORT = 41
        private const val REQ_IMPORT = 42
    }
}
