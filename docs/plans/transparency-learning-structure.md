# خريطة هيكل فصل «الشفافية والتعلم» — تخطيط قبل التنفيذ (القسم 1)

- **الحالة:** خطة معروضة على المالك للموافقة — لا ملف كود إنتاجي في هذا الأمر
- **الأساس:** فرع `feature/transparency-hub` من وسم `v1.0.0` (كومِت
  `4602e592956d1789b26c731a7f9e0942db1bff09`)، خط أساس البوابة الخضراء
  **1780/0/131**
- **القرار الحاكم:** ADR-0009 (الرقم الفعلي التالي بعد 0008 — أمر المالك قال
  0010 مع تعليمة التحقق)
- **قواعد لا تُناقش:** صفر محرك جديد من الصفر · صفر صلاحية في AndroidManifest ·
  صفر كود في خدمة IME إلا نقطة اللمس المطولة للميزة 3 · كل البيانات الجديدة في
  `noBackupFilesDir` بلا شبكة إطلاقاً · strings ثنائية اللغة (`values/` عربية
  افتراضية + `values-en/` إنجليزية)

---

## 1) الميزة 1 — مركز الشفافية الحي

### نقاط القص في المحركات الموجودة (أسماء من الشجرة)
| المحرك | العنصر الفعلي | ما تقرأه الميزة |
|---|---|---|
| `drs/privacy/DrsNetworkSentinel.kt` | `DrsNetworkSentinel.attestation(absoluteMode, baseline, permissionsNow): Attestation` | عدّاد بايتات حي: `rxSinceBaseline`/`txSinceBaseline`/`rxSincePrevious`/`txSincePrevious`/`zeroTraffic`/`countersSupported` |
| نفسه | `DrsNetworkSentinel.captureBaseline()` (يستدعى من `DrsApplication.onCreate`) + `readTrafficSnapshot()` عبر `TrafficStats.getUidRxBytes/TxBytes(Process.myUid())` | خط الأساس العملية — لا تكرار قياس |
| `drs/privacy/DrsPrivacyDashboard.kt` | قائمة `stores: List<StoreProbe>` + ثوابت `STORE_LEARNING/STORE_CRASH/STORE_UPDATES/STORE_CLIPBOARD_MEDIA` | تسجيل المخزن الجديد عقداً إلزامياً (`STORE_TRANSPARENCY = "transparency_log"`) بـ`fileUsage()`/`destroyPath()` القائمتين |
| `app/settings/privacy/PrivacyScreen.kt` | قسم الإقرار الحي (سطر 277+: `privacy__attestation__title`) | النمط المرجعي لعرض الإقرار — الشاشة الجديدة تحاكي النمط لا تكرره |

