/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.drs.smartkeyboard.lib.devtools.flogError
import org.drs.jetpref.datastore.model.PreferenceSerializer

/**
 * ذاكرة مطبخ الإيموجي (the kitchen memory) — favorites + history for
 * both the composition engine and the sticker library, persisted as one
 * JetPref custom pref (the exact pattern of EmojiHistory / DrsSearchHistory).
 *
 * Doctrine:
 * - keys are stable memory keys: "pair:<base>\u0000<second>" for
 *   compositions, "sticker:<id>" for library stickers;
 * - history is capped at 24 with dedupe move-to-front;
 * - history recording is a pure function the panel only calls OUTSIDE
 *   incognito (the gate lives at the call site like every other DRS
 *   counter) — favorites are explicit user intent and stay allowed.
 */
@Serializable
data class DrsKitchenMemory(
    val favorites: List<String> = emptyList(),
    val history: List<String> = emptyList(),
) {
    /** The most recent history entries, newest first. */
    val recentKeys: List<String> get() = history

    fun isFavorite(key: String): Boolean = key in favorites

    fun withFavoriteToggled(key: String): DrsKitchenMemory = if (key in favorites) {
        copy(favorites = favorites.filter { it != key })
    } else {
        copy(favorites = favorites + key)
    }

    fun withHistoryPush(key: String): DrsKitchenMemory =
        copy(history = (listOf(key) + history.filter { it != key }).take(HISTORY_CAP))

    object Serializer : PreferenceSerializer<DrsKitchenMemory> {
        private val json = Json { ignoreUnknownKeys = true }

        override fun serialize(value: DrsKitchenMemory): String = json.encodeToString(serializer(), value)

        override fun deserialize(value: String): DrsKitchenMemory = try {
            json.decodeFromString(serializer(), value)
        } catch (e: Exception) {
            flogError { "Failed to deserialize DrsKitchenMemory: $e" }
            Empty
        }
    }

    companion object {
        /** The history cap — a glanceable row, never a flood. */
        const val HISTORY_CAP: Int = 24

        val Empty = DrsKitchenMemory()
    }
}
