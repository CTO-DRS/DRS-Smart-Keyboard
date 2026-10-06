package com.drs.keyboard.view

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Emoji catalog loaded from assets (category \t emoji), plus recents and
 * favorites persisted in private storage. Categories appear in fixed order.
 */
object EmojiRepo {

    val CATEGORY_ORDER = listOf(
        "favorites", "recent", "smileys", "people", "animals", "food",
        "activity", "travel", "objects", "symbols"
    )

    private val byCategory = LinkedHashMap<String, MutableList<String>>(16)
    private val recents = LinkedHashSet<String>(24)
    private val favorites = LinkedHashSet<String>(32)

    // offline search index: emoji -> pre-normalized keyword list (EN + AR)
    private val keywords = HashMap<String, List<String>>(1024)
    private val searchOrder = ArrayList<String>(1024)

    fun load(context: Context) {
        if (byCategory.isNotEmpty()) return
        context.assets.open("dict/emoji.txt").use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).forEachLine { line ->
                val idx = line.indexOf('\t')
                if (idx > 0) {
                    val cat = line.substring(0, idx)
                    val em = line.substring(idx + 1)
                    byCategory.getOrPut(cat) { ArrayList() }.add(em)
                }
            }
        }
        runCatching {
            context.assets.open("dict/emoji_keys.txt").use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).forEachLine { line ->
                    val idx = line.indexOf('\t')
                    if (idx <= 0) return@forEachLine
                    val em = line.substring(0, idx)
                    val kws = line.substring(idx + 1).split(',')
                        .map { normalize(it.trim()) }
                        .filter { it.isNotEmpty() }
                    if (kws.isNotEmpty() && !keywords.containsKey(em)) {
                        keywords[em] = kws
                        searchOrder.add(em)
                    }
                }
            }
        }
        loadPersisted(context)
    }

    /** Normalize Arabic text for matching: strip harakat/tatweel, unify alef/ya/ha forms, lowercase. */
    private fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            if (c in '\u064B'..'\u0652' || c == '\u0640' || c == '\u0670') continue
            when (c) {
                'أ', 'إ', 'آ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                else -> sb.append(c.lowercaseChar())
            }
        }
        return sb.toString()
    }

    /**
     * Offline emoji search across EN+AR keywords. Substring match on any
     * keyword; results keep the catalog order, capped at 48 cells.
     */
    fun search(query: String): List<String> {
        val q = normalize(query.trim())
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<String>(48)
        for (em in searchOrder) {
            val kws = keywords[em] ?: continue
            var hit = false
            for (kw in kws) {
                if (kw.contains(q)) { hit = true; break }
            }
            if (hit || em == query.trim()) {
                out.add(em)
                if (out.size >= 48) break
            }
        }
        return out
    }

    private fun loadPersisted(context: Context) {
        val prefs = context.getSharedPreferences("drs_emoji", Context.MODE_PRIVATE)
        prefs.getString("recents", null)?.split('\u0001')?.forEach {
            if (it.isNotEmpty()) recents.add(it)
        }
        prefs.getString("favorites", null)?.split('\u0001')?.forEach {
            if (it.isNotEmpty()) favorites.add(it)
        }
    }

    fun category(category: String): List<String> = when (category) {
        "recent" -> recents.toList()
        "favorites" -> favorites.toList()
        else -> byCategory[category] ?: emptyList()
    }

    fun recordRecent(context: Context, emoji: String) {
        recents.remove(emoji)
        recents.add(emoji)
        while (recents.size > 24) recents.remove(recents.first())
        context.getSharedPreferences("drs_emoji", Context.MODE_PRIVATE)
            .edit().putString("recents", recents.joinToString("\u0001")).apply()
    }

    fun hasRecents(): Boolean = recents.isNotEmpty()

    /** Adds/removes a favorite. Returns true if the emoji is now a favorite. */
    fun toggleFavorite(context: Context, emoji: String): Boolean {
        val added = if (favorites.contains(emoji)) {
            favorites.remove(emoji); false
        } else {
            favorites.add(emoji); true
        }
        context.getSharedPreferences("drs_emoji", Context.MODE_PRIVATE)
            .edit().putString("favorites", favorites.joinToString("\u0001")).apply()
        return added
    }

    fun isFavorite(emoji: String): Boolean = favorites.contains(emoji)

    fun hasFavorites(): Boolean = favorites.isNotEmpty()

    // ---- backup / restore ------------------------------------------------

    fun exportFavorites(): String = favorites.joinToString("\u0001")

    fun exportRecents(): String = recents.joinToString("\u0001")

    /** Replace recents + favorites from a backup file. */
    fun restore(context: Context, favs: List<String>, recs: List<String>) {
        favorites.clear()
        favorites.addAll(favs.filter { it.isNotEmpty() })
        recents.clear()
        recents.addAll(recs.filter { it.isNotEmpty() })
        while (recents.size > 24) recents.remove(recents.first())
        context.getSharedPreferences("drs_emoji", Context.MODE_PRIVATE).edit()
            .putString("favorites", favorites.joinToString("\u0001"))
            .putString("recents", recents.joinToString("\u0001"))
            .apply()
    }
}