### ملفات جديدة (مسارات دقيقة)
| المسار | الدور | نقاء |
|---|---|---|
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/privacy/DrsTransparencyLog.kt` | مخزن JSON محدود: لقطات إقرار يومية، دوران 90 يوماً، `noBackupFilesDir/drs_transparency_log_v1.json`، كتابة ذرية (tmp+rename نمط `DrsLearningEngine.persist`)، سقف بايتات | نقي بالكامل (ساعة وملف قابلان للحقن) |
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/privacy/DrsAgeCertificate.kt` | شهادة العمر: حساب نقي من `firstUseAt` (طابع يُكتب مرة واحدة في مخزن الشفافية عند أول تشغيل) + أيام السجل الإثباتية + إجمالي البايتات المسجلة | نقي 100% |
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/privacy/DrsCertificatePng.kt` | رسم الشهادة: `Bitmap.createBitmap` → `Canvas` (نصوص ثنائية الاتجاه، العربية أصيلة) → `compress(PNG)` → ملف في `cacheDir/transparency/` | أندرويد فقط — بلا مكتبة |
| `app/src/main/kotlin/com/drs/smartkeyboard/app/settings/privacy/TransparencyHubScreen.kt` | شاشة المركز: العدّاد الحي + آخر 90 يوماً + الشهادة + زر مشاركة | Compose — نمط `PrivacyScreen` |

### ملفات معدلة (وماذا يتغير بالضبط)
| الملف | التغيير |
|---|---|
| `app/src/main/res/xml/file_paths.xml` | إضافة `<cache-path name="transparency" path="transparency/"/>` — مورد، ليس Manifest، ليس صلاحية (نقطة قرار معلنة 2 في ADR-0009) |
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/privacy/DrsPrivacyDashboard.kt` | تسجيل `StoreProbe` خامس (`STORE_TRANSPARENCY`) في `android()` factory: قياس ملف السجل وتدميره |
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/DrsApplication.kt` (أو نقطة onCreate المعتمدة) | كتابة `firstUseAt` مرة واحدة + تدوين لقطة إقرار عند بدء/انتهاء العملية (منبّه app-scoped — صفر كود IME) |
| `app/src/main/res/values/strings.xml` + `values-en/strings.xml` | سلاسل المركز (الجدول أدناه) |

### تدفق البيانات (بلا شبكة)
`Sentinel.attestation()` → لقطة يومية واحدة على الأقل (عند بدء العملية + عند فتح
المركز) → `DrsTransparencyLog` (JSON، دوران 90) → الشاشة تقرأ → الشهادة تحسب →
PNG → `FileProvider` (نموذج مشاركة قياسي `Intent.ACTION_SEND`).

---

## 2) الميزة 2 — محرك التعلم المرئي

### نقاط القص في المحركات الموجودة
| المحرك | العنصر الفعلي | ما تقرأه الميزة |
|---|---|---|
| `drs/DrsMyLexicon.kt` | `search(entries, query)` + `order(entries)` + `removeAllAt(state)` + `validate(word, vocalized)` + `overridesOf(state)` | القاموس الشخصي قابل للبحث/الفرز — العقود نقية موجودة، صفر خوارزمية جديدة |
| `drs/ui/DrsMyLexiconScreen.kt` | `DrsMyLexiconScreen()` + `LexiconEntryRow` (سطر 345) | الشاشة القائمة تبقى؛ الشاشة الجديدة أوسع وتصل إليها |
| `drs/ai/DrsLearningEngine.kt` | `exportState()` + `exportTrigramState()` + `size()` + `bigramRowCount()` + `trigramRowCount()` + `clear(file)` | ملخص «ماذا تعلمت» + زر المسح الكامل |
| `lib/ext/DrsEd25519.kt` | `DrsEd25519.sign(seed, message)` / `verify(publicKey, message, signature)` / `publicKeyFromSeed(seed)` | توقيع/تحقق التصدير والاستيراد (RFC 8032، مثبت بمتجهات `DrsM03Ed25519VectorsTest`) |
| `lib/ext/DrsPackageTrust.kt` | `RELEASE_KEY_ID = "cto-drs-release-1"` + `RELEASE_PUBKEY_HEX` | المفتاح المثبت الأول المقبول عند الاستيراد |
| `packages/manifest.json` + `tools/drs_package_sign.py` | بنية `sha256` + `signature` (hex) + `pubkey_id` | البنية المعاد استخدامها حرفياً لملف التبادل |
| `ime/keyboard/KeyboardManager.kt` | `DrsCorrectionRevert.arm` (سطرا **904** و**1606**) + `plan` (سطر 725) | **نقطة قرار المالك 1 (ADR-0009):** استدعاء زيادة العدّاد `DrsCorrectionCounters.onAutoCommit(...)` عند موضعي arm فقط — سطران، لا منطق IME جديد، عزل بنيوي باختبار |

### ملفات جديدة
| المسار | الدور | نقاء |
|---|---|---|
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/ai/DrsCorrectionCounters.kt` | عدّادات «العد من الآن»: `autoCommits`/`reverts`/`startedAt`؛ JSON في `noBackupFilesDir/drs_correction_counters_v1.json`؛ بلا تاريخ رجعي بنيوياً (لا حقل تاريخي أصلاً) | نقي بالكامل |
| `app/src/main/kotlin/com/drs/smartkeyboard/drs/DrsLexiconExchange.kt` | تصدير/استيراد موقّع: payload {`schema`,`exported_at`,`entries`,`sha256`} + `signature` + `pubkey_id`؛ توليد بذرة مفتاح التصدير مرة واحدة في `noBackupFilesDir/drs_export_key_v1.bin`؛ استيراد fail-closed (مفتاح مجهول/توقيع فاشل/ملف غير موقّع/مدخل غير صالح → رفض برسالة واضحة) | النواة نقي JVM؛ الأندرويد فقط لقراءة الملف عبر SAF |
| `app/src/main/kotlin/com/drs/smartkeyboard/app/settings/dictionary/LearningAboutMeScreen.kt` | شاشة «ماذا تعلمت عني»: بحث/فرز القاموس + عدّادات صادقة + ملخص محرك التعلم + تصدير/استيراد + «امسح كل ما تعلمته» بتأكيد مزدوج | Compose |

