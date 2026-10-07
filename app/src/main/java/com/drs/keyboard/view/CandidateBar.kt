package com.drs.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.drs.keyboard.theme.KeyboardTheme
import kotlin.math.max

/**
 * Suggestion strip: three candidates (center = primary) plus two utility
 * icons on the edges (clipboard history, keyboard settings). Long-press a
 * candidate to teach the learner the word. Also displays swipe results and
 * shortcut-expansion hints.
 */
class CandidateBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Callback {
        fun onCandidatePicked(word: String, isCorrection: Boolean)
        fun onCandidateLongPress(word: String)
        fun onClipboardIcon()
        fun onSettingsIcon()
        /** Long-press on the utility (⋮) icon — used for quick theme switching. */
        fun onSettingsLongPress() {}
        /** Long-press on the clipboard icon — opens the text editing toolbar. */
        fun onClipboardLongPress() {}
        /** Tap on the quick-paste chip (fresh clipboard text available). */
        fun onPasteChipTapped() {}
    }

    var callback: Callback? = null

    // password-strength meter state (secure fields only)
    private var strengthLevel = -1   // -1 hidden, 0..3 segments lit
    private var strengthLabel = ""
    private val strengthColors = intArrayOf(
        0xFFE5484D.toInt(), // weak    — red
        0xFFFFB224.toInt(), // fair    — amber
        0xFFA8C938.toInt(), // good    — lime
        0xFF30BD6D.toInt()  // strong  — green
    )

    private var theme: KeyboardTheme? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val primaryText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondaryText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val divider = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val watermark = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipRing = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillRing = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strengthFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strengthTrack = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strengthText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var words: List<Pair<String, Boolean>> = emptyList() // word + isCorrection
    private var hint: String? = null
    private var pasteHint = false          // hint is tappable → pastes the clipboard
    private var pressedZone = -1
    private var longPressFired = false

    private val d = resources.displayMetrics.density

    fun setTheme(t: KeyboardTheme) {
        theme = t
        fill.color = t.candidateBg
        stroke.color = t.strokeColor
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = d
        primaryText.color = t.keyTextColor
        primaryText.isAntiAlias = true
        primaryText.textAlign = Paint.Align.CENTER
        primaryText.isFakeBoldText = true
        secondaryText.color = t.keyTextColor
        secondaryText.isAntiAlias = true
        secondaryText.textAlign = Paint.Align.CENTER
        secondaryText.alpha = 200
        accentText.color = t.accentColor
        accentText.isAntiAlias = true
        accentText.textAlign = Paint.Align.CENTER
        accentText.isFakeBoldText = true
        divider.color = t.strokeColor
        iconPaint.color = t.keyTextColor
        iconPaint.isAntiAlias = true
        // idle brand mark: subtle DRS wordmark + accent dot
        watermark.color = t.keyTextColor
        watermark.isAntiAlias = true
        watermark.textAlign = Paint.Align.CENTER
        watermark.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        watermark.letterSpacing = 0.24f
        watermark.textSize = d * 11.5f
        chipRing.style = Paint.Style.STROKE
        chipRing.strokeWidth = d * 0.9f
        chipRing.color = t.keyTextColor
        chipRing.alpha = 34
        pillRing.style = Paint.Style.STROKE
        pillRing.strokeWidth = d * 1f
        pillRing.color = t.accentColor
        pillRing.alpha = 95
        strengthTrack.style = Paint.Style.FILL
        strengthTrack.color = t.keyTextColor
        strengthTrack.alpha = 36
        strengthText.color = t.keyTextColor
        strengthText.isAntiAlias = true
        strengthText.textAlign = Paint.Align.CENTER
        strengthText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        strengthText.textSize = d * 13f
        invalidate()
    }

    /** words: up to 3; first entry renders center, then right, then left. */
    fun setSuggestions(list: List<Pair<String, Boolean>>) {
        words = list.take(3)
        hint = null
        pasteHint = false
        strengthLevel = -1
        invalidate()
    }

    fun showHint(text: String) {
        hint = text
        pasteHint = false
        strengthLevel = -1
        invalidate()
    }

    /** Tappable centered chip: one tap pastes the clipboard text. */
    fun showPasteHint(text: String) {
        hint = text
        pasteHint = true
        words = emptyList()
        invalidate()
    }

    /**
     * Live password-strength meter (secure fields). level −1 hides it;
     * 0..3 lights that many segments in a semantic color.
     */
    fun showStrength(level: Int, label: String) {
        strengthLevel = level
        strengthLabel = label
        words = emptyList()
        hint = null
        pasteHint = false
        invalidate()
    }

    private fun clearStrength() {
        if (strengthLevel != -1) {
            strengthLevel = -1
            invalidate()
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthSpec),
            (d * 42f).toInt()
        )
    }

    private fun iconRect(which: Int): RectF {
        val size = d * 26f
        val cy = height / 2f
        return when (which) {
            0 -> RectF(d * 8f, cy - size / 2, d * 8f + size, cy + size / 2)   // clipboard
            else -> RectF(width - d * 8f - size, cy - size / 2, width - d * 8f, cy + size / 2)
        }
    }

    private fun wordZone(index: Int): RectF {
        // zones: [left icon][left word][center word][right word][right icon]
        val left = iconRect(0).right + d * 4
        val right = iconRect(1).left - d * 4
        val w = (right - left) / 3f
        return when (index) {
            0 -> RectF(left + w, 0f, left + 2 * w, height.toFloat())      // center
            1 -> RectF(left + 2 * w, 0f, right, height.toFloat())          // right
            else -> RectF(left, 0f, left + w, height.toFloat())            // left
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = d * 12f
        fill.alpha = max(fill.alpha, 10)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, fill)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, stroke)

        // utility icons inside tinted circles
        drawIconChip(canvas, iconRect(0), "clip")
        drawIconChip(canvas, iconRect(1), "kebab")

        val h = hint
        if (h != null) {
            val p = if (pasteHint) accentText else primaryText
            p.textSize = d * 14f
            p.alpha = if (pasteHint) 255 else 190
            canvas.drawText(h, width / 2f, height / 2f - (p.ascent() + p.descent()) / 2f, p)
            if (!pasteHint) primaryText.alpha = 255
            return
        }

        // live password-strength meter: 4-segment gauge + semantic label
        if (strengthLevel in 0..3 && words.isEmpty()) {
            val segW = d * 13f; val segH = d * 5f; val segGap = d * 2.5f
            val barW = 4 * segW + 3 * segGap
            val lw = strengthText.measureText(strengthLabel)
            val total = barW + d * 8f + lw
            var sx = width / 2f - total / 2f
            val segY = height / 2f - segH / 2f
            val color = strengthColors[strengthLevel.coerceAtMost(3)]
            for (i in 0 until 4) {
                if (i <= strengthLevel) {
                    strengthFill.color = color
                    strengthFill.alpha = if (i == strengthLevel) 235 else 145
                    canvas.drawRoundRect(sx, segY, sx + segW, segY + segH,
                        segH / 2f, segH / 2f, strengthFill)
                } else {
                    canvas.drawRoundRect(sx, segY, sx + segW, segY + segH,
                        segH / 2f, segH / 2f, strengthTrack)
                }
                sx += segW + segGap
            }
            val ty = height / 2f - (strengthText.ascent() + strengthText.descent()) / 2f
            strengthText.color = color
            canvas.drawText(strengthLabel, sx + d * 8f + lw / 2f, ty, strengthText)
            strengthText.color = theme?.keyTextColor ?: 0
            return
        }

        if (words.isEmpty()) {
            // idle state: quiet brand presence instead of an empty void
            val wm = "DRS"
            val tyw = height / 2f - (watermark.ascent() + watermark.descent()) / 2f
            watermark.alpha = 44
            canvas.drawText(wm, width / 2f, tyw, watermark)
            watermark.alpha = 255
            val dot = Paint(watermark).apply {
                color = theme?.accentColor ?: 0
                style = Paint.Style.FILL
                alpha = 105
            }
            val tw = watermark.measureText(wm)
            canvas.drawCircle(width / 2f + tw / 2f + d * 4f, height / 2f - d * 0.6f, d * 1.7f, dot)
            return
        }

        // candidate chips: center = primary pill, sides = soft pills
        for (i in words.indices) {
            val zone = wordZone(i)
            val (word, isCorrection) = words[i]
            val pillH = height - d * 10f
            chipRect.set(
                zone.left + d * 3f, (height - pillH) / 2f,
                zone.right - d * 3f, (height - pillH) / 2f + pillH)
            val pressed = pressedZone == i + 10
            chipFill.color = when {
                pressed -> 0x55000000 or ((theme?.accentColor ?: 0) and 0x00FFFFFF)
                isCorrection || i == 0 ->
                    0x30000000 or ((theme?.accentColor ?: 0) and 0x00FFFFFF)
                else -> 0x16000000 or ((theme?.keyTextColor ?: 0) and 0x00FFFFFF)
            }
            canvas.drawRoundRect(chipRect, pillH / 2f, pillH / 2f, chipFill)
            if (isCorrection || i == 0) {
                canvas.drawRoundRect(chipRect, pillH / 2f, pillH / 2f, pillRing)
            }

            val p = when {
                pressed -> accentText
                isCorrection -> accentText
                i == 0 -> primaryText
                else -> secondaryText
            }
            p.textSize = if (i == 0) d * 16.5f else d * 14.5f
            val ty = zone.centerY() - (p.ascent() + p.descent()) / 2f
            canvas.drawText(word, zone.centerX(), ty, p)
        }
    }

    private val chipRect = RectF()
    private val chipFill = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun drawIconChip(canvas: Canvas, rect: RectF, icon: String) {
        val pressed = (pressedZone == 0 && icon == "clip") || (pressedZone == 1 && icon == "kebab")
        chipFill.color = if (pressed)
            0x44000000 or ((theme?.accentColor ?: 0) and 0x00FFFFFF)
        else
            0x1A000000 or ((theme?.keyTextColor ?: 0) and 0x00FFFFFF)
        val cx = rect.centerX(); val cy = rect.centerY(); val rr = rect.width() / 2f
        canvas.drawCircle(cx, cy, rr, chipFill)
        chipRing.alpha = if (pressed) 70 else 34
        canvas.drawCircle(cx, cy, rr - d * 0.5f, chipRing)
        chipRing.alpha = 34
        iconPaint.color = if (pressed) theme?.accentColor ?: 0 else theme?.keyTextColor ?: 0
        IconPainter.draw(canvas, icon, cx, cy, d * 19f, iconPaint)
    }

    private fun blend(c1: Int, c2: Int, t: Float): Int {
        val a1 = c1 ushr 24; val r1 = (c1 shr 16) and 0xFF; val g1 = (c1 shr 8) and 0xFF; val b1 = c1 and 0xFF
        val a2 = c2 ushr 24; val r2 = (c2 shr 16) and 0xFF; val g2 = (c2 shr 8) and 0xFF; val b2 = c2 and 0xFF
        val a = (a1 + (a2 - a1) * t).toInt()
        val r = (r1 + (r2 - r1) * t).toInt()
        val g = (g1 + (g2 - g1) * t).toInt()
        val b = (b1 + (b2 - b1) * t).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedZone = zoneAt(event.x, event.y)
                longPressFired = false
                if (pressedZone >= 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (pressedZone >= 10 || pressedZone == 0 || pressedZone == 1) {
                        longPressHandler.postDelayed(longPressRunnable, 500)
                    }
                    postInvalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val z = zoneAt(event.x, event.y)
                if (z != pressedZone && !longPressFired) {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    pressedZone = z
                    postInvalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                val z = zoneAt(event.x, event.y)
                val idx = if (longPressFired) -1 else pressedZone
                pressedZone = -1
                longPressFired = false
                postInvalidate()
                if (z >= 0 && z == idx) {
                    when {
                        z == 0 -> callback?.onClipboardIcon()
                        z == 1 -> callback?.onSettingsIcon()
                        z == 5 -> callback?.onPasteChipTapped()
                        z >= 10 -> {
                            val wi = z - 10
                            if (wi < words.size) callback?.onCandidatePicked(words[wi].first, words[wi].second)
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                pressedZone = -1
                longPressFired = false
                postInvalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        val idx = pressedZone
        if (idx == 0) {
            // clipboard held: text editing toolbar (consumes the press)
            pressedZone = -1
            longPressFired = true
            postInvalidate()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            callback?.onClipboardLongPress()
        } else if (idx == 1) {
            // kebab held: quick theme cycle (consumes the press)
            pressedZone = -1
            longPressFired = true
            postInvalidate()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            callback?.onSettingsLongPress()
        } else if (idx >= 10) {
            pressedZone = -1
            longPressFired = true
            postInvalidate()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val wi = idx - 10
            if (wi < words.size) callback?.onCandidateLongPress(words[wi].first)
        }
    }

    private fun zoneAt(x: Float, y: Float): Int {
        if (iconRect(0).contains(x, y)) return 0
        if (iconRect(1).contains(x, y)) return 1
        if (pasteHint && x > iconRect(0).right + d * 4 &&
            x < iconRect(1).left - d * 4 && y in 0f..height.toFloat()) return 5
        for (i in words.indices) {
            if (wordZone(i).contains(x, y)) return 10 + i
        }
        return -1
    }
}
