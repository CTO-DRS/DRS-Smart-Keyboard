<div align="center">

# DRS Smart Keyboard v1.1.0 — الشفافية والتعلم

**Transparency & Learning — Second Release**

<img src="https://raw.githubusercontent.com/CTO-DRS/drs-sk-work/main/docs/images/hero.png" width="100%"/>

</div>

<div dir="rtl">

## نبذة عن المشروع

**DRS Smart Keyboard** لوحة مفاتيح أندرويد، مبنية بـ Kotlin وJetpack Compose
وMaterial 3، صُممت **عربية أولًا**: واجهة عربية كاملة بدعم RTL أصيل، تخطيطات
عربية مدروسة، واقتراحات وتصحيح عربي — مع إنجليزية كخيار ثانٍ كامل. كل ما
تكتبه يبقى على جهازك: لا حسابات، لا تتبع، لا شبكة إجبارية.

## ما في هذا الإصدار

- **النواة الكاملة**: تخطيطات عربية/إنجليزية وثماني لغات قواميس، شريط ذكي
  قابل للتخصيص، لوحة مهام، لوحة حركات، إيموجي بحث، تصحيح تلقائي حقيقي،
  ومدقق إملائي.
- **الحافظة الذكية**: محرر نصوص بستة عشر خوارزمية، كاشف أسطر برمجية، أقسام
  تقويمية وشارات فئات، ونافذة تعديل منبثقة فوق التطبيقات.
- **الذكاء العربي على الجهاز**: **13 محرك ذكاء اصطناعي محلي بالكامل** للتحليل
  والاقتراح والتصحيح — صفر تبعيات سحابية.
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة إصدار من 11 فحصًا
  (fail-closed).
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط (8/8 VALID).

## جديد في 1.1.0 — الشفافية والتعلم (ADR-0009)

- **مركز الشفافية الحي**: عدّاد بايتات حي لهذا التطبيق وحده + سجل اتصالات
  يومي بدوران 90 يوماً + شهادة عمر قابلة للمشاركة (تُرسم برمجياً وتُصدَّر
  PNG) — الإثبات من بيانات جهازك لا من وعود مكتوبة.
- **«ماذا تعلمت عني»**: قاموسك الشخصي المرئي بالبحث والفرز، وعدّادات تصحيح
  تبدأ «من الآن» بصدق، وتصدير موقَّع ed25519 واستيراد يقبل الموقَّع فقط.
- **لوحة الحركات المنبثقة**: ضغط مطول على الحرف العربي ينبثق لوحة الحركات
  الثماني (U+064B..U+0652) والشدة محطة رئيسة — عبر محرك المنبثقة القائم
  نفسه، والإفلات على الأصل حرف مجرد والإفلات خارجها إلغاء.
- **صفر صلاحيات جديدة**: فرق صلاحيات الـManifest منذ 1.0.0 فارغ بالدليل،
  والاختبارات نمت 1780 → 1844 أخضر.

## التحديث من الإصدارات السابقة

- ✅ **مستخدمو 1.0.0 يحدّثون موضعياً مباشرة (versionCode 2 > 1)** — ثبّت
  الحزمة الجديدة فوق القديمة من داخل التطبيق أو من صفحة الإصدارات، دون إلغاء
  تثبيت ودون فقدان بيانات.
