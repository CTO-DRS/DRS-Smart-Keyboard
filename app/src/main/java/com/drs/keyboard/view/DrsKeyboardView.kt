package com.drs.keyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.drs.keyboard.engine.SwipeDecoder
import com.drs.keyboard.keyboard.KeyDef
import com.drs.keyboard.theme.KeyboardTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The keyboard surface. Renders the active layout with the glass theme and
 * owns the full touch pipeline:
 *
 *   tap            → onKey(key)
 *   slide          → pressed key follows the finger, commit on release
 *   long-press     → alt-character popup above the key (drag to pick)
 *   backspace hold → repeat delete; drag left → delete whole word
 *   space drag     → cursor moves (tap still inserts a space)
 *   smooth curve   → swipe decoding (SwipeDecoder), trail drawn live
 */
class DrsKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Callback {
        fun onKey(key: KeyDef)
        fun onText(text: String)
        fun onSwipeWord(word: String)
        fun onCursorMove(delta: Int)
        fun onDeleteWord()
        fun onSpaceDragFinished()
        fun onSpaceLongPress()
    }

    private interface KeyRect {
        val key: KeyDef
        val rect: RectF
    }

    private class KR(override val key: KeyDef, override val rect: RectF) : KeyRect

    var callback: Callback? = null
    var swipeDecoder: SwipeDecoder? = null
    var swipeEnabled = true
    var cursorControlEnabled = true
    var rtl = false
    /** Delay before long-press fires (alt popup / caps lock / mode cycle). */
    var longPressDelayMs: Long = 380
    /** Enlarged preview bubble above the key currently being touched. */
    var keyPopupEnabled = true
    /** Multiplier applied over the theme key height (compact 0.85 / normal 1 / tall 1.15). */
    private var keyHeightScale = 1f
    /** Multiplier for key label text (0.85 small / 1 normal / 1.2 large). */
    var labelScale = 1f
        set(v) { field = v; invalidate() }

    private var theme: KeyboardTheme? = null
    private var rows: List<List<KeyDef>> = emptyList()
    private var keyRects: List<KR> = emptyList()
    private var keyHeightPx = 0f
    private var keyW = 0f

    // paints
    private val keyFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keySheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowRing = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trailHead = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val trailGlow = Paint(trailPaint)
    private val popupFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupAltText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // cached per-color vertical gradients (theme-keyed, cleared on setTheme)
    private val fillShaders = HashMap<Long, LinearGradient>(8)
    private var sheenShader: LinearGradient? = null

    // touch state
    private enum class Mode { IDLE, TAP, ALT, REPEAT, GESTURE }
    private var mode = Mode.IDLE
    private var pressed: KR? = null
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var pointerId = -1
    private var spaceDragMoved = false
    private var backspaceWordDeleted = false

    // gesture
    private val gesturePath = ArrayList<SwipeDecoder.Point>(64)

    // long press
    private val handler = Handler(Looper.getMainLooper())
    private var longPressedFired = false
    private var repeatFires = 0

    // alt popup
    private var popupKey: KR? = null
    private var popupIndex = -1
    private var popupRect: RectF? = null
    private var popupAlts: List<Int> = emptyList()

    // press-preview bubble state (derived — no extra lifecycle)
    private var shiftedVisual = false
    private val bubbleRect = RectF()

    private val longPressRunnable = object : Runnable {
        override fun run() {
            val k = pressed ?: return
            longPressedFired = true
            when {
                k.key.type == KeyDef.KeyType.BACKSPACE -> {
                    mode = Mode.REPEAT
                    repeatFires = 0
                    fireBackspace()
                }
                k.key.type == KeyDef.KeyType.SPACE -> {
                    // stationary long-press on space = display-mode cycle
                    mode = Mode.IDLE
                    pressed = null
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    callback?.onSpaceLongPress()
                    invalidate()
                }
                k.key.type == KeyDef.KeyType.SHIFT -> {
                    // long-press shift = caps lock: emulate double tap
                    callback?.onKey(k.key)
                    callback?.onKey(k.key)
                    mode = Mode.IDLE
                    pressed = null
                    invalidate()
                }
                k.key.type == KeyDef.KeyType.NAV &&
                    (k.key.code == KeyDef.CODE_NAV_LEFT || k.key.code == KeyDef.CODE_NAV_RIGHT) -> {
                    // long-press ← / → = jump a whole word (extends selection
                    // when selection mode is on — the service adds shift)
                    mode = Mode.IDLE
                    pressed = null
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    callback?.onKey(
                        KeyDef.fn("",
                            if (k.key.code == KeyDef.CODE_NAV_LEFT) KeyDef.CODE_NAV_WORD_LEFT
                            else KeyDef.CODE_NAV_WORD_RIGHT,
                            KeyDef.KeyType.NAV, 1f)
                    )
                    invalidate()
                }
                k.key.alts.isNotEmpty() -> {
                    mode = Mode.ALT
                    popupKey = k
                    popupAlts = k.key.alts
                    popupIndex = -1
                    computePopupRect()
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    invalidate()
                }
                else -> {
                    mode = Mode.IDLE
                }
            }
        }
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (mode != Mode.REPEAT) return
            fireBackspace()
            repeatFires++
            val delay = if (repeatFires < 12) 55L else 25L
            handler.postDelayed(this, delay)
        }
    }

    private fun fireBackspace() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        callback?.onKey(pressed?.key ?: KeyDef.fn("", KeyDef.CODE_BACKSPACE, KeyDef.KeyType.BACKSPACE, 1f))
    }

    /** Small label drawn centered on the space bar (language hint). */
    var spaceLabel: String = ""
        set(v) { field = v; invalidate() }

    // ------------------------------------------------------------------
    // Layout / theme
    // ------------------------------------------------------------------

    fun setTheme(t: KeyboardTheme) {
        theme = t
        val d = resources.displayMetrics.density
        keyHeightPx = t.keyHeightDp * d * keyHeightScale
        keyFill.color = t.keyColor
        keyStroke.color = t.strokeColor
        keyStroke.style = Paint.Style.STROKE
        keyStroke.strokeWidth = d * 1f
        labelText.color = t.keyTextColor
        labelText.isAntiAlias = true
        labelText.textAlign = Paint.Align.CENTER
        labelText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        smallText.color = t.keyTextColor
        smallText.isAntiAlias = true
        smallText.textAlign = Paint.Align.CENTER
        smallText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        pressFill.color = t.pressColor
        accentStroke.color = t.accentColor
        accentStroke.style = Paint.Style.STROKE
        accentStroke.strokeWidth = d * 1.6f
        // soft depth shadow below each key
        keyShadow.shader = LinearGradient(
            0f, 0f, 0f, d * 4f,
            0x3D000000, 0x00000000, Shader.TileMode.CLAMP
        )
        keyShadow.style = Paint.Style.FILL
        // top sheen on each key (glass)
        sheenShader = LinearGradient(
            0f, 0f, 0f, keyHeightPx * 0.62f,
            0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        keySheen.style = Paint.Style.FILL
        keySheen.shader = sheenShader
        // pressed glow ring
        glowRing.style = Paint.Style.STROKE
        glowRing.color = t.accentColor
        glowRing.alpha = 160
        glowRing.strokeWidth = d * 2.2f
        trailHead.style = Paint.Style.FILL
        trailHead.color = t.accentColor
        trailPaint.color = t.accentColor
        trailPaint.strokeWidth = d * 7f
        trailGlow.color = t.accentColor
        trailGlow.alpha = 60
        trailGlow.strokeWidth = d * 16f
        popupFill.color = t.bgColor
        popupFill.style = Paint.Style.FILL
        popupShadow.style = Paint.Style.FILL
        popupShadow.color = 0x59000000
        popupAltText.color = t.keyTextColor
        popupAltText.isAntiAlias = true
        popupAltText.textAlign = Paint.Align.CENTER
        popupAltText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        fillShaders.clear()
        invalidate()
    }

    fun setRows(newRows: List<List<KeyDef>>) {
        rows = newRows
        computeGeometry()
        requestLayout()
        invalidate()
    }

    /** Applies a height multiplier over the theme's key height and re-flows the layout. */
    fun applyKeyHeight(scale: Float) {
        keyHeightScale = scale
        theme?.let { setTheme(it) }
        computeGeometry()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val d = resources.displayMetrics.density
        val pad = (d * 5).toInt()
        val h = (rows.size * keyHeightPx + pad * 2 + (rows.size * d * 3)).toInt()
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthSpec), h)
    }

    private fun computeGeometry() {
        keyRects = emptyList()
        if (width == 0 || rows.isEmpty()) return
        val d = resources.displayMetrics.density
        val gap = d * 3f
        val rowH = keyHeightPx + d * 3f
        val pad = d * 5f
        val list = ArrayList<KR>(48)
        for ((rowIdx, row) in rows.withIndex()) {
            val totalUnits = row.sumOf { it.widthUnits.toDouble() }.toFloat()
            val unitPx = (width - pad * 2 - gap * (row.size - 1)) / totalUnits
            var x = pad
            // arrows stay in absolute LTR order even on the RTL Arabic layout
            val isNavRow = row.any { it.type == KeyDef.KeyType.NAV }
            val ordered = if (rtl && !isNavRow) row.reversed() else row
            for (k in ordered) {
                val kw = unitPx * k.widthUnits
                val rect = RectF(x, pad + rowIdx * rowH, x + kw, pad + rowIdx * rowH + keyHeightPx)
                list.add(KR(k, rect))
                x += kw + gap
            }
        }
        keyRects = list
        keyW = if (width > 0) width / 10f else 60f
        feedSwipeDecoder()
    }

    /** Feeds letter key centers to the swipe decoder for the current layout. */
    fun feedSwipeDecoder() {
        val dec = swipeDecoder ?: return
        val centers = HashMap<Char, Pair<Float, Float>>(36)
        for (kr in keyRects) {
            if (kr.key.type == KeyDef.KeyType.CHAR && kr.key.code > 0 &&
                kr.key.label.length == 1 && kr.key.label[0].isLetter()
            ) {
                centers[kr.key.label[0].lowercaseChar()] =
                    kr.rect.centerX() to kr.rect.centerY()
            }
        }
        dec.keyCenters = centers
        dec.keyRadius = if (keyRects.isNotEmpty()) max(
            keyRects[0].rect.width(),
            keyHeightPx
        ) / 2f else 60f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeGeometry()
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val radius = (theme?.cornerRadiusDp ?: 14) * d * 0.55f

        // pass 1: depth shadows below keys
        for (kr in keyRects) {
            val r = kr.rect
            scratch.set(r.left + d * 3f, r.bottom - d * 0.5f,
                r.right - d * 3f, r.bottom + d * 4f)
            canvas.drawRoundRect(scratch, radius * 0.8f, radius * 0.8f, keyShadow)
        }

        // pass 2: key bodies
        for (kr in keyRects) {
            val r = kr.rect
            val isPressed = pressed === kr && (mode == Mode.TAP || mode == Mode.ALT || mode == Mode.REPEAT)
            val functional = kr.key.isFunctional
            val active = kr.key.type == KeyDef.KeyType.SHIFT &&
                    (shiftIndicator || capsIndicator)
            val space = kr.key.type == KeyDef.KeyType.SPACE

            val base = when {
                space -> blend(theme?.keyColor ?: 0, theme?.accentColor ?: 0, 0.20f)
                active -> blend(theme?.keyColor ?: 0, theme?.accentColor ?: 0, 0.35f)
                functional -> theme?.specialKeyColor ?: 0
                else -> theme?.keyColor ?: 0
            }

            // fill: vertical gradient (lighter top → base bottom) for glass depth
            keyFill.shader = fillShaderFor(base, r.height())
            if (isPressed) {
                keyFill.shader = fillShaderFor(blend(base, theme?.accentColor ?: 0, 0.30f), r.height())
            }
            canvas.drawRoundRect(r, radius, radius, keyFill)
            keyFill.shader = null

            // glass sheen on the key's top half
            keySheen.shader = sheenShader
            canvas.drawRoundRect(r, radius, radius, keySheen)

            // stroke / active accent ring / pressed glow ring
            if (isPressed) {
                canvas.drawRoundRect(r, radius, radius, glowRing)
            } else if (active) {
                canvas.drawRoundRect(r, radius, radius, accentStroke)
            } else {
                canvas.drawRoundRect(r, radius, radius, keyStroke)
            }

            // content
            val icon = IconPainter.iconFor(kr.key)
            if (icon != null) {
                labelText.color = theme?.keyTextColor ?: 0
                val p = labelText
                p.style = if (icon == "shift" && (shiftIndicator || capsIndicator)) {
                    Paint.Style.FILL_AND_STROKE
                } else {
                    Paint.Style.FILL
                }
                IconPainter.draw(canvas, icon, r.centerX(), r.centerY(),
                    min(r.width(), r.height()) * 0.42f, p)
                p.style = Paint.Style.FILL
            } else {
                val label = if (kr.key.type == KeyDef.KeyType.SHIFT) "" else kr.key.label
                if (label.isNotEmpty()) {
                    val size = when {
                        kr.key.labelSize > 0 -> kr.key.labelSize * d * labelScale
                        space -> d * 12f * labelScale
                        label.length > 2 -> d * 13f * labelScale
                        else -> d * 19f * labelScale
                    }
                    if (size > 0) {
                        labelText.textSize = size
                        labelText.color = theme?.keyTextColor ?: 0
                        labelText.alpha = if (space) 115 else 255
                        val ty = r.centerY() - (labelText.ascent() + labelText.descent()) / 2f
                        val lx = if (space && spaceLabel.isNotEmpty()) spaceLabel else label
                        canvas.drawText(lx, r.centerX(), ty, labelText)
                        labelText.alpha = 255
                    }
                }
                // shifted alt letter shown small at top-right
                if (!functional && kr.key.shiftLabel != null && kr.key.shiftLabel != kr.key.label) {
                    smallText.textSize = d * 11f * labelScale
                    smallText.color = theme?.keyTextColor ?: 0
                    smallText.alpha = 150
                    canvas.drawText(kr.key.shiftLabel!!, r.right - d * 8f, r.top + d * 13f, smallText)
                    smallText.alpha = 255
                }
            }
        }

        // swipe trail: fading gradient ribbon + glowing head
        if (mode == Mode.GESTURE && gesturePath.size > 1) {
            drawTrail(canvas, d)
        }

        // alt popup + press-preview bubble
        drawPopup(canvas, d)
        drawPressBubble(canvas, d)
    }

    private val scratch = RectF()

    /**
     * Gboard-style touch preview: while a character key is held, an enlarged
     * glass bubble floats above it showing the character that would be typed
     * (shift-aware). Follows the finger across keys; hidden for functional
     * keys, space, and whenever the alt-char popup is up.
     */
    private fun drawPressBubble(canvas: Canvas, d: Float) {
        if (!keyPopupEnabled) return
        if (mode != Mode.TAP) return
        val k = pressed ?: return
        if (k.key.type != KeyDef.KeyType.CHAR) return
        val label = k.key.label
        if (label.isEmpty() || label == " " || label.length > 3) return
        val shown = if (shiftedVisual && k.key.shiftLabel != null) k.key.shiftLabel!! else label
        if (bubbleRect.isEmpty) {
            val w = d * 42f
            val h = d * 46f
            var left = k.rect.centerX() - w / 2f
            left = left.coerceIn(d * 2f, width - w - d * 2f)
            var top = k.rect.top - h - d * 7f
            if (top < d * 2f) top = k.rect.bottom + d * 7f
            bubbleRect.set(left, top, left + w, top + h)
        }
        val radius = 12 * d
        popupShadow.alpha = 66
        canvas.drawRoundRect(
            bubbleRect.left + d * 2f, bubbleRect.top + d * 4f,
            bubbleRect.right + d * 2f, bubbleRect.bottom + d * 4f, radius, radius, popupShadow)
        popupFill.alpha = ((theme?.bgColor?.ushr(24) ?: 242) * 0.985f).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(bubbleRect, radius, radius, popupFill)
        val sheen = LinearGradient(0f, bubbleRect.top, 0f, bubbleRect.height() * 0.6f,
            0x24FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        popupFill.shader = sheen
        canvas.drawRoundRect(bubbleRect, radius, radius, popupFill)
        popupFill.shader = null
        canvas.drawRoundRect(bubbleRect, radius, radius, accentStroke)
        popupAltText.textSize = d * 24f
        popupAltText.color = theme?.keyTextColor ?: 0
        val ty = bubbleRect.centerY() - (popupAltText.ascent() + popupAltText.descent()) / 2f
        canvas.drawText(shown, bubbleRect.centerX(), ty, popupAltText)
        bubbleRect.setEmpty()
    }

    /** Cached vertical gradient: lighten(base, .16) at top → base at bottom. */
    private fun fillShaderFor(base: Int, height: Float): LinearGradient {
        val key = ((base.toLong() and 0xFFFFFFFFL) shl 16) or (height.toInt().toLong() and 0xFFFFL)
        fillShaders[key]?.let { return it }
        val top = blend(base, 0xFFFFFFFF.toInt(), 0.16f) // white
        val g = LinearGradient(0f, 0f, 0f, height, top, base, Shader.TileMode.CLAMP)
        fillShaders[key] = g
        return g
    }

    /** Swipe ribbon: recent segments brightest, older fade out; glowing head dot. */
    private fun drawTrail(canvas: Canvas, d: Float) {
        val pts = gesturePath
        val n = pts.size
        if (n < 2) return
        // glow underlay
        path.reset()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until n) path.lineTo(pts[i].x, pts[i].y)
        canvas.drawPath(path, trailGlow)
        // fading ribbon: draw in chunks of 6 points, alpha ramps by age
        val chunk = 6
        var i = 0
        while (i < n - 1) {
            val end = min(i + chunk, n - 1)
            path.reset()
            path.moveTo(pts[i].x, pts[i].y)
            for (j in i + 1..end) path.lineTo(pts[j].x, pts[j].y)
            val age = i.toFloat() / (n - 1).coerceAtLeast(1)
            trailPaint.alpha = (60 + (1f - age) * 195).toInt().coerceIn(0, 255)
            canvas.drawPath(path, trailPaint)
            i = end
        }
        trailPaint.alpha = 255
        // glowing head
        val head = pts[n - 1]
        trailHead.alpha = 70
        canvas.drawCircle(head.x, head.y, d * 9f, trailHead)
        trailHead.alpha = 220
        canvas.drawCircle(head.x, head.y, d * 4.2f, trailHead)
    }

    private fun drawPopup(canvas: Canvas, d: Float) {
        val pk = popupKey ?: return
        val rect = popupRect ?: return
        if (popupAlts.isEmpty()) return
        val radius = 12 * d
        // floating depth shadow
        popupShadow.alpha = 66
        canvas.drawRoundRect(
            rect.left + d * 2f, rect.top + d * 4f,
            rect.right + d * 2f, rect.bottom + d * 4f, radius, radius, popupShadow)
        popupFill.alpha = ((theme?.bgColor?.ushr(24) ?: 242) * 0.985f).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, radius, radius, popupFill)
        // glass sheen on popup top
        val sheen = LinearGradient(0f, rect.top, 0f, rect.top + rect.height() * 0.55f,
            0x24FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        popupFill.shader = sheen
        canvas.drawRoundRect(rect, radius, radius, popupFill)
        popupFill.shader = null
        canvas.drawRoundRect(rect, radius, radius, accentStroke)
        popupAltText.textSize = d * 20f
        popupAltText.color = theme?.keyTextColor ?: 0
        val count = popupAlts.size
        val cell = rect.width() / count
        for (i in 0 until count) {
            val ch = String(Character.toChars(popupAlts[i]))
            val cx = rect.left + cell * i + cell / 2f
            if (i == popupIndex) {
                val pr = RectF(rect.left + cell * i + 2, rect.top + 2,
                    rect.left + cell * (i + 1) - 2, rect.bottom - 2)
                pressFill.color = theme?.pressColor ?: 0
                canvas.drawRoundRect(pr, radius * 0.6f, radius * 0.6f, pressFill)
                // selected glyph pops in accent
                popupAltText.color = theme?.accentColor ?: 0
            } else {
                popupAltText.color = theme?.keyTextColor ?: 0
            }
            val ty = rect.centerY() - (popupAltText.ascent() + popupAltText.descent()) / 2f
            canvas.drawText(ch, cx, ty, popupAltText)
        }
    }

    private var shiftIndicator = false
    private var capsIndicator = false

    fun setShiftVisuals(shifted: Boolean, capsLocked: Boolean) {
        shiftIndicator = shifted
        capsIndicator = capsLocked
        shiftedVisual = shifted
        invalidate()
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

    // ------------------------------------------------------------------
    // Touch
    // ------------------------------------------------------------------

    private fun hitTest(x: Float, y: Float): KR? {
        // allow slight vertical slop
        for (kr in keyRects) {
            if (kr.rect.contains(x, y)) return kr
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId = event.getPointerId(0)
                downX = event.x; downY = event.y
                lastX = downX; lastY = downY
                mode = Mode.TAP
                pressed = hitTest(downX, downY)
                spaceDragMoved = false
                backspaceWordDeleted = false
                longPressedFired = false
                gesturePath.clear()
                gesturePath.add(SwipeDecoder.Point(downX, downY, event.eventTime))
                if (pressed != null) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    handler.postDelayed(longPressRunnable, longPressDelayMs)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.findPointerIndex(pointerId) < 0) return true
                val x = event.x; val y = event.y
                lastX = x; lastY = y
                when (mode) {
                    Mode.TAP -> handleTapMove(x, y)
                    Mode.GESTURE -> gesturePath.add(SwipeDecoder.Point(x, y, event.eventTime))
                    Mode.ALT -> handleAltMove(x, y)
                    Mode.REPEAT -> {
                        // dragging left from backspace deletes the whole word
                        if (!backspaceWordDeleted && downX - x > keyW * 1.4f) {
                            handler.removeCallbacks(repeatRunnable)
                            backspaceWordDeleted = true
                            mode = Mode.IDLE
                            callback?.onDeleteWord()
                        }
                    }
                    else -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(repeatRunnable)
                finishTouch(event.actionMasked == MotionEvent.ACTION_UP)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTapMove(x: Float, y: Float) {
        val k = pressed ?: return

        // gestures launch from letter keys only (space = cursor, backspace = word delete)
        if (k.key.type == KeyDef.KeyType.CHAR) {
            startGestureIfNeeded(x, y, 0)
            if (mode == Mode.GESTURE) return
        }

        // space: cursor control
        if (k.key.type == KeyDef.KeyType.SPACE && cursorControlEnabled) {
            val dx = x - downX
            val steps = (dx / (keyW * 0.55f)).toInt()
            if (steps != 0) {
                spaceDragMoved = true
                handler.removeCallbacks(longPressRunnable)
                pressed = null
                callback?.onCursorMove(steps)
                // rebase so continuous drags step repeatedly
                downX += steps * keyW * 0.55f
                return
            }
        }
        // slide across keys: move the pressed key with the finger
        val hit = hitTest(x, y)
        if (hit !== k) {
            if (hit != null) {
                pressed = hit
                handler.removeCallbacks(longPressRunnable)
                if (hit.key.alts.isNotEmpty() || hit.key.type == KeyDef.KeyType.BACKSPACE) {
                    handler.postDelayed(longPressRunnable, longPressDelayMs)
                }
            } else {
                pressed = null
                handler.removeCallbacks(longPressRunnable)
            }
        }
    }

    private fun startGestureIfNeeded(x: Float, y: Float, time: Long) {
        if (!swipeEnabled) return
        val dx = abs(x - downX)
        val dy = abs(y - downY)
        if (dx > keyW * 2.1f || (dx > keyW * 1.2f && dy > keyHeightPx * 1.2f)) {
            mode = Mode.GESTURE
            pressed = null
            handler.removeCallbacks(longPressRunnable)
        }
    }

    private fun handleAltMove(x: Float, y: Float) {
        val rect = popupRect ?: return
        popupIndex = if (rect.contains(x, y)) {
            val cell = rect.width() / popupAlts.size
            ((x - rect.left) / cell).toInt().coerceIn(0, popupAlts.size - 1)
        } else -1
    }

    private fun finishTouch(commit: Boolean) {
        val k = pressed
        when (mode) {
            Mode.TAP -> {
                if (commit && k != null && !spaceDragMoved) {
                    callback?.onKey(k.key)
                }
                if (spaceDragMoved) callback?.onSpaceDragFinished()
            }
            Mode.GESTURE -> {
                if (commit) {
                    val word = swipeDecoder?.decode(gesturePath, 1)?.firstOrNull()
                    if (word != null) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        callback?.onSwipeWord(word)
                    }
                }
            }
            Mode.ALT -> {
                val pk = popupKey
                if (commit && pk != null && popupIndex >= 0 && popupIndex < popupAlts.size) {
                    val code = popupAlts[popupIndex]
                    val text = String(Character.toChars(code))
                    callback?.onText(text)
                }
            }
            Mode.REPEAT -> { /* repeats already applied */ }
            else -> {}
        }
        mode = Mode.IDLE
        pressed = null
        popupKey = null
        popupRect = null
        popupIndex = -1
        gesturePath.clear()
    }

    private fun computePopupRect() {
        val pk = popupKey ?: return
        val d = resources.displayMetrics.density
        val count = pk.key.alts.size.coerceAtLeast(1)
        val cell = d * 34f
        val w = cell * count + d * 8f
        val h = d * 44f
        var left = pk.rect.centerX() - w / 2f
        left = left.coerceIn(d * 2f, width - w - d * 2f)
        var top = pk.rect.top - h - d * 6f
        if (top < d * 2f) top = pk.rect.bottom + d * 6f
        popupRect = RectF(left, top, left + w, top + h)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    /** Entry point for the host to force-clear transient visuals. */
    fun resetTouchVisuals() {
        mode = Mode.IDLE
        pressed = null
        popupKey = null
        popupRect = null
        bubbleRect.setEmpty()
        gesturePath.clear()
        invalidate()
    }

    // helper used by host to detect gesture start during move (kept public for tests)
    fun maybeGesture(x: Float, y: Float, time: Long) = startGestureIfNeeded(x, y, time)
}
