package com.drs.keyboard.engine

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local typing statistics: per-day counters (keystrokes, completed words,
 * emoji, swipes) stored in private SharedPreferences — nothing leaves the
 * device, no dates beyond what the counters need.
 */
object StatsStore {

    private const val PREFS = "drs_stats"
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    const val KEYS = "keys"
    const val WORDS = "words"
    const val EMOJI = "emoji"
    const val SWIPES = "swipes"

    private var cachedDay = ""
    private val cachedCounts = HashMap<String, Int>(8)

    @Synchronized
    fun bump(context: Context, key: String, amount: Int = 1) {
        val today = dayFormat.format(Date())
        if (today != cachedDay) {
            cachedCounts.clear()
            cachedCounts.putAll(read(context, today))
            cachedDay = today
        }
        cachedCounts[key] = (cachedCounts[key] ?: 0) + amount
        val obj = JSONObject()
        for ((k, v) in cachedCounts) obj.put(k, v)
        prefs(context).edit().putString(today, obj.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context, day: String): Map<String, Int> {
        val out = HashMap<String, Int>(8)
        val raw = prefs(context).getString(day, null) ?: return out
        runCatching {
            val o = JSONObject(raw)
            for (k in o.keys()) out[k] = o.optInt(k, 0)
        }
        return out
    }

    /** Last n days as (date, counts) pairs, oldest first — today last. */
    fun lastDays(context: Context, n: Int): List<Pair<String, Map<String, Int>>> {
        val cal = Calendar.getInstance()
        val out = ArrayList<Pair<String, Map<String, Int>>>(n)
        for (i in n - 1 downTo 0) {
            val c = (cal.clone() as Calendar)
            c.add(Calendar.DAY_OF_YEAR, -i)
            val d = dayFormat.format(c.time)
            out.add(d to read(context, d))
        }
        return out
    }

    /** All-time totals across every recorded day. */
    fun totals(context: Context): Map<String, Int> {
        val acc = HashMap<String, Int>(8)
        for (day in prefs(context).all.keys) {
            for ((k, v) in read(context, day)) acc[k] = (acc[k] ?: 0) + v
        }
        return acc
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        cachedCounts.clear()
        cachedDay = ""
    }

    /** Localized short weekday label for a yyyy-MM-dd date. */
    fun dayLabel(date: String, locale: Locale): String = runCatching {
        val parsed = dayFormat.parse(date) ?: return "·"
        val out = SimpleDateFormat("EEE", locale).format(parsed)
        out
    }.getOrDefault("·")
}
