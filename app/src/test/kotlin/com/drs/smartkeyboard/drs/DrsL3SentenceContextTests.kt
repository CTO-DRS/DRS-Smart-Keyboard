/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsLearningEngine
import com.drs.smartkeyboard.drs.ai.DrsSentenceContext
import com.drs.smartkeyboard.ime.nlp.latin.LatinWordNormalize
import com.drs.smartkeyboard.ime.nlp.latin.SENTENCE_TERMINATORS
import com.drs.smartkeyboard.ime.nlp.latin.isWordInternalChar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * ذراع L3 «توقع الكلمة التالية الأعمق» — عقود المحلل الجُملي، سلسلة
 * الترقيم المغلقة، أعلى مفردات المستخدم، والسبب المغلق 1.13 في
 * مستشار البلاطات (الدمج بعقود الإطلاق دون كسرها). المرجعية المحاكاة:
 * `scripts/check_l3_sentence.py` (400 فحصًا أخضر) — والاختبارات هنا
 * تثبّت القوانين نفسها على JVM.
 */
class DrsL3SentenceContextTests : FunSpec({

    fun normalize(word: String): String = LatinWordNormalize.normalize(word)

    fun analyze(before: String): DrsSentenceContext.State =
        DrsSentenceContext.analyze(before, ::normalize, ::isWordInternalChar)

    // ------------------------------------------------------------------
    // 1) المرآة المثبتة: كتالوج النهايات = نهايات المزود (مصدر حقيقة واحد)
    // ------------------------------------------------------------------

    test("المرآة: نهايات المحلل الجُملي = نهايات المزود المعلنة") {
        DrsSentenceContext.TERMINALS shouldBe SENTENCE_TERMINATORS.toSet()
        // والتوقيعية حاضرة: النقطة والاستفهام العربية والتعجب والحذف والوقف
        setOf('.', '!', '?', '…', '؟', '۔') shouldBe DrsSentenceContext.TERMINALS
    }

    // ------------------------------------------------------------------
    // 2) قانون حد الجملة — إصلاح العبور: الجملة المنتهية لا سياق للجملة التالية
    // ------------------------------------------------------------------

    test("حد الجملة: موجبات مغلقة (نهاية بلا كلمة بعدها = بداية جديدة)") {
        analyze("كتبت الرسالة. ").freshStart shouldBe true
        analyze("كتبت الرسالة.").freshStart shouldBe true
        analyze("انتهى الأمر؟").freshStart shouldBe true
        analyze("انتهى الأمر۔").freshStart shouldBe true
        analyze("هل وصلتك!").freshStart shouldBe true
        analyze("ننتظر…").freshStart shouldBe true
        analyze("قال: \"انتهى.\"").freshStart shouldBe true   // مغلقة بعد النهاية
        analyze("انتهى.» ").freshStart shouldBe true
        analyze("The end. ").freshStart shouldBe true         // اللاتيني بنفس القانون
        analyze("Done?").freshStart shouldBe true
    }

    test("حد الجملة: سالبات مغلقة (منتصف الجملة = المسار القديم حرفيًا)") {
        analyze("كتبت الرسالة").freshStart shouldBe false     // المؤشر على الكلمة
        analyze("انتهى؟ طيب").freshStart shouldBe false       // كلمة بعد النهاية
        analyze("كتبت الرسالة، ثم").freshStart shouldBe false // الفاصل ليس نهاية
        analyze("الرسالة: ثم").freshStart shouldBe false      // النقطتان ليسا نهاية
        analyze("الرسالة؛ ثم").freshStart shouldBe false
        analyze("في الطريق").freshStart shouldBe false
        analyze("«الرسالة").freshStart shouldBe false
        analyze("").freshStart shouldBe false
        analyze("   ").freshStart shouldBe false
        analyze("…").freshStart shouldBe false                // نهاية بلا كلمة قبلها — صمت
        analyze(".").freshStart shouldBe false
        analyze("1250").freshStart shouldBe false
    }

    test("عقد اللا-op: منتصف الجملة يعيد الحالة الصامتة (لا بداية ولا تتمة ترقيم)") {
        // المرجعية: 1,200 حالة مولَّدة في check_l3_sentence.py — والعينة الموقعة هنا
        val battery = listOf(
            "كتبت الرسالة", "كتبت الرسالة، ثم", "في المشروع ", "القلم «",
            "سجل ٢٥ نقطة", "العدد 5 ", "hello world", "the end of",
        )
        for (t in battery) {
            val state = analyze(t)
            state.freshStart shouldBe false
            state.ordinalSuccessor shouldBe null
        }
    }

    test("النافذة المحدودة: نهاية بعيدة عن الذيل بلا كلمة بينها = صمت صادق لا ضجيج عبور") {
        // «.» ثم 70 مسافة: لا كلمة في النافذة (64) → صمت — والمزود يعيد
        // قائمة فارغة (لا سياق) فلا يعبر الحد أصلًا.
        val state = analyze("من وقت طويل. " + " ".repeat(70))
        state.freshStart shouldBe false
        state.ordinalSuccessor shouldBe null
    }

    // ------------------------------------------------------------------
    // 3) سلسلة الترقيم المغلقة — تتمة حتمية بعقد الإطلاق
    // ------------------------------------------------------------------

    test("سلسلة الترقيم: كل حلقة لها تتمتها الحتمية الوحيدة") {
        val chain = DrsSentenceContext.ORDINAL_CHAIN
        for (i in 0 until chain.size - 1) {
            analyze("نبدأ ${chain[i]}").ordinalSuccessor shouldBe chain[i + 1]
            analyze("نبدأ ${chain[i]}، ").ordinalSuccessor shouldBe chain[i + 1]
        }
    }

    test("سلسلة الترقيم: متغايرات التطبيع تصل للحلقة ذاتها (همزات وتنوين)") {
        analyze("نبدأ اولا").ordinalSuccessor shouldBe "ثانيًا"
        analyze("نبدأ أولا").ordinalSuccessor shouldBe "ثانيًا"
        analyze("نبدأ أولًا").ordinalSuccessor shouldBe "ثانيًا"
        analyze("ثم ثانيا، ").ordinalSuccessor shouldBe "ثالثًا"
    }

    test("سلسلة الترقيم: نهاية السلسلة تصمت، وغير الترقيم لا يخترع تتمة") {
        analyze("ثم ثامنًا").ordinalSuccessor shouldBe null
        analyze("في كتاب").ordinalSuccessor shouldBe null
        analyze("في القلم").ordinalSuccessor shouldBe null
    }

    test("حد الجملة يسكت الترقيم: جملة انتهت بعدّ لا تستمر تخمينًا") {
        val state = analyze("خلص العد ثامنًا. ")
        state.freshStart shouldBe true
        state.ordinalSuccessor shouldBe null
    }

    // ------------------------------------------------------------------
    // 4) الافتتاحيات المغلقة — طابق اللغة الأرضي لبداية الجملة
    // ------------------------------------------------------------------

    test("الافتتاحيات: كتالوج مغلق بترتيب ثابت وصيغ عرض طبيعية") {
        val openers = DrsSentenceContext.openers(14)
        openers shouldBe DrsSentenceContext.OPENERS
        openers.size shouldBe 14
        openers.first() shouldBe "كان"
        // صيغة العرض الطبيعية (بهمزاتها) لا الصيغ المطبَّعة
        openers shouldContain "أما"
        openers shouldContain "الآن"
        openers shouldContain "إذن"
    }

    test("الافتتاحيات: الحد حتمي وغير الموجب قائمة فارغة صادقة") {
        DrsSentenceContext.openers(4) shouldBe DrsSentenceContext.OPENERS.take(4)
        DrsSentenceContext.openers(0) shouldBe emptyList()
        DrsSentenceContext.openers(-3) shouldBe emptyList()
        // السقف فوق الحجم يعيد الكتالوج كله لا أكثر
        DrsSentenceContext.openers(99) shouldBe DrsSentenceContext.OPENERS
    }

    // ------------------------------------------------------------------
    // 5) أعلى مفردات المستخدم — الإشارة الشخصية لبداية الجملة
    // ------------------------------------------------------------------

    test("أعلى المفردات: عدّ تنازلي ورابط كسر التعادل = الكلمة تصاعديًا") {
        DrsLearningEngine.clear()
        try {
            val file = File(kotlin.io.path.createTempDirectory("drs-l3").toFile(), "learning.json")
            repeat(5) { DrsLearningEngine.learnWord("الكتاب") }
            repeat(5) { DrsLearningEngine.learnWord("القلم") }
            repeat(2) { DrsLearningEngine.learnWord("مشروع") }
            DrsLearningEngine.learnWord("خير")
            DrsLearningEngine.persist(file)
            DrsLearningEngine.load(file)
            val top = DrsLearningEngine.personalTopWords(4)
            // بعد التحلل: الكتاب والقلم متعادلان (4) — الرابط حتمي بترتيب
            // وحدات UTF-16: ق (U+0642) قبل ك (U+0643) — حتى بعد دورة الحفظ
            top.map { it.first } shouldBe listOf("القلم", "الكتاب", "مشروع", "خير")
            top[0].second shouldBe top[1].second
            // والحد صارم
            DrsLearningEngine.personalTopWords(2).map { it.first } shouldBe listOf("القلم", "الكتاب")
            DrsLearningEngine.personalTopWords(0) shouldBe emptyList()
        } finally {
            DrsLearningEngine.clear()
        }
    }

    test("أعلى المفردات: محرك فارغ = صمت صادق") {
        DrsLearningEngine.clear()
        try {
            DrsLearningEngine.personalTopWords(4) shouldBe emptyList()
        } finally {
            DrsLearningEngine.clear()
        }
    }

    // ------------------------------------------------------------------
    // 6) السبب المغلق 1.13 — دمج البلاطات بعقد الإطلاق المضمون
    // ------------------------------------------------------------------

    test("1.13 موجب: لصق جمل ملتصقة بحدّين فأكثر يرشّح «جملة لكل سطر»") {
        val picks = DrsTileContextAdvisor.advise("كتب الرسالة. ثم غادر. وعاد")
        picks shouldContain DrsTextTool.SENTENCE_PER_LINE
        // نهايات عربية وتعجب واستفهام لاتيني — كل تواقيع الصنف
        DrsTileContextAdvisor.advise("س1؟ س2؟ س3") shouldContain DrsTextTool.SENTENCE_PER_LINE
        DrsTileContextAdvisor.advise("one. two? three") shouldContain DrsTextTool.SENTENCE_PER_LINE
        // والمرشَّح نفسه سيطلق فعلًا — الدليل داخل الاختبار
        DrsTextTools.apply(DrsTextTool.SENTENCE_PER_LINE, "كتب. ثم غادر. وعاد") shouldBe
            "كتب.\nثم غادر.\nوعاد"
    }

    test("1.13 سالب: الصمت في كل فئات الرفض المغلقة") {
        // جملة واحدة بحدّ مفرد — صنف الالتباس (الاختصارات «ا.د.» وأشباهها)
        DrsTileContextAdvisor.advise("كتب الرسالة. ثم غادر") shouldNotContain
            DrsTextTool.SENTENCE_PER_LINE
        // اختصار بحدّين متلاصقين (لا فراغ بعدهما) — لا تواقيع أفقية
        DrsTileContextAdvisor.advise("ا.د. محمد") shouldNotContain DrsTextTool.SENTENCE_PER_LINE
        // نص مقسوم سلفًا على الأسطر — الأداة لا تغير شيئًا فلا تُرشَّح
        DrsTileContextAdvisor.advise("سطر\nجديد. بالفعل\nمقسوم") shouldNotContain
            DrsTextTool.SENTENCE_PER_LINE
        // نص بلا نهايات، ونهاية في آخر النص (لا نص بعدها)، والفارغ
        DrsTileContextAdvisor.advise("نص بلا نهايات هنا") shouldNotContain
            DrsTextTool.SENTENCE_PER_LINE
        DrsTileContextAdvisor.advise("جملة واحدة فقط. ") shouldNotContain
            DrsTextTool.SENTENCE_PER_LINE
        DrsTileContextAdvisor.advise("") shouldBe emptyList()
        // حرس المسار الساخن: فوق 256 يصمت
        val long = "جملة قصيرة. ".repeat(26)
        (long.length > 256) shouldBe true
        DrsTileContextAdvisor.advise(long) shouldNotContain DrsTextTool.SENTENCE_PER_LINE
    }

    test("الدمج لا يكسر عقود الإطلاق: الأسباب القديمة تظل وترتيبها ثابتًا مع 1.13") {
        // نص يضرب 1.13 وأسبابًا سابقة: الترتيب = ترتيب الأسباب، والسقف أربعة
        val picks = DrsTileContextAdvisor.advise("كتب الرسالة. ثم غادر. وعاد إلى العمل")
        picks shouldContain DrsTextTool.SENTENCE_PER_LINE
        // عربية عارية تظل ترشّح المشكِّل (السبب 3) بجانب 1.13
        DrsTileContextAdvisor.advise("كتب الرسالة. ثم غادر. وعاد") shouldContain
            DrsTextTool.TASHKEEL_TEXT
        // إشارات الكلمات الجديدة (حد الجملة/الترقيم) لا تسمّي أداة أبدًا —
        // توقّع الكلمات لا يختلس صوت البلاطات: صمت المستشار على الذيل الترقيمي
        DrsTileContextAdvisor.advise("نبدأ أولًا") shouldNotContain DrsTextTool.SENTENCE_PER_LINE
    }

    // ------------------------------------------------------------------
    // 7) جسر الشكلين: نهايات المحلل تعمل، وصف الأداة المغلق أضيق منها بحق
    // ------------------------------------------------------------------

    test("جسر الكتالوجين: كل نهاية (عدا الوقف «۔») توّقع حدّ الأداة الأفقي") {
        // الصنف الأدق للأداة: [. ! ؟ ? …] — «۔» نهاية جملة صحيحة لكن
        // أداة «جملة لكل سطر» عقدها المغلق أضيق — عدم تماثل موثق لا انجراف
        for (t in DrsSentenceContext.TERMINALS) {
            val fires = SENTENCE_BOUNDARY.containsMatchIn("س1${t} س2")
            if (t == '۔') {
                fires shouldBe false
            } else {
                fires shouldBe true
            }
        }
    }
})
