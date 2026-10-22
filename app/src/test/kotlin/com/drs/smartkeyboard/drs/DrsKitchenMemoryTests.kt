package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * عقود ذاكرة المطبخ — the history cap/dedupe, favorites toggle, and
 * the serializer round-trip (the exact JetPref pattern of EmojiHistory).
 */
class DrsKitchenMemoryTests : FunSpec({

    test("history pushes are deduped and move-to-front") {
        var m = DrsKitchenMemory.Empty
        m = m.withHistoryPush("a")
        m = m.withHistoryPush("b")
        m = m.withHistoryPush("a")
        m.history shouldBe listOf("a", "b")
    }

    test("history is capped at 24 — a glanceable row, never a flood") {
        var m = DrsKitchenMemory.Empty
        for (i in 0 until 30) {
            m = m.withHistoryPush("k$i")
        }
        m.history.shouldHaveSize(DrsKitchenMemory.HISTORY_CAP)
        m.history.first() shouldBe "k29"
        m.recentKeys shouldBe m.history
    }

    test("favorites toggle append/remove and the predicate agrees") {
        var m = DrsKitchenMemory.Empty
        m.isFavorite("pair:x") shouldBe false
        m = m.withFavoriteToggled("pair:x")
        m.isFavorite("pair:x") shouldBe true
        m.favorites.shouldContain("pair:x")
        m = m.withFavoriteToggled("pair:x")
        m.isFavorite("pair:x") shouldBe false
    }

    test("the serializer round-trips and fails closed to Empty") {
        val m = DrsKitchenMemory(
            favorites = listOf("pair:😀\u0000❤️", "sticker:st_kitchen_rocket"),
            history = listOf("sticker:st_kitchen_rocket", "pair:🐱\u0000🐶"),
        )
        val raw = DrsKitchenMemory.Serializer.serialize(m)
        raw.shouldContain("st_kitchen_rocket")
        DrsKitchenMemory.Serializer.deserialize(raw) shouldBe m
        DrsKitchenMemory.Serializer.deserialize("{ broken") shouldBe DrsKitchenMemory.Empty
    }
})
