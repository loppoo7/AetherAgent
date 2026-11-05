package com.aether.agent.data.api.llm

import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.model.AgentSettings
import com.aether.agent.domain.model.ApiStyle
import com.aether.agent.domain.model.LlmMessage
import com.aether.agent.domain.model.LlmReply
import com.aether.agent.domain.model.ToolCall
import com.aether.agent.domain.model.ToolSpec
import com.aether.agent.domain.repository.LlmGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Gateway untuk OpenAI-compatible, Anthropic Messages, dan Google Gemini generateContent. */
@Singleton
class LlmGatewayImpl @Inject constructor(private val http: HttpClient) : LlmGateway {

    override suspend fun complete(
        settings: AgentSettings,
        system: String,
        messages: List<LlmMessage>,
        tools: List<ToolSpec>,
    ): LlmReply {
        require(settings.apiKey.isNotBlank()) { "API key ${settings.provider.label} belum diisi di Pengaturan." }
        return readableErrors {
            when (settings.provider.style) {
                ApiStyle.OPENAI_COMPAT -> try {
                    openAiCompatible(settings, system, messages, tools)
                } catch (e: ResponseException) {
                    val detail = runCatching { e.response.bodyAsText() }.getOrDefault("")
                    if (e.response.status.value == 400 && detail.contains("temperature", ignoreCase = true)) {
                        openAiCompatible(settings, system, messages, tools, useTemperature = false)
                    } else {
                        throw IllegalStateException("HTTP ${e.response.status.value}: ${detail.take(300)}", e)
                    }
                }
                ApiStyle.ANTHROPIC -> anthropic(settings, system, messages, tools)
                ApiStyle.GEMINI -> gemini(settings, system, messages, tools)
            }
        }
    }

