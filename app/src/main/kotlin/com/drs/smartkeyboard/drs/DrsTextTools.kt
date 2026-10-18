/*
 * Copyright (C) 2021-2025 The DRS Smart Keyboard Project
 * Copyright (C) 2025 DRS Smart Keyboard contributors
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsArabicLetters
import com.drs.smartkeyboard.drs.ai.DrsWritingAssistant
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.Collator
import java.util.Locale

/**
 * pure text transformation engine for the technical user system.
 *
 * Every tool operates on a plain [String] and returns a new [String] - no
 * input-connection or UI dependency here, so all transformations are fully
 * unit-testable. The editor glue lives in
 * [com.drs.smartkeyboard.ime.editor.EditorInstance.performTextTool].
 *
 * Design rules:
 *  - Never throws: callers pass arbitrary user text; every function is
 *    written defensively and returns the input unchanged when it cannot be
 *    transformed.
 *  - Never reorders or drops content except where the tool's purpose IS
 *    removal (empty lines, duplicate lines, diacritics, extra spaces).
 *  - Locale-aware casing through [Locale] so Arabic/Turkish/etc. behave
 *    correctly on the user's device.
 */

/** Horizontal whitespace: spaces, tabs and NBSP (never newlines). */
private val HORIZONTAL_SPACES = Regex("[ \\t\\u00A0\\u2007\\u202F]+")

/** Arabic diacritics (harakat), small quranic marks and superscript alef. */
private val ARABIC_DIACRITICS = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

/** Arabic tatweel (kashida) character — pure elongation, carries no meaning. */
private const val TATWEEL = '\u0640'

/** First Arabic-Indic digit (٠). Digits 0-9 map to U+0660-U+0669. */
private const val ARABIC_INDIC_ZERO = '\u0660'

/** First EXTENDED Arabic-Indic digit (۰, Persian/Urdu). U+06F0-U+06F9. */
private const val EXTENDED_ARABIC_INDIC_ZERO = '\u06F0'

/** Punctuation that must never keep a space BEFORE it (Latin + Arabic). */
private val SPACE_BEFORE_PUNCT = Regex("[ \\t\\u00A0]+([,;:.!?،؛…؟)\\]\\}»])")

/**
 * Punctuation that should be followed by one space when the next character is
 * a letter. '.' is intentionally excluded for Latin text (domains, files);
 * it is handled separately for Arabic letters only.
 */
private val SPACE_AFTER_PUNCT = Regex("([,;:!?،؛…])(?=[\\p{IsAlphabetic}])")

/** Arabic letter following a '.' (sentence dot in Arabic text). */
private val DOT_BEFORE_ARABIC = Regex("\\.(?=[\\u0600-\\u06FF])")

/** zero-width / invisible formatting characters that carry no
 *  visible content and break search and copy. U+200C (ZWNJ) is deliberately
 *  NOT here: نصف المسافة is a real Arabic keyboard character with meaning. */
// internal — the tiles' context advisor (DrsTileSmart) shares
// the SAME closed mark class instead of re-spelling it (مصدر حقيقة واحد).
internal val ZERO_WIDTH_CHARS = Regex("[\\u200B\\u200D\\u200E\\u200F\\uFEFF\\u2066-\\u2069]")

/** sentence ender followed by horizontal whitespace — the
 *  boundary where SENTENCE_PER_LINE inserts the line break. The lookahead
 *  guarantees the ender is between sentences (not at the text's end), and
 *  existing newlines are left untouched so the tool is idempotent. */
private val SENTENCE_BOUNDARY = Regex("([.!؟?…])[ \\t\\u00A0]+(?=[^\\s])")

/** a run of one or more whitespace characters — the word
 *  separator REVERSE_WORDS splits each line on. */
private val WHITESPACE_RUN = Regex("\\s+")

/** emoji and pictograph blocks plus their joiners and
 *  selectors (VS16, skin-tone modifiers, ZWJ). Letters, digits,
 *  punctuation, whitespace and every script pass through untouched.
 *  NOTE: the supplementary-plane bounds use \x{...} — Java/Kotlin \uXXXX
 *  is exactly four digits, so \u1F000 would silently parse as U+1F00+"0"
 *  and corrupt the whole class (this really broke the first draft). */
// internal — shared with the tiles' context advisor
// (DrsTileSmart) so the emoji signal has one closed definition.
internal val EMOJI_CHARS = Regex(
    "[\\u2600-\\u27BF\\uFE0F\\u200D\\uD83C-\\uD83E\\uDC00-\\uDFFF\\x{1F000}-\\x{1FAFF}]",
)

/** list separators recognized by SPLIT_TO_LINES — both the
 *  Latin comma/semicolon and their Arabic counterparts (، ؛). */
private val LIST_SEPARATORS = Regex("[,،;؛]")

/** the boundary between a digit and a letter (either
 *  direction). \p{N} covers 0-9 and ٠-۹ alike, \p{L} covers Arabic and
 *  Latin letters; lookaround means insertion never consumes characters. */
private val DIGIT_THEN_LETTER = Regex("(?<=\\p{N})(?=\\p{L})")
private val LETTER_THEN_DIGIT = Regex("(?<=\\p{L})(?=\\p{N})")

/** every Unicode punctuation mark (category P) — the Latin
 *  set plus the Arabic ، ؛ ؟ and quotes, brackets, dashes and more. */
private val PUNCTUATION_CHARS = Regex("\\p{IsPunctuation}")

