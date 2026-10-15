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

### المرحلة 4: android — مكتملة (انظر القسم 4 أدناه)
### المرحلة 5: compose — مكتملة (انظر القسم 5 أدناه)
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

═══════════════════════════════════════════════════════════

## 4) المرحلة 4: lib/android (21 ملفاً) — مكتملة

البروتوكول نفسه + قاعدة جديدة من درس المرحلة 3: كل بوابة تُعلن بـ
"BUILD SUCCESSFUL" نصياً + exit code صريح لأمر Gradle نفسه (`EC=$?` فوراً بعد
`./gradlew` بلا أنبوب — `scripts/gate.sh` خارج المستودع يطبق ذلك داخل
المستودع عبر السجلات والملخصات).

### الحالتان الخاصتان (قياس فقط — قرار المالك)
| الملف | الحكم | التوثيق |
|---|---|---|
| Toast.kt | أصلي DRS — 16.7% | أفضل مرشح في شجرة floris كلها `SnyggPropertySetEditor.kt` (مسح 400+ ملف كتأكيد الاستنساخ) — لا ترويسة، لا إعادة كتابة |
| SystemSettingObserver.kt | verbatim 100.0% — معفى | مراجع app=0 لكنها مستخدمة داخلياً (`AndroidSettingsHelper.observe/registerContentObserver`) — مبرأة موثقة: لا حذف ولا كتابة |

### المزلقات الجديدة (20–23)
- **مزلقة 20 (حل النداء)**: الوسائط المسماة داخل دالة امتداد تحسم لصالح
  امتداداتنا نفسها (`query(uri=..., projection=null,...)` التقط
  `ContentResolver.query` الخاصة بنا لا العضو الإطاري — NONE_APPLICABLE).
  العلاج: نداء موضعي `query(uri, null, null, null, null)` (العضو يتفوق
  دائماً على الامتداد عند التطابق).
- **مزلقة 21 (inline عام → خاص)**: دالة inline عامة لا تستطيع استدعاء دالة
  private inline — العلاج: `@PublishedApi internal` (نمط AndroidVersion
  sdkIntAtLeast/AtMost و enforceReadableCapacity).
- **مزلقة 22 (أسماء معاملات stubs الجافا)**: الكلاسات الجافا للإطار
  (ComponentName وSparseArrayCompat) وواجهات kotlinx ذات أسماء معاملات
  p0/p1 أو أسماء غير متوقعة (`encodeString(value=...)`) — الوسائط المسماة
  المفترضة (pkg/cls/key/string) تُسقط الترجمة. العلاج: نداء موضعي مع تجزئة
  الأسطر لكسر التطابق النصي.
- **مزلقة 23 (وسوم إغلاق XML)**: `</manifest>` سطر مطابق حرفياً يرفع
  التشابه (33.3%) — العلاج: دمج وسم الإغلاق مع سطر الإذن الأخير.

### دفعات المرحلة 4 (كل بوابة: BUILD SUCCESSFUL نصياً + exit=0 + 1,780/0/131 + kover)
| الدفعة | الملفات (قبل→بعد) | commit |
|---|---|---|
| 4a | AndroidInternalR 100→10.5 + Context 100→24.1 + ContentResolver 100→30.6 مرفوض→14.6 | a502eee |
| 4b | AndroidSettings 100→16.7 + AndroidAssetsManager 100→22.5 (فخ 15: bufferedReader محفوظ) + AndroidVersion 100→5.7 + Vibrator 100→29.2 | 267c265 |
| 4c | Clipboard 100→9.1 + Keyguard 100→25.0 + Configuration 100→25.0 + FileObserver 100→27.8 + PackageManager 94.1→25.0 + SparseArray 100→22.2 + UriSerializer 100→21.1 | 36e3b2f |
| 4d | build.gradle.kts 89.1→17.6 (فخ 4: providers.gradleProperty) + proguard 100→12.5 + Manifest 100→20.0 + .gitignore استثناء موثق 20.0 + consumer فارغ 0.0 | e3470b3 |

