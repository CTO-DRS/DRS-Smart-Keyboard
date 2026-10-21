<div align="center">

# DRS Smart Keyboard v1.3.0 — الذكاء على الجهاز

**On-Device Intelligence — Fourth Release**

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
- **لوحة الأدوات النصية — 84 أداة**: تحويل الحالة والخطوط، ترتيب الأسطر
  وترقيمها، تفقيط الأرقام والمبالغ والتواريخ والأوقات والكسور والهجري،
  تنظيف وتطبيع، أبجديات الآلة (URL وBase64 ومورس وHex)، حاسبة ومحوّل
  وحدات، وزوج الأمان (قيس قوة كلمة المرور ومولّدها) — كلها حتمية على
  الجهاز بعقد «لا تخمين ولا تلف».
- **الذكاء العربي على الجهاز**: **13 محرك ذكاء اصطناعي محلي بالكامل** —
  والآن بسلم ذكاء معلن (L0→L5): تصحيح مكمّم بمعيار المستوى، توقع يقرأ
  الجملة، وأمان محلي — صفر تبعيات سحابية وصفر تنزيل نموذج.
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة إصدار من 11 فحصًا
  (fail-closed).
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط (8/8 VALID).

## جديد في 1.3.0 — الذكاء على الجهاز

أول قطع رسمي لمرحلة الذكاء من [خارطة الطريق](https://github.com/CTO-DRS/drs-sk-work/blob/main/docs/ROADMAP.md)
— ثلاثة أذرع مكتملة من سلّم الذكاء، كلها محلية خالصة بلا أي تنزيل سحابي:

- **ذراع L1 — الأمان المحلي (أداتان جديدتان، الكتالوج 82 → 84)**:
  «قيس قوة كلمة المرور» يقيس بالإنتروبيا الرياضية الخالصة (الطول ×
  log2 لأصناف المحارف الحاضرة) ويحكم بخمس درجات — وأنماط ضعيفة مغلقة
  (تكرار، تسلسل لوحة مفاتيح، شكل سنة، كلمات شائعة) **تخفض الحكم إلى
  ضعيف مهما كانت الأرقام** («مرحبا12345» يقيس 52 بت ويقرأ ضعيفًا).
  و«مولّد كلمة المرور» يدرج 16 محرفًا من أبجدية 68 بلا متشابهات (l1IO0o)
  بـSecureRandom وصنف مضمون من كل فئة — نحو 97 بت — **وبلا ترشيح آلي
  عمدًا**: جيل بضغطة واعية لا تخمينًا. حقول كلمات المرور ترفض السطح
  كله: لا شيء يغادر الجهاز.
- **ذراع L2 — محرك التصحيح المكمّم عند معيار المستوى**: الميزانية
  المكمَّمة الواعية (distance يحسب min(الحقيقي، الميزانية) بإشباع عند
  ميزانية المستدعي — نظرية min-plus مثبتة بالمحاكاة على 8,225 فحصًا)،
  والمسح المُدلَّل بالطول الحتمي كليًا (دلو أطوال مُسبق البناء + روابط
  كسر تعادل ثلاثية)، وميزان التشكيل الحي (خصم كامل للشكل المطابق ونصف
  لعائق واحد — بعائلة الكلمة نفسها فقط). **معيار خروج الإصدار مثبت**:
  اختبار A/B على مرجعية أخطاء كتابة حقيقية — المكمّم المندمج **18/18
  (100%)** مقابل 8/18 (44%) لخط delete-1 القديم. الميزانيات مثبتة
  بنيويًا: نموذج التصحيح **0.81MB** (< 15MB) ومسح كامل المعجم **94–166ms**
  دون سقف انحدار 500ms.
- **ذراع L3 — توقع الكلمة التالية الأعمق**: التوقع يقرأ الجملة لا الكلمة
  فقط. **حد الجملة الحارس** (`. ! ؟ ? … ۔` كتالوج مغلق) يمنع عبور نهاية
  الجملة إلى سياق الجملة التالية، **ومسار البداية الجديدة** يقترح من أعلى
  مفردات المستخدم وكتالوج افتتاحيات مغلق (14 بداية عربية) بثقة حتمية،
  **وسلسلة الترقيم** أولًا→ثانيًا→…→ثامنًا تُحقن تتمتها رأس الشريط بثقة
  0.9 (اقتراح فقط — نقرة المستخدم وحدها الإطلاق)، وحد الجملة يسكتها —
  جملة انتهت بعدّ لا تستمر تخمينًا.
- **ذكاء البلاطات يكبر بذكاء**: سببان مغلقان جديدان بعقد الإطلاق
  المضمون — 1.12 «الأمان الرقمي» (موضوع بصنفين فأكثر يُرشَّح لقيس قوة
  كلمة المرور) و1.13 «الجمل الملتصقة» (حد جملة واضح وحده يُرشَّح لجملة
  لكل سطر) — مع حرسَي المسار الساخن والالتباس، والإشارة الجُملية لا
  تسمّي أداة أبدًا: إشارة واحدة مستخدمان لا انجراف.
- **الاختبارات 1950 → 1995 أخضر** وصفر صلاحيات جديدة — فرق صلاحيات
  الـManifest منذ 1.2.0 فارغ بالدليل.

## التحديث من الإصدارات السابقة

- ✅ **من 1.2.0 إلى 1.3.0: تحديث موضعي مضمون** (نفس مفتاح التوقيع) —
  ثبّت فوق الإصدار الحالي مباشرة، وقاموسك ونسخك الاحتياطية كما هي.
- ⚠️ **مستخدمو 1.0.0/1.1.0 — تنبيه دوران المفتاح (جرى في 1.2.0)**:
  المفتاح السابق فقد وصوله مع ترحيل المستودع، فأعيد توليد مفتاح جديد
  **بنفس هوية الشهادة** (CN=DRS Smart Keyboard Release) — البصمة:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  التحديث الموضعي من 1.1.0 وما قبله غير متاح (أندرويد يرفض التحديث فوق
  بصمة مختلفة): صدّر قاموسك ونسخك الاحتياطية الموقّعة من الإصدار القديم،
  ألغِ تثبيته، ثم ثبّت 1.3.0 كتثبيت أول واستورد ما صدّرته.
- ✅ **من 1.2.0 فصاعدًا كل التحديثات الموضعية مضمونة** (نفس المفتاح
  لكل ما يلي 1.2.0).

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.3.0.apk` من مرفقات هذا الإصدار (أو من موقع
   المشروع).
2. تحقق من السلامة: قارن بصمة SHA-256 للملف مع ما في مرفق **SHA256SUMS.txt**
   (أو الجدول أسفل هذه الملاحظات).
3. **مستخدمو 1.2.0**: ثبّت فوق الإصدار مباشرة (تحديث موضعي مضمون) ·
   **مستخدمو 1.0.0/1.1.0**: ألغِ التثبيت أولًا (البصمة أعلاه) وصدّر
   قاموسك ونسخك الاحتياطية قبل الإلغاء ثم استوردها بعد التثبيت ·
   **مستخدمو v2.x (فلوريس بورد المشتق)**: ألغِ التثبيت أولًا ثم ثبّت
   الحزمة الجديدة كتثبيت أول.
4. فعّل «التثبيت من مصادر غير معروفة» لمتصفحك أو مدير ملفاتك عند الطلب، ثم أكمل
   التثبيت وفعّل لوحة المفاتيح من إعدادات النظام.

## بيان الامتثال للمصادر المفتوحة

يستند هذا المشروع إلى مكونين مشتقين بموجب رخصة **Apache-2.0** مع إسناد حرفي
كامل في الترويسات والوثائق:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

السجل الكامل للإفصاح والجداول: `THIRD_PARTY_NOTICES.md` في جذر المستودع.

## التحقق

- **1,995 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابات أداء · فحص apksigner
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
- **Text tools panel — 84 tools**: case and letter transforms, line sorting
  and numbering, number/amount/date/time/fraction/Hijri words, cleanup and
  normalization, machine alphabets (URL, Base64, Morse, Hex), a calculator
  and unit converter, and the security pair (password strength meter and
  generator) — all deterministic on-device under the "never guess,
  never corrupt" contract.
- **On-device Arabic intelligence**: **13 fully local AI engines** — now
  with a declared intelligence ladder (L0→L5): a standard-grade quantized
  corrector, sentence-aware prediction, and local security — zero cloud
  dependencies and zero model downloads.
- **Proven privacy**: automated privacy canary + an 11-check release gate
  (fail-closed).
- **Package center**: ed25519-signed extensions verified before unpacking
  (8/8 VALID).

## New in 1.3.0 — On-Device Intelligence

The first official cut of the roadmap's intelligence phase — three completed
arms of the intelligence ladder, all purely local with no cloud download:

- **Arm L1 — Local security (two new tools, catalog 82 → 84)**: the
  «Password strength meter» measures with pure mathematical entropy
  (length × log2 of present character classes) and judges on five grades —
  closed weak patterns (repeats, keyboard runs, year shapes, common words)
  **downgrade the verdict to weak regardless of the numbers** («مرحبا12345»
  measures 52 bits yet reads weak). The «Password generator» inserts 16
  characters from a 68-symbol alphabet without look-alikes (l1IO0o) using
  SecureRandom with a guaranteed class per category — about 97 bits —
  **deliberately with no automatic filtering**: a conscious tap generates,
  no guessing. Password fields reject the whole surface: nothing leaves
  the device.
- **Arm L2 — The quantized corrector at standard grade**: the
  budget-aware quantized distance (computes min(true, budget) with
  saturation at the caller's budget — a min-plus theorem proven by
  simulation over 8,225 checks), the fully deterministic length-bucketed
  scan (prebuilt length buckets + three-way tie-breaking), and the living
  vocalization scale (full discount for an exact shape match, half for one
  obstacle — same word family only). **The release exit gate is proven**:
  an A/B test on a real-typo benchmark — the integrated quantizer scores
  **18/18 (100%)** versus 8/18 (44%) for the old delete-1 baseline. The
  declared budgets are structural: correction model **0.81MB** (< 15MB)
  and a full-dictionary scan of **94–166ms** under the 500ms regression
  ceiling.
- **Arm L3 — Deeper next-word prediction**: prediction now reads the
  sentence, not just the word. The **sentence-boundary guard** (a closed
  catalog `. ! ؟ ? … ۔`) keeps end-of-sentence noise from crossing into
  the next sentence's context; a **new-sentence start path** suggests from
  the user's top words and a closed openings catalog (14 Arabic sentence
  starters) with deterministic confidence; the **numbering chain**
  أولًا→ثانيًا→…→ثامنًا injects its deterministic continuation at the top
  of the strip (confidence 0.9 — a suggestion only; the user's tap is the
  only trigger), and the sentence boundary silences it — a sentence that
  ended with a count does not keep guessing.
- **Smarter tiles, honestly**: two new closed reasons under the guaranteed
  firing contract — 1.12 «Digital security» (a subject with two or more
  classes suggests the strength meter) and 1.13 «Glued sentences» (a clear
  sentence boundary alone suggests one sentence per line) — with hot-path
  and ambiguity guards, and the sentence signal never names a tool: one
  signal, two uses, no drift.
- **Tests grew 1950 → 1995 green** with zero new permissions — the Manifest
  permission diff since 1.2.0 is empty by proof.

## Updating from previous releases

- ✅ **1.2.0 → 1.3.0: guaranteed in-place update** (same signing key) —
  install directly over the current version; your dictionary and signed
  backups stay as they are.
- ⚠️ **1.0.0/1.1.0 users — key rotation notice (happened in 1.2.0)**:
  the previous signing key's access was lost with the repository migration,
  so a new key was generated **with the same certificate identity**
  (CN=DRS Smart Keyboard Release) — fingerprint:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  In-place update from 1.1.0 and earlier is unavailable (Android refuses
  updates across different signatures): export your dictionary and
  ed25519-signed backups from the old release, uninstall it, then install
  1.3.0 as a fresh install and import what you exported.
- ✅ **From 1.2.0 onward, all in-place updates are guaranteed** (same key
  for everything after 1.2.0).

## Installation

1. Download `DRS-Smart-Keyboard-v1.3.0.apk` from this release's assets (or
   from the project website).
2. Verify integrity: compare the file's SHA-256 against the attached
   **SHA256SUMS.txt** (or the table below).
3. **1.2.0 users**: install directly over the current version (guaranteed
   in-place update) · **1.0.0/1.1.0 users**: uninstall first (fingerprint
   above) — export your dictionary and backups before uninstalling, and
   import after installing · **v2.x users (the derived FlorisBoard)**:
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

- **1,995 unit tests green** · Kover coverage gate · performance budgets ·
  an independent apksigner check.
- All release assets are scheduled by fingerprint in the attached
  **SHA256SUMS.txt**.
- The full fingerprint table is appended below these notes (generated
  automatically by CI).

</div>
