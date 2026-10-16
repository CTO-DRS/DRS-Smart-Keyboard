/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.privacy

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 1): the local persistence
 * layer of the live transparency hub.
 *
 * WHAT it stores (local only, noBackupFilesDir — never backed up,
 * never leaves the device, zero network by construction):
 *  - `firstUseAt`: a one-time timestamp written on the first process
 *    start after installation — the anchor of the age certificate;
 *  - daily attestation snapshots: one row per UTC day holding the
 *    byte deltas recorded that day (rx/tx), the last attestation time,
 *    and the honesty flags (`countersSupported`, `zeroTraffic`).
 *
 * WHAT it NEVER stores: field contents, keystrokes, destinations,
 * hostnames — the sentinel measures OUR per-UID counters only, so the
 * log can only ever say "this app moved X bytes" or "this app moved
 * nothing" — never "about what".
 *
 * DISCIPLINE (the ADR-0009 rotation decision):
 *  - hard 90-day rotation: when a new day is recorded, days older than
 *    the newest-90 window are dropped — a memory-bounded file that
 *    stays in the kilobytes forever, and a privacy bound (a longer log
 *    preserves device usage patterns in time for no additional proof);
 *  - atomic persistence (tmp + rename — the `DrsLearningEngine.persist`
 *    pattern), bounded (over-cap file is discarded, not trusted), and
 *    corruption-safe (a damaged file is thrown away, logging restarts
 *    clean);
 *  - HONESTY: when the ROM reports unsupported counters the day row
 *    says so explicitly — it never fabricates a zero it cannot prove;
 *    `zeroTraffic` is TRUE only when the counters are supported AND the
 *    recorded day totals are exactly zero.
 *
 * Core is pure (injectable file + clock, JVM-tested); the android()
 * factory wires the real file. The [Recorder] companion is the ONLY
 * wiring point — app-scoped (DrsApplication.onCreate + the hub screen),
 * zero code in the IME service by construction (structural tests guard
 * this).
 */
