#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
check_l3_sentence.py — المرجعية المحاكاة لذراع L3 «توقع الكلمة التالية الأعمق»
الدفعة 16 — قبل البناء (عقيدة الجلسة: المحاكاة أولًا والمرجع حين تختلف عن التوقع اليدوي)

يغطي أربعة قوانين مغلقة:
  1. قانون حد الجملة (freshStart): كتالوج النهايات المغلق {. ! ؟ ? …} + المغلقات {» " ' ) ] }
     — نافذة ذيلها نهايةٌ (بعد نزع الفراغ والمغلقات) = بداية جملة جديدة؛
     كلمة بعد آخر نهاية = منتصف جملة (المسار القديم حرفيًا — عقد اللا-op).
  2. قانون سلسلة الترقيم: آخر كلمة ترقيماً (بعد التطبيع المطابق لـ LatinWordNormalize)
     → تتمتها الحتمية من السلسلة المغلقة (أولًا→ثانيًا→…→ثامنًا)، وحد الجملة يسكتها.
  3. قانون مرشحي البداية الجديدة: أعلى مفردات المستخدم (عدّ تنازلي، رابط كسر التعادل
     = الكلمة) ثم بدايات الجملة المغلقة (14 بداية بترتيب ثابت) — ثقة 0..1 حتمية.
  4. السبب المغلق 1.13 في مستشار البلاطات: «جمل لكل سطر» يرشَّح فقط تحت عقد الإطلاق
     المضمون (التحويل سيغيّر النص فعلًا) + حرسا الالتباس: طول ≤ 256 وحدّان فأكثر
     (صنف الالتباس: الاختصارات «ا.د» بحدٍّ واحد تصمت).
"""

import itertools
import random
import re
import sys
import unicodedata

# ---------------------------------------------------------------------------
# الكتالوجات المغلقة — المرآة الحرفية لما سيُزرع في Kotlin
# ---------------------------------------------------------------------------

TERMINALS = {'.', '!', '؟', '?', '…', '۔'}       # نهايات الجملة المغلقة (مرآة SENTENCE_TERMINATORS بالمزود)
CLOSERS = {'»', '"', "'", ')', ']', '}'}       # مغلقات تُطوى عند فحص الذيل
TAIL_WINDOW = 64                                # نافذة الذيل المحدودة (مسار ساخن رخيص)

# سلسلة الترقيم المغلقة (8 حلقات) — صيغة العرض الطبيعية
ORDINAL_CHAIN = ["أولًا", "ثانيًا", "ثالثًا", "رابعًا", "خامسًا", "سادسًا", "سابعًا", "ثامنًا"]

# بدايات الجملة المغلقة (14) — بترتيب ثابت، صيغة العرض
OPENERS = ["كان", "قال", "ثم", "لكن", "أما", "كذلك", "عندما",
           "بعد", "قبل", "الآن", "اليوم", "ربما", "إذن", "هذا"]

# حد الافتتاحيات في مسار البداية الجديدة
OPENERS_TAKE = 4
PERSONAL_TAKE = 4

# مرآة LatinWordNormalize (الهمزات، التاء المربوطة، المقصورة، التشكيل، اللاتيني صغير)
def normalize(word: str) -> str:
    out = []
    for c in word:
        o = ord(c)
        if 0x064B <= o <= 0x065F or c == '\u0670' or c == '\u0640' or \
           0x06D6 <= o <= 0x06ED or 0x08F0 <= o <= 0x08F3:
            continue  # تشكيل/تطويل/علامات قرآنية: تُسقط
        if c in 'أإآٱ':
            out.append('ا')
        elif c == 'ة':
            out.append('ه')
        elif c in 'ىئ':
            out.append('ي')
        elif c == 'ؤ':
            out.append('و')
        elif c in 'یك':
            out.append('ي' if c == 'ی' else 'ك')
        else:
            out.append(c.lower())
    return ''.join(out)

# مرآة SENTENCE_BOUNDARY من DrsTextTools: نهاية + فراغ أفقي + نص بعدها
SENTENCE_BOUNDARY = re.compile(r'([.!؟?…])[ \t\u00A0]+(?=[^\s])')

# مرآة isWordInternalChar الحقيقية (p6-A1): isLetter + الحركات 064B..065F + الألف الخنجرية 0670
# + الفواصل العليا — وكل ما عداها ينهي الكلمة (الترقيم العربي ؟ ، ؛ ليس حرفًا —
# المحاكاة اصطادت العيب: المدى الخام 0600..06FF كان يحسبها حروفًا)
def is_word_char(c: str) -> bool:
    o = ord(c)
    return c.isalpha() or (0x064B <= o <= 0x065F) or o == 0x0670 or c == "'" or c == '\u2019'

# ---------------------------------------------------------------------------
# القانون 1+2: محلل حالة الجملة (مرآة DrsSentenceContext.analyze)
# ---------------------------------------------------------------------------

def last_word_index(tail: str):
    """مسح آخر كلمة من اليمين (واعٍ داخل الكلمة) — يعني (بداية، نهاية) أو None."""
    end = len(tail)
    while end > 0 and not is_word_char(tail[end - 1]):
        end -= 1
    if end == 0:
        return None
    start = end
    while start > 0 and is_word_char(tail[start - 1]):
        start -= 1
    return (start, end)

def analyze(before: str):
    """يعيد (freshStart, ordinalSuccessor) — حتمي كليًا.
    القانون: آخر كلمة في الذيل؛ ما بعدها (بعد طي الفراغ) إن كان كله نهايات/مغلقات
    = بداية جملة جديدة؛ إن كان فارغًا أو فيه ما ليس نهاية = منتصف جملة؛
    وحد الجملة يسكت الترقيم، والترقيم لا يُطرد إلا في منتصف الجملة."""
    if not before.strip():
        return (False, None)
    tail = before[-TAIL_WINDOW:]
    lw = last_word_index(tail)
    if lw is None:
        return (False, None)
    trailing = tail[lw[1]:]
    stripped = ''.join(ch for ch in trailing if not ch.isspace())
    fresh = len(stripped) > 0 and all(ch in TERMINALS or ch in CLOSERS for ch in stripped)
    ordinal = None
    if not fresh:
        nw = normalize(tail[lw[0]:lw[1]])
        for i, disp in enumerate(ORDINAL_CHAIN):
            if normalize(disp) == nw:
                if i + 1 < len(ORDINAL_CHAIN):
                    ordinal = ORDINAL_CHAIN[i + 1]
                break
    return (fresh, ordinal)

# ---------------------------------------------------------------------------
# القانون 3: مرشحو البداية الجديدة (مرآة مسار المزود)
# ---------------------------------------------------------------------------

MAX_WORD_COUNT = 200.0

def fresh_start_candidates(personal, is_arabic, limit):
    """personal: [(word, count)] مفروزة سلفًا (عدّ تنازلي ثم كلمة تصاعديًا).
    الإخراج: [(word, confidence)] — أعلى المفردات ثم الافتتاحيات."""
    out = []
    for w, c in personal[:PERSONAL_TAKE]:
        if len(out) >= limit:
            break
        out.append((w, min(0.8, max(0.2, c / MAX_WORD_COUNT))))
    if is_arabic:
        for opener in OPENERS:
            if len(out) >= limit:
                break
            out.append((opener, 0.25))
    return out

def personal_top(words_map, limit):
    """مرآة personalTopWords: عدّ تنازلي ثم رابط كسر التعادل = الكلمة تصاعديًا."""
    return sorted(words_map.items(), key=lambda kv: (-kv[1], kv[0]))[:limit]

# ---------------------------------------------------------------------------
# القانون 4: السبب المغلق 1.13 — ترشيح «جملة لكل سطر» بعقد إطلاق مضمون
# ---------------------------------------------------------------------------

def sentence_per_line_apply(text: str) -> str:
    return SENTENCE_BOUNDARY.sub(lambda m: m.group(1) + '\n', text)

def nominate_sentence_per_line(text: str) -> bool:
    """عقد الإطلاق نفسه (نمط 1.7–1.11): يرشَّح فقط ما سيطلق فعلًا،
    مع حرس المسار الساخن (طول ≤ 256) وحرس الالتباس (حدّان فأكثر —
    الاختصارات بحدٍّ واحد تصمت صادقًا)."""
    if not text or len(text) > 256:
        return False
    after = sentence_per_line_apply(text)
    if after == text:      # ما لا يطلق لا يرشَّح (سطر لكل جملة سلفًا / لا نهايات)
        return False
    return len(SENTENCE_BOUNDARY.findall(text)) >= 2

# ---------------------------------------------------------------------------
# الفحوص
# ---------------------------------------------------------------------------

checks = 0
fails = []

def ok(cond, msg):
    global checks
    checks += 1
    if not cond:
        fails.append(msg)

# ---- 1) قانون حد الجملة: موجبات مغلقة
fresh_cases = [
    "كتبت الرسالة. ",
    "كتبت الرسالة.",
    "انتهى الأمر؟",
    "انتهى الأمر۔",
    "هل وصلتك!",
    "ننتظر…",
    'قال: "انتهى."',
    "انتهى.» ",
    "The end. ",
    "Done?",
]
for t in fresh_cases:
    f, o = analyze(t)
    ok(f is True, f"fresh موجب فشل عند: {t!r} → {f}")

# ---- 2) قانون حد الجملة: سالبات مغلقة (منتصف جملة = المسار القديم)
mid_cases = [
    "كتبت الرسالة",
    "انتهى؟ طيب",   # كلمة بعد النهاية = منتصف الجملة الجديدة (سياق ثنائي صالح)
    "كتبت الرسالة، ثم",
    "الرسالة: ثم",
    "الرسالة؛ ثم",
    "في الطريق",
    "«الرسالة",
    "",
    "   ",
    "…",
    ".",
    "1250",
]
for t in mid_cases:
    f, o = analyze(t)
    ok(f is False, f"fresh سالب فشل عند: {t!r} → {f}")
    ok(o is None, f"ترقيم يجب أن يصمت عند: {t!r} → {o}")

# فاصلة/نقطتان/فاصلة منقوطة ليستا نهاية — السياق يبقى صالحًا
for sep in ['،', ':', '؛']:
    f, _ = analyze(f"كتبت الرسالة{sep} ")
    ok(f is False, f"الفاصل {sep} ليس نهاية جملة")

# «۔» نهاية صحيحة، والفاصل العربي «،» يبقى وسط الجملة
f, _ = analyze("كتبت الرسالة۔ ")
ok(f is True, "الوقف الكامل «۔» نهاية جملة")

# ---- 3) سلسلة الترقيم: الحتمية عبر السلسلة كلها
for i in range(len(ORDINAL_CHAIN) - 1):
    cur, nxt = ORDINAL_CHAIN[i], ORDINAL_CHAIN[i + 1]
    f, o = analyze(f"نبدأ {cur}")
    ok(f is False and o == nxt, f"سلسلة الترقيم فشلت: {cur} → {o} (المتوقع {nxt})")
    # بعد فاصلة عربية أيضًا
    f2, o2 = analyze(f"نبدأ {cur}، ")
    ok(o2 == nxt, f"الترقيم بعد الفاصلة فشل: {cur}، → {o2}")

# نهاية السلسلة: ثامنًا لا تتمة له (صمت صادق لا ابتداع)
f, o = analyze("ثم ثامنًا")
ok(o is None, f"نهاية السلسلة يجب أن تصمت: {o}")

# متغايرات التطبيع تصل للحلقة ذاتها
for variant in ["اولا", "أولا", "أولًا", "اَولاً"]:
    f, o = analyze(f"نبدأ {variant}")
    ok(o == "ثانيًا", f"متغاير التطبيع فشل: {variant} → {o}")

# حد الجملة يسكت الترقيم (جملة انتهت بترقيم = انتهى العد لا يستمر تخمينًا)
f, o = analyze("خلص العد ثامنًا. ")
ok(f is True and o is None, f"إسكات الترقيم عند الحد فشل: fresh={f} ord={o}")

# غير الترقيم لا يخترع تتمة
for w in ["كتاب", "القلم", "مشروع"]:
    f, o = analyze(f"في {w}")
    ok(o is None, f"كلمة غير ترقيمية اخترعت تتمة: {w} → {o}")

# ---- 4) مرشحو البداية: الترتيب والثقة والحدود
pm = {"الكتاب": 5, "العمل": 200, "مشروع": 5, "خير": 1}
top = personal_top(pm, 4)
ok([w for w, _ in top] == ["العمل", "الكتاب", "مشروع", "خير"],
   f"ترتيب personalTop فشل: {top}")

cands = fresh_start_candidates(top, True, 8)
ok(len(cands) == 8, f"طول المرشحين فشل: {len(cands)}")
ok([w for w, _ in cands][:4] == ["العمل", "الكتاب", "مشروع", "خير"], "المفردات أولًا")
ok([w for w, _ in cands][4:] == OPENERS[:4], f"الافتتاحيات بترتيبها: {cands[4:]}")
ok(cands[0][1] == 0.8 and cands[3][1] == 0.2, f"ثقة المفردات فشلت: {cands}")
ok(all(0.0 <= c <= 1.0 for _, c in cands), "عقد الثقة 0..1 انكسر")

# قصور الحد: limit=6 يقصّ الافتتاحيات آخرًا
c6 = fresh_start_candidates(top, True, 6)
ok(len(c6) == 6 and [w for w, _ in c6][4:] == OPENERS[:2], f"قص الحد فشل: {c6}")

# لاتيني: بلا افتتاحيات عربية
cL = fresh_start_candidates(top, False, 8)
ok([w for w, _ in cL] == ["العمل", "الكتاب", "مشروع", "خير"], f"اللاتيني بلا افتتاحيات: {cL}")

# مفردات فارغة: الافتتاحيات وحدها (اللغة طابق أرضي) — ومفردات+بلا عربية = صفوف مفردات فقط
c0 = fresh_start_candidates([], True, 5)
ok([w for w, _ in c0] == OPENERS[:5], f"الافتتاحيات وحدها فشلت: {c0}")
ok(fresh_start_candidates([], False, 5) == [], "فارغ كليًا = صمت صادق")

# ---- 5) السبب 1.13: عقد الإطلاق + حرسا الالتباس
pos13 = [
    "كتب الرسالة. ثم غادر. وعاد",          # حدّان: توقيع اللصق الحقيقي
    "س1؟ س2؟ س3",                            # نهايات عربية
    "one. two? three",                       # لاتيني
    "أولًا! ثانيًا! ثالثًا",                 # تعجب
]
for t in pos13:
    ok(nominate_sentence_per_line(t) is True, f"1.13 موجب فشل: {t!r}")

neg13 = [
    "",                                       # فارغ
    "جملة واحدة فقط. ",                      # حدٌّ واحد (وسيبتعد بالتتمة — نص منتهٍ)
    "جملة واحدة فقط.",
    "نص بلا نهايات هنا",
    "ا.د. محمد",                              # صنف الالتباس: اختصار بحدّين متلاصقين؟ لا فراغ بعدهما
    "كلمتان معاها هنا",                       # لا تواقيع
    "سطر\nجديد. بالفعل\nمقسوم",              # مقسوم سلفًا على الأسطر → الأداة لا تغير
]
for t in neg13:
    ok(nominate_sentence_per_line(t) is False, f"1.13 سالب فشل: {t!r}")

# اختصار «ا.د» وسط جملتين حقيقيتين: الأداة ستطلق فعلًا → الترشيح صادق (عقد الإطلاق أعلن من الخوف)
ok(nominate_sentence_per_line("ا.د. محمد كتب. ثم غادر. وعاد") is True,
   "1.13: لصق حقيقي فوق اختصار يرشَّح")

# الحدان فأكثر شرط: حد واحد صامت حتى لو سيطلق
one = "كتب. ثم غادر"
ok(len(SENTENCE_BOUNDARY.findall(one)) == 1 and nominate_sentence_per_line(one) is False,
   "1.13: حد واحد يجب أن يصمت")

# الطول: 257 محرفًا يصمت (حرس المسار الساخن)
long_text = ("جملة قصيرة. " * 26)  # > 256
ok(len(long_text) > 256 and nominate_sentence_per_line(long_text) is False, "1.13: حرس الطول فشل")

# ---- 6) عقد اللا-op التوليدي: منتصف الجملة على حالات مولَّدة
random.seed(20261021)
ar_letters = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
gen_mid_fail = 0
for _ in range(1200):
    n = random.randint(1, 9)
    w = ''.join(random.choice(ar_letters) for _ in range(n))
    tail = random.choice(["", " ", "، ", ": ", "؛ ", " «"])
    before = f"في {w}{tail}"
    f, o = analyze(before)
    if f is not False or o is not None:
        gen_mid_fail += 1
        if gen_mid_fail <= 5:
            fails.append(f"لا-op توليدي فشل: {before!r} → fresh={f} ord={o}")
        checks += 1
checks += 1  # عدّاد الكتلة
ok(gen_mid_fail == 0, f"عقد اللا-op التوليدي: {gen_mid_fail} حالة انكسرت")

# ---- 7) توليد حد الجملة: أي كلمة + نهاية (± فراغ/مغلقات) = بداية جديدة
gen_fresh_fail = 0
for _ in range(800):
    w = ''.join(random.choice(ar_letters) for _ in range(random.randint(2, 7)))
    term = random.choice(['.', '!', '؟', '?', '…'])
    closer = random.choice(['', '"', '»', ')'])
    space = random.choice(['', ' ', '  '])
    before = f"{w}{term}{closer}{space}"
    f, _ = analyze(before)
    if f is not True:
        gen_fresh_fail += 1
        if gen_fresh_fail <= 5:
            fails.append(f"حد الجملة التوليدي فشل: {before!r} → fresh={f}")
checks += 1
ok(gen_fresh_fail == 0, f"الحد التوليدي: {gen_fresh_fail} حالة انكسرت")

# ---- 8) السلسلة تحت التوليد: تتمة حتمية لكل حلقة بألف تنوينة
chain_fail = 0
for i in range(len(ORDINAL_CHAIN) - 1):
    for variant in [ORDINAL_CHAIN[i], normalize(ORDINAL_CHAIN[i])]:
        before = f"ثم {variant} "
        f, o = analyze(before)
        if o != ORDINAL_CHAIN[i + 1]:
            chain_fail += 1
            fails.append(f"السلسلة فشلت عند {variant!r}: {o}")
checks += 1
ok(chain_fail == 0, f"السلسلة التوليدية: {chain_fail} حلقات انكسرت")

# ---- 9) تلاشي ثقة المفردات: رتابة مع الترتيب
for trial in range(300):
    n = random.randint(1, 8)
    words = {''.join(random.choice(ar_letters) for _ in range(random.randint(2, 6))): random.randint(1, 200) for _ in range(n)}
    top = personal_top(words, PERSONAL_TAKE)
    cands = fresh_start_candidates(top, True, 8)
    confs = [c for _, c in cands[:len(top)]]
    ok(all(confs[i] >= confs[i + 1] for i in range(len(confs) - 1)),
       f"رتابة الثقة فشلت عند {trial}: {cands}")

# ---- 10) التطبيع مرآة Kotlin: عينات توقيعية
norm_pairs = [
    ("أولًا", "اولا"), ("ثانيًا", "ثانيا"), ("ثامنًا", "ثامنا"),
    ("إن", "ان"), ("اللة", "الله".replace('ة', 'ه')), ("على", "علي"),
    ("مدرسة", "مدرسه"), ("Hello", "hello"), ("Élève", "élève"),
]
for src, want in norm_pairs:
    ok(normalize(src) == want, f"تطبيع فشل: {src!r} → {normalize(src)!r} (المتوقع {want!r})")

# ---------------------------------------------------------------------------
print(f"فحوص L3: {checks} — أخضر: {checks - len(fails)} — أحمر: {len(fails)}")
if fails:
    for m in fails[:20]:
        print("  ✗", m)
    sys.exit(1)
print("CHECK_L3_SENTENCE_PASS")
