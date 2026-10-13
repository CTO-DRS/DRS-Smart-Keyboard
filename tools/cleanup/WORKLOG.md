# سجل عمل تنظيف lib/ — بروتوكول مصرفي

> هذا الملف جزء من المستودع ويُدفع مع كل مرحلة (قاعدة البروتوكول المصرفي).
> الأدوات في `tools/cleanup/` تُدفع دائماً مع السجل لتنجو من أي إعادة تهيئة بيئة.

═══════════════════════════════════════════════════════════

## 0) حادثة فقد العمل — موثقة بالأدلة (2026-10-12)

### ما حدث
كومِتات المراحل 1–5 (13 كومِتاً) من جولة التنظيف السابقة كانت **محلية فقط**
ولم تُدفع يوماً إلى أي remote. أعيدت تهيئة البيئة فُقدت بالكامل.

### الأدلة الجامعة (فحص 2026-10-12)
| الفحص | الأداة | النتيجة |
|---|---|---|
| remote الأصل | `git ls-remote --heads` + GitHub API | فرع واحد فقط `main @ 030fad9` — لا `cleanup/drs-purge` |
| forks | GitHub API `/forks` | صفر |
| bundles محلية | `find / -name "*.bundle"` | صفر |
| بقايا مستودع محلي | `find .git` dirs | صفر (بيئة فارغة) |
| توكنات | مسح `ghp_`/`github_pat_` | لا شيء |

### اكتشاف إضافي حاسم
**البيئة تُعاد تهيئتها بين كل رسالة/دور.** دليل: استنساخ + worklog أُنشئا في
دورٍ سابق وُجِدا مفقودين في الدور التالي؛ الناجي الوحيد كان ما أُنشئ داخل
الدور نفسه. لذلك: **لا ينجو شيء محلي عبر الأدوار — الدفع الفوري لكل خطوة هو
وسيلة البقاء الوحيدة**، وهذا هو مبرر البروتوكول المصرفي.

### ما نجا من الجولة السابقة
- `main @ 030fad9` على الأصل العام (نقطة أساس المرحلة 0) — لا غير.
- المنهجية والدروس موثقة في سياق الجلسة (14 مزلقاً معاد توثيقها أدناه).

### خطة التعافي المعتمدة من المالك
إعادة التنفيذ من `main @ 030fad9` ببروتوكول مصرفي: كل مرحلة تُدفع فور
اكتمالها + tag باسمها، والأدوات والسجلات داخل المستودع.

═══════════════════════════════════════════════════════════

## 1) البروتوكول المصرفي المعتمد (غير قابل للتفاوض)

1. ممنوع أي عمل محلي دون دفع: كل مرحلة تُدفع فور اكتمالها + tag باسمها.
2. كل السكربتات والأدوات داخل `tools/cleanup/` وتُدفع معها.
3. worklog + CSV + جدول القياسات تُحدَّث وتُدفع مع كل مرحلة — لا جدول نهائي
   واحد في الآخر. الجدول الكانوني: `tools/cleanup/measurements/canonical_similarity.csv`.
4. عند كل دفع يُرسل للمالك: URL + commit hash + اسم التاغ.
5. أمان التوكن: credential store خارج المستودع فقط؛ لا يظهر في أي ملف يُدفع.

### المراجع المثبتة للقياس (بلا مرجع مثبت لا قيمة للقياس)

**الحسم بالأرقام لا بالافتراض**: فُحص مرشحان لكل مستودع بأداة الاستعادة
(dry-run) وعُدّت التطابقات الحرفية (بصمة الجسم المطبّع) على كل ملفات lib/:

| المرشح | تطابق حرفي | fuzzy | بلا مقابلة | القرار |
|---|---|---|---|---|
| floris tag v0.4.6 | 60 | 10 | 77 | مرفوض |
| **floris main @ fe1241f** | **97** | **14** | **28** | **مثبّت ← مرجع القياس** |

jetpref: v0.3.1 مقابل main أعطت نفس البنية (وحدات datastore-model /
datastore-model-processor / datastore-ui / material-ui مطابقة)، والقرين
المعتمد مع floris-main هو **jetpref main @ 7a3df4e** (نفس توثيق الجولة
السابقة). jetpref ينتمي إلى `patrickgold/jetpref` وليس منظمة florisboard
(`florisboard/jetpref` غير موجود: Repository not found).