enum class DrsTextTool(
    val code: Int,
    /** Info-only tools show a result message instead of modifying text. */
    val isInfoOnly: Boolean = false,
    /** Editor-level tools that need the cursor position, not a transform. */
    val isEditorOp: Boolean = false,
    /** tools that commit an invisible mark at the cursor. */
    val isInsertMark: Boolean = false,
) {
    // ---- Case transformation (applies to selection, else whole field) ----
    UPPERCASE(-601),
    LOWERCASE(-602),
    TITLE_CASE(-603),
    SENTENCE_CASE(-604),

    // ---- Spacing and lines ----
    TRIM_SPACES(-611),
    TRIM_LINE_EDGES(-612),
    REMOVE_EMPTY_LINES(-613),
    REMOVE_LINE_BREAKS(-614),
    SORT_LINES(-615),
    REMOVE_DUPLICATE_LINES(-616),

    // line-ordering additions (same pure-transform family).
    NUMBER_LINES(-617),
    REVERSE_LINES(-618),

    // locale-aware quote wrapping (same pure-transform family).
    WRAP_QUOTES(-619),

    // collapse runs of blank lines into one (same family).
    COLLAPSE_EMPTY_LINES(-620),

    // descending line sort (the counterpart of SORT_LINES).
    SORT_LINES_DESC(-624),

    // ---- Arabic-specific ----
    REMOVE_DIACRITICS(-621),

    // Arabic digit conversions + tatweel removal.
    REMOVE_TATWEEL(-622),
    TO_ARABIC_DIGITS(-605),
    TO_WESTERN_DIGITS(-606),

    // unifies Arabic letter variants (hamza-carriers,
    // ta-marbuta, alef maqsura) — same letter-normalization family.
    NORMALIZE_ARABIC(-623),

    // case inversion, Arabic punctuation mapping, zero-width
    // cleanup and tab→space conversion (same pure-transform families).
    TOGGLE_CASE(-607),
    TO_ARABIC_PUNCTUATION(-608),
    REMOVE_ZERO_WIDTH(-609),
    TABS_TO_SPACES(-610),

    // length sort, word-level dedup, paren wrapping and one
    // sentence per line (same line/quick families).
    SORT_LINES_BY_LENGTH(-625),
    REMOVE_DUPLICATE_WORDS(-626),
    WRAP_PARENS(-627),
    SENTENCE_PER_LINE(-628),

    // the inverse tab↔space and punctuation mappings, the
    // edge-only blank trim and word-order reversal (same families).
    SPACES_TO_TABS(-629),
    TO_WESTERN_PUNCTUATION(-630),
    TRIM_BLANK_EDGES(-633),
    REVERSE_WORDS(-634),

    // PDF/web paste repair (presentation forms), list↔lines
    // splitting (Arabic comma aware), hashtag-style space removal, emoji
    // stripping and the URL encode/decode pair (same pure families).
    NORMALIZE_ARABIC_FORMS(-635),
    SPLIT_TO_LINES(-636),
    JOIN_LINES(-637),
    REMOVE_ALL_SPACES(-638),
    STRIP_EMOJI(-639),
    URL_ENCODE(-642),
    URL_DECODE(-643),

    // mixed-script cleanup — a single space between digits and
    // letters in both directions, and full Unicode punctuation removal
    // (same pure families).
    SEPARATE_DIGIT_LETTERS(-644),
    REMOVE_PUNCTUATION(-645),

    // ---- Punctuation and full clean-up ----
    NORMALIZE_PUNCTUATION(-631),
    CLEAN_TEXT(-632),

    // ---- Info (never modifies text) ----
    COUNT(-641, isInfoOnly = true),

    // ---- Editor-level line operations (cursor-relative) ----
    DELETE_LINE(-651, isEditorOp = true),
    DELETE_TO_LINE_START(-652, isEditorOp = true),
    DELETE_TO_LINE_END(-653, isEditorOp = true),

    // invisible directional/joining marks committed at the
    // cursor — the tools RTL authors keep reaching for in every other
    // editor («إدراج RLM/LRM/ZWJ/ZWNJ»). Insertion needs no text, works
    // on an empty field, and never replaces anything.
    INSERT_RLM(-654, isEditorOp = true, isInsertMark = true),
    INSERT_LRM(-655, isEditorOp = true, isInsertMark = true),
    INSERT_ZWJ(-656, isEditorOp = true, isInsertMark = true),
    INSERT_ZWNJ(-657, isEditorOp = true, isInsertMark = true),

    // (public): the deterministic sentence vocalization — the
    // exact counterpart of REMOVE_DIACRITICS. Every Arabic word run is
    // looked up in the local lexicons (the reviewed seed first, then the
    // 3000-word asset); a known word takes its canonical vocalized form,
    // an unknown word stays byte-identical — never an invented mark.
    // Pure, offline, on-device: the doctrine holds.
    TASHKEEL_TEXT(-658),

    // the deterministic number-to-Arabic-words rendering —
    // 1234 → «ألف ومائتان وأربعة وثلاثون». The WHOLE field must be a
    // clean integer (Western, Arabic-Indic or Extended digits, optional
    // leading minus); anything else returns byte-identical — the tool
    // never rewrites prose it does not understand. See [DrsNumberWords].
    NUMBER_WORDS(-659),

    // the deterministic financial tafqit — a clean monetary
    // amount becomes its formal check-writing words «فقط … لا غير»
    // (see [DrsTafqit]): closed five-currency catalog, documented
    // counted-noun agreement, feminine subunit counting; anything that
    // is not a clean amount returns byte-identical, never a guess.
    TAFQIT(-660),

    // the deterministic date-in-words rendering — the trilogy
    // closes ( numbers, amounts, dates): a clean
    // three-component date over one consistent separator becomes its
    // formal documentary phrase «الثالث من أكتوبر عام ألفين وستة وعشرين»
    // (see [DrsDateWords]): closed 31-day ordinal table, closed 12-month
    // catalog, the year through [DrsNumberWords] in the genitive; any
    // ambiguous or invalid shape returns byte-identical, never a guess.
    DATE_WORDS(-661),

    // the deterministic clock-time-in-words rendering — the
    // quartet closes ( numbers, amounts, dates,
    // times): a clean two-component clock time over the single
    // colon separator becomes its formal spoken phrase «الثالثة إلا
    // الربع» (see [DrsTimeWords]): closed 12-entry feminine hour table,
    // the documented quarter/half/إلا branches and feminine counted-noun
    // minutes; any ambiguous or invalid shape returns byte-identical,
    // never a guess.
    TIME_WORDS(-662),

    // the deterministic fraction-in-words rendering — the
    // first expansion of the fifth round ( fractions, ordinals,
    // weekdays): a clean two-component fraction over one fraction slash
    // becomes its formal written words «ثلاثة أرباع» (see
    // [DrsFractionWords]): the closed Arabic fraction-word zone
    // (denominator 2..10, numerator 1..10), the documented counted-noun
    // agreement (النصف، ثلثان، ثلاثة أثلاث) and no tanwin — house
    // style; any ambiguous or invalid shape returns byte-identical,
    // never a guess.
    FRACTION_WORDS(-663),

    // the deterministic weekday resolution — a clean date
    // resolves to its weekday name «السبت» (see [DrsDateWords]): the
    // same closed date parser as DATE_WORDS plus the documented
    // Sakamoto congruence (exhaustively verified against the reference
    // calendar for every accepted date 1..9999); any ambiguous or
    // invalid shape returns byte-identical, never a guess.
    WEEKDAY(-664),

    // the deterministic ordinal-in-words rendering — a clean
    // integer speaks its ordinal «الحادي والعشرون» (see
    // [DrsOrdinalWords]): the closed zone 1..9999, the documented
    // masculine-nominative-with-article convention (the citation form),
    // the الأول/الحادي written distinction and one source of truth for
    // the cardinal groups via [DrsNumberWords]; anything that is not a
    // clean number returns byte-identical, never a guess.
    ORDINAL_WORDS(-665),

    // the deterministic decimal-in-words rendering — the
    // sixth round opens with the decimal tail speaking: a clean
    // two-component decimal over one separator from the closed pair
    // { . ، ٫ } becomes its formal digit-by-digit words «واحد فاصلة
    // خمسة صفر» (see [DrsDecimalWords]): the integer part through
    // [DrsNumberWords] (value not position, the minus, no negative
    // zero), the tail digit-by-digit through the shared DIGIT_WORDS
    // alphabet with no simplification, and a fifteen-digit ceiling;
    // any ambiguous or invalid shape returns byte-identical, never a
    // guess.
    DECIMAL_WORDS(-666),

    // the deterministic words-to-number mirror — the sixth
    // round closes with the words speaking back: a clean release-lexicon
    // composition returns its number «ثلاثة وعشرون» → 23 (see
    // [DrsWordsToNumber]): the closed lexicon of exactly the words
    // [DrsNumberWords] emits, in both written cases never mixed (the
    // case locks on the first inflected word), the attached waw REQUIRED
    // at every join, segments strictly descending and every word
    // consumed; any shape the grammar does not name returns
    // byte-identical, never a guess.
    WORDS_TO_NUMBER(-667),

    // the deterministic Gregorian→Hijri conversion — the
    // seventh round opens with the calendar speaking: a clean date
    // becomes its Hijri documentary phrase «2026-02-18» → «الأول من
    // رمضان عام ألف وأربعمائة وسبعة وأربعين هجري» (see
    // [DrsHijriWords]): the same closed date parser as DATE_WORDS, the
    // platform's embedded Umm al-Qura table as the named convention
    // (the SAME source the number panel already speaks), the closed
    // zone 1300..1600 AH, the shared day ordinals and number words;
    // anything out of zone or out of grammar returns byte-identical,
    // never a guess.
    GREGORIAN_TO_HIJRI(-668),

    // the deterministic Hijri→Gregorian conversion — the
    // seventh round closes with the calendar speaking both ways: a
    // clean Hijri date in digits becomes its Gregorian documentary
    // phrase through the SAME formatter as DATE_WORDS — «1447/9/1» →
    // «الثامن عشر من فبراير عام ألفين وستة وعشرين» (see
    // [DrsHijriWords]): a closed Hijri parser mirroring the Gregorian
    // shape rules, the year pinned to the platform's closed zone
    // 1300..1600 AH, month lengths the platform's own, and one
    // phrase shape for one calendar; anything out of zone or out of
    // grammar returns byte-identical, never a guess.
    HIJRI_TO_GREGORIAN(-669),

    // the natural line sort — the eighth round opens with
    // the tiles speaking: the line sort that reads NUMBERS the way a
    // human does (file2 before file10, قائمة 2 before قائمة 10),
    // digit chunks compared numerically at arbitrary precision (no
    // Long overflow — length then lexicographic on the trimmed form),
    // letter chunks through the same locale Collator as SORT_LINES,
    // and digits-before-letters when a chunk pair mixes. Stable,
    // deterministic, no locale surprises beyond the collator.
    SORT_LINES_NATURAL(-670),

    // the deterministic URL slug — a text becomes its
    // link-safe path: diacritics, tatweel and invisible marks stripped,
    // hamza carriers folded (أإآٱ→ا، ة→ه، ى→ي)، spaces and underscores
    // to one dash, everything outside letters/digits/dashes removed,
    // dashes collapsed and trimmed. Arabic keeps its folded letters
    // (never transliterated — never invented); a text of nothing but
    // punctuation returns byte-identical, never an empty slug.
    SLUGIFY(-671),

    // the deterministic snake_case — words separated by
    // spaces, underscores or dashes (plus lower→upper camel boundaries
    // inside each word) join with one underscore, lowercased; Arabic
    // words join the same way with no case invented. Anything that
    // would not change returns byte-identical, never a rewrite.
    TO_SNAKE_CASE(-672),

    // the deterministic camelCase — the honest sibling:
    // tokens join with NO separator, the first lowercased and the rest
    // capitalized — and any token carrying a non-ASCII letter REFUSES
    // the whole transform byte-identically (camelCase is a Latin code
    // convention; inventing letter shapes for Arabic is a guess). The
    // same closed honesty: no change returns byte-identical.
    TO_CAMEL_CASE(-673),

    // the writing assistant speaks — the orphaned M1.5
    // engine (DrsWritingAssistant) joins the tool surface through its
    // OWN firing contract, verbatim: sixteen deterministic Arabic
    // phrase rewrites that fire ONLY when the FULL normalized TAIL of
    // the text equals the rule's normalized trigger — never a
    // containment hit (the M1.5 corruption case: «انشاء اللهية» stays
    // byte-identical). No firing returns the text unchanged — never a
    // partial rewrite, never a guess. See [DrsWritingAssistant.improveTail].
    IMPROVE_PHRASES(-674),

    // the spacing normalizer — the assistant's second
    // public function, wired verbatim: whitespace runs collapse to one
    // space, edges trim, spaces BEFORE closing punctuation (؟ ! ، ؛ . :)
    // and after opening ones vanish, punctuation runs collapse to a
    // single mark — all to a bounded FIXED POINT, so the tool is
    // idempotent by construction: applying it twice equals applying it
    // once, and a redisplayed result can never grow extra edits. See
    // [DrsWritingAssistant.normalizeSpacing].
    NORMALIZE_SPACING(-675),

    // the web-paste cleaner — copy from the web arrives wearing
    // markdown; the tool keeps the MEANING and drops the costume:
    // leading # headers, > blockquotes, - * + • bullets and --- rules
    // vanish, ***bold***/__bold__/*italic*/~~strike~~/`code`/``` fences
    // unwrap to their bare text, [text](url) speaks its text,
    // ![alt](url) its alt and <url> its address. Numbered lists are
    // never touched, and underscores are unwrapped ONLY inside Latin
    // content — Arabic snake-ish underscores (متغير_و_آخر) are
    // indistinguishable from them, so they are refused byte-identically,
    // never a guess. A text carrying none of the shapes returns
    // byte-identical. See [cleanWebPaste].
    CLEAN_MARKDOWN(-646),

    // the HTML-paste cleaner — the raw-markup sibling of
    // CLEAN_MARKDOWN: email/WhatsApp-web/CMS copies arrive as literal
    // HTML, and the tool keeps the PROSE and drops the markup:
    // <!-- comments --> and <!declarations> vanish, <script>/<style>
    // blocks vanish WITH their content (code is not prose), <br> becomes
    // a real newline, block-level closers (</p> </div> </li> </h1..6>…)
    // become newlines and <li> keeps its «- » bullet — while every other
    // WELL-FORMED tag (<tag …> or </tag>) strips to nothing. A lone '<'
    // that opens no tag (a < b) is NOT the shape and survives
    // byte-identically, and the CLOSED entity whitelist decodes in ONE
    // pass (&amp;lt; lands as &lt; — never a double decode); numeric
    // &#N; / &#xN; outside the valid code-point zone stay verbatim,
    // never invented. A text carrying none of the shapes returns
    // byte-identical. See [htmlToText].
    HTML_TO_TEXT(-647),

    // the text census — the info counterpart of COUNT: one
    // deterministic read-only profile of the whole field (letters by
    // script, digits, words, sentences, lines, punctuation/symbols,
    // invisible marks and emoji) surfaced as a toast, never a transform.
    // The same read-only discipline as COUNT: apply() returns the text
    // byte-identical; callers use [inspectText].
    TEXT_INSPECT(-648, isInfoOnly = true),

    // the link harvester — a message or a page full of scattered
    // URLs and emails speaks them plainly: every http/https/ftp
    // URL, every www. address that is not buried inside a larger
    // word or domain, and every strict ASCII email lands one per
    // line, in order of appearance, every one a verbatim substring
    // (only trailing sentence punctuation and an UNbalanced closing
    // bracket leave; the balanced wiki-style (…) belongs to the
    // link). No link, no list — byte-identical, never an invented
    // row. See [extractLinks].
    EXTRACT_LINKS(-649),

    // the Arabic stretch cleaner — «هلااااا» speaks like «هلا»:
    // a run of THREE or more identical Arabic letters collapses to
    // one letter (three in a row never occurs in standard
    // orthography), while legitimate doubles stay (الله، عدّ) and
    // Latin runs never touch (www survives — the tool's zone is
    // the Arabic letter block only). A diacritic after a stretched
    // run follows the collapsed letter, and nothing stretched
    // returns byte-identical — never a rewrite of honest text.
    // See [unstretchArabicLetters].
    REMOVE_LETTER_STRETCH(-650),

    // the Base64 encoder — the URL pair's payload sibling: text
    // speaks the machine's alphabet, UTF-8 bytes over the standard
    // RFC 4648 alphabet with its padding, ONE line (wrapping belongs
    // to the channel, not the value). Total and deterministic: every
    // non-empty text encodes, and the BASE64_DECODE mirror restores
    // the exact bytes.
    BASE64_ENCODE(-640),

    // the Base64 decoder — the honest mirror of [BASE64_ENCODE]:
    // wrapped tokens rejoin (spaces/tabs/newlines/the NBSP family
    // strip first), the URL-safe alphabet (- _) maps onto the
    // standard one, and the STRICT padded shape is required
    // (4-char groups, then 0–2 trailing pads, nothing else).
    // Decoded bytes must themselves decode as valid UTF-8 — a token
    // of random binary, an honest Latin word of the right length, a
    // malformed length or padding all return the input
    // byte-identically. Decoding must never destroy typing —
    // never a guess, never a replacement char. See [base64Decode].
    BASE64_DECODE(-676),

    // the Morse encoder — the telegraph sibling of the URL and
    // Base64 pairs, and the third machine alphabet: text speaks the
    // OFFICIAL ITU-R M.1677-1 table — every Arabic letter its historic
    // sign (29 signs over the 28 letters + the standalone hamza,
    // injective — the ITU Arabic zone carries NO collision), Latin
    // letters, digits and the closed punctuation set their
    // international signs. Letters separate with one space, words
    // with « / » (the channel's word mark — so a '/' in the input is
    // refused outright, never ambiguously encoded). Diacritics,
    // tatweel and zero-width marks strip first; hamza carriers fold
    // (أإآٱ→ا، ة→ه، ى→ي، ئ→ي، ؤ→و) and Arabic punctuation maps onto
    // its Latin homograph (؟،،؛ → ? , ;) — the same folding doctrine
    // as SLUGIFY and NORMALIZE_ARABIC, documented, never a guess.
    // Anything without a known sign (emoji, other scripts, presentation
    // forms) refuses the WHOLE text byte-identically — never an
    // invented sign. See [morseEncode].
    MORSE_ENCODE(-677),

    // the Morse decoder — the honest mirror of [MORSE_ENCODE]:
    // accepted glyphs normalize onto the canonical pair (dots {., ·},
    // dashes {- − ─ — –}) FIRST, then every sign speaks the ARABIC
    // letter of the decode map — the channel is scriptless and the
    // keyboard speaks Arabic, so every Latin homograph (SOS → سخس)
    // returns as its Arabic winner; digits and punctuation return
    // as-is. The reverse map is injective (56 entries, simulated and
    // proved) — with one DOCUMENTED ITU quirk: the international slash
    // sign (−··−) IS the Arabic sad, so it decodes ص and the encoder
    // refuses '/' outright. The strict shape: letters separated by
    // single spaces, words by « / » — an empty word (leading, trailing
    // or doubled slash), a sign outside the closed map, or any foreign
    // glyph returns the input byte-identically. Decoding must never
    // destroy typing — never a guess. See [morseDecode].
    MORSE_DECODE(-678),

    // the arithmetic evaluator — the field IS the expression:
    // «1250×12» answers 15000, «(50+25)÷3» answers 25, «٥٠ × ٤»
    // answers 200. A hand-written recursive-descent parser (never an
    // eval): + − × ÷ ^ over the standard precedence, right-associative
    // powers, unary minus, parentheses and the percent postfix binding
    // to its own number (pure math — 15% == 0.15, documented, no
    // calculator conventions). Eastern digits, the Arabic
    // decimal/thousands separators and the operator homographs
    // (×÷−،٪، fullwidth) fold onto the canonical grammar FIRST; a
    // Latin comma BETWEEN digits is a thousands separator. ANY foreign
    // character, an unbalanced parenthesis or a dangling operator
    // refuses the WHOLE text byte-identically — and a division by
    // zero (Infinity) refuses too. The result prints as an exact
    // integer when it is one (within 1e-9, inside the 9e15 safe
    // zone), else 12 SIGNIFICANT digits — never scientific notation,
    // never a float tail like 0.30000000000000004. A field with no
    // operator at all is not an attempt: bare numbers stay bare. See
    // [calculate].
    CALCULATE(-679),

    // the quantity converter — the field IS the quantity:
    // «5 كم» speaks its conversions ladder, «38 C» speaks °ف and ك,
    // and «5 كم إلى م» answers the ONE target — 5000 م. The closed
    // catalog covers SEVEN categories (length, mass, volume, area,
    // speed, data, time) over Arabic and English unit names, with the
    // temperature trio speaking REAL formulas (not factors). The
    // ladder skips the input unit's own line, and the target answers
    // its canonical Arabic display token no matter how it was typed
    // (the morse doctrine: the keyboard speaks Arabic — «3 kg to g»
    // answers 3000 جم). Anything that is not one whole quantity —
    // prose, a bare number, an unknown unit, a cross-category target —
    // returns byte-identical, never an invented conversion. See
    // [convertUnits].
    CONVERT_UNITS(-680),

    // the hex encoder — the fourth machine alphabet (after the URL
    // pair, Base64 and Morse): the text's UTF-8 bytes speak the
    // classic byte dump — lowercase two-digit pairs joined with one
    // space, one line. Total and deterministic: every non-empty text
    // encodes, and the HEX_DECODE mirror restores the exact bytes
    // (see [hexEncode]).
    HEX_ENCODE(-681),

    // the hex decoder — the honest mirror of [HEX_ENCODE]:
    // whitespace (the NBSP family included) folds away, then exactly
    // one of TWO closed shapes decodes: the SOLID dump (one token,
    // optional 0x/0X prefix, an even-length run of hex digits) or the
    // TOKEN list (every token EXACTLY two hex digits, each optionally
    // 0x-prefixed), in any letter case. The decoded bytes must
    // themselves decode as valid UTF-8 (REPORT, never a replacement
    // char) — an odd length, a non-hex character, mixed token lengths,
    // a colon/percent separator, an eastern-digit dump or binary bytes
    // all return the input byte-identically. A bare digit run is a
    // NUMBER first and a hex dump second — the advisor stays silent on
    // it (the one closed ambiguity class), while the tool itself
    // remains total over its shape. Decoding must never destroy
    // typing — never a guess. See [hexDecode].
    HEX_DECODE(-682);

    companion object {
        /** Inclusive range covering every tool code, for fast dispatch. */
        val CODE_RANGE = -682..-601

        fun fromCode(code: Int): DrsTextTool? = entries.firstOrNull { it.code == code }
    }
}

object DrsTextTools {

    /**
 * the invisible mark each insertion tool commits at the
     * cursor (pure, JVM-testable): RLM (U+200F) and LRM (U+200E) steer the
     * bidi resolution of neutral characters, ZWJ (U+200D) joins letters or
     * emoji into ligatures/sequences, ZWNJ (U+200C) — نصف المسافة — breaks
     * the join. Null for every non-insertion tool.
     */
    fun insertionMarkFor(tool: DrsTextTool): Char? = when (tool) {
        DrsTextTool.INSERT_RLM -> '\u200F'
        DrsTextTool.INSERT_LRM -> '\u200E'
        DrsTextTool.INSERT_ZWJ -> '\u200D'
        DrsTextTool.INSERT_ZWNJ -> '\u200C'
        else -> null
    }

