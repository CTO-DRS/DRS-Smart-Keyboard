<div align="center">

# DRS Smart Keyboard v1.4.2 — مطبخ الإيموجي والحزم المستلمة

**Emoji Kitchen & Received Packages — Seventh Release**

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
  بسلم ذكاء معلن (L0→L5) أُكملت أذرعه الأربعة الأولى — صفر تبعيات
  سحابية وصفر تنزيل نموذج.
- **اللوحة السابعة — مطبخ الإيموجي (DRS Emoji Kitchen)**: تركيب إيموجي
  أصلية 100% على الجهاز (لا أصول Google ولا واجهات خاصة ولا شبكة) بمحرك
  وصفات مغلق، ومكتبة ملصقات أصلية مرخّصة Apache-2.0، وإرسال متوافق
  بالواجهة العامة commitContent — والإرسال على جهازك الحقيقي هو الاختبار
  الذي بيدك أنت.
- **الخصوصية الإثباتية**: privacy canary آلي + بوابة MASVS الداخلية
  (34 فحصًا) + بوابة إصدار من 11 فحصًا (fail-closed) — والإثبات حي:
  عدّادات شبكة يقطرها التطبيق أمام المستخدم كل ثانية.
- **مركز الحزم**: امتدادات موقّعة ed25519 مع تحقق قبل فك الضغط — **وفي
  هذا الإصدار صارت الثمانية الرسمية تُثبَّت وتُستورد فعليًا** (عقد اختبار
  على الحزم الحقيقية نفسها: 9/9 أخضر).

## جديد في 1.4.2 — ثلاث دفعات

### 1) مطبخ الإيموجي — اللوحة السابعة الذكية (دفعة 19)

- **المحرك الخالص `DrsEmojiKitchen`**: 8 فئات مغلقة و48 مشاركًا (بكلمات
  عربية وإنجليزية) و13 وصفة تخطيط على 11 موضع مرساة بمقاييس محصورة واهتزاز
  زاوي حتمي — نفس الزوج نفس الخطة بايت-بايت، والزوج بلا وصفة يظهر صمتًا
  صادقًا بلا صورة وهمية أبدًا، والبحث العربي مُطبَّع (تشكيل/همزات/تاء
  مربوطة).
- **مكتبة الملصقات**: حزمة «أصليات مطبخ درز» v1.0.0 — 12 ملصقًا متجهيًا من
  تأليف المشروع حصرًا برخصة Apache-2.0 الموثقة في الفهرس نفسه، بستة
  تصنيفات وبحث ومفضلة وسجل (سقف 24).
- **الإرسال المتوافق**: الواجهة العامة الموثقة commitContent (API 25+)
  عبر FileProvider القائم — صورة حيث يقبل الحقل image/png، ثم نص الزوج
  الحقيقي حيث لا يقبلها، ثم الحافظة النظامية للملصقات؛ كل نتيجة توست
  صريح. بلا صلاحيات جديدة إطلاقًا (الخمسة كما هي).
- **اللوحة المدمجة**: تبويبان (تركيبات/ملصقات) ومعاينة حية هي نفسها
  PNG المُرسل، بتشريح اللوحات الذكية حرفيًا — ولا يُدوَّن أي شيء في
  وضع التخفي.

### 2) الحزم المستلمة أخيرًا — إصلاح جذر (دفعة 20)

- **البلاغ**: حزمة Sunset تفشل من مركز الحزم بـ«فشل التحقق»، وAmethyst
  تفشل من الاستيراد اليدوي بـ«أرشيف تالف» — رغم أن الأصول الثمانية
  سليمة بإثبات مستقل (sha256 مطابق وتوقيع ed25519 صالح في الثمانية).
- **السبب الجذري**: أرشيفات `.flex` الرسمية بُنيت ببايثون فتكتب **مدخلات
  ملفات بلا مدخلات مجلدات**، بينما كان فكُّ الضغط في التطبيق يشترط مدخل
  مجلد صريحًا قبل كتابة أي ملف متداخل — فأول حزمة خارجية-البناء (حزمنا
  الرسمية ذاتها!) تكشف الخلل الكامن.
