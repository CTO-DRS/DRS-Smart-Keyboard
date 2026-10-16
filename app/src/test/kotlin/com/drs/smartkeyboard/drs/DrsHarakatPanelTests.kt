/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsArabicLetters
import com.drs.smartkeyboard.ime.keyboard.ComputingEvaluator
import com.drs.smartkeyboard.ime.keyboard.DefaultComputingEvaluator
import com.drs.smartkeyboard.ime.keyboard.DrsHarakatPanel
import com.drs.smartkeyboard.ime.popup.MutablePopupSet
import com.drs.smartkeyboard.ime.text.key.KeyHintConfiguration
import com.drs.smartkeyboard.ime.text.keyboard.TextKeyData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.security.MessageDigest

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 3): the popup gesture board
 * tests. Built on the era baseline — these add on top.
 *
 * Layers proven here:
 *  - the board: a gated Arabic letter yields the EIGHT canonical
 *    stations (U+064B..U+0652) with the SHADDA as the main station
 *    (owner decision 3) and the seven remaining in canonical order;
 *  - the gate: EXACTLY DrsArabicLetters SUN ∪ MOON — Latin, digits,
 *    symbols, marks, hamza variants, multi-char labels → null (Latin
 *    excluded on purpose);
 *  - the merge semantics: through the real popup engine (the default
 *    evaluator) the shadda lands in `main` when the key's popup set is
 *    empty — the exact runtime path of the one declared merge point;
 *  - the threshold contract: the existing `keyboard__long_press_delay`
 *    (default 300) is the only threshold — no new setting exists in
 *    the source tree;
 *  - the structural isolation (the owner-mandated hard proof):
 *    TextKeyboardLayout.kt is BYTE-IDENTICAL to v1.0.0 (pinned hash),
 *    TextKey.kt carries exactly the import + the one merge call, the
 *    panel is referenced nowhere else in ime/, and the panel itself
 *    imports no learning/correction engine (prediction/correction
 *    isolation) and no network primitive.
 */
