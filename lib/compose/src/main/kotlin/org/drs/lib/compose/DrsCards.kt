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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared metrics and paddings for every DRS card surface. */
object DrsCardDefaults {
    val IconRequiredSize = 24.dp
    val IconSpacing = 12.dp

    val ContentPadding =
        androidx.compose.foundation.layout.PaddingValues(start = 0.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
}

/** Defaults of the outlined box family. */
object BoxDefaults { val OutlinedBoxShape =
    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    val ContentPadding = androidx.compose.foundation.layout.PaddingValues(all = 0.dp)
}

/** Builds the mandatory-icon slot used by the semantic card variants. */
@androidx.compose.runtime.Composable
private fun semanticCardIcon(image: androidx.compose.ui.graphics.vector.ImageVector):
    @androidx.compose.runtime.Composable () -> Unit = {
        androidx.compose.material3.Icon(
            imageVector = image,
            modifier = androidx.compose.ui.Modifier
                .padding(all = DrsCardDefaults.IconSpacing)
                .requiredSize(size = DrsCardDefaults.IconRequiredSize),
            contentDescription = null,
        )
    }

/** One-line (optionally two-line) card row with optional leading icon slot. */
@androidx.compose.runtime.Composable
fun DrsSimpleCard(
    text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    secondaryText: String? = null,
    backgroundColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
    contentColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = DrsCardDefaults.ContentPadding,
    icon: (@androidx.compose.runtime.Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickAction = onClick ?: { }
    val rowStartInset = if (icon == null) 16.dp else 0.dp
    androidx.compose.material3.Card(
        onClick = clickAction, modifier = modifier.fillMaxWidth(), enabled = onClick != null,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            contentColor = contentColor, containerColor = backgroundColor,
            disabledContainerColor = backgroundColor, disabledContentColor = contentColor,
        ),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.padding(contentPadding),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            icon?.invoke()
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .weight(weight = 1f)
                    .padding(start = rowStartInset),
            ) {
                androidx.compose.material3.Text(
                    text = text, modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                secondaryText?.let { secondary ->
                    androidx.compose.material3.Text(
                        text = secondary, modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/**
 * Tonal error card: the theme's error container instead of a raw red block —
 * a calm, accessible error surface harmonizing with the dynamic palette.
 */
@androidx.compose.runtime.Composable
fun DrsErrorCard(
    text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    showIcon: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = DrsCardDefaults.ContentPadding,
    onClick: (() -> Unit)? = null,
) {
    DrsSimpleCard(
        text = text, modifier = modifier, onClick = onClick,
        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
        contentPadding = contentPadding,
        icon = if (showIcon) semanticCardIcon(Icons.Filled.ErrorOutline) else null,
    )
}

/**
 * Adaptive tonal amber warning card — soft in light mode, deep honey in dark
 * mode — replacing the harsh raw yellow block of earlier revisions.
 */
@androidx.compose.runtime.Composable
fun DrsWarningCard(
    text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    showIcon: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = DrsCardDefaults.ContentPadding,
    onClick: (() -> Unit)? = null,
) {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val amberContainer = when {
        isDark -> androidx.compose.ui.graphics.Color(0xFF43380A)
        else -> androidx.compose.ui.graphics.Color(0xFFFFF0B3)
    }
    val amberOnContainer = when {
        isDark -> androidx.compose.ui.graphics.Color(0xFFFFE082)
        else -> androidx.compose.ui.graphics.Color(0xFF43310A)
    }
    DrsSimpleCard(
        text = text, modifier = modifier, onClick = onClick,
        backgroundColor = amberContainer, contentColor = amberOnContainer,
        contentPadding = contentPadding,
        icon = if (showIcon) semanticCardIcon(Icons.Outlined.Warning) else null,
    )
}

/** Neutral informational card on the default surface container. */
@androidx.compose.runtime.Composable
fun DrsInfoCard(
    text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    showIcon: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = DrsCardDefaults.ContentPadding,
    onClick: (() -> Unit)? = null,
) {
    DrsSimpleCard(
        text = text, modifier = modifier, onClick = onClick,
        contentPadding = contentPadding,
        icon = if (showIcon) semanticCardIcon(Icons.Filled.Info) else null,
    )
}

/**
 * Outlined box whose title/subtitle are plain strings; both are mapped onto
 * the lambda overload, with clickable titles wired to their callbacks.
 */
@androidx.compose.runtime.Composable
fun DrsOutlinedBox(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    title: String, onTitleClick: (() -> Unit)? = null,
    subtitle: String? = null, onSubtitleClick: (() -> Unit)? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
    shape: androidx.compose.ui.graphics.Shape = BoxDefaults.OutlinedBoxShape,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = BoxDefaults.ContentPadding,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DrsOutlinedBox(
        modifier = modifier,
        title = { androidx.compose.material3.Text(
            text = title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        ) },
        onTitleClick = onTitleClick,
        subtitle = subtitle?.let { sub ->
            { androidx.compose.material3.Text(
                text = sub,
                modifier = androidx.compose.ui.Modifier
                    .padding(bottom = 4.dp, start = 6.dp, end = 6.dp),
                color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.56f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp,
            ) }
        },
        onSubtitleClick = onSubtitleClick,
        borderWidth = borderWidth, borderColor = borderColor,
        shape = shape, contentPadding = contentPadding, content = content,
    )
}

/**
 * Outlined box with lambda-typed title/subtitle floating on the top border
 * line; title and subtitle are individually clickable when callbacks given.
 */
@androidx.compose.runtime.Composable
fun DrsOutlinedBox(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    title: (@androidx.compose.runtime.Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    subtitle: (@androidx.compose.runtime.Composable () -> Unit)? = null,
    onSubtitleClick: (() -> Unit)? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
    shape: androidx.compose.ui.graphics.Shape = BoxDefaults.OutlinedBoxShape,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = BoxDefaults.ContentPadding,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val hasTitle = title != null
    val titleGap = if (hasTitle) 11.dp else 0.dp
    val hasHeader = title != null && subtitle != null
    androidx.compose.foundation.layout.Box(
        modifier = modifier.padding(top = titleGap),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .border(width = borderWidth, color = borderColor, shape = shape)
                .clip(shape = shape)
                .padding(top = titleGap),
        ) {
            if (hasHeader) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .padding(bottom = 4.dp, start = 10.dp)
                        .rippleClickable(onSubtitleClick != null) {
                            onSubtitleClick?.invoke()
                        },
                ) {
                    subtitle?.invoke()
                }
            }
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(contentPadding),
                content = content,
            )
        }
        if (hasTitle) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .height(height = 23.dp).offset(x = 10.dp, y = (-12).dp)
                    .background(color = androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .rippleClickable(onTitleClick != null) {
                        onTitleClick?.invoke()
                    }
                    .padding(horizontal = 6.dp),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart,
            ) {
                title?.invoke()
            }
        }
    }
}

/** Standard outer spacing for a full-width outlined box. */
fun androidx.compose.ui.Modifier.defaultDrsOutlinedBox(): androidx.compose.ui.Modifier =
    this.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
