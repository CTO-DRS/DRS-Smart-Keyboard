# تقرير بوابة jetpref 1 — قياس مساحة API (قراءة فقط)

الحالة: مكتمل — صفر تعديل على ملفات الكود، صفر لمس لملفات jetpref.
البوابة: `BUILD SUCCESSFUL` + `EXIT_CODE=0` + **1780/0/131** (استئناف من `cleanup/phase5` @ 0444f65، شجرة نظيفة صفر فروق، مرجعا floris fe1241f / jetpref 7a3df4e).

---

## 1. مهمة 1 — جرد الاستخدام الفعلي

الأداة: `tools/cleanup/measure_jetpref_api_usage.py` (قراءة فقط، حتمية، تحذف التعليقات ومحارف النصوص قبل العدّ بنفس عقد ماسح التشابه).
المخرجات: `measurements/jetpref_api_usage.csv` (148 صفاً كانونياً) + `measurements/jetpref_api_usage_stats.txt`.

### 1.1 الأرقام الكانونية

| القياس | القيمة |
|---|---|
| ملفات استهلاك ممسوحة (app/src + benchmark + lib/android + lib/color + lib/compose + lib/kotlin + lib/snygg) | 585 |
| ملفات مرجِعة لـjetpref | **90** = 89 app/src + 1 lib/compose |
| أسطر استيراد org.drs.jetpref.* | **223** |
| استخدام مؤهل بالكامل خارج أسطر الاستيراد | **ملفان فقط**: `app/.../ext/KeyboardLayoutEditorScreen.kt` (JetPrefAlertDialog) و `lib/compose/.../DrsDropdownLikeButton.kt` (JetPrefTextField/JetPrefDropdownMenuDefaults) |
| صفوف API كانونية (package+اسم فريد؛ overloads مدمجة) | **148** صفاً = 161 تصريحاً — من 3 وحدات: datastore-model / datastore-ui / material-ui |
| صفوف عامة | 103 |
| رموز مستخدمة فعلاً | **41** = 27.7% من 148 — 39.8% من 103 العامة |
| توزيع الرموز المستخدمة (بالوحدة) | model 13 / ui 16 / material 12 |
| توزيع نقاط الاستخدام (1051 نقطة) | model 113 / ui 787 / material 151 → **حصة طبقة UI = 89.2%** |
| اختبارات (app/src/test) | **7 ملفات × رمز واحد** (`jetprefDataStoreOf` على نموذج الإنتاج ذاته — لا نماذج اختبار مستقلة) |
| benchmark / lib/android / lib/color / lib/kotlin / lib/snygg | **صفر** ملفات |
| استيراد ب alias | واحد: `collectAsState as jetprefCollectAsState` في `ime/window/ImeWindow.kt` (عُدّ ضمن ملفات collectAsState الـ39) |
| استيرادات wildcard | صفر (فحص دفاعي في الأداة) |

### 1.2 أعلى 10 رموز استخداماً

| الرمز | النوع | الوحدة | النقاط | الملفات |
|---|---|---|---|---|
| collectAsState | composable-extension-fun | datastore-ui | 241 | 39 |
| SwitchPreference | composable-fun | datastore-ui | 128 | 16 |
| Preference | composable-fun | datastore-ui | 108 | 21 |
| ListPreference | composable-fun | datastore-ui | 83 | 16 |
| PreferenceGroup | composable-extension-fun | datastore-ui | 83 | 19 |
| jetprefDataStoreOf | fun | datastore-model | 58 | 8 |
| DialogSliderPreference | composable-fun | datastore-ui | 46 | 9 |
| listPrefEntries | composable-fun | datastore-ui | 44 | 2 |
| JetPrefAlertDialog | composable-fun | material-ui | 42 | 15 |
| JetPrefTextField | composable-fun | material-ui | 32 | 7 |

الخلاصة البنيوية: الاستخدام واسع الأفق (90 ملفاً) لكنه مكثّف في **مصنّفات UI التفضيلية** (8 من أعلى 10)؛ الطبقة النموذجية المستهلكة في الإنتاج محصورة في AppPrefs.kt والمصنع و7 ملفات اختبار.

---

## 2. مهمة 2 — تحليل معالج KSP (datastore-model-processor)

### 2.1 ما الذي يولده (بالاسم والشكل — من قراءة ModelProcessor.kt كاملاً، 262 سطراً، ومن المولَّد الفعلي بعد البناء)