| المستودع | المرجع المعتمد | commit مقشور (peeled) | نسخة محلية |
|---|---|---|---|
| FlorisBoard | فرع `main` @ `fe1241f` | `fe1241f4921b3eae923571ff7a3e113a7e10d677` | `/repos/floris-main` (خارج المستودع، قابل لإعادة الاستنساخ بالأمر أدناه) |
| jetpref | فرع `main` @ `7a3df4e` | `7a3df4eb76ff81245244761f6c160eab31e9f73f` | `/repos/jetpref-main` (كذلك) |

هذان هما "ما يقابلهما" من تعليمات المالك: الوسمان v0.4.6/v0.3.1 فُحصا
ورُفضا بالأرقام أعلاه، والمرجع الحقيقي لمصدر النسخ هو فرعا main عند الهاشات
الموثقة — وهي نفسها الموثقة في الجولة السابقة (floris master @ fe1241f،
jetpref master @ 7a3df4e).

اكتشاف بنيوي: floris-main أذاب وحدة `lib/color` داخل `lib/snygg` (إعادة
هيكلة flest)، بينما DRS يحتفظ بوحدة `lib/color` — ملفات color تقابل ملفات
snygg/color في المرجع (الاستعادة تطابقها بالبصمة بغض النظر عن المسار).

### أوامر إعادة بناء نسخ المقارنة
```bash
git clone --depth 1 --branch main https://github.com/florisboard/florisboard.git /home/z/my-project/repos/floris-main
git clone --depth 1 --branch main https://github.com/patrickgold/jetpref.git /home/z/my-project/repos/jetpref-main
# تحقق: git -C <نسخة> rev-parse HEAD يجب أن يساوي الهاشات المثبتة أعلاه
```

### الأدوات المعتمدة
| الأداة | الغرض | ضمانة |
|---|---|---|
| `tools/cleanup/measure_similarity.py` | القياس الموحد للتشابه العكسي (تطبيع: إزالة ترويسات/تعليقات/whitespace + تحييد package/import + استبعاد أسطر الأقواس المجردة من الطرفين بالتساوي) | `--fail-above 30` كبوابة |
| `tools/cleanup/restore_headers.py` | استعادة ترويسات Apache-2.0 الأصلية حرفياً | جسم الملف SHA-256 قبل=بعد (صفر تغيير كود)؛ dry-run افتراضي |
| `tools/cleanup/deadcode_detector.py` | كاشف الميت بمسح مرجعي على مستوى المشروع كاملاً (app + lib + tests + benchmark) | لا يحذف أبداً — المترجم هو الفيصل |

### المزلقات الموثقة من الجولة السابقة (تُطبق مباشرة)
1. weight يتطلب RowScope — عند استخراج دوال من داخل Row مرر modifier من موقع الاستدعاء.
2. maxLineSpan في foundation 1.10.6 بمستقبِل LazyGridItemSpanScope — صحح النوع والاستيراد.
3. @Composable على systemAccentOrDefault جزء من التواقيع المجمدة — لا تسقطها.
4. تفويض by project يقرأ مفتاح gradle.properties بالاسم نفسه — استخدم providers.gradleProperty.
5. الاستيراد الصحيح ProvidableCompositionLocal (التي تحمل provides) لا CompositionLocal.
6. معامل key() اسمه keys (vararg جمع).
7. rememberSettingState ترجع MutableState<R> — مواقع الاستدعاء تكتب فيها.
8. كاشف الرموز يفشل على خاصيات الامتداد (ForwardDelete مستخدمة فعلاً) — الفيصل المترجم دائماً.
9. المسح المرجعي على مستوى المشروع كاملاً (app + lib + tests + benchmark) — لا app/src فقط.
10. التطبيع في القياس: إزالة whitespace/تعليقات/ترويسات + استبعاد أسطر الأقواس المجردة من الطرفين بالتساوي.
11. ModelProcessor حي عبر META-INF/services — لا يُقتل.
12. إخفاق اختبار عابر = تعارض Kotlin daemon غالباً — أعد التشغيل مرة قبل التشخيص.
13. بنّاء compose الأصلي يستخدم providers.gradleProperty فعلاً — الضغط الجذري مسموح.
14. العمالقة (Resources/DrsStepLayout/State) دفعة منفردة لكل واحد ببوابة كاملة بعد كل واحد.