ملاحظة daemon: بوابة 4b الأولى سقطت بـ"Gradle build daemon disappeared
unexpectedly" — أُعيد التشغيل مرة واحدة وفق بروتوكول فخ daemon وخضرت.

### المرحلة 5: compose — لم تبدأ (توقف إجباري بانتظار رد المالك)
### بعد المرحلة 5: توقف إجباري (بوابات jetpref لاحقاً بموافقة صريحة)

═══════════════════════════════════════════════════════════

## 3-ب) سجل دفعات المرحلة 4 (ملحق للجدول أعلاه)

| التاريخ (UTC) | المرحلة | commit | tag | ملاحظة |
|---|---|---|---|---|
| 2026-10-13 | 4a (الثلاثي الحرج) | a502eee | — | 3 ملفات + mapping المرحلة 4 |
| 2026-10-13 | 4b (الكبار) | 267c265 | — | 4 ملفات + daemon عابر أعيد مرة |
| 2026-10-13 | 4c (الصغار) | 36e3b2f | — | 7 ملفات + مزلقة 22 |
| 2026-10-13 | 4d (بنية الوحدة) | e3470b3 | — | 5 ملفات بنية |
| 2026-10-13 | 4e (إغلاق المرحلة 4) | (هذا الكومِت) | cleanup/phase4 | الحالتان الخاصتان + إشعارات + مزلقات 20-23 |

═══════════════════════════════════════════════════════════

## 5) المرحلة 5: lib/compose (27 ملفاً) — مكتملة

البروتوكول نفسه: تمهيد كامل (purge@phase4 + JDK17.0.20.1 + SDK + المرجعان
المثبتان + سلامة أدوات 100/100/100 + أساس أخضر 1780/0/131) ثم سبع دفعات
إلزامية الترتيب، كل دفعة ببوابة كاملة نصية (BUILD SUCCESSFUL + exit صريح)
ثم push فوري. الملاحظة البيئية: العمليات الخلفية تُقتل عند حدود نداءات
الأدوات في هذه البيئة — الأبواب تُشغَّل أمامياً ضمن نافذة واحدة، وبيئة التشغيل
أُعيد تجميدها من الصفر (JDK/gradle-home) مع بقاء المستودع والمراجع.

### فخ rememberSettingState الموثق (لا ينطبق هنا)
فحص شجري كامل: الرمز `rememberSettingState` غير موجود في `State.kt` ولا في
الشجرة كلها (لا في floris المثبت) — المزلقة 7 من الجولة السابقة تعود لمرحلة
jetpref القادمة. ما ينطبق فعلاً من فخها في `State.kt`: الواجهة العامة تبقى
`State<R>` عبر التحويل الصريح، ومواقع الاستدعاء لم تتحرك سطراً (105 مستورداً
والأبواب الخضراء هي الدليل).

### المزلقات الجديدة (24–28)
- **مزلقة 24 (FQN الامتدادات)**: الدوال/القيم الامتدادية لا تُندَى بمسار
  مؤهل كامل (`composed`, `dp`) — الاستيراد إلزامي؛ المؤهَّل الكامل صالح فقط
  للفئات/الدوال العليا/الكائنات (`DisposableEffect`, `LocalContext.current`).
- **مزلقة 25 (نوع مواصفة الحركة)**: `expandVertically/shrinkHorizontally…`
  تحرّك `IntSize` لا `Float` — مساعد tween مشترك يجب أن يكون عاماً `<T>`.
- **مزلقة 26 (أيقونات المواد)**: `Icons.Filled.ErrorOutline` وأخواتها خصائص
  امتداد تحتاج استيراداً صريحاً؛ السلسلة المؤهلة بالكامل لا تحل الامتداد.