- المشغِّل الوحيد: التضمين `@Preferences` — وهو معرَّف في **datastore-model** (`org.drs.jetpref.datastore.annotations.Preferences`، ملف Annotations.kt) وليس في وحدة المعالج؛ وحدة المعالج تملك ثابت الاسم فقط.
- لكل صنف مجرّد موسوم: ملف واحد `<Model>Impl.kt` في نفس الحزمة.
- الصنف المولَّد: `class DrsPreferenceModelImpl : DrsPreferenceModel()` — **لا دوال ولا أصناف أخرى**.
- المحتوى = خاصية override واحدة: `declaredPreferenceEntries: Map<PreferenceModel.TypedKey, PreferenceData<*>>` + كتلة `init` تبني:
  1. `entries = listOf(prop to "path.name", ...)` — يمشي على كل خاصية من نوع `PreferenceData` داخل النموذج، مع recursive استدعاء للحاملات المتداخلة (clipboard/editor/correction/suggestion/voice/privacy/theme) — **202 مدخلاً اليوم**؛
  2. حارس مفاتيح مكررة يرمي `PreferenceModelDuplicateKeyException`؛
  3. `declaredPreferenceEntries = entries.map{...}.associateBy { TypedKey(it.type, it.key) }`.
- حجم المولَّد الحالي: **230 سطراً** (كله قائمة مدخلات + الحارس).
- آلية الاستهلاك: **انعكاس بحت** — `jetprefDataStoreOf` (JetPrefJvm.kt) ينفذ `Class.forName(modelClass.qualifiedName + "Impl")` ثم `getDeclaredConstructor().newInstance()`. **صفر مراجع صريحة للصنف المولَّد في الشجرة كلها**.

### 2.2 عدّ رموز التضمين في app/src

| الرمز | المواضع | الدليل |
|---|---|---|
| `@Preferences` | **1 فقط** في الشجرة كلها: AppPrefs.kt:74 (فوق `abstract class DrsPreferenceModel : PreferenceModel()`) | rg على app+lib |
| `jetprefDataStoreOf(DrsPreferenceModel::class)` | 1 إنتاجي (AppPrefs.kt:72) + 7 ملفات اختبار (كلها على نموذج الإنتاج نفسه) | rg على app/src |
| تسجيل المعالج | خدمات SPI قياسية + `ksp(projects.lib.jetpref.datastoreModelProcessor)` في app/build.gradle.kts:254 | قراءة |

### 2.3 جواب التركز بالأرقام

**معزول، وليس منتشراً.** كل تبعية على المولِّد = **3 أسطر في ملف واحد** (AppPrefs.kt: سطرا استيراد + سطر التضمين + سطر المصنع)، وصفر مراجع مباشرة للناتج المولَّد (الربط الوحيد اتفاق تسمية انعكاسي `+ "Impl"`). لا وجود لأي أثر للمولِّد في واجهات التطبيق أو الشاشات أو الخدمات.

### 2.4 البديل اليدوي — تقدير بنيوي (من قراءة المولَّد فعلياً؛ لا تخمين)

- **شكل 1 — explicit factory + سجل مكتوب يدوياً:** ملف `DrsPreferenceModelImpl.kt` يدوي بنفس شكل المولَّد (~230 سطراً) + حذف سطرَي التضمين + استبدال المصنع باستدعاء مباشر. **أسطر app/src المتغيرة: 3 أسطر + ملف جديد واحد؛ صفر تغيير في ملفات الاستهلاك التسعين**. الكلفة الجارية: كل تفضيل يُضاف/يُحذف يتطلب تحديث السجل يدوياً (202 مدخلاً) — وخطر صمت حقيقي: كشف المكررات موجود في الحارس، أما **كشف الإسقاط فغير موجود** (تفضيل منسي في السجل يفقد استمراريته دون أي خطأ ظاهر).
- **شكل 2 — تفويض بانعكاس زمن التشغيل:** Impl من ~40 سطراً يمشي على `memberProperties` (kotlin-reflect) ويعيد بناء السجل ذاته — يلغي الصيانة اليدوية وخطر الإسقاط، لكنه يضيف تبعية kotlin-reflect (حجم + كلفة بدء تشغيل، حساسة للوحة مفاتيح).
- **شكل 3 — معالج KSP أصلي (إعادة استهداف):** ModelProcessor معزول ذاتياً (KSP + KotlinPoet فقط)، وترابطه بـjetpref = **4 ثوابت FQN** (~10–15 سطراً). إعادة استهدافه لإطار أصلي أرخص من شكل 1 وأخف من شكل 2.

---

## 3. مهمة 3 — تقرير القرار الثلاثي

حجم jetpref الكلي: 39 ملفاً / 6,081 سطراً (model 1,691 + ui 2,356 + material 1,772 + processor 262).

