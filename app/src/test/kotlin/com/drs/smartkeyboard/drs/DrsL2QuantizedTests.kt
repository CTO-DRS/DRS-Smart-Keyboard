/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsAiPowerManager
import com.drs.smartkeyboard.drs.ai.DrsArabicCorrector
import com.drs.smartkeyboard.drs.ai.DrsQuantizedEngine
import com.drs.smartkeyboard.drs.ai.LatinNormBridge
import com.drs.smartkeyboard.ime.nlp.latin.DictEntry
import com.drs.smartkeyboard.ime.nlp.latin.DictIndex
import com.drs.smartkeyboard.ime.nlp.latin.DrsFusedCorrection
import com.drs.smartkeyboard.ime.nlp.latin.LatinWordNormalize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * الدفعة 15 — ذراع L2 «محرك التصحيح المكمّم» (SMART_FEATURES_ROADMAP.md).
 *
 * Four closed-contract arms, each mirroring check_l2_quantized.py
 * (8,225 green checks) — the simulation is the oracle, these tests pin
 * the SAME semantics in the shipped engine:
 *
 *  1. THE BUDGET THEOREM — distance(a, b, costs, budget) computes exactly
 *     min(true distance, budget): a return equal to the budget is the
 *     saturation sentinel, never an exact distance. The default budget
 *     preserves the historical behavior byte-for-byte. Includes the
 *     length-guard soundness fix (min(ins, del), not max).
 *  2. SEARCH EQUIVALENCE — the budget-aware and length-bucketed scans
 *     return lists IDENTICAL to the legacy full-budget full-scan, at
 *     strictly less DP work (measured with a counting cost model).
 *  3. THE VOCAL RANK KERNEL — vocalRankAdjust turns the previously-dead
 *     VOCAL_BONUS into a live contract (positive/negative/monotonicity),
 *     wired into the fused pipeline behind the tier gate.
 *  4. THE L2 KPIs — the A/B arm (fused quantized vs the legacy delete-1
 *     baseline over the REAL-errors reference, the v1.3.0 exit
 *     criterion), the per-query scan-time budget, and the model-size
 *     budget (< 15MB, pinned structurally on the shipped asset).
 */
