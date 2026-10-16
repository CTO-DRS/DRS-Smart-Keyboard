/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.ime.keyboard

import com.drs.smartkeyboard.drs.ai.DrsArabicLetters
import com.drs.smartkeyboard.ime.popup.PopupSet
import com.drs.smartkeyboard.ime.text.keyboard.TextKeyData
import com.drs.smartkeyboard.ime.text.key.KeyType

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 3): the popup gesture board —
 * the eight canonical harakat stations (U+064B..U+0652) for Arabic
 * letters, surfaced through the EXISTING long-press popup engine.
 *
 * PURITY: 100% pure JVM — a letter label in, a popup set out, null for
 * everything the gate refuses. No Android, no layout assets, no
 * thresholds: the long-press threshold IS the existing
 * `keyboard__long_press_delay` (AppPrefs, default 300ms, with its
 * existing UI setting) and the display/drag/cancel/release mechanics
 * ARE the existing `PopupUiController` path — zero new code for any of
 * it (TextKeyboardLayout stays byte-identical to v1.0.0, pinned by a
 * structural test).
 *
 * THE GATE (DrsArabicLetters, exact): a board is produced ONLY for the
 * 28 letters of SUN ∪ MOON. Latin letters, digits, symbols, marks, and
 * everything else — null, no board. Latin is excluded on purpose (the
 * «what this chapter will NOT do» list).
 *
 * THE CANONICAL STATIONS (owner decision 3): tanwin fath/damm/kasr,
 * fatha, damma, kasra, SHADDA (the MAIN station), sukun — in Unicode
 * order U+064B..U+0652. Releasing on the letter itself inserts the bare
 * letter (the popup engine's standard behavior); the shadda is the
 * board's main station. Tanwin stays where it already lives (the
 * symbols page is untouched).
 */
object DrsHarakatPanel {

    /** The eight canonical stations in Unicode order — shadda included (the main). */
    val STATIONS: List<Char> = listOf(
        '\u064B', // tanwin fath ً
        '\u064C', // tanwin damm ٌ
        '\u064D', // tanwin kasr ٍ
        '\u064E', // fatha َ
        '\u064F', // damma ُ
        '\u0650', // kasra ِ
        '\u0651', // shadda ّ — the MAIN station (owner decision 3)
        '\u0652', // sukun ْ
    )

    const val SHADDA: Char = '\u0651'

    /** The board gate: EXACTLY the DrsArabicLetters SUN ∪ MOON letters. */
    fun isGatedLetter(c: Char): Boolean = c in DrsArabicLetters.SUN || c in DrsArabicLetters.MOON

    /**
     * The popup set for one letter key: main = shadda, relevant = the
     * seven remaining stations in canonical order. Null for anything the
     * gate refuses — the caller merges it into the existing popup
     * machinery (which renders, drags, cancels, and commits exactly as
     * it does today for layout-defined popups).
     */
    fun harakatPopupFor(label: String): PopupSet<AbstractKeyData>? {
        if (label.length != 1) return null
        val letter = label[0]
        if (!isGatedLetter(letter)) return null
        val main = station(SHADDA)
        val relevant = STATIONS.filter { it != SHADDA }.map { station(it) }
        return PopupSet<AbstractKeyData>(main = main, relevant = relevant)
    }

    private fun station(c: Char): TextKeyData = TextKeyData(
        type = KeyType.CHARACTER,
        code = c.code,
        label = c.toString(),
    )
}
