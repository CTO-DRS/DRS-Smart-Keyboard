/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.privacy.DrsLiveProofEngine
import com.drs.smartkeyboard.drs.privacy.DrsNetworkSentinel
import com.drs.smartkeyboard.drs.privacy.DrsPrivacyLock
import com.drs.smartkeyboard.ime.voice.VoiceRecognizerMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * v1.4.x security wave («الوجه الأمني») — the security tests the
 * roadmap pinned to this wave, all pure JVM:
 *
 *  1. the LIVE proof engine (DrsLiveProofEngine): the ticking half of
 *     the provable network block — verdict semantics, the zero-byte
 *     streak, counter resets, baseline precedence, and the byte/
 *     duration formatting the dashboard renders;
 *  2. the EXPANDED manifest contract: the original P3 twin of the CI
 *     canary (permission allowlist + cleartext) widened to the full
 *     attack surface — exported-component allowlist, binding
 *     permissions, providers, backup/extraction rules, profileable,
 *     queries scope;
 *  3. the absolute-mode gate matrix: every NetworkSurface refused
 *     under the lock, recognizer coercion, fail-closed provider.
 */
class DrsSecurityWaveTests : FunSpec({

    // --------------------------------------------- live proof engine

    context("DrsLiveProofEngine") {
        val supported = { rx: Long, tx: Long ->
            DrsNetworkSentinel.TrafficSnapshot(rx, tx, true)
        }

        test("first tick is zero-age silence with honest deltas") {
            var now = supported(0L, 0L)
            var t = 100L
            val engine = DrsLiveProofEngine(sample = { now }, clock = { t })
            val proof = engine.tick(absoluteMode = false)
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.PROVEN_ZERO
            proof.zeroStreakMs shouldBe 0L
            proof.observedMs shouldBe 0L
            proof.ticks shouldBe 1L
            proof.rxSinceBaseline shouldBe 0L
            proof.txSinceBaseline shouldBe 0L
            proof.countersSupported.shouldBeTrue()
            proof.counterResetObserved.shouldBeFalse()
        }

        test("the zero-byte streak grows only while silence holds") {
            var now = supported(0L, 0L)
            var t = 1_000L
            val engine = DrsLiveProofEngine(sample = { now }, clock = { t })
            engine.tick(absoluteMode = true).zeroStreakMs shouldBe 0L
            t += 1_000
            engine.tick(absoluteMode = true).zeroStreakMs shouldBe 1_000L
            t += 2_000
            val proof = engine.tick(absoluteMode = true)
            proof.zeroStreakMs shouldBe 3_000L
            proof.ticks shouldBe 3L
            proof.observedMs shouldBe 3_000L
        }

        test("any delta flips the verdict to MOVED with the honest numbers") {
            var now = supported(0L, 0L)
            var t = 0L
            val engine = DrsLiveProofEngine(sample = { now }, clock = { t })
            engine.tick(absoluteMode = false)
            t += 1_000
            now = supported(40L, 0L)
            val proof = engine.tick(absoluteMode = false)
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.MOVED
            proof.rxSinceBaseline shouldBe 40L
            proof.rxSincePrevious shouldBe 40L
            proof.txSinceBaseline shouldBe 0L
            // The silence just ended — the streak restarts from zero.
            proof.zeroStreakMs shouldBe 0L
        }

        test("silence after movement rebuilds a fresh streak") {
            var now = supported(0L, 0L)
            var t = 0L
            val engine = DrsLiveProofEngine(sample = { now }, clock = { t })
            engine.tick(absoluteMode = false)
            t += 1_000
            now = supported(10L, 5L)
            engine.tick(absoluteMode = false).verdict shouldBe DrsLiveProofEngine.Verdict.MOVED
            // Silence restarted at the movement tick (t=1000): one second
            // later the fresh streak is 1000, not 0.
            t += 1_000
            now = supported(10L, 5L)
            engine.tick(absoluteMode = false).zeroStreakMs shouldBe 1_000L
            t += 5_000
            val proof = engine.tick(absoluteMode = false)
            // Verdict stays MOVED (baseline deltas 10/5 are real movement
            // this session) while the streak keeps growing over the fresh
            // silent window — both truths at once.
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.MOVED
            proof.zeroStreakMs shouldBe 6_000L
        }

        test("unsupported counters refuse to claim a provable zero") {
            val engine = DrsLiveProofEngine(
                sample = { DrsNetworkSentinel.TrafficSnapshot(0L, 0L, false) },
                clock = { 0L },
            )
            val proof = engine.tick(absoluteMode = false)
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.UNSUPPORTED
            proof.countersSupported.shouldBeFalse()
        }

        test("a counter reset restarts the streak honestly instead of faking silence") {
            var now = supported(500L, 300L)
            var t = 0L
            val engine = DrsLiveProofEngine(sample = { now }, clock = { t }, baselineOverride = now)
            engine.tick(absoluteMode = false)
            t += 1_000
            now = supported(400L, 300L) // OS counter reset (reboot / wrap)
            val proof = engine.tick(absoluteMode = false)
            proof.counterResetObserved.shouldBeTrue()
            proof.zeroStreakMs shouldBe 0L
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.PROVEN_ZERO
            proof.rxSinceBaseline shouldBe 0L
            proof.rxSincePrevious shouldBe 0L
        }

        test("an explicit baseline wins over the first sample") {
            var now = supported(0L, 0L)
            val engine = DrsLiveProofEngine(
                sample = { now },
                clock = { 0L },
                baselineOverride = supported(1_000L, 2_000L),
            )
            val proof = engine.tick(absoluteMode = false)
            proof.verdict shouldBe DrsLiveProofEngine.Verdict.PROVEN_ZERO
            proof.rxSinceBaseline shouldBe 0L
            now = supported(0L, 2_500L)
            val proof2 = engine.tick(absoluteMode = false)
            proof2.verdict shouldBe DrsLiveProofEngine.Verdict.MOVED
            proof2.txSinceBaseline shouldBe 500L
        }

        test("deltas are never negative even when the counters jump backwards") {
            var now = supported(100L, 100L)
            val engine = DrsLiveProofEngine(
                sample = { now },
                clock = { 0L },
                baselineOverride = supported(0L, 0L),
            )
            engine.tick(absoluteMode = false)
            now = supported(0L, 0L)
            val proof = engine.tick(absoluteMode = false)
            proof.rxSinceBaseline shouldBe 0L
            proof.txSinceBaseline shouldBe 0L
            proof.rxSincePrevious shouldBe 0L
            proof.txSincePrevious shouldBe 0L
        }

        test("absoluteMode rides the proof verbatim") {
            val engine = DrsLiveProofEngine(
                sample = { supported(0L, 0L) },
                clock = { 0L },
            )
            engine.tick(absoluteMode = true).absoluteMode.shouldBeTrue()
            engine.tick(absoluteMode = false).absoluteMode.shouldBeFalse()
        }

        test("byte formatting pins the boundaries the user reads") {
            DrsLiveProofEngine.formatBytes(0L) shouldBe "0 B"
            DrsLiveProofEngine.formatBytes(999L) shouldBe "999 B"
            DrsLiveProofEngine.formatBytes(1_000L) shouldBe "1 KB"
            DrsLiveProofEngine.formatBytes(999_999L) shouldBe "999 KB"
            DrsLiveProofEngine.formatBytes(1_000_000L) shouldBe "1.0 MB"
            DrsLiveProofEngine.formatBytes(2_500_000L) shouldBe "2.5 MB"
            DrsLiveProofEngine.formatBytes(1_000_000_000L) shouldBe "1.0 GB"
            DrsLiveProofEngine.formatBytes(2_500_000_000L) shouldBe "2.5 GB"
        }

        test("duration formatting pins the boundaries the user reads") {
            DrsLiveProofEngine.formatDuration(0L) shouldBe "0:00"
            DrsLiveProofEngine.formatDuration(41_999L) shouldBe "0:41"
            DrsLiveProofEngine.formatDuration(60_000L) shouldBe "1:00"
            DrsLiveProofEngine.formatDuration(3_599_999L) shouldBe "59:59"
            DrsLiveProofEngine.formatDuration(3_600_000L) shouldBe "1:00:00"
            DrsLiveProofEngine.formatDuration(3_779_000L) shouldBe "1:02:59"
        }
    }

    // ------------------------------- expanded manifest contract

    context("manifest contract — v1.4.x expanded") {
        val manifest = resolveManifest()
        val manifestText = manifest.readText()
        val components = parseComponents(manifestText)

        test("the permission set stays exactly the five of the sentinel allowlist") {
            val actual = Regex("uses-permission android:name=\"([^\"]+)\"")
                .findAll(manifestText)
                .map { it.groupValues[1] }
                .sorted()
                .toList()
            actual shouldContainExactlyInAnyOrder DrsNetworkSentinel.EXPECTED_PERMISSIONS
        }

        test("every history-recurring over-privileged permission stays absent") {
            val forbidden = listOf(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.CAMERA",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_PHONE_STATE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
            )
            forbidden.forEach { name ->
                (!manifestText.contains("uses-permission android:name=\"$name\"")).shouldBeTrue()
            }
            (!manifestText.contains("QUERY_ALL_PACKAGES")).shouldBeTrue()
        }

        test("exactly five components are exported and each is on the allowlist") {
            val exportedTrue = components.filter { it.exported == true }.map { it.name }
            exportedTrue shouldContainExactlyInAnyOrder listOf(
                "com.drs.smartkeyboard.DrsImeService",
                "com.drs.smartkeyboard.DrsSpellCheckerService",
                "com.drs.smartkeyboard.app.DrsAppActivity",
                "com.drs.smartkeyboard.SettingsLauncherAlias",
                "com.drs.smartkeyboard.ime.clipboard.DrsCopyToClipboardActivity",
            )
        }

        test("every component with an intent-filter declares exported explicitly") {
            val undeclared = components.filter { it.hasIntentFilter && it.exported == null }
            (undeclared.isEmpty()).shouldBeTrue()
        }

        test("no component without an intent-filter is exported") {
            val bad = components.filter { !it.hasIntentFilter && it.exported == true }
            (bad.isEmpty()).shouldBeTrue()
        }

        test("the exported services carry their platform binding permissions") {
            components.first { it.name == "com.drs.smartkeyboard.DrsImeService" }
                .text.shouldContain("android.permission.BIND_INPUT_METHOD")
            components.first { it.name == "com.drs.smartkeyboard.DrsSpellCheckerService" }
                .text.shouldContain("android.permission.BIND_TEXT_SERVICE")
        }

        test("every provider stays unexported") {
            val providers = components.filter { it.type == "provider" }
            (providers.size >= 3).shouldBeTrue()
            providers.forEach { provider ->
                provider.exported shouldBe false
            }
        }

        test("the sensitive trampoline activities stay unexported") {
            listOf(
                "com.drs.smartkeyboard.ime.voice.VoicePermissionActivity",
                "com.drs.smartkeyboard.ime.clipboard.ClipEditorPopupActivity",
                "com.drs.smartkeyboard.drs.ui.DrsOnboardingActivity",
            ).forEach { name ->
                components.first { it.name == name }.exported shouldBe false
            }
        }

        test("the application tag keeps the backup/extraction/security wiring") {
            val appBlock = Regex("<application\\b.*?(?=<service|<activity|<provider|<queries)",
                RegexOption.DOT_MATCHES_ALL)
                .find(manifestText)!!.value
            appBlock.shouldContain("android:allowBackup=\"false\"")
            appBlock.shouldContain("android:dataExtractionRules=\"@xml/data_extraction_rules\"")
            appBlock.shouldContain("android:fullBackupContent=\"@xml/backup_rules\"")
            appBlock.shouldContain("android:networkSecurityConfig=\"@xml/network_security_config\"")
        }

        test("no shell profiling of the production process") {
            // Comments are stripped first — the manifest documents the
            // REMOVED <profileable> inside a comment, and the assertion
            // is about live XML, not about prose.
            val stripped = manifestText.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            (!stripped.contains("<profileable")).shouldBeTrue()
        }

        test("the queries block stays scoped to IME and spell-checker discovery") {
            val queriesBlock = Regex("<queries>.*</queries>", RegexOption.DOT_MATCHES_ALL)
                .find(manifestText)!!.value
            val actions = Regex("android:name=\"([^\"]+)\"")
                .findAll(queriesBlock)
                .map { it.groupValues[1] }
                .toList()
            actions shouldContainExactlyInAnyOrder listOf(
                "android.view.InputMethod",
                "android.service.textservice.SpellCheckerService",
            )
        }

        test("cleartext stays denied explicitly through the network security config") {
            val config = File(manifest.parentFile, "res/xml/network_security_config.xml")
            config.exists().shouldBeTrue()
            val text = config.readText()
            text.shouldContain("<base-config cleartextTrafficPermitted=\"false\">")
            (!text.lowercase().contains("cleartexttrafficpermitted=\"true\"")).shouldBeTrue()
        }

        test("the backup rules files referenced by the manifest exist") {
            File(manifest.parentFile, "res/xml/backup_rules.xml").exists().shouldBeTrue()
            File(manifest.parentFile, "res/xml/data_extraction_rules.xml").exists().shouldBeTrue()
        }
    }

    // ----------------------------- absolute-mode gate matrix

    context("DrsPrivacyLock — v1.4.x gate matrix") {
        val surfaces = DrsPrivacyLock.NetworkSurface.entries

        test("absolute mode refuses every network surface without exceptions") {
            surfaces.forEach { surface ->
                DrsPrivacyLock.allows(surface, absoluteMode = true).shouldBeFalse()
            }
        }

        test("without the lock every declared surface stays available") {
            surfaces.forEach { surface ->
                DrsPrivacyLock.allows(surface, absoluteMode = false).shouldBeTrue()
            }
        }

        test("the lock coerces every recognizer request to on-device only") {
            VoiceRecognizerMode.entries.forEach { requested ->
                DrsPrivacyLock.effectiveRecognizer(requested, absoluteMode = true) shouldBe
                    VoiceRecognizerMode.ON_DEVICE_ONLY
                DrsPrivacyLock.effectiveRecognizer(requested, absoluteMode = false) shouldBe requested
            }
        }

        test("a broken provider fails closed — the gates close, never open") {
            val original = DrsPrivacyLock.absoluteModeProvider
            try {
                DrsPrivacyLock.init { error("simulated provider crash") }
                DrsPrivacyLock.isAbsoluteMode().shouldBeTrue()
            } finally {
                DrsPrivacyLock.init(original)
            }
        }
    }
}) {
    companion object {

        /**
         * Walks up from the working directory until it finds the
         * module's manifest — AGP's unit-test working directory varies
         * between environments, so the resolver must not assume one.
         */
        internal fun resolveManifest(): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            repeat(6) {
                val candidate = File(dir, "app/src/main/AndroidManifest.xml")
                if (candidate.exists()) return candidate
                dir = dir?.parentFile
            }
            error("AndroidManifest.xml not found from ${System.getProperty("user.dir")}")
        }

        /**
         * Splits the manifest into component blocks (from each opening
         * tag to the next component tag) and reads name/exported ONLY
         * from the opening tag — child <intent-filter> names can never
         * pollute the component identity.
         */
        internal fun parseComponents(manifestText: String): List<ComponentBlock> {
            val starts = Regex("<(service|activity-alias|activity|receiver|provider)\\b")
                .findAll(manifestText)
                .toList()
            return starts.mapIndexed { i, match ->
                val blockEnd = starts.getOrNull(i + 1)?.range?.first ?: manifestText.length
                val block = manifestText.substring(match.range.first, blockEnd)
                val opening = block.substringBefore('>')
                ComponentBlock(
                    type = match.groupValues[1],
                    name = Regex("android:name=\"([^\"]+)\"").find(opening)!!.groupValues[1],
                    exported = Regex("android:exported=\"(true|false)\"")
                        .find(opening)?.groupValues[1]?.toBoolean(),
                    hasIntentFilter = block.contains("<intent-filter"),
                    text = block,
                )
            }
        }
    }

    /** One manifest component block with the identity attributes. */
    internal data class ComponentBlock(
        val type: String,
        val name: String,
        val exported: Boolean?,
        val hasIntentFilter: Boolean,
        val text: String,
    )
}

private fun String.shouldContain(needle: String) {
    if (!contains(needle)) throw AssertionError("expected to contain `$needle`: $this")
}
