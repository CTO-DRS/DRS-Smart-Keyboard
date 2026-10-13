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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

private val DefaultScrollbarSize = 4.dp
// Fast-in / very-slow-out curve for the scrollbar fade
private val ScrollbarAnimationEasing =
    androidx.compose.animation.core.CubicBezierEasing(1f, 0f, 0.82f, -0.13f)

/** Thumb rectangle in canvas coordinates, resolved per orientation. */
private class ScrollbarThumb(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

/**
 * Shared fade animation driving every DRS scrollbar: opaque while scrolling
 * or during the initial reveal window, then easing out over 950 ms.
 */
@androidx.compose.runtime.Composable
private fun animatedRouteScrollbarAlpha(
    isScrollInProgress: Boolean,
): androidx.compose.runtime.State<Float> {
    val isInitial =
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val targetAlpha = when {
        isScrollInProgress || isInitial.value -> 1f
        else -> 0f
    }
    val duration = when {
        isScrollInProgress || isInitial.value -> 0
        else -> 950
    }
    val alpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = duration, easing = ScrollbarAnimationEasing,
        ),
    )
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1850)
        isInitial.value = false
    }
    return alpha
}

/** Theme scrollbar tint, overridable through [customColor] when specified. */
@androidx.compose.runtime.Composable
private fun resolvedScrollbarColor(
    customColor: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.graphics.Color = customColor.takeOrElse {
    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
}

/** Vertical scroll container with an optional fading edge scrollbar. */
fun androidx.compose.ui.Modifier.drsVerticalScroll(
    state: androidx.compose.foundation.ScrollState? = null,
    showScrollbar: Boolean = true,
    scrollbarWidth: androidx.compose.ui.unit.Dp = DefaultScrollbarSize,
): androidx.compose.ui.Modifier = composed {
    val scrollState = state ?: androidx.compose.foundation.rememberScrollState()
    when {
        showScrollbar ->
            verticalScroll(scrollState)
                .drsScrollbar(scrollState, scrollbarWidth, isVertical = true)
        else -> verticalScroll(scrollState)
    }
}

/** Horizontal scroll container with an optional fading edge scrollbar. */
fun androidx.compose.ui.Modifier.drsHorizontalScroll(
    state: androidx.compose.foundation.ScrollState? = null,
    showScrollbar: Boolean = true,
    scrollbarHeight: androidx.compose.ui.unit.Dp = DefaultScrollbarSize,
): androidx.compose.ui.Modifier = composed {
    val scrollState = state ?: androidx.compose.foundation.rememberScrollState()
    when {
        showScrollbar ->
            horizontalScroll(scrollState)
                .drsScrollbar(scrollState, scrollbarHeight, isVertical = false)
        else -> horizontalScroll(scrollState)
    }
}

/** Fading scrollbar thumb for a plain [androidx.compose.foundation.ScrollState]. */
fun androidx.compose.ui.Modifier.drsScrollbar(
    state: androidx.compose.foundation.ScrollState,
    scrollbarSize: androidx.compose.ui.unit.Dp = DefaultScrollbarSize,
    isVertical: Boolean,
): androidx.compose.ui.Modifier = composed {
    val alpha = animatedRouteScrollbarAlpha(state.isScrollInProgress)
    val scrollbarColor = resolvedScrollbarColor(androidx.compose.ui.graphics.Color.Unspecified)

    drawWithContent {
        drawContent()
        val needDrawScrollbar = state.isScrollInProgress || alpha.value > 0f
        if (needDrawScrollbar && state.maxValue > 0) {
            val scrollMax = state.maxValue.toFloat()
            val scrollFraction = state.value / scrollMax
            val thicknessPx = scrollbarSize.toPx()
            val thumb = when {
                isVertical -> {
                    val containerLength = size.height - scrollMax
                    val thumbLength = containerLength * (1f - scrollMax / size.height)
                    ScrollbarThumb(
                        x = size.width - thicknessPx,
                        y = state.value + (containerLength - thumbLength) * scrollFraction,
                        width = thicknessPx,
                        height = thumbLength,
                    )
                }
                else -> {
                    val containerLength = size.width - scrollMax
                    val thumbLength = containerLength * (1f - scrollMax / size.width)
                    ScrollbarThumb(
                        x = state.value + (containerLength - thumbLength) * scrollFraction,
                        y = size.height - thicknessPx,
                        width = thumbLength,
                        height = thicknessPx,
                    )
                }
            }
            drawRect(
                color = scrollbarColor,
                topLeft = androidx.compose.ui.geometry.Offset(thumb.x, thumb.y),
                size = androidx.compose.ui.geometry.Size(thumb.width, thumb.height),
                alpha = alpha.value,
            )
        }
    }
}

/** Fading scrollbar thumb sized from the visible item count of a lazy list. */
fun androidx.compose.ui.Modifier.drsScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    size: androidx.compose.ui.unit.Dp = DefaultScrollbarSize,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    isVertical: Boolean,
): androidx.compose.ui.Modifier = composed {
    val alpha = animatedRouteScrollbarAlpha(state.isScrollInProgress)
    val scrollbarColor = resolvedScrollbarColor(color)

    val layoutInfo = androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf { state.layoutInfo }
    }.value
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val visibleItems = when {
        visibleItemsInfo.isEmpty() -> 0
        else -> androidx.compose.runtime.remember { visibleItemsInfo.size }
    }
    drawWithContent {
        drawContent()
        val firstVisible = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha.value > 0f
        if (needDrawScrollbar && firstVisible != null) {
            val thicknessPx = size.toPx()
            val first = state.layoutInfo.visibleItemsInfo.first()
            val thumb = when {
                isVertical -> {
                    val itemLength = this.size.height / state.layoutInfo.totalItemsCount
                    ScrollbarThumb(
                        x = this.size.width - thicknessPx,
                        y = (firstVisible - percentOffset(first)) * itemLength,
                        width = thicknessPx,
                        height = visibleItems * itemLength,
                    )
                }
                else -> {
                    val itemLength = this.size.width / state.layoutInfo.totalItemsCount
                    ScrollbarThumb(
                        x = (firstVisible - percentOffset(first)) * itemLength,
                        y = this.size.height - thicknessPx,
                        width = visibleItems * itemLength,
                        height = thicknessPx,
                    )
                }
            }
            drawRect(
                color = scrollbarColor,
                topLeft = androidx.compose.ui.geometry.Offset(thumb.x, thumb.y),
                size = androidx.compose.ui.geometry.Size(thumb.width, thumb.height),
                alpha = alpha.value,
            )
        }
    }
}

