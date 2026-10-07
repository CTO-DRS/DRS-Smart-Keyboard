package com.drs.keyboard.keyboard

/**
 * Static layout data. Every row is normalized to the same total width by the
 * renderer, so widthUnits only express *relative* key sizes.
 *
 * English long-press accents and Arabic alef variants cover the most common
 * needs; every char key may also expose a shifted variant.
 */
object Layouts {

    private fun s(str: String): List<Int> = str.map { it.code }

    // ---------------------------------------------------------------------
    // English QWERTY
    // ---------------------------------------------------------------------

    private val EN_ROW1 = listOf(
        KeyDef.char("q", 'q'.code, "Q", 'Q'.code, s("@#&*_")),
        KeyDef.char("w", 'w'.code, "W", 'W'.code, s("\"'?")),
        KeyDef.char("e", 'e'.code, "E", 'E'.code, s("éèêëęėē")),
        KeyDef.char("r", 'r'.code, "R", 'R'.code, s("ŕř")),
        KeyDef.char("t", 't'.code, "T", 'T'.code, s("țþ")),
        KeyDef.char("y", 'y'.code, "Y", 'Y'.code, s("ÿý")),
        KeyDef.char("u", 'u'.code, "U", 'U'.code, s("üûúùūű")),
        KeyDef.char("i", 'i'.code, "I", 'I'.code, s("îïíìįī")),
        KeyDef.char("o", 'o'.code, "O", 'O'.code, s("öôóòõœøő")),
        KeyDef.char("p", 'p'.code, "P", 'P'.code)
    )

    private val EN_ROW2 = listOf(
        KeyDef.char("a", 'a'.code, "A", 'A'.code, s("äàáâãåąæª"), width = 1.11f),
        KeyDef.char("s", 's'.code, "S", 'S'.code, s("ßśšşș")),
        KeyDef.char("d", 'd'.code, "D", 'D'.code, s("đď")),
        KeyDef.char("f", 'f'.code, "F", 'F'.code, s("€£¥")),
        KeyDef.char("g", 'g'.code, "G", 'G'.code, s("ĝ")),
        KeyDef.char("h", 'h'.code, "H", 'H'.code),
        KeyDef.char("j", 'j'.code, "J", 'J'.code),
        KeyDef.char("k", 'k'.code, "K", 'K'.code),
        KeyDef.char("l", 'l'.code, "L", 'L'.code, s("łľĺ"), width = 1.11f)
    )

    private val EN_ROW3 = listOf(
        KeyDef.fn("⇧", KeyDef.CODE_SHIFT, KeyDef.KeyType.SHIFT, 1.5f),
        KeyDef.char("z", 'z'.code, "Z", 'Z'.code, s("žźż"), width = 0.875f),
        KeyDef.char("x", 'x'.code, "X", 'X'.code, width = 0.875f),
        KeyDef.char("c", 'c'.code, "C", 'C'.code, s("çćčĉ"), width = 0.875f),
        KeyDef.char("v", 'v'.code, "V", 'V'.code, width = 0.875f),
        KeyDef.char("b", 'b'.code, "B", 'B'.code, width = 0.875f),
        KeyDef.char("n", 'n'.code, "N", 'N'.code, s("ñńň"), width = 0.875f),
        KeyDef.char("m", 'm'.code, "M", 'M'.code, width = 0.875f),
        KeyDef.fn("⌫", KeyDef.CODE_BACKSPACE, KeyDef.KeyType.BACKSPACE, 1.5f)
    )

    private val EN_ROW4 = listOf(
        KeyDef.fn("🌐", KeyDef.CODE_LANG, KeyDef.KeyType.LANG, 0.9f),
        KeyDef.fn("☺", KeyDef.CODE_EMOJI, KeyDef.KeyType.EMOJI, 0.9f),
        KeyDef.fn("?123", KeyDef.CODE_MODE_SYMBOLS, KeyDef.KeyType.MODE_SYMBOLS, 1.1f),
        KeyDef.char(",", ','.code, "!", '!'.code, s(";:"), width = 0.85f),
        KeyDef.fn(" ", ' '.code, KeyDef.KeyType.SPACE, 3.8f),
        KeyDef.char(".", '.'.code, "?", '?'.code, s("…!?"), width = 0.85f),
        KeyDef.fn("⏎", KeyDef.CODE_ENTER, KeyDef.KeyType.ENTER, 1.6f)
    )

    // ---------------------------------------------------------------------
    // Arabic (101 layout) — shift codes are explicit unicode codepoints
    // ---------------------------------------------------------------------

    private fun ar(label: String, code: Int, shiftLabel: String? = null,
                   shiftCode: Int = code, alts: String = "", width: Float = 1f) =
        KeyDef.char(label, code, shiftLabel, shiftCode,
            alts.map { it.code }, alts.map { it.toString() }, width)