- **الحل**: القارئ ينشئ سلسلة المجلدات الأب بنفسه بعد بوابة رحلة المسار
  نفسها وبسقوف zip-bomb كما هي — والحزم الثماني بلا تغيير بايت واحد
  فتبقى بصماتها وتواقيعها صالحة، **وأول عقد اختبار على الحزم الحقيقية
  (9/9) يمنع تكرار الفصل بين «الأصل الصحيح» و«القارئ العاجز»**.

### 3) حوكمة التوقيع — التخزين البارد (دفعة 18)

- بند نافذة v1.2.x مكتمل: بذرة الناشر ed25519 خارج git تمامًا بنقطة دافئة
  واحدة ونسختين باردتين بوسيطين جغرافيين بحيازة المالك، بطقوس توليد
  واستعادة وتدوير وفقد مكتملة (`docs/COLD_STORAGE.md`)، مثبتًا آليًا ببوابة
  `trust_chain_audit.sh` (8 فحوصات) تعمل في كل دفع.

## التحديث من الإصدارات السابقة

- ✅ **من 1.4.1 إلى 1.4.2: تحديث موضعي مضمون** (نفس مفتاح التوقيع) —
  ثبّت فوق الإصدار الحالي مباشرة، وقاموسك ونسخك الاحتياطية كما هي.
- ✅ **من 1.2.0 فصاعدًا كل التحديثات الموضعية مضمونة** (نفس المفتاح
  لكل ما يلي 1.2.0).
- ⚠️ **مستخدمو 1.0.0/1.1.0 — تنبيه دوران المفتاح (جرى في 1.2.0)**:
  المفتاح السابق فقد وصوله مع ترحيل المستودع، فأعيد توليد مفتاح جديد
  **بنفس هوية الشهادة** (CN=DRS Smart Keyboard Release) — البصمة:
  `54:FA:D7:4F:CD:91:0B:BB:65:F2:60:4F:32:82:27:F4:47:F9:AF:7E:6A:B9:41:66:D9:DC:67:56:97:1E:E3:20`.
  التحديث الموضعي من 1.1.0 وما قبله غير متاح (أندرويد يرفض التحديث فوق
  بصمة مختلفة): صدّر قاموسك ونسخك الاحتياطية الموقّعة من الإصدار القديم،
  ألغِ تثبيته، ثم ثبّت 1.4.2 كتثبيت أول واستورد ما صدّرته.

## التثبيت