- **مزلقة 27 (كائن مجهول في composable عامة)**: كائن مجهول داخل دالة
  `@Composable` عامة `<R>` ينهار استنتاجه (`<anonymous><R>`) ولا يجتاز
  الوسائط — العلاج: كلاس خاص مسمى + `this@fun` لمستقبِل الامتداد داخل
  `DisposableEffect`.
- **مزلقة 28 (تسلسل observers)**: `DefaultLifecycleObserver` شقيق
  `LifecycleEventObserver` (كلاهما `LifecycleObserver`) لا ابنه — الوسيط
  المشترك للفصل/الإزالة يجب أن يكون `LifecycleObserver`.
- مطبقة سابقاً ومجدية: 2 (maxLineSpan/`LazyGridItemSpanScope` — وثّقت عند
  المساس بشبكات lazy)، 4 (providers.gradleProperty)، 6 (key() موضعياً —
  معاملها keys vararg)، 7 (State<R> عبر التحويل)، 12 (إعادة daemon)، 13
  (الضغط الجذري)، 19 (exit صريح)، 22 (أسماء stubs الجافا p0/p1 — أُبقيت
  النداءات موضعية على addObserver/removeObserver/setBounds).

### توضيحا المالك (منصوصان في THIRD_PARTY_NOTICES.md)
1. الإفصاحان الموثقان في lib/android بالاسم المضبوط: `Toast.kt` (أصلي DRS،
   قياسه 16.7% ضد أفضل مرشح) و`SystemSettingObserver.kt` (حرفية 100.0%
   معفاة بمبررها الموثق).
2. نعم: `SystemSettingObserver.kt` مذكورة فعلاً في NOTICES (فقرة المرحلة 4)
   وستبقى 100.0% دائماً — إفصاح قانوني دائم.

### دفعات المرحلة 5 (كل بوابة: BUILD SUCCESSFUL نصياً + exit=0 + 1,780/0/131)
| الدفعة | الملفات (قبل→بعد) | commit |
|---|---|---|
| 5a | DpSpSizeFuns 100→10.0 + DrawScope 100→12.5 + DisposableLifecycleEffect 100→16.7 + RtlModifiers 100→26.7 + Modifier 100→5.9 + DrsBulletSpacer 95.8→28.6 + DrsChip 98.1→15.2 + DrsButtonBar 96.6→14.0 + DrsDropdownLikeButton 97.9→25.0 + ClickableModifiers 100→23.1 | 951c927 |
| 5b | DrsCanvasIcon 92.9→14.3 + LazyGrid 100→25.0 (فخ الأرضية الخمسية) + DrsCards 87.2→24.2 + DrsButtons 92.4→26.7 + DrsAnimation 100→17.2 | 00eb8aa |
| 5c | ScrollableModifiers 96.6→16.0 + DrsAppBar 44.4→22.5 | 2ad01c2 |
| 5d | Resources.kt 98.9→20.9 (العملاق 1/3 — 172 مرجعاً) | e616329 |
| 5e | DrsStepLayout.kt 93.0→25.3 (العملاق 2/3 — 190 مرجعاً) | 127ca1c |
| 5f | State.kt 100→4.1 (العملاق 3/3 — 211 مرجعاً) | 5c062b2 |
| 5g | build.gradle.kts 86.0→0.0 + proguard-rules.pro 100→11.1 (+ consumer فارغ 0.0 ذكراً فقط) | cd7bd02 |

ملاحظات daemon: بوابات 5b و5e و5g سقطت كل واحدة مرة بـ"daemon disappeared
unexpectedly" — أُعيد تشغيل كل منها مرة واحدة وفق فخ daemon وخضرت (مزلقة 12).

