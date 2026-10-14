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

## 2. jetpref — إعلان اعتماد صريح (قرار المالك النهائي: الخيار ج — الإبقاء القانوني)

**اعتماد رسمي**: وحدات jetpref الأربع تُبقى في الشجرة كمكوّن مساعد خارجي
مُندمج ومُصرَّح به، بقرار المالك المحسم بعد قياس بوابة jetpref 1
(`tools/cleanup/measurements/jetpref_gate1_report.md`) والمقارنة الثلاثية
الموثقة فيه. هذا القسم هو الإعلان القانوني الكامل لهذا الاعتماد.

| Item | Value |
|---|---|
| Upstream project | jetpref — https://github.com/patrickgold/jetpref |
| Author / copyright | Patrick Goldinger |
| Pinned reference | branch `main` @ commit `7a3df4eb76ff81245244761f6c160eab31e9f73f` (المرجع المثبت للقياس، جرى رفض v0.3.1 بالأرقام) |
| License | Apache-2.0 — https://www.apache.org/licenses/LICENSE-2.0 |
| Upstream license file | https://github.com/patrickgold/jetpref/blob/main/LICENSE |
| Vendored paths (modules) | `lib/jetpref/datastore-model`, `lib/jetpref/datastore-model-processor`, `lib/jetpref/datastore-ui`, `lib/jetpref/material-ui` |
| طبيعة النسخ | نسخ مدمجة ومعدلة: توحيد source-sets متعددة المنصات (KMP: commonMain/jvmCommonMain/androidMain → main واحد) + إعادة تسمية الحزم `dev.patrickgold.jetpref.*` → `org.drs.jetpref.*` |
| الترويسات | ترويسات Apache-2.0 الأصلية مستعادة ومحفوظة (المرحلة 1: 132 ملفاً؛ تصحيح المرحلة 2: +3 ملفات jetpref بإشعار قانوني قياسي) — سجل الملفات الفعلية في `tools/cleanup/measurements/canonical_verdicts.csv` |
| الإشعار الكامل | "Copyright 2025 Patrick Goldinger — Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License." — الترويسة الحرفية موجودة على رأس كل ملف مشتق |

### جدول الملفات المشتقة (المصدر: `canonical_verdicts.csv` — صفوف jetpref، 48 صفاً)

الإجمالي حسب الحكم: verbatim=32، slightly-modified=7، build-file=7،
resource-file=1 (تسجيل معالج KSP)، deleted-phase2=1
(`TextFieldPreference.kt` قُتل في المرحلة 2 — صفر مراجع مؤكدة).

**datastore-model**

| الملف | الحكم | التشابه | ملاحظة |
|---|---|---|---|
| build.gradle.kts | build-file | 77.4 | canonical=77.4% |
| proguard-rules.pro | build-file | — | no counterpart >= 0.50 |
| src/main/kotlin/org/drs/jetpref/datastore/JetPref.kt | slightly-modified | 100.0 | canonical=100.0% |
| src/main/kotlin/org/drs/jetpref/datastore/JetPrefJvm.kt | slightly-modified | 95.2 | phase2 reclass per owner ruling (option A); was drs-original (phase1 scan gap); best-match scan documented in phase2_review.csv |
| src/main/kotlin/org/drs/jetpref/datastore/Utils.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/annotations/Annotations.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/LocalTime.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceData.kt | slightly-modified | 100.0 | canonical=100.0% |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceDataJvm.kt | slightly-modified | 93.2 | phase2 reclass per owner ruling (option A); was drs-original (phase1 scan gap); best-match scan documented in phase2_review.csv |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceMigration.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceModel.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceSerializer.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceType.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/StringEncoder.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/Validator.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/runtime/AndroidAppDataStorage.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/runtime/DataStore.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/runtime/Exceptions.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/runtime/FileBasedStorage.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/runtime/Storage.kt | verbatim | 100.0 | — |

**datastore-model-processor**

| الملف | الحكم | التشابه | ملاحظة |
|---|---|---|---|
| build.gradle.kts | build-file | 69.2 | canonical=69.2% |
| src/main/kotlin/org/drs/jetpref/datastore/model/ModelProcessor.kt | slightly-modified | 97.9 | canonical=97.9% |
| src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider | resource-file | — | KSP SymbolProcessorProvider registration — NEVER delete (trap #11) |

**datastore-ui**

| الملف | الحكم | التشابه | ملاحظة |
|---|---|---|---|
| build.gradle.kts | build-file | 78.0 | canonical=78.0% |
| proguard-rules.pro | build-file | — | no counterpart >= 0.50 |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceDataAdapter.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceDataEvaluatorScope.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/Annotations.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/ColorPickerPreference.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/Common.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/DialogPrefStrings.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/DialogSliderPreference.kt | slightly-modified | 95.9 | canonical=95.9% |
| src/main/kotlin/org/drs/jetpref/datastore/ui/ListPreference.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/LocalTimePickerPreference.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/Preference.kt | slightly-modified | 100.0 | canonical=100.0% |
| src/main/kotlin/org/drs/jetpref/datastore/ui/PreferenceUi.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/SwitchPreference.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/datastore/ui/TextFieldPreference.kt | deleted-phase2 | 100.0 | category-a kill (0 refs project-wide, symbol-scan + compiler-confirmed); phase2 batch A |

**material-ui**

| الملف | الحكم | التشابه | ملاحظة |
|---|---|---|---|
| build.gradle.kts | build-file | 82.5 | canonical=82.5% |
| proguard-rules.pro | build-file | — | no counterpart >= 0.50 |
| src/main/kotlin/org/drs/jetpref/material/ui/Annotations.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/ColorRepresentation.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/Common.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/JetPrefAlertDialog.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/JetPrefColorPicker.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/JetPrefDropdown.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/JetPrefListItem.kt | verbatim | 100.0 | — |
| src/main/kotlin/org/drs/jetpref/material/ui/JetPrefTextField.kt | verbatim | 100.0 | — |

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

### ملاحظة تشغيلية (Maintenance note — الحالة النهائية)

**حُسم الوضع النهائي بقرار المالك (الخيار ج — الإبقاء القانوني، 2026-10-14)**:
المشتقات المتبقية في هذه الوثيقة هي الحالة الختامية للمشروع — بنية أصلية
مع مكوّنَين مساعدَين مُندمجَين مُصرَّح بهما (FlorisBoard: `lib/kotlin` و
`lib/snygg` + إفصاحان؛ jetpref: 4 وحدات بإعلان اعتماد كامل أعلاه). لا اقتلاع
لاحقاً مجدول؛ أي تغيير مستقبلي على هذه القائمة لا يبدأ إلا بقرار صريح جديد
من المالك (المسار الموثق للاستبدال المستقبلي إن رغب: `tools/cleanup/
measurements/jetpref_gate1_report.md`). المرجع المثبت للقياس موثق في
`tools/cleanup/WORKLOG.md` (الوسوم مرفوضة بالقياس — القرار نهائي).
