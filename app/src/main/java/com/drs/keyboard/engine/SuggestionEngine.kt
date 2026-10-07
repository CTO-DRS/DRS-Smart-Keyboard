package com.drs.keyboard.engine

/**
 * The brain that fuses four deterministic signals into one ranked strip:
 *
 *   1. prefix completion  (trie walk, frequency ranked)
 *   2. weighted fuzzy fix (adjacency-aware Damerau-Levenshtein)
 *   3. next-word stats    (bigram counts)
 *   4. personal history   (UserLearner frequency boosts)
 *
 * Everything is math on local data — no models, no network.
 */
class SuggestionEngine(
    val dictionaries: Map<String, Dictionary>, // lang -> dict ("en", "ar")
    val ngrams: Map<String, NgramModel>,       // lang -> model
    val learner: UserLearner
) {

    class Suggestion(
        val word: String,
        val isCorrection: Boolean, // typed word differs from suggestion
        score: Double
    ) {
        internal var score: Double = score
    }

    var adjacency: Map<String, Map<Char, Set<Char>>> = emptyMap() // lang -> neighbors

    fun dictFor(lang: String): Dictionary =
        dictionaries[lang] ?: dictionaries.values.first()

    private fun ngramFor(lang: String): NgramModel =
        ngrams[lang] ?: ngrams.values.first()

    private fun neighborsFor(lang: String): Map<Char, Set<Char>> =
        adjacency[lang] ?: emptyMap()

    private fun boost(lang: String, word: String, baseFreq: Int): Int =
        baseFreq + learner.freqOf(word) * 30

    /**
     * [typed] is the current (possibly incomplete) word; [prev] the word right
     * before it (empty after sentence start). Returns up to 3 suggestions.
     */
    fun suggest(lang: String, typed: String, prev: String, autocorrect: Boolean): List<Suggestion> {
        if (typed.isEmpty()) {
            if (prev.isEmpty()) return emptyList()
            return ngramFor(lang).nextWords(prev, 3)
                .filter { it.isNotEmpty() }
                .map { Suggestion(it, false, 1.0) }
        }
        if (typed.length > 24 || !typed[0].isLetter()) return emptyList()

        val dict = dictFor(lang)
        val neigh = neighborsFor(lang)
        val typedFreq = dict.freqOf(typed)
        val known = typedFreq > 0 || learner.freqOf(typed) > 0

        // 1) completions of the typed prefix
        val completions = dict.complete(typed, 10)

        // 2) fuzzy corrections when the prefix isn't a known word (or is rare)
        var corrections: List<Dictionary.Correction> = emptyList()
        if (autocorrect && (!known || typedFreq < 400)) {
            corrections = dict.correct(typed, neigh, 2.0, 8)
        }

        val merged = LinkedHashMap<String, Suggestion>()

        // exact match itself first (user may simply want the typed word)
        if (known) {
            merged[typed.lowercase()] = Suggestion(typed, false, 1e9 + boost(lang, typed, typedFreq))
        }

        val completionLimit = if (corrections.isEmpty()) 3 else 2
        for (c in completions.take(completionLimit + 1)) {
            if (merged.size >= 3) break
            val w = c.word.lowercase()
            if (w == typed.lowercase() && c.word == typed) continue
            if (w in merged) continue
            if (c.freq < 60 && merged.size >= 2) break // trailing weak fills
            merged[w] = Suggestion(c.word, false, boost(lang, w, c.freq).toDouble())
        }

        if (autocorrect) {
            for (c in corrections) {
                if (merged.size >= 3) break
                val w = c.word.lowercase()
                if (w in merged) continue
                if (w == typed.lowercase() && known) continue
                val boosted = boost(lang, w, c.freq) / 1000.0
                merged[w] = Suggestion(c.word, true, boosted - c.cost * 1.2)
            }
        }

        val list = merged.values.sortedByDescending { it.score }
        return list.take(3)
    }

    /** Decides which word (if any) to auto-commit when the user hits space. */
    fun pickAutocorrect(lang: String, typed: String, suggestions: List<Suggestion>): String? {
        if (typed.isEmpty() || suggestions.isEmpty()) return null
        val dict = dictFor(lang)
        if (dict.freqOf(typed) >= 800 || learner.freqOf(typed) > 2) return null // confident word
        val top = suggestions.first()
        return if (top.isCorrection && !top.word.equals(typed, ignoreCase = true)) top.word else null
    }
}
