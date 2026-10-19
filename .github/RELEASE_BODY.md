<div align="center">

# DRS Smart Keyboard v1.2.0 — أبجديات الآلة والحساب

**Machine Alphabets & Calculation — Third Release**

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
- **لوحة الأدوات النصية — 82 أداة**: تحويل الحالة والخطوط، ترتيب الأسطر
  وترقيمها، تفقيط الأرقام والمبالغ والتواريخ والأوقات والكسور والهجري،
  تنظيف وتطبيع، وأبجديات الآلة (URL وBase64 ومورس وHex) وحاسبة ومحوّل
  وحدات — كلها حتمية على الجهاز بعقد «لا تخمين ولا تلف».
- **الذكاء العربي على الجهاز**: **13 محرك ذكاء اصطناعي محلي بالكامل** للتحليل
  والاقتراح والتصحيح — صفر تبعيات سحابية.
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة إصدار من 11 فحصًا
  (fail-closed).
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط (8/8 VALID).

## جديد في 1.2.0 — أبجديات الآلة والحساب

- **أربعة أزواج في «أبجديات الآلة»** تنقل النص بين عالمي الكتابة والبايتات
  بعقد واحد لا يخمّن ولا يتلف: ترميز/فك الروابط (URL)، ترميز/فك Base64
  (كلمات السر ومفاتيح API تُغلّف بأمان)، ترميز/فك مورس (الجدول الرسمي
  ITU-R M.1677-1 للعربية واللاتينية — SOS تنطق سخس)، وترميز/فك Hex
  (سرد بايتات UTF-8 أزواجًا سداسية مقروءة).
- **الزوج الحسابي الأول**: حاسبة تعبيرات مكتوبة يدويًا (تنازلي متكرر لا
  eval — أولويات وأقواس ونسبة مئوية وأرقام عربية) تنطق الناتج نقيًا بلا
  ذيل عشري (0.1+0.2 = 0.3)، ومحوّل وحدات بسبع فئات مغلقة (طول/كتلة/
  حجم/مساحة/سرعة/بيانات/زمن) و183 مرادفًا عربيًا وإنجليزيًا، بمعادلات
  حرارية حقيقية لا معاملات.
- **تنظيف لصق الويب**: منظّف Markdown وHTML يحتفظان بالنثر ويسقطان
  الزخرفة، فاحص نص قراءة فقط (حروف/أرقام/كلمات/جمل/علامات خفية)،
  حاصد روابط سطرًا لكل رابط، ومهدّ تمدد الحروف (هلااااا → هلا).
- **ذكاء البلاطات يكبر بذكاء**: كل زوج فكّ جديد يُقترن بسبب مغلق بعقد
  الإطلاق المضمون — كتلة سليمة وحدها تُرشَّح، وما لا يُفكّ لا يُرشَّح:
  توصية بنتيجة مضمونة لا إشارة عامة.
- **الاختبارات 1844 → 1950 أخضر** وصفر صلاحيات جديدة — فرق صلاحيات
  الـManifest منذ 1.1.0 فارغ بالدليل.

## التحديث من الإصدارات السابقة — تنبيه دوران المفتاح

- ⚠️ **دوران مفتاح التوقيع في 1.2.0**: المفتاح السابق فقد وصوله مع ترحيل
  المستودع، فأعيد توليد مفتاح جديد **بنفس هوية الشهادة**
  (CN=DRS Smart Keyboard Release) — البصمة الجديدة:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
- ❗ **لذلك التحديث الموضعي من 1.1.0 غير متاح هذه المرة** (أندرويد يرفض
  التحديث فوق بصمة مختلفة): ألغِ تثبيت 1.1.0 أولًا ثم ثبّت 1.2.0 كتثبيت
  أول. قاموسك المستورد ونسخك الاحتياطية الموقّعة ed25519 تعمل في 1.2.0
  كالعادة — صدّرها من 1.1.0 قبل الإلغاء واستوردها بعده.
