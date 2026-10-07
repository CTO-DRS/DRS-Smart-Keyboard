package com.drs.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.drs.keyboard.theme.KeyboardTheme
import kotlin.math.max

/**
 * Text editing toolbar rendered in place of the suggestion strip:
 *
 *   [undo] [select] [select all] [copy] [cut] [paste] [clear] | [close]
 *
 * Actions are reported to the host which executes them through the
 * InputConnection (performContextMenuAction). "select" toggles selection
 * mode: the navigation arrows then extend the selection (shift + arrows).
 * Pure painted UI, themed glass.
 */
class EditBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Callback {
        /** action: "undo" | "select" | "selectall" | "copy" | "cut" | "paste" | "clear" | "close" */
        fun onEditAction(action: String)
    }

    var callback: Callback? = null
    /** Whether selection mode is on ("select" chip glows accent). */
    var selectActive = false
        set(v) { field = v; postInvalidate() }

    private var theme: KeyboardTheme? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var pressedZone = -1

    private val d = resources.displayMetrics.density
    private val icons = listOf(
        "undo", "select", "selectall", "copy", "cut", "paste", "clear", "close"
    )
    private val chipRect = RectF()

    fun setTheme(t: KeyboardTheme) {
        theme = t
        fill.color = t.candidateBg
        stroke.color = t.strokeColor
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = d
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthSpec),
            (d * 42f).toInt()
        )
    }

    private fun zoneRect(i: Int): RectF {
        val n = icons.size
        val pad = d * 7f
        val w = (width - pad * 2) / n
        return RectF(pad + i * w, d * 4f, pad + (i + 1) * w, height - d * 4f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = d * 12f
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, fill)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, stroke)

        val t = theme
        for (i in icons.indices) {
            val r = zoneRect(i)
            val inset = r.width() * 0.18f
            chipRect.set(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset)
            val pressed = pressedZone == i
            val active = icons[i] == "select" && selectActive
            chipFill.color = when {
                active -> (t?.accentColor ?: 0) and 0x00FFFFFF or 0x55000000
                pressed -> 0x44000000 or ((t?.accentColor ?: 0) and 0x00FFFFFF)
                else -> 0x1A000000 or ((t?.keyTextColor ?: 0) and 0x00FFFFFF)
            }
            val cr = chipRect.width() / 2f
            canvas.drawCircle(chipRect.centerX(), chipRect.centerY(), cr, chipFill)
            if (active) {
                // accent ring around the chip while selection mode is on
                stroke.color = t?.accentColor ?: 0
                stroke.strokeWidth = d * 1.4f
                canvas.drawCircle(chipRect.centerX(), chipRect.centerY(),
                    cr - d * 0.7f, stroke)
            }
            iconPaint.color = if (pressed || active) t?.accentColor ?: 0 else t?.keyTextColor ?: 0
            iconPaint.isAntiAlias = true
            IconPainter.draw(canvas, icons[i], chipRect.centerX(), chipRect.centerY(),
                max(chipRect.width(), d * 22f), iconPaint)
        }
    }

    private fun zoneAt(x: Float, y: Float): Int {
        for (i in icons.indices) {
            if (zoneRect(i).contains(x, y)) return i
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedZone = zoneAt(event.x, event.y)
                if (pressedZone >= 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    postInvalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val z = zoneAt(event.x, event.y)
                if (z != pressedZone) {
                    pressedZone = z
                    postInvalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val z = zoneAt(event.x, event.y)
                val idx = pressedZone
                pressedZone = -1
                postInvalidate()
                if (event.actionMasked == MotionEvent.ACTION_UP && z >= 0 && z == idx) {
                    callback?.onEditAction(icons[z])
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
