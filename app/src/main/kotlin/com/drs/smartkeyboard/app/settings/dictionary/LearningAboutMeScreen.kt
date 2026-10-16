/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.app.settings.dictionary

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.drs.smartkeyboard.R
import com.drs.smartkeyboard.drs.DrsLexiconExchange
import com.drs.smartkeyboard.drs.DrsMyLexicon
import com.drs.smartkeyboard.drs.DrsStore
import com.drs.smartkeyboard.drs.ai.DrsCorrectionCounters
import com.drs.smartkeyboard.drs.ai.DrsLearningEngine
import com.drs.smartkeyboard.lib.compose.DrsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drs.jetpref.datastore.ui.Preference
import org.drs.jetpref.datastore.ui.PreferenceGroup
import org.drs.lib.compose.stringRes
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 2): the «ماذا تعلمت عني»
 * screen — the visual layer over the EXISTING learning engines:
 *
 *  - «كلماتي» (DrsMyLexicon) searchable and in the canonical order —
 *    the same pure contracts the manager uses, no new algorithm;
 *  - the correction counters (DrsCorrectionCounters) — counting from
 *    NOW, honestly: no retroactive history exists, and the screen says
 *    so literally;
 *  - the learning summary (DrsLearningEngine size/rows) — what the
 *    personal tables actually hold;
 *  - SIGNED export (ed25519, the flex structure) and signed-ONLY
 *    import — unsigned and tampered files are rejected with distinct
 *    messages;
 *  - «امسح كل ما تعلمته» — double-confirmed, real deletion across the
 *    learning engine, the personal lexicon, and the counters (the
 *    transparency log is NOT learning — it lives in the hub).
 */
