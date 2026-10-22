/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * مكتبة الملصقات المحلية (the local sticker library) — the PURE index
 * layer of the DRS Emoji Kitchen feature.
 *
 * Sticker packs are ORIGINAL artwork authored for this project (see
 * docs/EMOJI_KITCHEN.md §4) and ship inside the APK: the index lives in
 * `assets/drs/kitchen/stickers.json`, the art in `res/drawable/st_kitchen_*.xml`.
 * The loader is fail-closed: any schema violation (wrong version, duplicate
 * ids, unknown category, empty keywords, MISSING LICENSE) rejects the whole
 * index — a library that cannot prove its license does not open.
 */
object DrsStickerLibrary {

    /** The closed sticker categories. */
    enum class DrsStickerCategory { SMILEYS, HEARTS, ANIMALS, FOOD, OBJECTS, CELEBRATION }

    @Serializable
    data class StickerPack(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val version: String,
        val author: String,
        val license: String,
        val licenseNote: String = "",
    )

    @Serializable
    data class Sticker(
        val id: String,
        val drawable: String,
        val category: DrsStickerCategory,
        val keywords: StickerKeywords,
    ) {
        /** The stable key used by favorites/history. */
        val memoryKey: String get() = "sticker:$id"
    }

    @Serializable
    data class StickerKeywords(val ar: List<String> = emptyList(), val en: List<String> = emptyList())

    @Serializable
    data class StickerIndex(
        val schemaVersion: Int,
        val pack: StickerPack,
        val stickers: List<Sticker>,
    )

    /** The asset path of the shipped index. */
    const val INDEX_ASSET: String = "drs/kitchen/stickers.json"

    /** The only accepted schema version. */
    const val SCHEMA_VERSION: Int = 1

    private val DRAWABLE_PATTERN = Regex("^st_kitchen_[a-z0-9_]+$")

    private val json = Json { ignoreUnknownKeys = false }

    /**
     * Parses and validates an index — null on ANY violation (fail-closed),
     * with the reason returned through [validate] for honest diagnostics.
     */
    fun load(raw: String): StickerIndex? {
        val doc = try {
            json.decodeFromString(StickerIndex.serializer(), raw)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return if (validate(doc) == null) doc else null
    }

    /** The full schema contract; null when the index is acceptable. */
    fun validate(doc: StickerIndex): String? {
        if (doc.schemaVersion != SCHEMA_VERSION) return "schemaVersion must be $SCHEMA_VERSION"
        val pack = doc.pack
        if (pack.id.isBlank()) return "pack.id required"
        if (pack.license.isBlank()) return "pack.license required"
        if (pack.nameAr.isBlank() || pack.nameEn.isBlank()) return "pack names required"
        val ids = doc.stickers.map { it.id }
        if (ids.isEmpty()) return "stickers required"
        if (ids.size != ids.toSet().size) return "sticker ids must be unique"
        for (s in doc.stickers) {
            if (!DRAWABLE_PATTERN.matches(s.drawable)) return "bad drawable name ${s.drawable}"
            if (s.keywords.ar.isEmpty() || s.keywords.en.isEmpty()) {
                return "keywords ar+en required for ${s.id}"
            }
        }
        return null
    }

    /** Honest search over id + ar + en keywords (same normalizer as the kitchen). */
    fun search(index: StickerIndex, query: String): List<Sticker> {
        val q = DrsEmojiKitchen.normalizeArabic(query)
        if (q.isEmpty()) return emptyList()
        return index.stickers.filter { s ->
            val hay = DrsEmojiKitchen.normalizeArabic(
                s.keywords.ar.joinToString(" ") + " " +
                    s.keywords.en.joinToString(" ") + " " + s.id,
            )
            q in hay
        }
    }

    /** Stickers of one category, in index order. */
    fun byCategory(index: StickerIndex, category: DrsStickerCategory): List<Sticker> =
        index.stickers.filter { it.category == category }

    /** Sticker lookup by id. */
    fun byId(index: StickerIndex, id: String): Sticker? =
        index.stickers.firstOrNull { it.id == id }
}