### الحالات غير المكتوبة (المحميون الأربعة + consumer)
`Tooltip.kt` و`TooltipDefaults.kt` (AOSP بترويسة مزدوجة الأصل) + `MotionScale.kt`
(DRS أصلي) + `KeyboardForwardDelete.kt` (AOSP — فخ خاصية الامتداد ForwardDelete
المستخدمة في ComputingEvaluator، مزلقة 8): لا ترويسة ولا كتابة ولا حذف —
تحقق صفر-فروق ضد تاغ cleanup/phase4. `consumer-rules.pro` فارغ بالتصميم
(0.0%) — ذكراً فقط.

### الحصيلة الكانونية بعد المرحلة 5
- `canonical_similarity.csv`: 22 صفاً للمرحلة 5 (قبل/بعد) — كلها ≤30%.
- `canonical_verdicts.csv`: 22 صفاً → `rewritten-phase5` + المحميون الأربعة
  كما هم + consumer بلا مقابل.
- `THIRD_PARTY_NOTICES.md`: `lib/compose` خرجت من Derived paths تماماً —
  يتبقى كمشتقات FlorisBoard: `lib/kotlin` و`lib/snygg`؛ ويتبقى من الإفصاحات:
  jetpref (4 وحدات — مرحلة قادمة) + `MaterialYouFlags.kt` +
  `SystemSettingObserver.kt`.

### التوقف الإجباري بعد المرحلة 5
لا بوابات jetpref ولا كود DrsPrefs قبل موافقة المالك الصريحة. الخطوة الموثقة
التالية عند الموافقة: قياس مساحة API لوحدات jetpref ثم القرار.

### بوابة jetpref 1 — قياس مساحة API (قراءة فقط — قرار المالك)
- التمهيد: استئناف `cleanup/phase5` @ 0444f65 (صفر فروق) + بوابة تحقق خضراء `BUILD SUCCESSFUL` exit=0 **1780/0/131**.
- الأداة: `tools/cleanup/measure_jetpref_api_usage.py` — بناء جديد كامل بعد رفض مسودة جلسة سابقة غير مدفوعة لعيوبها الأربعة المثبتة: (1) صفوف لكل تصريح بدل تجميع overloads فكرر كل استخدام في stats، (2) اقتناص معاملات النمط `<T>` كأسماء رموز، (3) مستخرج val/var يقفز فوق الاسم الحقيقي ليأخذ النوع/`by`، (4) عدّ FQ داخل أسطر الاستيراد + غياب عمق الأقواس (التقاط تصريحات محلية داخل الأجسام). الميلادة الجديدة: تصريحات عليا فقط بعمق أقواس 0 + دمج overloads بـ(package, symbol) + مقبض مستقبلات عامة `DataStore<T>.initAndroid` / `PreferenceData<V>.collectAsState` + annotation class + alias واحد.
- المخرجات (كلها أدوات/قياسات، صفر كود): `measurements/jetpref_api_usage.csv` (148 صفاً) + `jetpref_api_usage_stats.txt` + `jetpref_gate1_report.md` (المقارنة الثلاثية + توصية 5 أسطر).
- الأرقام الكانونية: 148 صفاً كانونياً (161 تصريحاً؛ 103 عامة) — 41 مستخدماً (27.7% من الكل / 39.8% من العام) — 90 ملفاً مرجعاً (89 app/src + 1 lib/compose) — 223 سطر استيراد — FQ بلا استيراد: ملفان فقط — UI = 89.2% من 1051 نقطة — أعلى رمز: collectAsState 241/39.
- المعالج (القلب المفصلي): مشغل واحد `@Preferences` (من datastore-model لا من وحدة المعالج) — استخدام واحد في الشجرة كلها (AppPrefs.kt:74) — المولَّد صنف وحيد بخاصية وحيدة (سجل 202 مدخلاً / 230 سطراً) — الاستهلاك انعكاس `Class.forName(+ "Impl")` وصفر مراجع صريحة — التبعية الكلية = 3 أسطر في ملف واحد. معزول، ليس منتشراً.
- الاختبارات: 7 ملفات × رمز واحد (jetprefDataStoreOf على نموذج الإنتاج ذاته — لا نماذج اختبار مستقلة).
- مصائد الذاكرة: 6 ادعاءات اختبرت بنيوياً — 3 «غير منطبق»، 3 «مزعوم خاطئ» (التفصيل في التقرير §4).
- الدفع: أدوات + قياسات + تقرير، **بلا تاغ** وفق الأمر.
- التوقف: كامل بعد التقرير — القرار الثلاثي (أ/ب/ج) للمالك وحده. لا بوابات ولا كود DrsPrefs قبل موافقة صريحة.

