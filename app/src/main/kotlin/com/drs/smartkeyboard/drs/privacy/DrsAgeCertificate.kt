/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.privacy

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 1): the age certificate —
 * a PURE computation from the transparency log's own data. Nothing is
 * invented: every field is either the one-time `firstUseAt` anchor or
 * an aggregate over the recorded daily snapshots.
 *
 * The honest claims it can make (and the ones it refuses to make):
 *  - ageDays — how long the device has been documenting (from
 *    `firstUseAt`, never from a fabricated history);
 *  - attestedDays — how many UTC days actually carry recorded
 *    evidence (never padded, never extrapolated);
 *  - totalRxBytes/totalTxBytes — the sum of RECORDED deltas. The
 *    wording everywhere is "recorded bytes" — the log is per-process
 *    session evidence, and the certificate must never inflate that
 *    into a claim about bytes it did not see;
 *  - zeroBytesClaim — TRUE only when at least one day is attested AND
 *    every recorded delta is exactly zero AND the counters were
 *    supported throughout. An unsupported ROM cannot earn this claim
 *    by absence of data — absence of proof is not proof of zero;
 *  - countersAlwaysSupported — FALSE when any attested day reported
 *    unsupported counters (the certificate then says so instead of a
 *    zero it cannot prove).
 *
 * The anti-tampering question (signed image, QR, watermark) is
 * explicitly OUT OF SCOPE for this chapter (ADR-0009 decision 4) —
 * the certificate is a readable, honest disclosure rendered from
 * local data, never an anti-forgery document.
 */
object DrsAgeCertificate {

    private const val DAY_MS = 86_400_000L

    data class Certificate(
        val firstUseAt: Long,
        val ageDays: Long,
        val attestedDays: Int,
        val totalRxBytes: Long,
        val totalTxBytes: Long,
        val zeroBytesClaim: Boolean,
        val countersAlwaysSupported: Boolean,
        val generatedAt: Long,
    )

    /** Computes the certificate from the log state — pure, JVM-tested. */
    fun compute(state: DrsTransparencyLog.State, now: Long): Certificate {
        val totalRx = state.days.sumOf { it.rxTotal }
        val totalTx = state.days.sumOf { it.txTotal }
        val attested = state.days.size
        val allSupported = state.days.isNotEmpty() && state.days.all { it.countersSupported }
        val zero = attested > 0 && totalRx == 0L && totalTx == 0L && allSupported
        val age = if (state.firstUseAt <= 0L) {
            0L
        } else {
            ((now - state.firstUseAt).coerceAtLeast(0L)) / DAY_MS
        }
        return Certificate(
            firstUseAt = state.firstUseAt,
            ageDays = age,
            attestedDays = attested,
            totalRxBytes = totalRx,
            totalTxBytes = totalTx,
            zeroBytesClaim = zero,
            countersAlwaysSupported = allSupported,
            generatedAt = now,
        )
    }

    /**
     * The certificate's rendered rows in canonical order — pure so both
     * the PNG renderer and the on-screen card show the SAME lines.
     * [labels] carries the localized strings; the numbers are formatted
     * here so the tested path is exactly the rendered path.
     */
    data class Labels(
        val firstUseDate: String,
        val ageLine: String,
        val daysLine: String,
        val totalsLine: String,
        val zeroClaim: String,
        val movedClaim: String,
        val unsupportedNote: String,
        val honestyNote: String,
    )

    fun rows(certificate: Certificate, labels: Labels): List<String> {
        val rows = mutableListOf<String>()
        if (certificate.firstUseAt > 0L) {
            rows += labels.firstUseDate
            rows += labels.ageLine
        }
        rows += labels.daysLine
        rows += labels.totalsLine
        rows += if (certificate.zeroBytesClaim) labels.zeroClaim else labels.movedClaim
        if (!certificate.countersAlwaysSupported) rows += labels.unsupportedNote
        rows += labels.honestyNote
        return rows
    }
}
