/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ai

/**
 * DRS Phase 2 (roadmap task 8): the on-device quantized suggestion engine.
 *
 * A self-contained, dependency-free correction engine that upgrades the
 * previous "edit distance <= 1" fallback into a weighted Damerau-
 * Levenshtein search with:
 *  - QUANTIZED integer scoring: every cost is a small Int (1..16), no
 *    floating point anywhere on the hot path. This keeps the arithmetic
 *    exact, allocation-free and fast on low-end cores (mirrors int8
 *    quantization semantics of ML runtimes without pulling any runtime).
 *  - CONFUSION-AWARE costs: substitution weights are supplied by a
 *    pluggable [DrsQuantizedEngine.CostModel] (the Arabic corrector
 *    supplies Arabic-specific weights, [QwertyCostModel] supplies Latin
 *    keyboard-adjacency ones).
 *  - BUDGET-AWARE quantization (L2, الدفعة 15): the saturation sentinel is
 *    the CALLER'S BUDGET, not the global maximum — [distance] computes
 *    exactly min(true distance, budget), so the tier budget (8/12/14)
 *    prunes the DP quadrants the caller would reject anyway. Proven:
 *    min-plus DP is monotone, so cell-saturating at `budget` yields
 *    min(true, budget) for every cell (see check_l2_quantized.py).
 *  - BEAM cutoff: rows whose every cell already saturates the budget end
 *    the distance computation early, and the dictionary scan skips words
 *    whose length band cannot reach the budget — and (L2) only the
 *    in-band length BUCKETS are visited at all, never the whole scan.
 *
 * The engine is PURE: no Android types, no I/O, no global state. The
 * provider owns dictionaries; the engine only computes.
 */
object DrsQuantizedEngine {

    /** Maximum supported correction cost (quantized cost budget). */
    const val MAX_DISTANCE = 16

    /**
     * Cost model for the quantized edit operations. All costs are small
     * integers (0 = free — reserved for IDENTICAL characters; 1..16 =
     * progressively worse).
     */
    interface CostModel {
        /**
         * Quantized cost of replacing [a] with [b]. Implementations MUST
         * return [MAX_DISTANCE] (or a value that saturates the budget) for
         * unrelated pairs, and small values for confusable pairs.
         */
        fun substitutionCost(a: Char, b: Char): Int

        /** Quantized insertion cost (defaults to 3). */
        fun insertionCost(): Int = 3

        /** Quantized deletion cost (defaults to 3). */
        fun deletionCost(): Int = 3

        /** Quantized transposition cost (defaults to 2 — adjacent typos are common). */
        fun transpositionCost(): Int = 2
    }

    /**
     * A candidate match produced by [search]. [cost] is the quantized edit
     * cost (0 means exact match; lower is better).
     */
    data class Match(val word: String, val cost: Int)

    /**
     * Default neutral cost model: every substitution saturates unless the
     * characters are equal (cost 0). Used when no better model is supplied.
     */
    val NEUTRAL: CostModel = object : CostModel {
        override fun substitutionCost(a: Char, b: Char): Int =
            if (a == b) 0 else MAX_DISTANCE
    }