### ملفات معدلة
| الملف | التغيير |
|---|---|
| `ime/keyboard/KeyboardManager.kt` | سطر زيادة عدّاد عند موضعي arm (904/1606) فقط — **بانتظار قرار المالك** |
| `drs/privacy/DrsPrivacyDashboard.kt` | تسجيل مخزن العدّادات (`STORE_CORRECTION_COUNTERS = "correction_counters"`) ليكون قابلاً للقياس والمسح من اللوحة أيضاً |
| ملاحة الإعدادات (نقطة تسجيل الشاشات المعتمدة — `HomeScreen`/تصنيف dictionary) | مدخل شاشة «ماذا تعلمت عني» |
| `values/strings.xml` + `values-en/strings.xml` | سلاسل الميزة 2 (الجدول أدناه) |

### مسح «كل ما تعلمته» (تأكيد مزدوج)
زر → حوار أول (شرح ما سيمحى) → حوار ثانٍ (تأكيد صريح باسم الإجراء) → تنفيذ:
`DrsLearningEngine.clear(learningFile())` + `DrsMyLexicon.removeAllAt` عبر مسار
الحالة المعتمد + `DrsCorrectionCounters.reset()` + مسح مخزن الشفافية اختيارياً؟
— **لا**: مخزن الشفافية ليس «تعلم» فيصير مربطاً مستقلاً في مركز الشفافية. ثم
`DrsPrivacyDashboard` يعكس الحالة فوراً لأن القياس حي.

---

## 3) الميزة 3 — لوحة الحركات المنبثقة

### نقاط القص في المحركات الموجودة
| المحرك | العنصر الفعلي | الدور |
|---|---|---|
| `ime/text/keyboard/TextKey.kt` | `computedPopups: MutablePopupSet<KeyData>` (سطر 28؛ الدمج 43–53) | **نقطة الدمج الوحيدة:** حقن حزمة الحركات الثماني عندما يكون المفتاح حرفاً عربياً |
| `ime/text/keyboard/TextKeyboardLayout.kt` | مسار الضغط المطول (سطرا **770–812**: `onLongPress` → `popupUiController.extend(key, size)` بشرط `isSuitableForPopups` ووجود popup keys) | **صفر تعديل** — الآلية القائمة ترسم أي حزمة: لوحة فوق الحرف، شدّ، سحب-وإفلات، إلغاء بإفلات خارجها |
| `ime/popup/PopupUiController.kt` + `PopupSet.kt` | `show`/`extend`/`hide`/`isSuitableForPopups` + `PopupSet(main, relevant)` | المحطة الرئيسة = الشدة (`main`)، والسبع البواقي `relevant` (نقطة قرار 3 في ADR-0009) |
| `app/AppPrefs.kt` | `keyboard__long_press_delay` (سطر 759، افتراضي **300**) + واجهته في `KeyboardScreen` (سطر 157) | العتبة — صفر إعداد جديد |
| `ime/input/InputEventDispatcher.kt` | `determineLongPressDelay(data)` (سطر 71) | مستهلك العتبة القائم — مضاعفات SPACE/SHIFT تبقى كما هي |
| `drs/ai/DrsArabicLetters.kt` | مجموعتا `SUN`/`MOON` (28 حرفاً) + `isMark` | بوابة «حرف عربي فقط» — اللاتينية مستثناة عمداً |

