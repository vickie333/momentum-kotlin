package com.momentum.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

val MomentumJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
val MomentumPrettyJson = Json(MomentumJson) {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun JsonElement.sortedKeys(): JsonElement = when (this) {
    is JsonObject -> JsonObject(entries.sortedBy { it.key }.associate { it.key to it.value.sortedKeys() })
    is JsonArray -> JsonArray(map { it.sortedKeys() })
    else -> this
}