    /**
     * Computes the quantized weighted Damerau-Levenshtein distance between
     * [a] and [b] under [costs], saturating at [budget]. Pure Int
     * arithmetic; three rolling rows, no other allocation.
     *
     * L2 CONTRACT (الدفعة 15): the result is EXACTLY min(true distance,
     * [budget]) — a return value equal to [budget] is the SATURATION
     * SENTINEL ("nothing within the budget can say how far"), never an
     * exact distance. Callers must therefore accept only values strictly
     * below their usable budget (see [search]). The default [budget]
     * preserves the historical behavior byte-for-byte.
     */
    fun distance(
        a: String,
        b: String,
        costs: CostModel = NEUTRAL,
        budget: Int = MAX_DISTANCE,
    ): Int {
        if (a == b) return 0
        val n = a.length
        val m = b.length
        if (n == 0) return (m * costs.insertionCost()).coerceAtMost(budget)
        if (m == 0) return (n * costs.deletionCost()).coerceAtMost(budget)
        // Saturation guard: the length difference is a lower bound on the
        // insertion/deletion cost (each length step costs at least MIN of
        // the two — the historical max() form was unsound for skewed
        // custom models; with the standard 3/3 both agree). Bail early
        // when even that lower bound reaches the budget.
        val insC = costs.insertionCost()
        val delC = costs.deletionCost()
        val lenDelta = if (n > m) n - m else m - n
        if (lenDelta * delC.coerceAtMost(insC) >= budget) return budget

        var prev2 = IntArray(m + 1) // row i-2 (for transpositions)
        var prev = IntArray(m + 1)  // row i-1
        var curr = IntArray(m + 1)  // row i

        for (j in 0..m) prev[j] = (j * insC).coerceAtMost(budget)

        for (i in 1..n) {
            curr[0] = (i * delC).coerceAtMost(budget)
            val ca = a[i - 1]
            var rowMin = curr[0]
            for (j in 1..m) {
                val cb = b[j - 1]
                val sub = prev[j - 1] + if (ca == cb) 0 else costs.substitutionCost(ca, cb)
                val del = prev[j] + delC
                val ins = curr[j - 1] + insC
                var best = sub
                if (del < best) best = del
                if (ins < best) best = ins
                if (i > 1 && j > 1) {
                    val pa = a[i - 2]
                    val pb = b[j - 2]
                    if (ca == pb && pa == cb) {
                        val trans = prev2[j - 2] + costs.transpositionCost()
                        if (trans < best) best = trans
                    }
                }
                curr[j] = best.coerceAtMost(budget)
                if (best < rowMin) rowMin = best
            }
            // Beam cutoff: when every cell of this row already saturates the
            // budget, every later row can only be >= it (curr[0] already is,
            // and each cell is a neighbor + a non-negative cost) — the final
            // min(true, budget) is exactly the sentinel. Stop early.
            if (rowMin >= budget) return budget
            val tmp = prev2
            prev2 = prev
            prev = curr
            curr = tmp
        }
        return prev[m]
    }

    /**
     * Searches [words] — a sequence of (word, frequency) pairs — for the
     * best matches to [query] within the quantized budget, returning up to
     * [limit] matches ordered by cost ascending, then frequency descending,
     * then word ascending (the third tiebreak makes the order FULLY
     * deterministic — equal-(cost, freq) words no longer depend on the
     * scan's visit order). [lengthBand] is a pure preprocessing cut.
     *
     * L2 (الدفعة 15): the scan is BUDGET-AWARE — the distance computation
     * saturates at min([maxCost] + 1, [MAX_DISTANCE]), pruning every DP
     * quadrant the acceptance filter would reject anyway. The accepted
     * result set is PROVABLY identical to the legacy full-budget scan
     * (check_l2_quantized.py, 8k+ cases).
     */
    fun search(
        query: String,
        words: Sequence<Pair<String, Int>>,
        costs: CostModel,
        limit: Int,
        maxCost: Int = MAX_DISTANCE,
        lengthBand: Int = 4,
    ): List<Match> {
        if (limit <= 0 || query.isEmpty()) return emptyList()
        return searchWithin(query, words, costs, limit, maxCost, lengthBand)
    }

