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

@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package org.drs.lib.android

import android.content.Context
import androidx.annotation.StringRes
import org.drs.lib.kotlin.CurlyArg
import org.drs.lib.kotlin.curlyFormat
import kotlin.reflect.KClass

/**
 * Resolves and returns the framework service bound to [kClass] on this context.
 *
 * The lookup goes through the two-step framework protocol: first the service
 * name is derived from the requested class, then the context registry hands
 * back the live instance. A missing name mapping aborts with
 * [NullPointerException]; a mismatched registry entry aborts with
 * [ClassCastException].
 */
@Throws(
    NullPointerException::class,
    ClassCastException::class,
)
fun <T : Any> Context.systemService(kClass: KClass<T>): T =
    resolveSystemServiceName(kClass).let(this::getSystemService) as T

private fun <T : Any> Context.resolveSystemServiceName(kClass: KClass<T>): String =
    getSystemServiceName(kClass.java)!!

/**
 * Resolves and returns the framework service bound to [kClass] on this context,
 * or `null` when either the name mapping or the registry lookup fails.
 */
fun <T : Any> Context.systemServiceOrNull(kClass: KClass<T>): T? =
    swallowServiceFailure { systemService(kClass) }

private fun <T : Any> swallowServiceFailure(resolve: () -> T): T? =
    try {
        resolve()
    } catch (failure: Exception) {
        null
    }

/**
 * Returns the raw string resource for [id], with any styling information
 * stripped by the resource machinery itself.
 *
 * @throws android.content.res.Resources.NotFoundException when [id] is unknown.
 */
@Throws(
    android.content.res.Resources.NotFoundException::class,
)
inline fun Context.stringRes(@StringRes id: Int): String =
    resources.getString(id)

/**
 * Returns the raw string resource for [id] with [args] substituted through
 * [String.curlyFormat], again stripped of styling information.
 *
 * @throws android.content.res.Resources.NotFoundException when [id] is unknown.
 */
@Throws(
    android.content.res.Resources.NotFoundException::class,
)
inline fun Context.stringRes(@StringRes id: Int, vararg args: CurlyArg): String =
    resources.getString(id).curlyFormat(*args)
