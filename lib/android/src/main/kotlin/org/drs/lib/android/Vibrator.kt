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

package org.drs.lib.android

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Returns the platform vibrator best suited for haptics on this device, or
 * `null` when the device carries no vibrator at all. On Android 12+ the
 * vibrator is obtained through the composited [VibratorManager]; older
 * platforms expose the single [Vibrator] service directly.
 */
fun Context.systemVibratorOrNull(): Vibrator? =
    resolvePlatformVibrator()?.takeIf { vibrator -> vibrator.hasVibrator() }

private fun Context.resolvePlatformVibrator(): Vibrator? = when {
    AndroidVersion.ATLEAST_API31_S -> systemServiceOrNull(VibratorManager::class)?.defaultVibrator
    else -> systemServiceOrNull(Vibrator::class)
}

/**
 * Plays a one-shot haptic of [duration] milliseconds scaled by [factor], with
 * an amplitude derived from [strength] (percent) scaled by [factor] when the
 * device supports amplitude control, falling back to the platform default
 * amplitude otherwise. Zero duration or zero strength is a no-op.
 */
fun Vibrator.vibrate(
    duration: Int,
    strength: Int,
    factor: Double = 1.0,
) {
    if (duration == 0 || strength == 0) { return }
    val activeDuration = (duration * factor).toLong().coerceAtLeast(1L)
    val activeStrength: Int = if (hasAmplitudeControl()) {
        (255.0 * (strength * factor / 100.0)).toInt().coerceIn(1, 255)
    } else {
        VibrationEffect.DEFAULT_AMPLITUDE
    }
    Log.d("Vibrator", "Perform haptic with duration=$activeDuration and strength=$activeStrength")
    vibrate(VibrationEffect.createOneShot(activeDuration, activeStrength))
}