@Composable
fun LearningAboutMeScreen() = DrsScreen {
    title = stringRes(R.string.learned__title)
    previewFieldVisible = false

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drsState by DrsStore.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var counters by remember { mutableStateOf<DrsCorrectionCounters.State?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showEraseFirst by remember { mutableStateOf(false) }
    var showEraseSecond by remember { mutableStateOf(false) }

    fun refreshCounters() {
        scope.launch {
            counters = withContext(Dispatchers.IO) {
                DrsCorrectionCounters.android(context).read()
            }
        }
    }

    LaunchedEffect(Unit) { refreshCounters() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = pendingExport
        if (uri != null && text != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(text.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    context.getString(
                        if (ok) R.string.learned__export__done else R.string.learned__export__failed,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        pendingExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes().toString(Charsets.UTF_8)
                        }
                    }.getOrNull()
                }
                when (val result = text?.let { DrsLexiconExchange.parseAndVerify(it) }) {
                    null ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    is DrsLexiconExchange.ImportResult.Verified -> {
                        val report = DrsLexiconExchange.importInto(
                            state = DrsStore.state.value,
                            entries = result.envelope.entries,
                            nowMs = System.currentTimeMillis(),
                        )
                        if (report.accepted > 0) {
                            DrsStore.update { report.state }
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__done)
                                .format(report.accepted, report.skippedDuplicates, report.rejected.size),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    DrsLexiconExchange.ImportResult.Unsigned ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__reject_unsigned),
                            Toast.LENGTH_LONG,
                        ).show()
                    DrsLexiconExchange.ImportResult.Tampered ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__reject_tampered),
                            Toast.LENGTH_LONG,
                        ).show()
                    DrsLexiconExchange.ImportResult.UnknownKey ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__reject_unknown_key),
                            Toast.LENGTH_LONG,
                        ).show()
                    DrsLexiconExchange.ImportResult.BadFormat ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__import__reject_format),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    content {
        // --- the correction counters (counting starts now — honestly) ---
        PreferenceGroup(title = stringRes(R.string.learned__counters_group)) {
            val c = counters
            Preference(
                icon = Icons.Default.School,
                title = if (c == null || c.autoCommits == 0L) {
                    stringRes(R.string.learned__counters_none)
                } else {
                    stringRes(R.string.learned__counters_value).format(c.autoCommits)
                },
                summary = if (c != null && c.startedAt != 0L) {
                    stringRes(R.string.learned__counters_started_at)
                        .format(formatDate(c.startedAt))
                } else {
                    stringRes(R.string.learned__counters_from_now_note)
                },
            )
        }

        // --- what the personal tables hold ------------------------------
        PreferenceGroup(title = stringRes(R.string.learned__tables_group)) {
            Preference(
                icon = Icons.Default.School,
                title = stringRes(R.string.learned__tables_summary).format(
                    DrsLearningEngine.size(),
                    DrsLearningEngine.bigramRowCount(),
                    DrsLearningEngine.trigramRowCount(),
                ),
                summary = stringRes(R.string.learned__tables_note),
            )
        }

        // --- «كلماتي» searchable + ordered --------------------------------
        PreferenceGroup(title = stringRes(R.string.learned__lexicon_group)) {
            val entries = DrsMyLexicon.order(DrsMyLexicon.search(drsState.myLexicon, query))
            Preference(
                icon = Icons.Default.Search,
                title = stringRes(R.string.learned__lexicon_count)
                    .format(drsState.myLexicon.size, DrsMyLexicon.MAX_ITEMS),
                summary = stringRes(R.string.learned__lexicon_note),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.learned__lexicon_search_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (entries.isEmpty()) {
                Preference(title = stringRes(R.string.learned__lexicon_empty))
            }
            for (entry in entries.take(50)) {
                Preference(
                    title = entry.word,
                    summary = if (entry.pinned) {
                        stringRes(R.string.learned__lexicon_entry_pinned).format(entry.vocalized)
                    } else {
                        entry.vocalized
                    },
                )
            }
            if (entries.size > 50) {
                Preference(title = stringRes(R.string.learned__lexicon_truncated).format(entries.size - 50))
            }
        }

        // --- signed exchange ----------------------------------------------
        PreferenceGroup(title = stringRes(R.string.learned__exchange_group)) {
            Preference(
                icon = Icons.Default.FileDownload,
                title = stringRes(R.string.learned__export),
                summary = stringRes(R.string.learned__export__summary),
                onClick = {
                    scope.launch {
                        val text = withContext(Dispatchers.IO) {
                            val seed = DrsLexiconExchange.ExportKey.android(context).ensure()
                            DrsLexiconExchange.buildEnvelope(
                                entries = DrsStore.state.value.myLexicon,
                                exportedAt = System.currentTimeMillis(),
                                seed = seed,
                            )?.let { DrsLexiconExchange.serialize(it) }
                        }
                        if (text == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.learned__export__empty),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            pendingExport = text
                            exportLauncher.launch(suggestedExportName())
                        }
                    }
                },
            )
            Preference(
                icon = Icons.Default.FileUpload,
                title = stringRes(R.string.learned__import),
                summary = stringRes(R.string.learned__import__summary),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
        }

        // --- the double-confirmed wipe -------------------------------------
        PreferenceGroup(title = stringRes(R.string.learned__erase_group)) {
            Preference(
                icon = Icons.Default.DeleteForever,
                title = stringRes(R.string.learned__erase),
                summary = stringRes(R.string.learned__erase__summary),
                onClick = { showEraseFirst = true },
            )
        }
    }

    // --- dialogs: erase is double-confirmed, never one slip ---------------
    if (showEraseFirst) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEraseFirst = false },
            title = { Text(stringResource(R.string.learned__erase_confirm_first_title)) },
            text = { Text(stringResource(R.string.learned__erase_confirm_first_body)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showEraseFirst = false
                    showEraseSecond = true
                }) { Text(stringResource(R.string.learned__erase_next)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEraseFirst = false }) {
                    Text(stringResource(R.string.action__cancel))
                }
            },
        )
    }
    if (showEraseSecond) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEraseSecond = false },
            title = { Text(stringResource(R.string.learned__erase_confirm_second_title)) },
            text = { Text(stringResource(R.string.learned__erase_confirm_second_body)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showEraseSecond = false
                    scope.launch {
                        withContext(Dispatchers.IO) { performWipe(context) }
                        refreshCounters()
                        Toast.makeText(
                            context,
                            context.getString(R.string.learned__erase_done),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.learned__erase_confirm_word)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEraseSecond = false }) {
                    Text(stringResource(R.string.action__cancel))
                }
            },
        )
    }
}

/**
 * The real wipe: the learning engine's file, the personal lexicon, and
 * the correction counters. The transparency log is NOT learning — it
 * stays in its own hub (ADR-0009). The dashboard reflects everything
 * immediately because its probes measure live.
 */
private fun performWipe(context: Context) {
    // the same file the dashboard's STORE_LEARNING probe destroys
    DrsLearningEngine.clear(
        File(context.applicationContext.noBackupFilesDir, "drs_learning_v1.json"),
    )
    DrsMyLexicon.removeAll()
    DrsCorrectionCounters.android(context).reset()
}

private fun formatDate(epochMs: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))

private fun suggestedExportName(): String =
    "drs-my-lexicon-" +
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.now()) +
        ".json"