    /**
     * Applies [tool] to [text]. Pure function; never throws.
     */
    fun apply(tool: DrsTextTool, text: String, locale: Locale = Locale.getDefault()): String {
        if (text.isEmpty() && tool != DrsTextTool.COUNT) return text
        return try {
            when (tool) {
                DrsTextTool.UPPERCASE -> text.uppercase(locale)
                DrsTextTool.LOWERCASE -> text.lowercase(locale)
                DrsTextTool.TITLE_CASE -> titleCase(text, locale)
                DrsTextTool.SENTENCE_CASE -> sentenceCase(text, locale)
                DrsTextTool.TRIM_SPACES -> collapseHorizontalSpaces(text).trim(' ', '\t', '\u00A0')
                DrsTextTool.TRIM_LINE_EDGES -> text.lines().joinToString("\n") { line ->
                    line.trim(' ', '\t', '\u00A0')
                }
                DrsTextTool.REMOVE_EMPTY_LINES -> text.lines()
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                DrsTextTool.REMOVE_LINE_BREAKS -> collapseHorizontalSpaces(
                    text.replace(Regex("\\s*\\n+\\s*"), " "),
                )
                DrsTextTool.SORT_LINES -> {
                    val trailingNewline = text.endsWith("\n")
                    val collator = Collator.getInstance(locale)
                    val sorted = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .sortedWith(compareBy(collator) { it })
                        .joinToString("\n")
                    if (trailingNewline) "$sorted\n" else sorted
                }
                DrsTextTool.REMOVE_DUPLICATE_LINES -> {
                    val seen = HashSet<String>()
                    text.lines()
                        .filter { line ->
                            val key = line.trim(' ', '\t', '\u00A0')
                            seen.add(key)
                        }
                        .joinToString("\n")
                }
                // prefix every line with its 1-based order, and
                // flip the line order. Both preserve a trailing newline and
                // keep blank lines exactly where they are.
                DrsTextTool.NUMBER_LINES -> {
                    val trailingNewline = text.endsWith("\n")
                    val numbered = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .mapIndexed { index, line -> "${index + 1}. $line" }
                        .joinToString("\n")
                    if (trailingNewline) "$numbered\n" else numbered
                }
                DrsTextTool.REVERSE_LINES -> {
                    val trailingNewline = text.endsWith("\n")
                    val reversed = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .asReversed()
                        .joinToString("\n")
                    if (trailingNewline) "$reversed\n" else reversed
                }
                // surround the text with quotation marks that
                // match the active locale — Arabic guillemets «» for Arabic,
                // straight quotes for everything else.
                DrsTextTool.WRAP_QUOTES -> {
                    val (openQuote, closeQuote) =
                        if (locale.language == "ar") "«" to "»" else "\"" to "\""
                    openQuote + text + closeQuote
                }
                // turn every run of consecutive blank lines
                // into exactly one blank line. Leading and trailing runs
                // are collapsed to one blank line as well (this tool's
                // purpose IS removal — of the extra blanks only; single
                // blank separators and the trailing newline are kept).
                DrsTextTool.COLLAPSE_EMPTY_LINES -> collapseEmptyLines(text)
                DrsTextTool.REMOVE_DIACRITICS -> text.replace(ARABIC_DIACRITICS, "")
                // (public): the sentence vocalization — the
                // lexicon-based counterpart of REMOVE_DIACRITICS (see
                // [tashkeelText]): known words vocalized, unknown words
                // byte-identical, everything else verbatim.
                DrsTextTool.TASHKEEL_TEXT -> tashkeelText(text)
                // the number-to-words rendering — a clean
                // integer field becomes its Arabic words, anything else
                // stays byte-identical (see [DrsNumberWords]).
                DrsTextTool.NUMBER_WORDS -> DrsNumberWords.arabicWordsOrNull(text) ?: text
                // the financial tafqit — a clean amount speaks
                // its formal check words, anything else stays byte-identical
                // (see [DrsTafqit]).
                DrsTextTool.TAFQIT -> DrsTafqit.tafqitOrNull(text) ?: text
                // the date-in-words rendering — a clean date
                // speaks its formal documentary phrase, anything else
                // stays byte-identical (see [DrsDateWords]).
                DrsTextTool.DATE_WORDS -> DrsDateWords.dateWordsOrNull(text) ?: text
                // the clock-time-in-words rendering — a clean
                // clock time speaks its formal spoken phrase, anything
                // else stays byte-identical (see [DrsTimeWords]).
                DrsTextTool.TIME_WORDS -> DrsTimeWords.timeWordsOrNull(text) ?: text
                // the fraction-in-words rendering — a clean
                // fraction field speaks its Arabic words, prose stays
                // byte-identical (see [DrsFractionWords]).
                DrsTextTool.FRACTION_WORDS -> DrsFractionWords.fractionWordsOrNull(text) ?: text
                // the weekday resolution — a clean date
                // resolves to its weekday name, prose stays byte-identical
                // (see [DrsDateWords.weekdayOrNull]).
                DrsTextTool.WEEKDAY -> DrsDateWords.weekdayOrNull(text) ?: text
                // the ordinal-in-words rendering — a clean
                // integer speaks its ordinal, prose stays byte-identical
                // (see [DrsOrdinalWords]).
                DrsTextTool.ORDINAL_WORDS -> DrsOrdinalWords.ordinalWordsOrNull(text) ?: text
                // the decimal-in-words rendering — a clean
                // decimal field speaks its digit-by-digit words, prose
                // stays byte-identical (see [DrsDecimalWords]).
                DrsTextTool.DECIMAL_WORDS -> DrsDecimalWords.decimalWordsOrNull(text) ?: text
                // the words-to-number mirror — a clean
                // lexical composition returns its number, prose stays
                // byte-identical (see [DrsWordsToNumber]).
                DrsTextTool.WORDS_TO_NUMBER -> DrsWordsToNumber.wordsToNumberOrNull(text)?.toString() ?: text
                // the Gregorian→Hijri conversion — a clean
                // date speaks its Hijri phrase, prose and out-of-zone
                // dates stay byte-identical (see [DrsHijriWords]).
                DrsTextTool.GREGORIAN_TO_HIJRI ->
                    DrsHijriWords.gregorianToHijriWordsOrNull(text) ?: text
                // the Hijri→Gregorian conversion — a clean
                // Hijri date speaks its Gregorian phrase through the
                // DATE_WORDS formatter, prose and out-of-zone dates
                // stay byte-identical (see [DrsHijriWords]).
                DrsTextTool.HIJRI_TO_GREGORIAN ->
                    DrsHijriWords.hijriToGregorianWordsOrNull(text) ?: text
                // the natural line sort — numbers the way a
                // human reads them, letter chunks through the same locale
                // collator as SORT_LINES (see [compareNaturalLines]).
                DrsTextTool.SORT_LINES_NATURAL -> {
                    val trailingNewline = text.endsWith("\n")
                    val collator = Collator.getInstance(locale)
                    val sorted = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .sortedWith { a, b -> compareNaturalLines(a, b, collator) }
                        .joinToString("\n")
                    if (trailingNewline) "$sorted\n" else sorted
                }
                // the link-safe slug (see [slugify]).
                DrsTextTool.SLUGIFY -> slugify(text)
                // the snake_case / camelCase pair (see the
                // two helpers — the camel one refuses non-ASCII tokens
                // byte-identically, the honest sibling).
                DrsTextTool.TO_SNAKE_CASE -> toSnakeCase(text)
                DrsTextTool.TO_CAMEL_CASE -> toCamelCase(text)
                // the writing assistant speaks — both tools
                // delegate VERBATIM to the M1.5 engine's public functions
                // (see [DrsWritingAssistant]): the tail-only phrase
                // rewrites and the fixed-point spacing normalizer. No
                // firing / no change returns the text byte-identical.
                DrsTextTool.IMPROVE_PHRASES -> DrsWritingAssistant.improveTail(text)
                DrsTextTool.NORMALIZE_SPACING -> DrsWritingAssistant.normalizeSpacing(text)
                // strip tatweel (kashida) elongation and
                // convert digits between Western and Arabic-Indic forms.
                // Both are per-character, lossless-for-everything-else
                // conversions that never touch letters or punctuation.
                DrsTextTool.REMOVE_TATWEEL -> text.replace(TATWEEL.toString(), "")
                DrsTextTool.TO_ARABIC_DIGITS -> mapChars(text) { ch ->
                    when {
                        ch in '0'..'9' ->
                            (ch.code - '0'.code + ARABIC_INDIC_ZERO.code).toChar()
                        // Persian/Urdu digits (۰-۹) map to the
                        // Arabic-Indic forms as well so ALL eastern digits
                        // normalize to one system.
                        ch.code in EXTENDED_ARABIC_INDIC_ZERO.code..EXTENDED_ARABIC_INDIC_ZERO.code + 9 ->
                            (ch.code - EXTENDED_ARABIC_INDIC_ZERO.code + ARABIC_INDIC_ZERO.code).toChar()
                        else -> ch
                    }
                }
                DrsTextTool.TO_WESTERN_DIGITS -> mapChars(text) { ch ->
                    when (ch.code) {
                        in ARABIC_INDIC_ZERO.code..ARABIC_INDIC_ZERO.code + 9 ->
                            (ch.code - ARABIC_INDIC_ZERO.code + '0'.code).toChar()
                        in EXTENDED_ARABIC_INDIC_ZERO.code..EXTENDED_ARABIC_INDIC_ZERO.code + 9 ->
                            (ch.code - EXTENDED_ARABIC_INDIC_ZERO.code + '0'.code).toChar()
                        else -> ch
                    }
                }
                // sort lines in DESCENDING order with the same
                // locale collator (deterministic counterpart of SORT_LINES).
                DrsTextTool.SORT_LINES_DESC -> {
                    val trailingNewline = text.endsWith("\n")
                    val collator = Collator.getInstance(locale)
                    val sorted = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .sortedWith(compareByDescending(collator) { it })
                        .joinToString("\n")
                    if (trailingNewline) "$sorted\n" else sorted
                }
                // unify Arabic letter variants that carry no
                // meaning once the text is copied elsewhere — per-character,
                // lossless for everything else (see [normalizeArabicLetters]).
                DrsTextTool.NORMALIZE_ARABIC -> normalizeArabicLetters(text)
                // invert the case of every cased Latin/Cyrillic/
                // Greek letter; Arabic (and every other script without case
                // pairs) passes through unchanged.
                DrsTextTool.TOGGLE_CASE -> mapChars(text) { ch ->
                    val upper = ch.uppercaseChar()
                    val lower = ch.lowercaseChar()
                    when {
                        // Uppercase letter: its lowercase form differs.
                        ch == upper && upper != lower -> lower
                        // Lowercase letter: its uppercase form differs.
                        ch == lower && upper != lower -> upper
                        // Caseless (Arabic, digits, punctuation): untouched.
                        else -> ch
                    }
                }
                // map the three Latin sentence punctuation marks
                // to their Arabic counterparts — per-character, everything
                // else untouched.
                DrsTextTool.TO_ARABIC_PUNCTUATION -> mapChars(text) { ch ->
                    when (ch) {
                        ',' -> '\u060C' // ،
                        ';' -> '\u061B' // ؛
                        '?' -> '\u061F' // ؟
                        else -> ch
                    }
                }
                // strip invisible zero-width formatting marks
                // that break search/copy (ZWSP, ZWJ, LRM/RLM, BOM, Unicode
                // isolates). ZWNJ (نصف المسافة) is intentionally kept.
                DrsTextTool.REMOVE_ZERO_WIDTH -> text.replace(ZERO_WIDTH_CHARS, "")
                // every tab becomes four real spaces so the
                // text is safe in fields that collapse or misrender tabs.
                DrsTextTool.TABS_TO_SPACES -> text.replace("\t", "    ")
                DrsTextTool.NORMALIZE_PUNCTUATION -> normalizePunctuation(text)
                // stable sort by line length (ties keep the
                // original relative order); trailing newline preserved.
                DrsTextTool.SORT_LINES_BY_LENGTH -> {
                    val trailingNewline = text.endsWith("\n")
                    val sorted = text.lines()
                        .let { if (trailingNewline) it.dropLast(1) else it }
                        .sortedBy { it.length }
                        .joinToString("\n")
                    if (trailingNewline) "$sorted\n" else sorted
                }
                // remove repeated words across the whole text
                // (first occurrence wins, all whitespace separators kept
                // verbatim) — see [removeDuplicateWords].
                DrsTextTool.REMOVE_DUPLICATE_WORDS -> removeDuplicateWords(text)
                // surround the text with parentheses.
                DrsTextTool.WRAP_PARENS -> "(" + text + ")"
                // one sentence per line — break after sentence
                // enders when horizontal whitespace follows and another
                // non-space character comes after it (idempotent, existing
                // newlines untouched).
                DrsTextTool.SENTENCE_PER_LINE -> text.replace(SENTENCE_BOUNDARY, "$1\n")
                // the inverse of TABS_TO_SPACES — every run of
                // four consecutive spaces becomes one real tab. The regex
                // scans left-to-right, so longer runs split deterministically
                // (8 spaces -> 2 tabs, 5 spaces -> tab + one space).
                DrsTextTool.SPACES_TO_TABS -> text.replace(" ".repeat(4).toRegex(), "\t")
                // the inverse of TO_ARABIC_PUNCTUATION — the
                // three Arabic sentence marks map back to their Latin
                // counterparts, per-character, everything else untouched.
                DrsTextTool.TO_WESTERN_PUNCTUATION -> mapChars(text) { ch ->
                    when (ch) {
                        '\u060C' -> ',' // ، -> ,
                        '\u061B' -> ';' // ؛ -> ;
                        '\u061F' -> '?' // ؟ -> ?
                        else -> ch
                    }
                }
                // strips blank lines at the text's EDGES only —
                // blank separators inside the text are kept exactly as they
                // are (distinct from COLLAPSE_EMPTY_LINES, which keeps one
                // blank separator, and REMOVE_EMPTY_LINES, which removes
                // them all). See [trimBlankEdges].
                DrsTextTool.TRIM_BLANK_EDGES -> trimBlankEdges(text)
                // reverses the word order of every line — the
                // word-level sibling of REVERSE_LINES. Tokens are joined
                // with a single space; blank lines stay blank and a
                // trailing newline survives as-is.
                DrsTextTool.REVERSE_WORDS -> text.lines().joinToString("\n") { line ->
                    line.split(WHITESPACE_RUN).filter { it.isNotEmpty() }
                        .asReversed()
                        .joinToString(" ")
                }
                // repairs Arabic text copied from PDFs and some
                // websites, where letters arrive as PRESENTATION FORMS
                // (U+FB50–U+FDFF, U+FE70–U+FEFF) that break search and are
                // invisible to NORMALIZE_ARABIC. NFKC folds them back to the
                // base letters (including the لا ligature); everything else
                // (letters, digits, spacing) passes through untouched.
                DrsTextTool.NORMALIZE_ARABIC_FORMS ->
                    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
                // turns a comma/semicolon list (Latin or Arabic
                // separators) into one item per line — the natural inverse
                // of JOIN_LINES, and a natural pre-step for NUMBER_LINES.
                DrsTextTool.SPLIT_TO_LINES -> text
                    .split(LIST_SEPARATORS)
                    .joinToString("\n") { it.trim(' ', '\t', '\u00A0') }
                // joins every line into a single list — Arabic
                // comma (،) for Arabic locales, Latin comma elsewhere. The
                // inverse of SPLIT_TO_LINES; blank lines are skipped.
                DrsTextTool.JOIN_LINES -> text.lines()
                    .filter { it.isNotBlank() }
                    .joinToString(if (locale.language == "ar") "، " else ", ") { it.trim(' ', '\t', '\u00A0') }
                // removes EVERY horizontal space (hashtag style
                // #وسم_عربي) — newlines are kept so multi-line text stays
                // line-per-tag. The aggressive sibling of TRIM_SPACES.
                DrsTextTool.REMOVE_ALL_SPACES -> text.replace(HORIZONTAL_SPACES, "")
                // strips emoji/pictographs (and their selectors
                // and joiners) so pasted social text no longer pollutes the
                // counting/sorting/search tools downstream.
                DrsTextTool.STRIP_EMOJI -> text.replace(EMOJI_CHARS, "")
                // percent-encodes the text as a UTF-8 URL
                // component (Arabic links become shareable everywhere).
                // Spaces become %20, not '+', per RFC 3986 paths.
                DrsTextTool.URL_ENCODE -> java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")
                // decodes percent-encoded text back to UTF-8;
                // malformed sequences return the input untouched so typing
                // is never destroyed (decode must never throw).
                DrsTextTool.URL_DECODE -> try {
                    java.net.URLDecoder.decode(text, "UTF-8")
                } catch (_: Throwable) {
                    text
                }
                // base64-encodes the text as UTF-8 — the standard
                // padded alphabet, one line; payloads travel as plain
                // ASCII through any field (the URL pair's data sibling).
                DrsTextTool.BASE64_ENCODE -> base64Encode(text)
                // decodes a Base64 token back to UTF-8: whitespace
                // rejoins the wrapped token, the URL-safe alphabet maps
                // onto the standard one, the STRICT padded shape is
                // required and the bytes must be valid UTF-8 — anything
                // else returns the input untouched so typing is never
                // destroyed (see [base64Decode]).
                DrsTextTool.BASE64_DECODE -> base64Decode(text)

                // the telegraph pair — the third machine alphabet
                // (see [morseEncode] / [morseDecode]).
                DrsTextTool.MORSE_ENCODE -> morseEncode(text)
                DrsTextTool.MORSE_DECODE -> morseDecode(text)
                // the arithmetic evaluator — the whole field IS the
                // expression (see [calculate]).
                DrsTextTool.CALCULATE -> calculate(text)
                // the quantity converter — the whole field IS the
                // quantity (see [convertUnits]).
                DrsTextTool.CONVERT_UNITS -> convertUnits(text)
                // the byte-dump pair — the fourth machine alphabet
                // (see [hexEncode] / [hexDecode]).
                DrsTextTool.HEX_ENCODE -> hexEncode(text)
                DrsTextTool.HEX_DECODE -> hexDecode(text)
                // one space between digits and letters in BOTH
                // directions (12abc -> 12 abc، ٣س -> ٣ س) — the mixed-script
                // cleanup pasted prices and codes need. The lookarounds never
                // consume characters, so punctuation and newlines survive;
                // the inserted space removes the boundary, so idempotent.
                DrsTextTool.SEPARATE_DIGIT_LETTERS -> text
                    .replace(DIGIT_THEN_LETTER, " ")
                    .replace(LETTER_THEN_DIGIT, " ")
                // removes EVERY Unicode punctuation mark (Latin
                // and Arabic ،؛؟ alike). Letters, digits, spaces, symbols and
                // currency survive — the search-prep sibling of CLEAN_TEXT.
                DrsTextTool.REMOVE_PUNCTUATION -> text.replace(PUNCTUATION_CHARS, "")
                DrsTextTool.CLEAN_TEXT -> normalizePunctuation(
                    collapseHorizontalSpaces(
                        text.lines()
                            .filter { it.isNotBlank() }
                            .joinToString("\n") { it.trim(' ', '\t', '\u00A0') },
                    ),
                ).trim(' ', '\t', '\u00A0')
                // the web-paste cleaner — markdown costume off,
                // meaning intact (see [cleanWebPaste]).
                DrsTextTool.CLEAN_MARKDOWN -> cleanWebPaste(text)
                // the HTML-paste cleaner — raw markup off, prose
                // intact (see [htmlToText]).
                DrsTextTool.HTML_TO_TEXT -> htmlToText(text)
                // the link harvester — links out, one per line,
                // order kept; nothing to harvest, byte-identical (see
                // [extractLinks]).
                DrsTextTool.EXTRACT_LINKS -> extractLinks(text)
                // the stretch cleaner — three-plus identical Arabic
                // letters collapse to one, legitimate doubles stay
                // (see [unstretchArabicLetters]).
                DrsTextTool.REMOVE_LETTER_STRETCH -> unstretchArabicLetters(text)
                // COUNT and TEXT_INSPECT never transform; callers use
                // [countInfo] / [inspectText] instead.
                DrsTextTool.COUNT -> text
                DrsTextTool.TEXT_INSPECT -> text
                // Editor ops need the live cursor; handled by EditorInstance.
                // The mark insertions commit at the cursor the same way —
                // nothing to transform here.
                DrsTextTool.DELETE_LINE, DrsTextTool.DELETE_TO_LINE_START,
                DrsTextTool.DELETE_TO_LINE_END,
                DrsTextTool.INSERT_RLM, DrsTextTool.INSERT_LRM,
                DrsTextTool.INSERT_ZWJ, DrsTextTool.INSERT_ZWNJ,
                -> text
            }
        } catch (_: Throwable) {
            text
        }
    }

