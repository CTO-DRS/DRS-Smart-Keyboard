/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import kotlinx.serialization.Serializable

/**
 * مطبخ الإيموجي (the DRS Emoji Kitchen) — the PURE composition engine.
 *
 * An ORIGINAL, on-device emoji-combination experience: two system-font
 * emoji are brought together by a CLOSED catalog of 13 layout recipes —
 * no Google assets, no undocumented APIs, no network, no ML. The plan is
 * a deterministic layout contract; the Canvas renderer (drs/ui, Android
 * side) executes it into a real bitmap.
 *
 * Doctrine (mirrored 1:1 by scripts/check_emoji_kitchen.py — 563 checks):
 * - closed catalogs: 8 categories, 48 participants (6 per category),
 *   13 recipes, 11 anchor slots — nothing open-ended;
 * - honest silence: an uncovered category pair yields NO plan (never a
 *   fake preview), and an identical pair yields NO plan;
 * - determinism: same pair → the same plan byte-for-byte (the angular
 *   jitter derives from a stable pair hash inside ±7.5°);
 * - pure JVM: zero Android imports (pinned by a structural test).
 */
object DrsEmojiKitchen {

    /** The closed composition categories. */
    enum class DrsKitchenCategory { FACE, HEART, HANDS, ANIMAL, FOOD, OBJECT, SYMBOL, WEATHER }

    /** A kitchen participant: a system emoji with its category and keywords. */
    @Serializable
    data class KitchenEmoji(
        val value: String,
        val category: DrsKitchenCategory,
        val keywordAr: String,
        val keywordEn: String,
    )

    /** The closed participant catalog — 6 per category, 48 total. */
    val PARTICIPANTS: List<KitchenEmoji> = listOf(
        KitchenEmoji("😀", DrsKitchenCategory.FACE, "ابتسامة", "smile"),
        KitchenEmoji("😂", DrsKitchenCategory.FACE, "ضحك دموع", "joy"),
        KitchenEmoji("🥰", DrsKitchenCategory.FACE, "حب", "love"),
        KitchenEmoji("😎", DrsKitchenCategory.FACE, "نظارة رائع", "cool"),
        KitchenEmoji("🤔", DrsKitchenCategory.FACE, "تفكير", "think"),
        KitchenEmoji("😍", DrsKitchenCategory.FACE, "عيون قلوب", "heart eyes"),
        KitchenEmoji("❤️", DrsKitchenCategory.HEART, "قلب", "heart"),
        KitchenEmoji("💔", DrsKitchenCategory.HEART, "قلب مكسور", "broken"),
        KitchenEmoji("💖", DrsKitchenCategory.HEART, "قلب لامع", "sparkle heart"),
        KitchenEmoji("💙", DrsKitchenCategory.HEART, "قلب أزرق", "blue"),
        KitchenEmoji("🧡", DrsKitchenCategory.HEART, "قلب برتقالي", "orange"),
        KitchenEmoji("💚", DrsKitchenCategory.HEART, "قلب أخضر", "green"),
        KitchenEmoji("👍", DrsKitchenCategory.HANDS, "إعجاب", "thumbs up"),
        KitchenEmoji("👏", DrsKitchenCategory.HANDS, "تصفيق", "clap"),
        KitchenEmoji("🙏", DrsKitchenCategory.HANDS, "شكر", "thanks"),
        KitchenEmoji("👋", DrsKitchenCategory.HANDS, "تلويح", "wave"),
        KitchenEmoji("💪", DrsKitchenCategory.HANDS, "قوة", "muscle"),
        KitchenEmoji("🤝", DrsKitchenCategory.HANDS, "مصافحة", "handshake"),
        KitchenEmoji("🐱", DrsKitchenCategory.ANIMAL, "قط", "cat"),
        KitchenEmoji("🐶", DrsKitchenCategory.ANIMAL, "كلب", "dog"),
        KitchenEmoji("🦁", DrsKitchenCategory.ANIMAL, "أسد", "lion"),
        KitchenEmoji("🐼", DrsKitchenCategory.ANIMAL, "باندا", "panda"),
        KitchenEmoji("🦊", DrsKitchenCategory.ANIMAL, "ثعلب", "fox"),
        KitchenEmoji("🐰", DrsKitchenCategory.ANIMAL, "أرنب", "rabbit"),
        KitchenEmoji("🍎", DrsKitchenCategory.FOOD, "تفاح", "apple"),
        KitchenEmoji("🍕", DrsKitchenCategory.FOOD, "بيتزا", "pizza"),
        KitchenEmoji("☕", DrsKitchenCategory.FOOD, "قهوة", "coffee"),
        KitchenEmoji("🍰", DrsKitchenCategory.FOOD, "كيكة", "cake"),
        KitchenEmoji("🍉", DrsKitchenCategory.FOOD, "بطيخ", "watermelon"),
        KitchenEmoji("🥑", DrsKitchenCategory.FOOD, "أفوكادو", "avocado"),
        KitchenEmoji("⚽", DrsKitchenCategory.OBJECT, "كرة", "ball"),
        KitchenEmoji("🎁", DrsKitchenCategory.OBJECT, "هدية", "gift"),
        KitchenEmoji("📱", DrsKitchenCategory.OBJECT, "هاتف", "phone"),
        KitchenEmoji("🚀", DrsKitchenCategory.OBJECT, "صاروخ", "rocket"),
        KitchenEmoji("🎸", DrsKitchenCategory.OBJECT, "قيثارة", "guitar"),
        KitchenEmoji("💡", DrsKitchenCategory.OBJECT, "مصباح", "bulb"),
        KitchenEmoji("⭐", DrsKitchenCategory.SYMBOL, "نجمة", "star"),
        KitchenEmoji("🔥", DrsKitchenCategory.SYMBOL, "نار", "fire"),
        KitchenEmoji("✨", DrsKitchenCategory.SYMBOL, "لمعان", "sparkles"),
        KitchenEmoji("💯", DrsKitchenCategory.SYMBOL, "مئة", "hundred"),
        KitchenEmoji("🎯", DrsKitchenCategory.SYMBOL, "هدف", "target"),
        KitchenEmoji("⚡", DrsKitchenCategory.SYMBOL, "برق", "zap"),
        KitchenEmoji("☀️", DrsKitchenCategory.WEATHER, "شمس", "sun"),
        KitchenEmoji("🌙", DrsKitchenCategory.WEATHER, "قمر", "moon"),
        KitchenEmoji("🌈", DrsKitchenCategory.WEATHER, "قوس قزح", "rainbow"),
        KitchenEmoji("⛅", DrsKitchenCategory.WEATHER, "غيم شمس", "cloudy"),
        KitchenEmoji("❄️", DrsKitchenCategory.WEATHER, "ثلج", "snow"),
        KitchenEmoji("🌧️", DrsKitchenCategory.WEATHER, "مطر", "rain"),
    )

