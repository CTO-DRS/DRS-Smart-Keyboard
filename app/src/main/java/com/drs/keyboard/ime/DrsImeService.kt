package com.drs.keyboard.ime

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextUtils
import android.view.KeyEvent
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.drs.keyboard.R
import com.drs.keyboard.clipboard.ClipboardHistory
import com.drs.keyboard.engine.Dictionary
import com.drs.keyboard.engine.EmojiSuggest
import com.drs.keyboard.engine.MathEval
import com.drs.keyboard.engine.NgramModel
import com.drs.keyboard.engine.ShortcutEngine
import com.drs.keyboard.engine.StatsStore
import com.drs.keyboard.engine.StrengthMeter
import com.drs.keyboard.engine.SuggestionEngine
import com.drs.keyboard.engine.SwipeDecoder
import com.drs.keyboard.engine.UserLearner
import com.drs.keyboard.keyboard.KeyDef
import com.drs.keyboard.keyboard.KeyboardState
import com.drs.keyboard.settings.Prefs
import com.drs.keyboard.settings.SettingsActivity
import com.drs.keyboard.theme.KeyboardTheme
import com.drs.keyboard.theme.ThemeRepo
import com.drs.keyboard.view.DrsKeyboardHost
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * The InputMethodService that ties the whole machine together:
 * typing pipeline, auto-correct, next-word prediction, swipe decoding,
 * shortcuts, double-space period, auto-cap, cursor control, haptics.
 */