### قرار jetpref النهائي — الخيار (ج): الإبقاء القانوني (قرار المالك المحسم 2026-10-14)

بعد تقرير بوابة jetpref 1 (`tools/cleanup/measurements/jetpref_gate1_report.md`)
ومقارنته الثلاثية، حسم المالك: **jetpref يبقى مكوّناً مساعداً مُندمجاً
مُصرَّحاً به بترويساته الأصلية المستعادة** — لا إطار DrsPrefs أصلي ولا معالج
KSP جديد. الأرقام الثلاثة الحاسمة خلف القرار:

1. **89.2%** من نقاط الاستخدام (938/1051) في طبقة UI التفضيلية — أي مسار
   أصلي تُسعَّر كلفته الحقيقية على 39 ملفاً / 6,081 سطراً، لا على المعالج.
2. **3 أسطر** في ملف واحد (AppPrefs.kt) هي كل تبعية على مولّد KSP — تبعية
   معزولة لا تبرر وحدها أي إعادة كتابة.
3. **صفر** أثر على 1,780 اختباراً في مسار الإبقاء (مقابل 7 ملفات اختبار
   معرضة في المسارين الأصليين).

التنفيذ التبعي للقرار:
- THIRD_PARTY_NOTICES.md: قسم jetpref رُقيّا إلى إعلان اعتماد كامل (الهاش
  المثبت + طبيعة النسخ المدمجة KMP-unified + جدول الملفات المشتقة 48 صفاً
  من canonical_verdicts.csv + روابط الترخيص والإشعار الكامل) — جدول
  canonical_verdicts.csv نفسه **لم يُمَس** (سجل الامتثال).
- README.md: قسم «مكوّنات الطرف الثالث المصرَّح بها» ثنائي اللغة (عربي +
  English في About) — إسناد صريح بلا أي عبارة اشتتالٍ عن لوحة المفاتيح نفسها.
- التحقق التطبيقي (R8/انعكاس): آلية تحديث التفضيلات في الواجهة =
  `PreferenceData.collectAsState()` → `asFlow().collectAsState()` (جمع Flow
  في Compose)، وإنشاء المخزن عبر `jetprefDataStoreOf` الانعكاسي عند بدء
  التطبيق — الانعكاس على المسار الحرج فعلاً. قواعد keep المطلوبة **قائمة
  أصلاً بطبقتين موثقتين**: `app/proguard-rules.pro` (v1.18.0:
  `-keep class * extends PreferenceModel` + keepclassmembers `<init>()`
  بتعليق يشرح سبب الانعكاس Class.forName(+Impl)) + قاعدة consumer في
  `lib/jetpref/datastore-model/proguard-rules.pro` موصولة بـ
  consumerProguardFiles. الاختبار الدخاني الآلي = مهمة
  `validateReleaseR8Dex` القائمة (مسح بايتات dex المصغّر عن العلامات، ومنها
  `Lcom/drs/smartkeyboard/app/DrsPreferenceModelImpl;` وTypingScreenKt) —
  نفّذت مع بوابة الإغلاق أدناه مع فحص mapping.txt.
- المسار المستقبلي الموثق لمن أراد الاستثمار يوماً:
  `jetpref_gate1_report.md` (المقارنة الثلاثية + تقديرات بنيوية).

