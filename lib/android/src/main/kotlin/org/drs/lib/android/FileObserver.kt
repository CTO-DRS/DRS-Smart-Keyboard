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

import android.os.FileObserver
import java.io.File

/**
 * Builds a file observer over [file] (or the directory it names) listening for
 * the event mask [mask] and forwarding every platform event to [onEvent].
 * Observation stays dormant until the caller invokes startWatching() on the
 * returned observer.
 *
 * @see android.os.FileObserver
 */
fun FileObserver(
    file: File,
    mask: Int,
    onEvent: (event: Int, path: String?) -> Unit,
): FileObserver = if (AndroidVersion.ATLEAST_API29_Q) {
    makeFileObserver(file.path, mask, onEvent)
} else {
    @Suppress("DEPRECATION")
    makeFileObserver(file.absolutePath, mask, onEvent)
}

private fun makeFileObserver(
    observedPath: String,
    mask: Int,
    onEvent: (event: Int, path: String?) -> Unit,
): FileObserver = object : FileObserver(observedPath, mask) {
    override fun onEvent(event: Int, path: String?) = onEvent.invoke(event, path)
}
