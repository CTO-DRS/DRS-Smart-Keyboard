<div align="center">

# DRS Smart Keyboard v1.4.1 — الوجه الأمني

**The Security Face — Sixth Release**

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
  بسلم ذكاء معلن (L0→L5) أُكملت أذرعه الأربعة الأولى: تصحيح مكمّم بمعيار
  المستوى، توقع يقرأ الجملة، أمان محلي، **وذكاء سياقي بين الحقول يرتب
  الأدوات حسب نوع الحقل دون قراءة محتواه أبدًا** — صفر تبعيات سحابية
  وصفر تنزيل نموذج.
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة MASVS الداخلية
  الجديدة (34 فحصًا) + بوابة إصدار من 11 فحصًا (fail-closed) — **والآن
  الإثبات حي: عدّادات شبكة يقطرها التطبيق أمام المستخدم كل ثانية**.
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط (8/8 VALID).

## جديد في 1.4.1 — الوجه الأمني (موجة v1.4.x كاملة)

تنفيذ موجة v1.4.x الأمنية كاملةً — النطاق الأمني الذي نُقل صدقًا من
«الخصوصية المثبتة» عند قطعها المبكر لذراع L4. أربعة مقطوعات:

- **الوضع المطلق بإثبات حيّ — `DrsLiveProofEngine` + لوحة 1 Hz**: لوحة
  الخصوصية لم تعد تقيس مرة واحدة عند الفتح؛ عدّاد يقطر **كل ثانية أثناء
  مشاهدة الشاشة** يعرض الصادر والوارد منذ بدء الجلسة، و«شريط صفر بايت»
  يكبر مع الصمت (0:00 → 12:34 → …). الصدق بنيويًا: الحكم ثلاثي — صفر
  مُثبت فقط عند دلتا صفرية على عدّادات مدعومة، أرقام صادقة عند أي
  حركة، وإعلان صريح عندما لا يدعم الروم العدّادات؛ وشريط الصمت ينقطع
  مع أي حركة **أو** مع إعادة ضبط عدّادات النظام — لا صمت متظاهر.
  القارئ واحد للإثباتين (اللحظي والحي) — لا انفراج ممكن.
- **اختبارات عقد الأذونات الموسعة — `DrsSecurityWaveTests` (28 اختبارًا)**:
  العقد اتسع من فحص الأذونات الخمسة إلى **السطح الكامل للمانيفست**:
  قائمة المصدَّرات المغلقة (خمسة مكوّنات بالضبط: خدمة IME ومدقق الإملائي
  بأذون ربط المنصة، نشاطا الإعداد/الاستيراد، والنسخ للحافظة)، إلزامية
  التصريح exported لكل ما له intent-filter، كل المزودات غير مصدَّرة،
  رباعي النسخ/الاستخراج/الشبكة في وسم application، حظر `<profileable>`
  (لا تنميط قشري للعملية الإنتاجية)، نطاق queries المغلق، والمنع الصريح
  لثمانية أذونات متكررة تاريخيًا + QUERY_ALL_PACKAGES — إضافة إلى مصفوفة
  القفل المطلق كاملة (الأسطح الأربعة × الحالتان + فشل مغلق + إجبار
  المُعرِّف على دون اتصال).
- **جولة MASVS الداخلية الأولى — `docs/MASVS_AUDIT.md` + بوابة CI**:
  معيار OWASP MASVS v2.0 بجميع مجموعاته الثمانية (التخزين، التشفير،
  المصادقة، الشبكة، المنصة، الكود، المرونة، الخصوصية) — النتيجة: **15
  بنودًا مطابقة، 5 غير منطبقة مُبرَّرة، 1 جزئي موثق، لا فاشل**. الجزء
  الآلي منها بوابة بناء جديدة `ci/masvs_audit.sh` (34 فحصًا) تعمل في
  CI كوظيفة «MASVS internal audit» بجانب privacy canary — أي انحراف
  مستقبلي يُسقط البناء قبل أن يصل للمستخدم.
- **تقرير الشفافية العدد الثاني**: أرقام الفترة v1.0.0 → v1.4.x بلا
  تعديل رجعي على العدد الأول: طلبات خارجية صفر، الأذون الخمسة بلا
  تغيير، إثبات حي جديد، وحدود صراحة معلنة.
- **الاختبارات 2013 → 2,041 أخضر** وصفر صلاحيات جديدة وصفر أدوات جديدة
  — و4 سلاسل خصوصية جديدة (عربية وإنجليزية) في لوحة الخصوصية حصرًا.

## التحديث من الإصدارات السابقة

- ✅ **من 1.4.0 إلى 1.4.1: تحديث موضعي مضمون** (نفس مفتاح التوقيع) —
  ثبّت فوق الإصدار الحالي مباشرة، وقاموسك ونسخك الاحتياطية كما هي.
- ✅ **من 1.2.0 فصاعدًا كل التحديثات الموضعية مضمونة** (نفس المفتاح
  لكل ما يلي 1.2.0).
