/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package org.drs.lib.compose

/**
 * Emits a single grid item stretched across the current line's full span,
 * usable as a section header inside lazy grids; [key] enables stable
 * identity and state retention across recompositions.
 */
fun androidx.compose.foundation.lazy.grid.LazyGridScope.header(
    key: Any? = null,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.lazy.grid.LazyGridItemScope.() -> Unit,
) = item(
    key = key,
    span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) },
    content = content,
)
