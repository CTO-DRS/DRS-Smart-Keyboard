/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.privacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 1): renders the age
 * certificate as a programmatic PNG — zero new libraries, the
 * platform's own graphics only:
 *  - the drawing engine is `android.graphics.Canvas` on an in-memory
 *    `Bitmap` (Arabic text is native to the platform renderer — RTL
 *    correct by construction, no text-shaping library);
 *  - the PNG container itself is encoded by [PngCodec] below — a PURE
 *    (java.util.zip only) RFC-2083 encoder that is byte-level tested
 *    on the JVM: the "a PNG is actually generated" proof does not need
 *    a device.
 *
 * The image lands in `cacheDir/transparency/` (the approved
 * `<cache-path name="transparency">` resource in file_paths.xml — a
 * resource, not a manifest permission) and is shared through the
 * EXISTING FileProvider (`${applicationId}.provider.file`).
 *
 * The row layout is computed by the PURE `DrsAgeCertificate.rows()` —
 * the on-screen card and the shared PNG render the exact same lines
 * (one tested path, two views).
 */
object DrsCertificatePng {

    /** The approved cache subdirectory (file_paths.xml cache-path resource). */
    const val CACHE_SUBDIR = "transparency"
    const val FILE_NAME = "drs_age_certificate.png"

    // canvas geometry — portrait certificate proportions
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val MARGIN = 96f
    private const val BG = 0xFF10141A.toInt()
    private const val FG = 0xFFF3F5F9.toInt()
    private const val ACCENT = 0xFF6FCF97.toInt()
    private const val MUTED = 0xFF9AA3B2.toInt()