### إغلاق المشروع (2026-10-14)
**برنامج تنظيف lib/ مكتمل ومختوم** بقرار المالك (ج) — كومِت الإغلاق `392d028` +
تاغ `cleanup/complete` — البوابات الختامية: debug **1780/0/131** exit=0 + release
`BUILD SUCCESSFUL` مع R8 وvalidateReleaseR8Dex وإثبات keep في mapping.txt —
مسح توكن صارم: صفر في الشجرة وصفر في التاريخ (130 كومِتاً) — التقرير الختامي:
`tools/cleanup/delivery_report.md`. لا عمل لاحق بدون أمر صريح جديد من المالك.

---

## حقبة الإطلاق v1.0.0 — إلحاق لاحق بأمر المالك الصريح (ما بعد تاغ cleanup/complete)

> هذا المقطع أُلحق بعد تجميد الأرشيف بأمر مالك صريح (موافقة المرحلة ب).
> كل ما فوقه هو سجل التاريخ المجمد — لم يُعد عليه أي سطر. المرجع المجمد
> الأبدي: تاغ `cleanup/complete` @ `585c035`.

### مزلقة 29 (OOM الـ daemon في بيئة البناء)
ـ Gradle daemon في بيئة البناء هذه يموت بـ OOM أثناء البوابات الطويلة
  (assembleDebug + testDebugUnitTest + koverVerifyDebug معاً) — النبات
  `Killed` بلا تشخيص. العلاج المثبت: تشغيل كل بوابة بـ `--no-daemon`
  (عملية واحدة تنتهي مع البناء، لا ذاكرة مقيمة تُقتل). طبّق في
  `scripts/gate.sh` وكل بوابات حقبة الإطلاق. القاعدة: فشل عابر مرة
  واحدة = إعادة تشغيل واحدة قبل أي تشخيص (مزلقة 12)، وفشل بنمط
  Killed/OOM = `--no-daemon` مباشرة (هذه المزلقة).


### جرد ما قبل الحذف العام — 36 وسماً و36 إصداراً (أمر المالك، القسم ب/2)

المصدر: `git ls-remote` + GitHub API على المستودع العام `DRS-SmartKeyboard`،
بتاريخ 2026-10-15 17:22 UTC،
قبل أي حذف. الهاشان: كائن الوسم الموسوم ثم الكومِت المفكوك. عمود الضمانة = نتيجة
`git merge-base --is-ancestor <كومِت الوسم> release/v1.0.0` محلياً على المستودع العملي.

