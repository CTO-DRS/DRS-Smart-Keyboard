# DRS Smart Keyboard — لوحة DRS الذكية

**Smart by mathematics, not by machines.**

A fully offline, glass-design Android keyboard. Every "smart" behavior runs on
deterministic algorithms — a trie dictionary, weighted edit distance, bigram
statistics and geometric gesture matching. **No AI models, no network access,
no telemetry.** The app requests a single permission (vibration) and performs
zero network calls by design.

## Download

Get the latest signed APK from **[GitHub Releases](../../releases)** (v1.14.0)
or the mirror: [gofile.io/d/YFXtZwLm](https://gofile.io/d/YFXtZwLm).
All releases since v1.3.0 share the same signing key, so upgrades install
in-place without uninstalling.

| | |
|---|---|
| Package | `com.drs.keyboard` |
| Version | 1.14.0 (versionCode 14) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Languages | English (QWERTY) + العربية (Arabic 101, RTL-aware) |
| APK size | < 1 MB |
| Dependencies | none (pure Android framework + Kotlin stdlib) |
| Permissions | VIBRATE only |

---

## Features

### Smart engine (100% algorithmic)
- **Word prediction** — trie-compressed dictionary (12,400 EN / 24,000 AR
  entries) with frequency-ranked prefix completion.
- **Auto-correct** — weighted Damerau-Levenshtein distance walked directly
  over the trie; substitutions between *physically adjacent* keys cost less,
  so `helli` → `hello`, not `hills`.
- **Inline calculator** — type a math expression (`12*8+5`, `2^10`, `√9`,
  `(4+6)*2`, Arabic-Indic digits `١٢×٣`) and the suggestion strip offers
  "= 101"; tap it to replace the expression with the result. Hand-written
  recursive-descent parser — no eval, no network.
- **Next-word prediction** — bigram statistics (2,000+ EN / 400+ AR pairs)
  suggest what usually follows the previous word.
- **Swipe typing** — draw a curve across letters; a geometric decoder filters
  candidates by first/last-letter proximity and scores path fidelity against
  each word's key sequence. Live trail rendered while you draw.
- **Personal learning** — words you type get frequency boosts (stored only in
  the app's private storage, clearable in one tap).
- **Text shortcuts** — `omw` → *on my way*, `السلام` → *السلام عليكم ورحمة الله وبركاته*;
  fully user-editable.
- **Auto-capitalization, caps lock, double-space period, auto-correct undo**
  (backspace right after a fix restores what you typed).
- **Emoji suggestions** — typing *thanks* / *شكرا* offers a matching chip
  (static bilingual rules, ~140 pairs).

### Keyboard surface
- Glass-depth design: translucent rounded panels, top sheen, layered strokes.
- Custom vector key icons (no font-glyph dependency).
- Long-press popups: accents (é à ü ñ …), Arabic alef variants (أ إ آ ء), symbols.
- **Arabic tashkeel row** (optional): fatha, damma, kasra, shadda, sukun,
  the three tanweens, dagger alef and tatweel — a tap joins the word being
  composed instead of breaking it.
- **Navigation arrows row** (optional): ← ↑ ↓ → for precise cursor moves.
- Number row toggle, dedicated symbols and number-pad layouts.
- **Text selection mode** — the edit toolbar's ⊹ select toggle turns the
  arrows row into selection arrows (shift + arrows natively); copy or cut
  when done. The arrows row appears automatically while selecting.
- **Private mode** — one switch stops *all* recording: no word learning,
  no n-grams, no typing stats, no clipboard history, no emoji recents.
  A 🔒 hint confirms it every time the keyboard opens.
- Cursor control: slide on the space bar to move the caret.
- Swipe left on backspace deletes the whole word.
- Key-press preview bubble (Gboard-style, shift-aware, toggleable).
- **Key label size**: small / normal / large.
- Emoji panel (999 emoji, 9 categories, recents, **offline bilingual search**
  with Arabic normalization, **five skin tones** for people & gestures).
- Clipboard hub: history, pinning, one-tap paste, sensitive clips skipped,
  **text-editing toolbar** (select / select-all / copy / cut / paste — hold
  the clipboard icon), **quick date & time chips**, **auto-clear on field
  exit** (history + system clipboard; pinned items survive).
- One-handed mode (left/right) + draggable floating keyboard
  (long-press the space bar to cycle modes).
- Keyboard height (compact / normal / tall) and long-press delay settings.
- Haptic feedback with three strength levels, **key sound styles
  (soft / normal / clear with per-key-type system clicks)** — both toggleable.
- Arabic-Indic digits (٠١٢٣…) option.

### Smart extras
- **Typing statistics** — weekly bar chart + all-time counters, on-device only.
- **Per-app language memory** — reopens each app in the language you last used there.
- **Backup & restore** — one JSON file via SAF (prefs, learned words,
  shortcuts, emoji favorites, theme). Zero storage permissions.
- **Auto night theme** — by clock (19:00–06:00) or by system dark mode.

### Appearance — full in-app customizer
- 16 pre-made theme packages (Midnight, Frost, Emerald, Rose, Ocean, Ink,
  Sand, Violet, Royal Gold, Carbon Red, Lavender Mist, Cocoa Cream, …).
- Theme editor: color pickers (HSV + alpha + hex) for every surface, corner
  radius, key height, panel opacity, glass effect toggle, background image.
- **Theme export / import** as JSON files.
- **Theme from wallpaper** — reads the system wallpaper's palette
  (WallpaperColors, offline, no permission) and builds a matching glass
  theme: dark or light by luminance, accent from the dominant color.
- Live preview keyboard rendered with the exact views the IME uses.

### Settings app
- 3-step setup wizard (enable → switch → personalize) with a completion
  banner once everything is ready.
- **Glass Depth 2.0 UI**: staggered entrance motion, editorial section
  headers, edge-faded dividers, gradient step badges with vivid check
  marks, theme-aware status-bar icons (light mode fixed).
- Language pack manager (enable/disable EN & AR).
- Typing behavior switches, shortcuts manager, privacy page.

---

## Building

### Android Studio (recommended)
1. Open Android Studio → **File ▸ Open** → select this folder.
2. Let Gradle sync (downloads AGP 8.5.2 + Kotlin 2.0.21 automatically).
3. **Run** on a device, or **Build ▸ Build APK(s)**.

### Command line
```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

### Manual pipeline (no Gradle required)
The project has zero external dependencies, so a full APK can be produced
with only the SDK build-tools and kotlinc — see `scripts/build_apk.sh`
(uses `aapt2 → kotlinc → d8 → zipalign → apksigner`).

---

## Installing & enabling

1. Download the APK from [Releases](../../releases) and install it
   (`adb install DRS-Smart-Keyboard-v1.14.0.apk` or sideload).
2. Open **DRS** → tap **1. Open enable screen** → switch DRS on.
3. Back in DRS → **2. Choose keyboard** → pick DRS.
4. Type anywhere. Use the 🌐 key to switch English ⇄ العربية,
   long-press the space bar for one-handed / floating modes.

---

## Project layout

```
app/src/main/
├── AndroidManifest.xml
├── assets/dict/                  # frequency lists, bigrams, emoji catalog
├── java/com/drs/keyboard/
│   ├── engine/                   # Dictionary (trie+DL), NgramModel, SwipeDecoder,
│   │                             # SuggestionEngine, UserLearner, ShortcutEngine
│   ├── keyboard/                 # KeyDef, Layouts (EN/AR/symbols/num), KeyboardState
│   ├── view/                     # DrsKeyboardView, DrsKeyboardHost, CandidateBar,
│   │                             # EmojiPanel, IconPainter
│   ├── clipboard/                # ClipboardHistory, ClipboardPanel
│   ├── theme/                    # KeyboardTheme, ThemeRepo (presets + persistence)
│   ├── ime/                      # DrsImeService (the InputMethodService)
│   └── settings/                 # settings app + theme editor + color picker
└── res/                          # strings (en + ar), themes, method.xml, icons
```

## How the "smart" works (no AI anywhere)

| Feature | Algorithm |
|---|---|
| Completion | Trie subtree walk, Zipf-frequency ranking |
| Auto-correct | Trie-walked weighted Damerau-Levenshtein, adjacency-weighted substitutions, DP-row pruning |
| Next word | Bigram counts + user-learned boosts |
| Swipe decoding | Endpoint filtering + ordered nearest-point path fidelity scoring |
| Learning | Private frequency table (max 500 words) |

All state lives in the app's private storage. Nothing leaves the device —
there is no network permission to do it with.

## Changelog (highlights)

- **v1.14.0** — settings-app visual overhaul: completed wizard steps now
  show a vivid white ✓ on a gradient green disc (the check was previously
  invisible — green-on-green), staggered entrance motion, editorial
  section headers with fading hairlines, edge-faded dividers, larger
  stat-card numerals, richer hero aurora + glass sheen, upright Arabic
  slogan (no synthetic italic), "all steps done" banner, and theme-aware
  status-bar icons in every screen (light-mode icons were white-on-white).
- **v1.13.0** — launcher shortcuts (long-press the icon → themes / backup /
  stats / learned words), per-app typing stats with usage bars, word-jump
  via long-press on ← / → (extends selection in select mode), optional
  space-snap before punctuation ("word ." → "word.").
- **v1.12.0** — edit-toolbar undo (multi-level, restores even a cleared
  field), clear-all button, proper-noun/acronym autocorrect protection
  (DRS, iPhone… stay untouched), bulk word-list import (one word per line).
- **v1.11.0** — inline calculator (offline recursive-descent parser, EN +
  Arabic-Indic digits), quick date/time insert chips, key sound styles,
  clipboard auto-clear privacy.
- **v1.10.0** — text selection mode (arrows extend selection), global
  private mode (zero recording), emoji skin tones, theme from wallpaper.
- **v1.9.0** — navigation arrows row, Arabic tashkeel row, key label size.
- **v1.8.0** — offline emoji search (EN+AR), autocorrect undo, punctuation
  auto-space, theme export/import.
- **v1.7.0** — key-press preview bubble, emoji suggestions, manual word add,
  night-trigger choice (clock / system dark).
- **v1.6.0** — typing statistics, auto night theme, quick-paste chip.
- **v1.5.0** — per-app language memory, backup & restore, learned-words
  manager, vibration strength.
- **v1.4.0** — emoji favorites, text-editing toolbar, 4 new themes.
- **v1.3.0** — field-aware security (password/OTP/email handling).
- **v1.0.0–v1.2.0** — core IME, themes, shortcuts, swipe typing, clipboard hub.