    /**
     * L2 (الدفعة 15): length-BUCKETED search — instead of visiting the
     * whole dictionary and skipping out-of-band words one by one, only the
     * in-band length buckets ([query].length ± [lengthBand]) are visited
     * AT ALL. Buckets are prebuilt once per dictionary (see the provider's
     * DictIndex), so the per-keystroke scan touches only words that can
     * possibly match. Acceptance and ordering are IDENTICAL to [search] —
     * both delegate to [searchWithin].
     */
    fun search(
        query: String,
        lengthBuckets: Map<Int, List<Pair<String, Int>>>,
        costs: CostModel,
        limit: Int,
        maxCost: Int = MAX_DISTANCE,
        lengthBand: Int = 4,
    ): List<Match> {
        if (limit <= 0 || query.isEmpty()) return emptyList()
        val qLen = query.length
        val scoped = ArrayList<Pair<String, Int>>()
        for (len in (qLen - lengthBand)..(qLen + lengthBand)) {
            lengthBuckets[len]?.let { scoped.addAll(it) }
        }
        return searchWithin(query, scoped.asSequence(), costs, limit, maxCost, lengthBand)
    }

    /** Shared kernel: budget-aware scan + acceptance + deterministic order. */
    private fun searchWithin(
        query: String,
        words: Sequence<Pair<String, Int>>,
        costs: CostModel,
        limit: Int,
        maxCost: Int,
        lengthBand: Int,
    ): List<Match> {
        val qLen = query.length
        // The caller's budget + 1 IS the saturation sentinel: any word whose
        // true distance exceeds maxCost saturates to a value > maxCost and
        // fails the acceptance filter — the accepted SET is identical to the
        // legacy full-budget scan, at strictly less work.
        val budget = (maxCost + 1).coerceAtMost(MAX_DISTANCE)
        val found = ArrayList<Match>(limit * 4)
        val freqOf = HashMap<String, Int>()

        for ((word, freq) in words) {
            val band = word.length - qLen
            if (band > lengthBand || -band > lengthBand) continue
            val cost = distance(query, word, costs, budget)
            // MAX_DISTANCE stays the GLOBAL sentinel (maxCost may itself be
            // MAX_DISTANCE): a cost equal to it is never a real match.
            if (cost < MAX_DISTANCE && cost <= maxCost) {
                freqOf[word] = freq
                found.add(Match(word, cost))
            }
        }

        // Order by cost ascending, then frequency descending, then word —
        // fully deterministic regardless of visit order; dedupe.
        found.sortWith(compareBy({ it.cost }, { -(freqOf[it.word] ?: 0) }, { it.word }))
        val seen = HashSet<String>(limit * 2)
        val out = ArrayList<Match>(limit)
        for (m in found) {
            if (seen.add(m.word)) {
                out.add(m)
                if (out.size >= limit) break
            }
        }
        return out
    }
}

/**
 * DRS Phase 2 (roadmap task 8): quantized QWERTY-adjacency cost model for
 * Latin-script corrections. Confusions between physically adjacent keys
 * (q/w, a/s, z/x, …) cost 1, same-row two-apart pairs cost 2, vertical
 * neighbors cost 2, everything else saturates. Combined with the engine's
 * budget this produces classic "fat finger" tolerance.
 */
object QwertyCostModel : DrsQuantizedEngine.CostModel {

    private val ROWS = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm",
    )

    private val POS: Map<Char, Pair<Int, Int>> = buildMap {
        ROWS.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch -> put(ch, r to c) }
        }
    }

    override fun substitutionCost(a: Char, b: Char): Int {
        if (a == b) return 0
        val la = a.lowercaseChar()
        val lb = b.lowercaseChar()
        if (la == lb) return 0
        val pa = POS[la] ?: return DrsQuantizedEngine.MAX_DISTANCE
        val pb = POS[lb] ?: return DrsQuantizedEngine.MAX_DISTANCE
        if (pa.first == pb.first) {
            val d = pa.second - pb.second
            return when {
                d == 1 || d == -1 -> 1
                d == 2 || d == -2 -> 2
                else -> DrsQuantizedEngine.MAX_DISTANCE
            }
        }
        // Vertical neighbors (adjacent rows, near columns) cost 2.
        val dr = pa.first - pb.first
        val dc = pa.second - pb.second
        if ((dr == 1 || dr == -1) && dc <= 1 && dc >= -1) return 2
        return DrsQuantizedEngine.MAX_DISTANCE
    }
}
