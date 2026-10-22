package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * عقود مكتبة الملصقات — the fail-closed index schema, license
 * mandatory-ness, closed categories, and search of [DrsStickerLibrary].
 */
class DrsStickerLibraryTests : FunSpec({

    fun packJson(stickers: String = DEFAULT_STICKERS, license: String = "Apache-2.0") = """
        {
          "schemaVersion": 1,
          "pack": {
            "id": "drs-kitchen-originals",
            "nameAr": "أصليات مطبخ درز",
            "nameEn": "DRS Kitchen Originals",
            "version": "1.0.0",
            "author": "DRS Smart Keyboard Project",
            "license": "$license",
            "licenseNote": "original artwork"
          },
          "stickers": $stickers
        }
    """.trimIndent()

    test("the shipped index parses and validates") {
        val index = DrsStickerLibrary.load(REAL_ASSET_JSON)
        index shouldNotBe null
        index!!.pack.id shouldBe "drs-kitchen-originals"
        index.pack.license shouldBe "Apache-2.0"
        index.stickers.shouldHaveSize(12)
    }

    test("2 stickers per closed category, 6 categories total") {
        DrsStickerLibrary.DrsStickerCategory.entries.shouldHaveSize(6)
        val index = DrsStickerLibrary.load(REAL_ASSET_JSON)!!
        for (c in DrsStickerLibrary.DrsStickerCategory.entries) {
            DrsStickerLibrary.byCategory(index, c).shouldHaveSize(2)
        }
    }

    test("fail-closed: wrong schema version is rejected") {
        DrsStickerLibrary.load(packJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")).shouldBeNull()
    }

    test("fail-closed: a missing license rejects the whole index") {
        DrsStickerLibrary.load(packJson(license = "")).shouldBeNull()
    }

    test("fail-closed: duplicate sticker ids are rejected") {
        val dup = """
            [
              {"id": "a", "drawable": "st_kitchen_rocket", "category": "OBJECTS",
               "keywords": {"ar": ["صاروخ"], "en": ["rocket"]}},
              {"id": "a", "drawable": "st_kitchen_gift_box", "category": "OBJECTS",
               "keywords": {"ar": ["هدية"], "en": ["gift"]}}
            ]
        """.trimIndent()
        DrsStickerLibrary.load(packJson(stickers = dup)).shouldBeNull()
    }

    test("fail-closed: unknown categories and empty keywords are rejected") {
        val badCat = """
            [{"id": "a", "drawable": "st_kitchen_rocket", "category": "SPACESHIPS",
              "keywords": {"ar": ["صاروخ"], "en": ["rocket"]}}]
        """.trimIndent()
        DrsStickerLibrary.load(packJson(stickers = badCat)).shouldBeNull()
        val noKw = """
            [{"id": "a", "drawable": "st_kitchen_rocket", "category": "OBJECTS",
              "keywords": {"ar": ["صاروخ"], "en": []}}]
        """.trimIndent()
        DrsStickerLibrary.load(packJson(stickers = noKw)).shouldBeNull()
    }

    test("fail-closed: malformed json is rejected") {
        DrsStickerLibrary.load("{ not json").shouldBeNull()
    }

    test("search hits ar and en keywords, empty query stays empty") {
        val index = DrsStickerLibrary.load(REAL_ASSET_JSON)!!
        DrsStickerLibrary.search(index, "صاروخ").map { it.id }.shouldContain("st_kitchen_rocket")
        DrsStickerLibrary.search(index, "rocket").map { it.id }.shouldContain("st_kitchen_rocket")
        DrsStickerLibrary.search(index, "").size shouldBe 0
    }

    test("every shipped drawable name maps to a REAL resource") {
        val index = DrsStickerLibrary.load(REAL_ASSET_JSON)!!
        for (s in index.stickers) {
            (com.drs.smartkeyboard.drs.ui.stickerDrawableRes(s.drawable) != null) shouldBe true
        }
    }
})

private val DEFAULT_STICKERS = "[]"
private val REAL_ASSET_JSON = run {
    val file = java.io.File("src/main/assets/drs/kitchen/stickers.json")
    if (file.exists()) file.readText() else "schemaVersion missing"
}
