/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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
 * Strokes a rectangular border fully inside [size], shifted by [topLeft].
 * The stroke is centered on the shrunken rectangle's edges, so half of the
 * stroke width bleeds outward on every side — matching the canvas contract.
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBorder(
    color: androidx.compose.ui.graphics.Color,
    stroke: androidx.compose.ui.graphics.drawscope.Stroke,
    topLeft: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    size: androidx.compose.ui.geometry.Size =
        androidx.compose.ui.geometry.Size(this.size.width - topLeft.x, this.size.height - topLeft.y),
    @androidx.annotation.FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
    blendMode: androidx.compose.ui.graphics.BlendMode =
        androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode,
) {
    val inset = stroke.width * 0.5f
    val shiftedTopLeft = androidx.compose.ui.geometry.Offset(
        topLeft.x + inset,
        topLeft.y + inset,
    )
    val innerSize = androidx.compose.ui.geometry.Size(
        size.width - stroke.width,
        size.height - stroke.width,
    )
    this.drawRect(
        color = color,
        topLeft = shiftedTopLeft,
        size = innerSize,
        alpha = alpha, style = stroke, colorFilter = colorFilter, blendMode = blendMode,
    )
}