- ✅ **من 1.2.0 فصاعدًا تعود التحديثات الموضعية المضمونة** (نفس المفتاح
  لكل ما يلي 1.2.0).

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.2.0.apk` من مرفقات هذا الإصدار (أو من موقع
   المشروع).
2. تحقق من السلامة: قارن بصمة SHA-256 للملف مع ما في مرفق **SHA256SUMS.txt**
   (أو الجدول أسفل هذه الملاحظات).
3. **مستخدمو 1.0.0/1.1.0 — تنبيه دوران المفتاح**: ألغِ التثبيت أولًا
   (البصمة الجديدة أعلاه) — وصدّر قاموسك ونسخك الاحتياطية من 1.1.0 قبل
   الإلغاء ثم استوردها بعد التثبيت · **مستخدمو v2.x**: ألغِ التثبيت أولاً
   ثم ثبّت الحزمة الجديدة كتثبيت أول.
4. فعّل «التثبيت من مصادر غير معروفة» لمتصفحك أو مدير ملفاتك عند الطلب، ثم أكمل
   التثبيت وفعّل لوحة المفاتيح من إعدادات النظام.

## بيان الامتثال للمصادر المفتوحة

يستند هذا المشروع إلى مكونين مشتقين بموجب رخصة **Apache-2.0** مع إسناد حرفي
كامل في الترويسات والوثائق:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

السجل الكامل للإفصاح والجداول: `THIRD_PARTY_NOTICES.md` في جذر المستودع.

## التحقق

- **1,950 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابات أداء · فحص apksigner
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
- **Text tools panel — 82 tools**: case and letter transforms, line sorting
  and numbering, number/amount/date/time/fraction/Hijri words, cleanup and
  normalization, machine alphabets (URL, Base64, Morse, Hex), a calculator
  and unit converter — all deterministic on-device under the "never guess,
  never corrupt" contract.
- **On-device Arabic intelligence**: **13 fully local AI engines** for
  analysis, suggestion and correction — zero cloud dependencies.
- **Proven privacy**: automated privacy canary + an 11-check release gate
  (fail-closed).
- **Package center**: ed25519-signed extensions verified before unpacking
  (8/8 VALID).

## New in 1.2.0 — Machine Alphabets & Calculation

- **Four "machine alphabets" pairs** move text between the writing world and
  the byte world under one contract — never guess, never corrupt: URL
  encode/decode, Base64 encode/decode (passwords and API keys travel safely),
  Morse encode/decode (the official ITU-R M.1677-1 table for Arabic and Latin
  — SOS speaks سخس), and Hex encode/decode (a readable UTF-8 byte dump in
  hex pairs).
- **The first calculation pair**: a hand-written expression parser
  (recursive-descent, no eval — precedence, parens, percent, Eastern Arabic
  digits) with clean output and no decimal tail (0.1+0.2 = 0.3), and a unit
  converter with seven closed categories (length/mass/volume/area/speed/
  data/time) and 183 unique Arabic/English spellings — real thermal formulas,
  not ratios.
- **Web-paste cleanup**: Markdown and HTML cleaners keep the prose and drop
  the costume, a read-only text inspector (letters/digits/words/sentences/
  hidden marks), a link harvester (one link per line), and a letter-stretch
  softer (هلااااا → هلا).
- **Smarter tiles, honestly**: every new decode pair adds a closed reason
  under a guaranteed firing contract — a clean block alone gets suggested;
  what cannot be decoded is never suggested: a recommendation with a
  guaranteed result, not a generic hint.
- **Tests grew 1844 → 1950 green** with zero new permissions — the Manifest
  permission diff since 1.1.0 is empty by proof.

## Updating from previous releases — key rotation notice

- ⚠️ **Key rotation in 1.2.0**: the previous signing key's access was lost
  with the repository migration, so a new key was generated **with the same
  certificate identity** (CN=DRS Smart Keyboard Release) — new fingerprint:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
- ❗ **In-place update from 1.1.0 is therefore unavailable this time**
  (Android refuses updates across different signatures): uninstall 1.1.0
  first, then install 1.2.0 as a fresh install. Your imported dictionary and
  ed25519-signed backups keep working in 1.2.0 — export them from 1.1.0
  before uninstalling and import after installing.
- ✅ **From 1.2.0 onward, guaranteed in-place updates return** (same key for
  everything after 1.2.0).

## Installation

1. Download `DRS-Smart-Keyboard-v1.2.0.apk` from this release's assets (or
   from the project website).
2. Verify integrity: compare the file's SHA-256 against the attached
   **SHA256SUMS.txt** (or the table below).
3. **1.0.0/1.1.0 users — key rotation notice**: uninstall first (new
   fingerprint above) — export your dictionary and backups from 1.1.0
   before uninstalling, and import after installing · **v2.x users**:
   uninstall first, then install the new package as a fresh install.
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

- **1,950 green unit tests** · Kover coverage gate · performance gates ·
  independent apksigner inspection.
- All release assets are listed with their fingerprints in the attached
  **SHA256SUMS.txt**.
- The complete hash table is appended below these notes (auto-generated by
  CI).

</div>
