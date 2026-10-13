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

import org.drs.lib.kotlin.io.FsDir
import org.drs.lib.kotlin.io.FsFile
import org.drs.lib.kotlin.io.subDir
import org.drs.lib.kotlin.io.subFile
import java.io.BufferedReader
import java.io.Reader
import java.nio.charset.Charset

/**
 * Public alias of the platform asset manager, the entry point for reading files
 * statically shipped inside the APK assets. The alias lets the DRS Smart
 * Keyboard coexist with the raw platform type without name collisions.
 */
typealias AndroidAssetManager =
    android.content.res.AssetManager

/** Opens [path] from the APK assets as a fresh [Reader] decoded with [charset]. */
fun AndroidAssetManager.reader(path: String, charset: Charset = Charsets.UTF_8): Reader =
    open(path).reader(charset)

/**
 * Opens [path] from the APK assets as a fresh buffered [BufferedReader] decoded
 * with [charset]. Referenced by name from the emoji pipeline (contract: keep).
 */
fun AndroidAssetManager.bufferedReader(path: String, charset: Charset = Charsets.UTF_8): BufferedReader =
    open(path).bufferedReader(charset)

/**
 * Loads the complete text of [path] from the APK assets, decoded with [charset].
 * The whole payload is held in memory, so oversized assets will exhaust the heap.
 */
fun AndroidAssetManager.readText(path: String, charset: Charset = Charsets.UTF_8): String =
    reader(path, charset).use { source -> source.readText() }

/** Streams the raw bytes of asset [path] into [dst], replacing its contents. */
fun AndroidAssetManager.copy(
    path: String,
    dst: FsFile,
) {
    open(path).use { source ->
        dst.outputStream().use { destination ->
            source.copyTo(destination)
        }
    }
}

/**
 * Recursively streams the asset subtree rooted at [path] into [dst]. A [path]
 * that addresses a plain file degrades to a single [copy] of that file. The
 * operation is not atomic: an IO failure mid-way leaves a partial copy behind.
 */
fun AndroidAssetManager.copyRecursively(
    path: String,
    dst: FsDir,
) {
    copyFromApkAssets(root = path, relative = "", target = dst)
}

private fun AndroidAssetManager.copyFromApkAssets(root: String, relative: String, target: FsDir) {
    val currentPath = when {
        root.isBlank() -> relative
        relative.isBlank() -> root
        else -> "$root/$relative"
    }
    val entries = list(currentPath) ?: return
    if (entries.isEmpty()) {
        copy(currentPath, target.subFile(relative))
    } else {
        target.subDir(relative).apply { mkdirs() }
        entries.forEach { entry ->
            copyFromApkAssets(root, if (relative.isBlank()) entry else "$relative/$entry", target)
        }
    }
}
