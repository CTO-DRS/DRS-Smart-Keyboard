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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Round-trips a [android.net.Uri] through its canonical string form, so
 * serialized payloads always carry the platform-parsed representation.
 */
object UriSerializer : KSerializer<android.net.Uri>
{
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor
        get() = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
            serialName = "URI",
            kind = kotlinx.serialization.descriptors.PrimitiveKind.STRING,
        )

    override fun deserialize(
        decoder: Decoder,
    ): android.net.Uri =
        android.net.Uri.parse(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: android.net.Uri,
    ) {
        encoder.encodeString(
            value = value.toString(),
        )
    }
}