    /** One drawn layer of a plan (a glyph + where/how it sits). */
    @Serializable
    data class KitchenLayer(
        val glyph: String,
        val cx: Float,
        val cy: Float,
        val scale: Float,
        val rotationDeg: Float,
    )

    /** A full composition plan — the deterministic contract the renderer executes. */
    @Serializable
    data class KitchenPlan(
        val recipeId: String,
        val base: String,
        val second: String,
        val layers: List<KitchenLayer>,
    ) {
        /** The stable pair key used by favorites/history. */
        val pairKey: String get() = "pair:$base\u0000$second"

        /** The honest text composition of the pair (the fallback send path). */
        val textPair: String get() = base + second
    }

    /** The closed anchor slots (relative canvas coordinates). */
    private val SLOTS: Map<String, Pair<Float, Float>> = mapOf(
        "BASE_CENTER" to (0.50f to 0.50f),
        "SECOND_CENTER" to (0.50f to 0.50f),
        "BOTTOM_RIGHT" to (0.74f to 0.72f),
        "TOP_RIGHT" to (0.76f to 0.30f),
        "BOTTOM_CENTER" to (0.50f to 0.78f),
        "RIGHT_OVERLAP" to (0.68f to 0.46f),
        "TOP_BACK" to (0.50f to 0.50f),
        "FRONT_CENTER" to (0.50f to 0.54f),
        "LEFT_HALF" to (0.36f to 0.50f),
        "RIGHT_HALF" to (0.66f to 0.50f),
        "TOP_CENTER" to (0.50f to 0.28f),
    )

    /** A closed recipe: (id, primary category, secondary category, layers). */
    private data class Recipe(
        val id: String,
        val primary: DrsKitchenCategory,
        val secondary: DrsKitchenCategory,
        val layers: List<Pair<String, Float>>,
    )

    /** The closed recipe catalog — 13 archetypes, first match wins. */
    private val RECIPES: List<Recipe> = listOf(
        Recipe("face_heart", DrsKitchenCategory.FACE, DrsKitchenCategory.HEART, listOf("BASE_CENTER" to 0.62f, "BOTTOM_RIGHT" to 0.34f)),
        Recipe("face_face", DrsKitchenCategory.FACE, DrsKitchenCategory.FACE, listOf("BASE_CENTER" to 0.66f, "RIGHT_OVERLAP" to 0.52f)),
        Recipe("face_hands", DrsKitchenCategory.FACE, DrsKitchenCategory.HANDS, listOf("BASE_CENTER" to 0.66f, "BOTTOM_CENTER" to 0.42f)),
        Recipe("face_food", DrsKitchenCategory.FACE, DrsKitchenCategory.FOOD, listOf("BASE_CENTER" to 0.66f, "TOP_RIGHT" to 0.36f)),
        Recipe("face_animal", DrsKitchenCategory.ANIMAL, DrsKitchenCategory.FACE, listOf("TOP_BACK" to 0.80f, "FRONT_CENTER" to 0.54f)),
        Recipe("heart_face", DrsKitchenCategory.HEART, DrsKitchenCategory.FACE, listOf("TOP_BACK" to 0.92f, "FRONT_CENTER" to 0.50f)),
        Recipe("heart_object", DrsKitchenCategory.HEART, DrsKitchenCategory.OBJECT, listOf("TOP_BACK" to 0.88f, "FRONT_CENTER" to 0.46f)),
        Recipe("heart_animal", DrsKitchenCategory.HEART, DrsKitchenCategory.ANIMAL, listOf("TOP_BACK" to 0.88f, "FRONT_CENTER" to 0.50f)),
        Recipe("heart_food", DrsKitchenCategory.HEART, DrsKitchenCategory.FOOD, listOf("TOP_BACK" to 0.88f, "FRONT_CENTER" to 0.46f)),
        Recipe("object_symbol", DrsKitchenCategory.OBJECT, DrsKitchenCategory.SYMBOL, listOf("BASE_CENTER" to 0.68f, "TOP_RIGHT" to 0.32f)),
        Recipe("weather_face", DrsKitchenCategory.WEATHER, DrsKitchenCategory.FACE, listOf("TOP_BACK" to 0.78f, "FRONT_CENTER" to 0.50f)),
        Recipe("symbol_symbol", DrsKitchenCategory.SYMBOL, DrsKitchenCategory.SYMBOL, listOf("LEFT_HALF" to 0.54f, "RIGHT_HALF" to 0.54f)),
        Recipe("hands_heart", DrsKitchenCategory.HANDS, DrsKitchenCategory.HEART, listOf("BASE_CENTER" to 0.72f, "TOP_CENTER" to 0.36f)),
    )

