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
 * Releases a gated settings observation: detaches the lifecycle observer and
 * unregisters the system settings listener in one call.
 */
private fun org.drs.lib.android.AndroidSettingsHelper.detachSettingsObservation(
    lifecycle: androidx.lifecycle.Lifecycle,
    eventObserver: androidx.lifecycle.LifecycleObserver,
    context: android.content.Context,
    observer: org.drs.lib.android.SystemSettingsObserver,
) {
    lifecycle.removeObserver(eventObserver)
    removeObserver(context = context, observer = observer)
}

/**
 * Gates a settings observation to the foreground: the listener registers on
 * resume and is released on pause, leaving no work done while backgrounded.
 */
private class ForegroundGatedSettingsObserver(
    private val helper: org.drs.lib.android.AndroidSettingsHelper,
    private val context: android.content.Context,
    private val key: String,
    private val listener: org.drs.lib.android.SystemSettingsObserver,
) : androidx.lifecycle.DefaultLifecycleObserver {
    override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
        helper.observe(context = context, key = key, observer = listener)
    }

    override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
        helper.removeObserver(context = context, observer = listener)
    }
}

/**
 * Observes a settings entry for as long as this composable stays alive,
 * exposing the raw string value; see the transforming overload below.
 */
@androidx.compose.runtime.Composable
fun org.drs.lib.android.AndroidSettingsHelper.observeAsState(
    key: String, foregroundOnly: Boolean = false,
) = observeAsState(key, foregroundOnly, { value -> value })

/**
 * Observes the settings entry [key] through this helper and exposes it as
 * compose state, re-reading via [transform] on every system change. With
 * [foregroundOnly] the underlying observation is attached on resume and
 * released on pause, otherwise it lives for the whole composition.
 */
@androidx.compose.runtime.Composable
fun <R> org.drs.lib.android.AndroidSettingsHelper.observeAsState(
    key: String, foregroundOnly: Boolean = false, transform: (String?) -> R,
): androidx.compose.runtime.State<R> {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val state = androidx.compose.runtime.remember(key) {
        androidx.compose.runtime.mutableStateOf(transform(getString(context, key)))
    }

    fun refreshObservedValue() {
        state.value = transform(getString(context = context, key = key))
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = org.drs.lib.android.SystemSettingsObserver(context) {
            refreshObservedValue()
        }
        when {
            foregroundOnly -> {
                val eventObserver: androidx.lifecycle.LifecycleObserver =
                    ForegroundGatedSettingsObserver(this@observeAsState, context, key, observer)
                val lifecycle = lifecycleOwner.lifecycle
                lifecycle.addObserver(eventObserver)
                onDispose {
                    detachSettingsObservation(
                        lifecycle, eventObserver, context, observer,
                    )
                }
            }
            else -> {
                observe(context = context, key = key, observer = observer)
                onDispose { removeObserver(context = context, observer = observer) }
            }
        }
    }
    return state as androidx.compose.runtime.State<R>
}
