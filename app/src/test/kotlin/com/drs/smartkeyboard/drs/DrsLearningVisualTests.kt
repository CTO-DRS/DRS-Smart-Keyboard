/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.ai.DrsCorrectionCounters
import com.drs.smartkeyboard.drs.ai.DrsLearningEngine
import com.drs.smartkeyboard.lib.ext.DrsEd25519
import com.drs.smartkeyboard.lib.ext.DrsPackageTrust
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 2): the visual learning
 * engine tests. Built on the era baseline (1780 → 1804 → …) — these
 * add on top.
 *
 * Layers proven here:
 *  - DrsCorrectionCounters: «العد من الآن» — a fresh store reads zero,
 *    unarmed arms are honest no-ops, the anchor stamps ONCE at the
 *    first counted correction, accumulation is exact, reset really
 *    wipes, and the PERSISTED model carries exactly the three fields
 *    (schema/startedAt/autoCommits) — no retroactive field exists in
 *    the file format itself;
 *  - DrsLexiconExchange: the signed round-trip (export → verify →
 *    import), the mandated distinct rejections (unsigned vs tampered
 *    vs unknown key vs bad format), digest determinism, the
 *    signature-covers-the-digest discipline, and the honest
 *    per-entry import report with duplicate skipping;
 *  - the wipe: a REAL deletion across the learning engine, the
 *    lexicon, and the counters;
 *  - structural isolation: the ONLY ime/ file allowed to know
 *    DrsCorrectionCounters is KeyboardManager.kt — with EXACTLY the
 *    import + the two declared counter lines at the arm sites
 *    (ADR-0009, owner decision 1); any third reference fails the
 *    build.
 */
