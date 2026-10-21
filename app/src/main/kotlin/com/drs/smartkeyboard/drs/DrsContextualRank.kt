/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsFieldKind

/**
 * ذراع L4 «الذكاء السياقي بين الحقول» — مرتِّب الأدوات حسب نوع الحقل.
 *
 * The FIELD side of L4 (the CONTENT side stays [DrsTileContextAdvisor]'s
 * — نص قبل المؤشر يرشّح، وهنا صفات الحقل ترتّب). Both engines are pure
 * and deterministic; they never overlap in source of truth: one reads
 * the caret's tail, the other reads `EditorInfo.inputType` and nothing
 * else. The ranking does not fire anything — **الترتيب لا الإطلاق**:
 * it reorders visibility inside the panel, and every tool keeps its own
 * honest no-op contract (the closed grammar refuses what it does not
 * understand) exactly like the recents and favorites rows beside it.
 *
 * العقيدة المغلقة:
 *  1. كتالوج مغلق لكل نوع — لا نتيجة تقريبية ولا vibes: كل قائمة
 *     موثقة بأسباب واقعية للحقل، بترتيب ثابت حتمي.
 *  2. الصمت الصادق: [DrsFieldKind.GENERAL] لا ترشيح له — حقل لا يعرف
 *     عن نفسه شيئًا يستحق إعادة ترتيب يبقى كما هو بلا تخمين.
 *  3. الخصوصية قبل الذكاء: [DrsFieldKind.PROTECTED] و[DrsFieldKind.RAW]
 *     أصمّان بنيويًا — قائمتا الترتيب فارغتان دائمًا، و[contentAdviceAllowed]
 *     يرفض تغذية مستشار المحتوى من حقول كلمات المرور حتى لو كشفت نصها
 *     (الحقول المرئية تفعل!) — fail-closed بالبناء لا بالاتفاق.
 *  4. تحويلات فقط: لا أداة معلومات ولا عملية محرر ولا علامة إدراج
 *     في أي قائمة (مثبت بنيويًا في الاختبارات).
 */
object DrsContextualRank {

    /** سقف الترتيب الظاهر — قائمة بلمحة واحدة. */
    const val MAX_RANKED = 6

    /**
     * القائمة المغلقة المرتبة لنوع الحقل — حتمية كلية (نفس النوع =
     * نفس القائمة بترتيبها نفسه، بلا اعتماد على أي حالة زيارة).
     */
    fun rankedTools(kind: DrsFieldKind): List<DrsTextTool> = when (kind) {
        // الخصوصية قبل الذكاء — أصمّ بنيويًا لا بالاتفاق:
        DrsFieldKind.PROTECTED, DrsFieldKind.RAW -> emptyList()
        // الصمت الصادق — لا تخمين من حقل عادي:
        DrsFieldKind.GENERAL -> emptyList()

        // البريد: فضح اللصق الصامت أولًا (محارف عرضية تكسر العنوان)
        // ثم نظافة المسافات، ثم شكل «البريد يكتب صغيرًا»، ثم نظافة
        // الرموز، وترميز رابط mailto يختم — ترتيب واقعي موثق.
        DrsFieldKind.EMAIL -> listOf(
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.LOWERCASE,
            DrsTextTool.STRIP_EMOJI,
            DrsTextTool.URL_ENCODE,
        )

        // الروابط: زوج URL أولًا (هذا حقل الروابط — أداتهما الأصلية)،
        // ثم فضح اللصق الصامت والمسافات، ونظافة الرموز تختم (الرابط
        // لا يحمل إيموجي) — كل أداة تحتفظ بعقد اللا-op الخاص بها.
        DrsFieldKind.URI -> listOf(
            DrsTextTool.URL_ENCODE,
            DrsTextTool.URL_DECODE,
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.STRIP_EMOJI,
        )

        // الهاتف: رقم قابل للاتصال أولًا (الأرقام الشرقية تُوحَّد
        // آليًا للاتصال)، ثم اللصق الصامت، ثم إزالة المسافات كلها —
        // الحقل يمضي للمودم لا للعين.
        DrsFieldKind.PHONE -> listOf(
            DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTool.REMOVE_ZERO_WIDTH,
            DrsTextTool.REMOVE_ALL_SPACES,
        )

        // الرقم: توحيد آلي غربي أولًا، ثم قراءة العدد كلماتًا (عقد
        // لازمي: أداة معلومات بلس تحويل — لا، NUMBER_WORDS تحويل نقي
        // يعيد النص كما هو إن لم ينطق)، والتفقيط للمبالغ يختم.
        DrsFieldKind.NUMBER -> listOf(
            DrsTextTool.TO_WESTERN_DIGITS,
            DrsTextTool.NUMBER_WORDS,
            DrsTextTool.TAFQIT,
        )

        // التاريخ/الوقت: رباعي التواريخ المغلق بترتيب التكرار الواقعي —
        // نطق التاريخ ثم الوقت ثم اليوم، والتحويل الهجري باتجاهيه.
        DrsFieldKind.DATETIME -> listOf(
            DrsTextTool.DATE_WORDS,
            DrsTextTool.TIME_WORDS,
            DrsTextTool.WEEKDAY,
            DrsTextTool.GREGORIAN_TO_HIJRI,
            DrsTextTool.HIJRI_TO_GREGORIAN,
        )

        // النثر: مساعد الكتابة أولًا (عقده المضمون يحمي من الضجيج)،
        // ثم عائلة التنظيف الفقرية المألوفة، وتقسيم الجمل الملصوقة
        // يختم — كلها أدوات الفقرة لا أدوات الحقل الآلي.
        DrsFieldKind.PROSE -> listOf(
            DrsTextTool.IMPROVE_PHRASES,
            DrsTextTool.TO_ARABIC_PUNCTUATION,
            DrsTextTool.REMOVE_LETTER_STRETCH,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.SENTENCE_PER_LINE,
        )

        // البحث/الترشيح: إزالة الضجيج الذي يكسر المطابقة — التشكيل
        // والتطويل أولًا، ثم المسافات، ثم التطبيع العربي الشامل.
        DrsFieldKind.SEARCH -> listOf(
            DrsTextTool.REMOVE_DIACRITICS,
            DrsTextTool.REMOVE_TATWEEL,
            DrsTextTool.TRIM_SPACES,
            DrsTextTool.NORMALIZE_ARABIC,
        )
    }.take(MAX_RANKED)

    /**
     * بوابة الخصوصية الحية لمستشار المحتوى: حقول كلمات المرور لا
     * تغذي [DrsTileContextAdvisor] أبدًا — حتى لو كشف الحقل نصه
     * (الحقول المرئية تفعل) — fail-closed بالبناء. كل الأنواع الأخرى
     * تبقى على عقد المحتوى المجرَّب دون تغيير.
     */
    fun contentAdviceAllowed(kind: DrsFieldKind): Boolean = !kind.isProtected
}
