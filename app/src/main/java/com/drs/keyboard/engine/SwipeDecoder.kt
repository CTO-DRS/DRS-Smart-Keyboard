package com.drs.keyboard.engine

import kotlin.math.sqrt

/**
 * Geometric swipe decoder — turns a finger path drawn across the keyboard
 * into the most plausible word.
 *
 * Algorithm (classic shape-based decoding, fully deterministic):
 *   1. Filter dictionary candidates by first/last letter proximity to the
 *      path's endpoints.
 *   2. For each candidate, walk its letters and measure how faithfully the
 *      finger path visits each letter's key (min distance from the letter's
 *      key center to the path polyline, averaged, normalized by path length).
 *   3. Rank by shape fidelity; break ties with word frequency.
 */
class SwipeDecoder {

    class Point(var x: Float, var y: Float, var t: Long)

    /** letter -> key center (px). Rebuilt whenever layout/theme size changes. */
    var keyCenters: Map<Char, Pair<Float, Float>> = emptyMap()

    /** Approximate key radius in px (half a key width). */
    var keyRadius: Float = 60f

    var frequencyOf: (String) -> Int = { 0 }

    fun decode(path: List<Point>, limit: Int = 1): List<String> {
        if (path.size < 4 || keyCenters.isEmpty()) return emptyList()
        val first = nearestLetter(path.first()) ?: return emptyList()
        val last = nearestLetter(path.last()) ?: return emptyList()
        if (first == last && pathDistance(path) < keyRadius * 6) return emptyList()

        val pathLen = pathDistance(path).coerceAtLeast(1f)
        val startR = keyRadius * 1.9f
        val endR = keyRadius * 2.2f

        val scored = ArrayList<Triple<String, Double, Int>>(256)

        // candidate set: words that begin and end near the path endpoints
        val candidates = ArrayList<String>(128)
        for (w in words()) {
            if (w.length < 2) continue
            val fc = keyCenters[w[0]] ?: continue
            val lc = keyCenters[w[w.length - 1]] ?: continue
            if (dist(fc.first, fc.second, path.first().x, path.first().y) > startR) continue
            if (dist(lc.first, lc.second, path.last().x, path.last().y) > endR) continue
            candidates.add(w)
            if (candidates.size > 900) break
        }

        val sampled = simplify(path, keep = 48)

        for (w in candidates) {
            var ok = true
            var total = 0.0
            var searchFrom = 0
            for (ch in w) {
                val kc = keyCenters[ch]
                if (kc == null) { ok = false; break }
                var best = Double.MAX_VALUE
                for (i in searchFrom until sampled.size) {
                    val d = dist(kc.first, kc.second, sampled[i].x, sampled[i].y).toDouble()
                    if (d < best) {
                        best = d
                        if (d == 0.0) break
                    }
                }
                if (best > keyRadius * 2.6) { ok = false; break }
                total += best
                // advance the window so letters must be visited in order
                searchFrom = advanceIndex(sampled, kc, searchFrom)
            }
            if (!ok) continue
            val shape = total / w.length / pathLen
            val freq = frequencyOf(w)
            val score = shape * 1e6 - freq * 0.5 - w.length * 2.0
            scored.add(Triple(w, score, freq))
        }

        scored.sortWith(compareBy { it.second })
        val out = ArrayList<String>(limit)
        for (s in scored) {
            if (out.none { it == s.first }) out.add(s.first)
            if (out.size >= limit) break
        }
        return out
    }

    private var wordList: List<String> = emptyList()

    fun setWords(words: List<String>) {
        wordList = words
    }

    private fun words(): List<String> = wordList

    private fun advanceIndex(path: List<Point>, center: Pair<Float, Float>, from: Int): Int {
        var i = from
        while (i < path.size - 1 &&
            dist(center.first, center.second, path[i].x, path[i].y) < keyRadius * 0.8
        ) i++
        return i
    }

    private fun simplify(src: List<Point>, keep: Int): List<Point> {
        if (src.size <= keep) return src
        val step = src.size.toDouble() / keep
        val out = ArrayList<Point>(keep)
        var i = 0.0
        while (out.size < keep && i.toInt() < src.size) {
            out.add(src[i.toInt()])
            i += step
        }
        if (out.last() !== src.last()) out.add(src.last())
        return out
    }

    private fun nearestLetter(p: Point): Char? {
        var best: Char? = null
        var bestD = Float.MAX_VALUE
        for ((c, xy) in keyCenters) {
            val d = dist(xy.first, xy.second, p.x, p.y)
            if (d < bestD) {
                bestD = d
                best = c
            }
        }
        return if (bestD < keyRadius * 1.6) best else null
    }

    private fun pathDistance(p: List<Point>): Float {
        var d = 0f
        for (i in 1 until p.size) {
            d += dist(p[i - 1].x, p[i - 1].y, p[i].x, p[i].y)
        }
        return d
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
