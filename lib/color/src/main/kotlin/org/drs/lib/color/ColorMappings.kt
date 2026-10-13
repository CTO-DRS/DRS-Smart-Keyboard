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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.DynamicScheme
import org.drs.lib.android.AndroidVersion

/**
 * Standby accent for environments without a platform accent and without an
 * explicit caller default — Material Green 500.
 */
val DEFAULT_GREEN = Color(red = 0x4C, green = 0xAF, blue = 0x50)

/**
 * Accent swatch catalogue — the Material tone-500 row kept as packed ARGB
 * words and decoded on demand. Element order is part of the long-standing
 * public contract of [ColorMappings.colors] and must not be reshuffled.
 */
private val SWATCH_ARGB_500 = intArrayOf(
    0xFF4CAF50.toInt(), 0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFFFF2C93.toInt(),
    0xFF9C27B0.toInt(), 0xFF673AB7.toInt(), 0xFF3F51B5.toInt(), 0xFF2196F3.toInt(),
    0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(), 0xFF009688.toInt(), 0xFF8BC34A.toInt(),
    0xFFCDDC39.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(),
    0xFF795548.toInt(), 0xFF607D8B.toInt(), 0xFF9E9E9E.toInt(),
)

object ColorMappings {
    /** Decoded swatch palette, positionally aligned with [SWATCH_ARGB_500]. */
    val colors: Array<Color> = Array(SWATCH_ARGB_500.size) { index ->
        Color(color = SWATCH_ARGB_500[index])
    }
}

/**
 * Reads the platform accent (system_accent1_500) under the ambient theme.
 * Available on S+ only — call sites must be gated on
 * [org.drs.lib.android.AndroidVersion.ATLEAST_API31_S].
 */
@Composable
@RequiresApi(Build.VERSION_CODES.S)
fun getSystemAccent(): Color {
    val resources = LocalResources.current
    val theme = LocalContext.current.theme
    return Color(color = resources.getColor(android.R.color.system_accent1_500, theme))
}

/**
 * Resolution order: caller-supplied default, then the platform accent on
 * S+, then [DEFAULT_GREEN].
 */
@Composable
fun systemAccentOrDefault(default: Color): Color {
    if (!default.isUnspecified) {
        return default
    }
    return if (AndroidVersion.ATLEAST_API31_S) getSystemAccent() else DEFAULT_GREEN
}

/**
 * Dynamic scheme builder pinned to [PaletteStyle.TonalSpot] — the Material
 * You standard that softly tints the entire surface system from one seed,
 * rather than the flatter feel of the plain neutral ramp.
 */
fun neutralDynamicColorScheme(
    primary: Color, isDark: Boolean, isAmoled: Boolean = false,
    secondary: Color? = null, tertiary: Color? = null, neutral: Color? = null,
    neutralVariant: Color? = null, error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot, contrastLevel: Double = Contrast.Default.value,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default, platform: DynamicScheme.Platform = DynamicScheme.Platform.Default,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme {
    return dynamicColorScheme(
        seedColor = primary, isDark = isDark, isAmoled = isAmoled, primary = primary,
        secondary = secondary, tertiary = tertiary, neutral = neutral,
        neutralVariant = neutralVariant, error = error, style = style,
        contrastLevel = contrastLevel, specVersion = specVersion,
        platform = platform, modifyColorScheme = modifyColorScheme,
    )
}
