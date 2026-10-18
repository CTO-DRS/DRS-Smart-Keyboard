/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the thirteenth text-tools round — the byte-dump pair: the hex
 * encoder (HEX_ENCODE, the total UTF-8 byte dump) and the hex decoder
 * (HEX_DECODE, the two-shape honest mirror). Every expectation below
 * was simulated in Python BEFORE the build (the session doctrine) —
 * check_hex.py, 92 green checks — and the engine is a byte-faithful
 * mirror of that simulation. The simulation caught one real design
 * decision BEFORE the build: the bare-digit run («1250», «4869») is
 * the ONE closed ambiguity class — the advisor stays silent on it
 * (a number first, a hex dump second) while the tool itself remains
 * total over its shape.
 */
class DrsTextToolsHexTests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // HEX_ENCODE — the total byte dump
    // -----------------------------------------------------------

    test("encode speaks lowercase spaced pairs") {
        fun enc(s: String) = DrsTextTools.apply(DrsTextTool.HEX_ENCODE, s, en)
        enc("Hi") shouldBe "48 69"
        enc("A") shouldBe "41"
        enc(" ") shouldBe "20"
        enc("م") shouldBe "d9 85"
        enc("مرحبا") shouldBe "d9 85 d8 b1 d8 ad d8 a8 d8 a7"
        enc("\uD83D\uDE00") shouldBe "f0 9f 98 80"
        enc("a\nb") shouldBe "61 0a 62"
        enc("\uFEFF") shouldBe "ef bb bf"
    }

    test("encode is total — it re-encodes already-hex text") {
        // unlike the decoders, the encoder has no refusal shape:
        // every non-empty text is its own bytes.
        DrsTextTools.apply(DrsTextTool.HEX_ENCODE, "48 69", en) shouldBe "34 38 20 36 39"
    }

    test("encode carries no uppercase and no separator but the space") {
        val dump = DrsTextTools.apply(DrsTextTool.HEX_ENCODE, "مرحبا Hi \uD83D\uDE00 \u0000", en)
        dump.all { it in "0123456789abcdef " } shouldBe true
    }

    // -----------------------------------------------------------
    // HEX_DECODE — the two-shape honest mirror
    // -----------------------------------------------------------

    test("decode speaks the spaced token shape in any case") {
        fun dec(s: String) = DrsTextTools.apply(DrsTextTool.HEX_DECODE, s, en)
        dec("48 69") shouldBe "Hi"
        dec("48 6C 6C 6F") shouldBe "Hllo"
        dec("d9 85") shouldBe "م"
        dec("d9 85 d8 b1 d8 ad d8 a8 d8 a7") shouldBe "مرحبا"
        dec("f0 9f 98 80") shouldBe "\uD83D\uDE00"
        dec("61 0a 62") shouldBe "a\nb"
        dec("00") shouldBe "\u0000"
        dec("ef bb bf") shouldBe "\uFEFF"
    }

    test("decode speaks the solid shape") {
        fun dec(s: String) = DrsTextTools.apply(DrsTextTool.HEX_DECODE, s, en)
        dec("4869") shouldBe "Hi"
        dec("48656c6c6f") shouldBe "Hello"
        dec("48656C6C6F") shouldBe "Hello"
        dec("d985d8b1d8add8a8d8a7") shouldBe "مرحبا"
        dec("48656c6c6f21") shouldBe "Hello!"
    }

    test("decode accepts the 0x/0X prefix per token and on the solid shape") {
        fun dec(s: String) = DrsTextTools.apply(DrsTextTool.HEX_DECODE, s, en)
        dec("0x48 0x69") shouldBe "Hi"
        dec("0X48 0X69") shouldBe "Hi"
        dec("0x48 69") shouldBe "Hi"
        dec("0x4869") shouldBe "Hi"
        dec("0X4869") shouldBe "Hi"
    }

    test("decode folds the whole whitespace family") {
        fun dec(s: String) = DrsTextTools.apply(DrsTextTool.HEX_DECODE, s, en)
        dec("  48   69  ") shouldBe "Hi"
        dec("48\u00A069") shouldBe "Hi"
        dec("48\t\n69") shouldBe "Hi"
    }

    test("decode(encode(x)) == x over hostile and odd texts") {
        val samples = listOf(
            "Hello, world!",
            "مرحبا بالعالم",
            "\uD83C\uDF0E globe",
            "a\nb\r\nc",
            "\u0000\u0001 control",
            "line1\nline2",
            "-tab\ttab-",
            "«اقتباس»",
            "mixed عربي 123",
            "\uFEFF BOM start",
            "x".repeat(500),
        )
        for (s in samples) {
            val dump = DrsTextTools.apply(DrsTextTool.HEX_ENCODE, s, en)
            DrsTextTools.apply(DrsTextTool.HEX_DECODE, dump, en) shouldBe s
        }
    }

    test("anything the closed shapes do not name refuses byte-identically") {
        for (hostile in listOf(
            "486",        // odd solid length
            "48 656",     // token not exactly two digits
            "48 656c",    // mixed token lengths
            "4865 6c6c",  // two solid tokens — neither shape
            "48:65:6c",   // colon separator — out of the closed catalog
            "%48",        // percent — URL_DECODE's territory
            "beef",       // letters-only bytes 0xBE 0xEF — invalid UTF-8
            "face",       // 0xFA 0xCE — invalid UTF-8
            "decade",     // 0xDE 0xCA 0xDE — invalid UTF-8
            "hello",      // 'h' not hex, five letters not two
            "مرحبا",      // Arabic — not the hex alphabet
            "٤٨",         // Eastern digits — the machine notation is Latin
            "0x",         // prefix alone, no digits
            "0xzz",       // prefix + non-hex
            "0x48 0x",    // second prefix empty
            "gg",         // not hex
            "48 69 x",    // trailing non-hex token
            "ff",         // lone 0xFF — invalid UTF-8
            "c3",         // lone lead byte — invalid UTF-8
            "e2 82",      // truncated three-byte sequence — invalid UTF-8
        )) {
            DrsTextTools.apply(DrsTextTool.HEX_DECODE, hostile, en) shouldBe hostile
        }
    }

    test("the bare digit run stays total on the tool side") {
        // the ONE closed ambiguity class is the ADVISOR's silence, not
        // a tool refusal: the shape IS a valid dump and the tool speaks.
        fun dec(s: String) = DrsTextTools.apply(DrsTextTool.HEX_DECODE, s, en)
        dec("4869") shouldBe "Hi"
        // a whitespace-only field is untouched — nothing to decode.
        dec("   ") shouldBe "   "
    }

    test("decode is honest on a valid-utf8 letters-only edge") {
        // 0xCD 0xAF is a valid two-byte sequence (U+032F) — the tool
        // speaks it; the ADVISOR's guards are the conservative layer.
        val out = DrsTextTools.apply(DrsTextTool.HEX_DECODE, "cdaf", en)
        (out != "cdaf").shouldBe(true)
    }

    // -----------------------------------------------------------
    // the byte-dump pair's codes and dispatch
    // -----------------------------------------------------------

    test("codes sit inside the dispatch range and resolve back") {
        DrsTextTool.HEX_ENCODE.code shouldBe -681
        DrsTextTool.HEX_DECODE.code shouldBe -682
        DrsTextTool.fromCode(-681) shouldBe DrsTextTool.HEX_ENCODE
        DrsTextTool.fromCode(-682) shouldBe DrsTextTool.HEX_DECODE
        (DrsTextTool.HEX_ENCODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
        (DrsTextTool.HEX_DECODE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
    }

    test("the pair is honest on its own output") {
        // decoding the canonical dump round-trips; decoding the decoded
        // text refuses byte-identically (prose is not the shape).
        val dump = DrsTextTools.apply(DrsTextTool.HEX_ENCODE, "مرحبا", en)
        dump shouldBe "d9 85 d8 b1 d8 ad d8 a8 d8 a7"
        DrsTextTools.apply(DrsTextTool.HEX_DECODE, dump, en) shouldBe "مرحبا"
        DrsTextTools.apply(DrsTextTool.HEX_DECODE, "مرحبا", en) shouldBe "مرحبا"
    }
})
