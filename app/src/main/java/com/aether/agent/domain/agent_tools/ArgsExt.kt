package com.aether.agent.domain.agent_tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

fun objectSchema(props: Map<String, String> = emptyMap(), required: List<String> = emptyList()): JsonObject =
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            props.forEach { (name, desc) ->
                putJsonObject(name) {
                    put("type", "string")
                    put("description", desc)
                }
            }
        }
        putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
    }