    private val AR_ROW1 = listOf(
        ar("ض", 0x0636, "ْ", 0x0652),
        ar("ص", 0x0635, "ً", 0x064B),
        ar("ث", 0x062B, "ٌ", 0x064C),
        ar("ق", 0x0642, "ٍ", 0x064D),
        ar("ف", 0x0641, "لإ", 0xFEF7),
        ar("غ", 0x063A, "إ", 0x0625),
        ar("ع", 0x0639, "‘", 0x2018),
        ar("ه", 0x0647, "÷", 0x00F7, "ة"),
        ar("خ", 0x062E, "؛", 0x061B, "ح"),
        ar("ح", 0x062D, "،", 0x060C),
        ar("ج", 0x062C, "<", 0x003C, "چ"),
        ar("د", 0x062F, ">", 0x003E)
    )

    private val AR_ROW2 = listOf(
        ar("ش", 0x0634, "ُ", 0x064F, width = 1.11f),
        ar("س", 0x0633, "ِ", 0x0650),
        ar("ي", 0x064A, "]", 0x005D, "ئى"),
        ar("ب", 0x0628, "[", 0x005B),
        ar("ل", 0x0644, "لأ", 0xFEF5, "ڵ"),
        ar("ا", 0x0627, "أ", 0x0623, "آإءٱ"),
        ar("ت", 0x062A, "ـ", 0x0640),
        ar("ن", 0x0646, "آ", 0x0622),
        ar("م", 0x0645, "/", 0x002F, width = 1.11f),
        ar("ك", 0x0643, ":", 0x003A, "گ")
    )

    private val AR_ROW3 = listOf(
        KeyDef.fn("⇧", KeyDef.CODE_SHIFT, KeyDef.KeyType.SHIFT, 1.5f),
        ar("ط", 0x0637, "ّ", 0x0651, width = 0.875f),
        ar("ئ", 0x0626, "}", 0x007D, width = 0.875f),
        ar("ء", 0x0621, "{", 0x007B, width = 0.875f),
        ar("ؤ", 0x0624, "\"", 0x0022, width = 0.875f),
        ar("ر", 0x0631, "لآ", 0xFEF9, width = 0.875f),
        ar("لا", 0xFEFB, "لآ", 0xFEF9, width = 0.875f),
        ar("ى", 0x0649, "ئ", 0x0626, "ي", width = 0.875f),
        ar("ة", 0x0629, "'", 0x0027, width = 0.875f),
        ar("و", 0x0648, "،", 0x060C, "ؤ", width = 0.875f),
        ar("ز", 0x0632, ".", 0x002E, "ژ", width = 0.875f),
        ar("ظ", 0x0638, "؟", 0x061F, width = 0.875f),
        KeyDef.fn("⌫", KeyDef.CODE_BACKSPACE, KeyDef.KeyType.BACKSPACE, 1.5f)
    )

    private val AR_ROW4 = listOf(
        KeyDef.fn("🌐", KeyDef.CODE_LANG, KeyDef.KeyType.LANG, 0.9f),
        KeyDef.fn("☺", KeyDef.CODE_EMOJI, KeyDef.KeyType.EMOJI, 0.9f),
        KeyDef.fn("؟123", KeyDef.CODE_MODE_SYMBOLS, KeyDef.KeyType.MODE_SYMBOLS, 1.1f),
        KeyDef.char("،", 0x060C, "!", '!'.code, s(";:"), width = 0.85f),
        KeyDef.fn(" ", ' '.code, KeyDef.KeyType.SPACE, 3.8f),
        KeyDef.char(".", '.'.code, "؟", 0x061F, s("…!؟"), width = 0.85f),
        KeyDef.fn("⏎", KeyDef.CODE_ENTER, KeyDef.KeyType.ENTER, 1.6f)
    )

    // ---------------------------------------------------------------------
    // Navigation arrows (optional row) — absolute directions, never RTL-flipped
    // ---------------------------------------------------------------------

    private fun nav(label: String, code: Int) =
        KeyDef.fn(label, code, KeyDef.KeyType.NAV, 2.5f)

    private val NAV_ROW = listOf(
        nav("←", KeyDef.CODE_NAV_LEFT),
        nav("↑", KeyDef.CODE_NAV_UP),
        nav("↓", KeyDef.CODE_NAV_DOWN),
        nav("→", KeyDef.CODE_NAV_RIGHT)
    )

    // ---------------------------------------------------------------------
    // Arabic tashkeel (optional row): fatha/damma/kasra/shadda/sukun,
    // the three tanweens, dagger alef and tatweel — inserted as combining
    // marks that join the word being composed instead of ending it.
    // ---------------------------------------------------------------------

