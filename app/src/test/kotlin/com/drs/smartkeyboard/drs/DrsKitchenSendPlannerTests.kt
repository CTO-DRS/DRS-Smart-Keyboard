package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * عقود مخطط الإرسال الصادق — the total, pure decision function:
 * image where supported → honest text pair → clipboard for stickers.
 */
class DrsKitchenSendPlannerTests : FunSpec({

    test("an image-accepting editor gets CONTENT") {
        DrsKitchenSendPlanner.plan(listOf("image/png"), true) shouldBe
            DrsKitchenSendPlanner.SendPath.CONTENT
        DrsKitchenSendPlanner.plan(listOf("image/jpeg", "image/png"), true) shouldBe
            DrsKitchenSendPlanner.SendPath.CONTENT
        // case-insensitive mime match
        DrsKitchenSendPlanner.plan(listOf("IMAGE/PNG"), true) shouldBe
            DrsKitchenSendPlanner.SendPath.CONTENT
    }

    test("a text-only editor gets the honest TEXT_PAIR") {
        DrsKitchenSendPlanner.plan(listOf("text/plain"), true) shouldBe
            DrsKitchenSendPlanner.SendPath.TEXT_PAIR
        DrsKitchenSendPlanner.plan(emptyList(), true) shouldBe
            DrsKitchenSendPlanner.SendPath.TEXT_PAIR
        DrsKitchenSendPlanner.plan(null, true) shouldBe
            DrsKitchenSendPlanner.SendPath.TEXT_PAIR
    }

    test("the setting off forces the text pair even where images are supported") {
        DrsKitchenSendPlanner.plan(listOf("image/png"), false) shouldBe
            DrsKitchenSendPlanner.SendPath.TEXT_PAIR
    }

    test("a sticker (no text pair) on a text editor falls back to CLIPBOARD") {
        DrsKitchenSendPlanner.plan(listOf("text/plain"), true, hasTextPair = false) shouldBe
            DrsKitchenSendPlanner.SendPath.CLIPBOARD
        DrsKitchenSendPlanner.plan(null, true, hasTextPair = false) shouldBe
            DrsKitchenSendPlanner.SendPath.CLIPBOARD
        // ...but an image editor still gets CONTENT
        DrsKitchenSendPlanner.plan(listOf("image/png"), true, hasTextPair = false) shouldBe
            DrsKitchenSendPlanner.SendPath.CONTENT
        // and the setting off sends a sticker to the clipboard too
        DrsKitchenSendPlanner.plan(listOf("image/png"), false, hasTextPair = false) shouldBe
            DrsKitchenSendPlanner.SendPath.CLIPBOARD
    }

    test("the image mime constant is the one the renderer produces") {
        DrsKitchenSendPlanner.IMAGE_MIME shouldBe "image/png"
    }
})
