<div align="center">

# DRS Smart Keyboard v1.0.0 — الإطلاق الأول الرسمي

**First Official Release — First Launch**

<img src="https://raw.githubusercontent.com/CTO-DRS/DRS-SmartKeyboard/main/docs/images/hero.png" width="100%"/>

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

## ⚠️ تحذير إجباري لمستخدمي الإصدارات القديمة (v2.x)

**يجب إلغاء تثبيت الإصدار القديم أولاً قبل تثبيت 1.0.0.**

هذا الإطلاق الرسمي يعيد ضبط ترقيم الإصدار من الصفر: `versionCode` عاد إلى
**1**، والتحديث الموضعي فوق أي إصدار قديم (v2.x أو أقدم) **مستحيل تقنياً** —
أندرويد يرفض تنزيل `versionCode`. حاول التثبيت فوق القديم وسيُرفض التثبيت.

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.0.0.apk` من مرفقات هذا الإصدار (أو من موقع
   المشروع).
2. تحقق من السلامة: قارن بصمة SHA-256 للملف مع ما في مرفق **SHA256SUMS.txt**
   (أو الجدول أسفل هذه الملاحظات).
3. **مستخدمو v2.x**: ألغِ التثبيت أولاً (التحذير أعلاه) — ثم ثبّت الحزمة الجديدة
   كتثبيت أول.
4. فعّل «التثبيت من مصادر غير معروفة» لمتصفحك أو مدير ملفاتك عند الطلب، ثم أكمل
   التثبيت وفعّل لوحة المفاتيح من إعدادات النظام.

## بيان الامتثال للمصادر المفتوحة

يستند هذا المشروع إلى مكونين مشتقين بموجب رخصة **Apache-2.0** مع إسناد حرفي
كامل في الترويسات والوثائق:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

السجل الكامل للإفصاح والجداول: `THIRD_PARTY_NOTICES.md` في جذر المستودع.

## التحقق

- **1,780 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابات أداء · فحص apksigner
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

## ⚠️ Mandatory warning for users of previous versions (v2.x)

**You must uninstall the old version first before installing 1.0.0.**

This official launch resets version numbering from scratch: `versionCode`
is back to **1**, and an in-place update over any older release (v2.x or
earlier) is **technically impossible** — Android refuses downgrades by
versionCode.

## Installation

1. Download `DRS-Smart-Keyboard-v1.0.0.apk` from this release's assets (or
   from the project website).
2. Verify integrity: compare the file's SHA-256 against the attached
   **SHA256SUMS.txt** (or the table below).
3. **v2.x users**: uninstall first (see warning above) — then install the new
   package as a fresh install.
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

- **1,780 green unit tests** · Kover coverage gate · performance gates ·
  independent apksigner inspection.
- All release assets are listed with their fingerprints in the attached
  **SHA256SUMS.txt**.
- The complete hash table is appended below these notes (auto-generated by
  CI).

</div>
