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

package org.drs.lib.compose

import androidx.compose.ui.res.vectorResource
import org.drs.lib.kotlin.curlyFormat

/** Formatter factory shared by the ambient default and the provider below. */
private fun buildLocalizedDateTimeFormatter(
    locale: java.util.Locale,
): java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter
        .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(java.time.ZoneId.systemDefault())

/** Maps a view configuration onto the compose layout direction enum. */
private fun resolveConfigurationLayoutDirection(
    resources: android.content.res.Resources,
): androidx.compose.ui.unit.LayoutDirection =
    when (resources.configuration.layoutDirection) {
        android.view.View.LAYOUT_DIRECTION_LTR -> androidx.compose.ui.unit.LayoutDirection.Ltr
        android.view.View.LAYOUT_DIRECTION_RTL -> androidx.compose.ui.unit.LayoutDirection.Rtl
        else -> error(message = "Given configuration specifies invalid layout direction!")
    }

private val ambientResourcesContext =
    androidx.compose.runtime.staticCompositionLocalOf<android.content.Context> {
        error(message = "resources context not initialized!!")
    }

private val ambientAppNameString = androidx.compose.runtime.staticCompositionLocalOf {
    "DRS Keyboard"
}

private val ambientActualLayoutDirection = androidx.compose.runtime.staticCompositionLocalOf {
    androidx.compose.ui.unit.LayoutDirection.Ltr
}

/** Locale-aware date/time formatter exposed to the composition subtree. */
val LocalLocalizedDateTimeFormatter = androidx.compose.runtime.staticCompositionLocalOf {
    buildLocalizedDateTimeFormatter(java.util.Locale.ROOT)
}

/**
 * Wraps [content] in an environment where string/plural/drawable lookups go
 * through [resourcesContext] (for per-app language overrides), the app name
 * resolves through [appName], and the effective layout direction is taken
 * from the context unless [forceLayoutDirection] overrides it.
 */
@androidx.compose.runtime.Composable
fun ProvideLocalizedResources(
    resourcesContext: android.content.Context,
    @androidx.annotation.StringRes appName: Int,
    forceLayoutDirection: androidx.compose.ui.unit.LayoutDirection? = null,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val actualLayoutDirection =
        resolveConfigurationLayoutDirection(resourcesContext.resources)
    val effectiveLayoutDirection = forceLayoutDirection ?: actualLayoutDirection
    val localeList = resourcesContext.resources.configuration.locales
    val dateTimeFormatter = androidx.compose.runtime.remember(resourcesContext) {
        buildLocalizedDateTimeFormatter(
            when {
                localeList.isEmpty -> java.util.Locale.getDefault()
                else -> localeList[0]
            },
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        ambientResourcesContext provides resourcesContext,
        LocalLocalizedDateTimeFormatter provides dateTimeFormatter,
        ambientActualLayoutDirection provides actualLayoutDirection,
        androidx.compose.ui.platform.LocalLayoutDirection provides effectiveLayoutDirection,
        ambientAppNameString provides androidx.compose.ui.res.stringResource(appName),
    ) {
        content()
    }
}

/**
 * Re-exposes the real (unforced) layout direction as the effective one for
 * the wrapped subtree — useful for previews that force a direction above.
 */
@androidx.compose.runtime.Composable
fun ProvideActualLayoutDirection(
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val actualLayoutDirection = ambientActualLayoutDirection.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides actualLayoutDirection,
        content = content,
    )
}

/** Looks up [id] in the localized context and formats [args] into it. */
@androidx.compose.runtime.Composable
fun stringRes(
    @androidx.annotation.StringRes id: Int,
    vararg args: org.drs.lib.kotlin.CurlyArg,
): String {
    val raw = ambientResourcesContext.current.resources.getString(id)
    return formatString(raw, args)
}

/** Quantity-aware lookup of [id] with [quantity], formatted with [args]. */
@androidx.compose.runtime.Composable
fun pluralsRes(
    @androidx.annotation.PluralsRes id: Int,
    quantity: Int,
    vararg args: org.drs.lib.kotlin.CurlyArg,
): String {
    val raw = ambientResourcesContext.current.resources
        .getQuantityString(id, quantity)
    return formatString(raw, args)
}

/** Injects the "{app_name}" placeholder value alongside the caller's args. */
@androidx.compose.runtime.Composable
private fun formatString(
    string: String,
    args: Array<out org.drs.lib.kotlin.CurlyArg>,
): String {
    val appNameArg = "app_name" to ambientAppNameString.current
    return string.curlyFormat(appNameArg, *args)
}

/** Loads a vector drawable resource into compose's [ImageVector] model. */
@androidx.compose.runtime.Composable
fun drawableRes(
    @androidx.annotation.DrawableRes resId: Int,
): androidx.compose.ui.graphics.vector.ImageVector =
    androidx.compose.ui.graphics.vector.ImageVector.vectorResource(resId)