- ⚠️ **مستخدمو v2.x أو أقدم**: التحديث الموضعي لا يزال مستحيلاً تقنياً
  (أندرويد يرفض تنزيل `versionCode` أقدم) — ألغِ التثبيت أولاً، ثم ثبّت 1.1.0
  كتثبيت أول.

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.1.0.apk` من مرفقات هذا الإصدار (أو من موقع
   المشروع).
2. تحقق من السلامة: قارن بصمة SHA-256 للملف مع ما في مرفق **SHA256SUMS.txt**
   (أو الجدول أسفل هذه الملاحظات).
3. **مستخدمو 1.0.0**: ثبّت فوق الحالي مباشرة (التحديث الموضعي مضمون) ·
   **مستخدمو v2.x**: ألغِ التثبيت أولاً (التنبيه أعلاه) — ثم ثبّت الحزمة
   الجديدة كتثبيت أول.
4. فعّل «التثبيت من مصادر غير معروفة» لمتصفحك أو مدير ملفاتك عند الطلب، ثم أكمل
   التثبيت وفعّل لوحة المفاتيح من إعدادات النظام.

## بيان الامتثال للمصادر المفتوحة

يستند هذا المشروع إلى مكونين مشتقين بموجب رخصة **Apache-2.0** مع إسناد حرفي
كامل في الترويسات والوثائق:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

السجل الكامل للإفصاح والجداول: `THIRD_PARTY_NOTICES.md` في جذر المستودع.

## التحقق

- **1,844 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابات أداء · فحص apksigner
  مستقل.
- كل مرفقات هذا الإصدار مجدولة بصماتها في ملف **SHA256SUMS.txt** المرفق.
- جدول البصمات الكامل ملحق أسفل هذه الملاحظات (مولّد آليًا بواسطة CI).

</div>

<div align="center">

**— English —**

</div>

<div dir="ltr">

## About the project

**DRS Smart Keyboard** is an Android keyboard built with Kotlin, Jetpack
Compose and Material 3, designed **Arabic-first**: a fully Arabic interface
with native RTL support, carefully crafted Arabic layouts, and Arabic
suggestion/correction — with English as a complete second option. Everything
you type stays on your device: no accounts, no tracking, no mandatory network.

## What's in this release

- **Complete core**: Arabic/English layouts and eight dictionary languages,
  customizable smart strip, toolbar, clipboard manager, gestures, emoji search,
  real autocorrect, and spell checking.
- **Smart clipboard**: a text editor with sixteen algorithms, code-line
  detection, calendar sections and category badges, and a popup edit window.
- **On-device Arabic intelligence**: **13 fully local AI engines** for
  analysis, suggestion and correction — zero cloud dependencies.
- **Proven privacy**: automated privacy canary + an 11-check release gate
  (fail-closed).
- **Package center**: ed25519-signed extensions verified before unpacking
  (8/8 VALID).

## New in 1.1.0 — Transparency & Learning (ADR-0009)

- **Live Transparency Hub**: a live byte counter for this app alone + a daily
  connection log with 90-day rotation + a shareable age certificate (drawn
  programmatically, exported as PNG) — proof from your device's own data, not
  written promises.
- **"What I learned about you"**: your personal visual dictionary with search
  and sorting, honest correction counters that start "from now", and ed25519
  signed export with import that accepts signed files only.
- **Popup Harakat panel**: long-pressing an Arabic letter pops the panel of
  the eight diacritics (U+064B..U+0652) with the shadda as a primary stop —
  through the existing popup engine itself; releasing on the origin yields a
  bare letter, releasing outside cancels.
- **Zero new permissions**: the Manifest permission diff since 1.0.0 is empty
  by proof, and tests grew 1780 → 1844 green.

## Updating from previous releases

- ✅ **1.0.0 users update in place directly (versionCode 2 > 1)** — install
  the new package over the old one from inside the app or the releases page,
  with no uninstall and no data loss.
- ⚠️ **v2.x or older users**: in-place update remains technically impossible
  (Android refuses a lower `versionCode`) — uninstall first, then install 1.1.0
  as a fresh install.

## Installation

1. Download `DRS-Smart-Keyboard-v1.1.0.apk` from this release's assets (or
   from the project website).
2. Verify integrity: compare the file's SHA-256 against the attached
   **SHA256SUMS.txt** (or the table below).
3. **1.0.0 users**: install directly over the current version (in-place update
   guaranteed) · **v2.x users**: uninstall first (see note above) — then
   install the new package as a fresh install.
4. Enable "Install from unknown sources" for your browser or file manager when
   prompted, finish the install, and enable the keyboard in system settings.

## Open-source compliance statement

This project builds upon two derived components under the **Apache-2.0**
license with full literal attribution in headers and docs:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

Full disclosure record and tables: `THIRD_PARTY_NOTICES.md` at the repository
root.

## Verification

- **1,844 green unit tests** · Kover coverage gate · performance gates ·
  independent apksigner inspection.
- All release assets are listed with their fingerprints in the attached
  **SHA256SUMS.txt**.
- The complete hash table is appended below these notes (auto-generated by
  CI).

</div>
