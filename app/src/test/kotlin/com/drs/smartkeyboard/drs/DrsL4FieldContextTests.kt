/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsFieldKind
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * ذراع L4 «الذكاء السياقي بين الحقول» — عقود المصنِّف النقي
 * `DrsFieldKind` (من inputType الخام وحده — بلا قراءة محتوى أبدًا)،
 * وكتالوج الترتيب المغلق `DrsContextualRank` (الترتيب لا الإطلاق)،
 * وعقيدة الخصوصية الحاسمة: عائلة كلمات المرور أصمّ تمامًا — لا ترتيب
 * أدوات ولا قراءة محتوى (fail-closed بالبناء). المرجعية المحاكاة:
 * `scripts/check_l4_fields.py` (296 فحصًا أخضر) — والاختبارات هنا
 * تثبّت القوانين نفسها على JVM.
 */
class DrsL4FieldContextTests : FunSpec({

    // ---- 1) المصنِّف: العينات الواقعية (مرآة جدول المحاكاة) ----

    test("عائلة الحماية كلها محمية — لا علم يكسرها") {
        // النصي والمرئي وويب ورقم PIN — بأعلامها الواقعية وبلاها
        DrsFieldKind.from(0x1 or 0x80) shouldBe DrsFieldKind.PROTECTED
        DrsFieldKind.from(0x1 or 0x80 or 0x80000) shouldBe DrsFieldKind.PROTECTED
        DrsFieldKind.from(0x1 or 0x90) shouldBe DrsFieldKind.PROTECTED
        DrsFieldKind.from(0x1 or 0xE0) shouldBe DrsFieldKind.PROTECTED
        DrsFieldKind.from(0x2 or 0x10) shouldBe DrsFieldKind.PROTECTED
        DrsFieldKind.from(0x2 or 0x10 or 0x80000) shouldBe DrsFieldKind.PROTECTED
        // كلمة المرور متعددة الأسطر تبقى محمية (الحماية تسبق العلم)
        DrsFieldKind.from(0x1 or 0x80 or 0x40000) shouldBe DrsFieldKind.PROTECTED
        // والبريد متعدد الأسطر يبقى بريدًا (التخصص يسبق العلم العام)
        DrsFieldKind.from(0x1 or 0x20 or 0x40000) shouldBe DrsFieldKind.EMAIL
    }

    test("التخصصات المغلقة: بريد وروابط وبحث ونثر") {
        DrsFieldKind.from(0x1 or 0x20) shouldBe DrsFieldKind.EMAIL
        DrsFieldKind.from(0x1 or 0xD0) shouldBe DrsFieldKind.EMAIL
        DrsFieldKind.from(0x1 or 0x10) shouldBe DrsFieldKind.URI
        DrsFieldKind.from(0x1 or 0xB0) shouldBe DrsFieldKind.SEARCH
        DrsFieldKind.from(0x1 or 0x40 or 0x2000 or 0x8000) shouldBe DrsFieldKind.PROSE
        DrsFieldKind.from(0x1 or 0x50) shouldBe DrsFieldKind.PROSE
        DrsFieldKind.from(0x1 or 0x2000 or 0x8000 or 0x40000) shouldBe DrsFieldKind.PROSE
    }

    test("الأرقام والهاتف والتاريخ — الأصناف الآلية") {
        DrsFieldKind.from(0x2) shouldBe DrsFieldKind.NUMBER
        DrsFieldKind.from(0x2 or 0x2000) shouldBe DrsFieldKind.NUMBER
        DrsFieldKind.from(0x2 or 0x2000 or 0x4000) shouldBe DrsFieldKind.NUMBER
        DrsFieldKind.from(0x3) shouldBe DrsFieldKind.PHONE
        DrsFieldKind.from(0x4 or 0x10) shouldBe DrsFieldKind.DATETIME
        DrsFieldKind.from(0x4 or 0x20) shouldBe DrsFieldKind.DATETIME
        DrsFieldKind.from(0x4) shouldBe DrsFieldKind.DATETIME
    }

    test("الصمت الصادق: النص العادي والمجهول — GENERAL لا RAW") {
        DrsFieldKind.from(0x1 or 0x2000 or 0x8000) shouldBe DrsFieldKind.GENERAL
        DrsFieldKind.from(0x1 or 0xA0) shouldBe DrsFieldKind.GENERAL
        DrsFieldKind.from(0x1 or 0x60) shouldBe DrsFieldKind.GENERAL
        DrsFieldKind.from(0x1 or 0x70) shouldBe DrsFieldKind.GENERAL
        DrsFieldKind.from(0x1 or 0x30) shouldBe DrsFieldKind.GENERAL
        DrsFieldKind.from(0x1 or 0xC0) shouldBe DrsFieldKind.GENERAL
        // TYPE_NULL والصنف المجهول — أصمّ لا مخمَّن
        DrsFieldKind.from(0x0) shouldBe DrsFieldKind.RAW
        DrsFieldKind.from(0x7) shouldBe DrsFieldKind.RAW
        DrsFieldKind.from(0x9 or 0x2000 or 0x40000) shouldBe DrsFieldKind.RAW
    }

    test("الحتمية الكاملة: نفس المدخل نفس النوع دائمًا") {
        val samples = listOf(0x0, 0x1, 0x2, 0x3, 0x4, 0x7, 0x11, 0x21, 0x51,
            0x81, 0x91, 0xB1, 0xD1, 0xE1, 0x12, 0x303105, 0x9, 0x40)
        for (raw in samples) {
            DrsFieldKind.from(raw) shouldBe DrsFieldKind.from(raw)
        }
    }

    // ---- 2) عقيدة الخصوصية الحاسمة: عائلة الحماية أصمّ تمامًا ----

    test("حقول الحماية بلا ترتيب أدوات أبدًا") {
        for (raw in listOf(0x1 or 0x80, 0x1 or 0x90, 0x1 or 0xE0, 0x2 or 0x10,
                0x1 or 0x80 or 0x40000)) {
            val kind = DrsFieldKind.from(raw)
            DrsContextualRank.rankedTools(kind).shouldBeEmpty()
        }
        DrsContextualRank.rankedTools(DrsFieldKind.RAW).shouldBeEmpty()
    }

    test("بوابة قراءة المحتوى: الحماية مرفوضة وكل غيرها مسموحة") {
        // الحقول المرئية تكشف نصها — البوابة ترفض فعلًا لا شكلًا
        DrsContextualRank.contentAdviceAllowed(DrsFieldKind.PROTECTED) shouldBe false
        DrsContextualRank.contentAdviceAllowed(DrsFieldKind.RAW) shouldBe false
        for (kind in DrsFieldKind.entries - DrsFieldKind.PROTECTED - DrsFieldKind.RAW) {
            DrsContextualRank.contentAdviceAllowed(kind) shouldBe true
        }
    }

    test("علامة isProtected صادقة للعائلة الصمّاء وحدها") {
        DrsFieldKind.PROTECTED.isProtected shouldBe true
        DrsFieldKind.RAW.isProtected shouldBe true
        for (kind in DrsFieldKind.entries - DrsFieldKind.PROTECTED - DrsFieldKind.RAW) {
            kind.isProtected shouldBe false
        }
    }

    // ---- 3) كتالوج الترتيب المغلق — القوائم الحرفية بالترتيب الحرفي ----

    test("ترتيب البريد: فضح اللصق الصامت أولًا") {
        DrsContextualRank.rankedTools(DrsFieldKind.EMAIL) shouldBe listOf(
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.LOWERCASE,
            DrsTextTool.STRIP_EMOJI,
            DrsTextTool.URL_ENCODE,
        )
    }

    test("ترتيب الروابط: زوج URL أولًا") {
        DrsContextualRank.rankedTools(DrsFieldKind.URI) shouldBe listOf(
            DrsTextTool.URL_ENCODE,
            DrsTextTool.URL_DECODE,
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.STRIP_EMOJI,
        )
    }

    test("ترتيب الهاتف والرقم والتاريخ والنثر والبحث") {
        DrsContextualRank.rankedTools(DrsFieldKind.PHONE) shouldBe listOf(
            DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.REMOVE_ALL_SPACES,
        )
        DrsContextualRank.rankedTools(DrsFieldKind.NUMBER) shouldBe listOf(
            DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTool.NUMBER_WORDS,
            DrsTextTool.TAFQIT,
        )
        DrsContextualRank.rankedTools(DrsFieldKind.DATETIME) shouldBe listOf(
            DrsTextTool.DATE_WORDS,
            DrsTextTool.TIME_WORDS,
            DrsTextTool.WEEKDAY,
            DrsTextTool.GREGORIAN_TO_HIJRI,
            DrsTextTool.HIJRI_TO_GREGORIAN,
        )
        DrsContextualRank.rankedTools(DrsFieldKind.PROSE) shouldBe listOf(
            DrsTextTool.IMPROVE_PHRASES,
            DrsTextTool.TO_ARABIC_PUNCTUATION,
            DrsTextTool.REMOVE_LETTER_STRETCH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.SENTENCE_PER_LINE,
        )
        DrsContextualRank.rankedTools(DrsFieldKind.SEARCH) shouldBe listOf(
            DrsTextTool.REMOVE_DIACRITICS,
            DrsTextTool.REMOVE_TATWEEL,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.NORMALIZE_ARABIC,
        )
    }

    test("الصمت الصادق: GENERAL بلا ترتيب مخمَّن") {
        DrsContextualRank.rankedTools(DrsFieldKind.GENERAL).shouldBeEmpty()
    }

    // ---- 4) البنية: الترتيب لا الإطلاق — تحويلات نقية فقط ----

    test("لا عملية محرر ولا علامة إدراج ولا أداة معلومات في أي ترتيب") {
        val forbidden = DrsTextTool.entries.filter {
            it.isEditorOp || it.isInsertMark || it.isInfoOnly
        }.toSet()
        for (kind in DrsFieldKind.entries) {
            for (tool in DrsContextualRank.rankedTools(kind)) {
                (tool in forbidden) shouldBe false
            }
        }
    }

    test("لا تكرار وسقف محترم وكل كود داخل مدى الكتالوج") {
        for (kind in DrsFieldKind.entries) {
            val ranked = DrsContextualRank.rankedTools(kind)
            ranked.size shouldBe ranked.distinct().size
            (ranked.size <= DrsContextualRank.MAX_RANKED) shouldBe true
            for (tool in ranked) {
                (tool.code in -684..-601) shouldBe true
            }
        }
    }

    test("حتمية القوائم: مرتان = مرة") {
        for (kind in DrsFieldKind.entries) {
            val first = DrsContextualRank.rankedTools(kind)
            val second = DrsContextualRank.rankedTools(kind)
            first shouldBe second
        }
    }

    // ---- 5) عقيدة اللا-op: كل أداة مرتبة تعيد السلسلة الفارغة سليمة ----

    test("كل أداة مرتبة لا-op على الفراغ — لا تلف ولا تخمين") {
        for (kind in DrsFieldKind.entries) {
            for (tool in DrsContextualRank.rankedTools(kind)) {
                DrsTextTools.apply(tool, "") shouldBe ""
            }
        }
    }

    // ---- 6) التكامل: المحركان لا يتصادمان — محتوىً وصفة من مصدرين ----

    test("المستشاران من مصدرين مستقلين: المرتب fieldKind والمستشار نص") {
        // حقل URI بنص لاتيني عادي: الترتيب من الصفة، والمستشار من
        // المحتوى — الصفة لا تختلس صوت المحتوى ولا العكس.
        val kind = DrsFieldKind.from(0x1 or 0x10)
        DrsContextualRank.rankedTools(kind).shouldContain(DrsTextTool.URL_ENCODE)
        // والنص العادي بلا أي شكل مغلق فالمستشار الصامت يصمت
        DrsTileContextAdvisor.advise("hello world").shouldBeEmpty()
    }

    test("حقل محمي بنص يشبه كلمة مرور: المستشار يبقى صامتًا بالبوابة") {
        // النص لو حُذر لاقترح مقياس القوة — لكن البوابة الحية تمنع
        // تغذية المستشار من حقل محمي مهما بدا النص.
        val kind = DrsFieldKind.from(0x1 or 0x80)
        DrsContextualRank.contentAdviceAllowed(kind) shouldBe false
        DrsContextualRank.rankedTools(kind).shouldHaveSize(0)
    }
})
