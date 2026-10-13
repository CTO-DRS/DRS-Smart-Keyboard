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

/** Icon-plus-label pair shared by every button bar button variant. */
@androidx.compose.runtime.Composable
private fun BarIconAndLabel(
    text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?,
) {
    icon?.let { drawable ->
        androidx.compose.material3.Icon(
            imageVector = drawable,
            modifier = androidx.compose.ui.Modifier.padding(end = 8.dp), contentDescription = null,
        )
    }
    androidx.compose.material3.Text(text)
}

/**
 * Horizontal action bar used at dialog / screen bottoms: a hairline divider
 * in surface color, then a row hosting the [content] buttons right-aligned.
 */
@androidx.compose.runtime.Composable
fun DrsButtonBar(content: @androidx.compose.runtime.Composable DrsButtonBarScope.() -> Unit) {
    androidx.compose.material3.Surface(
        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp, modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(height = 1.dp),
            ) { }
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier
                    .padding(start = 0.dp, end = 16.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                content(DrsButtonBarScope(this))
            }
        }
    }
}

/** Scope granting bar buttons access to their parent row arrangement. */
class DrsButtonBarScope(
    rowScope: androidx.compose.foundation.layout.RowScope,
) : androidx.compose.foundation.layout.RowScope by rowScope {
    /** Primary action button, filled with the theme's primary color pair. */
    @androidx.compose.runtime.Composable
    fun ButtonBarButton(text: String,
        modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
        enabled: Boolean = true, icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        onClick: () -> Unit,
    ) = androidx.compose.material3.Button(
        enabled = enabled, modifier = modifier.padding(start = 16.dp), onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
        ),
    ) { BarIconAndLabel(text, icon) }

    /** Low-emphasis action button, tinted with the primary content color. */
    @androidx.compose.runtime.Composable
    fun ButtonBarTextButton(text: String,
        modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
        enabled: Boolean = true, icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        onClick: () -> Unit,
    ) = androidx.compose.material3.TextButton(
        enabled = enabled, modifier = modifier.padding(start = 16.dp), onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        ),
    ) { BarIconAndLabel(text, icon) }

    /** Gap that pushes preceding buttons to the leading bar edge. */
    @androidx.compose.runtime.Composable
    fun ButtonBarSpacer() =
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.weight(weight = 1f))
}
