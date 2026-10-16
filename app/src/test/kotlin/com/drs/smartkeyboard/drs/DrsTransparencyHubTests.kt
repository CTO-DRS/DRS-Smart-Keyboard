/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import com.drs.smartkeyboard.drs.privacy.DrsAgeCertificate
import com.drs.smartkeyboard.drs.privacy.DrsCertificatePng
import com.drs.smartkeyboard.drs.privacy.DrsNetworkSentinel
import com.drs.smartkeyboard.drs.privacy.DrsPrivacyDashboard
import com.drs.smartkeyboard.drs.privacy.DrsTransparencyLog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.Inflater

/**
 * «الشفافية والتعلم» (ADR-0009 — feature 1): the live transparency
 * hub tests. Built on the era baseline 1780/0/131 — these add on top.
 *
 * Layers proven here:
 *  - DrsTransparencyLog: round-trip, same-day accumulation, the hard
 *    90-day rotation (day 91 evicts day 1), out-of-order stamps,
 *    corruption/over-cap discard, the write-once firstUseAt anchor,
 *    and the two honesty laws (unsupported is recorded, never faked
 *    as zero; zeroTraffic only with full evidence);
 *  - DrsAgeCertificate: pure computation — empty log earns no claim,
 *    unsupported days kill the zero claim, rows() renders the honest
 *    variants;
 *  - PngCodec: a PNG is ACTUALLY generated — byte-level (signature,
 *    IHDR dims, IDAT inflates to the exact scanlines, CRC32 verified),
 *    file-level (written and re-read) — with zero Android dependency;
 *  - the dashboard contract: the fifth store is visible in summary()
 *    and really destroyed by deleteAll();
 *  - structural isolation: feature-1 files import no network
 *    primitives and no TrafficStats, the IME tree holds zero
 *    references to them, the approved cache-path resource exists, and
 *    the dashboard registration cannot silently vanish. (The manifest
 *    permission set stays guarded by DrsP3PrivacyTests — unchanged.)
 */