### فحص دخاني للأدوات (أُجري قبل الكومِت)
- `measure_similarity.py`: قياس ذاتي = 100.0% ✓؛ ColorMappings ضد v0.4.6 =
  20.5% (ملف معدّل بعمق فعلاً — يفسّر رفض v0.4.6 كمرجع)؛ ضد main: فروق
  imports + إضافات getSystemAccent — تصنيفه النهائي يُقاس في المرحلة 3.
- `restore_headers.py`: اكتشاف **ترويسة DRS قصيرة بديلة** (4 أسطر) على 130
  ملفاً — المرحلة 1 = **استبدال** الترويسة لا إدراجها، والأداة عولجت لهذا
  (تجريد ترويسة DRS ثم ضمان جسم مطابق SHA-256). dry-run ضد المرجع المثبت:
  97 حرفياً + 14 fuzzy + 28 بلا مقابلة (اختبارات DRS أصلية + build files +
  ملفات محمية) + 9 أصول بلا ترويسة (تُعالج قانونياً في المرحلة 1). لا كتابة
  بدون --apply.
- `deadcode_detector.py`: يعمل على كامل المشروع (1,150 ملفاً متتبعاً) —
  التشغيل الفعلي مع بوابة بناء في المرحلة 2.

### استثناءات القياس الموثقة (لا يُطلب لها ≤30%)
- `MaterialYouFlags` — عقد بيانات.
- `.gitignore` — قالب عام.

═══════════════════════════════════════════════════════════

## 2) حالة المراحل

### الخطوة 0: التأسيس — تم
- [x] remote ثانٍ `work` = https://github.com/CTO-DRS/drs-sk-work (خاص)
- [x] دفع `main @ 030fad9` + `cleanup/drs-purge @ 030fad9`
- [x] أدوات التنظيف الثلاث داخل `tools/cleanup/` + فحص دخاني
- [x] مستودعات المقارنة مثبتة (الهاشات أعلاه)
- [x] هذا السجل

### المرحلة 1: الامتثال القانوني — مكتملة (2026-10-12)
- [x] كومِت تمهيدي `1a`: الجدول الكانوني `canonical_verdicts.csv` — **163 صفاً** كاملة
  تغطية lib/ (verbatim=106, slightly=24, deeply=3, drs-original=3, protected=3,
  resource-file=1, build-file=23) — قياس بالمقياس الموحد ضد المرجعين المثبتين.
- [x] استبدال الترويسات: **132 ملفاً** استُعيدت ترويستها الأصلية الحرفية
  (exact=115, fuzzy=17 بنسب 0.84–0.99، وترويسة قياسية للمشتقات أصولها بلا ترويسة)
  + **4 محميون لم يُمسّوا** (Tooltip/TooltipDefaults/MotionScale/KeyboardForwardDelete)
  + **12 بلا مقابل يحتفظون بترويسة DRS** (مؤلفات DRS: Toast.kt — قياسه ضد v0.4.6
  = 16.7% فقط أي أصله فعلياً DRS، JetPrefJvm.kt، PreferenceDataJvm.kt، اختبارات…).
- [x] THIRD_PARTY_NOTICES.md أعيد بناؤها بالمراجع الدقيقة + طبيعة الحالة
  (ملاحظة: `patrickgold/FlorisBoard` غير موجود على GitHub — الرابط الصحيح
  الموثق `florisboard/florisboard`، وpatrickgold هو المؤلف).

### بوابات المرحلة 1 (النتائج)
| البوابة | النتيجة | التفصيل |
|---|---|---|
| (أ) SHA-256 للأجسام | **PASS** | `gate_bodies.py`: 132 ملفاً، مخالفات=0 (صفر تغيير كود) |
| (ب) بناء كامل | **PASS** | assembleDebug ✅ + **1,780 اختباراً / 131 مجموعة / 0 إخفاق / 0 متخطى** + koverVerifyDebug ✅ (مطابق لخط الأساس) |
| (ج) الكلمات الممنوعة | **PASS** | مخالفات lib-scope=0. مسموح: 132 في الترويسات المستعادة، 36 في الإسنادات (notices/worklog)، 304 في بنية التنظيف (tools)، 867 بنائية (تسمية jetpref — لبوابات jetpref لاحقاً) |
| (د) أمان التوكن | **PASS** | النمط الصارم: صفر في شجرة العمل + صفر في كامل التاريخ + فحص مباشر لبادئة التوكن = نظيف |

