package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

/**
 * أسطح مطبخ الإيموجي — the structural pins: the ImeUiMode slot, the
 * KeyCode, the catalogue tail contract, the panel wiring surfaces, the
 * FileProvider cache path, the five-permission invariant, and the
 * AR/EN string parity of the kitchen block.
 */
class DrsKitchenSurfaceTests : FunSpec({

    fun src(rel: String): String = File("src/main/kotlin/com/drs/smartkeyboard/$rel").readText()
    fun res(rel: String): String = File("src/main/res/$rel").readText()

    test("the seventh smart panel occupies ImeUiMode slot 10") {
        com.drs.smartkeyboard.ime.ImeUiMode.EMOJI_KITCHEN.value shouldBe 10
        com.drs.smartkeyboard.ime.ImeUiMode.entries.size shouldBe 11
        com.drs.smartkeyboard.ime.ImeUiMode.fromInt(10) shouldBe
            com.drs.smartkeyboard.ime.ImeUiMode.EMOJI_KITCHEN
    }

    test("the engine action code is -235 and dispatches the real mode") {
        com.drs.smartkeyboard.ime.text.key.KeyCode.IME_UI_MODE_EMOJI_KITCHEN shouldBe -235
        val kb = src("ime/keyboard/KeyboardManager.kt")
        kb.shouldContain("KeyCode.IME_UI_MODE_EMOJI_KITCHEN -> {")
        kb.shouldContain("activeState.imeUiMode = ImeUiMode.EMOJI_KITCHEN")
        val win = src("ime/window/ImeWindow.kt")
        win.shouldContain("ImeUiMode.EMOJI_KITCHEN -> ProvideActualLayoutDirection { DrsEmojiKitchenPanel() }")
    }

    test("the unified catalogue keeps its tail-append contract with the kitchen last") {
        DrsUnifiedTools.byId("emoji_kitchen_panel")?.code shouldBe
            com.drs.smartkeyboard.ime.text.key.KeyCode.IME_UI_MODE_EMOJI_KITCHEN
        DrsUnifiedTools.ALL.last().id shouldBe "emoji_kitchen_panel"
        DrsUnifiedTools.ALL.size shouldBe 53
        DrsUnifiedTools.ALL[DrsUnifiedTools.ALL.size - 2].id shouldBe "my_texts_panel"
        DrsUnifiedTools.ALL[DrsUnifiedTools.ALL.size - 3].id shouldBe "smart_clipboard_panel"
        DrsUnifiedTools.ALL.first().id shouldBe "emoji"
    }

    test("the switcher chips carry the kitchen chip") {
        src("drs/ui/DrsSmartPanels.kt")
            .shouldContain("ImeUiMode.EMOJI_KITCHEN to R.string.panel__switcher_kitchen")
    }

    test("the kitchen counts as a smart tool press") {
        src("ime/smartbar/quickaction/QuickAction.kt")
            .shouldContain("KeyCode.IME_UI_MODE_EMOJI_KITCHEN,")
    }

    test("the FileProvider serves the kitchen cache path — no new permissions") {
        res("xml/file_paths.xml").shouldContain("emoji-kitchen/")
        val mf = File("src/main/AndroidManifest.xml").readText()
        val permissions = Regex("uses-permission android:name=\"([^\"]+)\"").findAll(mf).map { it.groupValues[1] }.toList()
        permissions.size shouldBe 5
        permissions.shouldContain("android.permission.INTERNET")
    }

    test("the send executor rides the documented commitContent API") {
        val editor = src("ime/editor/EditorInstance.kt")
        editor.shouldContain("fun commitImageContent(uri: Uri, description: String): Boolean")
        editor.shouldContain("InputConnectionCompat.commitContent")
        editor.shouldContain("val activeContentMimeTypes")
    }

    test("the kitchen strings keep the audited AR/EN parity") {
        val ar = Regex("<string name=\"([^\"]+)\">").findAll(res("values/strings.xml")).map { it.groupValues[1] }.toSet()
        val en = Regex("<string name=\"([^\"]+)\">").findAll(res("values-en/strings.xml")).map { it.groupValues[1] }.toSet()
        (ar - en).shouldBeEmptySet()
        (en - ar).shouldBeEmptySet()
        ar.shouldContain("panel__kitchen__title")
        ar.shouldContain("drs__unified__tool_emoji_kitchen_panel")
    }

    test("the sticker index ships with its license and 12 originals") {
        val index = File("src/main/assets/drs/kitchen/stickers.json").readText()
        index.shouldContain("\"license\": \"Apache-2.0\"")
        index.shouldContain("drs-kitchen-originals")
        Regex("\"id\":").findAll(index).count() shouldBe 13 // pack id + 12 sticker ids
    }
})

private fun Set<String>.shouldBeEmptySet() {
    if (isNotEmpty()) throw AssertionError("unexpected entries: $this")
}
