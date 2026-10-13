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

/** Shared tween curve for all directional enter/exit transitions below. */
private fun <T> drsTransitionSpec(
    duration: Int,
): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    androidx.compose.animation.core.tween(duration)

/** Fades in while expanding vertically from [expandFrom]. */
fun androidx.compose.animation.EnterTransition.Companion.verticalTween(
    duration: Int,
    expandFrom: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.Bottom,
): androidx.compose.animation.EnterTransition =
    androidx.compose.animation.fadeIn(drsTransitionSpec(duration)) +
        androidx.compose.animation.expandVertically(drsTransitionSpec(duration), expandFrom)

/** Fades in while expanding horizontally from [expandFrom]. */
fun androidx.compose.animation.EnterTransition.Companion.horizontalTween(
    duration: Int,
    expandFrom: androidx.compose.ui.Alignment.Horizontal = androidx.compose.ui.Alignment.End,
): androidx.compose.animation.EnterTransition =
    androidx.compose.animation.fadeIn(drsTransitionSpec(duration)) +
        androidx.compose.animation.expandHorizontally(drsTransitionSpec(duration), expandFrom)

/** Fades out while shrinking vertically towards [shrinkTowards]. */
fun androidx.compose.animation.ExitTransition.Companion.verticalTween(
    duration: Int,
    shrinkTowards: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.Bottom,
): androidx.compose.animation.ExitTransition =
    androidx.compose.animation.fadeOut(drsTransitionSpec(duration)) +
        androidx.compose.animation.shrinkVertically(drsTransitionSpec(duration), shrinkTowards)

/** Fades out while shrinking horizontally towards [shrinkTowards]. */
fun androidx.compose.animation.ExitTransition.Companion.horizontalTween(
    duration: Int,
    shrinkTowards: androidx.compose.ui.Alignment.Horizontal = androidx.compose.ui.Alignment.End,
): androidx.compose.animation.ExitTransition =
    androidx.compose.animation.fadeOut(drsTransitionSpec(duration)) +
        androidx.compose.animation.shrinkHorizontally(drsTransitionSpec(duration), shrinkTowards)