### استثناءات موثقة (بوابة ج)
- `pre-existing:owner-docs` = 2 (CHANGELOG.md:1166,1254 — سرد تاريخي للمالك يذكر
  FlorisBoard كسلف مباشر؛ محتوى أساس خارج نطاق lib/ — قرار المالك).
- `pre-existing:app-doc-comment` = 1 (DrsV2700Tests.kt:18 — تعليق توثيقي لاختبار
  مقارنة قوائم ميزات؛ يُعاد صياغته في مرحلة لاحقة تلمس الكود).
- `structural:jetpref-naming` = 867 (تسمية الوحدة المقلدة نفسها — ضمن بوابات
  jetpref اللاحقة بموافقة صريحة، ليست تسريباً).

### المرحلة 2: القتل الوقائي — مفتوحة: بند المراجعة مُسلَّم، **توقف إجباري قبل أول قتل** (براءة مشكوك فيها، بند 2)

#### التمهيد (2026-10-13)
- [x] استئناف من الخاص: `cleanup/drs-purge @ 1346140` (تاغا `cleanup/step0` + `cleanup/phase1` حاضران)
- [x] JDK 17.0.20.1 (Temurin) + SDK (platforms;android-36, build-tools;36.0.0) + `local.properties`
- [x] المرجعان المثبتان مُستنسخان بالهاش: floris `fe1241f4921b3eae923571ff7a3e113a7e10d677` ✓ jetpref `7a3df4eb76ff81245244761f6c160eab31e9f73f` ✓
- [x] سلامة الأدوات: ذاتي=100.0%؛ عينات حرفية floris(Context)=100.0% + jetpref(material-ui/Common)=100.0%

#### المهمة 1: إعادة تشغيل الكاشف (قراءة فقط)
`deadcode_phase2_start.csv` — **204 صفاً = 204 صفاً تطابق تام مع baseline step0 (صفر انحراف)**.
الفارق الوحيد بين المسحين كان نهايات أسطر CRLF/LF (بيئة الكتابة) — طُبّع وأُثبت التطابق
بالمقارنة الثنائية. التفسير: المرحلة 1 لمست الترويسات/التعليقات فقط وكاشف الرموز يجرد التعليقات.

#### تسليم بنود المراجعة الثلاثة (`phase2_review.py` + `phase2_review.csv`)
**بند 1 — الـfuzzy**: التصحيح المحاسبي: نثر المرحلة 1 قال «17»؛ **العدّ الفعلي في
`phase1_header_map.csv` = 27 fuzzy** (الحصيلة الصحيحة: exact=96 + exact+canonical=9 +
fuzzy=27 = 132 استعادة). الجدول هو المرجع، النثر كان خطأ إحصاء.

**بند 2 — الـ12 بلا مقابل**: 7 مُصادَق عليها غير مشتقة + **5 مشكوك في براءتها**:

| # | الملف | القياس | التصنيف | الحكم |
|---|---|---|---|---|
| 1 | android/Toast.kt | best=14.8% (مسح كامل floris lib/android + app) | drs-original | ✓ غير مشتق |
| 2-7 | 6× build.gradle.kts | 53.9–82.5% (build config) | build-file | ✓ إعدادات بناء DRS لا مشتق كود |
| 8 | compose/DrsAppBar.kt | canonical=44.4% vs FlorisAppBar | deeply-modified | ⚠ مشتق بلا ترويسة Apache |
| 9 | kotlin/MimeTypeFilter.kt | canonical=**100.0%** vs floris MimeTypeFilter | slightly-modified | ⚠ مشتق (جزء محذوف من الأصل) بلا ترويسة |
| 10 | jetpref/JetPref.kt | canonical=**100.0%** vs jetpref jvmCommon JetPref.kt | slightly-modified | ⚠ مشتق بلا ترويسة |
| 11 | jetpref/JetPrefJvm.kt | **fresh scan=95.2%** vs jetpref jvmCommon JetPref.kt (jetprefDataStoreOf منسوخ حرفياً، أُثبت بصرياً) | كان drs-original ← خطأ تصنيف | ⚠ مشتق بلا ترويسة |
| 12 | jetpref/model/PreferenceDataJvm.kt | **fresh scan=93.2%** vs jetpref jvmCommon PreferenceData.kt | كان drs-original ← خطأ تصنيف | ⚠ مشتق بلا ترويسة |

