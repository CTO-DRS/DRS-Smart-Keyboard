/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.app.settings.privacy

import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.drs.smartkeyboard.BuildConfig
import com.drs.smartkeyboard.R
import com.drs.smartkeyboard.drs.privacy.DrsAgeCertificate
import com.drs.smartkeyboard.drs.privacy.DrsCertificatePng
import com.drs.smartkeyboard.drs.privacy.DrsNetworkSentinel
import com.drs.smartkeyboard.drs.privacy.DrsPrivacyLock
import com.drs.smartkeyboard.drs.privacy.DrsTransparencyLog
import com.drs.smartkeyboard.lib.compose.DrsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drs.jetpref.datastore.ui.Preference
import org.drs.jetpref.datastore.ui.PreferenceGroup
import org.drs.lib.compose.stringRes

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 1): the live transparency
 * hub. Three honest sections, every number read from an existing
 * engine — nothing here measures, stores, or claims anything new:
 *
 *  1. THE LIVE COUNTER — the same TrafficStats per-UID deltas the
 *     privacy screen renders (`DrsNetworkSentinel`), shown here as the
 *     hub's headline;
 *  2. THE CONNECTION LOG — the last recorded daily snapshots from
 *     `DrsTransparencyLog` (when / documented purpose / recorded
 *     size), 90-day rotated, opened by recording a fresh snapshot;
 *  3. THE AGE CERTIFICATE — computed by `DrsAgeCertificate` from the
 *     log's own data, rendered to a programmatic PNG (`Canvas` +
 *     pure encoder) and shared through the EXISTING FileProvider.
 *
 * The log itself is registered in `DrsPrivacyDashboard` as the
 * `transparency_log` store (the mandatory StoreProbe contract), so the
 * privacy screen always sees it, measures it, and can destroy it.
 */
@Composable
fun TransparencyHubScreen() = DrsScreen {
    title = stringRes(R.string.transparency__title)
    previewFieldVisible = false

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var attestation by remember { mutableStateOf<DrsNetworkSentinel.Attestation?>(null) }
    var logState by remember { mutableStateOf<DrsTransparencyLog.State?>(null) }
    var sharing by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            // one background hop: fresh attestation (display) + a recorded
            // snapshot (the log's "at least one per open" contract)
            val (att, state) = withContext(Dispatchers.IO) {
                val displaySentinel = DrsNetworkSentinel.android(context)
                val a = displaySentinel.attestation(
                    absoluteMode = DrsPrivacyLock.isAbsoluteMode(),
                    baseline = DrsNetworkSentinel.baseline(),
                )
                val s = DrsTransparencyLog.Recorder.snapshotAndRecord()
                Pair(a, s)
            }
            attestation = att
            logState = state
        }
    }

    LaunchedEffect(Unit) { refresh() }

    content {
        // --- 1) the live counter ---------------------------------------
        PreferenceGroup(title = stringRes(R.string.transparency__live_group)) {
            val att = attestation
            Preference(
                icon = Icons.Default.Visibility,
                title = when {
                    att == null -> stringRes(R.string.privacy__attestation__measuring)
                    !att.countersSupported -> stringRes(R.string.privacy__attestation__unsupported)
                    att.zeroTraffic -> stringRes(R.string.privacy__attestation__ok)
                    else -> stringRes(R.string.privacy__attestation__moved)
                        .format(att.rxSinceBaseline, att.txSinceBaseline)
                },
                summary = stringRes(R.string.transparency__live__summary),
            )
        }

        // --- 2) the connection log -------------------------------------
        PreferenceGroup(title = stringRes(R.string.transparency__log_group)) {
            val state = logState
            if (state == null || state.days.isEmpty()) {
                Preference(
                    icon = Icons.Default.History,
                    title = stringRes(R.string.transparency__log__empty_title),
                    summary = stringRes(R.string.transparency__log__empty_summary),
                )
            } else {
                Preference(
                    icon = Icons.Default.History,
                    title = stringRes(R.string.transparency__log__count)
                        .format(state.days.size, DrsTransparencyLog.MAX_DAYS),
                    summary = stringRes(R.string.transparency__log__purpose),
                )
                for (day in state.days.asReversed().take(14)) {
                    Preference(
                        title = stringRes(R.string.transparency__log__day)
                            .format(formatDay(day.day)),
                        summary = when {
                            !day.countersSupported ->
                                stringRes(R.string.transparency__log__unsupported_day)
                            day.zeroTraffic ->
                                stringRes(R.string.transparency__log__zero_day)
                            else ->
                                stringRes(R.string.transparency__log__day_traffic)
                                    .format(
                                        formatBytesShort(day.rxTotal),
                                        formatBytesShort(day.txTotal),
                                    )
                        },
                    )
                }
            }
        }

        // --- 3) the age certificate ------------------------------------
        PreferenceGroup(title = stringRes(R.string.transparency__cert_group)) {
            val state = logState
            val certificate = state?.let { DrsAgeCertificate.compute(it, System.currentTimeMillis()) }
            if (certificate == null || state == null) {
                Preference(
                    icon = Icons.Default.Verified,
                    title = stringRes(R.string.transparency__cert__no_data_title),
                    summary = stringRes(R.string.transparency__cert__no_data_summary),
                )
            } else {
                for (line in DrsAgeCertificate.rows(certificate, DrsCertificatePng.labels(context, certificate))) {
                    Preference(
                        icon = Icons.Default.Verified,
                        title = line,
                    )
                }
                Preference(
                    icon = Icons.Default.Share,
                    title = stringRes(R.string.transparency__cert__share),
                    summary = stringRes(R.string.transparency__cert__share_summary),
                    onClick = {
                        if (!sharing) {
                            sharing = true
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    DrsCertificatePng.render(context, certificate)
                                }
                                sharing = false
                                if (file == null) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.transparency__cert__share_failed),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    sharePng(context, file)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Shares the rendered certificate through the EXISTING FileProvider (approved cache-path resource). */
private fun sharePng(context: android.content.Context, file: java.io.File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider.file",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.transparency__cert__share)),
        )
    } catch (_: Throwable) {
        Toast.makeText(
            context,
            context.getString(R.string.transparency__cert__share_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

/** UTC epochDay → a neutral yyyy-MM-dd label. */
private fun formatDay(epochDay: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(java.time.ZoneId.of("UTC"))
        .format(java.time.Instant.ofEpochMilli(epochDay * 86_400_000L))

private fun formatBytesShort(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%d KB".format(bytes / 1_000)
    else -> "$bytes B"
}
