# THIRD-PARTY SOURCE NOTICES / إشعارات المصادر الخارجية

This document discloses vendored / derived third-party source code shipped in
this repository under `lib/`, with exact upstream references. It is updated
per module as each cleanup phase completes.

---

## 1. FlorisBoard

| Item | Value |
|---|---|
| Upstream project | FlorisBoard — https://github.com/florisboard/florisboard |
| Author / copyright | The FlorisBoard Contributors (project by Patrick Goldinger) |
| Pinned reference | branch `main` @ commit `fe1241f4921b3eae923571ff7a3e113a7e10d677` |
| License | Apache-2.0 |
| Derived paths (modules) | `lib/kotlin`, `lib/snygg` (حُذفت `lib/android` في المرحلة 4، وحُذفت `lib/compose` في المرحلة 5 — انظر فقرتي المرحلتين أدناه) |

### طبيعة الحالة (Nature of the derived copies)

- الملفات المشتقة من FlorisBoard منقولة **مع تعديلات** (إعادة تسمية الحزم من
  `org.florisboard.*` إلى `org.drs.*`، إعادة تسمية بعض الملفات من البادئة
  `Floris*` إلى `Drs*`، وتعديلات داخلية متفاوتة من تعديل بسيط حتى إعادة كتابة
  عميقة — انظر `tools/cleanup/measurements/canonical_verdicts.csv` للتصنيف
  الكامل لكل ملف).
- **الترويسة الأصلية (Apache-2.0) كانت قد استُبدلت سابقاً بترويسة DRS قصيرة
  (4 أسطر)، واستُعادت الترويسة الأصلية الحرفية في هذه المرحلة (المرحلة 1)**
  عبر `tools/cleanup/restore_headers.py` مع ضمان ثبات جسم كل ملف (SHA-256).
- الملفات التي لا مقابل لها في المرجع المثبت (مؤلفات DRS) تحتفظ بترويسة DRS.
- **إعادة كتابة المرحلة 3 (وحدة color مكتملة)**: `lib/color` أعيدت كتابتها ضد
  المرجع المثبت (ColorPalette.kt ‏100.0→5.6%، ColorMappings.kt ‏74.7→26.0%،
  build.gradle.kts ‏81.0→25.0% — القياسات الموحدة في الجدولين الكانونيين
  `canonical_verdicts.csv` و`canonical_similarity.csv`) فخرجت الوحدة من قائمة
  المشتقات أعلاه **عدا `MaterialYouFlags.kt`** الذي يبقى نسخة حرفية (100.0%)
  معفياً بعقده البياني التسلسلي البحت (data class + Saver بلا منطق يُعاد
  تصميمه — استثناء موثق منذ step-0) — إبقاؤه في هذا القسم إفصاح قانوني.
- **إعادة كتابة المرحلة 4 (وحدة android مكتملة)**: `lib/android` (21 ملفاً)
  أعيدت كتابتها ضد المرجع المثبت وأُعيد قياسها موحّداً — 18 ملفاً معاد
  الكتابة كلها ≤30% (أعلاها Vibrator.kt ‏29.2%)، وحالتان خاصتان بقياس فقط
  بقرار المالك: `Toast.kt` أصلي DRS (أفضل مرشح في الشجرة 16.7%) و
  `SystemSettingObserver.kt` نسخة حرفية (100.0%) معفاة بمبررها الموثق
  (مراجع app=0 لكنها مستخدمة داخلياً في observe/registerContentObserver —
  لا حذف ولا كتابة) — إبقاؤه في هذا القسم إفصاح قانوني. القياسات الكاملة في
  `canonical_verdicts.csv` و`canonical_similarity.csv`، فخرجت `lib/android`
  من قائمة المشتقات أعلاه عدا الإفصاحين الموثقين.
- **تصحيح المرحلة 2 (قرار المالك — الخيار أ)**: `lib/kotlin/.../MimeTypeFilter.kt`
  و`lib/compose/.../DrsAppBar.kt` ثبت بالقياس أنهما مشتقان (100.0% و44.4% ضد
  المرجع المثبت) واستُعيدت ترويستهما الأصلية الحرفية من ملفَي الأصل
  (`MimeTypeFilter.kt` و`FlorisAppBar.kt`) مع ضمان ثبات الجسم SHA-256 —
  سجل الإصلاح: `tools/cleanup/measurements/phase2_header_fix.csv`.
- **إعادة كتابة المرحلة 5 (وحدة compose مكتملة)**: `lib/compose` (27 ملفاً)
  أعيدت كتابتها ضد المرجع المثبت في سبع دفعات (5a–5g) ببوابة بناء كاملة بعد
  كل دفعة — 22 ملفاً معاد الكتابة كلها ≤30% (أعلاها DrsBulletSpacer.kt ‏28.6%
  وRtlModifiers.kt ‏26.7%)، و4 ملفات محمية لم تُمسّ (`Tooltip.kt` و
  `TooltipDefaults.kt` بترويستهما الأصلية، و`MotionScale.kt` أصلي DRS، و
  `KeyboardForwardDelete.kt`)، و`consumer-rules.pro` فارغ بالتصميم (0.0%).
  فخرجت `lib/compose` من قائمة المشتقات أعلاه تماماً. القياسات الكاملة في
  `canonical_verdicts.csv` و`canonical_similarity.csv`.
