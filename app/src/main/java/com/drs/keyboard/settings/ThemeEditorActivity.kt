package com.drs.keyboard.settings

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.drs.keyboard.R
import com.drs.keyboard.keyboard.KeyboardState
import com.drs.keyboard.keyboard.Layouts
import com.drs.keyboard.theme.KeyboardTheme
import com.drs.keyboard.theme.ThemeRepo
import com.drs.keyboard.view.DrsKeyboardHost
import java.io.File

/**
 * Theme packages + the full editor. The top shows a live, interactive
 * preview keyboard rendered with the exact same views the IME uses; below
 * sit package presets and every color/size/effect control.
 */
class ThemeEditorActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var root: LinearLayout
    private lateinit var previewHost: FrameLayout
    private lateinit var previewKeyboard: DrsKeyboardHost
    private var theme: KeyboardTheme = ThemeRepo.presets().first()
    private var applyingProgrammatically = false

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, ThemeEditorActivity::class.java))
        }

        private const val PICK_IMAGE = 4001
        private const val EXPORT_THEME = 4002
        private const val IMPORT_THEME = 4003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        UiKit.applySystemBars(this, p)
        theme = ThemeRepo.loadCurrent(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@ThemeEditorActivity, 18), UiKit.dp(this@ThemeEditorActivity, 14),
                UiKit.dp(this@ThemeEditorActivity, 18), UiKit.dp(this@ThemeEditorActivity, 30))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(UiKit.screenHeader(this, p, getString(R.string.theme_editor),
            getString(R.string.themes_hint), "palette", 0xFF8B5CF6.toInt(),
            if (p.isDark) 0x268B5CF6 else 0x1A7C3AED))

        buildPreview()
        buildPackages()
        buildNightMode()
        buildEditor()
        refreshPreview()
    }

    private fun linearMargins(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }

    // ------------------------------------------------------------------
    // Live preview
    // ------------------------------------------------------------------

    private fun buildPreview() {
        val card = UiKit.card(this, p)
        card.addView(UiKit.title(this, p, getString(R.string.editor_preview)))
        previewHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, UiKit.dp(this@ThemeEditorActivity, 10), 0, 0)
            }
        }
        val state = KeyboardState().apply {
            enabledLangs = Prefs(this@ThemeEditorActivity).enabledLangs()
            buildAdjacency()
        }
        previewKeyboard = DrsKeyboardHost(this)
        previewKeyboard.state = state
        previewKeyboard.refreshLayout()
        previewHost.addView(previewKeyboard)
        card.addView(previewHost)
        root.addView(card, linearMargins(0, 0, 0, UiKit.dp(this, 14)))
    }

    private fun refreshPreview() {
        previewKeyboard.applyTheme(theme)
    }

    // ------------------------------------------------------------------
    // Packages
    // ------------------------------------------------------------------

    private fun buildPackages() {
        val card = UiKit.card(this, p)
        card.addView(UiKit.title(this, p, getString(R.string.themes_title)))
        card.addView(UiKit.body(this, p, getString(R.string.themes_hint)).apply {
            setPadding(0, UiKit.dp(this@ThemeEditorActivity, 4), 0, UiKit.dp(this@ThemeEditorActivity, 8))
        })
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var rowLL: LinearLayout? = null
        ThemeRepo.presets().forEachIndexed { i, preset ->
            if (i % 4 == 0) {
                rowLL = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                grid.addView(rowLL)
            }
            val cell = UiKit.accentChip(this, p, preset.bgColor, preset.name.substringBefore(' '))
            cell.setOnClickListener {
                // adopt the package wholesale, keep a chosen background image
                theme = preset.copy(bgImagePath = theme.bgImagePath)
                refreshPreview()
                persist()
            }
            cell.layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            rowLL?.addView(cell)
        }
        card.addView(grid)
        root.addView(card, linearMargins(0, 0, 0, UiKit.dp(this, 14)))
    }

    // ------------------------------------------------------------------
    // Auto night mode (19:00–06:00)
    // ------------------------------------------------------------------

    private lateinit var nightStatus: TextView

    private fun buildNightMode() {
        val c = this
        val prefs = Prefs(this)
        val card = UiKit.card(c, p)
        card.addView(UiKit.title(c, p, getString(R.string.night_title)))
        card.addView(UiKit.switchRow(c, p, getString(R.string.pref_autonight),
            getString(R.string.pref_autonight_sub), prefs.autoNight) {
            prefs.autoNight = it
            refreshNightStatus()
        })
        // what flips night mode: the clock (19:00–06:00) or the system dark mode
        card.addView(nightSourceRow(prefs))
        nightStatus = UiKit.body(c, p, "").apply {
            setPadding(0, UiKit.dp(c, 6), 0, UiKit.dp(c, 10))
        }
        card.addView(nightStatus)
        card.addView(UiKit.button(c, p, getString(R.string.night_save_btn), false) {
            ThemeRepo.saveNight(c, theme)
            refreshNightStatus()
            Toast.makeText(c, getString(R.string.night_saved, theme.name), Toast.LENGTH_SHORT).show()
        })
        refreshNightStatus()
        root.addView(card, linearMargins(0, 0, 0, UiKit.dp(this, 14)))
    }

    /** Two pills choosing the night trigger: clock schedule or system dark mode. */
    private fun nightSourceRow(prefs: Prefs): View {
        val c = this
        val labels = listOf(getString(R.string.ns_clock), getString(R.string.ns_system))
        val title = TextView(c).apply {
            text = getString(R.string.night_source)
            textSize = 15f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, UiKit.dp(c, 10), 0, UiKit.dp(c, 4))
        }
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEachIndexed { i, label ->
            val pill = TextView(c).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(c, 9), 0, UiKit.dp(c, 9))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, if (i == 0) UiKit.dp(c, 7) else 0, 0)
                }
                setOnClickListener {
                    prefs.nightSource = i
                    for (k in 0 until row.childCount) {
                        val tp = row.getChildAt(k) as TextView
                        val sel = k == i
                        tp.background = GradientDrawable().apply {
                            cornerRadius = UiKit.dp(c, 13).toFloat()
                            setColor(if (sel) p.accent
                            else 0x14000000 or (p.text and 0x00FFFFFF))
                        }
                        tp.setTextColor(if (sel) 0xFFFFFFFF.toInt() else p.subtext)
                    }
                    refreshNightStatus()
                }
            }
            // apply the persisted selection's look
            val sel = i == prefs.nightSource.coerceIn(0, 1)
            pill.background = GradientDrawable().apply {
                cornerRadius = UiKit.dp(c, 13).toFloat()
                setColor(if (sel) p.accent else 0x14000000 or (p.text and 0x00FFFFFF))
            }
            pill.setTextColor(if (sel) 0xFFFFFFFF.toInt() else p.subtext)
            row.addView(pill)
        }
        return LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(row) }
    }

    private fun refreshNightStatus() {
        val name = ThemeRepo.nightThemeName(this)
        nightStatus.text = if (name != null)
            getString(R.string.night_status_set, name)
        else getString(R.string.night_status_none)
    }

    // ------------------------------------------------------------------
    // Editor controls
    // ------------------------------------------------------------------

    private lateinit var controls: LinearLayout

    private fun buildEditor() {
        val card = UiKit.card(this, p)
        card.addView(UiKit.title(this, p, getString(R.string.theme_editor)))
        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, UiKit.dp(this@ThemeEditorActivity, 8), 0, 0)
        }
        card.addView(controls)

        fun colorRow(labelRes: Int, get: () -> Int, set: (Int) -> Unit) {
            controls.addView(UiKit.row(this, p, getString(labelRes), "") {
                ColorPickerDialog(this, get()) { c ->
                    set(c)
                    refreshPreview()
                    persist()
                }.show()
            })
        }

        colorRow(R.string.editor_background, { theme.bgColor }, { theme.bgColor = it })
        colorRow(R.string.editor_key, { theme.keyColor }, { theme.keyColor = it })
        colorRow(R.string.editor_special_key, { theme.specialKeyColor }, { theme.specialKeyColor = it })
        colorRow(R.string.editor_key_text, { theme.keyTextColor }, { theme.keyTextColor = it })
        colorRow(R.string.editor_accent, { theme.accentColor }, { theme.accentColor = it })
        colorRow(R.string.editor_press, { theme.pressColor }, { theme.pressColor = it })
        colorRow(R.string.editor_stroke, { theme.strokeColor }, { theme.strokeColor = it })

        controls.addView(sliderRow(R.string.editor_radius, 4, 28, theme.cornerRadiusDp) {
            theme.cornerRadiusDp = it
        })
        controls.addView(sliderRow(R.string.editor_key_height, 42, 68, theme.keyHeightDp) {
            theme.keyHeightDp = it
        })
        controls.addView(sliderRow(R.string.editor_opacity, 40, 100, theme.opacityPct) {
            theme.opacityPct = it
            theme.bgColor = (theme.bgColor and 0x00FFFFFF) or
                    ((it * 255 / 100) shl 24)
        })

        // glass switch
        controls.addView(UiKit.switchRow(this, p, getString(R.string.editor_glass),
            getString(R.string.editor_glass_sub), theme.glass) {
            theme.glass = it
            refreshPreview()
            persist()
        })

        // background image
        val imgRow = UiKit.row(this, p, getString(R.string.editor_pick_image),
            getString(R.string.editor_bg_image)) {
            runCatching {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(Intent.createChooser(intent, null), PICK_IMAGE)
            }.onFailure {
                Toast.makeText(this, it.message ?: "error", Toast.LENGTH_SHORT).show()
            }
        }
        controls.addView(imgRow)
        val remove = UiKit.row(this, p, getString(R.string.editor_remove_image), "") {
            theme.bgImagePath = null
            refreshPreview()
            persist()
        }
        controls.addView(remove)

        // ---- share: export / import the theme as a JSON file (SAF, no storage permission)
        controls.addView(UiKit.row(this, p, getString(R.string.theme_export),
            getString(R.string.theme_export_sub)) {
            runCatching {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    val safeName = theme.name.replace(" ", "_")
                    putExtra(Intent.EXTRA_TITLE, "DRS-theme-$safeName.json")
                }
                startActivityForResult(intent, EXPORT_THEME)
            }.onFailure {
                Toast.makeText(this, it.message ?: "error", Toast.LENGTH_SHORT).show()
            }
        })
        controls.addView(UiKit.row(this, p, getString(R.string.theme_import),
            getString(R.string.theme_import_sub)) {
            runCatching {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/json", "text/plain", "application/octet-stream"))
                }
                startActivityForResult(intent, IMPORT_THEME)
            }.onFailure {
                Toast.makeText(this, it.message ?: "error", Toast.LENGTH_SHORT).show()
            }
        })

        // ---- generate a matching glass theme from the system wallpaper -------
        controls.addView(UiKit.row(this, p, getString(R.string.theme_wallpaper),
            getString(R.string.theme_wallpaper_sub)) {
            applyWallpaperColors()
        })

        // reset
        val reset = UiKit.button(this, p, getString(R.string.editor_reset), false) {
            val preset = ThemeRepo.presets().firstOrNull { it.id == theme.id } ?: ThemeRepo.presets().first()
            val keepImage = theme.bgImagePath
            theme = preset.copy(bgImagePath = keepImage)
            syncEditorControls()
            refreshPreview()
            persist()
        }
        val wrap = FrameLayout(this)
        wrap.addView(reset, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        controls.addView(wrap, linearMargins(0, UiKit.dp(this, 6), 0, 0))

        root.addView(card, linearMargins(0, 0, 0, UiKit.dp(this, 14)))
    }

    /**
     * Builds a glass theme whose accent comes from the system wallpaper
     * (WallpaperColors — offline, no permission, API 27+). The panel goes
     * dark or light depending on the wallpaper's own luminance.
     */
    private fun applyWallpaperColors() {
        if (Build.VERSION.SDK_INT < 27) {
            Toast.makeText(this, getString(R.string.wallpaper_unsupported), Toast.LENGTH_SHORT).show()
            return
        }
        val colors = runCatching {
            WallpaperManager.getInstance(this)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()
        if (colors == null) {
            Toast.makeText(this, getString(R.string.wallpaper_fail), Toast.LENGTH_SHORT).show()
            return
        }
        val primary = colors.primaryColor.toArgb()
        val secondary = colors.secondaryColor?.toArgb() ?: primary
        val r = Color.red(primary) / 255f
        val g = Color.green(primary) / 255f
        val b = Color.blue(primary) / 255f
        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val dark = lum < 0.55f

        fun mix(c1: Int, c2: Int, ratio: Float): Int {
            val nr = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio).toInt()
            val ng = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio).toInt()
            val nb = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio).toInt()
            return Color.argb(255, nr, ng, nb)
        }

        val bg = if (dark) mix(primary, Color.argb(255, 14, 16, 24), 0.78f)
        else mix(primary, Color.argb(255, 243, 244, 248), 0.80f)
        val key = if (dark) 0x2EFFFFFF else 0x2E1A2032.toInt()
        val special = if (dark) 0x1F000000 else 0x1F1A2032.toInt()
        val text = if (dark) 0xF2FFFFFF.toInt() else 0xF21A2032.toInt()
        val stroke = if (dark) 0x26FFFFFF else 0x261A2032.toInt()
        val cand = if (dark) 0x26FFFFFF else 0x261A2032.toInt()
        val press = (primary and 0x00FFFFFF) or 0x3D000000

        theme = KeyboardTheme(
            id = "wallpaper-${System.currentTimeMillis()}",
            name = getString(R.string.wallpaper_name),
            bgColor = (bg and 0x00FFFFFF) or ((theme.opacityPct.coerceIn(40, 100) * 255 / 100) shl 24),
            keyColor = key,
            specialKeyColor = special,
            keyTextColor = text,
            accentColor = primary,
            pressColor = press,
            strokeColor = stroke,
            candidateBg = cand,
            cornerRadiusDp = theme.cornerRadiusDp,
            keyHeightDp = theme.keyHeightDp,
            opacityPct = theme.opacityPct,
            glass = true,
            bgImagePath = theme.bgImagePath
        ).apply {
            // keep a whisper of the secondary color in the special keys
            specialKeyColor = (secondary and 0x00FFFFFF) or (special and 0xFF000000.toInt())
        }
        syncEditorControls()
        refreshPreview()
        persist()
        Toast.makeText(this, getString(R.string.wallpaper_done), Toast.LENGTH_SHORT).show()
    }

    private fun sliderRow(labelRes: Int, minV: Int, maxV: Int, initial: Int,
                          onChange: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, UiKit.dp(this@ThemeEditorActivity, 10), 0, 0)
        }
        val label = TextView(this).apply {
            text = getString(labelRes) + "  ·  $initial"
            textSize = 15f
            setTextColor(p.text)
        }
        row.addView(label)
        val seek = SeekBar(this).apply {
            max = maxV - minV
            progress = initial - minV
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val v = progress + minV
                    label.text = getString(labelRes) + "  ·  $v"
                    onChange(v)
                    refreshPreview()
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    persist()
                }
            })
        }
        row.addView(seek)
        return row
    }

    private fun syncEditorControls() {
        // simplest correct approach: rebuild the editor section values by
        // re-rendering the preview; sliders keep their positions because the
        // theme object is mutated in place elsewhere
        refreshPreview()
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    private fun persist() {
        ThemeRepo.saveCurrent(this, theme)
        Prefs(this).themeEditorJustSaved = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                // copy into private storage so the IME can read it forever
                val dst = File(filesDir, "theme_bg_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    dst.outputStream().use { output -> input.copyTo(output) }
                }
                theme.bgImagePath = dst.absolutePath
                refreshPreview()
                persist()
                Toast.makeText(this, getString(R.string.editor_saved), Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (requestCode == EXPORT_THEME && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                // the background image is a private path — never travels with the file
                val exported = theme.copy(bgImagePath = null).toJson()
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(exported.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                Toast.makeText(this, getString(R.string.theme_exported), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, it.message ?: "error", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (requestCode == IMPORT_THEME && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                val text = contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: ""
                val imported = KeyboardTheme.fromJson(text)
                if (imported == null) {
                    Toast.makeText(this, getString(R.string.theme_import_bad), Toast.LENGTH_LONG).show()
                    return
                }
                // keep the current background image if one was picked locally
                imported.bgImagePath = theme.bgImagePath
                theme = imported
                refreshPreview()
                persist()
                Toast.makeText(this, getString(R.string.theme_imported, theme.name),
                    Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, it.message ?: "error", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }
}
