package com.drs.keyboard.keyboard

/**
 * Key model. Character keys carry a positive unicode codepoint; functional
 * keys use the negative codes below so they can never collide with text.
 */
class KeyDef(
    val label: String,
    val code: Int,
    val type: KeyType = KeyType.CHAR,
    val shiftLabel: String? = null,   // shifted variant of a char key
    val shiftCode: Int = code,
    val alts: List<Int> = emptyList(), // long-press popup (codepoints)
    val altLabels: List<String> = emptyList(),
    val widthUnits: Float = 1f,
    val labelSize: Float = 0f         // 0 = default
) {
    enum class KeyType { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, MODE_SYMBOLS, MODE_ALPHA, MODE_NUM, LANG, EMOJI, ONEHAND, NAV }

    val isFunctional: Boolean get() = type != KeyType.CHAR

    companion object {
        // --- functional codes (negative => never a unicode char) ---
        const val CODE_SHIFT = -1
        const val CODE_MODE_SYMBOLS = -2
        const val CODE_MODE_ALPHA = -3
        const val CODE_MODE_NUM = -4
        const val CODE_ENTER = -5
        const val CODE_BACKSPACE = -6
        const val CODE_LANG = -7
        const val CODE_EMOJI = -8
        const val CODE_ONEHAND = -9
        const val CODE_NAV_UP = -10
        const val CODE_NAV_DOWN = -11
        const val CODE_NAV_LEFT = -12
        const val CODE_NAV_RIGHT = -13

        // --- helpers -----------------------------------------------------
        fun char(label: String, code: Int, shiftLabel: String? = null, shiftCode: Int = code,
                 alts: List<Int> = emptyList(), altLabels: List<String> = emptyList(),
                 width: Float = 1f, labelSize: Float = 0f) =
            KeyDef(label, code, KeyType.CHAR, shiftLabel, shiftCode, alts, altLabels, width, labelSize)

        fun fn(label: String, code: Int, type: KeyType, width: Float) =
            KeyDef(label, code, type, null, code, emptyList(), emptyList(), width, 0f)
    }
}