class DrsImeService : android.inputmethodservice.InputMethodService(),
    DrsKeyboardHost.HostCallback {

    private val dictionaries = HashMap<String, Dictionary>(2)
    private val ngrams = HashMap<String, NgramModel>(2)
    private lateinit var learner: UserLearner
    private lateinit var shortcuts: ShortcutEngine
    private lateinit var suggestionEngine: SuggestionEngine
    private lateinit var clipboardHistory: ClipboardHistory
    private lateinit var prefs: Prefs
    private val swipeDecoder = SwipeDecoder()
    private val ui = Handler(Looper.getMainLooper())

    private var host: DrsKeyboardHost? = null
    private val state = KeyboardState()
    private val composing = StringBuilder()
    private var lastWord = ""
    private var lastCommitWasSpace = false
    private var lastSpaceTime = 0L
    private var boostCounter = 0
    private var spaceLongPressCount = 0

    // field-aware behaviour flags (reset every onStartInputView)
    private var secureField = false      // password / otp: no suggestions, no composing, no learning
    private var noLearnField = false     // IME_FLAG_NO_PERSONALIZED_LEARNING: never record anything
    private var noAutocorrectField = false // email / uri / subject fields: corrections would mangle input
    private var pendingPaste: String? = null // fresh system clipboard offered as a quick-paste chip

    /** True when nothing may be recorded: no-learn fields or the global private mode. */
    private fun privacyHold() = noLearnField || prefs.incognito

    // autocorrect undo: backspace right after a corrected word restores what was typed
    private var pendingUndo: String? = null
    private var pendingUndoWord: String = ""

    /**
     * Local multi-level undo: every text change WE make through the
     * InputConnection is recorded as a contiguous inserted run (or a cleared
     * block). Undo pops entries newest-first and reverts them only when the
     * text before the cursor still matches — so edits made by the app itself
     * never get mangled. Session-scoped: cleared on every field change.
     */
    private class UndoEntry(var text: String, val deletion: Boolean = false) {
        var time: Long = System.currentTimeMillis()
    }
    private val undoStack = ArrayDeque<UndoEntry>()
    private var undoCharMerged = false   // last entry grew from single keystrokes

    /** Record text we are about to commit. charMerge=true coalesces rapid
     *  single keystrokes into one run (capped) so undo works word-by-word. */
    private fun trackInsert(text: String, charMerge: Boolean = false) {
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        val top = undoStack.lastOrNull()
        if (charMerge && undoCharMerged && top != null && !top.deletion &&
            now - top.time < 1500 && top.text.length < 12
        ) {
            top.text += text
            top.time = now
        } else {
            undoStack.addLast(UndoEntry(text))
            if (undoStack.size > 50) undoStack.removeFirst()
            undoCharMerged = charMerge
        }
    }

    // text-selection mode (edit toolbar "select" toggle): arrows extend the selection
    private var selectMode = false

    // inline calculator: expression currently before the cursor + its result chip
    private var mathExpr: String? = null

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        learner = UserLearner.get(this)
        shortcuts = ShortcutEngine.get(this).also { it.ensureDefaults() }
        clipboardHistory = ClipboardHistory(this).apply {
            enabled = prefs.clipboardEnabled && !prefs.incognito
        }
        suggestionEngine = SuggestionEngine(dictionaries, ngrams, learner)
        Thread { loadEngines() }.start()
    }

    private fun loadEngines() {
        for (lang in listOf("en", "ar")) {
            val dict = Dictionary()
            dict.loadFromAssets(this, lang)
            val ngram = NgramModel()
            ngram.load(this, lang)
            synchronized(dictionaries) { dictionaries[lang] = dict }
            synchronized(ngrams) { ngrams[lang] = ngram }
        }
        ui.post {
            host?.let { h ->
                updateSwipeWordList()
                h.refreshLayout()
                updateSuggestions()
            }
        }
    }

    private fun Dictionary.loadFromAssets(context: Context, lang: String) {
        val name = if (lang == "ar") "dict/ar_freq.txt" else "dict/en_freq.txt"
        runCatching {
            context.assets.open(name).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).forEachLine { line ->
                    val idx = line.indexOf('\t')
                    if (idx > 0) {
                        val w = line.substring(0, idx)
                        val f = line.substring(idx + 1).toIntOrNull() ?: return@forEachLine
                        add(w, f)
                    }
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        val h = DrsKeyboardHost(this)
        h.hostCallback = this
        h.state = state
        h.attachClipboard(clipboardHistory)
        h.attachEmoji()
        h.applyTheme(currentTheme())
        host = h
        applySettingsToViews()
        restoreUiMode()
        h.refreshLayout()
        return h
    }

    /** Active theme honoring the auto-night schedule (clock or system dark). */
    private fun currentTheme(): KeyboardTheme {
        return if (prefs.autoNight && nightNow())
            ThemeRepo.loadNight(this) ?: ThemeRepo.loadCurrent(this)
        else ThemeRepo.loadCurrent(this)
    }

    /** Night trigger: by clock (19:00–06:00) or by the system dark-mode setting. */
    private fun nightNow(): Boolean = when (prefs.nightSource) {
        1 -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        else -> ThemeRepo.isNightHour()
    }

    private fun restoreUiMode() {
        val h = host ?: return
        when (prefs.uiMode) {
            1 -> h.setOneHand(false)
            2 -> h.setOneHand(true)
            3 -> h.applyFloating(true)
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // settings may have changed since last time
        clipboardHistory.enabled = prefs.clipboardEnabled
        state.enabledLangs = prefs.enabledLangs()
        if (state.lang !in state.enabledLangs) state.lang = state.enabledLangs.first()
        // per-app language memory: reopen an app in the language you last used there
        if (prefs.perAppLang) {
            val saved = savedLangFor(info.packageName)
            if (saved != null && saved in state.enabledLangs && saved != state.lang) {
                state.lang = saved
                updateSwipeWordList()
            }
        }
        state.numberRow = prefs.numberRow
        state.buildAdjacency()
        suggestionEngine.adjacency = state.adjacency
        state.resetForNewField(sentenceStart = true, autoCap = prefs.autoCap)
        updateSwipeWordList()
        host?.keyboardView?.cursorControlEnabled = prefs.cursorControl
        host?.keyboardView?.swipeEnabled = prefs.swipe
        host?.keyboardView?.longPressDelayMs = prefs.longPressMs.toLong()
        host?.keyboardView?.applyKeyHeight(keyHeightScale())
        host?.keyboardView?.keyPopupEnabled = prefs.keyPopup
        host?.keyboardView?.labelScale = labelScale()
        host?.arabicDigits = prefs.arabicDigits
        host?.arrowRow = prefs.arrowRow
        host?.tashkeelRow = prefs.tashkeelRow
        host?.refreshLayout()
        // incognito can be toggled in settings while the IME process lives
        clipboardHistory.enabled = prefs.clipboardEnabled && !prefs.incognito
        selectMode = false
        host?.setSelectActive(false)
        composing.setLength(0)
        lastWord = extractLastWord()
        lastCommitWasSpace = false
        pendingUndo = null
        pendingUndoWord = ""
        undoStack.clear()
        undoCharMerged = false
        // ---- field awareness -------------------------------------------------
        val cls = info.inputType and EditorInfo.TYPE_MASK_CLASS
        val vari = info.inputType and EditorInfo.TYPE_MASK_VARIATION
        secureField =
            vari == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
            vari == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            vari == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            (cls == EditorInfo.TYPE_CLASS_NUMBER &&
                vari == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
        noLearnField = secureField ||
            (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        noAutocorrectField =
            vari == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            vari == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            vari == EditorInfo.TYPE_TEXT_VARIATION_URI ||
            vari == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT
        if (secureField) {
            host?.candidateBar?.showHint(getString(R.string.secure_hint))
            ui.removeCallbacks(clearHintRunnable)
            ui.postDelayed(clearHintRunnable, 1200)
        } else if (prefs.incognito) {
            // remind the user that nothing is being recorded right now
            host?.candidateBar?.showHint(getString(R.string.incognito_hint))
            ui.removeCallbacks(clearHintRunnable)
            ui.postDelayed(clearHintRunnable, 1400)
        } else if (!editHintShown && prefs.clipboardEnabled) {
            // teach the hold-clipboard gesture once per IME process
            editHintShown = true
            host?.candidateBar?.showHint(getString(R.string.hint_editbar))
            ui.removeCallbacks(clearHintRunnable)
            ui.postDelayed(clearHintRunnable, 1600)
        }
        // honor field auto-cap
        if (prefs.autoCap && info.initialCapsMode != 0) {
            state.shifted = true
            host?.keyboardView?.setShiftVisuals(state.shifted, state.capsLocked)
        }
        updateSuggestions()
        // offer a one-tap paste of the system clipboard (hidden in secure fields)
        pendingPaste = if (secureField) null else systemClipText()
        if (pendingPaste != null) {
            host?.candidateBar?.showPasteHint(getString(R.string.paste_chip))
            ui.removeCallbacks(clearHintRunnable)
        }
    }

    private fun systemClipText(): String? = runCatching {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return null
        val item = cm.primaryClip?.getItemAt(0) ?: return null
        item.coerceToText(this)?.toString()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun keyHeightScale(): Float = when (prefs.keyboardHeight) {
        0 -> 0.85f
        2 -> 1.15f
        else -> 1f
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        learner.persist()
        selectMode = false
        host?.setSelectActive(false)
        host?.let { if (it.currentPanel() != DrsKeyboardHost.Panel.NONE) it.togglePanel(DrsKeyboardHost.Panel.NONE) }
        host?.keyboardView?.resetTouchVisuals()
        // privacy auto-clear: wipe clipboard traces when the user moves on
        if (prefs.clipAutoClear) {
            clipboardHistory.clearUnpinned()
            clipboardHistory.clearSystem()
        }
    }

    private fun extractLastWord(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(48, 0) ?: return ""
        val sb = StringBuilder()
        for (i in before.length - 1 downTo 0) {
            val c = before[i]
            if (c.isLetter() || c == '\'') sb.insert(0, c) else break
        }
        return sb.toString().lowercase()
    }

    // ------------------------------------------------------------------
    // Settings application
    // ------------------------------------------------------------------

    private fun applySettingsToViews() {
        val h = host ?: return
        h.keyboardView.swipeEnabled = prefs.swipe
        h.keyboardView.cursorControlEnabled = prefs.cursorControl
        h.keyboardView.keyPopupEnabled = prefs.keyPopup
        h.keyboardView.labelScale = labelScale()
        h.keyboardView.oneHanded = prefs.oneHanded
    }

    /** Key label text multiplier from the size setting (small/normal/large). */
    private fun labelScale(): Float = when (prefs.keyTextScale) {
        0 -> 0.85f
        2 -> 1.2f
        else -> 1f
    }

    private fun updateSwipeWordList() {
        val dict = dictionaries[state.lang] ?: return
        swipeDecoder.setWords(dict.words())
        swipeDecoder.frequencyOf = { w ->
            dict.freqOf(w) + learner.freqOf(w) * 30
        }
        host?.keyboardView?.swipeDecoder = swipeDecoder
        host?.keyboardView?.feedSwipeDecoder()
    }

    // ------------------------------------------------------------------
    // Key handling
    // ------------------------------------------------------------------

    override fun onKey(key: KeyDef) {
        if (prefs.vibrate) haptic()
        if (prefs.sound) {
            val am = audioManager()
            am?.playSoundEffect(soundEffectFor(key), soundVolume())
        }
        if (!prefs.incognito) StatsStore.bump(this, StatsStore.KEYS)

        when {
            key.type != KeyDef.KeyType.CHAR -> handleFunctional(key)
            else -> handleChar(key)
        }
    }

    /** Contextual key sound: space/delete/return get their own familiar click.
     *  Note: FX_KEY_SPACEBAR/DELETE/RETURN are hidden SDK constants (6/7/8);
     *  their integer ids are stable in AOSP and handled by AudioService. */
    private fun soundEffectFor(key: KeyDef): Int = when (key.type) {
        KeyDef.KeyType.SPACE -> 6     // FX_KEY_SPACEBAR
        KeyDef.KeyType.BACKSPACE -> 7 // FX_KEY_DELETE
        KeyDef.KeyType.ENTER -> 8     // FX_KEY_RETURN
        else -> AudioManager.FX_KEY_CLICK
    }

    /** Playback volume for key sounds from the style setting (soft/normal/clear). */
    private fun soundVolume(): Float = when (prefs.soundStyle) {
        0 -> 0.25f
        2 -> 1f
        else -> 0.55f
    }

    private fun handleChar(key: KeyDef) {
        val ic = currentInputConnection ?: return
        val shifted = state.shifted
        val code = if (shifted) key.shiftCode else key.code
        val label = if (shifted && key.shiftLabel != null) key.shiftLabel!! else key.label
        val text = String(Character.toChars(code))
        // a fresh keystroke cancels any pending autocorrect-undo window
        pendingUndo = null
        pendingUndoWord = ""

        val isLetter = text.length == 1 && text[0].isLetter()
        // Arabic diacritics (tashkeel) and tatweel: combining marks that JOIN
        // the word being composed instead of ending it — the word keeps its
        // autocorrect/suggestion flow and the shift state is preserved.
        val isCombining = text.length == 1 &&
            (text[0].code in 0x064B..0x0655 || text[0].code == 0x0670 ||
                text[0].code == 0x0640)
        if (isCombining) {
            pendingUndo = null
            pendingUndoWord = ""
            if (composing.isNotEmpty()) {
                composing.append(text)
                ic.setComposingText(composing.toString(), 1)
            } else {
                ic.commitText(text, 1)
            }
            lastCommitWasSpace = false
            updateSuggestions()
            return
        }
        if (!isLetter || secureField) {
            // punctuation / digits / symbols: end any current word.
            // secure fields: commit raw characters — no composing, no corrections.
            if (composing.isNotEmpty()) {
                val word = composing.toString()
                learnerBoost(word)
                lastWord = word.lowercase()
                composing.setLength(0)
                ic.finishComposingText()
                if (prefs.shortcuts) tryShortcut(word)
                trackInsert(word)
            }
            // read the char before the cursor BEFORE committing, so auto-space
            // can be skipped after a digit (3.14) or after the same mark (...)
            var prev = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            // space-snap: a mark typed right after "word " hugs the word
            if (prefs.spaceSnap && !secureField && !noAutocorrectField &&
                text.length == 1 && text[0] in ".!?…،؛؟,:;" &&
                prev.length == 2 && prev[1] == ' ' &&
                prev[0] != ' ' && prev[0].isLetterOrDigit()
            ) {
                ic.deleteSurroundingText(1, 0)
                prev = prev.dropLast(1)
            }
            prev = prev.takeLast(1)
            ic.commitText(text, 1)
            trackInsert(text, charMerge = true)
            val autoSp = prefs.autoSpacePunct && !secureField && !noAutocorrectField &&
                text.length == 1 && text[0] in ".!?…،؛؟" &&
                !prev.any { it.isDigit() } && prev != text
            if (autoSp) {
                ic.commitText(" ", 1)
                trackInsert(" ", charMerge = true)
            }
            lastCommitWasSpace = false
            state.consumeShift()
            syncShiftVisuals()
            checkSentenceShift(text)
            updateSuggestions()
            return
        }

        if (composing.isEmpty()) {
            ic.beginBatchEdit()
            composing.append(text)
            ic.setComposingText(composing.toString(), 1)
            ic.endBatchEdit()
        } else {
            composing.append(text)
            ic.setComposingText(composing.toString(), 1)
        }
        lastCommitWasSpace = false
        state.consumeShift()
        syncShiftVisuals()
        updateSuggestions()
    }

    // ------------------------------------------------------------------
    // Per-app language memory
    // ------------------------------------------------------------------

    private fun savedLangFor(pkg: String?): String? {
        if (pkg.isNullOrEmpty()) return null
        for (entry in prefs.appLangMap.split(';')) {
            val i = entry.indexOf('=')
            if (i > 0 && entry.substring(0, i) == pkg) return entry.substring(i + 1)
        }
        return null
    }

    private fun rememberLangForApp(lang: String) {
        if (!prefs.perAppLang) return
        val pkg = currentInputEditorInfo?.packageName ?: return
        if (pkg.isEmpty()) return
        val sb = StringBuilder()
        for (entry in prefs.appLangMap.split(';')) {
            val i = entry.indexOf('=')
            if (i <= 0) continue
            if (entry.substring(0, i) == pkg) continue
            if (sb.isNotEmpty()) sb.append(';')
            sb.append(entry)
        }
        if (sb.isNotEmpty()) sb.append(';')
        sb.append(pkg).append('=').append(lang)
        prefs.appLangMap = sb.toString()
    }

    private fun handleFunctional(key: KeyDef) {
        when (key.type) {
            KeyDef.KeyType.SPACE -> handleSpace()
            KeyDef.KeyType.BACKSPACE -> handleBackspace()
            KeyDef.KeyType.ENTER -> handleEnter()
            KeyDef.KeyType.NAV -> when (key.code) {
                KeyDef.CODE_NAV_LEFT -> onCursorMove(-1)
                KeyDef.CODE_NAV_RIGHT -> onCursorMove(1)
                KeyDef.CODE_NAV_UP -> sendDpad(KeyEvent.KEYCODE_DPAD_UP)
                KeyDef.CODE_NAV_DOWN -> sendDpad(KeyEvent.KEYCODE_DPAD_DOWN)
                KeyDef.CODE_NAV_WORD_LEFT -> jumpWord(-1)
                KeyDef.CODE_NAV_WORD_RIGHT -> jumpWord(1)
            }
            KeyDef.KeyType.SHIFT -> {
                state.tapShift()
                syncShiftVisuals()
            }
            KeyDef.KeyType.MODE_SYMBOLS -> {
                state.switchModeToSymbols()
                host?.refreshLayout()
            }
            KeyDef.KeyType.MODE_NUM -> {
                state.mode = KeyboardState.MODE_NUM
                host?.refreshLayout()
            }
            KeyDef.KeyType.MODE_ALPHA -> {
                state.mode = KeyboardState.MODE_ALPHA
                host?.refreshLayout()
            }
            KeyDef.KeyType.LANG -> {
                composingFinishQuietly()
                val newLang = state.cycleLanguage()
                prefs.lastLang = newLang
                rememberLangForApp(newLang)
                updateSwipeWordList()
                host?.refreshLayout()
                host?.candidateBar?.showHint(if (newLang == "ar") "العربية" else "English")
                ui.removeCallbacks(clearHintRunnable)
                ui.postDelayed(clearHintRunnable, 900)
                updateSuggestions()
            }
            KeyDef.KeyType.EMOJI -> {
                host?.togglePanel(DrsKeyboardHost.Panel.EMOJI)
            }
            else -> {}
        }
    }

    private val clearHintRunnable = Runnable {
        host?.candidateBar?.showHint("")
        updateSuggestions()
    }

    private fun syncShiftVisuals() {
        host?.keyboardView?.setShiftVisuals(state.shifted, state.capsLocked)
    }

    // ------------------------------------------------------------------
    // Text pipeline
    // ------------------------------------------------------------------

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()

        // double-space → ". "
        if (prefs.doubleSpace && lastCommitWasSpace && now - lastSpaceTime < 400 && composing.isEmpty()) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            trackInsert(". ")
            lastCommitWasSpace = false
            lastWord = ""
            state.setSentenceShift(prefs.autoCap)
            syncShiftVisuals()
            updateSuggestions()
            return
        }

        if (composing.isNotEmpty()) {
            val typed = composing.toString()
            // protect names & acronyms from mangling: any uppercase letter after
            // the first character (DRS, iPhone, NASA…) disables autocorrection
            val protectCap = typed.indexOfFirst { it.isUpperCase() } > 0
            val canCorrect = prefs.autocorrect && !noAutocorrectField && !protectCap
            val suggestions = suggestionEngine.suggest(
                state.lang, typed, lastWord, canCorrect
            )
            var finalWord = typed
            if (canCorrect) {
                val fix = suggestionEngine.pickAutocorrect(state.lang, typed, suggestions)
                if (fix != null) {
                    finalWord = fix
                    composing.setLength(0)
                    ic.setComposingText(finalWord, 1)
                }
            }
            // remember the correction so an immediate backspace can restore the typed word
            if (canCorrect && finalWord != typed) {
                pendingUndo = typed
                pendingUndoWord = finalWord
            } else {
                pendingUndo = null
                pendingUndoWord = ""
            }
            learnerBoost(finalWord)
            if (lastWord.isNotEmpty() && !privacyHold()) {
                (ngrams[state.lang])?.learn(lastWord, finalWord)
            }
            lastWord = finalWord.lowercase()
            composing.setLength(0)
            ic.finishComposingText()
            trackInsert(finalWord)
            if (!prefs.incognito) {
                StatsStore.bump(this, StatsStore.WORDS)
                // attribute the word to the foreground app (local-only file)
                StatsStore.bumpApp(this, currentInputEditorInfo?.packageName)
            }
            if (prefs.shortcuts && tryShortcut(finalWord)) {
                lastWord = ""
                pendingUndo = null
                pendingUndoWord = ""
            }
        } else if (lastWord.isNotEmpty()) {
            // typing spaces between dictations: keep bigram chain alive
            pendingUndo = null
            pendingUndoWord = ""
        }
        mathExpr = null // a space always ends any math expression

        ic.commitText(" ", 1)
        trackInsert(" ", charMerge = true)
        lastCommitWasSpace = true
        lastSpaceTime = now
        updateSuggestions()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        // undo window: the last committed word was an autocorrect fix — restore the typed word
        val orig = pendingUndo
        if (orig != null && composing.isEmpty() && pendingUndoWord.isNotEmpty()) {
            val expect = pendingUndoWord + " "
            val before = ic.getTextBeforeCursor(expect.length, 0) ?: ""
            if (before == expect) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(expect.length, 0)
                composing.append(orig)
                ic.setComposingText(orig, 1)
                ic.endBatchEdit()
                pendingUndo = null
                pendingUndoWord = ""
                lastCommitWasSpace = false
                lastWord = extractLastWord()
                host?.candidateBar?.showHint(getString(R.string.undo_done, orig))
                ui.removeCallbacks(clearHintRunnable)
                ui.postDelayed(clearHintRunnable, 1200)
                updateSuggestions()
                return
            }
            pendingUndo = null
            pendingUndoWord = ""
        }
        if (composing.isNotEmpty()) {
            composing.setLength(composing.length - 1)
            if (composing.isEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.setComposingText(composing.toString(), 1)
            }
        } else {
            // keep the undo ledger honest: if the char being deleted came from
            // the newest tracked run, shrink that run instead of leaving a
            // stale entry behind
            val victim = ic.getTextBeforeCursor(1, 0)?.toString() ?: ""
            val top = undoStack.lastOrNull()
            if (victim.isNotEmpty() && top != null && !top.deletion && top.text.endsWith(victim)) {
                top.text = top.text.dropLast(1)
                if (top.text.isEmpty()) {
                    undoStack.removeLast()
                    undoCharMerged = false
                }
            }
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        }
        lastCommitWasSpace = false
        updateSuggestions()
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        pendingUndoWord = ""
        if (composing.isNotEmpty()) {
            val word = composing.toString()
            learnerBoost(word)
            lastWord = word.lowercase()
            composing.setLength(0)
            ic.finishComposingText()
            trackInsert(word)
            if (prefs.shortcuts) tryShortcut(word)
        }
        val action = currentInputEditorInfo
        if (action != null && action.actionId != EditorInfo.IME_NULL &&
            action.actionId != EditorInfo.IME_ACTION_NONE
        ) {
            ic.performEditorAction(action.actionId)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
        lastCommitWasSpace = false
        state.setSentenceShift(prefs.autoCap)
        syncShiftVisuals()
        updateSuggestions()
    }

    /** "omw" + space → "on my way". Returns true if expanded. */
    private fun tryShortcut(word: String): Boolean {
        if (!prefs.shortcuts || secureField || word.isEmpty()) return false
        val expansion = shortcuts.expansionFor(word) ?: return false
        val ic = currentInputConnection ?: return false
        // typed word was already committed as composing text; delete it
        ic.deleteSurroundingText(word.length, 0)
        ic.commitText(expansion, 1)
        trackInsert(expansion)
        host?.candidateBar?.showHint("→ $expansion")
        ui.removeCallbacks(clearHintRunnable)
        ui.postDelayed(clearHintRunnable, 1200)
        return true
    }

    private fun learnerBoost(word: String) {
        if (!prefs.learning || privacyHold() || secureField) return
        if (word.length < 2 || !word[0].isLetter()) return
        learner.boost(word)
        if (++boostCounter % 5 == 0) learner.persist()
    }

    private fun checkSentenceShift(punct: String) {
        if (!prefs.autoCap) return
        if (punct.length == 1 && punct[0] in ".!?…") {
            state.setSentenceShift(true)
            syncShiftVisuals()
        }
    }

    private fun composingFinishQuietly() {
        val ic = currentInputConnection ?: return
        if (composing.isNotEmpty()) {
            ic.finishComposingText()
            composing.setLength(0)
        }
    }

    // ------------------------------------------------------------------
    // Suggestions
    // ------------------------------------------------------------------

    private fun updateSuggestions() {
        val bar = host?.candidateBar ?: return
        if (secureField) {
            // live offline strength meter — computed in place from the field
            // contents, never stored, never learned on (no network exists)
            val ic = currentInputConnection
            val pw = buildString {
                append(ic?.getTextBeforeCursor(64, 0) ?: "")
                append(ic?.getSelectedText(0) ?: "")
                append(ic?.getTextAfterCursor(64, 0) ?: "")
            }
            val r = StrengthMeter.analyze(pw)
            if (r.level < 0) {
                bar.showStrength(-1, "")
            } else {
                val label = getString(when (r.level) {
                    0 -> R.string.pwd_weak
                    1 -> R.string.pwd_fair
                    2 -> R.string.pwd_good
                    else -> R.string.pwd_strong
                })
                bar.showStrength(r.level, label)
            }
            return
        }
        if (dictionaries.isEmpty()) {
            bar.setSuggestions(emptyList())
            return
        }
        val typed = composing.toString()
        // same proper-noun protection as the space commit: internal capitals
        // (DRS, iPhone…) are never offered a "did you mean" fix
        val protectCap = typed.indexOfFirst { it.isUpperCase() } > 0
        val suggestions = suggestionEngine.suggest(
            state.lang, typed, lastWord,
            prefs.autocorrect && !noAutocorrectField && !protectCap
        )
        val items = suggestions.map { it.word to it.isCorrection }.toMutableList()
        // inline calculator: a complete math expression sits right before the cursor
        mathExpr = null
        val before = currentInputConnection?.getTextBeforeCursor(48, 0)?.toString() ?: ""
        val expr = MathEval.trailingExpr(before)
        if (expr != null) {
            val result = MathEval.evaluate(expr)
            if (result != null) {
                mathExpr = expr
                items.add(0, "= " + MathEval.format(result) to false)
            }
        }
        // rule-based emoji chip: matches the word being typed (or just finished)
        if (prefs.emojiSuggest && mathExpr == null && items.size < 3) {
            val probe = typed.ifEmpty { lastWord }
            val emoji = EmojiSuggest.forWord(probe)
            if (emoji != null) items.add(emoji to false)
        }
        bar.setSuggestions(items)
    }

    override fun onCandidatePicked(word: String, isCorrection: Boolean) {
        val ic = currentInputConnection ?: return
        // calculator result chip: replace the whole typed expression with the result
        if (word.startsWith("= ")) {
            val expr = mathExpr
            composingFinishQuietly()
            if (expr != null) ic.deleteSurroundingText(expr.length, 0)
            ic.commitText(word.substring(2), 1)
            trackInsert(word.substring(2))
            mathExpr = null
            lastCommitWasSpace = false
            updateSuggestions()
            return
        }
        // emoji chip: keep the typed word untouched, just add the emoji
        if (EmojiSuggest.isEmojiChip(word)) {
            composingFinishQuietly()
            // space only when a word sits right before the cursor
            val lead = if (extractLastWord().isNotEmpty()) " " else ""
            ic.commitText(lead + word, 1)
            trackInsert(lead + word)
            lastCommitWasSpace = false
            if (!prefs.incognito) StatsStore.bump(this, StatsStore.EMOJI)
            updateSuggestions()
            return
        }
        if (composing.isNotEmpty()) {
            composing.setLength(0)
            ic.setComposingText(word, 1)
            ic.finishComposingText()
            learnerBoost(word)
            lastWord = word.lowercase()
            ic.commitText(" ", 1)
            trackInsert(word + " ")
            lastCommitWasSpace = true
            lastSpaceTime = System.currentTimeMillis()
        } else {
            // next-word suggestion: commit directly
            ic.commitText(word + " ", 1)
            trackInsert(word + " ")
            lastWord = word.lowercase()
            learnerBoost(word)
            lastCommitWasSpace = true
            lastSpaceTime = System.currentTimeMillis()
        }
        state.consumeShift()
        syncShiftVisuals()
        updateSuggestions()
    }

    override fun onCandidateLongPress(word: String) {
        if (!prefs.learning || privacyHold()) return
        learner.boost(word, 5)
        learner.persist()
        host?.candidateBar?.showHint("★ $word")
        ui.removeCallbacks(clearHintRunnable)
        ui.postDelayed(clearHintRunnable, 1000)
    }

    // ------------------------------------------------------------------
    // Swipe / cursor / delete-word
    // ------------------------------------------------------------------

    override fun onSwipeWord(word: String) {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        pendingUndoWord = ""
        composingFinishQuietly()
        ic.commitText(word, 1)
        learnerBoost(word)
        if (lastWord.isNotEmpty() && !privacyHold()) (ngrams[state.lang])?.learn(lastWord, word)
        lastWord = word.lowercase()
        ic.commitText(" ", 1)
        trackInsert(word + " ")
        lastCommitWasSpace = true
        lastSpaceTime = System.currentTimeMillis()
        if (!prefs.incognito) StatsStore.bump(this, StatsStore.SWIPES)
        updateSuggestions()
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        pendingUndo = null
        pendingUndoWord = ""
        composingFinishQuietly()
        ic.commitText(text, 1)
        trackInsert(text)
        lastCommitWasSpace = false
        if (text.isNotEmpty() && text[0].code >= 0x1F000 ||
            text.isNotEmpty() && text[0].code in 0x2600..0x27BF) {
            if (!prefs.incognito) StatsStore.bump(this, StatsStore.EMOJI)
        }
        if (text.length == 1 && text[0] in ".!?…") checkSentenceShift(text)
        updateSuggestions()
    }

    override fun onCursorMove(delta: Int) {
        val ic = currentInputConnection ?: return
        val code = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        // in selection mode the arrows extend the selection (shift + arrow)
        val meta = if (selectMode) KeyEvent.META_SHIFT_ON else 0
        repeat(kotlin.math.abs(delta)) {
            ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
                KeyEvent.ACTION_DOWN, code, 0, meta))
            ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
                KeyEvent.ACTION_UP, code, 0, meta))
        }
    }

    /** Vertical / line-edge cursor moves for the arrows row (multiline fields). */
    private fun sendDpad(code: Int) {
        val ic = currentInputConnection ?: return
        val meta = if (selectMode) KeyEvent.META_SHIFT_ON else 0
        ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
            KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
            KeyEvent.ACTION_UP, code, 0, meta))
    }

    /** Long-press on ← / →: jump a whole word (Ctrl+arrow — the standard
     *  text-field semantics, honored by every editor). In selection mode the
     *  jump extends the selection word-by-word (Ctrl+Shift+arrow). */
    private fun jumpWord(dir: Int) {
        val ic = currentInputConnection ?: return
        val code = if (dir < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        var meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (selectMode) meta = meta or KeyEvent.META_SHIFT_ON
        ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
            KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
            KeyEvent.ACTION_UP, code, 0, meta))
    }

    override fun onDeleteWord() {
        val ic = currentInputConnection ?: return
        if (composing.isNotEmpty()) {
            composing.setLength(0)
            ic.commitText("", 1)
        } else {
            val before = ic.getTextBeforeCursor(64, 0) ?: ""
            var count = 0
            var i = before.length - 1
            while (i >= 0 && !before[i].isLetter()) { count++; i-- }
            while (i >= 0 && before[i].isLetter()) { count++; i-- }
            if (count > 0) ic.deleteSurroundingText(count, 0)
        }
        lastWord = extractLastWord()
        lastCommitWasSpace = false
        updateSuggestions()
    }

    override fun onSpaceDragFinished() { /* nothing to clean up */ }

    override fun onSpaceLongPress() {
        val h = host ?: return
        spaceLongPressCount++
        when (spaceLongPressCount % 4) {
            1 -> { h.setOneHand(right = false); prefs.uiMode = 1; toast(R.string.mode_onehand_left) }
            2 -> { h.setOneHand(right = true); prefs.uiMode = 2; toast(R.string.mode_onehand_right) }
            3 -> { h.applyFloating(true); prefs.uiMode = 3; toast(R.string.mode_floating) }
            else -> { h.setFullWidth(); prefs.uiMode = 0; toast(R.string.mode_full) }
        }
    }

    private fun toast(res: Int) {
        android.widget.Toast.makeText(this, res, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------------
    // Host plumbing
    // ------------------------------------------------------------------

    override fun onOpenSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requestHideSelf(0)
    }

    override fun onPanelChanged(panel: DrsKeyboardHost.Panel) {
        if (panel == DrsKeyboardHost.Panel.EMOJI) {
            host?.emojiPanel?.setTheme(currentTheme())
        }
    }

    /** Held ⋮ in the candidate bar: cycle to the next glass theme pack. */
    override fun onQuickThemeSwitch() {
        val presets = ThemeRepo.presets()
        val nightActive = prefs.autoNight && nightNow() && ThemeRepo.loadNight(this) != null
        val current = if (nightActive) ThemeRepo.loadNight(this)!! else ThemeRepo.loadCurrent(this)
        val idx = presets.indexOfFirst { it.id == current.id }
        val next = presets[(idx + 1).mod(presets.size)]
        if (nightActive) ThemeRepo.saveNight(this, next) else ThemeRepo.saveCurrent(this, next)
        host?.applyTheme(next)
        host?.candidateBar?.showHint(getString(R.string.quick_theme_hint, next.name))
        ui.removeCallbacks(clearHintRunnable)
        ui.postDelayed(clearHintRunnable, 1000)
    }

    // ------------------------------------------------------------------
    // Text editing toolbar (hold the clipboard icon)
    // ------------------------------------------------------------------

    /** Multi-level undo of OUR OWN recent edits: keystrokes, corrections,
     *  suggestions, swipes, shortcuts, pastes, emoji, calculator results. */
    private fun performUndo() {
        val ic = currentInputConnection ?: return
        while (undoStack.isNotEmpty()) {
            val e = undoStack.removeLast()
            undoCharMerged = false
            if (e.deletion) {
                // entry recorded a cleared block → restore it at the cursor
                ic.commitText(e.text, 1)
                showUndoHint(e.text)
                updateSuggestions()
                return
            }
            val before = ic.getTextBeforeCursor(e.text.length, 0)?.toString() ?: ""
            if (before == e.text) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(e.text.length, 0)
                ic.endBatchEdit()
                showUndoHint(e.text)
                updateSuggestions()
                return
            }
            // stale entry (app edited itself in between) — discard and keep looking
        }
        toast(R.string.edit_undo_none)
    }

    private fun showUndoHint(text: String) {
        val shown = text.replace("\n", "⏎")
        host?.candidateBar?.showHint(getString(R.string.edit_undo_done, shown))
        ui.removeCallbacks(clearHintRunnable)
        ui.postDelayed(clearHintRunnable, 1400)
    }

    /** Wipe the entire field (before + after the cursor), keep a restore
     *  entry on the undo stack so one tap of ↩ brings everything back. */
    private fun performClearAll() {
        val ic = currentInputConnection ?: return
        composingFinishQuietly()
        val cleared = StringBuilder()
        var guard = 0
        while (guard++ < 10) {
            val before = ic.getTextBeforeCursor(20000, 0)?.toString() ?: ""
            val after = ic.getTextAfterCursor(20000, 0)?.toString() ?: ""
            if (before.isEmpty() && after.isEmpty()) break
            cleared.append(before).append(after)
            ic.deleteSurroundingText(before.length, after.length)
        }
        pendingUndo = null
        pendingUndoWord = ""
        lastWord = ""
        lastCommitWasSpace = false
        if (cleared.isNotEmpty()) {
            undoStack.addLast(UndoEntry(cleared.toString(), deletion = true))
            if (undoStack.size > 50) undoStack.removeFirst()
            undoCharMerged = false
        }
        updateSuggestions()
    }

    /** One-tap paste from the quick-paste chip in the suggestion strip. */
    override fun onPasteChipTapped() {
        val text = pendingPaste ?: return
        val ic = currentInputConnection ?: return
        composingFinishQuietly()
        ic.commitText(text, 1)
        trackInsert(text)
        pendingPaste = null
        lastCommitWasSpace = false
        updateSuggestions()
        host?.candidateBar?.showHint(getString(R.string.edit_pasted))
        ui.removeCallbacks(clearHintRunnable)
        ui.postDelayed(clearHintRunnable, 1100)
    }

    override fun onEditAction(action: String) {
        val ic = currentInputConnection
        when (action) {
            "close" -> {
                if (selectMode) {
                    selectMode = false
                    host?.setSelectActive(false)
                    host?.arrowRow = prefs.arrowRow
                    host?.refreshLayout()
                }
                host?.showEditBar(false)
                return
            }
            "select" -> {
                selectMode = !selectMode
                host?.setSelectActive(selectMode)
                // the arrows must be on-screen while selecting
                host?.arrowRow = prefs.arrowRow || selectMode
                host?.refreshLayout()
                // the candidate strip is hidden while the edit bar is open → toast instead
                toast(if (selectMode) R.string.select_mode_on else R.string.select_mode_off)
                return
            }
            "selectall" -> ic?.performContextMenuAction(android.R.id.selectAll)
            "copy" -> ic?.performContextMenuAction(android.R.id.copy)
            "cut" -> ic?.performContextMenuAction(android.R.id.cut)
            "paste" -> {
                if (ic != null) {
                    // best-effort undo tracking: diff the text before the cursor
                    val before = ic.getTextBeforeCursor(4000, 0)?.toString() ?: ""
                    ic.performContextMenuAction(android.R.id.paste)
                    ui.postDelayed({
                        val ic2 = currentInputConnection ?: return@postDelayed
                        val after = ic2.getTextBeforeCursor(4000, 0)?.toString() ?: ""
                        if (after.length > before.length && after.startsWith(before)) {
                            trackInsert(after.substring(before.length))
                        }
                    }, 150)
                }
            }
            "undo" -> {
                performUndo()
                return  // keep the toolbar open for repeat presses
            }
            "clear" -> performClearAll()
        }
        // close the toolbar and confirm via the suggestion-strip hint
        host?.showEditBar(false)
        val msg = when (action) {
            "selectall" -> getString(R.string.edit_selected_all)
            "copy" -> getString(R.string.edit_copied)
            "cut" -> getString(R.string.edit_cut)
            "paste" -> getString(R.string.edit_pasted)
            "clear" -> getString(R.string.edit_cleared)
            else -> ""
        }
        if (msg.isNotEmpty()) {
            host?.candidateBar?.showHint(msg)
            ui.removeCallbacks(clearHintRunnable)
            ui.postDelayed(clearHintRunnable, 1100)
        }
    }

    private fun audioManager(): AudioManager? =
        getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** Key tap haptic with user-tunable strength (light / normal / strong). */
    private fun haptic() {
        val amp = when (prefs.vibrateStrength) {
            0 -> 48; 2 -> 224; else -> 128
        }
        val vib = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vib.hasVibrator()) return
        vib.vibrate(VibrationEffect.createOneShot(14, amp))
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    companion object {
        /** Discoverability hint for the edit toolbar: once per IME process. */
        private var editHintShown = false
    }
}
