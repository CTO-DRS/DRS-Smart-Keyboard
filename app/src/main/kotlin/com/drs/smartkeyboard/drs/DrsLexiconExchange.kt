/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.lib.ext.DrsEd25519
import com.drs.smartkeyboard.lib.ext.DrsPackageTrust
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 2): the signed lexicon
 * exchange — export/import of «كلماتي» (DrsMyLexicon) with the EXACT
 * flex signature structure the package catalog already uses
 * (`sha256` + `signature` hex + `pubkey_id`, ADR-0007 heritage).
 *
 * THE FILE (schema `drs-lexicon-exchange-1`, versioned for future
 * compatibility):
 *   { schema, version, exported_at, entries[], sha256, signature,
 *     pubkey_id, pubkey? }
 *
 * SIGNATURE SEMANTICS (fail-closed, mirroring DrsPackageTrust):
 *   - `sha256` = hex of SHA-256 over the CANONICAL serialization of the
 *     entries (same serializer both sides — deterministic bytes);
 *   - `signature` = ed25519 over the RAW 32-byte digest (the signature
 *     covers the content hash — ADR-0009 decision text);
 *   - verification re-derives the digest and re-checks the signature —
 *     ANY drift in entries, sha256, or signature is a rejection.
 *
 * THE TWO TRUSTED KEYS (both documented, no trust-on-first-use):
 *   1. `cto-drs-release-1` — the release key pinned in
 *      [DrsPackageTrust.RELEASE_PUBKEY_HEX] (official dictionaries);
 *   2. `drs-local-export-1` — the device-local export keypair, seeded
 *      ONCE into `noBackupFilesDir/drs_export_key_v1.bin`. Its public
 *      key rides the file itself (there is no way to know another
 *      device's local key — and the honest limitation is documented:
 *      this proves integrity-after-signing, NOT trusted origin against
 *      the device owner).
 *
 * WHY FREE IMPORT STAYS DEFERRED (ADR-0009 decision 2): an unsigned
 * dictionary file crossing shared storage is a poisoned-lexicon
 * injection vector — thousands of entries crafted to skew prediction.
 * The unsigned and the tampered are ALWAYS rejected with distinct
 * messages; the future free-import-with-detailed-manual-confirm flow
 * is documented in the ADR, not coded here.
 *
 * Pure JVM core (the only Android seam is the seed file location).
 */
object DrsLexiconExchange {

    const val SCHEMA = "drs-lexicon-exchange-1"
    const val VERSION = 1
    const val LOCAL_KEY_ID = "drs-local-export-1"

    /** The export seed rides noBackupFilesDir — never backed up, never networked. */
    const val EXPORT_KEY_FILE = "drs_export_key_v1.bin"
    const val EXPORT_KEY_BYTES = 32

    @Serializable
    data class Entry(
        @SerialName("word") val word: String,
        @SerialName("vocalized") val vocalized: String,
        @SerialName("pinned") val pinned: Boolean = false,
        @SerialName("created_at") val createdAtMs: Long = 0,
        @SerialName("updated_at") val updatedAtMs: Long = 0,
    )

    @Serializable
    data class Envelope(
        @SerialName("schema") val schema: String,
        @SerialName("version") val version: Int,
        @SerialName("exported_at") val exportedAt: Long,
        @SerialName("entries") val entries: List<Entry>,
        @SerialName("sha256") val sha256: String,
        @SerialName("signature") val signature: String,
        @SerialName("pubkey_id") val pubkeyId: String,
        /** Present only for the local export key (the release key is pinned in code). */
        @SerialName("pubkey") val pubkey: String? = null,
    )

    /** Per-entry honest import report. */
    data class RejectedEntry(val word: String, val error: DrsMyLexicon.Error)

    data class Report(
        val accepted: Int,
        val skippedDuplicates: Int,
        val rejected: List<RejectedEntry>,
        val state: DrsState,
    )

    /** Distinct, localizable import outcomes — the owner-mandated pair is Unsigned vs Tampered. */
    sealed interface ImportResult {
        data class Verified(val envelope: Envelope) : ImportResult
        /** Missing/blank signature fields — or a local-key file that carries no usable public key. */
        object Unsigned : ImportResult
        /** A signature present but failing verification — or a digest that no longer matches the entries. */
        object Tampered : ImportResult
        /** A key id this build does not trust. */
        object UnknownKey : ImportResult
        /** Not the exchange schema / not parseable. */
        object BadFormat : ImportResult
    }

    // ------------------------------------------------------------ export

