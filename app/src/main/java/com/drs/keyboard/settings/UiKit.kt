package com.drs.keyboard.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * Glass Depth 2.0 design system for the settings app. Zero dependencies —
 * layered gradient cards, painted vector icon tiles, ripples, pills and
 * badges. Light + dark, full RTL support.
 */
object UiKit {

    data class Palette(
        val bg: Int, val card: Int, val cardStroke: Int,
        val text: Int, val subtext: Int, val accent: Int, val isDark: Boolean,
        // 2.0 extensions
        val bgTop: Int = bg, val cardTop: Int = card,
        val accent2: Int = accent, val accentSoft: Int = accent,
        val green: Int = 0xFF34D399.toInt(), val amber: Int = 0xFFFBBF24.toInt(),
        val faint: Int = subtext
    )

    fun palette(context: Context): Palette {
        val night = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (night) Palette(
            0xFF0A0D13.toInt(), 0xE6181C24.toInt(), 0x26FFFFFF.toInt(),
            0xFFF2F5F9.toInt(), 0xFF9AA3AE.toInt(), 0xFF4D8DFF.toInt(), true,
            bgTop = 0xFF131826.toInt(), cardTop = 0xFF1D222D.toInt(),
            accent2 = 0xFF8B5CF6.toInt(), accentSoft = 0x2E4D8DFF,
            faint = 0xFF5A6472.toInt()
        ) else Palette(
            0xFFF0F3F9.toInt(), 0xF2FFFFFF.toInt(), 0x338A94A8.toInt(),
            0xFF141922.toInt(), 0xFF66707F.toInt(), 0xFF2E6BFF.toInt(), false,
            bgTop = 0xFFE4EBF7.toInt(), cardTop = 0xFFFFFFFF.toInt(),
            accent2 = 0xFF7C3AED.toInt(), accentSoft = 0x1F2E6BFF,
            faint = 0xFFA2AAB8.toInt()
        )
    }

    fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()

    private fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    // ---------- gradients ----------

    fun accentGradient(p: Palette): LinearGradient =
        LinearGradient(0f, 0f, 1f, 1f,
            intArrayOf(p.accent, p.accent2), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)

    private fun cardDrawable(context: Context, p: Palette, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            colors = intArrayOf(p.cardTop, p.card)
            orientation = GradientDrawable.Orientation.TL_BR
            cornerRadius = radius
            setStroke(dp(context, 1), p.cardStroke)
        }

    private fun glassLayers(context: Context, p: Palette, radius: Float): LayerDrawable {
        // soft top sheen strip over the base gradient
        val base = cardDrawable(context, p, radius)
        val sheen = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(0x14FFFFFF, 0x00FFFFFF)
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
        return LayerDrawable(arrayOf(base, sheen))
    }

