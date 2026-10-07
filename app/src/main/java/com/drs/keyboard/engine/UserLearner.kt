package com.drs.keyboard.engine

import android.content.Context
import org.json.JSONObject

/**
 * On-device word learning: boosts frequencies of words the user actually
 * types. Storage is this app's private SharedPreferences — nothing leaves
 * the device. A simple count table is fully deterministic and auditable.
 */
class UserLearner private constructor(private val context: Context) {

    private val counts = HashMap<String, Int>(512)
    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    init {
        load()
    }

    /** Process-wide instance: the IME and settings share one learning table. */
    companion object {
        private const val PREFS = "drs_learner"
        private const val KEY_WORDS = "word_counts"
        @Volatile private var instance: UserLearner? = null
        fun get(context: Context): UserLearner = instance ?: synchronized(this) {
            instance ?: UserLearner(context.applicationContext).also { instance = it }
        }
    }

    fun load() {
        counts.clear()
        val json = prefs.getString(KEY_WORDS, null) ?: return
        runCatching {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                counts[key] = obj.optInt(key, 0)
            }
        }
    }

    fun persist() {
        val obj = JSONObject()
        // keep the top 500 learned words
        for ((w, c) in counts.entries.sortedByDescending { it.value }.take(500)) {
            obj.put(w, c)
        }
        prefs.edit().putString(KEY_WORDS, obj.toString()).apply()
    }

    fun boost(word: String, amount: Int = 1) {
        if (word.length < 2 || word.length > 24) return
        val w = Dictionary.stripTashkeel(word.lowercase())
        if (!w.all { it.isLetter() }) return
        counts[w] = (counts[w] ?: 0) + amount
    }

    fun freqOf(word: String): Int =
        counts[Dictionary.stripTashkeel(word.lowercase())] ?: 0

    fun knownWords(): Set<String> = counts.keys

    /** Learned words with their counts, sorted strongest-first. */
    fun entries(): List<Pair<String, Int>> =
        counts.entries.sortedByDescending { it.value }.map { it.key to it.value }

    fun remove(word: String) {
        counts.remove(Dictionary.stripTashkeel(word.lowercase()))
        persist()
    }

    /** Replace the whole table (backup restore). */
    fun replaceAll(table: Map<String, Int>) {
        counts.clear()
        for ((w, c) in table) {
            if (w.length in 2..24 && w.all { it.isLetter() }) counts[w] = c
        }
        persist()
    }

    fun clear() {
        counts.clear()
        prefs.edit().remove(KEY_WORDS).apply()
    }
}
