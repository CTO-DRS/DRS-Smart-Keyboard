/*
 * Copyright (C) 2021-2025 The DRS Smart Keyboard Project
 * Copyright (C) 2025 DRS Smart Keyboard contributors
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * Unit tests for the pure text transformation engine behind the technical
 * text tools panel (). Every tool the UI exposes must behave
 * exactly as its description promises - these tests pin that behavior.
 */
class DrsTextToolsTest : FunSpec({

    val en = Locale.US

    test("uppercase and lowercase") {
        DrsTextTools.apply(DrsTextTool.UPPERCASE, "Hello World", en) shouldBe "HELLO WORLD"
        DrsTextTools.apply(DrsTextTool.LOWERCASE, "Hello World", en) shouldBe "hello world"
    }

    test("title case uppercases word starts only") {
        DrsTextTools.apply(DrsTextTool.TITLE_CASE, "hello world", en) shouldBe "Hello World"
        DrsTextTools.apply(DrsTextTool.TITLE_CASE, "héllo again", en) shouldBe "Héllo Again"
        // Punctuation between words starts a new word.
        DrsTextTools.apply(DrsTextTool.TITLE_CASE, "hello, world!", en) shouldBe "Hello, World!"
    }

    test("sentence case capitalizes after boundaries") {
        DrsTextTools.apply(DrsTextTool.SENTENCE_CASE, "HELLO WORLD. HOW ARE YOU? FINE!", en) shouldBe
            "Hello world. How are you? Fine!"
        DrsTextTools.apply(DrsTextTool.SENTENCE_CASE, "first line\nsecond line", en) shouldBe
            "First line\nSecond line"
    }

    test("trim spaces collapses runs and edges") {
        DrsTextTools.apply(DrsTextTool.TRIM_SPACES, "  a   b  c  ", en) shouldBe "a b c"
        // Newlines are preserved by design (only horizontal spaces collapse).
        DrsTextTools.apply(DrsTextTool.TRIM_SPACES, "a\nb", en) shouldBe "a\nb"
    }

    test("trim line edges keeps empty lines") {
        DrsTextTools.apply(DrsTextTool.TRIM_LINE_EDGES, "  a \n b  ", en) shouldBe "a\nb"
    }

    test("remove empty lines drops blank only") {
        DrsTextTools.apply(DrsTextTool.REMOVE_EMPTY_LINES, "a\n\n   \nb", en) shouldBe "a\nb"
    }

    test("remove line breaks joins with single space") {
        DrsTextTools.apply(DrsTextTool.REMOVE_LINE_BREAKS, "a\n  b\nc", en) shouldBe "a b c"
    }

    test("sort lines orders alphabetically") {
        DrsTextTools.apply(DrsTextTool.SORT_LINES, "cherry\napple\nbanana", en) shouldBe
            "apple\nbanana\ncherry"
    }

    test("remove duplicate lines keeps first occurrence") {
        DrsTextTools.apply(DrsTextTool.REMOVE_DUPLICATE_LINES, "a\nb\na\nc\nb", en) shouldBe "a\nb\nc"
    }

    test("number lines prefixes each line with its 1-based order") {
        DrsTextTools.apply(DrsTextTool.NUMBER_LINES, "a\nb\nc", en) shouldBe "1. a\n2. b\n3. c"
        // A trailing newline is preserved through the transform.
        DrsTextTools.apply(DrsTextTool.NUMBER_LINES, "a\nb\n", en) shouldBe "1. a\n2. b\n"
        // Blank lines are numbered too — the transform never reorders.
        DrsTextTools.apply(DrsTextTool.NUMBER_LINES, "x\n\ny", en) shouldBe "1. x\n2. \n3. y"
    }

    test("reverse lines flips the line order") {
        DrsTextTools.apply(DrsTextTool.REVERSE_LINES, "a\nb\nc", en) shouldBe "c\nb\na"
        // A trailing newline is preserved through the transform.
        DrsTextTools.apply(DrsTextTool.REVERSE_LINES, "a\nb\n", en) shouldBe "b\na\n"
    }

    test("wrap quotes surrounds text with locale-aware marks") {
        val ar = Locale("ar")
        DrsTextTools.apply(DrsTextTool.WRAP_QUOTES, "مرحبا", ar) shouldBe "«مرحبا»"
        DrsTextTools.apply(DrsTextTool.WRAP_QUOTES, "hello", en) shouldBe "\"hello\""
        // Multiline text is wrapped as one block, not per line.
        DrsTextTools.apply(DrsTextTool.WRAP_QUOTES, "a\nb", en) shouldBe "\"a\nb\""
    }

    test("remove diacritics strips arabic harakat") {
        // "مُحَمَّد" with diacritics -> "محمد" without.
        DrsTextTools.apply(DrsTextTool.REMOVE_DIACRITICS, "مُحَمَّد", en) shouldBe "محمد"
        // Plain text passes through unchanged.
        DrsTextTools.apply(DrsTextTool.REMOVE_DIACRITICS, "السلام عليكم", en) shouldBe "السلام عليكم"
    }

    test("normalize punctuation removes space before and adds after") {
        DrsTextTools.apply(DrsTextTool.NORMALIZE_PUNCTUATION, "hello , world !", en) shouldBe
            "hello, world!"
        // Arabic comma and question mark behave the same.
        DrsTextTools.apply(DrsTextTool.NORMALIZE_PUNCTUATION, "مرحبا ، كيف حالك ؟", en) shouldBe
            "مرحبا، كيف حالك؟"
    }

    test("normalize punctuation never breaks domains or numbers") {
        // '.' before an Arabic letter gains a space; before Latin it must not.
        DrsTextTools.apply(DrsTextTool.NORMALIZE_PUNCTUATION, "انتهى.كلمة example.com 3.14", en) shouldBe
            "انتهى. كلمة example.com 3.14"
    }

    test("clean text combines cleanup steps") {
        DrsTextTools.apply(DrsTextTool.CLEAN_TEXT, "  hello , world ! \n\n  ", en) shouldBe
            "hello, world!"
    }

    test("count info reports real counts") {
        val (chars, words, lines) = DrsTextTools.countInfo("hello drs world\nsecond line")
        chars shouldBe 27
        words shouldBe 5
        lines shouldBe 2
    }

    // digit conversion, tatweel removal, blank-line collapsing.

    test("to arabic digits converts only western digits") {
        DrsTextTools.apply(DrsTextTool.TO_ARABIC_DIGITS, "سنة 2026", en) shouldBe "سنة ٢٠٢٦"
        // Mixed content: letters, punctuation and spacing are untouched.
        DrsTextTools.apply(DrsTextTool.TO_ARABIC_DIGITS, "a1.b/c-2", en) shouldBe "a١.b/c-٢"
        // No digits -> unchanged.
        DrsTextTools.apply(DrsTextTool.TO_ARABIC_DIGITS, "hello", en) shouldBe "hello"
    }

    test("to western digits converts arabic-indic and persian digits") {
        DrsTextTools.apply(DrsTextTool.TO_WESTERN_DIGITS, "سنة ٢٠٢٦", en) shouldBe "سنة 2026"
        // Extended Arabic-Indic (Persian/Urdu) ۴۵۶.
        DrsTextTools.apply(DrsTextTool.TO_WESTERN_DIGITS, "\u06F4\u06F5\u06F6", en) shouldBe "456"
        // Letters and punctuation are untouched.
        DrsTextTools.apply(DrsTextTool.TO_WESTERN_DIGITS, "ابجد، x", en) shouldBe "ابجد، x"
    }

    test("digit conversions are inverse operations") {
        val original = "السطر 1 والأرقام 234 و5678"
        val roundTrip = DrsTextTools.apply(
            DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTools.apply(DrsTextTool.TO_ARABIC_DIGITS, original, en),
            en,
        )
        roundTrip shouldBe original
    }

    test("remove tatweel strips kashida only") {
        DrsTextTools.apply(DrsTextTool.REMOVE_TATWEEL, "مرحـــبــا", en) shouldBe "مرحبا"
        // Other characters survive untouched.
        DrsTextTools.apply(DrsTextTool.REMOVE_TATWEEL, "aـbـ1", en) shouldBe "ab1"
        // No tatweel -> unchanged.
        DrsTextTools.apply(DrsTextTool.REMOVE_TATWEEL, "بلا تطويل", en) shouldBe "بلا تطويل"
    }

    test("collapse empty lines merges consecutive blank runs into one") {
        DrsTextTools.apply(DrsTextTool.COLLAPSE_EMPTY_LINES, "a\n\n\n\nb\n\nc", en) shouldBe "a\n\nb\n\nc"
        // A single blank separator is preserved as-is.
        DrsTextTools.apply(DrsTextTool.COLLAPSE_EMPTY_LINES, "a\n\nb", en) shouldBe "a\n\nb"
        // Leading blank runs collapse to one leading blank line.
        DrsTextTools.apply(DrsTextTool.COLLAPSE_EMPTY_LINES, "\n\n\na", en) shouldBe "\na"
        // The trailing newline survives.
        DrsTextTools.apply(DrsTextTool.COLLAPSE_EMPTY_LINES, "a\n\n", en) shouldBe "a\n\n"
        // No blank runs -> unchanged.
        DrsTextTools.apply(DrsTextTool.COLLAPSE_EMPTY_LINES, "a\nb", en) shouldBe "a\nb"
    }

    /**
     * DRS p9 (Q3): every tool must honor at least one REAL contract element
     * on hostile input — the old `(result.length >= 0) shouldBe true` could
     * never fail. Grouped by tool family; an uncovered tool fails loudly so
     * future tools must pin an invariant too.
     */
    fun assertMeaningfulResult(tool: DrsTextTool, input: String, result: String) {
        val punctBeforeSpace = Regex("[ \\t\\u00A0]+([,;:.!?،؛…؟)\\]\\}»])")
        when (tool) {
            // 1:1 case mappings, per-char maps and line re-orderings keep the exact length
            DrsTextTool.UPPERCASE, DrsTextTool.LOWERCASE, DrsTextTool.TITLE_CASE,
            DrsTextTool.SENTENCE_CASE, DrsTextTool.TOGGLE_CASE,
            DrsTextTool.SORT_LINES, DrsTextTool.SORT_LINES_DESC, DrsTextTool.SORT_LINES_BY_LENGTH,
            DrsTextTool.REVERSE_LINES, DrsTextTool.NORMALIZE_ARABIC,
            DrsTextTool.TO_ARABIC_DIGITS, DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTool.TO_ARABIC_PUNCTUATION, DrsTextTool.TO_WESTERN_PUNCTUATION,
            -> (result.length == input.length) shouldBe true

            // removal/cleanup families may only shrink and never blank real content
            DrsTextTool.TRIM_SPACES, DrsTextTool.TRIM_LINE_EDGES, DrsTextTool.REMOVE_EMPTY_LINES,
            DrsTextTool.CLEAN_TEXT, DrsTextTool.TRIM_BLANK_EDGES, DrsTextTool.REMOVE_DIACRITICS,
            DrsTextTool.REMOVE_TATWEEL, DrsTextTool.REMOVE_DUPLICATE_WORDS,
            DrsTextTool.REMOVE_ZERO_WIDTH, DrsTextTool.STRIP_EMOJI, DrsTextTool.REMOVE_PUNCTUATION,
            -> {
                (result.length <= input.length) shouldBe true
                result.isNotBlank().shouldBeTrue()
            }

            DrsTextTool.REMOVE_LINE_BREAKS -> {
                (result.length <= input.length) shouldBe true
                result.contains('\n').shouldBeFalse()
            }

            // word order flips, the words themselves survive
            DrsTextTool.REVERSE_WORDS ->
                result.split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted() shouldBe
                    input.split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted()

            DrsTextTool.NUMBER_LINES -> {
                result.lines().size shouldBe input.lines().size
                result.lines().forEachIndexed { index, line ->
                    line.startsWith("${index + 1}. ").shouldBeTrue()
                }
            }

            DrsTextTool.REMOVE_DUPLICATE_LINES -> {
                (result.lines().size <= input.lines().size) shouldBe true
                result.lines().first() shouldBe input.lines().first() // first occurrence wins
            }

            DrsTextTool.WRAP_QUOTES -> {
                result.length shouldBe input.length + 2
                result.startsWith("\"").shouldBeTrue()
                result.endsWith("\"").shouldBeTrue()
            }

            DrsTextTool.WRAP_PARENS -> {
                result.length shouldBe input.length + 2
                result.startsWith("(").shouldBeTrue()
                result.endsWith(")").shouldBeTrue()
            }

            DrsTextTool.COLLAPSE_EMPTY_LINES ->
                (result.endsWith("\n") == input.endsWith("\n")).shouldBeTrue()

            DrsTextTool.TABS_TO_SPACES -> {
                result.contains('\t').shouldBeFalse()
                (result.length >= input.length) shouldBe true
            }

            DrsTextTool.SPACES_TO_TABS -> result.contains("    ").shouldBeFalse()

            DrsTextTool.NORMALIZE_PUNCTUATION -> punctBeforeSpace.containsMatchIn(result).shouldBeFalse()

            // idempotent normalizers: applying twice changes nothing further
            DrsTextTool.SENTENCE_PER_LINE, DrsTextTool.NORMALIZE_ARABIC_FORMS,
            DrsTextTool.SEPARATE_DIGIT_LETTERS,
            -> DrsTextTools.apply(tool, result, en) shouldBe result

            DrsTextTool.SPLIT_TO_LINES ->
                result.none { it == ',' || it == '،' || it == ';' || it == '؛' }.shouldBeTrue()

            DrsTextTool.JOIN_LINES -> result.contains('\n').shouldBeFalse()

            DrsTextTool.REMOVE_ALL_SPACES ->
                result.none { it == ' ' || it == '\t' || it == '\u00A0' }.shouldBeTrue()

            DrsTextTool.URL_ENCODE -> result.contains(' ').shouldBeFalse()

            // malformed "%s" escapes are preserved verbatim (documented catch path)
            DrsTextTool.URL_DECODE -> result shouldBe input

            DrsTextTool.COUNT -> result shouldBe input // the info tool never transforms

            // (public): the hostile input carries NO Arabic
            // LETTERS — only bare combining marks (ٍَّ), which strip to
            // nothing and are honestly kept as-is — so nothing can be
            // vocalized: the byte-identical passthrough IS the contract.
            DrsTextTool.TASHKEEL_TEXT -> result shouldBe input

            // the hostile input carries NO digits — the
            // number-to-words tool is a byte-identical passthrough on
            // anything it cannot parse as a clean integer.
            DrsTextTool.NUMBER_WORDS -> result shouldBe input

            // the hostile input ends in «most» — no closed
            // currency token peels from the tail and no clean amount
            // parses, so the tafqit tool is a byte-identical passthrough.
            DrsTextTool.TAFQIT -> result shouldBe input

            // the hostile input carries no separator from the
            // closed date set {- / .} and no digit components — no clean
            // date parses, so the date-words tool is a byte-identical
            // passthrough.
            DrsTextTool.DATE_WORDS -> result shouldBe input

            // the hostile input carries no clock colon and
            // no clean two-component time — the clock-time-words tool is
            // a byte-identical passthrough.
            DrsTextTool.TIME_WORDS -> result shouldBe input

            // the hostile input carries no fraction slash
            // and no digit components — the fraction-words tool is a
            // byte-identical passthrough.
            DrsTextTool.FRACTION_WORDS -> result shouldBe input

            // the hostile input carries no separator from
            // the closed date set {- / .} and no digit components — the
            // weekday tool is a byte-identical passthrough.
            DrsTextTool.WEEKDAY -> result shouldBe input

            // the hostile input carries no digits at all —
            // the ordinal-words tool is a byte-identical passthrough.
            DrsTextTool.ORDINAL_WORDS -> result shouldBe input

            // the hostile input carries no decimal separator
            // from the closed pair { . ، ٫ } — the decimal-words tool is
            // a byte-identical passthrough.
            DrsTextTool.DECIMAL_WORDS -> result shouldBe input

            // the hostile input carries no Arabic number
            // words — the words-to-number mirror is a byte-identical
            // passthrough.
            DrsTextTool.WORDS_TO_NUMBER -> result shouldBe input

            // the hostile input carries no separator from
            // the closed date set {- / .} and no digit components —
            // the Gregorian→Hijri conversion is a byte-identical
            // passthrough.
            DrsTextTool.GREGORIAN_TO_HIJRI -> result shouldBe input

            // the hostile input carries no separator from
            // the closed date set {- / .} and no digit components —
            // the Hijri→Gregorian conversion is a byte-identical
            // passthrough.
            DrsTextTool.HIJRI_TO_GREGORIAN -> result shouldBe input

            // the natural sort is a pure PERMUTATION of the
            // lines — every line in, every line out, none invented and
            // none lost (the collator may order them either way on a
            // hostile pair; the multiset may never change). Both sides
            // read through lines() — its \r handling is the tool's own.
            DrsTextTool.SORT_LINES_NATURAL ->
                result.lines().sorted() shouldBe input.lines().sorted()

            // the slug is never empty (the punctuation-only
            // passthrough keeps the input verbatim) and never carries
            // whitespace — every run became one dash or fell away.
            DrsTextTool.SLUGIFY -> {
                result.isEmpty().shouldBeFalse()
                result.none { it.isWhitespace() }.shouldBeTrue()
            }

            // snake_case owns the whitespace — every run is
            // a word boundary, so the result carries none.
            DrsTextTool.TO_SNAKE_CASE ->
                result.none { it.isWhitespace() }.shouldBeTrue()

            // the hostile tokens carry emoji and fullwidth
            // marks (non-ASCII) — the camel transform refuses the whole
            // text byte-identically, the honest sibling.
            DrsTextTool.TO_CAMEL_CASE -> result shouldBe input

            // the hostile tail is Latin («most») — no
            // Arabic trigger equals the normalized tail, so no rule
            // fires and the text returns byte-identical.
            DrsTextTool.IMPROVE_PHRASES -> result shouldBe input

            // the hostile input carries single spaces only
            // (no whitespace runs), no space-before-closing-punctuation
            // shapes and no edge whitespace — the bounded fixed-point
            // normalizer returns it byte-identically.
            DrsTextTool.NORMALIZE_SPACING -> result shouldBe input

            // the hostile input carries NO markdown shape
            // (no leading #/>/-/•, no emphasis pairs, no links, no
            // fences) — every closed rule refuses it, so the web-paste
            // cleaner is a byte-identical passthrough.
            DrsTextTool.CLEAN_MARKDOWN -> result shouldBe input

            // the hostile input carries NO tag, entity or markup
            // shape (no '<', no '&') — every closed rule refuses it, so
            // the HTML-paste cleaner is a byte-identical passthrough.
            DrsTextTool.HTML_TO_TEXT -> result shouldBe input

            // the hostile input carries NO link shape (no scheme
            // URL, no www. address, no strict email) — the harvester
            // finds nothing and returns the text byte-identically,
            // never an empty or invented row.
            DrsTextTool.EXTRACT_LINKS -> result shouldBe input

            // the hostile input carries NO run of three-plus
            // identical Arabic letters (its Arabic run is ٍَّ marks
            // and single letters) — the stretch cleaner is a
            // byte-identical passthrough.
            DrsTextTool.REMOVE_LETTER_STRETCH -> result shouldBe input

            // the encoder is TOTAL — the hostile text speaks its
            // exact UTF-8 Base64 — so the pinned invariant is the
            // mirror itself: the decoder restores the exact bytes.
            DrsTextTool.BASE64_ENCODE ->
                DrsTextTools.apply(DrsTextTool.BASE64_DECODE, result, en) shouldBe input

            // the hostile field carries $ { } ٍَّ and the emoji —
            // not the strict Base64 alphabet — so the decoder
            // refuses it byte-identically, never a replacement char.
            DrsTextTool.BASE64_DECODE -> result shouldBe input

            // the hostile text carries emoji, $, { } and Arabic marks —
            // NOT the closed morse alphabet — so the encoder refuses
            // the whole text byte-identically, never an invented sign.
            DrsTextTool.MORSE_ENCODE -> result shouldBe input

            // the hostile field carries letters and marks — not the
            // dot/dash glyph shape — so the decoder refuses it
            // byte-identically. Decode must never destroy typing.
            DrsTextTool.MORSE_DECODE -> result shouldBe input

            // the census is an INFO tool — apply() is the read-only
            // discipline itself: the text returns byte-identical and
            // callers read [DrsTextTools.inspectText] instead.
            DrsTextTool.TEXT_INSPECT -> result shouldBe input

            else -> error("DRS p9 (Q3): tool ${tool.name} has no pinned hostile-input invariant")
        }
    }

    test("tools never throw on hostile input and every result honors its contract") {
        val hostile = "\uD83C\uDF0E \u0000 \t\n\r$ { } %s %d ٍَّ most"
        for (tool in DrsTextTool.entries) {
            if (tool.isEditorOp) continue
            val result = DrsTextTools.apply(tool, hostile, en)
            assertMeaningfulResult(tool, hostile, result)
        }
        // Empty input is a no-op for all transformation tools.
        for (tool in DrsTextTool.entries) {
            if (tool.isEditorOp || tool.isInfoOnly) continue
            DrsTextTools.apply(tool, "", en) shouldBe ""
        }
    }

    test("every tool code is inside the dispatch range") {
        for (tool in DrsTextTool.entries) {
            (tool.code in DrsTextTool.CODE_RANGE) shouldBe true
            DrsTextTool.fromCode(tool.code) shouldBe tool
        }
    }

    // -----------------------------------------------------------
    // SORT_LINES_DESC
    // -----------------------------------------------------------

    test("sort lines descending orders lines with the locale collator") {
        DrsTextTools.apply(DrsTextTool.SORT_LINES_DESC, "banana\napple\ncherry", en) shouldBe
            "cherry\nbanana\napple"
        // The ascending counterpart is the exact inverse on distinct lines.
        DrsTextTools.apply(
            DrsTextTool.SORT_LINES,
            DrsTextTools.apply(DrsTextTool.SORT_LINES_DESC, "b\na\nc", en),
            en,
        ) shouldBe "a\nb\nc"
    }

    test("sort lines descending keeps blank lines and the trailing newline") {
        DrsTextTools.apply(DrsTextTool.SORT_LINES_DESC, "b\n\na\n", en) shouldBe "b\na\n\n"
    }

    // -----------------------------------------------------------
    // NORMALIZE_ARABIC
    // -----------------------------------------------------------

    test("normalize arabic unifies hamza carriers, ta-marbuta and alef maqsura") {
        // الأعلى: hamza-carrier alef AND final alef maqsura both unify.
        DrsTextTools.apply(DrsTextTool.NORMALIZE_ARABIC, "أحمد في الأعلى", en) shouldBe
            "احمد في الاعلي"
        // آية: hamza-carrier alef AND final ta-marbuta both unify.
        DrsTextTools.apply(DrsTextTool.NORMALIZE_ARABIC, "مدرسة وشاطىء وآية وإسلام", en) shouldBe
            "مدرسه وشاطيء وايه واسلام"
    }

    test("normalize arabic leaves other characters untouched") {
        // Latin, digits, punctuation, diacritics and tatweel all pass
        // through unchanged — the tool unifies LETTERS only.
        DrsTextTools.apply(DrsTextTool.NORMALIZE_ARABIC, "Hello 123! مُحَمَّـد", en) shouldBe
            "Hello 123! مُحَمَّـد"
    }

    // -----------------------------------------------------------
    // CLEAN_MARKDOWN
    // -----------------------------------------------------------

    test("clean markdown strips headers, quotes, bullets and rules") {
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "# عنوان رئيس", en) shouldBe "عنوان رئيس"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "### عنوان ثالث", en) shouldBe "عنوان ثالث"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "> اقتباس", en) shouldBe "اقتباس"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, ">> اقتباس متداخل", en) shouldBe "اقتباس متداخل"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "- بند أول", en) shouldBe "بند أول"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "* بند ثانٍ", en) shouldBe "بند ثانٍ"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "+ بند ثالث", en) shouldBe "بند ثالث"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "• بند رابع", en) shouldBe "بند رابع"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "---\nسطر", en) shouldBe "سطر"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "***\nسطر", en) shouldBe "سطر"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "- - -\nسطر", en) shouldBe "سطر"
        // a marker without its required space is NOT the shape — refusal.
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "#عنوان بلا مسافة", en) shouldBe
            "#عنوان بلا مسافة"
    }

    test("clean markdown unwraps emphasis, strike and code") {
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "**غامق**", en) shouldBe "غامق"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "__غامق__", en) shouldBe "غامق"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "***غامق مائل***", en) shouldBe "غامق مائل"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "*مائل*", en) shouldBe "مائل"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "_latin words_", en) shouldBe "latin words"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "~~مشطوب~~", en) shouldBe "مشطوب"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "`كود`", en) shouldBe "كود"
        DrsTextTools.apply(
            DrsTextTool.CLEAN_MARKDOWN,
            "```kotlin\nval x = 1\n```\n",
            en,
        ) shouldBe "val x = 1\n"
        // Arabic-underscore content is indistinguishable from Arabic
        // snake-ish text — the honest refusal, never a guess.
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "_مائل_", en) shouldBe "_مائل_"
    }

    test("clean markdown snake_case and numbered lists survive") {
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "some_var_name stays", en) shouldBe
            "some_var_name stays"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "a__b__c", en) shouldBe "a__b__c"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "متغير_و_آخر", en) shouldBe "متغير_و_آخر"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "1. بند مرقم", en) shouldBe "1. بند مرقم"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, "10) بند مرقم", en) shouldBe "10) بند مرقم"
    }

    test("clean markdown resolves links, images and autolinks") {
        DrsTextTools.apply(
            DrsTextTool.CLEAN_MARKDOWN,
            "[النص](https://example.com/page)",
            en,
        ) shouldBe "النص"
        DrsTextTools.apply(
            DrsTextTool.CLEAN_MARKDOWN,
            "![وصف الصورة](https://example.com/a.png)",
            en,
        ) shouldBe "وصف الصورة"
        DrsTextTools.apply(
            DrsTextTool.CLEAN_MARKDOWN,
            "![](https://example.com/a.png)",
            en,
        ) shouldBe ""
        DrsTextTools.apply(
            DrsTextTool.CLEAN_MARKDOWN,
            "<https://example.com/docs>",
            en,
        ) shouldBe "https://example.com/docs"
    }

    test("clean markdown handles a real web paste end to end") {
        val pasted = "# الميزة الجديدة\n\n- **سرعة**: أسرع بـ50%\n- *استقرار*: صفر أعطال\n\n> جرّبها الآن\n\n[التفاصيل](https://example.com/x)\n\n---"
        val cleaned = DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, pasted, en)
        cleaned shouldBe
            "الميزة الجديدة\n\nسرعة: أسرع بـ50%\nاستقرار: صفر أعطال\n\nجرّبها الآن\n\nالتفاصيل\n\n"
    }

    test("clean markdown refuses plain text byte-identically and is idempotent") {
        val plain = "نص عربي صافٍ بلا أي تنسيق، ورقم 1234، وبعض الترقيم!"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, plain, en) shouldBe plain
        val pasted = "# عنوان\n\n- **نقطة** مهمة\n\n[رابط](https://x.y)"
        val once = DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, pasted, en)
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, once, en) shouldBe once
    }

    // -----------------------------------------------------------
    // HTML_TO_TEXT
    // -----------------------------------------------------------

    test("html to text strips tags and turns structure into newlines") {
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "<p>نص أول</p><p>نص ثانٍ</p>", en) shouldBe
            "نص أول\nنص ثانٍ\n"
        // <br> variants become real newlines.
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "سطر<br>ثانٍ<br/>ثالث", en) shouldBe
            "سطر\nثانٍ\nثالث"
        // list items keep their bullet; ul/ol wrappers close into
        // newlines too (every block closer is one newline — one rule).
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "<ul><li>أول</li><li>ثانٍ</li></ul>", en) shouldBe
            "- أول\n- ثانٍ\n\n"
        // headings close into newlines too.
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "<h2>عنوان</h2>متن", en) shouldBe
            "عنوان\nمتن"
    }

    test("html to text drops script and style blocks with their content") {
        DrsTextTools.apply(
            DrsTextTool.HTML_TO_TEXT,
            "<style>p{color:red}</style>مرحبا<script>alert(\"x\")</script> بالعالم",
            en,
        ) shouldBe "مرحبا بالعالم"
    }

    test("html to text strips comments and declarations") {
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "<!-- ملاحظة -->نص<!DOCTYPE html>", en) shouldBe
            "نص"
    }

    test("html to text decodes the closed entity whitelist in one pass") {
        DrsTextTools.apply(
            DrsTextTool.HTML_TO_TEXT,
            "&amp;&lt;&gt;&quot;&nbsp;&mdash;&#x41;&#160;&#128512;&copy;",
            en,
        ) shouldBe "&<>\" —A\u00A0\uD83D\uDE00©"
        // one pass, never a double decode: &amp;lt; lands as &lt;.
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "&amp;lt; &amp;amp;", en) shouldBe
            "&lt; &amp;"
        // the soft hyphen is invisible junk — it decodes to NOTHING.
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, "&mdash;&shy;&ndash;", en) shouldBe
            "—–"
    }

    test("html to text leaves unknown and invalid entities verbatim") {
        val invalid = "&unknown; &#DDDDDDDD; &#xZZ; &#0; &#xD800;"
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, invalid, en) shouldBe invalid
    }

    test("html to text never touches a lone angle bracket or prose") {
        val prose = "a < b و c > d و 5<6 و <>"
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, prose, en) shouldBe prose
    }

    test("html to text handles a real paste end to end and is idempotent") {
        val pasted =
            "<div class=\"msg\"><h2>عرض جديد</h2><p>خصم <b>30%</b> &amp; شحن مجاني<br>لكل الطلبات</p><script>track(\"view\")</script></div>"
        val once = DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, pasted, en)
        once shouldBe "عرض جديد\nخصم 30% & شحن مجاني\nلكل الطلبات\n\n"
        DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, once, en) shouldBe once
    }

    test("html to text composes with clean markdown on the bullets it writes") {
        val html = "<li>بند</li>"
        val htmlCleaned = DrsTextTools.apply(DrsTextTool.HTML_TO_TEXT, html, en)
        htmlCleaned shouldBe "- بند\n"
        DrsTextTools.apply(DrsTextTool.CLEAN_MARKDOWN, htmlCleaned, en) shouldBe "بند\n"
    }

    // -----------------------------------------------------------
    // TEXT_INSPECT (the census)
    // -----------------------------------------------------------

    test("text census pins an exact mixed-script profile") {
        val census = DrsTextTools.inspectText("مرحبا World 12!\nسطر ثانٍ\u200F")
        census.chars shouldBe 25
        census.arabicLetters shouldBe 11
        census.latinLetters shouldBe 5
        census.otherLetters shouldBe 0
        census.digits shouldBe 2
        census.words shouldBe 5
        census.sentences shouldBe 2
        census.lines shouldBe 2
        census.spaces shouldBe 4
        census.punctuation shouldBe 1
        census.invisible shouldBe 1
        census.emoji shouldBe 0
    }

    test("text census counts emoji and reads an empty text as zeros") {
        val census = DrsTextTools.inspectText("أهلاً \uD83D\uDE00")
        census.chars shouldBe 8
        census.arabicLetters shouldBe 4
        census.emoji shouldBe 1
        census.words shouldBe 2
        census.sentences shouldBe 1
        val empty = DrsTextTools.inspectText("")
        empty shouldBe DrsTextTools.TextCensus(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    test("text census sentences count the closed enders only") {
        // no enders → one fragment; consecutive enders collapse.
        DrsTextTools.inspectText("سطر بلا نهاية").sentences shouldBe 1
        DrsTextTools.inspectText("أول. ثانٍ! ثالث؟").sentences shouldBe 3
        DrsTextTools.inspectText("...؟!").sentences shouldBe 0
    }
})