### ملفات جديدة
| المسار | الدور | نقاء |
|---|---|---|
| `app/src/main/kotlin/com/drs/smartkeyboard/ime/keyboard/DrsHarakatPanel.kt` | دالة نقية: `harakatPopupFor(label: String): List<TextKeyData>?` — الحرف العربي → الحركات الثماني U+064B..U+0652 (تنوين فتح/ضم/كسر، فتحة، ضمة، كسرة، سكون، شدة) كـ`TextKeyData` برموز الحركات، وغير العربي → null | نقي 100% — قابل للاختبار JVM بلا أصول |
| اختباراته (الجدول أدناه) | العزل البنيوي المفروض | — |

### ملفات معدلة (الحد الأدنى المعلن)
| الملف | التغيير | الحد |
|---|---|---|
| `ime/text/keyboard/TextKey.kt` | بعد بناء `computedPopups` الحالي: استدعاء `DrsHarakatPanel` وإضافة الحزمة للحرف العربي فقط | هذا هو «نقطة اللمس المطولة» كاملتها — لا سطر آخر في ime/ لهذه الميزة |
| `values/strings.xml` + `values-en/strings.xml` | وصف المحتوى لإتاحة المنبثقة على حروف العربية | — |

### العزل عن التنبؤ/التصحيح (لماذا لا يحتاج كود عزل)
محطات الحركات `TextKeyData` عادية برموز حركات — تدخل composer المسار العادي
للإدخال المباشر كما يدخل أي رمز من صفحة الرموز اليوم: لا تمر بصف الاقتراحات،
لا تُسلّح `DrsCorrectionRevert.arm` (ليست auto-commit)، ولا تستدعي
`DrsLearningEngine`. **العزل مفروض باختبارات بنيوية** تمنع أي مستقبلٍ إضافة
استدعاء تعلم/تصحيح في مسار الحركات (انظر الجدول).

---

## 4) خطة الاختبارات المفصلة (كلها JVM نقية ما أمكن — تُبنى على خط الأساس 1780/0/131)

### الميزة 1 — `DrsTransparencyLogTests` + `DrsAgeCertificateTests`
| النوع | الحالة |
|---|---|
| سلوكية | تسجيل لقطة → قراءة رجعة مطابقة (round-trip kotlinx.serialization)؛ لقطات عدة أيام → الإجماليات صحيحة |
| حدية — الدوران | كتابة يوم 91 → اليوم 1 محذوف؛ حجم الملف لا يتجاوز السقف مهما كُتب؛ طوابع خارج الترتيب لا تكسر الدوران |
| حدية — تلف | ملف مقطوع/JSON فاسد → يُرمى ويبدأ نظيفاً (نمط المحركات القائمة)؛ ملف بحجم فوق السقف → يُرمى |
| صدق | `countersSupported=false` (ROM يرجع -1) → السجل يدوّن «غير مدعوم» ولا يدّعي صفراً؛ `zeroTraffic=true` فقط حين الدلتا صفر فعلاً |
| بنيوي — عزل | فحص مصدر الميزة 1: صفر استيراد `java.net`/`okhttp`/`URLConnection` في ملفاتها؛ صفر صلاحية جديدة في Manifest (اختبار عقد Manifest القائم `DrsP3PrivacyTests` يبقى حارساً) |
| بنيوي — عقد اللوحة | `DrsPrivacyDashboard` يرجع المخزن الخامس في `summary()`؛ `deleteAll()` يمسحه |
| شهادة | `firstUseAt` غائب → يُكتب مرة واحدة ولا يتغير بعدها أبداً؛ الأيام = حساب نقي؛ إعادة توليد PNG متماثلة البنية (عدا طابع الوقت) |