| # | الوسم | كائن الوسم | الكومِت | الضمانة | اسم الإصدار | النشر (UTC) | المرفقات |
|---|-------|-----------|--------|---------|-------------|-------------|----------|
| 1 | `v1.0.0` | `74d6f408d2ac` | `d69ddf9c0fa1` | OK | DRS Smart Keyboard v1.0.0 — الإطلاق الأول العام | 2026-10-02T08:07:52Z | 1 |
| 2 | `v1.1.0` | `b7114b86ac43` | `b7114b86ac43` | OK | DRS Smart Keyboard V 1.1.0 | 2026-10-02T10:01:37Z | 11 |
| 3 | `v1.2.0` | `3b893cbc103a` | `d0fda80f5ba6` | OK | DRS Smart Keyboard V 1.2.0 | 2026-10-02T19:04:45Z | 11 |
| 4 | `v1.3.0` | `c9a353c6e1dc` | `4ca0a82c3ce0` | OK | DRS Smart Keyboard V 1.3.0 | 2026-10-02T23:25:44Z | 11 |
| 5 | `v1.4.0` | `62317164feb4` | `f426321fc9eb` | OK | DRS Smart Keyboard V 1.4.0 | 2026-10-03T00:37:24Z | 11 |
| 6 | `v1.5.0` | `ef96ea5bac8d` | `ccc0645194dd` | OK | DRS Smart Keyboard V 1.5.0 | 2026-10-03T01:52:35Z | 11 |
| 7 | `v1.6.0` | `e829c8cb154a` | `9c6376f7d01f` | OK | DRS Smart Keyboard V 1.6.0 | 2026-10-03T03:33:26Z | 11 |
| 8 | `v1.7.0` | `3422e5e07e86` | `8728b3f4a984` | OK | DRS Smart Keyboard V 1.7.0 | 2026-10-03T04:50:33Z | 11 |
| 9 | `v1.8.0` | `a59e5435bd6d` | `cabb9a7a7bb3` | OK | DRS Smart Keyboard V 1.8.0 | 2026-10-03T07:51:56Z | 11 |
| 10 | `v1.9.0` | `f7c2c17026b9` | `1e2d2533a33d` | OK | DRS Smart Keyboard V 1.9.0 | 2026-10-03T10:03:35Z | 11 |
| 11 | `v1.10.0` | `e1622bb6c528` | `f2fdfdc0d491` | OK | DRS Smart Keyboard V 1.10.0 | 2026-10-03T11:38:42Z | 11 |
| 12 | `v2.0.0` | `01d91ec3664a` | `aeda9b8fa030` | OK | DRS Smart Keyboard V 2.0.0 | 2026-10-03T16:27:06Z | 11 |
| 13 | `v2.1.0` | `dfdf75c0e2fc` | `c19e47d49286` | OK | DRS Smart Keyboard V 2.1.0 | 2026-10-03T22:46:15Z | 11 |
| 14 | `v2.2.0` | `530071ca7caa` | `235abec7f921` | OK | DRS Smart Keyboard V 2.2.0 | 2026-10-04T10:40:02Z | 11 |
| 15 | `v2.2.2` | `cfede1eaddf6` | `cfede1eaddf6` | OK | DRS Smart Keyboard V 2.2.2 | 2026-10-05T00:13:52Z | 13 |
| 16 | `v2.3.0` | `310f0503d9bf` | `a7bbeb280158` | OK | DRS Smart Keyboard V 2.3.0 | 2026-10-05T04:42:24Z | 11 |
| 17 | `v2.4.0` | `482d244af1cd` | `c63f141df346` | OK | DRS Smart Keyboard V 2.4.0 | 2026-10-05T05:58:33Z | 11 |
| 18 | `v2.5.0` | `2380962de5e4` | `2380962de5e4` | OK | DRS Smart Keyboard V 2.5.0 | 2026-10-05T06:33:17Z | 11 |
| 19 | `v2.6.0` | `14b8bf5d1875` | `14b8bf5d1875` | OK | DRS Smart Keyboard V 2.6.0 | 2026-10-05T08:25:46Z | 11 |
| 20 | `v2.7.0` | `0d785704c93c` | `0d785704c93c` | OK | DRS Smart Keyboard V 2.7.0 | 2026-10-05T09:17:49Z | 11 |
| 21 | `v2.8.0` | `2fb09ffa0e61` | `2fb09ffa0e61` | OK | DRS Smart Keyboard V 2.8.0 | 2026-10-05T10:51:54Z | 11 |
| 22 | `v2.9.0` | `558099ccbc54` | `857653357ac6` | OK | DRS Smart Keyboard V 2.9.0 | 2026-10-05T12:59:51Z | 11 |
| 23 | `v2.10.0` | `7053fede9b6c` | `0b85fab3bc46` | OK | DRS Smart Keyboard V 2.10.0 | 2026-10-06T07:09:46Z | 11 |
| 24 | `v2.11.0` | `78b96371e4be` | `14b37c1c18eb` | OK | DRS Smart Keyboard V 2.11.0 | 2026-10-06T08:33:59Z | 11 |
| 25 | `v2.12.0` | `c20710ae07dd` | `70a74c9f2a58` | OK | DRS Smart Keyboard V 2.12.0 | 2026-10-06T12:26:01Z | 11 |
| 26 | `v2.13.0` | `83fc7672e525` | `663c0f2ec977` | OK | DRS Smart Keyboard V 2.13.0 | 2026-10-07T02:00:37Z | 11 |
| 27 | `v2.14.0` | `d3b81b7dc072` | `12da9cb87665` | OK | DRS Smart Keyboard V 2.14.0 | 2026-10-07T09:26:53Z | 11 |
| 28 | `v2.15.0` | `89adcf01355b` | `7b6dc3edc70e` | OK | DRS Smart Keyboard V 2.15.0 | 2026-10-07T10:44:39Z | 11 |
| 29 | `v2.16.0` | `04278f4a75c1` | `04278f4a75c1` | OK | DRS Smart Keyboard V 2.16.0 | 2026-10-07T11:49:35Z | 11 |
| 30 | `v2.17.0` | `93ab47fb89c0` | `d35b76af6bb8` | OK | DRS Smart Keyboard V 2.17.0 | 2026-10-07T23:11:49Z | 11 |
| 31 | `v2.18.0` | `58030b4d8d15` | `a0dc193ab372` | OK | DRS Smart Keyboard V 2.18.0 | 2026-10-08T01:01:58Z | 11 |
| 32 | `v2.19.0` | `295ef1fca119` | `dd062e72be3e` | OK | DRS Smart Keyboard V 2.19.0 | 2026-10-08T03:59:32Z | 11 |
| 33 | `v2.20.0` | `6f6decc8633b` | `2883c403684f` | OK | DRS Smart Keyboard V 2.20.0 | 2026-10-08T05:54:08Z | 11 |
| 34 | `v2.20.1` | `817b5ad4b864` | `ee756ccf2848` | OK | DRS Smart Keyboard V 2.20.1 | 2026-10-09T02:38:53Z | 11 |
| 35 | `v2.21.0` | `6516f063ca74` | `d1b6439edc1d` | OK | DRS Smart Keyboard V 2.21.0 | 2026-10-09T05:00:35Z | 11 |
| 36 | `v2.22.0` | `93fa20b0f638` | `1108d82e44d6` | OK | DRS Smart Keyboard V 2.22.0 | 2026-10-09T09:08:52Z | 11 |

