/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ai

import android.content.Context
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 2): the correction counters —
 * the honest «العد من الآن» contract.
 *
 * WHAT they count: silently auto-committed corrections that were genuinely
 * rewritten — [onAutoCommit] is called ONLY from the two declared arm
 * sites in `KeyboardManager` (lines 904/1606, the owner-approved declared
 * exception to «zero IME code», decision 1 of ADR-0009) and increments
 * ONLY when `DrsCorrectionRevert.arm` actually armed (arm refuses the
 * false correction where committed == typed — counting the refusal would
 * be dishonest inflation).
 *
 * WHAT they never claim: history. There is NO retroactive field in the
 * model — by construction (no field, no backfill, no estimate). The
 * anchor `startedAt` is stamped at the FIRST counted correction, and the
 * screen says literally: counting starts now.
 *
 * Reverts are deliberately NOT counted here: the revert execution path
 * lives deeper in `KeyboardManager` and counting it would need a THIRD
 * IME line — outside the approved two-line exception (documented; a
 * future option requiring a new owner decision, never improvisation).
 *
 * Discipline (the established store pattern): JSON in
 * `noBackupFilesDir`, atomic persistence (tmp + rename), bounded, and
 * corruption-safe (a damaged file reads fresh — the counters restart
 * honestly at zero, they never fabricate a carried-over number).
 * Core is pure (injectable file + clock, JVM-tested).
 */
class DrsCorrectionCounters(
    private val file: File,
    private val now: () -> Long = System::currentTimeMillis,
) {

    @Serializable
    data class State(
        val schema: String = SCHEMA,
        /** The first counted correction — the «counting starts now» anchor. Zero until then. */
        val startedAt: Long = 0L,
        /** Silently auto-committed corrections actually rewritten (armed) since the anchor. */
        val autoCommits: Long = 0L,
    )

    /** Counts one auto-commit path — increments ONLY when the revert actually armed. */
    @Synchronized
    fun onAutoCommit(armed: Boolean) {
        if (!armed) return
        val state = readInternal()
        val startedAt = if (state.startedAt == 0L) now() else state.startedAt
        persist(state.copy(startedAt = startedAt, autoCommits = state.autoCommits + 1L))
    }

    @Synchronized
    fun read(): State = readInternal()

    /** The wipe button's real delete — counters restart from a fresh, honest zero. */
    @Synchronized
    fun reset() {
        file.delete()
    }

    // ------------------------------------------------------- internals

    private fun readInternal(): State {
        if (!file.exists()) return State()
        val bytes = try {
            file.readBytes()
        } catch (_: Throwable) {
            return State()
        }
        if (bytes.size > FILE_BYTES_CAP) return State()
        return try {
            val decoded = json.decodeFromString<State>(bytes.toString(Charsets.UTF_8))
            if (decoded.schema != SCHEMA) State() else decoded
        } catch (_: Throwable) {
            State()
        }
    }

    private fun persist(state: State) {
        try {
            val payload = json.encodeToString(state).toByteArray(Charsets.UTF_8)
            val parent = file.parentFile ?: return
            parent.mkdirs()
            val tmp = File(parent, file.name + ".tmp")
            tmp.writeBytes(payload)
            if (!tmp.renameTo(file)) {
                file.writeBytes(payload)
                tmp.delete()
            }
        } catch (_: Throwable) {
            // best-effort by design — counting never breaks the typing path
        }
    }

    companion object {

        const val FILE_NAME = "drs_correction_counters_v1.json"
        const val SCHEMA = "drs-correction-counters-1"
        const val FILE_BYTES_CAP = 8 * 1024

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun file(context: Context): File = File(context.applicationContext.noBackupFilesDir, FILE_NAME)

        fun android(context: Context): DrsCorrectionCounters =
            DrsCorrectionCounters(file(context))

        /** Pure static destroy for the dashboard probe. */
        fun clear(file: File) {
            file.delete()
        }

        /**
         * The process-scoped instance for the two declared arm-site
         * counter lines in KeyboardManager (ADR-0009, owner decision 1)
         * — one store per process, created lazily off the hot path.
         */
        @Volatile
        private var processInstance: DrsCorrectionCounters? = null

        fun forProcess(context: Context): DrsCorrectionCounters =
            processInstance ?: DrsCorrectionCounters(file(context)).also { processInstance = it }
    }
}
