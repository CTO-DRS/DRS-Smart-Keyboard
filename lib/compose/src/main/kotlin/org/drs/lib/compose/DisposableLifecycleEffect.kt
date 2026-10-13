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

/**
 * Observes [lifecycle] and forwards its foreground transitions: [onResume]
 * fires when the owner enters the resumed state, [onPause] when it leaves
 * it. The observer stays attached while this effect is in composition.
 */
@androidx.compose.runtime.Composable @androidx.compose.runtime.NonRestartableComposable
fun DisposableLifecycleEffect(
    lifecycle: androidx.lifecycle.Lifecycle =
        androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle,
    onResume: () -> Unit, onPause: () -> Unit,
) = androidx.compose.runtime.DisposableEffect(lifecycle) {
    val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
        override fun onResume(owner: androidx.lifecycle.LifecycleOwner) { onResume() }
        override fun onPause(owner: androidx.lifecycle.LifecycleOwner) { onPause() }
    }
    observer.let(lifecycle::addObserver)
    onDispose { lifecycle.removeObserver(observer) }
}