**بند 3 — تدقيق الاتساق**: 5 مخالفات (نفس الملفات 8–12): مشتق بقياس ≥44% يحمل
ترويسة DRS بدل ترويسة Apache الأصلية. (KeyboardForwardDelete: إنذار كاذب أُصلح —
ترويسة AOSP السطرين هي الصيغة القياسية الصحيحة للملفات المحمية.)

#### القرار المطلوب من المالك (قبل أي قتل)
- **خيار أ (الموصى به قانونياً)**: معاملة الخمسة كمشتقات — استعادة الترويسات
  الأصلية الحرفية بميكانيكا المرحلة 1 (ضمان SHA-256 للأجسام) + إدخالات
  THIRD_PARTY_NOTICES + تصحيح صفي drs-original في الجدول الكانوني إلى
  slightly-modified بالمراجع والأرقام أعلاه، ثم متابعة القتل في نفس الدور.
- **خيار ب**: تأجيل الترويسات إلى المراحل 3–5 (DrsAppBar/MimeTypeFilter ضمن
  نطاق إعادة الكتابة) — لكن jetpref الثلاثة خارج نطاق إعادة الكتابة الحالية.
- القتل الوقائي نفسه مستقل تماماً (الملفات الخمسة كلها حية وليست مرشحة للقتل).

#### الجزء 1: الإصلاح القانوني (بحكم المالك — الخيار أ) — مكتمل 2026-10-13
- [x] `phase2_header_fix.py`: 5/5 PASS — ترويسات حرفية (MimeTypeFilter ← floris
  MimeTypeFilter، JetPref.kt ← jetpref jvmCommon JetPref.kt، DrsAppBar ←
  FlorisAppBar) + ترويسة كانونية لjetpref للملفين المدموجين (JetPrefJvm،
  PreferenceDataJvm) — **ضمان SHA-256 للأجسام 5/5** — سجل كامل:
  `phase2_header_fix.csv`.
- [x] الجدول الكانوني: صفان صُحّحا من drs-original إلى slightly-modified
  (95.2% / 93.2%). THIRD_PARTY_NOTICES محدّثة (قسما floris وjetpref وDRS).
- [x] بوابة: assembleDebug + **1,780/0 إخفاق/131 مجموعة** + koverVerifyDebug ✅
- [x] كومِت قانوني منفصل + دفع (لا تاغ) — `ef206428affe`

#### الجزء 2: القتل الوقائي — مكتمل 2026-10-13

**جدول القتل:**

| الملف | الفئة | المقتول | الأسطر | البوابة المثبتة |
|---|---|---|---|---|
| jetpref/datastore-ui/ui/TextFieldPreference.kt | أ (ملف 0 مراجع) | الملف كاملاً | 146 | فحص رموز + بناء كامل أخضر |
| kotlin/Regex.kt ← RegexSerializer | ب (تعريف علوي) | class | 11 | compileDebugKotlin + بناء كامل |
| snygg/ui/SnyggBox.kt ← SimpleSnyggBox | ب | fun | 26 | كذلك |
| snygg/ui/SnyggChip.kt ← SimpleSnyggChip | ب | fun | 27 | كذلك |
| snygg/ui/SnyggColumn.kt ← SimpleSnyggColumn | ب | fun | 22 | كذلك |
| snygg/ui/SnyggIcon.kt ← SimpleSnyggIcon | ب | fun | 21 | كذلك |
| snygg/ui/SnyggIconButton.kt ← SimpleSnyggIconButton | ب | fun | 30 | كذلك |
| snygg/ui/SnyggListItem.kt ← SimpleSnyggListItem | ب | fun | 50 | كذلك |
| snygg/ui/SnyggRow.kt ← SimpleSnyggRow | ب | fun | 26 | كذلك |
| snygg/ui/SnyggSpacer.kt ← SimpleSnyggSpacer | ب | fun | 28 | كذلك |
| snygg/ui/SnyggText.kt ← SimpleSnyggText | ب | fun | 45 | كذلك |