| المعيار | (أ) DrsPrefs أصلي بلا معالج KSP + تعديل AppPrefs | (ب) معالج KSP أصلي جديد | (ج) الإبقاء المشروع: jetpref مستنسخ بترويسات الإفصاح |
|---|---|---|---|
| الكلفة (أسطر) | ~5,800–6,100 سطراً أصلياً جديداً (39 ملفاً، بشرط ≤30% تشابك) + إعادة توصيل 90 ملفاً / 223 سطر استيراد + سجل يدوي ~230 سطراً | = (أ) **ناقص** السجل اليدوي **زائد** إعادة استهداف المعالج 10–15 سطراً | 0 — الترويسات مستعادة منذ phase2 وTHIRD_PARTY_NOTICES يفصح عن jetpref منذ phase5 |
| الكلفة (أيام) | 10–15 يوم عمل موثق (بافتراض سرعة المراحل 3–5 وبوابة لكل دفعة) | 10.5–16 | 0 |
| الخطر | **مرتفع**: 89.2% من نقاط الاستخدام في طبقة UI تتطلب parity سلوكياً؛ + خطر صمت إسقاط مداخل السجل؛ + مخاطر دلالية في core (مهاجرات/مسلسلات/ملاحظة الحالة) | مرتفع (كأ) **ناقص** خطر السجل؛ تبعية KSP قائمة أصلاً (Room) | **صفر** — الوضع الراهن أخضر 1780/0/131 |
| التوفير | إسقاط 6,081 سطراً مستنسخاً من الشجرة | نفس (أ) | يحفظ 6,081 سطراً كمشتق موثق Apache-2.0 |
| أثر 1,780 اختباراً | 7 ملفات اختبار تمر عبر نفس المصنع والنموذج — صفر تعديل إن حُفظت توقيعات الواجهة؛ وإلا 7 أسطر استدعاء؛ الخطر الجوهري دلالي في core وليس في عدّ الاختبارات | كالعمود (أ) | صفر |

### التوصية (5 أسطر)

1. الأرقام تقول إن الكلفة الحقيقية في **طبقة UI** (938/1051 نقطة = 89.2%)، لا في مولّد الكود — أي قرار native يجب أن يُسعَّر على أساس 39 ملفاً و6,081 سطراً، لا على أساس المعالج.
2. تبعية المولِّد = 3 أسطر في ملف واحد + انعكاس اسم؛ **ليست قوة دافعة كافية وحدها** لأي إعادة كتابة.
3. (أ) يُهيمن عليه (ب) بنيوياً: نفس كلفة إطار العمل كاملة مقابل توفير معالج من 262 سطراً فقط، مع إضافة خطر سجل يدوي صامت — **(أ) غير مبرر بأي قراءة للأرقام**.
4. إن قرر المالك مساراً أصلياً يوماً ما، ف**(ب) هو الطريق** (أرخص من (أ) ويزيل خطر السجل)؛ أما اليوم فالأرقام تدعم (ج).
5. **(ج)**: كلفة صفر / خطر صفر / أثر اختبارات صفر، والتوافق القانوني قائم (Apache-2.0 + ترويسات مستعادة + إفصاح THIRD_PARTY_NOTICES) — القرار النهائي للمالك وحده، والقياس يتوقف هنا تماماً.

---

## 4. منهجية — مصائد الذاكرة المختبرة بنيوياً (لا تُعتمد إلا بالإثبات)

| المزعوم (ذاكرة) | الاختبار البنيوي | الحكم |
|---|---|---|
| الاستخدام المؤهل FQ منتشر بلا استيرادات | مسح FQ خارج أسطر الاستيراد على 585 ملفاً | **غير منطبق**: ملفان فقط |
| استيرادات wildcard لـjetpref | rg + فحص دفاعي في الأداة | **غير منطبق**: صفر |
| benchmark يستهلك jetpref | rg على benchmark | **غير منطبق**: صفر ملفات |
| الصنف المولَّد مُشار إليه في مصدر app | rg `DrsPreferenceModelImpl` | **مزعوم خاطئ**: 0 — انعكاس فقط |
| نماذج اختبار مستقلة موسومة @Preferences | rg على app/src/test | **مزعوم خاطئ**: الاختبارات تستخدم نموذج الإنتاج ذاته |
| lib/kotlin أو lib/snygg تستهلك jetpref | rg | **غير منطبق**: صفر |

## 5. التحقق المتقاطع للأرقام (مستقل عن الأداة)

- أسطر الاستيراد: الأداة 223 = rg 223.
- الملفات المرجعة: الأداة 90 = rg 89 (app/src) + 1 (lib/compose).
- عينات عدد الملفات لكل رمز: JetPrefTextField 7=7، jetprefDataStoreOf 8=8، DialogSliderPreference 9=9.
- رموز مفتاحية كانت مفقودة من مسودة سابقة وأصبحت مقيسة: collectAsState (241/39)، initAndroid (2)، @Preferences (2)، ExperimentalJetPrefDatastoreUi (24)، PreferenceGroup (83).
