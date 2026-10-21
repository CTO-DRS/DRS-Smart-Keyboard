<div align="center">

# DRS Smart Keyboard v1.4.0 — الخصوصية المثبتة

**Proven Privacy — Fifth Release**

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
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة إصدار من 11 فحصًا
  (fail-closed) — وفي هذا الإصدار صارت الخصوصية مثبتة بالبناء لا بالاتفاق.
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط (8/8 VALID).

## جديد في 1.4.0 — الخصوصية المثبتة

قطع مبكر بقرار المالك لذراع L4 الرابعة من سلّم الذكاء
([SMART_FEATURES_ROADMAP](https://github.com/CTO-DRS/drs-sk-work/blob/main/docs/SMART_FEATURES_ROADMAP.md))
المكتملة تقنيًا في الدفعة 17 — وثيمة الإصدار من عندها: خصوصية لا تُوعَد بل
تُثبت بالبناء. كلها محلية خالصة بلا أي تنزيل سحابي:

- **المصنِّف النقي `DrsFieldKind` — نوع الحقل من خصائصه وحدها**: كتالوج
  عشرة أنواع مغلق (عام/نثر/بريد/روابط/رقم/تاريخ/هاتف/بحث/محمي/خام) يُصنَّف
  من `EditorInfo.inputType` وحده **بلا قراءة محتوى الحقل أبدًا**. عائلة
  كلمات المرور (النصي والمرئي وويب ورقم PIN) تسبق كل علم بما فيه تعدد
  الأسطر، والصنف المجهول أصمّ صادقًا لا مخمَّنًا، والحتمية كاملة: نفس
  المدخل نفس النوع دائمًا.
- **كتالوج الترتيب المغلق `DrsContextualRank` — «الترتيب لا الإطلاق»**:
  سبعة أنواع مرتبة بقوائم واقعية موثقة — البريد يفضح اللصق الصامت أولًا،
  والروابط تُقدَّم بزوج URL الأصلي، والهاتف بأرقام قابلة للاتصال بلا
  عائلات رمزية، والرقم بتوحيد آلي وقراءة وتفقيط، والتاريخ برباعي التواريخ
  المغلق، والنثر بمساعد الكتابة أولًا، والبحث بإزالة ضجيج المطابقة —
  وثلاثة أصمّة بحكم التصميم (محمي/خام/عام: الصمت هو الصدق). بلا عملية
  محرر ولا علامة إدراج ولا أداة معلومات في أي قائمة — مثبت بنيويًا —
  وسقف 6 اقتراحات بلمحة واحدة.
- **بوابة الخصوصية الحية `contentAdviceAllowed`**: حقول كلمات المرور لا
  تغذي مستشار المحتوى أبدًا **حتى لو كشفت نصها** (الحقول المرئية تفعل) —
  fail-closed بالبناء لا بالاتفاق.
- **دمج اللوحة بعقودها دون كسرها**: رقاقة «سياق الحقل» تظهر فقط حين
  للترتيب ما يقوله صادقًا، وصفوف غنية بنمط «الأكثر استخدامًا» مع بوابة
  isEnabled نفسها، والتراجع الصادق لـ«الكل» عند تغيّر الحقل، والميزة غائبة
  بلا ضجيج — والمستشاران من مصدرين مستقلين لا انجراف (المُرتب من صفة
  الحقل والمستشار من المحتوى).
- **المحاكاة قبل البناء** (عقيدة الدفعة المثبتة): `check_l4_fields.py` —
  **296 فحصًا أخضر** على 33 عينة inputType واقعية منها 11 محمية، ثم **18
  اختبار عقد** في DrsL4FieldContextTests.
- **الاختبارات 1995 → 2013 أخضر** وصفر صلاحيات جديدة وصفر أدوات جديدة —
  وصفر سلاسل خارج لوحة الأدوات.
- **وأين النطاق الأمني المخطط لـ«الخصوصية المثبتة»؟** إثبات حظر الشبكة
  الدائم (عدّادات TrafficStats الحية) واختبارات عقد الأذونات الموسعة
  وجولة MASVS الداخلية وتقرير الشفافية العدد الثاني تنقل صدقًا إلى موجة
  **v1.4.x** التالية (خارطة الطريق محدثة بذلك) — أما هذا الإصدار فيثبت
  الخصوصية من جهة الذكاء: تصنيف بلا محتوى، وحماية أصمّ بالبناء.

## التحديث من الإصدارات السابقة

- ✅ **من 1.3.0 إلى 1.4.0: تحديث موضعي مضمون** (نفس مفتاح التوقيع) —
  ثبّت فوق الإصدار الحالي مباشرة، وقاموسك ونسخك الاحتياطية كما هي.
- ✅ **من 1.2.0 فصاعدًا كل التحديثات الموضعية مضمونة** (نفس المفتاح
  لكل ما يلي 1.2.0).
- ⚠️ **مستخدمو 1.0.0/1.1.0 — تنبيه دوران المفتاح (جرى في 1.2.0)**:
  المفتاح السابق فقد وصوله مع ترحيل المستودع، فأعيد توليد مفتاح جديد
  **بنفس هوية الشهادة** (CN=DRS Smart Keyboard Release) — البصمة:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  التحديث الموضعي من 1.1.0 وما قبله غير متاح (أندرويد يرفض التحديث فوق
  بصمة مختلفة): صدّر قاموسك ونسخك الاحتياطية الموقّعة من الإصدار القديم،
  ألغِ تثبيته، ثم ثبّت 1.4.0 كتثبيت أول واستورد ما صدّرته.

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.4.0.apk` من مرفقات هذا الإصدار (أو من موقع
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

- **2,013 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابات أداء · فحص apksigner
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
- **On-device Arabic intelligence**: **13 fully local AI engines** — with a
  declared intelligence ladder (L0→L5) whose first four arms are complete: a
  standard-grade quantized corrector, sentence-aware prediction, local
  security, and **cross-field contextual intelligence that ranks tools by
  field type without ever reading content** — zero cloud dependencies and
  zero model downloads.
- **Proven privacy**: automated privacy canary + an 11-check release gate
  (fail-closed) — and in this release privacy is proven by construction,
  not promised in prose.
- **Package center**: ed25519-signed extensions verified before unpacking
  (8/8 VALID).

## New in 1.4.0 — Proven Privacy

An owner-decided early cut of arm L4 — the fourth rung of the intelligence
ladder ([SMART_FEATURES_ROADMAP](https://github.com/CTO-DRS/drs-sk-work/blob/main/docs/SMART_FEATURES_ROADMAP.md)),
technically complete in batch 17 — and the release takes its name from it:
privacy that is proven by construction, not promised. All purely local, no
cloud download:

- **The pure classifier `DrsFieldKind` — field type from attributes alone**:
  a closed catalog of ten kinds (general/prose/email/URL/number/date/phone/
  search/protected/raw) classified from `EditorInfo.inputType` alone,
  **never reading the field's content**. The password family (text, visible,
  web, numeric PIN) precedes every flag including multi-line, the unknown
  kind is honestly deaf rather than hidden, and it is fully deterministic:
  same input, same kind, always.
- **The closed ranking catalog `DrsContextualRank` — "ranking, not
  firing"**: seven ranked kinds with documented real-world lists — email
  exposes silent paste first, links lead with the original URL pair, phone
  offers dialable numbers without symbol families, numbers get
  normalization/reading/words, dates use the closed four-tool set, prose
  puts the writing assistant first, search strips match noise — and three
  kinds are deaf by design (protected/raw/general: silence is honesty). No
  editor operation, no cursor mark, no info tool in any list — proven
  structurally — and a cap of 6 suggestions at one glance.
- **The living privacy gate `contentAdviceAllowed`**: password fields never
  feed the content advisor **even when their text is visible** (visible
  fields do) — fail-closed by construction, not by agreement.
- **Panel integration without breaking its contracts**: the «Field context»
  chip appears only when the ranking honestly has something to say; rich
  rows reuse the "most used" pattern under the same isEnabled gate; the
  honest fallback to «All» when the field changes; the feature is absent
  without noise — and the two advisors come from two independent sources
  with no drift (the ranker from field attributes, the advisor from
  content).
- **Simulation before the build** (the proven batch doctrine):
  `check_l4_fields.py` — **296 green checks** over 33 real-world inputType
  samples (11 protected), then **18 contract tests** in
  DrsL4FieldContextTests.
- **Tests grew 1995 → 2013 green** with zero new permissions, zero new
  tools — and zero new strings outside the tools panel.
- **And where is the planned security scope of "Proven Privacy"?** The
  permanent network-block proof (live TrafficStats counters), extended
  permission-contract tests, the full internal MASVS pass and Transparency
  Report #2 move honestly to the next **v1.4.x** wave (the roadmap is
  updated accordingly). This release proves privacy on the intelligence
  side: classification without content, and protection that is deaf by
  construction.

## Updating from previous releases

- ✅ **1.3.0 → 1.4.0: guaranteed in-place update** (same signing key) —
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
  1.4.0 as a fresh install and import what you exported.

## Installation

1. Download `DRS-Smart-Keyboard-v1.4.0.apk` from this release's assets (or
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

- **2,013 unit tests green** · Kover coverage gate · performance budgets ·
  an independent apksigner check.
- All release assets are scheduled by fingerprint in the attached
  **SHA256SUMS.txt**.
- The full fingerprint table is appended below these notes (generated
  automatically by CI).

</div>
