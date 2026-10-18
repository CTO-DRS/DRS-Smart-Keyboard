/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the ninth text-tools round — the link harvester (EXTRACT_LINKS)
 * and the Arabic stretch cleaner (REMOVE_LETTER_STRETCH). Every
 * expectation below was simulated in Python BEFORE the build (the
 * session doctrine) — check_extract_stretch.py — and the two engines
 * are byte-faithful mirrors of that simulation.
 */
class DrsTextToolsHarvestStretchTests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // EXTRACT_LINKS — the link harvester
    // -----------------------------------------------------------

    test("a single scheme URL is harvested out of Arabic prose") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "شوف هذا https://example.com/page وقولي رأيك", en) shouldBe
            "https://example.com/page"
    }

    test("links and emails land one per line in order of appearance") {
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "رابط أول https://a.b/c ورابط ثانٍ www.example.org وبريد me@example.com",
            en,
        ) shouldBe "https://a.b/c\nwww.example.org\nme@example.com"
    }

    test("trailing Arabic and Latin sentence punctuation is stripped") {
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "راجع https://example.com، أو https://example.org.",
            en,
        ) shouldBe "https://example.com\nhttps://example.org"
    }

    test("a balanced wiki-style bracket stays inside its link") {
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "(https://en.wikipedia.org/wiki/Java_(class))",
            en,
        ) shouldBe "https://en.wikipedia.org/wiki/Java_(class)"
        // depth goes net-negative: exactly the unbalanced closers leave.
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "الرابط https://x.com/page_(x)) بعمق سالب",
            en,
        ) shouldBe "https://x.com/page_(x)"
    }

    test("a lone closing bracket leaves with the sentence") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "شوف (https://x.com) اليوم", en) shouldBe
            "https://x.com"
    }

    test("strict email only — a bare a@b and a no-TLD domain are refused") {
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "راسلني على user.name+tag@sub.example.com.",
            en,
        ) shouldBe "user.name+tag@sub.example.com"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "a@b ليست بريدًا", en) shouldBe "a@b ليست بريدًا"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "ليس بريدًا: user@localhost", en) shouldBe
            "ليس بريدًا: user@localhost"
    }

    test("a bare scheme is not a link and percent signs are not either") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "https:// وحده ليس رابطًا", en) shouldBe
            "https:// وحده ليس رابطًا"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "عرض سعري: 50% خصم", en) shouldBe
            "عرض سعري: 50% خصم"
    }

    test("mailto keeps only the strict email inside it") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "اكتب mailto:info@example.com اليوم", en) shouldBe
            "info@example.com"
    }

    test("www refuses to fire inside a larger word or domain") {
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "بريد a@b.com ثم www.example.com/path?x=1، انتهى",
            en,
        ) shouldBe "a@b.com\nwww.example.com/path?x=1"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "كلمةawww.example.com مدفونة", en) shouldBe
            "كلمةawww.example.com مدفونة"
    }

    test("duplicates are the text's own business — order kept, no dedupe") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "https://x.com و https://x.com", en) shouldBe
            "https://x.com\nhttps://x.com"
    }

    test("line breaks are boundaries and case is kept verbatim") {
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "سطران https://a.com\nو www.b.com", en) shouldBe
            "https://a.com\nwww.b.com"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "HTTPS://EXAMPLE.COM/PATH كبيرة", en) shouldBe
            "HTTPS://EXAMPLE.COM/PATH"
        DrsTextTools.apply(
            DrsTextTool.EXTRACT_LINKS,
            "ftp://files.example.org/dir/file.tar.gz",
            en,
        ) shouldBe "ftp://files.example.org/dir/file.tar.gz"
    }

    test("the harvester is idempotent — applying twice equals once") {
        val prose = "رابط https://example.com/page وقوس (https://x.com) وبريد me@x.org."
        val once = DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, prose, en)
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, once, en) shouldBe once
    }

    // -----------------------------------------------------------
    // REMOVE_LETTER_STRETCH — the Arabic stretch cleaner
    // -----------------------------------------------------------

    test("stretched runs of three-plus collapse to one letter") {
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "هلااااا والله", en) shouldBe
            "هلا والله"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "مرحباااااا", en) shouldBe "مرحبا"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "وااااو", en) shouldBe "واو"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "سوووبر", en) shouldBe "سوبر"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "ااا", en) shouldBe "ا"
    }

    test("legitimate doubles stay — الله، عدّ، أمم") {
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "الله أكبر", en) shouldBe
            "الله أكبر"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "العدّاد أمين", en) shouldBe
            "العدّاد أمين"
    }

    test("three lams collapse to one — deterministic, never a guess") {
        // «الللله» (3 lams) ينكفي إلى حرف واحد: القاعدة حتمية لا
        // تُخمِّن التصحيح إلى لامين.
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "الللله", en) shouldBe "اله"
    }

    test("a diacritic after a stretched run follows the collapsed letter") {
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "جمييييل جدااااً", en) shouldBe
            "جميل جداً"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "شكككككراً جزيلاً", en) shouldBe
            "شكراً جزيلاً"
    }

    test("Latin, digits, tatweel and punctuation are out of the zone") {
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "www.sooo.com سليمة", en) shouldBe
            "www.sooo.com سليمة"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "رقم 111 ومجــــدة", en) shouldBe
            "رقم 111 ومجــــدة"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "ab", en) shouldBe "ab"
    }

    test("the stretch cleaner is idempotent and byte-identical when calm") {
        val calm = "الله أكبر والعدّاد أمين و www.example.com"
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, calm, en) shouldBe calm
        val stretched = "هلااااا والله جمييييل"
        val once = DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, stretched, en)
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, once, en) shouldBe once
    }

    test("both new tools never throw on hostile input and stay pure") {
        val hostile = "\uD83C\uDF0E \u0000 \t\n\r$ { } %s %d ٍَّ most"
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, hostile, en) shouldBe hostile
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, hostile, en) shouldBe hostile
        DrsTextTools.apply(DrsTextTool.EXTRACT_LINKS, "", en) shouldBe ""
        DrsTextTools.apply(DrsTextTool.REMOVE_LETTER_STRETCH, "", en) shouldBe ""
    }
})
