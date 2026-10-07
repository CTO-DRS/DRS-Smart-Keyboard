package com.drs.keyboard.engine

import android.content.Context
import org.json.JSONObject

/**
 * Text expansion shortcuts ("omw" -> "on my way"). Triggered when the user
 * finishes a word that exactly matches a short form. User-editable from the
 * settings app; a few sensible defaults ship preloaded. Stored locally only.
 */
class ShortcutEngine private constructor(private val context: Context) {

    private val map = LinkedHashMap<String, String>(32)
    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Process-wide instance: the IME and settings share one shortcut table. */
    companion object {
        private const val PREFS = "drs_shortcuts"
        private const val KEY_MAP = "shortcuts_json"
        @Volatile private var instance: ShortcutEngine? = null
        fun get(context: Context): ShortcutEngine = instance ?: synchronized(this) {
            instance ?: ShortcutEngine(context.applicationContext).also { instance = it }
        }
    }

    fun load() {
        map.clear()
        val json = prefs.getString(KEY_MAP, null) ?: return
        runCatching {
            val obj = JSONObject(json)
            for (k in obj.keys()) map[k.lowercase()] = obj.getString(k)
        }
    }

    fun persist() {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        prefs.edit().putString(KEY_MAP, obj.toString()).apply()
    }

    fun put(short: String, expansion: String, persistNow: Boolean = true) {
        map[short.lowercase().trim()] = expansion.trim()
        if (persistNow) persist()
    }

    /** Load defaults on first run (kept from the original init). */
    fun ensureDefaults() {
        if (prefs.getString(KEY_MAP, null) != null) return
        put("omw", "on my way", persistNow = false)
        put("brb", "be right back", persistNow = false)
        put("idk", "I don't know", persistNow = false)
        put("imho", "in my humble opinion", persistNow = false)
        put("btw", "by the way", persistNow = false)
        put("fyi", "for your information", persistNow = false)
        put("lmk", "let me know", persistNow = false)
        put("asap", "as soon as possible", persistNow = false)
        put("ty", "thank you", persistNow = false)
        put("np", "no problem", persistNow = false)
        put("ttyl", "talk to you later", persistNow = false)
        put("gm", "good morning", persistNow = false)
        put("gn", "good night", persistNow = false)
        put("السلام", "السلام عليكم ورحمة الله وبركاته", persistNow = false)
        put("الحمد", "الحمد لله رب العالمين", persistNow = false)
        put("انشاء", "إن شاء الله", persistNow = false)
        persist()
        load()
    }

    fun remove(short: String) {
        map.remove(short.lowercase().trim())
        persist()
    }

    fun all(): LinkedHashMap<String, String> = LinkedHashMap(map)

    /** Exact, case-insensitive lookup of a completed word. */
    fun expansionFor(word: String): String? = map[word.lowercase()]
}