class DrsTransparencyHubTests : FunSpec({

    val dayMs = 86_400_000L
    val base = 1_700_000_000_000L

    fun newCore(dir: File, clock: () -> Long): DrsTransparencyLog {
        val file = File(dir, DrsTransparencyLog.FILE_NAME)
        return DrsTransparencyLog(file, clock)
    }

    // ------------------------------------------------ DrsTransparencyLog

    context("DrsTransparencyLog") {
        test("record → read round-trips the day snapshot exactly") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.record(rxSincePrevious = 12L, txSincePrevious = 34L, countersSupported = true, attestedAt = t)
            val state = core.read()
            state.days shouldHaveSize 1
            val day = state.days.single()
            day.rxTotal shouldBe 12L
            day.txTotal shouldBe 34L
            day.countersSupported.shouldBeTrue()
            day.zeroTraffic.shouldBeFalse()
        }

        test("multiple recordings on the same day accumulate; zeroTraffic flips only on true zero") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.record(5, 3, true, t)
            core.record(6, 4, true, t + 1000)
            core.read().days.single().let {
                it.rxTotal shouldBe 11L
                it.txTotal shouldBe 7L
                it.zeroTraffic.shouldBeFalse()
            }
            // a fresh day with no bytes at all — the only honest zero
            t = base + dayMs
            core.record(0, 0, true, t)
            val day2 = core.read().days.first { it.day == base / dayMs + 1 }
            day2.rxTotal shouldBe 0L
            day2.txTotal shouldBe 0L
            day2.zeroTraffic.shouldBeTrue()
        }

        test("day 91 evicts day 1 — the 90-day rotation is a hard bound") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.ensureFirstUse()
            for (d in 0 until DrsTransparencyLog.MAX_DAYS) {
                t = base + d * dayMs
                core.record(1, 1, true, t)
            }
            core.read().days shouldHaveSize DrsTransparencyLog.MAX_DAYS
            val oldestBefore = core.read().days.minOf { it.day }
            // day 91 (index 90): the window slides, day 1 is gone forever
            t = base + 90 * dayMs
            core.record(2, 2, true, t)
            val state = core.read()
            state.days shouldHaveSize DrsTransparencyLog.MAX_DAYS
            state.days.minOf { it.day } shouldBe (oldestBefore + 1)
            state.days.maxOf { it.day } shouldBe (base / dayMs + 90)
        }

        test("out-of-order stamps never break the rotation or the sort") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base + 10 * dayMs
            val core = newCore(dir) { t }
            core.record(1, 1, true, t)
            t = base + 5 * dayMs
            core.record(1, 1, true, t)
            t = base + 20 * dayMs
            core.record(1, 1, true, t)
            val days = core.read().days
            days.map { it.day } shouldContainExactly listOf(
                base / dayMs + 5,
                base / dayMs + 10,
                base / dayMs + 20,
            )
        }

        test("a corrupted file is discarded and logging restarts clean") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            File(dir, DrsTransparencyLog.FILE_NAME).writeText("{{{not json at all")
            core.read().days shouldHaveSize 0
            core.record(7, 7, true, t)
            core.read().days shouldHaveSize 1
        }

        test("an over-cap file is discarded, never trusted") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            File(dir, DrsTransparencyLog.FILE_NAME).writeBytes(ByteArray(DrsTransparencyLog.FILE_BYTES_CAP + 1))
            core.read().days shouldHaveSize 0
            core.record(1, 1, true, t)
            core.read().days shouldHaveSize 1
        }

        test("a foreign-schema file is treated as absent") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            val core = newCore(dir) { base }
            File(dir, DrsTransparencyLog.FILE_NAME).writeText(
                """{"schema":"someone-elses","firstUseAt":1,"days":[]}""",
            )
            core.read().let {
                it.schema shouldBe DrsTransparencyLog.SCHEMA
                it.days shouldHaveSize 0
            }
        }

        test("firstUseAt is written once and never moves") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.ensureFirstUse() shouldBe base
            t = base + 7 * dayMs
            core.ensureFirstUse() shouldBe base
            core.record(1, 1, true, t)
            core.read().firstUseAt shouldBe base
        }

        test("HONESTY: unsupported counters are recorded as such, never faked as zero") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.record(0, 0, countersSupported = false, attestedAt = t)
            val day = core.read().days.single()
            day.countersSupported.shouldBeFalse()
            day.zeroTraffic.shouldBeFalse()
            day.rxTotal shouldBe 0L
        }

        test("HONESTY: unsupported later in the day AND-aggregates the day's flag") {
            val dir = Files.createTempDirectory("drs-tlog").toFile()
            var t = base
            val core = newCore(dir) { t }
            core.record(3, 3, true, t)
            t = base + 1000
            core.record(1, 1, false, t)
            val day = core.read().days.single()
            day.countersSupported.shouldBeFalse()
            day.zeroTraffic.shouldBeFalse()
        }
    }

    // ------------------------------------------------ DrsAgeCertificate

    context("DrsAgeCertificate") {
        fun labels() = DrsAgeCertificate.Labels(
            firstUseDate = "since %1\$s",
            ageLine = "age %1\$d",
            daysLine = "days %1\$d",
            totalsLine = "totals %1\$s %2\$s",
            zeroClaim = "ZERO",
            movedClaim = "MOVED",
            unsupportedNote = "UNSUPPORTED-NOTE",
            honestyNote = "HONESTY-NOTE",
        )

        test("an empty log earns no claim at all") {
            val cert = DrsAgeCertificate.compute(DrsTransparencyLog.State(), base)
            cert.attestedDays shouldBe 0
            cert.zeroBytesClaim.shouldBeFalse()
            cert.countersAlwaysSupported.shouldBeFalse()
            cert.ageDays shouldBe 0L
        }

        test("age counts from the firstUseAt anchor only") {
            val state = DrsTransparencyLog.State(
                firstUseAt = base,
                days = listOf(
                    DrsTransparencyLog.DaySnapshot(base / dayMs, base, 0, 0, true, true),
                ),
            )
            val cert = DrsAgeCertificate.compute(state, base + 3 * dayMs + 1000)
            cert.ageDays shouldBe 3L
            cert.attestedDays shouldBe 1
            cert.zeroBytesClaim.shouldBeTrue()
        }

        test("totals sum the recorded days; unsupported days kill the zero claim") {
            val state = DrsTransparencyLog.State(
                firstUseAt = base,
                days = listOf(
                    DrsTransparencyLog.DaySnapshot(1, base, 10, 0, true, false),
                    DrsTransparencyLog.DaySnapshot(2, base, 5, 7, true, false),
                    DrsTransparencyLog.DaySnapshot(3, base, 0, 0, false, false),
                ),
            )
            val cert = DrsAgeCertificate.compute(state, base)
            cert.totalRxBytes shouldBe 15L
            cert.totalTxBytes shouldBe 7L
            cert.zeroBytesClaim.shouldBeFalse()
            cert.countersAlwaysSupported.shouldBeFalse()
        }

        test("rows() renders the honest variants and the mandatory notes") {
            val zeroState = DrsTransparencyLog.State(
                firstUseAt = base,
                days = listOf(
                    DrsTransparencyLog.DaySnapshot(1, base, 0, 0, true, true),
                ),
            )
            val zeroRows = DrsAgeCertificate.rows(DrsAgeCertificate.compute(zeroState, base), labels())
            zeroRows shouldContainExactly listOf(
                "since %1\$s",
                "age %1\$d",
                "days %1\$d",
                "totals %1\$s %2\$s",
                "ZERO",
                "HONESTY-NOTE",
            )

            val movedState = DrsTransparencyLog.State(
                days = listOf(
                    DrsTransparencyLog.DaySnapshot(1, base, 100, 0, true, false),
                ),
            )
            val movedRows = DrsAgeCertificate.rows(DrsAgeCertificate.compute(movedState, base), labels())
            movedRows shouldContainExactly listOf(
                "days %1\$d",
                "totals %1\$s %2\$s",
                "MOVED",
                "HONESTY-NOTE",
            )

            val unsupportedState = DrsTransparencyLog.State(
                days = listOf(
                    DrsTransparencyLog.DaySnapshot(1, base, 0, 0, false, false),
                ),
            )
            val unsupportedRows =
                DrsAgeCertificate.rows(DrsAgeCertificate.compute(unsupportedState, base), labels())
            unsupportedRows shouldContainExactly listOf(
                "days %1\$d",
                "totals %1\$s %2\$s",
                "MOVED",
                "UNSUPPORTED-NOTE",
                "HONESTY-NOTE",
            )
        }
    }

    // ------------------------------------- PngCodec (a PNG is generated)

    context("PngCodec — a PNG is actually generated") {
        fun pngOf(pixels: IntArray, width: Int, height: Int): ByteArray =
            DrsCertificatePng.PngCodec.encode(pixels, width, height)

        fun chunksOf(png: ByteArray): List<Triple<String, ByteArray, Long>> {
            val result = mutableListOf<Triple<String, ByteArray, Long>>()
            var offset = 8
            while (offset < png.size) {
                val len = ((png[offset].toInt() and 0xFF) shl 24) or
                    ((png[offset + 1].toInt() and 0xFF) shl 16) or
                    ((png[offset + 2].toInt() and 0xFF) shl 8) or
                    (png[offset + 3].toInt() and 0xFF)
                val type = String(png, offset + 4, 4, Charsets.US_ASCII)
                val data = png.copyOfRange(offset + 8, offset + 8 + len)
                val crc = ((png[offset + 8 + len].toInt() and 0xFF) shl 24) or
                    ((png[offset + 9 + len].toInt() and 0xFF) shl 16) or
                    ((png[offset + 10 + len].toInt() and 0xFF) shl 8) or
                    (png[offset + 11 + len].toInt() and 0xFF)
                result.add(Triple(type, data, crc.toLong() and 0xFFFFFFFFL))
                offset += 12 + len
            }
            return result
        }

        test("the byte stream is a valid PNG: signature + IHDR + IDAT + IEND") {
            val pixels = intArrayOf(
                0xFF112233.toInt(), 0xFF445566.toInt(),
                0x80AABBCC.toInt(), 0x40DDEEFF.toInt(),
            )
            val png = pngOf(pixels, 2, 2)
            // signature
            DrsCertificatePng.PngCodec.SIGNATURE.forEachIndexed { i, b -> png[i] shouldBe b }
            val chunks = chunksOf(png)
            chunks.map { it.first } shouldContainExactly listOf("IHDR", "IDAT", "IEND")
        }

        test("IHDR carries the exact dimensions, 8-bit RGBA, no interlace") {
            val png = pngOf(IntArray(3 * 2) { 0xFF000000.toInt() }, 3, 2)
            val ihdr = chunksOf(png).first { it.first == "IHDR" }.second
            val w = ((ihdr[0].toInt() and 0xFF) shl 24) or ((ihdr[1].toInt() and 0xFF) shl 16) or
                ((ihdr[2].toInt() and 0xFF) shl 8) or (ihdr[3].toInt() and 0xFF)
            val h = ((ihdr[4].toInt() and 0xFF) shl 24) or ((ihdr[5].toInt() and 0xFF) shl 16) or
                ((ihdr[6].toInt() and 0xFF) shl 8) or (ihdr[7].toInt() and 0xFF)
            w shouldBe 3
            h shouldBe 2
            (ihdr[8].toInt() and 0xFF) shouldBe 8 // bit depth
            (ihdr[9].toInt() and 0xFF) shouldBe 6 // color type RGBA
            (ihdr[10].toInt() and 0xFF) shouldBe 0 // deflate
            (ihdr[12].toInt() and 0xFF) shouldBe 0 // no interlace
        }

        test("every chunk CRC verifies — the file is structurally sound") {
            val png = pngOf(IntArray(4 * 4) { it }, 4, 4)
            for ((type, data, crc) in chunksOf(png)) {
                val crc32 = CRC32()
                crc32.update(type.toByteArray(Charsets.US_ASCII))
                crc32.update(data)
                crc32.value shouldBe crc
            }
        }

        test("IDAT inflates to the exact raw scanlines (filter 0 + RGBA)") {
            val pixels = intArrayOf(0xFF112233.toInt(), 0x88445566.toInt())
            val png = pngOf(pixels, 2, 1)
            val idat = chunksOf(png).first { it.first == "IDAT" }.second
            val inflater = Inflater()
            inflater.setInput(idat)
            val raw = ByteArray(1 * (2 * 4 + 1))
            val n = inflater.inflate(raw)
            inflater.end()
            n shouldBe raw.size
            raw[0] shouldBe 0.toByte() // filter None
            // RGBA bytes of pixel 0 (ARGB 0xFF112233 → R=0x11 G=0x22 B=0x33 A=0xFF)
            raw[1] shouldBe 0x11.toByte()
            raw[2] shouldBe 0x22.toByte()
            raw[3] shouldBe 0x33.toByte()
            raw[4] shouldBe 0xFF.toByte()
            // pixel 1 (ARGB 0x88445566)
            raw[5] shouldBe 0x44.toByte()
            raw[6] shouldBe 0x55.toByte()
            raw[7] shouldBe 0x66.toByte()
            raw[8] shouldBe 0x88.toByte()
        }

        test("the generated PNG persists as a real file and re-reads byte-identical") {
            val dir = Files.createTempDirectory("drs-png").toFile()
            val png = pngOf(IntArray(8 * 8) { 0xFF204060.toInt() }, 8, 8)
            val out = File(dir, "certificate.png")
            out.writeBytes(png)
            File(out.path).readBytes() shouldBe png
            out.length().shouldBe(png.size.toLong())
        }
    }

    // ------------------------------- dashboard contract (the 5th store)

    context("dashboard contract — transparency_log is a registered store") {
        val attestation = DrsNetworkSentinel.Attestation(
            absoluteMode = false,
            rxSinceBaseline = 0, txSinceBaseline = 0, rxSincePrevious = 0, txSincePrevious = 0,
            countersSupported = true, zeroTraffic = true,
            permissions = emptyList(), checkedAt = 0,
        )

        test("summary() lists the transparency store with its real bytes; deleteAll() really destroys it") {
            val dir = Files.createTempDirectory("drs-dash").toFile()
            val file = File(dir, DrsTransparencyLog.FILE_NAME)
            file.writeText("""{"schema":"drs-transparency-log-1","firstUseAt":1,"days":[]}""")
            val dashboard = DrsPrivacyDashboard(
                stores = listOf(
                    DrsPrivacyDashboard.StoreProbe(
                        id = DrsPrivacyDashboard.STORE_TRANSPARENCY,
                        location = "noBackupFilesDir/${DrsTransparencyLog.FILE_NAME}",
                        measure = { DrsPrivacyDashboard.fileUsage(file) },
                        destroy = { DrsTransparencyLog.clear(file); true },
                    ),
                ),
                attestationProvider = { attestation },
            )
            val summary = dashboard.summary()
            summary.stores.map { it.id } shouldContainExactly listOf("transparency_log")
            summary.stores.single().bytes.shouldBe(file.length())
            val reports = dashboard.deleteAll()
            reports.single().deleted.shouldBeTrue()
            file.exists().shouldBeFalse()
        }
    }

    // --------------------------------- structural isolation (feature 1)

    context("structural isolation — feature 1") {
        val root = resolveRepoRoot()
        val main = File(root, "app/src/main/kotlin/com/drs/smartkeyboard")
        val f1Files = listOf(
            File(main, "drs/privacy/DrsTransparencyLog.kt"),
            File(main, "drs/privacy/DrsAgeCertificate.kt"),
            File(main, "drs/privacy/DrsCertificatePng.kt"),
        )

        test("feature-1 files import no network primitives and no TrafficStats") {
            for (f in f1Files) {
                f.exists().shouldBeTrue()
                val src = f.readText()
                src.shouldNotContain("java.net")
                src.shouldNotContain("okhttp")
                src.shouldNotContain("URLConnection")
                src.shouldNotContain("HttpURL")
                src.shouldNotContain("TrafficStats") // the hub READS attestations; only the Sentinel measures
            }
        }

        test("the IME tree holds zero references to the transparency feature") {
            val ime = File(main, "ime")
            val offenders = ime.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter {
                    it.readText().contains("DrsTransparencyLog") ||
                        it.readText().contains("DrsCertificatePng") ||
                        it.readText().contains("DrsAgeCertificate")
                }
                .map { it.name }
                .toList()
            offenders shouldHaveSize 0
        }

        test("the approved cache-path resource exists (owner decision 2) — a resource, not a permission") {
            val paths = File(root, "app/src/main/res/xml/file_paths.xml").readText()
            paths.shouldContain("name=\"transparency\"")
            paths.shouldContain("path=\"transparency/\"")
        }

        test("the dashboard registration cannot silently vanish") {
            val dash = File(main, "drs/privacy/DrsPrivacyDashboard.kt").readText()
            dash.shouldContain("STORE_TRANSPARENCY")
            dash.shouldContain("DrsTransparencyLog.FILE_NAME")
            val app = File(main, "DrsApplication.kt")
            app.readText().shouldContain("DrsTransparencyLog.Recorder.start")
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
    }
}
