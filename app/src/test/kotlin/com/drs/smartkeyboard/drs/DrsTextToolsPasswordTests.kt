/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the fourteenth text-tools round — the security pair: the password
 * strength meter (PASSWORD_STRENGTH, read-only census) and the strong
 * password generator (GENERATE_PASSWORD, cursor insertion). Every
 * expectation below was simulated in Python BEFORE the build (the
 * session doctrine) — check_smart_security.py, 61 green checks — and
 * the engine is a byte-faithful mirror of that simulation. The
 * simulation caught three real design decisions BEFORE the build:
 * a bare @ is a legitimate password symbol (the email shape is an @
 * FOLLOWED BY A DOT — «Test@2026» stays a subject, «user@mail.com»
 * does not), a common-list substring hit caps the verdict at «weak»
 * («مرحبا12345» measures 52 bits yet reads ضعيف), and the NBSP family
 * counts as whitespace (the hex-decoder family, closed).
 */
class DrsTextToolsPasswordTests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // the closed subject predicate
    // -----------------------------------------------------------
    test("the closed subject accepts password shapes only") {
        for (subject in listOf(
            "abc123", "Test@2026", "مرحبا12345", "qwerty1", "123456",
            "aB3!xY9#", "aVeryLongPassword_2026!", "a@mail",
        )) {
            DrsTextTools.isPasswordStrengthSubject(subject) shouldBe true
        }
        for (hostile in listOf(
            "hello world",       // whitespace
            "مرحبا",              // shorter than 6
            "a b12345",          // whitespace inside
            "user@mail.com",     // the email shape (@ followed by a dot)
            "https://x.com/abc", // the scheme
            "",                  // empty
        )) {
            DrsTextTools.isPasswordStrengthSubject(hostile) shouldBe false
        }
    }

    test("NBSP family counts as whitespace — the hex family, closed") {
        DrsTextTools.isPasswordStrengthSubject("abc\u00A0123") shouldBe false
        DrsTextTools.isPasswordStrengthSubject("abc\u2007123") shouldBe false
        DrsTextTools.isPasswordStrengthSubject("abc\u202F123") shouldBe false
    }

    // -----------------------------------------------------------
    // the entropy mathematics — the simulation's exact numbers
    // -----------------------------------------------------------
    test("entropy = length × log2(charset of present classes), floored") {
        fun report(t: String) = DrsTextTools.passwordStrength(t)!!
        // (26+10)=36 → 6·log2(36)=31.02 → 31 — but «abc123» is a
        // common-list hit, so the verdict caps at «weak».
        report("abc123").entropyBits shouldBe 31
        report("abc123").verdict shouldBe "ضعيف"
        report("abc123").classes shouldBe "حروف صغيرة + أرقام"
        report("abc123").notes shouldBe "كلمة شائعة في قوائم التخمين"
        // (26+26+10+32)=94 → 9·log2(94)=58.99 → 58 — متوسط.
        report("Test@2026").entropyBits shouldBe 58
        report("Test@2026").verdict shouldBe "متوسط"
        report("Test@2026").classes shouldBe "حروف صغيرة + حروف كبيرة + أرقام + رموز"
        report("Test@2026").notes shouldBe "يبدو أنه تاريخ"
        // 10 → 6·log2(10)=19.9 → 19 — the band-0 verdict stays.
        report("123456").entropyBits shouldBe 19
        report("123456").verdict shouldBe "ضعيف جدًا"
        report("123456").notes shouldBe "تسلسل لوحة مفاتيح • كلمة شائعة في قوائم التخمين"
        // 94 → 8·log2(94)=52.44 → 52 — clean, no patterns.
        report("aB3!xY9#").entropyBits shouldBe 52
        report("aB3!xY9#").verdict shouldBe "متوسط"
        report("aB3!xY9#").notes shouldBe "لا أنماط مقلقة"
        // (28+10)=38 → 10·log2(38)=52.48 → 52 — yet «12345» is a
        // common-list SUBSTRING: the simulation caught this cap.
        report("مرحبا12345").entropyBits shouldBe 52
        report("مرحبا12345").verdict shouldBe "ضعيف"
        report("مرحبا12345").classes shouldBe "أرقام + حروف عربية"
        // 94 → 16·log2(94)=104.9 → 104 — قوي.
        report("Aa1!Aa1!Aa1!Aa1!").entropyBits shouldBe 104
        report("Aa1!Aa1!Aa1!Aa1!").verdict shouldBe "قوي"
    }

    test("the closed weak patterns fire exactly as the simulation locked") {
        fun notes(t: String) = DrsTextTools.passwordStrength(t)!!.notes
        (notes("qwerty1").contains("تسلسل لوحة مفاتيح")) shouldBe true
        (notes("qwerty1").contains("كلمة شائعة في قوائم التخمين")) shouldBe true
        (notes("aaabbb111").contains("تكرار محارف متتابع")) shouldBe true
        (notes("Michael1985").contains("يبدو أنه تاريخ")) shouldBe true
        // common-list caps: high-entropy hits read «weak».
        DrsTextTools.passwordStrength("password1")!!.verdict shouldBe "ضعيف"
        DrsTextTools.passwordStrength("qwerty123")!!.verdict shouldBe "ضعيف"
        // a clean 16-char mixed run reads «strong» with no warnings.
        DrsTextTools.passwordStrength("Aa1!Aa1!Aa1!Aa1!")!!.notes shouldBe "لا أنماط مقلقة"
    }

    test("the class count drives the advisor's two-class minimum") {
        DrsTextTools.passwordClassCount("abcdef") shouldBe 1
        DrsTextTools.passwordClassCount("abc123") shouldBe 2
        DrsTextTools.passwordClassCount("مرحبا12345") shouldBe 2
        DrsTextTools.passwordClassCount("aB3!xY9#") shouldBe 4
        DrsTextTools.passwordClassCount("٠١٢٣٤٥") shouldBe 1
        DrsTextTools.passwordClassCount("😀😀😀😀😀😀") shouldBe 0
    }

    // -----------------------------------------------------------
    // the non-subject refusal — no invented measurement
    // -----------------------------------------------------------
    test("non-subjects return null byte-identically — apply() never transforms") {
        for (hostile in listOf(
            "", "hello world", "مرحبا", "user@mail.com", "https://x.com",
            "a b12345", "abc",
        )) {
            DrsTextTools.passwordStrength(hostile) shouldBe null
            DrsTextTools.apply(DrsTextTool.PASSWORD_STRENGTH, hostile, en) shouldBe hostile
        }
        // the tool stays TOTAL over its shape: a 6-emoji run IS a
        // subject (zero classes → 0 bits → the honest «ضعيف جدًا»),
        // only the advisor stays silent on it.
        val emoji = DrsTextTools.passwordStrength("😀😀😀😀😀😀")!!
        emoji.entropyBits shouldBe 0
        emoji.verdict shouldBe "ضعيف جدًا"
        emoji.classes shouldBe ""
        // the info pair never transforms through apply() — even subjects.
        DrsTextTools.apply(DrsTextTool.PASSWORD_STRENGTH, "abc123", en) shouldBe "abc123"
        DrsTextTools.apply(DrsTextTool.GENERATE_PASSWORD, "abc123", en) shouldBe "abc123"
    }

    // -----------------------------------------------------------
    // the generator — closed alphabet, guaranteed classes
    // -----------------------------------------------------------
    test("200 samples: length 16, closed 68-alphabet, all four classes, no look-alikes") {
        val lower = "abcdefghijkmnpqrstuvwxyz"
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val digit = "23456789"
        val symbol = "!@#\$%^&*-_=+"
        val alphabet = lower + upper + digit + symbol
        val seen = HashSet<String>()
        repeat(200) {
            val pw = DrsTextTools.generatePassword()
            pw.length shouldBe 16
            for (c in pw) {
                (c in alphabet) shouldBe true
            }
            (pw.any { it in lower }) shouldBe true
            (pw.any { it in upper }) shouldBe true
            (pw.any { it in digit }) shouldBe true
            (pw.any { it in symbol }) shouldBe true
            for (ambiguous in "l1IO0o") {
                (pw.contains(ambiguous)) shouldBe false
            }
            seen.add(pw)
        }
        seen.size shouldBe 200
    }

    // -----------------------------------------------------------
    // the seals of the pair itself
    // -----------------------------------------------------------
    test("PASSWORD_STRENGTH registers at -683 as info-only") {
        val tool = DrsTextTool.PASSWORD_STRENGTH
        tool.code shouldBe -683
        DrsTextTool.fromCode(-683) shouldBe tool
        (tool.code in DrsTextTool.CODE_RANGE) shouldBe true
        (tool.isInfoOnly) shouldBe true
        (tool.isEditorOp) shouldBe false
        (tool.isInsertMark) shouldBe false
    }

    test("GENERATE_PASSWORD registers at -684 as a plain tool") {
        val tool = DrsTextTool.GENERATE_PASSWORD
        tool.code shouldBe -684
        DrsTextTool.fromCode(-684) shouldBe tool
        (tool.code in DrsTextTool.CODE_RANGE) shouldBe true
        (tool.isInfoOnly) shouldBe false
        (tool.isEditorOp) shouldBe false
        (tool.isInsertMark) shouldBe false
    }
})
