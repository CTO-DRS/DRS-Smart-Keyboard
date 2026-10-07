package com.drs.keyboard.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Vector icon painter for functional keys. Drawing Paths directly avoids any
 * dependence on device symbol-font coverage (⇧ ⌫ ⏎ glyphs are missing on
 * some OEM ROMs), keeps icons razor sharp at any key size, and lets the
 * theme tint everything.
 */
object IconPainter {

    private val path = Path()
    private val tmp = RectF()

    fun draw(canvas: Canvas, which: String, cx: Float, cy: Float, size: Float, paint: Paint) {
        val h = size / 2f
        path.reset()
        when (which) {
            "shift" -> {
                // filled arrow: peak up, stem down
                path.moveTo(cx, cy - h)
                path.lineTo(cx + h * 0.85f, cy + h * 0.05f)
                path.lineTo(cx + h * 0.38f, cy + h * 0.05f)
                path.lineTo(cx + h * 0.38f, cy + h * 0.62f)
                path.lineTo(cx - h * 0.38f, cy + h * 0.62f)
                path.lineTo(cx - h * 0.38f, cy + h * 0.05f)
                path.lineTo(cx - h * 0.85f, cy + h * 0.05f)
                path.close()
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)
            }
            "backspace" -> {
                // left-pointing body with an X
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                tmp.set(cx - h, cy - h * 0.72f, cx + h * 0.95f, cy + h * 0.72f)
                path.moveTo(cx + h * 0.95f, cy - h * 0.72f)
                path.lineTo(cx - h * 0.25f, cy - h * 0.72f)
                path.lineTo(cx - h, cy)
                path.lineTo(cx - h * 0.25f, cy + h * 0.72f)
                path.lineTo(cx + h * 0.95f, cy + h * 0.72f)
                path.close()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.09f
                canvas.drawPath(path, paint)
                val k = size * 0.17f
                canvas.drawLine(cx - h * 0.18f - k, cy - k, cx - h * 0.18f + k, cy + k, paint)
                canvas.drawLine(cx + h * 0.18f - k, cy - k, cx - h * 0.18f - k + 2 * k, cy + k, paint)
                canvas.drawLine(cx + h * 0.18f - k, cy - k, cx + h * 0.18f + k, cy + k, paint)
                canvas.drawLine(cx - h * 0.18f + k, cy - k, cx + h * 0.18f - k, cy + k, paint)
            }
            "enter" -> {
                // return arrow: down then left
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.1f
                path.moveTo(cx + h * 0.8f, cy - h * 0.55f)
                path.lineTo(cx + h * 0.8f, cy + h * 0.15f)
                path.lineTo(cx - h * 0.65f, cy + h * 0.15f)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(cx - h * 0.15f, cy - h * 0.35f)
                path.lineTo(cx - h * 0.62f, cy + h * 0.15f)
                path.lineTo(cx - h * 0.15f, cy + h * 0.65f)
                canvas.drawPath(path, paint)
            }
            "globe" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                canvas.drawCircle(cx, cy, h * 0.82f, paint)
                canvas.drawLine(cx - h * 0.82f, cy, cx + h * 0.82f, cy, paint)
                val ellipse = Path()
                ellipse.addOval(
                    cx - h * 0.4f, cy - h * 0.82f,
                    cx + h * 0.4f, cy + h * 0.82f, Path.Direction.CW
                )
                canvas.drawPath(ellipse, paint)
            }
            "emoji" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.075f
                canvas.drawCircle(cx, cy, h * 0.82f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx - h * 0.3f, cy - h * 0.22f, size * 0.055f, paint)
                canvas.drawCircle(cx + h * 0.3f, cy - h * 0.22f, size * 0.055f, paint)
                paint.style = Paint.Style.STROKE
                val smile = Path()
                smile.addArc(
                    cx - h * 0.45f, cy - h * 0.15f, cx + h * 0.45f, cy + h * 0.75f,
                    25f, 130f
                )
                canvas.drawPath(smile, paint)
            }
            "onehand" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.09f
                // two arrows pointing to center
                canvas.drawLine(cx - h * 0.85f, cy, cx - h * 0.15f, cy, paint)
                path.moveTo(cx - h * 0.5f, cy - h * 0.35f)
                path.lineTo(cx - h * 0.12f, cy)
                path.lineTo(cx - h * 0.5f, cy + h * 0.35f)
                canvas.drawPath(path, paint)
                canvas.drawLine(cx + h * 0.85f, cy, cx + h * 0.15f, cy, paint)
                path.reset()
                path.moveTo(cx + h * 0.5f, cy - h * 0.35f)
                path.lineTo(cx + h * 0.12f, cy)
                path.lineTo(cx + h * 0.5f, cy + h * 0.35f)
                canvas.drawPath(path, paint)
            }
            "kebab" -> {
                paint.style = Paint.Style.FILL
                for (i in -1..1) {
                    canvas.drawCircle(cx + i * size * 0.24f, cy, size * 0.07f, paint)
                }
            }
            "clip" -> {
                // clipboard: body + protruding clip tab + two paper lines
                // (the old open-box read as a battery on dark themes)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                tmp.set(cx - h * 0.55f, cy - h * 0.60f, cx + h * 0.55f, cy + h * 0.82f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
                tmp.set(cx - h * 0.22f, cy - h * 0.92f, cx + h * 0.22f, cy - h * 0.42f)
                canvas.drawRoundRect(tmp, size * 0.05f, size * 0.05f, paint)
                val lineP = Paint(paint)
                lineP.strokeWidth = size * 0.06f
                lineP.alpha = (lineP.alpha * 0.75f).toInt()
                canvas.drawLine(cx - h * 0.28f, cy + h * 0.05f, cx + h * 0.28f, cy + h * 0.05f, lineP)
                canvas.drawLine(cx - h * 0.28f, cy + h * 0.45f, cx + h * 0.08f, cy + h * 0.45f, lineP)
            }
            "copy" -> {
                // two overlapping rounded rects
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.15f, cy - h * 0.85f, cx + h * 0.85f, cy + h * 0.15f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
                tmp.set(cx - h * 0.85f, cy - h * 0.15f, cx + h * 0.15f, cy + h * 0.85f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
            }
            "cut" -> {
                // scissors: two finger rings + crossing blades
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                canvas.drawCircle(cx - h * 0.45f, cy + h * 0.5f, h * 0.26f, paint)
                canvas.drawCircle(cx + h * 0.45f, cy + h * 0.5f, h * 0.26f, paint)
                canvas.drawLine(cx - h * 0.35f, cy + h * 0.3f, cx + h * 0.62f, cy - h * 0.75f, paint)
                canvas.drawLine(cx + h * 0.35f, cy + h * 0.3f, cx - h * 0.62f, cy - h * 0.75f, paint)
            }
            "paste" -> {
                // clipboard with a sheet inside
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.65f, cy - h * 0.8f, cx + h * 0.65f, cy + h * 0.85f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
                canvas.drawLine(cx - h * 0.25f, cy - h * 0.9f, cx + h * 0.25f, cy - h * 0.9f, paint)
                tmp.set(cx - h * 0.35f, cy - h * 0.35f, cx + h * 0.35f, cy + h * 0.5f)
                canvas.drawRoundRect(tmp, size * 0.06f, size * 0.06f, paint)
            }
            "selectall" -> {
                // dashed frame + text lines
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                tmp.set(cx - h * 0.85f, cy - h * 0.8f, cx + h * 0.85f, cy + h * 0.8f)
                canvas.drawRoundRect(tmp, size * 0.1f, size * 0.1f, paint)
                canvas.drawLine(cx - h * 0.5f, cy - h * 0.3f, cx + h * 0.5f, cy - h * 0.3f, paint)
                canvas.drawLine(cx - h * 0.5f, cy + h * 0.1f, cx + h * 0.35f, cy + h * 0.1f, paint)
                canvas.drawLine(cx - h * 0.5f, cy + h * 0.5f, cx + h * 0.15f, cy + h * 0.5f, paint)
            }
            "select" -> {
                // text-selection handles: two brackets with a highlighted middle
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.09f
                paint.strokeCap = Paint.Cap.ROUND
                val bw = h * 0.22f
                canvas.drawLine(cx - h * 0.55f, cy - h * 0.6f, cx - h * 0.55f, cy + h * 0.6f, paint)
                canvas.drawLine(cx - h * 0.55f, cy - h * 0.6f, cx - h * 0.55f + bw, cy - h * 0.6f, paint)
                canvas.drawLine(cx - h * 0.55f, cy + h * 0.6f, cx - h * 0.55f + bw, cy + h * 0.6f, paint)
                canvas.drawLine(cx + h * 0.55f, cy - h * 0.6f, cx + h * 0.55f, cy + h * 0.6f, paint)
                canvas.drawLine(cx + h * 0.55f - bw, cy - h * 0.6f, cx + h * 0.55f, cy - h * 0.6f, paint)
                canvas.drawLine(cx + h * 0.55f - bw, cy + h * 0.6f, cx + h * 0.55f, cy + h * 0.6f, paint)
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                canvas.drawCircle(cx - h * 0.05f, cy, h * 0.12f, dot)
                canvas.drawCircle(cx + h * 0.18f, cy, h * 0.12f, dot)
                canvas.drawCircle(cx + h * 0.41f, cy, h * 0.12f, dot)
            }
            "undo" -> {
                // curved return arrow: tail sweeps from the right, head points left
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.09f
                paint.strokeCap = Paint.Cap.ROUND
                path.reset()
                path.moveTo(cx + h * 0.72f, cy + h * 0.5f)
                path.quadTo(cx + h * 0.72f, cy - h * 0.58f, cx - h * 0.3f, cy - h * 0.52f)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx - h * 0.78f, cy - h * 0.52f)
                path.lineTo(cx - h * 0.18f, cy - h * 0.86f)
                path.lineTo(cx - h * 0.18f, cy - h * 0.18f)
                path.close()
                canvas.drawPath(path, paint)
            }
            "clear" -> {
                // trash can: lid + handle + ribbed body
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.08f
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(cx - h * 0.68f, cy - h * 0.5f, cx + h * 0.68f, cy - h * 0.5f, paint)
                canvas.drawLine(cx - h * 0.2f, cy - h * 0.5f, cx - h * 0.2f, cy - h * 0.8f, paint)
                canvas.drawLine(cx + h * 0.2f, cy - h * 0.5f, cx + h * 0.2f, cy - h * 0.8f, paint)
                tmp.set(cx - h * 0.48f, cy - h * 0.45f, cx + h * 0.48f, cy + h * 0.85f)
                canvas.drawRoundRect(tmp, size * 0.08f, size * 0.08f, paint)
                canvas.drawLine(cx - h * 0.16f, cy - h * 0.1f, cx - h * 0.16f, cy + h * 0.5f, paint)
                canvas.drawLine(cx + h * 0.16f, cy - h * 0.1f, cx + h * 0.16f, cy + h * 0.5f, paint)
            }
            "close" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size * 0.1f
                paint.strokeCap = Paint.Cap.ROUND
                val k = h * 0.62f
                canvas.drawLine(cx - k, cy - k, cx + k, cy + k, paint)
                canvas.drawLine(cx + k, cy - k, cx - k, cy + k, paint)
            }
        }
    }

    fun iconFor(key: com.drs.keyboard.keyboard.KeyDef): String? =
        when (key.type) {
            com.drs.keyboard.keyboard.KeyDef.KeyType.SHIFT -> "shift"
            com.drs.keyboard.keyboard.KeyDef.KeyType.BACKSPACE -> "backspace"
            com.drs.keyboard.keyboard.KeyDef.KeyType.ENTER -> "enter"
            com.drs.keyboard.keyboard.KeyDef.KeyType.LANG -> "globe"
            com.drs.keyboard.keyboard.KeyDef.KeyType.EMOJI -> "emoji"
            else -> null
        }
}
