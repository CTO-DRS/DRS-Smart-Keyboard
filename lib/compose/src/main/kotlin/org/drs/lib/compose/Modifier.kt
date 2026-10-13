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

/** Applies [modifier] onto the receiver only when [condition] holds. */
@androidx.compose.runtime.Composable
fun androidx.compose.ui.Modifier.conditional(
    condition: Boolean, modifier: @androidx.compose.runtime.Composable androidx.compose.ui.Modifier.() ->
        androidx.compose.ui.Modifier,
): androidx.compose.ui.Modifier =
    fold(condition, { modifier(androidx.compose.ui.Modifier) }, { androidx.compose.ui.Modifier })

/** Picks between [ifTrue] and [ifFalse] based on [condition] and chains the winner. */
@androidx.compose.runtime.Composable
fun androidx.compose.ui.Modifier.fold(
    condition: Boolean, ifTrue: @androidx.compose.runtime.Composable () -> androidx.compose.ui.Modifier,
    ifFalse: @androidx.compose.runtime.Composable () -> androidx.compose.ui.Modifier,
): androidx.compose.ui.Modifier = then(if (condition) ifTrue() else ifFalse())

/** Chains [modifier] when [value] is a [T], passing the cast instance. */
@androidx.compose.runtime.Composable
inline fun <reified T : Any> androidx.compose.ui.Modifier.ifIsInstance(
    value: Any, modifier: @androidx.compose.runtime.Composable (T) -> androidx.compose.ui.Modifier,
): androidx.compose.ui.Modifier =
    (value as? T)?.let { instance -> then(modifier(instance)) } ?: this
