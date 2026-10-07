package com.drs.keyboard.engine

import kotlin.math.ln

/**
 * Offline password-strength estimation. Pure deterministic math — charset
 * entropy plus a handful of structural penalties (common passwords, repeats,
 * sequences, keyboard runs). Nothing is stored, transmitted, or learned.
 *
 *   entropy ≈ length × log2(charsetSize), then penalties on 0–100 scale.
 */
object StrengthMeter {

    /** 0 = weak, 1 = fair, 2 = good, 3 = strong */
    class Result(val level: Int, val score: Int)

    // top common passwords (lowercase) — instant zero, offline list
    private val COMMON = setOf(
        "123456", "password", "123456789", "12345678", "12345", "qwerty",
        "1234567890", "1234567", "111111", "123123", "abc123", "password1",
        "1234", "qwerty123", "000000", "iloveyou", "1q2w3e4r", "qwertyuiop",
        "monkey", "dragon", "123321", "654321", "666666", "123qwe",
        "asdfghjkl", "zxcvbnm", "asdfgh", "1q2w3e", "letmein", "welcome",
        "admin", "login", "princess", "sunshine", "football", "baseball",
        "superman", "batman", "trustno1", "shadow", "master", "hello",
        "freedom", "whatever", "qazwsx", "michael", "nissan", "samsung"
    )

    // keyboard runs (both hands, common rows)
    private val KEY_RUNS = listOf(
        "qwertyuiop", "asdfghjkl", "zxcvbnm", "qazwsxedc", "1qaz2wsx",
        "azertyuiop", "poiuytr", "lkjhgf", "mnbvcx"
    )

    fun analyze(pw: String): Result {
        if (pw.isEmpty()) return Result(-1, 0) // nothing to show
        if (pw.lowercase() in COMMON) return Result(0, 5)

        var charset = 0.0
        var hasLower = false; var hasUpper = false
        var hasDigit = false; var hasSymbol = false; var hasWide = false
        for (c in pw) {
            when {
                c.isLowerCase() -> hasLower = true
                c.isUpperCase() -> hasUpper = true
                c.isDigit() -> hasDigit = true
                c.code < 128 -> hasSymbol = true
                else -> hasWide = true
            }
        }
        if (hasLower) charset += 26.0
        if (hasUpper) charset += 26.0
        if (hasDigit) charset += 10.0
        if (hasSymbol) charset += 33.0
        if (hasWide) charset += 100.0
        if (charset == 0.0) charset = 26.0

        val entropyBits = pw.length * (ln(charset) / ln(2.0))
        // 100% at ~ 100 bits (≈ 15 random chars over full ASCII)
        var score = ((entropyBits / 100.0) * 100).toInt().coerceIn(0, 100)

        // structural penalties
        val counts = HashMap<Char, Int>(16)
        for (c in pw) counts[c] = (counts[c] ?: 0) + 1
        val maxRepeat = counts.values.maxOrNull() ?: 0
        if (maxRepeat * 2 >= pw.length && pw.length >= 3) score -= 14     // aaab, 111222…
        if (hasSequence(pw)) score -= 10                                   // abc, 123, 456…
        if (hasKeyboardRun(pw.lowercase())) score -= 12                    // qwerty, asdf…
        if (!hasUpper && !hasDigit && !hasSymbol && pw.length < 9) score -= 10
        if (pw.length < 6) score = (score * 3 / 5)                         // short = hopeless
        score = score.coerceIn(0, 100)

        val level = when {
            score < 35 -> 0
            score < 60 -> 1
            score < 82 -> 2
            else -> 3
        }
        return Result(level, score)
    }

    /** abc, cba, 123, 987, aaa runs of 3+. */
    private fun hasSequence(pw: String): Boolean {
        if (pw.length < 3) return false
        var up = 1; var down = 1; var same = 1
        var best = 1
        for (i in 1 until pw.length) {
            val d = pw[i] - pw[i - 1]
            up = if (d == 1) up + 1 else 1
            down = if (d == -1) down + 1 else 1
            same = if (d == 0) same + 1 else 1
            best = maxOf(best, up, down, same)
        }
        return best >= 3
    }

    /** qwerty / asdf / zxcv style runs of 3+ chars. */
    private fun hasKeyboardRun(low: String): Boolean {
        for (run in KEY_RUNS) {
            for (i in 0..run.length - 3) {
                if (low.contains(run.substring(i, i + 3))) return true
            }
        }
        return false
    }
}
