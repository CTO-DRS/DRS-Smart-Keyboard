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

package org.drs.lib.compose

import androidx.compose.foundation.clickable
import androidx.compose.ui.composed

/**
 * Click handler with a material ripple indication. Since composed blocks
 * remember per placement, a fresh interaction source is allocated here so
 * every clickable keeps its own ripple state.
 */
fun androidx.compose.ui.Modifier.rippleClickable(
    enabled: Boolean = true, onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null, onClick: () -> Unit,
): androidx.compose.ui.Modifier = composed {
    val source = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    clickable(
        onClick = onClick, interactionSource = source,
        enabled = enabled, onClickLabel = onClickLabel,
        indication = androidx.compose.material3.ripple(), role = role,
    )
}