    private suspend fun openAiCompatible(
        s: AgentSettings, system: String, messages: List<LlmMessage>, tools: List<ToolSpec>,
        useTemperature: Boolean = true,
    ): LlmReply {
        val body = buildJsonObject {
            put("model", s.model)
            if (useTemperature) put("temperature", s.answerStyle.temperature)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                messages.forEach { add(it.toOpenAi()) }
            }
            if (tools.isNotEmpty()) putJsonArray("tools") {
                tools.forEach { t -> add(buildJsonObject {
                    put("type", "function")
                    putJsonObject("function") {
                        put("name", t.name); put("description", t.description); put("parameters", geminiSchema(t.parameters).jsonObject)
                    }
                }) }
            }
        }
        val res = http.post(s.provider.baseUrl.trimEnd('/') + "/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${s.apiKey}")
            contentType(ContentType.Application.Json); setBody(body.toString())
        }.body<JsonObject>()
        val msg = res["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: error("Respons provider tidak valid.")
        val text = msg["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val calls = msg["tool_calls"]?.jsonArray?.map { c ->
            val o = c.jsonObject; val fn = o["function"]!!.jsonObject
            ToolCall(o["id"]!!.jsonPrimitive.content, fn["name"]!!.jsonPrimitive.content,
                parseArgs(fn["arguments"]?.jsonPrimitive?.contentOrNull))
        }.orEmpty()
        return LlmReply(text, calls)
    }

    private suspend fun anthropic(
        s: AgentSettings, system: String, messages: List<LlmMessage>, tools: List<ToolSpec>,
    ): LlmReply {
        val body = buildJsonObject {
            put("model", s.model); put("max_tokens", 4096)
            put("temperature", minOf(s.answerStyle.temperature, 1.0)); put("system", system)
            putJsonArray("messages") { anthropicMessages(messages).forEach { add(it) } }
            if (tools.isNotEmpty()) putJsonArray("tools") {
                tools.forEach { t -> add(buildJsonObject {
                    put("name", t.name); put("description", t.description); put("input_schema", t.parameters)
                }) }
            }
        }
        val res = http.post(s.provider.baseUrl.trimEnd('/') + "/messages") {
            header("x-api-key", s.apiKey); header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json); setBody(body.toString())
        }.body<JsonObject>()
        val blocks = res["content"]?.jsonArray ?: error("Respons provider tidak valid.")
        val text = blocks.filter { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
            .joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }
        val calls = blocks.filter { it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use" }.map { b ->
            val o = b.jsonObject
            ToolCall(o["id"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content,
                o["input"]?.jsonObject ?: JsonObject(emptyMap()))
        }
        return LlmReply(text, calls)
    }

    /**
     * Gemini generateContent. Gemini 3.8 does not accept legacy temperature sampling
     * parameters; omit generationConfig and use the model's stable defaults.
     */
    private suspend fun gemini(
        s: AgentSettings, system: String, messages: List<LlmMessage>, tools: List<ToolSpec>,
    ): LlmReply {
        val model = s.model.trim().removePrefix("models/")
        require(model.matches(Regex("[A-Za-z0-9._-]+"))) { "Nama model Gemini tidak valid." }
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", system) }) }
            }
            putJsonArray("contents") { geminiContents(messages).forEach { add(it) } }
            if (tools.isNotEmpty()) putJsonArray("tools") {
                add(buildJsonObject {
                    putJsonArray("functionDeclarations") {
                        tools.forEach { t -> add(buildJsonObject {
                            put("name", t.name); put("description", t.description); put("parameters", t.parameters)
                        }) }
                    }
                })
            }
        }
        val res = http.post("${s.provider.baseUrl.trimEnd('/')}/models/$model:generateContent") {
            header("x-goog-api-key", s.apiKey)
            contentType(ContentType.Application.Json); setBody(body.toString())
        }.body<JsonObject>()

        val parts = res["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?: error("Gemini tidak mengembalikan kandidat jawaban. Periksa model, kuota, atau safety settings.")
        val text = parts.mapNotNull { p ->
            p.jsonObject["text"]?.jsonPrimitive?.contentOrNull
        }.joinToString("")
        val calls = parts.mapIndexedNotNull { index, part ->
            val call = part.jsonObject["functionCall"]?.jsonObject ?: return@mapIndexedNotNull null
            val name = call["name"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
            ToolCall("gemini-$index-$name", name, call["args"]?.jsonObject ?: JsonObject(emptyMap()), part.jsonObject["thoughtSignature"]?.jsonPrimitive?.contentOrNull)
        }
        if (text.isBlank() && calls.isEmpty()) error("Gemini mengembalikan respons kosong.")
        return LlmReply(text, calls)
    }

    private fun geminiContents(messages: List<LlmMessage>): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun push(role: String, parts: List<JsonObject>) {
            val last = out.lastOrNull()
            if (last != null && last["role"]?.jsonPrimitive?.content == role) {
                val merged = last["parts"]!!.jsonArray + parts
                out[out.lastIndex] = buildJsonObject {
                    put("role", role); put("parts", JsonArray(merged))
                }
            } else {
                out += buildJsonObject { put("role", role); putJsonArray("parts") { parts.forEach { add(it) } } }
            }
        }
        messages.forEach { m ->
            when (m) {
                is LlmMessage.User -> push("user", listOf(geminiTextPart(m.text)))
                is LlmMessage.Assistant -> {
                    val parts = buildList {
                        if (m.text.isNotBlank()) add(geminiTextPart(m.text))
                        m.toolCalls.forEach { call ->
                            add(buildJsonObject {
                                putJsonObject("functionCall") {
                                    put("name", call.name); put("args", call.arguments)
                                }
                                call.thoughtSignature?.let { put("thoughtSignature", it) }
                            })
                        }
                    }
                    push("model", parts.ifEmpty { listOf(geminiTextPart("(kosong)")) })
                }
                is LlmMessage.ToolResult -> push("user", listOf(buildJsonObject {
                    putJsonObject("functionResponse") {
                        put("name", m.name)
                        put("response", buildJsonObject { put("result", m.output) })
                        put("call_id", m.callId)
                    }
                }))
            }
        }
        return out
    }

    private fun geminiSchema(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> buildJsonObject {
            element.forEach { (key, value) ->
                if (key == "type" && value is kotlinx.serialization.json.JsonPrimitive) {
                    val type = value.contentOrNull
                    put(key, if (type in setOf("object", "string", "array", "number", "integer", "boolean")) type!!.uppercase() else value)
                } else put(key, geminiSchema(value))
            }
        }
        is JsonArray -> JsonArray(element.map(::geminiSchema))
        else -> element
    }

    private fun geminiTextPart(text: String): JsonObject = buildJsonObject { put("text", text) }

    private fun LlmMessage.toOpenAi(): JsonObject = when (val m = this) {
        is LlmMessage.User -> buildJsonObject { put("role", "user"); put("content", m.text) }
        is LlmMessage.Assistant -> buildJsonObject {
            put("role", "assistant"); put("content", m.text)
            if (m.toolCalls.isNotEmpty()) putJsonArray("tool_calls") {
                m.toolCalls.forEach { c -> add(buildJsonObject {
                    put("id", c.id); put("type", "function")
                    putJsonObject("function") { put("name", c.name); put("arguments", c.arguments.toString()) }
                }) }
            }
        }
        is LlmMessage.ToolResult -> buildJsonObject {
            put("role", "tool"); put("tool_call_id", m.callId); put("content", m.output)
        }
    }

    /** Anthropic mewajibkan role bergantian; hasil tool digabung ke pesan user yang sama. */
    private fun anthropicMessages(messages: List<LlmMessage>): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun push(role: String, blocks: List<JsonObject>) {
            val last = out.lastOrNull()
            if (last != null && last["role"]?.jsonPrimitive?.content == role) {
                val merged: List<JsonElement> = last["content"]!!.jsonArray + blocks
                out[out.lastIndex] = message(role, merged)
            } else out += message(role, blocks)
        }
        for (m in messages) when (m) {
            is LlmMessage.User -> push("user", listOf(textBlock(m.text)))
            is LlmMessage.Assistant -> {
                val blocks = buildList<JsonObject> {
                    if (m.text.isNotBlank()) add(textBlock(m.text))
                    m.toolCalls.forEach { c -> add(buildJsonObject {
                        put("type", "tool_use"); put("id", c.id); put("name", c.name); put("input", c.arguments)
                    }) }
                }
                push("assistant", blocks.ifEmpty { listOf(textBlock("(kosong)")) })
            }
            is LlmMessage.ToolResult -> push("user", listOf(buildJsonObject {
                put("type", "tool_result"); put("tool_use_id", m.callId); put("content", m.output)
            }))
        }
        return out
    }

    private fun message(role: String, content: List<JsonElement>): JsonObject = buildJsonObject {
        put("role", role); put("content", JsonArray(content))
    }
    private fun textBlock(text: String): JsonObject = buildJsonObject { put("type", "text"); put("text", text) }
    private fun parseArgs(raw: String?): JsonObject =
        runCatching { Json.parseToJsonElement(raw?.takeIf { it.isNotBlank() } ?: "{}").jsonObject }
            .getOrElse { JsonObject(emptyMap()) }
}
