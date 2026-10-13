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

package org.drs.lib.android

import android.content.res.Configuration

/**
 * Reports whether this configuration's orientation matches the given platform
 * constant. Both public probes below route through this single comparison so
 * the orientation contract lives in exactly one place.
 */
private fun Configuration.orientationMatches(expected: Int): Boolean =
    orientation == expected

/** True when the configuration currently resolves to portrait orientation. */
fun Configuration.isOrientationPortrait(): Boolean =
    orientationMatches(expected = Configuration.ORIENTATION_PORTRAIT)

/** True when the configuration currently resolves to landscape orientation. */
fun Configuration.isOrientationLandscape(): Boolean =
    orientationMatches(expected = Configuration.ORIENTATION_LANDSCAPE)
