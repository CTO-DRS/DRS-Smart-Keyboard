/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.drs.smartkeyboard.drs.DrsEmojiKitchen
import java.io.File

/**
 * راسم مطبخ الإيموجي (the kitchen art executor) — the thin Android layer
 * that turns a PURE [DrsEmojiKitchen.KitchenPlan] into a REAL rendered
 * PNG (no fake previews anywhere: what the panel shows is what gets sent).
 *
 * The glyphs are drawn from the system emoji font (the platform's own),
 * layered by the closed recipe plan onto a Canvas we own. The stickers
 * are the project's original vector drawables (docs/EMOJI_KITCHEN.md §4).
 *
 * Kept deliberately thin: every decision was proven pure-side (563-check
 * simulation + Kotest contracts); this file only executes pixels.
 */
object DrsKitchenArt {

    /** The master render size (px) for send-quality PNGs. */
    const val MASTER_SIZE: Int = 512

    /** The on-disk cache subdirectory (served through the FileProvider). */
    const val CACHE_DIR: String = "emoji-kitchen"

    /** The maximum number of cached compositions kept around. */
    private const val CACHE_KEEP: Int = 10

    /** Deterministic pastel background per recipe id (closed map). */
    private val RECIPE_BG: Map<String, Long> = mapOf(
        "face_heart" to 0xFFFFF0D3, "face_face" to 0xFFE3F2FD,
        "face_hands" to 0xFFFFE8E8, "face_food" to 0xFFE8F5E9,
        "face_animal" to 0xFFFFF9C4, "heart_face" to 0xFFFFD6E0,
        "heart_object" to 0xFFFCE4EC, "heart_animal" to 0xFFFFECDD,
        "heart_food" to 0xFFFFEBEE, "object_symbol" to 0xFFEDE7F6,
        "weather_face" to 0xFFE1F5FE, "symbol_symbol" to 0xFFFFF8E1,
        "hands_heart" to 0xFFF3E5F5,
    )

    /** The fallback background for an unknown recipe id (never hit by the closed catalog). */
    private const val DEFAULT_BG = 0xFFFFF4E3

    /**
     * Renders a plan into a real bitmap — the panel preview and the sent
     * PNG are the SAME function, so what you see is what you send.
     */
    fun renderPlan(plan: DrsEmojiKitchen.KitchenPlan, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (RECIPE_BG[plan.recipeId] ?: DEFAULT_BG).toInt()
            style = Paint.Style.FILL
        }
        val radius = sizePx * 0.12f
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, bg)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        for (layer in plan.layers) {
            canvas.save()
            canvas.rotate(layer.rotationDeg, layer.cx * sizePx, layer.cy * sizePx)
            text.textSize = layer.scale * sizePx * 0.78f
            val x = layer.cx * sizePx
            // baseline offset centers the glyph box on (cx, cy)
            val y = layer.cy * sizePx + text.textSize * 0.35f
            canvas.drawText(layer.glyph, x, y, text)
            canvas.restore()
        }
        return bitmap
    }

    /** Renders a project-original vector sticker into a real bitmap. */
    fun renderSticker(context: Context, drawableRes: Int, sizePx: Int): Bitmap {
        val drawable = context.getDrawable(drawableRes) ?: return emptyBitmap(sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun emptyBitmap(sizePx: Int): Bitmap =
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

    /**
     * Saves a PNG into the kitchen cache dir (pruning old files) and
     * returns its FileProvider content URI — the exact authority the
     * manifest already declares. Null only when the disk truly fails.
     */
    fun savePng(context: Context, bitmap: Bitmap, name: String): Uri? {
        return try {
            val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            dir.listFiles()?.sortedBy { it.lastModified() }?.dropLast(CACHE_KEEP - 1)
                ?.forEach { it.delete() }
            val file = File(dir, "$name.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider.file", file)
        } catch (_: Exception) {
            null
        }
    }

    /** A deterministic cache file name for a composition/sticker key. */
    fun fileNameFor(key: String): String =
        "kitchen_" + Integer.toHexString(key.hashCode())
}
