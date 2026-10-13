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
| Derived paths (modules) | `lib/android`, `lib/color`, `lib/compose`, `lib/kotlin`, `lib/snygg` |

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
النهائي في الجدول الكانوني.

---

### ملاحظة تشغيلية (Maintenance note)

يُحدَّث هذا الملف **وحدة وحدة** مع كل اقتلاع لاحق (المراحل 2–5 وما بعدها):
عند اكتمال إعادة كتابة وحدة بالكامل يُحذف قسمها من هنا أو يُعدَّل ليعكس
الحالة الجديدة. المرجع المثبت للقياس موثق في `tools/cleanup/WORKLOG.md`
(الوسوم مرفوضة بالقياس — القرار نهائي).