class DrsTransparencyLog(
    private val file: File,
    private val now: () -> Long,
) {

    /** One UTC day of recorded evidence. */
    @Serializable
    data class DaySnapshot(
        val day: Long,
        val lastAttestedAt: Long,
        val rxTotal: Long,
        val txTotal: Long,
        val countersSupported: Boolean,
        val zeroTraffic: Boolean,
    )

    /** The whole store: schema + the one-time first-use anchor + the days. */
    @Serializable
    data class State(
        val schema: String = SCHEMA,
        val firstUseAt: Long = 0L,
        val days: List<DaySnapshot> = emptyList(),
    )

    /**
     * Writes [firstUseAt] exactly once (subsequent calls never move it).
     * Returns the anchor. After an explicit [clear] the store is empty
     * by the user's own destructive choice — the anchor legitimately
     * restarts (the certificate documents from its stored evidence).
     */
    @Synchronized
    fun ensureFirstUse(): Long {
        val state = readInternal()
        if (state.firstUseAt != 0L) return state.firstUseAt
        val stamped = state.copy(firstUseAt = now())
        persist(stamped)
        return stamped.firstUseAt
    }

    /**
     * Records one attestation into its UTC day: the since-previous
     * deltas accumulate (multiple recordings per day add up — the hub's
     * open + the process start both land here), unsupported counters
     * are AND-aggregated for the day, and the 90-day rotation drops
     * whatever fell out of the window.
     */
    @Synchronized
    fun record(
        rxSincePrevious: Long,
        txSincePrevious: Long,
        countersSupported: Boolean,
        attestedAt: Long,
    ) {
        val state = readInternal()
        val today = now() / DAY_MS
        val days = state.days.toMutableList()
        val idx = days.indexOfFirst { it.day == today }
        if (idx >= 0) {
            val prev = days[idx]
            val rx = prev.rxTotal + rxSincePrevious.coerceAtLeast(0L)
            val tx = prev.txTotal + txSincePrevious.coerceAtLeast(0L)
            val supported = prev.countersSupported && countersSupported
            days[idx] = DaySnapshot(
                day = today,
                lastAttestedAt = attestedAt,
                rxTotal = rx,
                txTotal = tx,
                countersSupported = supported,
                zeroTraffic = supported && rx == 0L && tx == 0L,
            )
        } else {
            val rx = rxSincePrevious.coerceAtLeast(0L)
            val tx = txSincePrevious.coerceAtLeast(0L)
            days.add(
                DaySnapshot(
                    day = today,
                    lastAttestedAt = attestedAt,
                    rxTotal = rx,
                    txTotal = tx,
                    countersSupported = countersSupported,
                    zeroTraffic = countersSupported && rx == 0L && tx == 0L,
                ),
            )
        }
        val rotated = days
            .sortedBy { it.day }
            .filter { it.day >= today - (MAX_DAYS - 1) }
        persist(state.copy(days = rotated))
    }

    /** Reads the current state; a missing/damaged/over-cap file reads fresh. */
    @Synchronized
    fun read(): State = readInternal()

    /** Destroys the store (the dashboard probe and the era's honesty both require a real delete). */
    @Synchronized
    fun clear() {
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

    /** Atomic persist (tmp + rename); never throws — transparency logging is best-effort by design. */
    private fun persist(state: State) {
        try {
            val payload = json.encodeToString(state).toByteArray(Charsets.UTF_8)
            if (payload.size > FILE_BYTES_CAP) return
            val parent = file.parentFile ?: return
            parent.mkdirs()
            val tmp = File(parent, file.name + ".tmp")
            tmp.writeBytes(payload)
            if (!tmp.renameTo(file)) {
                file.writeBytes(payload)
                tmp.delete()
            }
        } catch (_: Throwable) {
            // best-effort by design — never break the caller over the log
        }
    }

    companion object {

        const val FILE_NAME = "drs_transparency_log_v1.json"
        const val SCHEMA = "drs-transparency-log-1"

        /** ADR-0009 decision 3: hard 90-day rotation — memory bound + privacy bound. */
        const val MAX_DAYS = 90

        /** Bounded like every other DRS store (the log stays in kilobytes forever). */
        const val FILE_BYTES_CAP = 64 * 1024

        private const val DAY_MS = 86_400_000L

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun file(context: Context): File = File(context.applicationContext.noBackupFilesDir, FILE_NAME)

        fun android(context: Context): DrsTransparencyLog =
            DrsTransparencyLog(file(context), now = System::currentTimeMillis)

        /** Pure static destroy for the dashboard probe (same pattern as DrsLearningEngine.clear(file)). */
        fun clear(file: File) {
            file.delete()
        }
    }

    /**
     * The process-scoped recorder — the ONLY writer of the log, wired
     * app-scoped (DrsApplication.onCreate + the hub screen). It holds
     * ONE process sentinel instance so `sincePrevious` deltas stay
     * honest across recordings (no new measurement engine: the same
     * [DrsNetworkSentinel] the dashboard uses, reused as the delta
     * source). All disk work happens off the main thread.
     */
    object Recorder {

        private val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "drs-transparency").apply { isDaemon = true }
        }

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var sentinel: DrsNetworkSentinel? = null

        /** Called once from DrsApplication.onCreate: stamps firstUseAt + records the process-start attestation. */
        fun start(context: Context) {
            appContext = context.applicationContext
            executor.execute { snapshotAndRecord() }
        }

        /**
         * Records a fresh attestation through the process sentinel and
         * returns the updated state — synchronous so callers already on
         * a background dispatcher (the hub screen refresh) read back
         * consistent numbers. Returns null only if [start] never ran.
         */
        @Synchronized
        fun snapshotAndRecord(): DrsTransparencyLog.State? {
            val context = appContext ?: return null
            val engine = sentinel ?: DrsNetworkSentinel.android(context).also { sentinel = it }
            val attestation = engine.attestation(
                absoluteMode = false,
                baseline = null,
                permissionsNow = emptyList(),
            )
            val core = DrsTransparencyLog.android(context)
            core.ensureFirstUse()
            core.record(
                rxSincePrevious = attestation.rxSincePrevious,
                txSincePrevious = attestation.txSincePrevious,
                countersSupported = attestation.countersSupported,
                attestedAt = attestation.checkedAt,
            )
            return core.read()
        }
    }
}
