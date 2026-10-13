/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@Suppress("unused") object AndroidVersion {
    /** Android 8.1 **/
    inline val ATLEAST_API27_O_MR1: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.O_MR1)
        get() = sdkIntAtLeast(Build.VERSION_CODES.O_MR1)
    inline val ATMOST_API27_O_MR1: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.O_MR1)

    /** Android 9 **/
    inline val ATLEAST_API28_P: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.P)
        get() = sdkIntAtLeast(Build.VERSION_CODES.P)
    inline val ATMOST_API28_P: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.P)

    /** Android 10 **/
    inline val ATLEAST_API29_Q: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.Q)
        get() = sdkIntAtLeast(Build.VERSION_CODES.Q)
    inline val ATMOST_API29_Q: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.Q)

    /** Android 11 **/
    inline val ATLEAST_API30_R: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.R)
        get() = sdkIntAtLeast(Build.VERSION_CODES.R)
    inline val ATMOST_API30_R: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.R)

    /** Android 12 **/
    inline val ATLEAST_API31_S: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.S)
        get() = sdkIntAtLeast(Build.VERSION_CODES.S)
    inline val ATMOST_API31_S: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.S)

    /** Android 12L **/
    inline val ATLEAST_API32_S_V2: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.S_V2)
        get() = sdkIntAtLeast(Build.VERSION_CODES.S_V2)
    inline val ATMOST_API32_S_V2: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.S_V2)

    /** Android 13 **/
    inline val ATLEAST_API33_T: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
        get() = sdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
    inline val ATMOST_API33_T: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.TIRAMISU)

    /** Android 14 **/
    inline val ATLEAST_API34_U: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        get() = sdkIntAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    inline val ATMOST_API34_U: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

    /** Android 15 **/
    inline val ATLEAST_API35_V: Boolean
        @ChecksSdkIntAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        get() = sdkIntAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    inline val ATMOST_API35_V: Boolean
        get() = sdkIntAtMost(Build.VERSION_CODES.VANILLA_ICE_CREAM)

    @PublishedApi
    internal fun sdkIntAtLeast(code: Int): Boolean = Build.VERSION.SDK_INT >= code

    @PublishedApi
    internal fun sdkIntAtMost(code: Int): Boolean = Build.VERSION.SDK_INT <= code
}
