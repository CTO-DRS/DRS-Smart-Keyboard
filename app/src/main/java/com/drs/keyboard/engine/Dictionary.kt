package com.drs.keyboard.engine

/**
 * Trie-backed frequency dictionary with:
 *  - prefix completion ranked by frequency (with user-frequency boost)
 *  - weighted Damerau-Levenshtein fuzzy search walked directly over the trie
 *    (keyboard-adjacency-aware substitution costs, early subtree pruning)
 *  - separate display forms for capitalized words (trie keys stay lowercase)
 *
 * Pure deterministic data structures — no ML, no network.
 */
class Dictionary {

    class Node {
        val children = HashMap<Char, Node>(8)
        var freq = 0
        var display: String? = null // set when word contains uppercase
    }

    private val root = Node()
    private var wordCount = 0
    private val allWords = ArrayList<String>(2048)

    val size: Int get() = wordCount

    fun clear() {
        root.children.clear()
        allWords.clear()
        wordCount = 0
    }

    fun add(word: String, freq: Int) {
        if (word.isEmpty()) return
        val key = word.lowercase()
        var node = root
        for (c in key) {
            node = node.children.getOrPut(c) { Node() }
        }
        node.freq = freq
        if (word != key) node.display = word
        wordCount++
        allWords.add(word)
    }

    /** Every stored word (display form) — feeds the swipe decoder. */
    fun words(): List<String> = allWords

    fun contains(word: String): Boolean {
        val n = findNode(word.lowercase()) ?: return false
        return n.freq > 0
    }

    fun freqOf(word: String): Int {
        val n = findNode(word.lowercase()) ?: return 0
        return n.freq
    }

    private fun findNode(key: String): Node? {
        var node = root
        for (c in key) {
            node = node.children[c] ?: return null
        }
        return node
    }

    private fun displayOf(node: Node, key: String): String = node.display ?: key

    // ---------------------------------------------------------------------
    // Prefix completion
    // ---------------------------------------------------------------------

    class Completion(val word: String, val freq: Int)

    /**
     * Returns words starting with [prefix], ranked by frequency, best first.
     * Visits at most [maxNodes] trie nodes as a safety cap for huge subtrees.
     */
    fun complete(prefix: String, limit: Int = 8, maxNodes: Int = 6000): List<Completion> {
        val key = prefix.lowercase()
        if (key.isEmpty()) return emptyList()
        val start = findNode(key) ?: return emptyList()
        val out = ArrayList<Completion>(limit)
        var visited = 0
        fun walk(node: Node, buf: StringBuilder) {
            if (out.size >= limit || visited > maxNodes) return
            visited++
            if (node.freq > 0) {
                out.add(Completion(displayOf(node, buf.toString()), node.freq))
            }
            for ((c, child) in node.children) {
                buf.append(c)
                walk(child, buf)
                buf.setLength(buf.length - 1)
                if (out.size >= limit || visited > maxNodes) return
            }
        }
        walk(start, StringBuilder(key))
        out.sortByDescending { it.freq }
        return out
    }

    // ---------------------------------------------------------------------
    // Weighted Damerau-Levenshtein correction over the trie
    // ---------------------------------------------------------------------

    class Correction(val word: String, val cost: Double, val freq: Int)

    /**
     * Finds dictionary words within weighted edit distance [maxCost] of [word].
     *
     * Costs: substitution 0.0 for identical chars, 0.9 when the two letters sit
     * next to each other on the keyboard, 1.4 otherwise; insertion/deletion 1.0;
     * transposition 0.7. The trie walk carries DP rows and prunes any branch
     * whose minimum row value already exceeds the budget, which keeps the scan
     * in the low-millisecond range for 20k+ words.
     *
     * [neighbors] maps each lowercase char to its on-keyboard neighbors.
     */
    fun correct(
        word: String,
        neighbors: Map<Char, Set<Char>>,
        maxCost: Double = 2.0,
        limit: Int = 8
    ): List<Correction> {
        val target = word.lowercase()
        val n = target.length
        if (n == 0 || n > 24) return emptyList()

        var prev: DoubleArray = DoubleArray(n + 1) { it * 1.0 }
        val results = ArrayList<Correction>(limit)

        fun walk(node: Node, depth: Int, row: DoubleArray, prevRow: DoubleArray?, buf: StringBuilder) {
            if (results.size >= limit * 4) return
            var minVal = Double.MAX_VALUE
            for (v in row) if (v < minVal) minVal = v
            if (minVal > maxCost) return
            if (node.freq > 0) {
                val finalCost = row[n]
                if (finalCost <= maxCost) {
                    results.add(Correction(displayOf(node, buf.toString()), finalCost, node.freq))
                }
            }
            if (depth >= n + 2) return
            for ((c, child) in node.children) {
                val next = DoubleArray(n + 1)
                next[0] = row[0] + 1.0
                for (i in 1..n) {
                    val ins = next[i - 1] + 1.0          // gap in dictionary word
                    val del = row[i - 1] + 1.0           // gap in candidate
                    val subCost = when {
                        target[i - 1] == c -> 0.0
                        neighbors[target[i - 1]]?.contains(c) == true -> 0.9
                        else -> 1.4
                    }
                    var best = minOf(ins, del, row[i] + subCost)
                    if (prevRow != null && i >= 2 && depth >= 2 && buf.length >= 2 &&
                        target[i - 1] == buf[buf.length - 2] && target[i - 2] == c
                    ) {
                        best = minOf(best, prevRow[i - 2] + 0.7)
                    }
                    next[i] = best
                }
                buf.append(c)
                walk(child, depth + 1, next, row, buf)
                buf.setLength(buf.length - 1)
            }
        }

        walk(root, 0, prev, null, StringBuilder())
        results.sortWith(compareBy({ it.cost }, { -it.freq }))
        val dedup = LinkedHashMap<String, Correction>()
        for (c in results) if (dedup.size < limit) dedup.putIfAbsent(c.word.lowercase(), c)
        return dedup.values.toList()
    }
}
