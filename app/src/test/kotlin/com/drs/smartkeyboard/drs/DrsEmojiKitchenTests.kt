package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * عقود مطبخ الإيموجي الخالص — the closed-catalog, determinism, honest
 * silence and search contracts of [DrsEmojiKitchen], mirroring the
 * 566-check python simulation (scripts/check_emoji_kitchen.py).
 */
class DrsEmojiKitchenTests : FunSpec({

    test("the catalogs are closed: 8 categories, 48 participants, 6 per category") {
        DrsEmojiKitchen.DrsKitchenCategory.entries.shouldHaveSize(8)
        DrsEmojiKitchen.PARTICIPANTS.shouldHaveSize(48)
        DrsEmojiKitchen.PARTICIPANTS.map { it.value }.toSet().shouldHaveSize(48)
        for (category in DrsEmojiKitchen.DrsKitchenCategory.entries) {
            DrsEmojiKitchen.PARTICIPANTS.count { it.category == category } shouldBe 6
        }
    }

    test("every participant carries ar+en keywords (the search contract)") {
        for (e in DrsEmojiKitchen.PARTICIPANTS) {
            e.keywordAr.isNotBlank() shouldBe true
            e.keywordEn.isNotBlank() shouldBe true
        }
    }

    test("determinism: same pair → the same plan byte-for-byte") {
        val a = DrsEmojiKitchen.plan("😀", "❤️")
        val b = DrsEmojiKitchen.plan("😀", "❤️")
        a shouldNotBe null
        a shouldBe b
        a!!.recipeId shouldBe "face_heart"
        a.textPair shouldBe "😀❤️"
        a.pairKey shouldBe "pair:😀\u0000❤️"
    }

    test("honest silence: uncovered pairs, identical pairs and strangers yield NO plan") {
        // the four uncovered category pairs proven by the simulation
        DrsEmojiKitchen.plan("🌧️", "❄️") shouldBe null // WEATHER+WEATHER
        DrsEmojiKitchen.plan("🐱", "⭐") shouldBe null // ANIMAL+SYMBOL
        DrsEmojiKitchen.plan("🍎", "🍕") shouldBe null // FOOD+FOOD
        DrsEmojiKitchen.plan("👍", "⚽") shouldBe null // HANDS+OBJECT
        // identical values never compose
        DrsEmojiKitchen.plan("😀", "😀") shouldBe null
        // non-participants are strangers
        DrsEmojiKitchen.plan("🈚", "😀") shouldBe null
        DrsEmojiKitchen.plan("😀", "🈚") shouldBe null
    }

    test("layout bounds: every layer inside the safe area with closed scales/rotations") {
        for (base in DrsEmojiKitchen.PARTICIPANTS.map { it.value }) {
            for (second in DrsEmojiKitchen.PARTICIPANTS.map { it.value }) {
                val plan = DrsEmojiKitchen.plan(base, second) ?: continue
                (plan.layers.size in 1..3) shouldBe true
                for (layer in plan.layers) {
                    (layer.cx in 0.10f..0.90f) shouldBe true
                    (layer.cy in 0.10f..0.90f) shouldBe true
                    (layer.scale in 0.30f..1.00f) shouldBe true
                    (layer.rotationDeg in -7.5f..7.5f) shouldBe true
                }
            }
        }
    }

    test("mirrored pairs have their own recipes and different plans") {
        val direct = DrsEmojiKitchen.plan("😀", "❤️")
        val mirrored = DrsEmojiKitchen.plan("❤️", "😀")
        direct shouldNotBe null
        mirrored shouldNotBe null
        direct shouldNotBe mirrored
        mirrored!!.recipeId shouldBe "heart_face"
    }

    test("suggestions: deterministic, capped, renderable, never self") {
        val sug = DrsEmojiKitchen.suggestions("😀")
        (sug.isEmpty()) shouldBe false
        (sug.size <= DrsEmojiKitchen.SUGGESTION_LIMIT) shouldBe true
        sug shouldBe DrsEmojiKitchen.suggestions("😀")
        (sug.contains("😀")) shouldBe false
        for (s in sug) {
            DrsEmojiKitchen.plan("😀", s) shouldNotBe null
        }
        DrsEmojiKitchen.suggestions("🈚").size shouldBe 0
    }

    test("arabic-normalized search hits across hamza/tashkeel forms and english") {
        DrsEmojiKitchen.searchEmoji("ابتسام").shouldContain("😀")
        DrsEmojiKitchen.searchEmoji("قلب مكسور").shouldContain("💔")
        DrsEmojiKitchen.searchEmoji("SMILE").shouldContain("😀")
        DrsEmojiKitchen.searchEmoji("").size shouldBe 0
        DrsEmojiKitchen.searchEmoji("نار").shouldContain("🔥")
    }

    test("the engine is pure JVM — no android imports in the source file") {
        val src = java.io.File("src/main/kotlin/com/drs/smartkeyboard/drs/DrsEmojiKitchen.kt").readText()
        (src.contains("import android")) shouldBe false
        src shouldContain "object DrsEmojiKitchen"
    }

    test("the 13-recipe catalog is closed and unique by pair") {
        // FACE composes with exactly four secondary categories (closed)
        val reachable = DrsEmojiKitchen.PARTICIPANTS
            .filter { it.value != "😀" && DrsEmojiKitchen.plan("😀", it.value) != null }
            .map { it.category.name }
            .toSet()
        reachable shouldBe setOf("HEART", "FACE", "HANDS", "FOOD")
        // the full reachable pair graph from both directions covers exactly the 13
        var renderable = 0
        for (a in DrsEmojiKitchen.PARTICIPANTS) {
            for (b in DrsEmojiKitchen.PARTICIPANTS) {
                if (a.value != b.value && DrsEmojiKitchen.plan(a.value, b.value) != null) renderable++
            }
        }
        renderable shouldBe 456 // the closed recipe graph: 11 recipes × 36 + 2 × 30 (self-category minus identical) — pinned
    }
})
