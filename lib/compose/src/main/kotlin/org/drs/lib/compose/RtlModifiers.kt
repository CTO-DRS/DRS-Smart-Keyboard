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

import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Mirrors the laid-out content horizontally when the layout direction is
 * right-to-left, so asymmetric decorations (indicators, arrows, sliders)
 * keep pointing the same way they do in left-to-right layouts.
 */
fun Modifier.autoMirrorForRtl(): Modifier =
    composed {
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val shouldMirror =
            layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
        val mirroredScale: Float = when {
            shouldMirror -> -1f
            else -> 1f
        }
        this.graphicsLayer(
            scaleX = mirroredScale,
            scaleY = 1f,
        )
    }
