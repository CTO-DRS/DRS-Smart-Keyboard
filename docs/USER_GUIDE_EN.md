# User Guide — DRS Smart Keyboard

- **Guide version:** 2.0.0 (unified) — covers app release **1.3.0 «On-Device Intelligence»** (versionCode 4, catalog of 84 tools) with the three completed intelligence arms (local security, quantized corrector, deeper next-word prediction)
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

From release 1.2.0 onward, in-place upgrades are guaranteed (same signing
certificate) — you never need to uninstall first, and moving from 1.2.0 to
1.3.0 is a direct in-place install. The sole historical exception: moving
from 1.1.0 or earlier (the signing key rotated in 1.2.0) — export your
dictionary and signed backups, uninstall, then install the new release as a
fresh install and import what you exported — this will not repeat in
upcoming releases.

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

**Clean HTML paste:** a copy from email or a CMS sometimes arrives as
raw HTML (`<p>`, `<br>`, `&amp;`…) — the «Clean HTML paste» tool in the
text tools panel keeps the prose and drops the markup: comments and
script/style blocks vanish, br becomes a newline and block closers
become lines while list items keep their bullet, every well-formed tag
strips to its text and the closed entity whitelist decodes in one pass
— a lone `<` (a < b) is not the shape and stays byte-identical, and
plain text without any shape returns as-is.

**Text inspector:** read-only, never edits — one message raises your
text's deterministic profile: letters by script (Arabic/Latin), digits,
words, sentences, lines, hidden marks and emoji, so you know what the
text carries before picking the cleanup tool worth firing.

**Extract links:** a WhatsApp message or email full of scattered URLs?
The «Extract links» tool in the text tools panel lands every link and
email on its own line, in order — http/https/ftp URLs, www. addresses
not buried inside a larger word, and strict ASCII email — stripping
trailing sentence punctuation and a lone closing bracket, while a
balanced wiki-style bracket stays inside its link; a text carrying no
links returns byte-identical.

**Remove letter stretch:** fast typing arrives as «هلااااا» and
«جمييييل»? The «Remove letter stretch» tool calms it: any run of three
or more identical Arabic letters collapses to one — «هلا», «جميل» —
while legitimate doubles stay (الله، عدّ) and Latin letters and digits
never touch (www survives); nothing stretched returns byte-identical.

**Base64 encode/decode:** need a password, API key or any text to
survive a field that only accepts letters and digits? «Base64 encode»
turns the text (as UTF-8) into standard one-line Base64 — letters,
digits, + and / with padding (=) — so it passes through any field and
app. When encoded text arrives (a JWT header like `eyJ…`, or a block
wrapped over lines) «Base64 decode» restores the readable text: wrapped
lines rejoin, the URL-safe alphabet (- and _) is accepted, and anything
that is not valid Base64 — an honest word, binary bytes — returns
byte-identical, so your typing is never corrupted. The smart tiles
suggest «Base64 decode» only when the decode is guaranteed to fire.

**Morse encode/decode:** sending text over a bare channel, solving a
puzzle, or reading an old telegram? «Morse encode» speaks the OFFICIAL
ITU table (ITU-R M.1677-1): every Arabic letter takes its historic
telegraph sign — ا = ·−، ب = −··· — and Latin letters, digits and
punctuation take the international ones; letters join with a space and
words with / : «مرحبا» becomes «−− ·−· ···· −··· ·−». Diacritics and
tatweel strip, hamza carriers fold (سؤال encodes as سوال), and anything
without a known sign (emoji, a slash) returns untouched — never an
invented sign. When Morse arrives in any dot/dash glyph family (‎.−‎ or
‎·−‎ or long dashes) «Morse decode» restores it — and since the channel
is scriptless, every letter speaks Arabic: ‎··· −−− ···‎ returns «سخس».
An unknown sign or empty word returns the text untouched — the decoder
never guesses and never destroys. The smart tiles suggest «Morse decode»
only when the decode is guaranteed to fire.

**Smart calculator & unit converter:** type `1250×12` in any field and
ask for the smart calculator — it answers 15000. A hand-written parser
with correct precedence, parentheses, powers (`2^10` = 1024) and a
percent that binds to its own number (15% = 0.15); Eastern digits
(٥٠ × ٤ = 200), the Arabic decimal separator and the thousands comma
(1,234+66 = 1300) all fold onto the grammar first, and the result is
clean — no float tail (0.1+0.2 = 0.3). Prose, an unbalanced paren and
a division by zero all return untouched — the calculator never
guesses. Type `5 كم` and ask for the unit converter — it speaks the
category ladder: 5000 م, 500000 سم, 3.10685596119 ميل, 16404.1994751
قدم — seven closed categories (length, mass, volume, area, speed,
data, time) over Arabic and English names, temperatures by their real
formulas (38 C = 100.4 °ف and 311.15 ك), and «إلى م»/«to m» names one
target that answers in its canonical Arabic token («3 kg to g»
returns 3000 جم). Anything that is not one whole quantity («عمره 25
سنة») returns untouched. When the whole field IS an expression or a
quantity, the smart tiles suggest the right tool — a guaranteed-result
recommendation.

**Hex encode & decode:** want to see your text the way the machine
sees it, or pass exact bytes through a strict channel? Hex encode
speaks the text's UTF-8 bytes as the classic readable dump: lowercase
two-digit pairs with one space between bytes — «م» becomes «d9 85»,
and emoji and control characters all travel. When a hex dump arrives
in any known shape — spaced pairs (`48 65 6c`), a solid run
(`48656c`) or 0x-prefixed tokens, in any letter case — Hex decode
speaks it back as text, and the bytes must be valid UTF-8: an odd
length, a foreign character, a colon separator, Eastern digits or
binary bytes all return untouched byte-for-byte — the decoder never
guesses and never destroys your typing. When the cursor stands before
a clean dump, the smart tiles suggest Hex decode — only when the
recommendation is guaranteed to fire; a bare number («1250») stays a
number and is never suggested.

**Password strength meter & generator (Security & privacy):** pasted a
password into a normal field and want the truth about it? The «Password
strength meter» reads only, never transforms: it measures entropy
mathematically (length × the variety of the classes present), answers
on a five-band verdict, and flags the closed weak patterns — repeats,
keyboard walks, year-shaped fragments, and common guessing-list words
(any list hit reads «weak» no matter the entropy). A bare number stays
a number and is never suggested; PINs are measured on an explicit tap.
And the «Password generator» writes a strong password at the cursor
(16 characters, ≈97 bits, no look-alike l1IO0o, one guaranteed member
of every class) — deliberately never auto-suggested: generation is a
conscious tap, not a guess. Both are purely local: nothing leaves your
device, and password fields themselves refuse the whole surface.

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
