/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.privacy

/**
 * DRS roadmap v1.4.x security wave («الوجه الأمني»): the LIVE half of
 * the provable network block. The static half is the CI canary
 * (ci/privacy_canary.sh) plus the manifest contract tests; the runtime
 * half until now was a one-shot attestation computed when the privacy
 * dashboard opened. This engine turns that snapshot into a continuous,
 * self-refreshing proof the user can watch: byte counters that tick
 * every second while the screen is visible, and a "zero-byte streak"
 * that can only grow while OUR code stays silent — the v1.4.x KPI of
 * «إثبات حي في الواجهة» (a live proof in the UI), not a slogan.
 *
 * Mechanics (honest by construction):
 *  - The engine samples the same per-UID TrafficStats counters the
 *    sentinel uses (our UID = this app's process). Counters are
 *    cumulative-since-boot and ROM-dependent; the engine reports
 *    DELTAS only — against the process baseline (captured at app
 *    start) and against the previous tick.
 *  - PROVEN_ZERO requires the counters to be supported AND every
 *    delta to be exactly zero. Anything else is reported as MOVED
 *    with the honest numbers, or UNSUPPORTED when the ROM cannot
 *    prove anything — never a zero it cannot back.
 *  - The zero-byte streak (zeroStreakMs) is the wall-clock age of the
 *    current silent window: it starts at the first observation and
 *    restarts whenever bytes move OR the OS resets its counters
 *    (counterResetObserved — a reboot or a ROM counter wrap would
 *    otherwise fake an unbroken silence).
 *
 * The engine is pure: injectable sampler and clock, no Android imports
 * (TrafficSnapshot lives on the sentinel). The UI owns the 1-second
 * ticker loop; the engine owns the semantics.
 */
class DrsLiveProofEngine(
    private val sample: () -> DrsNetworkSentinel.TrafficSnapshot,
    private val clock: () -> Long,
    private val baselineOverride: DrsNetworkSentinel.TrafficSnapshot? = null,
) {

    /** The three honest states the live panel can render. */
    enum class Verdict {

        /** Counters supported and every delta exactly zero — the only provable silence. */
        PROVEN_ZERO,

        /** Bytes moved (since baseline or since the previous tick) — numbers shown as-is. */
        MOVED,

        /** The ROM reports UNSUPPORTED counters — the proof is delegated to the CI canary. */
        UNSUPPORTED,
    }

    /**
     * One tick of the live proof. Immutable; the UI swaps it in once
     * per interval. [zeroStreakMs] is meaningful only under
     * [Verdict.PROVEN_ZERO] (under MOVED it is 0 — silence just ended).
     */
    data class LiveProof(
        val verdict: Verdict,
        val absoluteMode: Boolean,
        val rxSinceBaseline: Long,
        val txSinceBaseline: Long,
        val rxSincePrevious: Long,
        val txSincePrevious: Long,
        val zeroStreakMs: Long,
        val observedMs: Long,
        val ticks: Long,
        val countersSupported: Boolean,
        val counterResetObserved: Boolean,
        val checkedAt: Long,
    )

    private var first: DrsNetworkSentinel.TrafficSnapshot? = null
    private var previous: DrsNetworkSentinel.TrafficSnapshot? = null
    private var firstObservedAt: Long? = null

    /** Epoch ms of the observation that started the CURRENT silent window. */
    private var silentSince: Long? = null
    private var ticks: Long = 0L

    /**
     * Takes one sample and folds it into the running proof.
     * Baseline precedence matches the sentinel: an explicitly passed
     * [baselineOverride] (the process-start snapshot) wins; otherwise
     * the FIRST sample this engine observed becomes the baseline, so
     * repeated ticks never report deltas against `now` — which would
     * fake a zero.
     */
    @Synchronized
    fun tick(absoluteMode: Boolean): LiveProof {
        val now = sample()
        val nowMs = clock()
        ticks += 1
        if (first == null) first = now
        if (firstObservedAt == null) firstObservedAt = nowMs
        val base = baselineOverride ?: first ?: now
        val prev = previous
        val dRxBase = (now.rxBytes - base.rxBytes).coerceAtLeast(0L)
        val dTxBase = (now.txBytes - base.txBytes).coerceAtLeast(0L)

        if (prev == null) {
            // First observation of this engine instance: the silent
            // window starts now with a zero-age streak — it can only
            // grow from the next tick on.
            silentSince = nowMs
            previous = now
            return LiveProof(
                verdict = verdictOf(now.supported, dRxBase, dTxBase, movedNow = false),
                absoluteMode = absoluteMode,
                rxSinceBaseline = dRxBase,
                txSinceBaseline = dTxBase,
                rxSincePrevious = 0L,
                txSincePrevious = 0L,
                zeroStreakMs = 0L,
                observedMs = 0L,
                ticks = ticks,
                countersSupported = now.supported,
                counterResetObserved = false,
                checkedAt = nowMs,
            )
        }

        val dRxPrev = (now.rxBytes - prev.rxBytes).coerceAtLeast(0L)
        val dTxPrev = (now.txBytes - prev.txBytes).coerceAtLeast(0L)
        val reset = now.rxBytes < prev.rxBytes || now.txBytes < prev.txBytes
        val movedNow = dRxPrev > 0L || dTxPrev > 0L
        val streak = if (movedNow || reset) 0L else (nowMs - (silentSince ?: nowMs)).coerceAtLeast(0L)
        if (movedNow || reset) silentSince = nowMs
        previous = now
        return LiveProof(
            verdict = verdictOf(now.supported, dRxBase, dTxBase, movedNow),
            absoluteMode = absoluteMode,
            rxSinceBaseline = dRxBase,
            txSinceBaseline = dTxBase,
            rxSincePrevious = dRxPrev,
            txSincePrevious = dTxPrev,
            zeroStreakMs = streak,
            observedMs = (nowMs - (firstObservedAt ?: nowMs)).coerceAtLeast(0L),
            ticks = ticks,
            countersSupported = now.supported,
            counterResetObserved = reset,
            checkedAt = nowMs,
        )
    }

    private fun verdictOf(
        supported: Boolean,
        dRxBase: Long,
        dTxBase: Long,
        movedNow: Boolean,
    ): Verdict = when {
        !supported -> Verdict.UNSUPPORTED
        dRxBase > 0L || dTxBase > 0L || movedNow -> Verdict.MOVED
        else -> Verdict.PROVEN_ZERO
    }

    companion object {

        /** Android factory: the real per-UID sampler + the wall clock. */
        fun android(
            baseline: DrsNetworkSentinel.TrafficSnapshot? = DrsNetworkSentinel.baseline(),
            clock: () -> Long = System::currentTimeMillis,
        ): DrsLiveProofEngine = DrsLiveProofEngine(
            sample = DrsNetworkSentinel::readSnapshot,
            clock = clock,
            baselineOverride = baseline,
        )

        /**
         * Human byte formatting for the live panel — decimal (SI)
         * units, matching the dashboard's existing convention. Pure so
         * the tests pin the boundaries the user will read.
         */
        fun formatBytes(bytes: Long): String = when {
            bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000L -> "%d KB".format(bytes / 1_000)
            else -> "$bytes B"
        }

        /**
         * Human duration for the zero-byte streak — m:ss below one
         * hour, h:mm:ss above. Pure; tested at the boundaries.
         */
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1_000L
            val hours = totalSeconds / 3_600L
            val minutes = (totalSeconds % 3_600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }
}
