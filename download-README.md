# DRS Smart Keyboard (لوحة DRS الذكية)

## GitHub
- Repo: **https://github.com/CTO-DRS/DRS-Smart-Keyboard**
- Release v1.11.0: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/tag/v1.11.0

## Downloads / التنزيل
- **DRS-Smart-Keyboard-v1.11.0.apk** — latest release (961 KB, signed, versionCode 11)
  - GitHub: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/download/v1.11.0/DRS-Smart-Keyboard-v1.11.0.apk
  - gofile mirror: https://gofile.io/d/xIZTXfLM (md5 dba1ecd177ee2c65c9b3c5ac9f0b4784)
- **DRS-Keyboard-source.zip** — full Android Studio project source
  - GitHub release asset: https://github.com/CTO-DRS/DRS-Smart-Keyboard/releases/download/v1.11.0/DRS-Keyboard-source-v1.11.0.zip
  - gofile mirror: https://gofile.io/d/M2MtqNKA (md5 01947232941da78304b0b81e921ce1ac)

## What's new in v1.11.0
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