/** Fading scrollbar thumb sized from the visible item count of a lazy grid. */
fun androidx.compose.ui.Modifier.drsScrollbar(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    size: androidx.compose.ui.unit.Dp = DefaultScrollbarSize,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
): androidx.compose.ui.Modifier = composed {
    val alpha = animatedRouteScrollbarAlpha(state.isScrollInProgress)
    val scrollbarColor = resolvedScrollbarColor(color)

    val layoutInfo = androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf { state.layoutInfo }
    }.value
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val orientation = layoutInfo.orientation
    val visibleItems = when {
        visibleItemsInfo.isEmpty() -> 0
        else -> androidx.compose.runtime.remember { visibleItemsInfo.size }
    }
    val last = visibleItemsInfo.lastOrNull()
    val stacks = when {
        last == null -> 0
        orientation == androidx.compose.foundation.gestures.Orientation.Vertical ->
            androidx.compose.runtime.remember { last.column }
        else -> androidx.compose.runtime.remember { last.row }
    }
    drawWithContent {
        drawContent()
        val firstVisible = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha.value > 0f
        if (needDrawScrollbar && firstVisible != null) {
            val thicknessPx = size.toPx()
            val first = state.layoutInfo.visibleItemsInfo.first()
            val thumb = when {
                orientation == androidx.compose.foundation.gestures.Orientation.Vertical -> {
                    val itemLength = this.size.height / state.layoutInfo.totalItemsCount
                    val thumbStart = (firstVisible - stacks * percentOffset(first, orientation)) *
                        itemLength
                    ScrollbarThumb(
                        x = this.size.width - thicknessPx,
                        y = thumbStart,
                        width = thicknessPx,
                        height = (visibleItems * itemLength).coerceAtMost(this.size.height - thumbStart),
                    )
                }
                else -> {
                    val itemLength = this.size.width / state.layoutInfo.totalItemsCount
                    val thumbStart = (firstVisible - stacks * percentOffset(first, orientation)) *
                        itemLength
                    ScrollbarThumb(
                        x = thumbStart,
                        y = this.size.height - thicknessPx,
                        width = (visibleItems * itemLength).coerceAtMost(this.size.height - thumbStart),
                        height = thicknessPx,
                    )
                }
            }
            drawRect(
                color = scrollbarColor,
                topLeft = androidx.compose.ui.geometry.Offset(thumb.x, thumb.y),
                size = androidx.compose.ui.geometry.Size(thumb.width, thumb.height),
                alpha = alpha.value,
            )
        }
    }
}

/** Item's offset on main axis as a percentage of its size. */
internal fun percentOffset(
    item: androidx.compose.foundation.lazy.LazyListItemInfo,
): Float = item.offset.toFloat() / item.size

/** Item's offset on the given axis as a percentage of its size on that axis. */
internal fun percentOffset(
    item: androidx.compose.foundation.lazy.grid.LazyGridItemInfo,
    orientation: androidx.compose.foundation.gestures.Orientation,
): Float {
    val offsetAlong = when (orientation) {
        androidx.compose.foundation.gestures.Orientation.Horizontal -> item.offset.x
        else -> item.offset.y
    }
    val sizeAlong = when (orientation) {
        androidx.compose.foundation.gestures.Orientation.Horizontal -> item.size.width
        else -> item.size.height
    }
    return offsetAlong.toFloat() / sizeAlong
}