**الإجمالي: 1 ملف (146 سطراً) + 10 تعريفات عليا (296 سطراً) = 442 سطراً ميتاً.**

**جدول الأيتام (فئة ج):** فارغ — صفر أيتام خاصة مثبتة. الدليل:
1. المترجم الرسمي في هذا الخط (Kotlin 2.4.20 عبر AGP9 Build Tools API)
   **لا يصدر أصلاً تحذيرات never-used** — مثبت بملف استقصاء فيه 4 أعضاء
   خاصة ميتة صراحة: البناء أخضر وصفر تحذيرات (محذوف لاحقاً).
2. المسح الذاتي الشامل على مرشحي الكاشف: 46 مُشار إليها نصياً + 78 غير
   خاصة (أعضاء عامة/محليات) + 0 خاصة بلا مراجع. القاعدة الأعلى حُفظت:
   المترجم هو الفيصل، ولا قتل دون إثبته.

**دفوع أثبتها المترجم أثناء التنفيذ (تُدوَّن مزلقات جديدة):**
- 15. الاستيراد بالاسم (`import org.drs.lib.android.bufferedReader`) يجعل
  الملف "بلا جذع مرجعي" حياً تماماً — فحص الرموز قبل القتل إجباري
  (AndroidAssetsManager كاد يُقتل؛ المستخدم الحقيقي EmojiData.kt).
- 16. الاستخدام داخل الملف نفسه غير مرئي لمسح "0 مراجع مستثنى الذات" —
  فلتر استخدام-ذاتي إجباري (43 مرشحاً أُنقذوا: matchMimeTypeAgainstFilters نموذجاً).
- 17. `annotation class X` و`@Composable fun` أسطر تبدأ بـ@ — أي نازع رموز
  يقفزها يقتل خطأً (استُعيد 5 ملفات بعد أن أمسك المترجم ExperimentalJetPrefMaterial3Ui).
- 18. فخ daemon: مات Gradle daemon مرة عند البوابة (1,780/0 مسجلة من XML
  قبل الوفاة) — إعادة تشغيل واحدة = أخضر.

**نتيجة الكاشف الختامية:** 196 → 185 مرشحاً فريداً (المقتولون الـ11 فقط،
**صفر يتيم جديد**). الفروقات الثلاثة بين المسوح موثقة ومفسرة.

**بوابات الجزء 2:** كل دفعة: assembleDebug + testDebugUnitTest
(1,780/0/131) + koverVerifyDebug ✅ — الدفع فور كل بوابة خضراء.

**حسابات الجدول الكانوني بعد القتل الجزئي** (قياس جديد بعد كل دفعة):
Regex.kt 100→100 (المقتول إضافة DRS خالصة)، SnyggBox 81.1→76.4،
SnyggIconButton 90.8→87.7 (باقيها موجود في الأصل)، والبقية 100→100.
TextFieldPreference موسومة deleted-phase2.

### المرحلة 3: color — مكتملة (2026-10-13)

#### التمهيد
- [x] استئناف من `cleanup/drs-purge @ e331dbe` (تاغ `cleanup/phase2` حاضر)
- [x] JDK 17.0.20.1 (Temurin) + SDK (platforms;android-36, build-tools;36.0.0) + `local.properties` (متجاهل بواسطة git — فُحص بcheck-ignore)
- [x] المرجعان المثبتان: floris `fe1241f4921b3eae923571ff7a3e113a7e10d677` ✓ (920 ملفاً) + jetpref `7a3df4eb76ff81245244761f6c160eab31e9f73f` ✓ (105 ملفات، من `patrickgold/jetpref` — `florisboard/jetpref` غير موجود: Repository not found)
- [x] سلامة الأدوات: ذاتي=**100.0%** (79/79) + عينتان حرفيتان: floris AndroidAssetsManager=**100.0%** (33/33) + jetpref material-ui Common=**100.0%** (34/34)
- [x] بوابة أساس قبل أي تعديل: assembleDebug أخضر على بيئة معاد تهيئتها بالكامل (توزيعة Gradle 9.4.1 + اعتمادات نزلت من الصفر)

