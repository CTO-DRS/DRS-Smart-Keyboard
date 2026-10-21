/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ai

/**
 * ذراع L3 «توقع الكلمة التالية الأعمق» — محلل حالة الجملة (the
 * sentence-state analyzer) — the pure, closed-catalog signal that BOTH
 * the next-word prediction and the tiles' advisory share (نقطة الدمج
 * الواحدة — one signal, two consumers, no drift).
 *
 * THE CROSS-BOUNDARY BUG IT FIXES: the provider's word scan skips
 * trailing punctuation, so «كتبت الرسالة. » handed the FINISHED
 * sentence's last word to the bigram tables as the context of the NEXT
 * sentence — cross-boundary noise. This analyzer draws the line: a tail
 * that ends (after folding whitespace and closed closers) on a terminal
 * mark is a FRESH SENTENCE START, and the caller must not cross it.
 *
 * THE CLOSED CATALOGS (everything is enumerated, nothing is vibes):
 *
 *  1. [TERMINALS] — the sentence terminators: `. ! ؟ ? …`. This is the
 *     MIRROR of the ender class in DrsTextTools' SENTENCE_BOUNDARY regex
 *     (the «جملة لكل سطر» tool) — the equality is PINNED by tests, so
 *     the predictor and the tiles can never disagree about what ends a
 *     sentence. Comma/colon/semicolon are deliberately NOT here: they
 *     are mid-sentence separators and the word before them is a VALID
 *     bigram context.
 *
 *  2. [CLOSERS] — closing quotes/brackets that may sit after a
 *     terminator without opening a new sentence («انتهى."»).
 *
 *  3. [OPENERS] — the closed sentence-opener catalog: fourteen
 *     high-frequency single-word Arabic starters in a FIXED order (the
 *     same epistemic class as DrsContextRanker's answer openers — a
 *     curated language floor, never a guess). They surface ONLY in the
 *     fresh-start path of Arabic windows; Latin windows fall back to the
 *     user's own vocabulary alone.
 *
 *  4. [ORDINAL_CHAIN] — the closed enumeration chain
 *     (أولًا→ثانيًا→…→ثامنًا, eight links): when the last word IS a
 *     chain link, its successor is a DETERMINISTIC continuation — the
 *     language's own structure, the same certainty class as the curated
 *     trigram chains. The chain keys are matched through the
 *     CALLER-INJECTED normalizer (the DrsFusedCorrection pattern: the
 *     same function serves the provider's bigram keys, so the two can
 *     never drift).
 *
 * THE LAWS (pinned by the reference simulation `check_l3_sentence.py`
 * and the JVM tests):
 *  - a tail whose last word is followed (after whitespace folding) by
 *    ONLY terminators/closers is a fresh start; an empty trailing run
 *    (cursor right after the word) or ANY other char is mid-sentence;
 *  - a fresh start SUPPRESSES the ordinal successor — a sentence that
 *    ENDED with an ordinal likely finished the enumeration, and
 *    injecting «ثانيًا» after «أولًا.» would be a guess (يقين doctrine);
 *  - the scan is bounded to the last [TAIL_WINDOW] chars — the hot path
 *    stays O(window), never O(text).
 *
 * Purity: no Android types, no I/O, no global mutable state — every
 * call is deterministic on its input.
 */
object DrsSentenceContext {

    /** The closed sentence terminators — the pinned mirror of the provider's SENTENCE_TERMINATORS (including the Urdu/Arabic full stop «۔»). */
    val TERMINALS: Set<Char> = setOf('.', '!', '؟', '?', '…', '۔')

    /** Closed closers that may trail a terminator inside the same sentence unit. */
    val CLOSERS: Set<Char> = setOf('»', '"', '\'', ')', ']', '}')

    /** The bounded tail window — the hot path never scans beyond this. */
    const val TAIL_WINDOW = 64

    /** How many openers the fresh-start path surfaces by default. */
    const val OPENERS_TAKE = 4

    /** The closed opener catalog, natural display forms, fixed order. */
    val OPENERS: List<String> = listOf(
        "كان", "قال", "ثم", "لكن", "أما", "كذلك", "عندما",
        "بعد", "قبل", "الآن", "اليوم", "ربما", "إذن", "هذا",
    )

    /** The closed enumeration chain — natural display forms, fixed order. */
    val ORDINAL_CHAIN: List<String> = listOf(
        "أولًا", "ثانيًا", "ثالثًا", "رابعًا", "خامسًا", "سادسًا", "سابعًا", "ثامنًا",
    )

    /** The sentence state the provider's next-word path branches on. */
    data class State(
        /** True when the tail ends on a sentence boundary with NO word after it. */
        val freshStart: Boolean,
        /** The deterministic next ordinal when the tail word is a chain link (mid-sentence only). */
        val ordinalSuccessor: String?,
    )

    /**
     * Analyzes the text before the cursor. Deterministic, bounded, pure.
     *
     * @param normalize the SAME normalizer the caller's bigram keys use
     * @param isWordChar the SAME word-internal law the caller's word
     *   scan uses (the p6-A1 predicate) — one word law, no drift
     */
    fun analyze(
        before: CharSequence,
        normalize: (String) -> String,
        isWordChar: (Char) -> Boolean,
    ): State {
        if (before.isBlank()) return State(false, null)
        val tail = before.subSequence(
            (before.length - TAIL_WINDOW).coerceAtLeast(0),
            before.length,
        ).toString()
        val last = lastWord(tail, isWordChar) ?: return State(false, null)
        val trailing = tail.substring(last.second)
        val stripped = trailing.asSequence().filterNot { it.isWhitespace() }.toList()
        val freshStart = stripped.isNotEmpty() && stripped.all { it in TERMINALS || it in CLOSERS }
        var successor: String? = null
        if (!freshStart) {
            val norm = normalize(tail.substring(last.first, last.second))
            for (i in ORDINAL_CHAIN.indices) {
                if (normalize(ORDINAL_CHAIN[i]) == norm) {
                    if (i + 1 < ORDINAL_CHAIN.size) successor = ORDINAL_CHAIN[i + 1]
                    break
                }
            }
        }
        return State(freshStart, successor)
    }

    /**
     * The opener slice for the fresh-start path — display forms, fixed
     * catalog order, at most [limit]. A non-positive limit is the honest
     * empty.
     */
    fun openers(limit: Int): List<String> {
        if (limit <= 0) return emptyList()
        return OPENERS.take(limit)
    }

    /** The last word span of [tail] (start inclusive, end exclusive), or null. */
    private fun lastWord(tail: String, isWordChar: (Char) -> Boolean): Pair<Int, Int>? {
        var end = tail.length
        while (end > 0 && !isWordChar(tail[end - 1])) end--
        if (end == 0) return null
        var start = end
        while (start > 0 && isWordChar(tail[start - 1])) start--
        return start to end
    }
}
