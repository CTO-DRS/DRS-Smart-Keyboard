/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the tenth text-tools round — the payload pair: the Base64 encoder
 * (BASE64_ENCODE) and its honest decoder mirror (BASE64_DECODE).
 * Every expectation below was simulated in Python BEFORE the build
 * (the session doctrine) — check_base64.py — and the two engines are
 * byte-faithful mirrors of that simulation.
 */
class DrsTextToolsBase64Tests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // BASE64_ENCODE — the encoder (total, deterministic)
    // -----------------------------------------------------------

    test("latin text encodes over the standard padded alphabet") {
        DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, "Hello", en) shouldBe "SGVsbG8="
        DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, "Hello, world!", en) shouldBe
            "SGVsbG8sIHdvcmxkIQ=="
    }

    test("arabic text encodes as its exact UTF-8 bytes") {
        DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, "مرحبا", en) shouldBe "2YXYsdit2KjYpw=="
    }

    test("a JWT header speaks the classic eyJ shape") {
        DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, "{\"alg\":\"HS256\"}", en) shouldBe
            "eyJhbGciOiJIUzI1NiJ9"
    }

    test("emoji, newlines and tabs encode through their bytes") {
        val sample = "🌍 نص عربي\nسطر ثانٍ"
        val encoded = DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, sample, en)
        // the mirror restores the exact text — the encoder is total.
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, encoded, en) shouldBe sample
        // one line, no wrapping: the channel's business, not the value's.
        encoded.none { it == '\n' || it == '\r' }.shouldBe(true)
    }

    // -----------------------------------------------------------
    // BASE64_DECODE — the honest decoder mirror
    // -----------------------------------------------------------

    test("standard padded tokens decode to their exact text") {
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "SGVsbG8=", en) shouldBe "Hello"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "2YXYsdit2KjYpw==", en) shouldBe "مرحبا"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "SGVsbG8sIHdvcmxkIQ==", en) shouldBe
            "Hello, world!"
    }

    test("a JWT header decodes to its JSON") {
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "eyJhbGciOiJIUzI1NiJ9", en) shouldBe
            "{\"alg\":\"HS256\"}"
    }

    test("wrapped tokens rejoin — newlines, tabs and the NBSP family") {
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "SGVs\nbG8=", en) shouldBe "Hello"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "SGVs\tbG8=", en) shouldBe "Hello"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "SGVs\u00A0bG8=", en) shouldBe "Hello"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "  SGVsbG8=  ", en) shouldBe "Hello"
    }

    test("the URL-safe alphabet maps onto the standard one") {
        // '???' encodes to 'Pz8/' (standard) — its URL-safe form decodes too.
        DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, "???", en) shouldBe "Pz8/"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "Pz8_", en) shouldBe "???"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "Pz8/", en) shouldBe "???"
        // a well-formed single-pad token of two bytes decodes honestly.
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "Pz8=", en) shouldBe "??"
    }

    test("non-UTF-8 payloads refuse — binary is not typing") {
        // 'FF FE' bytes are never valid UTF-8 — the token returns untouched.
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "//4=", en) shouldBe "//4="
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "____", en) shouldBe "____"
    }

    test("honest words of the right length still refuse") {
        // 'helloworld' has no padding — not the shape.
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "hello world", en) shouldBe "hello world"
        // 'Testtest' is the shape but decodes to invalid UTF-8 — refuses.
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "Testtest", en) shouldBe "Testtest"
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "مرحبا بالعالم", en) shouldBe
            "مرحبا بالعالم"
    }

    test("malformed lengths and padding return byte-identical") {
        for (bad in listOf(
            "abc", "ab", "a", "SGVsbG8", "====", "=abc",
            "AB=CD", "a===", "A", "abcdefghi", "Pz8", "Pz8==",
        )) {
            DrsTextTools.apply(DrsTextTool.BASE64_DECODE, bad, en) shouldBe bad
        }
    }

    test("a whitespace-only field is untouched — nothing to decode") {
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, "   ", en) shouldBe "   "
    }

    // -----------------------------------------------------------
    // the mirror contract — decode∘encode = identity
    // -----------------------------------------------------------

    test("the pair round-trips across scripts, emoji and control bytes") {
        for (sample in listOf(
            "Hello",
            "مرحبا بالعالم!",
            "🌍🚀 emoji",
            "سطر\nجديد\tوباس",
            "{\"sub\":\"user\",\"admin\":false}",
            "12345",
        )) {
            val encoded = DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, sample, en)
            DrsTextTools.apply(DrsTextTool.BASE64_DECODE, encoded, en) shouldBe sample
        }
    }

    test("the hostile input round-trips through the pair") {
        val hostile = "\uD83C\uDF0E \u0000 \t\n\r$ { } %s %d ٍَّ most"
        val encoded = DrsTextTools.apply(DrsTextTool.BASE64_ENCODE, hostile, en)
        DrsTextTools.apply(DrsTextTool.BASE64_DECODE, encoded, en) shouldBe hostile
    }

    test("codes sit inside the dispatch range and resolve back") {
        DrsTextTool.BASE64_ENCODE.code shouldBe -640
        DrsTextTool.BASE64_DECODE.code shouldBe -676
        DrsTextTool.fromCode(-640) shouldBe DrsTextTool.BASE64_ENCODE
        DrsTextTool.fromCode(-676) shouldBe DrsTextTool.BASE64_DECODE
        (DrsTextTool.BASE64_ENCODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
        (DrsTextTool.BASE64_DECODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
    }
})
