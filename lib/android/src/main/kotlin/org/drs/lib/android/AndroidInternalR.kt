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
 * Resolution table for framework-internal (`com.android.internal.R.*`) resources.
 *
 * Every identifier is resolved lazily at access time through the system-wide
 * [android.content.res.Resources] handle, because the numeric identifiers are
 * device- and build-dependent. Callers must remain defensive: a modified system
 * image or a future platform release may drop any given entry, in which case
 * the accessor yields the sentinel id `0`.
 */
object AndroidInternalR {
    private const val SYSTEM_PACKAGE = "android"
    private const val KIND_STRING = "string"
    private const val KIND_DRAWABLE = "drawable"

    private fun identifier(kind: String, name: String): Int =
        android.content.res.Resources.getSystem().getIdentifier(name, kind, SYSTEM_PACKAGE)

    @android.annotation.SuppressLint("DiscouragedApi") @Suppress("ClassName")
    object string : Any() {
        val ime_action_go: Int get() = identifier(KIND_STRING, "ime_action_go")
        val ime_action_search: Int get() = identifier(KIND_STRING, "ime_action_search")
        val ime_action_send: Int get() = identifier(KIND_STRING, "ime_action_send")
        val ime_action_next: Int get() = identifier(KIND_STRING, "ime_action_next")
        val ime_action_done: Int get() = identifier(KIND_STRING, "ime_action_done")
        val ime_action_previous: Int get() = identifier(KIND_STRING, "ime_action_previous")
        val ime_action_default: Int get() = identifier(KIND_STRING, "ime_action_default")
    }

    @android.annotation.SuppressLint("DiscouragedApi") @Suppress("ClassName")
    object drawable : Any() {
        val ic_qs_one_handed_mode: Int get() = identifier(KIND_DRAWABLE, "ic_qs_one_handed_mode")
    }
}