    /**
     * Resolves the localized, value-formatted labels for a certificate.
     * Shared by the PNG renderer and the on-screen card so both views
     * carry identical text (android-only — needs Context resources).
     */
    fun labels(context: Context, certificate: DrsAgeCertificate.Certificate): DrsAgeCertificate.Labels {
        fun s(id: Int): String = context.getString(id)
        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(certificate.firstUseAt))
        return DrsAgeCertificate.Labels(
            firstUseDate = s(com.drs.smartkeyboard.R.string.transparency__cert__first_use)
                .format(date),
            ageLine = s(com.drs.smartkeyboard.R.string.transparency__cert__age)
                .format(certificate.ageDays),
            daysLine = s(com.drs.smartkeyboard.R.string.transparency__cert__days)
                .format(certificate.attestedDays),
            totalsLine = s(com.drs.smartkeyboard.R.string.transparency__cert__totals)
                .format(formatBytes(certificate.totalRxBytes), formatBytes(certificate.totalTxBytes)),
            zeroClaim = s(com.drs.smartkeyboard.R.string.transparency__cert__zero_claim),
            movedClaim = s(com.drs.smartkeyboard.R.string.transparency__cert__moved_claim),
            unsupportedNote = s(com.drs.smartkeyboard.R.string.transparency__cert__unsupported_note),
            honestyNote = s(com.drs.smartkeyboard.R.string.transparency__cert__honesty_note),
        )
    }

    /**
     * Renders the certificate PNG and returns the file. Runs on the
     * caller's dispatcher (the screen calls it under Dispatchers.IO).
     * Returns null on failure — the UI says so honestly.
     */
    fun render(context: Context, certificate: DrsAgeCertificate.Certificate): File? {
        return try {
            val labels = labels(context, certificate)
            val rows = DrsAgeCertificate.rows(certificate, labels)
            val title = context.getString(com.drs.smartkeyboard.R.string.transparency__cert__png_title)
            val ageHeadline = context.getString(com.drs.smartkeyboard.R.string.transparency__cert__png_age_headline)
                .format(certificate.ageDays)
            val brand = context.getString(com.drs.smartkeyboard.R.string.transparency__cert__png_brand)

            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(BG)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FG; textSize = 64f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ACCENT; textSize = 88f; typeface = Typeface.DEFAULT_BOLD
            }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FG; textSize = 40f }
            val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 28f }
            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT; textSize = 40f }
            val rule = Paint().apply { color = MUTED; strokeWidth = 2f }

            canvas.drawText(title, MARGIN, 160f, titlePaint)
            canvas.drawLine(MARGIN, 200f, WIDTH - MARGIN, 200f, rule)
            canvas.drawText(ageHeadline, MARGIN, 330f, headlinePaint)

            var y = 440f
            for (row in rows) {
                val paint = when (row) {
                    labels.zeroClaim -> accent
                    labels.honestyNote, labels.unsupportedNote -> muted
                    else -> body
                }
                canvas.drawText(row, MARGIN, y, paint)
                y += 64f
            }

            canvas.drawText(brand, MARGIN, HEIGHT - 72f, muted)

            val pixels = IntArray(WIDTH * HEIGHT)
            bitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
            bitmap.recycle()

            val pngBytes = PngCodec.encode(pixels, WIDTH, HEIGHT)
            val dir = File(context.cacheDir, CACHE_SUBDIR)
            dir.mkdirs()
            val out = File(dir, FILE_NAME)
            out.writeBytes(pngBytes)
            out
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%d KB".format(bytes / 1_000)
        else -> "$bytes B"
    }

    /**
     * PURE RFC-2083 PNG encoder (no Android, no libraries beyond
     * java.util.zip): raw RGBA pixel buffer → PNG byte stream
     * (signature + IHDR + IDAT[zlib] + IEND). Byte-level tested on the
     * JVM — the mandatory "a PNG is actually generated" proof.
     */
    object PngCodec {

        val SIGNATURE: ByteArray = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )

        private const val COLOR_TYPE_RGBA = 6
        private const val BIT_DEPTH = 8
        private const val FILTER_NONE: Byte = 0

        /** Encodes an ARGB IntArray into a complete PNG byte stream. */
        fun encode(pixels: IntArray, width: Int, height: Int): ByteArray {
            require(width > 0 && height > 0) { "png dims must be positive" }
            require(pixels.size >= width * height) { "pixel buffer smaller than the canvas" }

            val ihdr = ByteArray(13)
            putInt(ihdr, 0, width)
            putInt(ihdr, 4, height)
            ihdr[8] = BIT_DEPTH.toByte()
            ihdr[9] = COLOR_TYPE_RGBA.toByte()
            // ihdr[10] = 0 (deflate), ihdr[11] = 0 (adaptive filter), ihdr[12] = 0 (no interlace)

            // raw scanlines: each row prefixed with filter type 0 (None)
            val stride = width * 4
            val raw = ByteArray(height * (stride + 1))
            var out = 0
            for (y in 0 until height) {
                raw[out] = FILTER_NONE
                out += 1
                for (x in 0 until width) {
                    val p = pixels[y * width + x]
                    raw[out] = (p ushr 16).toByte() // R
                    raw[out + 1] = (p ushr 8).toByte() // G
                    raw[out + 2] = p.toByte() // B
                    raw[out + 3] = (p ushr 24).toByte() // A
                    out += 4
                }
            }

            return SIGNATURE +
                chunk("IHDR", ihdr) +
                chunk("IDAT", zlib(raw)) +
                chunk("IEND", ByteArray(0))
        }

        /** One PNG chunk: length + type + data + CRC32(type+data). */
        internal fun chunk(type: String, data: ByteArray): ByteArray {
            require(type.length == 4)
            val out = ByteArray(12 + data.size)
            putInt(out, 0, data.size)
            for (i in 0..3) out[4 + i] = type[i].code.toByte()
            data.copyInto(out, 8)
            val crc = CRC32()
            crc.update(out, 4, 4 + data.size)
            putInt(out, 8 + data.size, crc.value.toInt())
            return out
        }

        private fun zlib(data: ByteArray): ByteArray {
            val deflater = Deflater()
            deflater.setInput(data)
            deflater.finish()
            val buffer = ByteArrayOutputStream(data.size / 2 + 32)
            val window = ByteArray(8192)
            while (!deflater.finished()) {
                val n = deflater.deflate(window)
                buffer.write(window, 0, n)
            }
            deflater.end()
            return buffer.toByteArray()
        }

        private fun putInt(target: ByteArray, offset: Int, value: Int) {
            target[offset] = (value ushr 24).toByte()
            target[offset + 1] = (value ushr 16).toByte()
            target[offset + 2] = (value ushr 8).toByte()
            target[offset + 3] = value.toByte()
        }
    }
}