    private val RECIPE_BY_PAIR: Map<Pair<DrsKitchenCategory, DrsKitchenCategory>, Recipe> =
        RECIPES.associateBy { it.primary to it.secondary }

    private val BY_VALUE: Map<String, KitchenEmoji> = PARTICIPANTS.associateBy { it.value }

    /** Deterministic pair jitter inside ±7.5° (stable hash, never random). */
    private fun jitterOf(base: String, second: String): Float {
        var h = 2166136261L
        for (c in base + second) {
            h = h xor c.code.toLong()
            h *= 16777619L
        }
        val bucket = ((h ushr 1) % 7).toInt() - 3
        return bucket * 2.5f
    }

    /**
     * The deterministic plan for a pair — or null when honestly silent
     * (unknown emoji, identical pair, or an uncovered category pair).
     */
    fun plan(base: String, second: String): KitchenPlan? {
        val b = BY_VALUE[base] ?: return null
        val s = BY_VALUE[second] ?: return null
        if (base == second) return null
        val recipe = RECIPE_BY_PAIR[b.category to s.category] ?: return null
        val jitter = jitterOf(base, second)
        val layers = recipe.layers.mapIndexed { index, (slot, scale) ->
            val (cx, cy) = SLOTS.getValue(slot)
            val rot = if (slot == "RIGHT_OVERLAP" || slot == "TOP_BACK" || slot == "FRONT_CENTER") jitter else 0f
            KitchenLayer(
                glyph = if (index == 0) base else second,
                cx = cx,
                cy = cy,
                scale = scale,
                rotationDeg = rot,
            )
        }
        return KitchenPlan(recipe.id, base, second, layers)
    }

    /** The suggestion cap — a glanceable grid, never a flood. */
    const val SUGGESTION_LIMIT: Int = 12

    /**
     * Deterministic suggestions for a base emoji: every curated candidate
     * whose category pair has a recipe with the base's category, in
     * catalog order, capped. Every entry is guaranteed renderable.
     */
    fun suggestions(base: String, limit: Int = SUGGESTION_LIMIT): List<String> {
        val b = BY_VALUE[base] ?: return emptyList()
        val out = ArrayList<String>(limit)
        for (candidate in PARTICIPANTS) {
            if (out.size >= limit) break
            if (candidate.value == base) continue
            val pair = b.category to candidate.category
            val mirrored = candidate.category to b.category
            if (RECIPE_BY_PAIR.containsKey(pair) || RECIPE_BY_PAIR.containsKey(mirrored)) {
                out.add(candidate.value)
            }
        }
        return out
    }

    /** Arabic normalization — tashkeel off, hamza forms unified, ta/alef variants unified. */
    fun normalizeArabic(input: String): String = input
        .map { ch ->
            when (ch) {
                in '\u064B'..'\u0652' -> null
                '\u0670' -> null
                'أ', 'إ', 'آ' -> 'ا'
                'ة' -> 'ه'
                'ى' -> 'ي'
                else -> ch
            }
        }
        .filterNotNull()
        .joinToString("")
        .lowercase()
        .trim()

    /** Honest keyword search over the participant catalog (ar + en + the value). */
    fun searchEmoji(query: String): List<String> {
        val q = normalizeArabic(query)
        if (q.isEmpty()) return emptyList()
        val hits = ArrayList<String>()
        for (e in PARTICIPANTS) {
            val hay = normalizeArabic(e.keywordAr + " " + e.keywordEn + " " + e.value)
            if (q in hay) hits.add(e.value)
        }
        return hits
    }

    /** Category lookup for the panel (null for non-participants). */
    fun categoryOf(value: String): DrsKitchenCategory? = BY_VALUE[value]?.category

    /** Emoji lookup by value. */
    fun emojiOf(value: String): KitchenEmoji? = BY_VALUE[value]
}
