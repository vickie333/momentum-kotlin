package com.momentum

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.*

fun assertJsonEquivalent(expected: JsonElement, actual: JsonElement, path: String = "$") {
    when {
        expected is JsonObject && actual is JsonObject -> {
            assertEquals(expected.keys, actual.keys, "keys at $path")
            expected.forEach { (key, value) -> assertJsonEquivalent(value, actual.getValue(key), "$path.$key") }
        }
        expected is JsonArray && actual is JsonArray -> {
            assertEquals(expected.size, actual.size, "length at $path")
            expected.forEachIndexed { i, value -> assertJsonEquivalent(value, actual[i], "$path[$i]") }
        }
        expected is JsonPrimitive && actual is JsonPrimitive -> {
            val a = if(!expected.isString) expected.doubleOrNull else null
            val b = if(!actual.isString) actual.doubleOrNull else null
            if(a != null && b != null) assertTrue(abs(a-b) <= 1e-9, "number at $path: $a != $b")
            else { assertEquals(expected.isString, actual.isString, "type at $path"); assertEquals(expected.content, actual.content, "value at $path") }
        }
        else -> assertEquals(expected, actual, "value at $path")
    }
}