#### توضيح عدّ الكاشف 204 مقابل 196 (طلب المالك — سطر التقرير)
كلا الرقمين من **المسح المرجعي نفسه** (`deadcode_phase2_start.csv`) بأساسَي عدٍّ مختلفين:
**204 = صفوف خام**، **196 = مرشحون فريدون** بمفتاح `(file,kind,name)` — الفرق 8 صفوف
مكررة لأسماء تحمل تراكيب (overloads) في الملف الواحد، والكاشف يسجل كل تصريح صفاً
مستقلاً: `rawEncodedValues`×2 (DataStore.kt)، `fromJsonElementSpecialized`×2 +
`toJsonElementSpecialized`×2 (SnyggPropertySet.kt)، `cutCornerShape`×4
(SnyggPropertySetEditor.kt)، `propertySetEditor`×3 (SnyggStylesheetEditor.kt) —
فائض 1+1+1+3+2 = **8** ✓. لا حذف حدث بين الرقمين؛ الختامي 196→185 (11 مقتولاً) موثق في المرحلة 2.

#### جدول المرحلة (قياس موحد بالأداة الكانونية)

| الملف | التشابق قبل → بعد | الحكم |
|---|---|---|
| ColorPalette.kt | 100.0% → **5.6%** | جدول وصول: 35 مؤشراً دالياً (مراجع خصائص ColorScheme) بدل سلسلة when — التواقيع مجمدة |
| ColorMappings.kt | 74.7% → **26.0%** | إعادة تصميم كاملة: مصفوفة ARGB معبأة تُفك عند الطلب — 226 مرجعاً حية بلا مساس |
| MaterialYouFlags.kt | 100.0% (لم يُمسّ) | **معفى** — التبرير الإلزامي أدناه |
| build.gradle.kts | 81.0% → **25.0%** | providers.gradleProperty (مزلقة 4) + إعادة هيكلة إنشائية بدقة دلالية مطابقة |

**سطر تبرير إعفاء `MaterialYouFlags.kt` (إلزامي)**: الملف عقد بيانات تسلسلي بحت
(`@Serializable data class` بثلاث خصائص `@EncodeDefault` + `Saver` بجسم سطرين)
بلا أي منطق قابل لإعادة التصميم — أي إعادة كتابة له تعني حتماً تغيير أسماء
الخصائص أو أنواعها = كسر عقد التخزين (مفاتيح MaterialYouFlags في ملفات إعدادات
المستخدم) — الاستثناء موثق منذ step-0 (قسم «استثناءات القياس الموثقة»).

- شفافية: القياس الأول بعد إعادة كتابة ColorMappings كان **30.8% (فوق البوابة)**
  فحُزمت معاملات `neutralDynamicColorScheme` الأربعة المستقلة مع جيرانها (بترتيبها
  المجمد نفسه) وأعيد القياس = **26.0%** ✓.
- التواقيع المجمدة مثبتة بالبناء الكامل: `enum ColorPalette(val id)` بمدخلاته
  الـ35 + `colorNames` + `getColor`؛ `DEFAULT_GREEN` (بتاً: Color(0xFF4CAF50) ==
  Color(red=0x4C, green=0xAF, blue=0x50)) + `ColorMappings.colors: Array<Color>`
  + `getSystemAccent` (@RequiresApi(S)) + `systemAccentOrDefault` (**@Composable
  محفوظة — مزلقة 3**) + `neutralDynamicColorScheme` (13 معاملاً بترتيبها وقيمها
  الافتراضية، ودافع TonalSpot DRS باقٍ).

#### بوابات المرحلة 3 (ملف واحد = دفعة واحدة = بوابة كاملة)
| الدفعة | الملف | البوابة (assembleDebug + testDebugUnitTest + koverVerifyDebug) | النتيجة |
|---|---|---|---|
| 3a | ColorPalette.kt | أخضر | **1,780/0/131** |
| 3b | ColorMappings.kt | أخضر (exit صريح) | **1,780/0/131** |
| 3c | build.gradle.kts | أخضر (exit صريح) | **1,780/0/131** |
| 3d | الإغلاق (توثيق فقط) | أخضر (exit صريح) | **1,780/0/131** |

