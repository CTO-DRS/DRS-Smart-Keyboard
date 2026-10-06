package com.drs.keyboard.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import com.drs.keyboard.clipboard.ClipboardHistory
import com.drs.keyboard.clipboard.ClipboardPanel
import com.drs.keyboard.keyboard.KeyDef
import com.drs.keyboard.keyboard.KeyboardState
import com.drs.keyboard.keyboard.Layouts
import com.drs.keyboard.theme.KeyboardTheme
import java.io.File

/**
 * Root container of the keyboard window:
 *
 *   ┌───────────────────────────────┐
 *   │ candidate bar (glass strip)   │
 *   ├───────────────────────────────┤
 *   │                               │
 *   │ keyboard view (the keys)      │
 *   │                               │
 *   └───────────────────────────────┘
 *   + emoji / clipboard overlay panels
 *   + one-handed and floating geometry modes
 *
 * Draws the glass-depth backdrop: rounded translucent panel, soft shadow,
 * top sheen, optional blurred-background image.
 */
class DrsKeyboardHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface HostCallback {
        fun onKey(key: KeyDef)
        fun onText(text: String)
        fun onSwipeWord(word: String)
        fun onCursorMove(delta: Int)
        fun onDeleteWord()
        fun onSpaceDragFinished()
        fun onSpaceLongPress()
        fun onCandidatePicked(word: String, isCorrection: Boolean)
        fun onCandidateLongPress(word: String)
        fun onOpenSettings()
        fun onPanelChanged(panel: Panel)
        fun onQuickThemeSwitch()
        fun onEditAction(action: String)
        fun onPasteChipTapped()
    }

    enum class Panel { NONE, EMOJI, CLIPBOARD }

    var hostCallback: HostCallback? = null
    var state = KeyboardState()
    var theme: KeyboardTheme? = null
        private set
    var arabicDigits = false
    /** Optional navigation arrows row, toggled from settings. */
    var arrowRow = false
    /** Optional Arabic tashkeel row (alpha mode, Arabic layout only). */
    var tashkeelRow = false

    val candidateBar = CandidateBar(context)
    val editBar = EditBar(context)
    val keyboardView = DrsKeyboardView(context)
    lateinit var clipboardPanel: ClipboardPanel
    lateinit var emojiPanel: EmojiPanel

    private var panel = Panel.NONE
    private var editMode = false

    // glass rendering
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgBitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var bgBitmap: Bitmap? = null
    private val clipRect = RectF()

    // one-hand / floating
    var widthFraction = 1f        // 1.0 full, 0.86 one-hand, 0.72 floating
    var anchoredRight = true
    var floating = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragTransX = 0f
    private var dragTransY = 0f
    private var dragMode = false

    private val dragHandle = FrameLayout(context).apply {
        visibility = GONE
    }

    init {
        addView(keyboardView)
        addView(candidateBar)
        addView(editBar)
        editBar.visibility = GONE
        editBar.callback = object : EditBar.Callback {
            override fun onEditAction(action: String) {
                hostCallback?.onEditAction(action)
            }
        }
        candidateBar.callback = object : CandidateBar.Callback {
            override fun onCandidatePicked(word: String, isCorrection: Boolean) {
                hostCallback?.onCandidatePicked(word, isCorrection)
            }

            override fun onCandidateLongPress(word: String) {
                hostCallback?.onCandidateLongPress(word)
            }

            override fun onClipboardIcon() {
                togglePanel(Panel.CLIPBOARD)
            }

            override fun onClipboardLongPress() {
                // hold the clipboard icon: open the text editing toolbar
                showEditBar(true)
            }

            override fun onSettingsIcon() {
                hostCallback?.onOpenSettings()
            }

            override fun onSettingsLongPress() {
                hostCallback?.onQuickThemeSwitch()
            }

            override fun onPasteChipTapped() {
                hostCallback?.onPasteChipTapped()
            }
        }
        keyboardView.callback = object : DrsKeyboardView.Callback {
            override fun onKey(key: KeyDef) = hostCallback?.onKey(key) ?: Unit
            override fun onText(text: String) = hostCallback?.onText(text) ?: Unit
            override fun onSwipeWord(word: String) = hostCallback?.onSwipeWord(word) ?: Unit
            override fun onCursorMove(delta: Int) = hostCallback?.onCursorMove(delta) ?: Unit
            override fun onDeleteWord() = hostCallback?.onDeleteWord() ?: Unit
            override fun onSpaceDragFinished() = hostCallback?.onSpaceDragFinished() ?: Unit
            override fun onSpaceLongPress() = hostCallback?.onSpaceLongPress() ?: Unit
        }
        addView(dragHandle, LayoutParams(LayoutParams.MATCH_PARENT, dp(18)))
    }

    fun attachClipboard(history: ClipboardHistory) {
        clipboardPanel = ClipboardPanel(context, history).apply {
            visibility = GONE
            callback = object : ClipboardPanel.Callback {
                override fun onPaste(text: String) {
                    hostCallback?.onText(text)
                }

                override fun onDismiss() {
                    togglePanel(Panel.NONE)
                }
            }
        }
        addView(clipboardPanel)
    }

    fun attachEmoji() {
        emojiPanel = EmojiPanel(context).apply {
            visibility = GONE
            callback = object : EmojiPanel.Callback {
                override fun onEmojiPicked(emoji: String) {
                    hostCallback?.onText(emoji)
                }

                override fun onDismiss() {
                    togglePanel(Panel.NONE)
                }
            }
        }
        addView(emojiPanel)
    }

    // ------------------------------------------------------------------
    // Theme + geometry
    // ------------------------------------------------------------------

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        panelPaint.color = t.bgColor
        panelPaint.style = Paint.Style.FILL
        strokePaint.color = t.strokeColor
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = resources.displayMetrics.density
        val d = resources.displayMetrics.density
        sheenPaint.shader = LinearGradient(
            0f, 0f, 0f, d * 44f,
            0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        keyboardView.setTheme(t)
        candidateBar.setTheme(t)
        editBar.setTheme(t)
        loadBgImage(t.bgImagePath)
        if (this::emojiPanel.isInitialized) emojiPanel.setTheme(t)
        if (this::clipboardPanel.isInitialized) clipboardPanel.setTheme(t)
        invalidate()
    }

    private fun loadBgImage(path: String?) {
        bgBitmap = if (path != null && File(path).exists()) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        } else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = theme ?: return
        val d = resources.displayMetrics.density
        val radius = t.cornerRadiusDp * d
        clipRect.set(0f, 0f, width.toFloat(), height.toFloat())

        val save = canvas.save()
        // clip children drawing? no — just paint the backdrop
        val rrect = RectF(clipRect)
        canvas.drawRoundRect(rrect, radius, radius, panelPaint)
        val bmp = bgBitmap
        if (bmp != null) {
            val shader = android.graphics.BitmapShader(
                bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            val scale = maxOf(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
            val m = android.graphics.Matrix()
            m.setScale(scale, scale)
            shader.setLocalMatrix(m)
            bgBitmapPaint.shader = shader
            bgBitmapPaint.alpha = 90
            canvas.drawRoundRect(rrect, radius, radius, bgBitmapPaint)
        }
        if (t.glass) {
            canvas.drawRoundRect(rrect, radius, radius, sheenPaint)
            // crisp top edge highlight line
            edgePaint.shader = LinearGradient(
                0f, 0f, 0f, d * 2f,
                0x59FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rrect, radius, radius, edgePaint)
        }
        canvas.drawRoundRect(rrect, radius, radius, strokePaint)
        canvas.restoreToCount(save)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val d = resources.displayMetrics.density
        val w = right - left
        val h = bottom - top
        val overlay = panel != Panel.NONE
        val candidateH = if (overlay) 0 else dp(42)
        keyboardView.layout(0, candidateH, w, candidateH + keyboardView.measuredHeight)
        candidateBar.layout(0, 0, w, candidateH)
        editBar.layout(0, 0, w, candidateH)
        if (!overlay) {
            candidateBar.visibility = if (editMode) GONE else VISIBLE
            editBar.visibility = if (editMode) VISIBLE else GONE
            keyboardView.visibility = VISIBLE
        }
        dragHandle.layout(0, 0, w, dp(18))
        val overlayTop = dp(6)
        when (panel) {
            Panel.EMOJI -> if (this::emojiPanel.isInitialized) {
                emojiPanel.layout(0, overlayTop, w, h - overlayTop)
            }
            Panel.CLIPBOARD -> if (this::clipboardPanel.isInitialized) {
                clipboardPanel.layout(0, overlayTop, w, h - overlayTop)
            }
            else -> {}
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val d = resources.displayMetrics.density
        keyboardView.measure(widthSpec, heightSpec)
        candidateBar.measure(widthSpec, MeasureSpec.makeMeasureSpec(dp(42), MeasureSpec.EXACTLY))
        editBar.measure(widthSpec, MeasureSpec.makeMeasureSpec(dp(42), MeasureSpec.EXACTLY))
        val candidateH = if (panel != Panel.NONE) 0 else dp(42)
        val total = when (panel) {
            Panel.NONE -> candidateH + keyboardView.measuredHeight
            else -> (d * 250).toInt() + keyboardView.measuredHeight / 3
        }
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthSpec),
            total
        )
        if (this::emojiPanel.isInitialized && panel == Panel.EMOJI) {
            emojiPanel.measure(widthSpec, MeasureSpec.makeMeasureSpec(total - dp(6), MeasureSpec.EXACTLY))
        }
        if (this::clipboardPanel.isInitialized && panel == Panel.CLIPBOARD) {
            clipboardPanel.measure(widthSpec, MeasureSpec.makeMeasureSpec(total - dp(6), MeasureSpec.EXACTLY))
        }
    }

    // ------------------------------------------------------------------
    // Panels
    // ------------------------------------------------------------------

    fun togglePanel(p: Panel) {
        panel = if (panel == p) Panel.NONE else p
        if (this::emojiPanel.isInitialized) {
            emojiPanel.visibility = if (panel == Panel.EMOJI) VISIBLE else GONE
        }
        if (this::clipboardPanel.isInitialized) {
            clipboardPanel.visibility = if (panel == Panel.CLIPBOARD) VISIBLE else GONE
        }
        val overlay = panel != Panel.NONE
        keyboardView.visibility = if (overlay) INVISIBLE else VISIBLE
        candidateBar.visibility = if (overlay || editMode) GONE else VISIBLE
        editBar.visibility = if (!overlay && editMode) VISIBLE else GONE
        dragHandle.visibility = if (floating) VISIBLE else GONE
        requestLayout()
        invalidate()
        hostCallback?.onPanelChanged(panel)
    }

    fun currentPanel(): Panel = panel

    /** Shows/hides the text editing toolbar (replaces the suggestion strip). */
    fun showEditBar(on: Boolean) {
        editMode = on
        if (on && panel != Panel.NONE) togglePanel(Panel.NONE)
        val overlay = panel != Panel.NONE
        candidateBar.visibility = if (overlay || editMode) GONE else VISIBLE
        editBar.visibility = if (!overlay && editMode) VISIBLE else GONE
        requestLayout()
        invalidate()
    }

    // ------------------------------------------------------------------
    // One-hand / floating
    // ------------------------------------------------------------------

    fun setOneHand(right: Boolean) {
        floating = false
        widthFraction = 0.86f
        anchoredRight = right
        dragTransX = 0f
        dragTransY = 0f
        applyGeometry()
    }

    fun applyFloating(on: Boolean) {
        floating = on
        widthFraction = if (on) 0.72f else 1f
        dragHandle.visibility = if (on) VISIBLE else GONE
        applyGeometry()
    }

    fun isFloating(): Boolean = floating

    /** Back to full-width, bottom-anchored geometry. */
    fun setFullWidth() {
        floating = false
        widthFraction = 1f
        dragTransX = 0f
        dragTransY = 0f
        dragHandle.visibility = GONE
        applyGeometry()
    }

    private fun applyGeometry() {
        val parent = parent as? FrameLayout
        if (parent == null) { requestLayout(); invalidate(); return }
        val pw = parent.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val d = resources.displayMetrics.density
        val margin = (d * 6).toInt()
        val w = (pw * widthFraction).toInt() - margin * 2
        val lp = layoutParams as FrameLayout.LayoutParams
        lp.width = w
        lp.gravity = when {
            floating -> Gravity.TOP or Gravity.START
            anchoredRight -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.BOTTOM or Gravity.START
        }
        lp.setMargins(margin, margin, margin, margin)
        translationX = dragTransX
        translationY = dragTransY
        pivotX = if (anchoredRight) w.toFloat() else 0f
        pivotY = 0f
        layoutParams = lp
        requestLayout()
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        dragHandle.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragMode = true
                    dragStartX = e.rawX
                    dragStartY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> if (dragMode) {
                    dragTransX += e.rawX - dragStartX
                    dragTransY += e.rawY - dragStartY
                    dragStartX = e.rawX
                    dragStartY = e.rawY
                    translationX = dragTransX
                    translationY = dragTransY
                    true
                } else false
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragMode = false
                    true
                }
                else -> false
            }
        }
    }

    // ------------------------------------------------------------------
    // Layout refresh
    // ------------------------------------------------------------------

    /** Rebuilds the visible keyboard rows from the state. */
    fun refreshLayout() {
        val rows = Layouts.rows(state.lang, state.mode, state.shifted, state.numberRow,
            arabicDigits, arrowRow, tashkeelRow)
        keyboardView.rtl = state.lang == "ar"
        keyboardView.spaceLabel = when (state.lang) {
            "ar" -> "عربي 101"
            else -> "QWERTY"
        }
        keyboardView.setRows(rows)
        keyboardView.setShiftVisuals(state.shifted, state.capsLocked)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    init {
        post { setupDrag() }
    }
}