    private fun hk(label: String, code: Int) =
        KeyDef.char(label, code, labelSize = 20f)

    private val HARAKAT_ROW = listOf(
        hk("َ", 0x064E),   // fatha
        hk("ُ", 0x064F),   // damma
        hk("ِ", 0x0650),   // kasra
        hk("ّ", 0x0651),   // shadda
        hk("ْ", 0x0652),   // sukun
        hk("ً", 0x064B),   // fathatan
        hk("ٌ", 0x064C),   // dammatan
        hk("ٍ", 0x064D),   // kasratan
        hk("ٰ", 0x0670),   // dagger alef
        hk("ـ", 0x0640)    // tatweel
    )

    // ---------------------------------------------------------------------
    // Symbols
    // ---------------------------------------------------------------------

    private val SYM_ROW1 = listOf(
        KeyDef.char("@", '@'.code), KeyDef.char("#", '#'.code), KeyDef.char("$", '$'.code, alts = s("€£¥₹")),
        KeyDef.char("%", '%'.code), KeyDef.char("&", '&'.code), KeyDef.char("-", '-'.code, alts = s("–—_")),
        KeyDef.char("+", '+'.code, alts = s("±")), KeyDef.char("(", '('.code, alts = s("[{")),
        KeyDef.char(")", ')'.code, alts = s("]}"))
    )

    private val SYM_ROW2 = listOf(
        KeyDef.char("=", '='.code, alts = s("≈≠"), width = 1.05f),
        KeyDef.char("<", '<'.code, alts = s("«"), width = 1.05f),
        KeyDef.char(">", '>'.code, alts = s("»"), width = 1.05f),
        KeyDef.char("*", '*'.code, alts = s("†‡"), width = 1.05f),
        KeyDef.char("\"", '"'.code, alts = s("„“”«»"), width = 1.05f),
        KeyDef.char("'", '\''.code, alts = s("‘’‚"), width = 1.05f),
        KeyDef.char(":", ':'.code, width = 1.05f),
        KeyDef.char(";", ';'.code, width = 1.05f),
        KeyDef.char("!", '!'.code, alts = s("¡"), width = 1.05f),
        KeyDef.char("?", '?'.code, alts = s("¿؟"), width = 1.05f)
    )

    private val SYM_ROW3 = listOf(
        KeyDef.fn("=\\<", KeyDef.CODE_MODE_NUM, KeyDef.KeyType.MODE_NUM, 1.5f),
        KeyDef.char("/", '/'.code, alts = s("\\"), width = 0.875f),
        KeyDef.char("\\", '\\'.code, width = 0.875f),
        KeyDef.char("~", '~'.code, alts = s("≈"), width = 0.875f),
        KeyDef.char("^", '^'.code, width = 0.875f),
        KeyDef.char("_", '_'.code, width = 0.875f),
        KeyDef.char("|", '|'.code, width = 0.875f),
        KeyDef.char("•", '•'.code, alts = s("◦·"), width = 0.875f),
        KeyDef.char("°", '°'.code, alts = s("′″"), width = 0.875f),
        KeyDef.fn("⌫", KeyDef.CODE_BACKSPACE, KeyDef.KeyType.BACKSPACE, 1.5f)
    )

    private val SYM_ROW4 = listOf(
        KeyDef.fn("ABC", KeyDef.CODE_MODE_ALPHA, KeyDef.KeyType.MODE_ALPHA, 1.3f),
        KeyDef.char(",", ','.code, alts = s(";:"), width = 0.9f),
        KeyDef.fn(" ", ' '.code, KeyDef.KeyType.SPACE, 4.2f),
        KeyDef.char(".", '.'.code, alts = s("…"), width = 0.9f),
        KeyDef.fn("⏎", KeyDef.CODE_ENTER, KeyDef.KeyType.ENTER, 1.7f)
    )

    // ---------------------------------------------------------------------
    // Numbers (locked pad)
    // ---------------------------------------------------------------------

    private val NUM_ROW1 = listOf(
        KeyDef.char("1", '1'.code), KeyDef.char("2", '2'.code), KeyDef.char("3", '3'.code),
        KeyDef.char("4", '4'.code), KeyDef.char("5", '5'.code), KeyDef.char("6", '6'.code),
        KeyDef.char("7", '7'.code), KeyDef.char("8", '8'.code), KeyDef.char("9", '9'.code),
        KeyDef.char("0", '0'.code)
    )

