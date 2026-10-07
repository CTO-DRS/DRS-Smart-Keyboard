package com.drs.keyboard.keyboard

import kotlin.math.abs

/**
 * Transient input state: current layout mode, shift cycle (off → once →
 * locked), active language and the geometry-derived letter adjacency map
 * that powers weighted autocorrect.
 */
class KeyboardState {

    companion object {
        const val MODE_ALPHA = 0
        const val MODE_SYMBOLS = 1
        const val MODE_NUM = 2
        const val MAX_LANGS = 2
    }

    var lang: String = "en"
    var mode: Int = MODE_ALPHA
    var shifted: Boolean = false
    var capsLocked: Boolean = false
    var numberRow: Boolean = false

    /** populated by the service after building layouts; lang -> letter -> neighbors */
    var adjacency: Map<String, Map<Char, Set<Char>>> = emptyMap()

    /** ordered list of enabled languages, e.g. ["en", "ar"] */
    var enabledLangs: List<String> = listOf("en")

    fun resetForNewField(sentenceStart: Boolean, autoCap: Boolean) {
        mode = MODE_ALPHA
        capsLocked = false
        shifted = autoCap && sentenceStart
    }

    /** Press of the shift key: off→once / once→locked / locked→off. */
    fun tapShift() {
        if (!shifted && !capsLocked) {
            shifted = true
        } else if (shifted && !capsLocked) {
            shifted = false
            capsLocked = true
        } else {
            shifted = false
            capsLocked = false
        }
    }

    /** Shift applies to exactly one character, then decays (unless locked). */
    fun consumeShift() {
        if (!capsLocked) shifted = false
    }

    fun setSentenceShift(on: Boolean) {
        if (!capsLocked) shifted = on
    }

    fun switchModeToSymbols() {
        mode = if (mode == MODE_ALPHA) MODE_SYMBOLS else MODE_ALPHA
        // leaving alpha mode keeps caps-locked active
    }

    fun cycleLanguage(): String {
        if (enabledLangs.size < 2) return lang
        val idx = enabledLangs.indexOf(lang)
        lang = enabledLangs[(idx + 1).mod(enabledLangs.size)]
        mode = MODE_ALPHA
        return lang
    }

    /**
     * Builds letter adjacency from layout geometry: rows sit above each
     * other, each row normalized to 10 width-units and centered. Two letters
     * are neighbors when their horizontal distance ≤ 1.15 units and their
     * rows differ by ≤ 1.
     */
    fun buildAdjacency() {
        val map = HashMap<String, Map<Char, Set<Char>>>(4)
        for (lang in enabledLangs.distinct()) {
            val rows = Layouts.rows(lang, MODE_ALPHA, shifted = false, numberRow = false)
            data class Cell(val ch: Char, val cx: Float, val rowIdx: Int)
            val cells = ArrayList<Cell>(40)
            for ((rowIdx, row) in rows.withIndex()) {
                var total = 0f
                for (k in row) total += k.widthUnits
                var x = 0f
                for (k in row) {
                    if (k.type == KeyDef.KeyType.CHAR && k.code > 0 &&
                        k.label.length == 1 && k.label[0].isLetter() && k.shiftLabel?.length != 2
                    ) {
                        val cx = x + k.widthUnits / 2f - total / 2f // centered offset
                        cells.add(Cell(k.label[0].lowercaseChar(), cx, rowIdx))
                    }
                    x += k.widthUnits
                }
            }
            val adj = HashMap<Char, MutableSet<Char>>(40)
            for (c in cells) adj.getOrPut(c.ch) { HashSet() }
            for (i in cells.indices) {
                for (j in i + 1 until cells.size) {
                    val a = cells[i]
                    val b = cells[j]
                    if (abs(a.rowIdx - b.rowIdx) <= 1 && abs(a.cx - b.cx) <= 1.15f) {
                        adj[a.ch]?.add(b.ch)
                        adj[b.ch]?.add(a.ch)
                    }
                }
            }
            map[lang] = adj
        }
        adjacency = map
    }

    fun neighborsFor(lang: String): Map<Char, Set<Char>> =
        adjacency[lang] ?: emptyMap()
}
