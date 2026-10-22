/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

/**
 * مخطط إرسال مطبخ الإيموجي (the kitchen send planner) — the PURE
 * decision layer of the send mechanism.
 *
 * The Android runtime asks this planner what the honest path is, given
 * the active editor's accepted MIME types and the user's image-send
 * setting, then executes the returned path and reports it to the user.
 * The planner never lies: when the editor cannot receive images the
 * plan is the real text composition of the pair — announced as such.
 */
object DrsKitchenSendPlanner {

    /** The honest send paths. */
    enum class SendPath {
        /** The editor accepts image content — send the rendered PNG. */
        CONTENT,

        /** The editor does not accept images (or images are disabled) and
         * the item HAS a real text composition — commit it as text. */
        TEXT_PAIR,

        /** The editor does not accept images and the item has NO text
         * composition (a library sticker) — copy the PNG to the system
         * clipboard so the user can paste it; announced as such. */
        CLIPBOARD,
    }

    /** The MIME type the renderer produces and the FileProvider serves. */
    const val IMAGE_MIME: String = "image/png"

    /**
     * The decision, pure and total:
     * - images enabled AND the editor lists [IMAGE_MIME] → CONTENT;
     * - anything else (no mimes, no image support, setting off) →
     *   TEXT_PAIR — the fallback is a feature, not a failure.
     */
    fun plan(mimeTypes: List<String>?, imagesEnabled: Boolean, hasTextPair: Boolean = true): SendPath {
        if (imagesEnabled) {
            val mimes = mimeTypes ?: emptyList()
            if (mimes.any { it.lowercase() == IMAGE_MIME }) return SendPath.CONTENT
        }
        return if (hasTextPair) SendPath.TEXT_PAIR else SendPath.CLIPBOARD
    }
}
