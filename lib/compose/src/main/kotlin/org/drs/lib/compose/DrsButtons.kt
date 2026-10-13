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

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Leading icon plus label, the shared interior of every textful button. */
@androidx.compose.runtime.Composable
private fun ButtonLabel(icon: androidx.compose.ui.graphics.vector.ImageVector?, text: String) {
    if (icon != null) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            modifier = androidx.compose.ui.Modifier
                .padding(end = androidx.compose.material3.ButtonDefaults.IconSpacing)
                .size(size = androidx.compose.material3.ButtonDefaults.IconSize),
            contentDescription = null,
        )
    }
    androidx.compose.material3.Text(text)
}

/** Filled accent button with an optional leading [icon] before [text]. */
@androidx.compose.runtime.Composable
fun DrsButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape =
        androidx.compose.material3.MaterialTheme.shapes.small,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.material3.ButtonDefaults.ContentPadding,
    colors: androidx.compose.material3.ButtonColors =
        androidx.compose.material3.ButtonDefaults.buttonColors(),
) {
    androidx.compose.material3.Button(
        onClick = onClick, modifier = modifier, enabled = enabled,
        shape = shape, colors = colors, contentPadding = contentPadding,
    ) { ButtonLabel(icon, text) }
}

/** Outlined medium-emphasis button with an optional leading [icon]. */
@androidx.compose.runtime.Composable
fun DrsOutlinedButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape =
        androidx.compose.material3.MaterialTheme.shapes.small,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.material3.ButtonDefaults.ContentPadding,
    colors: androidx.compose.material3.ButtonColors =
        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick, modifier = modifier, enabled = enabled,
        shape = shape, colors = colors, contentPadding = contentPadding,
    ) { ButtonLabel(icon, text) }
}

/** Text-only low-emphasis button with an optional leading [icon]. */
@androidx.compose.runtime.Composable
fun DrsTextButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.ButtonDefaults.shape,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.material3.ButtonDefaults.TextButtonContentPadding,
    colors: androidx.compose.material3.ButtonColors =
        androidx.compose.material3.ButtonDefaults.textButtonColors(),
) {
    androidx.compose.material3.TextButton(
        onClick = onClick, modifier = modifier, enabled = enabled,
        shape = shape, colors = colors, contentPadding = contentPadding,
    ) { ButtonLabel(icon, text) }
}

/**
 * Icon-only button; [contentDescription] feeds TalkBack (a DRS v1.28.0 audit
 * fix — the old implicit null made buttons announce nothing at all), while
 * disabled icons render at the Material disabled-content alpha 0.38.
 */
@androidx.compose.runtime.Composable
fun DrsIconButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    iconModifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    iconColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    contentDescription: String? = null,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick, modifier = modifier, enabled = enabled,
    ) {
        val contentAlpha = if (enabled) 1f else 0.38f
        val contentColor = iconColor
            .takeOrElse { androidx.compose.material3.LocalContentColor.current }
            .copy(alpha = contentAlpha)
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon, modifier = iconModifier,
                contentDescription = contentDescription,
            )
        }
    }
}

/**
 * Arbitrary-content pressable box styled like an icon button, extended with
 * long-press support; shape, colors and size are fully caller-controlled.
 * Layout follows the material3 IconButton reference implementation.
 */
@androidx.compose.runtime.Composable
fun DrsIconButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.IconButtonColors =
        androidx.compose.material3.IconButtonDefaults.iconButtonColors(),
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    shape: androidx.compose.ui.graphics.Shape =
        androidx.compose.material3.IconButtonDefaults.standardShape,
    size: androidx.compose.ui.unit.DpSize = androidx.compose.ui.unit.DpSize(52.dp, 30.dp),
    contentDescription: String? = null,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val source = interactionSource ?: androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val containerColor = when {
        enabled -> colors.containerColor
        else -> colors.disabledContainerColor
    }
    val innerColor = when {
        enabled -> colors.contentColor
        else -> colors.disabledContentColor
    }
    androidx.compose.foundation.layout.Box(
        contentAlignment = androidx.compose.ui.Alignment.Center,
        modifier = modifier
            .size(size = size)
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .combinedClickable(
                onClick = onClick, onLongClick = onLongClick, enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                interactionSource = source, indication = androidx.compose.material3.ripple(),
            ),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides innerColor,
            content = content,
        )
    }
}