    /**
     * Builds the signed envelope. Returns null honestly when there is
     * nothing to export (an empty lexicon) or no export key — the UI
     * says so instead of writing an empty file.
     */
    fun buildEnvelope(
        entries: List<DrsMyLexiconEntry>,
        exportedAt: Long,
        seed: ByteArray?,
    ): Envelope? {
        if (seed == null || seed.size != EXPORT_KEY_BYTES) return null
        if (entries.isEmpty()) return null
        val exchangeEntries = entries.map {
            Entry(
                word = it.word,
                vocalized = it.vocalized,
                pinned = it.pinned,
                createdAtMs = it.createdAtMs,
                updatedAtMs = it.updatedAtMs,
            )
        }
        val digest = digestOf(exchangeEntries)
        val signature = DrsEd25519.sign(seed, digest)
        return Envelope(
            schema = SCHEMA,
            version = VERSION,
            exportedAt = exportedAt,
            entries = exchangeEntries,
            sha256 = digest.toHex(),
            signature = signature.toHex(),
            pubkeyId = LOCAL_KEY_ID,
            pubkey = DrsEd25519.publicKeyFromSeed(seed).toHex(),
        )
    }

    /** Serializes the envelope to the exchange text (one line of JSON). */
    fun serialize(envelope: Envelope): String = json.encodeToString(envelope)

    // ------------------------------------------------------------ import

    /** Parses and verifies — fail-closed at every step, never throws. */
    fun parseAndVerify(text: String): ImportResult {
        val envelope = try {
            json.decodeFromString<Envelope>(text)
        } catch (_: Throwable) {
            return ImportResult.BadFormat
        }
        if (envelope.schema != SCHEMA) return ImportResult.BadFormat
        if (envelope.signature.isBlank() || envelope.pubkeyId.isBlank()) return ImportResult.Unsigned

        val pubkey: ByteArray = when (envelope.pubkeyId) {
            DrsPackageTrust.RELEASE_KEY_ID -> DrsPackageTrust.pinnedReleasePubkey()
            LOCAL_KEY_ID -> envelope.pubkey?.hexToBytes() ?: return ImportResult.BadFormat
            else -> return ImportResult.UnknownKey
        }
        if (pubkey.size != EXPORT_KEY_BYTES) return ImportResult.Tampered

        val digest = envelope.sha256.hexToBytes() ?: return ImportResult.Tampered
        if (digest.size != 32) return ImportResult.Tampered
        val signature = envelope.signature.hexToBytes() ?: return ImportResult.Tampered

        // the signature covers the raw digest (the content hash)
        if (!DrsEd25519.verify(pubkey, digest, signature)) return ImportResult.Tampered

        // the digest must still match the entries riding the file
        val recomputed = digestOf(envelope.entries)
        if (!recomputed.contentEquals(digest)) return ImportResult.Tampered

        return ImportResult.Verified(envelope)
    }

    /**
     * Applies verified entries to the lexicon state — entry by entry
     * through [DrsMyLexicon.addAt] (the SAME validation the manual teach
     * dialog obeys), with an honest per-entry report. Re-importing the
     * same file skips everything (one entry per word key — never
     * duplicates).
     */
    fun importInto(state: DrsState, entries: List<Entry>, nowMs: Long): Report {
        var current = state
        var accepted = 0
        var skipped = 0
        val rejected = mutableListOf<RejectedEntry>()
        for (entry in entries) {
            if (current.myLexicon.any { it.word == entry.word }) {
                skipped += 1
                continue
            }
            when (val result = DrsMyLexicon.addAt(current, entry.word, entry.vocalized, nowMs)) {
                is DrsMyLexicon.Result.Accepted -> {
                    current = result.state
                    accepted += 1
                }
                is DrsMyLexicon.Result.Rejected ->
                    rejected += RejectedEntry(entry.word, result.error)
            }
        }
        return Report(accepted, skipped, rejected, current)
    }

    // -------------------------------------------------------- key helper

    /**
     * The device-local export keypair — seeded once, read forever. Pure
     * (injectable random); the android() factory wires the real file.
     */
    class ExportKey(private val file: File, random: java.util.Random = java.security.SecureRandom()) {

        private val rng: java.util.Random = random
        private var cached: ByteArray? = null

        /** Returns the 32-byte seed, creating it exactly once when absent. */
        @Synchronized
        fun ensure(): ByteArray? {
            cached?.let { return it }
            return try {
                if (file.exists()) {
                    val bytes = file.readBytes()
                    if (bytes.size != EXPORT_KEY_BYTES) return null
                    bytes.also { cached = it }
                } else {
                    val bytes = ByteArray(EXPORT_KEY_BYTES)
                    rng.nextBytes(bytes)
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                    bytes.also { cached = it }
                }
            } catch (_: Throwable) {
                null
            }
        }

        /** Android factory: the seed lives in noBackupFilesDir. */
        companion object {
            fun android(context: android.content.Context): ExportKey =
                ExportKey(File(context.applicationContext.noBackupFilesDir, EXPORT_KEY_FILE))
        }
    }

    // ------------------------------------------------------- internals

    internal fun digestOf(entries: List<Entry>): ByteArray =
        MessageDigest.getInstance("SHA-256")
            .digest(json.encodeToString(entries).toByteArray(Charsets.UTF_8))

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        if (isEmpty()) return null
        if (!all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) return null
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
