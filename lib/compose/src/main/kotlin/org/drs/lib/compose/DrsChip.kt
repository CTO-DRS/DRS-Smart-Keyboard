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

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

/** Renders one stack of chip icons, spacing them toward the label side. */
@androidx.compose.runtime.Composable
private fun ChipIconStack(
    icons: List<androidx.compose.ui.graphics.vector.ImageVector>,
    towardLabelEnd: Boolean,
) {
    androidx.compose.foundation.layout.Row {
        for (icon in icons) {
            val spacing = when {
                towardLabelEnd -> androidx.compose.ui.Modifier.padding(end = 8.dp)
                else -> androidx.compose.ui.Modifier.padding(start = 8.dp)
            }
            androidx.compose.material3.Icon(
                imageVector = icon,
                modifier = spacing.size(size = 16.dp), contentDescription = null,
            )
        }
    }
}

/**
 * A selectable filter chip showing [text] plus optional icon stacks before
 * and after the label; icons are clipped at 16 dp so chip rows stay even.
 */
@androidx.compose.runtime.Composable
fun DrsChip(
    text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onClick: () -> Unit = { }, selected: Boolean = false, enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape =
        androidx.compose.material3.MaterialTheme.shapes.small,
    leadingIcons: List<androidx.compose.ui.graphics.vector.ImageVector> = listOf(),
    trailingIcons: List<androidx.compose.ui.graphics.vector.ImageVector> = listOf(),
) {
    androidx.compose.material3.InputChip(
        selected = selected, onClick = onClick, enabled = enabled, shape = shape,
        label = {
            androidx.compose.material3.Text(
                text = text, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        modifier = modifier, leadingIcon = { ChipIconStack(leadingIcons, towardLabelEnd = true) },
        trailingIcon = { ChipIconStack(trailingIcons, towardLabelEnd = false) },
    )
}
