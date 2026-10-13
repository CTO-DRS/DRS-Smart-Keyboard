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

package org.drs.lib.compose

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

/**
 * A tiny round separator dot for toolbar rows: an 8 dp horizontal gap
 * followed by a 4 dp circle tinted with the theme's outline color, kept
 * vertically centered relative to its row siblings.
 */
@androidx.compose.runtime.Composable
fun androidx.compose.foundation.layout.RowScope.DrsBulletSpacer(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) = androidx.compose.material3.Surface(
    shape = androidx.compose.foundation.shape.CircleShape,
    color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
    modifier = modifier
        .align(androidx.compose.ui.Alignment.CenterVertically)
        .padding(start = 8.dp, end = 8.dp)
        .size(size = 4.dp),
    content = { },
)