1. نزّل `DRS-Smart-Keyboard-v1.4.2.apk` من مرفقات هذا الإصدار (أو من موقع
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
5. **جرّب الثيمات الثماني**: مركز الحزم → ثبّت أي سمة (Sunset/Ocean/Forest/
   Amethyst/Rose/Carbon/Arabian/Aurora) — في هذا الإصدار ينجح التثبيت
   فعلًا، وجرّب مطبخ الإيموجي من رقاقة المبدّل.

## بيان الامتثال للمصادر المفتوحة

يستند هذا المشروع إلى مكونين مشتقين بموجب رخصة **Apache-2.0** مع إسناد حرفي
كامل في الترويسات والوثائق:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

السجل الكامل للإفصاح والجداول: `THIRD_PARTY_NOTICES.md` في جذر المستودع.
ملصقات مطبخ الإيموجي وأصوله أصلية من تأليف المشروع برخصة Apache-2.0
(`docs/EMOJI_KITCHEN.md`).

## التحقق

- **2,089 اختبار وحدة أخضر** · بوابة تغطية Kover · بوابة MASVS الداخلية
  · بوابة trust-chain · بوابات أداء · فحص apksigner مستقل.
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
  declared intelligence ladder (L0→L5) whose first four arms are complete —
  zero cloud dependencies and zero model downloads.
- **The seventh panel — DRS Emoji Kitchen**: a 100% on-device original
  emoji-combination experience (no Google assets, no private APIs, no
  network) driven by a closed recipe engine, plus an original Apache-2.0
  licensed sticker library, and sending through the public commitContent
  API — the real send test is in your hands.
- **Proven privacy**: automated privacy canary + the internal MASVS gate
  (34 checks) + an 11-check release gate (fail-closed) — and the proof is
  live: network counters the app ticks in front of you every second.
- **Package center**: ed25519-signed extensions verified before unpacking —
  **and in this release all eight official packages finally install and
  import for real** (a contract test on the actual packages: 9/9 green).

## New in 1.4.2 — three batches

### 1) Emoji Kitchen — the seventh smart panel (batch 19)

- **The pure engine `DrsEmojiKitchen`**: 8 closed categories, 48
  participants (with Arabic and English keywords), 13 layout recipes over
  11 anchor positions with bounded scales and deterministic angular jitter —
  the same pair yields the same plan byte-for-byte; a pair without a recipe
  shows honest silence with no fake image, and Arabic search is normalized
  (diacritics/hamzas/teh marbuta).
- **Sticker library**: the "DRS Kitchen Originals" pack v1.0.0 — 12 vector
  stickers authored exclusively by this project under Apache-2.0 documented
  in the index itself, with six categories, search, favorites and history
  (cap 24).
- **Compatible sending**: the documented public commitContent API (API 25+)
  through the existing FileProvider — an image where the field accepts
  image/png, then the real pair as text where it does not, then the system
  clipboard for stickers; every outcome announces itself with an explicit
  toast. Zero new permissions (the five stay as they are).
- **Integrated panel**: two tabs (combinations/stickers) and a live preview
  identical to the sent PNG, dissecting the smart panels' shared anatomy —
  and nothing is recorded in incognito mode.

### 2) Received packages at last — a root fix (batch 20)

- **The report**: Sunset failed in the package center with "verification
  failed", Amethyst failed in manual import with "corrupted archive" — even
  though all eight assets were independently proven intact (matching
  sha256 and valid ed25519 signatures on all eight).
- **Root cause**: the official `.flex` archives were built by Python and
  contain **file entries only, no directory entries**, while the app's
  unzip required an explicit directory entry before writing any nested
  file — so the very first externally-built archive (our own official
  packages!) exposed the latent defect.
- **The fix**: the reader now creates the parent directory chain itself,
  behind the same zip-slip guard and with the zip-bomb caps untouched —
  the eight packages stay byte-identical so their checksums and signatures
  remain valid, and a **first-ever contract test on the real packages
  (9/9)** prevents the "intact asset vs. broken reader" split forever.

### 3) Signing governance — cold storage (batch 18)

- The v1.2.x window item is complete: the ed25519 publisher seed lives
  entirely outside git with one warm point and two cold copies on two
  geographically separate media under the owner's custody, with complete
  generate/restore/rotate/loss rituals (`docs/COLD_STORAGE.md`), proven
  automatically by the `trust_chain_audit.sh` gate (8 checks) on every push.

## Updating from previous releases

- ✅ **1.4.1 → 1.4.2: guaranteed in-place update** (same signing key) —
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
  1.4.2 as a fresh install and import what you exported.

## Installation

1. Download `DRS-Smart-Keyboard-v1.4.2.apk` from this release's assets (or
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
5. **Try the eight themes**: Package Center → install any theme
   (Sunset/Ocean/Forest/Amethyst/Rose/Carbon/Arabian/Aurora) — in this
   release installation genuinely succeeds; then try Emoji Kitchen from the
   switcher chip.

## Open-source compliance statement

This project builds upon two derived components under the **Apache-2.0**
license with full literal attribution in headers and docs:

- **jetpref** — Patrick Goldinger
- **snygg / kotlin** — The FlorisBoard Contributors

Full disclosure record and tables: `THIRD_PARTY_NOTICES.md` at the repository
root. Emoji Kitchen stickers and assets are original project authorship
under Apache-2.0 (`docs/EMOJI_KITCHEN.md`).

## Verification

- **2,089 unit tests green** · Kover coverage gate · the internal MASVS
  gate · the trust-chain gate · performance budgets · an independent
  apksigner check.
- All release assets are scheduled by fingerprint in the attached
  **SHA256SUMS.txt**.
- The full fingerprint table is appended below these notes (generated
  automatically by CI).

</div>
