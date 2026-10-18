/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the eleventh text-tools round — the telegraph pair: the Morse
 * encoder (MORSE_ENCODE) over the OFFICIAL ITU-R M.1677-1 table and
 * its honest decoder mirror (MORSE_DECODE). Every expectation below
 * was simulated in Python BEFORE the build (the session doctrine) —
 * check_morse.py, 74 green checks — and the two engines are
 * byte-faithful mirrors of that simulation. The simulation caught
 * real bugs BEFORE the build: the missing glyph normalization in the
 * decoder, the '/' walking through the punctuation table, and the
 * INTERNATIONAL collision −··− (sad ص vs the slash sign).
 */
class DrsTextToolsMorseTests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // MORSE_ENCODE — the official ITU Arabic signs
    // -----------------------------------------------------------

    test("arabic text encodes over the historic ITU signs") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "مرحبا", en) shouldBe
            "−− ·−· ···· −··· ·−"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "سلم عليكم", en) shouldBe
            "··· ·−·· −− / ·−·− ·−·· ·· −·− −−"
    }

    test("latin text encodes over the international signs, case-insensitive") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "SOS", en) shouldBe "··· −−− ···"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "sos", en) shouldBe "··· −−− ···"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "hello", en) shouldBe
            "···· · ·−·· ·−·· −−−"
    }

    test("digits speak the five-glyph ladder in both scripts") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "123", en) shouldBe
            "·−−−− ··−−− ···−−"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "٤٥", en) shouldBe "····− ·····"
    }

    test("punctuation speaks international signs and arabic marks fold") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "12.5؟", en) shouldBe
            "·−−−− ··−−− ·−·−·− ····· ··−−··"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "سلام!", en) shouldBe
            "··· ·−·· ·− −− −·−·−−"
    }

    test("hamza carriers fold — the slugify doctrine") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "سؤال", en) shouldBe "··· ·−− ·− ·−··"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "ةىأئ", en) shouldBe "··−·· ·· ·− ··"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "أَهْلاً بِكُم", en) shouldBe
            "·− ··−·· ·−·· ·− / −··· −·− −−"
    }

    test("the standalone hamza owns its own sign — جزء round-trips EXACT") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "جزء", en) shouldBe "·−−− −−−· ·"
        DrsTextTools.apply(
            DrsTextTool.MORSE_DECODE,
            DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "جزء", en),
            en,
        ) shouldBe "جزء"
    }

    test("whitespace runs are word gaps and one line is the channel shape") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "مرحبا\nيا", en) shouldBe
            "−− ·−· ···· −··· ·− / ·· ·−"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "سلم  عليكم", en) shouldBe
            "··· ·−·· −− / ·−·− ·−·· ·· −·− −−"
    }

    test("anything without a known sign refuses the whole text") {
        for (hostile in listOf(
            "a/b",          // the channel's own word mark
            "مرحبا🌍",       // emoji
            "سلامد友谊",     // cjk
            "ﻻ",            // presentation form (normalize first)
            "ψ",            // greek
            "«سلم»",        // guillemets carry no sign
        )) {
            DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, hostile, en) shouldBe hostile
        }
    }

    test("marks-only text is untouched — nothing codable, nothing lost") {
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "َـ", en) shouldBe "َـ"
    }

    // -----------------------------------------------------------
    // MORSE_DECODE — the honest mirror
    // -----------------------------------------------------------

    test("itu signs decode to their arabic letters") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "−− ·−· ···· −··· ·−", en) shouldBe
            "مرحبا"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "··· ·−·· −− / ·−·− ·−·· ·· −·− −−", en) shouldBe
            "سلم عليكم"
    }

    test("the channel is scriptless — latin homographs return arabic") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "··· −−− ···", en) shouldBe "سخس"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "···· · ·−·· ·−·· −−−", en) shouldBe
            "حءللخ"
    }

    test("digits and punctuation decode as themselves") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "·−−−− ··−−− ···−−", en) shouldBe "123"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "····− ·····", en) shouldBe "45"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "−−···", en) shouldBe "7"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "···−−", en) shouldBe "3"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "−·−·−−", en) shouldBe "!"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "·−−·−·", en) shouldBe "@"
    }

    test("any dot/dash glyph family normalizes onto the canonical pair") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, ".-", en) shouldBe "ا"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "·—", en) shouldBe "ا"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "··–··", en) shouldBe "ه"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "·.−−..", en) shouldBe "?"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, ".·−.", en) shouldBe "ف"
    }

    test("the documented itu quirk: the slash sign IS the sad") {
        // the international slash sign −··− is identical to ص in the
        // ITU tables themselves — the arabic winner owns it; the
        // encoder refuses '/' outright so the mirror stays coherent.
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "−··−", en) shouldBe "ص"
        DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, "ص/x", en) shouldBe "ص/x"
    }

    test("whitespace tolerance — edges trim, the nbsp family folds") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "  ·−  /  −·  ", en) shouldBe "ا ن"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "·−\u00A0/\u2007−·", en) shouldBe "ا ن"
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "   ", en) shouldBe "   "
    }

    test("broken shapes refuse byte-identically — decode never destroys") {
        for (bad in listOf(
            "hello",              // letters are not glyphs
            "مرحبا",              // arabic prose is not morse
            "−·x",                // a foreign glyph inside a sign
            "··/",                // trailing slash — empty word
            "/··",                // leading slash — empty word
            "·· // −−",           // doubled slash — empty middle word
            "······",             // six dots — no such sign
            "eyJhbGciOiJIUzI1NiJ9", // the jwt header is not morse
            "·−·−·−·",            // an alternator no table names
        )) {
            DrsTextTools.apply(DrsTextTool.MORSE_DECODE, bad, en) shouldBe bad
        }
    }

    test("an em-dash separator line is one long sign, not a rewrite") {
        DrsTextTools.apply(DrsTextTool.MORSE_DECODE, "———", en) shouldBe "خ"
    }

    // -----------------------------------------------------------
    // the mirror contract — decode∘encode = the folded identity
    // -----------------------------------------------------------

    test("the pair round-trips across the closed folded alphabet") {
        // decode∘encode = the FOLDED identity: diacritics/tatweel strip,
        // hamza carriers fold and Arabic punctuation westernizes — the
        // exact expected values simulated in check_morse.py.
        for (sample in listOf(
            "مرحبا بالعالم" to "مرحبا بالعالم",
            "سلم عليكم" to "سلم عليكم",
            "جزء" to "جزء",
            "12.5؟" to "12.5?",
            "سلام!" to "سلام!",
            "٤٥" to "45",
            "أَهْلاً بِكُم" to "اهلا بكم",
            "سَلْـمُ" to "سلم",
        )) {
            val encoded = DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, sample.first, en)
            DrsTextTools.apply(DrsTextTool.MORSE_DECODE, encoded, en) shouldBe sample.second
        }
    }

    test("latin input round-trips as its documented arabic homographs") {
        for (sample in listOf("hello", "SOS", "x y z")) {
            val encoded = DrsTextTools.apply(DrsTextTool.MORSE_ENCODE, sample, en)
            val decoded = DrsTextTools.apply(DrsTextTool.MORSE_DECODE, encoded, en)
            decoded shouldBe when (sample) {
                "hello" -> "حءللخ"
                "SOS" -> "سخس"
                else -> "ص ظ ذ"
            }
        }
    }

    test("codes sit inside the dispatch range and resolve back") {
        DrsTextTool.MORSE_ENCODE.code shouldBe -677
        DrsTextTool.MORSE_DECODE.code shouldBe -678
        DrsTextTool.fromCode(-677) shouldBe DrsTextTool.MORSE_ENCODE
        DrsTextTool.fromCode(-678) shouldBe DrsTextTool.MORSE_DECODE
        (DrsTextTool.MORSE_ENCODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
        (DrsTextTool.MORSE_DECODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
    }
})
