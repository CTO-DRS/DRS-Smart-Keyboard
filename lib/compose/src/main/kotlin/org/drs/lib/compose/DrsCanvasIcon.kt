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

import androidx.compose.ui.graphics.asImageBitmap

/**
 * Renders a vector/bitmap drawable resource as a compose image by rasterizing
 * it onto an ARGB bitmap first; [iconId] resolves against the ambient context.
 */
@androidx.compose.runtime.Composable
fun DrsCanvasIcon(
    @androidx.annotation.DrawableRes iconId: Int,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, contentDescription: String? = null,
) {
    val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(
        androidx.compose.ui.platform.LocalContext.current.resources,
        iconId, null,
    )
    if (drawable != null) {
        DrsCanvasIcon(drawable, modifier, contentDescription)
    }
}

/**
 * Rasterizes [drawable] at its intrinsic size and displays it; use this
 * overload when the drawable instance is already available at the call site.
 */
@androidx.compose.runtime.Composable
fun DrsCanvasIcon(
    drawable: android.graphics.drawable.Drawable,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, contentDescription: String? = null,
) {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    val bitmap = android.graphics.Bitmap.createBitmap(
        width, height, android.graphics.Bitmap.Config.ARGB_8888,
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.run {
        setBounds(0, 0, bitmap.width, bitmap.height)
        draw(canvas)
    }
    val image = bitmap.asImageBitmap()
    androidx.compose.foundation.Image(
        modifier = modifier, bitmap = image, contentDescription = contentDescription,
    )
}
