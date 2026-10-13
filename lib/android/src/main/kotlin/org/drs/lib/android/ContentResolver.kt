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

@file:Suppress("NOTHING_TO_INLINE")

package org.drs.lib.android

import android.content.ContentResolver
import android.net.Uri
import org.drs.lib.kotlin.io.FsFile
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Runs [android.content.ContentResolver.query] with every optional clause unset. */
inline fun ContentResolver.query(uri: Uri) =
    query(uri, null, null, null, null)

/** Runs [android.content.ContentResolver.query] with [projection] set and every other clause unset. */
inline fun ContentResolver.query(uri: Uri, projection: Array<String>) =
    query(uri, projection, null, null, null)

/** Streams the payload of [uri] into [block], refusing payloads above [maxSize] bytes. */
inline fun ContentResolver.read(
    uri: Uri,
    maxSize: Long = Long.MAX_VALUE,
    block: (InputStream) -> Unit,
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    enforceReadableCapacity(maxSize)
    val source = openInputStream(uri)
        ?: throw IllegalStateException("Cannot open input stream for given uri '$uri'")
    val descriptor = openAssetFileDescriptor(uri, "r")
        ?: throw IllegalStateException("Cannot open asset file descriptor for given uri '$uri'")
    descriptor.use { checked ->
        val totalSize = checked.length
        if (totalSize != android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH && totalSize > maxSize) {
            throw IllegalStateException("Contents of given uri '$uri' exceeds maximum size of $maxSize bytes!")
        }
    }
    source.use(block)
}

@PublishedApi
internal inline fun enforceReadableCapacity(maxSize: Long) =
    require(maxSize > 0) { "Argument `maxSize` must be greater than 0" }

/** Copies the payload of [uri] into [file], replacing its previous contents. */
inline fun ContentResolver.readToFile(
    uri: Uri,
    file: FsFile,
) {
    read(uri) { source ->
        file.outputStream().use { destination ->
            source.copyTo(destination)
        }
    }
}

/** Streams the payload of [uri] through a buffered reader into [block]. */
inline fun ContentResolver.readText(
    uri: Uri,
    block: (BufferedReader) -> Unit,
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    read(uri) { source ->
        source.bufferedReader().use(block)
    }
}

/** Reads the whole payload of [uri] as text. */
inline fun ContentResolver.readAllText(
    uri: Uri,
): String {
    var collected: String? = null
    read(uri) { source ->
        collected = source.bufferedReader().use { it.readText() }
    }
    return collected!!
}

/** Opens [uri] for writing and hands the stream to [block], truncating prior content. */
inline fun ContentResolver.write(
    uri: Uri,
    block: (OutputStream) -> Unit,
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    openOutputStream(uri, "wt")
        ?.use(block)
        ?: throw IllegalStateException("Cannot open input stream for given uri '$uri'")
}

/** Copies the contents of [file] into [uri], replacing the provider's prior data. */
inline fun ContentResolver.writeFromFile(
    uri: Uri,
    file: FsFile,
) {
    write(uri) { destination ->
        file.inputStream().use { source ->
            source.copyTo(destination)
        }
    }
}

/** Streams the payload of [uri] through a buffered writer into [block]. */
inline fun ContentResolver.writeText(
    uri: Uri,
    block: (BufferedWriter) -> Unit,
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    write(uri) { target ->
        target.bufferedWriter().use(block)
    }
}

/** Replaces the payload of [uri] with [text]. */
inline fun ContentResolver.writeAllText(
    uri: Uri,
    text: String,
) {
    write(uri) { target ->
        target.bufferedWriter().use { it.write(text) }
    }
}
