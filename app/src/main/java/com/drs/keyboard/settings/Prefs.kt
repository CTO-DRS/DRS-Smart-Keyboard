package com.drs.keyboard.settings

import android.content.Context

/** All user-tunable behavior flags, persisted locally. */
class Prefs(context: Context) {

    private val prefs =
        context.getSharedPreferences("drs_prefs", Context.MODE_PRIVATE)

    var autocorrect: Boolean
        get() = prefs.getBoolean("autocorrect", true)
        set(v) = prefs.edit().putBoolean("autocorrect", v).apply()

    var nextWord: Boolean
        get() = prefs.getBoolean("nextword", true)
        set(v) = prefs.edit().putBoolean("nextword", v).apply()

    var swipe: Boolean
        get() = prefs.getBoolean("swipe", true)
        set(v) = prefs.edit().putBoolean("swipe", v).apply()

    var shortcuts: Boolean
        get() = prefs.getBoolean("shortcuts", true)
        set(v) = prefs.edit().putBoolean("shortcuts", v).apply()

    var autoCap: Boolean
        get() = prefs.getBoolean("autocap", true)
        set(v) = prefs.edit().putBoolean("autocap", v).apply()

    var doubleSpace: Boolean
        get() = prefs.getBoolean("doublespace", true)
        set(v) = prefs.edit().putBoolean("doublespace", v).apply()

    /** Insert a space automatically after sentence punctuation (off by default). */
    var autoSpacePunct: Boolean
        get() = prefs.getBoolean("auto_space_punct", false)
        set(v) = prefs.edit().putBoolean("auto_space_punct", v).apply()

    var numberRow: Boolean
        get() = prefs.getBoolean("numberrow", false)
        set(v) = prefs.edit().putBoolean("numberrow", v).apply()

    var sound: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.edit().putBoolean("sound", v).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(v) = prefs.edit().putBoolean("vibrate", v).apply()

    var learning: Boolean
        get() = prefs.getBoolean("learning", true)
        set(v) = prefs.edit().putBoolean("learning", v).apply()

    var cursorControl: Boolean
        get() = prefs.getBoolean("cursor", true)
        set(v) = prefs.edit().putBoolean("cursor", v).apply()

    var clipboardEnabled: Boolean
        get() = prefs.getBoolean("clipboard", true)
        set(v) = prefs.edit().putBoolean("clipboard", v).apply()

    var langEnglish: Boolean
        get() = prefs.getBoolean("lang_en", true)
        set(v) = prefs.edit().putBoolean("lang_en", v).apply()

    var langArabic: Boolean
        get() = prefs.getBoolean("lang_ar", true)
        set(v) = prefs.edit().putBoolean("lang_ar", v).apply()

    var lastLang: String
        get() = prefs.getString("last_lang", null) ?: "en"
        set(v) = prefs.edit().putString("last_lang", v).apply()

    var themeEditorJustSaved: Boolean
        get() = prefs.getBoolean("theme_dirty", false)
        set(v) = prefs.edit().putBoolean("theme_dirty", v).apply()

    /** Keyboard height: 0 = compact, 1 = normal, 2 = tall (scales theme key height). */
    var keyboardHeight: Int
        get() = prefs.getInt("kb_height", 1)
        set(v) = prefs.edit().putInt("kb_height", v).apply()

    /** Long-press delay before alt-popup fires, in ms (250 / 380 / 550). */
    var longPressMs: Int
        get() = prefs.getInt("longpress_ms", 380)
        set(v) = prefs.edit().putInt("longpress_ms", v).apply()

    /** Use Arabic-Indic digits (٠١٢٣…) on the Arabic keyboard. */
    var arabicDigits: Boolean
        get() = prefs.getBoolean("arabic_digits", false)
        set(v) = prefs.edit().putBoolean("arabic_digits", v).apply()

    /** Persistent geometry mode: 0 = full, 1 = one-hand left, 2 = one-hand right, 3 = floating. */
    var uiMode: Int
        get() = prefs.getInt("ui_mode", 0)
        set(v) = prefs.edit().putInt("ui_mode", v).apply()

    /** Remember the last-used language per app and auto-switch on return. */
    var perAppLang: Boolean
        get() = prefs.getBoolean("per_app_lang", true)
        set(v) = prefs.edit().putBoolean("per_app_lang", v).apply()

    /** Serialized per-app language table: "pkg1=en;pkg2=ar". */
    var appLangMap: String
        get() = prefs.getString("app_lang_map", null) ?: ""
        set(v) = prefs.edit().putString("app_lang_map", v).apply()

    /** Vibration strength: 0 = light, 1 = normal, 2 = strong (VibrationEffect amplitude). */
    var vibrateStrength: Int
        get() = prefs.getInt("vibrate_strength", 1)
        set(v) = prefs.edit().putInt("vibrate_strength", v).apply()

    /** Auto-switch to the night theme between 19:00 and 06:00. */
    var autoNight: Boolean
        get() = prefs.getBoolean("auto_night", false)
        set(v) = prefs.edit().putBoolean("auto_night", v).apply()

    /** What triggers night mode: 0 = clock (19:00–06:00), 1 = follow system dark mode. */
    var nightSource: Int
        get() = prefs.getInt("night_source", 0)
        set(v) = prefs.edit().putInt("night_source", v).apply()

    /** Suggest a matching emoji chip while typing known words. */
    var emojiSuggest: Boolean
        get() = prefs.getBoolean("emoji_suggest", true)
        set(v) = prefs.edit().putBoolean("emoji_suggest", v).apply()

    /** Enlarged preview bubble above the key being touched. */
    var keyPopup: Boolean
        get() = prefs.getBoolean("key_popup", true)
        set(v) = prefs.edit().putBoolean("key_popup", v).apply()

    /** Show a navigation arrows row (← ↑ ↓ →) above the letters. */
    var arrowRow: Boolean
        get() = prefs.getBoolean("arrow_row", false)
        set(v) = prefs.edit().putBoolean("arrow_row", v).apply()

    /** Show the Arabic diacritics (tashkeel) row in Arabic alpha mode. */
    var tashkeelRow: Boolean
        get() = prefs.getBoolean("tashkeel_row", false)
        set(v) = prefs.edit().putBoolean("tashkeel_row", v).apply()

    /** Key label text scale: 0 = small (0.85x), 1 = normal, 2 = large (1.2x). */
    var keyTextScale: Int
        get() = prefs.getInt("key_text_scale", 1)
        set(v) = prefs.edit().putInt("key_text_scale", v).apply()

    /** Raw dump of drs_prefs for backup/restore. */
    fun exportAll(): MutableMap<String, *> = prefs.all

    /** Bulk-restore of drs_prefs (used by backup import). */
    fun importAll(data: Map<String, *>) {
        val e = prefs.edit()
        e.clear()
        for ((k, v) in data) {
            when (v) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Float -> e.putFloat(k, v)
                is String -> e.putString(k, v)
            }
        }
        e.apply()
    }

    fun enabledLangs(): List<String> {
        val out = ArrayList<String>(2)
        if (langEnglish) out.add("en")
        if (langArabic) out.add("ar")
        if (out.isEmpty()) out.add("en")
        return out
    }
}
