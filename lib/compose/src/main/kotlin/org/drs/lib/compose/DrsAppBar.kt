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

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

/**
 * DRS modern top app bar: transparent surface with a very subtle vertical
 * hero gradient, a bold title and a dynamic hairline divider that fades in
 * only while the content scrolls — a clean, premium look that adapts to
 * light/dark themes automatically. The hairline is drawn inside the bar so
 * it collapses together with the bar's shrinking height, never leaving an
 * orphaned line behind.
 */
@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun DrsAppBar(
    title: String,
    navigationIcon: (@androidx.compose.runtime.Composable () -> Unit)?,
    actions: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { },
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val dividerAlpha = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f) * 0.5f
    val heroGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            scheme.primary.copy(alpha = 0.06f),
            androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
    androidx.compose.material3.TopAppBar(
        title = {
            androidx.compose.material3.Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = scheme.onSurface,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        navigationIcon = navigationIcon ?: { },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = androidx.compose.ui.Modifier.drawBehind {
            drawRect(brush = heroGradient)
            // Hairline pinned to the live bottom edge of the collapsing bar
            val hairlinePx = 1.dp.toPx()
            val hairlineTop = size.height - hairlinePx
            drawRect(
                color = scheme.outlineVariant.copy(alpha = dividerAlpha),
                topLeft = androidx.compose.ui.geometry.Offset(0f, hairlineTop),
                size = androidx.compose.ui.geometry.Size(size.width, hairlinePx),
            )
        },
    )
}
