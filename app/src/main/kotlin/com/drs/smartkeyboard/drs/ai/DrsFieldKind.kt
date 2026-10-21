/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ai

/**
 * ذراع L4 «الذكاء السياقي بين الحقول» — كتالوج أنواع الحقول المغلق.
 *
 * This is the PURE field-kind classifier: it reads the raw
 * `EditorInfo.inputType` int and NOTHING else — never the content, never
 * the package, never a screenshot of context. One int in, one closed kind
 * out, fully deterministic. The doctrine matches [DrsSentenceContext]:
 * a pure JVM type with zero Android imports (the InputType masks are
 * mirrored below as documented literal constants) so every contract is
 * unit-testable without a device.
 *
 * الخصوصية قبل الذكاء — دائمًا: [PROTECTED] (عائلة كلمات المرور كلها:
 * النصي والمرئي وويب ورقم PIN) و[RAW] (TYPE_NULL والصنف المجهول)
 * «أصمّان» بنيويًا: لا ترتيب أدوات عليهما ولا قراءة محتوى لأجلهما.
 * الحماية لا تنكسر بأي علم إضافي (multiline أو غيره) — عائلة كلمات
 * المرور تسبق كل شيء في سلسلة القرار.
 *
 * [GENERAL] الصمت الصادق: حقل نص عادي لا يرشَّح له شيء — لا تخمين
 * من نوع الحقل حين لا يعرف الحقل عن نفسه شيئًا يستحق إعادة ترتيب.
 */
enum class DrsFieldKind {

    /** حقل نص عادي (أحادي السطر بلا تخصيص) — الصمت هو الصدق هنا. */
    GENERAL,

    /** نثر: رسائل قصيرة وطويلة وحقول متعددة الأسطر — أدوات الفقرة. */
    PROSE,

    /** بريد إلكتروني (نصي أو ويب) — التنظيف الصامت قبل الإرسال. */
    EMAIL,

    /** روابط: شريط عنوان المتصفح وحقول URI — زوج URL أولًا. */
    URI,

    /** رقم (عادي/عشري/موقّع) — الأرقام الآلية والتفقيط. */
    NUMBER,

    /** تاريخ/وقت/تاريخ-وقت — رباعي التواريخ المغلق. */
    DATETIME,

    /** هاتف — أرقام قابلة للاتصال بلا عائلات رمزية. */
    PHONE,

    /** حقل ترشيح/بحث (variation FILTER) — التطبيع قبل البحث. */
    SEARCH,

    /**
     * عائلة كلمات المرور (نصي 0x80 / مرئي 0x90 / ويب 0xE0 / رقم PIN)
     * — أصمّ تمامًا: الخصوصية قبل الذكاء دائمًا.
     */
    PROTECTED,

    /**
     * إدخال خام (TYPE_NULL) أو صنف مجهول — أصمّ أيضًا: ما لا يُعرف
     * عنه شيء لا يُخمَّن عنه شيء.
     */
    RAW;

    /** عائلة الحماية الصمّاء (PROTECTED + RAW) — لا ذكاء عليهما إطلاقًا. */
    val isProtected: Boolean
        get() = this == PROTECTED || this == RAW

    companion object {

        // مرآة أقنعة android.text.InputType — ثوابت حرفية موثقة
        // (قيم AOSP المستقرة منذ API 1) ليبقى النوع نقيًا بلا استيراد Android.
        private const val MASK_CLASS = 0x0000000F
        private const val MASK_VARIATION = 0x00000FF0
        private const val CLASS_NULL = 0x0
        private const val CLASS_TEXT = 0x1
        private const val CLASS_NUMBER = 0x2
        private const val CLASS_PHONE = 0x3
        private const val CLASS_DATETIME = 0x4

        private const val V_TEXT_URI = 0x10
        private const val V_TEXT_EMAIL = 0x20
        private const val V_TEXT_SHORT_MESSAGE = 0x40
        private const val V_TEXT_LONG_MESSAGE = 0x50
        private const val V_TEXT_PASSWORD = 0x80
        private const val V_TEXT_VISIBLE_PASSWORD = 0x90
        private const val V_TEXT_WEB_EDIT_TEXT = 0xA0
        private const val V_TEXT_FILTER = 0xB0
        private const val V_TEXT_WEB_EMAIL = 0xD0
        private const val V_TEXT_WEB_PASSWORD = 0xE0
        private const val V_NUMBER_PASSWORD = 0x10

        private const val FLAG_MULTI_LINE = 0x00040000

        /**
         * التصنيف الحتمي من inputType الخام وحده — int واحد، نوع واحد
         * مغلق. عائلة كلمات المرور تسبق كل شيء، ثم التخصصات، ثم علم
         * تعدد الأسطر، ثم الصمت الصادق.
         */
        fun from(rawInputType: Int): DrsFieldKind {
            val cls = rawInputType and MASK_CLASS
            val variation = rawInputType and MASK_VARIATION
            return when (cls) {
                CLASS_NULL -> RAW
                CLASS_PHONE -> PHONE
                CLASS_DATETIME -> DATETIME
                CLASS_NUMBER -> if (variation == V_NUMBER_PASSWORD) PROTECTED else NUMBER
                CLASS_TEXT -> when {
                    // عائلة الحماية أولًا — لا علم يكسرها (multiline مثبت بالمحاكاة)
                    variation == V_TEXT_PASSWORD ||
                        variation == V_TEXT_VISIBLE_PASSWORD ||
                        variation == V_TEXT_WEB_PASSWORD -> PROTECTED
                    variation == V_TEXT_EMAIL || variation == V_TEXT_WEB_EMAIL -> EMAIL
                    variation == V_TEXT_URI -> URI
                    variation == V_TEXT_FILTER -> SEARCH
                    variation == V_TEXT_SHORT_MESSAGE ||
                        variation == V_TEXT_LONG_MESSAGE -> PROSE
                    rawInputType and FLAG_MULTI_LINE != 0 -> PROSE
                    else -> GENERAL
                }
                // صنف مجهول — أصم صادقًا لا مخمَّن
                else -> RAW
            }
        }
    }
}
