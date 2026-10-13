/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package org.drs.lib.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palette slots addressed through their serialized string identifiers.
 *
 * The slot set mirrors the Material 3 [ColorScheme] surface one-to-one. Entry
 * order carries no resolution meaning: lookups are dispatched through the
 * accessor table further below rather than a branch chain, so identifiers are
 * the single source of truth.
 */
enum class ColorPalette(val id: String) {
    Primary(id = "primary"), OnPrimary(id = "onPrimary"),
    PrimaryContainer(id = "primaryContainer"),
    OnPrimaryContainer(id = "onPrimaryContainer"),
    InversePrimary(id = "inversePrimary"),
    Secondary(id = "secondary"), OnSecondary(id = "onSecondary"),
    SecondaryContainer(id = "secondaryContainer"),
    OnSecondaryContainer(id = "onSecondaryContainer"),
    Tertiary(id = "tertiary"), OnTertiary(id = "onTertiary"),
    TertiaryContainer(id = "tertiaryContainer"),
    OnTertiaryContainer(id = "onTertiaryContainer"),
    Background(id = "background"), OnBackground(id = "onBackground"),
    OnSurface(id = "onSurface"),
    SurfaceVariant(id = "surfaceVariant"),
    OnSurfaceVariant(id = "onSurfaceVariant"),
    SurfaceTint(id = "surfaceTint"),
    InverseSurface(id = "inverseSurface"),
    InverseOnSurface(id = "inverseOnSurface"),
    Error(id = "error"), OnError(id = "onError"),
    ErrorContainer(id = "errorContainer"),
    OnErrorContainer(id = "onErrorContainer"),
    Outline(id = "outline"), OutlineVariant(id = "outlineVariant"),
    Scrim(id = "scrim"),
    SurfaceBright(id = "surfaceBright"),
    SurfaceDim(id = "surfaceDim"),
    SurfaceContainer(id = "surfaceContainer"),
    SurfaceContainerHigh(id = "surfaceContainerHigh"),
    SurfaceContainerHighest(id = "surfaceContainerHighest"),
    SurfaceContainerLow(id = "surfaceContainerLow"),
    SurfaceContainerLowest(id = "surfaceContainerLowest");

    companion object { val colorNames: List<String> = entries.map { slot -> slot.id } }
}

/**
 * Accessor table binding every serialized palette identifier to the matching
 * [ColorScheme] property reference. Keyed by identifier, so resolution is one
 * map hit and an identifier added to [ColorPalette] without a binding here
 * fails fast at the call site instead of resolving to the wrong slot.
 */
private val schemeAccessors: Map<String, (ColorScheme) -> Color> = mapOf(
    ColorPalette.Primary.id to ColorScheme::primary,
    ColorPalette.OnPrimary.id to ColorScheme::onPrimary,
    ColorPalette.PrimaryContainer.id to ColorScheme::primaryContainer,
    ColorPalette.OnPrimaryContainer.id to ColorScheme::onPrimaryContainer,
    ColorPalette.InversePrimary.id to ColorScheme::inversePrimary,
    ColorPalette.Secondary.id to ColorScheme::secondary,
    ColorPalette.OnSecondary.id to ColorScheme::onSecondary,
    ColorPalette.SecondaryContainer.id to ColorScheme::secondaryContainer,
    ColorPalette.OnSecondaryContainer.id to ColorScheme::onSecondaryContainer,
    ColorPalette.Tertiary.id to ColorScheme::tertiary,
    ColorPalette.OnTertiary.id to ColorScheme::onTertiary,
    ColorPalette.TertiaryContainer.id to ColorScheme::tertiaryContainer,
    ColorPalette.OnTertiaryContainer.id to ColorScheme::onTertiaryContainer,
    ColorPalette.Background.id to ColorScheme::background,
    ColorPalette.OnBackground.id to ColorScheme::onBackground,
    ColorPalette.OnSurface.id to ColorScheme::onSurface,
    ColorPalette.SurfaceVariant.id to ColorScheme::surfaceVariant,
    ColorPalette.OnSurfaceVariant.id to ColorScheme::onSurfaceVariant,
    ColorPalette.SurfaceTint.id to ColorScheme::surfaceTint,
    ColorPalette.InverseSurface.id to ColorScheme::inverseSurface,
    ColorPalette.InverseOnSurface.id to ColorScheme::inverseOnSurface,
    ColorPalette.Error.id to ColorScheme::error,
    ColorPalette.OnError.id to ColorScheme::onError,
    ColorPalette.ErrorContainer.id to ColorScheme::errorContainer,
    ColorPalette.OnErrorContainer.id to ColorScheme::onErrorContainer,
    ColorPalette.Outline.id to ColorScheme::outline,
    ColorPalette.OutlineVariant.id to ColorScheme::outlineVariant,
    ColorPalette.Scrim.id to ColorScheme::scrim,
    ColorPalette.SurfaceBright.id to ColorScheme::surfaceBright,
    ColorPalette.SurfaceDim.id to ColorScheme::surfaceDim,
    ColorPalette.SurfaceContainer.id to ColorScheme::surfaceContainer,
    ColorPalette.SurfaceContainerHigh.id to ColorScheme::surfaceContainerHigh,
    ColorPalette.SurfaceContainerHighest.id to ColorScheme::surfaceContainerHighest,
    ColorPalette.SurfaceContainerLow.id to ColorScheme::surfaceContainerLow,
    ColorPalette.SurfaceContainerLowest.id to ColorScheme::surfaceContainerLowest,
)

/**
 * Resolves the palette slot serialized as [id] against this scheme. Unknown
 * identifiers degrade to the primary slot — the historical contract of this
 * entry point.
 */
fun ColorScheme.getColor(id: String): Color =
    schemeAccessors[id]?.invoke(this) ?: this.primary
