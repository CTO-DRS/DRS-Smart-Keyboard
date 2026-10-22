/*
 * Copyright (C) 2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.lib.ext

import com.drs.smartkeyboard.ime.keyboard.KeyboardExtension
import com.drs.smartkeyboard.ime.nlp.LanguagePackExtension
import com.drs.smartkeyboard.ime.theme.ThemeExtension
import com.drs.smartkeyboard.ime.theme.ThemeExtensionComponent
import com.drs.smartkeyboard.lib.io.ZipUtils
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.drs.lib.kotlin.io.FsDir
import org.drs.lib.kotlin.io.FsFile
import org.drs.lib.kotlin.io.readJson
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory

/**
 * «عقد الحزم الحقيقية» — the published `.flex` assets in the repository's
 * `packages/` directory must import through the EXACT production chain the
 * app runs for both the Package Center (DrsPackageManager.importVerifiedPackage)
 * and the manual file import (CacheManager.readFromUriIntoCache):
 *
 *   ZipUtils.unzip → extension.json → ExtensionJsonConfig polymorphic decode
 *   → id/type cross-check → stylesheet presence → pinned ed25519 signature
 *   over the raw file bytes → manifest sha256.
 *
 * This suite exists because v1.4.1's released packages failed on-device with
 * "corrupted archive" while every asset byte, checksum and signature was
 * provably intact — a pure parse-side failure this contract now pins forever.
 */
class DrsRealPackagesImportTest : FunSpec({

    val packagesDir: File = sequenceOf(File("../packages"), File("packages"))
        .firstOrNull { it.isDirectory }
        ?: error("packages/ directory not found — run from app/ module or repo root")

    val flexFiles: List<File> = packagesDir.listFiles { f -> f.name.endsWith(".flex") }
        .orEmpty()
        .sortedBy { it.name }

    fun manifestRoot() = FsFile(packagesDir, "manifest.json")
        .readText()
        .let { kotlinx.serialization.json.Json.parseToJsonElement(it).jsonObject }

    fun manifestEntryFor(flexFile: File) = manifestRoot()["packages"]!!.jsonArray
        .map { it.jsonObject }
        .firstOrNull {
            val id = it["package_id"]!!.jsonPrimitive.content
            val version = it["version"]!!.jsonPrimitive.content
            "pkg-${id.substringAfterLast('.')}-v$version.flex" == flexFile.name
        } ?: error("no manifest entry for ${flexFile.name}")

    test("the packages directory ships the full official catalog (8 signed themes)") {
        flexFiles.size shouldBe 8
    }

    flexFiles.forEach { flexFile ->
        test("${flexFile.name} imports through the exact production chain") {
            val rawBytes = flexFile.readBytes()
            val entry = manifestEntryFor(flexFile)

            // 1) unzip — the exact production unzip (ZipUtils.unzip, same caps)
            val staging = createTempDirectory("drs-pkg-${flexFile.name}").toFile()
            try {
                ZipUtils.unzip(srcFile = flexFile, dstDir = staging)

                // 2) manifest decode — the exact production polymorphic parse
                val manifestFile = FsFile(staging, ExtensionDefaults.MANIFEST_FILE_NAME)
                manifestFile.exists() shouldBe true
                val ext: Extension = manifestFile.readJson(ExtensionJsonConfig)

                // 3) id/type cross-check — mirrors importVerifiedPackage's gates
                val pkgId = entry["package_id"]!!.jsonPrimitive.content
                ext.meta.id shouldBe pkgId
                val actualType = when (ext) {
                    is ThemeExtension -> "ime/theme"
                    is KeyboardExtension -> "ime/keyboard"
                    is LanguagePackExtension -> "ime/languagepack"
                    else -> error("unknown package type for ${flexFile.name}")
                }
                actualType shouldBe entry["type"]!!.jsonPrimitive.content

                // 4) every theme component's stylesheet must exist in the archive
                ext.components().forEach { component ->
                    val theme = component as ThemeExtensionComponent
                    val sheet = FsFile(staging, theme.stylesheetPath())
                    sheet.exists() shouldBe true
                }

                // 5) pinned ed25519 signature over the raw file bytes
                val signature = entry["signature"]!!.jsonPrimitive.content
                val pubkeyId = entry["pubkey_id"]!!.jsonPrimitive.content
                DrsPackageTrust.verifyPackageSignature(rawBytes, signature, pubkeyId) shouldBe true

                // 6) sha256 contract of the manifest matches the shipped file
                val sha = MessageDigest.getInstance("SHA-256").digest(rawBytes)
                    .joinToString("") { "%02x".format(it) }
                sha shouldBe entry["sha256"]!!.jsonPrimitive.content
            } finally {
                staging.deleteRecursively()
            }
        }
    }
})
