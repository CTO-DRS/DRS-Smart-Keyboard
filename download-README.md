# DRS Smart Keyboard (لوحة DRS الذكية)

## GitHub
- Repo: **https://github.com/CTO-DRS/DRS-Smart-Keyboard**
- Release v1.16.0: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/tag/v1.16.0

## Downloads / التنزيل
- **DRS-Smart-Keyboard-v1.16.0.apk** — latest release (996 KB, signed, versionCode 16)
  - GitHub: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/download/v1.16.0/DRS-Smart-Keyboard-v1.16.0.apk
  - gofile mirror: https://gofile.io/d/yaMawbQh (md5 859c925655281b0fc39e02750e0e5765)
- **DRS-Keyboard-source.zip** — full Android Studio project source
  - GitHub release asset: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/download/v1.16.0/DRS-Keyboard-source.zip
  - gofile mirror: https://gofile.io/d/S1GZ5yX3 (md5 f182cb5f1d70d4a977cdb3edf63e0ab4)

## What's new in v1.16.0 — smart field intelligence
- 🎚️ **Live password-strength meter** — type in any password field and the suggestion strip becomes a real-time 4-segment gauge with a bilingual label (Weak ضعيفة / Fair مقبولة / Good جيدة / Strong قوية) in a semantic color (red → amber → lime → green)
- 🧮 **Honest scoring, zero AI** — charset entropy (length × log₂ of the pool) with structural penalties: 48 most-common passwords = instant zero, repeated characters (aaab/111222), sequences (abc/123/987), keyboard runs (qwe/asd/zxc), hopeless length (< 6)
- 🔒 **Privacy by architecture** — the analysis runs in place from the field contents; nothing is stored, nothing learned on, and the app has no network permission at all; secure fields still never show suggestions or learning
- ⏱️ **Quick number pad** — long-press `؟123` to jump straight to the locked number pad (was two taps); long-press `#+=` inside the pad to return to letters — one gesture each way
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.15.0 — typing-surface visual overhaul
- 🔠 **Caps Lock vs Shift, at a glance** — one Shift tap = soft accent tint + accent ring + accent glyph; Caps Lock = solid accent fill + bright glyph. No more guessing the state
- 👇 **Physical press feedback** — pressed keys sink 1.1 dp into the glass panel while the glow ring lights up
- ◌ **Elegant harakat hints** — bare combining marks (ً ُ ِ ّ ْ …) now sit centered on a tiny dotted circle (OneUI style) instead of floating at the key's top edge; other hints got a size/contrast bump
- ↔️ **Space-bar drag affordance** — subtle chevrons at both space-bar edges hint that sliding moves the cursor
- 📋 **Clipboard icon redrawn** — protruding clip tab + paper lines (the old open box read like a battery on dark themes); round-capped backspace strokes
- 🏷 **Candidate-bar presence** — idle bar shows a quiet DRS brand mark + accent dot; utility chips gained hairline rings; the primary suggestion pill wears a fine accent outline
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.14.0 — world-class settings look
- ✅ **The check marks are finally visible** — completed wizard steps showed a green ✓ painted on a solid green disc (invisible!); they now use a vivid gradient green disc with a bold white check and a glassy inner rim; pending steps get an accent ring
- 🎬 **Staggered entrance motion** — each section fades in and rises gently on first open (320 ms, decelerating); replays are skipped so refreshes stay instant
- 📰 **Editorial section headers** — the accent bar + label is now followed by a hairline that fades away from the text (RTL-aware direction)
- 🌫️ **Edge-faded dividers** — all card dividers dissolve at both ends instead of hard flat rules
- 🔢 **Bigger stat numerals** — 20sp bold with letter spacing; stat cards no longer show a dead ripple (they are display-only now)
- 🌌 **Richer hero aurora** — third teal glow depth + brighter keyboard watermark + top glass sheen; the glyph tile is stroked glass instead of a milky square
- 🖋 **Upright Arabic slogan** — the hero tagline no longer uses synthetic italic in RTL (it slanted Arabic glyphs awkwardly); English keeps its italic
- 🎉 **"All steps done" banner** — when enable + choose + personalize are all complete the wizard shows the ready line: "اكتملت كل الخطوات — DRS تعمل الآن كلوحة إدخال."
- 🔆 **Theme-aware status bars everywhere** — every screen now sets light status/navigation-bar icons in light mode (they were white-on-white before); all 8 screens unified through UiKit.applySystemBars
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.13.0
- 🚀 **Launcher shortcuts** — long-press the app icon to jump straight into Themes, Backup & restore, Typing stats, or Learned words (static shortcuts, zero extra permissions)
- 📊 **Per-app typing stats** — a new "Most-typed apps" card in the stats screen: app labels, word counters and proportional usage bars; stored in a separate local-only file, capped at the top 20 apps, and private mode never records anything
- ⏭️ **Word-jump arrows** — long-press ← / → in the arrows row to jump a whole word (standard Ctrl+arrow semantics); in selection mode it extends the selection word-by-word
- 🧲 **Space-snap before punctuation** (optional, off by default) — typing a mark right after "word " removes the stray space: "word ." → "word." (Typing → Typing feel)
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.12.0
- ↩️ **Multi-level undo in the edit toolbar** — every text change the keyboard makes (keystrokes, corrections, suggestions, swipes, shortcut expansions, pastes, emoji, calculator results) is recorded locally and can be undone step by step; safe by design: entries only apply when the text before the cursor still matches, and edits made by the app itself are never touched. The toolbar stays open for repeated presses
- 🗑️ **Clear-all button** — wipes the entire field (before and after the cursor) in one tap, with a restore entry on the undo stack so ↩ brings everything back
- 🛡️ **Proper-noun / acronym protection** — words with internal capitals (DRS, iPhone, NASA, McDonald) are never autocorrected, and no "did you mean" fix is offered while typing them; normal sentence-start capitals keep working
- 📥 **Bulk word-list import** — in the learned-words manager: import a plain .txt list (one word per line, also accepts "word,count" or tab-separated) into your personal dictionary; duplicates merge, counts clamp 1–99, and you get an "N words imported" toast
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.11.0
- 🧮 **Inline calculator** — type a math expression (`12*8+5`, `2^10`, `√9`, `(4+6)*2`, Arabic digits `١٢×٣` with `٫` decimals) and the suggestion strip offers "= 101"; tap it to replace the expression with the result. Hand-written recursive-descent parser — no eval(), no network; deliberately ignores `12:30` times
- 📅 **Quick date & time insert** — two chips at the top of the clipboard panel insert today's date and the current time in your device's locale format
- 🔊 **Key sound styles** — soft / normal / clear tap volume, and space, delete, return keep their familiar distinct system clicks (Typing → Sound & feedback)
- 🧹 **Clipboard auto-clear** (privacy) — erases the clipboard history and wipes the system clipboard when you leave a field; pinned items survive (Typing → Sound & feedback)
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.10.0
- ✂️ **Text selection mode** — new "select" toggle in the edit toolbar (hold 📋): the arrows row turns into selection arrows (Shift + arrows natively) so you can extend a selection, then copy or cut it. The arrows row appears automatically while selecting
- 🔒 **Private mode** — one switch stops *all* recording: no word learning, no n-grams, no typing stats, no clipboard history, no emoji recents. A 🔒 hint confirms it every time the keyboard opens (Typing → Smart engine)
- 🖐 **Emoji skin tones** — five skin tones for people & gesture emoji, applied across category grids and search results (tone picker inside the emoji panel)
- 🖼 **Theme from wallpaper** — the theme editor reads the system wallpaper palette (WallpaperColors — offline, zero permissions, Android 8.1+) and builds a matching glass theme: dark or light by luminance, accent from the dominant color
- Same signing certificate as v1.3.0+ — installs directly over them

