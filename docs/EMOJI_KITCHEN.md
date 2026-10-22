# مطبخ الإيموجي — DRS Emoji Kitchen

> الميزة: تجربة تركيب إيموجي أصلية + مكتبة ملصقات محلية مرخّصة — كلها
> على الجهاز، بلا شبكة، بلا أصول Google، وبلا واجهات خاصة غير موثقة.
> الحالة: منفذة — الدفعة 19.

## 1) المبدأ الحاكم (لماذا هذه التجربة «أصلية»)

- **لا أصول Google ولا Emoji Kitchen الرسمي:** التركيب هنا خوارزمية
  تخطيط محلية مغلقة ([DrsEmojiKitchen]) — نقارب رمزَي إيموجي من خط
  النظام نفسه في قِناع رسم Canvas الذي نملكه، بترتيبات مغلقة (13 وصفة
  بنيوية). الناتج صورة مرسومة فعلًا على الجهاز، لا صورة منسوخة ولا
  تركيبة «وهمية» معروضة كأنها حقيقية.
- **الصمت الصادق:** زوج فئات بلا وصفة في الكتالوج = لا معاينة ولا زر
  إرسال — أبدًا لا نعرض تركيبة مزيّفة (نفس عقيدة «الأصمّ بحكم
  التصميم» في ذراع L4).
- **الحتمية:** نفس الزوج = نفس الخطة بايت-بايت (الاهتزاز الزاوي مشتق
  من hash مستقر للزوج، ضمن مدى مغلق ‎±7.5°).
- **بلا صلاحيات جديدة:** الإرسال عبر `InputConnection.commitContent`
  (API عام موثق منذ 25) بـFileProvider المعلن أصلًا، مع سلسلة تسقّط
  صادقة: صورة → نص (الإيموجي نفسهما نصًّا) → إشعار بما حدث.

## 2) المكوّنات

| الطبقة | الملف | العقد |
|---|---|---|
| المحرك الخالص | `drs/DrsEmojiKitchen.kt` | 8 فئات مغلقة، 48 مشاركًا (6 لكل فئة)، 13 وصفة، `plan()` حتمية، `suggestions()`، بحث عربي مُطبَّع |
| مكتبة الملصقات | `drs/DrsStickerLibrary.kt` | فهرس `assets/drs/kitchen/stickers.json` بتحقق fail-closed (ترخيص إلزامي)، 6 تصنيفات، بحث |
| مخطط الإرسال | `drs/DrsKitchenSendPlanner.kt` | قرار المسار: CONTENT / TEXT_PAIR من أنواع MIME للحقل والإعداد |
| الذاكرة | `drs/DrsKitchenMemory.kt` | مفضلة + سجل (سقف 24، إزالة تكرار)، تفضيل JetPref مخصص، السجل لا يُدوَّن في التخفي |
| الراسم | `drs/ui/DrsKitchenArt.kt` | Canvas: خطة → PNG حقيقي؛ متجه ملصق → PNG؛ حفظ في cache/emoji-kitchen |
| اللوحة | `drs/ui/DrsEmojiKitchenPanelUi.kt` | اللوحة السابعة: تبويبان (تركيبات/ملصقات) بنفس تشريح اللوحات الذكية |

## 3) كتالوج الوصفات (مغلق — لا يزيد ولا ينقص إلا بقرار موثق)

كل وصفة = زوج فئات (الأولى أساس، الثانية زينة) → طبقتان: موضع مغلق
(11 موضعًا معرَّفًا: BASE_CENTER، TOP_BACK، FRONT_CENTER، …) + مقياس
مغلق [0.30..1.00]. الأزواج الحالية:

`FACE+HEART، FACE+FACE، FACE+HANDS، FACE+FOOD، ANIMAL+FACE،
HEART+FACE، HEART+OBJECT، HEART+ANIMAL، HEART+FOOD، OBJECT+SYMBOL،
WEATHER+FACE، SYMBOL+SYMBOL، HANDS+HEART`

كل زوج آخر = صمت صادق (يُثبت بالمحاكاة `scripts/check_emoji_kitchen.py`
وباختبار العقد `DrsEmojiKitchenTests`).

## 4) مكتبة الملصقات: الترخيص وطريقة إضافة حزمة

- **الحزمة الحالية:** `drs-kitchen-originals` v1.0.0 — 12 ملصقًا
  متجهيًا أصليًا من تأليف هذا المشروع حصرًا (لا مصدر خارجي)، مرخّصة
  بموجب رخصة المشروع [Apache-2.0]، في `res/drawable/st_kitchen_*.xml`.
- **الفهرس:** `app/src/main/assets/drs/kitchen/stickers.json` — مخطط
  `schemaVersion 1`: كتلة `pack` (id، nameAr، nameEn، version، author،
  **license إلزامي**، licenseNote) + قائمة `stickers` (id فريد،
  `drawable` بنمط `st_kitchen_[a-z0-9_]+`، category من الستة المغلقة،
  keywords عربية وإنجليزية غير فارغة).
- **إضافة حزمة جديدة:** (1) صمّم المتجهات في `res/drawable/` بالنمط
  نفسه، (2) أضف مدخلاتها إلى `stickers.json` (id وdrawable فريدان
  وعالميًا عبر الحزم — ادمج الحزم في مصفوفة واحدة، الفهرس حزمة
  منطقية واحدة حاليًا)، (3) وثّق الترخيص في كتلة `pack`، (4) شغّل
  `scripts/check_emoji_kitchen.py` ثم اختبار `DrsStickerLibraryTests`
  — أي مخالفة مخطط = رفض fail-closed عند التحميل.

## 5) آلية الإرسال (متوافقة مع Android)

1. **المسار الكامل:** راسم Canvas → PNG في `cacheDir/emoji-kitchen/`
   → `FileProvider` (authority القائم، مسار cache مضاف: `emoji-kitchen/`)
   → `EditorInstance.commitImageContent` عبر
   `InputConnectionCompat.commitContent` بـ`INPUT_CONTENT_GRANT_READ_URI_PERMISSION`.
2. **بوابة الدعم:** تُقرأ `EditorInfo.contentMimeTypes` للحقل النشط —
   لا يقبل `image/png`؟ → تسقّط صادق: `commitText` للزوجين نصًّا
   (التكوين النصي الحقيقي للتركيبة) + toast يصرّح بما حدث.
3. **التخفي:** السجل لا يُدوَّن في التخفي (كعدادات كل اللوحات)؛
   المفضلة بقاء صريح بإرادة المستخدم.

## 6) الإثبات

- المحاكاة: `scripts/check_emoji_kitchen.py` (563 فحصًا أخضر) —
  مصدر حقيقة واحد يُصدر فهرس الملصقات نفسه.
- اختبارات العقد: `DrsEmojiKitchenTests` + `DrsStickerLibraryTests` +
  `DrsKitchenSendPlannerTests` + `DrsKitchenMemoryTests` +
  `DrsKitchenSurfaceTests` (الأخير يثبت الأسطح: KeyCode -235،
  ImeUiMode 10، ذيل الكتالوج، تعادل السلاسل، خمسة أذونات ما تزال خمسة).