class DrsHarakatPanelTests : FunSpec({

    // ------------------------------------------------------ the board

    context("the eight canonical stations") {
        test("a gated letter yields 8 stations: main = shadda, relevant = the other 7 in order") {
            val popup = DrsHarakatPanel.harakatPopupFor("ض")!!
            val main = popup.main as? TextKeyData ?: throw AssertionError("expected a main station")
            main.code shouldBe 0x0651
            main.label shouldBe "ّ"
            popup.relevant.map { (it as TextKeyData).code } shouldContainExactly listOf(
                0x064B, 0x064C, 0x064D, 0x064E, 0x064F, 0x0650, 0x0652,
            )
            popup.relevant.map { (it as TextKeyData).label } shouldContainExactly listOf(
                "ً", "ٌ", "ٍ", "َ", "ُ", "ِ", "ْ",
            )
        }

        test("every station key is a plain CHARACTER with no nested popup and the exact code") {
            val popup = DrsHarakatPanel.harakatPopupFor("ف")!!
            val all = listOfNotNull(popup.main).map { it as TextKeyData } + popup.relevant.map { it as TextKeyData }
            all shouldHaveSize 8
            // main (shadda) first, then the seven stations in canonical order —
            // the full set is exactly the canonical range U+064B..U+0652
            all.map { it.code }.sorted() shouldContainExactly (0x064B..0x0652).toList()
            (all.first().code) shouldBe 0x0651
            for (data in all) {
                data.popup shouldBe null
            }
        }

        test("STATIONS is the exact canonical range U+064B..U+0652") {
            DrsHarakatPanel.STATIONS.map { it.code } shouldContainExactly (0x064B..0x0652).toList()
            DrsHarakatPanel.SHADDA shouldBe 'ّ'
        }
    }

    // -------------------------------------------------------- the gate

    context("the gate — DrsArabicLetters SUN/MOON, exact") {
        test("all 28 letters of SUN ∪ MOON get a board") {
            val letters = (DrsArabicLetters.SUN + DrsArabicLetters.MOON).sorted()
            letters shouldHaveSize 28
            for (letter in letters) {
                val popup = DrsHarakatPanel.harakatPopupFor(letter.toString())
                    ?: throw AssertionError("expected a board for $letter")
                popup.main as? TextKeyData ?: throw AssertionError("expected a main station for $letter")
            }
        }

        test("Latin letters get NO board (excluded on purpose)") {
            for (c in "abcdefghijklmnopqrstuvwxyz") {
                DrsHarakatPanel.harakatPopupFor(c.toString()).shouldBeNull()
                DrsHarakatPanel.harakatPopupFor(c.uppercase()).shouldBeNull()
            }
            DrsHarakatPanel.isGatedLetter('a').shouldBeFalse()
        }

        test("digits, symbols, marks, and non-SUN/MOON Arabic chars get NO board") {
            for (label in listOf("5", "@", "ّ", "ً", "ء", "أ", "إ", "آ", "ة", "ى", "ـ")) {
                DrsHarakatPanel.harakatPopupFor(label).shouldBeNull()
            }
            DrsHarakatPanel.isGatedLetter('ء').shouldBeFalse()
        }

        test("multi-char and empty labels get NO board") {
            DrsHarakatPanel.harakatPopupFor("ضض").shouldBeNull()
            DrsHarakatPanel.harakatPopupFor("SHIFT").shouldBeNull()
            DrsHarakatPanel.harakatPopupFor("").shouldBeNull()
        }
    }

    // ---------------------------------------------- the merge semantics

    context("the merge semantics through the real popup engine") {
        val evaluator: ComputingEvaluator = DefaultComputingEvaluator

        test("on an empty popup set the shadda lands as MAIN (the runtime path of the merge point)") {
            val popups = MutablePopupSet<TextKeyData>()
            DrsHarakatPanel.harakatPopupFor("س")?.let { popups.merge(it, evaluator) }
            popups.main?.code shouldBe 0x0651
            popups.relevant.map { it.code } shouldContainExactly listOf(
                0x064B, 0x064C, 0x064D, 0x064E, 0x064F, 0x0650, 0x0652,
            )
            // the no-hint render contract: main first (prioritized), then the stations
            val rendered = popups.getPopupKeys(KeyHintConfiguration.HINTS_DISABLED)
            rendered.prioritized.firstOrNull()?.code shouldBe 0x0651
            rendered.other shouldHaveSize 7
        }

        test("a null gate result is a no-op merge — the key keeps its (empty) popups") {
            val popups = MutablePopupSet<TextKeyData>()
            DrsHarakatPanel.harakatPopupFor("x")?.let { popups.merge(it, evaluator) }
            popups.main.shouldBeNull()
            popups.relevant shouldHaveSize 0
        }
    }

    // -------------------------------------------- the threshold contract

    context("the threshold contract — no new setting") {
        test("the ONLY threshold is the existing keyboard__long_press_delay (default 300)") {
            val prefs = File(resolveRepoRoot(), "app/src/main/kotlin/com/drs/smartkeyboard/app/AppPrefs.kt")
                .readText()
            prefs.shouldContain("long_press_delay")
            prefs.shouldContain("300")
        }

        test("no new threshold setting exists — press_delay keys are exactly the existing one") {
            val prefs = File(resolveRepoRoot(), "app/src/main/kotlin/com/drs/smartkeyboard/app/AppPrefs.kt")
                .readText()
            val keys = Regex("key = \"([^\"]+)\"").findAll(prefs).map { it.groupValues[1] }.toList()
            keys.filter { it.contains("press_delay") } shouldContainExactly listOf("keyboard__long_press_delay")
            keys.none { it.contains("gesture_board") } shouldBe true
            keys.none { it.contains("harakat_threshold") } shouldBe true
        }
    }

    // --------------------------------- structural isolation (feature 3)

    context("structural isolation — feature 3 (the hard proofs)") {
        val root = resolveRepoRoot()
        val main = File(root, "app/src/main/kotlin/com/drs/smartkeyboard")

        test("TextKeyboardLayout.kt is BYTE-IDENTICAL to v1.0.0 (pinned SHA-256)") {
            val file = File(main, "ime/text/keyboard/TextKeyboardLayout.kt")
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex shouldBe "1e5807cc6a325b717b6707555695c3ad48a792462d98abef564a7921e3d59eac"
        }

        test("TextKey.kt carries EXACTLY the import + the one merge call — nothing else") {
            val src = File(main, "ime/text/keyboard/TextKey.kt").readText()
            Regex("DrsHarakatPanel").findAll(src).count() shouldBe 2
            src.shouldContain("DrsHarakatPanel.harakatPopupFor")
            src.shouldContain("ADR-0009")
        }

        test("the panel is referenced NOWHERE else in ime/") {
            val offenders = File(main, "ime").walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.name != "TextKey.kt" && it.name != "DrsHarakatPanel.kt" }
                .filter { it.readText().contains("DrsHarakatPanel") }
                .map { it.name }
                .toList()
            offenders shouldHaveSize 0
        }

        test("the panel is pure: no network, no learning/correction engine, no layout assets") {
            val src = File(main, "ime/keyboard/DrsHarakatPanel.kt").readText()
            src.shouldNotContain("java.net")
            src.shouldNotContain("okhttp")
            src.shouldNotContain("TrafficStats")
            src.shouldNotContain("DrsLearningEngine")
            src.shouldNotContain("DrsCorrectionRevert")
            src.shouldNotContain("DrsCorrectionCounters")
            src.shouldNotContain("InputStream")
        }
    }
}) {
    companion object {
        /** Walks up from the working directory to the repo root (AGP's unit-test cwd varies). */
        internal fun resolveRepoRoot(): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            repeat(6) {
                if (File(dir, "app/src/main/AndroidManifest.xml").exists()) return dir!!
                dir = dir?.parentFile
            }
            error("repo root not found from ${System.getProperty("user.dir")}")
        }
    }
}