    private val NUM_ROW2 = listOf(
        KeyDef.char("-", '-'.code), KeyDef.char("/", '/'.code), KeyDef.char(":", ':'.code),
        KeyDef.char(";", ';'.code), KeyDef.char("(", '('.code), KeyDef.char(")", ')'.code),
        KeyDef.char("$", '$'.code), KeyDef.char("&", '&'.code), KeyDef.char("@", '@'.code),
        KeyDef.char("\"", '"'.code)
    )

    private val NUM_ROW3 = listOf(
        KeyDef.fn("#+=", KeyDef.CODE_MODE_SYMBOLS, KeyDef.KeyType.MODE_SYMBOLS, 1.5f),
        KeyDef.char(".", '.'.code, width = 0.875f),
        KeyDef.char(",", ','.code, width = 0.875f),
        KeyDef.char("?", '?'.code, width = 0.875f),
        KeyDef.char("!", '!'.code, width = 0.875f),
        KeyDef.char("'", '\''.code, width = 0.875f),
        KeyDef.char("=", '='.code, width = 0.875f),
        KeyDef.char("+", '+'.code, width = 0.875f),
        KeyDef.char("%", '%'.code, width = 0.875f),
        KeyDef.fn("⌫", KeyDef.CODE_BACKSPACE, KeyDef.KeyType.BACKSPACE, 1.5f)
    )

    private val NUM_ROW4 = listOf(
        KeyDef.fn("ABC", KeyDef.CODE_MODE_ALPHA, KeyDef.KeyType.MODE_ALPHA, 1.3f),
        KeyDef.char(",", ','.code, width = 0.9f),
        KeyDef.fn(" ", ' '.code, KeyDef.KeyType.SPACE, 4.2f),
        KeyDef.char(".", '.'.code, width = 0.9f),
        KeyDef.fn("⏎", KeyDef.CODE_ENTER, KeyDef.KeyType.ENTER, 1.7f)
    )

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    const val CODE_NUMBER_ROW = Int.MIN_VALUE + 1 // sentinel handled by KeyboardState

    // Arabic-Indic digit mapping (٠١٢٣٤٥٦٧٨٩) — applied when the user opts in
    // and the Arabic layout is active, so both number row and number pad adapt.
    private val AR_DIGITS = mapOf(
        '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
        '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩'
    )

    private fun arabicDigitKey(k: KeyDef): KeyDef {
        val lbl = k.label
        val mapped = if (lbl.length == 1) AR_DIGITS[lbl[0]] else null
        if (k.type == KeyDef.KeyType.CHAR && mapped != null) {
            return KeyDef(mapped.toString(), mapped.code, k.type,
                k.shiftLabel, k.shiftCode, k.alts, k.altLabels, k.widthUnits, k.labelSize)
        }
        return k
    }

    /** mode: 0=alpha, 1=symbols, 2=numbers; shifted: apply shifted char layer */
    fun rows(lang: String, mode: Int, shifted: Boolean, numberRow: Boolean,
             arabicDigits: Boolean = false, arrowRow: Boolean = false,
             tashkeelRow: Boolean = false): List<List<KeyDef>> {
        val out = ArrayList<List<KeyDef>>(6)
        when (mode) {
            0 -> {
                if (numberRow) out.add(NUM_ROW1)
                if (lang == "ar" && tashkeelRow && !numberRow) out.add(HARAKAT_ROW)
                if (arrowRow) out.add(NAV_ROW)
                if (lang == "ar") {
                    out.add(AR_ROW1); out.add(AR_ROW2); out.add(AR_ROW3); out.add(AR_ROW4)
                } else {
                    out.add(EN_ROW1); out.add(EN_ROW2); out.add(EN_ROW3); out.add(EN_ROW4)
                }
            }
            1 -> {
                if (arrowRow) out.add(NAV_ROW)
                out.add(SYM_ROW1); out.add(SYM_ROW2); out.add(SYM_ROW3); out.add(SYM_ROW4)
            }
            else -> {
                out.add(NUM_ROW1); out.add(NUM_ROW2); out.add(NUM_ROW3); out.add(NUM_ROW4)
            }
        }
        if (arabicDigits && lang == "ar") {
            return out.map { row -> row.map { arabicDigitKey(it) } }
        }
        return out
    }

    /** All letter keys of a language — used for adjacency + swipe decoding. */
    fun letters(lang: String): List<Char> {
        val rowsL = if (lang == "ar") listOf(AR_ROW1, AR_ROW2, AR_ROW3) else listOf(EN_ROW1, EN_ROW2, EN_ROW3)
        val out = ArrayList<Char>(32)
        for (row in rowsL) for (k in row) {
            if (k.type == KeyDef.KeyType.CHAR && k.code > 0 && k.label.length == 1 &&
                k.label[0].isLetter()
            ) out.add(k.label[0])
        }
        return out.distinct()
    }
}