## What was new in v1.9.0
- ➡️ **Navigation arrows row** — optional ← ↑ ↓ → row above the letters for precise cursor moves (works in alpha + symbols modes, never flipped in RTL)
- 🈶 **Arabic tashkeel row** — one-tap fatha / damma / kasra / shadda / sukun / the three tanweens / dagger-alef / tatweel; combining marks **join the word being composed** so autocorrect and suggestions keep flowing (shows in Arabic alpha mode)
- 🔠 **Key label size** — small / normal / large text on every key (visual accessibility)

## What was new in v1.8.0
- 🔍 **Emoji search** — a search field inside the emoji panel with English *and* Arabic keywords (ابتسامة، قلب، سيارة، love, cake…). Offline keyword index built into the app; Arabic matching ignores diacritics and unifies أ/إ/آ, ة/ه, ى/ي
- ↩️ **Undo autocorrect** — keyboard corrects a word and you disagree? Press backspace once and the original word you typed comes right back
- ⌨️ **Auto-space after punctuation** — optional: a space is typed automatically after . ! ? … ، ؛ ؟ (skipped after digits like 3.14 and repeated marks like ...)
- 🎨 **Theme export / import** — share any theme as a JSON file via the system picker, and load themes created on other devices (keeps background image choices on import)

## What was new in v1.7.0
- ⌨️ **Key-press preview bubble** — an enlarged glass bubble floats above the key you touch showing the exact character (shift-aware). Toggle in Typing → Layout & feel
- 😀 **Emoji suggestions** — typing a known word (thanks, شكرا, مبروك, love, هههه…) offers a matching emoji chip right in the suggestion strip. ~140 keyword rules, 100% offline and rule-based (no AI)
- ➕ **Add words manually** — in the learned-words manager you can now add your own names/dialect/slang; they get strong suggestion weight and are never autocorrected
- 🌙 **Night trigger choice** — auto night theme can now follow the clock (19:00–06:00) *or* the system dark-mode setting

## What was new in v1.6.0
- 📊 Typing stats — weekly bar chart + all-time totals, on-device only
- 🌙 Auto night theme by clock (19:00–06:00)
- ⎘ Quick-paste chip for freshly copied text
- 🐛 Fixed: transient hints in the suggestion strip were wiped instantly

## What was new in v1.5.0
- 🧠 Per-app language memory (each app reopens in its last-used language)
- 💾 Backup & restore as one JSON file via the system picker (no permissions)
- 📚 Learned-words manager (search, delete one, wipe all)
- 📳 Vibration strength (light/normal/strong) + real version number in settings

## What was new in v1.4.0
- ⭐ Emoji favorites (long-press to star) + fixed emoji catalog loading bug
- 📋 Text editing toolbar (hold 📋): select-all / copy / cut / paste
- 🎨 4 new themes (16 total)

## Install note
If you have v1.1.0/v1.2.0 or older, uninstall first (different signing certificates). From v1.3.0 onward upgrades are in-place.

## Requirements
Android 8.0+ (API 26). Zero permissions beyond vibration. Fully offline — no AI, no telemetry, no analytics.