    private fun rippleify(context: Context, drawable: android.graphics.drawable.Drawable,
                          radius: Float): RippleDrawable {
        val mask = GradientDrawable().apply {
            cornerRadius = radius
            setColor(Color.WHITE)
        }
        return RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF), drawable, mask)
    }

    private fun pressedTint(context: Context, drawable: android.graphics.drawable.Drawable,
                            radius: Float): RippleDrawable {
        val c = UiKit.palette(context)
        val mask = GradientDrawable().apply { cornerRadius = radius; setColor(Color.WHITE) }
        val tint = android.content.res.ColorStateList.valueOf(
            if (c.isDark) 0x22FFFFFF else 0x183366CC)
        return RippleDrawable(tint, drawable, mask)
    }

    // ---------- base card ----------

    fun card(context: Context, p: Palette): LinearLayout {
        val d = dp(context, 16)
        val r = dp(context, 22).toFloat()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d, d, d, d)
            background = glassLayers(context, p, r)
        }
    }

    fun cardRipple(context: Context, p: Palette, onClick: () -> Unit): LinearLayout {
        val d = dp(context, 16)
        val r = dp(context, 22).toFloat()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d, d, d, d)
            background = pressedTint(context, glassLayers(context, p, r), r)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    // ---------- typography ----------

    fun title(context: Context, p: Palette, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 17f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    fun bigTitle(context: Context, p: Palette, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 22f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    fun body(context: Context, p: Palette, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 13.5f
            setTextColor(p.subtext)
            setLineSpacing(dp(context, 2).toFloat(), 1f)
        }

    fun sectionHeader(context: Context, p: Palette, text: String): LinearLayout {
        val c = context
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 4), 0, dp(context, 4), dp(context, 10))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(c, 4), dp(c, 15))
                background = GradientDrawable().apply {
                    cornerRadius = dp(c, 2).toFloat()
                    setColor(p.accent)
                }
            })
            addView(TextView(context).apply {
                this.text = text
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(p.subtext)
                letterSpacing = 0.06f
                setPadding(dp(c, 8), 0, 0, 0)
            })
        }
    }

    // ---------- icon tile ----------

    fun iconTile(context: Context, p: Palette, icon: String, tint: Int,
                 tileColor: Int, tileSizeDp: Int = 44): View =
        tileView(context, p, icon, tint, tileColor, tileSizeDp)

    private fun tileView(context: Context, p: Palette, icon: String, tint: Int,
                         tileColor: Int, tileSizeDp: Int): View {
        val s = dp(context, tileSizeDp)
        val r = dp(context, 14).toFloat()
        val frame = FrameLayout(context)
        frame.background = GradientDrawable().apply {
            cornerRadius = r
            setColor(tileColor)
        }
        val v = object : View(context) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.STROKE
            }
            override fun onDraw(canvas: Canvas) {
                val size = width * 0.52f
                paint.strokeWidth = size * 0.075f
                SettingsIcons.draw(canvas, icon, width / 2f, height / 2f, size, paint)
            }
        }
        frame.addView(v, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.layoutParams = LinearLayout.LayoutParams(s, s)
        return frame
    }

    // ---------- nav row (icon + text + chevron) ----------

    fun navRow(context: Context, p: Palette, icon: String, iconTint: Int, iconBg: Int,
               titleText: String, subText: String,
               onClick: (() -> Unit)? = null): LinearLayout {
        val d = dp(context, 14)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 2), d, dp(context, 2), d)
            isClickable = true
            isFocusable = true
            onClick?.let { cb ->
                background = ripple(context)
                setOnClickListener { cb() }
            }
        }
        row.addView(tileView(context, p, icon, iconTint, iconBg, 44),
            LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)))
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(context, 14), 0, dp(context, 14), 0)
        }
        texts.addView(TextView(context).apply {
            text = titleText
            textSize = 15.5f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        if (subText.isNotEmpty()) {
            texts.addView(TextView(context).apply {
                text = subText
                textSize = 12.5f
                setTextColor(p.subtext)
                setPadding(0, dp(context, 3), 0, 0)
            })
        }
        row.addView(texts)
        // chevron: points to layout-end, auto-flipped for RTL
        val chev = object : View(context) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = p.faint
                style = Paint.Style.STROKE
            }
            override fun onDraw(canvas: Canvas) {
                val size = width * 0.62f
                paint.strokeWidth = size * 0.11f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                canvas.save()
                if (isRtl(context)) canvas.scale(-1f, 1f, width / 2f, height / 2f)
                SettingsIcons.draw(canvas, "chevron", width / 2f, height / 2f, size, paint)
                canvas.restore()
            }
        }
        chev.layoutParams = LinearLayout.LayoutParams(dp(context, 20), dp(context, 20))
        row.addView(chev)
        return row
    }

    fun ripple(context: Context): RippleDrawable {
        val c = UiKit.palette(context)
        val mask = GradientDrawable().apply { cornerRadius = dp(context, 14).toFloat(); setColor(Color.WHITE) }
        val tint = android.content.res.ColorStateList.valueOf(if (c.isDark) 0x24FFFFFF else 0x1A3366CC)
        return RippleDrawable(tint, null, mask)
    }

    // ---------- legacy row (still used by ThemeEditor / ColorPicker) ----------

    fun row(context: Context, p: Palette, titleText: String, subText: String,
            onClick: (() -> Unit)? = null): LinearLayout {
        val d = dp(context, 14)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, d, 0, d)
            isClickable = true
            isFocusable = true
            onClick?.let { cb ->
                background = ripple(context)
                setOnClickListener { cb() }
            }
            val texts = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(context).apply {
                this.text = titleText
                textSize = 15f
                setTextColor(p.text)
            })
            if (subText.isNotEmpty()) {
                texts.addView(TextView(context).apply {
                    this.text = subText
                    textSize = 12.5f
                    setTextColor(p.subtext)
                    setPadding(0, dp(context, 2), 0, 0)
                })
            }
            addView(texts)
        }
    }

    fun switchRow(context: Context, p: Palette, titleText: String, subText: String,
                  initial: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = row(context, p, titleText, subText, null)
        val sw = Switch(context).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
            trackDrawable?.setTint(if (initial) p.accent else p.faint)
        }
        row.addView(sw)
        row.setOnClickListener { sw.toggle() }
        return row
    }

    // ---------- button ----------

    fun button(context: Context, p: Palette, text: String, filled: Boolean,
               onClick: () -> Unit): TextView {
        val d = dp(context, 12)
        val r = dp(context, 16).toFloat()
        return TextView(context).apply {
            this.text = text
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(d * 2, d, d * 2, d)
            setTextColor(if (filled) Color.WHITE else p.accent)
            if (filled) {
                background = pressedTint(context, GradientDrawable().apply {
                    cornerRadius = r
                    orientation = GradientDrawable.Orientation.TL_BR
                    colors = intArrayOf(p.accent, p.accent2)
                }, r)
            } else {
                background = pressedTint(context, GradientDrawable().apply {
                    cornerRadius = r
                    setColor(p.accentSoft)
                    setStroke(1, p.accent)
                }, r)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    // ---------- pills / chips ----------

    fun statusPill(context: Context, p: Palette, text: String, active: Boolean): LinearLayout {
        val color = if (active) p.green else p.amber
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 20).toFloat()
                setColor(if (p.isDark) 0x33000000 else 0x14000000)
            }
            setPadding(dp(context, 12), dp(context, 7), dp(context, 14), dp(context, 7))
        }
        pill.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 8), dp(context, 8)).apply {
                setMargins(0, 0, dp(context, 8), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        })
        pill.addView(TextView(context).apply {
            this.text = text
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(color)
        })
        return pill
    }

    fun chip(context: Context, p: Palette, text: String, tint: Int): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(tint)
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 20).toFloat()
                setColor(tint and 0x2AFFFFFF or (tint and 0xFF000000.toInt()))
            }
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
        }

    // ---------- stat card (for feature grid) ----------

    fun statCard(context: Context, p: Palette, icon: String, value: String,
                 label: String, tint: Int, tileColor: Int): LinearLayout {
        val r = dp(context, 20).toFloat()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 10), dp(context, 16), dp(context, 10), dp(context, 16))
            background = pressedTint(context, glassLayers(context, p, r), r)
            isClickable = true
            isFocusable = true
        }
        card.addView(tileView(context, p, icon, tint, tileColor, 40))
        card.addView(TextView(context).apply {
            text = value
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(p.text)
            setPadding(0, dp(context, 10), 0, 0)
        })
        card.addView(TextView(context).apply {
            text = label
            textSize = 11.5f
            setTextColor(p.subtext)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 2), 0, 0)
        })
        return card
    }

    // ---------- step badge ----------

    fun stepBadge(context: Context, p: Palette, number: String, done: Boolean): View {
        val s = dp(context, 44)
        val frame = FrameLayout(context)
        frame.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (done) p.green and 0x30FFFFFF or (p.green and 0xFF000000.toInt()) else p.accentSoft)
        }
        if (done) {
            val check = object : View(context) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = p.green
                    style = Paint.Style.STROKE
                }
                override fun onDraw(canvas: Canvas) {
                    val size = width * 0.5f
                    paint.strokeWidth = size * 0.16f
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    SettingsIcons.draw(canvas, "check", width / 2f, height / 2f, size, paint)
                }
            }
            frame.addView(check, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            frame.addView(TextView(context).apply {
                text = number
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(p.accent)
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        frame.layoutParams = LinearLayout.LayoutParams(s, s)
        return frame
    }

    // ---------- misc ----------

    fun accentChip(context: Context, p: Palette, color: Int, label: String): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 44), dp(context, 44))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, p.cardStroke)
                }
            })
            addView(TextView(context).apply {
                this.text = label
                textSize = 11f
                setTextColor(p.subtext)
                gravity = Gravity.CENTER
                setPadding(0, dp(context, 4), 0, 0)
            })
        }

    fun divider(context: Context, p: Palette): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)
        )
        setBackgroundColor(p.cardStroke)
    }

    fun rounded(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    /** Full glass card background (with gradient sheen) wrapped in a ripple. */
    fun pressed(context: Context, radius: Float): RippleDrawable =
        pressedTint(context, glassLayers(context, palette(context), radius), radius)

    // ---------- screen header for sub-activities ----------

    fun screenHeader(activity: Activity, p: Palette, title: String, sub: String,
                     icon: String, tint: Int, tileColor: Int): View {
        val c = activity
        val bar = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(c, 6), 0, dp(c, 18))
        }
        val backTile = FrameLayout(c).apply {
            background = pressedTint(c, rounded(
                if (p.isDark) 0xFF222835.toInt() else 0xFFFFFFFF.toInt(),
                dp(c, 15).toFloat()), dp(c, 15).toFloat())
            isClickable = true
            isFocusable = true
        }
        val backIcon = object : View(c) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = p.text
                style = Paint.Style.STROKE
            }
            override fun onDraw(canvas: Canvas) {
                val size = width * 0.5f
                paint.strokeWidth = size * 0.12f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                canvas.save()
                if (isRtl(c)) canvas.scale(-1f, 1f, width / 2f, height / 2f)
                SettingsIcons.draw(canvas, "back", width / 2f, height / 2f, size, paint)
                canvas.restore()
            }
        }
        backTile.addView(backIcon, FrameLayout.LayoutParams(
            dp(c, 46), dp(c, 46)))
        backTile.setOnClickListener { activity.finish() }
        bar.addView(backTile)

        val tile = iconTile(c, p, icon, tint, tileColor, 46)
        val tileWrap = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(c, 12), 0, 0, 0)
        }
        tileWrap.addView(tile)
        bar.addView(tileWrap)

        val texts = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(c, 12), 0, 0, 0)
        }
        texts.addView(TextView(c).apply {
            text = title
            textSize = 19f
            setTextColor(p.text)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        if (sub.isNotEmpty()) {
            texts.addView(TextView(c).apply {
                text = sub
                textSize = 12.5f
                setTextColor(p.subtext)
                setPadding(0, dp(c, 2), 0, 0)
            })
        }
        bar.addView(texts)
        return bar
    }

    // hero gradient background for the top card
    fun heroDrawable(context: Context, p: Palette): android.graphics.drawable.Drawable {
        val r = dp(context, 26).toFloat()
        val gradient = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TL_BR
            colors = if (p.isDark)
                intArrayOf(0xFF1E2A4A.toInt(), 0xFF141824.toInt())
            else
                intArrayOf(0xFF3D6BFF.toInt(), 0xFF6D3AD6.toInt())
            cornerRadius = r
        }
        val decor = object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val hgt = bounds.height().toFloat()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                // soft glowing circles
                paint.color = if (p.isDark) 0x2E4D8DFF else 0x30FFFFFF
                canvas.drawCircle(w * 0.16f, hgt * 1.05f, w * 0.42f, paint)
                paint.color = if (p.isDark) 0x268B5CF6 else 0x22FFFFFF
                canvas.drawCircle(w * 0.94f, hgt * -0.15f, w * 0.5f, paint)
                // keyboard glyph watermark
                paint.color = 0x24FFFFFF
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.012f
                SettingsIcons.draw(canvas, "keyboard", w * 0.87f, hgt * 0.2f, w * 0.14f, paint)
            }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
        return LayerDrawable(arrayOf(gradient, decor))
    }

    // blurred accent glow behind the app glyph in the hero
    fun glowGlyphView(context: Context, p: Palette, sizeDp: Int = 58): View {
        val s = dp(context, sizeDp)
        return object : View(context) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
            }
            init {
                // BlurMaskFilter needs a software layer to actually render
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            override fun onDraw(canvas: Canvas) {
                val size = width * 0.56f
                paint.strokeWidth = size * 0.07f
                val glow = Paint(paint)
                glow.color = 0x66FFFFFF
                glow.maskFilter = BlurMaskFilter(width * 0.05f, BlurMaskFilter.Blur.NORMAL)
                SettingsIcons.draw(canvas, "keyboard", width / 2f, height / 2f, size, glow)
                SettingsIcons.draw(canvas, "keyboard", width / 2f, height / 2f, size, paint)
            }
        }.apply { layoutParams = LinearLayout.LayoutParams(s, s) }
    }
}