### الميزة 2 — `DrsCorrectionCountersTests` + `DrsLexiconExchangeTests`
| النوع | الحالة |
|---|---|
| صدق «العد من الآن» | عدّاد جديد يبدأ صفراً؛ لا وجود لحقل تاريخي رجعي في النموذج إطلاقاً (اختبار انعكاس على بنية JSON)؛ التراكم على arm متتالية صحيح |
| حدية — تصدير | قاموس فارغ → رفض تصدير برسالة صادقة؛ مدخلات بتنوين/مسافات → `validate` يرفض ويُدرج رسالة الخطأ؛ الحجم فوق السقف → رفض |
| حدية — استيراد | **ملف بلا توقيع → رفض برسالة واضحة ثنائية اللغة**؛ **ملف معدّل بعد التوقيع (بايت واحد) → رفض** (sha256 داخل الموقّع + التحقق)؛ توقيع بمفتاح مجهول → رفض؛ `pubkey_id` مشوه → رفض؛ JSON سليم البنية بلا توقيع حقل التوقيع → رفض |
| حدية — استيراد سليم | ملف موقّع من مفتاح التصدير المحلي → يدخل؛ مدخلات فوق حدود `DrsMyLexicon` تُرفض مدخلاً مدخلاً والباقي يدخل مع تقرير صادق؛ استيراد مرتين → لا تكرار (مفتاح الكلمة المطبع) |
| توقيع | round-trip كامل: صدّر → عدّل → استورد (رفض)؛ صدّر → استورد (نجاح) — توقيع `DrsEd25519` يتحقق عبر المفتاحين المثبتين فقط |
| بنيوي — عزل IME | **فحص المصدر:** الملف الوحيد في `ime/` المسموح تعديله للميزة 2 هو `KeyboardManager.kt` بسطرَي استدعاء زيادة عند موضعي arm — اختبار يقرأ مصدر `KeyboardManager` ويرفض أي مرجع آخر لـ`DrsCorrectionCounters` خارج الموضعين؛ فشل الاختبار = البناء يفشل |
| مسح | «امسح كل ما تعلمته»: محاكاة ضغطتين (تأكيد مزدوج) → محرك التعلم فارغ (`size()==0`) + القاموس فارغ + العدّادات صفر + ملفات noBackupFilesDir محذوفة فعلاً؛ ضغطة واحدة → لا حذف |

### الميزة 3 — `DrsHarakatPanelTests` + عزل بنيوي
| النوع | الحالة |
|---|---|
| سلوكية | حرف عربي (ض/ص/س…) → حزمة الحركات الثماني بالترتيب الكانوني U+064B..U+0652، `main` = شدة؛ الحرف المجرد يبقى مفتاحاً طبيعياً |
| حدية — البوابة | حرف لاتيني/رقم/رمز/`isMark` → null (لا لوحة)؛ لوحة الرموز والتنوينات القائمة لا تتأثر («التنوينات كما هي») |
| حدية — Shift | مفتاح shift-حامل لا يفقد لوحته؛ التبديل بين shift states لا يكرر الحزمة ولا يلغيها |
| عتبة | `longPressDelay` الافتراضي 300 يصل `determineLongPressDelay` دون تغيير؛ مضاعفات SPACE/SHIFT القائمة لم تُمس (اختبار عقد القيم) |
| RTL والمنبثقة | محتوى اللوحة محايد اتجاهياً بيانياً (رموز حركات) والترتيب الكانوني ثابت؛ فحص أن بنية المحطات لا تعتمد قياسات اتجاهية من التخطيط الأب |
| بنيوي — عزل IME | **اختبار قراءة مصدر:** الملفات المسموح تعديلها في `ime/` للميزة 3 = `TextKey.kt` (نقطة الدمج) فقط؛ `TextKeyboardLayout.kt` بلا أي فرق عن v1.0.0 (hash check)؛ صفر مرجع لـ`DrsHarakatPanel` خارج `TextKey` واختباراته |
| عزل تنبؤ/تصحيح | بيانات محطات الحركات لا تحمل `SwipeAction` تعلمياً ولا تمر بصف الاقتراحات؛ اختبار يمنع استدعاء `DrsLearningEngine`/`DrsCorrectionRevert` من وحدة الحركات (فحص تبعيات المصدر) |

### بوابة الحقبة
كل اختبار جديد يُبنى على خط الأساس **1780/0/131** — أي نمو يُسجّل ويُقارن
(`TESTS=1780+N FAILURES=0 ERRORS=0 SKIPPED=0`)، وكل بناء `--no-daemon`
(مزلقة 29).