- ⚠️ **مستخدمو 1.0.0/1.1.0 — تنبيه دوران المفتاح (جرى في 1.2.0)**:
  المفتاح السابق فقد وصوله مع ترحيل المستودع، فأعيد توليد مفتاح جديد
  **بنفس هوية الشهادة** (CN=DRS Smart Keyboard Release) — البصمة:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  التحديث الموضعي من 1.1.0 وما قبله غير متاح (أندرويد يرفض التحديث فوق
  بصمة مختلفة): صدّر قاموسك ونسخك الاحتياطية الموقّعة من الإصدار القديم،
  ألغِ تثبيته، ثم ثبّت 1.4.1 كتثبيت أول واستورد ما صدّرته.

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.4.1.apk` من مرفقات هذا الإصدار (أو من موقع
   المشروع).
2. تحقق من السلامة: قارن بصمة SHA-256 للملف مع ما في مرفق **SHA256SUMS.txt**
   (أو الجدول أسفل هذه الملاحظات).
3. **مستخدمو 1.2.0 وما بعده**: ثبّت فوق الإصدار مباشرة (تحديث موضعي مضمون) ·
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

- **2,041 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابة MASVS الداخلية
  الجديدة · بوابات أداء · فحص apksigner مستقل.
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
- **On-device Arabic intelligence**: **13 fully local AI engines** — with a
  declared intelligence ladder (L0→L5) whose first four arms are complete: a
  standard-grade quantized corrector, sentence-aware prediction, local
  security, and **cross-field contextual intelligence that ranks tools by
  field type without ever reading content** — zero cloud dependencies and
  zero model downloads.
- **Proven privacy**: automated privacy canary + the new internal MASVS
  gate (34 checks) + an 11-check release gate (fail-closed) — **and the
  proof is now live: network counters the app ticks in front of you every
  second**.
- **Package center**: ed25519-signed extensions verified before unpacking
  (8/8 VALID).

## New in 1.4.1 — The Security Face (the full v1.4.x wave)

The v1.4.x security wave executed in full — the security scope moved
honestly out of "Proven Privacy" when that release cut arm L4 early.
Four deliverables:

- **Absolute mode with a live proof — `DrsLiveProofEngine` + a 1 Hz panel**:
  the privacy dashboard no longer measures once at open; a counter ticks
  **every second while the screen is visible**, showing bytes sent and
  received since session start, plus a growing "zero-byte streak"
  (0:00 → 12:34 → …). Honesty by construction: a three-state verdict —
  proven zero only with a zero delta on supported counters, honest numbers
  on any movement, and an explicit unsupported state when the ROM cannot
  prove anything; the streak also restarts when the OS resets its counters —
  no faked unbroken silence. One sampler feeds both proofs (snapshot and
  live) — no divergence is possible.
- **Expanded permission-contract tests — `DrsSecurityWaveTests` (28 tests)**:
  the contract widened from the five-permission check to the **full
  manifest surface**: a closed exported-components allowlist (exactly five:
  the IME and spell-checker services with their platform binding
  permissions, the settings/import activities, and the copy-to-clipboard
  activity), mandatory explicit `exported` for every intent-filter
  component, all providers unexported, the backup/extraction/network
  quartet on the application tag, a `<profileable>` ban (no shell
  profiling of the production process), the closed `queries` scope, and an
  explicit ban on eight historically recurring permissions + QUERY_ALL_PACKAGES —
  plus the full absolute-mode gate matrix (all four surfaces × both states +
  fail-closed provider + on-device-only recognizer coercion).
- **First full internal MASVS pass — `docs/MASVS_AUDIT.md` + a CI gate**:
  OWASP MASVS v2.0 across all eight groups (storage, crypto, auth, network,
  platform, code, resilience, privacy) — **15 controls compliant, 5 not
  applicable (justified), 1 partial (documented), none failed**. Its
  machine-checkable slice is a new build gate `ci/masvs_audit.sh`
  (34 checks) running in CI as the "MASVS internal audit" job beside the
  privacy canary — any future deviation fails the build.
- **Transparency Report #2**: real numbers for the v1.0.0 → v1.4.x period
  with no retroactive edits to issue #1: zero third-party requests, the
  five permissions unchanged, the new live proof, and stated limitations.
- **Tests grew 2013 → 2,041 green** with zero new permissions, zero new
  tools — and 4 new privacy strings (Arabic + English) confined to the
  privacy panel.

## Updating from previous releases

- ✅ **1.4.0 → 1.4.1: guaranteed in-place update** (same signing key) —
  install directly over the current version; your dictionary and signed
  backups stay as they are.
- ✅ **From 1.2.0 onward, all in-place updates are guaranteed** (same key
  for everything after 1.2.0).
- ⚠️ **1.0.0/1.1.0 users — key rotation notice (happened in 1.2.0)**:
  the previous signing key's access was lost with the repository migration,
  so a new key was generated **with the same certificate identity**
  (CN=DRS Smart Keyboard Release) — fingerprint:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  In-place update from 1.1.0 and earlier is unavailable (Android refuses
  updates across different signatures): export your dictionary and
  ed25519-signed backups from the old release, uninstall it, then install
  1.4.1 as a fresh install and import what you exported.

## Installation

1. Download `DRS-Smart-Keyboard-v1.4.1.apk` from this release's assets (or
   from the project website).
2. Verify integrity: compare the file's SHA-256 against the attached
   **SHA256SUMS.txt** (or the table below).
3. **1.2.0+ users**: install directly over the current version (guaranteed
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

- **2,041 unit tests green** · Kover coverage gate · the new internal MASVS
  gate · performance budgets · an independent apksigner check.
- All release assets are scheduled by fingerprint in the attached
  **SHA256SUMS.txt**.
- The full fingerprint table is appended below these notes (generated
  automatically by CI).

</div>
