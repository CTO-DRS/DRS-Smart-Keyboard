/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drs.lib.android

/**
 * Alias of the platform clipboard manager so the DRS Smart Keyboard and the
 * raw platform type can be referenced without name clashes.
 */
typealias AndroidClipboardManager =
    android.content.ClipboardManager

/**
 * Alias of the platform primary-clip listener. Kept as a separate declaration
 * because the compiler refuses nested typealiases on the parent type; revisit
 * only when the upstream Kotlin issue around nested typealias resolution is
 * resolved (tracked upstream as KT-34281).
 */
typealias AndroidClipboardManager_OnPrimaryClipChangedListener =
    android.content.ClipboardManager.OnPrimaryClipChangedListener

/** Clears the clip on API 28+, falling back to replacing it with an empty clip below. */
fun AndroidClipboardManager.clearPrimaryClipAnyApi(): Unit {
    when {
        AndroidVersion.ATLEAST_API28_P -> clearPrimaryClip()
        else -> setPrimaryClip(android.content.ClipData.newPlainText("", ""))
    }
}

/** Applies [clip] when present, otherwise clears whatever is currently stored. */
fun AndroidClipboardManager.setOrClearPrimaryClip(clip: android.content.ClipData?) =
    clip?.let { pinned -> setPrimaryClip(pinned) } ?: clearPrimaryClipAnyApi()
