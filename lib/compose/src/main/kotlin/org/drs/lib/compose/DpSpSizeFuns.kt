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

package org.drs.lib.compose

import androidx.compose.ui.unit.*

/**
 * Scales the receiver by [factor], collapsing unspecified text units to
 * [fallback] instead of propagating an undefined result through layout.
 */
private fun TextUnit.timesOr(fallback: TextUnit, factor: Float): TextUnit =
    when {
        isUnspecified -> fallback
        else -> this * factor
    }

infix fun TextUnit.safeTimes(other: Float): TextUnit = timesOr(0.sp, other)

infix fun TextUnit.safeTimes(other: Double): TextUnit = when {
    isUnspecified -> this
    else -> this * other
}

infix fun TextUnit.safeTimes(other: Int): TextUnit = when {
    isUnspecified -> this
    else -> this * other
}

val DpSizeSaver: androidx.compose.runtime.saveable.Saver<androidx.compose.ui.unit.Dp, Float> =
    androidx.compose.runtime.saveable.Saver(
        save = { it.value },
        restore = { it.dp },
    )

context(density: androidx.compose.ui.unit.Density)
fun androidx.compose.ui.unit.IntRect.toDpRect(): androidx.compose.ui.unit.DpRect =
    with(density) {
        androidx.compose.ui.unit.DpRect(
            left.toDp(), top.toDp(),
            right.toDp(), bottom.toDp(),
        )
    }

context(density: androidx.compose.ui.unit.Density)
fun androidx.compose.ui.geometry.Offset.toDp(): androidx.compose.ui.unit.DpOffset =
    with(density) {
        androidx.compose.ui.unit.DpOffset(
            x.toDp(),
            y.toDp(),
        )
    }

object DpSizeSerializer : kotlinx.serialization.KSerializer<androidx.compose.ui.unit.Dp> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            "DpSize", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT,
        )

    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: androidx.compose.ui.unit.Dp,
    ) = encoder.encodeFloat(value.value)

    override fun deserialize(
        decoder: kotlinx.serialization.encoding.Decoder,
    ): androidx.compose.ui.unit.Dp = decoder.decodeFloat().dp
}
