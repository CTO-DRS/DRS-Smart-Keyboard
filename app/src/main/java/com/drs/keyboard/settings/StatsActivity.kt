package com.drs.keyboard.settings

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.drs.keyboard.R
import com.drs.keyboard.engine.StatsStore
import java.util.Locale

/**
 * Typing statistics, 100% on-device: a 7-day bar chart plus all-time
 * totals. Deterministic counters — no analytics SDK, no network.
 */
class StatsActivity : Activity() {

    private lateinit var p: UiKit.Palette
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = UiKit.palette(this)
        UiKit.applySystemBars(this, p)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            isVerticalScrollBarEnabled = false
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@StatsActivity, 18), UiKit.dp(this@StatsActivity, 14),
                UiKit.dp(this@StatsActivity, 18), UiKit.dp(this@StatsActivity, 30))
        }
        scroll.addView(root)
        setContentView(scroll)
        build()
    }

    private fun build() {
        val c = this
        val tBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val tViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED

        root.addView(UiKit.screenHeader(c, p, getString(R.string.stats_title),
            getString(R.string.stats_sub), "spark", p.accent, tBlue))

        // ---- weekly chart ----
        val chartCard = UiKit.card(c, p)
        chartCard.addView(UiKit.title(c, p, getString(R.string.stats_week)))
        val days = StatsStore.lastDays(c, 7)
        val chart = WeekChartView(c).apply {
            setData(days.map { StatsStore.dayLabel(it.first, Locale.getDefault()) to
                    (it.second[StatsStore.KEYS] ?: 0) })
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(c, 170)).apply { setMargins(0, UiKit.dp(c, 10), 0, 0) }
        }
        chartCard.addView(chart)
        chartCard.addView(UiKit.body(c, p, getString(R.string.stats_week_note)).apply {
            setPadding(0, UiKit.dp(c, 8), 0, 0)
        })
        root.addView(chartCard, pad(0, 0, 0, UiKit.dp(c, 16)))

        // ---- totals grid ----
        root.addView(UiKit.sectionHeader(c, p, getString(R.string.stats_totals).uppercase()),
            pad(0, 0, 0, UiKit.dp(c, 10)))

        val totals = StatsStore.totals(c)
        val loc = Locale.getDefault()
        val grid = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        val cellLp = LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val cardBlue = if (p.isDark) 0x264D8DFF else 0x1A2E6BFF
        val cardViolet = if (p.isDark) 0x268B5CF6 else 0x1A7C3AED
        val cardTeal = if (p.isDark) 0x262DD4BF else 0x1A0D9488
        val cardAmber = if (p.isDark) 0x26F59E0B else 0x1AD97706

        row1.addView(UiKit.statCard(c, p, "keyboard", format(totals[StatsStore.KEYS] ?: 0, loc),
            getString(R.string.stats_keys), p.accent, cardBlue), cellLp)
        row1.addView(space(10))
        row1.addView(UiKit.statCard(c, p, "spark", format(totals[StatsStore.WORDS] ?: 0, loc),
            getString(R.string.stats_words_n), 0xFF14B8A6.toInt(), cardTeal), cellLp)
        row2.addView(UiKit.statCard(c, p, "emoji", format(totals[StatsStore.EMOJI] ?: 0, loc),
            getString(R.string.stats_emoji_n), 0xFFEC4899.toInt(), cardViolet), cellLp)
        row2.addView(space(10))
        row2.addView(UiKit.statCard(c, p, "swipe", format(totals[StatsStore.SWIPES] ?: 0, loc),
            getString(R.string.stats_swipes), 0xFFF59E0B.toInt(), cardAmber), cellLp)

        grid.addView(row1, pad(0, 0, 0, UiKit.dp(c, 10)))
        grid.addView(row2)
        root.addView(grid, pad(0, 0, 0, UiKit.dp(c, 16)))

        // ---- per-app top list (local-only, incognito never records) ----
        val apps = StatsStore.topApps(c, 5)
        if (apps.isNotEmpty()) {
            val pm = packageManager
            val appsCard = UiKit.card(c, p)
            appsCard.addView(UiKit.title(c, p, getString(R.string.stats_apps)))
            val maxCount = apps.first().second.coerceAtLeast(1)
            for ((pkg, count) in apps) {
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                appsCard.addView(appRow(c, p, label, count, maxCount))
            }
            appsCard.addView(UiKit.body(c, p, getString(R.string.stats_apps_note)).apply {
                setPadding(0, UiKit.dp(c, 10), 0, 0)
            })
            root.addView(appsCard, pad(0, 0, 0, UiKit.dp(c, 16)))
        }

        // ---- privacy note + clear ----
        val note = UiKit.card(c, p)
        note.addView(UiKit.body(c, p, getString(R.string.stats_note)).apply {
            setPadding(0, 0, 0, UiKit.dp(c, 12))
        })
        note.addView(UiKit.button(c, p, getString(R.string.stats_clear), false) {
            StatsStore.clear(c)
            build()
        })
        root.addView(note)
    }

    private fun format(n: Int, loc: Locale): String =
        String.format(loc, "%,d", n)

    /** One app row: label + word count + a thin proportional usage bar. */
    private fun appRow(
        c: Context, p: UiKit.Palette, label: String, count: Int, maxCount: Int
    ): View {
        val row = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        val name = TextView(c).apply {
            text = label
            setTextColor(p.text)
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val num = TextView(c).apply {
            text = format(count, Locale.getDefault())
            setTextColor(p.subtext)
            textSize = 13f
            gravity = Gravity.END
        }
        top.addView(name, LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(num)
        row.addView(top)
        val barBg = FrameLayout(c).apply { setBackgroundColor(p.faint) }
        val bar = View(c).apply { setBackgroundColor(0xFF14B8A6.toInt()) }
        row.addView(barBg, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@StatsActivity, 4)).apply {
            setMargins(0, UiKit.dp(this@StatsActivity, 6), 0, UiKit.dp(this@StatsActivity, 10))
        })
        barBg.addView(bar, FrameLayout.LayoutParams(
            UiKit.dp(this@StatsActivity, 2), UiKit.dp(this@StatsActivity, 4)
        ))
        // width computed post-layout: proportion of the max
        barBg.post {
            bar.layoutParams = FrameLayout.LayoutParams(
                ((barBg.width.toFloat() * count / maxCount.coerceAtLeast(1)).toInt())
                    .coerceAtLeast(UiKit.dp(this@StatsActivity, 2)),
                UiKit.dp(this@StatsActivity, 4))
        }
        return row
    }

    private fun space(w: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@StatsActivity, w), 1)
    }

    private fun pad(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(l, t, r, b) }

    // ------------------------------------------------------------------
    // Painted 7-day bar chart (no chart library, no assets)
    // ------------------------------------------------------------------

    private inner class WeekChartView(context: Context) : View(context) {

        private var labels: List<String> = emptyList()
        private var values: List<Int> = emptyList()
        private val max get() = (values.maxOrNull() ?: 0)

        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private fun dpf(v: Int): Float = UiKit.dp(this@StatsActivity, v).toFloat()

        init {
            axisPaint.color = p.cardStroke
            axisPaint.strokeWidth = dpf(1)
            labelPaint.color = p.subtext
            labelPaint.textAlign = Paint.Align.CENTER
            labelPaint.textSize = dpf(11)
            valuePaint.color = p.accent
            valuePaint.textAlign = Paint.Align.CENTER
            valuePaint.isFakeBoldText = true
            valuePaint.textSize = dpf(11)
        }

        fun setData(data: List<Pair<String, Int>>) {
            labels = data.map { it.first }
            values = data.map { it.second }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val n = labels.size
            if (n == 0) return
            val baseY = height - dpf(18) - dpf(6)
            val topPad = dpf(16)
            val slot = width / n.toFloat()
            val barW = slot * 0.44f

            // baseline
            canvas.drawLine(slot * 0.2f, baseY, width - slot * 0.2f, baseY, axisPaint)

            for (i in 0 until n) {
                val cx = slot * i + slot / 2f
                val v = values.getOrElse(i) { 0 }
                val isMax = v == max && v > 0
                barPaint.color = when {
                    v <= 0 -> p.cardStroke
                    isMax -> p.accent
                    else -> if (p.isDark) 0x54000000 or (p.accent and 0x00FFFFFF)
                    else 0x42000000 or (p.accent and 0x00FFFFFF)
                }
                val h = if (max <= 0 || v <= 0) 0f
                else (baseY - topPad - dpf(4)) * (v.toFloat() / max)
                val rect = RectF(cx - barW / 2f, baseY - h, cx + barW / 2f, baseY)
                canvas.drawRoundRect(rect, barW / 2f, barW / 2f, barPaint)

                // value above the tallest bar only (keeps it clean)
                if (isMax) {
                    val ty = rect.top - dpf(4)
                    canvas.drawText(format(v, Locale.getDefault()), cx, ty, valuePaint)
                }
                // day label
                val ly = height - dpf(5)
                labelPaint.typeface = if (i == n - 1)
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                labelPaint.color = if (i == n - 1) p.text else p.subtext
                canvas.drawText(labels[i], cx, ly, labelPaint)
            }
        }
    }
}
