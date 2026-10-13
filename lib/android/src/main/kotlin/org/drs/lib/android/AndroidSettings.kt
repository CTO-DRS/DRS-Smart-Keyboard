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

import android.content.Context
import android.net.Uri
import android.provider.Settings
import org.drs.lib.kotlin.tryOrNull
import kotlin.reflect.KClass

/**
 * Gateway to one of the platform settings collections ([Settings.Global],
 * [Settings.Secure], [Settings.System]). Concrete subclasses bind the abstract
 * read/watch operations to a concrete collection; the key universe of each
 * collection is enumerated through reflection over its static string fields.
 */
abstract class AndroidSettingsHelper(private val kClass: KClass<*>, val groupId: String) {
    abstract fun getString(
        context: Context,
        key: String,
    ): String?

    abstract fun getUriFor(
        key: String,
    ): Uri?

    private fun reflectStaticStringFields(target: KClass<*>): Sequence<Pair<String, String>> =
        target.java.declaredFields.asSequence()
            .filter { field -> java.lang.reflect.Modifier.isStatic(field.modifiers) }
            .mapNotNull { field ->
                val value = try {
                    field.get(null) as? String
                } catch (ignored: Exception) {
                    null
                }
                value?.let { fetched -> field.name to fetched }
            }

    fun getAllKeys(): Sequence<Pair<String, String>> = reflectStaticStringFields(kClass)

    fun observe(context: Context, key: String, observer: SystemSettingsObserver) {
        val settingsUri = getUriFor(key) ?: return
        context.contentResolver.registerContentObserver(settingsUri, false, observer)
        observer.dispatchChange(false, settingsUri)
    }

    fun removeObserver(context: Context, observer: SystemSettingsObserver) {
        with(context.contentResolver) { unregisterContentObserver(observer) }
    }
}

private fun helperFor(
    settingsClass: KClass<*>,
    groupId: String,
    readString: (android.content.ContentResolver, String) -> String?,
    uriFor: (String) -> Uri?,
): AndroidSettingsHelper = object : AndroidSettingsHelper(settingsClass, groupId) {
    override fun getString(context: Context, key: String): String? = readString(context.contentResolver, key)

    override fun getUriFor(key: String): Uri? = uriFor(key)
}

object AndroidSettings {
    val Global = helperFor(
        settingsClass = Settings.Global::class,
        groupId = "global",
        readString = { resolver, key -> tryOrNull { Settings.Global.getString(resolver, key) } },
        uriFor = { key -> tryOrNull { Settings.Global.getUriFor(key) } },
    )

    val Secure = helperFor(
        settingsClass = Settings.Secure::class,
        groupId = "secure",
        readString = { resolver, key -> tryOrNull { Settings.Secure.getString(resolver, key) } },
        uriFor = { key -> tryOrNull { Settings.Secure.getUriFor(key) } },
    )

    val System = helperFor(
        settingsClass = Settings.System::class,
        groupId = "system",
        readString = { resolver, key -> tryOrNull { Settings.System.getString(resolver, key) } },
        uriFor = { key -> tryOrNull { Settings.System.getUriFor(key) } },
    )
}