**مزلقة 19 (جديدة)**: `$?` بعد أنبوب `gradlew | tail` يلتقط خروج tail لا Gradle —
الفيصل هو exit العملية الكامل أو سطر BUILD SUCCESSFUL/FAILED (اكتُشفت في بوابة 3b
حيث ظهرت أسطر stacktrace دون فشل فعلي — أُعيد الفحص بexit صريح = أخضر).

#### المزالق المطبقة في المرحلة 3
- مزلقة 3: @Composable على systemAccentOrDefault — محفوظة ✓
- مزلقة 4: by project → providers.gradleProperty — مطبقة في build.gradle.kts ✓
- مزلقة 10: التطبيع الموحد — كل القياسات بالأداة الكانونية نفسها ضد المرجعين المثبتين ✓
- مزلقة 12/18: لا إخفاق عابر في هذه المرحلة؛ الأبواب موثقة بexit صريح ✓
- مزلقة 13: الضغط الجذري مسموح — استُخدم في تجميع المعاملات/الأسطر ✓

#### الجدول الكانوني بعد المرحلة 3
- `canonical_verdicts.csv`: صفّا verbatim/deeply-modified (ColorPalette/ColorMappings)
  → `rewritten-phase3` (5.6% / 26.0%) + build-file محدّث (81.0→25.0%) + ملاحظة
  `exempt:data-contract (phase3)` على MaterialYouFlags.
- `canonical_similarity.csv`: جديد (يُبنى تدريجياً من هذه المرحلة) — قبل/بعد لكل
  ملف بطوابع زمنية وإصدار الأداة.
- `THIRD_PARTY_NOTICES.md`: `lib/color` خرجت من Derived paths لقسم FlorisBoard
  عدا إفصاح `MaterialYouFlags.kt` الموثق.

### المرحلة 4: android — لم تبدأ
### المرحلة 5: compose — لم تبدأ
### بعد المرحلة 5: توقف إجباري (بوابات jetpref لاحقاً بموافقة صريحة)

═══════════════════════════════════════════════════════════

## 3) سجل الدفعات (يُملأ عند كل دفع)

| التاريخ (UTC) | المرحلة | commit | tag | ملاحظة |
|---|---|---|---|---|
| 2026-10-12 | step-0 (تأسيس) | d6ad581 | cleanup/step0 | أدوات + سجل + مراجع مثبتة |
| 2026-10-12 | 1a (الجدول الكانوني) | 36c4ff2 | — | canonical_verdicts.csv 163 صفاً |
| 2026-10-12 | 1b (استعادة الترويسات) | 1346140 | cleanup/phase1 | 132 ترويسة + بوابات أربع خضراء |
| 2026-10-13 | 2a (مراجعة ما قبل القتل) | 7595386 | — | بنود المراجعة + توقف إجباري: 5 مشكوك في براءتهم |
| 2026-10-13 | 2b (إصلاح قانوني — الخيار أ) | ef206428affe | — | ترويسات 5 ملفات + تصحيح تصنيفين + SHA-256 5/5 |
| 2026-10-13 | 2c (قتل فئة أ) | ee37796af7a8 | — | TextFieldPreference.kt (146 سطراً) |
| 2026-10-13 | 2d (قتل فئة ب) | 4b91c7b7d3fc | — | 10 تعريفات عليا (296 سطراً) |
| 2026-10-13 | 2e (إغلاق المرحلة 2) | (هذا الكومِت) | cleanup/phase2 | جداول + فئة ج=0 + كاشف ختامي 185 |
| 2026-10-13 | 3a (ColorPalette) | d95a072 | — | 100.0→5.6% + بوابة 1,780/0/131 |
| 2026-10-13 | 3b (ColorMappings) | 18b1736 | — | 74.7→26.0% + بوابة 1,780/0/131 |
| 2026-10-13 | 3c (build.gradle.kts) | f8eab62 | — | 81.0→25.0% + بوابة 1,780/0/131 |
| 2026-10-13 | 3d (إغلاق المرحلة 3) | (هذا الكومِت) | cleanup/phase3 | WORKLOG + إشعارات + إعفاء MaterialYouFlags موثق |
