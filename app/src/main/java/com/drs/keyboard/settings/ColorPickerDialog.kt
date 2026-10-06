package com.drs.keyboard.settings

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/**
 * Compact HSV + alpha + hex color picker dialog. Two custom views:
 * a saturation/value square and a hue bar, plus an alpha slider.
 */
class ColorPickerDialog(
    context: Context,
    initial: Int,
    private val onPicked: (Int) -> Unit
) : Dialog(context) {

    private var hue: Float = 0f
    private var sat: Float = 0f
    private var value: Float = 0f
    private var alphaVal: Int = 255
    private lateinit var hexField: EditText
    private lateinit var svView: SvSquare
    private lateinit var hueView: HueBar
    private lateinit var alphaView: AlphaBar
    private lateinit var preview: View

    init {
        val hsv = FloatArray(3)
        Color.RGBToHSV(
            (initial shr 16) and 0xFF, (initial shr 8) and 0xFF, initial and 0xFF, hsv
        )
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
        alphaVal = (initial ushr 24) and 0xFF
        buildUi()
    }

    private fun currentColor(): Int {
        val rgb = Color.HSVToColor(floatArrayOf(hue, sat, value))
        return (alphaVal shl 24) or (rgb and 0xFFFFFF)
    }

    private fun buildUi() {
        val c = context
        val d = UiKit.dp(c, 16)
        val p = UiKit.palette(c)
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d, d, d, d)
            setBackgroundColor(p.bg)
        }
        val title = TextView(c).apply {
            text = c.getString(com.drs.keyboard.R.string.picker_title)
            textSize = 16f
            setTextColor(p.text)
        }
        root.addView(title)

        svView = SvSquare(c)
        root.addView(svView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 150)).apply {
            topMargin = UiKit.dp(c, 10)
        })
        hueView = HueBar(c)
        root.addView(hueView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 28)).apply {
            topMargin = UiKit.dp(c, 10)
        })
        alphaView = AlphaBar(c)
        root.addView(alphaView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(c, 28)).apply {
            topMargin = UiKit.dp(c, 8)
        })

        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = UiKit.dp(c, 10)
            setPadding(0, pad, 0, 0)
        }
        preview = View(c).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(c, 40), UiKit.dp(c, 40))
        }
        row.addView(preview)
        hexField = EditText(c).apply {
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = UiKit.dp(c, 10)
            }
            textSize = 14f
            setTextColor(p.text)
            setSingleLine()
        }
        row.addView(hexField)
        root.addView(row)

        val buttons = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val pad = UiKit.dp(c, 12)
            setPadding(0, pad, 0, 0)
        }
        buttons.addView(UiKit.button(c, p, c.getString(com.drs.keyboard.R.string.picker_cancel), false) {
            dismiss()
        })
        buttons.addView(UiKit.button(c, p, c.getString(com.drs.keyboard.R.string.picker_ok), true) {
            val color = currentColor()
            onPicked(color)
            dismiss()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = UiKit.dp(c, 10)
            }
        })
        root.addView(buttons)

        setContentView(root)
        refreshPreview()
    }

    private fun refreshPreview() {
        val color = currentColor()
        preview.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke(2, 0x66888888)
        }
        hexField.setText(String.format("#%08X", color))
        svView.invalidate()
        hueView.invalidate()
        alphaView.invalidate()
    }

    private inner class SvSquare(context: Context) : View(context) {
        private val paint = android.graphics.Paint(PaintFlags)

        override fun onDraw(canvas: android.graphics.Canvas) {
            val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                Color.WHITE, hueColor, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            // cursor
            paint.shader = null
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = if (value > 0.5f) Color.BLACK else Color.WHITE
            val cx = sat * width
            val cy = (1 - value) * height
            canvas.drawCircle(cx, cy, 10f, paint)
            paint.style = android.graphics.Paint.Style.FILL
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    sat = (event.x / width).coerceIn(0f, 1f)
                    value = (1 - event.y / height).coerceIn(0f, 1f)
                    refreshPreview()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    private inner class HueBar(context: Context) : View(context) {
        private val paint = android.graphics.Paint(PaintFlags)

        override fun onDraw(canvas: android.graphics.Canvas) {
            val sweep = floatArrayOf(
                0f, 60f, 120f, 180f, 240f, 300f, 360f
            )
            val colors = intArrayOf(
                Color.HSVToColor(floatArrayOf(0f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(60f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
            )
            paint.shader = SweepGradient(width / 2f, height / 2f, colors, sweep)
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(),
                height / 2f, height / 2f, paint)
            paint.shader = null
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            val cx = (hue / 360f) * width
            canvas.drawCircle(cx, height / 2f, height / 2.4f, paint)
            paint.style = android.graphics.Paint.Style.FILL
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    hue = (event.x / width * 360f).coerceIn(0f, 359.99f)
                    refreshPreview()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    private inner class AlphaBar(context: Context) : View(context) {
        private val paint = android.graphics.Paint(PaintFlags)

        override fun onDraw(canvas: android.graphics.Canvas) {
            val rgb = Color.HSVToColor(floatArrayOf(hue, sat, value))
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                (rgb and 0xFFFFFF), rgb, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(),
                height / 2f, height / 2f, paint)
            paint.shader = null
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            val cx = (alphaVal / 255f) * width
            canvas.drawCircle(cx, height / 2f, height / 2.4f, paint)
            paint.style = android.graphics.Paint.Style.FILL
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    alphaVal = ((event.x / width) * 255f).toInt().coerceIn(0, 255)
                    refreshPreview()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    companion object {
        // shared paint flags
        const val PaintFlags = android.graphics.Paint.ANTI_ALIAS_FLAG
    }
}