class DrsLearningVisualTests : FunSpec({

    // --------------------------------------------- DrsCorrectionCounters

    context("DrsCorrectionCounters — «العد من الآن»") {
        fun newCore(dir: File, now: () -> Long): DrsCorrectionCounters =
            DrsCorrectionCounters(File(dir, DrsCorrectionCounters.FILE_NAME), now)

        test("a fresh store starts honestly at zero with no anchor") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            var t = 1_000_000L
            val core = newCore(dir) { t }
            val state = core.read()
            state.autoCommits shouldBe 0L
            state.startedAt shouldBe 0L
        }

        test("an unarmed arm is an honest no-op — no inflation") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            var t = 1_000_000L
            val core = newCore(dir) { t }
            core.onAutoCommit(armed = false)
            core.onAutoCommit(armed = false)
            core.read().autoCommits shouldBe 0L
            core.read().startedAt shouldBe 0L
        }

        test("the first ARMED correction stamps the anchor exactly once") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            var t = 1_000_000L
            val core = newCore(dir) { t }
            core.onAutoCommit(armed = true)
            core.read().let {
                it.autoCommits shouldBe 1L
                it.startedAt shouldBe 1_000_000L
            }
            t = 9_999_999L
            core.onAutoCommit(armed = true)
            core.read().let {
                it.autoCommits shouldBe 2L
                it.startedAt shouldBe 1_000_000L // the anchor never moves
            }
        }

        test("the persisted model carries EXACTLY three fields — no retroactive field exists") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            val core = newCore(dir) { 5_000_000L }
            core.onAutoCommit(armed = true)
            val raw = File(dir, DrsCorrectionCounters.FILE_NAME).readText()
            val keys = Json.parseToJsonElement(raw).jsonObject.keys
            keys shouldContainExactlyInAnyOrder setOf("schema", "startedAt", "autoCommits")
        }

        test("a foreign extra field is ignored, never adopted") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            val file = File(dir, DrsCorrectionCounters.FILE_NAME)
            file.writeText(
                """{"schema":"drs-correction-counters-1","startedAt":7,"autoCommits":3,"retroactiveHistory":99999}""",
            )
            val core = newCore(dir) { 8_000_000L }
            core.read().let {
                it.autoCommits shouldBe 3L
                it.startedAt shouldBe 7L
            }
        }

        test("a corrupted file reads fresh — counters never fabricate a carried-over number") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            val file = File(dir, DrsCorrectionCounters.FILE_NAME)
            file.writeText("not json {{{")
            val core = newCore(dir) { 1L }
            core.read().autoCommits shouldBe 0L
        }

        test("reset really deletes — the wipe honesty proof") {
            val dir = Files.createTempDirectory("drs-cc").toFile()
            val file = File(dir, DrsCorrectionCounters.FILE_NAME)
            var t = 1L
            val core = newCore(dir) { t }
            core.onAutoCommit(armed = true)
            file.exists().shouldBeTrue()
            core.reset()
            file.exists().shouldBeFalse()
            core.read().autoCommits shouldBe 0L
        }
    }

    // ------------------------------------------------ DrsLexiconExchange

    context("DrsLexiconExchange — the signed exchange") {
        val seed = ByteArray(32) { it.toByte() } // deterministic test key
        val now = 1_700_000_000_000L

        fun sampleEntries(): List<DrsMyLexiconEntry> = listOf(
            DrsMyLexiconEntry(id = 1, word = "كتاب", vocalized = "كِتَاب", createdAtMs = now, updatedAtMs = now),
            DrsMyLexiconEntry(id = 2, word = "قمر", vocalized = "قَمَر", pinned = true, createdAtMs = now, updatedAtMs = now),
            DrsMyLexiconEntry(id = 3, word = "شمس", vocalized = "شَمْس", createdAtMs = now, updatedAtMs = now),
        )

        fun signedText(): String {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            return DrsLexiconExchange.serialize(envelope)
        }

        test("an empty lexicon refuses export honestly") {
            DrsLexiconExchange.buildEnvelope(emptyList(), now, seed) shouldBe null
        }

        test("a wrong-length seed refuses export honestly") {
            DrsLexiconExchange.buildEnvelope(sampleEntries(), now, ByteArray(16)) shouldBe null
        }

        test("the full round-trip: export → verify → import accepts everything") {
            val result = DrsLexiconExchange.parseAndVerify(signedText())
            val verified = result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Verified>()
            verified.envelope.entries shouldHaveSize 3
            verified.envelope.pubkeyId shouldBe DrsLexiconExchange.LOCAL_KEY_ID
            verified.envelope.pubkey?.length shouldBe 64
            verified.envelope.version shouldBe DrsLexiconExchange.VERSION

            val report = DrsLexiconExchange.importInto(DrsState(), verified.envelope.entries, now)
            report.accepted shouldBe 3
            report.skippedDuplicates shouldBe 0
            report.rejected shouldHaveSize 0
        }

        test("the digest is deterministic — the same entries always sign the same bytes") {
            val a = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val b = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            a.sha256 shouldBe b.sha256
            a.signature shouldBe b.signature
        }

        test("MANDATED: an unsigned file (no signature fields) is rejected AS UNSIGNED") {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val stripped = envelope.copy(signature = "", pubkeyId = "")
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(stripped))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Unsigned>()
        }

        test("MANDATED: a tampered file (one entry byte flipped after signing) is rejected AS TAMPERED") {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val tampered = envelope.copy(
                entries = envelope.entries.toMutableList().also {
                    it[0] = it[0].copy(vocalized = "كُتَاب")
                },
            )
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(tampered))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Tampered>()
        }

        test("MANDATED: a tampered sha256 field is rejected AS TAMPERED") {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val forged = envelope.copy(sha256 = "0".repeat(64))
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(forged))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Tampered>()
        }

        test("a signature from a foreign key is rejected AS TAMPERED (fail-closed verify)") {
            val entries = sampleEntries()
            val digest = DrsLexiconExchange.digestOf(
                entries.map { DrsLexiconExchange.Entry(it.word, it.vocalized, it.pinned, it.createdAtMs, it.updatedAtMs) },
            )
            val foreignSignature = DrsEd25519.sign(ByteArray(32) { 7 }, digest)
            val forged = DrsLexiconExchange.Envelope(
                schema = DrsLexiconExchange.SCHEMA,
                version = DrsLexiconExchange.VERSION,
                exportedAt = now,
                entries = entries.map { DrsLexiconExchange.Entry(it.word, it.vocalized, it.pinned, it.createdAtMs, it.updatedAtMs) },
                sha256 = digest.toHex(),
                signature = foreignSignature.toHex(),
                pubkeyId = DrsLexiconExchange.LOCAL_KEY_ID,
                pubkey = DrsEd25519.publicKeyFromSeed(seed).toHex(),
            )
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(forged))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Tampered>()
        }

        test("an unknown pubkey_id is rejected AS UNKNOWN KEY") {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val foreign = envelope.copy(pubkeyId = "someone-elses-key")
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(foreign))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.UnknownKey>()
        }

        test("a local-key file without the embedded public key is rejected (cannot verify)") {
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            val bare = envelope.copy(pubkey = null)
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(bare))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.BadFormat>()
        }

        test("garbage and foreign schemas are rejected AS BAD FORMAT") {
            DrsLexiconExchange.parseAndVerify("{{{ not json")
                .shouldBeInstanceOf<DrsLexiconExchange.ImportResult.BadFormat>()
            val envelope = DrsLexiconExchange.buildEnvelope(sampleEntries(), now, seed)!!
            DrsLexiconExchange.parseAndVerify(
                DrsLexiconExchange.serialize(envelope.copy(schema = "other-vendor-lexicon")),
            ).shouldBeInstanceOf<DrsLexiconExchange.ImportResult.BadFormat>()
        }

        test("re-importing the same file skips duplicates — never a second copy") {
            val verified = DrsLexiconExchange.parseAndVerify(signedText())
                .shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Verified>()
            val first = DrsLexiconExchange.importInto(DrsState(), verified.envelope.entries, now)
            first.accepted shouldBe 3
            val second = DrsLexiconExchange.importInto(first.state, verified.envelope.entries, now)
            second.accepted shouldBe 0
            second.skippedDuplicates shouldBe 3
        }

        test("invalid entries are rejected per-entry with an honest report; the rest enter") {
            val verified = DrsLexiconExchange.parseAndVerify(signedText())
                .shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Verified>()
            val poisoned = verified.envelope.entries + DrsLexiconExchange.Entry(word = "", vocalized = "")
            val report = DrsLexiconExchange.importInto(DrsState(), poisoned, now)
            report.accepted shouldBe 3
            report.rejected shouldHaveSize 1
            report.rejected.single().error shouldBe DrsMyLexicon.Error.EMPTY_WORD
        }

        test("the release key id is accepted as a trust anchor (verify fails honestly for a foreign signature)") {
            // the release key path resolves to the PINNED public key — a file
            // claiming the release id but signed by another key fails the
            // verify step (tampered), proving the pinned-key path is live.
            val entries = sampleEntries()
            val digest = DrsLexiconExchange.digestOf(
                entries.map { DrsLexiconExchange.Entry(it.word, it.vocalized, it.pinned, it.createdAtMs, it.updatedAtMs) },
            )
            val forged = DrsLexiconExchange.Envelope(
                schema = DrsLexiconExchange.SCHEMA,
                version = DrsLexiconExchange.VERSION,
                exportedAt = now,
                entries = entries.map { DrsLexiconExchange.Entry(it.word, it.vocalized, it.pinned, it.createdAtMs, it.updatedAtMs) },
                sha256 = digest.toHex(),
                signature = DrsEd25519.sign(seed, digest).toHex(),
                pubkeyId = DrsPackageTrust.RELEASE_KEY_ID,
            )
            val result = DrsLexiconExchange.parseAndVerify(DrsLexiconExchange.serialize(forged))
            result.shouldBeInstanceOf<DrsLexiconExchange.ImportResult.Tampered>()
        }
    }

    // ---------------------------------------------- the real wipe proof

    context("the wipe really wipes") {
        test("learning engine clear empties the live tables; lexicon removeAll empties the state") {
            DrsLearningEngine.learnWord("تجربة-المسح")
            DrsLearningEngine.size().shouldBe(1)
            val dir = Files.createTempDirectory("drs-wipe").toFile()
            DrsLearningEngine.clear(File(dir, "drs_learning_v1.json"))
            DrsLearningEngine.size() shouldBe 0

            val taught = DrsState(
                myLexicon = listOf(DrsMyLexiconEntry(id = 1, word = "كتاب", vocalized = "كِتَاب")),
            )
            DrsMyLexicon.removeAllAt(taught).myLexicon shouldHaveSize 0
        }
    }

    // --------------------------------- structural isolation (feature 2)

    context("structural isolation — feature 2") {
        val main = File(resolveRepoRoot(), "app/src/main/kotlin/com/drs/smartkeyboard")

        test("the ONLY ime/ file that knows DrsCorrectionCounters is KeyboardManager.kt") {
            val ime = File(main, "ime")
            val offenders = ime.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.name != "KeyboardManager.kt" }
                .filter { it.readText().contains("DrsCorrectionCounters") }
                .map { it.name }
                .toList()
            offenders shouldHaveSize 0
        }

        test("KeyboardManager carries EXACTLY the declared import + two counter lines (ADR-0009, decision 1)") {
            val src = File(main, "ime/keyboard/KeyboardManager.kt").readText()
            val references = Regex("DrsCorrectionCounters").findAll(src).count()
            references shouldBe 3 // one import + the two declared call sites
            val counterLines = Regex("DrsCorrectionCounters\\.forProcess").findAll(src).count()
            counterLines shouldBe 2
            // both declared sites, each annotated with the ADR reference
            var idx = src.indexOf("DrsCorrectionCounters.forProcess")
            while (idx >= 0) {
                src.substring(maxOf(0, idx - 400), idx).shouldContain("ADR-0009")
                idx = src.indexOf("DrsCorrectionCounters.forProcess", idx + 1)
            }
        }

        test("the dashboard registration for the counters store cannot silently vanish") {
            val dash = File(main, "drs/privacy/DrsPrivacyDashboard.kt").readText()
            dash.shouldContain("STORE_CORRECTION_COUNTERS")
            dash.shouldContain("DrsCorrectionCounters.FILE_NAME")
        }
    }
}) {
    companion object {
        /** Walks up from the working directory to the repo root (AGP's unit-test cwd varies). */
        internal fun resolveRepoRoot(): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            repeat(6) {
                if (File(dir, "app/src/main/AndroidManifest.xml").exists()) return dir!!
                dir = dir?.parentFile
            }
            error("repo root not found from ${System.getProperty("user.dir")}")
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