    /**
     * Real character/word/line counts for the [DrsTextTool.COUNT] info tool.
     * Words = whitespace-separated tokens; lines = number of lines.
     */
    fun countInfo(text: String): Triple<Int, Int, Int> {
        val chars = text.length
        val words = text.split(Regex("\\s+")).count { it.isNotEmpty() }
        val lines = when {
            text.isEmpty() -> 0
            else -> text.lines().size
        }
        return Triple(chars, words, lines)
    }

    /** Uppercases the first letter of every word (rest of the word kept). */
    private fun titleCase(text: String, locale: Locale): String {
        val sb = StringBuilder(text.length)
        var atWordStart = true
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                if (atWordStart) {
                    sb.append(ch.uppercase(locale))
                    atWordStart = false
                } else {
                    sb.append(ch)
                }
            } else {
                sb.append(ch)
                if (ch != '-' && ch != '\'' && ch != '\u2019' && ch != '\u00A0') {
                    // Word boundary: anything that is not a word character and
                    // not an intra-word separator (hyphen, apostrophe, NBSP).
                    atWordStart = true
                }
            }
        }
        return sb.toString()
    }

    /**
     * Lowercases the text, then uppercases the first letter after sentence
     * boundaries ('.', '!', '?', '؟', '…', newline) and at the very start.
     */
    private fun sentenceCase(text: String, locale: Locale): String {
        val lower = text.lowercase(locale)
        val sb = StringBuilder(lower.length)
        var capitalizeNext = true
        for (ch in lower) {
            if (capitalizeNext && ch.isLetter()) {
                sb.append(ch.uppercase(locale))
                capitalizeNext = false
            } else {
                sb.append(ch)
                if (ch in ".!?؟…" || ch == '\n') {
                    capitalizeNext = true
                }
            }
        }
        return sb.toString()
    }

    private fun collapseHorizontalSpaces(text: String): String =
        text.replace(HORIZONTAL_SPACES, " ")

    /**
 * removes repeated whitespace-delimited words across the
     * whole text — the word-level sibling of REMOVE_DUPLICATE_LINES. A
     * manual scanner with a one-run lookbehind keeps the output clean:
     * when a word is dropped, the whitespace run immediately before it is
     * dropped with it (no dangling trailing spaces), while separators
     * around kept words survive verbatim. The first occurrence of each
     * word wins and comparison is exact (case-sensitive) so names and
     * acronyms are never merged away.
     */
    private fun removeDuplicateWords(text: String): String {
        val seen = HashSet<String>()
        val sb = StringBuilder(text.length)
        val pending = StringBuilder()
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            if (ch.isWhitespace()) {
                pending.append(ch)
                index++
                continue
            }
            val wordStart = index
            while (index < text.length && !text[index].isWhitespace()) index++
            val word = text.substring(wordStart, index)
            if (seen.add(word)) {
                sb.append(pending)
                sb.append(word)
            }
            pending.clear()
        }
        // Whitespace after the final word is kept as-is.
        sb.append(pending)
        return sb.toString()
    }

    /** maps every character through [transform] (pure, per-char). */
    private inline fun mapChars(text: String, transform: (Char) -> Char): String {
        var changed = false
        for (ch in text) {
            if (transform(ch) != ch) {
                changed = true
                break
            }
        }
        if (!changed) return text
        val sb = StringBuilder(text.length)
        for (ch in text) sb.append(transform(ch))
        return sb.toString()
    }

    /**
 * removes blank lines at the START and END of the text
     * only. Blank lines between content are kept exactly as they are —
     * the edge-only sibling of COLLAPSE_EMPTY_LINES and
     * REMOVE_EMPTY_LINES. A trailing newline survives as-is when any
     * non-blank line remains; a text of only blank lines becomes "".
     */
    private fun trimBlankEdges(text: String): String {
        val trailingNewline = text.endsWith("\n")
        val lines = text.lines().let { if (trailingNewline) it.dropLast(1) else it }
        val first = lines.indexOfFirst { it.isNotBlank() }
        if (first == -1) return ""
        val last = lines.indexOfLast { it.isNotBlank() }
        val kept = lines.subList(first, last + 1).joinToString("\n")
        return if (trailingNewline) "$kept\n" else kept
    }

    /**
 * collapses every run of consecutive blank lines into a
     * single blank line. The line's own content (spacing included) is kept
     * for non-blank lines, and a trailing newline survives as-is.
     */
    private fun collapseEmptyLines(text: String): String {
        val trailingNewline = text.endsWith("\n")
        val lines = text.lines().let { if (trailingNewline) it.dropLast(1) else it }
        val out = StringBuilder(text.length)
        var previousWasBlank = false
        var first = true
        for (line in lines) {
            val blank = line.isBlank()
            if (blank && previousWasBlank) continue
            if (!first) out.append('\n')
            out.append(line)
            previousWasBlank = blank
            first = false
        }
        if (trailingNewline) out.append('\n')
        return out.toString()
    }

    /**
 * (public) — المُشكِّل الحتمي للنص الكامل (the deterministic
     * whole-text vocalizer). Scans [text] once: every maximal run of
     * Arabic word characters (letters, the nine harakat, tatweel — see
     * [DrsHarakatWordOps.isArabicWordChar]) is a word token; each token
     * is looked up via [DrsWordTashkeel.vocalize] (seed lexicon first,
     * then the installed 3000-word asset — both matched on the STRIPPED
     * form, so partially-marked words still resolve to the canonical
     * form). A known token is replaced by its canonical vocalization.
     *
 * طبقة القانون (the law layer): a token the lexicons
     * do NOT know is no longer always byte-identical. When the pure
     * DrsArabicLetters engine classifies it as a definite-article token
     * followed by a SUN letter and it carries no mark anywhere, the
     * token takes the one mark the assimilation law demands — the sun
     * letter's shadda, and NOTHING beyond it (الشرق → الشّرق): the
     * haraka after the shadda is lexical knowledge the law cannot
     * invent. This is the same partial style the seed lexicon itself
     * speaks for its known sun words (النَّاس، الَّذِي — shadda on the
     * sun letter, lam left bare), so one text never speaks two LAM
     * styles; a known word simply keeps its hand-reviewed haraka as
     * well. The lam's sukun stays the advisor's explicit suggestion,
     * never auto-applied. Every other unknown token — moon followers
     * (المغرب)، hamza carriers (الأمل)، partially-marked words،
     * non-article words — is emitted BYTE-IDENTICAL (the honest rule:
     * the tool never invents what the law does not fix).
     *
 * العقد الخاص المعلن في the COLLAPSED glued
     * form لل (ل + ال، الألف المنحلة) joins the law layer — every token
     * opening with لل + letter IS the preposition + the article, so a
     * sun follower takes the same partial-style shadda (للناس ← للنَّاس).
     * The other glued shapes (بالش فالش كالش والش) stay byte-identical
     * BY DOCUMENTED PROOF: والد/والدين and بالطو share the exact token
     * shape without containing an article — separating them is
     * dictionary knowledge, the guessing territory the creed forbids.
     * See [DrsArabicLetters.gluedShaddaForm].
     *
 * شهادة الجلالة (the majesty certification): the
     * CLOSED hand-pinned family of لفظ الجلالة tokens (لله والله
     * بالله فالله كالله تالله ولله وبالله فلله) joins the law layer —
     * each member is an exact enumerated string whose only reading is
     * the majesty name, so its lam takes the same partial-style shadda
     * (والله ← واللَّه) — a table like the fourteen sun letters, never
     * a pattern over an open class. The open [حرف + ال + شمسية] shape
 * (والد، بالش، بالطو) stays byte-identical per 's proof —
     * none of its members ends in لله. The seed lexicon still wins for
     * the members it hand-reviews (لله والله بالله). See
     * [DrsArabicLetters.majestyShaddaForm].
     *
     * Every non-Arabic character — spaces, newlines, punctuation,
     * digits, Latin — passes through verbatim, so punctuation and line
     * structure are preserved exactly. Idempotent by construction:
     * applying the tool to its own output re-derives the same canonical
     * forms (a law-marked token now carries a mark and passes through).
     */
    private fun tashkeelText(text: String): String {
        return buildString(text.length + 64) {
            var i = 0
            while (i < text.length) {
                val c = text[i]
                if (DrsHarakatWordOps.isArabicWordChar(c)) {
                    var j = i + 1
                    while (j < text.length && DrsHarakatWordOps.isArabicWordChar(text[j])) j++
                    val word = text.substring(i, j)
                    append(
                        DrsWordTashkeel.vocalize(word)
                            ?: DrsArabicLetters.gluedShaddaForm(word)
                            ?: DrsArabicLetters.majestyShaddaForm(word)
                            ?: DrsArabicLetters.shaddaForm(word)
                            ?: word,
                    )
                    i = j
                } else {
                    append(c)
                    i++
                }
            }
        }
    }

    /**
 * unifies Arabic letter variants commonly interchanged in
     * search and copy contexts: hamza-carried alefs (أ إ آ ٱ) become a bare
     * alef (ا), ta-marbuta (ة) becomes ha (ه) and alef maqsura (ى) becomes
     * ya (ي). Pure per-character mapping — diacritics, digits, punctuation
     * and every non-Arabic character pass through untouched.
     */
    private fun normalizeArabicLetters(text: String): String = mapChars(text) { ch ->
        when (ch) {
            '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627' // أ إ آ ٱ -> ا
            '\u0629' -> '\u0647'                              // ة -> ه
            '\u0649' -> '\u064A'                              // ى -> ي
            else -> ch
        }
    }

    /**
     * Fixes spacing around punctuation in both directions:
     *  - removes spaces before punctuation and closing brackets,
     *  - guarantees exactly one space after sentence punctuation when the
     *    next character is a letter ('.' only for Arabic letters so URLs,
     *    file names and decimals are never broken).
     */
    private fun normalizePunctuation(text: String): String {
        var out = text.replace(SPACE_BEFORE_PUNCT, "$1")
        out = out.replace(SPACE_AFTER_PUNCT, "$1 ")
        out = out.replace(DOT_BEFORE_ARABIC, ". ")
        return out
    }

    // ------------------------------------------------------------
    // the four new tiles' pure helpers
    // ------------------------------------------------------------

    /** The natural-sort chunking: digit runs vs non-digit runs. */
    private val NATURAL_CHUNKS = Regex("\\d+|\\D+")

    /**
     * The natural comparison of two lines: digit chunks compare
     * numerically at arbitrary precision ([compareNumericChunks] — no
     * Long overflow, file10 sorts after file9 whatever the magnitude),
     * letter chunks through the same locale Collator as SORT_LINES,
     * and a mixed pair puts digits first — one closed convention.
     */
    private fun compareNaturalLines(a: String, b: String, collator: Collator): Int {
        val ac = NATURAL_CHUNKS.findAll(a).map { it.value }.toList()
        val bc = NATURAL_CHUNKS.findAll(b).map { it.value }.toList()
        val n = minOf(ac.size, bc.size)
        for (i in 0 until n) {
            val x = ac[i]
            val y = bc[i]
            val xd = x.first().isDigit()
            val yd = y.first().isDigit()
            val cmp = when {
                xd && yd -> compareNumericChunks(x, y)
                xd -> -1
                yd -> 1
                else -> collator.compare(x, y)
            }
            if (cmp != 0) return cmp
        }
        return ac.size.compareTo(bc.size)
    }

    /**
     * Arbitrary-precision numeric chunk comparison: leading zeros
     * trimmed, then length decides, then lexicographic — never a Long.
     */
    private fun compareNumericChunks(x: String, y: String): Int {
        val xn = x.trimStart('0')
        val yn = y.trimStart('0')
        val byLen = xn.length.compareTo(yn.length)
        return if (byLen != 0) byLen else xn.compareTo(yn)
    }

    /** The slug's strip class: harakat, tatweel and the invisible marks. */
    private val SLUG_STRIP =
        Regex("[\u064B-\u0652\u0670\u0640\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF]")

    /** Runs of whitespace/underscores become ONE dash, then the rest is
     *  narrowed to letters/digits/dashes, dashes collapsed and trimmed. */
    private val SLUG_SEPARATORS = Regex("[\\s_]+")
    private val SLUG_KEEP = Regex("[^\\p{L}\\p{Nd}-]")
    private val SLUG_DASH_RUNS = Regex("-{2,}")

    /**
     * The link-safe slug: Arabic keeps its FOLDED letters (never
     * transliterated — never invented), everything outside letters/
     * digits/dashes falls away, and a text of nothing but punctuation
     * returns byte-identical — never an empty slug.
     */
    private fun slugify(text: String): String {
        var s = SLUG_STRIP.replace(text, "")
        for (ch in "أإآٱ") s = s.replace(ch, 'ا')
        s = s.replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .lowercase(Locale.ROOT)
        s = SLUG_SEPARATORS.replace(s, "-")
        s = SLUG_KEEP.replace(s, "")
        s = SLUG_DASH_RUNS.replace(s, "-")
        s = s.trim('-')
        return if (s.isEmpty()) text else s
    }

    // -----------------------------------------------------------
    // CLEAN_MARKDOWN — the web-paste cleaner
    // -----------------------------------------------------------

    // Line-leading shapes (MULTILINE): headers, blockquotes, bullets,
    // horizontal rules and code fences. Each needs its marker + the
    // required space (a bare “#عنوان” is NOT the shape — refusal).
    private val MD_HEADER = Regex("(?m)^[ \\t]{0,3}#{1,6}[ \\t]+")
    private val MD_BLOCKQUOTE = Regex("(?m)^[ \\t]{0,3}(?:>[ \\t]?)+")
    private val MD_BULLET = Regex("(?m)^[ \\t]{0,3}[-*+•][ \\t]+")
    private val MD_RULE =
        Regex("(?m)^[ \\t]{0,3}([-*_])[ \\t]*(?:\\1[ \\t]*){2,}[ \\t]*$\\n?")
    private val MD_FENCE = Regex("(?m)^[ \\t]{0,3}`{3,}[^\\n]*\\n?")

    // Inline shapes: images BEFORE links (an image embeds a link shape
    // inside itself), then autolinks, links, code spans, strike, and the
    // emphasis family. The underscore-italic form is unwrapped ONLY when
    // the content is Latin and the neighbors are not word characters —
    // the lookarounds + Latin-only content guard some_var_name AND
    // Arabic snake-ish underscores (متغير_و_آخر) alike.
    private val MD_IMAGE = Regex("!\\[([^\\]\\n]*)\\]\\([^)\\n]+\\)")
    private val MD_AUTOLINK = Regex("<(https?://[^>\\n]+)>")
    private val MD_LINK = Regex("\\[([^\\]\\n]+)\\]\\([^)\\n]+\\)")
    private val MD_CODE = Regex("`([^`\\n]+)`")
    private val MD_STRIKE = Regex("~~(.+?)~~")
    // The asterisk emphasis is legit intraword (a**b**c — CommonMark),
    // so no neighbor guards. The UNDERSCORE emphasis is guarded on both
    // sides: intraword __bold__ is NOT markdown, and unguarded it would
    // shred a__b__c and snake_case alike — the guard + the Latin-only
    // single-underscore rule keep those byte-identical. Triples run
    // before doubles (else ** eats ***).
    private val MD_BOLD_STAR3 = Regex("\\*\\*\\*(.+?)\\*\\*\\*")
    private val MD_BOLD_UND3 =
        Regex("(?<![A-Za-z0-9_])___(.+?)___(?![A-Za-z0-9_])")
    private val MD_BOLD_STAR2 = Regex("\\*\\*(.+?)\\*\\*")
    private val MD_BOLD_UND2 =
        Regex("(?<![A-Za-z0-9_])__(.+?)__(?![A-Za-z0-9_])")
    private val MD_ITALIC_STAR = Regex("\\*([^*\\n]+)\\*")
    private val MD_ITALIC_UND =
        Regex("(?<![A-Za-z0-9_])_([A-Za-z0-9][^_\\n]*?)_(?![A-Za-z0-9_])")

    /**
     * The web-paste cleaner: the copy that arrives wearing markdown
     * keeps its meaning and loses the costume — leading # headers, >
     * blockquotes, - * + • bullets and --- rules vanish; ***bold***,
     * __bold__, *italic*, ~~strike~~, `code` and ``` fences unwrap to
     * their bare text (underscore-italic for Latin content only);
     * [text](url) speaks its text, ![alt](url) its alt and <url> its
     * address. Numbered lists, snake_case and Arabic underscores are
     * never touched; tables and HTML entities are out of the closed
     * catalog; a text carrying none of the shapes is returned
     * byte-identical — never a cosmetic rewrite of prose that needs
     * none.
     */
    private fun cleanWebPaste(text: String): String {
        var out = MD_FENCE.replace(text, "")
        out = MD_RULE.replace(out, "")
        out = MD_HEADER.replace(out, "")
        out = MD_BLOCKQUOTE.replace(out, "")
        out = MD_BULLET.replace(out, "")
        out = MD_IMAGE.replace(out) { m -> m.groupValues[1] }
        out = MD_AUTOLINK.replace(out) { m -> m.groupValues[1] }
        out = MD_LINK.replace(out) { m -> m.groupValues[1] }
        out = MD_CODE.replace(out) { m -> m.groupValues[1] }
        out = MD_STRIKE.replace(out) { m -> m.groupValues[1] }
        out = MD_BOLD_STAR3.replace(out) { m -> m.groupValues[1] }
        out = MD_BOLD_UND3.replace(out) { m -> m.groupValues[1] }
        out = MD_BOLD_STAR2.replace(out) { m -> m.groupValues[1] }
        out = MD_BOLD_UND2.replace(out) { m -> m.groupValues[1] }
        out = MD_ITALIC_STAR.replace(out) { m -> m.groupValues[1] }
        out = MD_ITALIC_UND.replace(out) { m -> m.groupValues[1] }
        return out
    }

    // -----------------------------------------------------------
    // HTML_TO_TEXT — the raw-markup paste cleaner
    // -----------------------------------------------------------

    // Markup that is never prose: comments, <!declarations> and the
    // <script>/<style> blocks — the latter vanish WITH their content
    // (code pasted from a page's source is not the page's text).
    private val HTML_COMMENT = Regex("(?s)<!--.*?-->")
    private val HTML_DECL = Regex("<![^>]*>")
    private val HTML_SCRIPT = Regex("(?is)<script\\b.*?</script\\s*>")
    private val HTML_STYLE = Regex("(?is)<style\\b.*?</style\\s*>")

    // Structure carriers: <br> becomes a real newline, block-level
    // closers become newlines (one closed tag list), and <li> keeps its
    // «- » bullet so a pasted list still reads as a list — the exact
    // shape CLEAN_MARKDOWN strips, so the two cleaners compose.
    private val HTML_BR = Regex("(?i)<br\\s*/?>")
    private val HTML_BLOCK_END = Regex(
        "(?i)</(?:p|div|li|h[1-6]|tr|blockquote|pre|ul|ol|table|section|article|header|footer)>",
    )
    private val HTML_LI = Regex("(?i)<li(?:\\s[^<>]*)?>")

    // Every other WELL-FORMED tag: <tag …> or </tag> (attributes may not
    // carry '<' or '>'; a quoted '>' inside an attribute is OUT of the
    // closed catalog — the paste is left as-is there, never guessed).
    private val HTML_TAG = Regex("</?[A-Za-z][A-Za-z0-9:-]*(?:\\s[^<>]*)?/?>")

    // The closed entity whitelist + the numeric forms. One pass, no
    // double decode: &amp;lt; lands as &lt;, never as '<'.
    private val HTML_ENTITY =
        Regex("&(?:#x([0-9A-Fa-f]+)|#([0-9]+)|amp|lt|gt|quot|apos|nbsp|shy|mdash|ndash|hellip|laquo|raquo|copy|reg|trade);")

    /**
     * The raw-markup paste cleaner: HTML arrives, prose leaves. Comments
     * and declarations vanish, script/style blocks vanish WITH their
     * content, <br> becomes a newline, block-level closers become
     * newlines, <li> keeps its «- » bullet and every other well-formed
     * tag strips to nothing. The closed entity whitelist decodes in ONE
     * pass; a numeric reference outside the valid code-point zone (NUL,
     * surrogates, past 0x10FFFF) stays verbatim — never invented. A lone
     * '<' that opens no tag (a < b) and a text carrying none of the
     * closed shapes return byte-identical.
     */
    private fun htmlToText(text: String): String {
        var out = HTML_COMMENT.replace(text, "")
        out = HTML_DECL.replace(out, "")
        out = HTML_SCRIPT.replace(out, "")
        out = HTML_STYLE.replace(out, "")
        out = HTML_BR.replace(out, "\n")
        out = HTML_BLOCK_END.replace(out, "\n")
        out = HTML_LI.replace(out, "- ")
        out = HTML_TAG.replace(out, "")
        out = decodeHtmlEntities(out)
        return if (out == text) text else out
    }

    /** One-pass entity decode: a whitelisted name or a valid numeric
     *  code point (supplementary planes included via surrogate pairs),
     *  anything else verbatim — null means «leave the match as-is». */
    private fun decodeHtmlEntities(text: String): String {
        if ('&' !in text) return text
        return HTML_ENTITY.replace(text) { m ->
            val hex = m.groupValues[1]
            val dec = m.groupValues[2]
            val codepoint = when {
                hex.isNotEmpty() -> hex.toIntOrNull(16)
                dec.isNotEmpty() -> dec.toIntOrNull()
                else -> null
            }
            if (codepoint != null) {
                if (codepoint in 1..0x10FFFF &&
                    (codepoint < 0xD800 || codepoint > 0xDFFF)
                ) {
                    String(java.lang.Character.toChars(codepoint))
                } else {
                    m.value
                }
            } else {
                when (m.value) {
                    "&amp;" -> "&"
                    "&lt;" -> "<"
                    "&gt;" -> ">"
                    "&quot;" -> "\""
                    "&apos;" -> "'"
                    "&nbsp;" -> " "
                    "&shy;" -> ""
                    "&mdash;" -> "—"
                    "&ndash;" -> "–"
                    "&hellip;" -> "…"
                    "&laquo;" -> "«"
                    "&raquo;" -> "»"
                    "&copy;" -> "©"
                    "&reg;" -> "®"
                    "&trade;" -> "™"
                    else -> m.value
                }
            }
        }
    }

    // -----------------------------------------------------------
    // EXTRACT_LINKS — the link harvester
    // -----------------------------------------------------------

    /**
     * The closed link shapes in ONE scan, alternatives tried at each
     * position left-to-right (first match consumes its span, spans
     * never overlap):
     *  - a SCHEME URL: http/https/ftp (the only schemes the closed
     *    catalog speaks) then a run of non-whitespace, non-angle
     *    characters — the \b refuses «xhttps://», the charset refuses
     *    nothing else;
     *  - a STRICT ASCII email: local [A-Za-z0-9._%+-] at domain
     *    [A-Za-z0-9.-] with a ≥2-letter TLD (no bare a@b, no IDN, no
     *    IP domains — refused, never guessed);
     *  - a www. address whose preceding character is NOT a letter,
     *    digit, dot, dash or @ — never inside a larger word or domain
     *    (a@www.x matches as an EMAIL first; mail.www.example.com
     *    matches nowhere).
     */
    private val LINK_SHAPES = Regex(
        "(?i)\\b(?:(?:https?|ftp)://[^\\s<>]+)" +
            "|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}" +
            "|(?<![A-Za-z0-9._@-])www\\.[^\\s<>]+",
    )

    // Trailing sentence punctuation a link may arrive wearing —
    // the closed set (the Arabic ؟ ، ؛ included). Brackets are
    // deliberately NOT here: a balanced wiki-style (…) belongs to
    // the URL and only the UNbalanced closing ones leave.
    private val LINK_TRAILING_PUNCT = charArrayOf(
        '.', ',', ';', ':', '!', '?', '؟', '،', '؛', '"', '\'', '»', '«', '…',
    )

    /** One link match trimmed to its honest span: trailing sentence
     *  punctuation leaves (the closed set), then closing brackets
     *  leave ONLY while the link's bracket depth is net-negative
     *  (the balanced wiki-style (…) survives), then one final
     *  punctuation pass for the dot a bracket was hiding. */
    private fun trimLinkEdge(link: String): String {
        var out = link.trimEnd(*LINK_TRAILING_PUNCT)
        while (out.isNotEmpty()) {
            val last = out.last()
            val leaves = when (last) {
                ')' -> out.count { it == '(' } < out.count { it == ')' }
                ']' -> out.count { it == '[' } < out.count { it == ']' }
                '}' -> out.count { it == '{' } < out.count { it == '}' }
                else -> false
            }
            if (!leaves) break
            out = out.dropLast(1)
        }
        return out.trimEnd(*LINK_TRAILING_PUNCT)
    }

    /**
     * The link harvester: every scheme URL, www. address and strict
     * ASCII email in the text lands one per line, in order of
     * appearance, each a verbatim substring (case and bytes kept) —
     * duplicates are the text's own business, never silently deduped.
     * A text with no link shape returns byte-identical, never an
     * empty or invented row.
     */
    private fun extractLinks(text: String): String {
        val found = ArrayList<String>()
        for (match in LINK_SHAPES.findAll(text)) {
            val link = trimLinkEdge(match.value)
            if (link.isNotEmpty()) found += link
        }
        return if (found.isEmpty()) text else found.joinToString("\n")
    }

    // -----------------------------------------------------------
    // BASE64_ENCODE / BASE64_DECODE — the payload pair
    // -----------------------------------------------------------

    /** Whitespace that rejoining a wrapped token strips first:
     *  spaces, tabs, the CR/LF family and the NBSP family — never
     *  the token's own alphabet. */
    private val BASE64_WRAP_WS = Regex("[ \\t\\r\\n\\u00A0\\u2007\\u202F]+")

    /**
     * The STRICT RFC 4648 shape: four-char groups, then optionally a
     * final group of two chars + '==' or three chars + '='. Anything
     * shorter, longer, unpadded or double-padded is NOT the shape.
     */
    private val BASE64_SHAPE =
        Regex("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")

    /**
     * The encoder: UTF-8 bytes over the standard (not URL-safe)
     * alphabet with its padding, one line — total and deterministic.
     */
    private fun base64Encode(text: String): String =
        java.util.Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    /**
     * The decoder: wrapped tokens rejoin (spaces/tabs/newlines/the
     * NBSP family strip first — the token's own alphabet never
     * does), the URL-safe alphabet (- _) maps onto the standard one,
     * and the STRICT padded shape gates the decode; the decoded
     * bytes must themselves decode as valid UTF-8 (REPORT, never
     * the replacement char) — anything else returns the input
     * byte-identically. A whitespace-only field is untouched
     * (nothing to decode). Decode must never destroy typing.
     */
    private fun base64Decode(text: String): String {
        val compact = BASE64_WRAP_WS.replace(text, "")
        if (compact.isEmpty()) return text
        val normalized = compact.replace('-', '+').replace('_', '/')
        if (!BASE64_SHAPE.matches(normalized)) return text
        val bytes = try {
            java.util.Base64.getDecoder().decode(normalized)
        } catch (_: Throwable) {
            return text
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: Throwable) {
            text
        }
    }

    // -----------------------------------------------------------
    // MORSE_ENCODE / MORSE_DECODE — the telegraph pair
    // -----------------------------------------------------------

    /**
     * The OFFICIAL ITU-R M.1677-1 sign table — the encoder's single
     * source of truth: the 28 Arabic letters + the standalone hamza
     * (29 signs, injective — the ITU Arabic zone carries NO internal
     * collision), the 26 Latin letters, the 10 digits and the closed
     * punctuation set, all their international signs. NO slash entry:
     * its international sign (−··−) IS the Arabic sad in the ITU
     * tables themselves, the decoder speaks ص, and the encoder
     * refuses '/' outright (it is the channel's own word mark).
     */
    private val MORSE_SIGNS: Map<Char, String> = mapOf(
        // Arabic (ITU-R M.1677-1) — the historic telegraph signs.
        'ا' to "·−", 'ب' to "−···", 'ت' to "−", 'ث' to "−·−·",
        'ج' to "·−−−", 'ح' to "····", 'خ' to "−−−", 'د' to "−··",
        'ذ' to "−−··", 'ر' to "·−·", 'ز' to "−−−·", 'س' to "···",
        'ش' to "−−−−", 'ص' to "−··−", 'ض' to "···−", 'ط' to "··−",
        'ظ' to "−·−−", 'ع' to "·−·−", 'غ' to "−−·", 'ف' to "··−·",
        'ق' to "−−·−", 'ك' to "−·−", 'ل' to "·−··", 'م' to "−−",
        'ن' to "−·", 'ه' to "··−··", 'و' to "·−−", 'ي' to "··",
        'ء' to "·",
        // Latin (international) — homographs of the Arabic signs by
        // design: the channel is scriptless, decode speaks Arabic.
        'a' to "·−", 'b' to "−···", 'c' to "−·−·", 'd' to "−··",
        'e' to "·", 'f' to "··−·", 'g' to "−−·", 'h' to "····",
        'i' to "··", 'j' to "·−−−", 'k' to "−·−", 'l' to "·−··",
        'm' to "−−", 'n' to "−·", 'o' to "−−−", 'p' to "·−−·",
        'q' to "−−·−", 'r' to "·−·", 's' to "···", 't' to "−",
        'u' to "··−", 'v' to "···−", 'w' to "·−−", 'x' to "−··−",
        'y' to "−·−−", 'z' to "−−··",
        // Digits — the same five-glyph ladder for every script.
        '1' to "·−−−−", '2' to "··−−−", '3' to "···−−",
        '4' to "····−", '5' to "·····", '6' to "−····",
        '7' to "−−···", '8' to "−−−··", '9' to "−−−−·",
        '0' to "−−−−−",
        // Punctuation (international, closed) — no slash, see above.
        '.' to "·−·−·−", ',' to "−−··−−", '?' to "··−−··",
        '!' to "−·−·−−", '\'' to "·−−−−·", '"' to "·−··−·",
        '(' to "−·−−·", ')' to "−·−−·−", '&' to "·−···",
        ':' to "−−−···", ';' to "−·−·−·", '=' to "−···−",
        '+' to "·−·−·", '-' to "−····−", '_' to "··−−·−",
        '$' to "···−··−", '@' to "·−−·−·",
    )

    /**
     * The encode-side folds, applied BEFORE the sign lookup: hamza
     * carriers (أإآٱ→ا، ة→ه، ى→ي، ئ→ي، ؤ→و — the SLUGIFY doctrine),
     * Arabic punctuation onto its Latin homograph (؟،،؛ → ? , ;) and
     * the Eastern digit families onto Western digits. U+2212 lands on
     * the ASCII hyphen. Latin case folds in [morseEncode] itself.
     */
    private val MORSE_FOLDS: Map<Char, Char> = mapOf(
        'أ' to 'ا', 'إ' to 'ا', 'آ' to 'ا', 'ٱ' to 'ا',
        'ة' to 'ه', 'ى' to 'ي', 'ئ' to 'ي', 'ؤ' to 'و',
        '؟' to '?', '،' to ',', '؛' to ';', '−' to '-',
    ) + (0..9).associate { d -> (0x0660 + d).toChar() to ('0' + d) } +
        (0..9).associate { d -> (0x06F0 + d).toChar() to ('0' + d) }

    /**
     * The decode map — the sign-to-char mirror carrying the ARABIC
     * winners only (the Latin homographs are excluded: the channel is
     * scriptless and the keyboard speaks Arabic). 56 entries, injective
     * (simulated and proved — check_morse.py); digits and punctuation
     * return as themselves.
     */
    private val MORSE_REVERSE: Map<String, Char> =
        MORSE_SIGNS.filterKeys { it !in 'a'..'z' }
            .entries.associate { (ch, sign) -> sign to ch }

    /** Dot glyphs the decoder normalizes onto the canonical '·'. */
    private val MORSE_DOT_GLYPHS = charArrayOf('.', '·')

    /** Dash glyphs the decoder normalizes onto the canonical '−'. */
    private val MORSE_DASH_GLYPHS = charArrayOf('-', '−', '─', '—', '–')

    /** Whitespace that folds to one plain space in the decode shape:
     *  spaces, tabs, the CR/LF family and the NBSP family. */
    private val MORSE_WS = Regex("[ \\t\\r\\n\\u00A0\\u2007\\u202F]+")

    /** Marks stripped before the sign lookup: the harakat block,
     *  the Quranic annotations, the superscript alef, the tatweel
     *  and the zero-width family. */
    private fun isMorseInvisible(ch: Char): Boolean =
        ch.code in 0x064B..0x065F || ch.code == 0x0670 ||
            ch.code in 0x0610..0x061A || ch.code in 0x06D6..0x06ED ||
            ch.code == 0x0640 || ch.code in 0x200B..0x200F

    /** The word-gap whitespace class of the encode side. */
    private fun isMorseSpace(ch: Char): Boolean =
        ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' ||
            ch == '\u00A0' || ch == '\u2007' || ch == '\u202F'

    /**
     * The encoder: every character speaks its ITU sign, letters join
     * with one space, words with « / ». Folds first (see
     * [MORSE_FOLDS]), strips invisible marks, folds Latin case — and
     * REFUSES the whole text byte-identically the moment a character
     * has no sign ('/', emoji, other scripts, presentation forms) or
     * nothing codable remains (marks only). Total and deterministic
     * over the closed alphabet; never an invented sign.
     */
    private fun morseEncode(text: String): String {
        val words = ArrayList<String>()
        val signs = ArrayList<String>()
        var sawToken = false
        fun flushWord() {
            if (signs.isNotEmpty()) {
                words += signs.joinToString(" ")
                signs.clear()
            }
        }
        for (ch0 in text) {
            when {
                isMorseSpace(ch0) -> flushWord()
                isMorseInvisible(ch0) -> Unit
                ch0 == '/' -> return text
                else -> {
                    val folded = MORSE_FOLDS[ch0] ?: ch0
                    val key = if (folded in 'A'..'Z') folded.lowercaseChar() else folded
                    val sign = MORSE_SIGNS[key] ?: return text
                    sawToken = true
                    signs += sign
                }
            }
        }
        flushWord()
        if (!sawToken || words.isEmpty()) return text
        return words.joinToString(" / ")
    }

    /**
     * The decoder: whitespace (the NBSP family included) folds to one
     * plain space and edges trim; the words split on « / » and the
     * signs on spaces; every sign's glyphs normalize onto the
     * canonical pair FIRST, then the closed reverse map speaks —
     * Arabic letters (documented winners), digits, punctuation. An
     * empty word (leading/trailing/doubled slash), an empty sign, a
     * foreign glyph or an unknown sign returns the input
     * byte-identically. Decode must never destroy typing — never a
     * guess, never an invented letter.
     */
    private fun morseDecode(text: String): String {
        val s = MORSE_WS.replace(text, " ").trim(' ')
        if (s.isEmpty()) return text
        if (s.first() == '/' || s.last() == '/') return text
        val wordsOut = ArrayList<String>()
        for (word in s.split('/')) {
            val w = word.trim(' ')
            if (w.isEmpty()) return text
            val sb = StringBuilder(w.length)
            for (token in w.split(' ')) {
                if (token.isEmpty()) return text
                val canonical = buildString(token.length) {
                    for (c in token) when (c) {
                        in MORSE_DOT_GLYPHS -> append('·')
                        in MORSE_DASH_GLYPHS -> append('−')
                        else -> return text
                    }
                }
                val letter = MORSE_REVERSE[canonical] ?: return text
                sb.append(letter)
            }
            wordsOut += sb.toString()
        }
        return wordsOut.joinToString(" ")
    }

    // -----------------------------------------------------------
    // REMOVE_LETTER_STRETCH — the Arabic stretch cleaner
    // -----------------------------------------------------------

    /**
     * The tool's closed zone: a letter of the Arabic block
     * (U+0600..U+06FF) — [Character.isLetter] already refuses the
     * digits, the harakat and the punctuation there, and the three
     * modifier-letter stragglers are refused explicitly (the
     * tatweel U+0640, the superscript alef U+0670, the Koranic
     * small letters U+06E5..U+06E6). Latin letters are OUT of the
     * zone on purpose: «www» is three letters in a row and stays.
     */
    private fun isArabicBlockLetter(ch: Char): Boolean {
        if (ch.code !in 0x0600..0x06FF) return false
        if (!Character.isLetter(ch)) return false
        if (ch.code == 0x0640 || ch.code == 0x0670) return false
        if (ch.code in 0x06D6..0x06ED) return false
        return true
    }

    /**
     * The stretch cleaner: a run of THREE or more identical Arabic
     * letters collapses to ONE (three in a row never occurs in
     * standard orthography); a run of two is legitimate doubling —
     * الله، عدّ — and stays verbatim. A diacritic after a stretched
     * run follows the collapsed letter (the run breaks at any
     * non-letter). Latin, digits, punctuation and every non-Arabic
     * character are out of the zone, and a text with no stretched
     * run returns byte-identical.
     */
    private fun unstretchArabicLetters(text: String): String {
        if (text.length < 3) return text
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (isArabicBlockLetter(ch)) {
                var runEnd = i + 1
                while (runEnd < text.length && text[runEnd] == ch) runEnd++
                val runLen = runEnd - i
                if (runLen >= 3) {
                    out.append(ch)
                } else {
                    repeat(runLen) { out.append(ch) }
                }
                i = runEnd
            } else {
                out.append(ch)
                i++
            }
        }
        val result = out.toString()
        return if (result == text) text else result
    }

    // -----------------------------------------------------------
    // TEXT_INSPECT — the text census (the info counterpart)
    // -----------------------------------------------------------

    /** The invisible formatting marks (the same class REMOVE_ZERO_WIDTH
     *  strips): zero-width joiners/spaces, directionals, isolates, BOM. */
    private val INVISIBLE_CHARS =
        Regex("[\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF]")

    /** The closed sentence enders across scripts: Latin . ! ? the
     *  ellipsis and the Arabic question mark. */
    private val SENTENCE_ENDERS = charArrayOf('.', '!', '?', '…', '\u061F')

    /** The emoji code-point zones of the census: pictographs (both
     *  planes EMOJI_CHARS strips) plus the variation selector and the
     *  ZWJ joiner. Counted at CODE-POINT level — codePoints() decodes
     *  a surrogate pair whole, so one emoji counts ONE, never two
     *  halves (the char-level EMOJI_CHARS regex is for removal, not
     *  for counting). */
    private fun isEmojiCodePoint(cp: Int): Boolean = when (cp) {
        in 0x1F000..0x1FAFF -> true
        in 0x2600..0x27BF -> true
        0xFE0F, 0x200D -> true
        else -> false
    }

    /** One closed census row-bundle — every field a counted class of
     *  the same single scan, deterministic on every input. */
    data class TextCensus(
        val chars: Int,
        val arabicLetters: Int,
        val latinLetters: Int,
        val otherLetters: Int,
        val digits: Int,
        val words: Int,
        val sentences: Int,
        val lines: Int,
        val spaces: Int,
        val punctuation: Int,
        val invisible: Int,
        val emoji: Int,
    )

    /** True for punctuation and symbol categories (Arabic ،؛؟ included,
     *  currency and math symbols alike) — the census's sixth class.
     *  getType() returns Int while the category constants are bytes —
     *  the census compares in the constants' own type. */
    private fun isPunctOrSymbol(ch: Char): Boolean {
        return when (Character.getType(ch).toByte()) {
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.CONNECTOR_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION,
            Character.MATH_SYMBOL,
            Character.CURRENCY_SYMBOL,
            Character.MODIFIER_SYMBOL,
            Character.OTHER_SYMBOL,
            -> true
            else -> false
        }
    }

    /**
     * The read-only census behind [DrsTextTool.TEXT_INSPECT]: one scan
     * classifies every character (letters by script — Arabic/Latin/other,
     * the closed three-system digit set, whitespace, punctuation/symbols,
     * invisible marks, emoji) and counts the structural units (words,
     * sentences over the closed ender set, lines) exactly like
     * [countInfo] counts. Pure and never-throwing; the empty text
     * reports a zeroed census.
     */
    fun inspectText(text: String): TextCensus {
        var arabic = 0
        var latin = 0
        var other = 0
        var digits = 0
        var spaces = 0
        var punctuation = 0
        for (ch in text) {
            when {
                ch in '0'..'9' ||
                    ch in '\u0660'..'\u0669' ||
                    ch in '\u06F0'..'\u06F9' -> digits++
                ch.isLetter() -> when {
                    ch.code in 0x621..0x64A || ch.code in 0x671..0x6D3 -> arabic++
                    ch.code in 0x41..0x5A || ch.code in 0x61..0x7A -> latin++
                    else -> other++
                }
                ch.isWhitespace() -> spaces++
                isPunctOrSymbol(ch) -> punctuation++
            }
        }
        var sentences = 0
        var pending = false
        for (ch in text) {
            when {
                SENTENCE_ENDERS.contains(ch) -> {
                    if (pending) {
                        sentences++
                        pending = false
                    }
                }
                ch.isWhitespace() -> Unit // whitespace neither ends nor starts
                else -> pending = true
            }
        }
        if (pending) sentences++
        return TextCensus(
            chars = text.length,
            arabicLetters = arabic,
            latinLetters = latin,
            otherLetters = other,
            digits = digits,
            words = text.split(Regex("\\s+")).count { it.isNotEmpty() },
            sentences = sentences,
            lines = if (text.isEmpty()) 0 else text.lines().size,
            spaces = spaces,
            punctuation = punctuation,
            invisible = INVISIBLE_CHARS.findAll(text).sumOf { it.value.length },
            emoji = text.codePoints().toArray().count { cp -> isEmojiCodePoint(cp) },
        )
    }

    /** Word boundaries of the snake/camel pair: spaces, underscores, dashes. */
    private val CASE_WORD_SPLIT = Regex("[\\s_\\-]+")

    /** The two camel boundaries: a lowercase letter or digit before an
     *  uppercase (get|HTTP), and an uppercase before UPPER+lower (HTTP|Response). */
    private val CAMEL_BOUNDARY =
        Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")

    /**
     * snake_case: words (split on spaces/underscores/dashes, then on
     * lower→upper camel boundaries) join with one underscore, lowercased.
     * Arabic words join the same way with no case invented.
     */
    private fun toSnakeCase(text: String): String {
        val tokens = ArrayList<String>()
        for (raw in CASE_WORD_SPLIT.split(text.trim())) {
            if (raw.isEmpty()) continue
            tokens += CAMEL_BOUNDARY.split(raw).filter { it.isNotEmpty() }
        }
        if (tokens.isEmpty()) return text
        val joined = tokens.joinToString("_") { it.lowercase(Locale.ROOT) }
        return if (joined == text) text else joined
    }

    /**
     * camelCase — the honest sibling: tokens join with NO separator,
     * the first lowercased and the rest capitalized — and any token
     * carrying a non-ASCII letter refuses the whole transform
     * byte-identically (camelCase is a Latin code convention; inventing
     * letter shapes for Arabic is a guess).
     */
    private fun toCamelCase(text: String): String {
        val tokens = CASE_WORD_SPLIT.split(text.trim()).filter { it.isNotEmpty() }
        // One token has no join to perform — the honest no-op.
        if (tokens.size <= 1) return text
        if (tokens.any { token -> token.any { it.code > 127 } }) return text
        val first = tokens.first().lowercase(Locale.ROOT)
        val rest = tokens.drop(1).joinToString("") { token ->
            token.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
        val joined = first + rest
        return if (joined == text) text else joined
    }

    // -----------------------------------------------------------
    // CALCULATE — the arithmetic expression evaluator
    // -----------------------------------------------------------

    /**
     * The symbol homographs the calculator folds onto the canonical
     * grammar BEFORE parsing: the Arabic decimal/thousands separators,
     * the multiplication/division/minus family (× ÷ − – — and their
     * fullwidth siblings), the Arabic and fullwidth percent, the caret
     * family and the fullwidth parens. Digits fold in [calcNormalize].
     */
    private val CALC_FOLDS: Map<Char, String> = mapOf(
        '\u066B' to ".",  // arabic decimal separator ٫
        '\u066C' to "",   // arabic thousands separator ٬
        '\u00D7' to "*",  // ×
        '\u00F7' to "/",  // ÷
        '\u2212' to "-", '\u2013' to "-", '\u2014' to "-", '\uFE63' to "-",
        '\uFF0B' to "+", '\uFF0D' to "-", '\uFF0A' to "*", '\uFF0F' to "/",
        '\uFF08' to "(", '\uFF09' to ")", '\uFF1E' to ">",
        '\u066A' to "%", '\uFF05' to "%",
        '\uFF3E' to "^", '\u2038' to "^",
    )

    /** The NBSP family folds to a plain space in the parser's eyes. */
    private val CALC_SOFT_SPACES = charArrayOf('\u00A0', '\u2007', '\u202F')

    /** A longer "expression" is not a typing shape — the guard that
     *  keeps the advisor's hot path cheap. */
    private const val CALC_MAX_LEN = 256

    /** The number literal grammar (the parser anchors it itself). */
    private val CALC_NUMBER = Regex("\\d+(?:\\.\\d+)?|\\.\\d+")

    /** A Latin comma BETWEEN digits is a thousands separator —
     *  «1,234» speaks 1234. A comma anywhere else is a foreign
     *  character and refuses the parse. */
    private val CALC_THOUSANDS_COMMA = Regex("(?<=\\d),(?=\\d)")

    /**
     * The digit/symbol/separator normalization: Eastern digits (both
     * families) fold to 0-9, the folds table applies, the NBSP family
     * becomes a plain space and the between-digits comma vanishes.
     */
    private fun calcNormalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when {
                ch.code in 0x0660..0x0669 -> sb.append(('0' + (ch.code - 0x0660)))
                ch.code in 0x06F0..0x06F9 -> sb.append(('0' + (ch.code - 0x06F0)))
                ch in CALC_SOFT_SPACES -> sb.append(' ')
                else -> sb.append(CALC_FOLDS[ch] ?: ch.toString())
            }
        }
        return sb.toString().replace(CALC_THOUSANDS_COMMA, "")
    }

    /**
     * The recursive-descent parser — the exact mirror of the
     * session's check_calc_units.py: expr → term → unary → power →
     * primary, `^` right-associative, unary minus, the percent postfix
     * binding to its own number (pure math: 15% == 0.15), and the
     * number literal anchored where the parser stands. Any failure
     * sets [ok] and the caller refuses the text byte-identically.
     */
    private class CalcParser(private val s: String) {
        var index = 0
        var ok = true

        private fun peek(): Char = if (index < s.length) s[index] else '\u0000'

        private fun skipSpaces() {
            while (index < s.length && s[index] == ' ') index++
        }

        /** Parses the WHOLE string as one expression. */
        fun parse(): Double {
            val v = expr()
            skipSpaces()
            if (!ok || index != s.length) ok = false
            return v
        }

        private fun expr(): Double {
            var v = term()
            while (ok) {
                skipSpaces()
                val c = peek()
                if (c == '+' || c == '-') {
                    index++
                    val rhs = term()
                    v = if (c == '+') v + rhs else v - rhs
                } else {
                    break
                }
            }
            return v
        }

        private fun term(): Double {
            var v = unary()
            while (ok) {
                skipSpaces()
                val c = peek()
                if (c == '*' || c == '/') {
                    index++
                    val rhs = unary()
                    if (!ok) break
                    v = if (c == '*') v * rhs else v / rhs
                } else {
                    break
                }
            }
            return v
        }

        private fun unary(): Double {
            skipSpaces()
            when (peek()) {
                '-' -> {
                    index++
                    return -unary()
                }
                '+' -> {
                    index++
                    return unary()
                }
            }
            return power()
        }

        private fun power(): Double {
            val base = primary()
            if (!ok) return base
            skipSpaces()
            if (peek() == '^') {
                index++
                return Math.pow(base, unary())
            }
            return base
        }

        private fun primary(): Double {
            skipSpaces()
            if (peek() == '(') {
                index++
                val v = expr()
                skipSpaces()
                if (peek() != ')') {
                    ok = false
                    return 0.0
                }
                index++
                return v
            }
            val m = CALC_NUMBER.find(s, index) ?: run {
                ok = false
                return 0.0
            }
            if (m.range.first != index) {
                ok = false
                return 0.0
            }
            index = m.range.last + 1
            var v = m.value.toDouble()
            if (peek() == '%') {
                index++
                v /= 100.0
            }
            return v
        }
    }

    /**
     * The result formatter — an exact integer (within 1e-9, inside
     * the 9.007e15 safe zone) prints bare; anything else keeps 12
     * SIGNIFICANT digits (HALF_EVEN), trailing zeros stripped, plain
     * notation only. Null refuses the commit (NaN/Infinity — a
     * division by zero included).
     */
    private fun formatCalcResult(r: Double): String? {
        if (r.isNaN() || r.isInfinite()) return null
        if (r == 0.0) return "0"
        if (Math.abs(r - Math.rint(r)) < 1e-9 && Math.abs(r) < 9.007e15) {
            return Math.rint(r).toLong().toString()
        }
        return java.math.BigDecimal(r.toString(), java.math.MathContext(12))
            .stripTrailingZeros()
            .toPlainString()
    }

    /**
     * The evaluator behind [DrsTextTool.CALCULATE]: the WHOLE field
     * must parse as one expression — the firing contract requires at
     * least one operator/paren/percent in the normalized text (a bare
     * number is not an attempt), any foreign character, unbalanced
     * parenthesis, dangling operator or non-finite result refuses the
     * text byte-identically, and a result equal to the input is the
     * honest no-op. Pure, never throwing.
     */
    private fun calculate(text: String): String {
        val s = calcNormalize(text)
        if (s.length > CALC_MAX_LEN) return text
        if (s.none { it in "+-*/^%()" }) return text
        val p = CalcParser(s)
        val v = p.parse()
        if (!p.ok) return text
        val out = formatCalcResult(v) ?: return text
        return if (out == text) text else out
    }

    // -----------------------------------------------------------
    // CONVERT_UNITS — the quantity converter
    // -----------------------------------------------------------

    /** One closed unit group: its category, the exact factor into the
     *  category's base unit, the ONE Arabic display token the tool
     *  answers in, and every accepted spelling (Arabic and English). */
    private class UnitGroup(
        val category: String,
        val display: String,
        val factor: Double,
        val keys: List<String>,
    )

    /** The closed catalog — seven categories, every group's display
     *  token repeated first in its own keys (so the index always owns
     *  its display). Factors are EXACT definitions, not approximations
     *  (the mile is 1609.344 m, the gallon 3.785411784 l). */
    private val UNIT_GROUPS: List<UnitGroup> = listOf(
        // length (base meter)
        UnitGroup("length", "م", 1.0, listOf("م", "متر", "مترات", "m", "meter", "meters")),
        UnitGroup("length", "مم", 0.001, listOf("مم", "مليمتر", "mm", "millimeter")),
        UnitGroup("length", "سم", 0.01, listOf("سم", "سنتيمتر", "cm", "centimeter")),
        UnitGroup("length", "كم", 1000.0, listOf("كم", "كيلومتر", "كيلومترات", "km", "kilometer")),
        UnitGroup("length", "ميل", 1609.344, listOf("ميل", "أميال", "mi", "mile", "miles")),
        UnitGroup("length", "قدم", 0.3048, listOf("قدم", "أقدام", "ft", "foot", "feet")),
        UnitGroup("length", "بوصة", 0.0254, listOf("بوصة", "بوصات", "in", "inch", "inches")),
        UnitGroup("length", "ياردة", 0.9144, listOf("ياردة", "ياردات", "yd", "yard")),
        // mass (base kilogram)
        UnitGroup("mass", "كغ", 1.0, listOf(
            "كغ", "كيلوغرام", "كيلوجرام", "كيلوغرامات", "kg", "kilogram", "kilo",
        )),
        UnitGroup("mass", "جم", 0.001, listOf("جم", "غرام", "جرام", "غرامات", "g", "gram", "grams")),
        UnitGroup("mass", "مج", 0.000001, listOf("مج", "مليغرام", "مليجرام", "mg", "milligram")),
        UnitGroup("mass", "طن", 1000.0, listOf("طن", "أطنان", "t", "ton", "tonne")),
        UnitGroup("mass", "رطل", 0.45359237, listOf("رطل", "أرطال", "lb", "lbs", "pound")),
        UnitGroup("mass", "أوقية", 0.028349523125, listOf("أوقية", "oz", "ounce")),
        // volume (base liter)
        UnitGroup("volume", "ل", 1.0, listOf("ل", "لتر", "ليتر", "لترات", "l", "liter", "litre")),
        UnitGroup("volume", "مل", 0.001, listOf("مل", "مليلتر", "مليليلتر", "ml", "milliliter")),
        UnitGroup("volume", "م³", 1000.0, listOf("م³", "م3", "متر مكعب", "مكعب", "m3", "m³", "cubic meter")),
        UnitGroup("volume", "جالون", 3.785411784, listOf("جالون", "جالونات", "gal", "gallon")),
        UnitGroup("volume", "كوب", 0.24, listOf("كوب", "أكواب", "cup", "cups")),
        // area (base m²)
        UnitGroup("area", "م²", 1.0, listOf("م²", "م2", "متر مربع", "متر مربعة", "m2", "m²", "square meter")),
        UnitGroup("area", "سم²", 0.0001, listOf("سم²", "سم2", "سنتيمتر مربع", "cm2", "cm²")),
        UnitGroup("area", "كم²", 1000000.0, listOf("كم²", "كم2", "كيلومتر مربع", "km2", "km²")),
        UnitGroup("area", "هكتار", 10000.0, listOf("هكتار", "هكتارات", "ha", "hectare")),
        UnitGroup("area", "دونم", 1000.0, listOf("دونم", "دنم", "دونمات", "dunam")),
        // speed (base m/s)
        UnitGroup("speed", "م/س", 1.0, listOf("م/س", "م/ث", "متر في الثانية", "m/s", "mps")),
        UnitGroup("speed", "كم/س", 1000.0 / 3600.0, listOf(
            "كم/س", "كم/ساعة", "كيلومتر في الساعة", "km/h", "kph", "kmh",
        )),
        UnitGroup("speed", "ميل/س", 0.44704, listOf("ميل/س", "ميل في الساعة", "mph")),
        UnitGroup("speed", "عقدة", 1852.0 / 3600.0, listOf("عقدة", "عقد", "knot", "knots", "kn")),
        // data (base byte, SI decimal — 1 kb = 1000 bytes, documented)
        UnitGroup("data", "بايت", 1.0, listOf("بايت", "b", "byte", "bytes")),
        UnitGroup("data", "كيلوبايت", 1000.0, listOf("كيلوبايت", "kb")),
        UnitGroup("data", "ميجابايت", 1000000.0, listOf("ميجابايت", "mb")),
        UnitGroup("data", "جيجابايت", 1000000000.0, listOf("جيجابايت", "gb")),
        UnitGroup("data", "تيرابايت", 1000000000000.0, listOf("تيرابايت", "tb")),
        // time (base second; the year is the Julian year 365.25 d)
        UnitGroup("time", "ث", 1.0, listOf("ث", "ثانية", "ثوان", "ثواني", "s", "sec", "second")),
        UnitGroup("time", "د", 60.0, listOf("د", "دقيقة", "دقائق", "min", "minute")),
        UnitGroup("time", "ساعة", 3600.0, listOf("ساعة", "ساعات", "h", "hour", "hr")),
        UnitGroup("time", "يوم", 86400.0, listOf("يوم", "أيام", "d", "day", "days")),
        UnitGroup("time", "أسبوع", 604800.0, listOf("أسبوع", "أسابيع", "week", "weeks")),
        UnitGroup("time", "سنة", 31557600.0, listOf("سنة", "سنوات", "year", "years")),
    )

    /** The lookup key of one spelling: trimmed, whitespace-collapsed,
     *  case-folded (ROOT — never a locale surprise). */
    private fun canonicalUnitKey(k: String): String =
        k.trim().split(Regex("\\s+")).joinToString(" ").lowercase(Locale.ROOT)

    /** Every spelling resolves to its group (later duplicates of the
     *  same spelling would overwrite — the catalog is verified unique
     *  by the structural audit). */
    private val UNIT_INDEX: Map<String, UnitGroup> = UNIT_GROUPS
        .flatMap { g -> g.keys.map { canonicalUnitKey(it) to g } }
        .toMap()

    /** The closed per-category answer ladders — canonical display
     *  tokens only, the input unit's own line skipped at runtime. */
    private val UNIT_LADDERS: Map<String, List<String>> = mapOf(
        "length" to listOf("م", "كم", "سم", "ميل", "قدم"),
        "mass" to listOf("جم", "كغ", "طن", "رطل"),
        "volume" to listOf("مل", "ل", "جالون", "كوب"),
        "area" to listOf("م²", "كم²", "هكتار", "دونم"),
        "speed" to listOf("م/س", "كم/س", "ميل/س", "عقدة"),
        "data" to listOf("كيلوبايت", "ميجابايت", "جيجابايت"),
        "time" to listOf("ث", "د", "ساعة", "يوم"),
    )

    /** The temperature scales — a separate closed map because they are
     *  FORMULAS, not factors. «س» alone is NOT celsius (س = second is
     *  the older abbreviation); the explicit °س / سلسيوس / مئوية
     *  spellings own the scale. */
    private val TEMP_SCALE_KEYS: Map<String, Char> = mapOf(
        "°c" to 'C', "c" to 'C', "سلسيوس" to 'C', "مئوية" to 'C', "°س" to 'C', "س°" to 'C',
        "°f" to 'F', "f" to 'F', "فهرنهايت" to 'F', "°ف" to 'F', "ف°" to 'F',
        "k" to 'K', "ك" to 'K', "كلفن" to 'K', "°k" to 'K',
    )

    /** The canonical Arabic display token of each temperature scale. */
    private val TEMP_DISPLAY: Map<Char, String> =
        mapOf('C' to "°م", 'F' to "°ف", 'K' to "ك")

    private fun tempToCelsius(v: Double, from: Char): Double = when (from) {
        'F' -> (v - 32.0) * 5.0 / 9.0
        'K' -> v - 273.15
        else -> v
    }

    private fun tempFromCelsius(c: Double, to: Char): Double = when (to) {
        'F' -> c * 9.0 / 5.0 + 32.0
        'K' -> c + 273.15
        else -> c
    }

    /** The digit/separator normalization of the quantity shape — the
     *  Eastern digit families fold, the Arabic decimal separator
     *  becomes the dot and the Arabic thousands separator vanishes. */
    private fun unitsNormalize(text: String): String = buildString(text.length) {
        for (ch in text) {
            when {
                ch.code in 0x0660..0x0669 -> append(('0' + (ch.code - 0x0660)))
                ch.code in 0x06F0..0x06F9 -> append(('0' + (ch.code - 0x06F0)))
                ch == '\u066B' -> append('.')
                ch == '\u066C' -> Unit
                else -> append(ch)
            }
        }
    }

    /** The whole-text quantity shape: one number, optional spaces, ONE
     *  unit token (multi-word units join with single spaces). Anchored
     *  to the whole string — prose never matches. */
    private val UNIT_QUANTITY = Regex("^(-?\\d+(?:\\.\\d+)?|\\.\\d+)\\s*(.+)\\z")

    /** The explicit-target separator: «إلى»/«to» need surrounding
     *  spaces, the arrows may glue («5كم→م»). Split ONCE. */
    private val UNIT_TARGET_SEP = Regex("\\s+(?:إلى|→|->|to)\\s+|\\s*(?:→|->)\\s*")

    /**
     * The converter behind [DrsTextTool.CONVERT_UNITS]: the WHOLE
     * field must be one quantity — a number and ONE unit from the
     * closed catalog (an optional «إلى/to/→» target first split off).
     * No explicit target answers the category's ladder (the input
     * unit's own line skipped); an explicit target answers ONE line,
     * same category only, in its canonical Arabic display token. Any
     * other shape — prose, a bare number, an unknown unit, a
     * cross-category target — returns byte-identical, never an
     * invented conversion. Pure, never throwing.
     */
    private fun convertUnits(text: String): String {
        val s = unitsNormalize(text).replace('\u00A0', ' ').trim()
        val m = UNIT_QUANTITY.find(s) ?: return text
        val value = m.groupValues[1].toDouble()
        var unitRaw = m.groupValues[2].trim()
        var target: String? = null
        val parts = UNIT_TARGET_SEP.split(unitRaw, 2)
        if (parts.size == 2 && parts[0].trim().isNotEmpty() && parts[1].trim().isNotEmpty()) {
            unitRaw = parts[0].trim()
            target = parts[1].trim()
        }
        val key = canonicalUnitKey(unitRaw)
        val scale = TEMP_SCALE_KEYS[key]
        if (scale != null) {
            if (target != null) {
                val dst = TEMP_SCALE_KEYS[canonicalUnitKey(target)] ?: return text
                if (dst == scale) return text
                val out = formatCalcResult(tempFromCelsius(tempToCelsius(value, scale), dst))
                    ?: return text
                return "$out ${TEMP_DISPLAY[dst]}"
            }
            val lines = mutableListOf<String>()
            for (dst in listOf('C', 'F', 'K')) {
                if (dst == scale) continue
                val out = formatCalcResult(tempFromCelsius(tempToCelsius(value, scale), dst))
                    ?: return text
                lines += "$out ${TEMP_DISPLAY[dst]}"
            }
            return lines.joinToString("\n")
        }
        val group = UNIT_INDEX[key] ?: return text
        if (target != null) {
            val tgt = UNIT_INDEX[canonicalUnitKey(target)] ?: return text
            if (tgt.category != group.category) return text
            val out = formatCalcResult(value * group.factor / tgt.factor) ?: return text
            return "$out ${tgt.display}"
        }
        val ladder = UNIT_LADDERS[group.category] ?: return text
        val lines = mutableListOf<String>()
        for (tok in ladder) {
            val tgt = UNIT_INDEX[tok] ?: continue
            if (tgt.display == group.display) continue
            val out = formatCalcResult(value * group.factor / tgt.factor) ?: return text
            lines += "$out $tok"
        }
        return lines.joinToString("\n")
    }

    // -----------------------------------------------------------
    // HEX_ENCODE / HEX_DECODE — the byte-dump pair
    // -----------------------------------------------------------

    /** The canonical lowercase hex alphabet of the encode side. */
    private const val HEX_ALPHABET = "0123456789abcdef"

    /** Whitespace that folds away in the decode shape: spaces, tabs,
     *  the CR/LF family and the NBSP family. */
    private val HEX_WS = Regex("[ \\t\\r\\n\\u00A0\\u2007\\u202F]+")

    /** The hex value of one digit — negative when [ch] is outside the
     *  alphabet (both cases). Eastern digits are OUT by design: the
     *  byte dump is a machine notation, the catalog stays closed. */
    private fun hexValue(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch.code - '0'.code
        in 'a'..'f' -> ch.code - 'a'.code + 10
        in 'A'..'F' -> ch.code - 'A'.code + 10
        else -> -1
    }

    /**
     * The encoder: the text's UTF-8 bytes speak the classic byte dump —
     * lowercase two-digit pairs joined with ONE space, one line (the
     * xx.d-style listing the HEX_DECODE mirror reads back byte-exact).
     * Total and deterministic: every non-empty text encodes; the
     * per-byte mask keeps Java's signed byte off the output.
     */
    private fun hexEncode(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size * 3)
        for ((i, b) in bytes.withIndex()) {
            if (i > 0) sb.append(' ')
            val v = b.toInt() and 0xFF
            sb.append(HEX_ALPHABET[v ushr 4]).append(HEX_ALPHABET[v and 0x0F])
        }
        return sb.toString()
    }

    /**
     * The decoder: whitespace (the NBSP family included) folds to one
     * plain space and edges trim, then exactly one of TWO closed shapes
     * speaks: the SOLID dump (one token — an optional 0x/0X prefix over
     * an even-length run of hex digits) or the TOKEN list (every token
     * EXACTLY two hex digits, each optionally 0x-prefixed). The bytes
     * must decode as valid UTF-8 (REPORT, never a replacement char) —
     * an odd length, a non-hex character, mixed token lengths, a
     * colon/percent separator, an eastern-digit dump or binary bytes all
     * return the input byte-identically. Decode must never destroy
     * typing — never a guess.
     */
    private fun hexDecode(text: String): String {
        val s = HEX_WS.replace(text, " ").trim(' ')
        if (s.isEmpty()) return text
        val tokens = s.split(' ')
        val digits = StringBuilder()
        if (tokens.size == 1) {
            // Shape A — the SOLID dump.
            val body = hexStripPrefix(tokens[0]) ?: return text
            if (body.isEmpty() || body.length % 2 != 0) return text
            for (c in body) {
                if (hexValue(c) < 0) return text
                digits.append(c)
            }
        } else {
            // Shape B — the TOKEN list: every token exactly two digits.
            for (token in tokens) {
                val body = hexStripPrefix(token) ?: return text
                if (body.length != 2) return text
                for (c in body) {
                    if (hexValue(c) < 0) return text
                    digits.append(c)
                }
            }
        }
        val bytes = ByteArray(digits.length / 2)
        for (i in bytes.indices) {
            bytes[i] = (((hexValue(digits[i * 2]) shl 4) or hexValue(digits[i * 2 + 1]))).toByte()
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: Throwable) {
            text
        }
    }

    /** The token body after its optional 0x/0X prefix — null when the
     *  token carries no digits at all behind the prefix («0x» alone). */
    private fun hexStripPrefix(token: String): String? {
        val body = if (token.length > 2 &&
            (token.startsWith("0x") || token.startsWith("0X"))
        ) {
            token.substring(2)
        } else {
            token
        }
        return if (body.isEmpty()) null else body
    }
}
