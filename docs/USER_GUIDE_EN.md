# User Guide — DRS Smart Keyboard

- **Version:** 2.0.0 (unified)
- **Languages:** this is the English guide — Arabic version: [USER_GUIDE_AR.md](USER_GUIDE_AR.md)
- **Support:** [SUPPORT.md](../SUPPORT.md) — official channels and crash reports.

## 1. What is DRS Smart Keyboard?

A smart Android keyboard built on one explicit principle: **everything runs on
your device, and nothing leaves it**. Typing, correction, suggestions,
shortcuts, statistics, the economy system and backups — all local. No account,
no sign-in, no ads, and no network unless *you* request an update or a new
package. It is a full system service (IME) that replaces any other keyboard
without you losing anything.

## 2. Install & activate (two minutes)

1. Install the APK (verify `SHA256SUMS.txt` when downloading from GitHub).
2. Open the app → tap **Enable keyboard** → toggle "DRS Smart Keyboard" in the
   system settings (Android asks for confirmation — that is Android's own
   security step, not ours).
3. Return to the app → **Switch keyboard** → pick DRS.
4. Start typing in any field — try gliding between letters for gesture typing.

Upgrading over an older install works in place (same signing certificate since
the first unified release) — you never need to uninstall first.

## 3. A tour of the screens

All settings live in one unified app with clear sections:

| Section | What you'll find |
|---|---|
| **Basics** | Languages & subtypes, typing mode (gesture/tap), height, haptics & sound, themes |
| **Typing** | Correction & suggestions, literal typing, numbers & punctuation, space/backspace behavior |
| **Systems** | DRS systems: economy, profiles, daily stats, backup |
| **Tools** | Text tools, smart shortcuts, tech toolbar, internal clipboard |
| **Statistics** | Real daily numbers: keystrokes, words, speed — all local |
| **Diagnostics** | System status, event log, performance metrics, opt-in crash-log export |
| **Themes** | Built-in theme library + importing `.flex` packages from the package library |

## 4. The features unique to DRS

### Daily economy
Every active writing day earns points computed from your real statistics — a
motivation system with no ads and no purchases, and every number stays on the
device.

### Smart shortcuts
Type an abbreviation and it expands into full text automatically (your email
address, your signature, a recurring sentence). Create shortcuts from the
Tools screen and manage them in the shortcuts panel.

### Profiles
Multiple writing profiles: "Work" with formal tone and tech keys, "Personal"
with emoji and lighter colors — switch from the toolbar profile panel.

### Text tools & smart panels
Fast selection, case conversion, smart paste from the internal clipboard, and
a context-aware tools panel (number, URL, email fields).

**Clean web paste:** a copy from ChatGPT, WhatsApp or any website usually
arrives wearing Markdown (`**bold**`, `# heading`, `- bullet`,
`[link](url)`) — the «Clean web paste» tool in the text tools panel keeps the
meaning and drops the costume: bullets, headers, quotes and rules vanish,
links return to their text and images to their alt, while numbered lists and
underscores inside words (some_var_name) are never touched, and plain text
stays byte-identical — never a guess.

**Deterministic text vocalization:** in the text tools panel,
«Vocalize text» adds canonical harakat to every word the local 3000-word
lexicon knows — unknown words stay untouched, never guessed. Works on the
selection or the whole field, and nothing ever leaves your device.

**The deterministic smart clipboard:** copy a link, phone
number, verification code or amount, then open the smart clipboard panel from
the switcher chip — a seven-way content class with ready one-tap variants:
the tracking-parameter-free link, the unified Saudi phone forms, the extracted
code, the amount in seven currencies, and the Western/Arabic digit flip. In a
password field the clipboard is never read at all.

### The safe internal clipboard
Your last copied items with previews. Copied media is protected by gates: it
is never written to the system disk nor read before your approval, and its
orphaned files are cleaned automatically.

### Live transparency hub (transparency & learning era)
From Settings → Privacy & security → Live transparency hub: a live counter
of this app's own bytes, a daily-documented connection log with a hard
90-day rotation (when / documented purpose / size — it never stores what
you type), and an age certificate rendered from the log's own data and
shared as a PNG in one tap. Zero stays zero: if your device's counters are
unsupported the hub says so instead of claiming a zero it cannot prove.

### What we learned about you (transparency & learning era)
From Settings → Dictionary → What we learned about you: your personal
lexicon searchable and sortable, the learning tables with their real
counts, and the silent-correction counters — counting starts NOW, honestly
(no retroactive history, no estimates). Export your lexicon as an
ed25519-signed JSON; import accepts SIGNED files only (unsigned and
tampered are rejected with distinct messages); "erase everything we
learned" is a real, double-confirmed deletion.

### The popup gesture board (transparency & learning era)
Long-press any of the 28 Arabic letters (sun & moon) and the eight
canonical harakat stations pop up above it: the three tanwin forms, fatha,
damma, kasra, SHADDA (the main station), and sukun. Drag to a station and
release to insert it; release on the letter itself to insert it bare;
release outside the board to cancel. The threshold is your existing
long-press setting, and display/drag/cancel all run on the existing popup
engine — no new mechanics.

## 5. Privacy: what actually happens?

- **Network:** the app contacts no server by default. Update checks are manual
  (opt-in), and the package catalog uses HTTPS only when you request a package.
- **Password fields:** learning, voice and context features stop completely on
  password fields — enforced in code, not promised in prose.
- **Statistics & logs:** abstract numbers, never typed text. The crash log
  stores the exception class name and a stripped stack fingerprint — no
  message, no content, no field names.
- **Export:** nothing leaves your device unless you press the button (backup
  export, or a crash report you choose to share yourself).

## 6. Backup & restore

From **Systems → Backup**: create an archive with your preferences, adaptive
dictionary and shortcuts, protect it with a passphrase only you keep (it
cannot be recovered), and store the file wherever you like. Restore brings
everything back on a new device — and never touches the content of an active
field during the operation.

## 7. FAQ

**The keyboard doesn't appear after enabling?** Re-check step 2 (system
enable) then the switch. Try re-focusing the input field.

**No Arabic suggestions?** Download the language pack from the package
library, and make sure correction is on in the "Typing" section.

**Does it drain battery?** There is no background work at all — the app runs
only while the keyboard is open, and state is flushed once every 15 seconds.

**How do I move my setup to a new phone?** Backup on the old device → restore
on the new one. Or use the latest release and verify `SHA256SUMS`.

**Where do I report a problem?** [SUPPORT.md](../SUPPORT.md) — including a
crash report from the Diagnostics screen if available (you send it yourself).
