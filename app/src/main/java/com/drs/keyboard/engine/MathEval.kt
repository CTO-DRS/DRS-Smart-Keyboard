package com.drs.keyboard.engine

/**
 * Tiny offline math evaluator that powers the inline calculator chip in the
 * suggestion strip. Type "12*8+5" (or with Arabic-Indic digits and × ÷) and
 * the keyboard offers "= 101" — tap it to replace the expression with the
 * result. A hand-written recursive-descent parser: no eval(), no network,
 * fully deterministic.
 *
 * Grammar:
 *   expr   := term (('+'|'-') term)*
 *   term   := unary (('*'|'/'|'%') unary)*
 *   unary  := power | '-' unary
 *   power  := factor ('^' unary)?          (right-assoc, 2^3^2 = 512)
 *   factor := number | '(' expr ')' | SQRT factor
 *
 * Supported input: digits 0-9 and ٠-٩, '.' and '٫', '+ - * / % ^ ( )',
 * '×' 'x' as multiply, '÷' as divide, '√' square root. (No ':' — times
 * like 12:30 must not trigger the calculator.)
 */
object MathEval {

    private const val SQRT = '√'
    private const val MAX_LEN = 40
    private const val MAX_DEPTH = 12

    /** Characters that may appear inside a math expression. */
    private fun isMathChar(c: Char): Boolean =
        c in '0'..'9' || c in '٠'..'٩' || c == '.' || c == '٫' ||
            c == '+' || c == '-' || c == '*' || c == '/' || c == '%' ||
            c == '^' || c == '(' || c == ')' || c == '×' || c == 'x' ||
            c == 'X' || c == '÷' || c == SQRT

    /**
     * Scans the END of [before] for a trailing math expression ("the text
     * right before the cursor"). Returns the expression substring, or null
     * when there is no complete, evaluable expression at the tail.
     */
    fun trailingExpr(before: String): String? {
        if (before.isEmpty()) return null
        var start = before.length
        while (start > 0 && isMathChar(before[start - 1])) start--
        if (start >= before.length) return null
        val expr = before.substring(start)
        if (expr.length !in 3..MAX_LEN) return null
        if (!isMathExpr(expr)) return null
        return expr
    }

    /**
     * A cheap validity gate before parsing: needs at least one digit, at
     * least one operator, must not end with an operator or open paren,
     * balanced parentheses, no '//' or '**' runs, no percent in unary spot.
     */
    private fun isMathExpr(e: String): Boolean {
        var digits = 0
        var ops = 0
        var depth = 0
        for (i in e.indices) {
            val c = e[i]
            when {
                c.isDigitOfAny() -> digits++
                c == '(' -> depth++
                c == ')' -> { depth--; if (depth < 0) return false }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '%' ||
                    c == '^' || c == '×' || c == '÷' -> ops++
                c == SQRT -> {} // not counted as operator; needs digits after anyway
                c == '.' || c == '٫' -> {}
                else -> return false
            }
        }
        if (digits == 0 || ops == 0 || depth != 0) return false
        val last = e.last()
        if (last == '+' || last == '-' || last == '*' || last == '/' ||
            last == '%' || last == '^' || last == '(' || last == '×' ||
            last == '÷' || last == ':') return false
        if (e.contains("//") || e.contains("**") || e.contains("××") ||
            e.contains("÷÷") || e.contains("%%")) return false
        return true
    }

    private fun Char.isDigitOfAny(): Boolean =
        this in '0'..'9' || this in '٠'..'٩'

    /** Normalizes an expression to plain ASCII tokens before parsing. */
    private fun normalize(e: String): String {
        val sb = StringBuilder(e.length)
        for (c in e) {
            when {
                c in '٠'..'٩' -> sb.append(c - '٠')
                c == '٫' -> sb.append('.')
                c == '×' || c == 'x' || c == 'X' -> sb.append('*')
                c == '÷' -> sb.append('/')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Evaluates the expression, or null on malformed input / overflow guards. */
    fun evaluate(exprRaw: String): Double? {
        val expr = normalize(exprRaw)
        if (expr.length > MAX_LEN) return null
        val p = Parser(expr)
        val v = p.parseExpr() ?: return null
        if (!p.atEnd()) return null
        if (v.isNaN() || v.isInfinite()) return null
        return v
    }

    /** Formats a result chip: whole numbers without decimals, else up to 6 trimmed places. */
    fun format(v: Double): String {
        if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e15) {
            return v.toLong().toString()
        }
        val s = String.format(java.util.Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
        return s
    }

    // ------------------------------------------------------------------
    // Recursive-descent parser
    // ------------------------------------------------------------------

    private class Parser(private val s: String) {
        private var i = 0
        private var depth = 0

        fun atEnd() = i >= s.length

        private fun skip() { while (i < s.length && s[i] == ' ') i++ }

        private fun peek(): Char? { skip(); return if (i < s.length) s[i] else null }

        private fun eat(c: Char): Boolean {
            skip()
            if (i < s.length && s[i] == c) { i++; return true }
            return false
        }

        fun parseExpr(): Double? {
            if (depth > MAX_DEPTH) return null
            var left = parseTerm() ?: return null
            while (true) {
                val op = peek() ?: break
                if (op == '+') { i++; val r = parseTerm() ?: return null; left += r }
                else if (op == '-') { i++; val r = parseTerm() ?: return null; left -= r }
                else break
            }
            return left
        }

        private fun parseTerm(): Double? {
            if (depth > MAX_DEPTH) return null
            var left = parseUnary() ?: return null
            while (true) {
                val op = peek() ?: break
                when {
                    op == '*' -> { i++; val r = parseUnary() ?: return null; left *= r }
                    op == '/' -> {
                        i++; val r = parseUnary() ?: return null
                        if (r == 0.0) return null
                        left /= r
                    }
                    op == '%' -> {
                        i++; val r = parseUnary() ?: return null
                        if (r == 0.0) return null
                        left %= r
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseUnary(): Double? {
            skip()
            if (i < s.length && s[i] == '-') { i++; return parseUnary()?.unaryMinus() }
            if (i < s.length && s[i] == '+') { i++; return parseUnary() }
            return parsePower()
        }

        private fun parsePower(): Double? {
            val base = parseFactor() ?: return null
            if (peek() == '^') {
                i++
                val exp = parseUnary() ?: return null
                return pow(base, exp)
            }
            return base
        }

        private fun parseFactor(): Double? {
            skip()
            if (i >= s.length) return null
            val c = s[i]
            if (c == '(') {
                i++
                depth++
                val v = parseExpr()
                depth--
                if (v == null || !eat(')')) return null
                return v
            }
            if (c == SQRT) {
                i++
                val v = parseFactor() ?: return null
                return if (v < 0) null else kotlin.math.sqrt(v)
            }
            return parseNumber()
        }

        private fun parseNumber(): Double? {
            skip()
            val start = i
            while (i < s.length && s[i].isDigitOfAny()) i++
            if (i < s.length && s[i] == '.') {
                i++
                while (i < s.length && s[i].isDigitOfAny()) i++
            }
            if (i == start) return null
            // reject "1.2.3" style tokens: next char after a number must not be '.' followed by a digit
            if (i < s.length && s[i] == '.' && i + 1 < s.length && s[i + 1].isDigitOfAny()) return null
            return s.substring(start, i).toDoubleOrNull()
        }

        private fun pow(b: Double, e: Double): Double? {
            if (kotlin.math.abs(e) > 64) return null
            val r = Math.pow(b, e)
            return if (r.isNaN() || r.isInfinite()) null else r
        }
    }
}
