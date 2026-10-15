/*
 * Copyright (C) 2021-2025 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.ime

enum class ImeUiMode(val value: Int) {
    TEXT(0),
    MEDIA(1),
    CLIPBOARD(2),

    /** technical text tools panel (case, spacing, lines, info). */
    TEXT_TOOLS(3),

    /** the smart harakat panel (لوحة الحركات الذكية). */
    DIACRITICS(4),

    /** the context-aware smart symbols panel (لوحة الرموز الذكية). */
    SMART_SYMBOLS(5),

    /** the extended Arabic letters panel (لوحة الحروف الموسعة). */
    ARABIC_LETTERS(6),

    /** the context-aware smart numbers panel (لوحة الأرقام الذكية). */
    SMART_NUMBER(7),

    /** the deterministic smart clipboard panel (لوحة الحافظة الذكية). */
    SMART_CLIPBOARD(8),

    /** the saved-texts panel (لوحة نصوصي المحفوظة) — the sixth smart panel. */
    MY_TEXTS(9);

    companion object {
        fun fromInt(int: Int) = entries.firstOrNull { it.value == int } ?: TEXT
    }

    fun toInt(): Int = value
}