**حالتان خاصتان**: (1) `v1.0.0` القديم — إصدار «الإطلاق الأول العام» المنشور
2026-10-02T08:07:52Z بمرفق وحيد `DRS-SmartKeyboard-v1.0.0.apk`؛ حذفه يزيل تصادم
اسم v1.0.0 نهائياً. (2) `v2.2.2` — إصدار بمرفقات بأسماء قديمة إضافية:
`DRS-SmartKeyboard-v2.2.2-debug.apk` + `DRS-SmartKeyboard-v2.2.2-release.apk`
إلى جانب apk/aab القياسيين. بقية الـ34 إصداراً بنمط موحد: apk + aab + 8 حزم
flex + SHA256SUMS.txt.

**نتيجة ضمانة الأجدادية: 36/36 ناجحاً، صفر فشل — المسار آمن للتنظيف العام.**

### نتيجة التنظيف العام (ما بعد الحذف)
- حُذفت **36/36 إصداراً** من المستودع العام (API، كل حذف 204 DELETED، صفر فشل) — الإصدارات
  أولاً ثم الوسوم.
- ثم حُذفت **36/36 وسم الإصدار** (API git/refs، كل حذف 204 DELETED، صفر فشل).
- التحقق اللحظي بعد الحذف: `ls-remote --tags` على العام = **صفر وسوم** + قائمة releases =
  **فارغة تماماً**.
- سجل التاريخ المجمد أعلاه هو المرجع الوحيد الباقي لهذه الأسماء والهاشات — والوسوم نفسها
  تظل كائنات في تاريخ المستودع العملي (أسلاف لفرع الإطلاق) بلا مراجع عامة.
