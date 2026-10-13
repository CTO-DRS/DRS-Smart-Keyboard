/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

import androidx.collection.SparseArrayCompat

/**
 * Removes the entry bound to [key] and hands back the value that was stored,
 * or yields `null` untouched when no entry exists for the key.
 */
fun <T> SparseArrayCompat<T>.removeAndReturn(
    key: Int,
): T? {
    val existing = get(key) ?: return null
    remove(
        key,
    )
    return existing
}