- **توضيحان مطلوبان من المالك (المرحلة 5)**:
  1. الإفصاحان الموثقان في `lib/android` هما بالاسم المضبوط: `Toast.kt`
     (أصلي DRS — أفضل مرشح SnyggPropertySetEditor بقياس 16.7%) و
     `SystemSettingObserver.kt` (نسخة حرفية 100.0% معفاة بمبررها الموثق).
  2. نعم، `SystemSettingObserver.kt` مذكورة فعلاً في هذا الملف (الفقرة أعلاه
     للمرحلة 4) وستبقى هنا 100.0% دائماً — إفصاح قانوني دائم لا يُزال.

## 2. jetpref

| Item | Value |
|---|---|
| Upstream project | jetpref — https://github.com/patrickgold/jetpref |
| Author / copyright | Patrick Goldinger |
| Pinned reference | branch `main` @ commit `7a3df4eb76ff81245244761f6c160eab31e9f73f` |
| License | Apache-2.0 |
| Derived paths (modules) | `lib/jetpref/datastore-model`, `lib/jetpref/datastore-model-processor`, `lib/jetpref/datastore-ui`, `lib/jetpref/material-ui` |

### طبيعة الحالة (Nature of the derived copies)

- الملفات المشتقة من jetpref منقولة **مع تعديلات** (إعادة تسمية الحزم من
  `dev.patrickgold.jetpref.*` إلى `org.drs.jetpref.*` مع تعديلات متفاوتة —
  التصنيف الكامل في `tools/cleanup/measurements/canonical_verdicts.csv`).
- **الترويسة الأصلية (Apache-2.0) كانت قد استُبدلت سابقاً بترويسة DRS قصيرة
  (4 أسطر)، واستُعادت الترويسة الأصلية الحرفية في هذه المرحلة (المرحلة 1)**.
- ملف تسجيل معالج KSP
  (`lib/jetpref/datastore-model-processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`)
  ملف تسجيل خدمة مشتق من jetpref ولا يحمل ترويسة بطبيعته.
- **تصحيح المرحلة 2 (قرار المالك — الخيار أ)**: `datastore/JetPref.kt` و
  `datastore/JetPrefJvm.kt` و`datastore/model/PreferenceDataJvm.kt` ثبت بالقياس
  أنها مشتقة (100.0% و95.2% و93.2% ضد المرجع المثبت) — الأول استُعيدت ترويسته
  الحرفية من `jvmCommonMain JetPref.kt`، والملفان الآخران مدموجان من مصدرين
  فأُعطيا الترويسة الكانونية القياسية لjetpref (`Copyright 2025 Patrick
  Goldinger` + نص Apache-2.0) مع ضمان ثبات الجسم SHA-256، وصُحّح تصنيفهما في
  الجدول الكانوني من `drs-original` إلى `slightly-modified`.
- **قتل المرحلة 2 (فئة أ)**: `datastore-ui/ui/TextFieldPreference.kt` (نسخة
  مشتقة 100% من jetpref) حُذفت — 0 مراجع على مستوى المشروع كله، مؤكدة بفحص
  الرموز والبناء الكامل.

## 3. Android Open Source Project (AOSP)

| Item | Value |
|---|---|
| Upstream | AOSP — https://source.android.com |
| License | Apache-2.0 |
| Derived files | `lib/compose/.../tooltip/Tooltip.kt`, `TooltipDefaults.kt` (ترويسة AOSP الأصلية قائمة)، `lib/compose/.../icons/KeyboardForwardDelete.kt` |

- ملفات AOSP محمية: ترويساتها الأصلية صحيحة ولا تُمسّ (قائمة الحماية
  الموثقة في `tools/cleanup/WORKLOG.md`).

## 4. DRS originals

`lib/compose/.../MotionScale.kt` وملفات `Drs*`/الاختبارات التي لا مقابل
لها في المرجعين أعلاه هي مؤلفات أصلية لهذا المشروع (ترويسة DRS) — التصنيف
النهائي في الجدول الكانوني. (بعد تصحيح المرحلة 2 بقرار المالك: `DrsAppBar.kt`
و`MimeTypeFilter.kt` وملفات jetpref الثلاثة أُعيد تصنيفها كمشتقات — انظر
القسمين 1 و2 أعلاه؛ `Toast.kt` يبقى أصلياً بقياس 14.8%.)

---

### ملاحظة تشغيلية (Maintenance note)

يُحدَّث هذا الملف **وحدة وحدة** مع كل اقتلاع لاحق (المراحل 2–5 وما بعدها):
عند اكتمال إعادة كتابة وحدة بالكامل يُحذف قسمها من هنا أو يُعدَّل ليعكس
الحالة الجديدة. المرجع المثبت للقياس موثق في `tools/cleanup/WORKLOG.md`
(الوسوم مرفوضة بالقياس — القرار نهائي).