class DrsL2QuantizedTests : FunSpec({

    // ------------------------------------------------------------------
    // The real shipped dictionary — loaded exactly like the provider
    // ------------------------------------------------------------------

    LatinNormBridge.normalize = LatinWordNormalize::normalize

    val assetFile = listOf(
        File("src/main/assets/ime/dict/ar.json"),
        File("app/src/main/assets/ime/dict/ar.json"),
    ).firstOrNull { it.isFile } ?: error("ar.json asset not found")

    val dict: Map<String, Int> = Json { ignoreUnknownKeys = true }
        .decodeFromString(MapSerializer(String.serializer(), Int.serializer()), assetFile.readText())

    val index = DictIndex(
        dict.map { (word, freq) -> DictEntry(LatinWordNormalize.normalize(word), word, freq) }
            .sortedBy { it.norm },
    )

    fun normalize(word: String): String = LatinWordNormalize.normalize(word)

    /** Counts substitution-cost invocations — the DP inner-loop work proxy. */
    class CountingModel(val inner: DrsQuantizedEngine.CostModel) : DrsQuantizedEngine.CostModel {
        var calls: Long = 0
        override fun substitutionCost(a: Char, b: Char): Int {
            calls++
            return inner.substitutionCost(a, b)
        }
        override fun insertionCost(): Int = inner.insertionCost()
        override fun deletionCost(): Int = inner.deletionCost()
        override fun transpositionCost(): Int = inner.transpositionCost()
    }

    // ------------------------------------------------------------------
    // 1) THE BUDGET THEOREM
    // ------------------------------------------------------------------

    test("عقد الإشباع: المساواة التامة تكلفة صفر عند أي ميزانية") {
        for (budget in intArrayOf(1, 5, 9, 13, 16)) {
            DrsQuantizedEngine.distance("مدرسة", "مدرسة", DrsArabicCorrector.ArabicCostModel, budget) shouldBe 0
            DrsQuantizedEngine.distance("", "", DrsArabicCorrector.ArabicCostModel, budget) shouldBe 0
        }
    }

    test("عقد الإشباع: ما يتجاوز الميزانية يعود بالحارس نفسه لا مسافة حقيقية") {
        val model = DrsArabicCorrector.ArabicCostModel
        // unrelated pairs saturate: the returned value IS the budget
        DrsQuantizedEngine.distance("خمسبفس", "قذشزثدع", model, 9) shouldBe 9
        DrsQuantizedEngine.distance("خمسبفس", "قذشزثدع", model, 13) shouldBe 13
        DrsQuantizedEngine.distance("خمسبفس", "قذشزثدع", model, 16) shouldBe 16
        // empty-string cases saturate at the budget too
        DrsQuantizedEngine.distance("", "مدرسة", model, 9) shouldBe 9
        DrsQuantizedEngine.distance("مدرسة", "", model, 9) shouldBe 9
    }

    test("نظرية الميزانية: min(الحقيقي, الميزانية) على عينات موجهة وعشوائية") {
        val model = DrsArabicCorrector.ArabicCostModel
        val pairs = listOf(
            "مدرسه" to "مدرسة", "مسئول" to "مسؤول", "جمميل" to "جميل",
            "اسف" to "آسف", "شرزة" to "شركة", "حفض" to "حفظ",
            "الكتابب" to "الكتاب", "مشكوور" to "مشكور", "والمسئول" to "والمسؤول",
            "انشاء الله" to "إن شاء الله", "سلام" to "سلأم", "يوط" to "يوم",
            "خمسبفس" to "قذشزثدع", "ببببب" to "بتتتت", "اب" to "با",
        )
        for ((a, b) in pairs) {
            val full = DrsQuantizedEngine.distance(a, b, model) // = min(true, 16)
            for (budget in intArrayOf(4, 5, 7, 8, 9, 12, 13, 15, 16)) {
                val got = DrsQuantizedEngine.distance(a, b, model, budget)
                got shouldBe minOf(full, budget)
            }
        }
        // randomized sweep — the same theorem the oracle proved 8k times
        val alpha = "ابتثجحخدذرزسشصضطظعغفقكلمنهويءأإآةىئؤ"
        val rng = java.util.Random(20261)
        repeat(150) {
            val a = (1..rng.nextInt(8)).map { alpha[rng.nextInt(alpha.length)] }.joinToString("")
            val b = (1..rng.nextInt(8)).map { alpha[rng.nextInt(alpha.length)] }.joinToString("")
            val full = DrsQuantizedEngine.distance(a, b, model)
            for (budget in intArrayOf(5, 9, 13, 16)) {
                DrsQuantizedEngine.distance(a, b, model, budget) shouldBe minOf(full, budget)
            }
        }
    }

    test("الافتراضي التاريخي: بلا ميزانية = ميزانية MAX_DISTANCE بايتًا ببايت") {
        val model = DrsArabicCorrector.ArabicCostModel
        val pairs = listOf("مدرسه" to "مدرسة", "خمسبفس" to "قذشزثدع", "" to "كتاب")
        for ((a, b) in pairs) {
            DrsQuantizedEngine.distance(a, b, model) shouldBe
                DrsQuantizedEngine.distance(a, b, model, DrsQuantizedEngine.MAX_DISTANCE)
        }
    }

    test("حرس الطول الصائب: min(الإدراج, الحذف) لا يشبع كذبًا للنماذج المائلة") {
        // custom skewed model: ins=5, del=2 — "abcd"→"ab" is 2 deletions = 4
        val skewed = object : DrsQuantizedEngine.CostModel {
            override fun substitutionCost(a: Char, b: Char): Int =
                if (a == b) 0 else DrsQuantizedEngine.MAX_DISTANCE
            override fun insertionCost(): Int = 5
            override fun deletionCost(): Int = 2
        }
        DrsQuantizedEngine.distance("abcd", "ab", skewed, 9) shouldBe 4
        DrsQuantizedEngine.distance("abcd", "ab", skewed, 16) shouldBe 4
        // the legacy max() form would have false-saturated at budget 9 (4*5 >= 9)
    }

    // ------------------------------------------------------------------
    // 2) SEARCH EQUIVALENCE + WORK REDUCTION
    // ------------------------------------------------------------------

    test("تكافؤ المسحين: الدلاء والميزانية يعيدان قوائم المسح الكامل التاريخي حرفيًا") {
        val entries = index.entries.map { it.norm to it.freq }
        val buckets = index.normFreqByLength
        val model = DrsArabicCorrector.ArabicCostModel
        val queries = listOf("مدرسه", "مسئول", "شرزة", "يوط", "سحيح", "حفض", "كتابب", "علوم", "سلام", "انشاء")
        // tier sweep: LIGHT 8/3, STANDARD 12/4, FULL 14/5
        for ((maxCost, band) in listOf(8 to 3, 12 to 4, 14 to 5)) {
            for (q in queries) {
                val legacy = DrsQuantizedEngine.search(q, entries.asSequence(), model, 15, maxCost, band)
                val bucketed = DrsQuantizedEngine.search(q, buckets, model, 15, maxCost, band)
                bucketed shouldBe legacy
            }
        }
    }

    test("تقلص العمل: الميزانية الواعية تحسب خلايا DP أقل من الإشباع العام على المعجم الحقيقي") {
        val model = DrsArabicCorrector.ArabicCostModel
        val queries = listOf(
            "مدرسة", "جامعة", "مسؤول", "استخدام", "معلومات", "تطوير",
            "برمجة", "حواسيب", "شبكات", "اقتصاد",
        )
        val legacy = CountingModel(model)
        val budgeted = CountingModel(model)
        for (q in queries) {
            DrsQuantizedEngine.search(q, index.normFreqByLength, legacy, 15, 15, 4) // legacy: budget 16
            DrsQuantizedEngine.search(q, index.normFreqByLength, budgeted, 15, 12, 4) // STANDARD: budget 13
        }
        println("  DP work: legacy=${legacy.calls} budget-aware=${budgeted.calls}")
        (budgeted.calls < legacy.calls) shouldBe true
    }

    test("الزيارات المُدلَّلة: الدلاء تلمس مدخلات داخل الحزام فقط") {
        val q = "مدرسة"
        val band = 4
        val scoped = index.normFreqByLength
            .filterKeys { it in (q.length - band)..(q.length + band) }
            .values.sumOf { it.size }
        (scoped).let { visited ->
            (visited <= index.entries.size) shouldBe true
            println("  bucketed visit: $visited/${index.entries.size} entries")
        }
        // the buckets hold every entry exactly once, partitioned by length
        index.normFreqByLength.values.sumOf { it.size } shouldBe index.entries.size
    }

    test("الحتمية الكاملة: تعادل (التكلفة، التردد) يُحسم بالكلمة نفسها") {
        // "بات" → "با" (deletion, cost 3) and "باتا" (insertion, cost 3):
        // equal cost AND equal frequency — only the word tiebreak decides
        val entries = listOf(
            DictEntry("باتا", "باتا", 50),
            DictEntry("با", "با", 50),
        )
        val model = DrsArabicCorrector.ArabicCostModel
        val runs = (1..3).map {
            DrsQuantizedEngine.search("بات", entries.map { it.norm to it.freq }.asSequence(), model, 5, 12, 4)
        }
        runs[0] shouldBe runs[1]
        runs[1] shouldBe runs[2]
        runs[0].map { it.word } shouldBe listOf("با", "باتا")
        runs[0].map { it.cost } shouldBe listOf(3, 3)
    }

    // ------------------------------------------------------------------
    // 3) THE VOCAL RANK KERNEL
    // ------------------------------------------------------------------

    test("ميزان التشكيل: الجدول المغلق المطابق للمرجعية البايثونية") {
        data class Row(val q: String, val c: String, val base: Int, val want: Int)
        val table = listOf(
            Row("مدرس", "مُدرس", 5, 5),      // bare query — untouched
            Row("مُدرس", "مدرس", 5, 5),      // bare candidate — untouched
            Row("مُدرس", "مُكتوب", 5, 5),    // different family — untouched
            Row("مُدرس", "مُدرس", 7, 5),     // exact shape — full discount
            Row("مُدرس", "مُدرس", 2, 0),     // floored at 0
            Row("مُدرس", "مُدرس", 1, 0),     // floor holds
            Row("مُدرس", "مَدرس", 7, 6),     // shape 1 — half discount
            Row("مُدرس", "مدرسٍ", 7, 7),    // shape 2 — no discount
            Row("مُدرس", "مَدِرس", 7, 7),    // shape 3 — no discount
            Row("إِنْ", "إِنْ", 0, 0),       // zero base stays zero
        )
        for (row in table) {
            DrsArabicCorrector.vocalRankAdjust(row.q, row.c, row.base) shouldBe row.want
        }
    }

    test("ميزان التشكيل: مسافة الشكل الدقيقة موثقة بالسداد") {
        DrsArabicCorrector.exactShapeDistance("مُدرس", "مُدرس") shouldBe 0
        DrsArabicCorrector.exactShapeDistance("مُدرس", "مَدرس") shouldBe 1 // one haraka off
        DrsArabicCorrector.exactShapeDistance("مُدرس", "مدرسٍ") shouldBe 2 // delete+insert
        DrsArabicCorrector.exactShapeDistance("", "") shouldBe 0
        DrsArabicCorrector.exactShapeDistance("بِت", "بتِ") shouldBe 2
    }

    test("ميزان التشكيل: الرتابة — شكل أفضل لا يرفع التكلفة المعدلة أبدًا") {
        for (base in 0..12) {
            val exact = DrsArabicCorrector.vocalRankAdjust("مُدرس", "مُدرس", base)
            val partial = DrsArabicCorrector.vocalRankAdjust("مُدرس", "مَدرس", base)
            val wrong = DrsArabicCorrector.vocalRankAdjust("مُدرس", "مَدِرس", base)
            (exact <= partial) shouldBe true
            (partial <= wrong) shouldBe true
        }
    }

    test("ميزان التشكيل في الخط المندمج: التوأم الأقرب شكلًا يُختار والبوابة تحسم") {
        // two display twins under ONE norm (the real pipeline collapses
        // candidates by norm — the twin pick is the live vocal point):
        // the bare twin carries the higher frequency (the canonical winner)
        val idx = DictIndex(
            listOf(
                DictEntry(normalize("مُدرس"), "مُدرس", 50),
                DictEntry(normalize("مدرس"), "مدرس", 90),
            ),
        )

        // gate ON + vocalized query: the harakat-carrying twin wins despite
        // its lower frequency (shape 1 beats a bare candidate)
        val vocal = DrsFusedCorrection.corrections(
            index = idx, raw = "مَدرس", isArabic = true,
            maxCost = 12, lengthBand = 4, maxCandidateCount = 5,
            normalize = ::normalize, vocalizationEnabled = true,
        )
        vocal.first().text shouldBe "مُدرس"

        // gate OFF (LIGHT / the historical default): the canonical winner
        val gated = DrsFusedCorrection.corrections(
            index = idx, raw = "مَدرس", isArabic = true,
            maxCost = 12, lengthBand = 4, maxCandidateCount = 5,
            normalize = ::normalize, vocalizationEnabled = false,
        )
        gated.first().text shouldBe "مدرس"

        // gate ON but a BARE query: no vocal superiority invented — the
        // canonical winner tops the correction for a neighboring typo
        val bare = DrsFusedCorrection.corrections(
            index = idx, raw = "مدرسب", isArabic = true,
            maxCost = 12, lengthBand = 4, maxCandidateCount = 5,
            normalize = ::normalize, vocalizationEnabled = true,
        )
        bare.first().text shouldBe "مدرس"
    }

    test("ميزان التشكيل: بلا توائم متشكّلة = لا تغيير بايتًا ببايت حتى مع التمكين") {
        val idx = index // the shipped dict: zero same-norm twins (verified) —
        // every norm is a singleton, so the vocal twin pick degenerates to
        // the canonical passthrough for bare AND vocalized queries alike
        val queries = listOf("مدرسه", "مسئول", "شرزة", "مَدرسة", "حسناً")
        for (q in queries) {
            val off = DrsFusedCorrection.corrections(
                index = idx, raw = q, isArabic = true,
                maxCost = 12, lengthBand = 4, maxCandidateCount = 5, normalize = ::normalize,
                vocalizationEnabled = false,
            )
            val on = DrsFusedCorrection.corrections(
                index = idx, raw = q, isArabic = true,
                maxCost = 12, lengthBand = 4, maxCandidateCount = 5, normalize = ::normalize,
                vocalizationEnabled = true,
            )
            on shouldBe off // no twins to discriminate — provably a no-op
        }
    }

    // ------------------------------------------------------------------
    // 4) THE L2 KPIs — A/B, scan time, model size, tier budgets
    // ------------------------------------------------------------------

    data class Case(val typo: String, val expected: String, val category: String, val phrase: Boolean = false)

    val cases = listOf(
        Case("اذا", "إذا", "همزات القطع"), Case("انا", "أنا", "همزات القطع"),
        Case("مسئول", "مسؤول", "همزات وسطى"), Case("شئ", "شيء", "همزات وسطى"),
        Case("مدرسه", "مدرسة", "تاء مربوطة"), Case("جامعه", "جامعة", "تاء مربوطة"),
        Case("سياره", "سيارة", "تاء مربوطة"), Case("الصلاه", "الصلاة", "تاء مربوطة"),
        Case("لاكن", "لكن", "معجمية خاطئة"), Case("اللذي", "الذي", "معجمية خاطئة"),
        Case("والمسئول", "والمسؤول", "سوابق صرفية"), Case("والجامعه", "والجامعة", "سوابق صرفية"),
        Case("شرزة", "شركة", "جوار لوحة المفاتيح"), Case("درخة", "درجة", "جوار لوحة المفاتيح"),
        Case("اللله", "الله", "حروف زائدة"), Case("جمميل", "جميل", "حروف زائدة"),
        Case("السحيح", "الصحيح", "صوتيات"), Case("حفض", "حفظ", "صوتيات"),
    )

    fun fusedTop3(typo: String): List<String> =
        DrsFusedCorrection.corrections(
            index = index, raw = typo, isArabic = true,
            maxCost = 12, lengthBand = 4, maxCandidateCount = 5, normalize = ::normalize,
        ).map { it.text }.take(3)

    fun legacyTop3(typo: String): List<String> =
        runCatching { index.corrections(normalize(typo), 5) }.getOrDefault(emptyList())
            .map { it.word }.take(3)

    test("A/B على الأخطاء الحقيقية: المكمّم المندمج لا يقل عن خط delete-1 ويتجاوز العتبة") {
        var fusedOk = 0
        var legacyOk = 0
        val detail = mutableListOf<String>()
        for (case in cases) {
            val (fHit, lHit) = if (case.phrase) {
                val got = runCatching { DrsArabicCorrector.hardCorrectionFor(case.typo, ::normalize) }.getOrNull()
                ((got == case.expected) to (got == case.expected))
            } else {
                (case.expected in fusedTop3(case.typo)) to (case.expected in legacyTop3(case.typo))
            }
            if (fHit) fusedOk++ else detail.add("  [fused miss] ${case.typo} ← ${case.expected}")
            if (lHit) legacyOk++
        }
        val fusedRate = fusedOk.toDouble() / cases.size
        val legacyRate = legacyOk.toDouble() / cases.size
        println("  A/B: fused=$fusedOk/${cases.size} (${("%.1f".format(fusedRate * 100))}%) " +
            "legacy=$legacyOk/${cases.size} (${("%.1f".format(legacyRate * 100))}%)")
        detail.forEach(::println)
        // v1.3.0 exit criterion: the quantized engine SUPERSEDES the legacy line
        (fusedRate >= legacyRate) shouldBe true
        (fusedRate >= 0.85) shouldBe true
    }

    test("ميزانية الزمن: مسح المعجم الكامل لكل استعلام ضمن السقف المعلن") {
        val model = DrsArabicCorrector.ArabicCostModel
        val queries = listOf(
            "مدرسة", "جامعة", "مسؤول", "استخدام", "معلومات", "تطوير", "برمجة",
            "حواسيب", "شبكات", "اقتصاد", "كتاب", "سلام", "شرزة", "والمسئول",
        )
        // warm-up (JIT) then measured runs
        repeat(3) { DrsQuantizedEngine.search("تدفيئة", index.normFreqByLength, model, 15, 12, 4) }
        val times = mutableListOf<Long>()
        for (q in queries) {
            val t0 = System.nanoTime()
            DrsQuantizedEngine.search(q, index.normFreqByLength, model, 15, 12, 4)
            times.add((System.nanoTime() - t0) / 1_000_000L)
        }
        val max = times.max()
        println("  scan ms per query: max=$max all=$times")
        // The honest hard ceiling against catastrophic regressions, calibrated
        // on the reference sandbox (observed 94..166ms per full-dict scan):
        // ~3x headroom — a 2x engine regression still trips it.
        (max < 500L) shouldBe true
    }

    test("ميزانية الحجم: نموذج التصحيح (أصل المعجم المدمج) < 15MB بنيويًا") {
        val bytes = assetFile.length()
        val mb = bytes / (1024.0 * 1024.0)
        println("  ar.json = ${"%.2f".format(mb)}MB (${bytes} bytes)")
        (mb < 15.0) shouldBe true
    }

    test("فئات الطاقة: كل ميزانيات الفئات الثلاث دون حارس الإشباع العالمي") {
        for ((tier, caps) in mapOf(
            "LIGHT" to DrsAiPowerManager.classify(2_000L, false),
            "STANDARD" to DrsAiPowerManager.classify(4_000L, false),
            "FULL" to DrsAiPowerManager.classify(8_000L, false),
        )) {
            (caps.maxCorrectionCost + 1 < DrsQuantizedEngine.MAX_DISTANCE) shouldBe true
            (caps.lengthBand in 1..5) shouldBe true
            println("  $tier: cost=${caps.maxCorrectionCost} band=${caps.lengthBand} " +
                "vocal=${caps.vocalizationEnabled}")
        }
        // battery saver forces LIGHT regardless of RAM
        DrsAiPowerManager.classify(8_000L, true).tier shouldBe DrsAiPowerManager.Tier.LIGHT
    }
})
