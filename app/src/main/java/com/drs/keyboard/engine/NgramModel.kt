package com.drs.keyboard.engine

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Bigram next-word model: for a previous word, ranks the words that most
 * often follow it. Counts come from assets/dict/{lang}_bigrams.txt
 * (columns: word<TAB>next<TAB>count). Merged with per-user counts at runtime.
 */
class NgramModel {

    private val table = HashMap<String, HashMap<String, Int>>(1024)

    fun clear() = table.clear()

    fun load(context: Context, lang: String) {
        clear()
        val name = if (lang == "ar") "dict/ar_bigrams.txt" else "dict/en_bigrams.txt"
        runCatching {
            context.assets.open(name).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).forEachLine { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val a = parts[0]
                        val b = parts[1]
                        val c = parts[2].toIntOrNull() ?: return@forEachLine
                        table.getOrPut(a) { HashMap() }[b] = c
                    }
                }
            }
        }
    }

    fun learn(prev: String, next: String, amount: Int = 1) {
        val a = prev.lowercase()
        val b = next.lowercase()
        if (a.isEmpty() || b.isEmpty() || a.length > 24 || b.length > 24) return
        val m = table.getOrPut(a) { HashMap() }
        m[b] = (m[b] ?: 0) + amount
        if (m.size > 40) {
            // evict least-likely entries to bound memory
            val it = m.entries.sortedBy { it.value }.take(m.size - 40)
            for (e in it) m.remove(e.key)
        }
    }

    /** Top [limit] candidates that follow [prev], best first. */
    fun nextWords(prev: String, limit: Int = 3): List<String> {
        val m = table[prev.lowercase()] ?: return emptyList()
        return m.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value })
            .take(limit)
            .map { it.key }
    }
}
