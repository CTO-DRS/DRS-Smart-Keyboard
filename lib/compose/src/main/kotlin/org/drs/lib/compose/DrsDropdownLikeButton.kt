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

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate

/**
 * Read-only text field that behaves like a dropdown trigger: [item] is shown
 * through [org.drs.jetpref.material.ui.JetPrefTextField] with [appearance],
 * and the first press on the field hands control to [onClick] (the host then
 * opens the actual menu).
 */
@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun DrsDropdownLikeButton(
    item: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    isError: Boolean = false, onClick: () -> Unit = { },
    appearance: org.drs.jetpref.material.ui.JetPrefTextFieldAppearance =
        org.drs.jetpref.material.ui.JetPrefDropdownMenuDefaults.filled(),
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.wrapContentSize(androidx.compose.ui.Alignment.TopStart),
        content = {
            val source = androidx.compose.runtime.remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            }
            val pressed = source.collectIsPressedAsState().value

            if (pressed) { onClick() }

            org.drs.jetpref.material.ui.JetPrefTextField(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                value = item, onValueChange = { },
                enabled = true, readOnly = true, isError = isError, singleLine = true,
                trailingIcon = {
                    androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = true,
                        modifier = androidx.compose.ui.Modifier.rotate(degrees = 90f), // chevron right
                    )
                },
                appearance = appearance, interactionSource = source,
            )
        },
    )
}