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

package org.drs.lib.snygg.value

import org.junit.jupiter.api.assertAll
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggUriValueTest {
    private val encoder = SnyggUriValue

    @Test
    fun `deserialize uri values`() {
        val pairs = listOf(
            // valid
            "uri(`flex:/my_image.png`)" to SnyggUriValue("flex:/my_image.png"),
            "uri(`flex:/roboto.ttf`)" to SnyggUriValue("flex:/roboto.ttf"),
            // invalid
            "some-color" to null,
        )
        assertAll(pairs.map { (raw, expected) -> {
            assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
        } })
    }

    @Test
    fun `serialize uri values`() {
        val pairs = listOf(
            // valid
            SnyggUriValue("flex:/my_image.png") to "uri(`flex:/my_image.png`)",
            SnyggUriValue("flex:/roboto.ttf") to "uri(`flex:/roboto.ttf`)",
            // invalid
            SnyggDefinedVarValue("shenanigans") to null
        )
        assertAll(pairs.map { (snyggValue, expected) -> {
            assertEquals(expected, encoder.serialize(snyggValue).getOrNull(), "serialize $snyggValue")
        } })
    }

    @Test
    fun `check class of default value`() {
        assertIs<SnyggUriValue>(encoder.defaultValue())
    }
}