---

## 5) قائمة «ما لن يفعله هذا الفصل» (صريحة)

1. **لا آلية مضادة للتلاعب بالشهادة** — لا توقيع للصورة، لا QR تحققي، لا علامة
   مائية مشفرة؛ موثقة كبند عمل لاحق في ADR-0009 (البند 4).
2. **لا مزامنة سحابية للقاموس** — كل البيانات محلية `noBackupFilesDir`؛ المسار
   القائم `DrsSyncBundle` (E2E بمزامنة يدوية بين جهازين) كما هو بلا تغيير.
3. **لا استيراد حر** — غير الموقّع يُرفض دائماً؛ البديل المستقبلي (استيراد حر
   بتأكيد يدوي مفصّل) موثق في ADR-0009 فقط، لا كود.
4. **لا توصيات مشتقة من الشهادة** — الشهادة إفصاح، لا مدخل لأي محرك توصية.
5. **لا لوحة حركات للحروف اللاتينية** — البوابة `DrsArabicLetters` حصرية؛ أي
   توسيع لاحق أمر مالك مستقل.
6. **لا تاريخ رجعي للعدّادات** — «العد من الآن» حرفياً وبنيوياً.
7. **لا تعديل في `release.yml` أو أي CI** — ولا في `LifecycleInputMethodService`.
8. **لا تعديل ملفات التخطيط المثبتة** (`arabic.json` وغيرها) — الحقن برمجي نقي.

---

## 6) جدول الموارد

### سلاسل جديدة (تقريبياً — عربي `values/` + إنجليزي `values-en/`)
| المجموعة | العدد التقريبي | أمثلة المفاتيح |
|---|---|---|
| مركز الشفافية | ~22 | `transparency__title`، `transparency__live_bytes`، `transparency__log_days`، `transparency__certificate_share`، `transparency__unsupported` |
| الشهادة | ~8 | `certificate__title`، `certificate__age_days`، `certificate__zero_bytes_claim` |
| ماذا تعلمت عني | ~26 | `learned__title`، `learned__counters_started_at`، `learned__counters_from_now_note`، `learned__export`، `learned__import` |
| استيراد/رفض | ~10 | `learned__import_reject_unsigned`، `learned__import_reject_tampered`، `learned__import_reject_unknown_key` |
| المسح المزدوج | ~8 | `learned__erase_title`، `learned__erase_confirm_first`، `learned__erase_confirm_second` |
| الميزة 3 | ~4 | `harakat__popup_a11y` (وصف إتاحة) |
| **الإجمالي** | **~78** | كلها ثنائية اللغة من أول سطر، RTL أصيل |

### أصول
- **صفر أصل جديد** — أيقونات الشاشات من `androidx.compose.material.icons`
  (التبعية القائمة `libs.androidx.compose.material.icons`، المستخدمة فعلاً في
  `PrivacyScreen`: `DeleteForever`/`FileDownload`/`FileUpload`؛ المرشحات:
  `Icons.Outlined.Visibility` للشفافية، `Icons.AutoMirrored.Filled.ListAlt`
  للتعلم، `Icons.Filled.Grain` للحركات) — بلا مكتبة جديدة وبلا ملفات drawable.
- صورة الشهادة تُرسم وقت الطلب (`Canvas`) — لا أصل ولا قالب مضمّن.

---

## 7) ملخص ملفات الفصل كله

- **جديد: 8 ملفات كود** (4 للميزة 1، 3 للميزة 2، 1 للميزة 3) + 3–4 ملفات اختبار
  + ADR-0009 + هذا المستند.
- **معدل: ≤ 8 ملفات قائمة**، كلها بنقاط قص مسماة أعلاه، أكبرها سطران في
  `KeyboardManager.kt` (بانتظار قرار المالك) وسطران في `TextKey.kt` وتسجيلات
  `StoreProbe` وstrings.
- **صفر:** صلاحية، مكتبة، شبكة، كود في `LifecycleInputMethodService`، تعديل
  تخطيطات، تعديل CI.
