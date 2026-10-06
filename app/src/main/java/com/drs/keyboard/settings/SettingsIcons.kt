package com.drs.keyboard.settings

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Vector icon painter for the settings app. Paths drawn directly — no font
 * glyph dependence, no asset files, razor sharp at any density. All icons
 * are drawn centered on (cx, cy) inside a box of [size] px.
 */
object SettingsIcons {

    private val path = Path()
    private val tmp = RectF()

    fun draw(canvas: Canvas, which: String, cx: Float, cy: Float, size: Float, paint: Paint) {
        val h = size / 2f
        path.reset()
        val stroke = paint.strokeWidth
        when (which) {
            "keyboard" -> {
                // rounded keyboard body + rows of key dots
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.95f, cy - h * 0.68f, cx + h * 0.95f, cy + h * 0.68f)
                canvas.drawRoundRect(tmp, size * 0.16f, size * 0.16f, paint)
                paint.style = Paint.Style.FILL
                val kw = size * 0.075f
                for (row in 0..2) {
                    val y = cy - h * 0.34f + row * h * 0.36f
                    val count = if (row == 2) 3 else 5
                    val startX = cx - (count - 1) * kw * 1.5f
                    for (i in 0 until count) {
                        canvas.drawCircle(startX + i * kw * 3f, y, kw, paint)
                    }
                }
                // space bar
                tmp.set(cx - h * 0.4f, cy + h * 0.3f, cx + h * 0.4f, cy + h * 0.48f)
                canvas.drawRoundRect(tmp, size * 0.05f, size * 0.05f, paint)
            }
            "globe" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                canvas.drawCircle(cx, cy, h * 0.85f, paint)
                canvas.drawLine(cx - h * 0.85f, cy, cx + h * 0.85f, cy, paint)
                val ellipse = Path()
                ellipse.addOval(cx - h * 0.42f, cy - h * 0.85f,
                    cx + h * 0.42f, cy + h * 0.85f, Path.Direction.CW)
                canvas.drawPath(ellipse, paint)
            }
            "palette" -> {
                // painter's palette: blob with thumb hole + paint dots
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                tmp.set(cx - h * 0.9f, cy - h * 0.8f, cx + h * 0.9f, cy + h * 0.8f)
                path.addArc(tmp, 20f, 300f)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx - h * 0.18f, cy + h * 0.42f, size * 0.075f, paint)
                canvas.drawCircle(cx - h * 0.42f, cy + h * 0.05f, size * 0.075f, paint)
                canvas.drawCircle(cx - h * 0.1f, cy - h * 0.35f, size * 0.075f, paint)
                canvas.drawCircle(cx + h * 0.35f, cy - h * 0.3f, size * 0.075f, paint)
            }
            "gear" -> {
                // gear: center circle + 8 spokes
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                canvas.drawCircle(cx, cy, h * 0.42f, paint)
                val r1 = h * 0.58f
                val r2 = h * 0.85f
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    canvas.drawLine(
                        cx + (r1 * Math.cos(a)).toFloat(), cy + (r1 * Math.sin(a)).toFloat(),
                        cx + (r2 * Math.cos(a)).toFloat(), cy + (r2 * Math.sin(a)).toFloat(), paint)
                }
            }
            "bolt" -> {
                paint.style = Paint.Style.FILL
                path.moveTo(cx + h * 0.25f, cy - h * 0.95f)
                path.lineTo(cx - h * 0.55f, cy + h * 0.12f)
                path.lineTo(cx - h * 0.05f, cy + h * 0.12f)
                path.lineTo(cx - h * 0.25f, cy + h * 0.95f)
                path.lineTo(cx + h * 0.55f, cy - h * 0.18f)
                path.lineTo(cx + h * 0.02f, cy - h * 0.18f)
                path.close()
                canvas.drawPath(path, paint)
            }
            "shield" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                path.moveTo(cx, cy - h * 0.92f)
                path.lineTo(cx + h * 0.75f, cy - h * 0.6f)
                path.lineTo(cx + h * 0.75f, cy + h * 0.08f)
                path.quadTo(cx + h * 0.75f, cy + h * 0.65f, cx, cy + h * 0.95f)
                path.quadTo(cx - h * 0.75f, cy + h * 0.65f, cx - h * 0.75f, cy + h * 0.08f)
                path.lineTo(cx - h * 0.75f, cy - h * 0.6f)
                path.close()
                canvas.drawPath(path, paint)
                // inner check
                path.reset()
                path.moveTo(cx - h * 0.32f, cy - h * 0.05f)
                path.lineTo(cx - h * 0.06f, cy + h * 0.25f)
                path.lineTo(cx + h * 0.38f, cy - h * 0.3f)
                canvas.drawPath(path, paint)
            }
            "check" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.16f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                path.moveTo(cx - h * 0.62f, cy + h * 0.02f)
                path.lineTo(cx - h * 0.14f, cy + h * 0.5f)
                path.lineTo(cx + h * 0.66f, cy - h * 0.5f)
                canvas.drawPath(path, paint)
                paint.strokeCap = Paint.Cap.BUTT
                paint.strokeJoin = Paint.Join.MITER
            }
            "chevron" -> {
                // points toward end side (call site flips scaleX for RTL)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.12f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                path.moveTo(cx - h * 0.28f, cy - h * 0.52f)
                path.lineTo(cx + h * 0.3f, cy)
                path.lineTo(cx - h * 0.28f, cy + h * 0.52f)
                canvas.drawPath(path, paint)
                paint.strokeCap = Paint.Cap.BUTT
                paint.strokeJoin = Paint.Join.MITER
            }
            "spark" -> {
                paint.style = Paint.Style.FILL
                path.moveTo(cx, cy - h)
                path.quadTo(cx + h * 0.16f, cy - h * 0.16f, cx + h, cy)
                path.quadTo(cx + h * 0.16f, cy + h * 0.16f, cx, cy + h)
                path.quadTo(cx - h * 0.16f, cy + h * 0.16f, cx - h, cy)
                path.quadTo(cx - h * 0.16f, cy - h * 0.16f, cx, cy - h)
                path.close()
                canvas.drawPath(path, paint)
            }
            "lock" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.68f, cy - h * 0.1f, cx + h * 0.68f, cy + h * 0.85f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
                // shackle
                val arc = Path()
                arc.addArc(cx - h * 0.4f, cy - h * 0.95f, cx + h * 0.4f, cy + h * 0.05f, 180f, 180f)
                canvas.drawPath(arc, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy + h * 0.38f, size * 0.075f, paint)
            }
            "clip" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.62f, cy - h * 0.78f, cx + h * 0.62f, cy + h * 0.85f)
                canvas.drawRoundRect(tmp, size * 0.12f, size * 0.12f, paint)
                paint.style = Paint.Style.FILL
                tmp.set(cx - h * 0.26f, cy - h * 0.95f, cx + h * 0.26f, cy - h * 0.58f)
                canvas.drawRoundRect(tmp, size * 0.06f, size * 0.06f, paint)
            }
            "emoji" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                canvas.drawCircle(cx, cy, h * 0.85f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx - h * 0.3f, cy - h * 0.22f, size * 0.058f, paint)
                canvas.drawCircle(cx + h * 0.3f, cy - h * 0.22f, size * 0.058f, paint)
                paint.style = Paint.Style.STROKE
                val smile = Path()
                smile.addArc(cx - h * 0.46f, cy - h * 0.16f, cx + h * 0.46f, cy + h * 0.78f, 25f, 130f)
                canvas.drawPath(smile, paint)
            }
            "swipe" -> {
                // curve with arrowhead — gesture typing
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.085f
                paint.strokeCap = Paint.Cap.ROUND
                val curve = Path()
                curve.moveTo(cx - h * 0.8f, cy + h * 0.5f)
                curve.cubicTo(cx - h * 0.2f, cy + h * 0.9f, cx + h * 0.1f, cy - h * 0.7f, cx + h * 0.72f, cy - h * 0.35f)
                canvas.drawPath(curve, paint)
                path.reset()
                path.moveTo(cx + h * 0.32f, cy - h * 0.42f)
                path.lineTo(cx + h * 0.74f, cy - h * 0.36f)
                path.lineTo(cx + h * 0.6f, cy + h * 0.02f)
                canvas.drawPath(path, paint)
                paint.strokeCap = Paint.Cap.BUTT
            }
            "book" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                path.moveTo(cx, cy - h * 0.6f)
                path.quadTo(cx - h * 0.45f, cy - h * 0.85f, cx - h * 0.85f, cy - h * 0.6f)
                path.lineTo(cx - h * 0.85f, cy + h * 0.6f)
                path.quadTo(cx - h * 0.45f, cy + h * 0.35f, cx, cy + h * 0.6f)
                path.quadTo(cx + h * 0.45f, cy + h * 0.35f, cx + h * 0.85f, cy + h * 0.6f)
                path.lineTo(cx + h * 0.85f, cy - h * 0.6f)
                path.quadTo(cx + h * 0.45f, cy - h * 0.85f, cx, cy - h * 0.6f)
                path.lineTo(cx, cy + h * 0.6f)
                canvas.drawPath(path, paint)
            }
            "trash" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                canvas.drawLine(cx - h * 0.75f, cy - h * 0.6f, cx + h * 0.75f, cy - h * 0.6f, paint)
                canvas.drawLine(cx, cy - h * 0.85f, cx, cy - h * 0.6f, paint)
                tmp.set(cx - h * 0.55f, cy - h * 0.6f, cx + h * 0.55f, cy + h * 0.9f)
                canvas.drawRoundRect(tmp, size * 0.08f, size * 0.08f, paint)
                canvas.drawLine(cx - h * 0.16f, cy - h * 0.25f, cx - h * 0.16f, cy + h * 0.5f, paint)
                canvas.drawLine(cx + h * 0.16f, cy - h * 0.25f, cx + h * 0.16f, cy + h * 0.5f, paint)
            }
            "back" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.12f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                path.moveTo(cx + h * 0.32f, cy - h * 0.52f)
                path.lineTo(cx - h * 0.3f, cy)
                path.lineTo(cx + h * 0.32f, cy + h * 0.52f)
                canvas.drawPath(path, paint)
                paint.strokeCap = Paint.Cap.BUTT
                paint.strokeJoin = Paint.Join.MITER
            }
            "plus" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.13f
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(cx - h * 0.5f, cy, cx + h * 0.5f, cy, paint)
                canvas.drawLine(cx, cy - h * 0.5f, cx, cy + h * 0.5f, paint)
                paint.strokeCap = Paint.Cap.BUTT
            }
            "star" -> {
                paint.style = Paint.Style.FILL
                val outer = h * 0.95f
                val inner = h * 0.4f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outer else inner
                    val a = Math.toRadians((i * 36 - 90).toDouble())
                    val x = cx + (r * Math.cos(a)).toFloat()
                    val y = cy + (r * Math.sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            "crown" -> {
                paint.style = Paint.Style.FILL
                path.moveTo(cx - h * 0.85f, cy + h * 0.55f)
                path.lineTo(cx - h * 0.85f, cy - h * 0.45f)
                path.lineTo(cx - h * 0.42f, cy - h * 0.05f)
                path.lineTo(cx, cy - h * 0.75f)
                path.lineTo(cx + h * 0.42f, cy - h * 0.05f)
                path.lineTo(cx + h * 0.85f, cy - h * 0.45f)
                path.lineTo(cx + h * 0.85f, cy + h * 0.55f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
        paint.strokeWidth = stroke
    }
}
